package com.example.bioguard_movil.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.CyanDark
import com.example.bioguard_movil.ui.theme.CyanNeon
import com.example.bioguard_movil.ui.theme.DarkBackground
import com.example.bioguard_movil.ui.theme.DarkSurface
import com.example.bioguard_movil.ui.theme.GlassBorder
import com.example.bioguard_movil.ui.theme.InputBackground
import com.example.bioguard_movil.ui.theme.TextPrimary
import com.example.bioguard_movil.ui.theme.TextSecondary
import com.example.bioguard_movil.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricProfileScreen(
    onComplete: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf("Masculino") }
    var isDiabetic by remember { mutableStateOf(false) }
    var hasFamilyDiabetes by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf("Sedentario") }

    val activityLevels = listOf("Sedentario", "Ligero", "Moderado", "Intenso", "Muy intenso")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Perfil Biométrico",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 1.sp
            )

            Text(
                text = "Completa tu información para un análisis preciso",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            Text(
                text = "NOMBRE DE USUARIO",
                fontSize = 10.sp,
                color = CyanNeon,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tu nombre", color = TextTertiary) },
                leadingIcon = { Text(text = "👤", color = TextTertiary, fontSize = 16.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanNeon
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FECHA DE NACIMIENTO",
                fontSize = 10.sp,
                color = CyanNeon,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("dd / mm / aaaa", color = TextTertiary) },
                leadingIcon = { Text(text = "📅", color = TextTertiary, fontSize = 16.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanNeon
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PESO (KG)",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("70", color = TextTertiary) },
                        leadingIcon = { Text(text = "⚖", color = TextTertiary, fontSize = 16.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanNeon
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ESTATURA (CM)",
                        fontSize = 10.sp,
                        color = CyanNeon,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("175", color = TextTertiary) },
                        leadingIcon = { Text(text = "📏", color = TextTertiary, fontSize = 16.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SEXO",
                fontSize = 10.sp,
                color = CyanNeon,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedSex == "Masculino") CyanNeon else DarkSurface
                        )
                        .border(
                            width = 1.dp,
                            color = if (selectedSex == "Masculino") CyanNeon else GlassBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedSex = "Masculino" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Masculino",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (selectedSex == "Masculino") DarkBackground else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedSex == "Femenino") CyanNeon else DarkSurface
                        )
                        .border(
                            width = 1.dp,
                            color = if (selectedSex == "Femenino") CyanNeon else GlassBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedSex = "Femenino" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Femenino",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (selectedSex == "Femenino") DarkBackground else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NIVEL DE ACTIVIDAD FÍSICA",
                fontSize = 10.sp,
                color = CyanNeon,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = activityExpanded,
                onExpandedChange = { activityExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedActivity,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanNeon
                    )
                )

                ExposedDropdownMenu(
                    expanded = activityExpanded,
                    onDismissRequest = { activityExpanded = false },
                    containerColor = DarkSurface
                ) {
                    activityLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(text = level, color = TextPrimary) },
                            onClick = {
                                selectedActivity = level
                                activityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ANTECEDENTES",
                fontSize = 10.sp,
                color = CyanNeon,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDiabetic,
                        onCheckedChange = { isDiabetic = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyanNeon,
                            uncheckedColor = GlassBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "¿Es diabético diagnosticado?", fontSize = 13.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasFamilyDiabetes,
                        onCheckedChange = { hasFamilyDiabetes = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyanNeon,
                            uncheckedColor = GlassBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "¿Tiene familiares con diabetes?", fontSize = 13.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
            ) {
                Text(
                    text = "Finalizar Perfil y Vincular Reloj",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
