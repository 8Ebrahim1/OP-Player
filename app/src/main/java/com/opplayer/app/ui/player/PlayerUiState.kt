package com.opplayer.app.ui.player

import androidx.annotation.StringRes
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlaybackStatus
import com.opplayer.app.player.VideoScaleMode

/**
 * Everything the player UI needs in order to draw itself.
 *
 * The state is plain data with no Android or media3 types, which keeps it easy
 * to assert on in tests and cheap to compare during recomposition.
 *
 * Loading and failure are modelled by a single [PlaybackStatus] rather than an
 * `isBuffering` flag next to an error code, so contradictory combinations such
 * as "buffering and failed at once" are unrepresentable.
 */
data class PlayerUiState(
    val request: PlaybackRequest,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val isFullscreen: Boolean = false,
    val scaleMode: VideoScaleMode = VideoScaleMode.ZOOM,
    val speed: Float = DEFAULT_SPEED,
    val autoNextEnabled: Boolean = true,
    val autoRotateEnabled: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val isResolvingEpisode: Boolean = false,
    val canNavigateEpisodes: Boolean = false,
    val videoAspect: Float = 0f
) {

    /** True while the UI should show the spinner. */
    val isBuffering: Boolean get() = status.isLoading

    /** The error to render, or null when playback is healthy. */
    @get:StringRes
    val errorRes: Int? get() = status.errorMessageRes

    /** Video name plus episode label, as shown in the top bar. */
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
