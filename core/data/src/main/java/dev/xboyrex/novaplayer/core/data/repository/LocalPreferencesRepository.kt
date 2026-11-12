package dev.xboyrex.novaplayer.core.data.repository

import dev.xboyrex.novaplayer.core.common.di.DiQualifiers
import dev.xboyrex.novaplayer.core.datastore.datasource.AppPreferencesDataSource
import dev.xboyrex.novaplayer.core.datastore.datasource.PlayerPreferencesDataSource
import dev.xboyrex.novaplayer.core.model.ApplicationPreferences
import dev.xboyrex.novaplayer.core.model.PlayerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class LocalPreferencesRepository(
    private val appPreferencesDataSource: AppPreferencesDataSource,
    private val playerPreferencesDataSource: PlayerPreferencesDataSource,
    @Named(DiQualifiers.APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) : PreferencesRepository {

    override val applicationPreferences: StateFlow<ApplicationPreferences> =
        appPreferencesDataSource.preferences.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = ApplicationPreferences(),
        )

    override val playerPreferences: StateFlow<PlayerPreferences> =
        playerPreferencesDataSource.preferences.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = PlayerPreferences(),
        )

    override suspend fun updateApplicationPreferences(
        transform: suspend (ApplicationPreferences) -> ApplicationPreferences,
    ) {
        appPreferencesDataSource.update(transform = transform)
    }

    override suspend fun updatePlayerPreferences(
        transform: suspend (PlayerPreferences) -> PlayerPreferences,
    ) {
        playerPreferencesDataSource.update(transform = transform)
    }

    override suspend fun resetPreferences() {
        appPreferencesDataSource.update { ApplicationPreferences() }
        playerPreferencesDataSource.update { PlayerPreferences() }
    }
}
