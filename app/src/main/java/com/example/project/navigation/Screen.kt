package com.example.project.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {

    object Splash : Screen("splash", "Splash", Icons.Default.Star)
    object Login : Screen("login", "Login", Icons.Default.Add)
    object Habits : Screen("habits", "Habits", Icons.Default.Home)
    object Progress : Screen("progress", "Progress", Icons.Default.BarChart)
    object Hero : Screen("hero", "Hero", Icons.Default.Person)
    object Account : Screen("account", "Account", Icons.Default.Settings)
}
