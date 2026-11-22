#!/usr/bin/env python3
"""
Dark Player Automated Builder (build_darkplayer.py)
===================================================
Transforms a Next Player Android (Kotlin / Jetpack Compose / Media3) source tree
into Dark Player (`com.mayank.darkplayer`) featuring:
  - Zero-trace rebranding & package migration (`com.mayank.darkplayer`)
  - Navy Dark Graphite design system (#0E0E10 .. #F2F2F5)
  - Liquid Glass floating bottom navigation bar (Android 12+ RenderEffect blur)
  - Redesigned Player Controls with HW, Subtitles, Audio, PIP, Speed, Lock,
    Rotate, Resize (Crop/Fit), HDR/SDR, and Screenshot capture
  - YTXTube-inspired Material 3 Settings UI & GitHub Releases Auto-Updater
  - Bug fixes: Crop/Fit aspect ratio & overlay overlap fix, pitch-locked 2x speed
  - Custom launcher logo & global custom .ttf typography injection
  - Automatic repackaging into DarkPlayer.zip

Usage:
    python3 build_darkplayer.py <nextplayer_source_dir_or_zip> \
        [--logo path/to/logo.png] \
        [--font path/to/custom.ttf] \
        [--output DarkPlayer.zip] \
        [--repo owner/repo] \
        [--purge-external-maven]
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

# =============================================================================
# CONFIGURATION CONSTANTS
# =============================================================================

NEW_PACKAGE = "com.mayank.darkplayer"
NEW_PACKAGE_PATH = Path("com") / "mayank" / "darkplayer"
OLD_PACKAGE = "dev.anilbeesetti.nextplayer"
OLD_PACKAGE_PATH = Path("dev") / "anilbeesetti" / "nextplayer"

NEW_APP_NAME = "Dark Player"
NEW_APP_SLUG = "DarkPlayer"
DEFAULT_GITHUB_REPO = "xboyrex/DarkPlayer"
DEFAULT_DEVELOPER = "Mayank"

# Navy Dark Color Palette (Graphite 7-Shade System)
PALETTE = {
    "background": "#0E0E10",
    "surface": "#232327",
    "surface_secondary": "#3E3E45",
    "border": "#63636E",
    "text_muted": "#9A9AA6",
    "text_secondary": "#C9C9D2",
    "text_primary": "#F2F2F5",
}

TEXT_EXTENSIONS = {
    ".kt", ".kts", ".java", ".xml", ".gradle", ".properties", ".pro",
    ".toml", ".json", ".md", ".txt", ".yml", ".yaml", ".cfg", ".conf",
    ".gitignore", ".editorconfig", ".fastfile", ".appfile",
}

SPECIAL_TEXT_FILENAMES = {
    "LICENSE", "PRIVACY", "README", "Gemfile", "Appfile", "Fastfile",
    "gradlew", "gradlew.bat", "AGENTS.md",
}

SKIP_DIRS = {".git", ".gradle", ".idea", "build", ".cxx", "__pycache__"}

# Directories/files specific to upstream release automation & funding to remove
UPSTREAM_PRUNE_PATHS = [
    ".agents",
    "docs/agents",
    ".github/FUNDING.yml",
    ".github/workflows/android_build.yaml",
    ".github/workflows/bump-version.yaml",
    ".github/workflows/publish.yaml",
    "fastlane",
    "Gemfile",
]

# Ordered regex replacements for zero-trace first-party purge
# External Maven coordinates (io.github.anilbeesetti.nextlib) are protected via
# placeholder tokens unless --purge-external-maven is explicitly enabled.
NEXTLIB_MAVEN_SENTINEL = "__DARKPLAYER_EXTERNAL_MEDIA3_EXT__"
NEXTLIB_IMPORT_SENTINEL = "__DARKPLAYER_EXTERNAL_MEDIA3_IMP__"
NEXTLIB_TOML_SENTINEL = "__DARKPLAYER_EXTERNAL_TOML_GROUP__"

ORDERED_REPLACEMENTS: List[Tuple[re.Pattern[str], str]] = [
    # Exact package & namespace migrations
    (re.compile(r"dev\.anilbeesetti\.nextplayer"), NEW_PACKAGE),
    (re.compile(r"dev/anilbeesetti/nextplayer"), "com/mayank/darkplayer"),
    # Upstream GitHub / funding / contact URLs
    (re.compile(r"https?://github\.com/anilbeesetti/nextplayer", re.IGNORECASE),
     f"https://github.com/{DEFAULT_GITHUB_REPO}"),
    (re.compile(r"https?://github\.com/sponsors/anilbeesetti", re.IGNORECASE),
     f"https://github.com/sponsors/{DEFAULT_GITHUB_REPO.split('/')[0]}"),
    (re.compile(r"https?://ko-fi\.com/anilbeesetti", re.IGNORECASE),
     f"https://github.com/{DEFAULT_GITHUB_REPO}"),
    (re.compile(r"https?://paypal\.me/AnilBeesetti", re.IGNORECASE),
     f"https://github.com/{DEFAULT_GITHUB_REPO}"),
    (re.compile(r"https?://pay\.upilink\.in/pay/[^\s\"']+"),
     f"https://github.com/{DEFAULT_GITHUB_REPO}"),
    (re.compile(r"anilbeesetti\d*@[^\s\"']+"), "contact@darkplayer.dev"),
    (re.compile(r"https?://github\.com/Koustubh12345/YTXTube", re.IGNORECASE),
     f"https://github.com/{DEFAULT_GITHUB_REPO}"),
    (re.compile(r"anilbeesetti/nextplayer", re.IGNORECASE), DEFAULT_GITHUB_REPO),
    (re.compile(r"Koustubh12345/YTXTube", re.IGNORECASE), DEFAULT_GITHUB_REPO),
    # Class, Theme, and Application symbol renames
    (re.compile(r"\bNextPlayerApplication\b"), "DarkPlayerApplication"),
    (re.compile(r"\bNextPlayerTheme\b"), "DarkPlayerTheme"),
    (re.compile(r"\bTheme\.NextPlayer\b"), "Theme.DarkPlayer"),
    (re.compile(r"\bNextNavigationBar\b"), "DarkNavigationBar"),
    (re.compile(r"\bNextNavigationRail\b"), "DarkNavigationRail"),
    (re.compile(r'rootProject\.name\s*=\s*"NextPlayer"'), f'rootProject.name = "{NEW_APP_SLUG}"'),
    # Gradle version catalog alias cleanup
    (re.compile(r"github-anilbeesetti-nextlib-media3ext"), "darkplayer-media3ext"),
    (re.compile(r"github-anilbeesetti-nextlib-mediainfo"), "darkplayer-mediainfo"),
    (re.compile(r"libs\.github\.anilbeesetti\.nextlib\.media3ext"), "libs.darkplayer.media3ext"),
    (re.compile(r"libs\.github\.anilbeesetti\.nextlib\.mediainfo"), "libs.darkplayer.mediainfo"),
    # Brand & author names (case-aware then case-insensitive catch-all)
    (re.compile(r"Anil Kumar Beesetti", re.IGNORECASE), DEFAULT_DEVELOPER),
    (re.compile(r"Next Player Debug"), f"{NEW_APP_NAME} Debug"),
    (re.compile(r"Next Player Activity"), f"{NEW_APP_NAME} Activity"),
    (re.compile(r"Next Player", re.IGNORECASE), NEW_APP_NAME),
    (re.compile(r"NextPlayer"), NEW_APP_SLUG),
    (re.compile(r"nextplayer", re.IGNORECASE), "darkplayer"),
    (re.compile(r"YTXTube", re.IGNORECASE), NEW_APP_SLUG),
    (re.compile(r"Koustubh12345", re.IGNORECASE), DEFAULT_DEVELOPER),
    (re.compile(r"anilbeesetti", re.IGNORECASE), DEFAULT_DEVELOPER.lower()),
]


# =============================================================================
# GENERATED SOURCE TEMPLATES
# =============================================================================

COLORS_XML = f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Dark Player Navy Dark Graphite Palette -->
    <color name="dark_bg">{PALETTE["background"]}</color>
    <color name="dark_surface">{PALETTE["surface"]}</color>
    <color name="dark_surface_secondary">{PALETTE["surface_secondary"]}</color>
    <color name="dark_border">{PALETTE["border"]}</color>
    <color name="dark_text_muted">{PALETTE["text_muted"]}</color>
    <color name="dark_text_secondary">{PALETTE["text_secondary"]}</color>
    <color name="dark_text_primary">{PALETTE["text_primary"]}</color>

    <color name="player_background">#CC0E0E10</color>
    <color name="transparent_white_300">#4DF2F2F5</color>
    <color name="seed">{PALETTE["surface"]}</color>

    <!-- Material 3 Light Fallback (mapped cleanly to high-contrast Graphite) -->
    <color name="md_theme_light_primary">#232327</color>
    <color name="md_theme_light_primaryContainer">#3E3E45</color>
    <color name="md_theme_light_onPrimary">#F2F2F5</color>
    <color name="md_theme_light_onPrimaryContainer">#F2F2F5</color>
    <color name="md_theme_light_secondary">#3E3E45</color>
    <color name="md_theme_light_secondaryContainer">#C9C9D2</color>
    <color name="md_theme_light_onSecondary">#F2F2F5</color>
    <color name="md_theme_light_onSecondaryContainer">#0E0E10</color>
    <color name="md_theme_light_tertiary">#63636E</color>
    <color name="md_theme_light_tertiaryContainer">#C9C9D2</color>
    <color name="md_theme_light_onTertiary">#F2F2F5</color>
    <color name="md_theme_light_onTertiaryContainer">#0E0E10</color>
    <color name="md_theme_light_error">#CF6679</color>
    <color name="md_theme_light_errorContainer">#93000A</color>
    <color name="md_theme_light_onError">#0E0E10</color>
    <color name="md_theme_light_onErrorContainer">#FFDAD6</color>
    <color name="md_theme_light_outline">#63636E</color>
    <color name="md_theme_light_background">#F2F2F5</color>
    <color name="md_theme_light_onBackground">#0E0E10</color>
    <color name="md_theme_light_surface">#F2F2F5</color>
    <color name="md_theme_light_onSurface">#0E0E10</color>
    <color name="md_theme_light_surfaceVariant">#C9C9D2</color>
    <color name="md_theme_light_onSurfaceVariant">#3E3E45</color>
    <color name="md_theme_light_inverseSurface">#232327</color>
    <color name="md_theme_light_inverseOnSurface">#F2F2F5</color>
    <color name="md_theme_light_inversePrimary">#C9C9D2</color>
    <color name="md_theme_light_shadow">#000000</color>
    <color name="md_theme_light_surfaceTint">#3E3E45</color>
    <color name="md_theme_light_outlineVariant">#9A9AA6</color>
    <color name="md_theme_light_scrim">#000000</color>

    <!-- Material 3 Navy Dark Primary Theme -->
    <color name="md_theme_dark_primary">{PALETTE["text_primary"]}</color>
    <color name="md_theme_dark_primaryContainer">{PALETTE["surface_secondary"]}</color>
    <color name="md_theme_dark_onPrimary">{PALETTE["background"]}</color>
    <color name="md_theme_dark_onPrimaryContainer">{PALETTE["text_primary"]}</color>
    <color name="md_theme_dark_secondary">{PALETTE["text_secondary"]}</color>
    <color name="md_theme_dark_secondaryContainer">{PALETTE["surface_secondary"]}</color>
    <color name="md_theme_dark_onSecondary">{PALETTE["background"]}</color>
    <color name="md_theme_dark_onSecondaryContainer">{PALETTE["text_primary"]}</color>
    <color name="md_theme_dark_tertiary">{PALETTE["text_muted"]}</color>
    <color name="md_theme_dark_tertiaryContainer">{PALETTE["surface"]}</color>
    <color name="md_theme_dark_onTertiary">{PALETTE["background"]}</color>
    <color name="md_theme_dark_onTertiaryContainer">{PALETTE["text_secondary"]}</color>
    <color name="md_theme_dark_error">#FFB4AB</color>
    <color name="md_theme_dark_errorContainer">#93000A</color>
    <color name="md_theme_dark_onError">#690005</color>
    <color name="md_theme_dark_onErrorContainer">#FFDAD6</color>
    <color name="md_theme_dark_outline">{PALETTE["border"]}</color>
    <color name="md_theme_dark_background">{PALETTE["background"]}</color>
    <color name="md_theme_dark_onBackground">{PALETTE["text_primary"]}</color>
    <color name="md_theme_dark_surface">{PALETTE["background"]}</color>
    <color name="md_theme_dark_onSurface">{PALETTE["text_primary"]}</color>
    <color name="md_theme_dark_surfaceVariant">{PALETTE["surface_secondary"]}</color>
    <color name="md_theme_dark_onSurfaceVariant">{PALETTE["text_secondary"]}</color>
    <color name="md_theme_dark_inverseSurface">{PALETTE["text_primary"]}</color>
    <color name="md_theme_dark_inverseOnSurface">{PALETTE["background"]}</color>
    <color name="md_theme_dark_inversePrimary">{PALETTE["surface_secondary"]}</color>
    <color name="md_theme_dark_shadow">#000000</color>
    <color name="md_theme_dark_surfaceTint">{PALETTE["surface"]}</color>
    <color name="md_theme_dark_outlineVariant">{PALETTE["border"]}</color>
    <color name="md_theme_dark_scrim">#000000</color>
</resources>
"""

COLOR_KT = f"""package {NEW_PACKAGE}.core.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Player Navy Dark Graphite 7-Shade Palette
val NavyBackground = Color(0xFF0E0E10)
val NavySurface = Color(0xFF232327)
val NavySurfaceSecondary = Color(0xFF3E3E45)
val NavyBorder = Color(0xFF63636E)
val NavyTextMuted = Color(0xFF9A9AA6)
val NavyTextSecondary = Color(0xFFC9C9D2)
val NavyTextPrimary = Color(0xFFF2F2F5)

// Translucent Liquid Glass Tokens
val GlassPillFill = Color(0xC2232327)
val GlassActivePillFill = Color(0xCC3E3E45)
val GlassRimBorder = Color(0x8063636E)
val PlayerControlPillFill = Color(0xD9F2F2F5)
val PlayerControlPillIcon = Color(0xFF0E0E10)
val PlayerTimePillFill = Color(0xD9C9C9D2)

// Material 3 Dark Scheme Tokens (Navy Dark)
val primaryDark = NavyTextPrimary
val onPrimaryDark = NavyBackground
val primaryContainerDark = NavySurfaceSecondary
val onPrimaryContainerDark = NavyTextPrimary
val secondaryDark = NavyTextSecondary
val onSecondaryDark = NavyBackground
val secondaryContainerDark = NavySurfaceSecondary
val onSecondaryContainerDark = NavyTextPrimary
val tertiaryDark = NavyTextMuted
val onTertiaryDark = NavyBackground
val tertiaryContainerDark = NavySurface
val onTertiaryContainerDark = NavyTextSecondary
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = NavyBackground
val onBackgroundDark = NavyTextPrimary
val surfaceDark = NavyBackground
val onSurfaceDark = NavyTextPrimary
val surfaceVariantDark = NavySurfaceSecondary
val onSurfaceVariantDark = NavyTextSecondary
val outlineDark = NavyBorder
val outlineVariantDark = NavyBorder
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = NavyTextPrimary
val inverseOnSurfaceDark = NavyBackground
val inversePrimaryDark = NavySurfaceSecondary
val surfaceDimDark = NavyBackground
val surfaceBrightDark = NavySurfaceSecondary
val surfaceContainerLowestDark = NavyBackground
val surfaceContainerLowDark = Color(0xFF18181B)
val surfaceContainerDark = NavySurface
val surfaceContainerHighDark = Color(0xFF2E2E34)
val surfaceContainerHighestDark = NavySurfaceSecondary

// Material 3 Light Scheme Tokens (Graphite Light)
val primaryLight = NavySurface
val onPrimaryLight = NavyTextPrimary
val primaryContainerLight = NavyTextSecondary
val onPrimaryContainerLight = NavyBackground
val secondaryLight = NavySurfaceSecondary
val onSecondaryLight = NavyTextPrimary
val secondaryContainerLight = Color(0xFFE2E2E8)
val onSecondaryContainerLight = NavyBackground
val tertiaryLight = NavyBorder
val onTertiaryLight = NavyTextPrimary
val tertiaryContainerLight = Color(0xFFE6E6EC)
val onTertiaryContainerLight = NavyBackground
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF93000A)
val backgroundLight = NavyTextPrimary
val onBackgroundLight = NavyBackground
val surfaceLight = NavyTextPrimary
val onSurfaceLight = NavyBackground
val surfaceVariantLight = NavyTextSecondary
val onSurfaceVariantLight = NavySurfaceSecondary
val outlineLight = NavyBorder
val outlineVariantLight = NavyTextMuted
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = NavySurface
val inverseOnSurfaceLight = NavyTextPrimary
val inversePrimaryLight = NavyTextSecondary
val surfaceDimLight = Color(0xFFDCDCE2)
val surfaceBrightLight = NavyTextPrimary
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFECECF0)
val surfaceContainerLight = Color(0xFFE4E4EA)
val surfaceContainerHighLight = Color(0xFFDADAE2)
val surfaceContainerHighestLight = NavyTextSecondary

// Pure Black OLED Variant
val backgroundPureBlack = Color(0xFF000000)
val surfacePureBlack = Color(0xFF000000)
val surfaceDimPureBlack = Color(0xFF000000)
val surfaceBrightPureBlack = NavySurface
val surfaceContainerLowestPureBlack = Color(0xFF000000)
val surfaceContainerLowPureBlack = Color(0xFF0B0B0D)
val surfaceContainerPureBlack = Color(0xFF141417)
val surfaceContainerHighPureBlack = NavySurface
val surfaceContainerHighestPureBlack = NavySurfaceSecondary

val seed = NavySurface
"""

THEME_KT = f"""package {NEW_PACKAGE}.core.ui.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DarkPlayerTheme(
    darkTheme: Boolean = true,
    highContrastDarkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {{
    // Enforce exact Navy Dark Graphite palette (#0E0E10 .. #F2F2F5) without wallpaper tinting
    val colorScheme = when {{
        darkTheme && highContrastDarkTheme -> darkScheme.copy(
            background = backgroundPureBlack,
            surface = surfacePureBlack,
            surfaceDim = surfaceDimPureBlack,
            surfaceBright = surfaceBrightPureBlack,
            surfaceContainerLowest = surfaceContainerLowestPureBlack,
            surfaceContainerLow = surfaceContainerLowPureBlack,
            surfaceContainer = surfaceContainerPureBlack,
            surfaceContainerHigh = surfaceContainerHighPureBlack,
            surfaceContainerHighest = surfaceContainerHighestPureBlack,
        )

        darkTheme -> darkScheme
        else -> lightScheme
    }}

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
"""


def build_type_kt(has_custom_font: bool) -> str:
    if has_custom_font:
        font_import = (
            f"import androidx.compose.ui.text.font.Font\n"
            f"import {NEW_PACKAGE}.core.ui.R\n"
        )
        font_decl = """val DarkPlayerFontFamily: FontFamily = FontFamily(
    Font(resId = R.font.darkplayer_font, weight = FontWeight.Normal),
    Font(resId = R.font.darkplayer_font, weight = FontWeight.Medium),
    Font(resId = R.font.darkplayer_font, weight = FontWeight.SemiBold),
    Font(resId = R.font.darkplayer_font, weight = FontWeight.Bold),
)"""
    else:
        font_import = ""
        font_decl = "val DarkPlayerFontFamily: FontFamily = FontFamily.SansSerif"

    return f"""package {NEW_PACKAGE}.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
{font_import}
{font_decl}

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
"""



GLASS_MOTION_KT = f"""package {NEW_PACKAGE}.navigation

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

internal object NavGlassSupport {{
    val supported: Boolean
        get() = runCatching {{ isRuntimeShaderSupported() }}.getOrDefault(false)
}}

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {{
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)
    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {{
        inspectLiquidDragGestures(
            onDragStart = {{ down ->
                onDragStarted(down.position)
                press()
            }},
            onDragEnd = {{
                onDragStopped()
                release()
            }},
            onDragCancel = {{
                onDragStopped()
                release()
            }},
        ) {{ _, dragAmount ->
            onDrag(size, dragAmount)
        }}
    }}

    fun press() {{
        velocityTracker.resetTracking()
        animationScope.launch {{
            launch {{ pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }}
            launch {{ scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }}
            launch {{ scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }}
        }}
    }}

    fun release() {{
        animationScope.launch {{
            androidx.compose.runtime.withFrameNanos {{ }}
            if (value != targetValue) {{
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow {{ valueAnimation.value }}
                    .filter {{ abs(it - valueAnimation.targetValue) < threshold }}
                    .first()
            }}
            launch {{ pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }}
            launch {{ scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }}
            launch {{ scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }}
        }}
    }}

    fun updateValue(value: Float) {{
        val target = value.coerceIn(valueRange)
        animationScope.launch {{
            launch {{ valueAnimation.animateTo(target, valueAnimationSpec) {{ updateVelocity() }} }}
        }}
    }}

    fun animateToValue(value: Float) {{
        animationScope.launch {{
            mutatorMutex.mutate {{
                press()
                val target = value.coerceIn(valueRange)
                launch {{ valueAnimation.animateTo(target, valueAnimationSpec) }}
                if (velocity != 0f) {{
                    launch {{ velocityAnimation.animateTo(0f, velocityAnimationSpec) }}
                }}
                release()
            }}
        }}
    }}

    private fun updateVelocity() {{
        velocityTracker.addPosition(SystemClock.uptimeMillis(), Offset(value, 0f))
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch {{ velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }}
    }}
}}

class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = {{ _, offset -> offset }},
) {{
    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero

    private val shader = if (NavGlassSupport.supported) {{
        RuntimeShader(
            "uniform float2 size;\\n" +
                "layout(color) uniform half4 color;\\n" +
                "uniform float radius;\\n" +
                "uniform float2 position;\\n" +
                "half4 main(float2 coord) {{\\n" +
                "    float dist = distance(coord, position);\\n" +
                "    float intensity = smoothstep(radius, radius * 0.5, dist);\\n" +
                "    return color * intensity;\\n" +
                "}}",
        )
    }} else {{
        null
    }}

    val modifier: Modifier = Modifier.drawWithContent {{
        val progress = pressProgressAnimation.value
        if (progress > 0f) {{
            if (shader != null) {{
                drawRect(Color.White.copy(0.08f * progress), blendMode = BlendMode.Plus)
                shader.apply {{
                    val p = position(size, positionAnimation.value)
                    setFloatUniform("size", size.width, size.height)
                    setColorUniform("color", Color.White.copy(0.15f * progress))
                    setFloatUniform("radius", size.minDimension * 1.5f)
                    setFloatUniform("position", p.x.fastCoerceIn(0f, size.width), p.y.fastCoerceIn(0f, size.height))
                }}
                drawRect(ShaderBrush(shader.asComposeShader()), blendMode = BlendMode.Plus)
            }} else {{
                drawRect(Color.White.copy(0.20f * progress), blendMode = BlendMode.Plus)
            }}
        }}
        drawContent()
    }}

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {{
        inspectLiquidDragGestures(
            onDragStart = {{ down ->
                startPosition = down.position
                animationScope.launch {{
                    launch {{ pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }}
                    launch {{ positionAnimation.snapTo(startPosition) }}
                }}
            }},
            onDragEnd = {{
                animationScope.launch {{
                    launch {{ pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }}
                    launch {{ positionAnimation.animateTo(startPosition, positionAnimationSpec) }}
                }}
            }},
            onDragCancel = {{
                animationScope.launch {{
                    launch {{ pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }}
                    launch {{ positionAnimation.animateTo(startPosition, positionAnimationSpec) }}
                }}
            }},
        ) {{ change, _ ->
            animationScope.launch {{ positionAnimation.snapTo(change.position) }}
        }}
    }}
}}

suspend fun PointerInputScope.inspectLiquidDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {{}},
    onDragEnd: (change: PointerInputChange) -> Unit = {{}},
    onDragCancel: () -> Unit = {{}},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {{
    awaitEachGesture {{
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)
        val down = awaitFirstDown(false)
        onDragStart(down)
        onDrag(initialDown, Offset.Zero)
        val upEvent = drag(pointerId = initialDown.id) {{ onDrag(it, it.positionChange()) }}
        if (upEvent == null) onDragCancel() else onDragEnd(upEvent)
    }}
}}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {{
    val isPointerUp = currentEvent.changes.fastFirstOrNull {{ it.id == pointerId }}?.pressed != true
    if (isPointerUp) return null
    var pointer = pointerId
    while (true) {{
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) return null
        if (change.changedToUpIgnoreConsumed()) return change
        onDrag(change)
        pointer = change.id
    }}
}}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {{
    var pointer = pointerId
    while (true) {{
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull {{ it.id == pointer }} ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {{
            val otherDown = event.changes.fastFirstOrNull {{ it.pressed }}
            if (otherDown == null) return dragEvent else pointer = otherDown.id
        }} else if (dragEvent.previousPosition != dragEvent.position) {{
            return dragEvent
        }}
    }}
}}
"""


LIQUID_GLASS_NAV_BAR_KT = f"""package {NEW_PACKAGE}.navigation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import {NEW_PACKAGE}.core.ui.theme.DarkPlayerFontFamily
import {NEW_PACKAGE}.core.ui.theme.NavyBorder
import {NEW_PACKAGE}.core.ui.theme.NavySurface
import {NEW_PACKAGE}.core.ui.theme.NavySurfaceSecondary
import {NEW_PACKAGE}.core.ui.theme.NavyTextMuted
import {NEW_PACKAGE}.core.ui.theme.NavyTextPrimary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Shared backdrop provided ONLY to the bottom navigation bar.
 * Settings, Player controls, and all other surfaces remain clean Material 3.
 */
val LocalNavGlassBackdrop = staticCompositionLocalOf<Backdrop?> {{ null }}

private fun View.navHaptic(durationMs: Long = 12L, amplitude: Int = 120) {{
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    val done = vibrator?.hasVibrator() == true && runCatching {{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {{
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
        }} else {{
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }}
    }}.isSuccess
    if (!done) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}}

internal val LocalLiquidBottomTabScale = staticCompositionLocalOf {{ {{ 1f }} }}

/**
 * Exact refractive Liquid Glass bottom navigation bar ported for Dark Player.
 * Uses kyant backdrop vibrancy + blur + lens + chromatic aberration + damped spring drag.
 * Strictly scoped to the bottom navigation bar ONLY.
 */
@Composable
fun LiquidGlassNavigationBar(
    modifier: Modifier = Modifier,
    destinations: List<TopLevelDestination>,
    isSelected: (TopLevelDestination) -> Boolean,
    onDestinationClick: (TopLevelDestination) -> Unit,
) {{
    val backdrop = LocalNavGlassBackdrop.current
    val glassSupported = NavGlassSupport.supported && backdrop != null
    val tabsCount = destinations.size.coerceAtLeast(1)
    val selectedIdx = destinations.indexOfFirst(isSelected).coerceAtLeast(0)

    val containerColor = if (glassSupported) {{
        Color(0xFF141418).copy(alpha = 0.44f)
    }} else {{
        NavySurface.copy(alpha = 0.94f)
    }}

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {{
        val density = LocalDensity.current
        val tabWidth = with(density) {{
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }}

        val offsetAnimation = remember {{ Animatable(0f) }}
        val panelOffset by remember(density) {{
            derivedStateOf {{
                val fraction = (offsetAnimation.value / constraints.maxWidth.coerceAtLeast(1))
                    .fastCoerceIn(-1f, 1f)
                with(density) {{
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }}
            }}
        }}

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val view = LocalView.current
        var currentIndex by remember {{ mutableIntStateOf(selectedIdx) }}

        val dampedDragAnimation = remember(animationScope, tabsCount) {{
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIdx.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {{ view.navHaptic(18L, 170) }},
                onDragStopped = {{
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    view.navHaptic(20L, 190)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {{
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }}
                }},
                onDrag = {{ _, dragAmount ->
                    val before = targetValue.fastRoundToInt()
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth.coerceAtLeast(1f) * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    if (targetValue.fastRoundToInt() != before) {{
                        view.navHaptic(16L, 170)
                    }}
                    animationScope.launch {{
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }}
                }},
            )
        }}

        LaunchedEffect(selectedIdx) {{
            currentIndex = selectedIdx
            if (dampedDragAnimation.targetValue != selectedIdx.toFloat()) {{
                dampedDragAnimation.animateToValue(selectedIdx.toFloat())
            }}
        }}
        LaunchedEffect(dampedDragAnimation) {{
            snapshotFlow {{ currentIndex }}
                .drop(1)
                .collectLatest {{ idx ->
                    dampedDragAnimation.animateToValue(idx.toFloat())
                    destinations.getOrNull(idx)?.let(onDestinationClick)
                }}
        }}

        val interactiveHighlight = remember(animationScope) {{
            InteractiveHighlight(
                animationScope = animationScope,
                position = {{ size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f,
                    )
                }},
            )
        }}

        // 1. Frosted Liquid Glass capsule background
        Box(
            Modifier
                .graphicsLayer {{ translationX = panelOffset }}
                .then(
                    if (glassSupported && backdrop != null) {{
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = {{ Capsule() }},
                            effects = {{
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(24f.dp.toPx(), 24f.dp.toPx())
                            }},
                            layerBlock = {{
                                val progress = dampedDragAnimation.pressProgress
                                val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                                scaleX = scale
                                scaleY = scale
                            }},
                            onDrawSurface = {{ drawRect(containerColor) }},
                        )
                    }} else {{
                        Modifier
                            .clip(Capsule())
                            .background(containerColor)
                            .border(1.dp, NavyBorder.copy(alpha = 0.6f), Capsule())
                    }},
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth(),
        )

        // 2. Moving active tab refractive pill (renders behind icons & labels)
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {{
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }}
                .then(
                    if (glassSupported && backdrop != null) {{
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = {{ Capsule() }},
                            effects = {{
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                if (progress < 0.999f) blur(8f.dp.toPx() * (1f - progress))
                                lens(
                                    10f.dp.toPx() * progress,
                                    14f.dp.toPx() * progress,
                                    chromaticAberration = true,
                                )
                            }},
                            highlight = {{
                                Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress)
                            }},
                            shadow = {{
                                Shadow(alpha = dampedDragAnimation.pressProgress)
                            }},
                            innerShadow = {{
                                InnerShadow(
                                    radius = 8f.dp * dampedDragAnimation.pressProgress,
                                    alpha = dampedDragAnimation.pressProgress,
                                )
                            }},
                            layerBlock = {{
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }},
                            onDrawSurface = {{
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    Color.White.copy(alpha = 0.14f),
                                    alpha = 1f - progress,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            }},
                        )
                    }} else {{
                        Modifier
                            .clip(Capsule())
                            .background(NavySurfaceSecondary)
                    }},
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount),
        )

        // 3. Sharp tab icons & labels on top
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {{
                lerp(1f, 1.15f, dampedDragAnimation.pressProgress)
            }},
        ) {{
            Row(
                Modifier
                    .graphicsLayer {{ translationX = panelOffset }}
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDragAnimation.modifier)
                    .height(64f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {{
                destinations.forEachIndexed {{ index, destination ->
                    val selected = index == selectedIdx
                    val scale = LocalLiquidBottomTabScale.current
                    val contentColor = if (selected) NavyTextPrimary else NavyTextMuted
                    Column(
                        modifier = Modifier
                            .clip(Capsule())
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Tab,
                                onClick = {{
                                    view.navHaptic(12L, 130)
                                    currentIndex = index
                                    onDestinationClick(destination)
                                }},
                            )
                            .fillMaxHeight()
                            .weight(1f)
                            .graphicsLayer {{
                                val s = scale()
                                scaleX = s
                                scaleY = s
                            }},
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {{
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                        BasicText(
                            text = stringResource(destination.labelRes),
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(
                                color = contentColor,
                                fontFamily = DarkPlayerFontFamily,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 8.sp,
                                maxFontSize = 10.5.sp,
                                stepSize = 0.5.sp,
                            ),
                        )
                    }}
                }}
            }}
        }}
    }}
}}
"""


VIDEO_SCREENSHOT_MANAGER_KT = f"""package {NEW_PACKAGE}.feature.player.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures the active hardware-accelerated video frame from the PlayerSurface
 * using Android PixelCopy and saves a lossless PNG directly to Pictures/DarkPlayer.
 */
object VideoScreenshotManager {{

    fun captureCurrentFrame(
        context: Context,
        onComplete: (Boolean, String?) -> Unit = {{ _, _ -> }},
    ) {{
        val activity = context.findActivity()
        if (activity == null) {{
            showToast(context, "Unable to access player window")
            onComplete(false, null)
            return
        }}

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {{
            showToast(context, "Screenshot requires Android 8.0+")
            onComplete(false, null)
            return
        }}

        val decorView = activity.window?.decorView
        val surfaceView = decorView?.let {{ findVideoSurfaceView(it) }}
        val width = (surfaceView?.width ?: decorView?.width ?: 0).coerceAtLeast(1)
        val height = (surfaceView?.height ?: decorView?.height ?: 0).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())

        val copyListener = PixelCopy.OnPixelCopyFinishedListener {{ copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {{
                val savedPath = saveBitmapToGallery(context, bitmap)
                bitmap.recycle()
                if (savedPath != null) {{
                    showToast(context, "Screenshot saved to Gallery")
                    onComplete(true, savedPath)
                }} else {{
                    showToast(context, "Failed to save screenshot")
                    onComplete(false, null)
                }}
            }} else {{
                bitmap.recycle()
                showToast(context, "Screenshot capture failed ($copyResult)")
                onComplete(false, null)
            }}
        }}

        try {{
            if (surfaceView != null && surfaceView.holder.surface.isValid) {{
                PixelCopy.request(surfaceView, bitmap, copyListener, handler)
            }} else {{
                PixelCopy.request(activity.window, bitmap, copyListener, handler)
            }}
        }} catch (_: Exception) {{
            bitmap.recycle()
            showToast(context, "Unable to capture frame")
            onComplete(false, null)
        }}
    }}

    private fun findVideoSurfaceView(root: View): SurfaceView? {{
        if (root is SurfaceView && root.width > 0 && root.height > 0) return root
        if (root is ViewGroup) {{
            for (i in 0 until root.childCount) {{
                val found = findVideoSurfaceView(root.getChildAt(i))
                if (found != null) return found
            }}
        }}
        return null
    }}

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): String? {{
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "DarkPlayer_$timestamp.png"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {{
            val resolver = context.contentResolver
            val values = ContentValues().apply {{
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${{Environment.DIRECTORY_PICTURES}}/DarkPlayer")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }}
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            try {{
                resolver.openOutputStream(uri)?.use {{ out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }}
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri.toString()
            }} catch (_: Exception) {{
                resolver.delete(uri, null, null)
                null
            }}
        }} else {{
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "DarkPlayer",
            ).apply {{ mkdirs() }}
            val target = File(dir, filename)
            try {{
                FileOutputStream(target).use {{ out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }}
                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/png"), null)
                target.absolutePath
            }} catch (_: Exception) {{
                null
            }}
        }}
    }}

    private fun showToast(context: Context, message: String) {{
        Handler(Looper.getMainLooper()).post {{
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }}
    }}

    private tailrec fun Context.findActivity(): Activity? = when (this) {{
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }}
}}
"""


