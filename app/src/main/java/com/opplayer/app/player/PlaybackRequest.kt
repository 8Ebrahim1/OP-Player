package com.opplayer.app.player

import android.os.Parcelable
import com.opplayer.app.data.EpisodePattern
import kotlinx.parcelize.Parcelize

/**
 * Everything needed to start playing one item.
 *
 * Parcelable so the hosting composable can keep it in `rememberSaveable` and
 * survive configuration changes and process death.
 */
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

    /** True when [other] points at the exact same item as this request. */
    fun isSameItemAs(other: PlaybackRequest): Boolean =
        key == other.key && uri == other.uri
}
