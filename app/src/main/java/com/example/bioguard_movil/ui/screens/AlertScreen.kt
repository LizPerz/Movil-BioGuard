package com.example.bioguard_movil.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedDark
import com.example.bioguard_movil.ui.theme.RedLight
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.TextPrimary
import com.example.bioguard_movil.ui.theme.TextSecondary
import com.example.bioguard_movil.ui.theme.TextTertiary
import com.example.bioguard_movil.ui.theme.colorPalette
import com.example.bioguard_movil.ui.viewmodel.AlertViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertScreen(
    alertViewModel: AlertViewModel,
    onDismiss: () -> Unit = {},
    onEmergencyCall: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by alertViewModel.uiState.collectAsState()
    val context = LocalContext.current

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

    val pendingAlert = uiState.alertasPendientes.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
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

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedNeon)
            }
        } else if (pendingAlert != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
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
                    Text(text = "\u26a0", fontSize = 48.sp, color = RedNeon.copy(alpha = pulseAlpha))
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "ALERTA",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedNeon,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = pendingAlert.tipoAlerta.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedNeon,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.surface)
                        .border(width = 2.dp, color = RedNeon.copy(alpha = borderAlpha), shape = RoundedCornerShape(12.dp))
                        .padding(28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "ALERTA ACTIVA".uppercase(), fontSize = 12.sp, color = RedLight, letterSpacing = 2.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "PENDIENTE",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedNeon
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Requiere atenci\u00f3n inmediata",
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
                        text = pendingAlert.descripcion.ifEmpty { "Se detect\u00f3 una alerta en el paciente." },
                        fontSize = 13.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        alertViewModel.atenderAlerta(pendingAlert.id, "Emergencia atendida")
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911")))
                        onEmergencyCall()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedNeon)
                ) {
                    Text(
                        text = "LLAMADA DE EMERGENCIA",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        alertViewModel.atenderAlerta(pendingAlert.id, "Descartada por usuario")
                        onDismiss()
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
                ) {
                    Text(text = "CANCELAR ALERTA", color = TextSecondary, letterSpacing = 2.sp, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Alerta generada: ${pendingAlert.fechaCreacion.substringBefore("T")} ${pendingAlert.fechaCreacion.substringAfter("T", "").substringBefore(".")}",
                    fontSize = 10.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GreenNeon.copy(alpha = 0.1f))
                        .border(width = 2.dp, color = GreenNeon, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "\u2713", fontSize = 36.sp, color = GreenNeon)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SIN ALERTAS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenNeon,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "No hay alertas pendientes en este momento.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Text(
                        text = "VOLVER",
                        color = p.background,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
