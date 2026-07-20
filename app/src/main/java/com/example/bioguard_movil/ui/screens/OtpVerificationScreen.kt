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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bioguard_movil.ui.theme.CyanNeon
import com.example.bioguard_movil.ui.theme.DarkBackground
import com.example.bioguard_movil.ui.theme.DarkSurface
import com.example.bioguard_movil.ui.theme.GlassBorder
import com.example.bioguard_movil.ui.theme.TextPrimary
import com.example.bioguard_movil.ui.theme.TextSecondary
import com.example.bioguard_movil.ui.theme.TextTertiary
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OtpVerificationScreen(
    contactDisplay: String = "+52 ***1234",
    onVerified: () -> Unit = {},
    onBack: () -> Unit = {},
    onResend: () -> Unit = {}
) {
    val code = remember { mutableStateListOf("", "", "", "", "", "", "", "") }
    var isLoading by remember { mutableStateOf(false) }
    var triggerVerify by remember { mutableStateOf(false) }
    var resendTimer by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusRequesters = List(8) { remember { FocusRequester() } }
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

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

    LaunchedEffect(resendTimer) {
        if (resendTimer > 0) {
            kotlinx.coroutines.delay(1000.milliseconds)
            resendTimer--
        } else {
            canResend = true
        }
    }

    LaunchedEffect(triggerVerify) {
        if (triggerVerify) {
            kotlinx.coroutines.delay(2000.milliseconds)
            isLoading = false
            triggerVerify = false
            onVerified()
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
                text = "VERIFICACIÓN",
                fontSize = 11.sp,
                color = CyanNeon,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Código de Verificación",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Enviamos un código de 8 caracteres a",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = contactDisplay,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CyanNeon,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                code.forEachIndexed { index, digit ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .border(
                                width = if (digit.isNotEmpty()) 2.dp else 1.dp,
                                color = if (digit.isNotEmpty()) CyanNeon else GlassBorder,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = digit,
                            onValueChange = { value ->
                                if (value.length <= 1) {
                                    val char = value.uppercase()
                                    if (char.isNotEmpty() && (char[0].isLetter() || char[0].isDigit())) {
                                        code[index] = char
                                        if (index < 7) {
                                            focusRequesters[index + 1].requestFocus()
                                        }
                                    } else if (char.isEmpty()) {
                                        code[index] = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequesters[index]),
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Letras mayúsculas y números (8 caracteres)",
                fontSize = 10.sp,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(12.dp))
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
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "📷", fontSize = 20.sp, color = CyanNeon)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ESCANEAR CÓDIGO QR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Usa la cámara para escanear el código de verificación",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    isLoading = true
                    triggerVerify = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(10.dp)),
                enabled = !isLoading && code.all { it.isNotEmpty() },
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
                        text = "VERIFICAR",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (canResend) "¿No recibiste el código? " else "Reenviar en ${resendTimer}s ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (canResend) {
                    Text(
                        text = "Reenviar",
                        fontSize = 12.sp,
                        color = CyanNeon,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            resendTimer = 30
                            canResend = false
                            onResend()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "← Volver al inicio de sesión",
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onBack() }
                    .padding(8.dp)
            )
        }
    }
}
