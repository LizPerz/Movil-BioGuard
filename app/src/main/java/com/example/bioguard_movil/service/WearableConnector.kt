package com.example.bioguard_movil.service

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.Manifest
import android.content.pm.PackageManager
import com.example.bioguard_movil.network.LecturaSensorRequest
import com.example.bioguard_movil.network.CrearEventoRequest
import com.example.bioguard_movil.network.CrearAlertaRequest
import com.example.bioguard_movil.network.HeartbeatRequest
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import java.nio.charset.StandardCharsets

data class WatchEventDto(
    val bpm: Float,
    val temperatura: Float,
    val sudoracionGsr: Float,
    val nivelRiesgo: String,
    val timestamp: Long,
    val tipoEvento: String,
    val descripcion: String
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

class WearableConnector(
    private val context: Context,
    private val onReadingReceived: (LecturaSensorRequest) -> Unit,
    private val onEventReceived: (CrearEventoRequest) -> Unit,
    private val onAlertReceived: (CrearAlertaRequest) -> Unit,
    private val onHeartbeatReceived: (HeartbeatRequest) -> Unit
) : MessageClient.OnMessageReceivedListener {

    private val gson = Gson()
    private var lastAlertTime: Long = 0
    private val ALERT_COOLDOWN_MS = 30000L

    fun register(messageClient: MessageClient = Wearable.getMessageClient(context)) {
        messageClient.addListener(this)
    }

    fun unregister(messageClient: MessageClient = Wearable.getMessageClient(context)) {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(event: MessageEvent) {
        try {
            val jsonString = String(event.data, StandardCharsets.UTF_8)
            when (event.path) {
                "/sensors/readings" -> {
                    try {
                        val rawRequest = gson.fromJson(jsonString, LecturaSensorRequest::class.java)
                        val gsrConverted = convertGsrScoreToUs(rawRequest.sudoracionGsr)
                        val request = rawRequest.copy(sudoracionGsr = gsrConverted)
                        onReadingReceived(request)
                    } catch (e: Exception) {
                        println("Error al parsear lectura del reloj: ${e.message}")
                    }
                }
                "/sensors/events" -> {
                    try {
                        val dto = gson.fromJson(jsonString, WatchEventDto::class.java)
                        val request = CrearEventoRequest(
                            pacienteId = "",
                            dispositivoMac = getDeviceMac(context),
                            nivelRiesgo = dto.nivelRiesgo,
                            probabilidadMl = getProbabilidadMl(dto.nivelRiesgo),
                            descripcion = "${dto.tipoEvento}: ${dto.descripcion}"
                        )
                        onEventReceived(request)
                    } catch (e: Exception) {
                        println("Error al parsear evento del reloj: ${e.message}")
                    }
                }
                "/sensors/alerts" -> {
                    try {
                        val now = System.currentTimeMillis()
                        if (now - lastAlertTime < ALERT_COOLDOWN_MS) {
                            println("Alerta ignorada por cooldown")
                            return
                        }
                        lastAlertTime = now
                        val dto = gson.fromJson(jsonString, WatchAlertDto::class.java)
                        val request = CrearAlertaRequest(
                            pacienteId = "",
                            tipoAlerta = dto.tipoAlerta,
                            descripcion = dto.mensaje,
                            latitud = null,
                            longitud = null
                        )
                        onAlertReceived(request)
                    } catch (e: Exception) {
                        println("Error al parsear alerta del reloj: ${e.message}")
                    }
                }
                "/sensors/heartbeat" -> {
                    try {
                        val request = gson.fromJson(jsonString, HeartbeatRequest::class.java)
                        onHeartbeatReceived(request)
                    } catch (e: Exception) {
                        println("Error al parsear heartbeat del reloj: ${e.message}")
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun getDeviceMac(context: Context): String {
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            return wifiInfo.macAddress ?: ""
        } catch (e: Exception) {
            println("Error al obtener MAC del dispositivo: ${e.message}")
            return ""
        }
    }

    private fun getProbabilidadMl(nivelRiesgo: String): Double {
        return when (nivelRiesgo.lowercase()) {
            "bajo" -> 0.1
            "moderado" -> 0.5
            "alto", "critico" -> 1.0
            else -> 0.0
        }
    }

    private fun convertGsrScoreToUs(score: Double): Double {
        if (score <= 0.0) return 0.0
        return (score / 100.0 * 6.0).coerceIn(0.0, 6.0)
    }
}
