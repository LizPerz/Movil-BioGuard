package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.CrearTicketRequest
import com.example.bioguard_movil.network.TicketResponse

class SupportRepository(
    private val api: ApiService = RetrofitClient.api
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
