package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.NotificacionResponse

class NotificacionRepository(
    private val api: ApiService = RetrofitClient.api
) {

    suspend fun getNotificaciones(pacienteId: String): Resource<List<NotificacionResponse>> {
        return try {
            Resource.Success(api.getNotificaciones(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener notificaciones"))
        }
    }

    suspend fun markNotificacionLeida(id: String): Resource<String> {
        return try {
            val response = api.markNotificacionLeida(id)
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al marcar notificacion"))
        }
    }
}
