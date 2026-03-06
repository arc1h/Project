package com.example.project.data.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class NotificationWorker : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Habit Reminder"

        // 1. Check the Master Switch from AppSettings
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val isMasterNotificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        // 2. Only show visual alert if Master Switch is ON
        if (isMasterNotificationsEnabled) {
            showNotification(context, habitName)
        }

        // 3. We still save to Firestore so the user can see it in their History
        // even if the phone didn't "ping" them.
        val pendingResult = goAsync()
        saveNotificationToFirestore(habitName) {
            pendingResult.finish()
        }
    }

    private fun showNotification(context: Context, habitName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminders_channel"
        val vibrationPattern = longArrayOf(0, 500, 200, 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your daily habits"
                enableVibration(true)
                this.vibrationPattern = vibrationPattern // Fixed the 'val' reassignment issue
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hey!")
            .setContentText("Don't forget: $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(vibrationPattern)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitName.hashCode(), notification)
    }

    private fun saveNotificationToFirestore(title: String, onComplete: () -> Unit) {
        val uid = Firebase.auth.currentUser?.uid ?: run {
            onComplete()
            return
        }

        val notificationData = hashMapOf(
            "title" to title,
            "timestamp" to System.currentTimeMillis(),
            "type" to "REMINDER"
        )

        Firebase.firestore.collection("users").document(uid).collection("notifications")
            .add(notificationData)
            .addOnCompleteListener {
                onComplete() // Signal finish regardless of success or failure
            }
    }
}