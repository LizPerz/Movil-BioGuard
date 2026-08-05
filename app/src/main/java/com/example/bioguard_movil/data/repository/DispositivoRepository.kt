package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.InfoCompletaDispositivo
import com.example.bioguard_movil.network.VincularDispositivoRequest

class DispositivoRepository(
    private val api: ApiService = RetrofitClient.api
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
