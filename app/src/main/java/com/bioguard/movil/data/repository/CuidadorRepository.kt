package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.CuidadorResponse
import com.bioguard.movil.network.CrearCuidadorRequest
import com.bioguard.movil.network.CrearCuidadorResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CuidadorRepository @Inject constructor(
    private val api: ApiService
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
            val response = api.updateNivelAcceso(id, com.bioguard.movil.network.UpdateNivelAccesoRequest(nivelAcceso = nivelAcceso))
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
