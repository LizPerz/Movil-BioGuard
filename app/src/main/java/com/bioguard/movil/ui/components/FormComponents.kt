package com.bioguard.movil.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.colorPalette

@Composable
fun CheckCard(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = p.accent, uncheckedColor = p.border)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 13.sp, color = p.textPrimary)
        }
    }
}

@Composable
fun BioValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String? = null,
    isError: Boolean = !errorMessage.isNullOrEmpty(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val p = LocalThemeState.current.colorPalette()

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label, fontSize = 13.sp) },
            isError = isError,
            singleLine = true,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) RedNeon else p.accent,
                unfocusedBorderColor = if (isError) RedNeon else p.border,
                focusedContainerColor = p.inputBackground,
                unfocusedContainerColor = p.inputBackground,
                focusedTextColor = p.textPrimary,
                unfocusedTextColor = p.textPrimary,
                cursorColor = p.accent,
                errorBorderColor = RedNeon,
                errorLabelColor = RedNeon
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(
            visible = isError && !errorMessage.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorMessage ?: "",
                color = RedNeon,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
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
