package com.opplayer.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opplayer.app.player.VideoScaleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Player choices that must outlive a single playback session, so the sheet never falls back
 * to the defaults after the user has picked something.
 */
data class PlayerPreferences(
    val scaleMode: VideoScaleMode = VideoScaleMode.FIT,
    val speed: Float = DEFAULT_SPEED,
    val autoNextEnabled: Boolean = true,
    val autoRotateEnabled: Boolean = true,
    val gesturesEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_SPEED = 1f
    }
}

interface PlayerPreferencesStore {

    val preferences: Flow<PlayerPreferences>

    suspend fun save(preferences: PlayerPreferences)
}

private val Context.playerPreferencesStore by preferencesDataStore(
    name = "op_player_playback_settings"
)

class PlayerPreferencesRepository(private val context: Context) : PlayerPreferencesStore {

    override val preferences: Flow<PlayerPreferences> =
        context.playerPreferencesStore.data.map { stored ->
            val defaults = PlayerPreferences()

            PlayerPreferences(
                scaleMode = stored[KEY_SCALE_MODE]?.toScaleMode() ?: defaults.scaleMode,
                speed = stored[KEY_SPEED] ?: defaults.speed,
                autoNextEnabled = stored[KEY_AUTO_NEXT] ?: defaults.autoNextEnabled,
                autoRotateEnabled = stored[KEY_AUTO_ROTATE] ?: defaults.autoRotateEnabled,
                gesturesEnabled = stored[KEY_GESTURES] ?: defaults.gesturesEnabled
            )
        }

    override suspend fun save(preferences: PlayerPreferences) {
        context.playerPreferencesStore.edit { stored ->
            stored[KEY_SCALE_MODE] = preferences.scaleMode.name
            stored[KEY_SPEED] = preferences.speed
            stored[KEY_AUTO_NEXT] = preferences.autoNextEnabled
            stored[KEY_AUTO_ROTATE] = preferences.autoRotateEnabled
            stored[KEY_GESTURES] = preferences.gesturesEnabled
        }
    }

    private fun String.toScaleMode(): VideoScaleMode? =
        VideoScaleMode.entries.firstOrNull { it.name == this }

    private companion object {
        val KEY_SCALE_MODE = stringPreferencesKey("player_scale_mode")
        val KEY_SPEED = floatPreferencesKey("player_speed")
        val KEY_AUTO_NEXT = booleanPreferencesKey("player_auto_next")
        val KEY_AUTO_ROTATE = booleanPreferencesKey("player_auto_rotate")
        val KEY_GESTURES = booleanPreferencesKey("player_gestures")
    }
}
