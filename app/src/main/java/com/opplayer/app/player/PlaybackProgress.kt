package com.opplayer.app.player

/** Rules for the resume position that is stored for a video. */
object PlaybackProgress {

    /** Stopping this close to the end restarts the video instead of resuming in the credits. */
    const val END_THRESHOLD_MS = 3_000L

    fun resumePosition(positionMs: Long, durationMs: Long): Long = when {
        positionMs <= 0L -> 0L
        durationMs > 0L && positionMs >= durationMs - END_THRESHOLD_MS -> 0L
        else -> positionMs
    }
}
