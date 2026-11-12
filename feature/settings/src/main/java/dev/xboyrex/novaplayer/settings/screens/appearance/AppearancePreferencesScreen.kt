package dev.xboyrex.novaplayer.settings.screens.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Vibration
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.novaplayer.core.model.ThemeConfig
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.novaplayer.core.ui.components.NovaTopAppBar
import dev.xboyrex.novaplayer.core.ui.components.PreferenceSwitch
import dev.xboyrex.novaplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.novaplayer.core.ui.components.tvFocusDown
import dev.xboyrex.novaplayer.core.ui.components.tvListFocus
import dev.xboyrex.novaplayer.core.ui.designsystem.NovaIcons
import dev.xboyrex.novaplayer.core.ui.glass.GlassSettings
import dev.xboyrex.novaplayer.core.ui.glass.NovaRoundedHeavy
import dev.xboyrex.novaplayer.core.ui.glass.novaPressable
import dev.xboyrex.novaplayer.core.ui.theme.NovaPlayerTheme
import dev.xboyrex.novaplayer.core.ui.theme.supportsDynamicTheming

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
    val c = MaterialTheme.colorScheme
    val isAmoled = state.preferences.themeConfig != ThemeConfig.OFF && GlassSettings.themeMode != "white"

    Scaffold(
        topBar = {
            NovaTopAppBar(
                title = stringResource(id = R.string.appearance_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AppearancePreferencesEvent.NavigateUp)
                        },
                        modifier = Modifier.tvFocusDown(target = listFocusRequester),
                    ) {
                        Icon(
                            imageVector = NovaIcons.ArrowBack,
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
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = "Color Mode")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = c.surfaceContainerHigh,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Strictly Two Modes: Pure AMOLED Black & Pure White",
                        fontFamily = NovaRoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Switch instantly between #000000 pitch-black OLED mode and #FFFFFF clean white mode.",
                        fontSize = 12.sp,
                        color = c.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        listOf(
                            true to "Pure AMOLED Black",
                            false to "Pure White",
                        ).forEach { (amoled, label) ->
                            val selected = isAmoled == amoled
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) c.primary else c.surfaceContainerHighest)
                                    .novaPressable {
                                        GlassSettings.updateThemeMode(if (amoled) "amoled" else "white")
                                        onAction(
                                            AppearancePreferencesEvent.UpdateThemeConfig(
                                                themeConfig = if (amoled) ThemeConfig.ON else ThemeConfig.OFF,
                                            ),
                                        )
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selected) c.onPrimary else c.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            ListSectionTitle(text = "Liquid Glass & Interface")
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = "Liquid Glass UI",
                    description = "Refractive frosted glass navigation bar, overlays & dialogs",
                    icon = Icons.Rounded.AutoAwesome,
                    isChecked = GlassSettings.enabled,
                    onClick = { GlassSettings.updateEnabled(!GlassSettings.enabled) },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = "Use System Font",
                    description = "Use device default font instead of Nova Rounded Heavy headers",
                    icon = Icons.Rounded.FontDownload,
                    isChecked = GlassSettings.useSystemFont,
                    onClick = { GlassSettings.updateUseSystemFont(!GlassSettings.useSystemFont) },
                )
                PreferenceSwitch(
                    title = "Haptic Feedback",
                    description = "Tactile vibration ticks across navigation bar, sliders & controls",
                    icon = Icons.Rounded.Vibration,
                    isChecked = GlassSettings.hapticsEnabled,
                    onClick = { GlassSettings.updateHapticsEnabled(!GlassSettings.hapticsEnabled) },
                    isLastItem = !supportsDynamicTheming(),
                )
                if (supportsDynamicTheming()) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_theme),
                        description = stringResource(id = R.string.dynamic_theme_description),
                        icon = NovaIcons.Appearance,
                        isChecked = state.preferences.useDynamicColors,
                        onClick = { onAction(AppearancePreferencesEvent.ToggleUseDynamicColors) },
                        isLastItem = true,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AppearancePreferencesScreenPreview() {
    NovaPlayerTheme {
        AppearancePreferencesScreenContent(
            state = AppearancePreferencesUiState(),
            onAction = {},
        )
    }
}
