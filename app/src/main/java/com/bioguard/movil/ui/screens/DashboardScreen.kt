package com.bioguard.movil.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.R
import com.bioguard.movil.ui.components.BioHealthChart
import com.bioguard.movil.ui.components.ChartPoint
import com.bioguard.movil.ui.components.ErrorRetryBox
import com.bioguard.movil.ui.components.VitalDetailBottomSheet
import com.bioguard.movil.ui.model.HistoryItem
import com.bioguard.movil.ui.model.VitalSign
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.YellowNeon
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.DashboardViewModel
import com.bioguard.movil.util.rememberBioHaptic
import java.time.Instant

data class VitalDetailData(
    val title: String,
    val value: String,
    val status: String,
    val unit: String,
    val points: List<ChartPoint>
)

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    @Suppress("UNUSED_PARAMETER")
    onPendingAlert: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by dashboardViewModel.uiState.collectAsState()
    val haptic = rememberBioHaptic()

    var activeVitalDetail by remember { mutableStateOf<VitalDetailData?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ultimaLectura = uiState.lecturasRecientes.firstOrNull()
    val isRecentData = ultimaLectura?.timestamp?.let { timestamp ->
        runCatching { Instant.parse(timestamp).toEpochMilli() }
            .getOrNull()
            ?.let { System.currentTimeMillis() - it in 0..120_000L }
    } == true
    val lastTemp = ultimaLectura?.temperaturaC
    val lastGsr = ultimaLectura?.sudoracionGsr
    val lastPulse = ultimaLectura?.pulsoBpm

    val tempStatus = when {
        lastTemp == null -> stringResource(R.string.dashboard_no_data)
        lastTemp > 38.5 -> stringResource(R.string.dashboard_alert)
        lastTemp > 37.5 -> stringResource(R.string.dashboard_elevated)
        lastTemp < 35.0 -> stringResource(R.string.dashboard_low)
        else -> stringResource(R.string.dashboard_normal)
    }
    val tempStatusColor = when (tempStatus) {
        stringResource(R.string.dashboard_alert) -> RedNeon
        stringResource(R.string.dashboard_elevated), stringResource(R.string.dashboard_low) -> YellowNeon
        else -> GreenNeon
    }

    val gsrStatus = when {
        lastGsr == null -> stringResource(R.string.dashboard_no_data)
        lastGsr >= 8.0 -> stringResource(R.string.dashboard_elevated)
        else -> stringResource(R.string.dashboard_normal)
    }
    val gsrStatusColor = when (gsrStatus) {
        stringResource(R.string.dashboard_elevated) -> YellowNeon
        else -> GreenNeon
    }

    val pulseStatus = when {
        lastPulse == null -> stringResource(R.string.dashboard_no_data)
        lastPulse > 120 -> stringResource(R.string.dashboard_alert)
        lastPulse > 100 -> stringResource(R.string.dashboard_elevated_m)
        lastPulse < 50 -> stringResource(R.string.dashboard_low_m)
        else -> stringResource(R.string.dashboard_normal)
    }

    var showHelpForVital by remember { mutableStateOf<VitalSign?>(null) }

    val formattedPulse = lastPulse?.toInt()?.toString() ?: "--"
    val formattedTemp = lastTemp?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "--"
    val formattedGsr = lastGsr?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "--"

    val lastGlucose = (ultimaLectura?.glucosaEstimadaMgDl?.takeIf { it > 0.0 }
        ?: (95.0 + ((lastPulse ?: 72.0) - 72.0) * 0.45 + ((lastTemp ?: 36.5) - 36.5) * 12.0 + kotlin.math.max(0.0, (lastGsr ?: 45.0) - 45.0) * 0.5)).coerceIn(70.0, 220.0)
    val formattedGlucose = String.format(java.util.Locale.US, "%.0f", lastGlucose)
    val glucoseStatus = when {
        lastGlucose > 140.0 -> "Pico Elevado (>140 mg/dL)"
        lastGlucose < 70.0 -> "Hipoglucemia (<70 mg/dL)"
        else -> "Normal / Estable"
    }
    val glucoseStatusColor = when {
        lastGlucose > 140.0 -> RedNeon
        lastGlucose < 70.0 -> YellowNeon
        else -> GreenNeon
    }

    val lastRisk = ultimaLectura?.probabilidadPico
    val riskStatus = when {
        lastRisk == null -> stringResource(R.string.dashboard_no_data)
        lastRisk >= 0.7 -> "RIESGO ALTO"
        lastRisk >= 0.5 -> "RIESGO MODERADO"
        else -> "RIESGO BAJO"
    }
    val riskStatusColor = when (riskStatus) {
        "RIESGO ALTO" -> RedNeon
        "RIESGO MODERADO" -> YellowNeon
        "RIESGO BAJO" -> GreenNeon
        else -> p.textSecondary
    }

    val vitalSigns = listOf(
        VitalSign("Estimación Picos de Glucosa", formattedGlucose, "mg/dL", Icons.Filled.WaterDrop, Color(0xFFF43F5E), glucoseStatus, glucoseStatusColor),
        VitalSign(stringResource(R.string.dashboard_heart_rate), formattedPulse, "BPM", Icons.Filled.Favorite, p.accent, pulseStatus, if (pulseStatus == stringResource(R.string.dashboard_normal)) GreenNeon else if (pulseStatus == stringResource(R.string.dashboard_alert)) RedNeon else YellowNeon),
        VitalSign(stringResource(R.string.dashboard_temperature), formattedTemp, "\u00b0C", Icons.Filled.DeviceThermostat, p.accentSecondary, tempStatus, tempStatusColor),
        VitalSign(stringResource(R.string.dashboard_conductivity), formattedGsr, "\u00b5S", Icons.Filled.Bolt, YellowNeon, gsrStatus, gsrStatusColor),
        VitalSign("Riesgo IA de Pico Glucémico", if (lastRisk != null) "${(lastRisk * 100).toInt()}%" else "--", "probabilidad", Icons.Filled.Warning, if (lastRisk != null && lastRisk >= 0.5) RedNeon else p.accent, riskStatus, riskStatusColor)
    )

    val metabolicStatus = when {
        lastPulse == null && lastTemp == null -> stringResource(R.string.dashboard_loading)
        (lastPulse ?: 0.0) > 120 || (lastTemp ?: 0.0) > 38.5 -> stringResource(R.string.dashboard_critical)
        (lastPulse ?: 0.0) > 100 || (lastTemp ?: 0.0) > 37.5 -> stringResource(R.string.dashboard_alert)
        else -> stringResource(R.string.dashboard_normal)
    }
    val metabolicColor = when (metabolicStatus) {
        stringResource(R.string.dashboard_critical) -> RedNeon
        stringResource(R.string.dashboard_alert) -> YellowNeon
        stringResource(R.string.dashboard_elevated_m) -> YellowNeon
        else -> GreenNeon
    }

    val suggestion = when (metabolicStatus) {
        stringResource(R.string.dashboard_critical) -> stringResource(R.string.dashboard_sug_critical)
        stringResource(R.string.dashboard_alert) -> stringResource(R.string.dashboard_sug_alert)
        stringResource(R.string.dashboard_elevated_m) -> stringResource(R.string.dashboard_sug_elevated)
        stringResource(R.string.dashboard_low_m) -> stringResource(R.string.dashboard_sug_low)
        stringResource(R.string.dashboard_normal) -> stringResource(R.string.dashboard_sug_normal)
        else -> stringResource(R.string.dashboard_sug_loading)
    }

    val historyItems = uiState.lecturasRecientes.map { lectura ->
        HistoryItem(
            time = lectura.timestamp.substringAfter("T", "").substringBefore("."),
            pulse = "${lectura.pulsoBpm.toInt()} BPM",
            temp = "${lectura.temperaturaC}\u00b0C",
            status = if ((lectura.probabilidadPico ?: 0.0) < 0.5) stringResource(R.string.dashboard_history_normal) else stringResource(R.string.dashboard_history_elevated)
        )
    }

    val sortedReadings = uiState.lecturasRecientes.reversed()

    val pulseChartPoints = sortedReadings.map {
        val tsMs = runCatching { java.time.Instant.parse(it.timestamp).toEpochMilli() }.getOrDefault(0L)
        val (labelStr, timeStr) = try {
            val instant = java.time.Instant.parse(it.timestamp)
            val zdt = instant.atZone(java.time.ZoneId.systemDefault())
            val lbl = zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val full = zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            lbl to full
        } catch (_: Exception) {
            it.timestamp.substringAfter("T", "").take(5) to it.timestamp.substringAfter("T", "").take(8)
        }
        ChartPoint(
            label = labelStr,
            value = it.pulsoBpm.toFloat(),
            time = timeStr,
            timestampMs = tsMs
        )
    }

    val tempChartPoints = sortedReadings.map {
        val tsMs = runCatching { java.time.Instant.parse(it.timestamp).toEpochMilli() }.getOrDefault(0L)
        val (labelStr, timeStr) = try {
            val instant = java.time.Instant.parse(it.timestamp)
            val zdt = instant.atZone(java.time.ZoneId.systemDefault())
            val lbl = zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val full = zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            lbl to full
        } catch (_: Exception) {
            it.timestamp.substringAfter("T", "").take(5) to it.timestamp.substringAfter("T", "").take(8)
        }
        ChartPoint(
            label = labelStr,
            value = it.temperaturaC.toFloat(),
            time = timeStr,
            timestampMs = tsMs
        )
    }

    val gsrChartPoints = sortedReadings.map {
        val tsMs = runCatching { java.time.Instant.parse(it.timestamp).toEpochMilli() }.getOrDefault(0L)
        val (labelStr, timeStr) = try {
            val instant = java.time.Instant.parse(it.timestamp)
            val zdt = instant.atZone(java.time.ZoneId.systemDefault())
            val lbl = zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val full = zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            lbl to full
        } catch (_: Exception) {
            it.timestamp.substringAfter("T", "").take(5) to it.timestamp.substringAfter("T", "").take(8)
        }
        ChartPoint(
            label = labelStr,
            value = it.sudoracionGsr.toFloat(),
            time = timeStr,
            timestampMs = tsMs
        )
    }

    val glucoseChartPoints = sortedReadings.map {
        val tsMs = runCatching { java.time.Instant.parse(it.timestamp).toEpochMilli() }.getOrDefault(0L)
        val (labelStr, timeStr) = try {
            val instant = java.time.Instant.parse(it.timestamp)
            val zdt = instant.atZone(java.time.ZoneId.systemDefault())
            val lbl = zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val full = zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            lbl to full
        } catch (_: Exception) {
            it.timestamp.substringAfter("T", "").take(5) to it.timestamp.substringAfter("T", "").take(8)
        }
        val gVal = it.glucosaEstimadaMgDl ?: 0.0
        val calculatedGlucose = if (gVal > 0.0) {
            gVal
        } else {
            (95.0 + (it.pulsoBpm - 72.0) * 0.45 + (it.temperaturaC - 36.5) * 12.0 + kotlin.math.max(0.0, it.sudoracionGsr - 45.0) * 0.5).coerceIn(70.0, 220.0)
        }
        ChartPoint(
            label = labelStr,
            value = calculatedGlucose.toFloat(),
            time = timeStr,
            timestampMs = tsMs
        )
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(p.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = p.accent)
        }
    } else if (uiState.error != null && uiState.lecturasRecientes.isEmpty()) {
        ErrorRetryBox(
            message = uiState.error,
            onRetry = { dashboardViewModel.loadDashboard() }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(p.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.accent,
                            letterSpacing = 4.sp
                        )
                        Text(
                            text = stringResource(R.string.dashboard_live_monitoring),
                            fontSize = 10.sp,
                            color = p.textSecondary,
                            letterSpacing = 2.sp
                        )
                    }

                    val lastSyncMillis = ultimaLectura?.timestamp?.let { timestamp ->
                        runCatching { java.time.Instant.parse(timestamp).toEpochMilli() }.getOrNull()
                    } ?: 0L

                    val unifiedState = when {
                        uiState.connectionState == com.bioguard.movil.service.WearableConnectionState.STREAMING || isRecentData -> com.bioguard.movil.service.WearableConnectionState.STREAMING
                        uiState.connectionState == com.bioguard.movil.service.WearableConnectionState.PAIRED || uiState.connectionState == com.bioguard.movil.service.WearableConnectionState.CONNECTED -> com.bioguard.movil.service.WearableConnectionState.PAIRED
                        uiState.connectionState == com.bioguard.movil.service.WearableConnectionState.SYNCHRONIZED || ultimaLectura != null -> com.bioguard.movil.service.WearableConnectionState.SYNCHRONIZED
                        uiState.isLoading -> com.bioguard.movil.service.WearableConnectionState.SEARCHING
                        else -> com.bioguard.movil.service.WearableConnectionState.DISCONNECTED
                    }

                    val statusText = unifiedState.toDisplayString(lastSyncMillis)
                    val statusColor = when (unifiedState) {
                        com.bioguard.movil.service.WearableConnectionState.STREAMING -> GreenNeon
                        com.bioguard.movil.service.WearableConnectionState.SYNCHRONIZED -> YellowNeon
                        com.bioguard.movil.service.WearableConnectionState.PAIRED -> Color(0xFF38BDF8)
                        com.bioguard.movil.service.WearableConnectionState.SEARCHING -> YellowNeon
                        com.bioguard.movil.service.WearableConnectionState.DISCONNECTED -> RedNeon
                        else -> p.textSecondary
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(p.surface)
                                .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.performClick()
                                    dashboardViewModel.loadDashboard()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = statusText, fontSize = 9.sp, color = statusColor, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Interactive Glucose Spike Vector Chart
                BioHealthChart(
                    points = glucoseChartPoints,
                    lineColor = Color(0xFFF43F5E),
                    unit = "mg/dL",
                    title = "Tendencia y Picos de Glucosa Estimados",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Interactive Heart Rate Vector Chart
                BioHealthChart(
                    points = pulseChartPoints,
                    lineColor = p.accent,
                    unit = "BPM",
                    title = "Ritmo Cardíaco en Tiempo Real",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = stringResource(R.string.dashboard_metabolic_state), fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = metabolicStatus, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = metabolicColor, letterSpacing = 2.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(metabolicColor.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (metabolicStatus == stringResource(R.string.dashboard_critical)) Icons.Filled.Emergency else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = metabolicColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                vitalSigns.forEach { vital ->
                    VitalSignCard(
                        vital = vital,
                        pulseAlpha = pulseAlpha,
                        onHelpClick = {
                            haptic.performClick()
                            showHelpForVital = vital
                        },
                        onClick = {
                            haptic.performClick()
                            val points = when (vital.unit) {
                                "BPM" -> pulseChartPoints
                                "\u00b0C" -> tempChartPoints
                                "mg/dL", "probabilidad" -> glucoseChartPoints
                                else -> gsrChartPoints
                            }
                            activeVitalDetail = VitalDetailData(
                                title = vital.name,
                                value = vital.value,
                                status = vital.status,
                                unit = vital.unit,
                                points = points
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.dashboard_dynamic_suggestion),
                    fontSize = 11.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (metabolicStatus == stringResource(R.string.dashboard_critical)) Icons.Filled.Emergency else if (metabolicStatus == stringResource(R.string.dashboard_alert) || metabolicStatus == stringResource(R.string.dashboard_elevated_m)) Icons.Filled.Warning else Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = if (metabolicStatus == stringResource(R.string.dashboard_critical)) RedNeon else if (metabolicStatus == stringResource(R.string.dashboard_alert) || metabolicStatus == stringResource(R.string.dashboard_elevated_m)) YellowNeon else p.accent,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp, end = 8.dp)
                        )
                        Text(
                            text = suggestion,
                            fontSize = 13.sp,
                            color = p.textSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enviar Reporte Glucémico Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.accent.copy(alpha = 0.15f))
                        .border(width = 1.dp, color = p.accent, shape = RoundedCornerShape(12.dp))
                        .clickable(enabled = !uiState.isLoading) {
                            haptic.performClick()
                            dashboardViewModel.generarReporteGlucemico()
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = "Reporte Glucémico",
                                tint = p.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Reporte Glucémico",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.accent,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (uiState.isLoading) "Procesando..." else "Enviar análisis ML al backend",
                                    fontSize = 11.sp,
                                    color = p.textSecondary
                                )
                            }
                        }
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = p.accent,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = p.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón de Sincronización Manual (si hay datos pendientes)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(YellowNeon.copy(alpha = 0.1f))
                        .border(width = 1.dp, color = YellowNeon, shape = RoundedCornerShape(12.dp))
                        .clickable(enabled = !uiState.isLoading) {
                            haptic.performClick()
                            dashboardViewModel.sincronizarManualmente()
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "Sincronizar Datos",
                                tint = YellowNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Sincronizar Datos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YellowNeon,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Enviar reportes pendientes manualmente",
                                    fontSize = 11.sp,
                                    color = p.textSecondary
                                )
                            }
                        }
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = YellowNeon,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                tint = YellowNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RedNeon.copy(alpha = 0.1f))
                            .border(width = 1.dp, color = RedNeon, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = uiState.error!!,
                            fontSize = 11.sp,
                            color = RedNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.dashboard_recent_history),
                    fontSize = 11.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (historyItems.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historyItems) { item ->
                            HistoryCard(item = item)
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.dashboard_no_recent_data),
                        fontSize = 12.sp,
                        color = p.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    activeVitalDetail?.let { detail ->
        VitalDetailBottomSheet(
            title = detail.title,
            currentValue = detail.value,
            statusText = detail.status,
            unit = detail.unit,
            chartPoints = detail.points,
            onDismiss = { activeVitalDetail = null }
        )
    }

    showHelpForVital?.let { vital ->
        VitalInfoDialog(
            vitalName = vital.name,
            valueStr = vital.value,
            unit = vital.unit,
            onDismiss = { showHelpForVital = null }
        )
    }
}

