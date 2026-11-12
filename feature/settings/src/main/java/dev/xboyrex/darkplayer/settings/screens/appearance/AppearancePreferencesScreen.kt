package dev.xboyrex.darkplayer.settings.screens.appearance

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.model.MediaListCardStyle
import dev.xboyrex.darkplayer.core.model.MxAppThemeMode
import dev.xboyrex.darkplayer.core.model.NovaNavBarStyle
import dev.xboyrex.darkplayer.core.model.NovaSwitchStyle
import dev.xboyrex.darkplayer.core.model.ThemeConfig
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.PreferenceSlider
import dev.xboyrex.darkplayer.core.ui.components.PreferenceSwitch
import dev.xboyrex.darkplayer.core.ui.components.RadioTextButton
import dev.xboyrex.darkplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.darkplayer.core.ui.components.tvFocusDown
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoGradientBrush
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaSegmentedControl
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
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
    val listFocusRequester = rememberTvListFocusRequester()
    val accent = LocalNovaAccentColor.current
    val isPureWhiteMode = state.preferences.mxAppThemeMode == MxAppThemeMode.LIGHT_THEME

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = "Theme & Interface Switcher",
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
        containerColor = MaterialTheme.colorScheme.background,
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
            // 1. Strictly 2 Color Modes (AMOLED Pitch Black vs Pure White) + 2 Accent Palettes (Logo Gradient vs Device Dynamic)
            ListSectionTitle(text = "Color Mode & Accent Palette")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Background Mode (2 Options)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NovaThemeBannerCard(
                            title = "AMOLED Black",
                            subtitle = "Pure #000000 OLED",
                            selected = !isPureWhiteMode,
                            brush = Brush.linearGradient(listOf(Color(0xFF0A0B10), Color(0xFF000000))),
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
                        NovaThemeBannerCard(
                            title = "Pure White",
                            subtitle = "Clean Daylight UI",
                            selected = isPureWhiteMode,
                            brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF2F4F8))),
                            textColor = Color(0xFF0A0B10),
                            accent = accent,
                            onClick = {
                                onAction(
                                    AppearancePreferencesEvent.UpdatePreferences {
                                        it.copy(
                                            mxAppThemeMode = MxAppThemeMode.LIGHT_THEME,
                                            themeConfig = ThemeConfig.OFF,
                                            useHighContrastDarkTheme = false,
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text(
                        text = "Accent Color Source (2 Options)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NovaThemeBannerCard(
                            title = "Logo Gradient",
                            subtitle = "Blue • Violet • Pink",
                            selected = !state.preferences.useDynamicColors,
                            brush = NovaLogoGradientBrush,
                            textColor = Color.White,
                            accent = Color.White,
                            onClick = {
                                onAction(
                                    AppearancePreferencesEvent.UpdatePreferences {
                                        it.copy(
                                            useDynamicColors = false,
                                            mxClassicThemeHex = "#7C3AED",
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        NovaThemeBannerCard(
                            title = "Device Accent",
                            subtitle = "System Dynamic Color",
                            selected = state.preferences.useDynamicColors,
                            brush = Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ),
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = {
                                onAction(
                                    AppearancePreferencesEvent.UpdatePreferences {
                                        it.copy(useDynamicColors = true)
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 2. Compact Minimal UI Switcher Card
            ListSectionTitle(text = "Navigation Dock & Control Styles")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Navigation Dock Style",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    NovaSegmentedControl(
                        items = listOf(
                            NovaNavBarStyle.YTXTUBE_LIQUID_GLASS to "Floating Dock",
                            NovaNavBarStyle.GLASMIO_IOS27_MORPH to "Orbit Pill",
                            NovaNavBarStyle.MX_PRO_CLASSIC to "Classic Bar",
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
                        text = "Toggle Switch Style",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    NovaSegmentedControl(
                        items = listOf(
                            NovaSwitchStyle.GLASMIO_LIQUID to "Fluid Switch",
                            NovaSwitchStyle.MX_PRO_CLASSIC to "Compact Pill",
                            NovaSwitchStyle.MATERIAL_EXPRESSIVE to "Standard",
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
                    title = "Navigation Dock Footer Lift",
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
                    title = "Compact Minimal Folder Rows",
                    description = "Clean flat folder & video rows without path clutter",
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
                    isLastItem = true,
                )
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
private fun NovaThemeBannerCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    brush: Brush,
    textColor: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color.Gray.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = textColor,
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.78f),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
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
