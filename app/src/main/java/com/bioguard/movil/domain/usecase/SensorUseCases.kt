package com.bioguard.movil.domain.usecase

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.network.LecturaSensorResponse
import javax.inject.Inject

class GetRecentReadingsUseCase @Inject constructor(
    private val sensorRepository: SensorRepository
) {
    suspend operator fun invoke(pacienteId: String, limit: Int = 20): Resource<List<LecturaSensorResponse>> {
        if (pacienteId.isBlank()) return Resource.Error("ID de paciente no válido")
        return sensorRepository.getLecturas(pacienteId, limit)
    }
}
