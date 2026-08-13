package com.bioguard.movil.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.data.Formatters
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.UsuarioRepository
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.RetrofitClient
import com.bioguard.movil.ui.components.PerfilBiometricoData
import com.bioguard.movil.ui.components.PerfilBiometricoForm
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.colorPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    var initialBirthDate by remember { mutableStateOf("") }
    var initialSex by remember { mutableStateOf("M") }
    var initialWeight by remember { mutableStateOf("") }
    var initialHeight by remember { mutableStateOf("") }
    var initialActivity by remember { mutableStateOf("") }
    var initialDiabetic by remember { mutableStateOf(false) }
    var initialFamilyDiabetic by remember { mutableStateOf(false) }
    var initialPhoto by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        initialPhoto = prefs.patientPhoto.first()
        initialBirthDate = Formatters.toDisplayDigits(Formatters.toIsoDate(prefs.patientBirthDate.first() ?: "") ?: "")
        initialSex = when (prefs.patientSex.first()) {
            "F" -> "F"
            else -> "M"
        }
        initialWeight = prefs.patientWeight.first() ?: ""
        initialHeight = prefs.patientHeight.first() ?: ""
        initialActivity = prefs.patientActivityLevel.first() ?: ""
        initialDiabetic = prefs.patientIsDiabetic.first()
        initialFamilyDiabetic = prefs.patientFamilyDiabetes.first()
        loaded = true
    }

    fun guardarPerfil(data: PerfilBiometricoData) {
        scope.launch {
            isSaving = true
            val pacienteId = prefs.patientId.first() ?: prefs.userId.first()
            if (!pacienteId.isNullOrBlank()) {
                when (val bio = pacienteRepository.updateBiometria(
                    id = pacienteId,
                    fechaNacimiento = data.birthDateIso,
                    edad = Formatters.calculateAge(data.birthDateIso) ?: 0,
                    sexo = data.sexCode,
                    pesoKg = data.weightKg,
                    estaturaCm = data.heightCm,
                    esDiabetico = data.isDiabetic,
                    familiaresDiabetes = data.familyDiabetes,
                    actividadFisica = data.activity
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

            data.photoBase64?.let {
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
                birthDate = data.birthDateIso,
                sex = data.sexCode,
                weight = data.weightKg.toString(),
                height = data.heightCm.toString(),
                isDiabetic = data.isDiabetic,
                familyDiabetes = data.familyDiabetes,
                activityLevel = data.activity
            )
            data.photoBase64?.let { prefs.savePatientPhoto(it) }

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
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "BIENVENIDO",
                    fontSize = 11.sp,
                    color = p.accent,
                    letterSpacing = 3.sp
                )
                androidx.compose.material3.Text(
                    text = if (firstName.isNotBlank()) "Hola, $firstName" else "Hola",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                androidx.compose.material3.Text(
                    text = "Completa tu perfil biometrico para que la app y la web monitoreen tus signos con precision",
                    fontSize = 13.sp,
                    color = p.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
            }

            if (loaded) {
                PerfilBiometricoForm(
                    initialBirthDate = initialBirthDate,
                    initialSex = initialSex,
                    initialWeight = initialWeight,
                    initialHeight = initialHeight,
                    initialActivity = initialActivity,
                    initialDiabetic = initialDiabetic,
                    initialFamilyDiabetic = initialFamilyDiabetic,
                    initialPhoto = initialPhoto,
                    showPhotoPicker = true,
                    submitText = "GUARDAR Y CONTINUAR",
                    isSaving = isSaving,
                    onSave = { guardarPerfil(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    text = "Omitir por ahora",
                    fontSize = 12.sp,
                    color = p.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable { onSkip() }
                        .padding(8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
