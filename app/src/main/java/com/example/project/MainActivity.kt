package com.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.project.data.repository.ThemeManager
import com.example.project.navigation.AppRoot
import com.example.project.ui.theme.ProjectTheme

// Create a CompositionLocal for ThemeManager
val LocalThemeManager = compositionLocalOf<ThemeManager> {
    error("No ThemeManager provided")
}

class MainActivity : ComponentActivity() {
    private lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ThemeManager
        themeManager = ThemeManager(applicationContext)

        setContent {
            // Provide ThemeManager to the entire app
            CompositionLocalProvider(LocalThemeManager provides themeManager) {
                ProjectTheme(themeMode = themeManager.currentTheme.value) {
                    AppRoot()
                }
            }
        }
    }
}