package com.bioguard.movil.service

import com.bioguard.movil.network.HeartbeatRequest
import com.bioguard.movil.network.LecturaSensorRequest
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

object WearablePayloadValidator {
    const val MAX_PAYLOAD_BYTES = 16 * 1024
    private val telemetryPath = Regex("^/bioguard/telemetry/[A-Za-z0-9._:-]{1,160}$")

    fun isAllowedPath(path: String): Boolean = path in setOf(
        "/sensors/readings",
        "/sensors/events",
        "/sensors/alerts",
        "/sensors/heartbeat",
        "/bioguard/pair/ack",
        "/bioguard/heartbeat",
        "/commands/dismiss",
        "/sensors/dismiss"
    ) || telemetryPath.matches(path)

    fun isValid(reading: LecturaSensorRequest): Boolean {
        if (!reading.pulsoBpm.isFinite() || reading.pulsoBpm !in 1.0..250.0) return false
        if (!reading.temperaturaC.isFinite() || (reading.temperaturaC != 0.0 && reading.temperaturaC !in 20.0..50.0)) return false
        if (!reading.sudoracionGsr.isFinite() || reading.sudoracionGsr !in 0.0..1_000.0) return false
        if (!reading.hrv.isNullOrFiniteIn(0.0..1_000.0)) return false
        if (!reading.spo2.isNullOrFiniteIn(50.0..100.0, zeroIsUnavailable = true)) return false
        if (reading.pasos != null && reading.pasos !in 0..200_000) return false
        if (reading.timestamp.isNullOrBlank()) return false
        return true
    }

    fun isValid(heartbeat: HeartbeatRequest): Boolean {
        if (heartbeat.bateria != null && heartbeat.bateria !in -1..100) return false
        val sensors = heartbeat.sensoresActivos ?: return true
        return sensors.size <= 32 && sensors.all { it.length in 1..64 }
    }

    fun isValid(event: WatchEventDto): Boolean =
        event.bpm.isFinite() && event.bpm in 0f..250f &&
            event.temperatura.isFinite() && event.temperatura in 0f..45f &&
            event.sudoracionGsr.isFinite() && event.sudoracionGsr in 0f..1_000f &&
            event.nivelRiesgo.length in 1..32 && event.tipoEvento.length in 1..80 &&
            event.descripcion.length in 1..500 &&
            (event.probabilidadMl == null || (event.probabilidadMl.isFinite() && event.probabilidadMl in 0.0..1.0))

    fun isValid(alert: WatchAlertDto): Boolean =
        alert.tipoAlerta.length in 1..80 && alert.mensaje.length in 1..500 &&
            alert.nivelRiesgo.length in 1..32 && alert.bpm.isFinite() && alert.bpm in 0f..250f &&
            alert.temperatura.isFinite() && alert.temperatura in 0f..45f &&
            alert.sudoracionGsr.isFinite() && alert.sudoracionGsr in 0f..1_000f

    private fun Double?.isNullOrFiniteIn(range: ClosedFloatingPointRange<Double>, zeroIsUnavailable: Boolean = false): Boolean {
        if (this == null) return true
        if (zeroIsUnavailable && this == 0.0) return true
        return isFinite() && this in range
    }
}
