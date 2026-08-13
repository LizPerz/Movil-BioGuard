package com.bioguard.movil.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.data.Formatters
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.colorPalette
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Datos validados que emite [PerfilBiometricoForm] al presionar Guardar. */
data class PerfilBiometricoData(
    val birthDateIso: String,
    val sexCode: String,
    val weightKg: Double,
    val heightCm: Double,
    val isDiabetic: Boolean,
    val familyDiabetes: Boolean,
    val activity: String,
    val photoBase64: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilBiometricoForm(
    initialBirthDate: String = "",
    initialSex: String = "M",
    initialWeight: String = "",
    initialHeight: String = "",
    initialDiabetic: Boolean = false,
    initialFamilyDiabetic: Boolean = false,
    initialActivity: String = "Sedentario",
    initialPhoto: String? = null,
    showPhotoPicker: Boolean = true,
    submitText: String = "GUARDAR Y CONTINUAR",
    onCancel: (() -> Unit)? = null,
    isSaving: Boolean = false,
    onSave: (PerfilBiometricoData) -> Unit
) {
    val p = LocalThemeState.current.colorPalette()
    val context = LocalContext.current

    var photoBase64 by remember { mutableStateOf(initialPhoto) }
    var birthDate by remember { mutableStateOf(initialBirthDate) }
    var selectedSex by remember { mutableStateOf(Formatters.sexoToDisplay(initialSex)) }
    var weight by remember { mutableStateOf(initialWeight) }
    var height by remember { mutableStateOf(initialHeight) }
    var selectedActivity by remember { mutableStateOf(initialActivity.ifBlank { "Sedentario" }) }
    var isDiabetic by remember { mutableStateOf(initialDiabetic) }
    var hasFamilyDiabetes by remember { mutableStateOf(initialFamilyDiabetic) }
    var activityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    val computedAge = Formatters.calculateAge(birthDate)

    val activityLevels = listOf("Sedentario", "Ligero", "Moderado", "Intenso", "Muy intenso")

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(input)
                if (original != null) {
                    photoBase64 = encodePhoto(original)
                } else {
                    Toast.makeText(context, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun validarYGuardar() {
        val isoDate = Formatters.toIsoDate(birthDate)
        if (isoDate == null) {
            Toast.makeText(context, "Fecha de nacimiento invalida (dd/mm/aaaa)", Toast.LENGTH_SHORT).show()
            return
        }
        val edad = Formatters.calculateAge(isoDate)
        if (edad == null || edad !in 1..120) {
            Toast.makeText(context, "Fecha de nacimiento no valida (la edad debe estar entre 1 y 120 anos)", Toast.LENGTH_SHORT).show()
            return
        }
        val peso = weight.toDoubleOrNull()
        val estatura = height.toDoubleOrNull()
        if (peso == null || estatura == null || peso !in 1.0..300.0 || estatura !in 30.0..250.0) {
            Toast.makeText(context, "Indica un peso y una estatura validos", Toast.LENGTH_SHORT).show()
            return
        }
        onSave(
            PerfilBiometricoData(
                birthDateIso = isoDate,
                sexCode = Formatters.toSexoCode(selectedSex),
                weightKg = peso,
                heightCm = estatura,
                isDiabetic = isDiabetic,
                familyDiabetes = hasFamilyDiabetes,
                activity = selectedActivity,
                photoBase64 = photoBase64
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        if (showPhotoPicker) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(p.surface)
                            .border(width = 2.dp, color = p.accent, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val bitmap = photoBase64?.let { decodePhoto(it) }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Agregar foto",
                                tint = p.accent,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.accent.copy(alpha = 0.1f))
                            .border(width = 1.dp, color = p.accent, shape = RoundedCornerShape(8.dp))
                            .clickable { galleryLauncher.launch("image/*") }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            tint = p.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (photoBase64 != null) "CAMBIAR FOTO" else "AGREGAR FOTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.accent,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "FECHA DE NACIMIENTO", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = Formatters.toDisplayDigits(it) },
                    visualTransformation = Formatters.dateMaskTransformation,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("dd/mm/aaaa", color = p.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = {
                            val iso = Formatters.toIsoDate(birthDate)
                            if (iso != null) {
                                runCatching {
                                    val millis = LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                    datePickerState.selectedDateMillis = millis
                                }
                            }
                            showDatePicker = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Abrir calendario",
                                tint = p.accent
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = tfColors()
                )
                if (computedAge != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.accent.copy(alpha = 0.12f))
                            .border(width = 1.dp, color = p.accent, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = p.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edad calculada: $computedAge años",
                                fontSize = 12.sp,
                                color = p.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "La edad se calcula automáticamente al escribir la fecha",
                        fontSize = 11.sp,
                        color = p.textTertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "SEXO", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Masculino", "Femenino").forEach { option ->
                        val selected = selectedSex == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) p.accent else p.surface)
                                .border(width = 1.dp, color = if (selected) p.accent else p.border, shape = RoundedCornerShape(10.dp))
                                .clickable { selectedSex = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (option == "Masculino") "M" else "F", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selected) p.background else p.textSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "PESO (KG)", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = sanitizeDecimal(it, maxIntegerDigits = 3, maxDecimalDigits = 2) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("70", color = p.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = tfColors()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "ESTATURA (CM)", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = sanitizeDecimal(it, maxIntegerDigits = 3, maxDecimalDigits = 2) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("175", color = p.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = tfColors()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(text = "NIVEL DE ACTIVIDAD FISICA", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
            OutlinedTextField(
                value = selectedActivity,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                shape = RoundedCornerShape(10.dp),
                colors = tfColors()
            )
            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }, containerColor = p.surface) {
                activityLevels.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level, color = p.textPrimary) },
                        onClick = { selectedActivity = level; activityExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "ANTECEDENTES", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        CheckCard(text = "¿Es diabetico diagnosticado?", checked = isDiabetic, onCheckedChange = { isDiabetic = it })
        Spacer(modifier = Modifier.height(8.dp))
        CheckCard(text = "¿Tiene familiares con diabetes?", checked = hasFamilyDiabetes, onCheckedChange = { hasFamilyDiabetes = it })

        Spacer(modifier = Modifier.height(28.dp))

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            if (!day.isAfter(LocalDate.now())) {
                                birthDate = Formatters.toDisplayDigits(day.toString())
                            }
                        }
                        showDatePicker = false
                    }) {
                        Text("ACEPTAR", color = p.accent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("CANCELAR", color = p.textSecondary)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Button(
            onClick = { validarYGuardar() },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.accent, disabledContainerColor = p.accent.copy(alpha = 0.3f))
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = p.background, strokeWidth = 2.dp)
            } else {
                Text(text = submitText, color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            }
        }

        if (onCancel != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Cancelar",
                fontSize = 12.sp,
                color = p.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCancel() }
                    .padding(8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun encodePhoto(src: Bitmap): String {
    val maxDim = 512
    val max = maxOf(src.width, src.height)
    val scaled = if (max <= maxDim) src else {
        val scale = maxDim.toFloat() / max
        Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
    }
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

private fun decodePhoto(base64: String): Bitmap? {
    return try {
        val data = base64.substringAfter(";base64,").trim()
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

private fun sanitizeDecimal(input: String, maxIntegerDigits: Int, maxDecimalDigits: Int): String {
    val sanitized = input.filter { it.isDigit() || it == '.' }
    val parts = sanitized.split(".")
    val integer = parts.firstOrNull()?.take(maxIntegerDigits) ?: ""
    val decimal = parts.drop(1).firstOrNull()?.take(maxDecimalDigits) ?: ""
    return if (parts.size > 1) "$integer.$decimal" else integer
}
