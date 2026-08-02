package com.opplayer.app.player.fakes

import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlaybackStatus
import com.opplayer.app.player.PlayerEngine
import com.opplayer.app.player.PlayerEngineListener
import com.opplayer.app.player.subtitle.EmbeddedTrackInfo

/**
 * In-memory [PlayerEngine].
 *
 * Records everything the view model asks of it so coordination can be asserted
 * on the JVM, without an emulator or a real ExoPlayer.
 */
class FakePlayerEngine : PlayerEngine {

    data class Prepared(val request: PlaybackRequest, val startPositionMs: Long)

    val prepared = mutableListOf<Prepared>()
    val seeks = mutableListOf<Long>()
    val speeds = mutableListOf<Float>()

    var listener: PlayerEngineListener? = null
        private set

    override var currentPosition: Long = 0L
    override var duration: Long = 0L
    override var isPlaying: Boolean = false

    var playCount: Int = 0
        private set
    var pauseCount: Int = 0
        private set
    var retryCount: Int = 0
        private set
    var releaseCount: Int = 0
        private set

    var textTracksEnabled: Boolean = true
        private set
    var selectedTextTrack: Int? = null
        private set

    override fun setListener(listener: PlayerEngineListener?) {
        this.listener = listener
    }

    override fun prepare(request: PlaybackRequest, startPositionMs: Long) {
        prepared += Prepared(request, startPositionMs)
        currentPosition = startPositionMs
    }

    override fun play() {
        playCount++
        isPlaying = true
        listener?.onIsPlayingChanged(true)
    }

    override fun pause() {
        pauseCount++
        isPlaying = false
        listener?.onIsPlayingChanged(false)
    }

    override fun seekTo(positionMs: Long) {
        seeks += positionMs
        currentPosition = positionMs
    }

    override fun setSpeed(speed: Float) {
        speeds += speed
    }

    override fun retry() {
        retryCount++
    }

    override fun setTextTracksEnabled(enabled: Boolean) {
        textTracksEnabled = enabled
    }

    override fun selectEmbeddedTextTrack(index: Int) {
        selectedTextTrack = index
    }

    override fun release() {
        releaseCount++
        listener = null
    }

    // ------------------------------------------------------------- test hooks

    fun emitStatus(status: PlaybackStatus) {
        listener?.onStatusChanged(status)
    }

    fun emitIsPlaying(playing: Boolean) {
        isPlaying = playing
        listener?.onIsPlayingChanged(playing)
    }

    fun emitSeek() {
        listener?.onPositionDiscontinuity(true)
    }

    fun emitCue(atMs: Long, text: String?) {
        listener?.onEmbeddedCue(atMs, text)
    }

    fun emitTracks(tracks: List<EmbeddedTrackInfo>) {
        listener?.onEmbeddedTracksChanged(tracks)
    }
}
