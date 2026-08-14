package com.bioguard.movil.domain.model

data class Patient(
    val id: String,
    val nombre: String,
    val apellidoPaterno: String?,
    val apellidoMaterno: String?,
    val correo: String?,
    val fechaNacimiento: String?,
    val sexo: String?,
    val pesoKg: Double?,
    val estaturaCm: Double?,
    val esDiabetico: Boolean,
    val familiarDiabetico: Boolean
)

data class SensorReading(
    val id: String,
    val pacienteId: String,
    val pulsoBpm: Double,
    val temperaturaC: Double,
    val estresPct: Double,
    val hrv: Double,
    val spo2: Double,
    val timestamp: Long
)

data class Alert(
    val id: String,
    val pacienteId: String,
    val tipoAlerta: String,
    val descripcion: String,
    val nivelRiesgo: String,
    val timestamp: Long,
    val resuelta: Boolean
)

data class Medication(
    val id: String,
    val nombre: String,
    val dosis: String,
    val horario: String,
    val activa: Boolean,
    val ultimaToma: String?
)
