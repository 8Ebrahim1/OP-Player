package com.opplayer.app.ui.player

import com.opplayer.app.player.PlaybackProgress
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.ProgressSaver

class ProgressManager(saver: ProgressSaver? = null) {

    @Volatile
    private var saver: ProgressSaver? = saver

    val hasSaver: Boolean get() = saver != null

    fun setSaver(saver: ProgressSaver?) {
        this.saver = saver
    }

    fun clearSaver() {
        saver = null
    }

    fun save(request: PlaybackRequest, positionMs: Long, durationMs: Long) {
        val target = saver ?: return
        target.save(request, PlaybackProgress.resumePosition(positionMs, durationMs))
    }

    fun saveExact(request: PlaybackRequest, positionMs: Long) {
        saver?.save(request, positionMs.coerceAtLeast(0L))
    }
}
