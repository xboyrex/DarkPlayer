package dev.anilbeesetti.nextplayer.settings.screens.appearance

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.MediaListCardStyle
import dev.anilbeesetti.nextplayer.core.model.MxAppThemeMode
import dev.anilbeesetti.nextplayer.core.model.NovaNavBarStyle
import dev.anilbeesetti.nextplayer.core.model.NovaSwitchStyle
import dev.anilbeesetti.nextplayer.core.model.ThemeConfig
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSlider
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitchWithDivider
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusDown
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaAccentColor
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.supportsDynamicTheming
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog
import dev.anilbeesetti.nextplayer.settings.extensions.name
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
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
    ExperimentalLayoutApi::class,
)
@Composable
private fun AppearancePreferencesScreenContent(
    state: AppearancePreferencesUiState,
    onAction: (AppearancePreferencesEvent) -> Unit,
) {
    val listFocusRequester = rememberTvListFocusRequester()
    val accent = LocalNovaAccentColor.current
    val classicSwatches = remember {
        listOf(
            "#FF6E5B", "#F59331", "#F2C522", "#9D6644", "#B8DB18", "#5AD21F",
            "#3AC759", "#1EC980", "#429EED", "#0088FF", "#5C6BC0", "#7E57C2",
            "#D542E6", "#F04886", "#EB3B78", "#E8EAED", "#FF5252", "#FF9100",
            "#FBC02D", "#8D6E63", "#AED581", "#4CAF50", "#00E676", "#00BFA5",
            "#4FC3F7", "#1976D2", "#5E35B1", "#8E24AA", "#E040FB", "#FF4081",
        )
    }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = "App Theme & UI Studio",
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
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(focusRequester = listFocusRequester)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            // 1. MX Player Pro App Theme Modes & Classic Themes Swatch Grid (Screenshot #3 155414_MX Player Pro.png)
            ListSectionTitle(text = "MX Player Pro App Theme")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MxThemeBannerCard(
                            title = "Adaptive",
                            selected = state.preferences.mxAppThemeMode == MxAppThemeMode.ADAPTIVE,
                            brush = Brush.linearGradient(listOf(Color(0xFFB3D8FF), Color(0xFF7FA8E0))),
                            textColor = Color(0xFF1A2536),
                            accent = accent,
                            onClick = {
                                onAction(
                                    AppearancePreferencesEvent.UpdatePreferences {
                                        it.copy(mxAppThemeMode = MxAppThemeMode.ADAPTIVE)
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        MxThemeBannerCard(
                            title = "Dark Theme",
                            selected = state.preferences.mxAppThemeMode == MxAppThemeMode.DARK_THEME,
                            brush = Brush.linearGradient(listOf(Color(0xFF28344A), Color(0xFF1B2333))),
                            textColor = Color.White,
                            accent = accent,
                            onClick = {
                                onAction(
                                    AppearancePreferencesEvent.UpdatePreferences {
                                        it.copy(
                                            mxAppThemeMode = MxAppThemeMode.DARK_THEME,
                                            themeConfig = ThemeConfig.ON,
                                            useHighContrastDarkTheme = false,
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MxThemeBannerCard(
                            title = "Light Theme",
                            selected = state.preferences.mxAppThemeMode == MxAppThemeMode.LIGHT_THEME,
                            brush = Brush.linearGradient(listOf(Color(0xFFF3F6FA), Color(0xFFDCE5F2))),
                            textColor = Color(0xFF1A2536),
                            accent = accent,
                            onClick = {
                                onAction(
                                    AppearancePreferencesEvent.UpdatePreferences {
                                        it.copy(
                                            mxAppThemeMode = MxAppThemeMode.LIGHT_THEME,
                                            themeConfig = ThemeConfig.OFF,
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        MxThemeBannerCard(
                            title = "Amoled Black",
                            selected = state.preferences.mxAppThemeMode == MxAppThemeMode.AMOLED_BLACK,
                            brush = Brush.linearGradient(listOf(Color(0xFF08090C), Color(0xFF000000))),
                            textColor = Color.White,
                            accent = accent,
                            onClick = {
                                onAction(
                                    AppearancePreferencesEvent.UpdatePreferences {
                                        it.copy(
                                            mxAppThemeMode = MxAppThemeMode.AMOLED_BLACK,
                                            themeConfig = ThemeConfig.ON,
                                            useHighContrastDarkTheme = true,
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text(
                        text = "Classic Themes Accent Color",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 6,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        classicSwatches.forEach { hex ->
                            val swatchColor = remember(hex) {
                                runCatching { Color(hex.toColorInt()) }.getOrDefault(Color(0xFF0088FF))
                            }
                            val selected = state.preferences.mxClassicThemeHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(swatchColor)
                                    .border(
                                        width = if (selected) 2.5.dp else 0.5.dp,
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable {
                                        onAction(
                                            AppearancePreferencesEvent.UpdatePreferences {
                                                it.copy(mxClassicThemeHex = hex)
                                            },
                                        )
                                    },
                            )
                        }
                    }
                }
            }

            // 2. Navigation Bar & Toggle Switch Engine Selector
            ListSectionTitle(text = "Navigation Bar & Toggle Switch Engine")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Bottom Navigation Bar Design",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    GlasmioSegmentedControl(
                        items = listOf(
                            NovaNavBarStyle.YTXTUBE_LIQUID_GLASS to "YTX Liquid",
                            NovaNavBarStyle.GLASMIO_IOS27_MORPH to "Glasmio iOS",
                            NovaNavBarStyle.MX_PRO_CLASSIC to "MX Classic",
                        ),
                        selected = state.preferences.navBarStyle,
                        onSelected = { navStyle ->
                            onAction(
                                AppearancePreferencesEvent.UpdatePreferences {
                                    it.copy(navBarStyle = navStyle)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "Toggle Switch & Slider Style",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    GlasmioSegmentedControl(
                        items = listOf(
                            NovaSwitchStyle.GLASMIO_LIQUID to "Glasmio Glass",
                            NovaSwitchStyle.MX_PRO_CLASSIC to "MX Classic",
                            NovaSwitchStyle.MATERIAL_EXPRESSIVE to "Material 3",
                        ),
                        selected = state.preferences.switchStyle,
                        onSelected = { swStyle ->
                            onAction(
                                AppearancePreferencesEvent.UpdatePreferences {
                                    it.copy(switchStyle = swStyle)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSlider(
                    title = "Navigation Bar Footer Lift",
                    description = "${state.preferences.navBarFooterLiftDp} dp above bottom gesture bar",
                    icon = NextIcons.Appearance,
                    value = state.preferences.navBarFooterLiftDp.toFloat(),
                    valueRange = 8f..44f,
                    onValueChange = { lift ->
                        onAction(
                            AppearancePreferencesEvent.UpdatePreferences {
                                it.copy(navBarFooterLiftDp = lift.roundToInt().coerceIn(8, 44))
                            },
                        )
                    },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = "Liquid Glass Refraction Shaders",
                    description = "Enable real-time YTXTube & Glasmio backdrop refraction lens",
                    icon = NextIcons.Contrast,
                    isChecked = state.preferences.enableLiquidGlassEffects,
                    onClick = {
                        onAction(
                            AppearancePreferencesEvent.UpdatePreferences {
                                it.copy(enableLiquidGlassEffects = !it.enableLiquidGlassEffects)
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = "Clean MX Folder & Video Rows",
                    description = "Use flat borderless MX Player Pro rows instead of boxed cards",
                    icon = NextIcons.Movie,
                    isChecked = state.preferences.mediaListCardStyle == MediaListCardStyle.MX_PRO_CLEAN,
                    onClick = {
                        onAction(
                            AppearancePreferencesEvent.UpdatePreferences {
                                it.copy(
                                    mediaListCardStyle = if (it.mediaListCardStyle == MediaListCardStyle.MX_PRO_CLEAN) {
                                        MediaListCardStyle.SEGMENTED_CARD
                                    } else {
                                        MediaListCardStyle.MX_PRO_CLEAN
                                    },
                                )
                            },
                        )
                    },
                )
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
                )
                PreferenceSwitch(
                    title = stringResource(R.string.high_contrast_dark_theme),
                    description = stringResource(R.string.high_contrast_dark_theme_desc),
                    icon = NextIcons.Contrast,
                    isChecked = state.preferences.useHighContrastDarkTheme,
                    onClick = {
                        onAction(AppearancePreferencesEvent.ToggleUseHighContrastDarkTheme)
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

@Composable
private fun MxThemeBannerCard(
    title: String,
    selected: Boolean,
    brush: Brush,
    textColor: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2ED573))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Using",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AppearancePreferencesScreenPreview() {
    NextPlayerTheme {
        AppearancePreferencesScreenContent(
            state = AppearancePreferencesUiState(),
            onAction = {},
        )
    }
}
