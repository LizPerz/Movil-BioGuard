package com.bioguard.movil.ml

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * F1-F3 Motor for Glycemic Peak Prediction (Pico Glucémico)
 * Based on Python implementation in ML repo
 * 
 * F1: IMC calculation from weight/height
 * F2: Logistic z-score from vitals + IMC
 * F3: Sigmoid P(Pico) from z-score
 * Risk matrix: classification into clinical case
 */
data class PesosPico(
    val w0: Double = -8.0,      // intercept
    val w1: Double = 0.05,      // pulso (bpm)
    val w2: Double = 0.03,      // estrés (%)
    val w3: Double = -0.04,     // temperatura (°C)
    val w4: Double = 0.15       // IMC
) {
    companion object {
        val DEFAULT = PesosPico()
    }
}

data class GlycemicPrediction(
    val imc: Double,
    val z: Double,
    val pPico: Double,  // P(Pico) ∈ [0,1]
    val casoClinico: String,  // "Hipoglucemia Nocturna", "Hiperglucemia Severa", "Estado Optimo", "Vigilancia"
    val nivelRiesgo: String,  // "Critico Alto", "Moderado Alto", "Bajo (Estable)", "Por evaluar"
    val accionAutomatizada: String?  // action description
)

class GlycemicPeakPredictor(private val pesos: PesosPico = PesosPico.DEFAULT) {

    /**
     * F1: IMC = peso(kg) / estatura(m)^2
     */
    fun calcularIMC(pesoKg: Double, estaturaCm: Double): Double {
        if (estaturaCm <= 0 || pesoKg <= 0) return 0.0
        val estaturaM = estaturaCm / 100.0
        return pesoKg / (estaturaM * estaturaM)
    }

    /**
     * F2: Logistic z-score
     * z = w0 + w1*pulso + w2*estres + w3*temp + w4*imc
     */
    fun calcularZ(
        pulsoBpm: Double,
        estresPct: Double,
        temperaturaC: Double,
        imc: Double
    ): Double {
        return pesos.w0 +
                pesos.w1 * pulsoBpm +
                pesos.w2 * estresPct +
                pesos.w3 * temperaturaC +
                pesos.w4 * imc
    }

    /**
     * F3: Sigmoid function for P(Pico)
     * P(Pico) = 1 / (1 + exp(-z))
     */
    fun calcularPPico(z: Double): Double {
        return 1.0 / (1.0 + exp(-z))
    }

    /**
     * Risk matrix classification (sensores físicos reales del Galaxy Watch 7)
     * Returns clinical case, risk level, and recommended action
     *   - Hipoglucemia Nocturna: Pulso > 110, Temp < 35.0°C, Estres > 80%   -> Critico Alto
     *   - Hiperglucemia Severa:  Pulso 95-110, Temp > 37.2°C, Estres 60-80% -> Moderado Alto
     *   - Estado Optimo:         Pulso 60-80, Temp 36.0-36.7°C, Estres < 50% -> Bajo (Estable)
     *   - Vigilancia:            resto
     */
    fun clasificarRiesgo(
        pulsoBpm: Double,
        temperaturaC: Double,
        estresPct: Double
    ): Triple<String, String, String?> {
        return when {
            // Hipoglucemia Nocturna: Pulso >110 AND Temp <35 AND Estres >80%
            pulsoBpm > 110 && temperaturaC < 35 && estresPct > 80 -> {
                Triple("Hipoglucemia Nocturna", "Critico Alto", "Detonar alerta sonora/haptica en Reloj y Celular; si no hay respuesta en 60s, enviar SMS con ubicacion GPS a red familiar.")
            }
            // Hiperglucemia Severa: Pulso 95-110 AND Temp >37.2 AND Estres 60-80%
            pulsoBpm in 95.0..110.0 && temperaturaC > 37.2 && estresPct in 60.0..80.0 -> {
                Triple("Hiperglucemia Severa", "Moderado Alto", "Enviar notificacion dirigida al celular con recomendaciones de hidratacion y caminata ligera.")
            }
            // Estado Optimo: Pulso 60-80 bpm AND Temp 36-36.7°C AND Estres <50%
            pulsoBpm in 60.0..80.0 && temperaturaC in 36.0..36.7 && estresPct < 50 -> {
                Triple("Estado Optimo", "Bajo (Estable)", "Mantener streaming BLE continuo cada 10 segundos y realizar almacenamiento silencioso en SQLite local.")
            }
            // Vigilancia: monitoreo continuo
            else -> {
                Triple("Vigilancia", "Por evaluar", "Mantener monitoreo continuo; evaluar siguiente lectura en 10 segundos.")
            }
        }
    }

    /**
     * Full prediction pipeline
     * Returns complete glycemic peak prediction with all calculated fields
     */
    fun predecir(
        pesoKg: Double,
        estaturaCm: Double,
        pulsoBpm: Double,
        temperaturaC: Double,
        estresPct: Double
    ): GlycemicPrediction {
        // F1: IMC
        val imc = calcularIMC(pesoKg, estaturaCm)

        // F2: z-score
        val z = calcularZ(pulsoBpm, estresPct, temperaturaC, imc)

        // F3: P(Pico)
        val pPico = calcularPPico(z)

        // Risk matrix classification
        val (casoClinico, nivelRiesgo, accion) = clasificarRiesgo(
            pulsoBpm,
            temperaturaC,
            estresPct
        )

        return GlycemicPrediction(
            imc = imc,
            z = z,
            pPico = pPico,
            casoClinico = casoClinico,
            nivelRiesgo = nivelRiesgo,
            accionAutomatizada = accion
        )
    }

    companion object {
        const val VERSION = "pico-v1.0"

        /**
         * Create predictor with custom pesos from backend or use defaults
         */
        fun withCustomPesos(
            w0: Double = -8.0,
            w1: Double = 0.05,
            w2: Double = 0.03,
            w3: Double = -0.04,
            w4: Double = 0.15
        ): GlycemicPeakPredictor {
            return GlycemicPeakPredictor(PesosPico(w0, w1, w2, w3, w4))
        }
    }
}
