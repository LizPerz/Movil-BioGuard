package com.bioguard.movil.data

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object Formatters {

    /**
     * Convierte cualquier formato de fecha (DD/MM/AAAA, AAAA-MM-DD, DD-MM-AAAA, AAAA/MM/DD)
     * a formato ISO estándar "YYYY-MM-DD" de manera ultra segura sin lanzar excepciones.
     */
    fun toIsoDate(input: String): String? {
        val clean = input.trim()
        if (clean.isBlank()) return null
        val normalized = clean.replace('.', '/').replace('-', '/')
        val parsed = when {
            clean.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) ->
                runCatching { LocalDate.parse(clean, ISO_DATE) }.getOrNull()
            normalized.matches(Regex("""^\d{2}/\d{2}/\d{4}$""")) ->
                runCatching { LocalDate.parse(normalized, DISPLAY_DATE) }.getOrNull()
            clean.matches(Regex("""^\d{8}$""")) ->
                runCatching { LocalDate.parse(clean, COMPACT_DATE) }.getOrNull()
            else -> null
        } ?: return null

        if (parsed.year !in 1900..LocalDate.now().year || parsed.isAfter(LocalDate.now())) return null
        return parsed.format(ISO_DATE)
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
