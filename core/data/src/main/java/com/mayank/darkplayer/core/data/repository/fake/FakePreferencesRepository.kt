package com.mayank.darkplayer.core.data.repository.fake

import com.mayank.darkplayer.core.data.repository.PreferencesRepository
import com.mayank.darkplayer.core.model.ApplicationPreferences
import com.mayank.darkplayer.core.model.PlayerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakePreferencesRepository : PreferencesRepository {

    private val applicationPreferencesStateFlow = MutableStateFlow(value = ApplicationPreferences())
    private val playerPreferencesStateFlow = MutableStateFlow(value = PlayerPreferences())

    override val applicationPreferences: StateFlow<ApplicationPreferences>
        get() = applicationPreferencesStateFlow
    override val playerPreferences: StateFlow<PlayerPreferences>
        get() = playerPreferencesStateFlow

    override suspend fun updateApplicationPreferences(
        transform: suspend (ApplicationPreferences) -> ApplicationPreferences,
    ) {
        applicationPreferencesStateFlow.update { transform.invoke(it) }
    }

    override suspend fun updatePlayerPreferences(
        transform: suspend (PlayerPreferences) -> PlayerPreferences,
    ) {
        playerPreferencesStateFlow.update { transform.invoke(it) }
    }

    override suspend fun resetPreferences() {
        applicationPreferencesStateFlow.update { ApplicationPreferences() }
        playerPreferencesStateFlow.update { PlayerPreferences() }
    }
}
