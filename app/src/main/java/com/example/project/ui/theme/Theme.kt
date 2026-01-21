package com.example.project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    onPrimary = White,
    background = DarkBackground,
    surface = DarkBackground,
    onSurface = White,
    onBackground = White,
    outline = DarkGray
)

private val LightColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = White,
    background = White,
    surface = White,
    onSurface = Black,
    onBackground = Black,
    outline = LightGray
)

@Composable
fun ProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // turn off dynamicColor by default so Material You doesn't change colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}