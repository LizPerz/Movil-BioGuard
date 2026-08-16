package com.bioguard.movil.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bioguard.movil.R
import com.bioguard.movil.data.Formatters
import com.bioguard.movil.ui.components.ErrorRetryBox
import com.bioguard.movil.ui.theme.LocalThemeState
import com.bioguard.movil.ui.theme.RedNeon
import com.bioguard.movil.ui.theme.colorPalette
import com.bioguard.movil.ui.viewmodel.LocationViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.Instant

@Composable
fun LocationScreen(locationViewModel: LocationViewModel) {
    val uiState by locationViewModel.uiState.collectAsState()
    val p = LocalThemeState.current.colorPalette()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.location_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = p.accent,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading && uiState.ubicacion == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = p.accent)
                    }
                }

                uiState.error != null && uiState.ubicacion == null -> {
                    ErrorRetryBox(
                        message = uiState.error,
                        onRetry = { locationViewModel.retry() }
                    )
                }

                uiState.sinUbicacion && uiState.ubicacion == null -> {
                    UbicacionVacia()
                }

                else -> {
                    uiState.ubicacion?.let { ubicacion ->
                        MapaUbicacion(
                            latitud = ubicacion.latitud,
                            longitud = ubicacion.longitud,
                            esEmergencia = ubicacion.esEmergencia,
                            lastUpdatedAt = uiState.lastUpdatedAt,
                            onError = if (uiState.error != null) {
                                { locationViewModel.retry() }
                            } else null
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = p.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun UbicacionVacia() {
    val p = LocalThemeState.current.colorPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(p.surface)
                .border(width = 1.dp, color = p.border, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = p.textTertiary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.location_empty_title),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = p.textPrimary,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.location_empty_msg),
            fontSize = 13.sp,
            color = p.textSecondary
        )
    }
}

@Composable
private fun MapaUbicacion(
    latitud: Double,
    longitud: Double,
    esEmergencia: Boolean,
    lastUpdatedAt: Instant?,
    onError: (() -> Unit)?
) {
    val p = LocalThemeState.current.colorPalette()
    val markerRef = remember { mutableStateOf<Marker?>(null) }
    var lastLat by remember { mutableStateOf(Double.NaN) }
    var lastLon by remember { mutableStateOf(Double.NaN) }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val appContext = ctx.applicationContext
                Configuration.getInstance().load(
                    appContext,
                    appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                )
                Configuration.getInstance().userAgentValue = appContext.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)
                    onResume()
                }
            },
            update = { mv ->
                val gp = GeoPoint(latitud, longitud)
                val marker = markerRef.value ?: Marker(mv).also { m ->
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mv.overlays.add(m)
                    markerRef.value = m
                }
                marker.position = gp
                if (gp.latitude != lastLat || gp.longitude != lastLon) {
                    lastLat = gp.latitude
                    lastLon = gp.longitude
                    mv.controller.animateTo(gp)
                }
            },
            onRelease = { mv ->
                mv.onPause()
                mv.onDetach()
                markerRef.value = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, p.border, RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (esEmergencia) {
            EmergenciaBadge()
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (onError != null) {
            Text(
                text = stringResource(R.string.error_network),
                fontSize = 12.sp,
                color = RedNeon,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        TarjetaUbicacion(
            latitud = latitud,
            longitud = longitud,
            lastUpdatedAt = lastUpdatedAt,
            esEmergencia = esEmergencia
        )
    }
}

@Composable
private fun EmergenciaBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RedNeon.copy(alpha = 0.15f))
            .border(1.dp, RedNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "\u26a0", color = RedNeon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.location_emergency_badge),
            color = RedNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun TarjetaUbicacion(
    latitud: Double,
    longitud: Double,
    lastUpdatedAt: Instant?,
    esEmergencia: Boolean
) {
    val p = LocalThemeState.current.colorPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.surface)
            .border(1.dp, p.border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.location_coords_label),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = p.textTertiary,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "%.5f, %.5f".format(latitud, longitud),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (esEmergencia) RedNeon else p.accent
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.location_updated_label),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = p.textTertiary,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = lastUpdatedAt?.let { Formatters.formatInstant(it) } ?: "—",
            fontSize = 14.sp,
            color = p.textPrimary
        )
    }
}
