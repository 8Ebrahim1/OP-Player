package com.opplayer.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class VideoItem(
    val id: String = UUID.randomUUID().toString(),
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
}
