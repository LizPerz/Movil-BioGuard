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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.example.bioguard_movil.ui.theme.CyanNeon
import com.example.bioguard_movil.ui.theme.DarkBackground
import com.example.bioguard_movil.ui.theme.DarkSurface
import com.example.bioguard_movil.ui.theme.GlassBorder
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.TextPrimary
import com.example.bioguard_movil.ui.theme.TextSecondary
import com.example.bioguard_movil.ui.theme.TextTertiary
import com.example.bioguard_movil.ui.theme.YellowNeon

data class HistoryEntry(
    val date: String,
    val time: String,
    val pulse: String,
    val temp: String,
    val spo2: String,
    val status: String,
    val statusColor: Color
)

data class ChartBar(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun HistoryScreen() {
    var selectedFilter by remember { mutableStateOf("Hoy") }
    val filters = listOf("1h", "4h", "Hoy", "7 días")

    val chartData = listOf(
        ChartBar("08:00", 0.6f, CyanNeon),
        ChartBar("09:00", 0.7f, CyanNeon),
        ChartBar("10:00", 0.5f, CyanNeon),
        ChartBar("11:00", 0.8f, YellowNeon),
        ChartBar("12:00", 0.65f, CyanNeon),
        ChartBar("13:00", 0.75f, CyanNeon),
        ChartBar("14:00", 0.9f, GreenNeon)
    )

    val historyEntries = listOf(
        HistoryEntry("Hoy", "14:30", "75 BPM", "36.4°C", "97%", "Normal", GreenNeon),
        HistoryEntry("Hoy", "12:00", "82 BPM", "36.7°C", "96%", "Normal", GreenNeon),
        HistoryEntry("Ayer", "18:00", "88 BPM", "36.8°C", "95%", "Elevado", YellowNeon),
        HistoryEntry("Ayer", "10:00", "72 BPM", "36.3°C", "98%", "Normal", GreenNeon),
        HistoryEntry("15 Jul", "16:00", "95 BPM", "37.0°C", "94%", "Alerta", YellowNeon),
        HistoryEntry("15 Jul", "09:00", "70 BPM", "36.2°C", "98%", "Normal", GreenNeon)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "HISTORIAL CLÍNICO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Tendencia de signos vitales",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedFilter == filter) CyanNeon else DarkSurface
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedFilter == filter) CyanNeon else GlassBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedFilter = filter },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedFilter == filter) DarkBackground else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "FRECUENCIA CARDÍACA",
                fontSize = 10.sp,
                color = CyanNeon,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "PROMEDIO", fontSize = 10.sp, color = TextSecondary, letterSpacing = 1.sp)
                        Text(text = "76 BPM", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartData.forEach { bar ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height((bar.value * 100).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(bar.color.copy(alpha = 0.7f))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = bar.label.takeLast(2), fontSize = 8.sp, color = TextTertiary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "HISTORIAL DE EVENTOS",
                fontSize = 10.sp,
                color = CyanNeon,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            historyEntries.forEach { entry ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${entry.date} - ${entry.time}",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = "♥ ${entry.pulse}", fontSize = 11.sp, color = CyanNeon)
                                Text(text = "🌡 ${entry.temp}", fontSize = 11.sp, color = TextSecondary)
                                Text(text = "💧 ${entry.spo2}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(entry.statusColor.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = entry.status, fontSize = 9.sp, color = entry.statusColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
