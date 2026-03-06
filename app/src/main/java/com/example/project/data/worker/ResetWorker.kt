package com.example.project.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.project.data.model.Habit
import com.example.project.data.util.toHabit
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class ResetWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = Firebase.firestore
        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()

        // 1. Define "Today" at the start so all logic uses the same reference
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis
        val now = System.currentTimeMillis()

        try {
            val habitsRef = db.collection("users").document(uid).collection("habits")
            val snapshot = habitsRef.get().await()

            var missedCount = 0

            db.runBatch { batch ->
                for (doc in snapshot.documents) {
                    val habit = doc.toHabit() ?: continue
                    val type = habit.frequency.getTypeName()

                    // Logic: Should the checkbox clear so they can do it again?
                    val isNewWindow = if (type == "daily") {
                        // Uncheck if the last time they did it was BEFORE 12:00 AM today
                        habit.lastCompleted?.let { it < startOfToday } ?: true
                    } else {
                        // For non-daily, check if a full frequency cycle has passed
                        habit.lastCompleted?.let { (now - it) > habit.frequency.toMillis() } ?: true
                    }

                    if (isNewWindow && habit.isCompleted) {
                        batch.update(doc.reference, "completed", false)
                    }

                    // Logic: Is the streak actually DEAD? (Pass startOfToday as a parameter)
                    if (isHabitOverdue(habit, startOfToday)) {
                        missedCount++
                        batch.update(doc.reference, mapOf(
                            "streak" to 0,
                            "skippedCount" to habit.skippedCount + 1,
                            "completed" to false
                        ))
                    }
                }
            }.await()

            if (missedCount > 0) {
                val message = if (missedCount == 1) "You missed a habit! Streak reset." else "You missed $missedCount habits! Streaks reset."
                sendResetNotification(message)
                saveResetToFirestore(uid, message)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("ResetWorker", "Failed to reset habits", e)
            return Result.retry()
        }
    }

    private fun isHabitOverdue(habit: Habit, startOfToday: Long): Boolean {
        val lastTime = habit.lastCompleted ?: return false
        val now = System.currentTimeMillis()

        return when (habit.frequency.getTypeName()) {
            "daily" -> {
                // Gone if last done BEFORE yesterday's 12:00 AM
                val startOfYesterday = startOfToday - (24 * 60 * 60 * 1000L)
                lastTime < startOfYesterday
            }
            "weekly" -> {
                val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L
                val gracePeriod = 24 * 60 * 60 * 1000L
                (now - lastTime) > (oneWeekInMillis + gracePeriod)
            }
            else -> {
                (now - lastTime) > (habit.frequency.toMillis() + 3600000L)
            }
        }
    }

    private fun sendResetNotification(message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reset_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Streak Resets", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Streak Lost!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, notification)
    }

    private suspend fun saveResetToFirestore(uid: String, message: String) {
        val notificationData = hashMapOf(
            "title" to message,
            "timestamp" to System.currentTimeMillis(),
            "type" to "RESET" // Different type for visual styling if you want
        )
        try {
            Firebase.firestore.collection("users").document(uid)
                .collection("notifications").add(notificationData).await()
        } catch (e: Exception) {
            Log.e("ResetWorker", "Firestore write failed", e)
        }
    }
}