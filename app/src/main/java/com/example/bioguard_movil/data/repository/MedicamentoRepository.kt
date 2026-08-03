package com.example.bioguard_movil.data.repository

import com.example.bioguard_movil.data.Resource
import com.example.bioguard_movil.data.toUserMessage
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RetrofitClient
import com.example.bioguard_movil.network.MedicamentoResponse
import com.example.bioguard_movil.network.CrearMedicamentoRequest
import com.example.bioguard_movil.network.ActualizarMedicamentoRequest

class MedicamentoRepository(
    private val api: ApiService = RetrofitClient.api
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
            val response = api.crearMedicamento(CrearMedicamentoRequest(pacienteId = pacienteId, nombre = nombre, dosis = dosis, frecuencia = frecuencia, notas = notas))
            Resource.Success(response.message)
        } catch (e: Exception) {
            Resource.Error(e.toUserMessage("Error al crear medicamento"))
        }
    }

    suspend fun actualizarMedicamento(id: String, nombre: String, dosis: String, frecuencia: String, notas: String?): Resource<String> {
        return try {
            val response = api.actualizarMedicamento(id, ActualizarMedicamentoRequest(nombre = nombre, dosis = dosis, frecuencia = frecuencia, notas = notas))
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
