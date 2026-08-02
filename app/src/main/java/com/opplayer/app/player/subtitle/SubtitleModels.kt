package com.opplayer.app.player.subtitle

/** A single timed subtitle line parsed from an external subtitle file. */
data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

/** A subtitle file that was found next to a video (or picked by the user). */
data class SubtitleFileCandidate(
    val uri: String,
    val name: String
)

/** A fully loaded and parsed external subtitle file. */
data class LoadedSubtitle(
    val uri: String,
    val name: String,
    val cues: List<SubtitleCue>
)
