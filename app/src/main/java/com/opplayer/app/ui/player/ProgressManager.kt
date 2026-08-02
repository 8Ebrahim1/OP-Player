package com.opplayer.app.ui.player

import com.opplayer.app.player.PlaybackProgress
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.ProgressSaver

/**
 * Owns "where should this video resume from?".
 *
 * The saver is registered by whoever hosts the player (today: the Compose
 * screen, backed by `LibraryViewModel`) and cleared again on dispose, so a
 * detached UI can never be called back into.
 */
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

    /**
     * Stores the resume position, trimming positions that sit in the last few
     * seconds so the next play starts from the beginning instead of the credits.
     */
    fun save(request: PlaybackRequest, positionMs: Long, durationMs: Long) {
        val target = saver ?: return
        target.save(request, PlaybackProgress.resumePosition(positionMs, durationMs))
    }

    /** Stores [positionMs] verbatim, used when a new episode starts at zero. */
    fun saveExact(request: PlaybackRequest, positionMs: Long) {
        saver?.save(request, positionMs.coerceAtLeast(0L))
    }
}
