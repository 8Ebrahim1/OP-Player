package com.opplayer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opplayer.app.data.EpisodePattern
import com.opplayer.app.data.LibraryRepository
import com.opplayer.app.data.VideoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)

    val library: StateFlow<List<VideoItem>> = repository.library
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val localPositions: StateFlow<Map<String, Long>> = repository.localPositions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun addVideo(
        title: String,
        url: String,
        subtitleUrl: String?,
        pattern: EpisodePattern? = null
    ) {
        viewModelScope.launch {
            repository.updateLibrary { current ->
                current + VideoItem(
                    title = title.ifBlank { url.substringAfterLast('/') },
                    url = url.trim(),
                    subtitleUrl = subtitleUrl?.trim()?.takeIf { it.isNotBlank() },
                    addedAt = System.currentTimeMillis(),
                    pattern = pattern
                )
            }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.updateLibrary { current ->
                current.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
            }
        }
    }

    fun removeVideo(id: String) {
        viewModelScope.launch {
            repository.updateLibrary { current -> current.filterNot { it.id == id } }
        }
    }

    fun saveLibraryProgress(
        id: String,
        url: String,
        pattern: EpisodePattern?,
        episodeLabel: String?,
        positionMs: Long
    ) {
        viewModelScope.launch {
            repository.updateLibrary { current ->
                current.map { item ->
                    if (item.id != id) return@map item

                    val isSameEpisode = item.currentUrl?.let { it == url } ?: (item.url == url)

                    item.copy(
                        positionMs = positionMs.coerceAtLeast(0L),
                        lastPlayedAt = System.currentTimeMillis(),
                        currentUrl = url,
                        currentPattern = pattern
                            ?: if (isSameEpisode) item.currentPattern else null,
                        currentLabel = episodeLabel
                            ?: if (isSameEpisode) item.currentLabel else null
                    )
                }
            }
        }
    }

    fun resetProgress(id: String) {
        viewModelScope.launch {
            repository.updateLibrary { current ->
                current.map { item ->
                    if (item.id != id) {
                        item
                    } else {
                        item.copy(
                            positionMs = 0L,
                            currentUrl = null,
                            currentPattern = null,
                            currentLabel = null
                        )
                    }
                }
            }
        }
    }

    fun saveDevicePosition(uri: String, positionMs: Long) {
        viewModelScope.launch { repository.saveLocalPosition(uri, positionMs) }
    }
}
