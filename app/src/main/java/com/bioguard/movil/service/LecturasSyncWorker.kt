package com.bioguard.movil.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.LecturasSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker automático periódico que envía la cola offline de lecturas del sensor
 * (pasos, glucosa estimada, vitales, GPS, eventos y alertas) hacia el backend.
 * Se programa desde [com.bioguard.movil.BioGuardApplication] cada 15 minutos,
 * y respeta la sesión activa, el interruptor de sincronización y la ventana de lote.
 */
@HiltWorker
class LecturasSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: LecturasSyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando sincronización automática de la cola offline...")
            val result = syncRepository.sincronizarColaOffline("worker_automatico")

            if (result is Resource.Success) {
                Log.i(TAG, "Sincronización completada: ${result.data} envíos")
                Result.success()
            } else {
                Log.w(TAG, "Sincronización falló: ${(result as Resource.Error).message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en LecturasSyncWorker: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "LecturasSyncWorker"
        const val WORK_NAME = "lecturas_offline_sync"

        fun scheduleAutoSync(context: Context) {
            try {
                val syncRequest = PeriodicWorkRequestBuilder<LecturasSyncWorker>(
                    15, TimeUnit.MINUTES
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )

                Log.d(TAG, "Sincronización automática de lecturas programada cada 15 minutos")
            } catch (e: Exception) {
                Log.e(TAG, "Error programando sincronización automática: ${e.message}")
            }
        }

        fun cancelAutoSync(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Sincronización automática de lecturas cancelada")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelando sincronización: ${e.message}")
            }
        }
    }
}
