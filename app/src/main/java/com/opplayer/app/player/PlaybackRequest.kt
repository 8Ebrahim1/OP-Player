package com.opplayer.app.player

import com.opplayer.app.data.EpisodePattern

data class PlaybackRequest(
    val key: String,
    val title: String,
    val uri: String,
    val subtitleUrl: String? = null,
    val startPositionMs: Long = 0L,
    val source: Source,
    val pattern: EpisodePattern? = null,
    val episodeLabel: String? = null
) {
    enum class Source { LIBRARY, DEVICE }
}
