package com.bioguard.movil.domain.usecase

import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.PacienteRepository
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.network.DashboardSummary
import com.bioguard.movil.network.LecturaSensorResponse
import javax.inject.Inject

class GetSensorStatsUseCase @Inject constructor(
    private val pacienteRepository: PacienteRepository
) {
    suspend operator fun invoke(pacienteId: String): Resource<DashboardSummary> {
        if (pacienteId.isBlank()) return Resource.Error("ID de paciente no válido")
        return pacienteRepository.getDashboardSummary(pacienteId)
    }
}

class GetRecentReadingsUseCase @Inject constructor(
    private val sensorRepository: SensorRepository
) {
    suspend operator fun invoke(pacienteId: String, limit: Int = 20): Resource<List<LecturaSensorResponse>> {
        if (pacienteId.isBlank()) return Resource.Error("ID de paciente no válido")
        return sensorRepository.getLecturas(pacienteId, limit)
    }
}
