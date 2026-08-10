package com.bioguard.movil.service

import com.bioguard.movil.ml.LocalRiskLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNotificationPolicyTest {
    @Test
    fun `normal and observe levels never notify`() {
        assertFalse(LocalNotificationPolicy.shouldNotify(LocalRiskLevel.NORMAL, null, 1_000L, 0L))
        assertFalse(LocalNotificationPolicy.shouldNotify(LocalRiskLevel.OBSERVE, null, 1_000L, 0L))
    }

    @Test
    fun `first high alert is accepted`() {
        assertTrue(LocalNotificationPolicy.shouldNotify(LocalRiskLevel.HIGH, null, 1_000L, 0L))
    }

    @Test
    fun `same high alert is suppressed during cooldown`() {
        assertFalse(
            LocalNotificationPolicy.shouldNotify(
                LocalRiskLevel.HIGH,
                LocalRiskLevel.HIGH,
                LocalNotificationPolicy.HIGH_COOLDOWN_MILLIS - 1,
                0L
            )
        )
    }

    @Test
    fun `critical escalation bypasses high cooldown`() {
        assertTrue(
            LocalNotificationPolicy.shouldNotify(
                LocalRiskLevel.CRITICAL,
                LocalRiskLevel.HIGH,
                2_000L,
                1_500L
            )
        )
    }

    @Test
    fun `critical repeats only after critical cooldown`() {
        assertFalse(
            LocalNotificationPolicy.shouldNotify(
                LocalRiskLevel.CRITICAL,
                LocalRiskLevel.CRITICAL,
                LocalNotificationPolicy.CRITICAL_COOLDOWN_MILLIS - 1,
                0L
            )
        )
        assertTrue(
            LocalNotificationPolicy.shouldNotify(
                LocalRiskLevel.CRITICAL,
                LocalRiskLevel.CRITICAL,
                LocalNotificationPolicy.CRITICAL_COOLDOWN_MILLIS,
                0L
            )
        )
    }
}
