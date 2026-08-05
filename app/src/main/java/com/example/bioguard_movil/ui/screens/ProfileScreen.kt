package com.example.bioguard_movil.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.model.UserRole
import com.example.bioguard_movil.ui.theme.AppTheme
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.ThemeState
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.colorPalette
import com.example.bioguard_movil.ui.viewmodel.ProfileViewModel
import com.example.bioguard_movil.ui.components.SystemNotificationDialog
import com.example.bioguard_movil.ui.components.ConfirmDialog
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    role: UserRole = UserRole.UNKNOWN,
    onLogout: () -> Unit = {},
    themeState: ThemeState = ThemeState(),
    onThemeChange: (ThemeState) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMedications: () -> Unit = {},
    onNavigateToCuidadores: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by profileViewModel.uiState.collectAsState()
    var isDarkMode by remember(themeState) {
        mutableStateOf(themeState.theme != AppTheme.CLARO)
    }

    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var successDialogMessage by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            errorDialogMessage = it
            profileViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            successDialogMessage = it
            profileViewModel.clearMessages()
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(p.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = p.accent)
        }
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
                    text = "MI PERFIL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(p.accentDark.copy(alpha = 0.3f))
                                .border(width = 2.dp, color = p.accent, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "\uD83D\uDC64", fontSize = 40.sp, color = p.accent)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = uiState.perfil?.nombre ?: "Usuario BioGuard",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.textPrimary
                        )

                        Text(
                            text = uiState.perfil?.correo ?: "correo@bioguard.med",
                            fontSize = 13.sp,
                            color = p.textSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GreenNeon.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(text = "ACTIVO", fontSize = 10.sp, color = GreenNeon, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "DATOS BIOM\u00c9TRICOS",
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                ProfileInfoRow(label = "NOMBRE", value = uiState.perfil?.nombre ?: "-")
                ProfileInfoRow(label = "CORREO", value = uiState.perfil?.correo ?: "-")
                ProfileInfoRow(label = "FECHA REGISTRO", value = uiState.perfil?.fechaRegistro?.substringBefore("T") ?: "-")
                ProfileInfoRow(label = "PLAN", value = uiState.plan?.nombre ?: uiState.perfil?.planNombre ?: "-")

                if (role.canSeePlan) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "MI PLAN",
                        fontSize = 10.sp,
                        color = p.accent,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    uiState.plan?.let { plan ->
                        ProfileInfoRow(label = "L\u00cdMITE PACIENTES", value = "${plan.limitePacientes}")
                        ProfileInfoRow(label = "L\u00cdMITE CUIDADORES", value = "${plan.limiteCuidadores}")
                        ProfileInfoRow(label = "HISTORIAL", value = "${plan.retencionHistorialDias} d\u00edas")
                        ProfileInfoRow(label = "GPS CONTINUO", value = if (plan.gpsActivo) "S\u00ed" else "No")
                        ProfileInfoRow(label = "CONSOLA AI", value = if (plan.consolaIaActiva) "S\u00ed" else "No")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "CONFIGURACI\u00d3N",
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .clickable {
                            val newDark = !isDarkMode
                            onThemeChange(
                                if (newDark) {
                                    themeState.copy(theme = AppTheme.OSCURO, isDarkMode = true)
                                } else {
                                    themeState.copy(theme = AppTheme.CLARO, isDarkMode = false)
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Modo Oscuro", fontSize = 13.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
                            Text(text = if (isDarkMode) "Activado" else "Desactivado", fontSize = 10.sp, color = p.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkMode) p.accent else p.border)
                                .padding(2.dp),
                            contentAlignment = if (isDarkMode) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                ProfileMenuRow(text = "Notificaciones", subtitle = "Gestionar alertas", onClick = onNavigateToNotifications)
                ProfileMenuRow(text = "Ajustes de Sincronizacion", subtitle = "Frecuencia de datos y Guardian Nocturno", onClick = onNavigateToSettings)
                if (role.canManageMedicamentos) {
                    ProfileMenuRow(text = "Medicamentos", subtitle = "Gestionar dosis y tomas", onClick = onNavigateToMedications)
                }
                if (role.canManageCuidadores) {
                    ProfileMenuRow(text = "Cuidadores", subtitle = "Gestionar acceso de cuidadores", onClick = onNavigateToCuidadores)
                }
                if (role.canManagePayments) {
                    ProfileMenuRow(text = "Suscripci\u00f3n Premium", subtitle = "Plan activo - Toque para gestionar")
                }
                ProfileMenuRow(text = "Soporte T\u00e9cnico", subtitle = "Reportar un problema / Crear ticket", onClick = onNavigateToSupport)
                ProfileMenuRow(text = "Privacidad", subtitle = "Datos y seguridad")
                ProfileMenuRow(text = "Acerca de", subtitle = "v1.0.0")

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedNeon)
                ) {
                    Text(
                        text = "CERRAR SESI\u00d3N",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
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

        if (showLogoutConfirm) {
            ConfirmDialog(
                title = "CERRAR SESIÓN",
                message = "¿Estás seguro de que deseas cerrar tu sesión en BioGuard?",
                confirmText = "CERRAR SESIÓN",
                cancelText = "CANCELAR",
                onConfirm = {
                    showLogoutConfirm = false
                    onLogout()
                },
                onCancel = { showLogoutConfirm = false }
            )
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
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

@Composable
fun ProfileMenuRow(text: String, subtitle: String, onClick: (() -> Unit)? = null) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = text, fontSize = 13.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
                Text(text = subtitle, fontSize = 10.sp, color = p.textSecondary)
            }
            Text(text = "\u2192", fontSize = 14.sp, color = p.accent)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}
