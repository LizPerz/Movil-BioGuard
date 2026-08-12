package com.bioguard.movil.data.repository

import android.util.Log
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.local.PendingPredictionMlDao
import com.bioguard.movil.data.local.PendingPredictionMlEntity
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.GuardarPrediccionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionMlSyncRepository @Inject constructor(
    private val api: ApiService,
    private val pendingPredictionMlDao: PendingPredictionMlDao
) {

    /**
     * Guardar predicción ML localmente para envío posterior
     */
    suspend fun guardarLocalmente(request: GuardarPrediccionRequest): Resource<Long> {
        return try {
            val entity = PendingPredictionMlEntity(
                pacienteId = request.pacienteId,
                probabilidadPico = request.probabilidadPico,
                nivelRiesgo = request.nivelRiesgo,
                casoClinico = request.casoClinico,
                accionAutomatizada = request.accionAutomatizada,
                imc = request.imc,
                z = request.z,
                pPico = request.pPico,
                recomendacion = request.recomendacion,
                horasEstimadas = request.horasEstimadas,
                modeloVersion = request.modeloVersion
            )
            val id = pendingPredictionMlDao.insert(entity)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error("Error al guardar predicción: ${e.message}")
        }
    }

    /**
     * Obtener predicciones pendientes de sincronización
     */
    fun getPendientes(): Flow<List<PendingPredictionMlEntity>> {
        return pendingPredictionMlDao.getPendentes()
    }

    /**
     * Contar predicciones pendientes
     */
    fun contarPendientes(): Flow<Int> {
        return pendingPredictionMlDao.contarPendentes()
    }

    /**
     * Sincronizar lote de predicciones con el backend
     */
    suspend fun sincronizarLote(limit: Int = 50): Resource<Int> {
        return try {
            val pendientes = pendingPredictionMlDao.getPendentesSync(limit)
            if (pendientes.isEmpty()) {
                return Resource.Success(0)
            }

            var sincronizados = 0
            var fallos = 0

            for (entity in pendientes) {
                try {
                    val request = GuardarPrediccionRequest(
                        pacienteId = entity.pacienteId,
                        probabilidadPico = entity.probabilidadPico,
                        nivelRiesgo = entity.nivelRiesgo,
                        casoClinico = entity.casoClinico,
                        accionAutomatizada = entity.accionAutomatizada,
                        imc = entity.imc,
                        z = entity.z,
                        pPico = entity.pPico,
                        recomendacion = entity.recomendacion,
                        horasEstimadas = entity.horasEstimadas,
                        modeloVersion = entity.modeloVersion
                    )

                    api.guardarPrediccion(request)
                    pendingPredictionMlDao.marcarSincronizado(entity.id)
                    sincronizados++

                    Log.d("PredictionMlSync", "Predicción ${entity.id} sincronizada exitosamente")
                } catch (e: Exception) {
                    fallos++
                    pendingPredictionMlDao.incrementarIntentos(entity.id)
                    Log.w("PredictionMlSync", "Fallo sincronizando predicción ${entity.id}: ${e.message}")
                }
            }

            // Limpiar predicciones sincronizadas hace más de 7 días
            val sieteDisasAtras = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            pendingPredictionMlDao.limpiarAntiguos(sieteDisasAtras)

            Log.i("PredictionMlSync", "Lote sincronizado: $sincronizados exitosas, $fallos fallos")
            Resource.Success(sincronizados)
        } catch (e: Exception) {
            Log.e("PredictionMlSync", "Error sincronizando lote: ${e.message}")
            Resource.Error("Error sincronizando predicciones: ${e.message}")
        }
    }

    /**
     * Obtener historial local de predicciones
     */
    fun getHistorialLocal(limit: Int = 100): Flow<List<PendingPredictionMlEntity>> {
        return pendingPredictionMlDao.getRecientes(limit)
    }

    /**
     * Eliminar predicción pendiente
     */
    suspend fun eliminarPendiente(id: Long) {
        pendingPredictionMlDao.delete(id)
    }
}
