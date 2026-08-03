package com.opplayer.app.ui.player

import com.opplayer.app.data.EpisodePattern
import com.opplayer.app.player.EpisodeResolutionResult
import com.opplayer.app.player.EpisodeResolver
import com.opplayer.app.player.EpisodeTarget
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.fakes.FakeEpisodeResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EpisodeControllerTest {

    private val pattern = EpisodePattern(
        prefix = "https://cdn.test/s01e",
        suffix = ".mp4",
        episode = 1,
        pad = 2
    )

    private val request = PlaybackRequest(
        key = "item-1",
        title = "Show",
        uri = "https://cdn.test/s01e01.mp4",
        source = PlaybackRequest.Source.LIBRARY,
        pattern = pattern
    )

    @Test
    fun `returns the resolved target`() = runTest {
        val target = EpisodeTarget("https://cdn.test/s01e02.mp4", "E02", pattern)
        val controller = EpisodeController(FakeEpisodeResolver(EpisodeResolutionResult.Found(target)))

        val result = controller.resolve(request, forward = true)

        assertEquals(EpisodeResolutionResult.Found(target), result)
    }

    @Test
    fun `reports a timeout instead of hanging on the network`() = runTest {
        val resolver = FakeEpisodeResolver(
            result = EpisodeResolutionResult.NotFound,
            beforeReturn = { delay(30_000L) }
        )
        val controller = EpisodeController(resolver, timeoutMs = 8_000L)

        val result = controller.resolve(request, forward = true)

        assertEquals(EpisodeResolutionResult.Timeout, result)
    }

    @Test
    fun `finishes just below the timeout`() = runTest {
        val target = EpisodeTarget("https://cdn.test/s01e02.mp4", "E02", pattern)
        val resolver = FakeEpisodeResolver(
            result = EpisodeResolutionResult.Found(target),
            beforeReturn = { delay(7_000L) }
        )
        val controller = EpisodeController(resolver, timeoutMs = 8_000L)

        val result = controller.resolve(request, forward = true)

        assertEquals(EpisodeResolutionResult.Found(target), result)
    }

    @Test
    fun `surfaces a network failure separately from not found`() = runTest {
        val resolver = object : EpisodeResolver {
            override suspend fun resolve(
                request: PlaybackRequest,
                forward: Boolean
            ): EpisodeResolutionResult = throw IOException("offline")
        }

        val result = EpisodeController(resolver).resolve(request, forward = true)

        assertEquals(EpisodeResolutionResult.NetworkUnavailable, result)
    }

    @Test
    fun `navigation is only supported when a pattern is present`() {
        val controller = EpisodeController(FakeEpisodeResolver())

        assertTrue(controller.supports(request))
        assertFalse(controller.supports(request.copy(pattern = null, uri = "https://cdn.test/movie.mp4")))
    }
}
