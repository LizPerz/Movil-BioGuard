package com.example.bioguard_movil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.colorPalette

@Composable
fun CheckCard(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val p = LocalThemeState.current.colorPalette()
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = p.accent, uncheckedColor = p.border))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 13.sp, color = p.textPrimary)
        }
    }
}

@Composable
fun tfColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LocalThemeState.current.colorPalette().accent,
    unfocusedBorderColor = LocalThemeState.current.colorPalette().border,
    focusedContainerColor = LocalThemeState.current.colorPalette().inputBackground,
    unfocusedContainerColor = LocalThemeState.current.colorPalette().inputBackground,
    focusedTextColor = LocalThemeState.current.colorPalette().textPrimary,
    unfocusedTextColor = LocalThemeState.current.colorPalette().textPrimary,
    cursorColor = LocalThemeState.current.colorPalette().accent
)
