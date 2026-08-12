package com.bioguard.movil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPredictionMlDao {

    @Insert
    suspend fun insert(entity: PendingPredictionMlEntity): Long

    @Update
    suspend fun update(entity: PendingPredictionMlEntity)

    @Query("SELECT * FROM pending_predictions_ml WHERE sincronizado = 0 ORDER BY createdAt ASC")
    fun getPendentes(): Flow<List<PendingPredictionMlEntity>>

    @Query("SELECT * FROM pending_predictions_ml WHERE sincronizado = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendentesSync(limit: Int = 50): List<PendingPredictionMlEntity>

    @Query("SELECT * FROM pending_predictions_ml ORDER BY createdAt DESC LIMIT :limit")
    fun getRecientes(limit: Int = 100): Flow<List<PendingPredictionMlEntity>>

    @Query("DELETE FROM pending_predictions_ml WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_predictions_ml WHERE sincronizado = 1 AND ultimaSincronizacion < :beforeMillis")
    suspend fun limpiarAntiguos(beforeMillis: Long)

    @Query("SELECT COUNT(*) FROM pending_predictions_ml WHERE sincronizado = 0")
    fun contarPendentes(): Flow<Int>

    @Query("UPDATE pending_predictions_ml SET sincronizado = 1, ultimaSincronizacion = :timestamp, intentosSincronizacion = intentosSincronizacion + 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pending_predictions_ml SET intentosSincronizacion = intentosSincronizacion + 1 WHERE id = :id")
    suspend fun incrementarIntentos(id: Long)

    @Query("DELETE FROM pending_predictions_ml WHERE id IN (:ids)")
    suspend fun deleteMultiple(ids: List<Long>)
}
