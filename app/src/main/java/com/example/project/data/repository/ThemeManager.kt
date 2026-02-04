package com.example.project.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import com.example.project.ui.theme.ThemeMode

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var currentTheme = mutableStateOf(getSavedTheme())
        private set

    fun setTheme(mode: ThemeMode) {
        currentTheme.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    private fun getSavedTheme(): ThemeMode {
        val savedMode = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(savedMode ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }
}