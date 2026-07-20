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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
fun PasswordRecoveryScreen(
    onResetSuccess: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(1) }
    var contact by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var triggerAction by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val otpCode = remember { mutableStateListOf("", "", "", "", "", "", "", "") }
    val focusRequesters = List(8) { remember { FocusRequester() } }
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
            when (step) {
                1 -> step = 2
                2 -> step = 3
                3 -> onResetSuccess()
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1, 2, 3).forEachIndexed { index, s ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (s <= step) CyanNeon else GlassBorder
                            )
                    )
                    if (index < 2) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (s < step) CyanNeon else GlassBorder
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (step) {
                1 -> {
                    Text(
                        text = "RECUPERAR CONTRASEÑA",
                        fontSize = 11.sp,
                        color = CyanNeon,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Paso 1 de 3",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Ingresa tu correo o teléfono",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Te enviaremos un código para verificar tu identidad",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Correo o teléfono", color = TextTertiary) },
                        leadingIcon = { Text(text = "✉", color = TextTertiary, fontSize = 16.sp) },
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

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            triggerAction = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        enabled = !isLoading && contact.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            disabledContainerColor = CyanNeon.copy(alpha = 0.3f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Text(text = "ENVIAR CÓDIGO", color = DarkBackground, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
                        }
                    }
                }

                2 -> {
                    Text(
                        text = "VERIFICAR CÓDIGO",
                        fontSize = 11.sp,
                        color = CyanNeon,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Paso 2 de 3",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Código de verificación",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Ingresa el código de 8 caracteres enviado a tu contacto",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
                    ) {
                        otpCode.forEachIndexed { index, digit ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .border(
                                        width = if (digit.isNotEmpty()) 2.dp else 1.dp,
                                        color = if (digit.isNotEmpty()) CyanNeon else GlassBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = digit,
                                    onValueChange = { value ->
                                        if (value.length <= 1) {
                                            val char = value.uppercase()
                                            if (char.isNotEmpty() && (char[0].isLetter() || char[0].isDigit())) {
                                                otpCode[index] = char
                                                if (index < 7) {
                                                    focusRequesters[index + 1].requestFocus()
                                                }
                                            } else if (char.isEmpty()) {
                                                otpCode[index] = ""
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
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Letras mayúsculas y números (8 caracteres)",
                        fontSize = 10.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(bottom = 6.dp)
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
                            .padding(12.dp),
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
                                fontSize = 11.sp,
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
                        enabled = !isLoading && otpCode.all { it.isNotEmpty() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            disabledContainerColor = CyanNeon.copy(alpha = 0.3f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Text(text = "VERIFICAR", color = DarkBackground, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
                        }
                    }
                }

                3 -> {
                    Text(
                        text = "NUEVA CONTRASEÑA",
                        fontSize = 11.sp,
                        color = CyanNeon,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Paso 3 de 3",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Crea tu nueva contraseña",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nueva contraseña", color = TextTertiary) },
                        leadingIcon = { Text(text = "🔒", color = TextTertiary, fontSize = 16.sp) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(text = if (passwordVisible) "👁" else "👁‍🗨", color = CyanNeon, fontSize = 14.sp)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Confirmar contraseña", color = TextTertiary) },
                        leadingIcon = { Text(text = "🔒", color = TextTertiary, fontSize = 16.sp) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (confirmPassword.isNotEmpty() && newPassword == confirmPassword) GreenNeon else CyanNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanNeon
                        )
                    )

                    if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Las contraseñas no coinciden", fontSize = 11.sp, color = Color(0xFFFF4444))
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            triggerAction = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        enabled = !isLoading && newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword == confirmPassword,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            disabledContainerColor = CyanNeon.copy(alpha = 0.3f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Text(text = "RESTABLECER CONTRASEÑA", color = DarkBackground, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
