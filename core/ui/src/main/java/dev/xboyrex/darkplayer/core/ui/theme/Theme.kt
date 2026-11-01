package dev.xboyrex.darkplayer.core.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlin.math.abs

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

data class ThemePaletteChoice(
    val name: String,
    val hex: String,
)

const val SEED_SOLAR_GOLD = "#FACC15"
const val SEED_CRIMSON = "#FF0033"
const val SEED_ELECTRIC_BLUE = "#0284C7"
const val SEED_AURORA = "#6C2BD9"
const val SEED_LILAC_AIR = "#7454CE"
const val SEED_CREAM_ROSE = "#833761"
const val SEED_JAI = "#D6005C"
const val SEED_PUMPKIN = "#B0714A"
const val SEED_MOONSTONE = "#8A8F98"

val DARK_PLAYER_PALETTES = listOf(
    ThemePaletteChoice("Solar Gold", SEED_SOLAR_GOLD),
    ThemePaletteChoice("Electric", SEED_ELECTRIC_BLUE),
    ThemePaletteChoice("Crimson", SEED_CRIMSON),
    ThemePaletteChoice("Aurora", SEED_AURORA),
    ThemePaletteChoice("Lilac Air", SEED_LILAC_AIR),
    ThemePaletteChoice("Cream Rose", SEED_CREAM_ROSE),
    ThemePaletteChoice("Ruby", SEED_JAI),
    ThemePaletteChoice("Pumpkin", SEED_PUMPKIN),
    ThemePaletteChoice("Moonstone", SEED_MOONSTONE),
)

object ThemeState {
    var accentSeed by mutableStateOf(SEED_SOLAR_GOLD)
    var useSystemFont by mutableStateOf(false)
}

val RoundedHeavy: FontFamily
    get() = FontFamily.Default

private fun hexToHsl(hex: String): Triple<Float, Float, Float> {
    var h2 = hex.removePrefix("#")
    if (h2.length == 3) h2 = h2.map { "$it$it" }.joinToString("")
    val r = h2.substring(0, 2).toInt(16) / 255f
    val g = h2.substring(2, 4).toInt(16) / 255f
    val b = h2.substring(4, 6).toInt(16) / 255f
    val mx = maxOf(r, g, b)
    val mn = minOf(r, g, b)
    val l = (mx + mn) / 2f
    var h = 0f
    var s = 0f
    val d = mx - mn
    if (d != 0f) {
        s = d / (1 - abs(2 * l - 1))
        h = when (mx) {
            r -> (((g - b) / d) % 6)
            g -> ((b - r) / d + 2)
            else -> ((r - g) / d + 4)
        } * 60f
        if (h < 0) h += 360f
    }
    return Triple(h, s * 100f, l * 100f)
}

private fun hsl(h: Float, s: Float, l: Float): Color {
    val sN = s.coerceIn(0f, 100f) / 100f
    val lN = l.coerceIn(0f, 100f) / 100f
    val c = (1 - abs(2 * lN - 1)) * sN
    val x = c * (1 - abs(((h / 60f) % 2) - 1))
    val m = lN - c / 2
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
}

