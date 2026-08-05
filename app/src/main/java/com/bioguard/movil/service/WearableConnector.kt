package com.bioguard.movil.service

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.bioguard.movil.network.LecturaSensorRequest
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.HeartbeatRequest
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.nio.charset.StandardCharsets

data class WatchEventDto(
    val bpm: Float,
    val temperatura: Float,
    val sudoracionGsr: Float,
    val nivelRiesgo: String,
    val timestamp: Long,
    val tipoEvento: String,
    val descripcion: String,
    val probabilidadMl: Double? = null
)

data class WatchAlertDto(
    val tipoAlerta: String,
    val mensaje: String,
    val nivelRiesgo: String,
    val timestamp: Long,
    val bpm: Float,
    val temperatura: Float,
    val sudoracionGsr: Float
)

data class RiskThresholds(
    val criticalBpmHigh: Float = 135f,
    val criticalBpmLow: Float = 39f,
    val criticalTemp: Float = 39.0f,
    val moderateBpm: Float = 105f,
    val moderateTemp: Float = 37.8f,
    val moderateGsr: Float = 65f
)

class WearableConnector(
    private val context: Context,
    private val onReadingReceived: (LecturaSensorRequest) -> Unit,
    private val onEventReceived: (CrearEventoRequest) -> Unit,
    private val onAlertReceived: (CrearAlertaRequest) -> Unit,
    private val onHeartbeatReceived: (HeartbeatRequest) -> Unit
) : MessageClient.OnMessageReceivedListener {

    private val gson = Gson()

    private val dispositivoMac: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() } ?: "telefono-bioguard"
    }

    fun register(messageClient: MessageClient = Wearable.getMessageClient(context)) {
        messageClient.addListener(this)
    }

    fun unregister(messageClient: MessageClient = Wearable.getMessageClient(context)) {
        messageClient.removeListener(this)
    }

    fun sendAlertCommandToWatch(bpm: Float, temperatura: Float, gsr: Float, probability: Float) {
        val payload = """
            {
                "bpm": $bpm,
                "temperatura": $temperatura,
                "gsr": $gsr,
                "probability": $probability
            }
        """.trimIndent()
        
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)
        
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Log.w(TAG, "No hay relojes conectados para enviar comando de alerta")
                return@addOnSuccessListener
            }
            for (node in nodes) {
                messageClient.sendMessage(
                    node.id, 
                    "/commands/alert", 
                    payload.toByteArray(StandardCharsets.UTF_8)
                ).addOnSuccessListener {
                    Log.d(TAG, "Comando de alerta enviado al reloj ${node.displayName}")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Fallo al enviar comando de alerta al reloj ${node.displayName}", e)
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Fallo al obtener nodos conectados para enviar alerta", e)
        }
    }

    fun sendRiskThresholds(thresholds: RiskThresholds = RiskThresholds()) {
        val payload = gson.toJson(thresholds)
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Log.d(TAG, "No hay relojes conectados para enviar umbrales de riesgo")
                return@addOnSuccessListener
            }
            for (node in nodes) {
                messageClient.sendMessage(
                    node.id,
                    "/risk-thresholds",
                    payload.toByteArray(StandardCharsets.UTF_8)
                ).addOnSuccessListener {
                    Log.d(TAG, "Umbrales de riesgo enviados al reloj ${node.displayName}")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Fallo al enviar umbrales de riesgo al reloj ${node.displayName}", e)
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Fallo al obtener nodos conectados para enviar umbrales de riesgo", e)
        }
    }

    fun sendDismissCommandToWatch() {
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)
        
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Log.w(TAG, "No hay relojes conectados para enviar comando de descarte")
                return@addOnSuccessListener
            }
            for (node in nodes) {
                messageClient.sendMessage(
                    node.id, 
                    "/commands/dismiss", 
                    ByteArray(0)
                ).addOnSuccessListener {
                    Log.d(TAG, "Comando de descarte enviado al reloj ${node.displayName}")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Fallo al enviar comando de descarte al reloj ${node.displayName}", e)
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Fallo al obtener nodos conectados para enviar descarte", e)
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        try {
            val jsonString = String(event.data, StandardCharsets.UTF_8)
            when (event.path) {
                "/sensors/readings" -> {
                    val request = gson.fromJson(jsonString, LecturaSensorRequest::class.java)
                    onReadingReceived(request)
                }
                "/sensors/events" -> {
                    val dto = gson.fromJson(jsonString, WatchEventDto::class.java)
                    val request = CrearEventoRequest(
                        pacienteId = "",
                        dispositivoMac = dispositivoMac,
                        nivelRiesgo = dto.nivelRiesgo,
                        probabilidadMl = dto.probabilidadMl ?: 0.0,
                        descripcion = "${dto.tipoEvento}: ${dto.descripcion}"
                    )
                    onEventReceived(request)
                }
                "/sensors/alerts" -> {
                    val dto = gson.fromJson(jsonString, WatchAlertDto::class.java)
                    val request = CrearAlertaRequest(
                        pacienteId = "",
                        tipoAlerta = dto.tipoAlerta,
                        descripcion = dto.mensaje,
                        latitud = null,
                        longitud = null
                    )
                    onAlertReceived(request)
                }
                "/sensors/heartbeat" -> {
                    val request = gson.fromJson(jsonString, HeartbeatRequest::class.java)
                    onHeartbeatReceived(request)
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.w(TAG, "JSON malformado del reloj en ${event.path}: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje del reloj en ${event.path}", e)
        }
    }

    companion object {
        private const val TAG = "WearableConnector"
    }
}
