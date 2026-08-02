package com.opplayer.app.data

import kotlinx.serialization.Serializable

/**
 * Playback progress of one library item.
 *
 * Stored separately from [VideoItem] on purpose: a resume position changes every
 * few seconds while title, URL and favourite flag almost never do. Keeping the
 * two apart means an autosave rewrites a small map instead of the whole library.
 */
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

/** Progress fields of this item, as they are persisted. */
fun VideoItem.progress(): LibraryProgress = LibraryProgress(
    positionMs = positionMs,
    lastPlayedAt = lastPlayedAt,
    currentUrl = currentUrl,
    currentPattern = currentPattern,
    currentLabel = currentLabel
)

/** The same item with every progress field cleared, ready for the static store. */
fun VideoItem.withoutProgress(): VideoItem = copy(
    positionMs = 0L,
    lastPlayedAt = 0L,
    currentUrl = null,
    currentPattern = null,
    currentLabel = null
)

/** The same item with [progress] applied, as the UI expects to see it. */
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

/**
 * Keeps the [limit] most recently played entries.
 *
 * Mirrors [trimProgress] for device videos so neither store can grow forever.
 */
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
