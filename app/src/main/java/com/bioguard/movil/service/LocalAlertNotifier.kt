package com.bioguard.movil.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bioguard.movil.MainActivity
import com.bioguard.movil.R
import com.bioguard.movil.ml.LocalRiskAssessment
import com.bioguard.movil.ml.LocalRiskLevel

data class LocalNotificationResult(
    val acceptedByPolicy: Boolean,
    val displayed: Boolean
)

object LocalNotificationPolicy {
    const val HIGH_COOLDOWN_MILLIS = 15 * 60 * 1000L
    const val CRITICAL_COOLDOWN_MILLIS = 5 * 60 * 1000L

    fun shouldNotify(
        level: LocalRiskLevel,
        lastLevel: LocalRiskLevel?,
        nowMillis: Long,
        lastNotificationMillis: Long
    ): Boolean {
        if (level.rank < LocalRiskLevel.HIGH.rank) return false
        if (lastLevel == null || level.rank > lastLevel.rank) return true
        val cooldown = if (level == LocalRiskLevel.CRITICAL) {
            CRITICAL_COOLDOWN_MILLIS
        } else {
            HIGH_COOLDOWN_MILLIS
        }
        return nowMillis - lastNotificationMillis >= cooldown
    }
}

class LocalAlertNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)
    private val policyStore = context.getSharedPreferences(POLICY_STORE, Context.MODE_PRIVATE)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        systemManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITORING,
                "Monitoreo activo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Estado persistente del monitoreo Bluetooth local"
                setShowBadge(false)
            }
        )

        systemManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PREVENTIVE,
                "Alertas preventivas locales",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cambios relevantes calculados localmente en el teléfono"
                enableVibration(true)
            }
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        systemManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CRITICAL,
                "Alertas críticas locales",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Señales persistentes que requieren revisión inmediata"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(
                    alarmSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        )
    }

    fun monitoringNotification(): Notification = NotificationCompat.Builder(context, CHANNEL_MONITORING)
        .setContentTitle("BioGuard activo")
        .setContentText("Recibiendo y conservando lecturas del wearable")
        .setSmallIcon(R.drawable.ic_notification)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(mainPendingIntent(openAlert = false))
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .build()

    @Synchronized
    fun notifyAssessment(
        assessment: LocalRiskAssessment,
        nightGuardian: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): LocalNotificationResult {
        val lastLevel = policyStore.getString(KEY_LAST_LEVEL, null)
            ?.let { runCatching { LocalRiskLevel.valueOf(it) }.getOrNull() }
        val lastTime = policyStore.getLong(KEY_LAST_TIME, 0L)
        if (!LocalNotificationPolicy.shouldNotify(assessment.level, lastLevel, nowMillis, lastTime)) {
            return LocalNotificationResult(acceptedByPolicy = false, displayed = false)
        }

        policyStore.edit()
            .putString(KEY_LAST_LEVEL, assessment.level.name)
            .putLong(KEY_LAST_TIME, nowMillis)
            .apply()

        if (!canPostNotifications()) {
            return LocalNotificationResult(acceptedByPolicy = true, displayed = false)
        }

        val critical = assessment.level == LocalRiskLevel.CRITICAL
        val title = when {
            critical -> "BioGuard: revisión inmediata"
            nightGuardian -> "Guardián nocturno: cambio detectado"
            else -> "BioGuard: cambio preventivo detectado"
        }
        val reason = assessment.reasons.firstOrNull { it.isNotBlank() }
            ?: "La lectura se alejó de los parámetros configurados."
        val message = "$reason. Confirma el estado de la persona; esta alerta no es un diagnóstico."
        val channel = if (critical) CHANNEL_CRITICAL else CHANNEL_PREVENTIVE

        val notification = NotificationCompat.Builder(context, channel)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (critical) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(openAlert = true))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification())
            .build()

        return try {
            manager.notify(if (critical) NOTIFICATION_CRITICAL else NOTIFICATION_PREVENTIVE, notification)
            LocalNotificationResult(acceptedByPolicy = true, displayed = true)
        } catch (_: SecurityException) {
            LocalNotificationResult(acceptedByPolicy = true, displayed = false)
        }
    }

    fun notifyEmergencySos(description: String) {
        if (!canPostNotifications()) return
        val title = "🚨 ¡SOLICITUD DE AUXILIO / SOS!"
        val message = if (description.isNotBlank()) description else "El paciente presionó el botón de pánico en el reloj para solicitar ayuda inmediata."

        val notification = NotificationCompat.Builder(context, CHANNEL_CRITICAL)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(mainPendingIntent(openAlert = true), true)
            .setContentIntent(mainPendingIntent(openAlert = true))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            manager.notify((System.currentTimeMillis() % 10000).toInt() + 1000, notification)
        } catch (_: SecurityException) { }
    }

    fun cancelAllNotifications() {
        try {
            manager.cancelAll()
        } catch (_: Exception) { }
    }

    private fun publicNotification(): Notification = NotificationCompat.Builder(context, CHANNEL_PREVENTIVE)
        .setContentTitle("BioGuard")
        .setContentText("Abre la aplicación para revisar una alerta")
        .setSmallIcon(R.drawable.ic_notification)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()

    private fun mainPendingIntent(openAlert: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_alert", openAlert)
        }
        return PendingIntent.getActivity(
            context,
            if (openAlert) 41 else 40,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permissionGranted && manager.areNotificationsEnabled()
    }

    companion object {
        const val CHANNEL_MONITORING = "bioguard_monitoring_v2"
        const val CHANNEL_PREVENTIVE = "bioguard_preventive_v2"
        const val CHANNEL_CRITICAL = "bioguard_critical_v2"
        const val NOTIFICATION_MONITORING = 991
        private const val NOTIFICATION_PREVENTIVE = 992
        private const val NOTIFICATION_CRITICAL = 993
        private const val POLICY_STORE = "local_alert_policy"
        private const val KEY_LAST_LEVEL = "last_level"
        private const val KEY_LAST_TIME = "last_time"
    }
}
