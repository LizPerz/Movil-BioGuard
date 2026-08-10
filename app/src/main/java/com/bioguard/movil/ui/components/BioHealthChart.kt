package com.bioguard.movil.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.util.rememberBioHaptic

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable

data class ChartPoint(
    val label: String,
    val value: Float,
    val time: String = "",
    val timestampMs: Long = 0L
)

enum class TimeRangeFilter(val label: String, val durationMs: Long) {
    H1("1h", 1 * 3600_000L),
    H2("2h", 2 * 3600_000L),
    H4("4h", 4 * 3600_000L),
    H8("8h", 8 * 3600_000L),
    H12("12h", 12 * 3600_000L),
    H24("24h", 24 * 3600_000L),
    H36("36h", 36 * 3600_000L),
    H72("72h", 72 * 3600_000L),
    W1("1 Sem", 7 * 24 * 3600_000L),
    M1("1 Mes", 30 * 24 * 3600_000L),
    ALL("Todo", Long.MAX_VALUE)
}

@Composable
fun BioHealthChart(
    points: List<ChartPoint>,
    lineColor: Color = LocalThemeState.current.colorPalette().accent,
    unit: String = "",
    title: String? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalThemeState.current.colorPalette()
    val haptic = rememberBioHaptic()
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTimeRange by remember { mutableStateOf(TimeRangeFilter.ALL) }

    val filteredPoints = remember(points, selectedTimeRange) {
        if (points.isEmpty()) return@remember emptyList()
        val hasTimestamps = points.any { it.timestampMs > 0 }
        val now = if (hasTimestamps) points.maxOf { it.timestampMs } else System.currentTimeMillis()
        val cutoff = if (selectedTimeRange.durationMs == Long.MAX_VALUE) 0L else (now - selectedTimeRange.durationMs)

        val rawFiltered = if (selectedTimeRange == TimeRangeFilter.ALL || !hasTimestamps) {
            points
        } else {
            val matching = points.filter { it.timestampMs >= cutoff }
            if (matching.isEmpty()) points.takeLast(10) else matching
        }

        if (rawFiltered.size > 35) {
            val step = rawFiltered.size.toFloat() / 30f
            (0 until 30).map { i ->
                val idx = (i * step).toInt().coerceIn(0, rawFiltered.size - 1)
                rawFiltered[idx]
            }
        } else {
            rawFiltered
        }
    }
    
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(filteredPoints) {
        selectedPointIndex = null
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text("Sin datos para graficar", color = theme.textSecondary, fontSize = 13.sp)
        }
        return
    }

    val currentDisplayPoints = if (filteredPoints.isEmpty()) points else filteredPoints
    val minValue = currentDisplayPoints.minOf { it.value }
    val maxValue = currentDisplayPoints.maxOf { it.value }
    val valueRange = if (maxValue == minValue) 1f else (maxValue - minValue)

    fun formatVal(valFloat: Float): String {
        return if (unit == "°C" || unit == "µS" || valFloat % 1f != 0f) {
            String.format(java.util.Locale.US, "%.1f", valFloat)
        } else {
            valFloat.toInt().toString()
        }
    }

    val firstTs = currentDisplayPoints.firstOrNull()?.timestampMs ?: 0L
    val lastTs = currentDisplayPoints.lastOrNull()?.timestampMs ?: 0L
    val spanMs = if (lastTs > firstTs) lastTs - firstTs else 0L
    val spanMin = (spanMs / 60_000L).coerceAtLeast(1)
    val spanText = if (spanMin < 60) {
        "${currentDisplayPoints.size} lecturas en últimos ${spanMin} min"
    } else {
        "${currentDisplayPoints.size} lecturas en últimas ${spanMin / 60} h"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (title != null) {
                    Text(
                        text = title,
                        color = theme.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "📊 $spanText",
                    color = theme.textTertiary,
                    fontSize = 10.sp
                )
            }

            selectedPointIndex?.let { idx ->
                val pt = currentDisplayPoints.getOrNull(idx) ?: currentDisplayPoints.last()
                val formattedVal = formatVal(pt.value)
                val timeStr = pt.time.ifEmpty { pt.label }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "📍 $formattedVal $unit",
                        color = lineColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = timeStr,
                        color = theme.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } ?: run {
                val last = currentDisplayPoints.last()
                val formattedVal = formatVal(last.value)
                val timeStr = last.time.ifEmpty { last.label }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Último: $formattedVal $unit",
                        color = theme.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = timeStr,
                        color = theme.textTertiary,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Time Range Filter Bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(TimeRangeFilter.values()) { filter ->
                val isSelected = selectedTimeRange == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) lineColor else theme.inputBackground)
                        .clickable {
                            haptic.performSelection()
                            selectedTimeRange = filter
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) theme.background else theme.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentDisplayPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val stepX = width / (currentDisplayPoints.size - 1).coerceAtLeast(1)
                            val index = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, currentDisplayPoints.size - 1)
                            if (selectedPointIndex != index) {
                                selectedPointIndex = index
                                haptic.performSelection()
                            }
                        }
                    }
                    .pointerInput(currentDisplayPoints) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val width = size.width
                            val stepX = width / (currentDisplayPoints.size - 1).coerceAtLeast(1)
                            val index = ((change.position.x + stepX / 2) / stepX).toInt().coerceIn(0, currentDisplayPoints.size - 1)
                            if (selectedPointIndex != index) {
                                selectedPointIndex = index
                                haptic.performSelection()
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val progress = animationProgress.value

                // Grid lines (horizontal)
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = height * (i.toFloat() / gridLines)
                    drawLine(
                        color = theme.border.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Points coordinates
                val stepX = width / (currentDisplayPoints.size - 1).coerceAtLeast(1)
                val offsets = currentDisplayPoints.mapIndexed { idx, pt ->
                    val x = idx * stepX
                    val normalizedY = (pt.value - minValue) / valueRange
                    val y = height - (normalizedY * (height - 30.dp.toPx())) - 15.dp.toPx()
                    Offset(x, y)
                }

                if (offsets.size >= 2) {
                    val path = Path()
                    val fillPath = Path()

                    path.moveTo(offsets[0].x, offsets[0].y)
                    fillPath.moveTo(offsets[0].x, height)
                    fillPath.lineTo(offsets[0].x, offsets[0].y)

                    for (i in 0 until offsets.size - 1) {
                        val p1 = offsets[i]
                        val p2 = offsets[i + 1]
                        val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                        val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)

                        path.cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            p2.x, p2.y
                        )
                        fillPath.cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            p2.x, p2.y
                        )
                    }

                    fillPath.lineTo(offsets.last().x, height)
                    fillPath.close()

                    // Draw gradient fill under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.35f * progress),
                                lineColor.copy(alpha = 0.02f)
                            )
                        )
                    )

                    // Draw Bezier stroke path
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Highlight data points
                    offsets.forEachIndexed { idx, offset ->
                        val isSelected = selectedPointIndex == idx
                        val radius = if (isSelected) 8.dp.toPx() else 4.dp.toPx()
                        
                        if (isSelected) {
                            // Vertical dashed guide line
                            drawLine(
                                color = lineColor.copy(alpha = 0.6f),
                                start = Offset(offset.x, 0f),
                                end = Offset(offset.x, height),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        drawCircle(
                            color = if (isSelected) theme.background else lineColor,
                            radius = radius,
                            center = offset
                        )
                        drawCircle(
                            color = lineColor,
                            radius = if (isSelected) 6.dp.toPx() else 2.5.dp.toPx(),
                            center = offset,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-Axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayLabels = if (points.size > 5) {
                listOf(points.first(), points[points.size / 2], points.last())
            } else {
                points
            }
            displayLabels.forEach { pt ->
                Text(
                    text = pt.label,
                    color = theme.textTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
