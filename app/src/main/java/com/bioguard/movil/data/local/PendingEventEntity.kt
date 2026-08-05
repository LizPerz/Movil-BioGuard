package com.bioguard.movil.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pending_events", indices = [Index(value = ["timestamp"])])
data class PendingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pacienteId: String,
    val dispositivoMac: String,
    val nivelRiesgo: String,
    val probabilidadMl: Double,
    val descripcion: String,
    val timestamp: String
)
