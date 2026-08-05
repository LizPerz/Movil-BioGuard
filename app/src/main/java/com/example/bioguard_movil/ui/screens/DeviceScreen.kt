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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.bioguard_movil.ui.viewmodel.DeviceViewModel

@Composable
fun DeviceScreen(deviceViewModel: DeviceViewModel) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by deviceViewModel.uiState.collectAsState()

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

    val reloj = uiState.dispositivo?.reloj
    val isConnected = reloj?.conectado ?: false
    val deviceName = reloj?.modelo ?: "Sin dispositivo"
    val bateria = reloj?.bateria
    val ultimaSincronizacion = reloj?.ultimaSincronizacion
    val sensores = reloj?.sensoresDisponibles
    val conectado = reloj?.conectado ?: false

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(p.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = p.accent)
        }
    } else if (uiState.error != null && uiState.dispositivo == null) {
        ErrorRetryBox(
            message = uiState.error,
            onRetry = { deviceViewModel.loadDispositivo() }
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
                    text = "DISPOSITIVO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Estado y configuraci\u00f3n del wearable",
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
                            Text(text = "\u231a", fontSize = 40.sp, color = p.accent)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = deviceName,
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
                                    if (conectado) GreenNeon.copy(alpha = 0.1f) else RedNeon.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (conectado) GreenNeon else RedNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (conectado) "CONECTADO" else "DESCONECTADO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (conectado) GreenNeon else RedNeon,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "INFORMACI\u00d3N DEL DISPOSITIVO",
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                DeviceInfoRow(label = "MODELO", value = deviceName)
                DeviceInfoRow(label = "CONEXI\u00d3N", value = if (conectado) "Activa" else "Inactiva")
                bateria?.let {
                    DeviceInfoRow(label = "BATER\u00cdA", value = "$it%")
                }
                ultimaSincronizacion?.let {
                    DeviceInfoRow(label = "\u00daLTIMA SINCRONIZACI\u00d3N", value = it.replace("T", " ").substringBefore("."))
                }
                if (!sensores.isNullOrEmpty()) {
                    DeviceInfoRow(label = "SENSORES", value = sensores.joinToString(", "))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { deviceViewModel.loadDispositivo() },
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
