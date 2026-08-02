package com.opplayer.app.player.subtitle

/** How many overlapping cues are scanned back through before giving up. */
private const val OVERLAP_SCAN_LIMIT = 8

/**
 * Text that should be visible at [positionMs], or null when nothing is due.
 *
 * [offsetMs] shifts the subtitles in time: a positive value shows them later.
 * The receiver must be sorted by [SubtitleCue.startMs] (see [sortedForPlayback]);
 * the lookup is a binary search, so files with thousands of lines stay cheap to
 * query on every position tick.
 */
fun List<SubtitleCue>.textAt(positionMs: Long, offsetMs: Long = 0L): String? {
    if (isEmpty()) return null

    val target = positionMs - offsetMs
    var low = 0
    var high = size - 1
    var candidate = -1

    while (low <= high) {
        val middle = (low + high) ushr 1
        if (this[middle].startMs <= target) {
            candidate = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }

    if (candidate < 0) return null

    var index = candidate
    var scanned = 0

    while (index >= 0 && scanned < OVERLAP_SCAN_LIMIT) {
        val cue = this[index]
        if (target <= cue.endMs) return cue.text.takeIf { it.isNotBlank() }
        index--
        scanned++
    }

    return null
}

/** Sorts cues so [textAt] can binary search them. */
fun List<SubtitleCue>.sortedForPlayback(): List<SubtitleCue> = sortedBy { it.startMs }
