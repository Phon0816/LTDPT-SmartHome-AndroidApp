package com.example.safehome.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.safehome.R
import com.example.safehome.ui.notification.NotificationActivity

object NotificationHelper {

    private const val CHANNEL_ID = "safehome_alerts"
    private const val CHANNEL_NAME = "SafeHome alerts"

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: String? = null,
        type: String? = null,
        deviceId: String? = null,
        screen: String? = null
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, NotificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notificationId", notificationId)
            putExtra("type", type)
            putExtra("deviceId", deviceId)
            putExtra("screen", screen)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                notificationId.hashCode(),
                notification
            )
        } catch (_: SecurityException) {
            // Android 13+ can deny notification permission; token sync must keep working.
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Cảnh báo và thông báo từ SafeHome"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
