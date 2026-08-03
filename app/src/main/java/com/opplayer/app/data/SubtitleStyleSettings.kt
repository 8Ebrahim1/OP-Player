package com.opplayer.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SubtitleStyleSettings(
    val enabled: Boolean = true,
    val textSizeSp: Float = 20f,
    val textColorArgb: Long = 0xFFFFFFFFL,
    val backgroundArgb: Long = 0x99000000L,
    val bold: Boolean = false,
    val outline: Boolean = true,
    val bottomMarginDp: Float = 32f
) {
    companion object {
        const val MIN_TEXT_SIZE_SP = 12f
        const val MAX_TEXT_SIZE_SP = 44f
        const val MIN_BOTTOM_MARGIN_DP = 8f
        const val MAX_BOTTOM_MARGIN_DP = 140f

        val TEXT_COLORS = listOf(
            0xFFFFFFFFL,
            0xFFFFEB3BL,
            0xFF00E5FFL,
            0xFF69F0AEL,
            0xFFFFAB40L,
            0xFFFF80ABL,
            0xFF000000L
        )

        val BACKGROUND_COLORS = listOf(
            0x00000000L,
            0x59000000L,
            0x99000000L,
            0xE0000000L,
            0xB3FFFFFFL
        )
    }
}

private val Context.subtitleStyleStore by preferencesDataStore(name = "op_player_subtitle_style")

class SubtitleStyleRepository(private val context: Context) {

    val settings: Flow<SubtitleStyleSettings> = context.subtitleStyleStore.data.map { preferences ->
        val defaults = SubtitleStyleSettings()

        SubtitleStyleSettings(
            enabled = preferences[KEY_ENABLED] ?: defaults.enabled,
            textSizeSp = preferences[KEY_TEXT_SIZE] ?: defaults.textSizeSp,
            textColorArgb = preferences[KEY_TEXT_COLOR] ?: defaults.textColorArgb,
            backgroundArgb = preferences[KEY_BACKGROUND] ?: defaults.backgroundArgb,
            bold = preferences[KEY_BOLD] ?: defaults.bold,
            outline = preferences[KEY_OUTLINE] ?: defaults.outline,
            bottomMarginDp = preferences[KEY_BOTTOM_MARGIN] ?: defaults.bottomMarginDp
        )
    }

    suspend fun save(settings: SubtitleStyleSettings) {
        context.subtitleStyleStore.edit { preferences ->
            preferences[KEY_ENABLED] = settings.enabled
            preferences[KEY_TEXT_SIZE] = settings.textSizeSp
            preferences[KEY_TEXT_COLOR] = settings.textColorArgb
            preferences[KEY_BACKGROUND] = settings.backgroundArgb
            preferences[KEY_BOLD] = settings.bold
            preferences[KEY_OUTLINE] = settings.outline
            preferences[KEY_BOTTOM_MARGIN] = settings.bottomMarginDp
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("subtitle_enabled")
        val KEY_TEXT_SIZE = floatPreferencesKey("subtitle_text_size")
        val KEY_TEXT_COLOR = longPreferencesKey("subtitle_text_color")
        val KEY_BACKGROUND = longPreferencesKey("subtitle_background")
        val KEY_BOLD = booleanPreferencesKey("subtitle_bold")
        val KEY_OUTLINE = booleanPreferencesKey("subtitle_outline")
        val KEY_BOTTOM_MARGIN = floatPreferencesKey("subtitle_bottom_margin")
    }
}
