package com.bioguard.movil.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlycemicPeakPredictorTest {

    private val predictor = GlycemicPeakPredictor()

    // ============ F1: IMC ============

    @Test
    fun `calcularIMC usa metros al cuadrado`() {
        assertEquals(22.85714, predictor.calcularIMC(70.0, 175.0), 0.001)
    }

    @Test
    fun `calcularIMC retorna cero con estatura o peso no positivos`() {
        assertEquals(0.0, predictor.calcularIMC(70.0, 0.0), 0.0)
        assertEquals(0.0, predictor.calcularIMC(0.0, 175.0), 0.0)
        assertEquals(0.0, predictor.calcularIMC(70.0, -10.0), 0.0)
        assertEquals(0.0, predictor.calcularIMC(-5.0, 175.0), 0.0)
    }

    // ============ F2: z-score ============

    @Test
    fun `calcularZ con pesos por defecto`() {
        // z = -8 + 0.05*72 + 0.03*30 - 0.04*36.6 + 0.15*22.85714 = -1.535429
        val imc = predictor.calcularIMC(70.0, 175.0)
        assertEquals(-1.535429, predictor.calcularZ(72.0, 30.0, 36.6, imc), 0.0001)
    }

    @Test
    fun `calcularZ sube con mayor pulso y estres`() {
        val imc = 22.85714
        val base = predictor.calcularZ(60.0, 20.0, 36.5, imc)
        val alto = predictor.calcularZ(120.0, 90.0, 36.5, imc)
        assertTrue(alto > base)
    }

    @Test
    fun `calcularZ baja con mayor temperatura`() {
        val imc = 22.85714
        val frio = predictor.calcularZ(72.0, 30.0, 34.0, imc)
        val caliente = predictor.calcularZ(72.0, 30.0, 39.0, imc)
        assertTrue(frio > caliente)
    }

    @Test
    fun `calcularZ con pesos cero da el intercepto`() {
        val predictorNeutro = GlycemicPeakPredictor.withCustomPesos(w0 = 0.0, w1 = 0.0, w2 = 0.0, w3 = 0.0, w4 = 0.0)
        assertEquals(0.0, predictorNeutro.calcularZ(72.0, 30.0, 36.6, 22.85714), 0.0)
    }

    // ============ F3: P(Pico) ============

    @Test
    fun `calcularPPico es simetrica en cero`() {
        assertEquals(0.5, predictor.calcularPPico(0.0), 0.0001)
    }

    @Test
    fun `calcularPPico satura en los extremos`() {
        assertTrue(predictor.calcularPPico(10.0) > 0.999)
        assertTrue(predictor.calcularPPico(-10.0) < 0.001)
    }

    @Test
    fun `calcularPPico se mantiene en el intervalo 0 1`() {
        for (z in -20..20 step 1) {
            val p = predictor.calcularPPico(z.toDouble())
            assertTrue("pPico($z)=$p fuera de [0,1]", p in 0.0..1.0)
        }
    }

    // ============ Matriz de riesgo ============

    @Test
    fun `clasificarRiesgo detecta hipoglucemia nocturna`() {
        val (caso, nivel, accion) = predictor.clasificarRiesgo(pulsoBpm = 120.0, temperaturaC = 34.5, estresPct = 85.0)
        assertEquals("Hipoglucemia Nocturna", caso)
        assertEquals("Critico Alto", nivel)
        assertNotNull(accion)
    }

    @Test
    fun `clasificarRiesgo detecta hiperglucemia severa en el borde inferior`() {
        val (caso, nivel, _) = predictor.clasificarRiesgo(pulsoBpm = 95.0, temperaturaC = 37.3, estresPct = 60.0)
        assertEquals("Hiperglucemia Severa", caso)
        assertEquals("Moderado Alto", nivel)
    }

    @Test
    fun `clasificarRiesgo detecta hiperglucemia severa en el borde superior`() {
        val (caso, _, _) = predictor.clasificarRiesgo(pulsoBpm = 110.0, temperaturaC = 37.3, estresPct = 80.0)
        assertEquals("Hiperglucemia Severa", caso)
    }

    @Test
    fun `clasificarRiesgo sale del rango de hiperglucemia a 111 bpm`() {
        val (caso, _, _) = predictor.clasificarRiesgo(pulsoBpm = 111.0, temperaturaC = 37.3, estresPct = 70.0)
        assertTrue(caso != "Hiperglucemia Severa")
    }

    @Test
    fun `clasificarRiesgo detecta estado optimo`() {
        val (caso, nivel, accion) = predictor.clasificarRiesgo(pulsoBpm = 70.0, temperaturaC = 36.3, estresPct = 40.0)
        assertEquals("Estado Optimo", caso)
        assertEquals("Bajo (Estable)", nivel)
        assertNotNull(accion)
    }

    @Test
    fun `clasificarRiesgo cae en vigilancia para el resto`() {
        val (caso, nivel, accion) = predictor.clasificarRiesgo(pulsoBpm = 85.0, temperaturaC = 36.8, estresPct = 55.0)
        assertEquals("Vigilancia", caso)
        assertEquals("Por evaluar", nivel)
        assertNotNull(accion)
    }

    // ============ Pipeline completo ============

    @Test
    fun `predecir encadena F1 F2 F3 y la matriz de riesgo`() {
        val result = predictor.predecir(
            pesoKg = 70.0, estaturaCm = 175.0,
            pulsoBpm = 120.0, temperaturaC = 34.5, estresPct = 85.0
        )

        assertEquals(22.85714, result.imc, 0.001)
        assertEquals("Hipoglucemia Nocturna", result.casoClinico)
        assertEquals("Critico Alto", result.nivelRiesgo)
        assertTrue(result.pPico in 0.0..1.0)
        assertNotNull(result.accionAutomatizada)
        // z y pPico consistentes: pPico = sigmoid(z)
        val expected = predictor.calcularPPico(result.z)
        assertEquals(expected, result.pPico, 0.0001)
    }

    @Test
    fun `predecir con signos normales clasifica como estado optimo`() {
        val result = predictor.predecir(
            pesoKg = 70.0, estaturaCm = 175.0,
            pulsoBpm = 70.0, temperaturaC = 36.3, estresPct = 40.0
        )

        assertEquals("Estado Optimo", result.casoClinico)
        assertEquals("Bajo (Estable)", result.nivelRiesgo)
    }

    @Test
    fun `predecir con datos invalidos de IMC no revienta`() {
        val result = predictor.predecir(
            pesoKg = 0.0, estaturaCm = 0.0,
            pulsoBpm = 100.0, temperaturaC = 37.8, estresPct = 70.0
        )

        assertEquals(0.0, result.imc, 0.0)
        assertEquals("Hiperglucemia Severa", result.casoClinico)
        assertTrue(result.pPico in 0.0..1.0)
    }

    @Test
    fun `withCustomPesos con pesos neutros da probabilidad 0 5`() {
        val neutro = GlycemicPeakPredictor.withCustomPesos(0.0, 0.0, 0.0, 0.0, 0.0)
        assertEquals(0.5, neutro.calcularPPico(neutro.calcularZ(72.0, 30.0, 36.6, 22.85714)), 0.0001)
    }
}
