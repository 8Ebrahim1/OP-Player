package com.opplayer.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage(val storageKey: String) {

    SYSTEM("system"),
    PERSIAN("fa"),
    ENGLISH("en");

    companion object {
        fun fromStorageKey(value: String?): AppLanguage =
            entries.firstOrNull { it.storageKey == value } ?: SYSTEM
    }
}

enum class AppLayoutDirection(val storageKey: String) {
    AUTO("auto"),
    RTL("rtl"),
    LTR("ltr");

    companion object {
        fun fromStorageKey(value: String?): AppLayoutDirection =
            entries.firstOrNull { it.storageKey == value } ?: AUTO
    }
}

enum class VideoSortOrder(val storageKey: String) {
    NAME_ASC("name_asc"),
    NAME_DESC("name_desc"),
    DATE_NEWEST("date_newest"),
    DATE_OLDEST("date_oldest");

    companion object {
        fun fromStorageKey(value: String?): VideoSortOrder =
            entries.firstOrNull { it.storageKey == value } ?: NAME_ASC
    }
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val layoutDirection: AppLayoutDirection = AppLayoutDirection.AUTO,
    val onboardingCompleted: Boolean = false,
    val videoSortOrder: VideoSortOrder = VideoSortOrder.NAME_ASC
)

private val Context.appSettingsStore by preferencesDataStore(name = "op_player_app_settings")

class AppSettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.appSettingsStore.data.map { preferences ->
        val defaults = AppSettings()

        AppSettings(
            language = AppLanguage.fromStorageKey(preferences[KEY_LANGUAGE]),
            layoutDirection = AppLayoutDirection.fromStorageKey(preferences[KEY_DIRECTION]),
            onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED]
                ?: defaults.onboardingCompleted,
            videoSortOrder = VideoSortOrder.fromStorageKey(preferences[KEY_VIDEO_SORT_ORDER])
        )
    }

    suspend fun save(settings: AppSettings) {
        context.appSettingsStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = settings.language.storageKey
            preferences[KEY_DIRECTION] = settings.layoutDirection.storageKey
            preferences[KEY_ONBOARDING_COMPLETED] = settings.onboardingCompleted
            preferences[KEY_VIDEO_SORT_ORDER] = settings.videoSortOrder.storageKey
        }
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_DIRECTION = stringPreferencesKey("app_layout_direction")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_VIDEO_SORT_ORDER = stringPreferencesKey("video_sort_order")
    }
}
