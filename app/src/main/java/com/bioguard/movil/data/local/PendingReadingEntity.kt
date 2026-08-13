package com.bioguard.movil.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_readings",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["sourceMessageId"], unique = true)
    ]
)
data class PendingReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pulsoBpm: Double,
    val temperaturaC: Double,
    val sudoracionGsr: Double,
    val hrv: Double?,
    val spo2: Double?,
    val pasos: Int? = null,
    val probabilidadPico: Double? = null,
    val timestamp: String,
    val sourceMessageId: String? = null
)
