package com.opplayer.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

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

    fun normalized(): VideoItem =
        if (id.isNotBlank()) this else copy(id = legacyIdFor(url, title))

    companion object {

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

        fun legacyIdFor(url: String, title: String): String {
            val seed = url.ifBlank { title }
            return "legacy-" + UUID.nameUUIDFromBytes(seed.toByteArray()).toString()
        }
    }
}
