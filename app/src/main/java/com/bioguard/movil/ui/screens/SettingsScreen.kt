package com.bioguard.movil.ui.screens

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
import java.util.Locale
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.SettingsViewModel
import com.bioguard.movil.service.CloudSyncPhase
import androidx.compose.ui.res.stringResource
import com.bioguard.movil.R
import com.bioguard.movil.ui.model.AppPermission
import com.bioguard.movil.ui.model.EffectiveAccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    access: EffectiveAccess = EffectiveAccess(),
    onBack: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val saveSuccessMessage = stringResource(R.string.settings_save_success)

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, saveSuccessMessage, Toast.LENGTH_SHORT).show()
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
            text = stringResource(R.string.settings_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = p.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.settings_description),
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
                        text = stringResource(R.string.settings_auto_sync_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.settings_auto_sync_desc),
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
                    text = stringResource(R.string.settings_sync_freq, uiState.syncIntervalMinutes),
                    fontSize = 14.sp,
                    color = p.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = uiState.syncIntervalMinutes.toFloat(),
                    onValueChange = { settingsViewModel.updateSyncInterval(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22,
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.settings_batch_title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                    Text(text = stringResource(R.string.settings_batch_desc), fontSize = 12.sp, color = p.textSecondary)
                }
                Switch(
                    checked = uiState.isBatchSyncEnabled,
                    onCheckedChange = settingsViewModel::updateBatchSyncEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = p.accent, checkedTrackColor = p.accent.copy(alpha = 0.5f), uncheckedThumbColor = p.textSecondary, uncheckedTrackColor = p.border)
                )
            }

            if (uiState.isBatchSyncEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = stringResource(R.string.settings_start_hour), fontSize = 12.sp, color = p.textSecondary)
                    Text(text = "${String.format(Locale.ROOT, "%02d", uiState.batchStartHour)}:00 hrs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.settings_end_hour), fontSize = 12.sp, color = p.textSecondary)
                    Text(text = "${String.format(Locale.ROOT, "%02d", uiState.batchEndHour)}:00 hrs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
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
            Text(text = stringResource(R.string.settings_end_hour), fontSize = 12.sp, color = p.textSecondary)
            Slider(
                value = uiState.batchEndHour.toFloat(),
                onValueChange = { settingsViewModel.updateBatchHours(uiState.batchStartHour, it.toInt()) },
                valueRange = 0f..23f,
                steps = 22,
                colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.border)
            )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(p.surface)
                .border(1.dp, p.border, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_local_intelligence_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.settings_local_intelligence_desc),
                        fontSize = 12.sp,
                        color = p.textSecondary
                    )
                }
                Switch(
                    checked = uiState.isLocalAnalysisEnabled,
                    onCheckedChange = settingsViewModel::updateLocalAnalysisEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = p.accent,
                        checkedTrackColor = p.accent.copy(alpha = 0.5f),
                        uncheckedThumbColor = p.textSecondary,
                        uncheckedTrackColor = p.border
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_local_alerts_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.settings_local_alerts_desc),
                        fontSize = 12.sp,
                        color = p.textSecondary
                    )
                }
                Switch(
                    checked = uiState.isLocalAlertsEnabled,
                    onCheckedChange = settingsViewModel::updateLocalAlertsEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = p.accent,
                        checkedTrackColor = p.accent.copy(alpha = 0.5f),
                        uncheckedThumbColor = p.textSecondary,
                        uncheckedTrackColor = p.border
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CARD DE GUARDIÁN NOCTURNO
        if (access.allows(AppPermission.NIGHT_GUARDIAN)) {
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
                        text = stringResource(R.string.settings_night_guardian_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.settings_night_guardian_desc),
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
                        Text(text = stringResource(R.string.settings_sleep_range), fontSize = 12.sp, color = p.textSecondary)
                        Text(
                            text = "De ${String.format(Locale.ROOT, "%02d", uiState.nightGuardianStartHour)}:00 a ${String.format(Locale.ROOT, "%02d", uiState.nightGuardianEndHour)}:00 hrs",
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
        }

        if (access.planName != null && !access.planName.contains("Premium", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material3.TextButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://bioguard.app/planes"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mejorar plan a Premium", color = p.accent, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = settingsViewModel::syncNow,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = p.surface),
            border = BorderStroke(1.dp, p.accent),
            shape = RoundedCornerShape(8.dp),
            enabled = !uiState.isManualSyncing
        ) {
            if (uiState.isManualSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = p.accent)
            } else {
                Text(text = "Sincronizar ahora (${uiState.pendingItems} pendientes)", color = p.textPrimary, fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.cloudSyncPhase != CloudSyncPhase.IDLE &&
            uiState.cloudSyncPhase != CloudSyncPhase.RUNNING
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (uiState.cloudSyncPhase) {
                    CloudSyncPhase.SUCCESS -> stringResource(R.string.settings_sync_success)
                    CloudSyncPhase.NO_NETWORK -> stringResource(R.string.settings_sync_no_network)
                    CloudSyncPhase.FAILED -> stringResource(R.string.settings_sync_failed)
                    else -> ""
                },
                color = if (uiState.cloudSyncPhase == CloudSyncPhase.SUCCESS) p.accent else p.textSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                Text(text = stringResource(R.string.settings_saving), fontWeight = FontWeight.Bold)
            } else {
                Text(text = stringResource(R.string.settings_save_btn), fontWeight = FontWeight.Bold, color = p.background)
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
            Text(text = stringResource(R.string.settings_cancel_btn), color = p.textPrimary)
        }
    }
}
