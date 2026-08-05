package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.InfoCompletaDispositivo
import com.bioguard.movil.network.VincularDispositivoRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DispositivoRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun vincularDispositivo(nombreDispositivo: String, macAddress: String): Resource<String> {
        return try {
            val response = api.vincularDispositivo(VincularDispositivoRequest(nombre = nombreDispositivo, macAddress = macAddress))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al vincular dispositivo"))
        }
    }

    suspend fun getInfoCompleta(pacienteId: String): Resource<InfoCompletaDispositivo> {
        return try {
            Resource.Success(api.getInfoCompleta(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener dispositivo"))
        }
    }
}
