package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.ReporteResumenResponse

class ReporteRepository(
    private val api: ApiService = RetrofitClient.api
) {

    suspend fun getReporteResumen(pacienteId: String): Resource<ReporteResumenResponse> {
        return try {
            Resource.Success(api.getReporteResumen(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener reporte"))
        }
    }
}
