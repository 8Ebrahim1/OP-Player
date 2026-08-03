package com.opplayer.app.player.subtitle

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class SubtitleFileCandidate(
    val uri: String,
    val name: String
)

data class LoadedSubtitle(
    val uri: String,
    val name: String,
    val cues: List<SubtitleCue>
)
