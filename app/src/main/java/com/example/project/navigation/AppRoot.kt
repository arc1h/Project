package com.example.project.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project.LocalThemeManager
import com.example.project.data.viewmodel.HeroViewModel
import com.example.project.data.viewmodel.UserViewModel
import com.example.project.ui.screens.AccountDetails
import com.example.project.ui.screens.AccountScreen
import com.example.project.ui.screens.AppSettings
import com.example.project.ui.screens.FriendHero
import com.example.project.ui.screens.HabitsScreen
import com.example.project.ui.screens.HeroScreen
import com.example.project.ui.screens.History
import com.example.project.ui.screens.LoginScreen
import com.example.project.ui.screens.Notifications
import com.example.project.ui.screens.ProfileSearch
import com.example.project.ui.screens.ProgressScreen
import com.example.project.ui.screens.Shop
import com.example.project.ui.screens.SplashScreen
import com.example.project.ui.theme.HeroTypography
import com.example.project.ui.theme.NavigationBar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val userViewModel: UserViewModel = viewModel()
    val heroViewModel: HeroViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()

    // Get ThemeManager from CompositionLocal
    val themeManager = LocalThemeManager.current

    var showSplash by remember { mutableStateOf(true) }
    var initialDestination by remember { mutableStateOf<String?>(null) }

    // Handle splash screen and initial navigation
    LaunchedEffect(Unit) {
        delay(2500) // Show splash for 2.5 seconds

        initialDestination = if (auth.currentUser == null) {
            Screen.Login.route
        } else {
            Screen.Habits.route
        }

        // Small delay to ensure destination is set
        delay(100)
        showSplash = false
    }

    // Show splash until initial destination is determined
    if (showSplash || initialDestination == null) {
        SplashScreen()
        return
    }

    // Main app with navigation
    Scaffold(
        bottomBar = {
            val currentRoute = navController
                .currentBackStackEntryAsState().value?.destination?.route
            val isHero = currentRoute == Screen.Hero.route

            if (currentRoute !in listOf(Screen.Splash.route, Screen.Login.route)) {
                if (isHero) {
                    MaterialTheme(typography = HeroTypography) {
                        NavigationBar(navController)
                    }
                } else {
                    NavigationBar(navController)
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = initialDestination!!,
            modifier = Modifier
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    navController = navController,
                    onLoginSuccess = {
                        // Refresh user data when user logs in
                        userViewModel.refresh()
                    }
                )
            }

            composable(Screen.Habits.route) {
                HabitsScreen(userViewModel = userViewModel)
            }

            composable(Screen.Progress.route) {
                ProgressScreen()
            }

            composable(Screen.Hero.route) {
                HeroScreen(
                    heroViewModel = heroViewModel,
                    navController = navController
                )
            }

            composable("friend_hero/{friendUid}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("friendUid") ?: ""
                FriendHero(navController, id)
            }

            composable("shop") {
                Shop(navController = navController)
            }

            composable(Screen.Account.route) {
                AccountScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }

            // Secondary screens
            composable("profile_search") {
                ProfileSearch(navController = navController)
            }

            composable("notifications") {
                Notifications(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }

            composable("history") {
                History(
                    navController = navController
                )
            }

            composable("app_settings") {
                AppSettings(
                    navController = navController,
                    currentThemeMode = themeManager.currentTheme.value,
                    onThemeModeChange = { newMode ->
                        themeManager.setTheme(newMode)
                    }
                )
            }

            composable("account_details") {
                AccountDetails(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
        }
    }
}