package com.example.bioguard_movil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PendingDataDao {

    // Sensor Readings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: PendingReadingEntity)

    @Query("SELECT * FROM pending_readings ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingReadings(limit: Int): List<PendingReadingEntity>

    @Query("DELETE FROM pending_readings WHERE id IN (:ids)")
    suspend fun deleteReadings(ids: List<Long>)

    // GPS Tracking
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGps(gps: PendingGpsEntity)

    @Query("SELECT * FROM pending_gps ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingGps(limit: Int): List<PendingGpsEntity>

    @Query("DELETE FROM pending_gps WHERE id IN (:ids)")
    suspend fun deleteGps(ids: List<Long>)

    @Transaction
    suspend fun insertReadingAndGps(reading: PendingReadingEntity, gps: PendingGpsEntity) {
        insertReading(reading)
        insertGps(gps)
    }

    // Eventos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvento(evento: PendingEventoEntity)

    @Query("SELECT * FROM pending_eventos ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingEventos(limit: Int): List<PendingEventoEntity>

    @Query("DELETE FROM pending_eventos WHERE id IN (:ids)")
    suspend fun deleteEventos(ids: List<Long>)

    // Alertas
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerta(alerta: PendingAlertaEntity)

    @Query("SELECT * FROM pending_alertas ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingAlertas(limit: Int): List<PendingAlertaEntity>

    @Query("DELETE FROM pending_alertas WHERE id IN (:ids)")
    suspend fun deleteAlertas(ids: List<Long>)

    // Heartbeats
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartbeat(heartbeat: PendingHeartbeatEntity)

    @Query("SELECT * FROM pending_heartbeats ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingHeartbeats(limit: Int): List<PendingHeartbeatEntity>

    @Query("DELETE FROM pending_heartbeats WHERE id IN (:ids)")
    suspend fun deleteHeartbeats(ids: List<Long>)
}
