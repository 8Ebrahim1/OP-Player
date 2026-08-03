package com.opplayer.app.ui.player

import com.opplayer.app.R
import com.opplayer.app.data.EpisodePattern
import com.opplayer.app.player.EpisodeResolutionResult
import com.opplayer.app.player.EpisodeTarget
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlaybackStatus
import com.opplayer.app.player.PlayerMessage
import com.opplayer.app.player.fakes.FakeEpisodeResolver
import com.opplayer.app.player.fakes.FakePlayerEngine
import com.opplayer.app.player.fakes.FakeProgressSaver
import com.opplayer.app.player.fakes.FakeSubtitleSource
import com.opplayer.app.player.subtitle.EmbeddedTrackInfo
import com.opplayer.app.player.subtitle.LoadedSubtitle
import com.opplayer.app.player.subtitle.SubtitleChoice
import com.opplayer.app.player.subtitle.SubtitleCue
import com.opplayer.app.player.subtitle.SubtitleFileCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlayerViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val patternA = EpisodePattern(
        prefix = "https://cdn.test/s01e",
        suffix = ".mp4",
        episode = 1,
        pad = 2
    )

    private val requestA = PlaybackRequest(
        key = "item-1",
        title = "Show",
        uri = "https://cdn.test/s01e01.mp4",
        startPositionMs = 4_000L,
        source = PlaybackRequest.Source.LIBRARY,
        pattern = patternA,
        episodeLabel = "E01"
    )

    private val requestB = PlaybackRequest(
        key = "item-2",
        title = "Other",
        uri = "https://cdn.test/other.mp4",
        source = PlaybackRequest.Source.LIBRARY
    )

    private val engine = FakePlayerEngine()
    private val saver = FakeProgressSaver()
    private val resolver = FakeEpisodeResolver()
    private val subtitleSource = FakeSubtitleSource()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(
        request: PlaybackRequest = requestA,
        timeoutMs: Long = 8_000L
    ): PlayerViewModel = PlayerViewModel(
        initialRequest = request,
        engine = engine,
        subtitleSource = subtitleSource,
        episodeResolver = resolver,
        progressManager = ProgressManager(saver),
        episodeTimeoutMs = timeoutMs
    )

    private fun TestScope.collectMessages(viewModel: PlayerViewModel): List<PlayerMessage> {
        val messages = mutableListOf<PlayerMessage>()
        backgroundScope.launch { viewModel.messages.collect { messages += it } }
        return messages
    }

    @Test
    fun `opening a request prepares the engine at the resume position`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        assertEquals(1, engine.prepared.size)
        assertEquals(requestA, engine.prepared.first().request)
        assertEquals(4_000L, engine.prepared.first().startPositionMs)
        assertEquals(PlaybackStatus.Preparing, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.canNavigateEpisodes)

        viewModel.releaseResources()
    }

    @Test
    fun `switching requests saves the old position and prepares the new item`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            engine.currentPosition = 12_000L
            engine.duration = 120_000L

            viewModel.onRequest(requestB)

            assertEquals(requestA to 12_000L, saver.saved.first())
            assertEquals(requestB, engine.prepared.last().request)
            assertEquals(requestB, viewModel.uiState.value.request)
            assertFalse(viewModel.uiState.value.canNavigateEpisodes)

            viewModel.releaseResources()
        }

    @Test
    fun `re-delivering the same request does not restart playback`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onRequest(requestA.copy(startPositionMs = 999L))

        assertEquals(1, engine.prepared.size)

        viewModel.releaseResources()
    }

    @Test
    fun `retry asks the engine again and shows the spinner`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.emitStatus(PlaybackStatus.Error(R.string.error_network))
        assertEquals(R.string.error_network, viewModel.uiState.value.errorRes)

        viewModel.retry()

        assertEquals(1, engine.retryCount)
        assertEquals(PlaybackStatus.Preparing, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.errorRes)
        assertTrue(viewModel.uiState.value.isBuffering)

        viewModel.releaseResources()
    }

    @Test
    fun `seeking moves forward and backward by the seek step`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.duration = 120_000L
        engine.currentPosition = 30_000L

        viewModel.seekBy(forward = true)
        viewModel.seekBy(forward = false)

        assertEquals(listOf(45_000L, 30_000L), engine.seeks)

        viewModel.releaseResources()
    }

    @Test
    fun `seeking never runs past the end or before the start`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.duration = 20_000L
        engine.currentPosition = 19_000L
        viewModel.seekBy(forward = true)

        engine.currentPosition = 1_000L
        viewModel.seekBy(forward = false)

        assertEquals(listOf(20_000L, 0L), engine.seeks)

        viewModel.releaseResources()
    }

    @Test
    fun `playback ending starts the next episode automatically`() = runTest(dispatcher) {
        val target = EpisodeTarget("https://cdn.test/s01e02.mp4", "E02", patternA.next())
        resolver.result = EpisodeResolutionResult.Found(target)

        val viewModel = createViewModel()
        val messages = collectMessages(viewModel)

        engine.emitStatus(PlaybackStatus.Ended)
        advanceUntilIdle()

        assertEquals(target.url, viewModel.uiState.value.request.uri)
        assertEquals("E02", viewModel.uiState.value.request.episodeLabel)
        assertEquals(0L, engine.prepared.last().startPositionMs)
        assertTrue(messages.any { it.textRes == R.string.now_playing_episode })

        viewModel.releaseResources()
    }

    @Test
    fun `auto next stays off when the user disabled it`() = runTest(dispatcher) {
        resolver.result = EpisodeResolutionResult.Found(
            EpisodeTarget("https://cdn.test/s01e02.mp4", "E02")
        )

        val viewModel = createViewModel()
        viewModel.setAutoNextEnabled(false)

        engine.emitStatus(PlaybackStatus.Ended)
        advanceUntilIdle()

        assertTrue(resolver.calls.isEmpty())
        assertEquals(requestA.uri, viewModel.uiState.value.request.uri)

        viewModel.releaseResources()
    }

    @Test
    fun `manual navigation walks backwards too`() = runTest(dispatcher) {
        resolver.result = EpisodeResolutionResult.Found(
            EpisodeTarget("https://cdn.test/s01e00.mp4", "E00")
        )

        val viewModel = createViewModel()
        viewModel.navigateEpisode(forward = false)
        advanceUntilIdle()

        assertEquals(false, resolver.calls.single().second)
        assertEquals("https://cdn.test/s01e00.mp4", viewModel.uiState.value.request.uri)
        assertFalse(viewModel.uiState.value.isResolvingEpisode)

        viewModel.releaseResources()
    }

    @Test
    fun `a missing next episode keeps the current one and explains why`() = runTest(dispatcher) {
        resolver.result = EpisodeResolutionResult.NotFound

        val viewModel = createViewModel()
        val messages = collectMessages(viewModel)

        viewModel.navigateEpisode(forward = true)
        advanceUntilIdle()

        assertEquals(requestA.uri, viewModel.uiState.value.request.uri)
        assertFalse(viewModel.uiState.value.isResolvingEpisode)
        assertEquals(R.string.next_episode_not_found, messages.last().textRes)

        viewModel.releaseResources()
    }

    @Test
    fun `a stalled lookup reports a timeout instead of spinning forever`() = runTest(dispatcher) {
        val slowResolver = FakeEpisodeResolver(
            result = EpisodeResolutionResult.NotFound,
            beforeReturn = { delay(60_000L) }
        )

        val viewModel = PlayerViewModel(
            initialRequest = requestA,
            engine = engine,
            subtitleSource = subtitleSource,
            episodeResolver = slowResolver,
            progressManager = ProgressManager(saver),
            episodeTimeoutMs = 8_000L
        )
        val messages = collectMessages(viewModel)

        viewModel.navigateEpisode(forward = true)
        advanceUntilIdle()

        assertEquals(R.string.episode_resolve_timeout, messages.last().textRes)
        assertFalse(viewModel.uiState.value.isResolvingEpisode)

        viewModel.releaseResources()
    }

    @Test
    fun `an offline lookup is reported as a network problem`() = runTest(dispatcher) {
        resolver.result = EpisodeResolutionResult.NetworkUnavailable

        val viewModel = createViewModel()
        val messages = collectMessages(viewModel)

        viewModel.navigateEpisode(forward = true)
        advanceUntilIdle()

        assertEquals(R.string.episode_network_unavailable, messages.last().textRes)

        viewModel.releaseResources()
    }

    @Test
    fun `navigation is refused when the link carries no episode marker`() = runTest(dispatcher) {
        val viewModel = createViewModel(request = requestB)
        val messages = collectMessages(viewModel)

        viewModel.navigateEpisode(forward = true)
        advanceUntilIdle()

        assertTrue(resolver.calls.isEmpty())
        assertEquals(R.string.episode_pattern_missing, messages.last().textRes)

        viewModel.releaseResources()
    }

    @Test
    fun `a position inside the closing seconds resumes from the start`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.duration = 60_000L
        engine.currentPosition = 58_500L

        viewModel.saveProgress()

        assertEquals(0L, saver.lastPosition)

        viewModel.releaseResources()
    }

    @Test
    fun `an external subtitle found next to the video is loaded automatically`() =
        runTest(dispatcher) {
            subtitleSource.candidates = mapOf(
                requestA.uri to listOf(SubtitleFileCandidate("file://a.srt", "a.srt"))
            )
            subtitleSource.loaded = mapOf(
                "file://a.srt" to LoadedSubtitle(
                    uri = "file://a.srt",
                    name = "a.srt",
                    cues = listOf(SubtitleCue(0L, 5_000L, "hello"))
                )
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.subtitleState.value.usingExternal)
            assertEquals("a.srt", viewModel.subtitleState.value.externalName)
            assertEquals("hello", viewModel.subtitleText.value)

            viewModel.releaseResources()
        }

    @Test
    fun `choosing an embedded track drops the external file`() = runTest(dispatcher) {
        subtitleSource.candidates = mapOf(
            requestA.uri to listOf(SubtitleFileCandidate("file://a.srt", "a.srt"))
        )
        subtitleSource.loaded = mapOf(
            "file://a.srt" to LoadedSubtitle(
                uri = "file://a.srt",
                name = "a.srt",
                cues = listOf(SubtitleCue(0L, 5_000L, "hello"))
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        engine.emitTracks(
            listOf(
                EmbeddedTrackInfo(index = 0, language = "fa", selected = true),
                EmbeddedTrackInfo(index = 1, language = "en", selected = false)
            )
        )
        viewModel.onSubtitleChoice(SubtitleChoice.EmbeddedTrack(1))
        advanceUntilIdle()

        assertEquals(1, engine.selectedTextTrack)
        assertFalse(viewModel.subtitleState.value.usingExternal)
        assertNull(viewModel.subtitleState.value.externalName)

        viewModel.releaseResources()
    }

    @Test
    fun `subtitles can be turned off and on again`() = runTest(dispatcher) {
        subtitleSource.candidates = mapOf(
            requestA.uri to listOf(SubtitleFileCandidate("file://a.srt", "a.srt"))
        )
        subtitleSource.loaded = mapOf(
            "file://a.srt" to LoadedSubtitle(
                uri = "file://a.srt",
                name = "a.srt",
                cues = listOf(SubtitleCue(0L, 5_000L, "hello"))
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSubtitleChoice(SubtitleChoice.Off)
        assertTrue(viewModel.subtitleState.value.disabled)
        assertFalse(engine.textTracksEnabled)
        assertNull(viewModel.subtitleText.value)

        viewModel.onSubtitleChoice(SubtitleChoice.CurrentExternal)
        assertFalse(viewModel.subtitleState.value.disabled)
        assertTrue(engine.textTracksEnabled)
        assertEquals("hello", viewModel.subtitleText.value)

        viewModel.releaseResources()
    }

    @Test
    fun `the subtitle offset is clamped to one minute`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.setSubtitleOffset(5_000_000L)
        assertEquals(PlayerViewModel.SUBTITLE_OFFSET_LIMIT_MS, viewModel.subtitleState.value.offsetMs)

        viewModel.setSubtitleOffset(-5_000_000L)
        assertEquals(
            -PlayerViewModel.SUBTITLE_OFFSET_LIMIT_MS,
            viewModel.subtitleState.value.offsetMs
        )

        viewModel.releaseResources()
    }

    @Test
    fun `releasing saves the last position and frees the engine exactly once`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            engine.duration = 120_000L
            engine.currentPosition = 42_000L

            viewModel.releaseResources()
            viewModel.releaseResources()

            assertEquals(1, engine.releaseCount)
            assertEquals(42_000L, saver.lastPosition)
            assertNull(engine.listener)
        }

    @Test
    fun `a detached screen is never called back into`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.duration = 120_000L
        engine.currentPosition = 7_000L

        viewModel.clearProgressSaver()
        viewModel.saveProgress()

        assertTrue(saver.saved.isEmpty())

        viewModel.releaseResources()
    }

    @Test
    fun `going to the background pauses and stores the position`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.duration = 120_000L
        engine.currentPosition = 9_000L

        viewModel.pauseForBackground()

        assertEquals(1, engine.pauseCount)
        assertEquals(9_000L, saver.lastPosition)

        viewModel.releaseResources()
    }

    @Test
    fun `engine status is mirrored into the ui state`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        engine.emitStatus(PlaybackStatus.Buffering)
        assertTrue(viewModel.uiState.value.isBuffering)

        engine.emitStatus(PlaybackStatus.Ready)
        assertFalse(viewModel.uiState.value.isBuffering)
        assertNull(viewModel.uiState.value.errorRes)

        viewModel.releaseResources()
    }
}
