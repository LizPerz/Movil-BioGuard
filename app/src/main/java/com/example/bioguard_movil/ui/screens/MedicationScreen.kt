package com.example.bioguard_movil.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.components.tfColors
import com.example.bioguard_movil.ui.components.SystemNotificationDialog
import com.example.bioguard_movil.ui.components.ConfirmDialog
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.colorPalette
import com.example.bioguard_movil.ui.viewmodel.MedicationViewModel

@Composable
fun MedicationScreen(
    medicationViewModel: MedicationViewModel,
    onBack: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by medicationViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var successDialogMessage by remember { mutableStateOf<String?>(null) }
    var medicationIdToDelete by remember { mutableStateOf<String?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            errorDialogMessage = it
            medicationViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            successDialogMessage = it
            medicationViewModel.clearMessages()
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
                    text = "MEDICAMENTOS",
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
                Text(text = "+ AGREGAR MEDICAMENTO", color = p.background, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading && uiState.medicamentos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accent)
                }
            } else if (uiState.medicamentos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Sin medicamentos registrados", color = p.textSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.medicamentos, key = { it.id }) { med ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.surface)
                                .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = med.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "${med.dosis}  \u2022  ${med.frecuencia}", fontSize = 12.sp, color = p.textSecondary)
                                    med.notas?.let {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = it, fontSize = 11.sp, color = p.textTertiary)
                                    }
                                    med.ultimaToma?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Ultima toma: ${it.substringBefore("T")}", fontSize = 10.sp, color = p.textTertiary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "ELIMINAR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedNeon,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { medicationIdToDelete = med.id }
                                            .padding(6.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { medicationViewModel.registrarToma(med.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                                    ) {
                                        Text(text = "TOMADA", color = p.background, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = p.surface,
            title = { Text("Nuevo medicamento", color = p.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre", color = p.textSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                    OutlinedTextField(value = dosis, onValueChange = { dosis = it }, label = { Text("Dosis", color = p.textSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                    OutlinedTextField(value = frecuencia, onValueChange = { frecuencia = it }, label = { Text("Frecuencia", color = p.textSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                    OutlinedTextField(value = notas, onValueChange = { notas = it }, label = { Text("Notas", color = p.textSecondary) }, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        medicationViewModel.crearMedicamento(nombre.trim(), dosis.trim(), frecuencia.trim(), notas.trim().ifEmpty { null })
                        showAddDialog = false
                        nombre = ""
                        dosis = ""
                        frecuencia = ""
                        notas = ""
                    },
                    enabled = nombre.isNotBlank() && dosis.isNotBlank() && frecuencia.isNotBlank()
                ) {
                    Text("GUARDAR", color = p.accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCELAR", color = p.textSecondary)
                }
            }
        )
    }

    if (medicationIdToDelete != null) {
        ConfirmDialog(
            title = "ELIMINAR MEDICAMENTO",
            message = "¿Estás seguro de que deseas eliminar este medicamento?",
            confirmText = "ELIMINAR",
            cancelText = "CANCELAR",
            onConfirm = {
                medicationIdToDelete?.let { id -> medicationViewModel.deleteMedicamento(id) }
                medicationIdToDelete = null
            },
            onCancel = { medicationIdToDelete = null }
        )
    }

    if (errorDialogMessage != null) {
        SystemNotificationDialog(
            title = "AVISO DE ERROR",
            message = errorDialogMessage ?: "",
            isError = true,
            onDismiss = { errorDialogMessage = null }
        )
    }

    if (successDialogMessage != null) {
        SystemNotificationDialog(
            title = "OPERACIÓN EXITOSA",
            message = successDialogMessage ?: "",
            isError = false,
            onDismiss = { successDialogMessage = null }
        )
    }
}
