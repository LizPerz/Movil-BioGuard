package com.bioguard.movil.service

import com.bioguard.movil.network.HeartbeatRequest
import com.bioguard.movil.network.LecturaSensorRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearablePayloadValidatorTest {
    @Test
    fun `accepts supported durable telemetry`() {
        val reading = LecturaSensorRequest(72.0, 0.0, 0.0, 41.2, null, 123, "2026-08-09T18:20:30Z")
        assertTrue(WearablePayloadValidator.isValid(reading))
        assertTrue(WearablePayloadValidator.isAllowedPath("/bioguard/telemetry/42"))
    }

    @Test
    fun `rejects non finite and physiologically impossible readings`() {
        assertFalse(WearablePayloadValidator.isValid(LecturaSensorRequest(Double.NaN, 36.5, 1.0, null, null, null, "2026-08-09T18:20:30Z")))
        assertFalse(WearablePayloadValidator.isValid(LecturaSensorRequest(72.0, 36.5, 1.0, null, 140.0, null, "2026-08-09T18:20:30Z")))
    }

    @Test
    fun `rejects unknown or prefix-confused paths`() {
        assertFalse(WearablePayloadValidator.isAllowedPath("/bioguard/telemetryevil/42"))
        assertFalse(WearablePayloadValidator.isAllowedPath("/commands/alert"))
        assertTrue(WearablePayloadValidator.isAllowedPath("/bioguard/pair/ack"))
    }

    @Test
    fun `bounds heartbeat metadata`() {
        assertTrue(WearablePayloadValidator.isValid(HeartbeatRequest(bateria = 80, sensoresActivos = listOf("pulso", "hrv"))))
        assertFalse(WearablePayloadValidator.isValid(HeartbeatRequest(bateria = 101)))
        assertFalse(WearablePayloadValidator.isValid(HeartbeatRequest(sensoresActivos = listOf("x".repeat(65)))))
    }
}
