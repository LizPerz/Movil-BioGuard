package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.LecturaSensorRequest
import com.bioguard.movil.network.LecturaSensorResponse
import com.bioguard.movil.network.CrearEventoRequest
import com.bioguard.movil.network.EventoMetabolicoResponse
import com.bioguard.movil.network.AtenderEventoRequest
import com.bioguard.movil.network.TrackingGpsRequest
import com.bioguard.movil.network.TrackingResponse

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun sendLectura(request: LecturaSensorRequest): Resource<String> {
        return try {
            val response = api.sendLectura(request)
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al enviar lectura"))
        }
    }

    suspend fun sendLecturas(requests: List<LecturaSensorRequest>): Resource<String> {
        return try {
            requests.forEach { api.sendLectura(it) }
            Resource.Success("Lecturas enviadas")
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
            val response = api.atenderEvento(eventoId, AtenderEventoRequest(cuidadorId = cuidadorId))
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
