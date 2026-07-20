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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.GreenNeon
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.RedNeon
import com.example.bioguard_movil.ui.theme.ThemeState
import com.example.bioguard_movil.ui.theme.YellowNeon
import com.example.bioguard_movil.ui.theme.colorPalette

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    themeState: ThemeState = ThemeState(),
    onThemeChange: (ThemeState) -> Unit = {}
) {
    val p = LocalThemeState.current.colorPalette()
    var isDarkMode by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "MI PERFIL",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = p.accent,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.surface)
                    .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(p.accentDark.copy(alpha = 0.3f))
                            .border(width = 2.dp, color = p.accent, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👤", fontSize = 40.sp, color = p.accent)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Usuario BioGuard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textPrimary
                    )

                    Text(
                        text = "perfil@bioguard.med",
                        fontSize = 13.sp,
                        color = p.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenNeon.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = "ACTIVO", fontSize = 10.sp, color = GreenNeon, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DATOS BIOMÉTRICOS",
                fontSize = 10.sp,
                color = p.accent,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            ProfileInfoRow(label = "NOMBRE", value = "Usuario BioGuard")
            ProfileInfoRow(label = "CORREO", value = "perfil@bioguard.med")
            ProfileInfoRow(label = "TELÉFONO", value = "+52 55 1234 5678")
            ProfileInfoRow(label = "FECHA DE NACIMIENTO", value = "01/01/1990")
            ProfileInfoRow(label = "SEXO", value = "Masculino")
            ProfileInfoRow(label = "PESO", value = "75 kg")
            ProfileInfoRow(label = "ESTATURA", value = "178 cm")
            ProfileInfoRow(label = "ACTIVIDAD", value = "Moderado")

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "MÉDICOS VINCULADOS",
                fontSize = 10.sp,
                color = p.accent,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            DoctorCard(
                name = "Dr. Carlos Mendoza",
                specialty = "Cardiología",
                hospital = "Hospital Angeles"
            )

            DoctorCard(
                name = "Dra. Ana García",
                specialty = "Endocrinología",
                hospital = "IMSS Unidad 21"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CONTACTOS DE EMERGENCIA",
                fontSize = 10.sp,
                color = p.accent,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            EmergencyContactCard(
                name = "María López",
                relation = "Esposa",
                phone = "+52 55 9876 5432"
            )

            EmergencyContactCard(
                name = "Roberto López",
                relation = "Hermano",
                phone = "+52 55 5555 1234"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CONFIGURACIÓN",
                fontSize = 10.sp,
                color = p.accent,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            ProfileMenuRow(text = "Notificaciones", subtitle = "Gestionar alertas")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.surface)
                    .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .clickable {
                        isDarkMode = !isDarkMode
                        onThemeChange(themeState.copy(isDarkMode = isDarkMode))
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Modo Oscuro", fontSize = 13.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
                        Text(text = if (isDarkMode) "Activado" else "Desactivado", fontSize = 10.sp, color = p.textSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) p.accent else p.border)
                            .padding(2.dp),
                        contentAlignment = if (isDarkMode) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            ProfileMenuRow(text = "Suscripción Premium", subtitle = "Plan activo - Toque para gestionar")
            ProfileMenuRow(text = "Editar Perfil", subtitle = "Actualizar datos personales")
            ProfileMenuRow(text = "Privacidad", subtitle = "Datos y seguridad")
            ProfileMenuRow(text = "Acerca de", subtitle = "v1.0.0")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedNeon)
            ) {
                Text(
                    text = "CERRAR SESIÓN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    val p = LocalThemeState.current.colorPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, color = p.textSecondary, letterSpacing = 1.sp)
        Text(text = value, fontSize = 12.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
fun DoctorCard(name: String, specialty: String, hospital: String) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(p.accentDark.copy(alpha = 0.2f))
                    .border(width = 1.dp, color = p.accent.copy(alpha = 0.3f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🩺", fontSize = 20.sp, color = p.accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = name, fontSize = 13.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
                Text(text = specialty, fontSize = 11.sp, color = p.accent)
                Text(text = hospital, fontSize = 10.sp, color = p.textSecondary)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun EmergencyContactCard(name: String, relation: String, phone: String) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RedNeon.copy(alpha = 0.1f))
                    .border(width = 1.dp, color = RedNeon.copy(alpha = 0.3f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚨", fontSize = 20.sp, color = RedNeon)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = name, fontSize = 13.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
                Text(text = relation, fontSize = 11.sp, color = p.accent)
                Text(text = phone, fontSize = 10.sp, color = p.textSecondary)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ProfileMenuRow(text: String, subtitle: String) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(p.surface)
            .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = text, fontSize = 13.sp, color = p.textPrimary, fontWeight = FontWeight.Medium)
                Text(text = subtitle, fontSize = 10.sp, color = p.textSecondary)
            }
            Text(text = "→", fontSize = 14.sp, color = p.accent)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}
