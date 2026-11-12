package dev.xboyrex.darkplayer.settings.screens.player

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.common.extensions.isPipFeatureSupported
import dev.xboyrex.darkplayer.core.model.ControlButtonsPosition
import dev.xboyrex.darkplayer.core.model.MxQuickShortcut
import dev.xboyrex.darkplayer.core.model.PlayerControlStyle
import dev.xboyrex.darkplayer.core.model.PlayerPreferences
import dev.xboyrex.darkplayer.core.model.Resume
import dev.xboyrex.darkplayer.core.model.ScreenOrientation
import dev.xboyrex.darkplayer.core.model.VolumeBrightnessBarStyle
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.ClickablePreferenceItem
import dev.xboyrex.darkplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.PreferenceSlider
import dev.xboyrex.darkplayer.core.ui.components.PreferenceSwitch
import dev.xboyrex.darkplayer.core.ui.components.RadioTextButton
import dev.xboyrex.darkplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.darkplayer.core.ui.components.tvFocusDown
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.preview.DayNightPreview
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.settings.composables.OptionsDialog
import dev.xboyrex.darkplayer.settings.extensions.name
import dev.xboyrex.darkplayer.settings.screens.player.PlayerPreferencesUiEvent.UpdatePreferredControlButtonsPosition
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaSegmentedControl
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaFluidSlider
import kotlin.math.roundToInt

