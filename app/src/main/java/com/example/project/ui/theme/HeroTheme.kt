package com.example.project.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.project.R

val PixelFont = FontFamily(
    Font(
        resId = R.font.pixel,
        weight = FontWeight.SemiBold
    )
)

val PixelTitle = FontFamily(
    Font(
        resId = R.font.pixel_title,
        weight = FontWeight.Bold
    )
)

val HeroTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = PixelFont
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = PixelFont
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = PixelFont
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = PixelFont
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = PixelFont
    )
)