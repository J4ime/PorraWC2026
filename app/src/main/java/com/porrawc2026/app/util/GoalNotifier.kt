package com.porrawc2026.app.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

object GoalNotifier {

    private const val CHANNEL_ID = "goal_alerts"
    private const val CHANNEL_NAME = "Goles en directo"
    private var nextId = 1000

    fun init(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones cuando un goleador de tu porra marca"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun notifyGoal(context: Context, playerName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("\u26BD \u00A1${playerName} ha marcado!")
            .setContentText("Un goleador de tu porra acaba de marcar en directo")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(nextId++, notification)
        }.onFailure { android.util.Log.e("GoalNotifier", "Failed to show goal notification for $playerName", it) }
    }
}
