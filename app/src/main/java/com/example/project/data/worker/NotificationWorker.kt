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

        // 1. Show the visual notification
        showNotification(context, habitName)

        // 2. Async Firestore Write
        val pendingResult = goAsync()
        saveNotificationToFirestore(habitName) {
            pendingResult.finish()
        }
        // Alarms should be managed by your HabitViewModel to ensure the time stays consistent
        // with what the user actually selected in the Dialog.
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