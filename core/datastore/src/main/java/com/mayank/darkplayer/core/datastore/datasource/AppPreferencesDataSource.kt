package com.mayank.darkplayer.core.datastore.datasource

import androidx.datastore.core.DataStore
import com.mayank.darkplayer.core.common.Logger
import com.mayank.darkplayer.core.common.di.DiQualifiers
import com.mayank.darkplayer.core.model.ApplicationPreferences
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [])
class AppPreferencesDataSource(
    @Named(DiQualifiers.APP_PREFERENCES) private val appPreferences:
    DataStore<ApplicationPreferences>,
) : PreferencesDataSource<ApplicationPreferences> {

    companion object {
        private const val TAG = "AppPreferencesDataSource"
    }

    override val preferences = appPreferences.data

    override suspend fun update(
        transform: suspend (ApplicationPreferences) -> ApplicationPreferences,
    ) {
        try {
            appPreferences.updateData(transform = transform)
        } catch (ioException: Exception) {
            Logger.logError(
                tag = TAG,
                message = "Failed to update app preferences: $ioException",
            )
        }
    }
}
