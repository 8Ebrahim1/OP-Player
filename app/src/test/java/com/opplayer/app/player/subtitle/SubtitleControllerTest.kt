package com.opplayer.app.player.subtitle

import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlayerMessage
import com.opplayer.app.player.fakes.FakePlayerEngine
import com.opplayer.app.player.fakes.FakeSubtitleSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleControllerTest {

    private val engine = FakePlayerEngine()
    private val positionMs = MutableStateFlow(0L)
    private val messages = mutableListOf<PlayerMessage>()

    private val videoA = PlaybackRequest(
        key = "a",
        title = "A",
        uri = "https://cdn.test/a.mp4",
        source = PlaybackRequest.Source.LIBRARY
    )

    private val videoB = PlaybackRequest(
        key = "b",
        title = "B",
        uri = "https://cdn.test/b.mp4",
        source = PlaybackRequest.Source.LIBRARY
    )

    private val subtitleA = LoadedSubtitle(
        uri = "file://a.srt",
        name = "a.srt",
        cues = listOf(SubtitleCue(0L, 10_000L, "line A"))
    )

    private val subtitleB = LoadedSubtitle(
        uri = "file://b.srt",
        name = "b.srt",
        cues = listOf(SubtitleCue(0L, 10_000L, "line B"))
    )

    private fun source() = FakeSubtitleSource(
        candidates = mapOf(
            videoA.uri to listOf(SubtitleFileCandidate(subtitleA.uri, subtitleA.name)),
            videoB.uri to listOf(SubtitleFileCandidate(subtitleB.uri, subtitleB.name))
        ),
        loaded = mapOf(subtitleA.uri to subtitleA, subtitleB.uri to subtitleB)
    )

    private fun controller(scope: CoroutineScope, source: SubtitleSource) = SubtitleController(
        scope = scope,
        engine = engine,
        source = source,
        positionMs = positionMs,
        onMessage = { messages += it }
    )

    @Test
    fun `loads the subtitle that sits next to the video`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val controller = controller(scope, source())

        controller.onOpen(videoA)
        advanceUntilIdle()

        assertTrue(controller.state.value.usingExternal)
        assertEquals("a.srt", controller.state.value.externalName)
        assertEquals("line A", controller.text.value)

        scope.cancel()
    }

    /**
     * The scenario that motivated the request identity guard: a lookup for video
     * A that ignores cancellation must never overwrite video B's subtitles.
     */
    @Test
    fun `a slow lookup for the previous video is discarded`() = runTest {
        val gate = CompletableDeferred<Unit>()

        val slowSource = object : FakeSubtitleSource(
            candidates = source().candidates,
            loaded = source().loaded
        ) {
            override suspend fun findCandidates(videoUri: String): List<SubtitleFileCandidate> {
                // NonCancellable models an I/O call that does not observe cancellation.
                if (videoUri == videoA.uri) withContext(NonCancellable) { gate.await() }
                return super.findCandidates(videoUri)
            }
        }

        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val controller = controller(scope, slowSource)

        controller.onOpen(videoA)
        advanceUntilIdle()

        // The user gets bored and opens another video while A is still searching.
        controller.onOpen(videoB)
        advanceUntilIdle()

        // Only now does A's lookup come back.
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("b.srt", controller.state.value.externalName)
        assertEquals(listOf(SubtitleFileCandidate(subtitleB.uri, subtitleB.name)), controller.state.value.candidates)
        assertEquals("line B", controller.text.value)
        assertTrue(messages.none { it.argument == "a.srt" })

        scope.cancel()
    }

    @Test
    fun `opening a new video clears the previous subtitle state`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val source = FakeSubtitleSource()
        val controller = controller(scope, source)

        controller.onOpen(videoA)
        advanceUntilIdle()
        controller.setOffset(2_000L)
        controller.onCue(0L, "embedded")

        controller.onOpen(videoB)
        advanceUntilIdle()

        assertEquals(0L, controller.state.value.offsetMs)
        assertFalse(controller.state.value.usingExternal)
        assertNull(controller.text.value)

        scope.cancel()
    }

    @Test
    fun `embedded cues are shown when no external file exists`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val controller = controller(scope, FakeSubtitleSource())

        controller.onOpen(videoA)
        advanceUntilIdle()

        controller.onCue(1_000L, "embedded line")
        positionMs.value = 1_500L
        advanceUntilIdle()

        assertEquals("embedded line", controller.text.value)

        // Cues are not re-delivered after a seek, so the timeline is dropped.
        controller.onSeek()
        advanceUntilIdle()
        assertNull(controller.text.value)

        scope.cancel()
    }

    @Test
    fun `the offset shifts external cues`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val controller = controller(scope, source())

        controller.onOpen(videoA)
        advanceUntilIdle()

        positionMs.value = 12_000L
        advanceUntilIdle()
        assertNull(controller.text.value)

        controller.setOffset(-5_000L)
        advanceUntilIdle()
        assertEquals("line A", controller.text.value)

        scope.cancel()
    }

    @Test
    fun `the offset is clamped to one minute in both directions`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val controller = controller(scope, FakeSubtitleSource())

        controller.setOffset(Long.MAX_VALUE)
        assertEquals(SubtitleController.OFFSET_LIMIT_MS, controller.state.value.offsetMs)

        controller.setOffset(Long.MIN_VALUE)
        assertEquals(-SubtitleController.OFFSET_LIMIT_MS, controller.state.value.offsetMs)

        scope.cancel()
    }

    @Test
    fun `a failed load reports the failure only when the user asked for the file`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val controller = controller(
            scope,
            FakeSubtitleSource(
                candidates = mapOf(videoA.uri to listOf(SubtitleFileCandidate("file://x.srt", "x.srt"))),
                loaded = emptyMap()
            )
        )

        controller.onOpen(videoA)
        advanceUntilIdle()
        assertTrue(messages.isEmpty())

        controller.onChoice(SubtitleChoice.ExternalFile(0))
        advanceUntilIdle()
        assertEquals(1, messages.size)

        scope.cancel()
    }
}
