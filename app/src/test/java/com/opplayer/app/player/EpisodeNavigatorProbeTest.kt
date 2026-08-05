package com.opplayer.app.player

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Candidates are probed in concurrent batches, so these tests cover the parts that concurrency
 * could easily break: candidate order must still win over answer order, a batch must not stop
 * the search early, and a temporary network failure must never look like the end of a series.
 */
class EpisodeNavigatorProbeTest {

    private val seasonUrl = "http://example.com/dl/Show.S02E01.mkv"

    private class RecordingProbe(
        private val answer: (String) -> AvailabilityResult
    ) : AvailabilityProbe {

        val probed = CopyOnWriteArrayList<String>()

        override suspend fun probe(url: String): AvailabilityResult {
            probed += url
            return answer(url)
        }
    }

    @Test
    fun `the first candidate wins even when every probe answers at once`() = runTest {
        val probe = RecordingProbe { AvailabilityResult.Available }

        val found = EpisodeNavigator.resolveNext(seasonUrl, probe)
            as EpisodeNavigator.Resolution.Found

        assertEquals(2, found.candidate.season)
        assertEquals(2, found.candidate.episode)
    }

    @Test
    fun `the next season is used when the next episode is missing`() = runTest {
        val probe = RecordingProbe { url ->
            if (url.contains("S03E01")) AvailabilityResult.Available
            else AvailabilityResult.NotAvailable
        }

        val found = EpisodeNavigator.resolveNext(seasonUrl, probe)
            as EpisodeNavigator.Resolution.Found

        assertEquals(3, found.candidate.season)
        assertEquals(1, found.candidate.episode)
    }

    @Test
    fun `a previous season lookup starts at the last episode and stops when it hits one`() =
        runTest {
            val probe = RecordingProbe { url ->
                if (url.contains("S01E12")) AvailabilityResult.Available
                else AvailabilityResult.NotAvailable
            }

            val found = EpisodeNavigator.resolvePrevious(seasonUrl, probe)
                as EpisodeNavigator.Resolution.Found

            assertEquals(1, found.candidate.season)
            assertEquals(12, found.candidate.episode)
            assertTrue(probe.probed.first().contains("S01E24"))
            assertTrue(
                "probed ${probe.probed.size} urls",
                probe.probed.size < EpisodeNavigator.MAX_PREVIOUS_SEASON_EPISODE
            )
        }

    @Test
    fun `an unreachable candidate does not hide an available one in a later batch`() = runTest {
        val probe = RecordingProbe { url ->
            when {
                url.contains("S01E20") -> AvailabilityResult.Available
                url.contains("S01E24") -> AvailabilityResult.NetworkUnavailable
                else -> AvailabilityResult.NotAvailable
            }
        }

        val found = EpisodeNavigator.resolvePrevious(seasonUrl, probe)
            as EpisodeNavigator.Resolution.Found

        assertEquals(20, found.candidate.episode)
    }

    @Test
    fun `an offline lookup is reported as a network problem`() = runTest {
        val probe = RecordingProbe { AvailabilityResult.NetworkUnavailable }

        assertEquals(
            EpisodeNavigator.Resolution.NetworkUnavailable,
            EpisodeNavigator.resolvePrevious(seasonUrl, probe)
        )
    }

    @Test
    fun `nothing available anywhere is a plain not found`() = runTest {
        val probe = RecordingProbe { AvailabilityResult.NotAvailable }

        assertEquals(
            EpisodeNavigator.Resolution.NotFound,
            EpisodeNavigator.resolveNext(seasonUrl, probe)
        )
        assertEquals(2, probe.probed.size)
    }
}
