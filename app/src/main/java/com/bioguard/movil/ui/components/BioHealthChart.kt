package com.bioguard.movil.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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

data class ChartPoint(
    val label: String,
    val value: Float,
    val time: String = ""
)

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
    
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
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

    val minValue = points.minOf { it.value }
    val maxValue = points.maxOf { it.value }
    val valueRange = if (maxValue == minValue) 1f else (maxValue - minValue)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.surface)
            .padding(16.dp)
    ) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = theme.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                selectedPointIndex?.let { idx ->
                    val pt = points[idx]
                    Text(
                        text = "${pt.value.toInt()} $unit (${pt.time.ifEmpty { pt.label }})",
                        color = lineColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } ?: run {
                    val last = points.last()
                    Text(
                        text = "Último: ${last.value.toInt()} $unit",
                        color = theme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val stepX = width / (points.size - 1).coerceAtLeast(1)
                            val index = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, points.size - 1)
                            selectedPointIndex = index
                            haptic.performSelection()
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
                val stepX = width / (points.size - 1).coerceAtLeast(1)
                val offsets = points.mapIndexed { idx, pt ->
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
                        val radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx()
                        
                        drawCircle(
                            color = if (isSelected) theme.background else lineColor,
                            radius = radius,
                            center = offset
                        )
                        drawCircle(
                            color = lineColor,
                            radius = if (isSelected) 5.dp.toPx() else 2.5.dp.toPx(),
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
