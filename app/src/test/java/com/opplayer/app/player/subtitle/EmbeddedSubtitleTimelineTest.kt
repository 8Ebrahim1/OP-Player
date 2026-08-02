package com.opplayer.app.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedSubtitleTimelineTest {

    @Test
    fun `an empty timeline shows nothing`() {
        val timeline = EmbeddedSubtitleTimeline()

        assertTrue(timeline.isEmpty())
        assertNull(timeline.textAt(1_000L))
    }

    @Test
    fun `a cue is shown from the moment it arrives`() {
        val timeline = EmbeddedSubtitleTimeline().withCueGroup(1_000L, "hello")

        assertNull(timeline.textAt(500L))
        assertEquals("hello", timeline.textAt(1_500L))
    }

    @Test
    fun `the next cue closes the previous one`() {
        val timeline = EmbeddedSubtitleTimeline()
            .withCueGroup(1_000L, "first")
            .withCueGroup(2_000L, "second")

        assertEquals("first", timeline.textAt(1_500L))
        assertEquals("second", timeline.textAt(2_500L))
    }

    @Test
    fun `an empty cue group clears the subtitle`() {
        val timeline = EmbeddedSubtitleTimeline()
            .withCueGroup(1_000L, "first")
            .withCueGroup(2_000L, null)

        assertEquals("first", timeline.textAt(1_500L))
        assertNull(timeline.textAt(2_500L))
    }

    @Test
    fun `a blank cue group clears the subtitle`() {
        val timeline = EmbeddedSubtitleTimeline()
            .withCueGroup(1_000L, "first")
            .withCueGroup(2_000L, "  ")

        assertNull(timeline.textAt(2_500L))
    }

    @Test
    fun `an open cue expires instead of staying on screen forever`() {
        val timeline = EmbeddedSubtitleTimeline().withCueGroup(1_000L, "only line")
        val end = 1_000L + EmbeddedSubtitleTimeline.MAX_CUE_DURATION_MS

        assertEquals("only line", timeline.textAt(end))
        assertNull(timeline.textAt(end + 1L))
    }

    @Test
    fun `the delay shifts embedded cues later`() {
        val timeline = EmbeddedSubtitleTimeline().withCueGroup(1_000L, "hello")

        assertEquals("hello", timeline.textAt(2_500L, delayMs = 1_000L))
    }

    @Test
    fun `a negative delay is ignored`() {
        val timeline = EmbeddedSubtitleTimeline().withCueGroup(1_000L, "hello")

        assertEquals("hello", timeline.textAt(1_500L, delayMs = -5_000L))
    }

    @Test
    fun `seeking backwards drops cues from the future`() {
        val timeline = EmbeddedSubtitleTimeline()
            .withCueGroup(1_000L, "first")
            .withCueGroup(5_000L, "later")
            .withCueGroup(2_000L, "after seek")

        assertEquals(2, timeline.cues.size)
        assertEquals("after seek", timeline.textAt(2_500L))
    }

    @Test
    fun `a negative timestamp is clamped to zero`() {
        val timeline = EmbeddedSubtitleTimeline().withCueGroup(-500L, "hello")

        assertEquals(0L, timeline.cues.first().startMs)
    }

    @Test
    fun `history is capped`() {
        var timeline = EmbeddedSubtitleTimeline()
        repeat(EmbeddedSubtitleTimeline.MAX_CUES * 2) { index ->
            timeline = timeline.withCueGroup(index * 1_000L, "line $index")
        }

        assertEquals(EmbeddedSubtitleTimeline.MAX_CUES, timeline.cues.size)
    }
}
