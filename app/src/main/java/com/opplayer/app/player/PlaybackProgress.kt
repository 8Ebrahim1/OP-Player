package com.opplayer.app.player

object PlaybackProgress {

    const val END_THRESHOLD_MS = 3_000L

    fun resumePosition(positionMs: Long, durationMs: Long): Long = when {
        positionMs <= 0L -> 0L
        durationMs > 0L && positionMs >= durationMs - END_THRESHOLD_MS -> 0L
        else -> positionMs
    }
}
