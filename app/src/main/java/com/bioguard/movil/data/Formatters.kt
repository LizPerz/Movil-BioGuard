package com.bioguard.movil.data

import java.time.Instant
import java.time.OffsetDateTime

object Formatters {

    /**
     * Convierte cualquier formato de fecha (DD/MM/AAAA, AAAA-MM-DD, DD-MM-AAAA, AAAA/MM/DD)
     * a formato ISO estándar "YYYY-MM-DD" de manera ultra segura sin lanzar excepciones.
     */
    fun toIsoDate(input: String): String {
        val clean = input.trim()
        if (clean.isBlank()) return "2000-01-01"

        // Si ya viene en formato AAAA-MM-DD
        if (clean.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
            return clean
        }

        // Dividir por delimitadores comunes: / - .
        val parts = clean.split(Regex("""[/.\-]""")).filter { it.isNotBlank() }
        if (parts.size == 3) {
            val p1 = parts[0].toIntOrNull() ?: 1
            val p2 = parts[1].toIntOrNull() ?: 1
            val p3 = parts[2].toIntOrNull() ?: 2000

            // Caso AAAA/MM/DD
            if (p1 > 1000) {
                val year = p1.coerceIn(1900, 2100)
                val month = p2.coerceIn(1, 12)
                val day = p3.coerceIn(1, 31)
                return "%04d-%02d-%02d".format(year, month, day)
            }
            // Caso DD/MM/AAAA o DD-MM-AAAA (ej. 23/12/2003)
            else if (p3 > 1000) {
                val day = p1.coerceIn(1, 31)
                val month = p2.coerceIn(1, 12)
                val year = p3.coerceIn(1900, 2100)
                return "%04d-%02d-%02d".format(year, month, day)
            }
        }

        return "2000-01-01"
    }

    /**
     * Convierte una fecha ISO (YYYY-MM-DD) a formato legible para el usuario (DD/MM/YYYY)
     */
    fun toDisplayDate(isoOrRaw: String): String {
        val iso = toIsoDate(isoOrRaw)
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
}
