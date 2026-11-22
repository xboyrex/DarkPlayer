package com.mayank.darkplayer.core.datastore.datasource

import androidx.datastore.core.DataStore
import com.mayank.darkplayer.core.common.Logger
import com.mayank.darkplayer.core.common.di.DiQualifiers
import com.mayank.darkplayer.core.model.PlayerPreferences
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [])
class PlayerPreferencesDataSource(
    @Named(DiQualifiers.PLAYER_PREFERENCES) private val preferencesDataStore:
    DataStore<PlayerPreferences>,
) : PreferencesDataSource<PlayerPreferences> {

    companion object {
        private const val TAG = "PlayerPreferencesDataSource"
    }

    override val preferences = preferencesDataStore.data

    override suspend fun update(transform: suspend (PlayerPreferences) -> PlayerPreferences) {
        try {
            preferencesDataStore.updateData(transform = transform)
        } catch (ioException: Exception) {
            Logger.logError(
                tag = TAG,
                message = "Failed to update app preferences: $ioException",
            )
        }
    }
}
