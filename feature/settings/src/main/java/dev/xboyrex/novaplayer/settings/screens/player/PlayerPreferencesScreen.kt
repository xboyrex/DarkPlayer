package dev.xboyrex.novaplayer.settings.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.xboyrex.novaplayer.core.common.extensions.isPipFeatureSupported
import dev.xboyrex.novaplayer.core.model.ControlButtonsPosition
import dev.xboyrex.novaplayer.core.model.PlayerPreferences
import dev.xboyrex.novaplayer.core.model.Resume
import dev.xboyrex.novaplayer.core.model.ScreenOrientation
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.components.ClickablePreferenceItem
import dev.xboyrex.novaplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.novaplayer.core.ui.components.NovaTopAppBar
import dev.xboyrex.novaplayer.core.ui.components.PreferenceSlider
import dev.xboyrex.novaplayer.core.ui.components.PreferenceSwitch
import dev.xboyrex.novaplayer.core.ui.components.RadioTextButton
import dev.xboyrex.novaplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.novaplayer.core.ui.components.tvFocusDown
import dev.xboyrex.novaplayer.core.ui.components.tvListFocus
import dev.xboyrex.novaplayer.core.ui.designsystem.NovaIcons
import dev.xboyrex.novaplayer.core.ui.preview.DayNightPreview
import dev.xboyrex.novaplayer.core.ui.theme.NovaPlayerTheme
import dev.xboyrex.novaplayer.settings.composables.OptionsDialog
import dev.xboyrex.novaplayer.settings.extensions.name
import dev.xboyrex.novaplayer.settings.screens.player.PlayerPreferencesUiEvent.UpdatePreferredControlButtonsPosition

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
    Scaffold(
        topBar = {
            NovaTopAppBar(
                title = stringResource(id = R.string.player_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(PlayerPreferencesUiEvent.NavigateUp)
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
            ListSectionTitle(text = stringResource(id = R.string.interface_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = stringResource(id = R.string.material_you_controls),
                    description = stringResource(id = R.string.material_you_controls_description),
                    icon = NovaIcons.Appearance,
                    isChecked = state.preferences.useMaterialYouControls,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleUseMaterialYouControls) },
                    isFirstItem = true,
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
                    icon = NovaIcons.Timer,
                    value = state.preferences.controllerAutoHideTimeout.toFloat(),
                    valueRange = 0.0f..600.0f,
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
                                imageVector = NovaIcons.History,
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
                    icon = NovaIcons.Resume,
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
                    description = state.preferences.defaultPlaybackSpeed.toString(),
                    icon = NovaIcons.Speed,
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
                                imageVector = NovaIcons.History,
                                contentDescription = stringResource(
                                    id = R.string.reset_default_playback_speed,
                                ),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.autoplay_settings),
                    description = stringResource(
                        id = R.string.autoplay_settings_description,
                    ),
                    icon = NovaIcons.Player,
                    isChecked = state.preferences.autoplay,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleAutoplay) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.tunneled_playback),
                    description = stringResource(id = R.string.tunneled_playback_description),
                    icon = NovaIcons.Decoder,
                    isChecked = state.preferences.enableTunneledPlayback,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleTunneledPlayback) },
                )
                if (LocalContext.current.isPipFeatureSupported) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.pip_settings),
                        description = stringResource(
                            id = R.string.pip_settings_description,
                        ),
                        icon = NovaIcons.Pip,
                        isChecked = state.preferences.autoPip,
                        onClick = { onAction(PlayerPreferencesUiEvent.ToggleAutoPip) },
                    )
                }
                PreferenceSwitch(
                    title = stringResource(id = R.string.background_play),
                    description = stringResource(
                        id = R.string.background_play_description,
                    ),
                    icon = NovaIcons.Headset,
                    isChecked = state.preferences.autoBackgroundPlay,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleAutoBackgroundPlay) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.remember_brightness_level),
                    description = stringResource(
                        id = R.string.remember_brightness_level_description,
                    ),
                    icon = NovaIcons.Brightness,
                    isChecked = state.preferences.rememberPlayerBrightness,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleRememberBrightnessLevel) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.remember_selections),
                    description = stringResource(id = R.string.remember_selections_description),
                    icon = NovaIcons.Selection,
                    isChecked = state.preferences.rememberSelections,
                    onClick = { onAction(PlayerPreferencesUiEvent.ToggleRememberSelections) },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.player_screen_orientation),
                    description = state.preferences.playerScreenOrientation.name(),
                    icon = NovaIcons.Rotation,
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
    NovaPlayerTheme {
        PlayerPreferencesScreenContent(
            state = PlayerPreferencesUiState(),
            onAction = {},
        )
    }
}