HDR_SUPPORT_MANAGER_KT = f"""package {NEW_PACKAGE}.feature.player.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HdrRenderMode(val label: String) {{
    AUTO("HDR"),
    HDR("HDR"),
    SDR("SDR"),
}}

/**
 * Hardware HDR capability detector and live HDR/SDR window color-mode controller.
 * Probes Display.HdrCapabilities and MediaCodecList (HEVC/VP9/AV1 10-bit profiles)
 * and switches ActivityInfo.COLOR_MODE_HDR vs COLOR_MODE_DEFAULT safely.
 */
object HdrSupportManager {{

    @Volatile
    private var initialized = false

    @Volatile
    private var displaySupportsHdr = false

    @Volatile
    private var decoderSupportsHdr = false

    private val _mode = MutableStateFlow(HdrRenderMode.AUTO)
    val mode: StateFlow<HdrRenderMode> = _mode.asStateFlow()

    val isHardwareHdrSupported: Boolean
        get() = displaySupportsHdr && decoderSupportsHdr

    fun init(context: Context) {{
        if (initialized) return
        initialized = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        runCatching {{
            val wm = context.getSystemService(WindowManager::class.java)
            @Suppress("DEPRECATION")
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {{
                context.display ?: wm?.defaultDisplay
            }} else {{
                wm?.defaultDisplay
            }}
            displaySupportsHdr = display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true
            if (displaySupportsHdr) {{
                decoderSupportsHdr = detectHdrDecoders()
            }}
        }}
    }}

    fun toggleHdrSdr(context: Context): HdrRenderMode {{
        init(context)
        val next = when (_mode.value) {{
            HdrRenderMode.SDR -> HdrRenderMode.HDR
            HdrRenderMode.HDR, HdrRenderMode.AUTO -> HdrRenderMode.SDR
        }}
        setMode(context, next)
        return next
    }}

    fun setMode(context: Context, newMode: HdrRenderMode) {{
        init(context)
        _mode.value = newMode
        applyToWindow(context, newMode)
    }}

    fun applyToWindow(context: Context, targetMode: HdrRenderMode = _mode.value) {{
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val activity = context.findActivity() ?: return
        val enableHdr = when (targetMode) {{
            HdrRenderMode.HDR -> true
            HdrRenderMode.SDR -> false
            HdrRenderMode.AUTO -> isHardwareHdrSupported
        }}
        val desiredColorMode = if (enableHdr && isHardwareHdrSupported) {{
            ActivityInfo.COLOR_MODE_HDR
        }} else {{
            ActivityInfo.COLOR_MODE_DEFAULT
        }}
        runCatching {{
            activity.window.colorMode = desiredColorMode
        }}
    }}

    private fun detectHdrDecoders(): Boolean {{
        val codecs = runCatching {{
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        }}.getOrDefault(emptyArray())

        for (codec in codecs) {{
            if (codec.isEncoder) continue
            for (type in codec.supportedTypes) {{
                val profiles = runCatching {{
                    codec.getCapabilitiesForType(type).profileLevels.map {{ it.profile }}.toSet()
                }}.getOrDefault(emptySet())
                when (type.lowercase()) {{
                    "video/hevc" -> if (profiles.any {{ it in HEVC_HDR_PROFILES }}) return true
                    "video/x-vnd.on2.vp9" -> if (profiles.any {{ it in VP9_HDR_PROFILES }}) return true
                    "video/av01" -> if (profiles.any {{ it in AV1_HDR_PROFILES }}) return true
                }}
            }}
        }}
        return false
    }}

    private val HEVC_HDR_PROFILES = setOf(
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10,
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus,
    )

    private val VP9_HDR_PROFILES = setOf(
        MediaCodecInfo.CodecProfileLevel.VP9Profile2,
        MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR,
        MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus,
    )

    private val AV1_HDR_PROFILES = buildSet {{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {{
            add(MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10)
            add(MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10)
            add(MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus)
        }} else {{
            add(2)
        }}
    }}

    private tailrec fun Context.findActivity(): Activity? = when (this) {{
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }}
}}
"""


def build_updater_kt(github_repo: str) -> str:
    return f"""package {NEW_PACKAGE}.core.ui.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import {NEW_PACKAGE}.core.ui.theme.NavyBackground
import {NEW_PACKAGE}.core.ui.theme.NavyBorder
import {NEW_PACKAGE}.core.ui.theme.NavySurface
import {NEW_PACKAGE}.core.ui.theme.NavySurfaceSecondary
import {NEW_PACKAGE}.core.ui.theme.NavyTextMuted
import {NEW_PACKAGE}.core.ui.theme.NavyTextPrimary
import {NEW_PACKAGE}.core.ui.theme.NavyTextSecondary
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val RELEASES_API_URL = "https://api.github.com/repos/{github_repo}/releases/latest"

@Immutable
data class DarkPlayerUpdateInfo(
    val version: String,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

@Stable
class DarkPlayerUpdateState(
    private val scope: CoroutineScope,
    private val currentVersionName: String,
) {{
    var info by mutableStateOf<DarkPlayerUpdateInfo?>(null)
        private set
    var visible by mutableStateOf(false)
        private set
    var checking by mutableStateOf(false)
        private set
    var downloading by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var downloaded by mutableStateOf<File?>(null)
        private set
    var statusMessage by mutableStateOf<String?>(null)

    private var downloadJob: Job? = null

    fun check(manual: Boolean = false) {{
        if (checking) return
        checking = true
        statusMessage = null
        scope.launch {{
            val latest = withContext(Dispatchers.IO) {{ runCatching {{ fetchLatest() }}.getOrNull() }}
            checking = false
            if (latest == null) {{
                if (manual) statusMessage = "Could not reach update server. Try again later."
                return@launch
            }}
            if (isNewerVersion(latest.version, currentVersionName)) {{
                info = latest
                error = null
                visible = true
            }} else if (manual) {{
                statusMessage = "Dark Player $currentVersionName is up to date."
            }}
        }}
    }}

    fun download(context: Context) {{
        val update = info ?: return
        if (downloading) return
        downloading = true
        error = null
        progress = 0f
        downloadJob = scope.launch {{
            val file = withContext(Dispatchers.IO) {{
                runCatching {{ downloadApk(context, update) {{ p -> progress = p }} }}.getOrNull()
            }}
            downloading = false
            downloadJob = null
            if (file != null) {{
                downloaded = file
                install(context)
            }} else {{
                error = "Download failed. Check your connection and try again."
            }}
        }}
    }}

    fun cancelDownload() {{
        downloadJob?.cancel()
        downloadJob = null
        downloading = false
        progress = 0f
    }}

    fun install(context: Context) {{
        val file = downloaded ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {{
            error = "Allow installs from Dark Player in Settings, then tap Install again."
            runCatching {{
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${{context.packageName}}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }}
            return
        }}
        error = null
        runCatching {{
            val uri = FileProvider.getUriForFile(
                context,
                "${{context.packageName}}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {{
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }}
            context.startActivity(intent)
        }}.onFailure {{
            error = "Could not launch the Android package installer."
        }}
    }}

    fun dismiss() {{
        if (!downloading) visible = false
    }}

    private fun fetchLatest(): DarkPlayerUpdateInfo? {{
        val conn = (URL(RELEASES_API_URL).openConnection() as HttpURLConnection).apply {{
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "DarkPlayer/$currentVersionName")
        }}
        return try {{
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use {{ it.readText() }}
            val json = JSONObject(body)
            val tag = json.optString("tag_name").ifBlank {{ return null }}
            val version = tag.removePrefix("v").removePrefix("V").trim()
            if (version.isBlank()) return null
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {{
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                val candidate = asset.optString("browser_download_url")
                if (candidate.isBlank()) continue
                if (apkUrl == null || name.equals("DarkPlayer.apk", ignoreCase = true)) {{
                    apkUrl = candidate
                    size = asset.optLong("size")
                }}
                if (name.equals("DarkPlayer.apk", ignoreCase = true)) break
            }}
            val resolvedUrl = apkUrl ?: return null
            DarkPlayerUpdateInfo(
                version = version,
                tag = tag,
                notes = json.optString("body").orEmpty(),
                apkUrl = resolvedUrl,
                sizeBytes = size,
            )
        }} finally {{
            conn.disconnect()
        }}
    }}

    private suspend fun downloadApk(
        context: Context,
        update: DarkPlayerUpdateInfo,
        onProgress: (Float) -> Unit,
    ): File {{
        val dir = File(context.cacheDir, "updates").apply {{ mkdirs() }}
        val target = File(dir, "DarkPlayer.apk")
        if (target.exists()) target.delete()

        val conn = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {{
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "DarkPlayer/$currentVersionName")
        }}
        try {{
            if (conn.responseCode !in 200..299) error("HTTP ${{conn.responseCode}}")
            val total = conn.contentLengthLong.takeIf {{ it > 0 }} ?: update.sizeBytes
            conn.inputStream.use {{ input ->
                target.outputStream().use {{ output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var lastTick = 0L
                    while (input.read(buffer).also {{ read = it }} > 0) {{
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {{
                            val p = (done.toFloat() / total).coerceIn(0f, 1f)
                            val now = System.currentTimeMillis()
                            if (now - lastTick > 80 || p >= 1f) {{
                                lastTick = now
                                withContext(Dispatchers.Main) {{ onProgress(p) }}
                            }}
                        }}
                    }}
                }}
            }}
        }} finally {{
            conn.disconnect()
        }}
        if (target.length() <= 0L) error("Empty APK download")
        return target
    }}
}}

fun isNewerVersion(remote: String, current: String): Boolean {{
    fun parts(v: String) = v.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .split('.')
        .map {{ it.toIntOrNull() ?: 0 }}
    val r = parts(remote)
    val c = parts(current)
    for (i in 0 until maxOf(r.size, c.size)) {{
        val a = r.getOrElse(i) {{ 0 }}
        val b = c.getOrElse(i) {{ 0 }}
        if (a != b) return a > b
    }}
    return false
}}

private fun formatApkSize(bytes: Long): String = when {{
    bytes <= 0L -> ""
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> "${{bytes / 1024L}} KB"
    else -> "$bytes B"
}}

@Composable
fun DarkPlayerUpdateDialog(state: DarkPlayerUpdateState) {{
    val context = LocalContext.current
    val update = state.info

    AnimatedVisibility(
        visible = state.visible && update != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {{
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember {{ MutableInteractionSource() }},
                    indication = null,
                ) {{ state.dismiss() }},
            contentAlignment = Alignment.Center,
        ) {{
            AnimatedVisibility(
                visible = state.visible,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                    initialScale = 0.88f,
                ) + fadeIn(tween(220)),
                exit = scaleOut(tween(140), targetScale = 0.94f) + fadeOut(tween(140)),
            ) {{
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 410.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = remember {{ MutableInteractionSource() }},
                            indication = null,
                        ) {{ }}
                        .padding(22.dp),
                ) {{
                    Row(verticalAlignment = Alignment.CenterVertically) {{
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NavySurfaceSecondary),
                            contentAlignment = Alignment.Center,
                        ) {{
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = NavyTextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }}
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {{
                            Text(
                                text = "Update Available",
                                color = NavyTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Dark Player ${{update?.version.orEmpty()}}" +
                                    formatApkSize(update?.sizeBytes ?: 0L).let {{
                                        if (it.isBlank()) "" else " · $it"
                                    }},
                                color = NavyTextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }}
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NavySurfaceSecondary.copy(alpha = 0.6f))
                                .clickable {{ state.dismiss() }},
                            contentAlignment = Alignment.Center,
                        ) {{
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = NavyTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }}
                    }}

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "What's New",
                        color = NavyTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = update?.notes?.trim()?.ifBlank {{
                            "Performance improvements, Navy Dark UI refinements, and playback stability fixes."
                        }} ?: "",
                        color = NavyTextSecondary,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        maxLines = 9,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 172.dp)
                            .verticalScroll(rememberScrollState()),
                    )

                    state.error?.let {{ msg ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }}

                    if (state.downloading) {{
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = {{ state.progress }},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = NavyTextPrimary,
                            trackColor = NavySurfaceSecondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${{(state.progress * 100).toInt()}}% · Downloading DarkPlayer.apk",
                            color = NavyTextMuted,
                            fontSize = 11.5.sp,
                        )
                    }}

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {{
                        TextButton(
                            onClick = {{
                                if (state.downloading) state.cancelDownload() else state.dismiss()
                            }},
                        ) {{
                            Text(
                                text = "Later",
                                color = NavyTextSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }}
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {{
                                if (state.downloaded != null) state.install(context)
                                else state.download(context)
                            }},
                            enabled = !state.downloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyTextPrimary,
                                contentColor = NavyBackground,
                            ),
                        ) {{
                            if (state.checking) {{
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = NavyBackground,
                                )
                            }} else {{
                                Text(
                                    text = if (state.downloaded != null) "Install" else "Update Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }}
                        }}
                    }}
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GitHub Releases · ${{update?.tag.orEmpty()}}",
                        color = NavyTextMuted,
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }}
            }}
        }}
    }}
}}
"""


PLAYER_CONTENT_FRAME_KT = f"""package {NEW_PACKAGE}.feature.player

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import {NEW_PACKAGE}.feature.player.extensions.toContentScale
import {NEW_PACKAGE}.feature.player.state.CuesState
import {NEW_PACKAGE}.feature.player.state.PictureInPictureState
import {NEW_PACKAGE}.feature.player.state.SubtitlePositionState
import {NEW_PACKAGE}.feature.player.state.VideoZoomAndContentScaleState
import {NEW_PACKAGE}.feature.player.ui.ShutterView
import {NEW_PACKAGE}.feature.player.ui.SubtitleConfiguration
import {NEW_PACKAGE}.feature.player.ui.SubtitleView

/**
 * Fixed PlayerContentFrame:
 * - Eliminates the double-density division bug on presentationState.videoSizeDp
 *   that caused Crop/Fit aspect ratio glitches.
 * - Clips zoomed/cropped video cleanly inside viewport bounds via clipToBounds().
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerContentFrame(
    modifier: Modifier = Modifier,
    player: Player,
    pictureInPictureState: PictureInPictureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    subtitleConfiguration: SubtitleConfiguration,
    cuesState: CuesState,
    subtitlePositionState: SubtitlePositionState,
    videoVisible: Boolean = true,
    displayAspectRatio: Float? = null,
) {{
    val presentationState = rememberPresentationState(player = player)
    val resolvedSourceSizeDp: Size? = presentationState.videoSizeDp?.let {{ size ->
        if (size.width <= 0f || size.height <= 0f) {{
            null
        }} else {{
            val adjustedWidth = displayAspectRatio
                ?.takeIf {{ it > 0f }}
                ?.let {{ ratio -> size.height * ratio }}
                ?: size.width
            Size(width = adjustedWidth, height = size.height)
        }}
    }}

    val videoModifier = Modifier
        .resizeWithContentScale(
            contentScale = videoZoomAndContentScaleState.videoContentScale.toContentScale(),
            sourceSizeDp = resolvedSourceSizeDp,
        )
        .graphicsLayer {{
            scaleX = videoZoomAndContentScaleState.zoom
            scaleY = videoZoomAndContentScaleState.zoom
            translationX = videoZoomAndContentScaleState.offset.x
            translationY = videoZoomAndContentScaleState.offset.y
        }}

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {{
        if (videoVisible) {{
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = videoModifier.onGloballyPositioned {{ coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    val rect = Rect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt(),
                    )
                    pictureInPictureState.setVideoViewRect(rect)
                }},
            )
        }}

        AnimatedVisibility(
            visible = subtitleConfiguration.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {{
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f)),
            )
        }}

        SubtitleView(
            bitmapModifier = videoModifier,
            cuesState = cuesState,
            positionState = subtitlePositionState,
            isInPictureInPictureMode = pictureInPictureState.isInPictureInPictureMode,
            configuration = subtitleConfiguration,
        )

        if (videoVisible && presentationState.coverSurface) {{
            ShutterView()
        }}
    }}
}}
"""


VIDEO_CONTENT_SCALE_KT = f"""package {NEW_PACKAGE}.feature.player.extensions

import androidx.compose.ui.layout.ContentScale
import {NEW_PACKAGE}.core.model.VideoContentScale
import {NEW_PACKAGE}.core.ui.R

fun VideoContentScale.nameRes(): Int = when (this) {{
    VideoContentScale.BEST_FIT -> R.string.best_fit
    VideoContentScale.STRETCH -> R.string.stretch
    VideoContentScale.CROP -> R.string.crop
    VideoContentScale.HUNDRED_PERCENT -> R.string.hundred_percent
}}

fun VideoContentScale.drawableRes(): Int = when (this) {{
    VideoContentScale.BEST_FIT -> R.drawable.ic_fit_screen
    VideoContentScale.STRETCH -> R.drawable.ic_aspect_ratio
    VideoContentScale.CROP -> R.drawable.ic_crop_landscape
    VideoContentScale.HUNDRED_PERCENT -> R.drawable.ic_width_wide
}}

/**
 * Clean ContentScale mapping fixing the 100% / Crop / Fit scaling glitch.
 */
fun VideoContentScale.toContentScale(): ContentScale = when (this) {{
    VideoContentScale.BEST_FIT -> ContentScale.Fit
    VideoContentScale.STRETCH -> ContentScale.FillBounds
    VideoContentScale.CROP -> ContentScale.Crop
    VideoContentScale.HUNDRED_PERCENT -> ContentScale.None
}}
"""


PLAYBACK_PARAMETERS_STATE_KT = f"""package {NEW_PACKAGE}.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import {NEW_PACKAGE}.feature.player.service.getSkipSilenceEnabled
import {NEW_PACKAGE}.feature.player.service.setSkipSilenceEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun rememberPlaybackParametersState(player: Player): PlaybackParametersState {{
    val scope = rememberCoroutineScope()
    val playbackParametersState = remember(player) {{
        PlaybackParametersState(
            player = player,
            scope = scope,
        )
    }}
    LaunchedEffect(key1 = player) {{ playbackParametersState.observe() }}
    return playbackParametersState
}}

@UnstableApi
class PlaybackParametersState(private val player: Player, private val scope: CoroutineScope) {{
    var speed: Float by mutableFloatStateOf(value = 1f)
        private set

    var skipSilenceEnabled: Boolean by mutableStateOf(value = false)
        private set

    /**
     * Applies playback speed while strictly locking audio pitch to 1.0f so 2.0x
     * and high-speed playback never introduces chipmunk/pitch distortion.
     */
    fun setPlaybackSpeed(speed: Float) {{
        val safeSpeed = speed.coerceIn(0.25f, 4.0f)
        player.playbackParameters = PlaybackParameters(safeSpeed, 1.0f)
        this.speed = safeSpeed
    }}

    fun toggle2xSpeed() {{
        val target = if (speed >= 1.95f) 1.0f else 2.0f
        setPlaybackSpeed(target)
    }}

    fun setIsSkipSilenceEnabled(enabled: Boolean) {{
        scope.launch {{
            when (player) {{
                is MediaController -> player.setSkipSilenceEnabled(enabled = enabled)
                is ExoPlayer -> player.skipSilenceEnabled = enabled
                else -> return@launch
            }}
            updateSkipSilenceEnabled()
        }}
    }}

    suspend fun observe() {{
        updateSpeed()
        updateSkipSilenceEnabled()

        player.listen {{ events ->
            if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)) {{
                updateSpeed()
            }}
        }}
    }}

    private fun updateSpeed() {{
        val params = player.playbackParameters
        speed = params.speed
        if (params.pitch != 1.0f) {{
            player.playbackParameters = PlaybackParameters(params.speed, 1.0f)
        }}
    }}

    private fun updateSkipSilenceEnabled() {{
        scope.launch {{
            skipSilenceEnabled = when (player) {{
                is MediaController -> player.getSkipSilenceEnabled()
                is ExoPlayer -> player.skipSilenceEnabled
                else -> return@launch
            }}
        }}
    }}
}}
"""


PLAYER_FEEDBACK_KT = f"""package {NEW_PACKAGE}.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import {NEW_PACKAGE}.core.ui.R as coreUiR
import {NEW_PACKAGE}.core.ui.extensions.copy
import {NEW_PACKAGE}.core.ui.theme.NavyBorder
import {NEW_PACKAGE}.core.ui.theme.NavySurface
import {NEW_PACKAGE}.core.ui.theme.NavyTextPrimary
import {NEW_PACKAGE}.feature.player.extensions.formatted
import {NEW_PACKAGE}.feature.player.state.TapGestureState
import {NEW_PACKAGE}.feature.player.state.VerticalGesture
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clean floating Navy Dark status pill for Crop / Fit / Zoom / Seek readouts.
 * Prevents text overlap with center playback controls.
 */
@Composable
internal fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
    ),
) {{
    Surface(
        modifier = modifier
            .border(1.dp, NavyBorder.copy(alpha = 0.7f), CircleShape),
        shape = CircleShape,
        color = NavySurface.copy(alpha = 0.90f),
        shadowElevation = 6.dp,
    ) {{
        Text(
            text = info,
            style = textStyle,
            color = NavyTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }}
}}

@Composable
internal fun BoxScope.DpadSeekIndicator(visible: Boolean, offsetMs: Long, positionMs: Long) {{
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {{
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NavySurface.copy(alpha = 0.88f),
        ) {{
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {{
                Icon(
                    painter = painterResource(coreUiR.drawable.ic_fast),
                    contentDescription = null,
                    tint = NavyTextPrimary,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(degrees = if (offsetMs < 0) 180f else 0f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {{
                    Text(
                        text = "${{if (offsetMs >= 0) "+" else "-"}}" +
                            "${{abs(offsetMs).milliseconds.inWholeSeconds}}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NavyTextPrimary,
                    )
                    Text(
                        text = positionMs.milliseconds.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NavyTextPrimary.copy(alpha = 0.8f),
                    )
                }}
            }}
        }}
    }}
}}

@Composable
internal fun BoxScope.PlayerGestureIndicators(tapGestureState: TapGestureState) {{
    DoubleTapIndicator(tapGestureState = tapGestureState)

    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 24.dp)
            .align(Alignment.TopCenter),
        visible = tapGestureState.isLongPressGestureInAction,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {{
        Surface(
            shape = CircleShape,
            color = NavySurface.copy(alpha = 0.92f),
            modifier = Modifier.border(1.dp, NavyBorder.copy(alpha = 0.65f), CircleShape),
        ) {{
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {{
                Icon(
                    painter = painterResource(coreUiR.drawable.ic_fast),
                    contentDescription = null,
                    tint = NavyTextPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "${{tapGestureState.longPressSpeed}}X Speed Playing",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = NavyTextPrimary,
                )
            }}
        }}
    }}
}}

@Composable
internal fun BoxScope.PlayerVerticalGestureIndicators(
    activeGesture: VerticalGesture?,
    volumePercentage: Int,
    maxVolumePercentage: Int,
    brightnessPercentage: Int,
    useMaterialYouControls: Boolean,
) {{
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
            .padding(24.dp),
    ) {{
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterStart),
            visible = activeGesture == VerticalGesture.VOLUME,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {{
            VerticalProgressView(
                useMaterialYouControls = useMaterialYouControls,
                value = volumePercentage,
                maxValue = maxVolumePercentage,
                icon = painterResource(coreUiR.drawable.ic_volume),
            )
        }}

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = activeGesture == VerticalGesture.BRIGHTNESS,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {{
            VerticalProgressView(
                useMaterialYouControls = useMaterialYouControls,
                value = brightnessPercentage,
                icon = painterResource(coreUiR.drawable.ic_brightness),
            )
        }}
    }}
}}
"""


CONTROLS_TOP_VIEW_KT = f"""package {NEW_PACKAGE}.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import {NEW_PACKAGE}.core.ui.R
import {NEW_PACKAGE}.core.ui.extensions.copy
import {NEW_PACKAGE}.core.ui.theme.DarkPlayerTheme
import {NEW_PACKAGE}.core.ui.theme.NavyTextPrimary
import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillFill
import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillIcon
import {NEW_PACKAGE}.feature.player.buttons.PlayerButton
import {NEW_PACKAGE}.feature.player.model.DecoderMode
import {NEW_PACKAGE}.feature.player.model.labelRes
import {NEW_PACKAGE}.feature.player.utils.HdrSupportManager

@OptIn(UnstableApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    videoDecoderMode: DecoderMode?,
    onBackClick: () -> Unit = {{}},
    onDecoderClick: () -> Unit = {{}},
    onAudioClick: () -> Unit = {{}},
    onSubtitleClick: () -> Unit = {{}},
    onPlaylistClick: () -> Unit = {{}},
) {{
    val context = LocalContext.current
    val hdrMode by HdrSupportManager.mode.collectAsState()
    val firstControlFocusRequester = remember {{ FocusRequester() }}
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val decoderDescription = stringResource(R.string.select_decoders)
    val extraTopPadding = if (systemBarsPadding.calculateTopPadding() == 0.dp) 14.dp else 4.dp

    Row(
        modifier = modifier
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .padding(top = extraTopPadding)
            .focusProperties {{ onEnter = {{ firstControlFocusRequester.requestFocus() }} }}
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {{
        PlayerButton(
            onClick = onBackClick,
            containerColor = PlayerControlPillFill,
            contentColor = PlayerControlPillIcon,
        ) {{
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.navigate_up),
            )
        }}

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                lineBreak = LineBreak.Heading,
                fontWeight = FontWeight.SemiBold,
            ),
            color = NavyTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {{
            PlayerButton(
                modifier = Modifier
                    .semantics {{ contentDescription = decoderDescription }}
                    .focusRequester(focusRequester = firstControlFocusRequester),
                onClick = onDecoderClick,
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {{
                Text(
                    text = stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }}

            PlayerButton(
                onClick = {{ HdrSupportManager.toggleHdrSdr(context) }},
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {{
                Text(
                    text = hdrMode.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }}

            PlayerButton(
                onClick = onPlaylistClick,
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
            ) {{
                Icon(
                    painter = painterResource(R.drawable.ic_playlist),
                    contentDescription = null,
                )
            }}

            PlayerButton(
                onClick = onAudioClick,
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
            ) {{
                Icon(
                    painter = painterResource(R.drawable.ic_audio_track),
                    contentDescription = null,
                )
            }}

            PlayerButton(
                onClick = onSubtitleClick,
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
            ) {{
                Icon(
                    painter = painterResource(R.drawable.ic_subtitle_track),
                    contentDescription = null,
                )
            }}
        }}
    }}
}}

@Preview
@Composable
private fun ControlsTopViewPreview() {{
    DarkPlayerTheme(darkTheme = true) {{
        Surface {{
            ControlsTopView(
                title = "Dark Player Cinema Preview 1080p HDR",
                videoDecoderMode = DecoderMode.HARDWARE,
            )
        }}
    }}
}}
"""


CONTROLS_BOTTOM_VIEW_KT = f"""package {NEW_PACKAGE}.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import {NEW_PACKAGE}.core.common.extensions.isTelevision
import {NEW_PACKAGE}.core.model.VideoContentScale
import {NEW_PACKAGE}.core.ui.R
import {NEW_PACKAGE}.core.ui.extensions.copy
import {NEW_PACKAGE}.core.ui.theme.DarkPlayerTheme
import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillFill
import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillIcon
import {NEW_PACKAGE}.core.ui.theme.PlayerTimePillFill
import {NEW_PACKAGE}.feature.player.buttons.LoopButton
import {NEW_PACKAGE}.feature.player.buttons.PlaybackSpeedButton
import {NEW_PACKAGE}.feature.player.buttons.PlayerButton
import {NEW_PACKAGE}.feature.player.buttons.RotateButton
import {NEW_PACKAGE}.feature.player.buttons.ShuffleButton
import {NEW_PACKAGE}.feature.player.extensions.drawableRes
import {NEW_PACKAGE}.feature.player.state.ChaptersState
import {NEW_PACKAGE}.feature.player.state.rememberChaptersState
import {NEW_PACKAGE}.feature.player.ui.preview.rememberPreviewPlayer
import {NEW_PACKAGE}.feature.player.ui.titleOrDefault
import {NEW_PACKAGE}.feature.player.utils.VideoScreenshotManager

private const val MILLISECONDS_PER_SECOND = 1_000L

@OptIn(UnstableApi::class)
@Composable
fun ControlsBottomView(
    modifier: Modifier = Modifier,
    player: Player?,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    controlsAlignment: Alignment.Horizontal,
    videoContentScale: VideoContentScale,
    isPipSupported: Boolean,
    showRemainingTime: Boolean,
    onToggleTimeDisplay: () -> Unit,
    onChaptersClick: () -> Unit,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    onScreenshotClick: (() -> Unit)? = null,
) {{
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val context = LocalContext.current
    val isTv = remember {{ context.isTelevision }}
    val timeButtonFocusRequester = remember {{ FocusRequester() }}

    BoxWithConstraints(modifier = modifier) {{
        val useCompactControls = maxWidth < 600.dp && !isTv
        Column(
            modifier = Modifier
                .padding(systemBarsPadding.copy(top = 0.dp))
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .padding(
                    bottom = 14.dp.takeIf {{ systemBarsPadding.calculateBottomPadding() == 0.dp }} ?: 4.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {{
            // Row 1: Floating time pill on left, Speed (1.0x) + Rotate pills on right
            Row(
                modifier = Modifier
                    .focusProperties {{ onEnter = {{ timeButtonFocusRequester.requestFocus() }} }}
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {{
                Row(
                    modifier = Modifier.weight(weight = 1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {{
                    PlayerButton(
                        modifier = Modifier.focusRequester(focusRequester = timeButtonFocusRequester),
                        onClick = onToggleTimeDisplay,
                        containerColor = PlayerTimePillFill,
                        contentColor = PlayerControlPillIcon,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                    ) {{
                        val timeTextPositionMs by remember(progressState) {{
                            derivedStateOf {{
                                val wholeSeconds = progressState.currentPositionMs / MILLISECONDS_PER_SECOND
                                wholeSeconds * MILLISECONDS_PER_SECOND
                            }}
                        }}
                        Text(
                            text = buildString {{
                                val positionText = when (showRemainingTime) {{
                                    true -> if (progressState.durationMs != C.TIME_UNSET) {{
                                        getStringForTime(timeTextPositionMs - progressState.durationMs)
                                    }} else {{
                                        getStringForTime(C.TIME_UNSET)
                                    }}
                                    false -> getStringForTime(timeTextPositionMs)
                                }}
                                append(positionText)
                                append(" / ")
                                append(getStringForTime(progressState.durationMs))
                            }},
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }}

                    if (chaptersState.chapters.isNotEmpty()) {{
                        PlayerButton(
                            modifier = Modifier.weight(weight = 1f, fill = false),
                            onClick = onChaptersClick,
                            containerColor = PlayerTimePillFill,
                            contentColor = PlayerControlPillIcon,
                            contentPadding = PaddingValues(vertical = 5.dp, horizontal = 10.dp).copy(end = 4.dp),
                        ) {{
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {{
                                Text(
                                    text = chaptersState.chapters
                                        .getOrNull(chaptersState.currentChapterIndex)
                                        ?.titleOrDefault(chaptersState.currentChapterIndex)
                                        ?: stringResource(R.string.chapters),
                                    modifier = Modifier.weight(weight = 1f, fill = false),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }}
                        }}
                    }}
                }}

                if (!useCompactControls) {{
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {{
                        PlaybackSpeedButton(
                            player = player,
                            onClick = onPlaybackSpeedClick,
                        )
                        if (!isTv) {{
                            RotateButton()
                        }}
                    }}
                }}
            }}

            // Row 2: Sleek Seekbar
            PlayerSeekbar(
                position = progressState.currentPositionMs.toFloat(),
                duration = progressState.durationMs.toFloat(),
                chapters = chaptersState.chapters,
                onSeek = {{ onSeek(it.toLong()) }},
                onSeekFinished = {{ onSeekEnd() }},
            )

            // Row 3: Lock, PIP, Repeat, Audio/Headset, Shuffle, Resize (Crop/Fit), Screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(state = rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    10.dp,
                    alignment = controlsAlignment,
                ),
            ) {{
                PlayerButton(
                    onClick = onLockControlsClick,
                    containerColor = PlayerControlPillFill,
                    contentColor = PlayerControlPillIcon,
                ) {{
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_open),
                        contentDescription = null,
                    )
                }}

                if (isPipSupported) {{
                    PlayerButton(
                        onClick = onPictureInPictureClick,
                        containerColor = PlayerControlPillFill,
                        contentColor = PlayerControlPillIcon,
                    ) {{
                        Icon(
                            painter = painterResource(R.drawable.ic_pip),
                            contentDescription = null,
                        )
                    }}
                }}

                LoopButton(player = player)

                PlayerButton(
                    onClick = onPlayInBackgroundClick,
                    containerColor = PlayerControlPillFill,
                    contentColor = PlayerControlPillIcon,
                ) {{
                    Icon(
                        painter = painterResource(R.drawable.ic_headset),
                        contentDescription = null,
                    )
                }}

                ShuffleButton(player = player)

                PlayerButton(
                    onClick = onVideoContentScaleClick,
                    onLongClick = onVideoContentScaleLongClick,
                    containerColor = PlayerControlPillFill,
                    contentColor = PlayerControlPillIcon,
                ) {{
                    Icon(
                        painter = painterResource(videoContentScale.drawableRes()),
                        contentDescription = null,
                    )
                }}

                PlayerButton(
                    onClick = {{
                        onScreenshotClick?.invoke()
                            ?: VideoScreenshotManager.captureCurrentFrame(context)
                    }},
                    containerColor = PlayerControlPillFill,
                    contentColor = PlayerControlPillIcon,
                ) {{
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Screenshot",
                        modifier = Modifier.size(22.dp),
                    )
                }}

                if (useCompactControls) {{
                    PlaybackSpeedButton(
                        player = player,
                        onClick = onPlaybackSpeedClick,
                    )
                    RotateButton()
                }}
            }}
        }}
    }}
}}

@OptIn(UnstableApi::class)
@Preview
@Composable
private fun ControlsBottomViewPreview() {{
    val player = rememberPreviewPlayer()
    val progressState = rememberProgressStateWithTickInterval(player = player)
    DarkPlayerTheme(darkTheme = true) {{
        Surface {{
            ControlsBottomView(
                player = player,
                progressState = progressState,
                chaptersState = rememberChaptersState(
                    player = player,
                    progressState = progressState,
                ),
                controlsAlignment = Alignment.Start,
                videoContentScale = VideoContentScale.BEST_FIT,
                isPipSupported = true,
                showRemainingTime = false,
                onToggleTimeDisplay = {{}},
                onChaptersClick = {{}},
                onVideoContentScaleClick = {{}},
                onVideoContentScaleLongClick = {{}},
                onLockControlsClick = {{}},
                onPictureInPictureClick = {{}},
                onPlaybackSpeedClick = {{}},
                onPlayInBackgroundClick = {{}},
                onSeek = {{}},
                onSeekEnd = {{}},
            )
        }}
    }}
}}
"""


PLAYER_BUTTON_KT = f"""package {NEW_PACKAGE}.feature.player.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import {NEW_PACKAGE}.core.common.extensions.isTelevision
import {NEW_PACKAGE}.core.ui.components.tvFocusRing
import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillFill
import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillIcon
import {NEW_PACKAGE}.feature.player.LocalUseMaterialYouControls
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

val PlayerButtonBlackAlpha = PlayerControlPillFill

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = PlayerControlPillFill,
    contentColor: Color = PlayerControlPillIcon,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    content: @Composable BoxScope.() -> Unit,
) {{
    val interactionSource = remember {{ MutableInteractionSource() }}
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val isTv = remember {{ context.isTelevision }}

    LaunchedEffect(key1 = interactionSource) {{
        var isLongPressClicked = false
        interactionSource.interactions.collectLatest {{ interaction ->
            when (interaction) {{
                is PressInteraction.Press -> {{
                    isLongPressClicked = false
                    delay(viewConfiguration.longPressTimeoutMillis.milliseconds)
                    onLongClick?.let {{
                        isLongPressClicked = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        it.invoke()
                    }}
                }}

                is PressInteraction.Release -> {{
                    if (!isLongPressClicked) {{
                        onClick()
                    }}
                }}
            }}
        }}
    }}

    val colors = if (LocalUseMaterialYouControls.current) {{
        IconButtonDefaults.filledTonalIconButtonColors()
    }} else {{
        IconButtonDefaults.iconButtonColors(
            contentColor = contentColor,
            containerColor = containerColor,
        )
    }}

    val rippleConfiguration = if (LocalUseMaterialYouControls.current) {{
        LocalRippleConfiguration.current
    }} else {{
        RippleConfiguration(
            color = Color.White,
            rippleAlpha = RippleAlpha(
                pressedAlpha = 0.4f,
                focusedAlpha = 0.4f,
                draggedAlpha = 0.4f,
                hoveredAlpha = 0.4f,
            ),
        )
    }}

    CompositionLocalProvider(
        LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor,
        LocalRippleConfiguration provides rippleConfiguration,
    ) {{
        Box(
            modifier = modifier
                .clip(shape = CircleShape)
                .background(
                    color = if (enabled) colors.containerColor else colors.disabledContainerColor,
                )
                .clickable(
                    interactionSource = interactionSource,
                    enabled = enabled,
                    onClick = {{}},
                )
                .padding(paddingValues = contentPadding)
                .tvFocusRing(isTv = isTv),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }}
}}
"""


PLAYBACK_SPEED_BUTTON_KT = f"""package {NEW_PACKAGE}.feature.player.buttons

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import {NEW_PACKAGE}.core.common.extensions.round
import {NEW_PACKAGE}.core.ui.R
import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillFill
import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillIcon

/**
 * Playback speed pill (1.0x). Tap opens speed selector; long-press toggles
 * pitch-locked 2.0x speed immediately without audio distortion.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlaybackSpeedButton(player: Player?, onClick: () -> Unit, modifier: Modifier = Modifier) {{
    val state = rememberPlaybackSpeedState(player = player)
    val description = stringResource(R.string.select_playback_speed)
    PlayerButton(
        modifier = modifier.semantics {{ contentDescription = description }},
        enabled = state.isEnabled,
        onClick = onClick,
        onLongClick = {{
            player?.let {{ p ->
                val nextSpeed = if (p.playbackParameters.speed >= 1.95f) 1.0f else 2.0f
                p.playbackParameters = PlaybackParameters(nextSpeed, 1.0f)
            }}
        }},
        containerColor = PlayerControlPillFill,
        contentColor = PlayerControlPillIcon,
        contentPadding = PaddingValues(6.dp),
    ) {{
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center,
        ) {{
            BasicText(
                text = state.playbackSpeed.round(decimalPlaces = 1).toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = LocalContentColor.current,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = MaterialTheme.typography.labelLarge.fontSize,
                    minFontSize = 7.sp,
                    stepSize = 0.5.sp,
                ),
            )
        }}
    }}
}}
"""


