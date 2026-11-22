package com.mayank.darkplayer.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import com.mayank.darkplayer.core.ui.R

val DarkPlayerFontFamily: FontFamily = FontFamily(
    Font(resId = R.font.darkplayer_font, weight = FontWeight.Normal),
    Font(resId = R.font.darkplayer_font, weight = FontWeight.Medium),
    Font(resId = R.font.darkplayer_font, weight = FontWeight.SemiBold),
    Font(resId = R.font.darkplayer_font, weight = FontWeight.Bold),
)

private val DefaultTypography = Typography()

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = DarkPlayerFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = DarkPlayerFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = DarkPlayerFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = DarkPlayerFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = DarkPlayerFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = DarkPlayerFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = DarkPlayerFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = DarkPlayerFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = DarkPlayerFontFamily),
    bodyLarge = TextStyle(
        fontFamily = DarkPlayerFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = DarkPlayerFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = DarkPlayerFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = DarkPlayerFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = DarkPlayerFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = DarkPlayerFontFamily),
)
