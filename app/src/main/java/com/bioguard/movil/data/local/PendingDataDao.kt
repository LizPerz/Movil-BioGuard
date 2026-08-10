package com.bioguard.movil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingDataDao {

    @Query("SELECT COUNT(*) FROM pending_readings")
    suspend fun countPendingReadings(): Int

    @Query("SELECT COUNT(*) FROM pending_gps")
    suspend fun countPendingGps(): Int

    @Query("SELECT COUNT(*) FROM pending_events")
    suspend fun countPendingEvents(): Int

    @Query("SELECT COUNT(*) FROM pending_alerts")
    suspend fun countPendingAlerts(): Int

    // Sensor Readings
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReading(reading: PendingReadingEntity): Long

    @Query("SELECT * FROM pending_readings ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingReadings(limit: Int): List<PendingReadingEntity>

    @Query("DELETE FROM pending_readings WHERE id IN (:ids)")
    suspend fun deleteReadings(ids: List<Long>): Int

    // GPS Tracking
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGps(gps: PendingGpsEntity): Long

    @Query("SELECT * FROM pending_gps ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingGps(limit: Int): List<PendingGpsEntity>

    @Query("DELETE FROM pending_gps WHERE id IN (:ids)")
    suspend fun deleteGps(ids: List<Long>): Int

    // Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PendingEventEntity): Long

    @Query("SELECT * FROM pending_events ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int): List<PendingEventEntity>

    @Query("DELETE FROM pending_events WHERE id IN (:ids)")
    suspend fun deleteEvents(ids: List<Long>): Int

    // Alerts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PendingAlertEntity): Long

    @Query("SELECT * FROM pending_alerts ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingAlerts(limit: Int): List<PendingAlertEntity>

    @Query("DELETE FROM pending_alerts WHERE id IN (:ids)")
    suspend fun deleteAlerts(ids: List<Long>): Int

    // Full purge (logout) — elimina toda la cola offline del usuario anterior
    @Query("DELETE FROM pending_readings")
    suspend fun clearReadings(): Int

    @Query("DELETE FROM pending_gps")
    suspend fun clearGps(): Int

    @Query("DELETE FROM pending_events")
    suspend fun clearEvents(): Int

    @Query("DELETE FROM pending_alerts")
    suspend fun clearAlerts(): Int
}
