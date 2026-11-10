package dev.xboyrex.darkplayer.settings.screens.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.model.ThemeConfig
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.PreferenceSwitch
import dev.xboyrex.darkplayer.core.ui.components.PreferenceSwitchWithDivider
import dev.xboyrex.darkplayer.core.ui.components.RadioTextButton
import dev.xboyrex.darkplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.darkplayer.core.ui.components.tvFocusDown
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.MxClassicThemeSwatches
import dev.xboyrex.darkplayer.core.ui.glass.NavBarStyle
import dev.xboyrex.darkplayer.core.ui.glass.SwitchStyle
import dev.xboyrex.darkplayer.core.ui.glass.UiStylePreferences
import dev.xboyrex.darkplayer.core.ui.glass.VolumeHudStyle
import dev.xboyrex.darkplayer.core.ui.glass.popupBackgroundBlur
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.core.ui.theme.DARK_PLAYER_PALETTES
import dev.xboyrex.darkplayer.core.ui.theme.DarkPlayerTheme
import dev.xboyrex.darkplayer.core.ui.theme.schemeForSeed
import dev.xboyrex.darkplayer.core.ui.theme.supportsDynamicTheming
import dev.xboyrex.darkplayer.settings.composables.OptionsDialog
import dev.xboyrex.darkplayer.settings.extensions.name
import kotlin.math.roundToInt

