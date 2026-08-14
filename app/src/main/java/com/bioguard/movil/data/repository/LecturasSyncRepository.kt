package com.bioguard.movil.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.local.BioGuardDatabase
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.LecturaSensorRequest
import com.bioguard.movil.network.TrackingGpsRequest
import com.bioguard.movil.util.InstallationIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Sincronización de la cola offline de lecturas (pasos, glucosa estimada, vitales, GPS,
 * eventos y alertas) hacia el backend. Usada por el worker automático periódico
 * [com.bioguard.movil.service.LecturasSyncWorker] y reutilizable desde cualquier punto.
 */
@Singleton
class LecturasSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
    private val database: BioGuardDatabase
) {

    private val prefs get() = UserPreferences(context)
    private val dao get() = database.pendingDataDao()

    suspend fun sincronizarColaOffline(trigger: String): Resource<Int> {
        return try {
            if (!prefs.isSyncEnabled.first()) {
                Log.d(TAG, "Sincronización $trigger omitida: sincronización desactivada")
                return Resource.Success(0)
            }
            if (prefs.patientId.first() == null) {
                Log.d(TAG, "Sincronización $trigger omitida: sin paciente vinculado")
                return Resource.Success(0)
            }
            if (prefs.isBatchSyncEnabled.first() && !isInBatchWindow()) {
                Log.d(TAG, "Sincronización $trigger omitida: fuera de la ventana de lote")
                return Resource.Success(0)
            }
            if (!hasValidatedInternet()) {
                Log.d(TAG, "Sin conexión validada; la cola offline se conserva")
                return Resource.Error("Sin conexión validada")
            }

            val enviadas = syncPendingReadings() + syncPendingGps() + syncPendingEvents() + syncPendingAlerts()
            Log.i(TAG, "Sincronización $trigger completada: $enviadas envíos")
            Resource.Success(enviadas)
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización $trigger: ${e.message}", e)
            Resource.Error("Error sincronizando cola offline: ${e.message}")
        }
    }

    private suspend fun isInBatchWindow(): Boolean {
        val startHour = prefs.batchStartHour.first()
        val endHour = prefs.batchEndHour.first()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            currentHour in startHour until endHour
        } else {
            currentHour >= startHour || currentHour < endHour
        }
    }

    private fun hasValidatedInternet(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private suspend fun syncPendingReadings(): Int {
        val patientId = prefs.patientId.first() ?: return 0
        val pending = dao.getPendingReadings(100)
        if (pending.isEmpty()) return 0
        val installId = InstallationIdentity.getOrCreate(context)
        val requests = pending.map {
            LecturaSensorRequest(
                pacienteId = patientId,
                pulsoBpm = it.pulsoBpm,
                temperaturaC = it.temperaturaC,
                estresPct = it.estresPct,
                hrv = it.hrv,
                spo2 = it.spo2,
                pasos = it.pasos,
                glucosaEstimadaMgDl = it.glucosaEstimadaMgDl?.takeIf { g -> g > 0.0 },
                probabilidadPico = it.probabilidadPico,
                timestamp = it.timestamp,
                sourceMessageId = it.sourceMessageId ?: "$installId:reading:${it.id}"
            )
        }
        return try {
            api.sendLecturasBatch(requests)
            dao.deleteReadings(pending.map { it.id })
            requests.size
        } catch (e: Exception) {
            Log.w(TAG, "Error al sincronizar lecturas en lote (se reintentara): ${e.message}")
            0
        }
    }

    private suspend fun syncPendingGps(): Int {
        val patientId = prefs.patientId.first() ?: return 0
        val pending = dao.getPendingGps(100)
        if (pending.isEmpty()) return 0
        val installId = InstallationIdentity.getOrCreate(context)
        val requests = pending.map {
            TrackingGpsRequest(
                pacienteId = patientId,
                latitud = it.latitud,
                longitud = it.longitud,
                esEmergencia = it.esEmergencia,
                sourceMessageId = "$installId:gps:${it.id}"
            )
        }
        return try {
            api.sendTrackingBatch(requests)
            dao.deleteGps(pending.map { it.id })
            requests.size
        } catch (e: Exception) {
            Log.w(TAG, "Error al sincronizar GPS en lote (se reintentara): ${e.message}")
            0
        }
    }

    private suspend fun syncPendingEvents(): Int {
        val pending = dao.getPendingEvents(50)
        if (pending.isEmpty()) return 0
        var enviadas = 0
        for (evento in pending) {
            try {
                api.sendEvento(
                    CrearEventoRequest(
                        pacienteId = evento.pacienteId,
                        dispositivoMac = evento.dispositivoMac,
                        nivelRiesgo = evento.nivelRiesgo,
                        probabilidadMl = evento.probabilidadMl,
                        descripcion = evento.descripcion
                    )
                )
                dao.deleteEvents(listOf(evento.id))
                enviadas++
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar evento offline ${evento.id} (se reintentara): ${e.message}")
            }
        }
        return enviadas
    }

    private suspend fun syncPendingAlerts(): Int {
        val pending = dao.getPendingAlerts(50)
        if (pending.isEmpty()) return 0
        var enviadas = 0
        for (alerta in pending) {
            try {
                api.crearAlerta(
                    CrearAlertaRequest(
                        pacienteId = alerta.pacienteId,
                        tipo = alerta.tipoAlerta,
                        nivel = "CRITICAL",
                        titulo = alerta.tipoAlerta,
                        mensaje = alerta.descripcion
                    )
                )
                dao.deleteAlerts(listOf(alerta.id))
                enviadas++
            } catch (e: Exception) {
                Log.w(TAG, "Error al sincronizar alerta offline ${alerta.id} (se reintentara): ${e.message}")
            }
        }
        return enviadas
    }

    companion object {
        private const val TAG = "LecturasSyncRepository"
    }
}
