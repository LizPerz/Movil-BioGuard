package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.NotificacionResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificacionRepository @Inject constructor(
    private val api: ApiService
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
