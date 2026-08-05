package com.opplayer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opplayer.app.data.AppLanguage
import com.opplayer.app.data.AppLayoutDirection
import com.opplayer.app.data.AppSettings
import com.opplayer.app.data.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppSettingsRepository(application)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {

        // Collected continuously so changes written elsewhere are reflected instead of
        // reading the stored value only once at startup.
        viewModelScope.launch {
            repository.settings
                .catch { emit(AppSettings()) }
                .collect { stored ->
                    _settings.value = stored
                    _loaded.value = true
                }
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        if (updated == _settings.value) return

        _settings.value = updated
        viewModelScope.launch { runCatching { repository.save(updated) } }
    }

    fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }

    fun setLayoutDirection(direction: AppLayoutDirection) =
        update { it.copy(layoutDirection = direction) }

    fun completeOnboarding() = update { it.copy(onboardingCompleted = true) }

    fun restartOnboarding() = update { it.copy(onboardingCompleted = false) }
}
