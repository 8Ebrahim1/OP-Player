package com.opplayer.app.player

import com.opplayer.app.data.EpisodePattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkPatternDetectorTest {

    private fun url(episode: String) =
        "http://example.com/dl/Black.Torch.S01E$episode.720p.mkv"

    private fun detected(first: String, second: String): EpisodePattern {
        val result = LinkPatternDetector.detect(first, second)
        assertTrue(
            "expected Detected but was $result",
            result is LinkPatternDetector.Result.Detected
        )
        return (result as LinkPatternDetector.Result.Detected).pattern
    }

    private fun rejected(first: String, second: String): LinkPatternDetector.Failure {
        val result = LinkPatternDetector.detect(first, second)
        assertTrue(
            "expected Rejected but was $result",
            result is LinkPatternDetector.Result.Rejected
        )
        return (result as LinkPatternDetector.Result.Rejected).failure
    }

    @Test
    fun detectsSingleStepBetweenConsecutiveEpisodes() {
        val pattern = detected(url("06"), url("07"))
        assertEquals(6, pattern.episode)
        assertEquals(1, pattern.step)
        assertEquals(2, pattern.pad)
    }

    @Test
    fun rebuiltUrlMatchesBothLinks() {
        val pattern = detected(url("06"), url("07"))
        assertEquals(url("06"), pattern.url)
        assertEquals(url("07"), pattern.next()!!.url)
    }

    @Test
    fun detectsStepOfTwo() {
        val pattern = detected(url("04"), url("06"))
        assertEquals(2, pattern.step)
        assertEquals(url("06"), pattern.next()!!.url)
    }

    @Test
    fun rejectsEmptyInput() {
        assertEquals(LinkPatternDetector.Failure.EMPTY, rejected("", url("07")))
        assertEquals(LinkPatternDetector.Failure.EMPTY, rejected(url("07"), "   "))
    }

    @Test
    fun rejectsIdenticalLinks() {
        assertEquals(LinkPatternDetector.Failure.IDENTICAL, rejected(url("06"), url("06")))
    }

    @Test
    fun rejectsDecreasingEpisodeNumbers() {
        assertEquals(LinkPatternDetector.Failure.NOT_INCREASING, rejected(url("07"), url("06")))
    }

    @Test
    fun rejectsTooLargeStep() {
        assertEquals(LinkPatternDetector.Failure.NOT_INCREASING, rejected(url("01"), url("09")))
    }

    @Test
    fun rejectsNonNumericDifference() {
        assertEquals(
            LinkPatternDetector.Failure.NOT_NUMERIC,
            rejected(
                "http://example.com/dl/first-part.mkv",
                "http://example.com/dl/second-part.mkv"
            )
        )
    }

    @Test
    fun rejectsVeryLongNumericDifference() {
        assertEquals(
            LinkPatternDetector.Failure.TOO_LONG,
            rejected(
                "http://example.com/dl/ep000001.mkv",
                "http://example.com/dl/ep000002.mkv"
            )
        )
    }

    @Test
    fun detectsChangeInsideQueryString() {
        val pattern = detected(
            "http://example.com/watch?ep=6&token=abc",
            "http://example.com/watch?ep=7&token=abc"
        )
        assertEquals(6, pattern.episode)
        assertEquals(1, pattern.pad)
    }
}