package com.opplayer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opplayer.app.R
import com.opplayer.app.data.EpisodePattern
import com.opplayer.app.data.LibraryProgressUpdater
import com.opplayer.app.data.LibraryRepository
import com.opplayer.app.data.VideoItem
import com.opplayer.app.player.Clock
import com.opplayer.app.player.PlaybackRequest
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    application: Application,
    private val clock: Clock
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, Clock.SYSTEM)

    private val repository = LibraryRepository(application)

    private val messageChannel = Channel<Int>(
        capacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** String resources describing a failed write, so the UI can tell the user about it. */
    val messages: Flow<Int> = messageChannel.receiveAsFlow()

    init {

        viewModelScope.launch { repository.migrateLegacyDataIfNeeded() }
    }

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
            val written = repository.updateLibrary { current ->
                current + VideoItem.create(
                    title = title.ifBlank { url.substringAfterLast('/') },
                    url = url.trim(),
                    subtitleUrl = subtitleUrl?.trim()?.takeIf { it.isNotBlank() },
                    addedAt = clock.currentTimeMillis(),
                    pattern = pattern
                )
            }

            reportWriteResult(written)
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            val written = repository.updateLibrary { current ->
                current.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
            }

            reportWriteResult(written)
        }
    }

    fun removeVideo(id: String) {
        viewModelScope.launch {
            val written = repository.updateLibrary { current ->
                current.filterNot { it.id == id }
            }

            repository.forgetLibraryProgress(setOf(id))
            reportWriteResult(written)
        }
    }

    /**
     * A refused write means the stored library was unreadable and got backed up instead of
     * being overwritten. Silently swallowing that made the app look like it had saved the
     * change, so the failure is surfaced to the UI instead.
     */
    private suspend fun reportWriteResult(written: Boolean) {
        if (!written) messageChannel.send(R.string.library_write_failed)
    }

    fun saveLibraryProgress(request: PlaybackRequest, positionMs: Long) {
        val nowMs = clock.currentTimeMillis()
        viewModelScope.launch {
            repository.saveLibraryProgress(request, positionMs, nowMs)
        }
    }

    fun resetProgress(id: String) {
        viewModelScope.launch { repository.resetLibraryProgress(id) }
    }

    /**
     * A VIEW intent from another app carries no resume position, and [localPositions] starts out
     * empty, so the stored value is read straight from the repository instead.
     */
    suspend fun devicePosition(uri: String): Long =
        repository.localPositions.first()[uri] ?: 0L

    fun saveDevicePosition(uri: String, positionMs: Long) {
        viewModelScope.launch { repository.saveLocalPosition(uri, positionMs) }
    }

    fun saveProgress(request: PlaybackRequest, positionMs: Long) {
        when (request.source) {
            PlaybackRequest.Source.LIBRARY -> saveLibraryProgress(request, positionMs)
            PlaybackRequest.Source.DEVICE -> saveDevicePosition(request.key, positionMs)
        }
    }
}
