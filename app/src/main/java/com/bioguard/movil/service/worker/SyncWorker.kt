package com.bioguard.movil.service.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bioguard.movil.data.local.BioGuardDatabase
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.LecturaSensorRequest
import com.bioguard.movil.network.RetrofitClient
import com.bioguard.movil.network.TrackingGpsRequest
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

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

        var hasTransientFailures = false

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
            } catch (e: HttpException) {
                // 4xx errors mean invalid payload (e.g. 400 Bad Request) -> delete to avoid infinite loop
                if (e.code() in 400..499) {
                    db.pendingDataDao().deleteReadings(pendingReadings.map { it.id })
                } else {
                    hasTransientFailures = true
                }
            } catch (e: IOException) {
                hasTransientFailures = true
            } catch (_: Exception) {
                // Unexpected errors -> purge to prevent lock
                db.pendingDataDao().deleteReadings(pendingReadings.map { it.id })
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
            } catch (e: HttpException) {
                if (e.code() in 400..499) {
                    db.pendingDataDao().deleteGps(pendingGps.map { it.id })
                } else {
                    hasTransientFailures = true
                }
            } catch (e: IOException) {
                hasTransientFailures = true
            } catch (_: Exception) {
                db.pendingDataDao().deleteGps(pendingGps.map { it.id })
            }
        }

        // 3. Sync events
        val pendingEvents = db.pendingDataDao().getPendingEvents(50)
        if (pendingEvents.isNotEmpty()) {
            val sent = mutableListOf<Long>()
            val discard = mutableListOf<Long>()
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
                } catch (e: HttpException) {
                    if (e.code() in 400..499) discard.add(event.id) else hasTransientFailures = true
                } catch (e: IOException) {
                    hasTransientFailures = true
                } catch (_: Exception) {
                    discard.add(event.id)
                }
            }
            if (sent.isNotEmpty()) db.pendingDataDao().deleteEvents(sent)
            if (discard.isNotEmpty()) db.pendingDataDao().deleteEvents(discard)
        }

        // 4. Sync alerts
        val pendingAlerts = db.pendingDataDao().getPendingAlerts(50)
        if (pendingAlerts.isNotEmpty()) {
            val sent = mutableListOf<Long>()
            val discard = mutableListOf<Long>()
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
                } catch (e: HttpException) {
                    if (e.code() in 400..499) discard.add(alert.id) else hasTransientFailures = true
                } catch (e: IOException) {
                    hasTransientFailures = true
                } catch (_: Exception) {
                    discard.add(alert.id)
                }
            }
            if (sent.isNotEmpty()) db.pendingDataDao().deleteAlerts(sent)
            if (discard.isNotEmpty()) db.pendingDataDao().deleteAlerts(discard)
        }

        return if (hasTransientFailures) Result.retry() else Result.success()
    }
}
