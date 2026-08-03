package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.CuidadorResponse
import com.example.bioguard_movil.network.CrearCuidadorRequest
import com.example.bioguard_movil.network.CrearCuidadorResponse

class CuidadorRepository(
    private val api: ApiService = RetrofitClient.api
) {

    suspend fun getCuidadores(): Resource<List<CuidadorResponse>> {
        return try {
            Resource.Success(api.getCuidadores())
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener cuidadores"))
        }
    }

    suspend fun getCuidadorById(id: String): Resource<CuidadorResponse> {
        return try {
            Resource.Success(api.getCuidadorById(id))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener cuidador"))
        }
    }

    suspend fun crearCuidador(
        pacienteId: String,
        nombre: String,
        parentesco: String,
        telefono: String,
        correo: String,
        nivelAcceso: String
    ): Resource<CrearCuidadorResponse> {
        return try {
            Resource.Success(
                api.crearCuidador(
                    CrearCuidadorRequest(
                        pacienteId = pacienteId,
                        nombre = nombre,
                        parentesco = parentesco,
                        telefono = telefono,
                        correo = correo,
                        nivelAcceso = nivelAcceso
                    )
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al crear cuidador"))
        }
    }

    suspend fun updateNivelAcceso(id: String, nivelAcceso: String): Resource<String> {
        return try {
            val response = api.updateNivelAcceso(id, com.example.bioguard_movil.network.UpdateNivelAccesoRequest(nivelAcceso = nivelAcceso))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al actualizar nivel de acceso"))
        }
    }

    suspend fun deleteCuidador(id: String): Resource<String> {
        return try {
            api.deleteCuidador(id)
            Resource.Success("Cuidador eliminado")
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al eliminar cuidador"))
        }
    }
}
