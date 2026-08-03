package com.opplayer.app.player.subtitle

data class EmbeddedSubtitleTimeline(
    val cues: List<SubtitleCue> = emptyList()
) {

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

    fun textAt(positionMs: Long, delayMs: Long = 0L): String? =
        cues.textAt(positionMs, delayMs.coerceAtLeast(0L))

    fun isEmpty(): Boolean = cues.isEmpty()

    private fun closeAt(source: List<SubtitleCue>, atMs: Long): List<SubtitleCue> {
        val last = source.lastOrNull() ?: return source
        if (last.endMs <= atMs) return source
        return source.dropLast(1) + last.copy(endMs = maxOf(last.startMs, atMs))
    }

    companion object {

        const val MAX_CUE_DURATION_MS = 10_000L

        const val MAX_CUES = 120
    }
}
