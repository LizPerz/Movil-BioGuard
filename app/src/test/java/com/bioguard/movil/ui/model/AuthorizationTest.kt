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
    fun `caregiver fallback includes health access`() {
        val access = EffectiveAccess.restricted(UserRole.CUIDADOR, "patient-1")

        assertTrue(access.allows(AppPermission.ALERT_READ))
        assertTrue(access.allows(AppPermission.HEALTH_SUMMARY))
        assertTrue(access.allows(AppPermission.HEALTH_HISTORY))
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

    @Test
    fun `caregiver can always view realtime location`() {
        val access = EffectiveAccess(
            role = UserRole.CUIDADOR,
            patientId = "patient-1",
            planGpsActivo = false
        )

        assertTrue(access.permiteVerUbicacion())
    }

    @Test
    fun `owner can view realtime location only with gps in plan`() {
        val sinGps = EffectiveAccess(role = UserRole.DUEÑO, patientId = "patient-1", planGpsActivo = false)
        val conGps = EffectiveAccess(role = UserRole.DUEÑO, patientId = "patient-1", planGpsActivo = true)

        assertFalse(sinGps.permiteVerUbicacion())
        assertTrue(conGps.permiteVerUbicacion())
    }

    @Test
    fun `patient cannot view realtime location`() {
        val access = EffectiveAccess(role = UserRole.PACIENTE, patientId = "patient-1", planGpsActivo = true)

        assertFalse(access.permiteVerUbicacion())
    }

    @Test
    fun `unknown role cannot view realtime location`() {
        val access = EffectiveAccess(role = UserRole.UNKNOWN)

        assertFalse(access.permiteVerUbicacion())
    }
}
