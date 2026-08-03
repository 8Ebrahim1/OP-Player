package com.opplayer.app.data

import kotlinx.serialization.Serializable

@Serializable
data class LibraryProgress(
    val positionMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val currentUrl: String? = null,
    val currentPattern: EpisodePattern? = null,
    val currentLabel: String? = null
) {
    val isEmpty: Boolean
        get() = positionMs <= 0L &&
            lastPlayedAt <= 0L &&
            currentUrl == null &&
            currentPattern == null &&
            currentLabel == null
}

fun VideoItem.progress(): LibraryProgress = LibraryProgress(
    positionMs = positionMs,
    lastPlayedAt = lastPlayedAt,
    currentUrl = currentUrl,
    currentPattern = currentPattern,
    currentLabel = currentLabel
)

fun VideoItem.withoutProgress(): VideoItem = copy(
    positionMs = 0L,
    lastPlayedAt = 0L,
    currentUrl = null,
    currentPattern = null,
    currentLabel = null
)

fun VideoItem.withProgress(progress: LibraryProgress?): VideoItem {
    if (progress == null) return this

    return copy(
        positionMs = progress.positionMs,
        lastPlayedAt = progress.lastPlayedAt,
        currentUrl = progress.currentUrl,
        currentPattern = progress.currentPattern,
        currentLabel = progress.currentLabel
    )
}

fun trimLibraryProgress(
    entries: Map<String, LibraryProgress>,
    limit: Int
): Map<String, LibraryProgress> {
    if (limit <= 0) return emptyMap()
    if (entries.size <= limit) return entries

    return entries.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, LibraryProgress>> { it.value.lastPlayedAt }
                .thenByDescending { it.value.positionMs }
                .thenBy { it.key }
        )
        .take(limit)
        .associate { it.key to it.value }
}
