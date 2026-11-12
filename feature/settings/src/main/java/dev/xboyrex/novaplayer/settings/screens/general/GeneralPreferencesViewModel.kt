package dev.xboyrex.novaplayer.settings.screens.general

import android.net.Uri
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dev.xboyrex.novaplayer.core.common.Logger
import dev.xboyrex.novaplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.novaplayer.core.media.extensions.clearAllCache
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.base.MviViewModel
import dev.xboyrex.novaplayer.settings.backup.SettingsBackupService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class GeneralPreferencesViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val imageLoader: ImageLoader,
    private val settingsBackupService: SettingsBackupService,
    @InjectedParam internal var output: Output,
) : MviViewModel<GeneralPreferencesUiState, GeneralPreferencesUiEvent>() {

    data class Output(val navigateUp: () -> Unit, val showMessage: (Int) -> Unit = {})

    private val stateInternal = MutableStateFlow(value = GeneralPreferencesUiState())
    override val state: StateFlow<GeneralPreferencesUiState> = stateInternal.asStateFlow()
    private var pendingImport: String? = null

    override fun onAction(action: GeneralPreferencesUiEvent) {
        when (action) {
            is GeneralPreferencesUiEvent.NavigateUp -> output.navigateUp()

            is GeneralPreferencesUiEvent.ShowDialog -> showDialog(action.value)

            is GeneralPreferencesUiEvent.ClearThumbnailCache -> clearThumbnailCache()

            is GeneralPreferencesUiEvent.ResetSettings -> resetSettings()

            is GeneralPreferencesUiEvent.ExportSettings -> exportSettings(
                action.uri,
                action.includeNetworkConnections,
            )

            is GeneralPreferencesUiEvent.PrepareImport -> prepareImport(action.uri)

            is GeneralPreferencesUiEvent.ImportSettings -> importSettings()
        }
    }

    private fun showDialog(value: GeneralPreferencesDialog?) {
        if (value != GeneralPreferencesDialog.ImportSettingsDialog) pendingImport = null
        stateInternal.update { it.copy(showDialog = value) }
    }

    private fun clearThumbnailCache() {
        viewModelScope.launch { imageLoader.clearAllCache() }
    }

    private fun resetSettings() {
        viewModelScope.launch { preferencesRepository.resetPreferences() }
    }

    private fun exportSettings(uri: Uri, includeNetworkConnections: Boolean) {
        showDialog(null)
        backupOperation(R.string.settings_export_failed) {
            settingsBackupService.exportSettings(
                uri,
                includeNetworkConnections,
            )
            output.showMessage(R.string.settings_export_success)
        }
    }

    private fun prepareImport(uri: Uri) {
        backupOperation(R.string.settings_import_invalid) {
            pendingImport = settingsBackupService.readSettings(uri)
            showDialog(GeneralPreferencesDialog.ImportSettingsDialog)
        }
    }

    private fun importSettings() {
        val content = pendingImport ?: return
        showDialog(null)
        backupOperation(R.string.settings_import_failed) {
            settingsBackupService.importSettings(content)
            output.showMessage(R.string.settings_import_success)
        }
    }

    private fun backupOperation(failureMessage: Int, operation: suspend () -> Unit) {
        if (stateInternal.value.isBusy) return
        stateInternal.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                operation()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Logger.logError(
                    tag = "GeneralPreferencesViewModel",
                    message = "Settings backup failed: ${exception.javaClass.simpleName}",
                )
                output.showMessage(failureMessage)
            } finally {
                stateInternal.update { it.copy(isBusy = false) }
            }
        }
    }
}

data class GeneralPreferencesUiState(
    val showDialog: GeneralPreferencesDialog? = null,
    val isBusy: Boolean = false,
)

sealed interface GeneralPreferencesDialog {
    data object ClearThumbnailCacheDialog : GeneralPreferencesDialog
    data object ResetSettingsDialog : GeneralPreferencesDialog
    data object ExportSettingsDialog : GeneralPreferencesDialog
    data object ImportSettingsDialog : GeneralPreferencesDialog
}

sealed interface GeneralPreferencesUiEvent {
    data object NavigateUp : GeneralPreferencesUiEvent

    data class ShowDialog(val value: GeneralPreferencesDialog?) : GeneralPreferencesUiEvent
    data object ClearThumbnailCache : GeneralPreferencesUiEvent
    data object ResetSettings : GeneralPreferencesUiEvent
    data class ExportSettings(val uri: Uri, val includeNetworkConnections: Boolean) :
        GeneralPreferencesUiEvent
    data class PrepareImport(val uri: Uri) : GeneralPreferencesUiEvent
    data object ImportSettings : GeneralPreferencesUiEvent
}