@Composable
fun VitalInfoDialog(
    vitalName: String,
    valueStr: String,
    unit: String,
    onDismiss: () -> Unit
) {
    val p = LocalThemeState.current.colorPalette()
    val valDouble = valueStr.toDoubleOrNull()

    val (titleStatus, bodyText) = when {
        vitalName.contains("Temperatura", ignoreCase = true) || unit == "°C" -> {
            when {
                valDouble == null -> "Información de Temperatura" to "Sin lecturas suficientes para evaluar la temperatura corporal."
                valDouble in 36.0..37.5 -> "Normal (36.0°C - 37.5°C)" to "Una temperatura corporal de ${valueStr}°C se considera normal y no indica fiebre. La temperatura fisiológica saludable oscila entre 36.0 y 37.5°C. Si no presentas otros síntomas, el valor es totalmente seguro."
                valDouble in 37.6..38.5 -> "Elevada / Febrícula (37.6°C - 38.5°C)" to "Una temperatura de ${valueStr}°C está ligeramente por encima de lo habitual (febrícula). Procura mantenerte hidratado y en reposo."
                valDouble > 38.5 -> "Fiebre Alta (> 38.5°C)" to "Una temperatura de ${valueStr}°C indica fiebre alta. Se sugiere reposo, hidratación constante y consultar a tu médico o red de cuidadores."
                else -> "Temperatura Baja (< 36.0°C)" to "Una temperatura de ${valueStr}°C se encuentra por debajo de 36.0°C. Procura mantener un ambiente cálido y abrigarte adecuadamente."
            }
        }
        vitalName.contains("Cardíaco", ignoreCase = true) || vitalName.contains("Pulso", ignoreCase = true) || unit == "BPM" -> {
            when {
                valDouble == null -> "Información de Ritmo Cardíaco" to "Sin lecturas suficientes para evaluar el pulso en reposo."
                valDouble in 60.0..100.0 -> "Normal (60 - 100 BPM)" to "Un ritmo cardíaco en reposo de ${valueStr} BPM está dentro del rango óptimo y saludable (60 a 100 latidos por minuto). Refleja un adecuado desempeño cardiovascular."
                valDouble > 100.0 -> "Elevado / Taquicardia (> 100 BPM)" to "Un pulso de ${valueStr} BPM está por encima del rango promedio en reposo. Puede responder a ejercicio reciente, estrés, deshidratación o consumo de café."
                else -> "Pulso Bajo / Bradicardia (< 60 BPM)" to "Un pulso de ${valueStr} BPM se encuentra por debajo de 60 latidos por minuto. Es común en personas deportistas; en reposo prolongado vigila mareos."
            }
        }
        vitalName.contains("Oxígeno", ignoreCase = true) || unit == "%" -> {
            "Saturación de Oxígeno (SpO2: 95% - 100%)" to "Una saturación de oxígeno en sangre de ${valueStr}% refleja una adecuada oxigenación arterial y excelente función respiratoria. Valores superiores al 95% se consideran completamente sanos."
        }
        vitalName.contains("Pasos", ignoreCase = true) || unit == "pasos" -> {
            "Conteo de Pasos y Actividad Física" to "Se han registrado ${valueStr} pasos durante el día gracias al sensor de acelerometría y movimiento del reloj inteligente. Mantenerse por encima de 5,000 a 8,000 pasos diarios promueve la salud metabólica."
        }
        vitalName.contains("Sueño", ignoreCase = true) -> {
            "Monitoreo Nocturno del Sueño" to "Análisis procesado por el smartwatch que clasifica el descanso en sueño ligero, profundo, REM y vigilia. Ayuda a evaluar la calidad de recuperación celular."
        }
        vitalName.contains("ECG", ignoreCase = true) -> {
            "Electrocardiograma (Derivación Única)" to "Trazado eléctrico capturado a través de los electrodos capacitivos del botón lateral para detectar signos de arritmia o fibrilación auricular."
        }
        else -> {
            when {
                valDouble == null -> "Información de Conductividad (GSR)" to "Sin lecturas suficientes para evaluar la respuesta galvánica de la piel."
                valDouble <= 50.0 -> "Normal / Estable (0 - 50 µS)" to "Una conductividad galvánica de la piel de ${valueStr} µS indica niveles normales de sudoración y estabilidad en el sistema nervioso simpático (bajo nivel de estrés)."
                else -> "Elevada (> 50 µS)" to "Una respuesta galvánica de ${valueStr} µS refleja mayor actividad sudorípara. Suele vincularse a picos de estrés, estimulación emocional o esfuerzo físico."
            }
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = p.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = titleStatus, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
            }
        },
        text = {
            Text(text = bodyText, fontSize = 13.sp, color = p.textSecondary, lineHeight = 20.sp)
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(text = "Entendido", color = p.background, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = p.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun VitalSignCard(
    vital: VitalSign,
    pulseAlpha: Float,
    onHelpClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = vital.icon,
                    contentDescription = null,
                    tint = vital.color.copy(alpha = pulseAlpha),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = vital.name, fontSize = 11.sp, color = p.textSecondary, letterSpacing = 2.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = vital.value,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = vital.color,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = vital.unit,
                            fontSize = 13.sp,
                            color = p.textSecondary,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(p.inputBackground)
                        .clickable { onHelpClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "?", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(vital.statusColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = vital.status, fontSize = 9.sp, color = vital.statusColor, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = item.time, fontSize = 13.sp, color = p.accent, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = p.accent,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = item.pulse, fontSize = 11.sp, color = p.textPrimary)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.DeviceThermostat,
                    contentDescription = null,
                    tint = p.accentSecondary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = item.temp, fontSize = 11.sp, color = p.textPrimary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(GreenNeon.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = item.status, fontSize = 8.sp, color = GreenNeon)
            }
        }
    }
}
