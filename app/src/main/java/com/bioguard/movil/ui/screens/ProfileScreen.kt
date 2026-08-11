package com.bioguard.movil.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.R
import com.bioguard.movil.ui.components.ConfirmDialog
import com.bioguard.movil.ui.components.SystemNotificationDialog
import com.bioguard.movil.ui.model.AppPermission
import com.bioguard.movil.ui.model.EffectiveAccess
import com.bioguard.movil.ui.model.UserRole
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.ui.theme.AppTheme
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.ThemeState
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.ProfileViewModel
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlinx.coroutines.flow.first

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    access: EffectiveAccess = EffectiveAccess(),
    userName: String? = null,
    onLogout: () -> Unit = {},
    themeState: ThemeState = ThemeState(),
    onThemeChange: (ThemeState) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMedications: () -> Unit = {},
    onNavigateToCuidadores: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDevice: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val context = LocalContext.current
    val uiState by profileViewModel.uiState.collectAsState()
    val prefs = remember { UserPreferences(context) }
    var localPhoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        localPhoto = prefs.patientPhoto.first()
    }
    var isDarkMode by remember(themeState) {
        mutableStateOf(themeState.theme != AppTheme.CLARO)
    }

    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var successDialogMessage by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showEditBiometriaModal by remember { mutableStateOf(false) }
    var uploadingPhoto by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = runCatching {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }.getOrNull() ?: return@rememberLauncherForActivityResult
            val resized = resizeBitmap(bitmap, maxDim = 512)
            uploadingPhoto = true
            profileViewModel.updateFotoPerfil(encodeToBase64(resized))
        }
    }

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

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) uploadingPhoto = false
    }

    if (showEditBiometriaModal && access.allows(AppPermission.PATIENT_MANAGE)) {
        EditBiometriaDialog(
            currentBirth = uiState.biometria.fechaNacimiento,
            currentSex = uiState.biometria.sexo,
            currentWeight = uiState.biometria.pesoKg.takeIf { it > 0.0 }?.toString().orEmpty(),
            currentHeight = uiState.biometria.estaturaCm.takeIf { it > 0.0 }?.toString().orEmpty(),
            currentDiabetic = uiState.biometria.esDiabetico,
            currentFamilyDiabetic = uiState.biometria.familiaresDiabetes,
            currentActivity = uiState.biometria.actividadFisica,
            onDismiss = { showEditBiometriaModal = false },
            onSave = { birth, sex, weight, height, isDiabetic, familyDiabetic, activity ->
                showEditBiometriaModal = false
                profileViewModel.updateBiometriaPaciente(
                    fechaNacimiento = birth,
                    sexo = sex,
                    pesoKg = weight.toDoubleOrNull() ?: 0.0,
                    estaturaCm = height.toDoubleOrNull() ?: 0.0,
                    esDiabetico = isDiabetic,
                    familiaresDiabetes = familyDiabetic,
                    actividadFisica = activity
                )
            }
        )
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
                    text = stringResource(R.string.profile_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ── Card de Información del Usuario ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val fotoBase64 = uiState.perfil?.fotoPerfil?.takeIf { it.isNotBlank() }
                            ?: (if (access.role == UserRole.PACIENTE) localPhoto else null)
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(p.accentDark.copy(alpha = 0.2f))
                                .border(width = 2.dp, color = p.accent, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (fotoBase64?.isNotBlank() == true) {
                                val bitmap = remember(fotoBase64) { decodeBase64ToBitmap(fotoBase64) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = p.accent,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = p.accent,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uploadingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = p.accent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddAPhoto,
                                    contentDescription = null,
                                    tint = p.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (fotoBase64?.isNotBlank() == true) "CAMBIAR FOTO" else "AGREGAR FOTO",
                                    color = p.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val fullUser = uiState.perfil
                        val displayName = fullUser?.nombre?.takeIf { it.isNotBlank() }
                            ?: userName?.takeIf { it.isNotBlank() }
                            ?: "Usuario BioGuard"

                        Text(
                            text = displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.textPrimary
                        )

                        Text(
                            text = fullUser?.correo ?: "Sin correo registrado",
                            fontSize = 12.sp,
                            color = p.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── PERFIL BIOMÉTRICO Y MÉDICO DEL PACIENTE ──
                if (access.allows(AppPermission.PATIENT_MANAGE)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.HealthAndSafety,
                        contentDescription = null,
                        tint = p.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PERFIL BIOMÉTRICO Y DATO MÉDICO",
                        fontSize = 10.sp,
                        color = p.accent,
                        letterSpacing = 2.sp
                    )
                }

                val bio = uiState.biometria
                ProfileInfoRow(label = "FECHA NACIMIENTO", value = bio.fechaNacimiento.takeIf { it.isNotBlank() }?.let { com.bioguard.movil.data.Formatters.toDisplayDate(it) } ?: "Sin registrar")
                ProfileInfoRow(label = "EDAD", value = bio.edad?.let { "$it años" } ?: "Sin registrar")
                ProfileInfoRow(label = "SEXO BIOLÓGICO", value = com.bioguard.movil.data.Formatters.sexoToDisplay(bio.sexo))
                ProfileInfoRow(label = "PESO CORPORAL", value = if (bio.pesoKg > 0.0) "${bio.pesoKg} kg" else "Sin registrar")
                ProfileInfoRow(label = "ESTATURA", value = if (bio.estaturaCm > 0.0) "${bio.estaturaCm} cm" else "Sin registrar")
                ProfileInfoRow(label = "NIVEL ACTIVIDAD FISICA", value = bio.actividadFisica.ifBlank { "Sin registrar" })
                ProfileInfoRow(label = "DIAGNÓSTICO DIABETES", value = if (bio.esDiabetico) "Sí" else "No")
                ProfileInfoRow(label = "ANTECEDENTES FAMILIARES DIABETES", value = if (bio.familiaresDiabetes) "Sí" else "No")

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showEditBiometriaModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = p.background,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EDITAR INFORMACIÓN MÉDICA",
                        color = p.background,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── PLAN Y CONFIGURACIÓN ──
                if (access.allows(AppPermission.BILLING_MANAGE)) {
                Text(
                    text = stringResource(R.string.profile_my_plan),
                    fontSize = 10.sp,
                    color = p.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                uiState.plan?.let { plan ->
                    ProfileInfoRow(label = stringResource(R.string.profile_limit_patients), value = "${plan.limitePacientes}")
                    ProfileInfoRow(label = stringResource(R.string.profile_limit_caregivers), value = "${plan.limiteCuidadores}")
                    ProfileInfoRow(label = stringResource(R.string.profile_history), value = stringResource(R.string.profile_days_format, plan.retencionHistorialDias))
                    ProfileInfoRow(label = stringResource(R.string.profile_gps_continuous), value = if (plan.gpsActivo) stringResource(R.string.profile_yes) else stringResource(R.string.profile_no))
                    ProfileInfoRow(label = stringResource(R.string.profile_ai_console), value = if (plan.consolaIaActiva) stringResource(R.string.profile_yes) else stringResource(R.string.profile_no))
                }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.profile_settings_title),
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
                            Text(text = stringResource(R.string.profile_dark_mode), fontSize = 13.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
                            Text(text = if (isDarkMode) stringResource(R.string.profile_activated) else stringResource(R.string.profile_deactivated), fontSize = 10.sp, color = p.textSecondary)
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

                ProfileMenuRow(text = stringResource(R.string.profile_notifications), subtitle = stringResource(R.string.profile_notifications_desc), onClick = onNavigateToNotifications)
                if (access.allows(AppPermission.DEVICE_READ) || access.allows(AppPermission.DEVICE_PAIR)) {
                    ProfileMenuRow(text = "Dispositivo Vinculado", subtitle = "Gestiona o conecta tu wearable / parche", onClick = onNavigateToDevice)
                }
                ProfileMenuRow(text = stringResource(R.string.profile_sync_settings), subtitle = stringResource(R.string.profile_sync_settings_desc), onClick = onNavigateToSettings)
                if (access.allows(AppPermission.MEDICATION_READ)) {
                    ProfileMenuRow(text = stringResource(R.string.profile_medications), subtitle = stringResource(R.string.profile_medications_desc), onClick = onNavigateToMedications)
                }
                if (access.allows(AppPermission.CAREGIVER_MANAGE)) {
                    ProfileMenuRow(text = stringResource(R.string.profile_caregivers), subtitle = stringResource(R.string.profile_caregivers_desc), onClick = onNavigateToCuidadores)
                }
                if (access.allows(AppPermission.BILLING_MANAGE)) {
                    ProfileMenuRow(text = stringResource(R.string.profile_premium), subtitle = stringResource(R.string.profile_premium_desc))
                }
                ProfileMenuRow(text = stringResource(R.string.profile_support), subtitle = stringResource(R.string.profile_support_desc), onClick = onNavigateToSupport)
                ProfileMenuRow(text = stringResource(R.string.profile_privacy), subtitle = stringResource(R.string.profile_privacy_desc))
                ProfileMenuRow(text = stringResource(R.string.profile_about), subtitle = "v1.0.0")

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
                        text = stringResource(R.string.profile_logout),
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

        if (showLogoutConfirm) {
            ConfirmDialog(
                title = stringResource(R.string.profile_logout),
                message = stringResource(R.string.profile_logout_confirm_msg),
                confirmText = stringResource(R.string.profile_logout),
                cancelText = stringResource(R.string.register_cancel),
                onConfirm = {
                    showLogoutConfirm = false
                    onLogout()
                },
                onCancel = { showLogoutConfirm = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBiometriaDialog(
    currentBirth: String,
    currentSex: String,
    currentWeight: String,
    currentHeight: String,
    currentDiabetic: Boolean,
    currentFamilyDiabetic: Boolean,
    currentActivity: String,
    onDismiss: () -> Unit,
    onSave: (birth: String, sex: String, weight: String, height: String, isDiabetic: Boolean, familyDiabetic: Boolean, activity: String) -> Unit
) {
    val p = LocalThemeState.current.colorPalette()

    var birthDate by remember(currentBirth) {
        mutableStateOf(com.bioguard.movil.data.Formatters.toDisplayDigits(currentBirth))
    }
    var weight by remember { mutableStateOf(currentWeight) }
    var height by remember { mutableStateOf(currentHeight) }
    var selectedSex by remember(currentSex) {
        mutableStateOf(com.bioguard.movil.data.Formatters.sexoToDisplay(currentSex))
    }
    var isDiabetic by remember { mutableStateOf(currentDiabetic) }
    var hasFamilyDiabetes by remember { mutableStateOf(currentFamilyDiabetic) }
    var selectedActivity by remember { mutableStateOf(currentActivity) }
    var activityExpanded by remember { mutableStateOf(false) }

    val activityLevels = listOf("Sedentario", "Ligero", "Moderado", "Intenso", "Muy intenso")

    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = p.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Editar Información Médica", color = p.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = RedNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = birthDate,
                    onValueChange = {
                        birthDate = com.bioguard.movil.data.Formatters.toDisplayDigits(it)
                        validationError = null
                    },
                    visualTransformation = com.bioguard.movil.data.Formatters.dateMaskTransformation,
                    label = { Text("Fecha de nacimiento (dd/mm/aaaa)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.accent, unfocusedBorderColor = p.border)
                )

                val editAge = com.bioguard.movil.data.Formatters.toIsoDate(birthDate)
                    ?.let { com.bioguard.movil.data.Formatters.calculateAge(it) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.accent.copy(alpha = 0.12f))
                        .border(width = 1.dp, color = p.accent, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = editAge?.let { "Edad calculada: $it años" } ?: "Escribe una fecha para calcular la edad",
                        fontSize = 12.sp,
                        color = p.accent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { 
                            weight = it.filter(Char::isDigit).take(3)
                            validationError = null
                        },
                        label = { Text("Peso (kg)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.accent, unfocusedBorderColor = p.border)
                    )

                    OutlinedTextField(
                        value = height,
                        onValueChange = { 
                            height = it.filter(Char::isDigit).take(3)
                            validationError = null
                        },
                        label = { Text("Estatura (cm)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.accent, unfocusedBorderColor = p.border)
                    )
                }

                Text(text = "Sexo Biológico", fontSize = 11.sp, color = p.textSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Masculino", "Femenino").forEach { sexOption ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedSex == sexOption) p.accent else p.surface)
                                .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
                                .clickable { selectedSex = sexOption }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sexOption,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSex == sexOption) p.background else p.textPrimary
                            )
                        }
                    }
                }

                Text(text = "Nivel de Actividad Física", fontSize = 11.sp, color = p.textSecondary)
                ExposedDropdownMenuBox(
                    expanded = activityExpanded,
                    onExpandedChange = { activityExpanded = !activityExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedActivity,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.accent, unfocusedBorderColor = p.border)
                    )
                    ExposedDropdownMenu(
                        expanded = activityExpanded,
                        onDismissRequest = { activityExpanded = false },
                        modifier = Modifier.background(p.surface)
                    ) {
                        activityLevels.forEach { lvl ->
                            DropdownMenuItem(
                                text = { Text(text = lvl, color = p.textPrimary) },
                                onClick = {
                                    selectedActivity = lvl
                                    activityExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDiabetic,
                        onCheckedChange = { isDiabetic = it },
                        colors = CheckboxDefaults.colors(checkedColor = p.accent)
                    )
                    Text(text = "Diagnosticado con Diabetes", fontSize = 12.sp, color = p.textPrimary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasFamilyDiabetes,
                        onCheckedChange = { hasFamilyDiabetes = it },
                        colors = CheckboxDefaults.colors(checkedColor = p.accent)
                    )
                    Text(text = "Antecedentes de Diabetes Familiar", fontSize = 12.sp, color = p.textPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val wNum = weight.toDoubleOrNull()
                    val hNum = height.toDoubleOrNull()

                    if (birthDate.isBlank()) {
                        validationError = "Por favor ingresa tu fecha de nacimiento"
                        return@Button
                    }
                    if (wNum == null || wNum <= 0.0 || wNum > 300.0) {
                        validationError = "Ingresa un peso válido entre 1 y 300 kg"
                        return@Button
                    }
                    if (hNum == null || hNum <= 0.0 || hNum > 250.0) {
                        validationError = "Ingresa una estatura válida entre 1 y 250 cm"
                        return@Button
                    }

                    val isoBirth = com.bioguard.movil.data.Formatters.toIsoDate(birthDate)
                    if (isoBirth == null) {
                        validationError = "Ingresa una fecha válida en formato dd/mm/aaaa"
                        return@Button
                    }
                    val birthAge = com.bioguard.movil.data.Formatters.calculateAge(isoBirth)
                    if (birthAge == null || birthAge <= 0 || birthAge > 120) {
                        validationError = "Ingresa una fecha de nacimiento prudente (1 a 120 años)"
                        return@Button
                    }
                    onSave(isoBirth, com.bioguard.movil.data.Formatters.toSexoCode(selectedSex), weight, height, isDiabetic, hasFamilyDiabetes, selectedActivity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(text = "Guardar", color = p.background, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = p.textSecondary)
            }
        },
        containerColor = p.surface
    )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = text, fontSize = 13.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 10.sp, color = p.textSecondary)
        }
        Text(text = "›", fontSize = 18.sp, color = p.textSecondary)
    }
    Spacer(modifier = Modifier.height(6.dp))
}

private fun resizeBitmap(src: Bitmap, maxDim: Int = 512): Bitmap {
    val width = src.width
    val height = src.height
    if (width <= maxDim && height <= maxDim) return src
    val scale = maxDim.toFloat() / maxOf(width, height)
    val newWidth = (width * scale).toInt().coerceAtLeast(1)
    val newHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(src, newWidth, newHeight, true)
}

private fun encodeToBase64(src: Bitmap): String {
    val stream = ByteArrayOutputStream()
    src.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    val bytes = stream.toByteArray()
    return Base64.getEncoder().encodeToString(bytes)
}

private fun decodeBase64ToBitmap(base64: String): Bitmap? {
    return runCatching {
        val bytes = Base64.getDecoder().decode(base64)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
