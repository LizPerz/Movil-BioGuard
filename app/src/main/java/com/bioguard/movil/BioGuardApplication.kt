package com.bioguard.movil

import android.app.Application
import com.bioguard.movil.service.LecturasSyncWorker
import com.bioguard.movil.service.LocalAlertNotifier
import com.bioguard.movil.service.PredictionMlSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BioGuardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalAlertNotifier(this).createChannels()
        // Sincronización automática periódica de la cola offline hacia el backend.
        // Se reprograma en cada arranque de la app; KEEP evita duplicados.
        LecturasSyncWorker.scheduleAutoSync(this)
        // Sincronización automática de predicciones ML pendientes cada 15 minutos.
        PredictionMlSyncWorker.scheduleAutoSync(this)
    }
}
