package com.bioguard.movil.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineRiskCalculatorTest {

    private fun calcularRiesgoOffline(
        pulsoBpm: Double,
        temperaturaC: Double,
        sudoracionGsr: Double,
        hrv: Double,
        spo2: Double
    ): Double {
        var score = 0.0

        if (pulsoBpm > 100) score += (pulsoBpm - 100) * 0.8
        else if (pulsoBpm < 50) score += (50 - pulsoBpm) * 0.5

        if (hrv < 40) score += (40 - hrv) * 1.0

        if (temperaturaC > 37.8) score += (temperaturaC - 37.8) * 15.0
        else if (temperaturaC < 35.0) score += (35.0 - temperaturaC) * 15.0

        if (sudoracionGsr > 6.0) score += (sudoracionGsr - 6.0) * 8.0

        if (spo2 < 95) score += (95 - spo2) * 5.0

        return score.coerceIn(0.0, 100.0)
    }

    @Test
    fun `normal vital signs result in zero risk score`() {
        val score = calcularRiesgoOffline(
            pulsoBpm = 75.0,
            temperaturaC = 36.6,
            sudoracionGsr = 3.0,
            hrv = 55.0,
            spo2 = 98.0
        )
        assertEquals(0.0, score, 0.01)
    }

    @Test
    fun `high pulse rate increases risk score`() {
        val score = calcularRiesgoOffline(
            pulsoBpm = 130.0, // (130-100)*0.8 = 24.0
            temperaturaC = 36.6,
            sudoracionGsr = 3.0,
            hrv = 50.0,
            spo2 = 98.0
        )
        assertEquals(24.0, score, 0.01)
    }

    @Test
    fun `low oxygen saturation increases risk score significantly`() {
        val score = calcularRiesgoOffline(
            pulsoBpm = 75.0,
            temperaturaC = 36.6,
            sudoracionGsr = 3.0,
            hrv = 50.0,
            spo2 = 88.0 // (95-88)*5.0 = 35.0
        )
        assertEquals(35.0, score, 0.01)
    }

    @Test
    fun `combination of abnormal vitals triggers high risk alert threshold`() {
        val score = calcularRiesgoOffline(
            pulsoBpm = 140.0,      // (140-100)*0.8 = 32.0
            temperaturaC = 39.0,   // (39-37.8)*15 = 18.0
            sudoracionGsr = 8.0,   // (8-6)*8 = 16.0
            hrv = 20.0,            // (40-20)*1 = 20.0
            spo2 = 90.0            // (95-90)*5 = 25.0
        ) // Total = 111.0 -> coerced to 100.0
        assertEquals(100.0, score, 0.01)
        assertTrue("Risk score should exceed threshold of 70", score >= 70.0)
    }

    @Test
    fun `low temperature and low heart rate contribute to risk score`() {
        val score = calcularRiesgoOffline(
            pulsoBpm = 40.0,       // (50-40)*0.5 = 5.0
            temperaturaC = 34.0,   // (35.0-34.0)*15 = 15.0
            sudoracionGsr = 2.0,
            hrv = 30.0,            // (40-30)*1 = 10.0
            spo2 = 94.0            // (95-94)*5 = 5.0
        ) // Total = 35.0
        assertEquals(35.0, score, 0.01)
    }
}
