package com.example.project.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.project.navigation.Screen

@Composable
fun NavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // Habits
        NavigationBarItem(
            selected = currentRoute == Screen.Habits.route,
            onClick = {
                navController.navigate(Screen.Habits.route) {
                    popUpTo(Screen.Habits.route) { inclusive = true }
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Habits",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Habits") },
            colors = itemColors
        )

        // Progress
        NavigationBarItem(
            selected = currentRoute == Screen.Progress.route,
            onClick = {
                navController.navigate(Screen.Progress.route) {
                    popUpTo(Screen.Habits.route)
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Progress",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Progress") },
            colors = itemColors
        )

        // Hero
        NavigationBarItem(
            selected = currentRoute == Screen.Hero.route,
            onClick = {
                navController.navigate(Screen.Hero.route) {
                    popUpTo(Screen.Habits.route)
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Hero",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Hero") },
            colors = itemColors
        )

        // Account
        NavigationBarItem(
            selected = currentRoute == Screen.Account.route,
            onClick = {
                navController.navigate(Screen.Account.route) {
                    popUpTo(Screen.Habits.route)
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Account",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Account") },
            colors = itemColors
        )
    }
}