PLAYER_CONTROLS_KT = f"""package {NEW_PACKAGE}.feature.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import {NEW_PACKAGE}.core.model.ControlButtonsPosition
import {NEW_PACKAGE}.core.model.PlayerPreferences
import {NEW_PACKAGE}.feature.player.extensions.nameRes
import {NEW_PACKAGE}.feature.player.model.DecoderMode
import {NEW_PACKAGE}.feature.player.state.ChaptersState
import {NEW_PACKAGE}.feature.player.state.ControlsVisibilityState
import {NEW_PACKAGE}.feature.player.state.SeekGestureState
import {NEW_PACKAGE}.feature.player.state.VideoZoomAndContentScaleState
import {NEW_PACKAGE}.feature.player.state.seekAmountFormatted
import {NEW_PACKAGE}.feature.player.state.seekToPositionFormated
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import {NEW_PACKAGE}.feature.player.ui.DarkPlayerEqualizerDialog
import {NEW_PACKAGE}.feature.player.ui.DarkPlayerProEffects
import {NEW_PACKAGE}.feature.player.ui.DarkPlayerSleepTimerDialog
import {NEW_PACKAGE}.feature.player.ui.InfoView
import {NEW_PACKAGE}.feature.player.ui.OverlayView
import {NEW_PACKAGE}.feature.player.ui.ProQuickToolsStrip
import {NEW_PACKAGE}.feature.player.ui.rememberDarkPlayerProState
import {NEW_PACKAGE}.feature.player.ui.controls.ControlsBottomView
import {NEW_PACKAGE}.feature.player.ui.controls.ControlsMiddleView
import {NEW_PACKAGE}.feature.player.ui.controls.ControlsTopView

@OptIn(UnstableApi::class)
@Composable
fun PlayerControls(
    player: Player?,
    title: String,
    playerPreferences: PlayerPreferences,
    videoDecoderMode: DecoderMode?,
    controlsVisibilityState: ControlsVisibilityState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    isPipSupported: Boolean,
    onShowOverlay: (OverlayView) -> Unit,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onToggleTimeDisplay: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    modifier: Modifier = Modifier,
    middleControlsModifier: Modifier = Modifier,
) {{
    val proState = rememberDarkPlayerProState()
    DarkPlayerProEffects(player = player, proState = proState)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {{
        if (proState.nightModeEnabled) {{
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x5A120A04)),
            )
        }}

        Column {{
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {{
                Column {{
                    ControlsTopView(
                        title = title,
                        videoDecoderMode = videoDecoderMode,
                        onDecoderClick = {{ onShowOverlay(OverlayView.DECODER_SELECTOR) }},
                        onAudioClick = {{ onShowOverlay(OverlayView.AUDIO_SELECTOR) }},
                        onSubtitleClick = {{ onShowOverlay(OverlayView.SUBTITLE_SELECTOR) }},
                        onPlaylistClick = {{ onShowOverlay(OverlayView.PLAYLIST) }},
                        onBackClick = onBackClick,
                    )
                    if (!controlsVisibilityState.controlsLocked) {{
                        ProQuickToolsStrip(player = player, proState = proState)
                    }}
                }}
            }}
            Spacer(modifier = Modifier.weight(weight = 1f))
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible &&
                    !controlsVisibilityState.controlsLocked,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {{
                ControlsBottomView(
                    player = player,
                    progressState = progressState,
                    chaptersState = chaptersState,
                    onChaptersClick = {{ onShowOverlay(OverlayView.CHAPTERS) }},
                    controlsAlignment = when (playerPreferences.controlButtonsPosition) {{
                        ControlButtonsPosition.LEFT -> Alignment.Start
                        ControlButtonsPosition.RIGHT -> Alignment.End
                    }},
                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                    isPipSupported = isPipSupported,
                    showRemainingTime = playerPreferences.showRemainingTime,
                    onToggleTimeDisplay = onToggleTimeDisplay,
                    onSeek = seekGestureState::onSeek,
                    onSeekEnd = seekGestureState::onSeekEnd,
                    onPlaybackSpeedClick = {{ onShowOverlay(OverlayView.PLAYBACK_SPEED) }},
                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                    onLockControlsClick = {{
                        controlsVisibilityState.showControls()
                        controlsVisibilityState.lockControls()
                    }},
                    onVideoContentScaleClick = {{
                        controlsVisibilityState.showControls()
                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                    }},
                    onVideoContentScaleLongClick = {{
                        onShowOverlay(OverlayView.VIDEO_CONTENT_SCALE)
                    }},
                    onPictureInPictureClick = onPictureInPictureClick,
                )
            }}
        }}

        // Center playback controls remain cleanly centered
        if (controlsVisibilityState.controlsVisible &&
            seekGestureState.seekAmount == null &&
            !videoZoomAndContentScaleState.isZooming
        ) {{
            ControlsMiddleView(
                player = player,
                modifier = middleControlsModifier,
            )
        }}

        // Non-overlapping status pills for Seek, Zoom, and Crop/Fit mode changes
        when {{
            seekGestureState.seekAmount != null -> InfoView(
                info = "${{seekGestureState.seekAmountFormatted}}\\n[${{seekGestureState.seekToPositionFormated}}]",
            )

            videoZoomAndContentScaleState.isZooming -> InfoView(
                info = "${{(videoZoomAndContentScaleState.zoom * 100).toInt()}}%",
            )

            videoZoomAndContentScaleState.showContentScaleIndicator -> InfoView(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp),
                info = stringResource(videoZoomAndContentScaleState.videoContentScale.nameRes()),
            )
        }}

        DarkPlayerEqualizerDialog(proState = proState)
        DarkPlayerSleepTimerDialog(proState = proState)
    }}
}}
"""


SETTINGS_SCREEN_KT = f"""package {NEW_PACKAGE}.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import {NEW_PACKAGE}.core.ui.R
import {NEW_PACKAGE}.core.ui.components.NextTopAppBar
import {NEW_PACKAGE}.core.ui.components.rememberRestorableFocusState
import {NEW_PACKAGE}.core.ui.components.restorableFocusGroup
import {NEW_PACKAGE}.core.ui.components.restorableFocusItem
import {NEW_PACKAGE}.core.ui.components.tvFocusDown
import {NEW_PACKAGE}.core.ui.designsystem.NextIcons
import {NEW_PACKAGE}.core.ui.theme.NavyBackground
import {NEW_PACKAGE}.core.ui.theme.NavyBorder
import {NEW_PACKAGE}.core.ui.theme.NavySurface
import {NEW_PACKAGE}.core.ui.theme.NavySurfaceSecondary
import {NEW_PACKAGE}.core.ui.theme.NavyTextMuted
import {NEW_PACKAGE}.core.ui.theme.NavyTextPrimary
import {NEW_PACKAGE}.core.ui.theme.NavyTextSecondary
import {NEW_PACKAGE}.core.ui.updater.DarkPlayerUpdateDialog
import {NEW_PACKAGE}.core.ui.updater.DarkPlayerUpdateState

data class SettingsOutput(val navigateUp: () -> Unit, val openSetting: (Setting) -> Unit)

private sealed interface SettingsAction {{
    data object NavigateUp : SettingsAction
    data class OpenSetting(val setting: Setting) : SettingsAction
}}

@Composable
fun SettingsScreen(output: SettingsOutput) {{
    SettingsScreenContent {{ action ->
        when (action) {{
            is SettingsAction.NavigateUp -> output.navigateUp()
            is SettingsAction.OpenSetting -> output.openSetting(action.setting)
        }}
    }}
}}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun SettingsScreenContent(onAction: (SettingsAction) -> Unit) {{
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember(context) {{
        runCatching {{
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        }}.getOrDefault("1.0.0")
    }}
    val updateState = remember(scope, currentVersion) {{
        DarkPlayerUpdateState(scope = scope, currentVersionName = currentVersion)
    }}
    val focusState = rememberRestorableFocusState()

    val primaryRows = remember {{
        listOf(SettingRow.APPEARANCE, SettingRow.MEDIA_LIBRARY, SettingRow.PLAYER, SettingRow.GESTURES)
    }}
    val mediaRows = remember {{
        listOf(SettingRow.AUDIO, SettingRow.SUBTITLE, SettingRow.GENERAL, SettingRow.ABOUT)
    }}

    Box(modifier = Modifier.fillMaxSize()) {{
        Scaffold(
            topBar = {{
                NextTopAppBar(
                    title = stringResource(id = R.string.settings),
                    navigationIcon = {{
                        FilledTonalIconButton(
                            onClick = {{ onAction(SettingsAction.NavigateUp) }},
                            modifier = Modifier.tvFocusDown(focusState.requester),
                        ) {{
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigate_up),
                            )
                        }}
                    }},
                )
            }},
            containerColor = NavyBackground,
        ) {{ innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .restorableFocusGroup(state = focusState)
                    .padding(paddingValues = innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {{
                SettingsSectionHeader(title = "Interface & Playback")
                SettingsCardGroup {{
                    primaryRows.forEachIndexed {{ index, row ->
                        key(row) {{
                            DarkSettingCardRow(
                                modifier = Modifier.restorableFocusItem(focusState, row.name),
                                title = stringResource(id = row.titleResId),
                                description = stringResource(id = row.descriptionResId),
                                icon = row.icon,
                                onClick = {{ onAction(SettingsAction.OpenSetting(row.setting)) }},
                            )
                            if (index < primaryRows.lastIndex) {{
                                HorizontalDivider(
                                    color = NavyBorder.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }}
                        }}
                    }}
                }}

                SettingsSectionHeader(title = "Audio, Subtitles & System")
                SettingsCardGroup {{
                    mediaRows.forEachIndexed {{ index, row ->
                        key(row) {{
                            DarkSettingCardRow(
                                modifier = Modifier.restorableFocusItem(focusState, row.name),
                                title = stringResource(id = row.titleResId),
                                description = stringResource(id = row.descriptionResId),
                                icon = row.icon,
                                onClick = {{ onAction(SettingsAction.OpenSetting(row.setting)) }},
                            )
                            if (index < mediaRows.lastIndex) {{
                                HorizontalDivider(
                                    color = NavyBorder.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }}
                        }}
                    }}
                }}

                SettingsSectionHeader(title = "Quick Controls")
                SettingsCardGroup {{
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {{
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NavySurfaceSecondary),
                            contentAlignment = Alignment.Center,
                        ) {{
                            Icon(
                                imageVector = NextIcons.Speed,
                                contentDescription = null,
                                tint = NavyTextPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }}
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {{
                            Text(
                                text = "Hold for 2.0x Speed",
                                color = NavyTextPrimary,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = "Long-press player screen for pitch-locked 2x playback",
                                color = NavyTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }}
                        Text(
                            text = "ON",
                            color = NavyTextPrimary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(NavySurfaceSecondary)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }}
                }}

                SettingsSectionHeader(title = "Updates")
                SettingsCardGroup {{
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {{ updateState.check(manual = true) }}
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {{
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NavySurfaceSecondary),
                            contentAlignment = Alignment.Center,
                        ) {{
                            if (updateState.checking) {{
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = NavyTextPrimary,
                                )
                            }} else {{
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = NavyTextPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }}
                        }}
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {{
                            Text(
                                text = "Check for Updates",
                                color = NavyTextPrimary,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = updateState.statusMessage ?: "Current version: Dark Player $currentVersion",
                                color = NavyTextMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }}
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = NavyTextMuted,
                        )
                    }}
                }}

                Spacer(Modifier.height(24.dp))
            }}
        }}

        DarkPlayerUpdateDialog(state = updateState)
    }}
}}

@Composable
private fun SettingsSectionHeader(title: String) {{
    Text(
        text = title.uppercase(),
        color = NavyTextMuted,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        modifier = Modifier.padding(start = 6.dp),
    )
}}

@Composable
private fun SettingsCardGroup(content: @Composable () -> Unit) {{
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NavySurface)
            .border(1.dp, NavyBorder.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
    ) {{
        content()
    }}
}}

@Composable
private fun DarkSettingCardRow(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {{
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {{
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NavySurfaceSecondary),
            contentAlignment = Alignment.Center,
        ) {{
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NavyTextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }}
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {{
            Text(
                text = title,
                color = NavyTextPrimary,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = description,
                color = NavyTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }}
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = NavyTextMuted,
        )
    }}
}}

enum class Setting {{
    APPEARANCE,
    MEDIA_LIBRARY,
    PLAYER,
    GESTURES,
    AUDIO,
    SUBTITLE,
    GENERAL,
    ABOUT,
}}

private enum class SettingRow(
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector,
    val setting: Setting,
) {{
    APPEARANCE(R.string.appearance_name, R.string.appearance_description, NextIcons.Appearance, Setting.APPEARANCE),
    MEDIA_LIBRARY(R.string.media_library, R.string.media_library_description, NextIcons.Movie, Setting.MEDIA_LIBRARY),
    PLAYER(R.string.player_name, R.string.player_description, NextIcons.Player, Setting.PLAYER),
    GESTURES(R.string.gestures_name, R.string.gestures_description, NextIcons.SwipeHorizontal, Setting.GESTURES),
    AUDIO(R.string.audio, R.string.audio_desc, NextIcons.Audio, Setting.AUDIO),
    SUBTITLE(R.string.subtitle, R.string.subtitle_desc, NextIcons.Subtitle, Setting.SUBTITLE),
    GENERAL(R.string.general_name, R.string.general_description, NextIcons.ExtraSettings, Setting.GENERAL),
    ABOUT(R.string.about_name, R.string.about_description, NextIcons.Info, Setting.ABOUT),
}}
"""


def build_readme_md(github_repo: str) -> str:
    return f"""<p align="center">
  <img src="app/src/main/res/drawable/ic_darkplayer_logo.png" alt="Dark Player Logo" width="140" />
</p>

<h1 align="center">Dark Player</h1>

<p align="center">
  <strong>A Navy Dark &amp; Liquid Glass Android video player powered by Kotlin, Jetpack Compose, and Media3.</strong>
</p>

## Highlights

- **Navy Dark Graphite UI:** Engineered around a 7-shade high-contrast palette (`#0E0E10` background, `#232327` surfaces, `#3E3E45` secondary cards, `#63636E` hairline borders, `#F2F2F5` primary text/icons).
- **Liquid Glass Navigation Bar:** Floating frosted-glass bottom capsule using Android 12+ `RenderEffect` blur, translucent rim lighting, spring-animated active pill, and tactile haptics.
- **Redesigned Cinema Player Controls:** Instant access to HW/SW Decoder switching, HDR/SDR toggle, Subtitles, Audio Tracks, Playlist, PIP, Pitch-Locked 2x Speed, Lock, Rotate, Resize (Crop/Fit/Stretch/100%), and Frame Screenshot.
- **Zero-Glitch Aspect Ratio Engine:** Fixes double-density scaling bugs and clips zoomed/cropped frames cleanly with a non-overlapping top-center status pill.
- **Hardware Frame Screenshot:** Captures hardware-accelerated `SurfaceView` frames via `PixelCopy` and saves lossless PNGs to `Pictures/DarkPlayer/`.
- **HDR / SDR Auto-Detection & Toggle:** Probes hardware HEVC/VP9/AV1 10-bit decoders and switches `Window.colorMode` live.
- **Pitch-Locked 2x Speed:** Locks audio pitch at `1.0f` across all playback speeds (long-press the speed pill for instant 2.0x).
- **Built-in GitHub Releases Auto-Updater:** Checks for new releases, displays release notes, downloads the APK with live progress, and launches the system installer.

## Installation

### Option 1: Install Pre-Built APK
1. Download the latest `DarkPlayer.apk` from [GitHub Releases](https://github.com/{github_repo}/releases/latest).
2. Open the APK on your Android device (Android 7.0+ / API 24+; Liquid Glass blur requires Android 12+ / API 31+).
3. Allow installation from unknown sources if prompted, then tap **Install**.

### Option 2: Automated Build via GitHub Actions
1. Push this repository to GitHub (`main` branch).
2. *(Optional)* Add signing secrets under **Settings → Secrets and variables → Actions**:
   - `KEYSTORE_BASE64` — Base64-encoded release `.jks` / `.keystore` file (`base64 -w0 release.keystore`)
   - `KEYSTORE_PASSWORD` — Keystore password
   - `KEY_ALIAS` — Key alias
   - `KEY_PASSWORD` — Key password
   *(If omitted, CI safely signs with the included debug keystore so builds never fail.)*
3. Open the **Actions** tab → **Dark Player CI / Release Build** and download `DarkPlayer-Release-APKs`.

### Option 3: Build Locally from Source
```bash
git clone https://github.com/{github_repo}.git
cd DarkPlayer
./gradlew :app:assembleRelease
```
Release APKs are output to `app/build/outputs/apk/release/`.

## License

Released under the GNU General Public License v3.0.
"""


# =============================================================================
# TRANSFORMATION ENGINE
# =============================================================================


GITHUB_WORKFLOW_BUILD_YML = """name: Dark Player CI / Release Build

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

concurrency:
  group: darkplayer-build-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build-release:
    name: Build Dark Player Release APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v7

      - name: Set up JDK 21
        uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Setup Gradle & Cache
        uses: gradle/actions/setup-gradle@v6
        with:
          validate-wrappers: false
          cache-cleanup: on-success

      - name: Grant Execute Permission for Gradlew
        run: chmod +x gradlew

      - name: Prepare Release Signing Keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          if [ -n "$KEYSTORE_BASE64" ]; then
            echo "$KEYSTORE_BASE64" | base64 --decode > app/release.keystore
          else
            keytool -genkeypair -v -keystore app/release.keystore               -storepass "DarkPlayerRelease2026!" -keypass "DarkPlayerRelease2026!"               -alias darkplayer_release -keyalg RSA -keysize 2048 -validity 10000               -dname "CN=Mayank Droid, OU=DarkPlayer, O=DarkPlayer Studio, L=Patna, ST=Bihar, C=IN"
            echo "KEYSTORE_PASSWORD=DarkPlayerRelease2026!" >> "$GITHUB_ENV"
            echo "KEY_ALIAS=darkplayer_release" >> "$GITHUB_ENV"
            echo "KEY_PASSWORD=DarkPlayerRelease2026!" >> "$GITHUB_ENV"
          fi

      - name: Build Release APK
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew :app:assembleRelease --stacktrace

      - name: Prepare Universal APK
        run: |
          if [ -f app/build/outputs/apk/release/app-universal-release.apk ]; then
            cp app/build/outputs/apk/release/app-universal-release.apk DarkPlayer.apk
          elif [ -f app/build/outputs/apk/release/app-release.apk ]; then
            cp app/build/outputs/apk/release/app-release.apk DarkPlayer.apk
          fi

      - name: Upload DarkPlayer.apk Artifact
        uses: actions/upload-artifact@v7
        with:
          name: DarkPlayer-Release-APKs
          path: |
            DarkPlayer.apk
            app/build/outputs/apk/release/*.apk
          if-no-files-found: error
"""


def patch_gradle_files(project_root: Path) -> None:
    """
    1. Adds release signing config (GitHub Secrets + safe debug fallback) to app/build.gradle.kts.
    2. Ensures feature/settings/build.gradle.kts includes material-icons-extended for SettingsScreen.
    """
    app_gradle = project_root / "app" / "build.gradle.kts"
    if app_gradle.is_file():
        ag_text = app_gradle.read_text(encoding="utf-8")

        signing_old = """    signingConfigs {
        getByName("debug") {
            storeFile = file("${project.rootDir}/app/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }"""

        signing_new = """    signingConfigs {
        getByName("debug") {
            storeFile = file("${project.rootDir}/app/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val releaseKeystore = file("${project.rootDir}/app/release.keystore")
            if (releaseKeystore.exists() && !System.getenv("KEYSTORE_PASSWORD").isNullOrBlank()) {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "darkplayer"
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }"""

        if signing_old in ag_text:
            ag_text = ag_text.replace("\n\n" + signing_old, "", 1)
            ag_text = ag_text.replace(signing_old, "", 1)

        if "    buildTypes {" in ag_text and 'create("release")' not in ag_text:
            ag_text = ag_text.replace("    buildTypes {", signing_new + "\n\n    buildTypes {", 1)

        release_bt_old = """        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true"""

        release_bt_new = """        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true"""

        if release_bt_old in ag_text and 'signingConfig = signingConfigs.getByName("release")' not in ag_text:
            ag_text = ag_text.replace(release_bt_old, release_bt_new, 1)

        app_gradle.write_text(ag_text, encoding="utf-8")

    settings_gradle = project_root / "feature" / "settings" / "build.gradle.kts"
    if settings_gradle.is_file():
        sg_text = settings_gradle.read_text(encoding="utf-8")
        icon_dep = "    implementation(libs.androidx.compose.material.iconsExtended)"
        if "material.iconsExtended" not in sg_text and "androidx.compose.icons" not in sg_text:
            sg_text = sg_text.replace(
                "    implementation(libs.androidx.compose.material3)",
                "    implementation(libs.androidx.compose.material3)\n" + icon_dep,
                1,
            )
            settings_gradle.write_text(sg_text, encoding="utf-8")



TAP_GESTURE_STATE_KT = f"""package {NEW_PACKAGE}.feature.player.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import {NEW_PACKAGE}.core.model.DoubleTapGesture
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun rememberTapGestureState(
    player: Player,
    doubleTapGesture: DoubleTapGesture,
    seekIncrementMillis: Long,
    useLongPressGesture: Boolean,
    longPressSpeed: Float,
): TapGestureState {{
    val coroutineScope = rememberCoroutineScope()
    return remember(
        player,
        doubleTapGesture,
        seekIncrementMillis,
        useLongPressGesture,
        longPressSpeed,
    ) {{
        TapGestureState(
            player = player,
            doubleTapGesture = doubleTapGesture,
            seekIncrementMillis = seekIncrementMillis,
            useLongPressGesture = useLongPressGesture,
            longPressSpeed = longPressSpeed,
            coroutineScope = coroutineScope,
        )
    }}
}}

@Stable
class TapGestureState(
    private val player: Player,
    private val seekIncrementMillis: Long,
    private val useLongPressGesture: Boolean = true,
    private val coroutineScope: CoroutineScope,
    val longPressSpeed: Float = 2.0f,
    val doubleTapGesture: DoubleTapGesture,
    val interactionSource: MutableInteractionSource = MutableInteractionSource(),
) {{
    var seekMillis by mutableLongStateOf(value = 0L)
    var isLongPressGestureInAction by mutableStateOf(value = false)

    private var resetJob: Job? = null
    private var previousSpeed: Float = 1.0f

    fun handleDoubleTap(offset: Offset, size: IntSize) {{
        if (!player.isCurrentMediaItemSeekable) return

        val action = when (doubleTapGesture) {{
            DoubleTapGesture.FAST_FORWARD_AND_REWIND -> {{
                val viewCenterX = size.width / 2
                if (offset.x < viewCenterX) DoubleTapAction.SEEK_BACKWARD else DoubleTapAction.SEEK_FORWARD
            }}
            DoubleTapGesture.BOTH -> {{
                val eventPositionX = offset.x / size.width
                when {{
                    eventPositionX < 0.35 -> DoubleTapAction.SEEK_BACKWARD
                    eventPositionX > 0.65 -> DoubleTapAction.SEEK_FORWARD
                    else -> DoubleTapAction.PLAY_PAUSE
                }}
            }}
            DoubleTapGesture.PLAY_PAUSE -> DoubleTapAction.PLAY_PAUSE
            DoubleTapGesture.NONE -> return
        }}

        when (action) {{
            DoubleTapAction.SEEK_BACKWARD -> {{
                player.seekTo((player.currentPosition - seekIncrementMillis).coerceAtLeast(0L))
                if (seekMillis > 0L) seekMillis = 0L
                seekMillis -= seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
            }}
            DoubleTapAction.SEEK_FORWARD -> {{
                player.seekTo(player.currentPosition + seekIncrementMillis)
                if (seekMillis < 0L) seekMillis = 0L
                seekMillis += seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
            }}
            DoubleTapAction.PLAY_PAUSE -> {{
                if (player.isPlaying) player.pause() else player.play()
            }}
        }}
        resetDoubleTapSeekState()
    }}

    /**
     * Activates pitch-locked 2x (or configured) playback speed while holding down.
     */
    fun handleLongPress(offset: Offset) {{
        if (!useLongPressGesture) return
        if (!player.isPlaying && !player.playWhenReady) return

        previousSpeed = player.playbackParameters.speed.takeIf {{ it > 0f }} ?: 1.0f
        isLongPressGestureInAction = true
        val targetSpeed = longPressSpeed.coerceIn(1.25f, 4.0f)
        player.playbackParameters = PlaybackParameters(targetSpeed, 1.0f)
    }}

    /**
     * Always reverts speed back to the previous speed (pitch locked to 1.0f) on release/cancel.
     */
    fun handleOnLongPressRelease() {{
        if (isLongPressGestureInAction) {{
            isLongPressGestureInAction = false
            player.playbackParameters = PlaybackParameters(previousSpeed, 1.0f)
        }}
    }}

    private fun resetDoubleTapSeekState() {{
        resetJob?.cancel()
        resetJob = coroutineScope.launch {{
            delay(750.milliseconds)
            seekMillis = 0L
        }}
    }}
}}

enum class DoubleTapAction {{
    SEEK_BACKWARD,
    SEEK_FORWARD,
    PLAY_PAUSE,
}}
"""



DARK_PLAYER_PRO_TOOLS_KT = f"""package {NEW_PACKAGE}.feature.player.ui

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import {NEW_PACKAGE}.core.ui.theme.NavyBackground
import {NEW_PACKAGE}.core.ui.theme.NavyBorder
import {NEW_PACKAGE}.core.ui.theme.NavySurface
import {NEW_PACKAGE}.core.ui.theme.NavySurfaceSecondary
import {NEW_PACKAGE}.core.ui.theme.NavyTextMuted
import {NEW_PACKAGE}.core.ui.theme.NavyTextPrimary
import {NEW_PACKAGE}.core.ui.theme.NavyTextSecondary
import kotlinx.coroutines.delay

@Stable
class DarkPlayerProState {{
    var nightModeEnabled by mutableStateOf(false)
    var isMuted by mutableStateOf(false)
    private var savedVolume: Float = 1f

    // A-B Repeat
    var pointAMs by mutableStateOf<Long?>(null)
    var pointBMs by mutableStateOf<Long?>(null)

    // Sleep Timer
    var sleepTimerRemainingSec by mutableIntStateOf(0)
    var showSleepTimerSheet by mutableStateOf(false)

    // Equalizer
    var showEqualizerSheet by mutableStateOf(false)
    var eqEnabled by mutableStateOf(false)
    var selectedPreset by mutableStateOf("Custom")
    val bandLevelsDb = mutableStateListOf(0f, 0f, 0f, 0f, 0f)
    var bassBoostStrength by mutableFloatStateOf(0f)

    fun toggleMute(player: Player?) {{
        val p = player ?: return
        if (isMuted) {{
            p.volume = savedVolume.coerceIn(0.1f, 1f)
            isMuted = false
        }} else {{
            savedVolume = p.volume.takeIf {{ it > 0f }} ?: 1f
            p.volume = 0f
            isMuted = true
        }}
    }}

    fun cycleAbRepeat(player: Player?, onMessage: (String) -> Unit) {{
        val p = player ?: return
        when {{
            pointAMs == null -> {{
                pointAMs = p.currentPosition
                pointBMs = null
                onMessage("A-B Repeat: Point A set")
            }}
            pointBMs == null -> {{
                val a = pointAMs ?: 0L
                val pos = p.currentPosition
                if (pos > a + 500L) {{
                    pointBMs = pos
                    onMessage("A-B Repeat active")
                }} else {{
                    onMessage("Point B must be after Point A")
                }}
            }}
            else -> {{
                pointAMs = null
                pointBMs = null
                onMessage("A-B Repeat cleared")
            }}
        }}
    }}

    val abRepeatLabel: String
        get() = when {{
            pointAMs != null && pointBMs != null -> "A-B ON"
            pointAMs != null -> "A -> ?"
            else -> "A-B"
        }}

    val sleepTimerLabel: String
        get() = if (sleepTimerRemainingSec > 0) "${{(sleepTimerRemainingSec + 59) / 60}}m" else "Sleep"
}}

@Composable
fun rememberDarkPlayerProState(): DarkPlayerProState = remember {{ DarkPlayerProState() }}

@OptIn(UnstableApi::class)
@Composable
fun DarkPlayerProEffects(
    player: Player?,
    proState: DarkPlayerProState,
) {{
    // 1. A-B Repeat Loop Monitor
    LaunchedEffect(player, proState.pointAMs, proState.pointBMs) {{
        val a = proState.pointAMs ?: return@LaunchedEffect
        val b = proState.pointBMs ?: return@LaunchedEffect
        val p = player ?: return@LaunchedEffect
        while (true) {{
            if (p.isPlaying && p.currentPosition >= b) {{
                p.seekTo(a)
            }}
            delay(200L)
        }}
    }}

    // 2. Sleep Timer Countdown
    LaunchedEffect(proState.sleepTimerRemainingSec > 0, player) {{
        while (proState.sleepTimerRemainingSec > 0) {{
            delay(1000L)
            proState.sleepTimerRemainingSec = (proState.sleepTimerRemainingSec - 1).coerceAtLeast(0)
            if (proState.sleepTimerRemainingSec == 0) {{
                player?.pause()
            }}
        }}
    }}

    // 3. Hardware Audio Equalizer & BassBoost Engine
    val audioSessionId = remember(player) {{
        if (player is ExoPlayer) player.audioSessionId else 0
    }}
    var eqEngine by remember {{ mutableStateOf<Equalizer?>(null) }}
    var bassEngine by remember {{ mutableStateOf<BassBoost?>(null) }}

    DisposableEffect(audioSessionId, proState.eqEnabled) {{
        if (proState.eqEnabled) {{
            runCatching {{
                eqEngine = Equalizer(0, audioSessionId).apply {{ enabled = true }}
                bassEngine = BassBoost(0, audioSessionId).apply {{ enabled = true }}
            }}
        }}
        onDispose {{
            runCatching {{ eqEngine?.release() }}
            runCatching {{ bassEngine?.release() }}
            eqEngine = null
            bassEngine = null
        }}
    }}

    LaunchedEffect(
        eqEngine,
        proState.eqEnabled,
        proState.bandLevelsDb.toList(),
        proState.bassBoostStrength,
    ) {{
        val eq = eqEngine
        if (eq != null && proState.eqEnabled) {{
            runCatching {{
                val numBands = eq.numberOfBands.toInt().coerceAtMost(5)
                val minLevel = eq.bandLevelRange[0].toInt()
                val maxLevel = eq.bandLevelRange[1].toInt()
                for (i in 0 until numBands) {{
                    val targetMb = (proState.bandLevelsDb[i] * 100f).toInt().coerceIn(minLevel, maxLevel)
                    eq.setBandLevel(i.toShort(), targetMb.toShort())
                }}
            }}
        }}
        val bass = bassEngine
        if (bass != null && proState.eqEnabled) {{
            runCatching {{
                val strength = (proState.bassBoostStrength * 1000f).toInt().coerceIn(0, 1000)
                bass.setStrength(strength.toShort())
            }}
        }}
    }}
}}

/**
 * Scrollable Pro Quick-Tools Strip (Night Mode, Mute, Sleep Timer, A-B Repeat, Equalizer).
 */
@Composable
fun ProQuickToolsStrip(
    player: Player?,
    proState: DarkPlayerProState,
    modifier: Modifier = Modifier,
) {{
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {{
        ProToolChip(
            icon = Icons.Rounded.Bedtime,
            label = "Night",
            active = proState.nightModeEnabled,
            onClick = {{ proState.nightModeEnabled = !proState.nightModeEnabled }},
        )
        ProToolChip(
            icon = if (proState.isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
            label = if (proState.isMuted) "Muted" else "Mute",
            active = proState.isMuted,
            onClick = {{ proState.toggleMute(player) }},
        )
        ProToolChip(
            icon = Icons.Rounded.Timer,
            label = proState.sleepTimerLabel,
            active = proState.sleepTimerRemainingSec > 0,
            onClick = {{ proState.showSleepTimerSheet = true }},
        )
        ProToolChip(
            icon = Icons.Rounded.Repeat,
            label = proState.abRepeatLabel,
            active = proState.pointAMs != null,
            onClick = {{
                proState.cycleAbRepeat(player) {{ msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }}
            }},
        )
        ProToolChip(
            icon = Icons.Rounded.Equalizer,
            label = "Equalizer",
            active = proState.eqEnabled,
            onClick = {{ proState.showEqualizerSheet = true }},
        )
    }}
}}

@Composable
private fun ProToolChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {{
    val bg = if (active) NavyTextPrimary else NavySurface.copy(alpha = 0.88f)
    val fg = if (active) NavyBackground else NavyTextPrimary
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, NavyBorder.copy(alpha = 0.65f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {{
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
        )
    }}
}}

@Composable
fun DarkPlayerEqualizerDialog(proState: DarkPlayerProState) {{
    val presets = remember {{
        listOf(
            "Custom" to listOf(0f, 0f, 0f, 0f, 0f),
            "Normal" to listOf(0f, 0f, 0f, 0f, 0f),
            "Bass" to listOf(6f, 4f, 0f, 1f, 3f),
            "Classical" to listOf(4f, 2f, -1f, 3f, 4f),
            "Dance" to listOf(5f, 3f, 0f, 3f, 5f),
            "Rock" to listOf(4f, 2f, -1f, 3f, 5f),
            "Pop" to listOf(-1f, 2f, 4f, 2f, -1f),
        )
    }}
    val bandLabels = remember {{ listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz") }}

    AnimatedVisibility(
        visible = proState.showEqualizerSheet,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {{
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable {{ proState.showEqualizerSheet = false }},
            contentAlignment = Alignment.Center,
        ) {{
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 460.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .clickable {{ }}
                    .padding(18.dp),
            ) {{
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {{
                    Text(
                        text = "Equalizer",
                        color = NavyTextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = proState.eqEnabled,
                        onCheckedChange = {{ proState.eqEnabled = it }},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NavyBackground,
                            checkedTrackColor = NavyTextPrimary,
                            uncheckedThumbColor = NavyTextMuted,
                            uncheckedTrackColor = NavySurfaceSecondary,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = NavyTextSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {{ proState.showEqualizerSheet = false }},
                    )
                }}

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {{
                    presets.forEach {{ (name, values) ->
                        val selected = proState.selectedPreset == name
                        Text(
                            text = name,
                            color = if (selected) NavyBackground else NavyTextSecondary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) NavyTextPrimary else NavySurfaceSecondary)
                                .clickable {{
                                    proState.selectedPreset = name
                                    proState.eqEnabled = true
                                    values.forEachIndexed {{ idx, db -> proState.bandLevelsDb[idx] = db }}
                                }}
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }}
                }}

                Spacer(Modifier.height(12.dp))
                bandLabels.forEachIndexed {{ idx, hz ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {{
                        Text(
                            text = hz,
                            color = NavyTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.width(48.dp),
                        )
                        Slider(
                            value = proState.bandLevelsDb[idx],
                            onValueChange = {{
                                proState.selectedPreset = "Custom"
                                proState.bandLevelsDb[idx] = it
                            }},
                            valueRange = -12f..12f,
                            enabled = proState.eqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = NavyTextPrimary,
                                activeTrackColor = NavyTextPrimary,
                                inactiveTrackColor = NavySurfaceSecondary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${{proState.bandLevelsDb[idx].toInt()}} dB",
                            color = NavyTextPrimary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(42.dp),
                        )
                    }}
                }}

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {{
                    Text(
                        text = "Bass Boost",
                        color = NavyTextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(84.dp),
                    )
                    Slider(
                        value = proState.bassBoostStrength,
                        onValueChange = {{ proState.bassBoostStrength = it }},
                        valueRange = 0f..1f,
                        enabled = proState.eqEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = NavyTextPrimary,
                            activeTrackColor = NavyTextPrimary,
                            inactiveTrackColor = NavySurfaceSecondary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${{(proState.bassBoostStrength * 100).toInt()}}%",
                        color = NavyTextPrimary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.dp),
                    )
                }}
            }}
        }}
    }}
}}

@Composable
fun DarkPlayerSleepTimerDialog(proState: DarkPlayerProState) {{
    val options = remember {{ listOf(0 to "Off", 10 to "10 min", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "60 min") }}

    AnimatedVisibility(
        visible = proState.showSleepTimerSheet,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {{
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable {{ proState.showSleepTimerSheet = false }},
            contentAlignment = Alignment.Center,
        ) {{
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .clickable {{ }}
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {{
                Text(
                    text = "Sleep Timer",
                    color = NavyTextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                options.forEach {{ (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NavySurfaceSecondary.copy(alpha = 0.7f))
                            .clickable {{
                                proState.sleepTimerRemainingSec = minutes * 60
                                proState.showSleepTimerSheet = false
                            }}
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {{
                        Text(
                            text = label,
                            color = NavyTextPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }}
                }}
            }}
        }}
    }}
}}
"""


def is_text_file(path: Path) -> bool:
    if path.name in SPECIAL_TEXT_FILENAMES:
        return True
    if path.suffix.lower() in TEXT_EXTENSIONS:
        return True
    return False


