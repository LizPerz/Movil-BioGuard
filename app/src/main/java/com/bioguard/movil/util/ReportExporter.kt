package com.bioguard.movil.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.bioguard.movil.network.EventoMetabolicoResponse
import com.bioguard.movil.network.LecturaSensorResponse
import java.io.File

object ReportExporter {

    fun exportReadingsCsv(context: Context, lecturas: List<LecturaSensorResponse>): File? {
        return try {
            val fileName = "bioguard_reporte_biometrico_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            file.bufferedWriter().use { writer ->
                writer.write("Timestamp,PulsoBPM,TemperaturaC,SudoracionGSR,HRV,SpO2,NivelRiesgo\n")
                for (l in lecturas) {
                    writer.write("${l.timestamp},${l.pulsoBpm},${l.temperaturaC},${l.sudoracionGsr},${l.hrv ?: 0.0},${l.spo2 ?: 0.0},${l.nivelRiesgo ?: "-"}\n")
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun exportEventsCsv(context: Context, eventos: List<EventoMetabolicoResponse>): File? {
        return try {
            val fileName = "bioguard_historial_eventos_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            file.bufferedWriter().use { writer ->
                writer.write("FechaEvento,NivelRiesgo,ProbabilidadML,Descripcion,Atendida\n")
                for (ev in eventos) {
                    writer.write("${ev.fechaEvento},${ev.nivelRiesgo},${ev.probabilidadMl},\"${ev.descripcion}\",${ev.atendida}\n")
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun shareReportFile(context: Context, file: File, title: String = "Compartir Reporte BioGuard") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
