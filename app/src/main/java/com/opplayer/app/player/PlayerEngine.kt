package com.opplayer.app.player

import com.opplayer.app.player.subtitle.EmbeddedTrackInfo

/**
 * Events pushed out of a [PlayerEngine].
 *
 * Every callback has a default no-op body so a collaborator can observe only
 * the events it cares about.
 */
interface PlayerEngineListener {

    fun onStatusChanged(status: PlaybackStatus) = Unit

    fun onIsPlayingChanged(isPlaying: Boolean) = Unit

    /** [isSeek] is true only for user or programmatic seeks, not for item transitions. */
    fun onPositionDiscontinuity(isSeek: Boolean) = Unit

    /** Width / height ratio of the current video, or 0 when it is unknown. */
    fun onVideoAspectChanged(aspect: Float) = Unit

    /** An embedded (in container) cue group; [text] is null when the cue is cleared. */
    fun onEmbeddedCue(atMs: Long, text: String?) = Unit

    fun onEmbeddedTracksChanged(tracks: List<EmbeddedTrackInfo>) = Unit
}

/**
 * The playback surface the app depends on.
 *
 * This is deliberately the smallest abstraction that covers everything the view
 * model needs, so playback coordination can be unit tested with a fake engine
 * instead of a real ExoPlayer instance.
 */
interface PlayerEngine {

    val currentPosition: Long

    val duration: Long

    val isPlaying: Boolean

    fun setListener(listener: PlayerEngineListener?)

    /** Loads [request] and starts playing from [startPositionMs]. */
    fun prepare(request: PlaybackRequest, startPositionMs: Long = 0L)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    /** Re-prepares the current item after an error. */
    fun retry()

    fun setTextTracksEnabled(enabled: Boolean)

    /** Selects the embedded text track at [index] as reported by [PlayerEngineListener]. */
    fun selectEmbeddedTextTrack(index: Int)

    fun release()
}
