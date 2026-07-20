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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.colorPalette

data class IncidentItem(
    val date: String,
    val time: String,
    val duration: String,
    val intensity: String,
    val intensityColor: androidx.compose.ui.graphics.Color
)

@Composable
fun ReportsScreen() {
    val p = LocalThemeState.current.colorPalette()
    val incidents = listOf(
        IncidentItem("15/07/2026", "14:30", "2 min", "ALTA", RedNeon),
        IncidentItem("15/07/2026", "09:15", "1 min", "MEDIA", YellowNeon),
        IncidentItem("14/07/2026", "22:40", "3 min", "BAJA", GreenNeon),
        IncidentItem("14/07/2026", "16:00", "1 min", "MEDIA", YellowNeon),
        IncidentItem("13/07/2026", "11:20", "4 min", "ALTA", RedNeon)
    )

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
                        Text(text = "REPORTE DIARIO", fontSize = 12.sp, color = p.textPrimary, fontWeight = FontWeight.Bold)
                        Text(text = "Gratuito - disponible cada 24h", fontSize = 11.sp, color = p.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.accent.copy(alpha = 0.1f))
                            .border(width = 1.dp, color = p.accent, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .clickable { }
                    ) {
                        Text(text = "PDF", fontSize = 11.sp, color = p.accent, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        Text(text = "REPORTE PREMIUM", fontSize = 12.sp, color = p.textPrimary, fontWeight = FontWeight.Bold)
                        Text(text = "Ilimitado - análisis avanzado", fontSize = 11.sp, color = p.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔒", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(p.surface)
                                .border(width = 1.dp, color = YellowNeon, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .clickable { }
                        ) {
                            Text(text = "UPGRADE", fontSize = 11.sp, color = YellowNeon, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HISTORIAL DE INCIDENTES",
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
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "FECHA", fontSize = 9.sp, color = p.textSecondary, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                        Text(text = "HORA", fontSize = 9.sp, color = p.textSecondary, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                        Text(text = "DURACIÓN", fontSize = 9.sp, color = p.textSecondary, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                        Text(text = "INTENSIDAD", fontSize = 9.sp, color = p.textSecondary, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    }

                    incidents.forEach { incident ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = incident.date, fontSize = 11.sp, color = p.textPrimary, modifier = Modifier.weight(1f))
                            Text(text = incident.time, fontSize = 11.sp, color = p.textPrimary, modifier = Modifier.weight(1f))
                            Text(text = incident.duration, fontSize = 11.sp, color = p.textPrimary, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(incident.intensityColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .weight(1f)
                            ) {
                                Text(text = incident.intensity, fontSize = 9.sp, color = incident.intensityColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(p.border.copy(alpha = 0.3f)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
