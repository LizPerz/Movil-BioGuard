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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.colorPalette

data class MetricOption(
    val name: String,
    val color: Color,
    val icon: String
)

@Composable
fun AnalysisScreen() {
    val p = LocalThemeState.current.colorPalette()

    var selectedMetric by remember { mutableStateOf("Pulso") }
    var selectedTimeFilter by remember { mutableStateOf("Hoy") }

    val timeFilters = listOf("1h", "4h", "Hoy", "7 días")
    val metrics = listOf(
        MetricOption("Pulso", p.accent, "♥"),
        MetricOption("Temperatura", p.accentSecondary, "🌡"),
        MetricOption("Conductividad", YellowNeon, "⚡"),
        MetricOption("Glucosa", RedNeon, "💉"),
        MetricOption("SpO2", GreenNeon, "💧")
    )

    val chartData = when (selectedMetric) {
        "Pulso" -> listOf(72f, 75f, 78f, 74f, 80f, 76f, 73f, 77f, 75f, 79f, 72f, 78f)
        "Temperatura" -> listOf(36.2f, 36.4f, 36.5f, 36.3f, 36.6f, 36.4f, 36.3f, 36.5f, 36.4f, 36.6f, 36.2f, 36.5f)
        "Conductividad" -> listOf(1.0f, 1.2f, 1.1f, 1.3f, 1.2f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.2f, 1.1f)
        "Glucosa" -> listOf(95f, 100f, 110f, 105f, 98f, 102f, 108f, 112f, 106f, 99f, 97f, 103f)
        else -> listOf(96f, 97f, 98f, 96f, 97f, 98f, 99f, 97f, 96f, 98f, 97f, 96f)
    }

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
                text = "ANÁLISIS DE DATOS",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = p.accent,
                letterSpacing = 4.sp
            )
            Text(
                text = "Historial metabólico interactivo",
                fontSize = 13.sp,
                color = p.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeFilters.forEach { filter ->
                    val isSelected = selectedTimeFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) p.accent else p.surface)
                            .border(width = 1.dp, color = if (isSelected) p.accent else p.border, shape = RoundedCornerShape(8.dp))
                            .clickable { selectedTimeFilter = filter },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = filter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) p.background else p.textSecondary, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "MÉTRICAS",
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
                    val isSelected = selectedMetric == metric.name
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) metric.color.copy(alpha = 0.15f) else p.surface)
                            .border(width = 1.dp, color = if (isSelected) metric.color else p.border, shape = RoundedCornerShape(8.dp))
                            .clickable { selectedMetric = metric.name },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = metric.icon, fontSize = 18.sp)
                    }
                }
            }

            val chartColor = metrics.find { it.name == selectedMetric }?.color ?: p.accent

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
                        Text(text = selectedMetric.uppercase(), fontSize = 12.sp, color = p.accent, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Text(text = selectedTimeFilter, fontSize = 11.sp, color = p.textSecondary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxVal = chartData.maxOf { it }
                    val minVal = chartData.minOf { it }
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
                    val current = chartData.last()
                    val trend = if (current > avg) "↑" else if (current < avg) "↓" else "→"
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