private val lightScheme = lightColorScheme(
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
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
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
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
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

fun schemeForSeed(context: Context, seedHex: String?, dark: Boolean): ColorScheme {
    if (seedHex == null && supportsDynamicTheming()) {
        return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    val hex = seedHex ?: SEED_SOLAR_GOLD
    if (hex.equals(SEED_SOLAR_GOLD, ignoreCase = true)) {
        return if (dark) darkScheme else lightScheme
    }
    val (h, s, _) = hexToHsl(hex)
    val isMoonstone = hex.equals(SEED_MOONSTONE, ignoreCase = true)
    val isPumpkin = hex.equals(SEED_PUMPKIN, ignoreCase = true)
    val accent = when {
        isMoonstone -> 7f
        isPumpkin -> 37f
        else -> maxOf(s, 52f)
    }
    val surf = when {
        isMoonstone -> 3f
        isPumpkin -> 22f
        else -> if (dark) 12f else 14f
    }
    return if (!dark) {
        lightColorScheme(
            primary = hsl(h, accent, 42f),
            onPrimary = Color.White,
            primaryContainer = hsl(h, minOf(accent, 68f), 90f),
            onPrimaryContainer = hsl(h, accent, 16f),
            secondary = hsl(h, 26f, 40f),
            onSecondary = Color.White,
            secondaryContainer = hsl(h, 22f, 88f),
            onSecondaryContainer = hsl(h, 26f, 20f),
            tertiary = hsl((h + 40f) % 360f, 40f, 45f),
            background = hsl(h, surf, 98f),
            onBackground = hsl(h, 10f, 12f),
            surface = hsl(h, surf, 98f),
            onSurface = hsl(h, 10f, 12f),
            surfaceVariant = hsl(h, surf, 90f),
            onSurfaceVariant = hsl(h, minOf(12f, surf), 36f),
            surfaceContainerLowest = hsl(h, surf, 100f),
            surfaceContainerLow = hsl(h, surf, 96f),
            surfaceContainer = hsl(h, surf, 95f),
            surfaceContainerHigh = hsl(h, surf, 93f),
            surfaceContainerHighest = hsl(h, surf, 90f),
            outline = hsl(h, minOf(10f, surf), 54f),
            outlineVariant = hsl(h, minOf(10f, surf), 84f),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD5),
            onErrorContainer = Color(0xFF410E0B),
        )
    } else {
        darkColorScheme(
            primary = hsl(h, minOf(accent, 75f), 76f),
            onPrimary = hsl(h, accent, 14f),
            primaryContainer = hsl(h, minOf(accent, 58f), 30f),
            onPrimaryContainer = hsl(h, 60f, 92f),
            secondary = hsl(h, 22f, 80f),
            onSecondary = hsl(h, 20f, 18f),
            secondaryContainer = hsl(h, 18f, 26f),
            onSecondaryContainer = hsl(h, 18f, 90f),
            tertiary = hsl((h + 40f) % 360f, 40f, 78f),
            background = hsl(h, surf, 6f),
            onBackground = hsl(h, 8f, 94f),
            surface = hsl(h, surf, 6f),
            onSurface = hsl(h, 8f, 96f),
            surfaceVariant = hsl(h, surf, 26f),
            onSurfaceVariant = hsl(h, minOf(10f, surf), 80f),
            surfaceContainerLowest = hsl(h, surf, 4f),
            surfaceContainerLow = hsl(h, surf, 9f),
            surfaceContainer = hsl(h, surf, 12f),
            surfaceContainerHigh = hsl(h, surf, 16f),
            surfaceContainerHighest = hsl(h, surf, 21f),
            outline = hsl(h, minOf(8f, surf), 60f),
            outlineVariant = hsl(h, minOf(8f, surf), 28f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
        )
    }
}

@Composable
private fun SystemBars(dark: Boolean, windowBg: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context as? ComponentActivity
    SideEffect {
        val window = activity?.window ?: (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        if (activity != null) {
            val barStyle = if (dark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                )
            }
            runCatching {
                activity.enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    DisposableEffect(windowBg) {
        (view.context as? Activity)?.window?.setBackgroundDrawable(ColorDrawable(windowBg.toArgb()))
        onDispose { }
    }
}

@Composable
fun DarkPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastDarkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    accentSeed: String = ThemeState.accentSeed,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val baseScheme = when {
        dynamicColor && supportsDynamicTheming() -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> schemeForSeed(context, accentSeed, darkTheme)
    }

    val colorScheme = if (darkTheme && highContrastDarkTheme) {
        baseScheme.copy(
            background = backgroundPureBlack,
            surface = surfacePureBlack,
            surfaceDim = surfaceDimPureBlack,
            surfaceBright = surfaceBrightPureBlack,
            surfaceContainerLowest = surfaceContainerLowestPureBlack,
            surfaceContainerLow = surfaceContainerLowPureBlack,
            surfaceContainer = surfaceContainerPureBlack,
            surfaceContainerHigh = surfaceContainerHighPureBlack,
            surfaceContainerHighest = surfaceContainerHighestPureBlack,
            onBackground = Color(0xFFF5F5F5),
            onSurface = Color(0xFFF5F5F5),
            outlineVariant = Color(0xFF1F1F1F),
        )
    } else {
        baseScheme
    }

    SystemBars(dark = darkTheme, windowBg = colorScheme.background)

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@Composable
fun NextPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastDarkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    DarkPlayerTheme(
        darkTheme = darkTheme,
        highContrastDarkTheme = highContrastDarkTheme,
        dynamicColor = dynamicColor,
        content = content,
    )
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
