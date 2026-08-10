package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.AlertaResponse
import com.bioguard.movil.network.CrearAlertaRequest
import com.bioguard.movil.network.AtenderAlertaRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertaRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun crearAlerta(pacienteId: String, tipoAlerta: String, nivel: String, titulo: String, mensaje: String): Resource<String> {
        return try {
            val response = api.crearAlerta(
                CrearAlertaRequest(
                    pacienteId = pacienteId,
                    tipo = tipoAlerta,
                    nivel = nivel,
                    titulo = titulo,
                    mensaje = mensaje
                )
            )
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al crear alerta"))
        }
    }

    suspend fun getAlertas(pacienteId: String): Resource<List<AlertaResponse>> {
        return try {
            Resource.Success(api.getAlertasByPaciente(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener alertas"))
        }
    }

    suspend fun atenderAlerta(id: String, cuidadorId: String, accionTomada: String?): Resource<String> {
        return try {
            val response = api.atenderAlerta(id, AtenderAlertaRequest(cuidadorId = cuidadorId, accionTomada = accionTomada))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al atender alerta"))
        }
    }
}
