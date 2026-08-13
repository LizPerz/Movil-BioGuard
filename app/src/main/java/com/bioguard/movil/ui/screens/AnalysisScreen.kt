package com.bioguard.movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.bioguard.movil.ui.model.MetricOption
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.YellowNeon
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.AnalysisViewModel
import com.bioguard.movil.util.rememberBioHaptic

@Composable
fun AnalysisScreen(analysisViewModel: AnalysisViewModel) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by analysisViewModel.uiState.collectAsState()
    val haptic = rememberBioHaptic()

    val timeFilters = listOf("1h", "4h", "Hoy", "7 d\u00edas")
    val metrics = listOf(
        MetricOption("Pulso", p.accent, Icons.Filled.Favorite),
        MetricOption("Temperatura", p.accentSecondary, Icons.Filled.DeviceThermostat),
        MetricOption("Conductividad", YellowNeon, Icons.Filled.Bolt)
    )
    val metricEmojis = mapOf("Pulso" to "\u2764\uFE0F", "Temperatura" to "\uD83C\uDF21\uFE0F", "Conductividad" to "\u26A1")

    val chartPoints = uiState.lecturas.map { it ->
        val valFloat = when (uiState.selectedMetric) {
            "Pulso" -> it.pulsoBpm.toFloat()
            "Temperatura" -> it.temperaturaC.toFloat()
            "Conductividad" -> it.sudoracionGsr.toFloat()
            else -> it.pulsoBpm.toFloat()
        }
        ChartPoint(
            label = it.timestamp.substringAfter("T", "").take(5),
            value = valFloat,
            time = it.timestamp.substringAfter("T", "").take(8)
        )
    }

    val chartColor = metrics.find { it.name == uiState.selectedMetric }?.color ?: p.accent
    val unit = when (uiState.selectedMetric) {
        "Pulso" -> "BPM"
        "Temperatura" -> "\u00b0C"
        else -> "\u00b5S"
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
    } else if (uiState.error != null && uiState.lecturas.isEmpty()) {
        ErrorRetryBox(
            message = uiState.error,
            onRetry = { analysisViewModel.loadLecturas() }
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
                Text(
                    text = stringResource(R.string.analysis_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 4.sp
                )
                Text(
                    text = stringResource(R.string.analysis_desc),
                    fontSize = 13.sp,
                    color = p.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.lecturas.isNotEmpty()) GreenNeon else p.border)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.lecturas.isNotEmpty()) "EN VIVO \u00B7 actualizado con el reloj" else "SIN DATOS DEL RELOJ A\u00DAN",
                            fontSize = 10.sp,
                            color = if (uiState.lecturas.isNotEmpty()) GreenNeon else p.textSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performSelection()
                                analysisViewModel.loadLecturas()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "\uD83D\uDD04", fontSize = 14.sp)
                    }
                }

                if (uiState.lecturas.isEmpty() && uiState.error == null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "\uD83D\uDC5F", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Conecta tu reloj BioGuard",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Las lecturas aparecer\u00E1n aqu\u00ED en tiempo real. Toca \uD83D\uDD04 para recargar.",
                                fontSize = 12.sp,
                                color = p.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeFilters.forEach { filter ->
                        val isSelected = uiState.selectedTimeFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) p.accent else p.surface)
                                .border(width = 1.dp, color = if (isSelected) p.accent else p.border, shape = RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performSelection()
                                    analysisViewModel.selectTimeFilter(filter)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = filter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) p.background else p.textSecondary, letterSpacing = 1.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.analysis_metrics),
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metrics.forEach { metric ->
                        val isSelected = uiState.selectedMetric == metric.name
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) metric.color.copy(alpha = 0.15f) else p.surface)
                                .border(width = 1.dp, color = if (isSelected) metric.color else p.border, shape = RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performSelection()
                                    analysisViewModel.selectMetric(metric.name)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = metric.icon,
                                    contentDescription = metric.name,
                                    tint = metric.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = metricEmojis[metric.name] ?: "", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BioHealthChart Vector Canvas Render
                BioHealthChart(
                    points = chartPoints,
                    lineColor = chartColor,
                    unit = unit,
                    title = "${metricEmojis[uiState.selectedMetric] ?: ""} Tendencia (${uiState.selectedMetric.uppercase()})"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.analysis_summary),
                    fontSize = 10.sp,
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
                        .padding(16.dp)
                ) {
                    Column {
                        val rawValues = chartPoints.map { it.value }
                        val avg = if (rawValues.isNotEmpty()) rawValues.average() else 0.0
                        val current = rawValues.lastOrNull() ?: 0f
                        val trend = if (current > avg) "\u2191" else if (current < avg) "\u2193" else "\u2192"
                        val trendColor = if (current > avg) RedNeon else if (current < avg) GreenNeon else p.textSecondary

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = stringResource(R.string.analysis_average), fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp)
                                Text(text = "%.1f".format(avg), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = stringResource(R.string.analysis_current), fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "%.1f".format(current), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = chartColor)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = trend, fontSize = 20.sp, color = trendColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
