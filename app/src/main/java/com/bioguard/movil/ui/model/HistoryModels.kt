package com.bioguard.movil.ui.model

import androidx.compose.ui.graphics.Color

data class HistoryEntry(
    val date: String,
    val time: String,
    val pulse: String,
    val temp: String,
    val estres: String,
    val status: String,
    val statusColor: Color
)

data class ChartBar(
    val label: String,
    val value: Float,
    val color: Color
)
