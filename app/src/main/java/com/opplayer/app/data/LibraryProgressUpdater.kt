package com.opplayer.app.data

import com.opplayer.app.player.PlaybackRequest

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

    fun applyTo(
        items: List<VideoItem>,
        request: PlaybackRequest,
        positionMs: Long,
        nowMs: Long
    ): List<VideoItem> = items.map { item ->
        if (item.id == request.key) apply(item, request, positionMs, nowMs) else item
    }

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
