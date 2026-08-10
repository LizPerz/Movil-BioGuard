package com.bioguard.movil.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.UsuarioRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.RetrofitClient
import com.bioguard.movil.ui.components.CheckCard
import com.bioguard.movil.ui.components.tfColors
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.colorPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private fun resizeBitmap(src: Bitmap, maxDim: Int = 512): Bitmap {
    val max = maxOf(src.width, src.height)
    if (max <= maxDim) return src
    val scale = maxDim.toFloat() / max
    return Bitmap.createScaledBitmap(
        src,
        (src.width * scale).toInt(),
        (src.height * scale).toInt(),
        true
    )
}

private fun encodeToBase64(src: Bitmap): String {
    val out = ByteArrayOutputStream()
    src.compress(Bitmap.CompressFormat.JPEG, 70, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

private fun decodeBase64ToBitmap(base64: String): Bitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    userName: String? = null,
    onContinue: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context) }
    val pacienteRepository = remember { PacienteRepository(RetrofitClient.api) }
    val usuarioRepository = remember { UsuarioRepository(RetrofitClient.api) }

    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var birthDate by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf("Masculino") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf("Sedentario") }
    var isDiabetic by remember { mutableStateOf(false) }
    var hasFamilyDiabetes by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val activityLevels = listOf("Sedentario", "Ligero", "Moderado", "Intenso", "Muy intenso")

    LaunchedEffect(Unit) {
        photoBase64 = prefs.patientPhoto.first()
        birthDate = prefs.patientBirthDate.first() ?: ""
        selectedSex = when (prefs.patientSex.first()) {
            "F" -> "Femenino"
            else -> "Masculino"
        }
        weight = prefs.patientWeight.first() ?: ""
        height = prefs.patientHeight.first() ?: ""
        selectedActivity = prefs.patientActivityLevel.first() ?: "Sedentario"
        isDiabetic = prefs.patientIsDiabetic.first()
        hasFamilyDiabetes = prefs.patientFamilyDiabetes.first()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(input)
                if (original != null) {
                    photoBase64 = encodeToBase64(resizeBitmap(original))
                } else {
                    Toast.makeText(context, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun guardarPerfil() {
        scope.launch {
            val isoDate = Formatters.toIsoDate(birthDate)
            if (isoDate == null) {
                Toast.makeText(context, "Fecha de nacimiento invalida (dd/mm/aaaa)", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val peso = weight.toDoubleOrNull()
            val estatura = height.toDoubleOrNull()
            if (peso == null || estatura == null || peso !in 1.0..300.0 || estatura !in 30.0..250.0) {
                Toast.makeText(context, "Indica un peso y una estatura validos", Toast.LENGTH_SHORT).show()
                return@launch
            }
            isSaving = true

            val pacienteId = prefs.patientId.first() ?: prefs.userId.first()
            if (!pacienteId.isNullOrBlank()) {
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
                    is Resource.Error -> Toast.makeText(
                        context,
                        "Biometria guardada localmente (${bio.message})",
                        Toast.LENGTH_LONG
                    ).show()
                    is Resource.Success -> {}
                    is Resource.Loading -> {}
                }
            }

            photoBase64?.let {
                when (val foto = usuarioRepository.updateFotoPerfil(it)) {
                    is Resource.Error -> Toast.makeText(
                        context,
                        "Foto guardada localmente (${foto.message})",
                        Toast.LENGTH_LONG
                    ).show()
                    is Resource.Success -> {}
                    is Resource.Loading -> {}
                }
            }

            prefs.savePatientBiometrics(
                birthDate = isoDate,
                sex = Formatters.toSexoCode(selectedSex),
                weight = peso.toString(),
                height = estatura.toString(),
                isDiabetic = isDiabetic,
                familyDiabetes = hasFamilyDiabetes,
                activityLevel = selectedActivity
            )
            photoBase64?.let { prefs.savePatientPhoto(it) }

            isSaving = false
            onContinue()
        }
    }

    val firstName = userName?.trim()?.split("\\s+".toRegex())?.firstOrNull().orEmpty()

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

            Text(
                text = "BIENVENIDO",
                fontSize = 11.sp,
                color = p.accent,
                letterSpacing = 3.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (firstName.isNotBlank()) "Hola, $firstName" else "Hola",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Completa tu perfil biometrico para que la app y la web monitoreen tus signos con precision",
                fontSize = 13.sp,
                color = p.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

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
                        val bitmap = photoBase64?.let { decodeBase64ToBitmap(it) }
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

            Text(text = "NIVEL DE ACTIVIDAD FISICA", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
            ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
                OutlinedTextField(value = selectedActivity, onValueChange = {}, modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable), readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) }, shape = RoundedCornerShape(10.dp), colors = tfColors())
                ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }, containerColor = p.surface) {
                    activityLevels.forEach { level -> DropdownMenuItem(text = { Text(level, color = p.textPrimary) }, onClick = { selectedActivity = level; activityExpanded = false }) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "ANTECEDENTES", fontSize = 10.sp, color = p.accent, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
            CheckCard(text = "¿Es diabetico diagnosticado?", checked = isDiabetic, onCheckedChange = { isDiabetic = it })
            Spacer(modifier = Modifier.height(8.dp))
            CheckCard(text = "¿Tiene familiares con diabetes?", checked = hasFamilyDiabetes, onCheckedChange = { hasFamilyDiabetes = it })

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { guardarPerfil() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, disabledContainerColor = p.accent.copy(alpha = 0.3f))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = p.background, strokeWidth = 2.dp)
                } else {
                    Text(text = "GUARDAR Y CONTINUAR", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Omitir por ahora",
                fontSize = 12.sp,
                color = p.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkip() }
                    .padding(8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
