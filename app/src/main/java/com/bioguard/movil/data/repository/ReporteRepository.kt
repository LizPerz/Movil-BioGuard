package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.ReporteResumenResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReporteRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getReporteResumen(pacienteId: String): Resource<ReporteResumenResponse> {
        return try {
            Resource.Success(api.getReporteResumen(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener reporte"))
        }
    }
}
