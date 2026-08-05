package com.example.bioguard_movil.ui.screens

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.components.ErrorRetryBox
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.colorPalette
import com.example.bioguard_movil.ui.model.MetricOption
import com.example.bioguard_movil.ui.viewmodel.AnalysisViewModel

@Composable
fun AnalysisScreen(analysisViewModel: AnalysisViewModel) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by analysisViewModel.uiState.collectAsState()

    val timeFilters = listOf("1h", "4h", "Hoy", "7 d\u00edas")
    val metrics = listOf(
        MetricOption("Pulso", p.accent, "\u2665"),
        MetricOption("Temperatura", p.accentSecondary, "\uD83C\uDF21"),
        MetricOption("Conductividad", YellowNeon, "\u26a1")
    )

    val chartData = when (uiState.selectedMetric) {
        "Pulso" -> uiState.lecturas.map { it.pulsoBpm.toFloat() }
        "Temperatura" -> uiState.lecturas.map { it.temperaturaC.toFloat() }
        "Conductividad" -> uiState.lecturas.map { it.sudoracionGsr.toFloat() }
        else -> uiState.lecturas.map { it.pulsoBpm.toFloat() }
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
                    text = "AN\u00c1LISIS DE DATOS",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Historial metab\u00f3lico interactivo",
                    fontSize = 13.sp,
                    color = p.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

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
                                .clickable { analysisViewModel.selectTimeFilter(filter) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = filter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) p.background else p.textSecondary, letterSpacing = 1.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "M\u00c9TRICAS",
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
                                .clickable { analysisViewModel.selectMetric(metric.name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = metric.icon, fontSize = 18.sp)
                        }
                    }
                }

                val chartColor = metrics.find { it.name == uiState.selectedMetric }?.color ?: p.accent

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = uiState.selectedMetric.uppercase(), fontSize = 12.sp, color = p.accent, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                            Text(text = uiState.selectedTimeFilter, fontSize = 11.sp, color = p.textSecondary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val maxVal = chartData.maxOfOrNull { it } ?: 0f
                        val minVal = chartData.minOfOrNull { it } ?: 0f
                        val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                val yLabels = listOf(maxVal, (maxVal + minVal) / 2, minVal)
                                yLabels.forEach { label ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "%.0f".format(label), fontSize = 9.sp, color = p.textSecondary, modifier = Modifier.width(32.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(p.border.copy(alpha = 0.3f)))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxSize().padding(start = 36.dp, top = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                chartData.forEach { value ->
                                    val heightFraction = if (range > 0) (value - minVal) / range else 0.5f
                                    Box(
                                        modifier = Modifier
                                            .width(12.dp)
                                            .height((heightFraction * 140).dp.coerceAtLeast(4.dp))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(chartColor)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "RESUMEN",
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
                        val avg = chartData.average()
                        val current = chartData.lastOrNull() ?: 0f
                        val trend = if (current > avg) "\u2191" else if (current < avg) "\u2193" else "\u2192"
                        val trendColor = if (current > avg) RedNeon else if (current < avg) GreenNeon else p.textSecondary

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "PROMEDIO", fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp)
                                Text(text = "%.1f".format(avg), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "ACTUAL", fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp)
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
