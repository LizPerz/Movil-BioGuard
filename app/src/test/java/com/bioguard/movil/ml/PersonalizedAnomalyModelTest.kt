package com.bioguard.movil.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizedAnomalyModelTest {
    private val model = PersonalizedAnomalyModel()

    @Test
    fun `normal sample without enough baseline uses safety rules only`() {
        val assessment = model.assess(
            current = VitalSample(heartRateBpm = 72.0, temperatureC = 36.6, spo2Percent = 98.0),
            baseline = List(10) { VitalSample(heartRateBpm = 70.0) }
        )

        assertEquals(LocalRiskLevel.NORMAL, assessment.level)
        assertFalse(assessment.personalizedModelReady)
        assertNull(assessment.anomalyProbability)
        assertEquals(0.0, assessment.score, 0.001)
    }

    @Test
    fun `critical oxygen rule works without internet or trained baseline`() {
        val assessment = model.assess(
            current = VitalSample(heartRateBpm = 72.0, spo2Percent = 86.0),
            baseline = emptyList()
        )

        assertEquals(LocalRiskLevel.CRITICAL, assessment.level)
        assertTrue(assessment.score >= 90.0)
        assertTrue(assessment.reasons.any { it.contains("Oxigenación") })
    }

    @Test
    fun `personalized model detects a strong deviation inside broad safety range`() {
        val baseline = List(30) { index ->
            VitalSample(heartRateBpm = 69.0 + (index % 3))
        }

        val assessment = model.assess(
            current = VitalSample(heartRateBpm = 96.0),
            baseline = baseline
        )

        assertTrue(assessment.personalizedModelReady)
        assertNotNull(assessment.anomalyProbability)
        assertTrue(assessment.anomalyProbability!! >= 0.70)
        assertTrue(assessment.score >= 70.0)
        assertTrue(assessment.reasons.contains("Patrón diferente a la línea base personal"))
    }

    @Test
    fun `personalized analysis can be disabled while hard safety rules remain`() {
        val baseline = List(30) { VitalSample(heartRateBpm = 70.0) }
        val assessment = model.assess(
            current = VitalSample(heartRateBpm = 145.0),
            baseline = baseline,
            personalizedAnalysisEnabled = false
        )

        assertFalse(assessment.personalizedModelReady)
        assertNull(assessment.anomalyProbability)
        assertEquals(LocalRiskLevel.CRITICAL, assessment.level)
    }

    @Test
    fun `unavailable zero sensors are ignored instead of treated as clinical zeros`() {
        val assessment = model.assess(
            current = VitalSample(
                heartRateBpm = 72.0,
                temperatureC = 0.0,
                gsr = 0.0,
                hrvMs = 0.0,
                spo2Percent = 0.0
            ),
            baseline = emptyList()
        )

        assertEquals(LocalRiskLevel.NORMAL, assessment.level)
        assertTrue(assessment.reasons.isEmpty())
    }
}
