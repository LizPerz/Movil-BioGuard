package com.bioguard.movil

import androidx.compose.ui.text.AnnotatedString
import com.bioguard.movil.data.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

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

    @Test
    fun `parseIsoTimestamp maneja UTC y offset explicito`() {
        assertEquals(
            Instant.parse("2026-08-09T18:20:30Z"),
            Formatters.parseIsoTimestamp("2026-08-09T18:20:30Z")
        )
        // +06:00 se convierte al instante UTC equivalente
        assertEquals(
            Instant.parse("2026-08-09T12:20:30Z"),
            Formatters.parseIsoTimestamp("2026-08-09T18:20:30+06:00")
        )
    }

    @Test
    fun `parseIsoTimestamp trata timestamp sin zona como UTC`() {
        assertEquals(
            Instant.parse("2026-08-09T18:20:30Z"),
            Formatters.parseIsoTimestamp("2026-08-09T18:20:30")
        )
    }

    @Test
    fun `parseIsoTimestamp devuelve null para invalidos`() {
        assertNull(Formatters.parseIsoTimestamp(""))
        assertNull(Formatters.parseIsoTimestamp("abc"))
        assertNull(Formatters.parseIsoTimestamp("2026-13-45T99:00:00"))
    }

    @Test
    fun `formatInstant formatea en la zona indicada`() {
        val instant = Instant.parse("2026-08-09T18:20:30Z")

        assertEquals(
            "18:20:30  09/08/2026",
            Formatters.formatInstant(instant, ZoneId.of("UTC"))
        )
        assertEquals(
            "20:20:30  09/08/2026",
            Formatters.formatInstant(instant, ZoneId.of("Europe/Berlin"))
        )
    }

    @Test
    fun `formatInstant convierte timestamps con offset`() {
        val instant = Formatters.parseIsoTimestamp("2026-08-09T18:20:30+06:00")!!

        assertEquals("12:20:30  09/08/2026", Formatters.formatInstant(instant, ZoneId.of("UTC")))
    }

    @Test
    fun toDisplayDate_convierteIsoADisplay() {
        assertEquals("03/07/1995", Formatters.toDisplayDate("1995-07-03"))
        assertEquals("passthrough", Formatters.toDisplayDate("passthrough"))
    }

    @Test
    fun toDisplayDigits_ordenaDigitosCuandoVieneIso() {
        assertEquals("03071995", Formatters.toDisplayDigits("1995-07-03"))
        assertEquals("03121990", Formatters.toDisplayDigits("03/12/1990"))
    }

    @Test
    fun sexoToDisplay_mapsCodigosYEtiquetas() {
        assertEquals("Femenino", Formatters.sexoToDisplay("F"))
        assertEquals("Masculino", Formatters.sexoToDisplay("M"))
        assertEquals("Masculino", Formatters.sexoToDisplay("Masculino"))
        assertEquals("Femenino", Formatters.sexoToDisplay("femenino"))
        assertEquals("Otro", Formatters.sexoToDisplay("O"))
        assertEquals("Otro", Formatters.sexoToDisplay("Otro"))
        assertEquals("Sin registrar", Formatters.sexoToDisplay(""))
    }

    @Test
    fun sexoToCode_aceptaEtiquetasYCodigos() {
        assertEquals("F", Formatters.sexoToCode("F"))
        assertEquals("F", Formatters.sexoToCode("Femenino"))
        assertEquals("O", Formatters.sexoToCode("Otro"))
        assertEquals("M", Formatters.sexoToCode("X"))
        assertEquals("M", Formatters.sexoToCode(""))
    }

    @Test
    fun `calculateAge calcula anios reales`() {
        val birth = LocalDate.of(1995, 7, 3)
        val expected = Period.between(birth, LocalDate.now()).years

        assertEquals(expected, Formatters.calculateAge("1995-07-03"))
    }

    @Test
    fun `calculateAge rechaza fechas invalidas o futuras`() {
        val future = LocalDate.now().plusYears(1).toString()

        assertNull(Formatters.calculateAge(future))
        assertNull(Formatters.calculateAge(null))
        assertNull(Formatters.calculateAge("abc"))
        assertNull(Formatters.calculateAge("31/02/1900"))
    }

    @Test
    fun `dateMaskTransformation agrupa digitos en dd slash mm slash aaaa`() {
        val transformed = Formatters.dateMaskTransformation.filter(AnnotatedString("12031995"))

        assertEquals("12/03/1995", transformed.text.text)
        // cursor: digito 4 (posicion original) -> posicion transformada 5
        assertEquals(5, transformed.offsetMapping.originalToTransformed(4))
        // y de vuelta
        assertEquals(4, transformed.offsetMapping.transformedToOriginal(5))
    }

    @Test
    fun `dateMaskTransformation mascara parcial`() {
        val transformed = Formatters.dateMaskTransformation.filter(AnnotatedString("123"))

        assertEquals("12/3", transformed.text.text)
    }
}
