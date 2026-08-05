package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.CrearAlertaRequest
import com.example.bioguard_movil.network.AtenderAlertaRequest

class AlertaRepository(
    private val api: ApiService = RetrofitClient.api
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
