package com.opplayer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlinx.serialization.json.Json

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

class EpisodePatternValidationTest {

    @Test(expected = IllegalArgumentException::class)
    fun `a zero step is rejected`() {
        EpisodePattern(prefix = "a", suffix = "b", episode = 1, pad = 2, step = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a negative episode is rejected`() {
        EpisodePattern(prefix = "a", suffix = "b", episode = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a zero padding is rejected`() {
        EpisodePattern(prefix = "a", suffix = "b", episode = 1, pad = 0)
    }

    @Test
    fun `normalized repairs invalid legacy values`() {
        val pattern = EpisodePattern.normalized(
            prefix = "a",
            suffix = "b",
            episode = -5,
            pad = 0,
            step = 0
        )

        assertEquals(0, pattern.episode)
        assertEquals(1, pattern.pad)
        assertEquals(1, pattern.step)
    }

    @Test
    fun `a valid pattern always moves forward`() {
        val pattern = EpisodePattern(prefix = "a", suffix = "b", episode = 3, step = 2)

        assertEquals(5, pattern.next()?.episode)
        assertEquals(1, pattern.previous()?.episode)
    }

    @Test
    fun `a serialized round trip keeps the values`() {
        val pattern = EpisodePattern(prefix = "a", suffix = "b", episode = 4, pad = 3, step = 2)
        val json = Json.encodeToString(EpisodePattern.serializer(), pattern)

        assertEquals(pattern, Json.decodeFromString(EpisodePattern.serializer(), json))
    }

    @Test
    fun `decoding a legacy zero step does not throw`() {
        val json = """{"prefix":"a","suffix":"b","episode":1,"pad":0,"step":0}"""

        val pattern = Json.decodeFromString(EpisodePattern.serializer(), json)

        assertEquals(1, pattern.step)
        assertEquals(2, pattern.next()?.episode)
    }
}
