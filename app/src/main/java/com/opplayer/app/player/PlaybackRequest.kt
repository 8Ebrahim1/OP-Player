package com.opplayer.app.player

import android.os.Parcelable
import com.opplayer.app.data.EpisodePattern
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaybackRequest(
    val key: String,
    val title: String,
    val uri: String,
    val subtitleUrl: String? = null,
    val startPositionMs: Long = 0L,
    val source: Source,
    val pattern: EpisodePattern? = null,
    val episodeLabel: String? = null
) : Parcelable {

    enum class Source { LIBRARY, DEVICE }

    fun isSameItemAs(other: PlaybackRequest): Boolean =
        key == other.key && uri == other.uri
}
