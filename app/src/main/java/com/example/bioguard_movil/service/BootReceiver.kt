package com.example.bioguard_movil.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.bioguard_movil.datastore.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Reinicio detectado, verificando sesion...")
            val prefs = UserPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userId = prefs.userId.first()
                    if (!userId.isNullOrEmpty()) {
                        Log.d(TAG, "Usuario con sesion activa ($userId). Iniciando BioGuardMonitoringService...")
                        BioGuardMonitoringService.start(context)
                    } else {
                        Log.d(TAG, "Sin sesion activa. No se inicia el servicio.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en BootReceiver: ${e.message}", e)
                }
            }
        }
    }
}
