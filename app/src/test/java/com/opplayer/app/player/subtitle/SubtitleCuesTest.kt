package com.opplayer.app.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleCuesTest {

    private val cues = listOf(
        SubtitleCue(startMs = 1_000L, endMs = 2_000L, text = "first"),
        SubtitleCue(startMs = 4_000L, endMs = 6_000L, text = "second")
    )

    @Test
    fun `an empty list never shows anything`() {
        assertNull(emptyList<SubtitleCue>().textAt(1_000L))
    }

    @Test
    fun `nothing is shown before the first cue`() {
        assertNull(cues.textAt(500L))
    }

    @Test
    fun `the cue covering the position is shown`() {
        assertEquals("first", cues.textAt(1_500L))
        assertEquals("second", cues.textAt(5_000L))
    }

    @Test
    fun `cue bounds are inclusive`() {
        assertEquals("first", cues.textAt(1_000L))
        assertEquals("first", cues.textAt(2_000L))
    }

    @Test
    fun `nothing is shown in the gap between cues`() {
        assertNull(cues.textAt(3_000L))
    }

    @Test
    fun `nothing is shown after the last cue`() {
        assertNull(cues.textAt(60_000L))
    }

    @Test
    fun `a positive offset shows the cue later`() {
        assertEquals("first", cues.textAt(2_500L, offsetMs = 1_000L))
        assertNull(cues.textAt(1_500L, offsetMs = 1_000L))
    }

    @Test
    fun `a negative offset shows the cue earlier`() {
        assertEquals("first", cues.textAt(500L, offsetMs = -1_000L))
    }

    @Test
    fun `blank cues are treated as empty`() {
        val blank = listOf(SubtitleCue(startMs = 0L, endMs = 5_000L, text = "   "))
        assertNull(blank.textAt(1_000L))
    }

    @Test
    fun `an overlapping long cue is still found behind a short one`() {
        val overlapping = listOf(
            SubtitleCue(startMs = 0L, endMs = 5_000L, text = "long"),
            SubtitleCue(startMs = 1_000L, endMs = 2_000L, text = "short")
        )

        assertEquals("short", overlapping.textAt(1_500L))
        assertEquals("long", overlapping.textAt(4_000L))
    }

    @Test
    fun `sortedForPlayback orders cues by start time`() {
        val unsorted = listOf(
            SubtitleCue(startMs = 5_000L, endMs = 6_000L, text = "b"),
            SubtitleCue(startMs = 1_000L, endMs = 2_000L, text = "a")
        )

        assertEquals(listOf("a", "b"), unsorted.sortedForPlayback().map { it.text })
    }

    @Test
    fun `lookup works on a large subtitle file`() {
        val many = (0 until 5_000).map { index ->
            val start = index * 2_000L
            SubtitleCue(startMs = start, endMs = start + 1_500L, text = "line $index")
        }

        assertEquals("line 0", many.textAt(100L))
        assertEquals("line 2500", many.textAt(5_000_500L))
        assertEquals("line 4999", many.textAt(9_999_000L))
        assertNull(many.textAt(1_800L))
    }
}
