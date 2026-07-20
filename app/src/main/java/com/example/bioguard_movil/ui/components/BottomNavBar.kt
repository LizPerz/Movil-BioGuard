package com.example.bioguard_movil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bioguard_movil.ui.theme.LocalThemeState
import com.example.bioguard_movil.ui.theme.colorPalette

data class BottomNavItem(
    val label: String,
    val icon: String,
    val route: String
)

@Composable
fun BioGuardBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemSelected: (String) -> Unit
) {
    val p = LocalThemeState.current.colorPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.surface.copy(alpha = 0.95f))
            .border(width = 1.dp, color = p.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onItemSelected(item.route) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.icon,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) p.accent else p.textSecondary,
                        letterSpacing = 1.sp
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(width = 20.dp, height = 2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(p.accent)
                        )
                    }
                }
            }
        }
    }
}
