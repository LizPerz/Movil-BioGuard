package com.bioguard.movil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.bioguard.movil.R
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
        val w = PageManager(doc, context, pacienteNombre, pacienteId, lecturas)
        w.start()

        w.drawPatientCard()
        w.sectionTitle("Indicadores del periodo")
        w.drawKpis(reporte, lecturas)

        w.spacing(12f)
        w.sectionTitle("Ultimas lecturas")
        w.drawLecturas(lecturas)

        w.spacing(12f)
        w.sectionTitle("Eventos metabolicos")
        w.drawEventos(eventos)

        w.finish()

        runCatching {
            FileOutputStream(file).use { doc.writeTo(it) }
        }.onFailure { e -> Log.e(TAG, "No se pudo escribir el PDF", e) }
        doc.close()
        return file
    }

    private enum class Align { LEFT, RIGHT, CENTER }

    private class TableColumn(val title: String, val width: Float, val align: Align = Align.LEFT)

    private enum class Icon { HEART, THERMOMETER, BOLT, CHART, SHIELD, BELL, DOC, PERSON }

    private class PageManager(
        private val doc: PdfDocument,
        private val context: Context,
        private val pacienteNombre: String,
        private val pacienteId: String?,
        private val lecturas: List<LecturaSensorResponse>
    ) {
        private val brandGreen = Color.rgb(0, 204, 158)
        private val ink = Color.rgb(34, 40, 49)
        private val gray = Color.rgb(120, 130, 140)
        private val borderGray = Color.rgb(230, 235, 240)
        private val bgLight = Color.rgb(245, 247, 250)
        private val headerFill = Color.rgb(240, 245, 243)
        private val red = Color.rgb(239, 68, 68)
        private val orange = Color.rgb(245, 158, 11)
        private val greenOk = Color.rgb(34, 197, 94)
        private val blue = Color.rgb(59, 130, 246)
        private val blueIconBg = Color.rgb(219, 234, 254)
        private val redIconBg = Color.rgb(254, 226, 226)
        private val orangeIconBg = Color.rgb(255, 237, 213)
        private val yellowIconBg = Color.rgb(254, 249, 195)
        private val pinkIconBg = Color.rgb(252, 231, 243)
        private val avatarBg = Color.rgb(224, 232, 240)

        private var page: PdfDocument.Page? = null
        private val canvas: Canvas get() = page!!.canvas
        var y: Float = 0f
            private set
        private var pageNumber = 0

        private val fechaHora: String =
            SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale("es", "MX")).format(Date())

        private val bottomLimit: Float get() = PAGE_HEIGHT - FOOTER_HEIGHT - 8f

        // ---- Paints (letterSpacing en 0f para evitar texto separado) ----
        private val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen; textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }
        private val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }
        private val headerDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 9f; letterSpacing = 0f
        }
        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        private val accentLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen; strokeWidth = 2.5f
        }
        private val sectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }
        private val sectionBar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = brandGreen }
        private val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; style = Paint.Style.FILL
        }
        private val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderGray; style = Paint.Style.STROKE; strokeWidth = 0.8f
        }
        private val cardLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 8f; letterSpacing = 0f
        }
        private val cardLabelGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen; textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }
        private val cardValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }
        private val cardSubValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 8f; letterSpacing = 0f
        }
        private val patientName = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }
        private val patientMeta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 9f; letterSpacing = 0f
        }
        private val fieldValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }
        private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 6.5f; letterSpacing = 0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val tableHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }
        private val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 9.5f; letterSpacing = 0f
        }
        private val rowPaintGray = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 9.5f; letterSpacing = 0f
        }
        private val tableHeaderLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen; strokeWidth = 1.5f
        }
        private val separator = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderGray; strokeWidth = 0.6f
        }
        private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 8f; letterSpacing = 0f
        }
        private val compactHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray; textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
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
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0f
        }

        private fun roundedRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, cardFill)
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, cardBorder)
        }

        private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.FILL
        }

        // ---- Iconos vectoriales dibujados en el Canvas (sin emojis) ----
        private fun drawIcon(icon: Icon, cx: Float, cy: Float, size: Float, iconColor: Int) {
            val fill = fillPaint(iconColor)
            when (icon) {
                Icon.HEART -> {
                    val r = size * 0.26f
                    canvas.drawCircle(cx - r, cy - r * 0.4f, r, fill)
                    canvas.drawCircle(cx + r, cy - r * 0.4f, r, fill)
                    val tri = Path()
                    tri.moveTo(cx - r * 1.9f, cy - r * 0.15f)
                    tri.lineTo(cx + r * 1.9f, cy - r * 0.15f)
                    tri.lineTo(cx, cy + r * 1.5f)
                    tri.close()
                    canvas.drawPath(tri, fill)
                }
                Icon.THERMOMETER -> {
                    canvas.drawRoundRect(
                        cx - size * 0.08f, cy - size * 0.42f,
                        cx + size * 0.08f, cy + size * 0.12f, size * 0.08f, size * 0.08f, fill
                    )
                    canvas.drawCircle(cx, cy + size * 0.28f, size * 0.2f, fill)
                }
                Icon.BOLT -> {
                    val bolt = Path()
                    bolt.moveTo(cx + size * 0.12f, cy - size * 0.42f)
                    bolt.lineTo(cx - size * 0.2f, cy + size * 0.06f)
                    bolt.lineTo(cx, cy + size * 0.06f)
                    bolt.lineTo(cx - size * 0.12f, cy + size * 0.42f)
                    bolt.lineTo(cx + size * 0.2f, cy - size * 0.06f)
                    bolt.lineTo(cx, cy - size * 0.06f)
                    bolt.close()
                    canvas.drawPath(bolt, fill)
                }
                Icon.CHART -> {
                    val bw = size * 0.12f
                    val baseY = cy + size * 0.42f
                    canvas.drawRoundRect(cx - size * 0.36f, baseY - size * 0.45f, cx - size * 0.36f + bw, baseY, 2f, 2f, fill)
                    canvas.drawRoundRect(cx - bw / 2f, baseY - size * 0.8f, cx + bw / 2f, baseY, 2f, 2f, fill)
                    canvas.drawRoundRect(cx + size * 0.36f - bw, baseY - size * 0.6f, cx + size * 0.36f, baseY, 2f, 2f, fill)
                }
                Icon.SHIELD -> {
                    val sh = Path()
                    sh.moveTo(cx, cy - size * 0.42f)
                    sh.lineTo(cx + size * 0.32f, cy - size * 0.34f)
                    sh.lineTo(cx + size * 0.32f, cy + size * 0.05f)
                    sh.lineTo(cx, cy + size * 0.42f)
                    sh.lineTo(cx - size * 0.32f, cy + size * 0.05f)
                    sh.lineTo(cx - size * 0.32f, cy - size * 0.34f)
                    sh.close()
                    canvas.drawPath(sh, fill)
                }
                Icon.BELL -> {
                    val bell = Path()
                    bell.addArc(RectF(cx - size * 0.3f, cy - size * 0.32f, cx + size * 0.3f, cy + size * 0.3f), 190f, 160f)
                    bell.lineTo(cx + size * 0.22f, cy + size * 0.28f)
                    bell.lineTo(cx - size * 0.22f, cy + size * 0.28f)
                    bell.close()
                    canvas.drawPath(bell, fill)
                    canvas.drawCircle(cx, cy + size * 0.38f, size * 0.09f, fill)
                    canvas.drawCircle(cx, cy - size * 0.4f, size * 0.05f, fill)
                }
                Icon.DOC -> {
                    val r = RectF(cx - size * 0.3f, cy - size * 0.36f, cx + size * 0.3f, cy + size * 0.36f)
                    canvas.drawRoundRect(r, size * 0.06f, size * 0.06f, fill)
                    val fold = Path()
                    fold.moveTo(cx + size * 0.3f, cy - size * 0.16f)
                    fold.lineTo(cx + size * 0.18f, cy - size * 0.36f)
                    fold.lineTo(cx + size * 0.3f, cy - size * 0.36f)
                    fold.close()
                    canvas.drawPath(fold, fillPaint(Color.WHITE))
                    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE; strokeWidth = size * 0.07f
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(cx - size * 0.18f, cy - size * 0.2f, cx + size * 0.1f, cy - size * 0.2f, line)
                    canvas.drawLine(cx - size * 0.18f, cy - size * 0.05f, cx + size * 0.12f, cy - size * 0.05f, line)
                    canvas.drawLine(cx - size * 0.18f, cy + size * 0.1f, cx + size * 0.05f, cy + size * 0.1f, line)
                }
                Icon.PERSON -> {
                    canvas.drawCircle(cx, cy - size * 0.12f, size * 0.16f, fill)
                    val body = Path()
                    body.addArc(RectF(cx - size * 0.32f, cy - size * 0.08f, cx + size * 0.32f, cy + size * 0.45f), 0f, 180f)
                    body.close()
                    canvas.drawPath(body, fill)
                }
            }
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
            // Logo + marca (izquierda)
            val logoBitmap = logoBitmap()
            val logoH = 32f
            val logoW = if (logoBitmap != null) logoH * logoBitmap.width / logoBitmap.height.toFloat() else 0f
            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, MARGIN, y - 4f, bitmapPaint)
            }
            val textLeft = MARGIN + logoW + 10f
            canvas.drawText("BioGuard", textLeft, y + 26f, brandPaint)

            // Titulo + fecha (derecha, alineados a la derecha)
            val rightX = PAGE_WIDTH - MARGIN
            canvas.drawText("Reporte de salud", rightX - headerTitlePaint.measureText("Reporte de salud"), y + 14f, headerTitlePaint)
            canvas.drawText(fechaHora, rightX - headerDatePaint.measureText(fechaHora), y + 32f, headerDatePaint)

            // Linea de acento verde
            y += 42f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, accentLine)
            y += 14f
        }

        private fun logoBitmap(): Bitmap? {
            val full = runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.bio_guard)
            }.getOrNull() ?: return null
            val cropped = cropLogoBitmap(full)
            val dstH = 34f
            val dstW = dstH * cropped.width / cropped.height.toFloat()
            return Bitmap.createScaledBitmap(cropped, dstW.toInt(), dstH.toInt(), true)
        }

        private fun cropLogoBitmap(bitmap: Bitmap): Bitmap {
            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            var minX = w; var minY = h; var maxX = -1; var maxY = -1
            var i = 0
            for (yy in 0 until h) {
                for (xx in 0 until w) {
                    val alpha = pixels[i] ushr 24
                    if (alpha > 100) {
                        if (xx < minX) minX = xx
                        if (xx > maxX) maxX = xx
                        if (yy < minY) minY = yy
                        if (yy > maxY) maxY = yy
                    }
                    i++
                }
            }
            if (maxX <= minX || maxY <= minY) return bitmap
            val pad = 4
            val left = maxOf(0, minX - pad)
            val top = maxOf(0, minY - pad)
            val right = minOf(w, maxX + 1 + pad)
            val bottom = minOf(h, maxY + 1 + pad)
            return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        }

        private fun drawCompactHeader() {
            canvas.drawText("BioGuard \u00b7 Reporte de salud", MARGIN, y, compactHeader)
            canvas.drawLine(MARGIN, y + 6f, PAGE_WIDTH - MARGIN, y + 6f, tableHeaderLine)
            y += 24f
        }

        private fun drawFooter() {
            val lineY = PAGE_HEIGHT - 44f
            val textY = PAGE_HEIGHT - 26f
            canvas.drawLine(MARGIN, lineY, PAGE_WIDTH - MARGIN, lineY, separator)
            canvas.drawText(
                "Generado por BioGuard \u00b7 Documento informativo de monitoreo, no sustituye un diagnostico medico.",
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

        // ---- Card de Paciente ----
        fun drawPatientCard() {
            val cardTop = y
            val cardHeight = 64f
            val cardBottom = cardTop + cardHeight
            ensureSpace(cardHeight + 14f)

            roundedRect(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom, 10f)

            val avatarCx = MARGIN + 24f
            val avatarCy = cardTop + 30f
            val avatarR = 17f
            canvas.drawCircle(avatarCx, avatarCy, avatarR, fillPaint(avatarBg))
            drawIcon(Icon.PERSON, avatarCx, avatarCy, avatarR * 1.5f, gray)

            val infoLeft = MARGIN + 52f
            canvas.drawText("PACIENTE", infoLeft, cardTop + 15f, cardLabelGreen)
            val nombre = pacienteNombre.ifBlank { "Sin nombre" }
            val nombreCorto = if (nombre.length > 30) nombre.take(27) + "..." else nombre
            canvas.drawText(nombreCorto, infoLeft, cardTop + 33f, patientName)
            val idMeta = pacienteId?.let { "ID: $it" } ?: "Sin ID"
            canvas.drawText(idMeta, infoLeft, cardTop + 48f, patientMeta)

            val valueRight = PAGE_WIDTH - MARGIN - 14f
            val periodo = periodoTexto()
            val generado = fechaHora
            val maxValW = maxOf(fieldValue.measureText(periodo), fieldValue.measureText(generado))
            val labelRight = valueRight - maxValW - 10f

            canvas.drawText("PERIODO", labelRight - cardLabelGreen.measureText("PERIODO"), cardTop + 20f, cardLabelGreen)
            canvas.drawText(periodo, valueRight - fieldValue.measureText(periodo), cardTop + 20f, fieldValue)

            canvas.drawText("GENERADO", labelRight - cardLabelGreen.measureText("GENERADO"), cardTop + 40f, cardLabelGreen)
            canvas.drawText(generado, valueRight - fieldValue.measureText(generado), cardTop + 40f, fieldValue)

            y = cardBottom + 12f
        }

        private fun periodoTexto(): String {
            val fechas = lecturas.mapNotNull { l ->
                runCatching {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
                        .parse((l.timestamp.substringBefore("T") + "T" + l.timestamp.substringAfter("T").take(5)))
                }.getOrNull()
            }
            if (fechas.isEmpty()) return "Sin lecturas"
            val fmt = SimpleDateFormat("d MMM yyyy", Locale("es", "MX"))
            val a = runCatching { fmt.format(fechas.first()) }.getOrElse { "-" }
            val b = runCatching { fmt.format(fechas.last()) }.getOrElse { "-" }
            return if (a == b) a else "$a - $b"
        }

        // ---- Grid de KPIs (3x2) ----
        fun drawKpis(reporte: ReporteResumenResponse, lecturas: List<LecturaSensorResponse>) {
            data class Kpi(
                val label: String,
                val value: String,
                val subValue: String,
                val icon: Icon,
                val iconColor: Int,
                val iconBg: Int
            )

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
                else -> "\u2014"
            }
            val avgPulso = reporte.promedioPulso?.let { "%.0f".format(it) } ?: "\u2014"

            val kpis = listOf(
                Kpi("PULSO PROMEDIO", avgPulso, "BPM", Icon.HEART, red, redIconBg),
                Kpi("TEMPERATURA PROMEDIO", avgTemp?.let { "%.1f".format(it) } ?: "\u2014", "\u00b0C", Icon.THERMOMETER, orange, orangeIconBg),
                Kpi("ESTRES PROMEDIO", avgEstres?.let { "%.0f".format(it) } ?: "\u2014", "%", Icon.BOLT, orange, yellowIconBg),
                Kpi("LECTURAS", "${reporte.totalLecturas}", "", Icon.CHART, red, pinkIconBg),
                Kpi("RIESGO MAXIMO", maxRiesgoLabel, "", Icon.SHIELD, red, redIconBg),
                Kpi("EVENTOS", "${reporte.totalEventos}", "sin criticos", Icon.BELL, blue, blueIconBg)
            )

            val gap = 10f
            val cardW = (CONTENT_WIDTH - 2 * gap) / 3f
            val cardH = 100f

            kpis.chunked(3).forEach { row ->
                ensureSpace(cardH + gap)
                row.forEachIndexed { i, kpi ->
                    val left = MARGIN + i * (cardW + gap)
                    val top = y
                    val right = left + cardW
                    val bottom = top + cardH

                    roundedRect(left, top, right, bottom, 8f)

                    val iconCx = left + 22f
                    val iconCy = top + 21f
                    val iconR = 13f
                    canvas.drawCircle(iconCx, iconCy, iconR, fillPaint(kpi.iconBg))
                    drawIcon(kpi.icon, iconCx, iconCy, 16f, kpi.iconColor)

                    canvas.drawText(kpi.label, left + 13f, top + 47f, cardLabel)

                    canvas.drawText(kpi.value, left + 13f, top + 63f, cardValue)
                    if (kpi.subValue.isNotBlank()) {
                        val vw = cardValue.measureText(kpi.value)
                        canvas.drawText(kpi.subValue, left + 13f + vw + 6f, top + 63f, cardSubValue)
                    }

                    val badgeText = if (kpi.label == "EVENTOS") "SIN EVENTOS" else "SIN DATOS"
                    val badgeH = 13f
                    val badgeW = badgePaint.measureText(badgeText) + 12f
                    val badgeLeft = left + 13f
                    val badgeTop = bottom - badgeH - 8f
                    canvas.drawRoundRect(
                        badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH,
                        4f, 4f, fillPaint(bgLight)
                    )
                    badgePaint.color = gray
                    canvas.drawText(badgeText, badgeLeft + 6f, badgeTop + 9.5f, badgePaint)
                }
                y += cardH + gap
            }
            y += 4f
        }

        // ---- Tablas ----
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
                canvas.drawText("%.1f\u00b0C".format(l.temperaturaC), rightEdge(cols, 2) - rowPaint.measureText("%.1f\u00b0C".format(l.temperaturaC)), base, rowPaint)
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
                val nivel = e.nivelRiesgo ?: "-"
                val nivelPaint = fitPaint(riskPaints[e.nivelRiesgo] ?: rowPaint, nivel, cols[1].width - 8f)
                canvas.drawText(nivel, colLeft(cols, 1) + 4f, base, nivelPaint)
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
                val x = when (col.align) {
                    Align.RIGHT -> rightEdge(cols, i) - tableHeader.measureText(col.title)
                    Align.CENTER -> colLeft(cols, i) + (cols[i].width - tableHeader.measureText(col.title)) / 2f
                    else -> colLeft(cols, i) + 4f
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

        private fun fitPaint(base: Paint, text: String, maxWidth: Float): Paint {
            val p = Paint(base)
            while (p.measureText(text) > maxWidth && p.textSize > 6f) {
                p.textSize -= 0.5f
            }
            return p
        }
    }
}
