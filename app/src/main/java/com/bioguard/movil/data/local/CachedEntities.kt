package com.bioguard.movil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_readings")
data class CachedReadingEntity(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val pulsoBpm: Double,
    val temperaturaC: Double,
    val sudoracionGsr: Double,
    val hrv: Double,
    val spo2: Double,
    val pasos: Int,
    val calorias: Double,
    val fechaHora: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_events")
data class CachedEventEntity(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val dispositivoMac: String,
    val nivelRiesgo: String,
    val probabilidadMl: Double,
    val descripcion: String,
    val fechaHora: String,
    val atendido: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_alerts")
data class CachedAlertEntity(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val tipoAlerta: String,
    val descripcion: String,
    val latitud: Double?,
    val longitud: Double?,
    val atendida: Boolean = false,
    val fechaHora: String,
    val timestamp: Long = System.currentTimeMillis()
)
