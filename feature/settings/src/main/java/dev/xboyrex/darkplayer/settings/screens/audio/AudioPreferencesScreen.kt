package dev.xboyrex.darkplayer.settings.screens.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import dev.xboyrex.darkplayer.core.ui.glass.popupBackgroundBlur
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.audio.EqualizerAndDolbyStudioContent
import dev.xboyrex.darkplayer.core.ui.components.ClickablePreferenceItem
import dev.xboyrex.darkplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.PreferenceSwitch
import dev.xboyrex.darkplayer.core.ui.components.RadioTextButton
import dev.xboyrex.darkplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.darkplayer.core.ui.components.tvFocusDown
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.settings.composables.OptionsDialog
import dev.xboyrex.darkplayer.settings.utils.LocalesHelper

@Composable
fun AudioPreferencesScreen(viewModel: AudioPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AudioPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun AudioPreferencesScreenContent(
    state: AudioPreferencesUiState,
    onAction: (AudioPreferencesUiEvent) -> Unit,
) {
    val languages = remember {
        listOf(
            element = Pair(
                first = "None",
                second = "",
            ),
        ) +
            LocalesHelper.getAvailableLocales()
    }

    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        modifier = Modifier.popupBackgroundBlur(),
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.audio),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AudioPreferencesUiEvent.NavigateUp)
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
            ListSectionTitle(text = stringResource(id = R.string.playback))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.preferred_audio_lang),
                    description =
                        LocalesHelper.getLocaleDisplayLanguage(
                            state.preferences.preferredAudioLanguage,
                        )
                            .takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.preferred_audio_lang_description),
                    icon = NextIcons.Language,
                    onClick = {
                        onAction(
                            AudioPreferencesUiEvent.ShowDialog(
                                value = AudioPreferenceDialog.AudioLanguageDialog,
                            ),
                        )
                    },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.require_audio_focus),
                    description = stringResource(R.string.require_audio_focus_desc),
                    icon = NextIcons.Focus,
                    isChecked = state.preferences.requireAudioFocus,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleRequireAudioFocus) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.pause_on_headset_disconnect),
                    description = stringResource(id = R.string.pause_on_headset_disconnect_desc),
                    icon = NextIcons.HeadsetOff,
                    isChecked = state.preferences.pauseOnHeadsetDisconnect,
                    onClick = { onAction(AudioPreferencesUiEvent.TogglePauseOnHeadsetDisconnect) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.system_volume_panel),
                    description = stringResource(id = R.string.system_volume_panel_desc),
                    icon = NextIcons.Headset,
                    isChecked = state.preferences.showSystemVolumePanel,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleShowSystemVolumePanel) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.volume_boost),
                    description = stringResource(R.string.volume_boost_desc),
                    icon = NextIcons.VolumeUp,
                    isChecked = state.preferences.enableVolumeBoost,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleVolumeBoost) },
                    isLastItem = true,
                )
            }

            ListSectionTitle(text = "EQUALIZER & DOLBY ATMOS STUDIO")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0E131D))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(top = 14.dp),
            ) {
                EqualizerAndDolbyStudioContent(
                    modifier = Modifier.height(620.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        state.showDialog?.let { showDialog ->
            when (showDialog) {
                AudioPreferenceDialog.AudioLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.preferred_audio_lang),
                        onDismissClick = {
                            onAction(AudioPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(items = languages) {
                            RadioTextButton(
                                text = it.first,
                                selected = it.second == state.preferences.preferredAudioLanguage,
                                onClick = {
                                    onAction(AudioPreferencesUiEvent.UpdateAudioLanguage(it.second))
                                    onAction(AudioPreferencesUiEvent.ShowDialog(value = null))
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
private fun AudioPreferencesScreenPreview() {
    NextPlayerTheme {
        AudioPreferencesScreenContent(
            state = AudioPreferencesUiState(),
            onAction = {},
        )
    }
}
