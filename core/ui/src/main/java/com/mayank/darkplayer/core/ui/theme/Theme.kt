package com.mayank.darkplayer.core.ui.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val baseLightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = Color(0xFFF4EFE8),
    onBackground = Color(0xFF1D231E),
    surface = Color(0xFFF4EFE8),
    onSurface = Color(0xFF1D231E),
    surfaceVariant = Color(0xFFD9E1D9),
    onSurfaceVariant = Color(0xFF566058),
    outline = Color(0xFF777785),
    outlineVariant = Color(0xFFC9BDAF),
    scrim = scrimLight,
    inverseSurface = Color(0xFF1A1A1F),
    inverseOnSurface = Color(0xFFF2F2F5),
    inversePrimary = primaryDark,
    surfaceDim = Color(0xFFEAEAF0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFCFBF8),
    surfaceContainerLow = Color(0xFFF9F6F0),
    surfaceContainer = Color(0xFFFCFBF8),
    surfaceContainerHigh = Color(0xFFEFEAE2),
    surfaceContainerHighest = Color(0xFFE5DEC9),
)

private val baseDarkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DarkPlayerTheme(
    darkTheme: Boolean = true,
    highContrastDarkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    DarkPlayerUiPrefs.ensureLoaded(context)

    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (DarkPlayerUiPrefs.themeMode) {
        DarkPlayerThemeMode.SYSTEM -> systemDark
        DarkPlayerThemeMode.DARK -> true
        DarkPlayerThemeMode.LIGHT -> false
        DarkPlayerThemeMode.AMOLED -> true
    }
    val isAmoled = DarkPlayerUiPrefs.themeMode == DarkPlayerThemeMode.AMOLED ||
        (effectiveDark && highContrastDarkTheme)
    val pal = DarkPlayerUiPrefs.palette

    val colorScheme = when {
        effectiveDark && isAmoled -> baseDarkScheme.copy(
            primary = pal.accentDark,
            onPrimary = pal.onAccentDark,
            primaryContainer = pal.accentDark.copy(alpha = 0.22f),
            onPrimaryContainer = Color(0xFFF5F5F8),
            secondary = pal.accentDark,
            onSecondary = pal.onAccentDark,
            background = Color(0xFF000000),
            onBackground = Color(0xFFF5F5F8),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFF5F5F8),
            surfaceDim = Color(0xFF000000),
            surfaceBright = Color(0xFF141418),
            surfaceContainerLowest = Color(0xFF000000),
            surfaceContainerLow = Color(0xFF0A0A0D),
            surfaceContainer = Color(0xFF0F0F13),
            surfaceContainerHigh = Color(0xFF16161B),
            surfaceContainerHighest = Color(0xFF202027),
        )

        effectiveDark -> baseDarkScheme.copy(
            primary = pal.accentDark,
            onPrimary = pal.onAccentDark,
            primaryContainer = pal.accentDark.copy(alpha = 0.22f),
            onPrimaryContainer = Color(0xFFF4EFE8),
            secondary = pal.swatchSecondary,
            onSecondary = Color(0xFF121613),
            background = Color(0xFF0D100E),
            onBackground = Color(0xFFF4EFE8),
            surface = Color(0xFF0D100E),
            onSurface = Color(0xFFF4EFE8),
            onSurfaceVariant = Color(0xFFA7B2A8),
            surfaceContainerLowest = Color(0xFF0A0D0B),
            surfaceContainerLow = Color(0xFF131815),
            surfaceContainer = Color(0xFF171D19),
            surfaceContainerHigh = Color(0xFF1F2621),
            surfaceContainerHighest = Color(0xFF28302A),
            outlineVariant = Color(0xFF2C352E),
        )

        else -> baseLightScheme.copy(
            primary = pal.accentLight,
            onPrimary = pal.onAccentLight,
            primaryContainer = Color(0xFFD9E1D9),
            onPrimaryContainer = Color(0xFF111114),
            secondary = pal.accentLight,
            onSecondary = pal.onAccentLight,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
