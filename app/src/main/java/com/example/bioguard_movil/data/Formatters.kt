package com.example.bioguard_movil.data

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

object Formatters {

    fun toIsoDate(ddMmYyyy: String): String? {
        val parts = ddMmYyyy.trim().split("/")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        if (year !in 1900..2100) return null
        return try {
            val date = LocalDate.of(year, month, day)
            "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
        } catch (e: DateTimeException) {
            null
        }
    }

    fun toSexoCode(sexo: String): String =
        if (sexo.equals("Femenino", ignoreCase = true)) "F" else "M"

    fun parseIsoTimestamp(timestamp: String): Instant? =
        runCatching { OffsetDateTime.parse(timestamp).toInstant() }
            .getOrElse {
                runCatching {
                    Instant.parse(timestamp.removeSuffix("Z") + "Z")
                }.getOrNull()
            }
}
