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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.colorPalette

@Composable
fun DeviceScreen() {
    val p = LocalThemeState.current.colorPalette()
    val infiniteTransition = rememberInfiniteTransition(label = "connected")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val batteryLevel = 78
    val isConnected = true
    val signalStrength = 65

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
                text = "DISPOSITIVO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = p.accent,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Estado y configuración del wearable",
                fontSize = 12.sp,
                color = p.textSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.surface)
                    .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(p.accentDark.copy(alpha = 0.2f))
                            .border(width = 2.dp, color = p.accent.copy(alpha = pulseAlpha), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⌚", fontSize = 40.sp, color = p.accent)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Galaxy Watch 6",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textPrimary
                    )

                    Text(
                        text = "BioGuard Sensor v2.1",
                        fontSize = 12.sp,
                        color = p.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isConnected) GreenNeon.copy(alpha = 0.1f) else RedNeon.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) GreenNeon else RedNeon)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "CONECTADO" else "DESCONECTADO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isConnected) GreenNeon else RedNeon,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SEÑAL BLUETOOTH",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Intensidad de señal", fontSize = 13.sp, color = p.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            (1..5).forEach { bar ->
                                val filled = bar <= (signalStrength / 20f).toInt().coerceIn(0, 5)
                                Box(
                                    modifier = Modifier
                                        .size(width = 6.dp, height = (8 + bar * 4).dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (filled) p.accent else p.border)
                                )
                                if (bar < 5) Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                    Text(
                        text = "$signalStrength%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (signalStrength > 60) GreenNeon else if (signalStrength > 30) YellowNeon else RedNeon
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BATERÍA",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Nivel de batería", fontSize = 13.sp, color = p.textPrimary)
                        Text(
                            text = "$batteryLevel%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (batteryLevel > 50) GreenNeon else if (batteryLevel > 20) YellowNeon else RedNeon
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { batteryLevel / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (batteryLevel > 50) GreenNeon else if (batteryLevel > 20) YellowNeon else RedNeon,
                        trackColor = p.border,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (batteryLevel > 50) "Batería suficiente" else if (batteryLevel > 20) "Carga moderada" else "Batería baja",
                        fontSize = 11.sp,
                        color = p.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "INFORMACIÓN DEL DISPOSITIVO",
                fontSize = 10.sp,
                color = p.accent,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            DeviceInfoRow(label = "MODELO", value = "Galaxy Watch 6 Classic")
            DeviceInfoRow(label = "FIRMWARE", value = "v2.1.4")
            DeviceInfoRow(label = "ID DEL DISPOSITIVO", value = "BG-2026-XXXX")
            DeviceInfoRow(label = "ÚLTIMA SINCRONIZACIÓN", value = "Hoy, 14:30")
            DeviceInfoRow(label = "SENSORES", value = "PPG, Temperatura, EDA")

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(
                    text = "RECONECTAR",
                    color = p.background,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    val p = LocalThemeState.current.colorPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, color = p.textSecondary, letterSpacing = 1.sp)
        Text(text = value, fontSize = 12.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(6.dp))
}
