package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.PrediccionResponse
import com.example.bioguard_movil.network.DiagnosticarRequest
import com.example.bioguard_movil.network.DiagnosticarResponse

class MlRepository(
    private val api: ApiService = RetrofitClient.api
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
