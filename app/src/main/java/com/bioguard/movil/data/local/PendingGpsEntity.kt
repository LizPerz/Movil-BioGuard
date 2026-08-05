package com.bioguard.movil.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pending_gps", indices = [Index(value = ["timestamp"])])
data class PendingGpsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitud: Double,
    val longitud: Double,
    val esEmergencia: Boolean,
    val timestamp: String
)
