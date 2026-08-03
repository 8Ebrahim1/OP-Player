package com.opplayer.app.player

fun interface ProgressSaver {
    fun save(request: PlaybackRequest, positionMs: Long)
}
