package com.bioguard.movil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para almacenar localmente predicciones ML pendientes de envío
 * Permitir guardar reportes cuando no hay conectividad
 */
@Entity(tableName = "pending_predictions_ml")
data class PendingPredictionMlEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pacienteId: String,
    val probabilidadPico: Double,
    val nivelRiesgo: String,
    val casoClinico: String? = null,
    val accionAutomatizada: String? = null,
    val imc: Double? = null,
    val z: Double? = null,
    val pPico: Double? = null,
    val recomendacion: String? = null,
    val horasEstimadas: Int? = null,
    val modeloVersion: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val intentosSincronizacion: Int = 0,
    val ultimaSincronizacion: Long? = null,
    val sincronizado: Boolean = false
)
