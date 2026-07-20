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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.AppTheme
import com.example.bioguard_movil.ui.theme.ThemeState
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.colorPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {},
    themeState: ThemeState = ThemeState(),
    onThemeChange: (ThemeState) -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    var step by remember { mutableStateOf(1) }

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

    val p = LocalThemeState.current.colorPalette()
    val activityLevels = listOf("Sedentario", "Ligero", "Moderado", "Intenso", "Muy intenso")

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
                    OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("dd/mm/aaaa", color = p.textTertiary) }, singleLine = true, shape = RoundedCornerShape(10.dp), colors = tfColors())
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
                OutlinedTextField(value = selectedActivity, onValueChange = {}, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) }, shape = RoundedCornerShape(10.dp), colors = tfColors())
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

            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = p.accent)) {
                Text(text = "CONTINUAR", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
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
    var selectedTheme by remember { mutableStateOf("Oscuro") }
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
    val discoveredDevices = listOf(
        "Galaxy Watch 6",
        "Galaxy Watch 5",
        "BioGuard Sensor v2"
    )
    var selectedDevice by remember { mutableStateOf("") }

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

            Text(text = "DISPOSITIVOS ENCONTRADOS", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))

            discoveredDevices.forEach { device ->
                val isSelected = selectedDevice == device
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) p.accent.copy(alpha = 0.1f) else p.surface)
                        .border(width = 1.dp, color = if (isSelected) p.accent else p.border, shape = RoundedCornerShape(10.dp))
                        .clickable { selectedDevice = device }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⌚", fontSize = 18.sp, color = p.accent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = device, fontSize = 14.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
                        }
                        if (isSelected) {
                            Text(text = "✓", color = p.accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)),
                enabled = selectedDevice.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, disabledContainerColor = p.accent.copy(alpha = 0.3f))
            ) {
                Text(text = "FINALIZAR Y VINCULAR", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CheckCard(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = p.accent, uncheckedColor = p.border))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 13.sp, color = p.textPrimary)
        }
    }
}

@Composable
fun tfColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LocalThemeState.current.colorPalette().accent,
    unfocusedBorderColor = LocalThemeState.current.colorPalette().border,
    focusedContainerColor = LocalThemeState.current.colorPalette().inputBackground,
    unfocusedContainerColor = LocalThemeState.current.colorPalette().inputBackground,
    focusedTextColor = LocalThemeState.current.colorPalette().textPrimary,
    unfocusedTextColor = LocalThemeState.current.colorPalette().textPrimary,
    cursorColor = LocalThemeState.current.colorPalette().accent
)
