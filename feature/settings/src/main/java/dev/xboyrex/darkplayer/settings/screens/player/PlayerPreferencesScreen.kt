package dev.xboyrex.darkplayer.settings.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.common.extensions.isPipFeatureSupported
import dev.xboyrex.darkplayer.core.model.ControlButtonsPosition
import dev.xboyrex.darkplayer.core.model.LoopMode
import dev.xboyrex.darkplayer.core.model.PlayerPreferences
import dev.xboyrex.darkplayer.core.model.Resume
import dev.xboyrex.darkplayer.core.model.ScreenOrientation
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
import dev.xboyrex.darkplayer.core.ui.preview.DayNightPreview
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.settings.composables.OptionsDialog
import dev.xboyrex.darkplayer.settings.extensions.name
import dev.xboyrex.darkplayer.settings.screens.player.PlayerPreferencesUiEvent.UpdatePreferredControlButtonsPosition

@Composable
fun PlayerPreferencesScreen(viewModel: PlayerPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PlayerPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun PlayerPreferencesScreenContent(
    state: PlayerPreferencesUiState,
    onAction: (PlayerPreferencesUiEvent) -> Unit,
) {
    val listFocusRequester = rememberTvListFocusRequester()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(focusRequester = listFocusRequester)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = navBottomInset + 32.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.interface_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = stringResource(id = R.string.material_you_controls),
                    description = stringResource(id = R.string.material_you_controls_description),
                    icon = NextIcons.Appearance,
                    isChecked = state.preferences.useMaterialYouControls,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleUseMaterialYouControls) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.control_buttons_alignment),
                    description = state.preferences.controlButtonsPosition.name(),
                    icon = NextIcons.ButtonsPosition,
                    onClick = {
                        onAction(
                            PlayerPreferencesUiEvent.ShowDialog(
                                value = PlayerPreferenceDialog.ControlButtonsDialog,
                            ),
                        )
                    },
                )
                PreferenceSwitch(
                    title = "Show remaining time",
                    description = "Display remaining duration on the player seekbar timer",
                    icon = NextIcons.Timer,
                    isChecked = state.preferences.showRemainingTime,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleShowRemainingTime) },
                )
                PreferenceSwitch(
                    title = "Minimal translucent controls",
                    description = "Use ultra-clean glass control buttons during playback",
                    icon = NextIcons.Style,
                    isChecked = state.preferences.hidePlayerButtonsBackground,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleHidePlayerButtonsBackground) },
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
                    valueRange = 0.0f..60.0f,
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
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.loop_mode),
                    description = when (state.preferences.loopMode) {
                        LoopMode.OFF -> stringResource(R.string.loop_mode_off)
                        LoopMode.ONE -> stringResource(R.string.loop_mode_one)
                        LoopMode.ALL -> stringResource(R.string.loop_mode_all)
                    },
                    icon = NextIcons.Replay,
                    onClick = { onAction(PlayerPreferencesUiEvent.CycleLoopMode) },
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
                PreferenceSwitch(
                    title = stringResource(id = R.string.remember_selections),
                    description = stringResource(id = R.string.remember_selections_description),
                    icon = NextIcons.Selection,
                    isChecked = state.preferences.rememberSelections,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleRememberSelections) },
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
            }
        }
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
