package com.opplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {

    @Test
    fun `a fresh video resumes from the start`() {
        assertEquals(0L, PlaybackProgress.resumePosition(positionMs = 0L, durationMs = 100_000L))
    }

    @Test
    fun `a negative position is clamped to the start`() {
        assertEquals(0L, PlaybackProgress.resumePosition(positionMs = -1L, durationMs = 100_000L))
    }

    @Test
    fun `a position in the middle is kept`() {
        assertEquals(
            45_000L,
            PlaybackProgress.resumePosition(positionMs = 45_000L, durationMs = 100_000L)
        )
    }

    @Test
    fun `a position inside the end threshold restarts the video`() {
        assertEquals(
            0L,
            PlaybackProgress.resumePosition(positionMs = 98_000L, durationMs = 100_000L)
        )
    }

    @Test
    fun `the end threshold itself restarts the video`() {
        val duration = 100_000L
        assertEquals(
            0L,
            PlaybackProgress.resumePosition(
                positionMs = duration - PlaybackProgress.END_THRESHOLD_MS,
                durationMs = duration
            )
        )
    }

    @Test
    fun `an unknown duration keeps the stored position`() {
        assertEquals(30_000L, PlaybackProgress.resumePosition(positionMs = 30_000L, durationMs = 0L))
        assertEquals(30_000L, PlaybackProgress.resumePosition(positionMs = 30_000L, durationMs = -1L))
    }
}
