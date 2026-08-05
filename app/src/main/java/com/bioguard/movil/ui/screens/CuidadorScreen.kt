package com.bioguard.movil.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.datastore.UserPreferences
import com.bioguard.movil.ui.components.tfColors
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.CuidadorViewModel
import com.bioguard.movil.ui.components.SystemNotificationDialog
import com.bioguard.movil.ui.components.ConfirmDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.bioguard.movil.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuidadorScreen(
    cuidadorViewModel: CuidadorViewModel,
    onBack: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by cuidadorViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context) }
    var toastMessage by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var cuidadorIdToDelete by remember { mutableStateOf<String?>(null) }
    var nombre by remember { mutableStateOf("") }
    var parentesco by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var nivelAcceso by remember { mutableStateOf("Solo lecturas") }
    var nivelExpanded by remember { mutableStateOf(false) }
    val niveles = listOf("Solo lecturas", "Lecturas y alertas", "Control total")

    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var successDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            errorDialogMessage = it
            cuidadorViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            successDialogMessage = it
            cuidadorViewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = "\u2190",
                    color = p.accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.cuidador_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 3.sp
                )
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(text = stringResource(R.string.cuidador_add_btn), color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading && uiState.cuidadores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accent)
                }
            } else if (uiState.cuidadores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.cuidador_empty), color = p.textSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.cuidadores, key = { it.id ?: it.hashCode().toString() }) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.surface)
                                .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = c.nombre ?: stringResource(R.string.cuidador_no_name), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "${c.parentesco ?: "-"}  \u2022  ${c.nivelAcceso ?: "-"}", fontSize = 12.sp, color = p.textSecondary)
                                c.telefono?.let {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = it, fontSize = 11.sp, color = p.textTertiary)
                                }
                                c.codigoAccesoQr?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = stringResource(R.string.cuidador_code, it), fontSize = 10.sp, color = p.accent)
                                }
                            }
                            Text(
                                text = stringResource(R.string.cuidador_delete),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedNeon,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { c.id?.let { cuidadorIdToDelete = it } }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (cuidadorIdToDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.cuidador_delete_title),
            message = stringResource(R.string.cuidador_delete_msg),
            confirmText = stringResource(R.string.cuidador_delete),
            cancelText = stringResource(R.string.cuidador_cancel),
            onConfirm = {
                cuidadorIdToDelete?.let { id -> cuidadorViewModel.deleteCuidador(id) }
                cuidadorIdToDelete = null
            },
            onCancel = { cuidadorIdToDelete = null }
        )
    }

    val noPatientMsg = stringResource(R.string.cuidador_no_patient)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = p.surface,
            title = { Text(stringResource(R.string.cuidador_new_title), color = p.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text(stringResource(R.string.cuidador_name), color = p.textSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                    OutlinedTextField(value = parentesco, onValueChange = { parentesco = it }, label = { Text(stringResource(R.string.cuidador_relationship), color = p.textSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                    OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text(stringResource(R.string.cuidador_phone), color = p.textSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                    OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text(stringResource(R.string.cuidador_email), color = p.textSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                    ExposedDropdownMenuBox(
                        expanded = nivelExpanded,
                        onExpandedChange = { nivelExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = nivelAcceso,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.cuidador_access_level), color = p.textSecondary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nivelExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            colors = tfColors()
                        )
                        ExposedDropdownMenu(
                            expanded = nivelExpanded,
                            onDismissRequest = { nivelExpanded = false },
                            containerColor = p.surface
                        ) {
                            niveles.forEach { nivel ->
                                DropdownMenuItem(
                                    text = { Text(nivel, color = p.textPrimary) },
                                    onClick = {
                                        nivelAcceso = nivel
                                        nivelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val patientId = prefs.patientId.first()
                            if (patientId == null) {
                                toastMessage = noPatientMsg
                            } else {
                                cuidadorViewModel.crearCuidador(nombre.trim(), parentesco.trim(), telefono.trim(), correo.trim(), nivelAcceso, patientId)
                            }
                        }
                        showAddDialog = false
                        nombre = ""
                        parentesco = ""
                        telefono = ""
                        correo = ""
                        nivelAcceso = "Solo lecturas"
                    },
                    enabled = nombre.isNotBlank() && parentesco.isNotBlank()
                ) {
                    Text(stringResource(R.string.cuidador_save), color = p.accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cuidador_cancel), color = p.textSecondary)
                }
            }
        )
    }

    if (errorDialogMessage != null) {
        SystemNotificationDialog(
            title = stringResource(R.string.cuidador_error),
            message = errorDialogMessage ?: "",
            isError = true,
            onDismiss = { errorDialogMessage = null }
        )
    }

    if (successDialogMessage != null) {
        SystemNotificationDialog(
            title = stringResource(R.string.cuidador_success),
            message = successDialogMessage ?: "",
            isError = false,
            onDismiss = { successDialogMessage = null }
        )
    }
}
