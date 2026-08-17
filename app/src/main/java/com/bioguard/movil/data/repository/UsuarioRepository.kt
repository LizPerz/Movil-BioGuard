package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.UsuarioWebResponse
import com.bioguard.movil.network.UpdatePerfilRequest
import com.bioguard.movil.network.UpdateCorreoRequest
import com.bioguard.movil.network.MiPlanResponse
import com.bioguard.movil.network.SesionResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getMiPerfil(): Resource<UsuarioWebResponse> {
        return try {
            Resource.Success(api.getMiPerfil())
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener perfil"))
        }
    }

    suspend fun getMiAcceso(): Resource<com.bioguard.movil.network.EffectiveAccessResponse> {
        return try {
            Resource.Success(api.getMiAcceso())
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) {
                // Fallback si el endpoint aún no está activo en el servidor desplegado
                Resource.Success(
                    com.bioguard.movil.network.EffectiveAccessResponse(
                        rol = "dueno",
                        pacienteId = null,
                        nivelAccesoCuidador = null,
                        cuidadorDentroDelPlan = true,
                        plan = null,
                        permisos = listOf("account.profile", "patient.read")
                    )
                )
            } else {
                Resource.Error(e.toUserMessage("Error al obtener acceso"))
            }
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener acceso"))
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
            val response = api.updateMiCorreo(UpdateCorreoRequest(nuevoCorreo = nuevoCorreo, passwordActual = password))
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
            val response = api.updateFotoPerfil(com.bioguard.movil.network.UpdateFotoRequest(fotoBase64 = base64Foto))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al actualizar foto de perfil"))
        }
    }

    suspend fun deleteFotoPerfil(): Resource<String> {
        return try {
            val response = api.deleteFotoPerfil()
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al eliminar foto de perfil"))
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
