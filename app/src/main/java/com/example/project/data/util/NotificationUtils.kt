package com.example.project.data.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.project.data.notifications.NotificationWorker
import java.util.Calendar

fun scheduleHabitAlarm(context: Context, habitName: String, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Check if we have permission to schedule exact alarms (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            // Log it or show a Toast. You'll need to ask the user for permission in Settings.
            Log.e("AlarmUtils", "Cannot schedule exact alarm: Permission missing")
            return
        }
    }

    val intent = Intent(context, NotificationWorker::class.java).apply {
        putExtra("HABIT_NAME", habitName)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        habitName.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DATE, 1)
        }
    }

    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    } catch (e: SecurityException) {
        Log.e("AlarmUtils", "SecurityException: Exact alarm permission revoked", e)
    }
}

fun cancelHabitAlarm(context: Context, habitName: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationWorker::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        habitName.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}