package com.bioguard.movil.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizationTest {
    @Test
    fun `roles are matched exactly`() {
        assertEquals(UserRole.DUEÑO, UserRole.from("dueno"))
        assertEquals(UserRole.PACIENTE, UserRole.from(" PACIENTE "))
        assertEquals(UserRole.ADMINISTRADOR, UserRole.from("administrador"))
        assertEquals(UserRole.UNKNOWN, UserRole.from("superdueno"))
        assertEquals(UserRole.UNKNOWN, UserRole.from("administrador_global"))
    }

    @Test
    fun `unknown role fails closed`() {
        val access = EffectiveAccess.restricted(UserRole.UNKNOWN)

        assertTrue(access.permissions.isEmpty())
        assertFalse(access.allows(AppPermission.CAREGIVER_MANAGE))
        assertFalse(access.allows(AppPermission.BILLING_MANAGE))
    }

    @Test
    fun `caregiver fallback is alert only`() {
        val access = EffectiveAccess.restricted(UserRole.CUIDADOR, "patient-1")

        assertTrue(access.allows(AppPermission.ALERT_READ))
        assertFalse(access.allows(AppPermission.HEALTH_SUMMARY))
        assertFalse(access.allows(AppPermission.HEALTH_HISTORY))
        assertFalse(access.allows(AppPermission.MEDICATION_READ))
    }

    @Test
    fun `package permissions are never inferred by fallback`() {
        val owner = EffectiveAccess.restricted(UserRole.DUEÑO)

        assertFalse(owner.allows(AppPermission.REPORT_EXPORT))
        assertFalse(owner.allows(AppPermission.NIGHT_GUARDIAN))
        assertFalse(owner.allows(AppPermission.GPS_CONTINUOUS))
        assertFalse(owner.allows(AppPermission.AI_CONSOLE))
    }
}
