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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bioguard.movil.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.SupportViewModel
import com.bioguard.movil.ui.theme.GreenNeon
import com.bioguard.movil.ui.theme.YellowNeon
import com.bioguard.movil.ui.theme.RedNeon

@Composable
fun SupportScreen(
    supportViewModel: SupportViewModel,
    onBack: () -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    val uiState by supportViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var asunto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("soporte_general") }
    var prioridad by remember { mutableStateOf("normal") }

    var toastMessage by remember { mutableStateOf("") }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotEmpty()) {
            Toast.makeText(context.applicationContext, toastMessage, Toast.LENGTH_SHORT).show()
            toastMessage = ""
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            toastMessage = it
            supportViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            toastMessage = it
            asunto = ""
            descripcion = ""
            supportViewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
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
                    text = stringResource(R.string.support_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent,
                    letterSpacing = 3.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Form Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface)
                            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.support_create_ticket),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.accent,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Asunto
                        OutlinedTextField(
                            value = asunto,
                            onValueChange = { asunto = it },
                            label = { Text(stringResource(R.string.support_subject_label), fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = p.accent,
                                unfocusedBorderColor = p.border,
                                focusedLabelColor = p.accent,
                                unfocusedLabelColor = p.textSecondary,
                                focusedTextColor = p.textPrimary,
                                unfocusedTextColor = p.textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Categoria Row Select
                        Text(stringResource(R.string.support_category_label), fontSize = 11.sp, color = p.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categories = listOf(
                                "soporte_general" to stringResource(R.string.support_cat_general),
                                "problema_dispositivo" to stringResource(R.string.support_cat_device),
                                "duda_factura" to stringResource(R.string.support_cat_payments)
                            )
                            categories.forEach { (catId, catLabel) ->
                                val selected = categoria == catId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) p.accent.copy(alpha = 0.2f) else p.surface)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) p.accent else p.border,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { categoria = catId }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = catLabel,
                                        fontSize = 10.sp,
                                        color = if (selected) p.accent else p.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Prioridad Row Select
                        Text(stringResource(R.string.support_priority_label), fontSize = 11.sp, color = p.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val priorities = listOf("baja", "normal", "alta")
                            priorities.forEach { prio ->
                                val selected = prioridad == prio
                                val prioColor = when (prio) {
                                    "alta" -> RedNeon
                                    "baja" -> GreenNeon
                                    else -> YellowNeon
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) prioColor.copy(alpha = 0.2f) else p.surface)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) prioColor else p.border,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { prioridad = prio }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prio.uppercase(),
                                        fontSize = 10.sp,
                                        color = if (selected) prioColor else p.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Descripcion
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            label = { Text(stringResource(R.string.support_detail_label), fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = p.accent,
                                unfocusedBorderColor = p.border,
                                focusedLabelColor = p.accent,
                                unfocusedLabelColor = p.textSecondary,
                                focusedTextColor = p.textPrimary,
                                unfocusedTextColor = p.textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (asunto.isBlank() || descripcion.isBlank()) {
                                    toastMessage = context.getString(R.string.support_error_empty)
                                } else {
                                    supportViewModel.crearTicket(asunto, descripcion, categoria, prioridad)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = stringResource(R.string.support_send_btn),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Tickets Title
                item {
                    Text(
                        text = stringResource(R.string.support_history_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.accent,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (uiState.tickets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.support_no_tickets),
                                color = p.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(uiState.tickets, key = { it.id }) { ticket ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(p.surface)
                                .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ticket.asunto,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ticket.descripcion,
                                    fontSize = 11.sp,
                                    color = p.textSecondary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ticket.fechaCreacion.substringBefore("T"),
                                    fontSize = 9.sp,
                                    color = p.textTertiary
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Status Tag
                            val isClosed = ticket.estado.equals("cerrado", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isClosed) p.border else GreenNeon.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ticket.estado.uppercase(),
                                    fontSize = 9.sp,
                                    color = if (isClosed) p.textSecondary else GreenNeon,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