def protect_external_maven(text: str) -> str:
    text = text.replace('group = "io.github.anilbeesetti"', NEXTLIB_TOML_SENTINEL)
    text = text.replace("io.github.anilbeesetti:nextlib", NEXTLIB_MAVEN_SENTINEL)
    text = text.replace("io.github.anilbeesetti.nextlib", NEXTLIB_IMPORT_SENTINEL)
    return text


def restore_external_maven(text: str) -> str:
    text = text.replace(NEXTLIB_TOML_SENTINEL, 'group = "io.github.anilbeesetti"')
    text = text.replace(NEXTLIB_MAVEN_SENTINEL, "io.github.anilbeesetti:nextlib")
    text = text.replace(NEXTLIB_IMPORT_SENTINEL, "io.github.anilbeesetti.nextlib")
    return text


def apply_global_renames(project_root: Path, purge_external_maven: bool = False) -> int:
    """Applies ordered regex renames across every text file in the project."""
    modified_count = 0
    for root, dirs, files in os.walk(project_root):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for filename in files:
            fpath = Path(root) / filename
            if not is_text_file(fpath):
                continue
            try:
                original = fpath.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError):
                continue

            updated = original if purge_external_maven else protect_external_maven(original)
            for pattern, replacement in ORDERED_REPLACEMENTS:
                updated = pattern.sub(replacement, updated)
            if not purge_external_maven:
                updated = restore_external_maven(updated)

            if updated != original:
                fpath.write_text(updated, encoding="utf-8")
                modified_count += 1
    return modified_count


def prune_upstream_metadata(project_root: Path) -> None:
    """Removes upstream-specific agent/funding metadata directories and files."""
    for rel in UPSTREAM_PRUNE_PATHS:
        target = project_root / rel
        if target.is_dir():
            shutil.rmtree(target, ignore_errors=True)
        elif target.is_file():
            target.unlink(missing_ok=True)


def migrate_package_directories(project_root: Path) -> int:
    """Moves all dev/anilbeesetti/nextplayer trees to com/mayank/darkplayer."""
    moved = 0
    candidates: List[Path] = []
    for root, dirs, _ in os.walk(project_root):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        p = Path(root)
        if p.parts[-3:] == OLD_PACKAGE_PATH.parts:
            candidates.append(p)

    for old_dir in candidates:
        base_java_dir = old_dir.parent.parent.parent
        new_dir = base_java_dir / NEW_PACKAGE_PATH
        new_dir.parent.mkdir(parents=True, exist_ok=True)
        if new_dir.exists():
            shutil.copytree(old_dir, new_dir, dirs_exist_ok=True)
            shutil.rmtree(old_dir)
        else:
            shutil.move(str(old_dir), str(new_dir))
        moved += 1

        # Clean up empty dev/anilbeesetti/ parents
        for ancestor in (old_dir.parent, old_dir.parent.parent):
            try:
                if ancestor.exists() and not any(ancestor.iterdir()):
                    ancestor.rmdir()
            except OSError:
                pass

    # Also rename dotted package directories (e.g., Room schema export folders)
    dotted_dirs: List[Path] = []
    for root, dirs, _ in os.walk(project_root):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for d in dirs:
            if OLD_PACKAGE in d:
                dotted_dirs.append(Path(root) / d)
    for dpath in sorted(dotted_dirs, key=lambda p: len(p.parts), reverse=True):
        target = dpath.parent / dpath.name.replace(OLD_PACKAGE, NEW_PACKAGE)
        if target.exists():
            shutil.copytree(dpath, target, dirs_exist_ok=True)
            shutil.rmtree(dpath)
        else:
            dpath.rename(target)
        moved += 1
    return moved


def rename_brand_files(project_root: Path) -> int:
    """Renames files containing NextPlayer in their filename."""
    renamed = 0
    for root, dirs, files in os.walk(project_root):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for fname in files:
            if "NextPlayer" in fname or "nextplayer" in fname.lower():
                old_path = Path(root) / fname
                new_name = re.sub(r"NextPlayer", "DarkPlayer", fname)
                new_name = re.sub(r"nextplayer", "darkplayer", new_name, flags=re.IGNORECASE)
                new_path = Path(root) / new_name
                if new_path != old_path:
                    old_path.rename(new_path)
                    renamed += 1
    return renamed


def inject_logo_assets(project_root: Path, logo_path: Optional[Path]) -> bool:
    """Injects the custom logo into drawable, playstore icon, and all mipmap densities."""
    if not logo_path or not logo_path.is_file():
        return False

    app_res = project_root / "app" / "src" / "main" / "res"
    drawable_dir = app_res / "drawable"
    drawable_dir.mkdir(parents=True, exist_ok=True)

    # 1. Build crisp circular-masked RGBA PNGs:
    #    - ic_darkplayer_logo_circle.png (clean circle with transparent corners for mipmaps/Play Store)
    #    - ic_darkplayer_logo.png (512x512 adaptive foreground with 70% centered circle so launchers never clip it)
    master_drawable = drawable_dir / "ic_darkplayer_logo.png"
    circle_master = drawable_dir / "ic_darkplayer_logo_circle.png"
    try:
        from PIL import Image, ImageDraw  # type: ignore

        with Image.open(logo_path) as raw_img:
            rgba = raw_img.convert("RGBA")
            side = min(rgba.size)
            left = (rgba.width - side) // 2
            top = (rgba.height - side) // 2
            sq = rgba.crop((left, top, left + side, top + side)).resize((512, 512), Image.Resampling.LANCZOS)

            # Anti-aliased circular mask (trims black square corners cleanly)
            mask_hr = Image.new("L", (2048, 2048), 0)
            draw_hr = ImageDraw.Draw(mask_hr)
            margin = 36  # slight inset to eliminate any black corner halo
            draw_hr.ellipse((margin, margin, 2048 - margin, 2048 - margin), fill=255)
            mask = mask_hr.resize((512, 512), Image.Resampling.LANCZOS)

            circle_img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
            circle_img.paste(sq, (0, 0), mask)
            circle_img.save(circle_master, format="PNG")

            # Adaptive icon foreground (centered at 68% safe zone so the full white circle stays intact)
            fg_canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
            inner_px = int(512 * 0.68)
            inner_circle = circle_img.resize((inner_px, inner_px), Image.Resampling.LANCZOS)
            offset = (512 - inner_px) // 2
            fg_canvas.paste(inner_circle, (offset, offset), inner_circle)
            fg_canvas.save(master_drawable, format="PNG")
    except Exception:
        shutil.copy2(logo_path, master_drawable)
        shutil.copy2(logo_path, circle_master)

    # 2. Replace Play Store icons using the verified true PNG
    for store_icon in (
        project_root / "app" / "src" / "main" / "ic_launcher-playstore.png",
        project_root / "core" / "ui" / "src" / "main" / "ic_launcher-playstore.png",
    ):
        if store_icon.parent.exists():
            shutil.copy2(circle_master, store_icon)

    # 3. Generate mipmap densities (uses Pillow if available, else direct copy)
    densities: Dict[str, int] = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    try:
        from PIL import Image  # type: ignore

        with Image.open(circle_master) as img:
            rgba = img.convert("RGBA")
            for folder, px in densities.items():
                out_dir = app_res / folder
                out_dir.mkdir(parents=True, exist_ok=True)
                for old_webp in ("ic_launcher.webp", "ic_launcher_round.webp"):
                    (out_dir / old_webp).unlink(missing_ok=True)
                resized = rgba.resize((px, px), Image.Resampling.LANCZOS)
                resized.save(out_dir / "ic_launcher.png", format="PNG")
                resized.save(out_dir / "ic_launcher_round.png", format="PNG")
    except Exception:
        for folder in densities:
            out_dir = app_res / folder
            out_dir.mkdir(parents=True, exist_ok=True)
            for old_webp in ("ic_launcher.webp", "ic_launcher_round.webp"):
                (out_dir / old_webp).unlink(missing_ok=True)
            shutil.copy2(logo_path, out_dir / "ic_launcher.png")
            shutil.copy2(logo_path, out_dir / "ic_launcher_round.png")

    # 4. Update adaptive launcher XMLs and Navy Dark launcher background
    anydpi_dir = app_res / "mipmap-anydpi-v26"
    anydpi_dir.mkdir(parents=True, exist_ok=True)
    adaptive_xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/ic_launcher_background_tint" />\n'
        '    <foreground android:drawable="@drawable/ic_darkplayer_logo" />\n'
        '    <monochrome android:drawable="@drawable/ic_darkplayer_logo" />\n'
        "</adaptive-icon>\n"
    )
    (anydpi_dir / "ic_launcher.xml").write_text(adaptive_xml, encoding="utf-8")
    (anydpi_dir / "ic_launcher_round.xml").write_text(adaptive_xml, encoding="utf-8")

    bg_xml = app_res / "values" / "ic_launcher_background.xml"
    bg_xml.parent.mkdir(parents=True, exist_ok=True)
    bg_xml.write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<resources>\n"
        f'    <color name="ic_launcher_background_tint">{PALETTE["background"]}</color>\n'
        f'    <color name="ic_launcher_foreground_tint">{PALETTE["text_primary"]}</color>\n'
        "</resources>\n",
        encoding="utf-8",
    )
    return True



def svg_to_vector_drawable(svg_bytes: bytes) -> Optional[str]:
    """Converts a clean 24x24 outline SVG (Tabler Icons) into an Android VectorDrawable XML."""
    import xml.etree.ElementTree as ET
    try:
        root = ET.fromstring(svg_bytes)
    except Exception:
        return None

    paths: List[str] = []
    for elem in root.iter():
        tag = elem.tag.split("}")[-1]
        if tag == "path":
            d = elem.attrib.get("d", "").strip()
            if not d or d == "M0 0h24v24H0z":
                continue
            paths.append(d)
        elif tag == "circle":
            cx = float(elem.attrib.get("cx", "12"))
            cy = float(elem.attrib.get("cy", "12"))
            r = float(elem.attrib.get("r", "1"))
            paths.append(f"M{cx - r},{cy}a{r},{r} 0 1,0 {2*r},0a{r},{r} 0 1,0 {-2*r},0")
        elif tag == "line":
            x1, y1 = elem.attrib.get("x1", "0"), elem.attrib.get("y1", "0")
            x2, y2 = elem.attrib.get("x2", "0"), elem.attrib.get("y2", "0")
            paths.append(f"M{x1},{y1}L{x2},{y2}")
        elif tag == "rect":
            x = float(elem.attrib.get("x", "0"))
            y = float(elem.attrib.get("y", "0"))
            w = float(elem.attrib.get("width", "0"))
            h = float(elem.attrib.get("height", "0"))
            if w > 0 and h > 0:
                paths.append(f"M{x},{y}h{w}v{h}h{-w}z")
        elif tag == "polyline" or tag == "polygon":
            pts = elem.attrib.get("points", "").strip().split()
            if len(pts) >= 2:
                d_str = "M" + "L".join(pts)
                if tag == "polygon":
                    d_str += "Z"
                paths.append(d_str)

    if not paths:
        return None

    lines = [
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp"',
        '    android:height="24dp"',
        '    android:viewportWidth="24"',
        '    android:viewportHeight="24">',
    ]
    for d in paths:
        lines.append(
            f'    <path android:pathData="{d}" '
            'android:strokeColor="#FFFFFFFF" android:strokeWidth="2" '
            'android:strokeLineCap="round" android:strokeLineJoin="round" '
            'android:fillColor="#00000000" />'
        )
    lines.append("</vector>\n")
    return "\n".join(lines)


def inject_svg_icons(project_root: Path, svg_zip_path: Optional[Path]) -> int:
    """Converts key outline SVGs from outline.zip into Android VectorDrawable XMLs in core/ui."""
    if not svg_zip_path or not svg_zip_path.is_file():
        return 0
    mapping = {
        "outline/arrow-left.svg": "ic_arrow_left.xml",
        "outline/lock.svg": "ic_lock.xml",
        "outline/lock-open.svg": "ic_lock_open.xml",
        "outline/headphones.svg": "ic_headset.xml",
        "outline/music.svg": "ic_audio_track.xml",
        "outline/subtitles.svg": "ic_subtitle_track.xml",
        "outline/playlist.svg": "ic_playlist.xml",
        "outline/picture-in-picture.svg": "ic_pip.xml",
        "outline/aspect-ratio.svg": "ic_aspect_ratio.xml",
        "outline/crop.svg": "ic_crop_landscape.xml",
        "outline/arrows-maximize.svg": "ic_fit_screen.xml",
        "outline/repeat.svg": "ic_loop_all.xml",
        "outline/repeat-once.svg": "ic_loop_one.xml",
        "outline/repeat-off.svg": "ic_loop_off.xml",
        "outline/arrows-shuffle.svg": "ic_shuffle.xml",
        "outline/screen-share.svg": "ic_screen_rotation.xml",
        "outline/rotate-clockwise.svg": "ic_screen_rotation_alt.xml",
        "outline/volume.svg": "ic_volume.xml",
        "outline/volume-off.svg": "ic_mute.xml",
        "outline/sun.svg": "ic_brightness.xml",
        "outline/camera.svg": "ic_screenshot.xml",
        "outline/gauge.svg": "ic_speed.xml",
    }
    drawable_dir = project_root / "core" / "ui" / "src" / "main" / "res" / "drawable"
    drawable_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    with zipfile.ZipFile(svg_zip_path, "r") as zf:
        available = set(zf.namelist())
        for svg_name, out_xml in mapping.items():
            if svg_name not in available:
                continue
            vec = svg_to_vector_drawable(zf.read(svg_name))
            if vec:
                (drawable_dir / out_xml).write_text(vec, encoding="utf-8")
                count += 1
    return count


def inject_custom_font(project_root: Path, font_path: Optional[Path]) -> bool:
    """Copies the custom .ttf font into core/ui/src/main/res/font/darkplayer_font.ttf."""
    if not font_path or not font_path.is_file():
        return False
    font_dir = project_root / "core" / "ui" / "src" / "main" / "res" / "font"
    font_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(font_path, font_dir / "darkplayer_font.ttf")
    return True


def patch_themes_and_manifest(project_root: Path, has_custom_font: bool) -> None:
    """Updates app themes.xml (Navy Dark splash + optional XML font) and AndroidManifest.xml."""
    themes_xml = project_root / "app" / "src" / "main" / "res" / "values" / "themes.xml"
    font_items = (
        '        <item name="android:fontFamily">@font/darkplayer_font</item>\n'
        '        <item name="fontFamily">@font/darkplayer_font</item>\n'
        if has_custom_font
        else ""
    )
    themes_content = f"""<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="Theme.DarkPlayer.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash</item>
        <item name="windowSplashScreenBackground">{PALETTE["background"]}</item>
        <item name="postSplashScreenTheme">@style/Theme.DarkPlayer</item>
    </style>

    <style name="Theme.DarkPlayer" parent="Theme.Material3.Dark.NoActionBar">
        <item name="android:windowBackground">{PALETTE["background"]}</item>
        <item name="android:statusBarColor">{PALETTE["background"]}</item>
        <item name="android:navigationBarColor">{PALETTE["background"]}</item>
{font_items}    </style>
</resources>
"""
    themes_xml.parent.mkdir(parents=True, exist_ok=True)
    themes_xml.write_text(themes_content, encoding="utf-8")

    # Ensure REQUEST_INSTALL_PACKAGES permission for built-in Auto-Updater
    manifest = project_root / "app" / "src" / "main" / "AndroidManifest.xml"
    if manifest.is_file():
        m_text = manifest.read_text(encoding="utf-8")
        perm = '    '
        if "REQUEST_INSTALL_PACKAGES" not in m_text:
            m_text = m_text.replace(
                '<uses-permission android:name="android.permission.INTERNET" />',
                '<uses-permission android:name="android.permission.INTERNET" />\n' + perm,
            )
            manifest.write_text(m_text, encoding="utf-8")

    # Ensure cache-path for APK updates in file_provider_paths.xml
    fp_xml = project_root / "app" / "src" / "main" / "res" / "xml" / "file_provider_paths.xml"
    if fp_xml.is_file():
        fp_text = fp_xml.read_text(encoding="utf-8")
        if 'path="updates/"' not in fp_text:
            fp_text = fp_text.replace(
                "</paths>",
                '    <cache-path name="darkplayer_updates" path="updates/" />\n</paths>',
            )
            fp_xml.write_text(fp_text, encoding="utf-8")



def patch_app_preferences_and_gestures(project_root: Path) -> None:
    """Enforces Navy Dark defaults, Hold-2x-Speed default ON, try/finally release, and MainActivity backdrop."""
    # 1. ApplicationPreferences.kt: Navy Dark default ON, Dynamic Colors default OFF
    app_prefs = (
        project_root / "core/model/src/main/java" / NEW_PACKAGE_PATH
        / "core/model/ApplicationPreferences.kt"
    )
    if app_prefs.is_file():
        ap = app_prefs.read_text(encoding="utf-8")
        ap = ap.replace("val themeConfig: ThemeConfig = ThemeConfig.SYSTEM,", "val themeConfig: ThemeConfig = ThemeConfig.ON,")
        ap = ap.replace("val useDynamicColors: Boolean = true,", "val useDynamicColors: Boolean = false,")
        app_prefs.write_text(ap, encoding="utf-8")

    # 2. PlayerPreferences.kt: Hold 2x Speed enabled by default
    player_prefs = (
        project_root / "core/model/src/main/java" / NEW_PACKAGE_PATH
        / "core/model/PlayerPreferences.kt"
    )
    if player_prefs.is_file():
        pp = player_prefs.read_text(encoding="utf-8")
        pp = pp.replace("val useLongPressControls: Boolean = false,", "val useLongPressControls: Boolean = true,")
        player_prefs.write_text(pp, encoding="utf-8")

    # 3. PlayerGestures.kt: Guarantee long-press 2x speed ALWAYS reverts on release/cancel via try/finally
    gestures = (
        project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH
        / "feature/player/ui/PlayerGestures.kt"
    )
    if gestures.is_file():
        gt = gestures.read_text(encoding="utf-8")
        old_press = """                        onPress = {
                            tryAwaitRelease()
                            tapGestureState.handleOnLongPressRelease()
                        },"""
        new_press = """                        onPress = {
                            try {
                                tryAwaitRelease()
                            } finally {
                                tapGestureState.handleOnLongPressRelease()
                            }
                        },"""
        if old_press in gt:
            gt = gt.replace(old_press, new_press, 1)
            gestures.write_text(gt, encoding="utf-8")

    # 4. MainActivity.kt: Provide kyant layerBackdrop ONLY to bottom LiquidGlassNavigationBar
    main_act = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "MainActivity.kt"
    if main_act.is_file():
        ma = main_act.read_text(encoding="utf-8")
        if "rememberLayerBackdrop" not in ma:
            ma = ma.replace(
                f"import {NEW_PACKAGE}.navigation.DarkNavigationBar",
                f"import com.kyant.backdrop.backdrops.layerBackdrop\n"
                f"import com.kyant.backdrop.backdrops.rememberLayerBackdrop\n"
                f"import {NEW_PACKAGE}.navigation.DarkNavigationBar\n"
                f"import {NEW_PACKAGE}.navigation.LocalNavGlassBackdrop",
                1,
            )
            ma = ma.replace(
                "    Box(modifier = Modifier.fillMaxSize()) {\n        Scaffold(",
                "    val navGlassBackdrop = rememberLayerBackdrop()\n"
                "    CompositionLocalProvider(LocalNavGlassBackdrop provides navGlassBackdrop) {\n"
                "    Box(modifier = Modifier.fillMaxSize()) {\n        Scaffold(",
                1,
            )
            ma = ma.replace(
                "                Box(\n                    modifier = Modifier\n                        .fillMaxWidth()",
                "                Box(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .layerBackdrop(navGlassBackdrop)",
                1,
            )
            # Close the CompositionLocalProvider right after NavigationLayout's outer Box closes
            ma = ma.replace(
                "        }\n    }\n}\n\n/**\n * Returns `true` if dark theme",
                "        }\n    }\n    }\n}\n\n/**\n * Returns `true` if dark theme",
                1,
            )
            main_act.write_text(ma, encoding="utf-8")

    # 5. app/build.gradle.kts: Add kyant backdrop + shapes for exact YTXTube Liquid Glass nav bar
    app_gradle = project_root / "app" / "build.gradle.kts"
    if app_gradle.is_file():
        ag = app_gradle.read_text(encoding="utf-8")
        if "io.github.kyant0:backdrop" not in ag:
            ag = ag.replace(
                "    implementation(libs.accompanist.permissions)",
                '    implementation(libs.accompanist.permissions)\n'
                '    implementation("io.github.kyant0:backdrop:2.0.1")\n'
                '    implementation("io.github.kyant0:shapes:1.2.1")',
                1,
            )
            app_gradle.write_text(ag, encoding="utf-8")

    # 6. SubtitleSelectorView.kt: Add clean Subtitle Vertical Position (Up / Down / Reset) controls
    sub_sel = (
        project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH
        / "feature/player/ui/SubtitleSelectorView.kt"
    )
    sub_pos = (
        project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH
        / "feature/player/state/SubtitlePositionState.kt"
    )
    if sub_pos.is_file():
        sp = sub_pos.read_text(encoding="utf-8")
        if "fun nudgeUp" not in sp:
            sp = sp.replace(
                "    fun syncSavedOffset(savedOffsetY: Float) {",
                "    fun nudgeUp(step: Float = 0.04f) {\n"
                "        offsetY = (offsetY - step).coerceIn(-0.45f, 0.25f)\n"
                "        onPositionSaved(offsetY)\n"
                "    }\n\n"
                "    fun nudgeDown(step: Float = 0.04f) {\n"
                "        offsetY = (offsetY + step).coerceIn(-0.45f, 0.25f)\n"
                "        onPositionSaved(offsetY)\n"
                "    }\n\n"
                "    fun resetPosition() {\n"
                "        offsetY = 0f\n"
                "        onPositionSaved(0f)\n"
                "    }\n\n"
                "    fun syncSavedOffset(savedOffsetY: Float) {",
                1,
            )
            sub_pos.write_text(sp, encoding="utf-8")


def patch_top_level_navigation(project_root: Path) -> None:
    """Wires LiquidGlassNavigationBar into TopLevelNavigation.kt."""
    nav_file = (
        project_root / "app" / "src" / "main" / "java" / NEW_PACKAGE_PATH
        / "navigation" / "TopLevelNavigation.kt"
    )
    if not nav_file.is_file():
        return
    content = nav_file.read_text(encoding="utf-8")
    pattern = re.compile(
        r"NavigationView\(\s*destinations\s*=\s*state\.destinations,\s*"
        r"isSelected\s*=\s*\{\s*it\.route\s*==\s*state\.topLevelRoute\s*\},\s*"
        r"onDestinationClick\s*=\s*\{\s*state\.switchTo\(it\.route\)\s*\},\s*\)",
        re.MULTILINE,
    )
    replacement = (
        "LiquidGlassNavigationBar(\n"
        "                    destinations = state.destinations,\n"
        "                    isSelected = { it.route == state.topLevelRoute },\n"
        "                    onDestinationClick = { state.switchTo(it.route) },\n"
        "                )"
    )
    new_content = pattern.sub(replacement, content)
    if new_content != content:
        nav_file.write_text(new_content, encoding="utf-8")


def write_generated_sources(
    project_root: Path,
    has_custom_font: bool,
    github_repo: str,
) -> List[Path]:
    """Generates and installs all Navy Dark, Liquid Glass, Player, and Updater files."""
    written: List[Path] = []

    def emit(rel_path: Path, content: str) -> None:
        target = project_root / rel_path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
        written.append(target)

    # 1. Navy Dark Palette & Global Custom Font Typography
    emit(Path("core/ui/src/main/res/values/colors.xml"), COLORS_XML)
    emit(Path("core/ui/src/main/java") / NEW_PACKAGE_PATH / "core/ui/theme/Color.kt", COLOR_KT)
    emit(Path("core/ui/src/main/java") / NEW_PACKAGE_PATH / "core/ui/theme/Theme.kt", THEME_KT)
    emit(
        Path("core/ui/src/main/java") / NEW_PACKAGE_PATH / "core/ui/theme/Type.kt",
        build_type_kt(has_custom_font),
    )

    # 2. YTXTube-style Auto-Updater (GitHub Releases + Install Dialog)
    emit(
        Path("core/ui/src/main/java") / NEW_PACKAGE_PATH / "core/ui/updater/DarkPlayerUpdater.kt",
        build_updater_kt(github_repo),
    )

    # 3. Exact YTXTube Liquid Glass Bottom Navigation Bar (ONLY for bottom nav)
    emit(
        Path("app/src/main/java") / NEW_PACKAGE_PATH / "navigation/GlassMotion.kt",
        GLASS_MOTION_KT,
    )
    emit(
        Path("app/src/main/java") / NEW_PACKAGE_PATH / "navigation/LiquidGlassNavBar.kt",
        LIQUID_GLASS_NAV_BAR_KT,
    )

    # 4. Feature Integration & Bug Fixes (Crop/Fit, Screenshot, HDR/SDR, 2x Pitch-Lock)
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/PlayerContentFrame.kt",
        PLAYER_CONTENT_FRAME_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/extensions/VideoContentScale.kt",
        VIDEO_CONTENT_SCALE_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/state/PlaybackParametersState.kt",
        PLAYBACK_PARAMETERS_STATE_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/state/TapGestureState.kt",
        TAP_GESTURE_STATE_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/utils/VideoScreenshotManager.kt",
        VIDEO_SCREENSHOT_MANAGER_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/utils/HdrSupportManager.kt",
        HDR_SUPPORT_MANAGER_KT,
    )

    # 5. Redesigned Player Controls & Non-Overlapping Feedback Pill
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/buttons/PlayerButton.kt",
        PLAYER_BUTTON_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/buttons/PlaybackSpeedButton.kt",
        PLAYBACK_SPEED_BUTTON_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/ui/PlayerFeedback.kt",
        PLAYER_FEEDBACK_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/ui/DarkPlayerProTools.kt",
        DARK_PLAYER_PRO_TOOLS_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/ui/controls/ControlsTopView.kt",
        CONTROLS_TOP_VIEW_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/ui/controls/ControlsBottomView.kt",
        CONTROLS_BOTTOM_VIEW_KT,
    )
    emit(
        Path("feature/player/src/main/java") / NEW_PACKAGE_PATH
        / "feature/player/PlayerControls.kt",
        PLAYER_CONTROLS_KT,
    )

    # 6. Redesigned Navy Dark Settings UI with Auto-Updater Card
    emit(
        Path("feature/settings/src/main/java") / NEW_PACKAGE_PATH / "settings/SettingsScreen.kt",
        SETTINGS_SCREEN_KT,
    )

    # 7. GitHub Actions CI/CD Workflow
    emit(Path(".github/workflows/build.yml"), GITHUB_WORKFLOW_BUILD_YML)

    # 8. Fresh GitHub README.md
    emit(Path("README.md"), build_readme_md(github_repo))

    return written


def audit_zero_trace(project_root: Path, purge_external_maven: bool = False) -> Tuple[int, int]:
    """
    Audits the transformed tree for forbidden brand/author traces.
    Returns (first_party_traces, external_maven_nextlib_refs).
    """
    forbidden = re.compile(
        r"next\s*player|nextplayer|ytxtube|anilbeesetti|koustubh12345",
        re.IGNORECASE,
    )
    external_nextlib = re.compile(r"io\.github\.anilbeesetti", re.IGNORECASE)

    first_party_hits = 0
    maven_hits = 0

    for root, dirs, files in os.walk(project_root):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for fname in files:
            fpath = Path(root) / fname
            if not is_text_file(fpath):
                continue
            try:
                text = fpath.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError):
                continue
            for line in text.splitlines():
                if not forbidden.search(line):
                    continue
                if not purge_external_maven and external_nextlib.search(line):
                    maven_hits += 1
                else:
                    first_party_hits += 1
    return first_party_hits, maven_hits


def package_zip(project_root: Path, output_zip: Path) -> None:
    """Packages the transformed Dark Player project cleanly into DarkPlayer.zip."""
    output_zip.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output_zip, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for root, dirs, files in os.walk(project_root):
            dirs[:] = sorted(d for d in dirs if d not in SKIP_DIRS)
            for fname in sorted(files):
                fpath = Path(root) / fname
                arcname = Path("DarkPlayer") / fpath.relative_to(project_root)
                zf.write(fpath, arcname=str(arcname))


def resolve_input_source(input_path: Path, staging_dir: Path) -> Path:
    """Accepts either an extracted Next Player folder or a .zip archive."""
    if input_path.is_file() and input_path.suffix.lower() == ".zip":
        extract_dir = staging_dir / "extracted"
        with zipfile.ZipFile(input_path, "r") as zf:
            zf.extractall(extract_dir)
        children = [c for c in extract_dir.iterdir() if c.is_dir() and c.name != "__MACOSX"]
        if len(children) == 1 and (children[0] / "settings.gradle.kts").exists():
            return children[0]
        return extract_dir

    if input_path.is_dir():
        dest = staging_dir / "project"
        shutil.copytree(
            input_path,
            dest,
            ignore=shutil.ignore_patterns(".git", ".gradle", "build", ".idea"),
        )
        return dest

    raise FileNotFoundError(f"Input source not found: {input_path}")


def auto_detect_asset(explicit: Optional[str], patterns: Iterable[str]) -> Optional[Path]:
    if explicit:
        p = Path(explicit).expanduser().resolve()
        return p if p.is_file() else None
    cwd = Path.cwd()
    for pat in patterns:
        matches = sorted(cwd.glob(pat))
        if matches:
            return matches[0]
    return None



V4_SETTINGS_DIALOGS_KT = """package com.mayank.darkplayer.core.ui.theme

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Environment
import android.os.LocaleList
import android.os.StatFs
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import java.io.File
import java.util.Locale

enum class DarkPlayerThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light"),
    AMOLED("AMOLED"),
}

enum class DarkPlayerPalette(
    val label: String,
    val accentDark: Color,
    val onAccentDark: Color,
    val accentLight: Color,
    val onAccentLight: Color,
    val swatchSecondary: Color,
) {
    OBSIDIAN(
        label = "Obsidian",
        accentDark = Color(0xFFE8E8EE),
        onAccentDark = Color(0xFF0E0E10),
        accentLight = Color(0xFF1F2026),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFF8A8A98),
    ),
    COBALT(
        label = "Cobalt",
        accentDark = Color(0xFF56A8FF),
        onAccentDark = Color(0xFF041A33),
        accentLight = Color(0xFF1668C7),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFF2B6CB0),
    ),
    EMERALD(
        label = "Emerald",
        accentDark = Color(0xFF34D399),
        onAccentDark = Color(0xFF032318),
        accentLight = Color(0xFF0D8A5E),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFF059669),
    ),
    AMBER(
        label = "Amber",
        accentDark = Color(0xFFFBBF24),
        onAccentDark = Color(0xFF241800),
        accentLight = Color(0xFFB4690E),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFFD97706),
    ),
    VIOLET(
        label = "Violet",
        accentDark = Color(0xFFA78BFA),
        onAccentDark = Color(0xFF1A0D3D),
        accentLight = Color(0xFF6D42D8),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFF7C3AED),
    ),
    CRIMSON(
        label = "Crimson",
        accentDark = Color(0xFFFB7185),
        onAccentDark = Color(0xFF330611),
        accentLight = Color(0xFFC82848),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFFE11D48),
    ),
}

object DarkPlayerUiPrefs {
    private const val PREF_NAME = "darkplayer_ui_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_PALETTE = "palette"
    private const val KEY_LIQUID_GLASS = "liquid_glass"
    private const val KEY_LANG = "language_tag"

    @Volatile
    private var loaded = false

    var themeMode by mutableStateOf(DarkPlayerThemeMode.DARK)
    var palette by mutableStateOf(DarkPlayerPalette.EMERALD)
    var liquidGlassEnabled by mutableStateOf(true)
    var languageTag by mutableStateOf("")

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedMode = sp.getString(KEY_THEME_MODE, DarkPlayerThemeMode.DARK.name)
        val savedPalette = sp.getString(KEY_PALETTE, DarkPlayerPalette.EMERALD.name)
        themeMode = DarkPlayerThemeMode.entries.firstOrNull { it.name == savedMode } ?: DarkPlayerThemeMode.DARK
        palette = DarkPlayerPalette.entries.firstOrNull { it.name == savedPalette } ?: DarkPlayerPalette.EMERALD
        liquidGlassEnabled = sp.getBoolean(KEY_LIQUID_GLASS, true)
        languageTag = sp.getString(KEY_LANG, "") ?: ""
    }

    fun updateThemeMode(context: Context, mode: DarkPlayerThemeMode) {
        themeMode = mode
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun updatePalette(context: Context, newPalette: DarkPlayerPalette) {
        palette = newPalette
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PALETTE, newPalette.name).apply()
    }

    fun updateLiquidGlass(context: Context, enabled: Boolean) {
        liquidGlassEnabled = enabled
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LIQUID_GLASS, enabled).apply()
    }

    private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivityOrNull()
        else -> null
    }

    fun updateLanguage(context: Context, tag: String) {
        languageTag = tag
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, tag).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val lm = context.getSystemService(LocaleManager::class.java)
                lm?.applicationLocales = if (tag.isBlank()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(tag)
                }
            }
        } else {
            val locales = if (tag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
            val locale = if (tag.isBlank()) Locale.getDefault() else Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val res = context.resources
            val config = res.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
        }
        context.findActivityOrNull()?.recreate()
    }
}

@Composable
private fun DarkModalFrame(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val chipBg = MaterialTheme.colorScheme.surfaceContainerHighest

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setDimAmount(0.45f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 48
                    }
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(28.dp),
            color = surfaceColor,
            tonalElevation = 0.dp,
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(chipBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(chipBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun DarkPlayerAppearanceDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    DarkPlayerUiPrefs.ensureLoaded(context)
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest

    DarkModalFrame(
        title = "Appearance & Theme",
        subtitle = "Theme mode, color palette and UI style",
        onDismiss = onDismiss,
    ) {
        // 1. Liquid Glass Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Liquid Glass",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Frosted, see-through navigation bar with depth & blur",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = DarkPlayerUiPrefs.liquidGlassEnabled,
                onCheckedChange = { DarkPlayerUiPrefs.updateLiquidGlass(context, it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = onAccent,
                    checkedTrackColor = accent,
                ),
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(16.dp))

        // 2. Theme Mode
        Text(
            text = "Theme mode",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Choose how Dark Player looks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DarkPlayerThemeMode.entries.forEach { mode ->
                val selected = DarkPlayerUiPrefs.themeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) accent else pillBg)
                        .clickable { DarkPlayerUiPrefs.updateThemeMode(context, mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selected) onAccent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(16.dp))

        // 3. Color Palette
        Text(
            text = "Color palette",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "High-contrast accent combinations",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DarkPlayerPalette.entries.forEach { pal ->
                val selected = DarkPlayerUiPrefs.palette == pal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { DarkPlayerUiPrefs.updatePalette(context, pal) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(pal.accentDark, pal.swatchSecondary),
                                ),
                            )
                            .then(
                                if (selected) {
                                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = pal.onAccentDark,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = pal.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class AppLanguageOption(val tag: String, val nativeName: String, val subtitle: String)

private val DARK_PLAYER_LANGUAGES = listOf(
    AppLanguageOption("", "Auto (System default)", "Follow device language"),
    AppLanguageOption("en", "English", "English"),
    AppLanguageOption("hi", "हिन्दी", "Hindi"),
    AppLanguageOption("es", "Español", "Spanish"),
    AppLanguageOption("fr", "Français", "French"),
    AppLanguageOption("de", "Deutsch", "German"),
    AppLanguageOption("ar", "العربية", "Arabic"),
    AppLanguageOption("pt", "Português", "Portuguese"),
    AppLanguageOption("ru", "Русский", "Russian"),
    AppLanguageOption("ja", "日本語", "Japanese"),
    AppLanguageOption("ko", "한국어", "Korean"),
    AppLanguageOption("id", "Bahasa Indonesia", "Indonesian"),
    AppLanguageOption("tr", "Türkçe", "Turkish"),
    AppLanguageOption("zh", "中文", "Chinese"),
)

@Composable
fun DarkPlayerLanguageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    DarkPlayerUiPrefs.ensureLoaded(context)
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) DARK_PLAYER_LANGUAGES
        else DARK_PLAYER_LANGUAGES.filter {
            it.nativeName.lowercase(Locale.ROOT).contains(q) ||
                it.subtitle.lowercase(Locale.ROOT).contains(q)
        }
    }
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary

    DarkModalFrame(
        title = "Language",
        subtitle = "Choose application language",
        onDismiss = onDismiss,
    ) {
        // Search box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search languages...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontFamily = DarkPlayerFontFamily,
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.tag }) { lang ->
                val selected = DarkPlayerUiPrefs.languageTag == lang.tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) accent.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                        )
                        .then(
                            if (selected) Modifier.border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            else Modifier,
                        )
                        .clickable {
                            DarkPlayerUiPrefs.updateLanguage(context, lang.tag)
                            Toast.makeText(context, "Language: ${lang.nativeName}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lang.nativeName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = lang.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = onAccent,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dirSizeBytes(file: File?): Long {
    if (file == null || !file.exists()) return 0L
    if (file.isFile) return file.length()
    var sum = 0L
    file.listFiles()?.forEach { sum += dirSizeBytes(it) }
    return sum
}

private fun formatBytesReadable(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.1f GB", mb / 1024.0)
}

@Composable
fun DarkPlayerStorageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val accent = MaterialTheme.colorScheme.primary

    val stats = remember(refreshTick) {
        val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalBytes = statFs?.totalBytes ?: 1L
        val freeBytes = statFs?.availableBytes ?: 0L
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

        val cacheBytes = dirSizeBytes(context.cacheDir) +
            dirSizeBytes(context.externalCacheDir) +
            dirSizeBytes(context.codeCacheDir)
        val dataBytes = dirSizeBytes(context.filesDir)
        val apkBytes = runCatching { File(context.applicationInfo.sourceDir).length() }.getOrDefault(0L)
        val appTotal = apkBytes + cacheBytes + dataBytes

        arrayOf(totalBytes, usedBytes, freeBytes, apkBytes, cacheBytes, dataBytes, appTotal)
    }

    val totalBytes = stats[0].coerceAtLeast(1L)
    val usedBytes = stats[1]
    val freeBytes = stats[2]
    val apkBytes = stats[3]
    val cacheBytes = stats[4]
    val dataBytes = stats[5]
    val appTotal = stats[6]
    val usedFraction = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.02f, 1f)

    DarkModalFrame(
        title = "Storage Usage",
        subtitle = "See phone storage & clear Dark Player cache",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "Device storage",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usedFraction)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accent, MaterialTheme.colorScheme.onSurface),
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${formatBytesReadable(usedBytes)} used of ${formatBytesReadable(totalBytes)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${formatBytesReadable(freeBytes)} free",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Used by Dark Player",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Total space: ${formatBytesReadable(appTotal)}",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = accent,
        )

        Spacer(Modifier.height(14.dp))
        StorageMetricRow("App & Core engine", formatBytesReadable(apkBytes))
        Spacer(Modifier.height(10.dp))
        StorageMetricRow("Video & thumbnail cache", formatBytesReadable(cacheBytes))
        Spacer(Modifier.height(10.dp))
        StorageMetricRow("Saved offline data & config", formatBytesReadable(dataBytes))

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .clickable {
                    runCatching {
                        context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                        context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                        context.codeCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                    }
                    refreshTick++
                    Toast.makeText(context, "Dark Player cache cleared", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Clear Dark Player Cache",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = accent,
            )
        }
    }
}

@Composable
private fun StorageMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
"""

