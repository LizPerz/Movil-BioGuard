package com.example.bioguard_movil.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pending_eventos", indices = [Index(value = ["timestamp"])])
data class PendingEventoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pacienteId: String = "",
    val dispositivoMac: String = "",
    val nivelRiesgo: String = "",
    val probabilidadMl: Double = 0.0,
    val descripcion: String = "",
    val timestamp: String = ""
)