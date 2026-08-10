package com.bioguard.movil

import com.bioguard.movil.data.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    @Test
    fun toIsoDate_convertsValidDate() {
        assertEquals("1995-07-03", Formatters.toIsoDate("03/07/1995"))
        assertEquals("2020-12-31", Formatters.toIsoDate("31/12/2020"))
    }

    @Test
    fun toIsoDate_handlesLeadingZerosAndWhitespace() {
        assertEquals(null, Formatters.toIsoDate("1/1/2000"))
        assertEquals("2001-02-03", Formatters.toIsoDate("  03/02/2001  "))
    }

    @Test
    fun toIsoDate_handlesInvalidInputWithFallback() {
        assertEquals(null, Formatters.toIsoDate(""))
        assertEquals("1995-07-03", Formatters.toIsoDate("03-07-1995"))
        assertEquals(null, Formatters.toIsoDate("abc"))
        assertEquals(null, Formatters.toIsoDate("31/02/2000"))
        assertEquals("2000-11-01", Formatters.toIsoDate("01112000"))
        assertEquals("01/11/2000", Formatters.formatDateInput("01112000"))
    }

    @Test
    fun toSexoCode_mapsGender() {
        assertEquals("M", Formatters.toSexoCode("Masculino"))
        assertEquals("F", Formatters.toSexoCode("Femenino"))
        assertEquals("F", Formatters.toSexoCode("femenino"))
        assertEquals("M", Formatters.toSexoCode(""))
    }
}
