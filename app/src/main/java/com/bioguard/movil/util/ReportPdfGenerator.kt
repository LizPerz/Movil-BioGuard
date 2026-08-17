package com.bioguard.movil.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.bioguard.movil.network.EventoMetabolicoResponse
import com.bioguard.movil.network.LecturaSensorResponse
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
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val FOOTER_HEIGHT = 52f

    fun generate(
        context: Context,
        reporte: ReporteResumenResponse,
        eventos: List<EventoMetabolicoResponse>,
        lecturas: List<LecturaSensorResponse>,
        pacienteNombre: String,
        pacienteId: String? = null
    ): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "BioGuard_Reporte_$stamp.pdf")

        val doc = PdfDocument()
        val w = PageManager(doc, pacienteNombre, pacienteId)
        w.start()

        w.sectionTitle("Resumen del periodo")
        w.drawKpis(reporte, lecturas)

        w.spacing(14f)
        w.sectionTitle("Ultimas lecturas")
        w.drawLecturas(lecturas)

        w.spacing(14f)
        w.sectionTitle("Eventos metabolicos")
        w.drawEventos(eventos)

        w.finish()

        runCatching {
            FileOutputStream(file).use { doc.writeTo(it) }
        }.onFailure { e -> Log.e(TAG, "No se pudo escribir el PDF", e) }
        doc.close()
        return file
    }

    private enum class Align { LEFT, RIGHT }

    private class TableColumn(val title: String, val width: Float, val align: Align = Align.LEFT)

    private class PageManager(
        private val doc: PdfDocument,
        private val pacienteNombre: String,
        private val pacienteId: String?
    ) {
        private val brandGreen = Color.rgb(0, 204, 158)
        private val ink = Color.rgb(34, 40, 49)
        private val gray = Color.rgb(120, 130, 140)
        private val borderGray = Color.rgb(220, 226, 232)
        private val headerFill = Color.rgb(240, 245, 243)
        private val red = Color.rgb(239, 68, 68)
        private val orange = Color.rgb(245, 158, 11)
        private val greenOk = Color.rgb(34, 197, 94)

        private var page: PdfDocument.Page? = null
        private val canvas: Canvas get() = page!!.canvas
        var y: Float = 0f
            private set
        private var pageNumber = 0

        private val fechaHora: String =
            SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale("es", "MX")).format(Date())

        private val bottomLimit: Float get() = PAGE_HEIGHT - FOOTER_HEIGHT - 8f

        // ---- Paints (reutilizables entre páginas) ----
        private val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen; textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val accentLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen; strokeWidth = 3f
        }
        private val metaLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val metaValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 10f
        }
        private val sectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val sectionBar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = brandGreen }
        private val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; style = Paint.Style.FILL
        }
        private val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderGray; style = Paint.Style.STROKE; strokeWidth = 1.2f
        }
        private val cardLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 9f
        }
        private val cardValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val tableHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 9.5f }
        private val rowPaintGray = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gray; textSize = 9.5f }
        private val tableHeaderLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen; strokeWidth = 1.5f
        }
        private val separator = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderGray; strokeWidth = 0.8f
        }
        private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 8.5f
        }
        private val compactHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        private val riskPaints: Map<String?, Paint> = mapOf(
            "Critico" to boldPaint(red),
            "Alta" to boldPaint(red),
            "Pre-Pico" to boldPaint(orange),
            "Media" to boldPaint(orange),
            "Normal" to boldPaint(greenOk),
            null to boldPaint(gray)
        )

        private fun boldPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // ---- Gestión de página ----
        fun start() {
            startNewPage(showFullHeader = true)
        }

        fun finish() {
            drawFooter()
            doc.finishPage(page)
            page = null
        }

        fun ensureSpace(height: Float) {
            if (page != null && y + height > bottomLimit) {
                drawFooter()
                doc.finishPage(page)
                page = null
            }
            if (page == null) {
                startNewPage(showFullHeader = pageNumber == 0)
            }
        }

        private fun startNewPage(showFullHeader: Boolean) {
            pageNumber++
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create()
            )
            y = MARGIN
            if (showFullHeader) drawHeader() else drawCompactHeader()
        }

        // ---- Header / footer ----
        private fun drawHeader() {
            val nombre = pacienteNombre.ifBlank { "Sin nombre" }
            val nombreLimpio = if (nombre.length > 48) nombre.take(45) + "..." else nombre

            canvas.drawText("BioGuard", MARGIN, y, brandPaint)
            y += 21f
            canvas.drawText("Reporte de salud", MARGIN, y, titlePaint)
            y += 10f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, accentLine)

            y += 24f
            canvas.drawText("Paciente: ", MARGIN, y, metaLabel)
            var x = MARGIN + metaLabel.measureText("Paciente: ")
            canvas.drawText(nombreLimpio, x, y, metaValue)
            x += metaValue.measureText(nombreLimpio) + 24f
            if (!pacienteId.isNullOrBlank()) {
                canvas.drawText("ID: ", x, y, metaLabel)
                val idLabel = pacienteId.let { id -> if (id.length > 24) id.take(21) + "..." else id }
                canvas.drawText(idLabel, x + metaLabel.measureText("ID: "), y, metaValue)
            }
            y += 17f
            canvas.drawText("Generado: ", MARGIN, y, metaLabel)
            canvas.drawText(fechaHora, MARGIN + metaLabel.measureText("Generado: "), y, metaValue)
            y += 26f
        }

        private fun drawCompactHeader() {
            canvas.drawText("BioGuard · Reporte de salud", MARGIN, y, compactHeader)
            canvas.drawLine(MARGIN, y + 6f, PAGE_WIDTH - MARGIN, y + 6f, tableHeaderLine)
            y += 24f
        }

        private fun drawFooter() {
            val lineY = PAGE_HEIGHT - 44f
            val textY = PAGE_HEIGHT - 26f
            canvas.drawLine(MARGIN, lineY, PAGE_WIDTH - MARGIN, lineY, separator)
            canvas.drawText(
                "Generado por BioGuard · Documento informativo de monitoreo, no sustituye un diagnostico medico.",
                MARGIN, textY, footerPaint
            )
            val pageTxt = "Pagina $pageNumber"
            canvas.drawText(pageTxt, PAGE_WIDTH - MARGIN - footerPaint.measureText(pageTxt), textY, footerPaint)
        }

        // ---- Secciones ----
        fun spacing(amount: Float) {
            y += amount
        }

        fun sectionTitle(title: String) {
            ensureSpace(30f)
            canvas.drawRect(MARGIN, y - 12f, MARGIN + 4f, y + 3f, sectionBar)
            canvas.drawText(title, MARGIN + 12f, y, sectionTitle)
            y += 24f
        }

        fun drawKpis(reporte: ReporteResumenResponse, lecturas: List<LecturaSensorResponse>) {
            data class Kpi(val label: String, val value: String)

            val avgTemp = lecturas.map { it.temperaturaC }.averageOrNull()
            val avgEstres = lecturas.map { it.estresPct }.averageOrNull()
            val maxRiesgo = lecturas.mapNotNull { it.nivelRiesgo }
                .let { niveles ->
                    when {
                        niveles.any { it.equals("Critico", ignoreCase = true) } -> "Critico"
                        niveles.any { it.equals("Alta", ignoreCase = true) } -> "Alta"
                        niveles.any { it.equals("Pre-Pico", ignoreCase = true) || it.equals("Media", ignoreCase = true) } -> "Moderado"
                        niveles.any { it.equals("Normal", ignoreCase = true) } -> "Normal"
                        else -> null
                    }
                }
            val maxRiesgoLabel = when (maxRiesgo) {
                "Critico" -> "Critico"
                "Alta" -> "Alta"
                "Moderado" -> "Atencion"
                "Normal" -> "Estable"
                else -> "Sin datos"
            }

            val kpis = listOf(
                Kpi("Lecturas registradas", "${reporte.totalLecturas}"),
                Kpi("Eventos metabolicos", "${reporte.totalEventos}"),
                Kpi("Alertas", "${reporte.totalAlertas}"),
                Kpi("Eventos criticos", "${reporte.eventosCriticos}"),
                Kpi("Alertas pendientes", "${reporte.alertasPendientes}"),
                Kpi("Pulso promedio", reporte.promedioPulso?.let { "%.0f BPM".format(it) } ?: "-"),
                Kpi("Temperatura promedio", avgTemp?.let { "%.1f\u00b0C".format(it) } ?: "-"),
                Kpi("Estres promedio", avgEstres?.let { "%.0f%%".format(it) } ?: "-"),
                Kpi("Riesgo maximo", maxRiesgoLabel)
            )

            val gap = 12f
            val cardW = (CONTENT_WIDTH - 2 * gap) / 3f
            val cardH = 54f

            kpis.chunked(3).forEach { row ->
                ensureSpace(cardH + gap)
                row.forEachIndexed { i, kpi ->
                    val left = MARGIN + i * (cardW + gap)
                    val top = y
                    val right = left + cardW
                    val bottom = top + cardH
                    canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, cardFill)
                    canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, cardBorder)
                    canvas.drawText(kpi.label, left + 12f, top + 18f, cardLabel)
                    canvas.drawText(kpi.value, left + 12f, top + 40f, cardValue)
                }
                y += cardH + gap
            }
            y += 2f
        }

        fun drawLecturas(lecturas: List<LecturaSensorResponse>) {
            val cols = listOf(
                TableColumn("Fecha / hora", 138f),
                TableColumn("Pulso", 86f, Align.RIGHT),
                TableColumn("Temp", 90f, Align.RIGHT),
                TableColumn("Estres", 88f, Align.RIGHT),
                TableColumn("Riesgo", 113f)
            )
            if (lecturas.isEmpty()) {
                ensureSpace(22f)
                canvas.drawText("No hay lecturas registradas en el periodo.", MARGIN, y, rowPaintGray)
                y += 22f
                return
            }
            drawTableHeader(cols)
            lecturas.take(15).forEach { l ->
                val rowH = 18f
                ensureSpace(rowH + 1f)
                val base = y
                val fecha = (l.timestamp.substringBefore("T") + " " + l.timestamp.substringAfter("T").take(5)).trim()
                canvas.drawText(fecha, colLeft(cols, 0) + 4f, base, rowPaint)
                canvas.drawText("%.0f".format(l.pulsoBpm), rightEdge(cols, 1) - rowPaint.measureText("%.0f".format(l.pulsoBpm)), base, rowPaint)
                canvas.drawText("%.1f°C".format(l.temperaturaC), rightEdge(cols, 2) - rowPaint.measureText("%.1f°C".format(l.temperaturaC)), base, rowPaint)
                canvas.drawText("%.0f%%".format(l.estresPct), rightEdge(cols, 3) - rowPaint.measureText("%.0f%%".format(l.estresPct)), base, rowPaint)
                canvas.drawText(l.nivelRiesgo ?: "-", colLeft(cols, 4) + 4f, base, riskPaints[l.nivelRiesgo] ?: rowPaint)
                y += rowH
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, separator)
            }
            y += 8f
        }

        fun drawEventos(eventos: List<EventoMetabolicoResponse>) {
            val cols = listOf(
                TableColumn("Fecha", 138f),
                TableColumn("Nivel", 120f),
                TableColumn("Probabilidad", 130f, Align.RIGHT),
                TableColumn("Estado", 127f)
            )
            if (eventos.isEmpty()) {
                ensureSpace(22f)
                canvas.drawText("No hay eventos metabolicos en el periodo.", MARGIN, y, rowPaintGray)
                y += 22f
                return
            }
            drawTableHeader(cols)
            eventos.take(15).forEach { e ->
                val rowH = 18f
                ensureSpace(rowH + 1f)
                val base = y
                canvas.drawText((e.fechaEvento ?: "-").substringBefore("T"), colLeft(cols, 0) + 4f, base, rowPaint)
                canvas.drawText(e.nivelRiesgo ?: "-", colLeft(cols, 1) + 4f, base, riskPaints[e.nivelRiesgo] ?: rowPaint)
                val prob = "%.0f%%".format(e.probabilidadMl * 100)
                canvas.drawText(prob, rightEdge(cols, 2) - rowPaint.measureText(prob), base, rowPaint)
                val estado = if (e.atendida) "Atendido" else "Pendiente"
                val estadoPaint = if (e.atendida) (riskPaints["Normal"] ?: boldPaint(greenOk)) else boldPaint(orange)
                canvas.drawText(estado, colLeft(cols, 3) + 4f, base, estadoPaint)
                y += rowH
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, separator)
            }
            y += 8f
        }

        private fun drawTableHeader(cols: List<TableColumn>) {
            ensureSpace(28f)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 17f, headerFillPaint())
            cols.forEachIndexed { i, col ->
                val x = if (col.align == Align.RIGHT) {
                    rightEdge(cols, i) - tableHeader.measureText(col.title)
                } else {
                    colLeft(cols, i) + 4f
                }
                canvas.drawText(col.title, x, y + 12f, tableHeader)
            }
            y += 17f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, tableHeaderLine)
            y += 7f
        }

        private fun headerFillPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = headerFill
        }

        private fun colLeft(cols: List<TableColumn>, index: Int): Float {
            var x = MARGIN
            for (i in 0 until index) x += cols[i].width
            return x
        }

        private fun rightEdge(cols: List<TableColumn>, index: Int): Float =
            colLeft(cols, index) + cols[index].width - 4f
    }
}
