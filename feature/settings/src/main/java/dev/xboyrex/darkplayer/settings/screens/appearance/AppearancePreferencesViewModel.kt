package dev.xboyrex.darkplayer.settings.screens.appearance

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import dev.xboyrex.darkplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.ThemeConfig
import dev.xboyrex.darkplayer.core.ui.base.MviViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class AppearancePreferencesViewModel(
    private val preferencesRepository: PreferencesRepository,
    @InjectedParam internal var output: Output,
) : MviViewModel<AppearancePreferencesUiState, AppearancePreferencesEvent>() {

    data class Output(val navigateUp: () -> Unit)

    private val stateInternal = MutableStateFlow(
        value = AppearancePreferencesUiState(
            preferences = preferencesRepository.applicationPreferences.value,
        ),
    )
    override val state: StateFlow<AppearancePreferencesUiState> = stateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { preferences ->
                stateInternal.update { it.copy(preferences = preferences) }
            }
        }
    }

    override fun onAction(action: AppearancePreferencesEvent) {
        when (action) {
            is AppearancePreferencesEvent.NavigateUp -> output.navigateUp()

            is AppearancePreferencesEvent.ShowDialog -> showDialog(action.value)

            is AppearancePreferencesEvent.ToggleDarkTheme -> toggleDarkTheme()

            is AppearancePreferencesEvent.UpdateThemeConfig -> updateThemeConfig(action.themeConfig)

            is AppearancePreferencesEvent.ToggleUseDynamicColors -> toggleUseDynamicColors()

            is AppearancePreferencesEvent.ToggleUseHighContrastDarkTheme ->
                toggleUseHighContrastDarkTheme()

            is AppearancePreferencesEvent.ToggleUseLiquidGlass ->
                toggleUseLiquidGlass()
        }
    }

    private fun showDialog(value: AppearancePreferenceDialog?) {
        stateInternal.update { it.copy(showDialog = value) }
    }

    private fun toggleDarkTheme() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(
                    themeConfig = if (it.themeConfig == ThemeConfig.ON) {
                        ThemeConfig.OFF
                    } else {
                        ThemeConfig.ON
                    },
                )
            }
        }
    }

    private fun updateThemeConfig(themeConfig: ThemeConfig) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(themeConfig = themeConfig)
            }
        }
    }

    private fun toggleUseDynamicColors() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(useDynamicColors = !it.useDynamicColors)
            }
        }
    }

    private fun toggleUseHighContrastDarkTheme() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(useHighContrastDarkTheme = !it.useHighContrastDarkTheme)
            }
        }
    }

    private fun toggleUseLiquidGlass() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(useLiquidGlass = !it.useLiquidGlass)
            }
        }
    }
}

@Stable
data class AppearancePreferencesUiState(
    val showDialog: AppearancePreferenceDialog? = null,
    val preferences: ApplicationPreferences = ApplicationPreferences(),
)

sealed interface AppearancePreferencesEvent {
    data object NavigateUp : AppearancePreferencesEvent

    data class ShowDialog(val value: AppearancePreferenceDialog?) : AppearancePreferencesEvent
    data object ToggleDarkTheme : AppearancePreferencesEvent
    data class UpdateThemeConfig(val themeConfig: ThemeConfig) : AppearancePreferencesEvent
    data object ToggleUseDynamicColors : AppearancePreferencesEvent
    data object ToggleUseHighContrastDarkTheme : AppearancePreferencesEvent
    data object ToggleUseLiquidGlass : AppearancePreferencesEvent
}

sealed interface AppearancePreferenceDialog {
    data object Theme : AppearancePreferenceDialog
}
