package com.opplayer.app.ui.player

import androidx.annotation.StringRes
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlaybackStatus
import com.opplayer.app.player.VideoScaleMode

/** Live preview shown while the user drags horizontally to seek. */
data class SeekPreview(
    val positionMs: Long,
    val deltaMs: Long,
    val durationMs: Long
)

data class PlayerUiState(
    val request: PlaybackRequest,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val isFullscreen: Boolean = false,
    val scaleMode: VideoScaleMode = VideoScaleMode.FIT,
    val speed: Float = DEFAULT_SPEED,
    val autoNextEnabled: Boolean = true,
    val autoRotateEnabled: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val isResolvingEpisode: Boolean = false,
    val canNavigateEpisodes: Boolean = false,
    val videoAspect: Float = 0f,
    val seekPreview: SeekPreview? = null
) {

    val isBuffering: Boolean get() = status.isLoading

    @get:StringRes
    val errorRes: Int? get() = status.errorMessageRes

    val displayTitle: String
        get() = listOfNotNull(
            request.title.takeIf { it.isNotBlank() },
            request.episodeLabel?.takeIf { it.isNotBlank() }
        ).joinToString(TITLE_SEPARATOR)

    companion object {
        const val DEFAULT_SPEED = 1f
        const val TITLE_SEPARATOR = "  \u00b7  "
    }
}
