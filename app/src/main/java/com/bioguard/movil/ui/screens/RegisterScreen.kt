package com.bioguard.movil.ui.screens

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.ui.theme.CyanNeon
import com.bioguard.movil.ui.theme.DarkBackground
import com.bioguard.movil.ui.theme.DarkSurface
import com.bioguard.movil.ui.theme.GlassBackground
import com.bioguard.movil.ui.theme.GlassBorder
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.InputBackground
import com.bioguard.movil.ui.theme.InputBorder
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.TextPrimary
import com.bioguard.movil.ui.theme.TextSecondary
import com.bioguard.movil.ui.theme.TextTertiary
import androidx.compose.runtime.collectAsState
import com.bioguard.movil.ui.viewmodel.AuthViewModel
import com.bioguard.movil.ui.components.SystemNotificationDialog

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val uiState by authViewModel.uiState.collectAsState()

    val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passwordValid = password.length >= 6
    val passwordsMatch = password == confirmPassword

    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var successDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            isLoading = false
            successDialogMessage = it
            authViewModel.clearSuccess()
            if (!uiState.requiresVerification) {
                onRegisterSuccess()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            isLoading = false
            errorDialogMessage = it
            authViewModel.clearError()
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
                        text = "NOMBRE",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(100) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tu primer y segundo nombre", color = TextTertiary) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
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
                        text = "APELLIDO PATERNO",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = apellidoPaterno,
                        onValueChange = { apellidoPaterno = it.take(100) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tu apellido paterno", color = TextTertiary) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
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
                        text = "APELLIDO MATERNO (OPCIONAL)",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = apellidoMaterno,
                        onValueChange = { apellidoMaterno = it.take(100) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tu apellido materno", color = TextTertiary) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
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
                        text = "CORREO ELECTR\u00d3NICO",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.take(254) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("correo@ejemplo.com", color = TextTertiary) },
                        leadingIcon = {
                            Text(text = "\u2709", color = TextTertiary, fontSize = 16.sp)
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

                    if (email.isNotEmpty() && !emailValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Ingresa un correo electr\u00f3nico v\u00e1lido", fontSize = 11.sp, color = RedNeon, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CONTRASE\u00d1A",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", color = TextTertiary) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
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
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )

                    if (password.isNotEmpty() && !passwordValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "La contrase\u00f1a debe tener al menos 6 caracteres", fontSize = 11.sp, color = RedNeon, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CONFIRMAR CONTRASE\u00d1A",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", color = TextTertiary) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
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
                                Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                            }
                        }
                    )

                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Las contrase\u00f1as no coinciden", fontSize = 11.sp, color = RedNeon, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (emailValid && passwordValid && passwordsMatch && name.isNotEmpty() && apellidoPaterno.isNotEmpty()) {
                                isLoading = true
                                authViewModel.register(name, apellidoPaterno, apellidoMaterno, email, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        enabled = !isLoading && name.isNotEmpty() && apellidoPaterno.isNotEmpty() && emailValid && passwordValid && passwordsMatch,
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
                            text = "\u00bfYa tienes cuenta? Iniciar sesi\u00f3n",
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

        if (uiState.requiresVerification) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { authViewModel.clearVerificationState() }
            ) {
                var verificationCode by remember { mutableStateOf("") }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VERIFICAR CUENTA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            letterSpacing = 3.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "Introduce el código OTP enviado a tu correo:\n${uiState.pendingEmail}",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 20.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        OtpInputField(
                            otpText = verificationCode,
                            onOtpTextChange = { verificationCode = it }
                        )

                        if (uiState.error != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "",
                                color = RedNeon,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { authViewModel.clearVerificationState() }
                            ) {
                                Text("CANCELAR", color = TextSecondary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (verificationCode.length == 6) {
                                        authViewModel.verificarOtp(uiState.pendingEmail ?: "", verificationCode)
                                    }
                                },
                                enabled = verificationCode.length == 6 && !uiState.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanNeon,
                                    disabledContainerColor = CyanNeon.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = DarkBackground,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("VERIFICAR", color = DarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (errorDialogMessage != null) {
            SystemNotificationDialog(
                title = "AVISO DE ERROR",
                message = errorDialogMessage ?: "",
                isError = true,
                onDismiss = { errorDialogMessage = null }
            )
        }

        if (successDialogMessage != null) {
            SystemNotificationDialog(
                title = "OPERACIÓN EXITOSA",
                message = successDialogMessage ?: "",
                isError = false,
                onDismiss = { successDialogMessage = null }
            )
        }
    }
}

@Composable
fun OtpInputField(
    otpText: String,
    onOtpTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    otpCount: Int = 6
) {
    val focusRequesters = remember { List(otpCount) { FocusRequester() } }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(otpCount) { index ->
            val digit = otpText.getOrNull(index)?.toString() ?: ""
            var isFocused by remember { mutableStateOf(false) }

            BasicTextField(
                value = digit,
                onValueChange = { newValue ->
                    val digits = newValue.filter { it.isDigit() }
                    if (digits.length > 1) {
                        val fullCode = digits.take(otpCount)
                        onOtpTextChange(fullCode)
                        val lastIndex = (fullCode.length - 1).coerceIn(0, otpCount - 1)
                        focusRequesters[lastIndex].requestFocus()
                    } else if (digits.length == 1) {
                        val currentChars = otpText.toCharArray().toMutableList()
                        while (currentChars.size <= index) {
                            currentChars.add(' ')
                        }
                        currentChars[index] = digits[0]
                        val updated = String(currentChars.toCharArray()).replace(" ", "").take(otpCount)
                        onOtpTextChange(updated)
                        if (index < otpCount - 1) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    } else if (digits.isEmpty()) {
                        if (otpText.length > index) {
                            val updated = otpText.removeRange(index, index + 1)
                            onOtpTextChange(updated)
                        }
                        if (index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    }
                },
                modifier = Modifier
                    .width(40.dp)
                    .height(50.dp)
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { isFocused = it.isFocused }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Backspace) {
                            if (digit.isEmpty() && index > 0) {
                                focusRequesters[index - 1].requestFocus()
                                return@onKeyEvent true
                            }
                        }
                        false
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(InputBackground)
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) CyanNeon else GlassBorder,
                        shape = RoundedCornerShape(10.dp)
                    ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (index == otpCount - 1) ImeAction.Done else ImeAction.Next
                ),
                singleLine = true,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
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
