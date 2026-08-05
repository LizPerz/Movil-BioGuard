package com.example.bioguard_movil.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bioguard_movil.MainActivity
import com.example.bioguard_movil.network.ApiService
import com.example.bioguard_movil.network.RegisterFcmTokenRequest
import com.example.bioguard_movil.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BioGuardMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val api: ApiService = RetrofitClient.api

    companion object {
        const val CHANNEL_STANDARD = "bioguard_estandar"
        const val CHANNEL_CRITICAL = "bioguard_critico"
        const val NOTIFICATION_ID_CRITICAL = 992
        const val NOTIFICATION_ID_STANDARD = 993
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            try {
                api.registerFcmToken(RegisterFcmTokenRequest(token))
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "Alerta BioGuard"
        val message = remoteMessage.notification?.body ?: data["message"] ?: "Se ha detectado una actualizacion"
        val isCritical = data["level"] == "critical" || data["priority"] == "high"

        if (isCritical) {
            sendCriticalNotification(title, message)
            serviceScope.launch {
                try {
                    val bpm = data["bpm"]?.toFloatOrNull() ?: 120f
                    val temp = data["temperatura"]?.toFloatOrNull() ?: 38.5f
                    val gsr = data["gsr"]?.toFloatOrNull() ?: 8.0f
                    val probability = data["probability"]?.toFloatOrNull() ?: 0.95f
                    
                    val connector = WearableConnector(this@BioGuardMessagingService, {}, {}, {}, {})
                    connector.sendAlertCommandToWatch(bpm, temp, gsr, probability)
                } catch (e: Exception) {
                    android.util.Log.e("BIOGUARD_FCM", "Error enviando alerta al reloj desde FCM", e)
                }
            }
        } else {
            sendStandardNotification(title, message)
        }
    }

    private fun sendStandardNotification(title: String, message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(manager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_STANDARD)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        manager.notify(NOTIFICATION_ID_STANDARD, builder.build())
    }

    private fun sendCriticalNotification(title: String, message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(manager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_alert", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_CRITICAL)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)

        manager.notify(NOTIFICATION_ID_CRITICAL, builder.build())
    }

    private fun createChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_STANDARD) == null) {
                val standardChannel = NotificationChannel(
                    CHANNEL_STANDARD,
                    "Alertas Estandar",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Canal para recordatorios de medicamentos y alertas preventivas"
                }
                manager.createNotificationChannel(standardChannel)
            }

            if (manager.getNotificationChannel(CHANNEL_CRITICAL) == null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val criticalChannel = NotificationChannel(
                    CHANNEL_CRITICAL,
                    "Alertas Criticas",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Canal para alertas de emergencia y riesgo critico"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                    setBypassDnd(true)
                    setSound(soundUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                }
                manager.createNotificationChannel(criticalChannel)
            }
        }
    }
}
