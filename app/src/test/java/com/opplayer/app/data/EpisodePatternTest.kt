package com.opplayer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class EpisodePatternTest {

    private val pattern = EpisodePattern(
        prefix = "http://example.com/dl/Series.S01E",
        suffix = ".720p.mkv",
        episode = 6,
        pad = 2,
        step = 1
    )

    @Test
    fun urlUsesPadding() {
        assertEquals("http://example.com/dl/Series.S01E06.720p.mkv", pattern.url)
    }

    @Test
    fun urlForKeepsPaddingForLargerNumbers() {
        assertEquals("http://example.com/dl/Series.S01E124.720p.mkv", pattern.urlFor(124))
    }

    @Test
    fun nextAdvancesByStep() {
        val next = pattern.next()
        assertNotNull(next)
        assertEquals(7, next!!.episode)
    }

    @Test
    fun nextRespectsCustomStep() {
        val next = pattern.copy(step = 3).next()
        assertEquals(9, next!!.episode)
    }

    @Test
    fun previousGoesBackByStep() {
        val previous = pattern.previous()
        assertNotNull(previous)
        assertEquals(5, previous!!.episode)
    }

    @Test
    fun previousIsNullBelowZero() {
        assertNull(pattern.copy(episode = 0, step = 2).previous())
    }

    @Test
    fun nextIsNullBeyondMaxEpisode() {
        assertNull(pattern.copy(episode = 9999).next())
    }

    @Test
    fun labelIsTwoDigits() {
        assertEquals("E06", pattern.label())
        assertEquals("E124", pattern.copy(episode = 124).label())
    }
}
