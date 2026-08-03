package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.UsuarioWebResponse
import com.example.bioguard_movil.network.UpdatePerfilRequest
import com.example.bioguard_movil.network.UpdateCorreoRequest
import com.example.bioguard_movil.network.MiPlanResponse
import com.example.bioguard_movil.network.SesionResponse

class UsuarioRepository(
    private val api: ApiService = RetrofitClient.api
) {

    suspend fun getMiPerfil(): Resource<UsuarioWebResponse> {
        return try {
            Resource.Success(api.getMiPerfil())
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener perfil"))
        }
    }

    suspend fun updateMiPerfil(nombre: String?, apellidoPaterno: String?, apellidoMaterno: String?): Resource<String> {
        return try {
            val response = api.updateMiPerfil(UpdatePerfilRequest(nombre = nombre, apellidoPaterno = apellidoPaterno, apellidoMaterno = apellidoMaterno))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al actualizar perfil"))
        }
    }

    suspend fun updateMiCorreo(nuevoCorreo: String, password: String): Resource<String> {
        return try {
            val response = api.updateMiCorreo(UpdateCorreoRequest(nuevoCorreo = nuevoCorreo, password = password))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al actualizar correo"))
        }
    }

    suspend fun getMiPlan(): Resource<MiPlanResponse> {
        return try {
            Resource.Success(api.getMiPlan())
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener plan"))
        }
    }

    suspend fun getMisSesiones(): Resource<List<SesionResponse>> {
        return try {
            Resource.Success(api.getMisSesiones())
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener sesiones"))
        }
    }

    suspend fun deleteSesion(id: String): Resource<String> {
        return try {
            val response = api.deleteSesion(id)
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al eliminar sesion"))
        }
    }

    suspend fun updateFotoPerfil(base64Foto: String): Resource<String> {
        return try {
            val response = api.updateFotoPerfil(com.example.bioguard_movil.network.UpdateFotoRequest(fotoUrl = base64Foto))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al actualizar foto de perfil"))
        }
    }

    suspend fun cancelarPlan(): Resource<String> {
        return try {
            val response = api.cancelarSuscripcion()
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al cancelar plan"))
        }
    }
}
