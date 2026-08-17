package com.bioguard.movil.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserPreferencesTest {

    private lateinit var prefs: UserPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = UserPreferences(context)
        runBlocking { prefs.clearAll() }
    }

    @Test
    fun `planGpsActivo por defecto es false`() = runBlocking {
        assertFalse(prefs.planGpsActivo.first())
    }

    @Test
    fun `saveEffectiveAccess persiste paciente nivel plan permisos y gps`() = runBlocking {
        prefs.saveEffectiveAccess(
            patientId = "p-1",
            caregiverAccessLevel = "lectura",
            planName = "Guardian Pro",
            permissionCodes = setOf("patient.read", "account.profile"),
            planGpsActivo = true
        )

        assertEquals("p-1", prefs.patientId.first())
        assertEquals("lectura", prefs.caregiverAccessLevel.first())
        assertEquals("Guardian Pro", prefs.planName.first())
        assertEquals(setOf("patient.read", "account.profile"), prefs.accessPermissions.first())
        assertTrue(prefs.planGpsActivo.first())
    }

    @Test
    fun `saveEffectiveAccess sobreescribe planGpsActivo a false`() = runBlocking {
        prefs.saveEffectiveAccess(patientId = "p-1", caregiverAccessLevel = null, planName = null, permissionCodes = emptySet(), planGpsActivo = true)
        prefs.saveEffectiveAccess(patientId = "p-1", caregiverAccessLevel = null, planName = null, permissionCodes = emptySet(), planGpsActivo = false)

        assertFalse(prefs.planGpsActivo.first())
    }

    @Test
    fun `saveEffectiveAccess con valores en blanco elimina las claves`() = runBlocking {
        prefs.saveEffectiveAccess(patientId = "p-1", caregiverAccessLevel = "lectura", planName = "Pro", permissionCodes = setOf("a"))
        prefs.saveEffectiveAccess(patientId = null, caregiverAccessLevel = "", planName = "  ", permissionCodes = emptySet())

        assertNull(prefs.patientId.first())
        assertNull(prefs.caregiverAccessLevel.first())
        assertNull(prefs.planName.first())
        assertTrue(prefs.accessPermissions.first().isEmpty())
    }

    @Test
    fun `accessPermissions ordena y filtra vacios`() = runBlocking {
        prefs.saveEffectiveAccess(patientId = null, caregiverAccessLevel = null, planName = null, permissionCodes = setOf("z", "", "a"))

        assertEquals(setOf("a", "z"), prefs.accessPermissions.first())
    }

    @Test
    fun `clearSession borra datos de acceso pero conserva biometria`() = runBlocking {
        prefs.saveEffectiveAccess(patientId = "p-1", caregiverAccessLevel = "lectura", planName = "Pro", permissionCodes = setOf("a"), planGpsActivo = true)
        prefs.savePatientBiometrics(
            birthDate = "1995-07-03", sex = "F", weight = "60", height = "165",
            isDiabetic = true, familyDiabetes = false, activityLevel = "Baja"
        )

        prefs.clearSession()

        assertNull(prefs.patientId.first())
        assertNull(prefs.planName.first())
        assertFalse(prefs.planGpsActivo.first())
        assertTrue(prefs.accessPermissions.first().isEmpty())
        assertEquals("1995-07-03", prefs.patientBirthDate.first())
        assertEquals("F", prefs.patientSex.first())
    }

    @Test
    fun `clearAll elimina todo incluyendo biometria`() = runBlocking {
        prefs.saveEffectiveAccess(patientId = "p-1", caregiverAccessLevel = null, planName = null, permissionCodes = setOf("a"), planGpsActivo = true)
        prefs.savePatientBiometrics(
            birthDate = "1995-07-03", sex = "F", weight = "60", height = "165",
            isDiabetic = true, familyDiabetes = false, activityLevel = "Baja"
        )

        prefs.clearAll()

        assertNull(prefs.patientId.first())
        assertNull(prefs.patientBirthDate.first())
        assertFalse(prefs.planGpsActivo.first())
    }

    @Test
    fun `saveUserData persiste identidad`() = runBlocking {
        prefs.saveUserData(userId = "u-1", userName = "Leslie", userRole = "dueno")

        assertEquals("u-1", prefs.userId.first())
        assertEquals("Leslie", prefs.userName.first())
        assertEquals("dueno", prefs.userRole.first())
    }

    @Test
    fun `isDarkMode por defecto true y saveTheme lo cambia`() = runBlocking {
        assertTrue(prefs.isDarkMode.first())

        prefs.saveTheme(theme = "light", isDarkMode = false)

        assertEquals("light", prefs.theme.first())
        assertFalse(prefs.isDarkMode.first())
    }

    @Test
    fun `clearSession conserva el tema`() = runBlocking {
        prefs.saveTheme(theme = "light", isDarkMode = false)
        prefs.saveEffectiveAccess(patientId = "p-1", caregiverAccessLevel = null, planName = null, permissionCodes = emptySet())

        prefs.clearSession()

        assertEquals("light", prefs.theme.first())
        assertFalse(prefs.isDarkMode.first())
    }

    @Test
    fun `device y sync defaults se leen sin excepcion`() = runBlocking {
        assertTrue(prefs.isSyncEnabled.first())
        assertEquals(5, prefs.syncIntervalMinutes.first())
        assertFalse(prefs.isDeviceConnected.first())
        assertNull(prefs.deviceId.first())
        assertTrue(prefs.isLocalAnalysisEnabled.first())
        assertFalse(prefs.hasSeenOnboarding.first())
    }
}
