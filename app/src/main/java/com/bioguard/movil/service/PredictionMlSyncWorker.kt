package com.bioguard.movil.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bioguard.movil.data.repository.PredictionMlSyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Worker para sincronizar predicciones ML pendientes en background cada 15 minutos
 * (o cuando hay conectividad disponible)
 */
class PredictionMlSyncWorker(
    context: Context,
    params: androidx.work.WorkerParameters,
    private val syncRepository: PredictionMlSyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando sincronización de predicciones ML...")
            val result = syncRepository.sincronizarLote(limit = 50)
            
            if (result is com.bioguard.movil.data.Resource.Success) {
                val sincronizados = result.data
                Log.i(TAG, "Sincronización completada: $sincronizados predicciones enviadas")
                Result.success()
            } else {
                Log.w(TAG, "Sincronización falló: ${(result as com.bioguard.movil.data.Resource.Error).message}")
                // Reintentar en 15 minutos
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en PredictionMlSyncWorker: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "PredictionMlSyncWorker"
        const val WORK_NAME = "prediction_ml_sync"

        /**
         * Programar sincronización automática cada 15 minutos
         */
        fun scheduleAutoSync(context: Context) {
            try {
                val syncRequest = PeriodicWorkRequestBuilder<PredictionMlSyncWorker>(
                    15, TimeUnit.MINUTES
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
                
                Log.d(TAG, "Sincronización automática programada cada 15 minutos")
            } catch (e: Exception) {
                Log.e(TAG, "Error programando sincronización automática: ${e.message}")
            }
        }

        /**
         * Cancelar sincronización automática
         */
        fun cancelAutoSync(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Sincronización automática cancelada")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelando sincronización: ${e.message}")
            }
        }
    }
}
