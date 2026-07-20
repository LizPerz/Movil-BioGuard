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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.CyanNeon
import com.example.bioguard_movil.ui.theme.DarkBackground
import com.example.bioguard_movil.ui.theme.DarkSurface
import com.example.bioguard_movil.ui.theme.GlassBackground
import com.example.bioguard_movil.ui.theme.GlassBorder
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.InputBackground
import com.example.bioguard_movil.ui.theme.InputBorder
import com.example.bioguard_movil.ui.theme.TextPrimary
import com.example.bioguard_movil.ui.theme.TextSecondary
import com.example.bioguard_movil.ui.theme.TextTertiary
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var triggerRegister by remember { mutableStateOf(false) }

    LaunchedEffect(triggerRegister) {
        if (triggerRegister) {
            kotlinx.coroutines.delay(2000.milliseconds)
            isLoading = false
            triggerRegister = false
            onRegisterSuccess()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
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
                        colors = listOf(CyanNeon.copy(alpha = glowAlpha * 0.06f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
                text = "CREAR CUENTA",
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = CyanNeon.copy(alpha = 0.1f),
                        spotColor = CyanNeon.copy(alpha = 0.15f)
                    )
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
                        text = "NOMBRE COMPLETO",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tu nombre", color = TextTertiary) },
                        leadingIcon = {
                            Text(text = "👤", color = TextTertiary, fontSize = 16.sp)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CORREO ELECTRÓNICO",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("correo@ejemplo.com", color = TextTertiary) },
                        leadingIcon = {
                            Text(text = "✉", color = TextTertiary, fontSize = 16.sp)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CONTRASEÑA",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••", color = TextTertiary) },
                        leadingIcon = {
                            Text(text = "🔒", color = TextTertiary, fontSize = 16.sp)
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CONFIRMAR CONTRASEÑA",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••", color = TextTertiary) },
                        leadingIcon = {
                            Text(text = "🔒", color = TextTertiary, fontSize = 16.sp)
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Text(text = if (confirmPasswordVisible) "👁" else "👁‍🗨", color = CyanNeon, fontSize = 14.sp)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            triggerRegister = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        enabled = !isLoading && name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword,
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
                                text = "CREAR CUENTA",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onBackToLogin) {
                        Text(
                            text = "¿Ya tienes cuenta? Iniciar sesión",
                            color = CyanNeon,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBackground)
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
                    text = "REGISTRO SEGURO Y CIFRADO",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
