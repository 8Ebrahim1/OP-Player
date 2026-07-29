package com.opplayer.app.data

import kotlinx.serialization.Serializable

@Serializable
data class EpisodePattern(
    val prefix: String,
    val suffix: String,
    val episode: Int,
    val pad: Int = 1,
    val step: Int = 1
) {
    val url: String get() = urlFor(episode)

    fun urlFor(value: Int): String =
        prefix + value.toString().padStart(pad.coerceAtLeast(1), '0') + suffix

    fun next(): EpisodePattern? {
        val value = episode + step
        return if (value in 0..MAX_EPISODE) copy(episode = value) else null
    }

    fun previous(): EpisodePattern? {
        val value = episode - step
        return if (value >= 0) copy(episode = value) else null
    }

    fun label(): String = "E" + episode.toString().padStart(2, '0')

    private companion object {
        const val MAX_EPISODE = 9999
    }
}
