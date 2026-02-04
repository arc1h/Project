package com.example.project.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.example.project.R

class NotificationWorker : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Habit Reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminders"

        // Vibration pattern: Wait 0ms, Vibrate 500ms, Wait 200ms, Vibrate 500ms
        var vibrationPattern = longArrayOf(0, 500, 200, 500)

        val channel = NotificationChannel(
            channelId,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for habit reminders"
            enableLights(true)
            lightColor = Color.MAGENTA
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this icon exists!
            .setContentTitle("Habit Time!")
            .setContentText("Don't forget to: $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(vibrationPattern) // Vibration for older Android versions
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitName.hashCode(), notification)
    }
}