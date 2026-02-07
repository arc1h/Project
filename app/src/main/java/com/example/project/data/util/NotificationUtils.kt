package com.example.project.data.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.project.data.worker.NotificationWorker
import java.util.Calendar

fun scheduleHabitAlarm(context: Context, habitName: String, hour: Int, minute: Int) {
    val intent = Intent(context, NotificationWorker::class.java).apply {
        putExtra("HABIT_NAME", habitName)
    }

    // FIX: Use habitName.hashCode() so it matches the cancel function
    val requestCode = habitName.hashCode()

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        // If the time has already passed today, schedule for tomorrow
        if (before(Calendar.getInstance())) {
            add(Calendar.DATE, 1)
        }
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            // Option A: Fallback to a non-exact alarm (might be late)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            // Option B: Ask user to enable permission in Settings
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    } else {
        // For older Android versions, no check is needed
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
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