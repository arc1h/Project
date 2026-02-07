package com.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.project.data.repository.ThemeManager
import com.example.project.navigation.AppRoot
import com.example.project.ui.theme.ProjectTheme
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.project.data.worker.ResetWorker
import java.util.concurrent.TimeUnit

// Create a CompositionLocal for ThemeManager
val LocalThemeManager = compositionLocalOf<ThemeManager> {
    error("No ThemeManager provided")
}

class MainActivity : ComponentActivity() {
    private lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themeManager = ThemeManager(applicationContext)

        // --- SCHEDULE THE RESET WORKER ---
        scheduleHabitReset(applicationContext)

        setContent {
            CompositionLocalProvider(LocalThemeManager provides themeManager) {
                ProjectTheme(themeMode = themeManager.currentTheme.value) {
                    AppRoot()
                }
            }
        }
    }

    private fun scheduleHabitReset(context: android.content.Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Needs internet for Firestore
            .build()

        // Runs every 12 hours to check for missed habits
        val resetRequest = PeriodicWorkRequestBuilder<ResetWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "HabitResetWork",
            ExistingPeriodicWorkPolicy.KEEP, // Keeps existing worker if already scheduled
            resetRequest
        )
    }
}