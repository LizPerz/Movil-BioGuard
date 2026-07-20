package com.example.bioguard_movil.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bioguard_movil.ui.theme.CyanNeon
import com.example.bioguard_movil.ui.theme.DarkBackground
import com.example.bioguard_movil.ui.theme.DarkSurface
import com.example.bioguard_movil.ui.theme.GlassBorder
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.InputBackground
import com.example.bioguard_movil.ui.theme.TextPrimary
import com.example.bioguard_movil.ui.theme.TextSecondary
import com.example.bioguard_movil.ui.theme.TextTertiary
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CodeAccessScreen(
    onCodeEntered: () -> Unit = {}
) {
    var codigo by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var triggerAction by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var toastMessage by remember { mutableStateOf("") }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotEmpty()) {
            Toast.makeText(context.applicationContext, toastMessage, Toast.LENGTH_SHORT).show()
            toastMessage = ""
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        toastMessage = if (isGranted) "Cámara activada. Escanea el código QR." else "Permiso de cámara denegado"
    }

    LaunchedEffect(triggerAction) {
        if (triggerAction) {
            kotlinx.coroutines.delay(1500.milliseconds)
            isLoading = false
            triggerAction = false
            onCodeEntered()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
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
                        colors = listOf(CyanNeon.copy(alpha = glowAlpha * 0.05f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BIOGUARD",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon,
                letterSpacing = 8.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "SISTEMA DE MONITOREO MÉDICO",
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CÓDIGO DE ACCESO",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { codigo = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ingresa tu código", color = TextTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanNeon
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(10.dp))
                            .clickable {
                                val permission = Manifest.permission.CAMERA
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, permission
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    toastMessage = "Cámara activada. Escanea el código QR."
                                } else {
                                    cameraPermissionLauncher.launch(permission)
                                }
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "📷", fontSize = 18.sp, color = CyanNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ESCANEAR CÓDIGO QR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            triggerAction = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        enabled = !isLoading && codigo.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            disabledContainerColor = CyanNeon.copy(alpha = 0.3f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = DarkBackground,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "ACCEDER",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBorder.copy(alpha = 0.2f))
                    .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GreenNeon)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DISPOSITIVO BIOMÉTRICO ACTIVO",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
