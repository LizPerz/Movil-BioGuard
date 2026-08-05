package com.example.bioguard_movil.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.colorPalette
import com.example.bioguard_movil.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Configuración guardada exitosamente", Toast.LENGTH_SHORT).show()
            settingsViewModel.resetSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Ajustes de Sincronización",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = p.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Configura los intervalos y ventanas horarias de transmisión de datos al servidor",
            fontSize = 14.sp,
            color = p.textSecondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // CARD DE TRANSMISIÓN AUTOMÁTICA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(p.surface)
                .border(1.dp, p.border, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Envío automático",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Text(
                        text = "Sincronizar telemetría al detectar red",
                        fontSize = 12.sp,
                        color = p.textSecondary
                    )
                }
                Switch(
                    checked = uiState.isSyncEnabled,
                    onCheckedChange = { settingsViewModel.updateSyncEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = p.accent,
                        checkedTrackColor = p.accent.copy(alpha = 0.5f),
                        uncheckedThumbColor = p.textSecondary,
                        uncheckedTrackColor = p.border
                    )
                )
            }

            if (uiState.isSyncEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Frecuencia de sincronización: ${uiState.syncIntervalMinutes} minutos",
                    fontSize = 14.sp,
                    color = p.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = uiState.syncIntervalMinutes.toFloat(),
                    onValueChange = { settingsViewModel.updateSyncInterval(it.toInt()) },
                    valueRange = 15f..120f,
                    steps = 6, // 15, 30, 45, 60, 75, 90, 105, 120
                    colors = SliderDefaults.colors(
                        thumbColor = p.accent,
                        activeTrackColor = p.accent,
                        inactiveTrackColor = p.border
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CARD DE SUBIDA POR LOTES (BATCH)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(p.surface)
                .border(1.dp, p.border, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Ventana de Transmisión en Lotes",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.textPrimary
            )
            Text(
                text = "Subida pesada de historiales acumulados offline",
                fontSize = 12.sp,
                color = p.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Hora de inicio", fontSize = 12.sp, color = p.textSecondary)
                    Text(text = "${String.format("%02d", uiState.batchStartHour)}:00 hrs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Hora de fin", fontSize = 12.sp, color = p.textSecondary)
                    Text(text = "${String.format("%02d", uiState.batchEndHour)}:00 hrs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = uiState.batchStartHour.toFloat(),
                onValueChange = { settingsViewModel.updateBatchHours(it.toInt(), uiState.batchEndHour) },
                valueRange = 0f..23f,
                steps = 22,
                colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.border)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CARD DE GUARDIÁN NOCTURNO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(p.surface)
                .border(1.dp, p.border, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Guardián Nocturno",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Text(
                        text = "Monitoreo más sensible y alarmas durante el sueño",
                        fontSize = 12.sp,
                        color = p.textSecondary
                    )
                }
                Switch(
                    checked = uiState.isNightGuardianEnabled,
                    onCheckedChange = { settingsViewModel.updateNightGuardianEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = p.accent,
                        checkedTrackColor = p.accent.copy(alpha = 0.5f),
                        uncheckedThumbColor = p.textSecondary,
                        uncheckedTrackColor = p.border
                    )
                )
            }

            if (uiState.isNightGuardianEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Rango de sueño", fontSize = 12.sp, color = p.textSecondary)
                        Text(
                            text = "De ${String.format("%02d", uiState.nightGuardianStartHour)}:00 a ${String.format("%02d", uiState.nightGuardianEndHour)}:00 hrs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = uiState.nightGuardianStartHour.toFloat(),
                    onValueChange = { settingsViewModel.updateNightGuardianHours(it.toInt(), uiState.nightGuardianEndHour) },
                    valueRange = 18f..23f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.border)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // BOTÓN DE GUARDAR
        Button(
            onClick = { settingsViewModel.saveSettings() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = p.accent),
            shape = RoundedCornerShape(8.dp),
            enabled = !uiState.isSaving
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = p.background)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Guardando...", fontWeight = FontWeight.Bold)
            } else {
                Text(text = "Guardar Ajustes", fontWeight = FontWeight.Bold, color = p.background)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, p.border)
        ) {
            Text(text = "Cancelar y volver", color = p.textPrimary)
        }
    }
}
