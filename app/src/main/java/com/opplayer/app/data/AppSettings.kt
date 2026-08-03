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

data class AppSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val layoutDirection: AppLayoutDirection = AppLayoutDirection.AUTO,
    val onboardingCompleted: Boolean = false
)

private val Context.appSettingsStore by preferencesDataStore(name = "op_player_app_settings")

class AppSettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.appSettingsStore.data.map { preferences ->
        val defaults = AppSettings()

        AppSettings(
            language = AppLanguage.fromStorageKey(preferences[KEY_LANGUAGE]),
            layoutDirection = AppLayoutDirection.fromStorageKey(preferences[KEY_DIRECTION]),
            onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED]
                ?: defaults.onboardingCompleted
        )
    }

    suspend fun save(settings: AppSettings) {
        context.appSettingsStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = settings.language.storageKey
            preferences[KEY_DIRECTION] = settings.layoutDirection.storageKey
            preferences[KEY_ONBOARDING_COMPLETED] = settings.onboardingCompleted
        }
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_DIRECTION = stringPreferencesKey("app_layout_direction")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
