package com.bioguard.movil.data.repository

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.toUserMessage
import com.bioguard.movil.network.ApiService
import com.bioguard.movil.network.MedicamentoResponse
import com.bioguard.movil.network.CrearMedicamentoRequest
import com.bioguard.movil.network.ActualizarMedicamentoRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicamentoRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getMedicamentos(pacienteId: String): Resource<List<MedicamentoResponse>> {
        return try {
            Resource.Success(api.getMedicamentos(pacienteId))
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al obtener medicamentos"))
        }
    }

    suspend fun crearMedicamento(pacienteId: String, nombre: String, dosis: String, frecuencia: String, notas: String?): Resource<String> {
        return try {
            val response = api.crearMedicamento(CrearMedicamentoRequest(pacienteId = pacienteId, nombre = nombre, dosis = dosis, horario = frecuencia, notas = notas))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al crear medicamento"))
        }
    }

    suspend fun actualizarMedicamento(id: String, nombre: String, dosis: String, frecuencia: String, notas: String?): Resource<String> {
        return try {
            val response = api.actualizarMedicamento(id, ActualizarMedicamentoRequest(nombre = nombre, dosis = dosis, horario = frecuencia, notas = notas))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al actualizar medicamento"))
        }
    }

    suspend fun registrarToma(id: String): Resource<String> {
        return try {
            val response = api.registrarToma(id)
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al registrar toma"))
        }
    }

    suspend fun deleteMedicamento(id: String): Resource<String> {
        return try {
            api.deleteMedicamento(id)
            Resource.Success("Medicamento eliminado")
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al eliminar medicamento"))
        }
    }
}
