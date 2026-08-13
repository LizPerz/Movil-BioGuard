package com.bioguard.movil.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.bioguard.movil.network.EventoMetabolicoResponse
import com.bioguard.movil.network.ReporteResumenResponse
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportPdfGenerator {

    private const val TAG = "ReportPdfGenerator"

    // A4 en puntos
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 40f

    fun generate(
        context: Context,
        reporte: ReporteResumenResponse,
        eventos: List<EventoMetabolicoResponse>,
        pacienteNombre: String
    ): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "BioGuard_Reporte_$stamp.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawHeader(canvas, pacienteNombre)

        y = drawSectionTitle(canvas, y, "Resumen del periodo")
        y = drawKpis(canvas, y, reporte)

        y += 20f
        y = drawSectionTitle(canvas, y, "Eventos metabolicos")
        y = drawEventosTable(canvas, y, eventos)

        y = Math.max(y, PAGE_HEIGHT - 90f)
        drawFooter(canvas, y)

        doc.finishPage(page)
        runCatching {
            FileOutputStream(file).use { doc.writeTo(it) }
        }.onFailure { e -> Log.e(TAG, "No se pudo escribir el PDF", e) }
        doc.close()
        return file
    }

    private fun drawHeader(canvas: Canvas, pacienteNombre: String): Float {
        var y = MARGIN

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 204, 158)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("BioGuard", MARGIN, y, brandPaint)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 130, 140)
            textSize = 12f
        }
        y += 20f
        canvas.drawText("Reporte de salud", MARGIN, y, subPaint)

        // Línea acento
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 204, 158)
            strokeWidth = 3f
        }
        y += 12f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)

        // Meta del paciente
        y += 26f
        val metaLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 120, 130)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 48, 56)
            textSize = 11f
        }
        val ahora = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale("es", "MX"))
            .format(Date())
        canvas.drawText("Paciente: ", MARGIN, y, metaLabel)
        val nameWidth = metaLabel.measureText("Paciente: ")
        canvas.drawText(pacienteNombre.ifBlank { "Sin nombre" }, MARGIN + nameWidth, y, metaValue)
        y += 18f
        canvas.drawText("Generado: ", MARGIN, y, metaLabel)
        canvas.drawText(ahora, MARGIN + metaLabel.measureText("Generado: "), y, metaValue)
        y += 30f
        return y
    }

    private fun drawSectionTitle(canvas: Canvas, startY: Float, title: String): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 48, 56)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        // Barra lateral acento
        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 204, 158) }
        canvas.drawRect(MARGIN, y - 12f, MARGIN + 4f, y + 3f, bar)
        canvas.drawText(title, MARGIN + 12f, y, paint)
        y += 24f
        return y
    }

    private fun drawKpis(canvas: Canvas, startY: Float, reporte: ReporteResumenResponse): Float {
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 226, 232)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 120, 130)
            textSize = 9f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 48, 56)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        data class Kpi(val label: String, val value: String)

        val kpis = listOf(
            Kpi("Lecturas", "${reporte.totalLecturas}"),
            Kpi("Eventos", "${reporte.totalEventos}"),
            Kpi("Alertas", "${reporte.totalAlertas}"),
            Kpi("Eventos criticos", "${reporte.eventosCriticos}"),
            Kpi("Alertas pendientes", "${reporte.alertasPendientes}"),
            Kpi("Pulso promedio", reporte.promedioPulso?.let { "%.0f BPM".format(it) } ?: "-")
        )

        val gap = 10f
        val totalGap = gap * (kpis.size - 1)
        val cardW = (PAGE_WIDTH - 2 * MARGIN - totalGap) / kpis.size
        val cardH = 58f
        var y = startY

        kpis.forEachIndexed { i, kpi ->
            val left = MARGIN + i * (cardW + gap)
            val top = y
            val right = left + cardW
            val bottom = top + cardH
            canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, cardPaint)
            canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, borderPaint)
            val cx = left + cardW / 2f
            canvas.drawText(kpi.label, cx - labelPaint.measureText(kpi.label) / 2f, top + 20f, labelPaint)
            canvas.drawText(kpi.value, cx - valuePaint.measureText(kpi.value) / 2f, top + 44f, valuePaint)
        }
        y += cardH + 8f
        return y
    }

    private fun drawEventosTable(canvas: Canvas, startY: Float, eventos: List<EventoMetabolicoResponse>): Float {
        var y = startY
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 100, 110)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 48, 56)
            textSize = 10f
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 229, 234)
            strokeWidth = 1f
        }

        val colFecha = 90f
        val colRiesgo = 150f
        val colProb = 120f
        val colEstado = 120f

        val cols = floatArrayOf(MARGIN, colFecha, colRiesgo, colProb, colEstado)

        if (eventos.isEmpty()) {
            canvas.drawText("No hay eventos metabolicos en el periodo.", MARGIN, y, rowPaint)
            y += 24f
            return y
        }

        fun drawHeaderRow() {
            canvas.drawText("Fecha", cols[0] + 4f, y, headerPaint)
            canvas.drawText("Nivel", cols[1] + 4f, y, headerPaint)
            canvas.drawText("Probabilidad", cols[2] + 4f, y, headerPaint)
            canvas.drawText("Estado", cols[3] + 4f, y, headerPaint)
            y += 14f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 6f
        }

        drawHeaderRow()

        eventos.take(12).forEach { e ->
            if (y > PAGE_HEIGHT - 120f) {
                // Nueva página automática del documento
                return y
            }
            val fecha = e.fechaEvento?.substringBefore("T") ?: "-"
            val riesgo = e.nivelRiesgo ?: "-"
            val prob = "%.0f%%".format(e.probabilidadMl * 100)
            val estado = if (e.atendida) "Atendido" else "Pendiente"
            canvas.drawText(fecha, cols[0] + 4f, y, rowPaint)
            canvas.drawText(riesgo, cols[1] + 4f, y, rowPaint)
            canvas.drawText(prob, cols[2] + 4f, y, rowPaint)
            canvas.drawText(estado, cols[3] + 4f, y, rowPaint)
            y += 16f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 4f
        }
        y += 12f
        return y
    }

    private fun drawFooter(canvas: Canvas, startY: Float) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(140, 148, 156)
            textSize = 9f
        }
        canvas.drawText(
            "Generado por BioGuard · Documento informativo de monitoreo, no sustituye un diagnostico medico.",
            MARGIN,
            startY,
            footerPaint
        )
    }
}
