package com.bioguard.movil.service.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bioguard.movil.data.local.BioGuardDatabase
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.LecturaSensorRequest
import com.bioguard.movil.network.RetrofitClient
import com.bioguard.movil.network.TrackingGpsRequest
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val db = BioGuardDatabase.getInstance(context)
        val prefs = UserPreferences(context)
        val patientId = prefs.patientId.first() ?: return Result.success()
        val api = RetrofitClient.api

        var hasFailures = false

        // 1. Sync readings batch
        val pendingReadings = db.pendingDataDao().getPendingReadings(100)
        if (pendingReadings.isNotEmpty()) {
            try {
                val requests = pendingReadings.map {
                    LecturaSensorRequest(
                        pulsoBpm = it.pulsoBpm,
                        temperaturaC = it.temperaturaC,
                        sudoracionGsr = it.sudoracionGsr,
                        hrv = it.hrv,
                        spo2 = it.spo2,
                        timestamp = it.timestamp
                    )
                }
                api.sendLecturasBatch(requests)
                db.pendingDataDao().deleteReadings(pendingReadings.map { it.id })
            } catch (_: Exception) {
                hasFailures = true
            }
        }

        // 2. Sync GPS tracking batch
        val pendingGps = db.pendingDataDao().getPendingGps(100)
        if (pendingGps.isNotEmpty()) {
            try {
                val requests = pendingGps.map {
                    TrackingGpsRequest(
                        latitud = it.latitud,
                        longitud = it.longitud,
                        esEmergencia = it.esEmergencia
                    )
                }
                api.sendTrackingBatch(requests)
                db.pendingDataDao().deleteGps(pendingGps.map { it.id })
            } catch (_: Exception) {
                hasFailures = true
            }
        }

        // 3. Sync events
        val pendingEvents = db.pendingDataDao().getPendingEvents(50)
        if (pendingEvents.isNotEmpty()) {
            val sent = mutableListOf<Long>()
            for (event in pendingEvents) {
                try {
                    api.sendEvento(
                        CrearEventoRequest(
                            pacienteId = event.pacienteId,
                            dispositivoMac = event.dispositivoMac,
                            nivelRiesgo = event.nivelRiesgo,
                            probabilidadMl = event.probabilidadMl,
                            descripcion = event.descripcion
                        )
                    )
                    sent.add(event.id)
                } catch (_: Exception) {
                    hasFailures = true
                }
            }
            if (sent.isNotEmpty()) db.pendingDataDao().deleteEvents(sent)
        }

        // 4. Sync alerts
        val pendingAlerts = db.pendingDataDao().getPendingAlerts(50)
        if (pendingAlerts.isNotEmpty()) {
            val sent = mutableListOf<Long>()
            for (alert in pendingAlerts) {
                try {
                    api.crearAlerta(
                        CrearAlertaRequest(
                            pacienteId = alert.pacienteId,
                            tipoAlerta = alert.tipoAlerta,
                            descripcion = alert.descripcion,
                            latitud = alert.latitud,
                            longitud = alert.longitud
                        )
                    )
                    sent.add(alert.id)
                } catch (_: Exception) {
                    hasFailures = true
                }
            }
            if (sent.isNotEmpty()) db.pendingDataDao().deleteAlerts(sent)
        }

        return if (hasFailures) Result.retry() else Result.success()
    }
}