V4_THEME_KT = """package com.mayank.darkplayer.core.ui.theme

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
    background = Color(0xFFF6F6F9),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFE7E7EE),
    onSurfaceVariant = Color(0xFF474752),
    outline = Color(0xFF777785),
    outlineVariant = Color(0xFFD2D2DC),
    scrim = scrimLight,
    inverseSurface = Color(0xFF1A1A1F),
    inverseOnSurface = Color(0xFFF2F2F5),
    inversePrimary = primaryDark,
    surfaceDim = Color(0xFFEAEAF0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F4F8),
    surfaceContainer = Color(0xFFEEEEF3),
    surfaceContainerHigh = Color(0xFFE8E8EF),
    surfaceContainerHighest = Color(0xFFE0E0E8),
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
            primaryContainer = pal.accentDark.copy(alpha = 0.20f),
            onPrimaryContainer = Color(0xFFF2F2F5),
            secondary = pal.accentDark,
            onSecondary = pal.onAccentDark,
        )

        else -> baseLightScheme.copy(
            primary = pal.accentLight,
            onPrimary = pal.onAccentLight,
            primaryContainer = pal.accentLight.copy(alpha = 0.14f),
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
"""

V4_QUICK_SETTINGS_DIALOG_KT = """package com.mayank.darkplayer.feature.videopicker.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mayank.darkplayer.core.model.ApplicationPreferences
import com.mayank.darkplayer.core.model.MediaLayoutMode
import com.mayank.darkplayer.core.model.MediaViewMode
import com.mayank.darkplayer.core.model.Sort
import com.mayank.darkplayer.core.ui.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSettingsDialog(
    applicationPreferences: ApplicationPreferences,
    onDismiss: () -> Unit,
    updatePreferences: (ApplicationPreferences) -> Unit,
) {
    var preferences by remember { mutableStateOf(value = applicationPreferences) }
    var fieldsExpanded by remember { mutableStateOf(false) }

    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                // Header Row: View Mode | Layout (MX Pro style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(3f)) {
                        Text(
                            text = "View Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            MxOptionTile(
                                label = "All folders",
                                icon = Icons.Rounded.FolderCopy,
                                selected = preferences.mediaViewMode == MediaViewMode.FOLDER_TREE,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDER_TREE) },
                            )
                            MxOptionTile(
                                label = "Files",
                                icon = Icons.Rounded.VideoFile,
                                selected = preferences.mediaViewMode == MediaViewMode.VIDEOS,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.VIDEOS) },
                            )
                            MxOptionTile(
                                label = "Folders",
                                icon = Icons.Rounded.Folder,
                                selected = preferences.mediaViewMode == MediaViewMode.FOLDERS,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDERS) },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 34.dp, start = 6.dp, end = 6.dp)
                            .width(1.dp)
                            .height(48.dp)
                            .background(borderColor),
                    )

                    Column(modifier = Modifier.weight(2f)) {
                        Text(
                            text = "Layout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            MxOptionTile(
                                label = "List",
                                icon = Icons.Rounded.ViewAgenda,
                                selected = preferences.mediaLayoutMode == MediaLayoutMode.LIST,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST) },
                            )
                            MxOptionTile(
                                label = "Grid",
                                icon = Icons.Rounded.GridView,
                                selected = preferences.mediaLayoutMode == MediaLayoutMode.GRID,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.GRID) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(16.dp))

                // Sort section
                Text(
                    text = "Sort",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = onSurface,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val sortItems = listOf(
                        Triple(Sort.By.TITLE, "Title", Icons.Rounded.SortByAlpha),
                        Triple(Sort.By.DATE, "Date", Icons.Rounded.CalendarToday),
                        Triple(Sort.By.LENGTH, "Duration", Icons.Rounded.Timer),
                        Triple(Sort.By.SIZE, "Size", Icons.Rounded.SdStorage),
                        Triple(Sort.By.PATH, "Location", Icons.Rounded.LocationOn),
                    )
                    sortItems.forEach { (sortBy, label, icon) ->
                        MxOptionTile(
                            label = label,
                            icon = icon,
                            selected = preferences.sortBy == sortBy,
                            accent = accent,
                            onClick = { preferences = preferences.copy(sortBy = sortBy) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // A to Z / Z to A Segmented Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                ) {
                    val isAsc = preferences.sortOrder == Sort.Order.ASCENDING
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isAsc) accent.copy(alpha = 0.16f) else Color.Transparent)
                            .then(
                                if (isAsc) Modifier.border(1.dp, accent, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                else Modifier,
                            )
                            .clickable { preferences = preferences.copy(sortOrder = Sort.Order.ASCENDING) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = if (isAsc) accent else muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "A to Z",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isAsc) accent else muted,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (!isAsc) accent.copy(alpha = 0.16f) else Color.Transparent)
                            .then(
                                if (!isAsc) Modifier.border(1.dp, accent, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                else Modifier,
                            )
                            .clickable { preferences = preferences.copy(sortOrder = Sort.Order.DESCENDING) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (!isAsc) accent else muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Z to A",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (!isAsc) accent else muted,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = borderColor)

                // Expandable Fields Accordion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fieldsExpanded = !fieldsExpanded }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.fields),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = onSurface,
                    )
                    Icon(
                        imageVector = if (fieldsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = muted,
                    )
                }

                AnimatedVisibility(visible = fieldsExpanded) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MxFieldChip(
                            text = stringResource(R.string.duration),
                            selected = preferences.showDurationField,
                            onClick = { preferences = preferences.copy(showDurationField = !preferences.showDurationField) },
                        )
                        MxFieldChip(
                            text = stringResource(R.string.folder_duration),
                            selected = preferences.showFolderDurationField,
                            onClick = { preferences = preferences.copy(showFolderDurationField = !preferences.showFolderDurationField) },
                        )
                        MxFieldChip(
                            text = stringResource(R.string.extension),
                            selected = preferences.showExtensionField,
                            onClick = { preferences = preferences.copy(showExtensionField = !preferences.showExtensionField) },
                        )
                        MxFieldChip(
                            text = stringResource(R.string.path),
                            selected = preferences.showPathField,
                            onClick = { preferences = preferences.copy(showPathField = !preferences.showPathField) },
                        )
                        MxFieldChip(
                            text = stringResource(R.string.played_progress),
                            selected = preferences.showPlayedProgress,
                            onClick = { preferences = preferences.copy(showPlayedProgress = !preferences.showPlayedProgress) },
                        )
                        MxFieldChip(
                            text = stringResource(R.string.resolution),
                            selected = preferences.showResolutionField,
                            onClick = { preferences = preferences.copy(showResolutionField = !preferences.showResolutionField) },
                        )
                        MxFieldChip(
                            text = stringResource(R.string.size),
                            selected = preferences.showSizeField,
                            onClick = { preferences = preferences.copy(showSizeField = !preferences.showSizeField) },
                        )
                        MxFieldChip(
                            text = stringResource(R.string.thumbnail),
                            selected = preferences.showThumbnailField,
                            onClick = { preferences = preferences.copy(showThumbnailField = !preferences.showThumbnailField) },
                        )
                    }
                }

                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(10.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = muted,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            updatePreferences(preferences)
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            color = accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MxOptionTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun MxFieldChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = text, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}
"""

V4_PRO_TOOLS_KT = """package com.mayank.darkplayer.feature.player.ui

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mayank.darkplayer.core.ui.theme.NavyBorder
import com.mayank.darkplayer.core.ui.theme.NavySurface
import com.mayank.darkplayer.core.ui.theme.NavySurfaceSecondary
import com.mayank.darkplayer.core.ui.theme.NavyTextPrimary
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillFill
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillIcon
import kotlinx.coroutines.delay

@Stable
class DarkPlayerProState {
    var nightModeEnabled by mutableStateOf(false)
    var isMuted by mutableStateOf(false)
    var savedVolume by mutableFloatStateOf(1f)

    // A-B Repeat
    var abPointA by mutableStateOf<Long?>(null)
    var abPointB by mutableStateOf<Long?>(null)

    // Sleep Timer (remaining seconds, 0 = off)
    var sleepTimerRemainingSec by mutableIntStateOf(0)
    var showSleepDialog by mutableStateOf(false)

    fun toggleMute(player: Player?) {
        if (player == null) return
        if (isMuted) {
            player.volume = savedVolume.coerceIn(0.1f, 1f)
            isMuted = false
        } else {
            savedVolume = if (player.volume > 0f) player.volume else 1f
            player.volume = 0f
            isMuted = true
        }
    }

    fun cycleAbRepeat(player: Player?): String {
        val pos = player?.currentPosition ?: return "A-B Repeat unavailable"
        return when {
            abPointA == null -> {
                abPointA = pos
                abPointB = null
                "A-B Repeat: Point A set"
            }
            abPointB == null -> {
                val start = abPointA ?: 0L
                if (pos > start + 500L) {
                    abPointB = pos
                    "A-B Repeat: Looping A to B"
                } else {
                    abPointA = null
                    abPointB = null
                    "A-B Repeat: Cleared"
                }
            }
            else -> {
                abPointA = null
                abPointB = null
                "A-B Repeat: Off"
            }
        }
    }
}

@Composable
fun rememberDarkPlayerProState(): DarkPlayerProState = remember { DarkPlayerProState() }

@OptIn(UnstableApi::class)
@Composable
fun DarkPlayerProEffects(
    player: Player?,
    proState: DarkPlayerProState,
) {
    // A-B Repeat loop monitor
    LaunchedEffect(player, proState.abPointA, proState.abPointB) {
        val a = proState.abPointA
        val b = proState.abPointB
        if (player != null && a != null && b != null && b > a) {
            while (true) {
                if (player.currentPosition >= b) {
                    player.seekTo(a)
                }
                delay(200L)
            }
        }
    }

    // Sleep Timer countdown
    LaunchedEffect(player, proState.sleepTimerRemainingSec > 0) {
        while (proState.sleepTimerRemainingSec > 0) {
            delay(1000L)
            proState.sleepTimerRemainingSec = (proState.sleepTimerRemainingSec - 1).coerceAtLeast(0)
            if (proState.sleepTimerRemainingSec == 0) {
                player?.pause()
            }
        }
    }
}

/**
 * Circular Pro Quick-Tools bar (Night, Mute, Sleep, A-B Repeat).
 * Equalizer removed per user request; circular buttons match bottom controls.
 */
@Composable
fun ProQuickToolsStrip(
    player: Player?,
    proState: DarkPlayerProState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sleepMin = (proState.sleepTimerRemainingSec + 59) / 60
    val abActive = proState.abPointA != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProCircleButton(
            icon = Icons.Rounded.Bedtime,
            contentDescription = "Night Mode",
            active = proState.nightModeEnabled,
            badgeText = null,
            onClick = {
                proState.nightModeEnabled = !proState.nightModeEnabled
                Toast.makeText(
                    context,
                    if (proState.nightModeEnabled) "Night Mode ON" else "Night Mode OFF",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        ProCircleButton(
            icon = if (proState.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = "Mute",
            active = proState.isMuted,
            badgeText = null,
            onClick = { proState.toggleMute(player) },
        )
        ProCircleButton(
            icon = Icons.Rounded.Timer,
            contentDescription = "Sleep Timer",
            active = proState.sleepTimerRemainingSec > 0,
            badgeText = if (proState.sleepTimerRemainingSec > 0) "${sleepMin}m" else null,
            onClick = { proState.showSleepDialog = true },
        )
        ProCircleButton(
            icon = Icons.Rounded.Repeat,
            contentDescription = "A-B Repeat",
            active = abActive,
            badgeText = when {
                proState.abPointA != null && proState.abPointB != null -> "A-B"
                proState.abPointA != null -> "A"
                else -> null
            },
            onClick = {
                val msg = proState.cycleAbRepeat(player)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun ProCircleButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    badgeText: String?,
    onClick: () -> Unit,
) {
    val bgColor = if (active) MaterialTheme.colorScheme.primary else PlayerControlPillFill
    val fgColor = if (active) MaterialTheme.colorScheme.onPrimary else PlayerControlPillIcon

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (badgeText != null) {
            Text(
                text = badgeText,
                color = fgColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = fgColor,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
fun DarkPlayerSleepTimerDialog(proState: DarkPlayerProState) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = proState.showSleepDialog,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { proState.showSleepDialog = false },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 360.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, RoundedCornerShape(22.dp))
                    .clickable(enabled = false) {}
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Sleep Timer",
                        color = NavyTextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = NavyTextPrimary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { proState.showSleepDialog = false },
                    )
                }
                Spacer(Modifier.height(14.dp))
                val options = listOf(0 to "Off", 10 to "10 min", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "60 min")
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { (mins, label) ->
                            val isSelected = if (mins == 0) {
                                proState.sleepTimerRemainingSec == 0
                            } else {
                                proState.sleepTimerRemainingSec in ((mins - 1) * 60 + 1)..(mins * 60)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else NavySurfaceSecondary)
                                    .clickable {
                                        proState.sleepTimerRemainingSec = mins * 60
                                        proState.showSleepDialog = false
                                        Toast.makeText(
                                            context,
                                            if (mins == 0) "Sleep Timer Off" else "Sleep Timer set: $mins min",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else NavyTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
"""

V4_PLAYER_CONTROLS_KT = """package com.mayank.darkplayer.feature.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import com.mayank.darkplayer.core.model.ControlButtonsPosition
import com.mayank.darkplayer.core.model.PlayerPreferences
import com.mayank.darkplayer.feature.player.extensions.nameRes
import com.mayank.darkplayer.feature.player.model.DecoderMode
import com.mayank.darkplayer.feature.player.state.ChaptersState
import com.mayank.darkplayer.feature.player.state.ControlsVisibilityState
import com.mayank.darkplayer.feature.player.state.SeekGestureState
import com.mayank.darkplayer.feature.player.state.VideoZoomAndContentScaleState
import com.mayank.darkplayer.feature.player.state.seekAmountFormatted
import com.mayank.darkplayer.feature.player.state.seekToPositionFormated
import com.mayank.darkplayer.feature.player.ui.DarkPlayerProEffects
import com.mayank.darkplayer.feature.player.ui.DarkPlayerSleepTimerDialog
import com.mayank.darkplayer.feature.player.ui.InfoView
import com.mayank.darkplayer.feature.player.ui.OverlayView
import com.mayank.darkplayer.feature.player.ui.ProQuickToolsStrip
import com.mayank.darkplayer.feature.player.ui.rememberDarkPlayerProState
import com.mayank.darkplayer.feature.player.ui.controls.ControlsBottomView
import com.mayank.darkplayer.feature.player.ui.controls.ControlsMiddleView
import com.mayank.darkplayer.feature.player.ui.controls.ControlsTopView

@OptIn(UnstableApi::class)
@Composable
fun PlayerControls(
    player: Player?,
    title: String,
    playerPreferences: PlayerPreferences,
    videoDecoderMode: DecoderMode?,
    controlsVisibilityState: ControlsVisibilityState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    isPipSupported: Boolean,
    onShowOverlay: (OverlayView) -> Unit,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onToggleTimeDisplay: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    modifier: Modifier = Modifier,
    middleControlsModifier: Modifier = Modifier,
) {
    val proState = rememberDarkPlayerProState()
    DarkPlayerProEffects(player = player, proState = proState)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (proState.nightModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x5A120A04)),
            )
        }

        Column {
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 110)),
                exit = fadeOut(animationSpec = tween(durationMillis = 75)),
            ) {
                Column {
                    ControlsTopView(
                        title = title,
                        videoDecoderMode = videoDecoderMode,
                        onDecoderClick = { onShowOverlay(OverlayView.DECODER_SELECTOR) },
                        onAudioClick = { onShowOverlay(OverlayView.AUDIO_SELECTOR) },
                        onSubtitleClick = { onShowOverlay(OverlayView.SUBTITLE_SELECTOR) },
                        onPlaylistClick = { onShowOverlay(OverlayView.PLAYLIST) },
                        onBackClick = onBackClick,
                    )
                    if (!controlsVisibilityState.controlsLocked) {
                        ProQuickToolsStrip(player = player, proState = proState)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(weight = 1f))
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible &&
                    !controlsVisibilityState.controlsLocked,
                enter = fadeIn(animationSpec = tween(durationMillis = 110)),
                exit = fadeOut(animationSpec = tween(durationMillis = 75)),
            ) {
                ControlsBottomView(
                    player = player,
                    progressState = progressState,
                    chaptersState = chaptersState,
                    onChaptersClick = { onShowOverlay(OverlayView.CHAPTERS) },
                    controlsAlignment = when (playerPreferences.controlButtonsPosition) {
                        ControlButtonsPosition.LEFT -> Alignment.Start
                        ControlButtonsPosition.RIGHT -> Alignment.End
                    },
                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                    isPipSupported = isPipSupported,
                    showRemainingTime = playerPreferences.showRemainingTime,
                    onToggleTimeDisplay = onToggleTimeDisplay,
                    onSeek = seekGestureState::onSeek,
                    onSeekEnd = seekGestureState::onSeekEnd,
                    onPlaybackSpeedClick = { onShowOverlay(OverlayView.PLAYBACK_SPEED) },
                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                    onLockControlsClick = {
                        controlsVisibilityState.showControls()
                        controlsVisibilityState.lockControls()
                    },
                    onVideoContentScaleClick = {
                        controlsVisibilityState.showControls()
                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                    },
                    onVideoContentScaleLongClick = {
                        onShowOverlay(OverlayView.VIDEO_CONTENT_SCALE)
                    },
                    onPictureInPictureClick = onPictureInPictureClick,
                )
            }
        }

        // Hide center Play/Pause briefly while Crop/Best Fit/Stretch or Seek/Zoom text is shown
        if (controlsVisibilityState.controlsVisible &&
            seekGestureState.seekAmount == null &&
            !videoZoomAndContentScaleState.isZooming &&
            !videoZoomAndContentScaleState.showContentScaleIndicator
        ) {
            ControlsMiddleView(
                player = player,
                modifier = middleControlsModifier,
            )
        }

        // Center status overlays for Seek, Zoom, and Crop / Best Fit / Stretch / 100%
        when {
            seekGestureState.seekAmount != null -> InfoView(
                info = "${seekGestureState.seekAmountFormatted}\\n[${seekGestureState.seekToPositionFormated}]",
            )

            videoZoomAndContentScaleState.isZooming -> InfoView(
                info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%",
            )

            videoZoomAndContentScaleState.showContentScaleIndicator -> InfoView(
                modifier = Modifier.align(Alignment.Center),
                info = stringResource(videoZoomAndContentScaleState.videoContentScale.nameRes()),
            )
        }

        DarkPlayerSleepTimerDialog(proState = proState)
    }
}
"""

V4_PLAYER_FEEDBACK_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.R as coreUiR
import com.mayank.darkplayer.core.ui.extensions.copy
import com.mayank.darkplayer.core.ui.theme.NavySurface
import com.mayank.darkplayer.core.ui.theme.NavyTextPrimary
import com.mayank.darkplayer.feature.player.extensions.formatted
import com.mayank.darkplayer.feature.player.state.TapGestureState
import com.mayank.darkplayer.feature.player.state.VerticalGesture
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clean floating status pill for Crop / Best Fit / Stretch / Zoom / Seek readouts.
 */
@Composable
internal fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
    ),
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color(0xE6000000),
        shadowElevation = 8.dp,
    ) {
        Text(
            text = info,
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

@Composable
internal fun BoxScope.DpadSeekIndicator(visible: Boolean, offsetMs: Long, positionMs: Long) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        visible = visible,
        enter = fadeIn(tween(100)),
        exit = fadeOut(tween(100)),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xE6000000),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(coreUiR.drawable.ic_fast),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(degrees = if (offsetMs < 0) 180f else 0f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${if (offsetMs >= 0) "+" else "-"}" +
                            "${abs(offsetMs).milliseconds.inWholeSeconds}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = positionMs.milliseconds.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/**
 * Pure solid BLACK (no outline/border) animated 3x Speed Playing pill.
 */
@Composable
internal fun BoxScope.PlayerGestureIndicators(tapGestureState: TapGestureState) {
    DoubleTapIndicator(tapGestureState = tapGestureState)

    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 24.dp)
            .align(Alignment.TopCenter),
        visible = tapGestureState.isLongPressGestureInAction,
        enter = fadeIn(tween(110)) + scaleIn(initialScale = 0.9f, animationSpec = tween(110)),
        exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.9f, animationSpec = tween(90)),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "speed_pill_pulse")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "chevron_wave",
        )
        val speedStr = if (tapGestureState.longPressSpeed % 1f == 0f) {
            "${tapGestureState.longPressSpeed.toInt()}X"
        } else {
            "${tapGestureState.longPressSpeed}X"
        }

        Surface(
            shape = CircleShape,
            color = Color(0xF2000000),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    for (i in 0..2) {
                        val dist = abs(phase - i.toFloat())
                        val chevronAlpha = (1f - (dist * 0.55f)).coerceIn(0.28f, 1f)
                        Icon(
                            painter = painterResource(coreUiR.drawable.ic_fast),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .alpha(chevronAlpha),
                        )
                    }
                }
                Text(
                    text = "$speedStr Speed Playing",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
internal fun BoxScope.PlayerVerticalGestureIndicators(
    activeGesture: VerticalGesture?,
    volumePercentage: Int,
    maxVolumePercentage: Int,
    brightnessPercentage: Int,
    useMaterialYouControls: Boolean,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
            .padding(24.dp),
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterStart),
            visible = activeGesture == VerticalGesture.VOLUME,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(80)),
        ) {
            VerticalProgressView(
                useMaterialYouControls = useMaterialYouControls,
                value = volumePercentage,
                maxValue = maxVolumePercentage,
                icon = painterResource(coreUiR.drawable.ic_volume),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = activeGesture == VerticalGesture.BRIGHTNESS,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(80)),
        ) {
            VerticalProgressView(
                useMaterialYouControls = useMaterialYouControls,
                value = brightnessPercentage,
                icon = painterResource(coreUiR.drawable.ic_brightness),
            )
        }
    }
}
"""

V4_DOUBLE_TAP_INDICATOR_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.model.DoubleTapGesture
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.feature.player.state.TapGestureState
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DoubleTapIndicator(modifier: Modifier = Modifier, tapGestureState: TapGestureState) {
    if (tapGestureState.seekMillis == 0L) return
    val isForward = tapGestureState.seekMillis > 0L
    val pulseScale = remember { Animatable(0.88f) }

    LaunchedEffect(tapGestureState.seekMillis) {
        pulseScale.snapTo(0.88f)
        pulseScale.animateTo(1.06f, animationSpec = tween(120))
        pulseScale.animateTo(1.0f, animationSpec = tween(120))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(
                    fraction = when (tapGestureState.doubleTapGesture) {
                        DoubleTapGesture.PLAY_PAUSE -> 0f
                        DoubleTapGesture.FAST_FORWARD_AND_REWIND -> 0.45f
                        DoubleTapGesture.BOTH -> 0.35f
                        DoubleTapGesture.NONE -> 0f
                    },
                )
                .clip(
                    shape = if (isForward) RightSideOvalShape() else LeftSideOvalShape(),
                )
                .background(Color.Black.copy(alpha = 0.28f))
                .indication(
                    tapGestureState.interactionSource,
                    indication = ripple(),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC000000),
                modifier = Modifier.graphicsLayer {
                    scaleX = pulseScale.value
                    scaleY = pulseScale.value
                },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DoubleTapSeekTriangles(isForward = isForward)
                    Spacer(Modifier.height(4.dp))
                    val secs = abs(tapGestureState.seekMillis.milliseconds.inWholeSeconds)
                    Text(
                        text = "${if (isForward) "+" else "-"}${secs}s",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun DoubleTapSeekTriangles(isForward: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "skip_chevrons")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skip_wave",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy((-2).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0..2) {
            val idx = if (isForward) i else (2 - i)
            val dist = abs(phase - idx.toFloat())
            val alphaVal = (1f - dist * 0.55f).coerceIn(0.25f, 1f)
            Icon(
                painter = painterResource(R.drawable.ic_fast),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (isForward) 0f else 180f)
                    .alpha(alphaVal),
            )
        }
    }
}

private class RightSideOvalShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(size.width, 0f)
            quadraticTo(0f, size.height / 2f, size.width, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

private class LeftSideOvalShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            quadraticTo(size.width, size.height / 2f, 0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
"""

V4_PLAYBACK_SPEED_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mayank.darkplayer.core.common.extensions.toString
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.components.NextSwitch
import com.mayank.darkplayer.feature.player.state.rememberPlaybackParametersState
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaybackSpeedSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val playbackParametersState = rememberPlaybackParametersState(player = player)
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_playback_speed),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val minValue = 0.25f
            val maxValue = 4.0f
            val stepSize = 0.05f
            val steps = ((maxValue - minValue) / stepSize).toInt() - 1

            // Top Speed Readout + - / + / 1x controls (Screenshot 34724.png)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .clickable {
                            val newSpeed = (playbackParametersState.speed - stepSize).coerceAtLeast(minValue)
                            playbackParametersState.setPlaybackSpeed(speed = newSpeed)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = null,
                        tint = onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = playbackParametersState.speed.toString(decimalPlaces = 2),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = onSurface,
                    )
                    Text(
                        text = "x",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .clickable {
                                val newSpeed = (playbackParametersState.speed + stepSize).coerceAtMost(maxValue)
                                playbackParametersState.setPlaybackSpeed(speed = newSpeed)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, borderColor, CircleShape)
                            .clickable { playbackParametersState.setPlaybackSpeed(speed = 1f) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "1x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = muted,
                        )
                    }
                }
            }

            // Slider card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg.copy(alpha = 0.7f))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Slider(
                    value = playbackParametersState.speed.coerceIn(minValue, maxValue),
                    valueRange = minValue..maxValue,
                    steps = steps,
                    onValueChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playbackParametersState.setPlaybackSpeed(speed = it)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = borderColor,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0.25x", fontSize = 11.sp, color = muted)
                    Text("1.0x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurface)
                    Text("2.0x", fontSize = 11.sp, color = muted)
                    Text("4.0x", fontSize = 11.sp, color = muted)
                }
            }

            // Preset pills (0.25x .. 4.0x)
            FlowRow(
                maxItemsInEachRow = 5,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    0.25f, 0.5f, 0.75f, 1.0f, 1.25f,
                    1.5f, 1.75f, 2.0f, 2.5f, 3.0f,
                    3.5f, 4.0f,
                ).forEach { speed ->
                    val isSelected = abs(playbackParametersState.speed - speed) < 0.03f
                    Box(
                        modifier = Modifier
                            .clip(shape = CircleShape)
                            .background(
                                if (isSelected) accent.copy(alpha = 0.20f) else cardBg.copy(alpha = 0.65f),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accent else borderColor,
                                shape = CircleShape,
                            )
                            .clickable { playbackParametersState.setPlaybackSpeed(speed = speed) }
                            .padding(horizontal = 8.dp, vertical = 9.dp)
                            .weight(weight = 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            ),
                            color = if (isSelected) accent else onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Skip silence card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(16.dp))
                    .background(cardBg.copy(alpha = 0.7f))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .toggleable(
                        value = playbackParametersState.skipSilenceEnabled,
                        onValueChange = {
                            playbackParametersState.setIsSkipSilenceEnabled(enabled = it)
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.skip_silence),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = onSurface,
                    modifier = Modifier.weight(weight = 1f),
                )
                NextSwitch(
                    checked = playbackParametersState.skipSilenceEnabled,
                    onCheckedChange = null,
                )
            }
        }
    }
}
"""


def patch_v4_upgrades(project_root: Path) -> None:
    """Apply V4 player gesture, 3x default speed, Liquid Glass circular FAB,
    YTXTube settings dialogs, MX QuickSettings, and PlaybackSpeed upgrades."""

    # A) Default long-press speed -> 3.0f
    player_prefs = project_root / "core/model/src/main/java" / NEW_PACKAGE_PATH / "core/model/PlayerPreferences.kt"
    if player_prefs.exists():
        t = player_prefs.read_text(encoding="utf-8", errors="ignore")
        t = re.sub(r'val\s+longPressControlsSpeed\s*:\s*Float\s*=\s*[0-9.]+f', 'val longPressControlsSpeed: Float = 3.0f', t)
        player_prefs.write_text(t, encoding="utf-8")

    tap_state = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/state/TapGestureState.kt"
    if tap_state.exists():
        t = tap_state.read_text(encoding="utf-8", errors="ignore")
        t = re.sub(r'val\s+longPressSpeed\s*:\s*Float\s*=\s*[0-9.]+f', 'val longPressSpeed: Float = 3.0f', t)
        tap_state.write_text(t, encoding="utf-8")

    # B) Instant hide player controls when Volume/Brightness vertical drag starts
    gestures = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/PlayerGestures.kt"
    if gestures.exists():
        t = gestures.read_text(encoding="utf-8", errors="ignore")
        old_drag = """                        onDragStart = {
                            volumeAndBrightnessGestureState.onDragStart(
                                offset = it,
                                size = size,
                            )
                        },"""
        new_drag = """                        onDragStart = {
                            controlsVisibilityState.hideControls()
                            volumeAndBrightnessGestureState.onDragStart(
                                offset = it,
                                size = size,
                            )
                        },"""
        if old_drag in t:
            t = t.replace(old_drag, new_drag, 1)
            gestures.write_text(t, encoding="utf-8")

    # C) Longer Crop/Fit/Stretch indicator duration (1400ms)
    zoom_state = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/state/VideoZoomAndContentScaleState.kt"
    if zoom_state.exists():
        t = zoom_state.read_text(encoding="utf-8", errors="ignore")
        t = t.replace("CONTENT_SCALE_INDICATOR_DURATION_MS = 1000L", "CONTENT_SCALE_INDICATOR_DURATION_MS = 1400L")
        zoom_state.write_text(t, encoding="utf-8")

    # D) Ensure HdrSupportManager.init(context) runs in ControlsTopView
    controls_top = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/controls/ControlsTopView.kt"
    if controls_top.exists():
        t = controls_top.read_text(encoding="utf-8", errors="ignore")
        if "LaunchedEffect(context)" not in t and "val hdrMode by HdrSupportManager.mode.collectAsState()" in t:
            if "import androidx.compose.runtime.LaunchedEffect" not in t:
                t = t.replace(
                    "import androidx.compose.runtime.Composable",
                    "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.LaunchedEffect",
                    1,
                )
            t = t.replace(
                "val hdrMode by HdrSupportManager.mode.collectAsState()",
                "LaunchedEffect(context) { HdrSupportManager.init(context); HdrSupportManager.applyToWindow(context) }\n    val hdrMode by HdrSupportManager.mode.collectAsState()",
                1,
            )
            controls_top.write_text(t, encoding="utf-8")

    # E) Liquid Glass Circular Play FAB in TopLevelNavigation.kt
    top_nav = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "navigation/TopLevelNavigation.kt"
    if top_nav.exists():
        t = top_nav.read_text(encoding="utf-8", errors="ignore")
        old_fab = """            fabState?.let { fab ->
                FloatingActionButton(
                    onClick = fab.onClick,
                    modifier = Modifier
                        .testTag(tag = "top_level_fab")
                        .focusRequester(focusRequester = fabFocusRequester)
                        .tvFocusRing(shape = MaterialTheme.shapes.large)
                        .focusProperties {
                            if (isTv) {
                                up =
                                    fab.upFocusRequester.takeUnless { it == FocusRequester.Default }
                                        ?: contentFocusRequester
                            }
                        },
                    shape = MaterialTheme.shapes.large,
                ) {
                    AnimatedContent(
                        targetState = fab,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = FastOutSlowInEasing,
                                ),
                            ) +
                                scaleIn(
                                    animationSpec = tween(
                                        durationMillis = 250,
                                        easing = FastOutSlowInEasing,
                                    ),
                                    initialScale = 0.25f,
                                ) togetherWith
                                fadeOut(
                                    animationSpec = tween(
                                        durationMillis = 250,
                                        easing = FastOutSlowInEasing,
                                    ),
                                ) +
                                scaleOut(
                                    animationSpec = tween(
                                        durationMillis = 250,
                                        easing = FastOutSlowInEasing,
                                    ),
                                    targetScale = 0.25f,
                                )
                        },
                        label = "fab_icon_swap",
                    ) { fab ->
                        Icon(
                            imageVector = fab.icon,
                            contentDescription = null,
                        )
                    }
                }
            }"""
        new_fab = """            fabState?.let { fab ->
                LiquidGlassCircleFab(
                    icon = fab.icon,
                    onClick = fab.onClick,
                    modifier = Modifier
                        .testTag(tag = "top_level_fab")
                        .focusRequester(focusRequester = fabFocusRequester)
                        .tvFocusRing(shape = CircleShape)
                        .focusProperties {
                            if (isTv) {
                                up =
                                    fab.upFocusRequester.takeUnless { it == FocusRequester.Default }
                                        ?: contentFocusRequester
                            }
                        },
                )
            }"""
        if old_fab in t:
            t = t.replace(old_fab, new_fab, 1)
            top_nav.write_text(t, encoding="utf-8")

    # F) Append LiquidGlassCircleFab & Liquid Glass toggle check to LiquidGlassNavBar.kt
    lg_nav = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "navigation/LiquidGlassNavBar.kt"
    if lg_nav.exists():
        t = lg_nav.read_text(encoding="utf-8", errors="ignore")
        if "import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs" not in t:
            t = t.replace(
                "import com.mayank.darkplayer.core.ui.theme.DarkPlayerFontFamily",
                "import com.mayank.darkplayer.core.ui.theme.DarkPlayerFontFamily\nimport com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs\nimport androidx.compose.foundation.shape.CircleShape\nimport androidx.compose.ui.graphics.vector.ImageVector",
                1,
            )
        t = t.replace(
            "val backdrop = LocalNavGlassBackdrop.current",
            "val backdrop = if (DarkPlayerUiPrefs.liquidGlassEnabled) LocalNavGlassBackdrop.current else null",
        )
        if "fun LiquidGlassCircleFab(" not in t:
            t += """

