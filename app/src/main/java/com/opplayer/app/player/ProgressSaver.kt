package com.opplayer.app.player

/**
 * Persists the resume position of a playback request.
 *
 * The player depends on this interface only, so the Compose layer can hand in a
 * repository backed implementation and tests can hand in a recording fake.
 */
fun interface ProgressSaver {
    fun save(request: PlaybackRequest, positionMs: Long)
}
