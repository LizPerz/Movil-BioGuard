package com.bioguard.movil.ml

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

data class VitalSample(
    val heartRateBpm: Double,
    val temperatureC: Double? = null,
    val gsr: Double? = null,
    val hrvMs: Double? = null,
    val spo2Percent: Double? = null
)

enum class LocalRiskLevel(val rank: Int) {
    NORMAL(0),
    OBSERVE(1),
    HIGH(2),
    CRITICAL(3)
}

data class LocalRiskAssessment(
    val score: Double,
    val safetyRuleScore: Double,
    val anomalyProbability: Double?,
    val level: LocalRiskLevel,
    val reasons: List<String>,
    val personalizedModelReady: Boolean,
    val modelVersion: String = PersonalizedAnomalyModel.MODEL_VERSION
)

/**
 * Personalized one-class anomaly detector. It learns only from the patient's
 * recent valid baseline and never reports a medical diagnosis.
 */
class PersonalizedAnomalyModel {

    fun assess(
        current: VitalSample,
        baseline: List<VitalSample>,
        personalizedAnalysisEnabled: Boolean = true
    ): LocalRiskAssessment {
        val safety = safetyRules(current)
        val validBaseline = baseline.filter { it.heartRateBpm in HEART_RATE_RANGE }
        val modelReady = personalizedAnalysisEnabled && validBaseline.size >= MIN_BASELINE_SAMPLES
        val anomaly = if (modelReady) anomalyProbability(current, validBaseline) else null
        val anomalyScore = (anomaly ?: 0.0) * 100.0
        val finalScore = max(safety.score, anomalyScore).coerceIn(0.0, 100.0)

        val reasons = buildList {
            addAll(safety.reasons)
            if (anomaly != null && anomaly >= 0.70) {
                add("Patrón diferente a la línea base personal")
            }
        }.distinct().take(MAX_REASONS)

        return LocalRiskAssessment(
            score = finalScore,
            safetyRuleScore = safety.score,
            anomalyProbability = anomaly,
            level = when {
                finalScore >= 90.0 -> LocalRiskLevel.CRITICAL
                finalScore >= 70.0 -> LocalRiskLevel.HIGH
                finalScore >= 45.0 -> LocalRiskLevel.OBSERVE
                else -> LocalRiskLevel.NORMAL
            },
            reasons = reasons,
            personalizedModelReady = modelReady
        )
    }

    private fun anomalyProbability(current: VitalSample, baseline: List<VitalSample>): Double {
        val deviations = buildList {
            standardizedDeviation(
                current.heartRateBpm,
                baseline.map { it.heartRateBpm },
                minimumScale = 5.0
            )?.let { add(it to 1.0) }
            standardizedDeviation(
                current.temperatureC.validIn(TEMPERATURE_RANGE),
                baseline.mapNotNull { it.temperatureC.validIn(TEMPERATURE_RANGE) },
                minimumScale = 0.20
            )?.let { add(it to 0.8) }
            standardizedDeviation(
                current.gsr.validIn(GSR_RANGE),
                baseline.mapNotNull { it.gsr.validIn(GSR_RANGE) },
                minimumScale = 0.50
            )?.let { add(it to 0.5) }
            standardizedDeviation(
                current.hrvMs.validIn(HRV_RANGE),
                baseline.mapNotNull { it.hrvMs.validIn(HRV_RANGE) },
                minimumScale = 5.0
            )?.let { add(it to 0.8) }
            standardizedDeviation(
                current.spo2Percent.validIn(SPO2_RANGE),
                baseline.mapNotNull { it.spo2Percent.validIn(SPO2_RANGE) },
                minimumScale = 1.0
            )?.let { add(it to 1.2) }
        }

        if (deviations.isEmpty()) return 0.0
        val weightedExcess = deviations.sumOf { (z, weight) -> max(0.0, z - NORMAL_Z_LIMIT) * weight }
        val totalWeight = deviations.sumOf { it.second }
        return (1.0 - exp(-weightedExcess / totalWeight)).coerceIn(0.0, 1.0)
    }

    private fun standardizedDeviation(
        value: Double?,
        values: List<Double>,
        minimumScale: Double
    ): Double? {
        if (value == null || values.size < MIN_FEATURE_SAMPLES) return null
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        val scale = max(sqrt(variance), minimumScale)
        return abs(value - mean) / scale
    }

    private data class SafetyAssessment(val score: Double, val reasons: List<String>)

    private fun safetyRules(sample: VitalSample): SafetyAssessment {
        var score = 0.0
        val reasons = mutableListOf<String>()

        if (sample.heartRateBpm in HEART_RATE_RANGE) {
            when {
                sample.heartRateBpm >= 140.0 || sample.heartRateBpm <= 35.0 -> {
                    score = max(score, 95.0)
                    reasons += "Frecuencia cardiaca muy fuera del rango configurado"
                }
                sample.heartRateBpm > 110.0 || sample.heartRateBpm < 45.0 -> {
                    score = max(score, 75.0)
                    reasons += "Frecuencia cardiaca fuera del rango configurado"
                }
            }
        }

        sample.spo2Percent.validIn(SPO2_RANGE)?.let { spo2 ->
            when {
                spo2 < 88.0 -> {
                    score = max(score, 95.0)
                    reasons += "Oxigenación muy por debajo del rango configurado"
                }
                spo2 < 92.0 -> {
                    score = max(score, 82.0)
                    reasons += "Oxigenación por debajo del rango configurado"
                }
                spo2 < 95.0 -> score = max(score, 45.0)
            }
        }

        sample.temperatureC.validIn(TEMPERATURE_RANGE)?.let { temperature ->
            when {
                temperature >= 39.5 || temperature <= 34.0 -> {
                    score = max(score, 92.0)
                    reasons += "Temperatura muy fuera del rango configurado"
                }
                temperature > 38.0 || temperature < 35.0 -> {
                    score = max(score, 72.0)
                    reasons += "Temperatura fuera del rango configurado"
                }
            }
        }

        sample.hrvMs.validIn(HRV_RANGE)?.let { hrv ->
            if (hrv < 20.0) {
                score = max(score, 55.0)
                reasons += "Variabilidad cardiaca reducida"
            }
        }

        sample.gsr.validIn(GSR_RANGE)?.let { gsr ->
            if (gsr > 12.0) {
                score = max(score, 50.0)
                reasons += "Respuesta electrodérmica elevada"
            }
        }

        return SafetyAssessment(score.coerceIn(0.0, 100.0), reasons)
    }

    private fun Double?.validIn(range: ClosedFloatingPointRange<Double>): Double? =
        this?.takeIf { it.isFinite() && it in range }

    companion object {
        const val MODEL_VERSION = "personalized-one-class-v1"
        const val MIN_BASELINE_SAMPLES = 20
        private const val MIN_FEATURE_SAMPLES = 10
        private const val NORMAL_Z_LIMIT = 2.0
        private const val MAX_REASONS = 3
        private val HEART_RATE_RANGE = 25.0..240.0
        private val TEMPERATURE_RANGE = 25.0..45.0
        private val GSR_RANGE = 0.0..100.0
        private val HRV_RANGE = 1.0..300.0
        private val SPO2_RANGE = 50.0..100.0
    }
}