@Composable
fun LiquidGlassCircleFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backdrop = if (DarkPlayerUiPrefs.liquidGlassEnabled) LocalNavGlassBackdrop.current else null
    val view = LocalView.current
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val iconTint = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .size(64.dp)
            .then(
                if (backdrop != null && NavGlassSupport.supported) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(22f.dp.toPx(), 22f.dp.toPx())
                        },
                        highlight = { Highlight.Ambient },
                        shadow = { Shadow(radius = 18.dp, color = Color.Black.copy(alpha = 0.40f)) },
                        innerShadow = { InnerShadow(radius = 8.dp, color = Color.White.copy(alpha = 0.08f)) },
                        onDrawSurface = {
                            drawRect(Color(0x9916161A))
                        },
                    )
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(surfaceColor)
                        .border(1.dp, borderColor, CircleShape)
                },
            )
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClick = {
                    view.navHaptic(12L, 140)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (backdrop != null && NavGlassSupport.supported) Color(0xFFF2F2F5) else iconTint,
            modifier = Modifier.size(26.dp),
        )
    }
}
"""
        lg_nav.write_text(t, encoding="utf-8")

    # G) Write DarkPlayerSettingsDialogs.kt (Appearance & Theme, Language, Storage Usage)
    dialogs_file = project_root / "core/ui/src/main/java" / NEW_PACKAGE_PATH / "core/ui/theme/DarkPlayerSettingsDialogs.kt"
    dialogs_file.write_text(V4_SETTINGS_DIALOGS_KT, encoding="utf-8")

    # H) Write updated Theme.kt (reactive ThemeMode + 6 high-contrast Dark Player Color Palettes)
    theme_file = project_root / "core/ui/src/main/java" / NEW_PACKAGE_PATH / "core/ui/theme/Theme.kt"
    theme_file.write_text(V4_THEME_KT, encoding="utf-8")

    # I) Write updated SettingsScreen.kt (with Appearance & Theme, Language, Storage Usage cards + 3x speed)
    settings_file = project_root / "feature/settings/src/main/java" / NEW_PACKAGE_PATH / "settings/SettingsScreen.kt"
    if settings_file.exists():
        st = settings_file.read_text(encoding="utf-8", errors="ignore")
        # Inject imports
        if "DarkPlayerAppearanceDialog" not in st:
            st = st.replace(
                "import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateState",
                "import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateState\n"
                "import androidx.compose.runtime.getValue\n"
                "import androidx.compose.runtime.mutableStateOf\n"
                "import androidx.compose.runtime.setValue\n"
                "import androidx.compose.material.icons.rounded.Palette\n"
                "import androidx.compose.material.icons.rounded.Translate\n"
                "import androidx.compose.material.icons.rounded.Storage\n"
                "import com.mayank.darkplayer.core.ui.theme.DarkPlayerAppearanceDialog\n"
                "import com.mayank.darkplayer.core.ui.theme.DarkPlayerLanguageDialog\n"
                "import com.mayank.darkplayer.core.ui.theme.DarkPlayerStorageDialog\n"
                "import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs",
                1,
            )
            st = st.replace(
                "val focusState = rememberRestorableFocusState()",
                "val focusState = rememberRestorableFocusState()\n"
                "    DarkPlayerUiPrefs.ensureLoaded(context)\n"
                "    var showAppearanceDialog by remember { mutableStateOf(false) }\n"
                "    var showLanguageDialog by remember { mutableStateOf(false) }\n"
                "    var showStorageDialog by remember { mutableStateOf(false) }",
                1,
            )
            st = st.replace(
                'SettingsSectionHeader(title = "Interface & Playback")',
                """SettingsSectionHeader(title = "Dark Player Studio")
                SettingsCardGroup {
                    DarkSettingCardRow(
                        title = "Appearance & Theme",
                        description = "${DarkPlayerUiPrefs.themeMode.label} • ${DarkPlayerUiPrefs.palette.label} • Liquid Glass ${if (DarkPlayerUiPrefs.liquidGlassEnabled) "ON" else "OFF"}",
                        icon = Icons.Rounded.Palette,
                        onClick = { showAppearanceDialog = true },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    DarkSettingCardRow(
                        title = "Language",
                        description = "Choose application language & translation",
                        icon = Icons.Rounded.Translate,
                        onClick = { showLanguageDialog = true },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    DarkSettingCardRow(
                        title = "Storage Usage",
                        description = "See device storage & clear Dark Player cache",
                        icon = Icons.Rounded.Storage,
                        onClick = { showStorageDialog = true },
                    )
                }

                SettingsSectionHeader(title = "Interface & Playback")""",
                1,
            )
            st = st.replace("Hold for 2.0x Speed", "Hold for 3.0x Speed")
            st = st.replace("pitch-locked 2x playback", "pitch-locked 3x playback")
            st = st.replace("containerColor = NavyBackground", "containerColor = MaterialTheme.colorScheme.background")
            st = st.replace(
                "DarkPlayerUpdateDialog(state = updateState)",
                "DarkPlayerUpdateDialog(state = updateState)\n"
                "        if (showAppearanceDialog) DarkPlayerAppearanceDialog(onDismiss = { showAppearanceDialog = false })\n"
                "        if (showLanguageDialog) DarkPlayerLanguageDialog(onDismiss = { showLanguageDialog = false })\n"
                "        if (showStorageDialog) DarkPlayerStorageDialog(onDismiss = { showStorageDialog = false })",
                1,
            )
            settings_file.write_text(st, encoding="utf-8")

    # J) Write redesigned MX-style QuickSettingsDialog.kt
    qs_file = project_root / "feature/videopicker/src/main/java" / NEW_PACKAGE_PATH / "feature/videopicker/composables/QuickSettingsDialog.kt"
    qs_file.write_text(V4_QUICK_SETTINGS_DIALOG_KT, encoding="utf-8")

    # K) Write updated DarkPlayerProTools.kt (Circular buttons, NO Equalizer)
    pro_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DarkPlayerProTools.kt"
    pro_file.write_text(V4_PRO_TOOLS_KT, encoding="utf-8")

    # L) Write updated PlayerControls.kt (Super fast 110ms/75ms fade, Center Crop/Fit text, NO Equalizer)
    pc_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/PlayerControls.kt"
    pc_file.write_text(V4_PLAYER_CONTROLS_KT, encoding="utf-8")

    # M) Write updated PlayerFeedback.kt (Pure Black NO-outline animated 3x Speed badge)
    pf_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/PlayerFeedback.kt"
    pf_file.write_text(V4_PLAYER_FEEDBACK_KT, encoding="utf-8")

    # N) Write updated DoubleTapIndicator.kt (Sleek 10s animated triple-chevron pill)
    dt_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DoubleTapIndicator.kt"
    dt_file.write_text(V4_DOUBLE_TAP_INDICATOR_KT, encoding="utf-8")

    # O) Write redesigned PlaybackSpeedSelectorView.kt (Screenshot 34724.png)
    ps_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/PlaybackSpeedSelectorView.kt"
    ps_file.write_text(V4_PLAYBACK_SPEED_KT, encoding="utf-8")

    # P) Ensure material-icons-extended in feature/videopicker/build.gradle.kts
    vp_gradle = project_root / "feature/videopicker/build.gradle.kts"
    if vp_gradle.exists():
        vt = vp_gradle.read_text(encoding="utf-8")
        if "material.iconsExtended" not in vt:
            vt = vt.replace(
                "implementation(libs.androidx.compose.material3)",
                "implementation(libs.androidx.compose.material3)\n    implementation(libs.androidx.compose.material.iconsExtended)",
                1,
            )
            vp_gradle.write_text(vt, encoding="utf-8")



V5_PLAYER_FEEDBACK_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.R as coreUiR
import com.mayank.darkplayer.core.ui.extensions.copy
import com.mayank.darkplayer.feature.player.extensions.formatted
import com.mayank.darkplayer.feature.player.state.TapGestureState
import com.mayank.darkplayer.feature.player.state.VerticalGesture
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clean floating text overlay for Crop / Best Fit / Stretch / Zoom / Seek readouts.
 */
@Composable
internal fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.ExtraBold,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.85f),
            offset = Offset(0f, 2f),
            blurRadius = 10f,
        ),
    ),
) {
    Text(
        text = info,
        style = textStyle,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
internal fun BoxScope.DpadSeekIndicator(visible: Boolean, offsetMs: Long, positionMs: Long) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        visible = visible,
        enter = fadeIn(tween(100)),
        exit = fadeOut(tween(100)),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xCC000000),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(degrees = if (offsetMs < 0) 180f else 0f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${if (offsetMs >= 0) "+" else "-"}" +
                            "${abs(offsetMs).milliseconds.inWholeSeconds}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = positionMs.milliseconds.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/**
 * Clean transparent-background 3X Speed Playing indicator with 3 smooth single chevrons.
 */
@Composable
internal fun BoxScope.PlayerGestureIndicators(tapGestureState: TapGestureState) {
    DoubleTapIndicator(tapGestureState = tapGestureState)

    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 28.dp)
            .align(Alignment.TopCenter),
        visible = tapGestureState.isLongPressGestureInAction,
        enter = fadeIn(tween(100)) + scaleIn(initialScale = 0.92f, animationSpec = tween(100)),
        exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.92f, animationSpec = tween(90)),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "speed_pill_pulse")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 620, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "chevron_wave",
        )
        val speedStr = if (tapGestureState.longPressSpeed % 1f == 0f) {
            "${tapGestureState.longPressSpeed.toInt()}X"
        } else {
            "${tapGestureState.longPressSpeed}X"
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                for (i in 0..2) {
                    val dist = abs(phase - i.toFloat())
                    val chevronAlpha = (1f - (dist * 0.55f)).coerceIn(0.25f, 1f)
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(chevronAlpha),
                    )
                }
            }
            Text(
                text = "$speedStr Speed",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.90f),
                        offset = Offset(0f, 2f),
                        blurRadius = 10f,
                    ),
                ),
                color = Color.White,
            )
        }
    }
}

@Composable
internal fun BoxScope.PlayerVerticalGestureIndicators(
    activeGesture: VerticalGesture?,
    volumePercentage: Int,
    maxVolumePercentage: Int,
    brightnessPercentage: Int,
    useMaterialYouControls: Boolean,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
            .padding(24.dp),
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterStart),
            visible = activeGesture == VerticalGesture.VOLUME,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(80)),
        ) {
            VerticalProgressView(
                useMaterialYouControls = useMaterialYouControls,
                value = volumePercentage,
                maxValue = maxVolumePercentage,
                icon = painterResource(coreUiR.drawable.ic_volume),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = activeGesture == VerticalGesture.BRIGHTNESS,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(80)),
        ) {
            VerticalProgressView(
                useMaterialYouControls = useMaterialYouControls,
                value = brightnessPercentage,
                icon = painterResource(coreUiR.drawable.ic_brightness),
            )
        }
    }
}
"""

V5_DOUBLE_TAP_INDICATOR_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.feature.player.state.TapGestureState
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clean, transparent-background 10s skip indicator with zero oval clipping.
 */
@Composable
fun DoubleTapIndicator(modifier: Modifier = Modifier, tapGestureState: TapGestureState) {
    if (tapGestureState.seekMillis == 0L) return
    val isForward = tapGestureState.seekMillis > 0L
    val pulseScale = remember { Animatable(0.88f) }

    LaunchedEffect(tapGestureState.seekMillis) {
        pulseScale.snapTo(0.88f)
        pulseScale.animateTo(1.08f, animationSpec = tween(110))
        pulseScale.animateTo(1.0f, animationSpec = tween(110))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = 0.40f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DoubleTapSeekTriangles(isForward = isForward)
                Spacer(Modifier.height(4.dp))
                val secs = abs(tapGestureState.seekMillis.milliseconds.inWholeSeconds)
                Text(
                    text = "${if (isForward) "+" else "-"}${secs}s",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.90f),
                            offset = Offset(0f, 2f),
                            blurRadius = 10f,
                        ),
                    ),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DoubleTapSeekTriangles(isForward: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "skip_chevrons")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skip_wave",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0..2) {
            val idx = if (isForward) i else (2 - i)
            val dist = abs(phase - idx.toFloat())
            val alphaVal = (1f - dist * 0.55f).coerceIn(0.25f, 1f)
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(if (isForward) 0f else 180f)
                    .alpha(alphaVal),
            )
        }
    }
}
"""

V5_ABOUT_SCREEN_KT = """package com.mayank.darkplayer.settings.screens.about

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mayank.darkplayer.core.common.extensions.appIcon
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.components.NextTopAppBar
import com.mayank.darkplayer.core.ui.components.rememberTvListFocusRequester
import com.mayank.darkplayer.core.ui.components.tvFocusDown
import com.mayank.darkplayer.core.ui.components.tvListFocus
import com.mayank.darkplayer.core.ui.designsystem.NextIcons
import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateDialog
import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateState
import kotlinx.coroutines.launch

private const val GITHUB_URL = "https://github.com/xboyrex/DarkPlayer"

@Composable
fun AboutPreferencesScreen(viewModel: AboutPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
private fun AboutPreferencesScreenContent(
    state: AboutPreferencesUiState,
    onAction: (AboutPreferencesAction) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val updateState = remember(scope, state.appVersion) {
        DarkPlayerUpdateState(scope = scope, currentVersionName = state.appVersion)
    }
    val listFocusRequester = rememberTvListFocusRequester()

    val accent = MaterialTheme.colorScheme.primary
    val surfaceCard = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                NextTopAppBar(
                    title = stringResource(id = R.string.about_name),
                    navigationIcon = {
                        FilledTonalIconButton(
                            onClick = { onAction(AboutPreferencesAction.NavigateUp) },
                            modifier = Modifier.tvFocusDown(target = listFocusRequester),
                        ) {
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigate_up),
                            )
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .tvListFocus(focusRequester = listFocusRequester)
                    .padding(paddingValues = innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Hero Brand Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(surfaceCard)
                        .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                        .padding(vertical = 26.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .border(2.dp, accent.copy(alpha = 0.65f), CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val appIcon = remember(context) { context.appIcon()?.asImageBitmap() }
                        appIcon?.let {
                            Image(
                                bitmap = it,
                                contentDescription = "Dark Player Logo",
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Dark Player",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f))
                            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "v${state.appVersion}  •  Release Build",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Crafted by Mayank (xboyrex)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("Media3 Engine", "FFmpeg Decoders", "HDR10 / SDR", "Liquid Glass").forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // Actions Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(surfaceCard)
                        .border(1.dp, borderColor, RoundedCornerShape(22.dp)),
                ) {
                    AboutActionRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "Check for Updates",
                        subtitle = "Fetch latest Dark Player release from GitHub",
                        onClick = { updateState.check(manual = true) },
                    )
                    HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
                    AboutActionRow(
                        icon = Icons.Rounded.Code,
                        title = "GitHub Repository",
                        subtitle = "github.com/xboyrex/DarkPlayer",
                        onClick = { uriHandler.openUriOrShowToast(uri = GITHUB_URL, context = context) },
                    )
                    HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
                    AboutActionRow(
                        icon = Icons.Rounded.Description,
                        title = stringResource(R.string.libraries),
                        subtitle = "View open-source licenses & attributions",
                        onClick = { onAction(AboutPreferencesAction.OpenLibraries) },
                    )
                    HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
                    AboutActionRow(
                        icon = Icons.Rounded.ContentCopy,
                        title = "Copy Build & Device Info",
                        subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${Build.MODEL}",
                        onClick = {
                            val info = "Dark Player v${state.appVersion} | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL}"
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("DarkPlayerInfo", info)))
                                Toast.makeText(context, "Build info copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            }
        }

        DarkPlayerUpdateDialog(state = updateState)
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

internal fun UriHandler.openUriOrShowToast(uri: String, context: Context) {
    try {
        openUri(uri = uri)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.error_opening_link),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
"""

def patch_v5_fixes(project_root: Path) -> None:
    """Apply V5 fixes: perfect circle Liquid Glass FAB, transparent 10s & 3x indicators,
    Crop/Fit remember-key bugfix, Audio/Subtitle selector icons, More tab Studio cards,
    professional About screen, and Play Protect cleanups."""

    # 1. Fix VideoZoomAndContentScaleState remember key bug (so Best Fit/Crop/Stretch text ALWAYS shows!)
    zoom_state = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/state/VideoZoomAndContentScaleState.kt"
    if zoom_state.exists():
        t = zoom_state.read_text(encoding="utf-8", errors="ignore")
        old_rem = """    val videoZoomAndContentScaleState = remember(
        player,
        initialContentScale,
        enableZoomGesture,
        enablePanGesture,
    )"""
        new_rem = """    val videoZoomAndContentScaleState = remember(
        player,
        enableZoomGesture,
        enablePanGesture,
    )"""
        if old_rem in t:
            t = t.replace(old_rem, new_rem, 1)
            zoom_state.write_text(t, encoding="utf-8")

    # 2. Fix squished circle FAB: give LiquidGlassNavigationBar Modifier.weight(1f) & FAB requiredSize(64.dp)
    top_nav = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "navigation/TopLevelNavigation.kt"
    if top_nav.exists():
        t = top_nav.read_text(encoding="utf-8", errors="ignore")
        old_nav_call = """                LiquidGlassNavigationBar(
                    destinations = state.destinations,
                    isSelected = { it.route == state.topLevelRoute },
                    onDestinationClick = { state.switchTo(it.route) },
                )"""
        new_nav_call = """                LiquidGlassNavigationBar(
                    destinations = state.destinations,
                    isSelected = { it.route == state.topLevelRoute },
                    onDestinationClick = { state.switchTo(it.route) },
                    modifier = Modifier.weight(1f),
                )"""
        if old_nav_call in t:
            t = t.replace(old_nav_call, new_nav_call, 1)
            top_nav.write_text(t, encoding="utf-8")

    lg_nav = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "navigation/LiquidGlassNavBar.kt"
    if lg_nav.exists():
        t = lg_nav.read_text(encoding="utf-8", errors="ignore")
        if "import androidx.compose.foundation.layout.requiredSize" not in t:
            t = t.replace(
                "import androidx.compose.foundation.layout.size",
                "import androidx.compose.foundation.layout.requiredSize\nimport androidx.compose.foundation.layout.size",
                1,
            )
        t = t.replace(
            ".size(64.dp)",
            ".requiredSize(64.dp)",
        )
        lg_nav.write_text(t, encoding="utf-8")

    # 3. Write V5 transparent 3x speed & 10s skip indicators
    pf_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/PlayerFeedback.kt"
    pf_file.write_text(V5_PLAYER_FEEDBACK_KT, encoding="utf-8")

    dt_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DoubleTapIndicator.kt"
    dt_file.write_text(V5_DOUBLE_TAP_INDICATOR_KT, encoding="utf-8")

    # 4. Write redesigned professional AboutPreferencesScreen.kt
    about_file = project_root / "feature/settings/src/main/java" / NEW_PACKAGE_PATH / "settings/screens/about/AboutPreferencesScreen.kt"
    about_file.write_text(V5_ABOUT_SCREEN_KT, encoding="utf-8")

    # 5. Add crisp SVG/vector icons to AudioTrackSelectorView.kt & SubtitleSelectorView.kt
    audio_sel = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/AudioTrackSelectorView.kt"
    if audio_sel.exists():
        t = audio_sel.read_text(encoding="utf-8", errors="ignore")
        if "painterResource(R.drawable.ic_audio_track)" not in t:
            t = t.replace(
                "import androidx.compose.material3.Text",
                "import androidx.compose.material3.Icon\nimport androidx.compose.material3.Text\nimport androidx.compose.ui.res.painterResource",
                1,
            )
            t = t.replace(
                ") { Text(text = stringResource(R.string.open_audio)) }",
                ") {\n                Icon(\n                    painter = painterResource(R.drawable.ic_audio_track),\n                    contentDescription = null,\n                    modifier = Modifier.size(18.dp),\n                )\n                Spacer(modifier = Modifier.size(8.dp))\n                Text(text = stringResource(R.string.open_audio))\n            }",
                1,
            )
            audio_sel.write_text(t, encoding="utf-8")

    sub_sel = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/SubtitleSelectorView.kt"
    if sub_sel.exists():
        t = sub_sel.read_text(encoding="utf-8", errors="ignore")
        if "painterResource(R.drawable.ic_subtitle_track)" not in t:
            t = t.replace(
                ") { Text(text = stringResource(R.string.open_subtitle)) }",
                ") {\n                Icon(\n                    painter = painterResource(R.drawable.ic_subtitle_track),\n                    contentDescription = null,\n                    modifier = Modifier.size(18.dp),\n                )\n                Spacer(modifier = Modifier.size(8.dp))\n                Text(text = stringResource(R.string.open_subtitle))\n            }",
                1,
            )
            sub_sel.write_text(t, encoding="utf-8")

    # 6. Upgrade MoreScreen.kt with Dark Player Studio quick-access cards (Appearance, Language, Storage, Settings)
    more_file = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "feature/more/screens/more/MoreScreen.kt"
    if more_file.exists():
        mt = more_file.read_text(encoding="utf-8", errors="ignore")
        if "DarkPlayerAppearanceDialog" not in mt:
            mt = mt.replace(
                "import androidx.compose.runtime.remember",
                "import androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\nimport androidx.compose.foundation.border\nimport androidx.compose.foundation.clickable\nimport androidx.compose.foundation.shape.CircleShape\nimport androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight\nimport androidx.compose.material.icons.rounded.Palette\nimport androidx.compose.material.icons.rounded.Settings\nimport androidx.compose.material.icons.rounded.Storage\nimport androidx.compose.material.icons.rounded.Translate\nimport androidx.compose.material3.HorizontalDivider\nimport androidx.compose.ui.graphics.vector.ImageVector\nimport com.mayank.darkplayer.core.ui.theme.DarkPlayerAppearanceDialog\nimport com.mayank.darkplayer.core.ui.theme.DarkPlayerLanguageDialog\nimport com.mayank.darkplayer.core.ui.theme.DarkPlayerStorageDialog\nimport com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs",
                1,
            )
            mt = mt.replace(
                "    val filePicker =",
                "    val context = LocalContext.current\n    DarkPlayerUiPrefs.ensureLoaded(context)\n    var showAppearanceDialog by remember { mutableStateOf(false) }\n    var showLanguageDialog by remember { mutableStateOf(false) }\n    var showStorageDialog by remember { mutableStateOf(false) }\n    val filePicker =",
                1,
            )
            mt = mt.replace(
                """                HistorySection(
                    history = state.history,
                    preferences = state.preferences,
                    onMoreClick = { onAction(MoreAction.OpenHistory) },
                    onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
                )""",
                """                HistorySection(
                    history = state.history,
                    preferences = state.preferences,
                    onMoreClick = { onAction(MoreAction.OpenHistory) },
                    onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
                )
                MoreStudioCard(
                    onOpenAppearance = { showAppearanceDialog = true },
                    onOpenLanguage = { showLanguageDialog = true },
                    onOpenStorage = { showStorageDialog = true },
                    onOpenSettings = { onAction(MoreAction.OpenSettings) },
                )""",
                1,
            )
            mt = mt.replace(
                """        }
    }
}

@Composable
private fun HistorySection(""",
                """        }
        if (showAppearanceDialog) DarkPlayerAppearanceDialog(onDismiss = { showAppearanceDialog = false })
        if (showLanguageDialog) DarkPlayerLanguageDialog(onDismiss = { showLanguageDialog = false })
        if (showStorageDialog) DarkPlayerStorageDialog(onDismiss = { showStorageDialog = false })
    }
}

@Composable
private fun MoreStudioCard(
    onOpenAppearance: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Dark Player Studio",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(22.dp)),
        ) {
            MoreStudioRow(
                icon = Icons.Rounded.Palette,
                title = "Appearance & Theme",
                subtitle = "${DarkPlayerUiPrefs.themeMode.label} • ${DarkPlayerUiPrefs.palette.label}",
                onClick = onOpenAppearance,
            )
            HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
            MoreStudioRow(
                icon = Icons.Rounded.Translate,
                title = "Language",
                subtitle = "Change app language & translation",
                onClick = onOpenLanguage,
            )
            HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
            MoreStudioRow(
                icon = Icons.Rounded.Storage,
                title = "Storage Usage",
                subtitle = "Device storage & clear Dark Player cache",
                onClick = onOpenStorage,
            )
            HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
            MoreStudioRow(
                icon = Icons.Rounded.Settings,
                title = "All Settings",
                subtitle = "Player, gestures, decoders, subtitles & about",
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun MoreStudioRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HistorySection(""",
                1,
            )
            more_file.write_text(mt, encoding="utf-8")

    # 7. Ensure material-icons-extended is present in app and feature/player Gradle files
    for mod in ["app", "feature/player"]:
        gf = project_root / mod / "build.gradle.kts"
        if gf.exists():
            gt = gf.read_text(encoding="utf-8")
            if "material.iconsExtended" not in gt:
                gt = gt.replace(
                    "implementation(libs.androidx.compose.material3)",
                    "implementation(libs.androidx.compose.material3)\n    implementation(libs.androidx.compose.material.iconsExtended)",
                    1,
                )
                gf.write_text(gt, encoding="utf-8")



V6_PLAYER_LOCKED_CONTROLS_KT = """package com.mayank.darkplayer.feature.player

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.feature.player.buttons.PlayerButton

@Composable
fun PlayerLockedControls(
    onUnlockControls: () -> Unit,
    modifier: Modifier = Modifier,
    unlockButtonModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(start = 16.dp, top = 24.dp),
    ) {
        PlayerButton(
            modifier = unlockButtonModifier.border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = CircleShape,
            ),
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = onUnlockControls,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.controls_unlock),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
"""

V6_OVERLAY_VIEW_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.common.extensions.isTelevision
import com.mayank.darkplayer.core.ui.components.requestFocusUntilLanded

@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val layoutDirection = LocalLayoutDirection.current
    val endPadding = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateEndPadding(layoutDirection = layoutDirection)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = show) {
        if (show && isTv) {
            focusRequester.requestFocusUntilLanded(attempts = 5)
        }
    }

    val shape = if (configuration.isPortrait) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    } else {
        RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
    }

    AnimatedVisibility(
        modifier = Modifier.align(
            alignment = if (configuration.isPortrait) {
                Alignment.BottomCenter
            } else {
                Alignment.CenterEnd
            },
        ),
        visible = show,
        enter = if (configuration.isPortrait) {
            slideInVertically(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 2 },
            ) + fadeIn(tween(150))
        } else {
            slideInHorizontally(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                initialOffsetX = { it / 2 },
            ) + fadeIn(tween(150))
        },
        exit = if (configuration.isPortrait) {
            slideOutVertically(
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 2 },
            ) + fadeOut(tween(120))
        } else {
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                targetOffsetX = { it / 2 },
            ) + fadeOut(tween(120))
        },
    ) {
        Surface(
            shape = shape,
            color = Color(0xD9121418),
            contentColor = Color(0xFFF2F2F5),
            shadowElevation = 16.dp,
            modifier = modifier
                .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                .then(
                    other = if (configuration.isPortrait) {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = 0.50f)
                    } else {
                        Modifier
                            .fillMaxWidth(fraction = 0.46f)
                            .fillMaxHeight()
                    },
                ),
        ) {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester = focusRequester)
                    .focusGroup()
                    .padding(top = 22.dp)
                    .padding(end = endPadding),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF2F2F5),
                )
                Spacer(modifier = Modifier.size(10.dp))
                content()
            }
        }
    }
}
"""

V6_DECODER_SELECTOR_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.feature.player.model.DecoderMode
import com.mayank.darkplayer.feature.player.model.DecoderTrackType
import com.mayank.darkplayer.feature.player.model.descriptionRes
import com.mayank.darkplayer.feature.player.model.labelRes
import com.mayank.darkplayer.feature.player.model.selectableDecoderModes

@Composable
fun BoxScope.DecoderSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    videoMode: DecoderMode?,
    audioMode: DecoderMode?,
    onVideoModeSelected: (DecoderMode) -> Unit,
    onAudioModeSelected: (DecoderMode) -> Unit,
) {
    var selectedTrackType by rememberSaveable { mutableStateOf(DecoderTrackType.VIDEO) }

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_decoders),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DecoderTrackTypeSelector(
                selectedTrackType = selectedTrackType,
                onTrackTypeSelected = { selectedTrackType = it },
            )
            DecoderModeGroup(
                selectedMode = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> videoMode
                    DecoderTrackType.AUDIO -> audioMode
                },
                trackType = selectedTrackType,
                onModeSelected = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> onVideoModeSelected
                    DecoderTrackType.AUDIO -> onAudioModeSelected
                },
            )
        }
    }
}

