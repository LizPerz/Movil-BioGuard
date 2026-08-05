package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.LecturaSensorRequest
import com.example.bioguard_movil.network.LecturaSensorResponse
import com.example.bioguard_movil.network.CrearEventoRequest
import com.example.bioguard_movil.network.EventoMetabolicoResponse
import com.example.bioguard_movil.network.AtenderEventoRequest
import com.example.bioguard_movil.network.TrackingGpsRequest
import com.example.bioguard_movil.network.TrackingResponse

class SensorRepository(
    private val api: ApiService = RetrofitClient.api
) {

    suspend fun sendLectura(request: LecturaSensorRequest): Resource<String> {
        return try {
            val response = api.sendLecturas(listOf(request))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al enviar lectura"))
        }
    }

    suspend fun sendLecturas(requests: List<LecturaSensorRequest>): Resource<String> {
        return try {
            val response = api.sendLecturas(requests)
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al enviar lecturas"))
        }
    }

    suspend fun getLecturas(pacienteId: String, limite: Int = 100): Resource<List<LecturaSensorResponse>> {
        return try {
            Resource.Success(api.getLecturas(pacienteId, limite))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener lecturas"))
        }
    }

    suspend fun sendEvento(request: CrearEventoRequest): Resource<String> {
        return try {
            val response = api.sendEvento(request)
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al enviar evento"))
        }
    }

    suspend fun getEventos(pacienteId: String, limite: Int = 50): Resource<List<EventoMetabolicoResponse>> {
        return try {
            Resource.Success(api.getEventos(pacienteId, limite))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener eventos"))
        }
    }

    suspend fun atenderEvento(eventoId: String, cuidadorId: String, accion: String): Resource<String> {
        return try {
            val response = api.atenderEvento(eventoId, AtenderEventoRequest(cuidadorId = cuidadorId, notasAtencion = accion))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al atender evento"))
        }
    }

    suspend fun sendTracking(request: TrackingGpsRequest): Resource<String> {
        return try {
            val response = api.sendTracking(request)
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al enviar tracking"))
        }
    }

    suspend fun getTrackingActual(pacienteId: String): Resource<TrackingResponse> {
        return try {
            Resource.Success(api.getTrackingActual(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener ubicacion"))
        }
    }

    suspend fun getTrackingRuta(pacienteId: String, desde: String, hasta: String): Resource<List<TrackingResponse>> {
        return try {
            Resource.Success(api.getTrackingRuta(pacienteId, desde, hasta))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener ruta"))
        }
    }
}
