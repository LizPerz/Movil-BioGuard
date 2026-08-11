package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.CrearPacienteRequest
import com.bioguard.movil.network.CrearPacienteResponse
import com.bioguard.movil.network.ActualizarBiometriaRequest
import kotlinx.coroutines.flow.first

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacienteRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun resolvePatientId(prefs: UserPreferences): String? {
        val cached = prefs.patientId.first()
        if (!cached.isNullOrBlank()) return cached

        return try {
            val miPaciente = api.getMiPaciente()
            if (miPaciente.id.isNotBlank()) {
                prefs.savePatientId(miPaciente.id)
                miPaciente.id
            } else null
        } catch (e: Exception) {
            android.util.Log.w("PacienteRepository", "No se pudo obtener mi-paciente del backend: ${e.message}")
            null
        }
    }

    suspend fun getBiometria(pacienteId: String): Resource<com.bioguard.movil.network.BiometriaResponse> {
        return try {
            Resource.Success(api.getBiometria(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener biometría del paciente"))
        }
    }

    suspend fun crearPaciente(nombre: String, esDiabetico: Boolean): Resource<CrearPacienteResponse> {
        return try {
            Resource.Success(api.crearPaciente(CrearPacienteRequest(nombre = nombre, esDiabetico = esDiabetico)))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al crear paciente"))
        }
    }

    suspend fun updateBiometria(
        id: String,
        fechaNacimiento: String,
        edad: Int = 0,
        sexo: String,
        pesoKg: Double,
        estaturaCm: Double,
        esDiabetico: Boolean,
        familiaresDiabetes: Boolean,
        actividadFisica: String
    ): Resource<String> {
        return try {
            val response = api.updateBiometria(
                id,
                ActualizarBiometriaRequest(
                    fechaNacimiento = fechaNacimiento,
                    edad = edad,
                    sexo = sexo,
                    pesoKg = pesoKg,
                    estaturaCm = estaturaCm,
                    esDiabetico = esDiabetico,
                    familiaresDiabetes = familiaresDiabetes,
                    actividadFisica = actividadFisica
                )
            )
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al actualizar biometria"))
        }
    }
}