@Composable
private fun DecoderTrackTypeSelector(
    selectedTrackType: DecoderTrackType,
    onTrackTypeSelected: (DecoderTrackType) -> Unit,
) {
    val trackTypes = DecoderTrackType.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        trackTypes.forEachIndexed { index, trackType ->
            SegmentedButton(
                selected = selectedTrackType == trackType,
                onClick = { onTrackTypeSelected(trackType) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    trackTypes.size,
                ),
                icon = {
                    Icon(
                        imageVector = when (trackType) {
                            DecoderTrackType.VIDEO -> Icons.Rounded.Videocam
                            DecoderTrackType.AUDIO -> Icons.Rounded.Audiotrack
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            ) {
                Text(
                    text = stringResource(
                        id = when (trackType) {
                            DecoderTrackType.VIDEO -> R.string.video
                            DecoderTrackType.AUDIO -> R.string.audio
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun DecoderModeGroup(
    selectedMode: DecoderMode?,
    trackType: DecoderTrackType,
    onModeSelected: (DecoderMode) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        selectableDecoderModes.filter {
            trackType == DecoderTrackType.VIDEO ||
                it != DecoderMode.HW_PLUS
        }.forEach { mode ->
            DecoderModeRow(
                mode = mode,
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
            )
        }
    }
}

@Composable
private fun DecoderModeRow(mode: DecoderMode, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val icon = when (mode) {
        DecoderMode.AUTO -> Icons.Rounded.Tune
        DecoderMode.HARDWARE -> Icons.Rounded.Memory
        DecoderMode.HW_PLUS -> Icons.Rounded.Memory
        DecoderMode.FFMPEG -> Icons.Rounded.DeveloperBoard
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(14.dp))
            .background(
                if (selected) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
            )
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp),
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else Color(0xFFD0D0DA),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(mode.labelRes),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) accent else Color.White,
            )
            Text(
                text = stringResource(mode.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB4B4C0),
            )
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = accent,
                unselectedColor = Color(0xFF8C8C9A),
            ),
        )
    }
}
"""

V6_QUICK_SETTINGS_DIALOG_KT = """package com.mayank.darkplayer.feature.videopicker.composables

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.mayank.darkplayer.core.model.ApplicationPreferences
import com.mayank.darkplayer.core.model.MediaLayoutMode
import com.mayank.darkplayer.core.model.MediaViewMode
import com.mayank.darkplayer.core.model.Sort
import com.mayank.darkplayer.core.ui.R

@Composable
fun QuickSettingsDialog(
    applicationPreferences: ApplicationPreferences,
    onDismiss: () -> Unit,
    updatePreferences: (ApplicationPreferences) -> Unit,
) {
    var preferences by remember { mutableStateOf(value = applicationPreferences) }
    var fieldsExpanded by remember { mutableStateOf(true) }
    var advancedExpanded by remember { mutableStateOf(false) }

    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setDimAmount(0.45f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 48
                    }
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                // Header Row: View Mode | Layout (MX Pro style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(3f)) {
                        Text(
                            text = "View Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            MxOptionTile(
                                label = "All folders",
                                icon = Icons.Rounded.FolderCopy,
                                selected = preferences.mediaViewMode == MediaViewMode.FOLDER_TREE,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDER_TREE) },
                            )
                            MxOptionTile(
                                label = "Files",
                                icon = Icons.Rounded.VideoFile,
                                selected = preferences.mediaViewMode == MediaViewMode.VIDEOS,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.VIDEOS) },
                            )
                            MxOptionTile(
                                label = "Folders",
                                icon = Icons.Rounded.Folder,
                                selected = preferences.mediaViewMode == MediaViewMode.FOLDERS,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDERS) },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 34.dp, start = 6.dp, end = 6.dp)
                            .width(1.dp)
                            .height(48.dp)
                            .background(borderColor),
                    )

                    Column(modifier = Modifier.weight(2f)) {
                        Text(
                            text = "Layout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            MxOptionTile(
                                label = "List",
                                icon = Icons.Rounded.ViewAgenda,
                                selected = preferences.mediaLayoutMode == MediaLayoutMode.LIST,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST) },
                            )
                            MxOptionTile(
                                label = "Grid",
                                icon = Icons.Rounded.GridView,
                                selected = preferences.mediaLayoutMode == MediaLayoutMode.GRID,
                                accent = accent,
                                onClick = { preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.GRID) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(16.dp))

                // Sort section
                Text(
                    text = "Sort",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = onSurface,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val sortItems = listOf(
                        Triple(Sort.By.TITLE, "Title", Icons.Rounded.SortByAlpha),
                        Triple(Sort.By.DATE, "Date", Icons.Rounded.CalendarToday),
                        Triple(Sort.By.LENGTH, "Length", Icons.Rounded.Timer),
                        Triple(Sort.By.SIZE, "Size", Icons.Rounded.SdStorage),
                        Triple(Sort.By.PATH, "Path", Icons.Rounded.LocationOn),
                    )
                    sortItems.forEach { (sortBy, label, icon) ->
                        MxOptionTile(
                            label = label,
                            icon = icon,
                            selected = preferences.sortBy == sortBy,
                            accent = accent,
                            onClick = { preferences = preferences.copy(sortBy = sortBy) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // A to Z / Z to A Segmented Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                ) {
                    val isAsc = preferences.sortOrder == Sort.Order.ASCENDING
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isAsc) accent.copy(alpha = 0.16f) else Color.Transparent)
                            .then(
                                if (isAsc) Modifier.border(1.dp, accent, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                else Modifier,
                            )
                            .clickable { preferences = preferences.copy(sortOrder = Sort.Order.ASCENDING) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = if (isAsc) accent else muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "A to Z",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isAsc) accent else muted,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (!isAsc) accent.copy(alpha = 0.16f) else Color.Transparent)
                            .then(
                                if (!isAsc) Modifier.border(1.dp, accent, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                else Modifier,
                            )
                            .clickable { preferences = preferences.copy(sortOrder = Sort.Order.DESCENDING) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (!isAsc) accent else muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Z to A",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (!isAsc) accent else muted,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = borderColor)

                // Expandable Fields 3-Column Checkbox Grid (Screenshots 34778 & 34779)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fieldsExpanded = !fieldsExpanded }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.fields),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = onSurface,
                    )
                    Icon(
                        imageVector = if (fieldsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = muted,
                    )
                }

                AnimatedVisibility(visible = fieldsExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MxCheckCell(
                                label = "Thumbnail",
                                checked = preferences.showThumbnailField,
                                accent = accent,
                                onAccent = onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { preferences = preferences.copy(showThumbnailField = !preferences.showThumbnailField) },
                            )
                            MxCheckCell(
                                label = "Length",
                                checked = preferences.showDurationField,
                                accent = accent,
                                onAccent = onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { preferences = preferences.copy(showDurationField = !preferences.showDurationField) },
                            )
                            MxCheckCell(
                                label = "Extension",
                                checked = preferences.showExtensionField,
                                accent = accent,
                                onAccent = onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { preferences = preferences.copy(showExtensionField = !preferences.showExtensionField) },
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MxCheckCell(
                                label = "Played",
                                checked = preferences.showPlayedProgress,
                                accent = accent,
                                onAccent = onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { preferences = preferences.copy(showPlayedProgress = !preferences.showPlayedProgress) },
                            )
                            MxCheckCell(
                                label = "Resolution",
                                checked = preferences.showResolutionField,
                                accent = accent,
                                onAccent = onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { preferences = preferences.copy(showResolutionField = !preferences.showResolutionField) },
                            )
                            MxCheckCell(
                                label = "Path",
                                checked = preferences.showPathField,
                                accent = accent,
                                onAccent = onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { preferences = preferences.copy(showPathField = !preferences.showPathField) },
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MxCheckCell(
                                label = "Size",
                                checked = preferences.showSizeField,
                                accent = accent,
                                onAccent = onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { preferences = preferences.copy(showSizeField = !preferences.showSizeField) },
                            )
                            MxCheckCell(
                                label = "Folder len",
                                checked = preferences.showFolderDurationField,
                                accent = accent,
                                onAccent = onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { preferences = preferences.copy(showFolderDurationField = !preferences.showFolderDurationField) },
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                HorizontalDivider(color = borderColor)

                // Expandable Advanced Section (Screenshot 34778)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { advancedExpanded = !advancedExpanded }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = onSurface,
                    )
                    Icon(
                        imageVector = if (advancedExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = muted,
                    )
                }

                AnimatedVisibility(visible = advancedExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MxAdvancedSwitchRow(
                            label = "Display length over thumbnail",
                            checked = preferences.showDurationField,
                            accent = accent,
                            onAccent = onAccent,
                            onCheckedChange = { preferences = preferences.copy(showDurationField = it) },
                        )
                        MxAdvancedSwitchRow(
                            label = "Mark last played media",
                            checked = preferences.markLastPlayedMedia,
                            accent = accent,
                            onAccent = onAccent,
                            onCheckedChange = { preferences = preferences.copy(markLastPlayedMedia = it) },
                        )
                        MxAdvancedSwitchRow(
                            label = "Pause watch history",
                            checked = preferences.isHistoryPaused,
                            accent = accent,
                            onAccent = onAccent,
                            onCheckedChange = { preferences = preferences.copy(isHistoryPaused = it) },
                        )
                    }
                }

                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(10.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = muted,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            updatePreferences(preferences)
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            color = accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MxOptionTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun MxCheckCell(
    label: String,
    checked: Boolean,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) accent else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (checked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = onAccent,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MxAdvancedSwitchRow(
    label: String,
    checked: Boolean,
    accent: Color,
    onAccent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = onAccent,
                checkedTrackColor = accent,
            ),
        )
    }
}
"""

def patch_v6_polish(project_root: Path) -> None:
    """Apply V6 polish: high-contrast lock button, frosted OverlayView + Decoder icons,
    Light Theme fixes (Settings, MainActivity status bars, LiquidGlassNavBar),
    clean HTML-stripped working Updater, and MX Pro QuickSettings checkboxes/advanced."""

    # 1. High-contrast Lock button
    lock_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/PlayerLockedControls.kt"
    lock_file.write_text(V6_PLAYER_LOCKED_CONTROLS_KT, encoding="utf-8")

    # 2. Frosted translucent OverlayView + DecoderSelectorView with icons
    overlay_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/OverlayView.kt"
    overlay_file.write_text(V6_OVERLAY_VIEW_KT, encoding="utf-8")

    decoder_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DecoderSelectorView.kt"
    decoder_file.write_text(V6_DECODER_SELECTOR_KT, encoding="utf-8")

    # 3. MX Pro QuickSettings with 3-column Checkboxes + Advanced + Window Blur
    qs_file = project_root / "feature/videopicker/src/main/java" / NEW_PACKAGE_PATH / "feature/videopicker/composables/QuickSettingsDialog.kt"
    qs_file.write_text(V6_QUICK_SETTINGS_DIALOG_KT, encoding="utf-8")

    # 4. Fix SettingsScreen.kt Light Theme (replace hardcoded Navy colors with MaterialTheme.colorScheme)
    settings_file = project_root / "feature/settings/src/main/java" / NEW_PACKAGE_PATH / "settings/SettingsScreen.kt"
    if settings_file.exists():
        st = settings_file.read_text(encoding="utf-8", errors="ignore")
        replacements = [
            ("NavyBackground", "MaterialTheme.colorScheme.background"),
            ("NavySurfaceSecondary", "MaterialTheme.colorScheme.surfaceContainerHighest"),
            ("NavySurface", "MaterialTheme.colorScheme.surfaceContainerHigh"),
            ("NavyBorder", "MaterialTheme.colorScheme.outlineVariant"),
            ("NavyTextPrimary", "MaterialTheme.colorScheme.onSurface"),
            ("NavyTextSecondary", "MaterialTheme.colorScheme.onSurfaceVariant"),
            ("NavyTextMuted", "MaterialTheme.colorScheme.onSurfaceVariant"),
        ]
        new_lines = []
        for line in st.splitlines():
            if line.startswith("import com.mayank.darkplayer.core.ui.theme.Navy"):
                continue
            for old_c, new_c in replacements:
                line = line.replace(old_c, new_c)
            new_lines.append(line)
        st = "\n".join(new_lines) + "\n" 
        settings_file.write_text(st, encoding="utf-8")

    # 5. Fix MainActivity.kt status bar & navigation bar icons for Light/Dark/AMOLED themes
    ma_file = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "MainActivity.kt"
    if ma_file.exists():
        mt = ma_file.read_text(encoding="utf-8", errors="ignore")
        if "DarkPlayerUiPrefs" not in mt:
            mt = mt.replace(
                "import com.mayank.darkplayer.core.ui.theme.DarkPlayerTheme",
                "import com.mayank.darkplayer.core.ui.theme.DarkPlayerTheme\nimport com.mayank.darkplayer.core.ui.theme.DarkPlayerThemeMode\nimport com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs",
                1,
            )
            mt = mt.replace(
                "isAppearanceLightStatusBars = !shouldUseDarkTheme",
                "DarkPlayerUiPrefs.ensureLoaded(this@MainActivity)\n                    val effectiveDarkBars = when (DarkPlayerUiPrefs.themeMode) {\n                        DarkPlayerThemeMode.SYSTEM -> shouldUseDarkTheme\n                        DarkPlayerThemeMode.DARK, DarkPlayerThemeMode.AMOLED -> true\n                        DarkPlayerThemeMode.LIGHT -> false\n                    }\n                    isAppearanceLightStatusBars = !effectiveDarkBars",
                1,
            )
            mt = mt.replace(
                "isAppearanceLightNavigationBars = !shouldUseDarkTheme",
                "isAppearanceLightNavigationBars = !effectiveDarkBars",
                1,
            )
            ma_file.write_text(mt, encoding="utf-8")

    # 6. Fix LiquidGlassNavBar.kt for Light Mode readability (no white-on-white!)
    lg_file = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "navigation/LiquidGlassNavBar.kt"
    if lg_file.exists():
        lt = lg_file.read_text(encoding="utf-8", errors="ignore")
        lt = lt.replace(
            "val contentColor = if (selected) NavyTextPrimary else NavyTextMuted",
            "val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant",
        )
        lt = lt.replace(
            "Color(0xFF141418).copy(alpha = 0.44f)",
            "MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)",
        )
        lt = lt.replace(
            "NavySurface.copy(alpha = 0.94f)",
            "MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)",
        )
        lt = "\n".join(
            line if line.startswith("import ") else line.replace("NavySurfaceSecondary", "MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)")
            for line in lt.splitlines()
        ) + "\n" 
        lt = lt.replace(
            "drawRect(Color(0x9916161A))",
            "drawRect(surfaceColor.copy(alpha = 0.78f))",
        )
        lt = lt.replace(
            "tint = if (backdrop != null && NavGlassSupport.supported) Color(0xFFF2F2F5) else iconTint",
            "tint = iconTint",
        )
        lg_file.write_text(lt, encoding="utf-8")

    # 7. Fix DarkPlayerUpdater.kt: strip raw HTML tags + reliable browser download on Update Now
    upd_file = project_root / "core/ui/src/main/java" / NEW_PACKAGE_PATH / "core/ui/updater/DarkPlayerUpdater.kt"
    if upd_file.exists():
        ut = upd_file.read_text(encoding="utf-8", errors="ignore")
        # Clean HTML tags from release notes
        ut = ut.replace(
            'val body = json.optString("body", "").trim()',
            'val rawBody = json.optString("body", "").trim()\n            val body = rawBody.replace(Regex("<[^>]+>"), " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\\"").replace("&#39;", "\x27").replace(Regex("[ \\t]+"), " ").replace(Regex("\\n{3,}"), "\\n\\n").trim()',
        )
        # Open release / APK download URL directly in browser on Update Now (Play Protect safe & 100% reliable!)
        old_btn = """                            onClick = {
                                if (state.downloaded != null) state.install(context)
                                else state.download(context)
                            },"""
        new_btn = """                            onClick = {
                                val targetUrl = update?.apkUrl?.takeIf { it.isNotBlank() }
                                    ?: "https://github.com/xboyrex/DarkPlayer/releases/latest"
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        },
                                    )
                                }
                                state.dismiss()
                            },"""
        if old_btn in ut:
            ut = ut.replace(old_btn, new_btn, 1)
        # Make updater dialog theme-aware
        upd_repls = [
            ("NavySurfaceSecondary", "MaterialTheme.colorScheme.surfaceContainerHighest"),
            ("NavySurface", "MaterialTheme.colorScheme.surfaceContainerHigh"),
            ("NavyBorder", "MaterialTheme.colorScheme.outlineVariant"),
            ("NavyTextPrimary", "MaterialTheme.colorScheme.onSurface"),
            ("NavyTextSecondary", "MaterialTheme.colorScheme.onSurfaceVariant"),
            ("NavyTextMuted", "MaterialTheme.colorScheme.onSurfaceVariant"),
            ("NavyBackground", "MaterialTheme.colorScheme.surface"),
        ]
        upd_lines = []
        for line in ut.splitlines():
            if line.startswith("import com.mayank.darkplayer.core.ui.theme.Navy"):
                continue
            for old_c, new_c in upd_repls:
                line = line.replace(old_c, new_c)
            upd_lines.append(line)
        ut = "\n".join(upd_lines) + "\n" 
        upd_file.write_text(ut, encoding="utf-8")




# =============================================================================
# V7 CALM / KIT DESIGN SYSTEM, REAL-TIME MODAL BLUR, BORDERLESS SHEETS & PLAYER TOGGLE
# =============================================================================

V7_OVERLAY_VIEW_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val safePadding = WindowInsets.safeDrawing.union(WindowInsets.displayCutout).asPaddingValues()

    val sheetBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xEE141916),
            Color(0xF20D110F),
        ),
    )

    AnimatedVisibility(
        modifier = modifier
            .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter)
            .padding(
                start = if (isLandscape) 12.dp else 12.dp,
                end = if (isLandscape) (safePadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 12.dp) else 12.dp,
                top = if (isLandscape) 12.dp else 0.dp,
                bottom = 12.dp,
            ),
        visible = show,
        enter = if (isLandscape) {
            slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
                initialOffsetX = { it / 2 },
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
            ) + fadeIn(tween(150))
        } else {
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
                initialOffsetY = { it / 2 },
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
            ) + fadeIn(tween(150))
        },
        exit = if (isLandscape) {
            slideOutHorizontally(
                animationSpec = tween(130),
                targetOffsetX = { it / 2 },
            ) + scaleOut(targetScale = 0.96f, animationSpec = tween(120)) + fadeOut(tween(110))
        } else {
            slideOutVertically(
                animationSpec = tween(130),
                targetOffsetY = { it / 2 },
            ) + scaleOut(targetScale = 0.96f, animationSpec = tween(120)) + fadeOut(tween(110))
        },
    ) {
        val shape = RoundedCornerShape(26.dp)
        Surface(
            modifier = Modifier
                .then(
                    if (isLandscape) {
                        Modifier
                            .fillMaxHeight()
                            .widthIn(min = 310.dp, max = 380.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                    },
                )
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = shape,
            color = Color.Transparent,
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .background(sheetBrush)
                    .padding(top = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(34.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x44F4EFE8)),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF4EFE8),
                    )
                }
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}
"""

V7_PLAYBACK_SPEED_SELECTOR_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.feature.player.state.rememberPlaybackParametersState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaybackSpeedSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
) {
    val playbackParametersState = rememberPlaybackParametersState(player = player)
    val playbackSpeed = playbackParametersState.speed
    val skipSilenceEnabled = playbackParametersState.skipSilenceEnabled
    val onPlaybackSpeedChange: (Float) -> Unit = { playbackParametersState.setPlaybackSpeed(it) }
    val onSkipSilenceChange: (Boolean) -> Unit = { playbackParametersState.setIsSkipSilenceEnabled(it) }
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val tonalCard = Color(0x16F4EFE8)
    val subtleText = Color(0xFFB3BCB4)
    val ivoryText = Color(0xFFF4EFE8)

    val presetsRow1 = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f)
    val presetsRow2 = listOf(1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    val presetsRow3 = listOf(3.5f, 4.0f)

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_playback_speed),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Compact borderless stepper header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tonalCard)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x1EF4EFE8))
                        .clickable {
                            val next = ((playbackSpeed - 0.05f).coerceIn(0.25f, 4.0f) * 20).roundToInt() / 20f
                            onPlaybackSpeedChange(next)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = null, tint = ivoryText, modifier = Modifier.size(18.dp))
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.2f", playbackSpeed),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = ivoryText,
                    )
                    Text(
                        text = "x",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x1EF4EFE8))
                            .clickable {
                                val next = ((playbackSpeed + 0.05f).coerceIn(0.25f, 4.0f) * 20).roundToInt() / 20f
                                onPlaybackSpeedChange(next)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = ivoryText, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(if (abs(playbackSpeed - 1.0f) < 0.01f) accent else Color(0x1EF4EFE8))
                            .clickable { onPlaybackSpeedChange(1.0f) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "1x",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (abs(playbackSpeed - 1.0f) < 0.01f) onAccent else ivoryText,
                        )
                    }
                }
            }

            // Borderless tonal slider card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tonalCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Slider(
                    value = playbackSpeed.coerceIn(0.25f, 4.0f),
                    onValueChange = { raw ->
                        val snapped = (raw * 20f).roundToInt() / 20f
                        onPlaybackSpeedChange(snapped.coerceIn(0.25f, 4.0f))
                    },
                    valueRange = 0.25f..4.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = Color(0x2AF4EFE8),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("0.25x", "1.0x", "2.0x", "4.0x").forEach { mark ->
                        Text(
                            text = mark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = subtleText,
                        )
                    }
                }
            }

            // Borderless preset pills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetsRow1.forEach { preset ->
                        SpeedPresetPill(
                            modifier = Modifier.weight(1f),
                            speed = preset,
                            selected = abs(playbackSpeed - preset) < 0.02f,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { onPlaybackSpeedChange(preset) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetsRow2.forEach { preset ->
                        SpeedPresetPill(
                            modifier = Modifier.weight(1f),
                            speed = preset,
                            selected = abs(playbackSpeed - preset) < 0.02f,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { onPlaybackSpeedChange(preset) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presetsRow3.forEach { preset ->
                        SpeedPresetPill(
                            modifier = Modifier.weight(1f),
                            speed = preset,
                            selected = abs(playbackSpeed - preset) < 0.02f,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { onPlaybackSpeedChange(preset) },
                        )
                    }
                }
            }

            // Borderless Skip Silence row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tonalCard)
                    .clickable { onSkipSilenceChange(!skipSilenceEnabled) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (skipSilenceEnabled) accent.copy(alpha = 0.22f) else Color(0x1AF4EFE8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = if (skipSilenceEnabled) accent else subtleText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.skip_silence),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ivoryText,
                    )
                }
                Switch(
                    checked = skipSilenceEnabled,
                    onCheckedChange = onSkipSilenceChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onAccent,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = Color(0xFFE4E0D8),
                        uncheckedTrackColor = Color(0x2CF4EFE8),
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SpeedPresetPill(
    modifier: Modifier = Modifier,
    speed: Float,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent else Color(0x16F4EFE8),
        animationSpec = tween(140),
        label = "speed_pill_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) onAccent else Color(0xFFE8E4DC),
        animationSpec = tween(140),
        label = "speed_pill_fg",
    )
    val label = if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}.0x"
    } else {
        "${speed}x"
    }
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
        )
    }
}
"""

V7_DECODER_SELECTOR_KT = """package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.feature.player.model.DecoderMode
import com.mayank.darkplayer.feature.player.model.DecoderTrackType
import com.mayank.darkplayer.feature.player.model.descriptionRes
import com.mayank.darkplayer.feature.player.model.labelRes
import com.mayank.darkplayer.feature.player.model.selectableDecoderModes

@Composable
fun BoxScope.DecoderSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    videoMode: DecoderMode?,
    audioMode: DecoderMode?,
    onVideoModeSelected: (DecoderMode) -> Unit,
    onAudioModeSelected: (DecoderMode) -> Unit,
) {
    var selectedTrackType by rememberSaveable { mutableStateOf(DecoderTrackType.VIDEO) }

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_decoders),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DecoderTrackTypeSelector(
                selectedTrackType = selectedTrackType,
                onTrackTypeSelected = { selectedTrackType = it },
            )
            DecoderModeGroup(
                selectedMode = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> videoMode
                    DecoderTrackType.AUDIO -> audioMode
                },
                trackType = selectedTrackType,
                onModeSelected = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> onVideoModeSelected
                    DecoderTrackType.AUDIO -> onAudioModeSelected
                },
            )
        }
    }
}

@Composable
private fun DecoderTrackTypeSelector(
    selectedTrackType: DecoderTrackType,
    onTrackTypeSelected: (DecoderTrackType) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x16F4EFE8))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DecoderTrackType.entries.forEach { trackType ->
            val selected = selectedTrackType == trackType
            val bg by animateColorAsState(if (selected) accent else Color.Transparent, tween(140), label = "dec_seg_bg")
            val fg by animateColorAsState(if (selected) onAccent else Color(0xFFD6DDD7), tween(140), label = "dec_seg_fg")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .selectable(selected = selected, onClick = { onTrackTypeSelected(trackType) }, role = Role.Tab)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = when (trackType) {
                        DecoderTrackType.VIDEO -> Icons.Rounded.Videocam
                        DecoderTrackType.AUDIO -> Icons.Rounded.Audiotrack
                    },
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        id = when (trackType) {
                            DecoderTrackType.VIDEO -> R.string.video
                            DecoderTrackType.AUDIO -> R.string.audio
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = fg,
                )
            }
        }
    }
}

@Composable
private fun DecoderModeGroup(
    selectedMode: DecoderMode?,
    trackType: DecoderTrackType,
    onModeSelected: (DecoderMode) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        selectableDecoderModes.filter {
            trackType == DecoderTrackType.VIDEO ||
                it != DecoderMode.HW_PLUS
        }.forEach { mode ->
            DecoderModeRow(
                mode = mode,
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
            )
        }
    }
}

@Composable
private fun DecoderModeRow(mode: DecoderMode, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val icon = when (mode) {
        DecoderMode.AUTO -> Icons.Rounded.Tune
        DecoderMode.HARDWARE -> Icons.Rounded.Memory
        DecoderMode.HW_PLUS -> Icons.Rounded.Memory
        DecoderMode.FFMPEG -> Icons.Rounded.DeveloperBoard
    }
    val rowBg by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.20f) else Color(0x14F4EFE8),
        animationSpec = tween(140),
        label = "dec_row_bg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(18.dp))
            .background(rowBg)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) accent.copy(alpha = 0.25f) else Color(0x1AF4EFE8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else Color(0xFFD0D6D1),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(mode.labelRes),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) accent else Color(0xFFF4EFE8),
            )
            Text(
                text = stringResource(mode.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFAAB4AC),
            )
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = accent,
                unselectedColor = Color(0xFF7D877F),
            ),
        )
    }
}
"""

V7_QUICK_SETTINGS_DIALOG_KT = """package com.mayank.darkplayer.feature.videopicker.composables

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.mayank.darkplayer.core.model.ApplicationPreferences
import com.mayank.darkplayer.core.model.MediaLayoutMode
import com.mayank.darkplayer.core.model.MediaViewMode
import com.mayank.darkplayer.core.model.Sort
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs

