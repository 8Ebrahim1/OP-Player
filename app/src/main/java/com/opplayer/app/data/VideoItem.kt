package com.opplayer.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * One entry of the online library.
 *
 * [id] is deliberately **not** defaulted to a random UUID: entries written by
 * old versions of the app have no id at all, and generating a fresh one on every
 * decode gave the same video a new identity each launch, which detached it from
 * its saved progress. A blank id is repaired deterministically by [normalized].
 * Use [create] for genuinely new items.
 */
@Serializable
data class VideoItem(
    val id: String = "",
    val title: String,
    val url: String,
    val subtitleUrl: String? = null,
    val isFavorite: Boolean = false,
    val positionMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val addedAt: Long = 0L,
    val pattern: EpisodePattern? = null,
    val currentUrl: String? = null,
    val currentPattern: EpisodePattern? = null,
    val currentLabel: String? = null
) {
    val hasProgress: Boolean
        get() = positionMs > 0L || (currentUrl != null && currentUrl != url)

    /**
     * The same item with a stable id.
     *
     * The fallback is derived from the URL, so the same legacy entry always maps
     * to the same id, on this device and on the next launch.
     */
    fun normalized(): VideoItem =
        if (id.isNotBlank()) this else copy(id = legacyIdFor(url, title))

    companion object {
        /** Creates a brand new item with a fresh identity. */
        fun create(
            title: String,
            url: String,
            subtitleUrl: String? = null,
            addedAt: Long = 0L,
            pattern: EpisodePattern? = null
        ): VideoItem = VideoItem(
            id = UUID.randomUUID().toString(),
            title = title,
            url = url,
            subtitleUrl = subtitleUrl,
            addedAt = addedAt,
            pattern = pattern
        )

        /** Deterministic id for an entry that was stored before ids existed. */
        fun legacyIdFor(url: String, title: String): String {
            val seed = url.ifBlank { title }
            return "legacy-" + UUID.nameUUIDFromBytes(seed.toByteArray()).toString()
        }
    }
}
