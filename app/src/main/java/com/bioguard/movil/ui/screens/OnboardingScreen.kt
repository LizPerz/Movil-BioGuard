package com.bioguard.movil.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bioguard.movil.data.Formatters
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.AppTheme
import com.bioguard.movil.ui.theme.ThemeState
import com.bioguard.movil.ui.theme.YellowNeon
import com.bioguard.movil.ui.components.CheckCard
import com.bioguard.movil.ui.components.tfColors
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.colorPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings

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
        1 -> AppearanceThemeStep(
            onNext = { step = 2 },
            themeState = themeState,
            onThemeChange = onThemeChange
        )
        2 -> BluetoothPairingStep(
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
    val pacienteRepository = remember { PacienteRepository(com.bioguard.movil.network.RetrofitClient.api) }

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
            OutlinedTextField(value = nombre, onValueChange = { nombre = it.take(120) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tu nombre", color = p.textTertiary) }, singleLine = true, shape = RoundedCornerShape(10.dp), colors = tfColors())

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "FECHA DE NACIMIENTO", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = Formatters.formatDateInput(it) },
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
                    OutlinedTextField(value = weight, onValueChange = { weight = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("70", color = p.textTertiary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(10.dp), colors = tfColors())
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "ESTATURA (CM)", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(value = height, onValueChange = { height = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("175", color = p.textTertiary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(10.dp), colors = tfColors())
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context) }

    var isScanning by remember { mutableStateOf(false) }
    var pairedDeviceName by remember { mutableStateOf<String?>(null) }
    var discoveredDevices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var hasPermissions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        hasPermissions = perms.all { perm ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, perm
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun triggerScan() {
        if (!hasPermissions) {
            Toast.makeText(context, "Se requieren permisos de Bluetooth. Activalos en Ajustes > Aplicaciones > BioGuard > Permisos.", Toast.LENGTH_LONG).show()
            return
        }
        scope.launch {
            isScanning = true
            discoveredDevices = emptyList()

            val realDevices = mutableListOf<Pair<String, String>>()
            try {
                val nodeClient = com.google.android.gms.wearable.Wearable.getNodeClient(context)
                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    realDevices.add((node.displayName.ifBlank { "SmartWatch WearOS" }) to node.id)
                }
            } catch (e: Exception) {
                android.util.Log.w("Onboarding", "WearOS node discovery: ${e.message}")
            }

            try {
                val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                val adapter = bluetoothManager?.adapter
                if (adapter != null && adapter.isEnabled) {
                    val scanner = adapter.bluetoothLeScanner
                    if (scanner != null) {
                        val bleDevices = mutableListOf<Pair<String, String>>()
                        val scanCallback = object : android.bluetooth.le.ScanCallback() {
                            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                                result?.device?.let { dev ->
                                    val name = try { dev.name } catch (_: SecurityException) { null }
                                    if (name != null && bleDevices.none { it.second == dev.address }) {
                                        bleDevices.add(name to dev.address)
                                        discoveredDevices = realDevices + bleDevices
                                    }
                                }
                            }
                            override fun onScanFailed(errorCode: Int) {
                                android.util.Log.w("Onboarding", "BLE scan failed: $errorCode")
                            }
                        }
                        try {
                            scanner.startScan(scanCallback)
                            kotlinx.coroutines.delay(8000)
                            scanner.stopScan(scanCallback)
                        } catch (e: SecurityException) {
                            android.util.Log.w("Onboarding", "BLE scan permission error: ${e.message}")
                        }
                        discoveredDevices = realDevices + bleDevices
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("Onboarding", "BLE scan error: ${e.message}")
            }

            isScanning = false

            if (discoveredDevices.isEmpty()) {
                Toast.makeText(context, "No se detectaron dispositivos wearables físicos encendidos ni cercanos. Asegúrate de tener Bluetooth activado.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun vincular(nombre: String, mac: String) {
        scope.launch {
            prefs.saveDeviceData(mac, nombre, isConnected = true)
            pairedDeviceName = nombre
            Toast.makeText(context, "Dispositivo '$nombre' vinculado correctamente", Toast.LENGTH_SHORT).show()
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

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Vinculación Wearable",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Conecta tu smartwatch o parche biométrico",
                fontSize = 13.sp,
                color = p.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = 0.08f))
                        .border(width = 1.dp, color = p.accent.copy(alpha = 0.2f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = 0.12f))
                        .border(width = 1.dp, color = p.accent.copy(alpha = 0.3f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(p.surface)
                        .border(width = 2.dp, color = if (pairedDeviceName != null) GreenNeon else p.accent, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (pairedDeviceName != null) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Vinculado",
                            tint = GreenNeon,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Watch,
                            contentDescription = "Wearable",
                            tint = p.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (pairedDeviceName != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenNeon.copy(alpha = 0.1f))
                        .border(width = 1.dp, color = GreenNeon, shape = RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "¡Dispositivo Vinculado!", fontWeight = FontWeight.Bold, color = GreenNeon, fontSize = 15.sp)
                        Text(text = pairedDeviceName ?: "", color = p.textPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.surface)
                        .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
                        .clickable { if (!isScanning) triggerScan() }
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isScanning) {
                            CircularProgressIndicator(color = p.accent, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Escaneando dispositivos cercanos...", fontSize = 13.sp, color = p.textPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Smartphone,
                                contentDescription = "Dispositivo",
                                tint = p.accent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Toca para buscar dispositivos cercanos",
                                fontSize = 14.sp,
                                color = p.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Asegúrate de tener Bluetooth activado",
                                fontSize = 11.sp,
                                color = p.textSecondary
                            )
                        }
                    }
                }
            }

            if (discoveredDevices.isNotEmpty() && pairedDeviceName == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "DISPOSITIVOS DISPONIBLES", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                discoveredDevices.forEach { (nombre, mac) ->
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
                        Column {
                            Text(text = nombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = p.textPrimary)
                            Text(text = mac, fontSize = 10.sp, color = p.textSecondary)
                        }
                        Button(
                            onClick = { vincular(nombre, mac) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                        ) {
                            Text(text = "Vincular", fontSize = 11.sp, color = p.background, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(text = "FINALIZAR E IR AL INICIO", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Omitir por ahora (puedes vincularlo luego en Dispositivo)",
                fontSize = 12.sp,
                color = p.textSecondary,
                modifier = Modifier
                    .clickable { onComplete() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

