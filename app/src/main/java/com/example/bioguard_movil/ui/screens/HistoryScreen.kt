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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.components.ErrorRetryBox
import com.example.bioguard_movil.ui.model.HistoryEntry
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.colorPalette
import com.example.bioguard_movil.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by historyViewModel.uiState.collectAsState()
    val p = LocalThemeState.current.colorPalette()
    val filters = listOf("1h", "4h", "Hoy", "7 d\u00edas")

    val historyEntries = uiState.lecturas.mapIndexed { index, lectura ->
        val status = when {
            lectura.pulsoBpm > 100 -> "Alerta"
            lectura.pulsoBpm > 85 -> "Elevado"
            else -> "Normal"
        }
        val statusColor = when (status) {
            "Alerta" -> RedNeon
            "Elevado" -> YellowNeon
            else -> GreenNeon
        }
        HistoryEntry(
            date = lectura.timestamp.take(10),
            time = lectura.timestamp.drop(11).take(5),
            pulse = "${lectura.pulsoBpm.toInt()} BPM",
            temp = "%.1f\u00b0C".format(lectura.temperaturaC),
            gsr = "%.1f".format(lectura.sudoracionGsr),
            status = status,
            statusColor = statusColor
        )
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(p.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = p.accent)
        }
    } else if (uiState.error != null && uiState.lecturas.isEmpty()) {
        ErrorRetryBox(
            message = uiState.error,
            onRetry = { historyViewModel.loadHistory() }
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "\u2190",
                        color = p.accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onBack() }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HISTORIAL CL\u00cdNICO",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.accent,
                        letterSpacing = 3.sp
                    )
                }

                Text(
                    text = "Tendencia de signos vitales",
                    fontSize = 12.sp,
                    color = p.textSecondary,
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
                                    if (uiState.selectedFilter == filter) p.accent else p.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (uiState.selectedFilter == filter) p.accent else p.border,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { historyViewModel.selectFilter(filter) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (uiState.selectedFilter == filter) p.background else p.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "FRECUENCIA CARD\u00cdACA",
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val pulseValues = uiState.lecturas.map { it.pulsoBpm.toFloat() }
                val avgPulse = pulseValues.average().toInt().toString()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "PROMEDIO", fontSize = 10.sp, color = p.textSecondary, letterSpacing = 1.sp)
                            Text(text = "$avgPulse BPM", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = p.accent)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (pulseValues.isNotEmpty()) {
                            val maxVal = pulseValues.max()
                            val minVal = pulseValues.min()
                            val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

                            Row(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                pulseValues.forEach { value ->
                                    val heightFraction = if (range > 0) (value - minVal) / range else 0.5f
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height((heightFraction * 100).dp.coerceAtLeast(4.dp))
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(p.accent.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(text = "Sin datos disponibles", fontSize = 12.sp, color = p.textTertiary, modifier = Modifier.padding(vertical = 40.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "HISTORIAL DE EVENTOS",
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (uiState.eventos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(p.surface).border(1.dp, p.border, RoundedCornerShape(10.dp)).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No hay eventos registrados", fontSize = 13.sp, color = p.textTertiary)
                    }
                } else {
                    uiState.eventos.forEach { evento ->
                        val intensityColor = when (evento.nivelRiesgo) {
                            "Alta", "Critico" -> RedNeon
                            "Media" -> YellowNeon
                            else -> GreenNeon
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.surface)
                                .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${(evento.fechaEvento ?: "").substringBefore("T")}  ${(evento.fechaEvento ?: "").drop(11).take(5)}",
                                        fontSize = 12.sp,
                                        color = p.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = evento.descripcion ?: "Evento metabolico",
                                        fontSize = 11.sp,
                                        color = p.textSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(intensityColor.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${evento.nivelRiesgo ?: "-"} \u00b7 %.0f%%".format(evento.probabilidadMl * 100),
                                        fontSize = 9.sp,
                                        color = intensityColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "REGISTRO DE LECTURAS",
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (historyEntries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(p.surface).border(1.dp, p.border, RoundedCornerShape(10.dp)).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No hay lecturas disponibles", fontSize = 13.sp, color = p.textTertiary)
                    }
                } else {
                    historyEntries.forEach { entry ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.surface)
                                .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
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
                                        color = p.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(text = "\u2665 ${entry.pulse}", fontSize = 11.sp, color = p.accent)
                                        Text(text = "\uD83C\uDF21 ${entry.temp}", fontSize = 11.sp, color = p.textSecondary)
                                        Text(text = "\uD83C\uDF0A GSR ${entry.gsr}", fontSize = 11.sp, color = p.textSecondary)
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
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
