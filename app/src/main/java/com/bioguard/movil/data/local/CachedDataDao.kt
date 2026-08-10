package com.bioguard.movil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedDataDao {

    // Cached Readings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<CachedReadingEntity>): List<Long>

    @Query("SELECT * FROM cached_readings WHERE pacienteId = :pacienteId ORDER BY timestamp DESC LIMIT :limit")
    fun getCachedReadings(pacienteId: String, limit: Int = 50): Flow<List<CachedReadingEntity>>

    @Query("SELECT * FROM cached_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getAllCachedReadings(limit: Int = 50): Flow<List<CachedReadingEntity>>

    @Query("SELECT * FROM cached_readings WHERE pacienteId = :pacienteId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReadingsSnapshot(pacienteId: String, limit: Int = 120): List<CachedReadingEntity>

    @Query("DELETE FROM cached_readings WHERE pacienteId = :pacienteId")
    suspend fun clearReadings(pacienteId: String): Int

    @Query("DELETE FROM cached_readings")
    suspend fun clearAllReadings(): Int

    // Cached Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CachedEventEntity>): List<Long>

    @Query("SELECT * FROM cached_events WHERE pacienteId = :pacienteId ORDER BY timestamp DESC")
    fun getCachedEvents(pacienteId: String): Flow<List<CachedEventEntity>>

    @Query("DELETE FROM cached_events WHERE pacienteId = :pacienteId")
    suspend fun clearEvents(pacienteId: String): Int

    @Query("DELETE FROM cached_events")
    suspend fun clearAllEvents(): Int

    // Cached Alerts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<CachedAlertEntity>): List<Long>

    @Query("SELECT * FROM cached_alerts WHERE pacienteId = :pacienteId ORDER BY timestamp DESC")
    fun getCachedAlerts(pacienteId: String): Flow<List<CachedAlertEntity>>

    @Query("DELETE FROM cached_alerts WHERE pacienteId = :pacienteId")
    suspend fun clearAlerts(pacienteId: String): Int

    @Query("DELETE FROM cached_alerts")
    suspend fun clearAllAlerts(): Int
}
