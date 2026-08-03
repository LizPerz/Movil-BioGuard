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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.colorPalette
import com.example.bioguard_movil.ui.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel,
    onNavigateToHistory: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by reportsViewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(p.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = p.accent)
        }
    } else if (uiState.error != null && uiState.reporte == null) {
        ErrorRetryBox(
            message = uiState.error,
            onRetry = { reportsViewModel.refresh() }
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
                    text = "REPORTES E HISTORIAL",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Incidentes y descargas",
                    fontSize = 13.sp,
                    color = p.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                uiState.reporte?.let { reporte ->
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "RESUMEN GENERAL", fontSize = 12.sp, color = p.textPrimary, fontWeight = FontWeight.Bold)
                                    Text(text = "Datos consolidados", fontSize = 11.sp, color = p.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportStatItem("LECTURAS", "${reporte.totalLecturas}", p.accent)
                                ReportStatItem("EVENTOS", "${reporte.totalEventos}", YellowNeon)
                                ReportStatItem("ALERTAS", "${reporte.totalAlertas}", RedNeon)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportStatItem("MEDICAMENTOS", "${reporte.totalMedicamentos}", GreenNeon)
                                ReportStatItem("CR\u00cdTICOS", "${reporte.eventosCriticos}", RedNeon)
                                ReportStatItem("PENDIENTES", "${reporte.alertasPendientes}", YellowNeon)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "HISTORIAL DE EVENTOS",
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (uiState.eventos.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "FECHA", fontSize = 9.sp, color = p.textSecondary, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                Text(text = "RIESGO", fontSize = 9.sp, color = p.textSecondary, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                Text(text = "PROB.", fontSize = 9.sp, color = p.textSecondary, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                Text(text = "ESTADO", fontSize = 9.sp, color = p.textSecondary, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                            }

                            uiState.eventos.forEach { evento ->
                                val intensityColor = when (evento.nivelRiesgo) {
                                    "Alta", "Critico" -> RedNeon
                                    "Media" -> YellowNeon
                                    else -> GreenNeon
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = (evento.fechaEvento ?: "").substringBefore("T"), fontSize = 11.sp, color = p.textPrimary, modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(intensityColor.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .weight(1f)
                                    ) {
                                        Text(text = evento.nivelRiesgo ?: "-", fontSize = 9.sp, color = intensityColor, fontWeight = FontWeight.Bold)
                                    }
                                    Text(text = "%.0f%%".format(evento.probabilidadMl * 100), fontSize = 11.sp, color = p.textPrimary, modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (evento.atendida) GreenNeon.copy(alpha = 0.1f) else YellowNeon.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .weight(1f)
                                    ) {
                                        Text(text = if (evento.atendida) "Atendido" else "Pendiente", fontSize = 9.sp, color = if (evento.atendida) GreenNeon else YellowNeon, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(p.border.copy(alpha = 0.3f)))
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Sin eventos registrados", fontSize = 13.sp, color = p.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Text(
                        text = "VER HISTORIAL COMPLETO",
                        color = p.background,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ReportStatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    val p = LocalThemeState.current.colorPalette()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(p.background).padding(8.dp)
    ) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 8.sp, color = p.textSecondary, letterSpacing = 1.sp)
    }
}
