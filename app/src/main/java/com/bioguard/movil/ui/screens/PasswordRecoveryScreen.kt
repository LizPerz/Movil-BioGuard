package com.bioguard.movil.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bioguard.movil.R
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
import com.bioguard.movil.ui.theme.CyanNeon
import com.bioguard.movil.ui.theme.DarkBackground
import com.bioguard.movil.ui.theme.DarkSurface
import com.bioguard.movil.ui.theme.GlassBorder
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.InputBackground
import com.bioguard.movil.ui.theme.TextPrimary
import com.bioguard.movil.ui.theme.TextSecondary
import com.bioguard.movil.ui.theme.TextTertiary
import com.bioguard.movil.ui.viewmodel.AuthViewModel

@Composable
fun PasswordRecoveryScreen(
    authViewModel: AuthViewModel,
    onResetSuccess: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by authViewModel.uiState.collectAsState()
    var step by remember { mutableIntStateOf(1) }
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val code = remember { mutableStateListOf("", "", "", "", "", "", "", "") }
    val focusRequesters = List(8) { remember { FocusRequester() } }

    val context = LocalContext.current

    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var successDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            errorDialogMessage = it
            authViewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            successDialogMessage = it
            authViewModel.clearSuccess()
            when (step) {
                1 -> step = 2
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
                            .background(if (s <= step) CyanNeon else GlassBorder)
                    )
                    if (index < 2) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (s < step) CyanNeon else GlassBorder)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (step) {
                1 -> {
                    Text(
                        text = stringResource(R.string.password_recovery_title),
                        fontSize = 11.sp,
                        color = CyanNeon,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_step1),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_enter_email),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_desc),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_email_label),
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.take(254) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.password_recovery_email_hint), color = TextTertiary) },
                        leadingIcon = { Text(text = "\u2709", color = TextTertiary, fontSize = 16.sp) },
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

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { authViewModel.forgotPassword(email) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        enabled = !uiState.isLoading && email.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            disabledContainerColor = CyanNeon.copy(alpha = 0.3f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Text(text = stringResource(R.string.password_recovery_send_code), color = DarkBackground, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
                        }
                    }
                }

                2 -> {
                    Text(
                        text = stringResource(R.string.password_recovery_verify_title),
                        fontSize = 11.sp,
                        color = CyanNeon,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_step2),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_verify_code),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_verify_desc),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = email,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CyanNeon,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
                    ) {
                        code.forEachIndexed { index, digit ->
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
                                            if (char.isNotEmpty() && char[0].isLetterOrDigit()) {
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
                        text = stringResource(R.string.password_recovery_verify_hint),
                        fontSize = 10.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            step = 3
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        enabled = code.all { it.isNotEmpty() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            disabledContainerColor = CyanNeon.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(text = stringResource(R.string.password_recovery_verify_btn), color = DarkBackground, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.password_recovery_resend_prompt),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.password_recovery_resend_link),
                            fontSize = 12.sp,
                            color = CyanNeon,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                authViewModel.forgotPassword(email)
                            }
                        )
                    }
                }

                3 -> {
                    Text(
                        text = stringResource(R.string.password_recovery_new_title),
                        fontSize = 11.sp,
                        color = CyanNeon,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_step3),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_new_desc),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Text(
                        text = stringResource(R.string.password_recovery_new_label),
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.password_recovery_new_hint), color = TextTertiary) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.password_recovery_confirm_label),
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.password_recovery_confirm_hint), color = TextTertiary) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

                    if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = stringResource(R.string.password_recovery_mismatch), fontSize = 11.sp, color = Color(0xFFFF4444))
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            val codeStr = code.joinToString("")
                            authViewModel.resetPassword(codeStr, email, newPassword)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        enabled = !uiState.isLoading && newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword == confirmPassword && newPassword.length >= 6,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            disabledContainerColor = CyanNeon.copy(alpha = 0.3f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Text(text = stringResource(R.string.password_recovery_reset_btn), color = DarkBackground, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.password_recovery_back_login),
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onBack() }
                    .padding(8.dp)
            )
        }

        if (errorDialogMessage != null) {
            SystemNotificationDialog(
                title = stringResource(R.string.dialog_error_title),
                message = errorDialogMessage ?: "",
                isError = true,
                onDismiss = { errorDialogMessage = null }
            )
        }

        if (successDialogMessage != null) {
            SystemNotificationDialog(
                title = stringResource(R.string.dialog_success_title),
                message = successDialogMessage ?: "",
                isError = false,
                onDismiss = { successDialogMessage = null }
            )
        }
    }
}

@Composable
fun SystemNotificationDialog(
    title: String,
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(com.bioguard.movil.ui.theme.DarkSurface)
                .border(
                    width = 1.dp,
                    color = com.bioguard.movil.ui.theme.GlassBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = if (isError) com.bioguard.movil.ui.theme.RedNeon else com.bioguard.movil.ui.theme.CyanNeon,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = message,
                    color = com.bioguard.movil.ui.theme.TextPrimary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isError) com.bioguard.movil.ui.theme.RedNeon else com.bioguard.movil.ui.theme.CyanNeon
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_understood),
                        color = if (isError) Color.White else com.bioguard.movil.ui.theme.DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
