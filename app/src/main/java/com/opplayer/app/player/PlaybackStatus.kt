package com.opplayer.app.player

import androidx.annotation.StringRes

/**
 * Explicit playback lifecycle state.
 *
 * Replaces the old `isBuffering` + `errorRes` pair, which allowed impossible
 * combinations such as "buffering and failed at the same time".
 */
sealed interface PlaybackStatus {

    /** Nothing is loaded yet, or playback was torn down. */
    data object Idle : PlaybackStatus

    /** A media item was handed to the engine and the first frame is not ready. */
    data object Preparing : PlaybackStatus

    /** Playing, or ready to play. */
    data object Ready : PlaybackStatus

    /** Re-buffering during playback. */
    data object Buffering : PlaybackStatus

    /** The current item played to the end. */
    data object Ended : PlaybackStatus

    /** Playback failed; [messageRes] is the user facing reason. */
    data class Error(@StringRes val messageRes: Int) : PlaybackStatus

    /** True while the UI should show a spinner. */
    val isLoading: Boolean
        get() = this is Preparing || this is Buffering

    /** The error message to render, or null when playback is healthy. */
    @get:StringRes
    val errorMessageRes: Int?
        get() = (this as? Error)?.messageRes
}
