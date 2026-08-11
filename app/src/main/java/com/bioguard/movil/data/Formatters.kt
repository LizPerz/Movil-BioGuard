package com.bioguard.movil.data

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object Formatters {

    /**
     * Convierte cualquier formato de fecha (DD/MM/AAAA, AAAA-MM-DD, DD-MM-AAAA, AAAA/MM/DD
     * y timestamps como "AAAA-MM-DDTHH:mm:ss" o con zona horaria) a ISO "YYYY-MM-DD".
     * Tolerante a la fecha con hora que devuelve el backend.
     */
    fun toIsoDate(input: String): String? {
        val clean = input.trim()
        if (clean.isBlank()) return null

        val day = runCatching { OffsetDateTime.parse(clean).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(clean).toLocalDate() }.getOrNull()

        val parsed = when {
            day != null -> day
            clean.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) ->
                runCatching { LocalDate.parse(clean, ISO_DATE) }.getOrNull()
            clean.replace('.', '/').replace('-', '/').matches(Regex("""^\d{2}/\d{2}/\d{4}$""")) ->
                runCatching {
                    LocalDate.parse(clean.replace('.', '/').replace('-', '/'), DISPLAY_DATE)
                }.getOrNull()
            clean.matches(Regex("""^\d{8}$""")) ->
                runCatching { LocalDate.parse(clean, COMPACT_DATE) }.getOrNull()
            else -> null
        } ?: return null

        if (parsed.year !in 1900..LocalDate.now().year || parsed.isAfter(LocalDate.now())) return null
        return parsed.format(ISO_DATE)
    }

    /**
     * Extrae solo los dígitos de una fecha escrita (max 8) para usarla como estado de un campo.
     * Si viene en ISO (YYYY-MM-DD) la convierte primero a DDMMYYYY para mantener el orden visible.
     * La mascara visual inserta las barras sin romper el cursor.
     */
    fun toDisplayDigits(display: String): String {
        val clean = display.trim()
        val iso = if (clean.length >= 8 && clean.indexOf('-') == 4) {
            toIsoDate(clean)
        } else null
        val source = iso?.let { toDisplayDate(it) } ?: display
        return source.filter(Char::isDigit).take(8)
    }

    /**
     * VisualTransformation que muestra "dd/mm/aaaa" a partir de dígitos puros.
     * El cursor nunca salta porque el estado del campo solo contiene dígitos.
     */
    val dateMaskTransformation: VisualTransformation = VisualTransformation { text ->
        val digits = text.text.filter(Char::isDigit).take(8)
        val formatted = formatDigitsAsDate(digits)
        TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val n = offset.coerceIn(0, digits.length)
                    val slashes = (if (n > 2) 1 else 0) + (if (n > 4) 1 else 0)
                    return (n + slashes).coerceIn(0, formatted.length)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val p = offset.coerceIn(0, formatted.length)
                    val slashes = formatted.substring(0, p).count { it == '/' }
                    return (p - slashes).coerceIn(0, digits.length)
                }
            }
        )
    }

    private fun formatDigitsAsDate(digits: String): String {
        return buildString {
            digits.forEachIndexed { index, c ->
                if (index == 2 || index == 4) append('/')
                append(c)
            }
        }
    }

    fun formatDateInput(input: String): String {
        val digits = input.filter(Char::isDigit).take(8)
        return when {
            digits.length >= 5 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
            digits.length >= 3 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
            else -> digits
        }
    }

    /**
     * Convierte una fecha ISO (YYYY-MM-DD) a formato legible para el usuario (DD/MM/YYYY)
     */
    fun toDisplayDate(isoOrRaw: String): String {
        val iso = toIsoDate(isoOrRaw) ?: return isoOrRaw
        val parts = iso.split("-")
        return if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            isoOrRaw
        }
    }

    fun toSexoCode(sexo: String): String =
        if (sexo.equals("Femenino", ignoreCase = true)) "F" else "M"

    fun sexoToDisplay(sexo: String): String = when {
        sexo.equals("F", ignoreCase = true) -> "Femenino"
        sexo.equals("M", ignoreCase = true) -> "Masculino"
        sexo.equals("Masculino", ignoreCase = true) -> "Masculino"
        sexo.equals("Femenino", ignoreCase = true) -> "Femenino"
        sexo.equals("O", ignoreCase = true) || sexo.equals("Otro", ignoreCase = true) -> "Otro"
        else -> sexo.ifBlank { "Sin registrar" }
    }

    fun sexoToCode(sexo: String): String = when {
        sexo.equals("F", ignoreCase = true) || sexo.equals("Femenino", ignoreCase = true) -> "F"
        sexo.equals("O", ignoreCase = true) || sexo.equals("Otro", ignoreCase = true) -> "O"
        else -> "M"
    }

    /**
     * Calcula la edad en años a partir de una fecha en formato ISO (YYYY-MM-DD).
     * Retorna null si la fecha es inválida o futura.
     */
    fun calculateAge(isoDate: String?): Int? {
        val parsed = toIsoDate(isoDate ?: return null) ?: return null
        val birth = runCatching { LocalDate.parse(parsed, ISO_DATE) }.getOrNull() ?: return null
        val today = LocalDate.now()
        if (birth.isAfter(today)) return null
        var age = today.year - birth.year
        if (today.dayOfYear < birth.dayOfYear) age--
        return age
    }

    fun parseIsoTimestamp(timestamp: String): Instant? =
        runCatching { OffsetDateTime.parse(timestamp).toInstant() }
            .getOrElse {
                runCatching { Instant.parse(if (timestamp.endsWith("Z")) timestamp else "${timestamp}Z") }.getOrNull()
            }

    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)
    private val COMPACT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMuuuu")
        .withResolverStyle(ResolverStyle.STRICT)
}
