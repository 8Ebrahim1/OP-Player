package com.opplayer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opplayer.app.data.EpisodePattern
import com.opplayer.app.data.LibraryProgressUpdater
import com.opplayer.app.data.LibraryRepository
import com.opplayer.app.data.VideoItem
import com.opplayer.app.player.Clock
import com.opplayer.app.player.PlaybackRequest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The secondary constructor below is required, not cosmetic.
 *
 * `viewModel()` builds this class reflectively through
 * `AndroidViewModelFactory`, which calls `getConstructor(Application::class)`.
 * A Kotlin default argument does not produce that constructor: the bytecode
 * only carries `(Application, Clock)` plus a synthetic bridge that takes a
 * bitmask, so the lookup fails with `NoSuchMethodException` and the app dies on
 * the first frame. The overload is declared explicitly rather than through
 * `@JvmOverloads` so it is visible in the source and survives any refactor.
 * Tests keep injecting a fake [Clock] through the primary constructor.
 */
class LibraryViewModel(
    application: Application,
    private val clock: Clock
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, Clock.SYSTEM)

    private val repository = LibraryRepository(application)

    init {
        // One-off repair of data written by older versions, recorded under its
        // own key so it runs exactly once and never depends on another write.
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
            repository.updateLibrary { current ->
                current + VideoItem.create(
                    title = title.ifBlank { url.substringAfterLast('/') },
                    url = url.trim(),
                    subtitleUrl = subtitleUrl?.trim()?.takeIf { it.isNotBlank() },
                    addedAt = clock.currentTimeMillis(),
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
            // The progress store is separate now, so it has to be cleaned up too,
            // otherwise a re-added URL would inherit a stale position.
            repository.forgetLibraryProgress(setOf(id))
        }
    }

    /**
     * Stores the resume position of a library item.
     *
     * Writes only the small progress map; the merge rules live in
     * [LibraryProgressUpdater].
     */
    fun saveLibraryProgress(request: PlaybackRequest, positionMs: Long) {
        val nowMs = clock.currentTimeMillis()
        viewModelScope.launch {
            repository.saveLibraryProgress(request, positionMs, nowMs)
        }
    }

    fun resetProgress(id: String) {
        viewModelScope.launch { repository.resetLibraryProgress(id) }
    }

    fun saveDevicePosition(uri: String, positionMs: Long) {
        viewModelScope.launch { repository.saveLocalPosition(uri, positionMs) }
    }

    /** Routes a saved position to the right store based on where the item came from. */
    fun saveProgress(request: PlaybackRequest, positionMs: Long) {
        when (request.source) {
            PlaybackRequest.Source.LIBRARY -> saveLibraryProgress(request, positionMs)
            PlaybackRequest.Source.DEVICE -> saveDevicePosition(request.key, positionMs)
        }
    }
}
