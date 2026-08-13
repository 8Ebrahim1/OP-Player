package com.opplayer.app.player.fakes

import com.opplayer.app.data.PlayerPreferences
import com.opplayer.app.data.PlayerPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePlayerPreferencesStore(
    initial: PlayerPreferences = PlayerPreferences()
) : PlayerPreferencesStore {

    private val state = MutableStateFlow(initial)

    val saved = mutableListOf<PlayerPreferences>()

    override val preferences: Flow<PlayerPreferences> = state

    override suspend fun save(preferences: PlayerPreferences) {
        saved += preferences
        state.value = preferences
    }
}
