package com.example.eduu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Event Reminder"
        val message = intent.getStringExtra("message") ?: "You have an upcoming event!"

        showNotification(context, title, message)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "edu_calendar_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel (Required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Calendar Events", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for scheduled study events"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // --- FIX: Create Intent to Open App ---
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // -------------------------------------

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar) // Replace with R.drawable.ic_launcher_foreground if available
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // <--- Attach the click action here
            .setAutoCancel(true) // Dismiss notification when clicked
            .build()

        // Use a unique ID (System time) so notifications don't overwrite each other in the tray
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}