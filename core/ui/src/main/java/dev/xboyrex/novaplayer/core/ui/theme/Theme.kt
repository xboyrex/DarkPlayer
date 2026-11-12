package dev.xboyrex.novaplayer.core.ui.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val pureWhiteScheme = lightColorScheme(
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
    background = Color.White,
    onBackground = onBackgroundLight,
    surface = Color.White,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val pureAmoledBlackScheme = darkColorScheme(
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
    background = Color.Black,
    onBackground = onBackgroundDark,
    surface = Color.Black,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = Color.Black,
    surfaceBright = surfaceBrightPureBlack,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = surfaceContainerLowPureBlack,
    surfaceContainer = surfaceContainerPureBlack,
    surfaceContainerHigh = surfaceContainerHighPureBlack,
    surfaceContainerHighest = surfaceContainerHighestPureBlack,
)

/**
 * Nova Player strictly supports two high-contrast visual modes:
 * 1. Pure AMOLED Black (`darkTheme = true`) — `#000000` background & surface
 * 2. Pure White (`darkTheme = false`) — `#FFFFFF` background & surface
 */
@Composable
fun NovaPlayerTheme(
    darkTheme: Boolean = true,
    highContrastDarkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && supportsDynamicTheming() -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context = context).copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceDim = Color.Black,
                    surfaceBright = surfaceBrightPureBlack,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = surfaceContainerLowPureBlack,
                    surfaceContainer = surfaceContainerPureBlack,
                    surfaceContainerHigh = surfaceContainerHighPureBlack,
                    surfaceContainerHighest = surfaceContainerHighestPureBlack,
                    onBackground = Color(0xFFF5F5F7),
                    onSurface = Color(0xFFF5F5F7),
                )
            } else {
                dynamicLightColorScheme(context = context).copy(
                    background = Color.White,
                    surface = Color.White,
                    surfaceBright = Color.White,
                    surfaceContainerLowest = Color.White,
                    surfaceContainerLow = surfaceContainerLowLight,
                    surfaceContainer = surfaceContainerLight,
                    surfaceContainerHigh = surfaceContainerHighLight,
                    surfaceContainerHighest = surfaceContainerHighestLight,
                    onBackground = Color(0xFF0A0A0A),
                    onSurface = Color(0xFF0A0A0A),
                )
            }
        }
        darkTheme -> pureAmoledBlackScheme
        else -> pureWhiteScheme
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
