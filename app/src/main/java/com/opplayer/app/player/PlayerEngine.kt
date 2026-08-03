package com.opplayer.app.player

import com.opplayer.app.player.subtitle.EmbeddedTrackInfo

interface PlayerEngineListener {

    fun onStatusChanged(status: PlaybackStatus) = Unit

    fun onIsPlayingChanged(isPlaying: Boolean) = Unit

    fun onPositionDiscontinuity(isSeek: Boolean) = Unit

    fun onVideoAspectChanged(aspect: Float) = Unit

    fun onEmbeddedCue(atMs: Long, text: String?) = Unit

    fun onEmbeddedTracksChanged(tracks: List<EmbeddedTrackInfo>) = Unit
}

interface PlayerEngine {

    val currentPosition: Long

    val duration: Long

    val isPlaying: Boolean

    fun setListener(listener: PlayerEngineListener?)

    fun prepare(request: PlaybackRequest, startPositionMs: Long = 0L)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    fun retry()

    fun setTextTracksEnabled(enabled: Boolean)

    fun selectEmbeddedTextTrack(index: Int)

    fun release()
}
