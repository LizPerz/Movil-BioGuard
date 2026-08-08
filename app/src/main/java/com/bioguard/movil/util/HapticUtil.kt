package com.bioguard.movil.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class BioHapticHelper(private val hapticFeedback: HapticFeedback) {
    fun performClick() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performSuccess() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performWarning() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performSelection() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

@Composable
fun rememberBioHaptic(): BioHapticHelper {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { BioHapticHelper(haptic) }
}
