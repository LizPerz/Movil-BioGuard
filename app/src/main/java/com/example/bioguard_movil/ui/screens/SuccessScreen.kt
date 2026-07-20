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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import com.example.bioguard_movil.ui.theme.CyanNeon
import com.example.bioguard_movil.ui.theme.DarkBackground
import com.example.bioguard_movil.ui.theme.DarkSurface
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.GlassBackground
import com.example.bioguard_movil.ui.theme.GlassBorder
import com.example.bioguard_movil.ui.theme.TextPrimary
import com.example.bioguard_movil.ui.theme.TextSecondary
import com.example.bioguard_movil.ui.theme.YellowNeon

@Composable
fun SuccessScreen(
    onContinue: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
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
                        colors = listOf(GreenNeon.copy(alpha = glowAlpha * 0.08f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        ambientColor = GreenNeon.copy(alpha = 0.4f),
                        spotColor = GreenNeon.copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(GreenNeon.copy(alpha = 0.2f), DarkSurface)
                        )
                    )
                    .border(width = 2.dp, color = GreenNeon, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✓", fontSize = 64.sp, color = GreenNeon, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "SINCRONIZACIÓN COMPLETA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GreenNeon,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tu perfil ha sido enlazado correctamente con tu dispositivo biométrico.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GreenNeon)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "DISPOSITIVO CONECTADO", fontSize = 10.sp, color = CyanNeon, letterSpacing = 2.sp)
                            Text(text = "BioGuard Sensor v2.1", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CyanNeon)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "USUARIO REGISTRADO", fontSize = 10.sp, color = CyanNeon, letterSpacing = 2.sp)
                            Text(text = "perfil@bioguard.med", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(YellowNeon)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "CALIBRACIÓN", fontSize = 10.sp, color = YellowNeon, letterSpacing = 2.sp)
                            Text(text = "Sensores listos para monitoreo", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
            ) {
                Text(
                    text = "CONTINULAR AL DASHBOARD",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 14.sp
                )
            }
        }
    }
}