@Composable
fun AppearancePreferencesScreen(viewModel: AppearancePreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppearancePreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun AppearancePreferencesScreenContent(
    state: AppearancePreferencesUiState,
    onAction: (AppearancePreferencesEvent) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        UiStylePreferences.init(context)
        GlassSettings.init(context)
    }

    val listFocusRequester = rememberTvListFocusRequester()
    val c = MaterialTheme.colorScheme
    val systemDark = isSystemInDarkTheme()
    val isDark = when (state.preferences.themeConfig) {
        ThemeConfig.SYSTEM -> systemDark
        ThemeConfig.ON -> true
        ThemeConfig.OFF -> false
    }

    Scaffold(
        modifier = Modifier.popupBackgroundBlur(),
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.appearance_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AppearancePreferencesEvent.NavigateUp)
                        },
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(focusRequester = listFocusRequester)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            ListSectionTitle(text = "ACCENT COLOR PALETTE & MX CLASSIC SWATCHES")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.surfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = c.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Signature Nova Player Palettes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                )
                Text(
                    text = "Top 10 signature Nova Player color combinations",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DARK_PLAYER_PALETTES.forEach { palette ->
                        val selected = !state.preferences.useDynamicColors &&
                            state.preferences.themeAccentSeed.equals(palette.hex, ignoreCase = true)
                        val scheme = schemeForSeed(
                            hex = palette.hex,
                            darkTheme = isDark,
                            highContrast = state.preferences.useHighContrastDarkTheme,
                        )
                        Column(
                            modifier = Modifier
                                .width(68.dp)
                                .pressable {
                                    onAction(
                                        AppearancePreferencesEvent.UpdateThemeAccentSeed(palette.hex),
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) scheme.primaryContainer else Color.Transparent)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(scheme.primary, scheme.primaryContainer),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = scheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = palette.name,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) c.primary else c.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "MX Player Pro Classic Theme Swatches",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MxClassicThemeSwatches.forEachIndexed { index, swatchColor ->
                        val isSelected = UiStylePreferences.mxSwatchIndex == index
                        Box(
                            modifier = Modifier
                                .size(width = 58.dp, height = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 2.2.dp else 0.8.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable {
                                    UiStylePreferences.updateMxSwatchIndex(index)
                                    val r = (swatchColor.red * 255).roundToInt().coerceIn(0, 255)
                                    val g = (swatchColor.green * 255).roundToInt().coerceIn(0, 255)
                                    val b = (swatchColor.blue * 255).roundToInt().coerceIn(0, 255)
                                    val hexStr = String.format("#%02X%02X%02X", r, g, b)
                                    onAction(AppearancePreferencesEvent.UpdateThemeAccentSeed(hexStr))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Theme Swatch",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            ListSectionTitle(text = "NAVIGATION BAR DESIGN & FOOTER LIFT")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                NavBarStyle.entries.forEach { style ->
                    val selected = UiStylePreferences.navBarStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { UiStylePreferences.updateNavBarStyle(style) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { UiStylePreferences.updateNavBarStyle(style) },
                            colors = RadioButtonDefaults.colors(selectedColor = c.primary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = style.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = c.onSurface,
                            )
                            Text(
                                text = style.subtitle,
                                fontSize = 11.5.sp,
                                color = c.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Navigation Bar Lift from Footer",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = c.onSurface,
                    )
                    Text(
                        text = "${UiStylePreferences.navBarBottomOffsetDp} dp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = c.primary,
                    )
                }
                Slider(
                    value = UiStylePreferences.navBarBottomOffsetDp.toFloat(),
                    onValueChange = { UiStylePreferences.updateNavBarBottomOffsetDp(it.roundToInt()) },
                    valueRange = 6f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = c.primary,
                        activeTrackColor = c.primary,
                    ),
                )
            }

            ListSectionTitle(text = "VOLUME & BRIGHTNESS HUD DESIGN")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VolumeHudStyle.entries.forEach { style ->
                    val selected = UiStylePreferences.volumeHudStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { UiStylePreferences.updateVolumeHudStyle(style) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { UiStylePreferences.updateVolumeHudStyle(style) },
                            colors = RadioButtonDefaults.colors(selectedColor = c.primary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = style.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = c.onSurface,
                            )
                            Text(
                                text = style.subtitle,
                                fontSize = 11.5.sp,
                                color = c.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            ListSectionTitle(text = "SWITCH TOGGLE UI DESIGN")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SwitchStyle.entries.forEach { style ->
                    val selected = UiStylePreferences.switchStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { UiStylePreferences.updateSwitchStyle(style) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { UiStylePreferences.updateSwitchStyle(style) },
                            colors = RadioButtonDefaults.colors(selectedColor = c.primary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = style.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = c.onSurface,
                            )
                            Text(
                                text = style.subtitle,
                                fontSize = 11.5.sp,
                                color = c.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            ListSectionTitle(text = stringResource(id = R.string.appearance_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dark_theme),
                    description = state.preferences.themeConfig.name(),
                    isChecked = state.preferences.themeConfig == ThemeConfig.ON,
                    onChecked = { onAction(AppearancePreferencesEvent.ToggleDarkTheme) },
                    icon = NextIcons.DarkMode,
                    onClick = {
                        onAction(
                            AppearancePreferencesEvent.ShowDialog(
                                value = AppearancePreferenceDialog.Theme,
                            ),
                        )
                    },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(R.string.high_contrast_dark_theme),
                    description = stringResource(R.string.high_contrast_dark_theme_desc),
                    icon = NextIcons.Contrast,
                    isChecked = state.preferences.useHighContrastDarkTheme,
                    onClick = {
                        onAction(AppearancePreferencesEvent.ToggleUseHighContrastDarkTheme)
                    },
                )
                PreferenceSwitch(
                    title = "Liquid Glass Navigation Bar",
                    description = "Toggle between refractive Liquid Glass and Solid Navigation Bar",
                    icon = NextIcons.Style,
                    isChecked = state.preferences.useLiquidGlass,
                    onClick = {
                        GlassSettings.updateEnabled(!state.preferences.useLiquidGlass)
                        onAction(AppearancePreferencesEvent.ToggleUseLiquidGlass)
                    },
                )
                PreferenceSwitch(
                    title = "Use System Font",
                    description = "Use device default font across all screens",
                    icon = NextIcons.Font,
                    isChecked = state.preferences.useSystemFont,
                    onClick = {
                        onAction(AppearancePreferencesEvent.ToggleUseSystemFont)
                    },
                    isLastItem = !supportsDynamicTheming(),
                )
                if (supportsDynamicTheming()) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_theme),
                        description = stringResource(id = R.string.dynamic_theme_description),
                        icon = NextIcons.Appearance,
                        isChecked = state.preferences.useDynamicColors,
                        onClick = { onAction(AppearancePreferencesEvent.ToggleUseDynamicColors) },
                        isLastItem = true,
                    )
                }
            }
        }

        state.showDialog?.let { showDialog ->
            when (showDialog) {
                AppearancePreferenceDialog.Theme -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.dark_theme),
                        onDismissClick = {
                            onAction(AppearancePreferencesEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(ThemeConfig.entries.toTypedArray()) {
                            RadioTextButton(
                                text = it.name(),
                                selected = (it == state.preferences.themeConfig),
                                onClick = {
                                    onAction(
                                        AppearancePreferencesEvent.UpdateThemeConfig(
                                            themeConfig = it,
                                        ),
                                    )
                                    onAction(AppearancePreferencesEvent.ShowDialog(value = null))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AppearancePreferencesScreenPreview() {
    DarkPlayerTheme {
        AppearancePreferencesScreenContent(
            state = AppearancePreferencesUiState(),
            onAction = {},
        )
    }
}
