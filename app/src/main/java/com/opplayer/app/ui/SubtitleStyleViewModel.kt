package com.opplayer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opplayer.app.data.SubtitleStyleRepository
import com.opplayer.app.data.SubtitleStyleSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SubtitleStyleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubtitleStyleRepository(application)

    private val _settings = MutableStateFlow(SubtitleStyleSettings())
    val settings: StateFlow<SubtitleStyleSettings> = _settings.asStateFlow()

    init {

        // Collected continuously so a style saved from another screen stays in sync.
        viewModelScope.launch {
            repository.settings
                .catch { emit(SubtitleStyleSettings()) }
                .collect { stored -> _settings.value = stored }
        }
    }

    fun update(transform: (SubtitleStyleSettings) -> SubtitleStyleSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        viewModelScope.launch { runCatching { repository.save(updated) } }
    }

    fun reset() = update { SubtitleStyleSettings() }
}
