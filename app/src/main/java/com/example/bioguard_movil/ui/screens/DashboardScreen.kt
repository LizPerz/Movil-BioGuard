package com.example.bioguard_movil.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

data class VitalSign(
    val name: String,
    val value: String,
    val unit: String,
    val icon: String,
    val color: Color,
    val status: String,
    val statusColor: Color
)

data class HistoryItem(
    val time: String,
    val pulse: String,
    val temp: String,
    val status: String
)

@Composable
fun DashboardScreen() {
    val p = LocalThemeState.current.colorPalette()
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

    val vitalSigns = listOf(
        VitalSign("RITMO CARDÍACO", "78", "BPM", "♥", p.accent, "NORMAL", GreenNeon),
        VitalSign("TEMPERATURA", "36.5", "°C", "🌡", p.accentSecondary, "NORMAL", GreenNeon),
        VitalSign("CONDUCTIVIDAD", "1.2", "µS", "⚡", YellowNeon, "ELEVADA", YellowNeon)
    )

    val metabolicStatus = "ESTABLE"
    val metabolicColor = GreenNeon
    val suggestion = when (metabolicStatus) {
        "CRÍTICO" -> "⚠ Atención médica inmediata requerida. Consulta a tu especialista."
        "ALERTA" -> "⚠ Niveles metabólicos alterados. Revisa tu alimentación e hidratación."
        "ELEVADO" -> "📈 Monitoreo continuo recomendado. Evita esfuerzos intensos."
        "BAJO" -> "📉 Se recomienda ingesta de carbohidratos y descanso."
        else -> "✅ Estado metabólico óptimo. Mantén tu rutina saludable."
    }

    val historyItems = listOf(
        HistoryItem("14:30", "75 BPM", "36.4°C", "Normal"),
        HistoryItem("14:00", "72 BPM", "36.3°C", "Normal"),
        HistoryItem("13:30", "80 BPM", "36.6°C", "Normal"),
        HistoryItem("13:00", "78 BPM", "36.5°C", "Normal"),
        HistoryItem("12:30", "82 BPM", "36.7°C", "Normal")
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BIOGUARD",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.accent,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "MONITOREO EN VIVO",
                        fontSize = 10.sp,
                        color = p.textSecondary,
                        letterSpacing = 2.sp
                    )
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
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(7.dp).clip(CircleShape).background(GreenNeon)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "ONLINE", fontSize = 9.sp, color = GreenNeon, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

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
                        Text(text = "ESTADO METABÓLICO", fontSize = 10.sp, color = p.textSecondary, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = metabolicStatus, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = metabolicColor, letterSpacing = 2.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(metabolicColor.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = if (metabolicStatus == "CRÍTICO") "⚠" else "✓", fontSize = 18.sp, color = metabolicColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            vitalSigns.forEach { vital ->
                VitalSignCard(vital = vital, pulseAlpha = pulseAlpha)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SUGERENCIA DINÁMICA",
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
                    Text(
                        text = if (metabolicStatus == "CRÍTICO") "🚨" else if (metabolicStatus == "ALERTA" || metabolicStatus == "ELEVADO") "⚠" else "💡",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
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

            Text(
                text = "HISTORIAL RECIENTE",
                fontSize = 11.sp,
                color = p.accent,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyItems) { item ->
                    HistoryCard(item = item)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun VitalSignCard(vital: VitalSign, pulseAlpha: Float) {
    val p = LocalThemeState.current.colorPalette()
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = vital.icon, fontSize = 24.sp, color = vital.color.copy(alpha = pulseAlpha))
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
                Text(text = "♥", fontSize = 11.sp, color = p.accent)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = item.pulse, fontSize = 11.sp, color = p.textPrimary)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌡", fontSize = 11.sp, color = p.accentSecondary)
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
