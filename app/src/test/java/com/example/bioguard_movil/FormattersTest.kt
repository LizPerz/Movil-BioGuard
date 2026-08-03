package com.example.bioguard_movil

import com.example.bioguard_movil.data.Formatters
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
        assertEquals("2000-01-01", Formatters.toIsoDate("1/1/2000"))
        assertEquals("2001-02-03", Formatters.toIsoDate("  03/02/2001  "))
    }

    @Test
    fun toIsoDate_rejectsInvalidInput() {
        assertNull(Formatters.toIsoDate(""))
        assertNull(Formatters.toIsoDate("31/13/2020"))
        assertNull(Formatters.toIsoDate("32/01/2020"))
        assertNull(Formatters.toIsoDate("01/01/1899"))
        assertNull(Formatters.toIsoDate("03-07-1995"))
        assertNull(Formatters.toIsoDate("abc"))
    }

    @Test
    fun toSexoCode_mapsGender() {
        assertEquals("M", Formatters.toSexoCode("Masculino"))
        assertEquals("F", Formatters.toSexoCode("Femenino"))
        assertEquals("F", Formatters.toSexoCode("femenino"))
        assertEquals("M", Formatters.toSexoCode(""))
    }
}
