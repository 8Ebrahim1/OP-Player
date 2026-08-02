package com.opplayer.app.data

import com.opplayer.app.player.PlaybackRequest

/**
 * Applies a playback position to a library item.
 *
 * Pure on purpose: the clock is passed in as [nowMs] so the rules around
 * "is this still the same episode?" can be unit tested deterministically,
 * without reaching for `System.currentTimeMillis()`.
 */
object LibraryProgressUpdater {

    fun apply(
        item: VideoItem,
        request: PlaybackRequest,
        positionMs: Long,
        nowMs: Long
    ): VideoItem {
        val sameEpisode = item.currentUrl?.let { it == request.uri } ?: (item.url == request.uri)

        return item.copy(
            positionMs = positionMs.coerceAtLeast(0L),
            lastPlayedAt = nowMs,
            currentUrl = request.uri,
            currentPattern = request.pattern ?: item.currentPattern.takeIf { sameEpisode },
            currentLabel = request.episodeLabel ?: item.currentLabel.takeIf { sameEpisode }
        )
    }

    /** Applies [apply] to the matching item only, leaving the rest of the list untouched. */
    fun applyTo(
        items: List<VideoItem>,
        request: PlaybackRequest,
        positionMs: Long,
        nowMs: Long
    ): List<VideoItem> = items.map { item ->
        if (item.id == request.key) apply(item, request, positionMs, nowMs) else item
    }

    /**
     * Applies a position to the standalone progress record of an item.
     *
     * Same rules as [apply], but it touches only the progress store, so an
     * autosave never rewrites the whole library.
     */
    fun applyProgress(
        current: LibraryProgress?,
        request: PlaybackRequest,
        positionMs: Long,
        nowMs: Long
    ): LibraryProgress {
        val previous = current ?: LibraryProgress()
        val sameEpisode = previous.currentUrl?.let { it == request.uri } ?: true

        return LibraryProgress(
            positionMs = positionMs.coerceAtLeast(0L),
            lastPlayedAt = nowMs,
            currentUrl = request.uri,
            currentPattern = request.pattern ?: previous.currentPattern.takeIf { sameEpisode },
            currentLabel = request.episodeLabel ?: previous.currentLabel.takeIf { sameEpisode }
        )
    }

    /** Clears every trace of progress for [id]. */
    fun reset(items: List<VideoItem>, id: String): List<VideoItem> = items.map { item ->
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
