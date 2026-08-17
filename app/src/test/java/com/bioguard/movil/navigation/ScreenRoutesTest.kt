package com.bioguard.movil.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRoutesTest {

    @Test
    fun `bottomBarRoutes incluye la tab de Ubicacion`() {
        val routes = Screen.bottomBarRoutes

        assertEquals(listOf(Screen.DASHBOARD, Screen.ANALYSIS, Screen.REPORTS, Screen.DEVICE, Screen.PROFILE, Screen.LOCATION), routes)
        assertTrue(routes.contains(Screen.LOCATION))
    }

    @Test
    fun `las rutas del bottom bar son unicas`() {
        val routes = Screen.bottomBarRoutes
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `Screen LOCATION tiene el contrato de deep links esperado`() {
        assertEquals("location", Screen.LOCATION)
    }
}
