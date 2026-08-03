package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.datastore.UserPreferences
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.CrearPacienteRequest
import com.example.bioguard_movil.network.CrearPacienteResponse
import com.example.bioguard_movil.network.ActualizarBiometriaRequest
import com.example.bioguard_movil.network.DashboardSummary
import kotlinx.coroutines.flow.first

class PacienteRepository(
    private val api: ApiService = RetrofitClient.api
) {

    suspend fun resolvePatientId(prefs: UserPreferences): String? {
        return prefs.patientId.first()
    }

    suspend fun crearPaciente(nombre: String, esDiabetico: Boolean): Resource<CrearPacienteResponse> {
        return try {
            Resource.Success(api.crearPaciente(CrearPacienteRequest(nombre = nombre, esDiabetico = esDiabetico)))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al crear paciente"))
        }
    }

    suspend fun getDashboardSummary(pacienteId: String): Resource<DashboardSummary> {
        return try {
            Resource.Success(api.getDashboardSummary(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener resumen del paciente"))
        }
    }

    suspend fun updateBiometria(
        id: String,
        fechaNacimiento: String,
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
