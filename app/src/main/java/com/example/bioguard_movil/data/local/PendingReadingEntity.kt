package com.example.bioguard_movil.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pending_readings", indices = [Index(value = ["timestamp"])])
data class PendingReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pulsoBpm: Double,
    val temperaturaC: Double,
    val sudoracionGsr: Double,
    val hrv: Double?,
    val spo2: Double?,
    val timestamp: String
)
