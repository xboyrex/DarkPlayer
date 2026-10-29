package dev.xboyrex.darkplayer.settings.screens.player

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import dev.xboyrex.darkplayer.core.common.extensions.round
import dev.xboyrex.darkplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.darkplayer.core.model.ControlButtonsPosition
import dev.xboyrex.darkplayer.core.model.LoopMode
import dev.xboyrex.darkplayer.core.model.PlayerPreferences
import dev.xboyrex.darkplayer.core.model.Resume
import dev.xboyrex.darkplayer.core.model.ScreenOrientation
import dev.xboyrex.darkplayer.core.ui.base.MviViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PlayerPreferencesViewModel(
    private val preferencesRepository: PreferencesRepository,
    @InjectedParam internal var output: Output,
) : MviViewModel<PlayerPreferencesUiState, PlayerPreferencesUiEvent>() {

    data class Output(val navigateUp: () -> Unit)

    private val stateInternal = MutableStateFlow(
        value = PlayerPreferencesUiState(
            preferences = preferencesRepository.playerPreferences.value,
        ),
    )
    override val state: StateFlow<PlayerPreferencesUiState> = stateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { preferences ->
                stateInternal.update { it.copy(preferences = preferences) }
            }
        }
    }

    override fun onAction(action: PlayerPreferencesUiEvent) {
        when (action) {
            is PlayerPreferencesUiEvent.NavigateUp -> output.navigateUp()

            is PlayerPreferencesUiEvent.ShowDialog -> showDialog(action.value)

            is PlayerPreferencesUiEvent.UpdatePlaybackResume -> updatePlaybackResume(action.resume)

            is PlayerPreferencesUiEvent.ToggleAutoplay -> toggleAutoplay()

            is PlayerPreferencesUiEvent.ToggleTunneledPlayback -> toggleTunneledPlayback()

            is PlayerPreferencesUiEvent.ToggleAutoPip -> toggleAutoPip()

            is PlayerPreferencesUiEvent.ToggleAutoBackgroundPlay -> toggleAutoBackgroundPlay()

            is PlayerPreferencesUiEvent.ToggleRememberBrightnessLevel ->
                toggleRememberBrightnessLevel()

            is PlayerPreferencesUiEvent.ToggleRememberSelections -> toggleRememberSelections()

            is PlayerPreferencesUiEvent.UpdatePreferredPlayerOrientation ->
                updatePreferredPlayerOrientation(
                    action.value,
                )

            is PlayerPreferencesUiEvent.UpdatePreferredControlButtonsPosition ->
                updatePreferredControlButtonsPosition(
                    action.value,
                )

            is PlayerPreferencesUiEvent.UpdateDefaultPlaybackSpeed -> updateDefaultPlaybackSpeed(
                action.value,
            )

            is PlayerPreferencesUiEvent.UpdateControlAutoHideTimeout ->
                updateControlAutoHideTimeout(
                    action.value,
                )

            is PlayerPreferencesUiEvent.ToggleUseMaterialYouControls ->
                toggleUseMaterialYouControls()

            is PlayerPreferencesUiEvent.ToggleShowRemainingTime ->
                toggleShowRemainingTime()

            is PlayerPreferencesUiEvent.ToggleHidePlayerButtonsBackground ->
                toggleHidePlayerButtonsBackground()

            is PlayerPreferencesUiEvent.CycleLoopMode ->
                cycleLoopMode()
        }
    }

    private fun showDialog(value: PlayerPreferenceDialog?) {
        stateInternal.update { it.copy(showDialog = value) }
    }

    private fun updatePlaybackResume(resume: Resume) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(
                    resume = resume,
                )
            }
        }
    }

    private fun toggleAutoplay() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoplay = !it.autoplay) }
        }
    }

    private fun toggleAutoPip() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoPip = !it.autoPip) }
        }
    }

    private fun toggleTunneledPlayback() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(enableTunneledPlayback = !it.enableTunneledPlayback)
            }
        }
    }

    private fun toggleAutoBackgroundPlay() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(autoBackgroundPlay = !it.autoBackgroundPlay)
            }
        }
    }

    private fun toggleRememberBrightnessLevel() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(rememberPlayerBrightness = !it.rememberPlayerBrightness)
            }
        }
    }

    private fun toggleRememberSelections() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(rememberSelections = !it.rememberSelections)
            }
        }
    }

    private fun updatePreferredPlayerOrientation(value: ScreenOrientation) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(playerScreenOrientation = value)
            }
        }
    }

    private fun updatePreferredControlButtonsPosition(value: ControlButtonsPosition) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(controlButtonsPosition = value)
            }
        }
    }

    private fun updateDefaultPlaybackSpeed(value: Float) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(defaultPlaybackSpeed = value.round(decimalPlaces = 1))
            }
        }
    }

    private fun updateControlAutoHideTimeout(value: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(controllerAutoHideTimeout = value)
            }
        }
    }

    private fun toggleUseMaterialYouControls() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(useMaterialYouControls = !it.useMaterialYouControls)
            }
        }
    }

    private fun toggleShowRemainingTime() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(showRemainingTime = !it.showRemainingTime)
            }
        }
    }

    private fun toggleHidePlayerButtonsBackground() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(hidePlayerButtonsBackground = !it.hidePlayerButtonsBackground)
            }
        }
    }

    private fun cycleLoopMode() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                val nextMode = when (it.loopMode) {
                    LoopMode.OFF -> LoopMode.ALL
                    LoopMode.ALL -> LoopMode.ONE
                    LoopMode.ONE -> LoopMode.OFF
                }
                it.copy(loopMode = nextMode)
            }
        }
    }
}

@Stable
data class PlayerPreferencesUiState(
    val showDialog: PlayerPreferenceDialog? = null,
    val preferences: PlayerPreferences = PlayerPreferences(),
)

sealed interface PlayerPreferenceDialog {
    data object ResumeDialog : PlayerPreferenceDialog
    data object PlayerScreenOrientationDialog : PlayerPreferenceDialog
    data object ControlButtonsDialog : PlayerPreferenceDialog
}

sealed interface PlayerPreferencesUiEvent {
    data object NavigateUp : PlayerPreferencesUiEvent

    data class ShowDialog(val value: PlayerPreferenceDialog?) : PlayerPreferencesUiEvent
    data class UpdatePlaybackResume(val resume: Resume) : PlayerPreferencesUiEvent
    data object ToggleAutoplay : PlayerPreferencesUiEvent
    data object ToggleTunneledPlayback : PlayerPreferencesUiEvent
    data object ToggleAutoPip : PlayerPreferencesUiEvent
    data object ToggleAutoBackgroundPlay : PlayerPreferencesUiEvent
    data object ToggleRememberBrightnessLevel : PlayerPreferencesUiEvent
    data object ToggleRememberSelections : PlayerPreferencesUiEvent
    data class UpdatePreferredPlayerOrientation(val value: ScreenOrientation) :
        PlayerPreferencesUiEvent
    data class UpdatePreferredControlButtonsPosition(val value: ControlButtonsPosition) :
        PlayerPreferencesUiEvent
    data class UpdateDefaultPlaybackSpeed(val value: Float) : PlayerPreferencesUiEvent
    data class UpdateControlAutoHideTimeout(val value: Int) : PlayerPreferencesUiEvent
    data object ToggleUseMaterialYouControls : PlayerPreferencesUiEvent
    data object ToggleShowRemainingTime : PlayerPreferencesUiEvent
    data object ToggleHidePlayerButtonsBackground : PlayerPreferencesUiEvent
    data object CycleLoopMode : PlayerPreferencesUiEvent
}
