package com.bioguard.movil.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiContractTest {

    private val gson = Gson()

    // ============ TRACKING / GPS ============

    @Test
    fun `TrackingResponse parsea todos los campos incluyendo esEmergencia`() {
        val json = """
            {"longitud": -70.66, "latitud": -33.45, "timestamp": "2026-08-09T18:20:30Z", "esEmergencia": true}
        """.trimIndent()

        val tracking = gson.fromJson(json, TrackingResponse::class.java)

        assertEquals(-70.66, tracking.longitud, 0.0001)
        assertEquals(-33.45, tracking.latitud, 0.0001)
        assertEquals("2026-08-09T18:20:30Z", tracking.timestamp)
        assertTrue(tracking.esEmergencia)
    }

    @Test
    fun `TrackingResponse esEmergencia por defecto en false`() {
        val json = """{"longitud": 1.0, "latitud": 2.0, "timestamp": "2026-08-09T18:20:30Z"}"""

        val tracking = gson.fromJson(json, TrackingResponse::class.java)

        assertFalse(tracking.esEmergencia)
    }

    @Test
    fun `TrackingGpsRequest serializa pacienteId esEmergencia y sourceMessageId`() {
        val request = TrackingGpsRequest(
            pacienteId = "p-1",
            latitud = -33.45,
            longitud = -70.66,
            esEmergencia = true,
            sourceMessageId = "/gps/tracking/42"
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"pacienteId\":\"p-1\""))
        assertTrue(json.contains("\"esEmergencia\":true"))
        assertTrue(json.contains("\"sourceMessageId\":\"/gps/tracking/42\""))
    }

    @Test
    fun `TrackingGpsRequest no exige pacienteId cuando es nulo`() {
        val request = TrackingGpsRequest(
            pacienteId = null,
            latitud = 1.0,
            longitud = 2.0
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"esEmergencia\":false"))
        assertFalse(json.contains("\"pacienteId\""))
    }

    // ============ ACCESO / PLAN ============

    @Test
    fun `EffectiveAccessResponse parsea plan con gpsContinuo y permisos`() {
        val json = """
            {
              "rol": "dueno",
              "pacienteId": "p-1",
              "cuidadorDentroDelPlan": true,
              "plan": {"nombre": "Guardian Pro", "gpsContinuo": true},
              "permisos": ["patient.read", "account.profile"]
            }
        """.trimIndent()

        val acceso = gson.fromJson(json, EffectiveAccessResponse::class.java)

        assertEquals("dueno", acceso.rol)
        assertEquals("p-1", acceso.pacienteId)
        assertTrue(acceso.cuidadorDentroDelPlan)
        assertNotNull(acceso.plan)
        assertEquals("Guardian Pro", acceso.plan!!.nombre)
        assertTrue(acceso.plan!!.gpsActivo)
        assertEquals(setOf("patient.read", "account.profile"), acceso.permisos.toSet())
    }

    @Test
    fun `EffectiveAccessResponse plan acepta nombre alternativo gpsActivo`() {
        val json = """
            {"rol": "cuidador", "plan": {"nombre": "Plan Familiar", "gpsActivo": true}}
        """.trimIndent()

        val acceso = gson.fromJson(json, EffectiveAccessResponse::class.java)

        assertEquals("cuidador", acceso.rol)
        assertTrue(acceso.plan!!.gpsActivo)
    }

    @Test
    fun `EffectiveAccessResponse sin plan deja gps inactivo`() {
        val json = """{"rol": "cuidador", "pacienteId": "p-9"}"""

        val acceso = gson.fromJson(json, EffectiveAccessResponse::class.java)

        assertNull(acceso.plan)
        assertFalse(acceso.plan?.gpsActivo ?: false)
    }

    @Test
    fun `MiPlanResponse usa alternates id y retencionHistorialDias`() {
        val json = """
            {"planId": "plan-2", "nombre": "Esencial", "retencionHistorialDias": 30, "gpsContinuo": true, "consolaIaActiva": true}
        """.trimIndent()

        val plan = gson.fromJson(json, MiPlanResponse::class.java)

        assertEquals("plan-2", plan.planId)
        assertEquals("Esencial", plan.nombre)
        assertEquals(30, plan.retencionHistorialDias)
        assertTrue(plan.gpsActivo)
        assertTrue(plan.consolaIaActiva)
    }

    // ============ DASHBOARD ============

    @Test
    fun `DashboardSummary parsea ultimaUbicacion anidada y listas`() {
        val json = """
            {
              "paciente": {"id": "p-1", "nombre": "Juan"},
              "ultimaUbicacion": {"longitud": -70.66, "latitud": -33.45, "timestamp": "2026-08-09T18:20:30Z", "esEmergencia": true},
              "alertasPendientesCount": 3,
              "alertasRecientes": [
                {"id": "a-1", "nivel": "alto", "titulo": "Alerta 1"},
                {"id": "a-2", "nivel": "bajo", "titulo": "Alerta 2"}
              ],
              "eventosRecientes": [{"id": "e-1", "nivelRiesgo": "Critico Alto"}]
            }
        """.trimIndent()

        val dashboard = gson.fromJson(json, DashboardSummary::class.java)

        assertEquals("Juan", dashboard.paciente?.nombre)
        val ubicacion = dashboard.ultimaUbicacion
        assertNotNull(ubicacion)
        assertEquals(-33.45, ubicacion!!.latitud, 0.0001)
        assertEquals(3, dashboard.alertasPendientesCount)
        assertEquals(2, dashboard.alertasRecientes.size)
        assertEquals(1, dashboard.eventosRecientes.size)
        assertEquals("a-1", dashboard.alertasRecientes[0].id)
        assertTrue(dashboard.alertasRecientes[0].titulo == "Alerta 1")
    }

    @Test
    fun `DashboardSummary usa defaults cuando faltan campos`() {
        val dashboard = gson.fromJson("{}", DashboardSummary::class.java)

        assertNull(dashboard.paciente)
        assertNull(dashboard.ultimaUbicacion)
        assertEquals(0, dashboard.alertasPendientesCount)
        assertTrue(dashboard.alertasRecientes.isEmpty())
        assertTrue(dashboard.eventosRecientes.isEmpty())
    }

    // ============ AUTH COMPAT ============

    @Test
    fun `LoginCodigoResponse usa accessToken cuando esta presente`() {
        val response = gson.fromJson(
            """{"accessToken": "abc", "refreshToken": "r", "userId": "u", "nombre": "N", "rol": "dueno"}""",
            LoginCodigoResponse::class.java
        )

        assertEquals("abc", response.effectiveAccessToken)
    }

    @Test
    fun `LoginCodigoResponse cae a token cuando accessToken ausente`() {
        val response = gson.fromJson(
            """{"token": "legacy", "refreshToken": "r", "userId": "u", "nombre": "N", "rol": "dueno"}""",
            LoginCodigoResponse::class.java
        )

        assertEquals("legacy", response.effectiveAccessToken)
    }

    @Test
    fun `Verificar2faRequest envia codigo como fallback de codigoOtp`() {
        val request = Verificar2faRequest(correo = "a@b.cl", codigoOtp = "123456")

        val json = gson.toJson(request)

        assertTrue(json.contains("\"correo\":\"a@b.cl\""))
        assertTrue(json.contains("\"codigo\":\"123456\""))
    }

    @Test
    fun `RefreshTokenRequest serializa ambos tokens`() {
        val json = gson.toJson(RefreshTokenRequest(accessToken = "a", refreshToken = "r"))

        assertTrue(json.contains("\"accessToken\":\"a\""))
        assertTrue(json.contains("\"refreshToken\":\"r\""))
    }

    @Test
    fun `MessageResponse parsea requiresVerification opcional`() {
        val msg = gson.fromJson("""{"message": "OK"}""", MessageResponse::class.java)
        assertEquals("OK", msg.message)
        assertNull(msg.requiresVerification)
    }
}
