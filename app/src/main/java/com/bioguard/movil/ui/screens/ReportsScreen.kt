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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.ui.components.ErrorRetryBox
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.YellowNeon
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.ReportsViewModel
import androidx.compose.ui.res.stringResource
import com.bioguard.movil.R

@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel,
    canReadHistory: Boolean = false,
    onNavigateToHistory: () -> Unit = {},
    userName: String? = null
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by reportsViewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(canReadHistory) {
        reportsViewModel.loadReportes(canReadHistory)
    }

    fun generarYCompartirPdf() {
        val reporte = uiState.reporte ?: return
        val file = com.bioguard.movil.util.ReportPdfGenerator.generate(
            context = context,
            reporte = reporte,
            eventos = uiState.eventos,
            lecturas = uiState.lecturas,
            pacienteNombre = userName ?: "Paciente",
            pacienteId = uiState.pacienteId
        )
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "BioGuard - Reporte de salud")
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "Reporte de salud generado por BioGuard. Abre el PDF adjunto o guardalo para imprimirlo."
            )
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            android.content.Intent.createChooser(shareIntent, "Compartir reporte (PDF)")
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
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.reports_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 4.sp
                )
                Text(
                    text = stringResource(R.string.reports_desc),
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
                                    Text(text = stringResource(R.string.reports_summary), fontSize = 12.sp, color = p.textPrimary, fontWeight = FontWeight.Bold)
                                    Text(text = stringResource(R.string.reports_summary_desc), fontSize = 11.sp, color = p.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReportStatItem(stringResource(R.string.reports_lecturas), "${reporte.totalLecturas}", p.accent, modifier = Modifier.weight(1f))
                                ReportStatItem(stringResource(R.string.reports_eventos), "${reporte.totalEventos}", YellowNeon, modifier = Modifier.weight(1f))
                                ReportStatItem(stringResource(R.string.reports_alertas), "${reporte.totalAlertas}", RedNeon, modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReportStatItem(stringResource(R.string.reports_criticos), "${reporte.eventosCriticos}", RedNeon, modifier = Modifier.weight(1f))
                                ReportStatItem(stringResource(R.string.reports_pendientes), "${reporte.alertasPendientes}", YellowNeon, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.reporte != null) {
                    Button(
                        onClick = { generarYCompartirPdf() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedNeon)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DESCARGAR / COMPARTIR PDF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (canReadHistory) Text(
                    text = stringResource(R.string.reports_history),
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (canReadHistory && uiState.eventos.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = stringResource(R.string.reports_date), fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
                                    Text(text = stringResource(R.string.reports_risk), fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                    Text(text = stringResource(R.string.reports_prob), fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                    Text(text = stringResource(R.string.reports_status), fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                }
                            }

                            items(uiState.eventos) { evento ->
                                val intensityColor = when (evento.nivelRiesgo) {
                                    "Alta", "Critico" -> RedNeon
                                    "Media" -> YellowNeon
                                    else -> GreenNeon
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = (evento.fechaEvento ?: "").substringBefore("T"), fontSize = 11.sp, color = p.textPrimary, modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(intensityColor.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(text = evento.nivelRiesgo ?: "-", fontSize = 9.sp, color = intensityColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(text = "%.0f%%".format(evento.probabilidadMl * 100), fontSize = 11.sp, color = p.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (evento.atendida) GreenNeon.copy(alpha = 0.1f) else YellowNeon.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(text = if (evento.atendida) stringResource(R.string.reports_attended) else stringResource(R.string.reports_pending), fontSize = 9.sp, color = if (evento.atendida) GreenNeon else YellowNeon, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(p.border.copy(alpha = 0.3f)))
                            }
                        }
                    }
                } else if (canReadHistory) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.reports_empty), fontSize = 13.sp, color = p.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (canReadHistory) Button(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Text(
                        text = stringResource(R.string.reports_full_history),
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
fun ReportStatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val p = LocalThemeState.current.colorPalette()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(p.background)
            .padding(8.dp)
    ) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 8.sp, color = p.textSecondary, letterSpacing = 1.sp)
    }
}