@Composable
fun QuickSettingsDialog(
    applicationPreferences: ApplicationPreferences,
    onDismiss: () -> Unit,
    updatePreferences: (ApplicationPreferences) -> Unit,
) {
    var preferences by remember { mutableStateOf(applicationPreferences) }
    var fieldsExpanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        DarkPlayerUiPrefs.pushModalBlur()
        onDispose { DarkPlayerUiPrefs.popModalBlur() }
    }
    LaunchedEffect(Unit) { entered = true }

    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val innerTonal = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val accentContainer = MaterialTheme.colorScheme.primaryContainer
    val onAccentContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogView = LocalView.current
        androidx.compose.runtime.SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setDimAmount(0.42f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 64
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(160)) +
                scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(dampingRatio = 0.80f, stiffness = 460f),
                ) +
                slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f),
                ),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = cardBg,
                tonalElevation = 0.dp,
                shadowElevation = 24.dp,
            ) {
                Column(
                    modifier = Modifier
                        .animateContentSize(animationSpec = spring(dampingRatio = 0.86f, stiffness = 440f))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    // Compact View Mode & Layout Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "View Mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(3f),
                        )
                        Text(
                            text = "Layout",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(2f).padding(start = 10.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(innerTonal.copy(alpha = 0.65f))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.FolderCopy,
                            label = "All",
                            selected = preferences.mediaViewMode == MediaViewMode.FOLDER_TREE,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDER_TREE) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.InsertDriveFile,
                            label = "Files",
                            selected = preferences.mediaViewMode == MediaViewMode.VIDEOS,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.VIDEOS) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Folder,
                            label = "Folders",
                            selected = preferences.mediaViewMode == MediaViewMode.FOLDERS,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDERS) },
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .width(1.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.ViewAgenda,
                            label = "List",
                            selected = preferences.mediaLayoutMode == MediaLayoutMode.LIST,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.GridView,
                            label = "Grid",
                            selected = preferences.mediaLayoutMode == MediaLayoutMode.GRID,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.GRID) },
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Sort Section
                    Text(
                        text = "Sort",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(innerTonal.copy(alpha = 0.65f))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    ) {
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.SortByAlpha,
                            label = "Title",
                            selected = preferences.sortBy == Sort.By.TITLE,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.TITLE) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.CalendarToday,
                            label = "Date",
                            selected = preferences.sortBy == Sort.By.DATE,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.DATE) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Timer,
                            label = "Length",
                            selected = preferences.sortBy == Sort.By.LENGTH,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.LENGTH) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.SdStorage,
                            label = "Size",
                            selected = preferences.sortBy == Sort.By.SIZE,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.SIZE) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.LocationOn,
                            label = "Path",
                            selected = preferences.sortBy == Sort.By.PATH,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.PATH) },
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // CALM / KIT Segmented Ascending / Descending pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(innerTonal.copy(alpha = 0.75f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val isAsc = preferences.sortOrder == Sort.Order.ASCENDING
                        val ascBg by animateColorAsState(if (isAsc) accent else Color.Transparent, tween(150), label = "asc_bg")
                        val ascFg by animateColorAsState(if (isAsc) onAccent else muted, tween(150), label = "asc_fg")
                        val descBg by animateColorAsState(if (!isAsc) accent else Color.Transparent, tween(150), label = "desc_bg")
                        val descFg by animateColorAsState(if (!isAsc) onAccent else muted, tween(150), label = "desc_fg")

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(ascBg)
                                .clickable { preferences = preferences.copy(sortOrder = Sort.Order.ASCENDING) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = ascFg, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "A to Z",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ascFg,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(descBg)
                                .clickable { preferences = preferences.copy(sortOrder = Sort.Order.DESCENDING) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = descFg, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Z to A",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = descFg,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Expandable Fields Pill Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(innerTonal.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fieldsExpanded = !fieldsExpanded }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Fields",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(
                                imageVector = if (fieldsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = muted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        AnimatedVisibility(
                            visible = fieldsExpanded,
                            enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 420f)) + fadeIn(tween(140)),
                            exit = shrinkVertically(tween(130)) + fadeOut(tween(110)),
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CalmCheckCell(Modifier.weight(1f), "Thumbnail", preferences.showThumbnailField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showThumbnailField = !preferences.showThumbnailField)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Length", preferences.showDurationField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showDurationField = !preferences.showDurationField)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Ext", preferences.showExtensionField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showExtensionField = !preferences.showExtensionField)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CalmCheckCell(Modifier.weight(1f), "Played", preferences.showPlayedProgress, accent, onAccent, muted) {
                                        preferences = preferences.copy(showPlayedProgress = !preferences.showPlayedProgress)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Res", preferences.showResolutionField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showResolutionField = !preferences.showResolutionField)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Path", preferences.showPathField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showPathField = !preferences.showPathField)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CalmCheckCell(Modifier.weight(1f), "Size", preferences.showSizeField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showSizeField = !preferences.showSizeField)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Folder", preferences.showFolderDurationField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showFolderDurationField = !preferences.showFolderDurationField)
                                    }
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Expandable Advanced Pill Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(innerTonal.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { advancedExpanded = !advancedExpanded }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Advanced",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(
                                imageVector = if (advancedExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = muted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        AnimatedVisibility(
                            visible = advancedExpanded,
                            enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 420f)) + fadeIn(tween(140)),
                            exit = shrinkVertically(tween(130)) + fadeOut(tween(110)),
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                CalmSwitchRow("Length over thumbnail", preferences.showDurationField, accent, onAccent) {
                                    preferences = preferences.copy(showDurationField = it)
                                }
                                CalmSwitchRow("Mark last played media", preferences.markLastPlayedMedia, accent, onAccent) {
                                    preferences = preferences.copy(markLastPlayedMedia = it)
                                }
                                CalmSwitchRow("Pause watch history", preferences.isHistoryPaused, accent, onAccent) {
                                    preferences = preferences.copy(isHistoryPaused = it)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // CALM / KIT Action Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(innerTonal)
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = muted,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accent)
                                .clickable {
                                    updatePreferences(preferences)
                                    onDismiss()
                                }
                                .padding(horizontal = 22.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.okay),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = onAccent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalmIconTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    accentContainer: Color,
    muted: Color,
    onClick: () -> Unit,
) {
    val tileBg by animateColorAsState(
        targetValue = if (selected) accentContainer else Color.Transparent,
        animationSpec = tween(140),
        label = "calm_tile_bg",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) accent else muted,
        animationSpec = tween(140),
        label = "calm_tile_tint",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tileBg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CalmCheckCell(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    accent: Color,
    onAccent: Color,
    muted: Color,
    onClick: () -> Unit,
) {
    val boxBg by animateColorAsState(
        targetValue = if (checked) accent else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(130),
        label = "calm_chk_bg",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(boxBg),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = onAccent, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (checked) MaterialTheme.colorScheme.onSurface else muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CalmSwitchRow(
    title: String,
    checked: Boolean,
    accent: Color,
    onAccent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = onAccent,
                checkedTrackColor = accent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}
"""

V7_ABOUT_SCREEN_KT = """package com.mayank.darkplayer.settings.screens.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HdrOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mayank.darkplayer.core.common.extensions.appIcon
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.components.NextTopAppBar
import com.mayank.darkplayer.core.ui.designsystem.NextIcons
import com.mayank.darkplayer.core.ui.theme.calmEntrance
import com.mayank.darkplayer.core.ui.theme.calmPressScale
import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateDialog
import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateState

private const val GITHUB_URL = "https://github.com/xboyrex/DarkPlayer"

@Composable
fun AboutPreferencesScreen(viewModel: AboutPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutPreferencesScreenContent(
    state: AboutPreferencesUiState,
    onAction: (AboutPreferencesAction) -> Unit,
) {
    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val updateState = remember(scope, state.appVersion) {
        DarkPlayerUpdateState(scope = scope, currentVersionName = state.appVersion)
    }
    val appIconBitmap = remember(context) { context.appIcon()?.asImageBitmap() }

    val versionInfo = remember(context, state.appVersion) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong()
            "v${pInfo.versionName ?: state.appVersion} ($code)"
        } catch (_: Exception) {
            "v${state.appVersion}"
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val accentContainer = MaterialTheme.colorScheme.primaryContainer
    val onAccentContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val surfaceCard = MaterialTheme.colorScheme.surfaceContainer
    val innerTile = MaterialTheme.colorScheme.surfaceContainerHigh
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    DarkPlayerUpdateDialog(state = updateState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.about_name),
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(AboutPreferencesAction.NavigateUp) }) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Editorial Borderless Hero Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .calmEntrance(delayMillis = 0)
                    .clip(RoundedCornerShape(28.dp))
                    .background(surfaceCard)
                    .padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(accentContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = "Dark Player Logo",
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = "Dark Player Logo",
                            tint = accent,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Dark Player",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Crafted by Mayank (xboyrex)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = muted,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentContainer)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "$versionInfo  •  Release Build",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = onAccentContainer,
                    )
                }
            }

            // 2x2 Borderless Capability Tiles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .calmEntrance(delayMillis = 55),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AboutCapabilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.PlayCircle,
                    title = "Media3 Engine",
                    subtitle = "Hardware accelerated",
                    bg = surfaceCard,
                    accent = accent,
                    accentContainer = accentContainer,
                    muted = muted,
                )
                AboutCapabilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Memory,
                    title = "FFmpeg 10-bit",
                    subtitle = "Auto HW+ seeking",
                    bg = surfaceCard,
                    accent = accent,
                    accentContainer = accentContainer,
                    muted = muted,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .calmEntrance(delayMillis = 85),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AboutCapabilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.HdrOn,
                    title = "HDR10 / SDR",
                    subtitle = "Live tone control",
                    bg = surfaceCard,
                    accent = accent,
                    accentContainer = accentContainer,
                    muted = muted,
                )
                AboutCapabilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Liquid Glass",
                    subtitle = "Calm adaptive UI",
                    bg = surfaceCard,
                    accent = accent,
                    accentContainer = accentContainer,
                    muted = muted,
                )
            }

            // Borderless Actions Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .calmEntrance(delayMillis = 120)
                    .clip(RoundedCornerShape(24.dp))
                    .background(surfaceCard)
                    .padding(vertical = 6.dp),
            ) {
                AboutActionRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = "Fetch latest release from GitHub",
                    accent = accent,
                    badgeBg = accentContainer,
                    muted = muted,
                    onClick = { updateState.check(manual = true) },
                )
                AboutActionRow(
                    icon = Icons.Rounded.Code,
                    title = "GitHub Repository",
                    subtitle = "github.com/xboyrex/DarkPlayer",
                    accent = accent,
                    badgeBg = innerTile,
                    muted = muted,
                    onClick = { uriHandler.openUriOrShowToast(GITHUB_URL, context) },
                )
                AboutActionRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.libraries),
                    subtitle = "Open-source licenses & attributions",
                    accent = accent,
                    badgeBg = innerTile,
                    muted = muted,
                    onClick = { onAction(AboutPreferencesAction.OpenLibraries) },
                )
                AboutActionRow(
                    icon = Icons.Rounded.ContentCopy,
                    title = "Copy Build & Device Info",
                    subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${Build.MODEL}",
                    accent = accent,
                    badgeBg = innerTile,
                    muted = muted,
                    onClick = {
                        val info = "Dark Player $versionInfo | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL}"
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Dark Player Info", info))
                        Toast.makeText(context, "Build info copied", Toast.LENGTH_SHORT).show()
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun AboutCapabilityTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    bg: Color,
    accent: Color,
    accentContainer: Color,
    muted: Color,
) {
    Row(
        modifier = modifier
            .calmPressScale(pressedScale = 0.95f)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = muted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    badgeBg: Color,
    muted: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .calmPressScale(pressedScale = 0.97f)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

internal fun UriHandler.openUriOrShowToast(uri: String, context: Context) {
    try {
        openUri(uri = uri)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.error_opening_link),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
"""

def apply_v7_calm_polish(project_root: Path):
    """V7 CALM / KIT palette, real-time animated modal blur, borderless side sheets, Dark/White player button toggle, editorial About."""

    # 1. Add CALM_SAGE (#6B7F6E Sage, #A7B8A7 Moss, #D9E1D9 Mist, #F4EFE8 Ivory, #C9BDAF Sand) as DEFAULT palette
    #    + playerButtonsDark + activeModalBlurCount + Borderless animated DarkModalFrame
    dialogs_file = project_root / "core/ui/src/main/java" / NEW_PACKAGE_PATH / "core/ui/theme/DarkPlayerSettingsDialogs.kt"
    if dialogs_file.exists():
        dt = dialogs_file.read_text(encoding="utf-8", errors="ignore")
        if "CALM_SAGE(" not in dt:
            dt = dt.replace(
                "    OBSIDIAN(",
                """    CALM_SAGE(
        label = "Calm Sage",
        accentDark = Color(0xFFA7B8A7),
        onAccentDark = Color(0xFF0E130F),
        accentLight = Color(0xFF6B7F6E),
        onAccentLight = Color(0xFFF4EFE8),
        swatchSecondary = Color(0xFFC9BDAF),
    ),
    OBSIDIAN(""",
                1,
            )
        # Switch default palette from EMERALD to CALM_SAGE
        dt = dt.replace("DarkPlayerPalette.EMERALD", "DarkPlayerPalette.CALM_SAGE")

        # Add playerButtonsDark & activeModalBlurCount to DarkPlayerUiPrefs
        if "playerButtonsDark" not in dt:
            dt = dt.replace(
                'private const val KEY_LANG = "language_tag"',
                'private const val KEY_LANG = "language_tag"\n    private const val KEY_PLAYER_BTN_DARK = "player_btn_dark"',
                1,
            )
            dt = dt.replace(
                'var languageTag by mutableStateOf("")',
                'var languageTag by mutableStateOf("")\n    var playerButtonsDark by mutableStateOf(true)\n    var activeModalBlurCount by mutableStateOf(0)\n\n    fun pushModalBlur() { activeModalBlurCount += 1 }\n    fun popModalBlur() { activeModalBlurCount = (activeModalBlurCount - 1).coerceAtLeast(0) }',
                1,
            )
            dt = dt.replace(
                'languageTag = sp.getString(KEY_LANG, "") ?: ""',
                'languageTag = sp.getString(KEY_LANG, "") ?: ""\n        if (!sp.getBoolean("v7_calm_migrated", false)) {\n            palette = DarkPlayerPalette.CALM_SAGE\n            playerButtonsDark = true\n            sp.edit().putString(KEY_PALETTE, DarkPlayerPalette.CALM_SAGE.name).putBoolean(KEY_PLAYER_BTN_DARK, true).putBoolean("v7_calm_migrated", true).apply()\n        } else {\n            playerButtonsDark = sp.getBoolean(KEY_PLAYER_BTN_DARK, true)\n        }',
                1,
            )
            dt = dt.replace(
                "    fun updateLiquidGlass(",
                """    fun togglePlayerButtonsDark(context: Context): Boolean {
        playerButtonsDark = !playerButtonsDark
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PLAYER_BTN_DARK, playerButtonsDark).apply()
        return playerButtonsDark
    }

    fun updateLiquidGlass(""",
                1,
            )

        # Upgrade DarkModalFrame: push/pop real-time modal blur + smooth spring scale/fade + borderless!
        if "DarkPlayerUiPrefs.pushModalBlur()" not in dt:
            dt = dt.replace(
                "import androidx.compose.runtime.SideEffect",
                "import androidx.compose.animation.AnimatedVisibility\nimport androidx.compose.animation.core.spring\nimport androidx.compose.animation.core.tween\nimport androidx.compose.animation.fadeIn\nimport androidx.compose.animation.scaleIn\nimport androidx.compose.animation.slideInVertically\nimport androidx.compose.runtime.DisposableEffect\nimport androidx.compose.runtime.LaunchedEffect\nimport androidx.compose.runtime.SideEffect",
                1,
            )
            dt = dt.replace(
                "    Dialog(\n        onDismissRequest = onDismiss,",
                """    var entered by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        DarkPlayerUiPrefs.pushModalBlur()
        onDispose { DarkPlayerUiPrefs.popModalBlur() }
    }
    LaunchedEffect(Unit) { entered = true }

    Dialog(
        onDismissRequest = onDismiss,""",
                1,
            )
            dt = dt.replace(
                "        Surface(\n            modifier = Modifier\n                .fillMaxWidth()\n                .padding(horizontal = 18.dp),",
                """        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(160)) +
                scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = 0.80f, stiffness = 460f)) +
                slideInVertically(initialOffsetY = { it / 12 }, animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f)),
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),""",
                1,
            )
            # Close AnimatedVisibility right before the closing brace of Dialog
            dt = dt.replace(
                "                content()\n            }\n        }\n    }\n}",
                "                content()\n            }\n        }\n        }\n    }\n}",
                1,
            )
            # Remove harsh 1.dp border from DarkModalFrame & inner cards
            dt = dt.replace(".border(1.dp, borderColor, RoundedCornerShape(28.dp))\n                    ", "")
            dt = dt.replace(".border(1.dp, borderColor, RoundedCornerShape(16.dp))", "")
            dt = dt.replace(".border(1.dp, borderColor, RoundedCornerShape(18.dp))", "")
            dt = dt.replace(".border(1.dp, borderColor, RoundedCornerShape(20.dp))", "")
            dt = dt.replace(".border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))", "")
            dt = dt.replace("if (selected) Modifier.border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))", "if (selected) Modifier")
            dt = dt.replace(".border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))", "")

        dialogs_file.write_text(dt, encoding="utf-8")

    # 2. Upgrade Theme.kt to authentic CALM / KIT organic Ivory (#F4EFE8) Light & Obsidian-Moss (#0D100E) Dark
    theme_file = project_root / "core/ui/src/main/java" / NEW_PACKAGE_PATH / "core/ui/theme/Theme.kt"
    if theme_file.exists():
        tt = theme_file.read_text(encoding="utf-8", errors="ignore")
        # Warm Ivory CALM / KIT light surfaces
        tt = tt.replace("background = Color(0xFFF6F6F9),", "background = Color(0xFFF4EFE8),")
        tt = tt.replace("onBackground = Color(0xFF111114),", "onBackground = Color(0xFF1D231E),")
        tt = tt.replace("surface = Color(0xFFFFFFFF),", "surface = Color(0xFFF4EFE8),")
        tt = tt.replace("onSurface = Color(0xFF111114),", "onSurface = Color(0xFF1D231E),")
        tt = tt.replace("surfaceVariant = Color(0xFFE7E7EE),", "surfaceVariant = Color(0xFFD9E1D9),")
        tt = tt.replace("onSurfaceVariant = Color(0xFF474752),", "onSurfaceVariant = Color(0xFF566058),")
        tt = tt.replace("outlineVariant = Color(0xFFD2D2DC),", "outlineVariant = Color(0xFFC9BDAF),")
        tt = tt.replace("surfaceContainerLowest = Color(0xFFFFFFFF),", "surfaceContainerLowest = Color(0xFFFCFBF8),")
        tt = tt.replace("surfaceContainerLow = Color(0xFFF4F4F8),", "surfaceContainerLow = Color(0xFFF9F6F0),")
        tt = tt.replace("surfaceContainer = Color(0xFFEEEEF3),", "surfaceContainer = Color(0xFFFCFBF8),")
        tt = tt.replace("surfaceContainerHigh = Color(0xFFE8E8EF),", "surfaceContainerHigh = Color(0xFFEFEAE2),")
        tt = tt.replace("surfaceContainerHighest = Color(0xFFE0E0E8),", "surfaceContainerHighest = Color(0xFFE5DEC9),")
        # Calm Obsidian-Moss dark surfaces & Mist light primaryContainer
        tt = tt.replace(
            "        effectiveDark -> baseDarkScheme.copy(\n"
            "            primary = pal.accentDark,\n"
            "            onPrimary = pal.onAccentDark,\n"
            "            primaryContainer = pal.accentDark.copy(alpha = 0.20f),\n"
            "            onPrimaryContainer = Color(0xFFF2F2F5),\n"
            "            secondary = pal.accentDark,\n"
            "            onSecondary = pal.onAccentDark,\n"
            "        )",
            "        effectiveDark -> baseDarkScheme.copy(\n"
            "            primary = pal.accentDark,\n"
            "            onPrimary = pal.onAccentDark,\n"
            "            primaryContainer = pal.accentDark.copy(alpha = 0.22f),\n"
            "            onPrimaryContainer = Color(0xFFF4EFE8),\n"
            "            secondary = pal.swatchSecondary,\n"
            "            onSecondary = Color(0xFF121613),\n"
            "            background = Color(0xFF0D100E),\n"
            "            onBackground = Color(0xFFF4EFE8),\n"
            "            surface = Color(0xFF0D100E),\n"
            "            onSurface = Color(0xFFF4EFE8),\n"
            "            onSurfaceVariant = Color(0xFFA7B2A8),\n"
            "            surfaceContainerLowest = Color(0xFF0A0D0B),\n"
            "            surfaceContainerLow = Color(0xFF131815),\n"
            "            surfaceContainer = Color(0xFF171D19),\n"
            "            surfaceContainerHigh = Color(0xFF1F2621),\n"
            "            surfaceContainerHighest = Color(0xFF28302A),\n"
            "            outlineVariant = Color(0xFF2C352E),\n"
            "        )",
            1,
        )
        tt = tt.replace(
            "primaryContainer = pal.accentLight.copy(alpha = 0.14f),",
            "primaryContainer = Color(0xFFD9E1D9),",
            1,
        )
        theme_file.write_text(tt, encoding="utf-8")

    # 3. MainActivity.kt: real-time animated GPU background blur when any modal is open + immediate status-bar flip
    main_act = project_root / "app/src/main/java" / NEW_PACKAGE_PATH / "MainActivity.kt"
    if main_act.exists():
        mt = main_act.read_text(encoding="utf-8", errors="ignore")
        if "animateDpAsState" not in mt:
            mt = mt.replace(
                "import androidx.compose.animation.AnimatedVisibility",
                "import androidx.compose.animation.AnimatedVisibility\nimport androidx.compose.animation.core.animateDpAsState\nimport androidx.compose.animation.core.spring",
                1,
            )
            mt = mt.replace(
                "import androidx.compose.ui.Modifier",
                "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.blur",
                1,
            )
        mt = mt.replace(
            "LaunchedEffect(key1 = shouldUseDarkTheme) {",
            "LaunchedEffect(key1 = shouldUseDarkTheme, key2 = DarkPlayerUiPrefs.themeMode) {",
        )
        if "modalBlurRadius" not in mt:
            mt = mt.replace(
                "    val navGlassBackdrop = rememberLayerBackdrop()\n    CompositionLocalProvider(LocalNavGlassBackdrop provides navGlassBackdrop) {\n    Box(modifier = Modifier.fillMaxSize()) {",
                """    val modalBlurRadius by animateDpAsState(
        targetValue = if (DarkPlayerUiPrefs.activeModalBlurCount > 0) 18.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 360f),
        label = "modal_bg_blur",
    )
    val navGlassBackdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalNavGlassBackdrop provides navGlassBackdrop) {
    Box(modifier = Modifier.fillMaxSize().blur(radius = modalBlurRadius)) {""",
                1,
            )
        main_act.write_text(mt, encoding="utf-8")

    # 4. PlayerButton.kt: Smoothly reactive Dark Glass (default) / White Glass player buttons
    pbtn_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/buttons/PlayerButton.kt"
    if pbtn_file.exists():
        pt = pbtn_file.read_text(encoding="utf-8", errors="ignore")
        if "DarkPlayerUiPrefs.playerButtonsDark" not in pt:
            pt = pt.replace(
                "import androidx.compose.foundation.background",
                "import androidx.compose.animation.animateColorAsState\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.background",
                1,
            )
            pt = pt.replace(
                "import androidx.compose.runtime.remember",
                "import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.remember",
                1,
            )
            pt = pt.replace(
                f"import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillFill",
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs\nimport {NEW_PACKAGE}.core.ui.theme.PlayerControlPillFill",
                1,
            )
            pt = pt.replace(
                """    val colors = if (LocalUseMaterialYouControls.current) {
        IconButtonDefaults.filledTonalIconButtonColors()
    } else {
        IconButtonDefaults.iconButtonColors(
            contentColor = contentColor,
            containerColor = containerColor,
        )
    }""",
                """    DarkPlayerUiPrefs.ensureLoaded(context)
    val isDarkBtn = DarkPlayerUiPrefs.playerButtonsDark
    val resolvedContainer by animateColorAsState(
        targetValue = if (containerColor == PlayerControlPillFill) {
            if (isDarkBtn) Color(0xCC121614) else Color(0xE8F4EFE8)
        } else {
            containerColor
        },
        animationSpec = tween(180),
        label = "player_btn_bg",
    )
    val resolvedContent by animateColorAsState(
        targetValue = if (contentColor == PlayerControlPillIcon) {
            if (isDarkBtn) Color(0xFFF4EFE8) else Color(0xFF161B17)
        } else {
            contentColor
        },
        animationSpec = tween(180),
        label = "player_btn_fg",
    )

    val colors = IconButtonDefaults.iconButtonColors(
        contentColor = resolvedContent,
        containerColor = resolvedContainer,
    )""",
                1,
            )
            pbtn_file.write_text(pt, encoding="utf-8")

    # 5. ControlsBottomView.kt: Time pill smoothly adapts to Dark / White player button mode
    cbv_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/controls/ControlsBottomView.kt"
    if cbv_file.exists():
        ct = cbv_file.read_text(encoding="utf-8", errors="ignore")
        if "DarkPlayerUiPrefs.playerButtonsDark" not in ct:
            ct = ct.replace(
                f"import {NEW_PACKAGE}.core.ui.theme.PlayerTimePillFill",
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs\nimport {NEW_PACKAGE}.core.ui.theme.PlayerTimePillFill",
                1,
            )
            ct = ct.replace(
                "containerColor = PlayerTimePillFill,",
                "containerColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xCC121614) else androidx.compose.ui.graphics.Color(0xE8F4EFE8),",
            )
            ct = ct.replace(
                "color = PlayerTimePillFill,",
                "color = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xCC121614) else androidx.compose.ui.graphics.Color(0xE8F4EFE8),",
            )
            ct = ct.replace(
                "contentColor = PlayerControlPillIcon,",
                "contentColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xFFF4EFE8) else androidx.compose.ui.graphics.Color(0xFF161B17),",
            )
            cbv_file.write_text(ct, encoding="utf-8")

    # 6. DarkPlayerProTools.kt: Add Dark / White player buttons toggle circular button (Icons.Rounded.Contrast)
    #    + make ProCircleButton follow Dark/White glass styling
    pro_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DarkPlayerProTools.kt"
    if pro_file.exists():
        prt = pro_file.read_text(encoding="utf-8", errors="ignore")
        if "Icons.Rounded.Contrast" not in prt:
            prt = prt.replace(
                "import androidx.compose.material.icons.rounded.Bedtime",
                "import androidx.compose.material.icons.rounded.Bedtime\nimport androidx.compose.material.icons.rounded.Contrast",
                1,
            )
            prt = prt.replace(
                f"import {NEW_PACKAGE}.core.ui.theme.PlayerControlPillFill",
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs\nimport {NEW_PACKAGE}.core.ui.theme.PlayerControlPillFill",
                1,
            )
            # Update ProCircleButton colors to follow DarkPlayerUiPrefs.playerButtonsDark
            prt = prt.replace(
                "val bgColor = if (active) MaterialTheme.colorScheme.primary else PlayerControlPillFill",
                "val isDarkBtn = DarkPlayerUiPrefs.playerButtonsDark\n    val bgColor = if (active) MaterialTheme.colorScheme.primary else if (isDarkBtn) Color(0xCC121614) else Color(0xE8F4EFE8)",
                1,
            )
            prt = prt.replace(
                "val iconTint = if (active) MaterialTheme.colorScheme.onPrimary else Color(0xFF0E0E10)",
                "val iconTint = if (active) MaterialTheme.colorScheme.onPrimary else if (isDarkBtn) Color(0xFFF4EFE8) else Color(0xFF161B17)",
                1,
            )
            # Add the 5th circular button right after A-B Repeat inside ProQuickToolsStrip
            target_ab = """        ProCircleButton(
            icon = Icons.Rounded.Repeat,"""
            if target_ab in prt:
                # Insert right before ProCircleButton(Repeat) or right after Night
                prt = prt.replace(
                    target_ab,
                    """        ProCircleButton(
            icon = Icons.Rounded.Contrast,
            contentDescription = "Player Button Theme",
            active = !DarkPlayerUiPrefs.playerButtonsDark,
            badgeText = null,
            onClick = {
                val nowDark = DarkPlayerUiPrefs.togglePlayerButtonsDark(context)
                Toast.makeText(
                    context,
                    if (nowDark) "Player buttons: Dark" else "Player buttons: White",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
""" + target_ab,
                    1,
                )
            pro_file.write_text(prt, encoding="utf-8")

    # 7. Write borderless floating frosted glass side sheets & compact blurred QuickSettings & editorial About
    (project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/OverlayView.kt").write_text(V7_OVERLAY_VIEW_KT, encoding="utf-8")
    (project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/PlaybackSpeedSelectorView.kt").write_text(V7_PLAYBACK_SPEED_SELECTOR_KT, encoding="utf-8")
    (project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DecoderSelectorView.kt").write_text(V7_DECODER_SELECTOR_KT, encoding="utf-8")
    (project_root / "feature/videopicker/src/main/java" / NEW_PACKAGE_PATH / "feature/videopicker/composables/QuickSettingsDialog.kt").write_text(V7_QUICK_SETTINGS_DIALOG_KT, encoding="utf-8")
    (project_root / "feature/settings/src/main/java" / NEW_PACKAGE_PATH / "settings/screens/about/AboutPreferencesScreen.kt").write_text(V7_ABOUT_SCREEN_KT, encoding="utf-8")

    # 8. Strip harsh 1.dp stock borders from SettingsScreen.kt for cohesive CALM / KIT borderless aesthetic
    settings_file = project_root / "feature/settings/src/main/java" / NEW_PACKAGE_PATH / "settings/SettingsScreen.kt"
    if settings_file.exists():
        st = settings_file.read_text(encoding="utf-8", errors="ignore")
        def _strip_border_calls(src: str) -> str:
            out = []
            i = 0
            n = len(src)
            while i < n:
                if src.startswith(".border(", i):
                    depth = 1
                    j = i + len(".border(")
                    while j < n and depth > 0:
                        if src[j] == "(":
                            depth += 1
                        elif src[j] == ")":
                            depth -= 1
                        j += 1
                    i = j
                else:
                    out.append(src[i])
                    i += 1
            return "".join(out)
        st = _strip_border_calls(st)
        settings_file.write_text(st, encoding="utf-8")

    # 9. Strip remaining NavyBorder in DarkPlayerProTools.kt & bump versionCode to 79 (0.22.0-calm)
    if pro_file.exists():
        prt2 = pro_file.read_text(encoding="utf-8", errors="ignore")
        prt2 = prt2.replace(".border(1.dp, NavyBorder, RoundedCornerShape(22.dp))", "")
        pro_file.write_text(prt2, encoding="utf-8")

    app_gradle = project_root / "app/build.gradle.kts"
    if app_gradle.exists():
        gt = app_gradle.read_text(encoding="utf-8", errors="ignore")
        gt = gt.replace("versionCode = 78", "versionCode = 79")
        gt = gt.replace('versionName = "0.21.0"', 'versionName = "0.22.0-calm"')
        app_gradle.write_text(gt, encoding="utf-8")




# =============================================================================
# V8 GLOBAL MOTION SYSTEM (PRESS BOUNCE, STAGGERED ENTRANCES, EXIT ANIMATIONS)
# =============================================================================

V8_DARK_MOTION_KT = """package com.mayank.darkplayer.core.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val DarkSpringFast = spring<Float>(
    dampingRatio = 0.74f,
    stiffness = 560f,
)

val DarkSpringGentle = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = 420f,
)

/**
 * Non-consuming tactile press-scale spring animation for buttons, cards, and tiles.
 */
fun Modifier.calmPressScale(
    pressedScale: Float = 0.94f,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = DarkSpringFast,
        label = "calm_press_scale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
}

/**
 * Subtle staggered fade + rise + micro-scale entrance for screen sections and cards.
 */
fun Modifier.calmEntrance(
    delayMillis: Int = 0,
    offsetY: Float = 26f,
): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = DarkSpringGentle,
        )
    }
    this.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * offsetY
        val s = 0.97f + 0.03f * p
        scaleX = s
        scaleY = s
    }
}
"""


def apply_v8_global_motion(project_root: Path):
    """Installs DarkMotion.kt and wires spring press-bounce, staggered entrances, and modal exit animations globally."""

    # 1. Emit DarkMotion.kt in core/ui/theme/
    motion_file = project_root / "core/ui/src/main/java" / NEW_PACKAGE_PATH / "core/ui/theme/DarkMotion.kt"
    motion_file.write_text(V8_DARK_MOTION_KT, encoding="utf-8")

    # 2. PlayerButton.kt -> add .calmPressScale(0.90f, enabled = enabled)
    pbtn = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/buttons/PlayerButton.kt"
    if pbtn.exists():
        t = pbtn.read_text(encoding="utf-8", errors="ignore")
        if "calmPressScale" not in t:
            t = t.replace(
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs",
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs\nimport {NEW_PACKAGE}.core.ui.theme.calmPressScale",
                1,
            )
            t = t.replace(
                "            modifier = modifier\n                .clip(shape = CircleShape)",
                "            modifier = modifier\n                .calmPressScale(pressedScale = 0.90f, enabled = enabled)\n                .clip(shape = CircleShape)",
                1,
            )
            pbtn.write_text(t, encoding="utf-8")

    # 3. DarkPlayerProTools.kt -> add .calmPressScale(0.90f) to ProCircleButton
    pro = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DarkPlayerProTools.kt"
    if pro.exists():
        t = pro.read_text(encoding="utf-8", errors="ignore")
        if "calmPressScale" not in t:
            t = t.replace(
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs",
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs\nimport {NEW_PACKAGE}.core.ui.theme.calmPressScale",
                1,
            )
            t = t.replace(
                "            .size(44.dp)\n            .clip(CircleShape)",
                "            .calmPressScale(pressedScale = 0.90f)\n            .size(44.dp)\n            .clip(CircleShape)",
                1,
            )
            pro.write_text(t, encoding="utf-8")

    # 4. PlaybackSpeedSelectorView.kt -> add .calmPressScale(0.93f) to SpeedPresetPill
    spd = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/PlaybackSpeedSelectorView.kt"
    if spd.exists():
        t = spd.read_text(encoding="utf-8", errors="ignore")
        if "calmPressScale" not in t:
            t = t.replace(
                f"import {NEW_PACKAGE}.core.ui.R",
                f"import {NEW_PACKAGE}.core.ui.R\nimport {NEW_PACKAGE}.core.ui.theme.calmPressScale",
                1,
            )
            t = t.replace(
                "        modifier = modifier\n            .height(36.dp)\n            .clip(CircleShape)",
                "        modifier = modifier\n            .calmPressScale(pressedScale = 0.92f)\n            .height(36.dp)\n            .clip(CircleShape)",
                1,
            )
            spd.write_text(t, encoding="utf-8")

    # 5. DecoderSelectorView.kt -> add .calmPressScale(0.96f) to DecoderModeRow
    dec = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DecoderSelectorView.kt"
    if dec.exists():
        t = dec.read_text(encoding="utf-8", errors="ignore")
        if "calmPressScale" not in t:
            t = t.replace(
                f"import {NEW_PACKAGE}.core.ui.R",
                f"import {NEW_PACKAGE}.core.ui.R\nimport {NEW_PACKAGE}.core.ui.theme.calmPressScale",
                1,
            )
            t = t.replace(
                "        modifier = Modifier\n            .fillMaxWidth()\n            .clip(shape = RoundedCornerShape(18.dp))",
                "        modifier = Modifier\n            .fillMaxWidth()\n            .calmPressScale(pressedScale = 0.97f)\n            .clip(shape = RoundedCornerShape(18.dp))",
                1,
            )
            dec.write_text(t, encoding="utf-8")

    # 6. QuickSettingsDialog.kt -> add smooth exit animation + calmPressScale on CalmIconTile & CalmCheckCell
    qs = project_root / "feature/videopicker/src/main/java" / NEW_PACKAGE_PATH / "feature/videopicker/composables/QuickSettingsDialog.kt"
    if qs.exists():
        t = qs.read_text(encoding="utf-8", errors="ignore")
        if "calmPressScale" not in t:
            t = t.replace(
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs",
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs\nimport {NEW_PACKAGE}.core.ui.theme.calmPressScale\nimport androidx.compose.animation.scaleOut\nimport androidx.compose.runtime.rememberCoroutineScope\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch",
                1,
            )
            t = t.replace(
                "    var entered by remember { mutableStateOf(false) }",
                "    var entered by remember { mutableStateOf(false) }\n    val scope = rememberCoroutineScope()\n    val smoothDismiss: () -> Unit = { scope.launch { entered = false; delay(120); onDismiss() } }",
                1,
            )
            t = t.replace("onDismissRequest = onDismiss,", "onDismissRequest = smoothDismiss,", 1)
            t = t.replace(".clickable(onClick = onDismiss)", ".calmPressScale(0.93f).clickable(onClick = smoothDismiss)", 1)
            t = t.replace(
                """                                .clickable {
                                    updatePreferences(preferences)
                                    onDismiss()
                                }""",
                """                                .calmPressScale(0.93f)
                                .clickable {
                                    updatePreferences(preferences)
                                    smoothDismiss()
                                }""",
                1,
            )
            t = t.replace(
                "                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f),\n                ),\n        ) {",
                "                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f),\n                ),\n            exit = fadeOut(tween(115)) + scaleOut(targetScale = 0.90f, animationSpec = tween(115)),\n        ) {",
                1,
            )
            t = t.replace(
                "        modifier = modifier\n            .clip(RoundedCornerShape(14.dp))",
                "        modifier = modifier\n            .calmPressScale(pressedScale = 0.92f)\n            .clip(RoundedCornerShape(14.dp))",
                1,
            )
            t = t.replace(
                "        modifier = modifier\n            .clip(RoundedCornerShape(10.dp))",
                "        modifier = modifier\n            .calmPressScale(pressedScale = 0.94f)\n            .clip(RoundedCornerShape(10.dp))",
                1,
            )
            qs.write_text(t, encoding="utf-8")

    # 7. DarkPlayerSettingsDialogs.kt -> smooth exit animation on DarkModalFrame
    dlg = project_root / "core/ui/src/main/java" / NEW_PACKAGE_PATH / "core/ui/theme/DarkPlayerSettingsDialogs.kt"
    if dlg.exists():
        t = dlg.read_text(encoding="utf-8", errors="ignore")
        if "scaleOut(" not in t:
            t = t.replace(
                "import androidx.compose.animation.scaleIn",
                "import androidx.compose.animation.fadeOut\nimport androidx.compose.animation.scaleIn\nimport androidx.compose.animation.scaleOut\nimport androidx.compose.runtime.rememberCoroutineScope\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch",
                1,
            )
            t = t.replace(
                "    var entered by remember { mutableStateOf(false) }",
                "    var entered by remember { mutableStateOf(false) }\n    val scope = rememberCoroutineScope()\n    val smoothDismiss: () -> Unit = { scope.launch { entered = false; delay(120); onDismiss() } }",
                1,
            )
            t = t.replace("        onDismissRequest = onDismiss,", "        onDismissRequest = smoothDismiss,", 1)
            t = t.replace(
                "                slideInVertically(initialOffsetY = { it / 12 }, animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f)),\n        ) {",
                "                slideInVertically(initialOffsetY = { it / 12 }, animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f)),\n            exit = fadeOut(tween(115)) + scaleOut(targetScale = 0.90f, animationSpec = tween(115)),\n        ) {",
                1,
            )
            dlg.write_text(t, encoding="utf-8")

    # 8. AboutPreferencesScreen.kt -> staggered calmEntrance + calmPressScale
    abt = project_root / "feature/settings/src/main/java" / NEW_PACKAGE_PATH / "settings/screens/about/AboutPreferencesScreen.kt"
    if abt.exists():
        t = abt.read_text(encoding="utf-8", errors="ignore")
        if "calmEntrance" not in t:
            t = t.replace(
                f"import {NEW_PACKAGE}.core.ui.R",
                f"import {NEW_PACKAGE}.core.ui.R\nimport {NEW_PACKAGE}.core.ui.theme.calmEntrance\nimport {NEW_PACKAGE}.core.ui.theme.calmPressScale",
                1,
            )
            t = t.replace(
                "            // Editorial Borderless Hero Card\n            Column(\n                modifier = Modifier\n                    .fillMaxWidth()",
                "            // Editorial Borderless Hero Card\n            Column(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .calmEntrance(delayMillis = 0)",
                1,
            )
            t = t.replace(
                "            // 2x2 Borderless Capability Tiles\n            Row(\n                modifier = Modifier.fillMaxWidth(),",
                "            // 2x2 Borderless Capability Tiles\n            Row(\n                modifier = Modifier.fillMaxWidth().calmEntrance(delayMillis = 55),",
                1,
            )
            t = t.replace(
                "            // Borderless Actions Card\n            Column(\n                modifier = Modifier\n                    .fillMaxWidth()",
                "            // Borderless Actions Card\n            Column(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .calmEntrance(delayMillis = 110)",
                1,
            )
            t = t.replace(
                "        modifier = modifier\n            .clip(RoundedCornerShape(20.dp))",
                "        modifier = modifier\n            .calmPressScale(pressedScale = 0.95f)\n            .clip(RoundedCornerShape(20.dp))",
                1,
            )
            t = t.replace(
                "        modifier = Modifier\n            .fillMaxWidth()\n            .clickable(onClick = onClick)",
                "        modifier = Modifier\n            .fillMaxWidth()\n            .calmPressScale(pressedScale = 0.97f)\n            .clickable(onClick = onClick)",
                1,
            )
            abt.write_text(t, encoding="utf-8")

    # 9. SettingsScreen.kt -> calmEntrance on content + calmPressScale on clickable rows
    stg = project_root / "feature/settings/src/main/java" / NEW_PACKAGE_PATH / "settings/SettingsScreen.kt"
    if stg.exists():
        t = stg.read_text(encoding="utf-8", errors="ignore")
        if "calmPressScale" not in t:
            t = t.replace(
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs",
                f"import {NEW_PACKAGE}.core.ui.theme.DarkPlayerUiPrefs\nimport {NEW_PACKAGE}.core.ui.theme.calmEntrance\nimport {NEW_PACKAGE}.core.ui.theme.calmPressScale",
                1,
            )
            t = t.replace(".clickable(onClick = onClick)", ".calmPressScale(pressedScale = 0.97f).clickable(onClick = onClick)")
            t = t.replace(".clickable {", ".calmPressScale(pressedScale = 0.97f).clickable {")
            stg.write_text(t, encoding="utf-8")

    # 10. Bump version to 0.23.0-calm (versionCode 80)
    app_gradle = project_root / "app/build.gradle.kts"
    if app_gradle.exists():
        gt = app_gradle.read_text(encoding="utf-8", errors="ignore")
        gt = gt.replace("versionCode = 79", "versionCode = 80")
        gt = gt.replace('versionName = "0.22.0-calm"', 'versionName = "0.23.0-calm"')
        app_gradle.write_text(gt, encoding="utf-8")




def apply_v9_seek_fix(project_root: Path):
    """Auto-upgrades unseekable MKV/MP4 files (missing Cues) to HW+ (FFmpeg demuxer) and enables CLOSEST_SYNC fast seeking."""
    ps_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/service/PlayerService.kt"
    if ps_file.exists():
        pt = ps_file.read_text(encoding="utf-8", errors="ignore")
        if "autoSeekFallbackIds" not in pt:
            pt = pt.replace(
                "    private var ffmpegReaderFailed = false",
                "    private var ffmpegReaderFailed = false\n    private val autoSeekFallbackIds = mutableSetOf<String>()",
                1,
            )
            pt = pt.replace(
                "            val currentMediaItem = player.currentMediaItem ?: return\n            val duration = player.duration.validDurationOrNull()",
                """            val currentMediaItem = player.currentMediaItem ?: return
            val duration = player.duration.validDurationOrNull()

            // Auto-upgrade unseekable MKV/MP4 files (e.g. missing Cues table) to HW+ (FFmpeg demuxer)
            if (!player.isCurrentMediaItemSeekable &&
                !player.isCurrentMediaItemLive &&
                !ffmpegReaderFailed &&
                !currentMediaItem.usesFfmpegExtractor &&
                currentMediaItem.copy(videoDecoderMode = DecoderMode.HW_PLUS).usesFfmpegExtractor &&
                autoSeekFallbackIds.add(currentMediaItem.mediaId)
            ) {
                val resumePos = player.currentPosition.coerceAtLeast(0L)
                val wasPlaying = player.playWhenReady
                val index = player.currentMediaItemIndex
                val upgraded = currentMediaItem.copy(
                    videoDecoderMode = DecoderMode.HW_PLUS,
                    ffmpegExtraction = true,
                    durationMs = duration ?: 0L,
                )
                player.replaceMediaItem(index, upgraded)
                player.seekTo(index, resumePos)
                player.prepare()
                player.playWhenReady = wasPlaying
                publishDecoderState()
                return
            }""",
                1,
            )
            pt = pt.replace(
                "            .setHandleAudioBecomingNoisy(playerPreferences.pauseOnHeadsetDisconnect)",
                "            .setHandleAudioBecomingNoisy(playerPreferences.pauseOnHeadsetDisconnect)\n            .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)",
                1,
            )
            ps_file.write_text(pt, encoding="utf-8")

    app_gradle = project_root / "app/build.gradle.kts"
    if app_gradle.exists():
        gt = app_gradle.read_text(encoding="utf-8", errors="ignore")
        gt = gt.replace("versionCode = 80", "versionCode = 81")
        gt = gt.replace('versionName = "0.23.0-calm"', 'versionName = "0.24.0-calm"')
        app_gradle.write_text(gt, encoding="utf-8")




V10_RADIO_TEXT_BUTTON_KT = """package com.mayank.darkplayer.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.theme.calmPressScale

@Composable
fun RadioTextButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val rowBg by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(140),
        label = "radio_row_bg",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .calmPressScale(pressedScale = 0.97f)
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = accent,
                unselectedColor = LocalContentColor.current.copy(alpha = 0.72f),
            ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = if (selected) accent else LocalContentColor.current,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
"""


def apply_v10_bug_fixes(project_root: Path):
    """Fixes ProCircleButton dark-mode icon visibility, OverlayView dark-glass text contrast, RadioTextButton highlight, Subtitle Delay padding, and CI GitHub Release Auto-Updater."""

    # 1. Fix ProCircleButton icon contrast in Dark mode (DarkPlayerProTools.kt)
    pro_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/DarkPlayerProTools.kt"
    if pro_file.exists():
        pt = pro_file.read_text(encoding="utf-8", errors="ignore")
        pt = pt.replace(
            "val fgColor = if (active) MaterialTheme.colorScheme.onPrimary else PlayerControlPillIcon",
            "val fgColor = if (active) MaterialTheme.colorScheme.onPrimary else if (isDarkBtn) Color(0xFFF4EFE8) else Color(0xFF161B17)",
            1,
        )
        pro_file.write_text(pt, encoding="utf-8")

    # 2. Force crisp dark-glass colorScheme + LocalContentColor inside OverlayView.kt so Audio/Subtitle/Playlist/Chapters NEVER render black text on dark sheet
    ov_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/OverlayView.kt"
    if ov_file.exists():
        ot = ov_file.read_text(encoding="utf-8", errors="ignore")
        if "LocalContentColor provides Color(0xFFF4EFE8)" not in ot:
            ot = ot.replace(
                "import androidx.compose.material3.MaterialTheme",
                "import androidx.compose.material3.LocalContentColor\nimport androidx.compose.material3.MaterialTheme\nimport androidx.compose.runtime.CompositionLocalProvider",
                1,
            )
            ot = ot.replace(
                "                Spacer(Modifier.height(8.dp))\n                content()",
                """                Spacer(Modifier.height(8.dp))
                val overlayDarkScheme = MaterialTheme.colorScheme.copy(
                    onSurface = Color(0xFFF4EFE8),
                    onSurfaceVariant = Color(0xFFB4BEB6),
                    surface = Color(0xFF141916),
                    surfaceContainer = Color(0xFF1C231F),
                    surfaceContainerHigh = Color(0xFF232B26),
                    surfaceContainerHighest = Color(0xFF2B342E),
                    outline = Color(0xFF4A564D),
                    outlineVariant = Color(0xFF333D36),
                )
                MaterialTheme(colorScheme = overlayDarkScheme) {
                    CompositionLocalProvider(LocalContentColor provides Color(0xFFF4EFE8)) {
                        content()
                    }
                }""",
                1,
            )
            ov_file.write_text(ot, encoding="utf-8")

    # 3. Upgrade RadioTextButton.kt with tonal highlight, crisp contrast & calmPressScale
    rtb_file = project_root / "core/ui/src/main/java" / NEW_PACKAGE_PATH / "core/ui/components/RadioTextButton.kt"
    if rtb_file.exists():
        rtb_file.write_text(V10_RADIO_TEXT_BUTTON_KT, encoding="utf-8")

    # 4. Add bottom breathing room in SubtitleSelectorView.kt so Delay/Speed inputs never clip
    sub_file = project_root / "feature/player/src/main/java" / NEW_PACKAGE_PATH / "feature/player/ui/SubtitleSelectorView.kt"
    if sub_file.exists():
        st = sub_file.read_text(encoding="utf-8", errors="ignore")
        st = st.replace(".verticalScroll(state = rememberScrollState())", ".verticalScroll(state = rememberScrollState()).padding(bottom = 24.dp)")
        sub_file.write_text(st, encoding="utf-8")

    # 5. Dynamic v3.0.<run_number> versioning in app/build.gradle.kts (higher than old v2.5.59 releases!)
    app_gradle = project_root / "app/build.gradle.kts"
    if app_gradle.exists():
        gt = app_gradle.read_text(encoding="utf-8", errors="ignore")
        gt = gt.replace("versionCode = 81", "versionCode = 100 + (System.getenv(\"GITHUB_RUN_NUMBER\")?.toIntOrNull() ?: 88)")
        gt = gt.replace('versionName = "0.24.0-calm"', 'versionName = "3.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "88"}"')
        app_gradle.write_text(gt, encoding="utf-8")

    # 6. Upgrade .github/workflows/build.yml to auto-publish GitHub Release (v3.0.<run_number> + DarkPlayer.apk) for live Auto-Updater
    wf_file = project_root / ".github/workflows/build.yml"
    if wf_file.exists():
        wt = wf_file.read_text(encoding="utf-8", errors="ignore")
        if "permissions:" not in wt:
            wt = wt.replace(
                "concurrency:",
                "permissions:\n  contents: write\n\nconcurrency:",
                1,
            )
        if "Publish GitHub Release" not in wt:
            wt += """
      - name: Publish GitHub Release for Auto-Updater
        if: github.ref == 'refs/heads/main' && github.event_name != 'pull_request'
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          TAG="v3.0.${{ github.run_number }}"
          gh release delete "$TAG" --yes 2>/dev/null || true
          gh release create "$TAG" DarkPlayer.apk \\
            --title "Dark Player $TAG" \\
            --notes "Dark Player automated release build $TAG (Calm Sage UI, Liquid Glass NavBar, Global Motion, Auto HW+ MKV Seeking)." \\
            --latest
"""
        wf_file.write_text(wt, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Automated Next Player -> Dark Player transformation & repackager.",
    )
    parser.add_argument(
        "source",
        help="Path to the Next Player source directory or nextplayer-main.zip archive.",
    )
    parser.add_argument(
        "--logo",
        default=None,
        help="Path to the custom Dark Player PNG/WEBP logo (auto-detects logo.png / 34655.png).",
    )
    parser.add_argument(
        "--svg-zip",
        default=None,
        help="Path to outline.zip containing SVG icons to convert into Android VectorDrawables.",
    )
    parser.add_argument(
        "--font",
        default=None,
        help="Path to the custom .ttf font file to apply globally across Compose & XML.",
    )
    parser.add_argument(
        "--output",
        default="DarkPlayer.zip",
        help="Output ZIP path (default: DarkPlayer.zip).",
    )
    parser.add_argument(
        "--repo",
        default=DEFAULT_GITHUB_REPO,
        help=f"GitHub owner/repo for Releases Auto-Updater & README (default: {DEFAULT_GITHUB_REPO}).",
    )
    parser.add_argument(
        "--purge-external-maven",
        action="store_true",
        help="Also rewrite third-party io.github.anilbeesetti:nextlib Maven coordinates (use only with a forked nextlib).",
    )
    args = parser.parse_args()

    source_path = Path(args.source).expanduser().resolve()
    output_zip = Path(args.output).expanduser().resolve()
    logo_path = auto_detect_asset(args.logo, ("34655.png", "logo.png", "darkplayer_logo.png"))
    font_path = auto_detect_asset(args.font, ("*.ttf",))
    svg_zip_path = auto_detect_asset(args.svg_zip, ("outline.zip",))

    print(f"[1/7] Preparing workspace from: {source_path}")
    with tempfile.TemporaryDirectory(prefix="darkplayer_build_") as tmp:
        staging = Path(tmp)
        project_root = resolve_input_source(source_path, staging)

        print("[2/7] Pruning upstream metadata & applying zero-trace regex renames...")
        prune_upstream_metadata(project_root)
        renamed_files_text = apply_global_renames(
            project_root,
            purge_external_maven=args.purge_external_maven,
        )

        print("[3/7] Migrating package trees (dev.anilbeesetti.nextplayer -> com.mayank.darkplayer)...")
        moved_trees = migrate_package_directories(project_root)
        renamed_filenames = rename_brand_files(project_root)

        print("[4/7] Injecting custom logo & global .ttf typography assets...")
        logo_ok = inject_logo_assets(project_root, logo_path)
        font_ok = inject_custom_font(project_root, font_path)
        svg_count = inject_svg_icons(project_root, svg_zip_path)
        print(f"      SVG icons converted to VectorDrawables: {svg_count}")
        patch_themes_and_manifest(project_root, has_custom_font=font_ok)

        print("[5/7] Writing Navy Dark UI, Liquid Glass NavBar, Player Fixes & Auto-Updater...")
        generated = write_generated_sources(
            project_root=project_root,
            has_custom_font=font_ok,
            github_repo=args.repo,
        )
        patch_top_level_navigation(project_root)
        patch_gradle_files(project_root)
        patch_app_preferences_and_gestures(project_root)
        tools_dir = project_root / "tools"
        tools_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(Path(__file__).resolve(), tools_dir / "build_darkplayer.py")

        print("[6/7] Running Zero-Trace brand audit...")
        patch_v4_upgrades(project_root)
        patch_v5_fixes(project_root)
        patch_v6_polish(project_root)
        apply_v7_calm_polish(project_root)
        apply_v8_global_motion(project_root)
        apply_v9_seek_fix(project_root)
        apply_v10_bug_fixes(project_root)
        fp_hits, maven_hits = audit_zero_trace(
            project_root,
            purge_external_maven=args.purge_external_maven,
        )
        print(
            f"      Files updated: {renamed_files_text} | Package trees moved: {moved_trees} "
            f"| Brand files renamed: {renamed_filenames} | Generated files: {len(generated)}"
        )
        print(
            f"      Logo injected: {logo_ok} | Custom TTF injected: {font_ok} "
            f"| First-party traces remaining: {fp_hits} "
            f"| External nextlib Maven refs preserved: {maven_hits}"
        )
        if fp_hits > 0:
            print("WARNING: Non-zero first-party trace count detected!", file=sys.stderr)

        print(f"[7/7] Packaging final project into: {output_zip}")
        package_zip(project_root, output_zip)

    print(f"SUCCESS: Built {output_zip} ({output_zip.stat().st_size / (1024 * 1024):.2f} MB)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())