package com.opplayer.app.player.subtitle

/**
 * Cue history of an embedded (in container) text track.
 *
 * media3 reports embedded cues as "this is what should be on screen right now"
 * and never reports an end time, so a cue is closed as soon as the next cue
 * group arrives and expires after [MAX_CUE_DURATION_MS] at the latest.
 * Without that, the last line before a long silence stayed on screen forever.
 *
 * Keeping the history (instead of only the current line) is what makes the
 * subtitle delay slider work for embedded tracks.
 */
data class EmbeddedSubtitleTimeline(
    val cues: List<SubtitleCue> = emptyList()
) {

    /**
     * Applies a cue group emitted at [atMs].
     *
     * A null or blank [text] is a "clear" event: it only closes the open cue.
     * Cues that start after [atMs] are dropped, which keeps the timeline correct
     * after a backwards seek.
     */
    fun withCueGroup(atMs: Long, text: String?): EmbeddedSubtitleTimeline {
        val startMs = atMs.coerceAtLeast(0L)
        val kept = closeAt(cues.filter { it.startMs <= startMs }, startMs)

        if (text.isNullOrBlank()) {
            return if (kept == cues) this else EmbeddedSubtitleTimeline(kept)
        }

        val cue = SubtitleCue(
            startMs = startMs,
            endMs = startMs + MAX_CUE_DURATION_MS,
            text = text
        )

        return EmbeddedSubtitleTimeline((kept + cue).takeLast(MAX_CUES))
    }

    /** Text to render at [positionMs]. Embedded tracks can only be delayed, never advanced. */
    fun textAt(positionMs: Long, delayMs: Long = 0L): String? =
        cues.textAt(positionMs, delayMs.coerceAtLeast(0L))

    fun isEmpty(): Boolean = cues.isEmpty()

    private fun closeAt(source: List<SubtitleCue>, atMs: Long): List<SubtitleCue> {
        val last = source.lastOrNull() ?: return source
        if (last.endMs <= atMs) return source
        return source.dropLast(1) + last.copy(endMs = maxOf(last.startMs, atMs))
    }

    companion object {
        /** Hard cap for a cue that is never followed by another one. */
        const val MAX_CUE_DURATION_MS = 10_000L

        /** Enough history for the largest supported subtitle delay. */
        const val MAX_CUES = 120
    }
}
