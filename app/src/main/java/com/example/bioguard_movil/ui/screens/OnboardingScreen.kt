package com.example.bioguard_movil.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.data.Formatters
import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.repository.PacienteRepository
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.AppTheme
import com.example.bioguard_movil.ui.theme.ThemeState
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.components.CheckCard
import com.example.bioguard_movil.ui.components.tfColors
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.colorPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {},
    themeState: ThemeState = ThemeState(),
    onThemeChange: (ThemeState) -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    var step by remember { mutableIntStateOf(1) }

    when (step) {
        1 -> BiometricProfileStep(
            onNext = { step = 2 }
        )
        2 -> AppearanceThemeStep(
            onNext = { step = 3 },
            themeState = themeState,
            onThemeChange = onThemeChange
        )
        3 -> BluetoothPairingStep(
            onComplete = onComplete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricProfileStep(
    onNext: () -> Unit = {}
) {
    var nombre by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf("Masculino") }
    var activityExpanded by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf("Sedentario") }
    var isDiabetic by remember { mutableStateOf(false) }
    var hasFamilyDiabetes by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val p = LocalThemeState.current.colorPalette()
    val activityLevels = listOf("Sedentario", "Ligero", "Moderado", "Intenso", "Muy intenso")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context) }
    val pacienteRepository = remember { PacienteRepository() }

    fun crearPaciente() {
        scope.launch {
            isSaving = true
            val isoDate = Formatters.toIsoDate(birthDate)
            if (isoDate == null) {
                isSaving = false
                Toast.makeText(context, "Fecha de nacimiento invalida (dd/mm/aaaa)", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val peso = weight.toDoubleOrNull()
            val estatura = height.toDoubleOrNull()
            if (peso == null || estatura == null || peso !in 1.0..300.0 || estatura !in 30.0..250.0) {
                isSaving = false
                Toast.makeText(context, "Indica un peso y una estatura validos", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val existingPatientId = prefs.patientId.first() ?: prefs.userId.first()
            if (!existingPatientId.isNullOrEmpty()) {
                // El paciente ya existe en el backend (creado previamente en Web/dueño e ingresado por QR)
                when (val bio = pacienteRepository.updateBiometria(
                    id = existingPatientId,
                    fechaNacimiento = isoDate,
                    sexo = Formatters.toSexoCode(selectedSex),
                    pesoKg = peso,
                    estaturaCm = estatura,
                    esDiabetico = isDiabetic,
                    familiaresDiabetes = hasFamilyDiabetes,
                    actividadFisica = selectedActivity
                )) {
                    is Resource.Success -> {
                        isSaving = false
                        onNext()
                    }
                    is Resource.Error -> {
                        isSaving = false
                        Toast.makeText(context, bio.message, Toast.LENGTH_LONG).show()
                        onNext()
                    }
                    is Resource.Loading -> {}
                }
            } else {
                // Solo si no existe pacienteId previo (flujo dueño), se crea el paciente
                when (val result = pacienteRepository.crearPaciente(nombre.trim(), isDiabetic)) {
                    is Resource.Success -> {
                        val pacienteId = result.data.pacienteId
                        prefs.savePatientId(pacienteId)
                        when (val bio = pacienteRepository.updateBiometria(
                            id = pacienteId,
                            fechaNacimiento = isoDate,
                            sexo = Formatters.toSexoCode(selectedSex),
                            pesoKg = peso,
                            estaturaCm = estatura,
                            esDiabetico = isDiabetic,
                            familiaresDiabetes = hasFamilyDiabetes,
                            actividadFisica = selectedActivity
                        )) {
                            is Resource.Success -> {
                                isSaving = false
                                onNext()
                            }
                            is Resource.Error -> {
                                isSaving = false
                                Toast.makeText(context, bio.message, Toast.LENGTH_LONG).show()
                                onNext()
                            }
                            is Resource.Loading -> {}
                        }
                    }
                    is Resource.Error -> {
                        isSaving = false
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.accent))
                Box(modifier = Modifier.width(28.dp).height(2.dp).background(p.border))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.border))
                Box(modifier = Modifier.width(28.dp).height(2.dp).background(p.border))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.border))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Perfil Biométrico",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Completa tu información para un análisis preciso",
                fontSize = 13.sp,
                color = p.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Text(text = "NOMBRE COMPLETO", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(value = nombre, onValueChange = { nombre = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tu nombre", color = p.textTertiary) }, singleLine = true, shape = RoundedCornerShape(10.dp), colors = tfColors())

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "FECHA DE NACIMIENTO", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(8)
                            birthDate = when {
                                digits.length >= 5 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
                                digits.length >= 3 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
                                else -> digits
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("dd/mm/aaaa", color = p.textTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = tfColors()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "SEXO", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(if (selectedSex == "Masculino") p.accent else p.surface).border(width = 1.dp, color = if (selectedSex == "Masculino") p.accent else p.border, shape = RoundedCornerShape(10.dp)).clickable { selectedSex = "Masculino" }, contentAlignment = Alignment.Center) {
                            Text(text = "M", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selectedSex == "Masculino") p.background else p.textSecondary)
                        }
                        Box(modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(if (selectedSex == "Femenino") p.accent else p.surface).border(width = 1.dp, color = if (selectedSex == "Femenino") p.accent else p.border, shape = RoundedCornerShape(10.dp)).clickable { selectedSex = "Femenino" }, contentAlignment = Alignment.Center) {
                            Text(text = "F", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selectedSex == "Femenino") p.background else p.textSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "PESO (KG)", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("70", color = p.textTertiary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(10.dp), colors = tfColors())
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "ESTATURA (CM)", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(value = height, onValueChange = { height = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("175", color = p.textTertiary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(10.dp), colors = tfColors())
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "NIVEL DE ACTIVIDAD FÍSICA", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
            ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
                OutlinedTextField(value = selectedActivity, onValueChange = {}, modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable), readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) }, shape = RoundedCornerShape(10.dp), colors = tfColors())
                ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }, containerColor = p.surface) {
                    activityLevels.forEach { level -> DropdownMenuItem(text = { Text(level, color = p.textPrimary) }, onClick = { selectedActivity = level; activityExpanded = false }) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "ANTECEDENTES", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
            CheckCard(text = "¿Es diabético diagnosticado?", checked = isDiabetic, onCheckedChange = { isDiabetic = it })
            Spacer(modifier = Modifier.height(8.dp))
            CheckCard(text = "¿Tiene familiares con diabetes?", checked = hasFamilyDiabetes, onCheckedChange = { hasFamilyDiabetes = it })

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { crearPaciente() },
                enabled = nombre.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, disabledContainerColor = p.accent.copy(alpha = 0.3f))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = p.background, strokeWidth = 2.dp)
                } else {
                    Text(text = "CONTINUAR", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AppearanceThemeStep(
    onNext: () -> Unit = {},
    themeState: ThemeState = ThemeState(),
    onThemeChange: (ThemeState) -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    var selectedTheme by remember { mutableStateOf(
        when (themeState.theme) {
            AppTheme.CLARO -> "Claro"
            AppTheme.CYBERPUNK -> "Cyberpunk"
            AppTheme.SALUD -> "Salud"
            else -> "Oscuro"
        }
    ) }
    val themes = listOf("Oscuro", "Claro", "Cyberpunk", "Salud")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.accent))
                Box(modifier = Modifier.width(28.dp).height(2.dp).background(p.accent))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.accent))
                Box(modifier = Modifier.width(28.dp).height(2.dp).background(p.border))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.border))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Selección de Apariencia",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Elige el tema visual de la aplicación",
                fontSize = 13.sp,
                color = p.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            themes.forEach { theme ->
                val isSelected = selectedTheme == theme
                val previewColor = when (theme) {
                    "Oscuro" -> p.background
                    "Claro" -> Color(0xFFF5F5F5)
                    "Cyberpunk" -> Color(0xFF0A0020)
                    "Salud" -> Color(0xFF001A0F)
                    else -> p.background
                }
                val accentColor = when (theme) {
                    "Oscuro" -> p.accent
                    "Claro" -> Color(0xFF00CC9E)
                    "Cyberpunk" -> Color(0xFFFF00FF)
                    "Salud" -> GreenNeon
                    else -> p.accent
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) p.surface else previewColor)
                        .border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) p.accent else p.border, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            selectedTheme = theme
                            val newAppTheme = when (theme) {
                                "Oscuro" -> AppTheme.OSCURO
                                "Claro" -> AppTheme.CLARO
                                "Cyberpunk" -> AppTheme.CYBERPUNK
                                "Salud" -> AppTheme.SALUD
                                else -> AppTheme.OSCURO
                            }
                            onThemeChange(themeState.copy(theme = newAppTheme))
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(accentColor))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = theme, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (theme == "Claro") Color.Black else p.textPrimary)
                        }
                        if (isSelected) {
                            Text(text = "✓", color = p.accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = p.accent)) {
                Text(text = "CONTINUAR", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun BluetoothPairingStep(
    onComplete: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    var isScanning by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.accent))
                Box(modifier = Modifier.width(28.dp).height(2.dp).background(p.accent))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.accent))
                Box(modifier = Modifier.width(28.dp).height(2.dp).background(p.accent))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(p.accent))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Vinculación Bluetooth",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Selecciona tu dispositivo wearable",
                fontSize = 13.sp,
                color = p.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = 0.08f))
                        .border(width = 1.dp, color = p.accent.copy(alpha = 0.2f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = 0.12f))
                        .border(width = 1.dp, color = p.accent.copy(alpha = 0.3f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(p.surface)
                        .border(width = 2.dp, color = p.accent, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⌚", fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "VINCULAR DISPOSITIVO", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(p.surface)
                    .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
                    .clickable { isScanning = !isScanning }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (isScanning) "\u231B" else "\uD83D\uDCF1", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isScanning) "Buscando dispositivos..." else "Toca para buscar dispositivos",
                        fontSize = 14.sp,
                        color = p.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aseg\u00farate de que tu dispositivo wearable est\u00e9 encendido y cerca",
                        fontSize = 11.sp,
                        color = p.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(text = "FINALIZAR", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


