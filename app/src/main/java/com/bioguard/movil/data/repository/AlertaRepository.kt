package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.AtenderAlertaRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertaRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun crearAlerta(pacienteId: String, tipoAlerta: String, descripcion: String, latitud: Double?, longitud: Double?): Resource<String> {
        return try {
            val response = api.crearAlerta(CrearAlertaRequest(pacienteId = pacienteId, tipoAlerta = tipoAlerta, descripcion = descripcion, latitud = latitud, longitud = longitud))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al crear alerta"))
        }
    }

    suspend fun atenderAlerta(id: String, notasAtencion: String): Resource<String> {
        return try {
            val response = api.atenderAlerta(id, AtenderAlertaRequest(notasAtencion = notasAtencion))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al atender alerta"))
        }
    }
}
