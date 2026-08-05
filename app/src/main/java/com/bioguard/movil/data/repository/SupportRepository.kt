package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.CrearTicketRequest
import com.bioguard.movil.network.TicketResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun crearTicket(asunto: String, descripcion: String, categoria: String?, prioridad: String?): Resource<String> {
        return try {
            val response = api.crearTicket(CrearTicketRequest(asunto, descripcion, categoria, prioridad))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al crear ticket de soporte"))
        }
    }

    suspend fun listarMisTickets(): Resource<List<TicketResponse>> {
        return try {
            Resource.Success(api.listarMisTickets())
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener tickets de soporte"))
        }
    }
}
