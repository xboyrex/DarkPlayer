package dev.xboyrex.novaplayer.settings.screens.audio

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.components.ClickablePreferenceItem
import dev.xboyrex.novaplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.novaplayer.core.ui.components.NovaTopAppBar
import dev.xboyrex.novaplayer.core.ui.components.PreferenceSwitch
import dev.xboyrex.novaplayer.core.ui.components.RadioTextButton
import dev.xboyrex.novaplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.novaplayer.core.ui.components.tvFocusDown
import dev.xboyrex.novaplayer.core.ui.components.tvListFocus
import dev.xboyrex.novaplayer.core.ui.designsystem.NovaIcons
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyController
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyPresetsData
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyStudioDialog
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyVectorLogo
import dev.xboyrex.novaplayer.core.ui.glass.NovaRoundedHeavy
import dev.xboyrex.novaplayer.core.ui.glass.novaPressable
import dev.xboyrex.novaplayer.core.ui.theme.NovaPlayerTheme
import dev.xboyrex.novaplayer.settings.composables.OptionsDialog
import dev.xboyrex.novaplayer.settings.utils.LocalesHelper

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
    var showDolbyStudio by rememberSaveable { mutableStateOf(false) }

    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        topBar = {
            NovaTopAppBar(
                title = stringResource(id = R.string.audio),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AudioPreferencesUiEvent.NavigateUp)
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
            Spacer(modifier = Modifier.height(8.dp))
            // Dolby Atmos Studio Launch Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .novaPressable { showDolbyStudio = true },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    ) {
                        NovaDolbyVectorLogo(
                            modifier = Modifier.padding(10.dp),
                            leftColor = MaterialTheme.colorScheme.onPrimary,
                            rightColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dolby Atmos & 20-Band Equalizer",
                            fontFamily = NovaRoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        val prof = NovaDolbyPresetsData.PROFILES.find { it.id == NovaDolbyController.currentProfile }?.name ?: "Movie"
                        Text(
                            text = if (NovaDolbyController.enabled) {
                                "Active · $prof · ${NovaDolbyController.currentPresetName}"
                            } else {
                                "Tap to open Dolby Atmos Studio"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                    }
                    Icon(imageVector = NovaIcons.ArrowForward, contentDescription = null)
                }
            }

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
                    icon = NovaIcons.Language,
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
                    title = stringResource(R.string.require_audio_focus),
                    description = stringResource(R.string.require_audio_focus_desc),
                    icon = NovaIcons.Focus,
                    isChecked = state.preferences.requireAudioFocus,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleRequireAudioFocus) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.pause_on_headset_disconnect),
                    description = stringResource(id = R.string.pause_on_headset_disconnect_desc),
                    icon = NovaIcons.HeadsetOff,
                    isChecked = state.preferences.pauseOnHeadsetDisconnect,
                    onClick = { onAction(AudioPreferencesUiEvent.TogglePauseOnHeadsetDisconnect) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.system_volume_panel),
                    description = stringResource(id = R.string.system_volume_panel_desc),
                    icon = NovaIcons.Headset,
                    isChecked = state.preferences.showSystemVolumePanel,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleShowSystemVolumePanel) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.volume_boost),
                    description = stringResource(id = R.string.volume_boost_desc),
                    icon = NovaIcons.VolumeUp,
                    isChecked = state.preferences.enableVolumeBoost,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleVolumeBoost) },
                    isLastItem = true,
                )
            }
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

    if (showDolbyStudio) {
        NovaDolbyStudioDialog(onDismiss = { showDolbyStudio = false })
    }
}

@PreviewLightDark
@Composable
private fun AudioPreferencesScreenPreview() {
    NovaPlayerTheme {
        AudioPreferencesScreenContent(
            state = AudioPreferencesUiState(),
            onAction = {},
        )
    }
}
