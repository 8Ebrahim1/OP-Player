package com.opplayer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opplayer.app.data.SubtitleStyleRepository
import com.opplayer.app.data.SubtitleStyleSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Holds the persisted subtitle appearance, shared by the player and the tab bar entry. */
class SubtitleStyleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubtitleStyleRepository(application)

    private val _settings = MutableStateFlow(SubtitleStyleSettings())
    val settings: StateFlow<SubtitleStyleSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = runCatching { repository.settings.first() }
                .getOrDefault(SubtitleStyleSettings())
        }
    }

    fun update(transform: (SubtitleStyleSettings) -> SubtitleStyleSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        viewModelScope.launch { runCatching { repository.save(updated) } }
    }

    fun reset() = update { SubtitleStyleSettings() }
}
