package com.bioguard.movil

import android.app.Application
import com.bioguard.movil.service.LocalAlertNotifier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BioGuardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalAlertNotifier(this).createChannels()
    }
}
