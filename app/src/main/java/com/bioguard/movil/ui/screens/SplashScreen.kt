package com.bioguard.movil.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioguard.movil.R
import com.bioguard.movil.ui.theme.CyanNeon
import com.bioguard.movil.ui.theme.DarkBackground
import com.bioguard.movil.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.res.stringResource

@Composable
fun SplashScreen(
    isAuthenticated: Boolean = false,
    onFinished: (authenticated: Boolean) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(3000.milliseconds)
        if (!finished) {
            finished = true
            onFinished(isAuthenticated)
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && !finished) {
            finished = true
            onFinished(true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanNeon.copy(alpha = glowAlpha * 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.bio_guard),
                contentDescription = stringResource(R.string.splash_image_desc),
                modifier = Modifier.size(100.dp).alpha(glowAlpha)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.splash_bioguard),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon,
                letterSpacing = 10.sp,
                modifier = Modifier.alpha(glowAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.splash_desc),
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(glowAlpha)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.splash_sub_desc),
                fontSize = 13.sp,
                color = TextSecondary.copy(alpha = glowAlpha),
                letterSpacing = 1.sp
            )
        }
    }
}