@Composable
fun PlayerPreferencesScreen(viewModel: PlayerPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PlayerPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

/**
 * MX Player Pro Player Settings Screen + Dialogs
 * Implements Screenshots #5 (`155435`), #6 (`155438`), #7 (`155442`), #8 (`155451`), #9 (`155452`), #10 (`155458`), #11 (`155507`), #12 (`155520`)
 * AND fixes Bug #5, #6, #7, #8 (`153552`–`153558_Nova Player.png`).
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
private fun PlayerPreferencesScreenContent(
    state: PlayerPreferencesUiState,
    onAction: (PlayerPreferencesUiEvent) -> Unit,
) {
    val listFocusRequester = rememberTvListFocusRequester()
    val accent = LocalNovaAccentColor.current

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.player_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(PlayerPreferencesUiEvent.NavigateUp)
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
            ListSectionTitle(text = stringResource(id = R.string.interface_name))

            // Player Style & Volume/Brightness Bar Style Segmented Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Player Interface Style",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    NovaSegmentedControl(
                        items = listOf(
                            PlayerControlStyle.MX_PRO to "Nova Pro",
                            PlayerControlStyle.LIQUID_GLASS to "Liquid Glass",
                            PlayerControlStyle.CLASSIC_NEXT to "Classic Next",
                        ),
                        selected = state.preferences.playerControlStyle,
                        onSelected = { pStyle ->
                            onAction(
                                PlayerPreferencesUiEvent.UpdatePreferences {
                                    it.copy(playerControlStyle = pStyle)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "Volume & Brightness Bar Style",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    NovaSegmentedControl(
                        items = listOf(
                            VolumeBrightnessBarStyle.MX_PRO_VERTICAL to "MX Pro Bar",
                            VolumeBrightnessBarStyle.GLASMIO_LIQUID_CAPSULE to "Fluid Capsule",
                            VolumeBrightnessBarStyle.NEXT_VERTICAL_PILL to "Minimal Pill",
                        ),
                        selected = state.preferences.volumeBrightnessBarStyle,
                        onSelected = { barStyle ->
                            onAction(
                                PlayerPreferencesUiEvent.UpdatePreferences {
                                    it.copy(volumeBrightnessBarStyle = barStyle)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // MX Player Pro Interface Rows: Theme, Screen, Controls, Navigation, Double-tap back, Quick zoom, Controller timeout
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = "Theme",
                    description = "Player controls accent & surface theme (${state.preferences.playerAccentHex})",
                    icon = NextIcons.Appearance,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.ShowDialog(
                                PlayerPreferenceDialog.MxPlayerThemeDialog,
                            ),
                        )
                    },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = "Screen",
                    description = "Screen settings: Orientation, Full Screen, Brightness, Battery/clock, etc.",
                    icon = NextIcons.Rotation,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.ShowDialog(
                                PlayerPreferenceDialog.MxScreenDialog,
                            ),
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = "Controls",
                    description = "Input controls: Touch actions, gestures, lock mode, Quick Shortcuts, etc.",
                    icon = NextIcons.Player,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.ShowDialog(
                                PlayerPreferenceDialog.MxControlsDialog,
                            ),
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = "Navigation",
                    description = "Navigation settings: seeking settings, forward/backward buttons, etc.",
                    icon = NextIcons.SwipeHorizontal,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.ShowDialog(
                                PlayerPreferenceDialog.MxNavigationDialog,
                            ),
                        )
                    },
                )
                PreferenceSwitch(
                    title = "Double-tap the back button",
                    description = "Press the back button twice to close playback screen.",
                    icon = NextIcons.DoubleTap,
                    isChecked = state.preferences.doubleTapBackToClose,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.UpdatePreferences {
                                it.copy(doubleTapBackToClose = !it.doubleTapBackToClose)
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = "Quick zoom",
                    description = "Skip zoom steps between 100% and 'fit to screen' (150%, 200%, etc).",
                    icon = NextIcons.Pinch,
                    isChecked = state.preferences.quickZoom,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.UpdatePreferences {
                                it.copy(quickZoom = !it.quickZoom)
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.material_you_controls),
                    description = stringResource(id = R.string.material_you_controls_description),
                    icon = NextIcons.Appearance,
                    isChecked = state.preferences.useMaterialYouControls,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleUseMaterialYouControls) },
                )
                PreferenceSlider(
                    title = stringResource(R.string.controller_timeout),
                    description = if (state.preferences.controllerAutoHideTimeout == 0) {
                        stringResource(R.string.off)
                    } else {
                        stringResource(
                            R.string.seconds,
                            state.preferences.controllerAutoHideTimeout,
                        )
                    },
                    icon = NextIcons.Timer,
                    value = state.preferences.controllerAutoHideTimeout.toFloat(),
                    valueRange = 1.0f..30.0f,
                    onValueChange = {
                        onAction(PlayerPreferencesUiEvent.UpdateControlAutoHideTimeout(it.toInt()))
                    },
                    onReset = {
                        onAction(
                            PlayerPreferencesUiEvent.UpdateControlAutoHideTimeout(
                                PlayerPreferences.DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT,
                            ),
                        )
                    },
                    isLastItem = true,
                    trailingContent = {
                        FilledIconButton(onClick = {
                            onAction(
                                PlayerPreferencesUiEvent.UpdateControlAutoHideTimeout(
                                    PlayerPreferences.DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT,
                                ),
                            )
                        }) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(
                                    id = R.string.reset_controller_timeout,
                                ),
                            )
                        }
                    },
                )
            }

            ListSectionTitle(text = stringResource(id = R.string.playback))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.resume),
                    description = stringResource(id = R.string.resume_description),
                    icon = NextIcons.Resume,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.ShowDialog(
                                value = PlayerPreferenceDialog.ResumeDialog,
                            ),
                        )
                    },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = "Resume only the first file",
                    description = "Apply resume setting only on the first file. Next files will start from the beginning.",
                    icon = NextIcons.Resume,
                    isChecked = state.preferences.resumeOnlyFirstFile,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.UpdatePreferences {
                                it.copy(resumeOnlyFirstFile = !it.resumeOnlyFirstFile)
                            },
                        )
                    },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.default_playback_speed),
                    description = "${state.preferences.defaultPlaybackSpeed}x",
                    icon = NextIcons.Speed,
                    value = state.preferences.defaultPlaybackSpeed,
                    valueRange = 0.2f..4.0f,
                    onValueChange = {
                        onAction(PlayerPreferencesUiEvent.UpdateDefaultPlaybackSpeed(value = it))
                    },
                    onReset = {
                        onAction(PlayerPreferencesUiEvent.UpdateDefaultPlaybackSpeed(value = 1f))
                    },
                    trailingContent = {
                        FilledIconButton(onClick = {
                            onAction(
                                PlayerPreferencesUiEvent.UpdateDefaultPlaybackSpeed(value = 1f),
                            )
                        }) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(
                                    id = R.string.reset_default_playback_speed,
                                ),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.remember_selections),
                    description = stringResource(id = R.string.remember_selections_description),
                    icon = NextIcons.Selection,
                    isChecked = state.preferences.rememberSelections,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleRememberSelections) },
                )
                PreferenceSwitch(
                    title = "Back to list",
                    description = "Return to the list after playback is completed.",
                    icon = NextIcons.Player,
                    isChecked = state.preferences.backToListAfterPlayback,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.UpdatePreferences {
                                it.copy(backToListAfterPlayback = !it.backToListAfterPlayback)
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = "Preview while seeking",
                    description = "Display preview image while changing playback position.",
                    icon = NextIcons.Movie,
                    isChecked = state.preferences.previewWhileSeeking,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.UpdatePreferences {
                                it.copy(previewWhileSeeking = !it.previewWhileSeeking)
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = "Fast seeking",
                    description = "Seek to nearest key-frame position instead of exact position.",
                    icon = NextIcons.Replay,
                    isChecked = state.preferences.fastSeeking,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.UpdatePreferences {
                                it.copy(fastSeeking = !it.fastSeeking)
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.autoplay_settings),
                    description = stringResource(
                        id = R.string.autoplay_settings_description,
                    ),
                    icon = NextIcons.Player,
                    isChecked = state.preferences.autoplay,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleAutoplay) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.tunneled_playback),
                    description = stringResource(id = R.string.tunneled_playback_description),
                    icon = NextIcons.Decoder,
                    isChecked = state.preferences.enableTunneledPlayback,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleTunneledPlayback) },
                )
                if (LocalContext.current.isPipFeatureSupported) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.pip_settings),
                        description = stringResource(
                            id = R.string.pip_settings_description,
                        ),
                        icon = NextIcons.Pip,
                        isChecked = state.preferences.autoPip,
                        onClick = { onAction(PlayerPreferencesUiEvent.ToggleAutoPip) },
                    )
                }
                PreferenceSwitch(
                    title = stringResource(id = R.string.background_play),
                    description = stringResource(
                        id = R.string.background_play_description,
                    ),
                    icon = NextIcons.Headset,
                    isChecked = state.preferences.autoBackgroundPlay,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleAutoBackgroundPlay) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.remember_brightness_level),
                    description = stringResource(
                        id = R.string.remember_brightness_level_description,
                    ),
                    icon = NextIcons.Brightness,
                    isChecked = state.preferences.rememberPlayerBrightness,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleRememberBrightnessLevel) },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.player_screen_orientation),
                    description = state.preferences.playerScreenOrientation.name(),
                    icon = NextIcons.Rotation,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.ShowDialog(
                                value = PlayerPreferenceDialog.PlayerScreenOrientationDialog,
                            ),
                        )
                    },
                    isLastItem = true,
                )
            }
        }

        state.showDialog?.let { showDialog ->
            when (showDialog) {
                PlayerPreferenceDialog.ResumeDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.resume),
                        onDismissClick = {
                            onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(Resume.entries.toTypedArray()) {
                            RadioTextButton(
                                text = it.name(),
                                selected = (it == state.preferences.resume),
                                onClick = {
                                    onAction(
                                        PlayerPreferencesUiEvent.UpdatePlaybackResume(resume = it),
                                    )
                                    onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                                },
                            )
                        }
                    }
                }

                PlayerPreferenceDialog.PlayerScreenOrientationDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.player_screen_orientation),
                        onDismissClick = {
                            onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(ScreenOrientation.entries.toTypedArray()) {
                            RadioTextButton(
                                text = it.name(),
                                selected = it == state.preferences.playerScreenOrientation,
                                onClick = {
                                    onAction(
                                        PlayerPreferencesUiEvent.UpdatePreferredPlayerOrientation(
                                            value = it,
                                        ),
                                    )
                                    onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                                },
                            )
                        }
                    }
                }

                PlayerPreferenceDialog.ControlButtonsDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.control_buttons_alignment),
                        onDismissClick = {
                            onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(ControlButtonsPosition.entries.toTypedArray()) {
                            RadioTextButton(
                                text = it.name(),
                                selected = it == state.preferences.controlButtonsPosition,
                                onClick = {
                                    onAction(
                                        UpdatePreferredControlButtonsPosition(
                                            value = it,
                                        ),
                                    )
                                    onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                                },
                            )
                        }
                    }
                }

                // MX Player Pro 20-Swatch Dual-Tone Theme Picker (Screenshot #5 155435_MX Player Pro.png)
                PlayerPreferenceDialog.MxPlayerThemeDialog -> {
                    val swatches = listOf(
                        "#FFFFFF", "#121212", "#F44336", "#E91E63", "#FF4081",
                        "#FFC107", "#FFEB3B", "#795548", "#69F0AE", "#0091FF",
                        "#40C4FF", "#7C4DFF", "#424242", "#616161", "#FF5722",
                        "#D81B60", "#FF1744", "#FFD600", "#8D6E63", "#00BFA5",
                        "#9FA8DA", "#9C27B0",
                    )
                    Dialog(
                        onDismissRequest = {
                            onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1F2633),
                            contentColor = Color.White,
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    text = "Theme",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                FlowRow(
                                    maxItemsInEachRow = 5,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    swatches.forEach { hex ->
                                        val bottomColor = runCatching { Color(hex.toColorInt()) }.getOrDefault(accent)
                                        val isSelected = state.preferences.playerAccentHex.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) accent else Color.White.copy(alpha = 0.2f),
                                                    shape = CircleShape,
                                                )
                                                .clickable {
                                                    onAction(
                                                        PlayerPreferencesUiEvent.UpdatePreferences {
                                                            it.copy(playerAccentHex = hex)
                                                        },
                                                    )
                                                    onAction(PlayerPreferencesUiEvent.ShowDialog(null))
                                                },
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArc(
                                                    color = Color(0xFF10131A),
                                                    startAngle = 180f,
                                                    sweepAngle = 180f,
                                                    useCenter = true,
                                                )
                                                drawArc(
                                                    color = bottomColor,
                                                    startAngle = 0f,
                                                    sweepAngle = 180f,
                                                    useCenter = true,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // MX Player Pro "Screen" Dialog (Screenshot #7 155442_MX Player Pro.png)
                PlayerPreferenceDialog.MxScreenDialog -> {
                    Dialog(
                        onDismissRequest = {
                            onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1F2633),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "Screen",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = "Brightness (${(state.preferences.playerBrightness * 100).roundToInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                NovaFluidSlider(
                                    value = state.preferences.playerBrightness,
                                    onValueChange = { b ->
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(
                                                    playerBrightness = b,
                                                    rememberPlayerBrightness = true,
                                                )
                                            },
                                        )
                                    },
                                    valueRange = 0.05f..1.0f,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                MxCheckboxRow(
                                    label = "Screen rotation button",
                                    checked = state.preferences.showScreenRotationButton,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(showScreenRotationButton = !it.showScreenRotationButton)
                                            },
                                        )
                                    },
                                )
                                MxCheckboxRow(
                                    label = "Display battery/clock in title bar",
                                    checked = state.preferences.showBatteryClockInTitleBar,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(showBatteryClockInTitleBar = !it.showBatteryClockInTitleBar)
                                            },
                                        )
                                    },
                                )
                                MxCheckboxRow(
                                    label = "Show Remaining Time",
                                    checked = state.preferences.showRemainingTime,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(showRemainingTime = !it.showRemainingTime)
                                            },
                                        )
                                    },
                                )
                                MxCheckboxRow(
                                    label = "Keep Screen On",
                                    checked = state.preferences.keepScreenOn,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(keepScreenOn = !it.keepScreenOn)
                                            },
                                        )
                                    },
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(
                                        onClick = { onAction(PlayerPreferencesUiEvent.ShowDialog(null)) },
                                    ) {
                                        Text("OK", color = accent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // MX Player Pro "Controls" Dialog (Screenshots #8 & #9 155451, 155452_MX Player Pro.png)
                PlayerPreferenceDialog.MxControlsDialog -> {
                    Dialog(
                        onDismissRequest = {
                            onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1F2633),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Controls",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = "Gestures",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent,
                                )
                                MxCheckboxRow(
                                    label = "Seek position",
                                    checked = state.preferences.useSeekControls,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(useSeekControls = !it.useSeekControls)
                                            },
                                        )
                                    },
                                )
                                MxCheckboxRow(
                                    label = "Video zoom & Pan",
                                    checked = state.preferences.useZoomControls,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(useZoomControls = !it.useZoomControls)
                                            },
                                        )
                                    },
                                )
                                MxCheckboxRow(
                                    label = "Volume (Right vertical swipe)",
                                    checked = state.preferences.enableVolumeSwipeGesture,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(enableVolumeSwipeGesture = !it.enableVolumeSwipeGesture)
                                            },
                                        )
                                    },
                                )
                                MxCheckboxRow(
                                    label = "Brightness (Left vertical swipe)",
                                    checked = state.preferences.enableBrightnessSwipeGesture,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(enableBrightnessSwipeGesture = !it.enableBrightnessSwipeGesture)
                                            },
                                        )
                                    },
                                )
                                MxCheckboxRow(
                                    label = "Speed FF (Long press)",
                                    checked = state.preferences.useLongPressControls,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(useLongPressControls = !it.useLongPressControls)
                                            },
                                        )
                                    },
                                )

                                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                                Text(
                                    text = "Quick Shortcuts",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent,
                                )
                                MxQuickShortcut.entries.forEach { shortcut ->
                                    MxCheckboxRow(
                                        label = shortcut.defaultTitle,
                                        checked = shortcut.name in state.preferences.enabledShortcuts,
                                        accent = accent,
                                        onToggle = {
                                            onAction(
                                                PlayerPreferencesUiEvent.UpdatePreferences { prefs ->
                                                    val next = prefs.enabledShortcuts.toMutableList()
                                                    if (shortcut.name in next) next.remove(shortcut.name) else next.add(shortcut.name)
                                                    prefs.copy(enabledShortcuts = next)
                                                },
                                            )
                                        },
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(
                                        onClick = { onAction(PlayerPreferencesUiEvent.ShowDialog(null)) },
                                    ) {
                                        Text("OK", color = accent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // MX Player Pro "Navigation" Dialog (Screenshot #10 155458_MX Player Pro.png)
                PlayerPreferenceDialog.MxNavigationDialog -> {
                    Dialog(
                        onDismissRequest = {
                            onAction(PlayerPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1F2633),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "Navigation",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                )
                                MxCheckboxRow(
                                    label = "Forward/backward moving button",
                                    checked = state.preferences.showForwardBackwardButtons,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(showForwardBackwardButtons = !it.showForwardBackwardButtons)
                                            },
                                        )
                                    },
                                )
                                Text(
                                    text = "Move Interval (${state.preferences.seekIncrement} sec.)",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                NovaFluidSlider(
                                    value = state.preferences.seekIncrement.toFloat(),
                                    onValueChange = { sec ->
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(seekIncrement = sec.roundToInt().coerceIn(3, 60))
                                            },
                                        )
                                    },
                                    valueRange = 3f..60f,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                MxCheckboxRow(
                                    label = "Previous/next button",
                                    checked = state.preferences.showPrevNextButtons,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(showPrevNextButtons = !it.showPrevNextButtons)
                                            },
                                        )
                                    },
                                )
                                MxCheckboxRow(
                                    label = "Display the current position while changing position",
                                    checked = state.preferences.displayPositionWhileSeeking,
                                    accent = accent,
                                    onToggle = {
                                        onAction(
                                            PlayerPreferencesUiEvent.UpdatePreferences {
                                                it.copy(displayPositionWhileSeeking = !it.displayPositionWhileSeeking)
                                            },
                                        )
                                    },
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(
                                        onClick = { onAction(PlayerPreferencesUiEvent.ShowDialog(null)) },
                                    ) {
                                        Text("OK", color = accent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MxCheckboxRow(
    label: String,
    checked: Boolean,
    accent: Color,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = Color.White.copy(alpha = 0.6f),
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

@DayNightPreview
@Composable
private fun PlayerPreferencesScreenPreview() {
    NextPlayerTheme {
        PlayerPreferencesScreenContent(
            state = PlayerPreferencesUiState(),
            onAction = {},
        )
    }
}
