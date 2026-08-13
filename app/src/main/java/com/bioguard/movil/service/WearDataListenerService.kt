package com.bioguard.movil.service

import android.content.Context
import android.util.Log
import com.bioguard.movil.data.local.BioGuardDatabase
import com.bioguard.movil.data.local.PendingDataDao
import com.bioguard.movil.data.local.PendingReadingEntity
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.ml.PersonalizedAnomalyModel
import com.bioguard.movil.ml.VitalSample
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.HeartbeatRequest
import com.bioguard.movil.network.LecturaSensorRequest
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

/**
 * WearableListenerService que recibe mensajes del reloj incluso cuando la app
 * no está en primer plano. Actúa como respaldo del listener programático
 * de WearableConnector para mayor resiliencia.
 */
class WearDataListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    companion object {
        private const val TAG = "WearDataListener"
    }

    private val db by lazy { BioGuardDatabase.getInstance(applicationContext) }
    private val pendingDao by lazy { db.pendingDataDao() }
    private val localRiskModel = PersonalizedAnomalyModel()

    override fun onMessageReceived(event: MessageEvent) {
        super.onMessageReceived(event)
        Log.d(TAG, "Mensaje recibido del reloj: path=${event.path}")

        serviceScope.launch {
            try {
                val jsonString = String(event.data, StandardCharsets.UTF_8)

                when (event.path) {
                    "/sensors/readings" -> {
                        handleReading(jsonString, event.path)
                    }
                    "/sensors/events" -> {
                        handleEvent(jsonString)
                    }
                    "/sensors/alerts" -> {
                        handleAlert(jsonString)
                    }
                    "/sensors/heartbeat", "/bioguard/heartbeat" -> {
                        handleHeartbeat(jsonString)
                    }
                    else -> {
                        if (event.path.startsWith("/bioguard/telemetry/")) {
                            handleReading(jsonString, event.path)
                            sendAck(event.path, event.sourceNodeId)
                        } else {
                            Log.w(TAG, "Path no manejado: ${event.path}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando mensaje del reloj: ${e.message}")
            }
        }
    }

    private suspend fun handleReading(json: String, path: String?) {
        val request = gson.fromJson(json, LecturaSensorRequest::class.java)
        if (request.pulsoBpm <= 0.0) {
            Log.w(TAG, "Lectura rechazada: BPM inválido")
            return
        }
        val patientId = UserPreferences(applicationContext).patientId.first() ?: "paciente-local"
        val baseline = db.cachedDataDao().getRecentReadingsSnapshot(patientId).map { cached ->
            VitalSample(
                heartRateBpm = cached.pulsoBpm,
                temperatureC = cached.temperaturaC.takeIf { it > 0.0 },
                gsr = cached.sudoracionGsr.takeIf { it > 0.0 },
                hrvMs = cached.hrv.takeIf { it > 0.0 },
                spo2Percent = cached.spo2.takeIf { it > 0.0 }
            )
        }
        val assessment = localRiskModel.assess(
            current = VitalSample(
                heartRateBpm = request.pulsoBpm,
                temperatureC = request.temperaturaC.takeIf { it > 0.0 },
                gsr = request.sudoracionGsr.takeIf { it > 0.0 },
                hrvMs = request.hrv?.takeIf { it > 0.0 },
                spo2Percent = request.spo2?.takeIf { it > 0.0 }
            ),
            baseline = baseline
        )
        val sourceId = request.sourceMessageId ?: System.currentTimeMillis().toString()
        val entity = PendingReadingEntity(
            sourceMessageId = sourceId,
            pulsoBpm = request.pulsoBpm,
            temperaturaC = request.temperaturaC,
            sudoracionGsr = request.sudoracionGsr,
            hrv = request.hrv ?: 0.0,
            spo2 = request.spo2 ?: 0.0,
            pasos = request.pasos ?: 0,
            glucosaEstimadaMgDl = request.glucosaEstimadaMgDl?.takeIf { it > 0.0 },
            probabilidadPico = (assessment.score / 100.0).coerceIn(0.0, 1.0),
            timestamp = request.timestamp
        )
        pendingDao.insertReading(entity)
        Log.d(TAG, "Lectura guardada en cola pendiente: BPM=${request.pulsoBpm}")
    }

    private suspend fun handleEvent(json: String) {
        data class WatchEventDto(
            val nivelRiesgo: String,
            val probabilidadMl: Double?,
            val tipoEvento: String,
            val descripcion: String
        )
        val dto = gson.fromJson(json, WatchEventDto::class.java)
        Log.d(TAG, "Evento del reloj: ${dto.tipoEvento} - ${dto.descripcion}")
    }

    private suspend fun handleAlert(json: String) {
        data class WatchAlertDto(
            val tipoAlerta: String,
            val mensaje: String,
            val nivelRiesgo: String
        )
        val dto = gson.fromJson(json, WatchAlertDto::class.java)
        Log.w(TAG, "ALERTA del reloj: ${dto.tipoAlerta} - ${dto.mensaje}")
    }

    private suspend fun handleHeartbeat(json: String) {
        val request = gson.fromJson(json, HeartbeatRequest::class.java)
        Log.d(TAG, "Heartbeat del reloj: batería=${request.bateria}%, sensores=${request.sensoresActivos}")
    }

    private suspend fun sendAck(ackPath: String, sourceNodeId: String) {
        try {
            val ackJson = gson.toJson(mapOf("ackPath" to ackPath))
            Wearable.getMessageClient(applicationContext)
                .sendMessage(sourceNodeId, "/bioguard/ack", ackJson.toByteArray(Charsets.UTF_8))
                .addOnSuccessListener {
                    Log.d(TAG, "ACK enviado al reloj: $ackPath")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error enviando ACK: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error enviando ACK: ${e.message}")
        }
    }
}
