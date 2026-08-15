package com.bioguard.movil.realtime

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bioguard.movil.network.Constants
import com.bioguard.movil.service.LocalAlertNotifier
import com.microsoft.signalr.Action3
import com.microsoft.signalr.Action5
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cliente SignalR único para la app. Se conecta a /hubs/bioguard con el token
 * de la sesión, se une al grupo del paciente activo y publica los eventos
 * recibidos (foto/perfil/cuidadores/lectura/alerta/ubicación) para que los
 * ViewModels se actualicen en tiempo real sin necesidad de refrescar la pantalla.
 * Las alertas remotas se muestran además como notificación local.
 */
@Singleton
class RealtimeHubClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var connection: HubConnection? = null
    private var currentPacienteId: String? = null
    private val notifier: LocalAlertNotifier by lazy { LocalAlertNotifier(context) }

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun connect(token: String, pacienteId: String) {
        if (pacienteId.isBlank() || currentPacienteId == pacienteId) return
        stop()
        currentPacienteId = pacienteId
        try {
            val url = Constants.BASE_URL + "hubs/bioguard?access_token=" + Uri.encode(token)
            val conn = HubConnectionBuilder.create(url).build()
            conn.on("FotoActualizada", { _events.tryEmit(EventFoto) })
            conn.on("PerfilActualizado", { _events.tryEmit(EventPerfil) })
            conn.on("CuidadoresActualizados", { _events.tryEmit(EventCuidadores) })
            conn.on("AlertaRecibida", Action5 { _: String, _: String, nivel: String, titulo: String, mensaje: String ->
                notifier.notifyRealtimeAlerta(
                    titulo = titulo,
                    mensaje = mensaje,
                    critical = nivel.uppercase().contains("CRITIC")
                )
                _events.tryEmit(EventAlerta)
            }, String::class.java, String::class.java, String::class.java, String::class.java, String::class.java)
            conn.on("LecturaActualizada", Action3 { _: String, _: Double, nivel: String ->
                if (nivel.uppercase() == "ALTO" || nivel.uppercase() == "CRITICO" || nivel.uppercase() == "CRITICAL") {
                    notifier.notifyRealtimeAlerta(
                        titulo = "BioGuard: riesgo glucémico elevado",
                        mensaje = "Nueva lectura del paciente con riesgo ${nivel.uppercase()}. Abre la app para revisarla.",
                        critical = nivel.uppercase().contains("CRITIC")
                    )
                }
                _events.tryEmit(EventLectura)
            }, String::class.java, java.lang.Double::class.java, String::class.java)
            conn.on("UbicacionActualizada", { _events.tryEmit(EventUbicacion) })
            conn.onClosed { error ->
                currentPacienteId = null
                connection = null
                Log.w(TAG, "Conexión SignalR cerrada: ${error?.message}")
            }
            conn.start().blockingAwait()
            conn.invoke("JoinPacienteGroup", pacienteId).blockingAwait()
            connection = conn
            Log.i(TAG, "Conectado al hub, unido al grupo paciente_$pacienteId")
        } catch (e: Exception) {
            currentPacienteId = null
            connection = null
            Log.w(TAG, "No se pudo conectar al hub SignalR: ${e.message}")
        }
    }

    fun stop() {
        try {
            connection?.stop()
        } catch (_: Exception) {
        }
        connection = null
        currentPacienteId = null
    }

    companion object {
        private const val TAG = "RealtimeHub"
        const val EventFoto = "foto"
        const val EventPerfil = "perfil"
        const val EventCuidadores = "cuidadores"
        const val EventLectura = "lectura"
        const val EventAlerta = "alerta"
        const val EventUbicacion = "ubicacion"
    }
}
