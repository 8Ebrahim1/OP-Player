package com.opplayer.app.player.fakes

import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.ProgressSaver

class FakeProgressSaver : ProgressSaver {

    val saved = mutableListOf<Pair<PlaybackRequest, Long>>()

    val lastPosition: Long?
        get() = saved.lastOrNull()?.second

    override fun save(request: PlaybackRequest, positionMs: Long) {
        saved += request to positionMs
    }
}
