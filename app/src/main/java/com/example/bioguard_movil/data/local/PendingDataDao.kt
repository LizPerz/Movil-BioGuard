package com.example.bioguard_movil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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

    // Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PendingEventEntity)

    @Query("SELECT * FROM pending_events ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int): List<PendingEventEntity>

    @Query("DELETE FROM pending_events WHERE id IN (:ids)")
    suspend fun deleteEvents(ids: List<Long>)

    // Alerts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PendingAlertEntity)

    @Query("SELECT * FROM pending_alerts ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingAlerts(limit: Int): List<PendingAlertEntity>

    @Query("DELETE FROM pending_alerts WHERE id IN (:ids)")
    suspend fun deleteAlerts(ids: List<Long>)
}
