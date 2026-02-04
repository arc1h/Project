package com.example.project.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.project.R
import com.example.project.ui.theme.PixelatedFont

val PixelatedFont = FontFamily(
    Font(
        resId = R.font.pixel_cond,
        weight = FontWeight.Normal
    )
)

val HeroTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = PixelatedFont
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = PixelatedFont
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = PixelatedFont
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = PixelatedFont
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = PixelatedFont
    )
)