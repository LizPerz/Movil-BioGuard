package com.example.bioguard_movil.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pending_alerts", indices = [Index(value = ["timestamp"])])
data class PendingAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pacienteId: String,
    val tipoAlerta: String,
    val descripcion: String,
    val latitud: Double?,
    val longitud: Double?,
    val timestamp: String
)
