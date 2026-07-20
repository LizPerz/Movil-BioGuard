package com.example.bioguard_movil.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.DarkBackground
import com.example.bioguard_movil.ui.theme.DarkSurface
import com.example.bioguard_movil.ui.theme.GlassBackground
import com.example.bioguard_movil.ui.theme.GlassBorder
import com.example.bioguard_movil.ui.theme.RedDark
import com.example.bioguard_movil.ui.theme.RedLight
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.TextPrimary
import com.example.bioguard_movil.ui.theme.TextSecondary
import com.example.bioguard_movil.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertScreen(
    onDismiss: () -> Unit = {},
    onEmergencyCall: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alert")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border"
    )

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(RedNeon.copy(alpha = glowIntensity * 0.1f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(width = 3.dp, color = RedNeon.copy(alpha = borderAlpha), shape = RoundedCornerShape(20.dp))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        ambientColor = RedNeon.copy(alpha = pulseAlpha * 0.6f),
                        spotColor = RedNeon.copy(alpha = pulseAlpha)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(RedNeon.copy(alpha = 0.3f), RedDark.copy(alpha = 0.05f))
                        )
                    )
                    .border(width = 2.dp, color = RedNeon.copy(alpha = pulseAlpha), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚠", fontSize = 48.sp, color = RedNeon.copy(alpha = pulseAlpha))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "ALERTA DE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = RedNeon,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "RITMO CARDÍACO",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = RedNeon,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(width = 2.dp, color = RedNeon.copy(alpha = borderAlpha), shape = RoundedCornerShape(12.dp))
                    .padding(28.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "FRECUENCIA CARDÍACA", fontSize = 10.sp, color = RedLight, letterSpacing = 3.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "140",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedNeon,
                            lineHeight = 64.sp
                        )
                        Text(
                            text = "BPM",
                            fontSize = 18.sp,
                            color = RedLight,
                            modifier = Modifier.padding(bottom = 10.dp, start = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Nivel Crítico Detectado",
                        fontSize = 13.sp,
                        color = RedNeon.copy(alpha = pulseAlpha),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedNeon.copy(alpha = 0.08f))
                    .border(width = 1.dp, color = RedNeon.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Se ha detectado una frecuencia cardíaca anormalmente alta. Se recomienda atención médica inmediata.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onEmergencyCall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedNeon)
            ) {
                Text(
                    text = "_LLAMADA DE EMERGENCIA_",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(10.dp))
            ) {
                Text(text = "CANCELAR ALERTA", color = TextSecondary, letterSpacing = 2.sp, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Alerta generada: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}",
                fontSize = 10.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}
