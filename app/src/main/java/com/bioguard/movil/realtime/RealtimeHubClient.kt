package com.bioguard.movil.realtime

import android.net.Uri
import android.util.Log
import com.bioguard.movil.network.Constants
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cliente SignalR único para la app. Se conecta a /hubs/bioguard con el token
 * de la sesión, se une al grupo del paciente activo y publica los eventos
 * recibidos (foto/perfil/cuidadores) para que los ViewModels se actualicen
 * en tiempo real sin necesidad de refrescar la pantalla.
 */
@Singleton
class RealtimeHubClient @Inject constructor() {

    private var connection: HubConnection? = null
    private var currentPacienteId: String? = null

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
    }
}
