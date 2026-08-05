package com.bioguard.movil.ui.model

import androidx.compose.ui.graphics.Color

data class VitalSign(
    val name: String,
    val value: String,
    val unit: String,
    val icon: String,
    val color: Color,
    val status: String,
    val statusColor: Color
)

data class HistoryItem(
    val time: String,
    val pulse: String,
    val temp: String,
    val status: String
)
