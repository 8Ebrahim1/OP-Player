package com.opplayer.app.ui.player

import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlayerEngine
import com.opplayer.app.player.PlayerEngineListener

/**
 * Playback commands, isolated from state.
 *
 * A very thin layer on purpose: it keeps the view model free of engine details
 * (clamping, seek steps, release bookkeeping) without adding a second source of
 * truth for UI state.
 */
class PlaybackController(private val engine: PlayerEngine) {

    var isReleased: Boolean = false
        private set

    val currentPosition: Long get() = if (isReleased) 0L else engine.currentPosition

    /** Duration in ms, or 0 when it is still unknown. */
    val duration: Long
        get() = if (isReleased) 0L else engine.duration.let { if (it > 0L) it else 0L }

    val isPlaying: Boolean get() = !isReleased && engine.isPlaying

    fun setListener(listener: PlayerEngineListener?) {
        if (!isReleased) engine.setListener(listener)
    }

    fun prepare(request: PlaybackRequest, startPositionMs: Long = request.startPositionMs) {
        if (!isReleased) engine.prepare(request, startPositionMs.coerceAtLeast(0L))
    }

    fun play() {
        if (!isReleased) engine.play()
    }

    fun pause() {
        if (!isReleased) engine.pause()
    }

    fun togglePlayPause() {
        if (isReleased) return
        if (engine.isPlaying) engine.pause() else engine.play()
    }

    fun seekTo(positionMs: Long) {
        if (!isReleased) engine.seekTo(positionMs.coerceAtLeast(0L))
    }

    /** Seeks relative to the current position, clamped to the media bounds. */
    fun seekBy(deltaMs: Long) {
        if (isReleased) return

        val duration = duration
        val target = (engine.currentPosition + deltaMs).coerceAtLeast(0L)
        engine.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
    }

    fun setSpeed(speed: Float) {
        if (!isReleased) engine.setSpeed(speed)
    }

    fun retry() {
        if (!isReleased) engine.retry()
    }

    fun setTextTracksEnabled(enabled: Boolean) {
        if (!isReleased) engine.setTextTracksEnabled(enabled)
    }

    fun selectEmbeddedTextTrack(index: Int) {
        if (!isReleased) engine.selectEmbeddedTextTrack(index)
    }

    /** Idempotent: releasing twice is a no-op, which the ownership tests rely on. */
    fun release() {
        if (isReleased) return

        isReleased = true
        engine.setListener(null)
        engine.release()
    }
}
