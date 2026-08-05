package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.PrediccionResponse
import com.bioguard.movil.network.DiagnosticarRequest
import com.bioguard.movil.network.DiagnosticarResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getPredicciones(pacienteId: String): Resource<List<PrediccionResponse>> {
        return try {
            Resource.Success(api.getPredicciones(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener predicciones"))
        }
    }

    suspend fun getPrediccionActual(pacienteId: String): Resource<PrediccionResponse> {
        return try {
            Resource.Success(api.getPrediccionActual(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener prediccion actual"))
        }
    }

    suspend fun diagnosticar(pacienteId: String, pulsoBpm: Double, temperaturaC: Double, sudoracionGsr: Double): Resource<DiagnosticarResponse> {
        return try {
            Resource.Success(api.diagnosticar(DiagnosticarRequest(pacienteId = pacienteId, pulsoBpm = pulsoBpm, temperaturaC = temperaturaC, sudoracionGsr = sudoracionGsr)))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al diagnosticar"))
        }
    }
}
