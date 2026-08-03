package com.opplayer.app.player

import com.opplayer.app.data.EpisodePattern
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvailabilityProbeTest {

    private val request = PlaybackRequest(
        key = "item-1",
        title = "Show",
        uri = "https://cdn.test/show.S01E01.mkv",
        source = PlaybackRequest.Source.LIBRARY
    )

    private fun probeOf(vararg answers: Pair<String, AvailabilityResult>): AvailabilityProbe {
        val map = answers.toMap()
        return AvailabilityProbe { url -> map[url] ?: AvailabilityResult.NotAvailable }
    }

    @Test
    fun `an offline probe is reported as a network problem`() = runTest {
        val resolver = NetworkEpisodeResolver(
            AvailabilityProbe { AvailabilityResult.NetworkUnavailable }
        )

        assertEquals(
            EpisodeResolutionResult.NetworkUnavailable,
            resolver.resolve(request, forward = true)
        )
    }

    @Test
    fun `a missing episode stays a missing episode`() = runTest {
        val resolver = NetworkEpisodeResolver(AvailabilityProbe { AvailabilityResult.NotAvailable })

        assertEquals(EpisodeResolutionResult.NotFound, resolver.resolve(request, forward = true))
    }

    @Test
    fun `the first available candidate wins`() = runTest {
        val resolver = NetworkEpisodeResolver(
            probeOf("https://cdn.test/show.S01E02.mkv" to AvailabilityResult.Available)
        )

        val result = resolver.resolve(request, forward = true)

        assertTrue(result is EpisodeResolutionResult.Found)
        assertEquals(
            "https://cdn.test/show.S01E02.mkv",
            (result as EpisodeResolutionResult.Found).target.url
        )
    }

    @Test
    fun `one unreachable candidate does not hide a working one`() = runTest {
        val resolver = NetworkEpisodeResolver(
            AvailabilityProbe { url ->
                if (url.contains("S02E01")) {
                    AvailabilityResult.Available
                } else {
                    AvailabilityResult.NetworkUnavailable
                }
            }
        )

        val result = resolver.resolve(request, forward = true)

        assertTrue(result is EpisodeResolutionResult.Found)
    }

    @Test
    fun `the first episode of a season walks back into the previous one`() = runTest {
        val secondSeason = request.copy(uri = "https://cdn.test/show.S02E01.mkv")
        val resolver = NetworkEpisodeResolver(
            probeOf("https://cdn.test/show.S01E12.mkv" to AvailabilityResult.Available)
        )

        val result = resolver.resolve(secondSeason, forward = false)

        assertTrue(result is EpisodeResolutionResult.Found)
        assertEquals(
            "https://cdn.test/show.S01E12.mkv",
            (result as EpisodeResolutionResult.Found).target.url
        )
    }

    @Test
    fun `a pattern based lookup keeps the network failure`() = runTest {
        val patternRequest = request.copy(
            pattern = EpisodePattern(
                prefix = "https://cdn.test/show.S01E",
                suffix = ".mkv",
                episode = 1,
                pad = 2
            )
        )
        val resolver = NetworkEpisodeResolver(
            AvailabilityProbe { AvailabilityResult.NetworkUnavailable }
        )

        assertEquals(
            EpisodeResolutionResult.NetworkUnavailable,
            resolver.resolve(patternRequest, forward = true)
        )
    }

    @Test
    fun `a non http url is never probed`() = runTest {
        val probe = HttpAvailabilityProbe()

        assertEquals(
            AvailabilityResult.NotAvailable,
            probe.probe("content://media/external/video/media/1")
        )
    }
}
