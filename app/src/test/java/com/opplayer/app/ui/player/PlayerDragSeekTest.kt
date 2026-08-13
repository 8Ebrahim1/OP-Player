package com.opplayer.app.ui.player

import com.opplayer.app.data.PlayerPreferences
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.VideoScaleMode
import com.opplayer.app.player.fakes.FakeEpisodeResolver
import com.opplayer.app.player.fakes.FakePlayerEngine
import com.opplayer.app.player.fakes.FakePlayerPreferencesStore
import com.opplayer.app.player.fakes.FakeProgressSaver
import com.opplayer.app.player.fakes.FakeSubtitleSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

class PlayerDragSeekTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val request = PlaybackRequest(
        key = "content://media/external/video/media/1",
        title = "clip.mp4",
        uri = "content://media/external/video/media/1",
        source = PlaybackRequest.Source.DEVICE,
        folderId = 7L
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

    private fun createViewModel(
        preferencesStore: FakePlayerPreferencesStore? = null
    ): PlayerViewModel = PlayerViewModel(
        initialRequest = request,
        engine = engine,
        subtitleSource = subtitleSource,
        episodeResolver = resolver,
        progressManager = ProgressManager(saver),
        preferencesStore = preferencesStore
    )

    @Test
    fun `the default scale mode is fit`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        assertEquals(VideoScaleMode.FIT, viewModel.uiState.value.scaleMode)

        viewModel.releaseResources()
    }

    @Test
    fun `stored player settings are restored`() = runTest(dispatcher) {
        val store = FakePlayerPreferencesStore(
            PlayerPreferences(
                scaleMode = VideoScaleMode.FILL,
                speed = 1.5f,
                autoNextEnabled = false,
                gesturesEnabled = false
            )
        )

        val viewModel = createViewModel(store)
        val state = viewModel.uiState.value

        assertEquals(VideoScaleMode.FILL, state.scaleMode)
        assertEquals(1.5f, state.speed, 0.001f)
        assertFalse(state.autoNextEnabled)
        assertFalse(state.gesturesEnabled)
        assertTrue(engine.speeds.contains(1.5f))

        viewModel.releaseResources()
    }

    @Test
    fun `changing a setting is written to the store`() = runTest(dispatcher) {
        val store = FakePlayerPreferencesStore()
        val viewModel = createViewModel(store)

        viewModel.setScaleMode(VideoScaleMode.ZOOM)
        viewModel.setSpeed(2f)

        assertEquals(VideoScaleMode.ZOOM, store.saved.last().scaleMode)
        assertEquals(2f, store.saved.last().speed, 0.001f)

        viewModel.releaseResources()
    }

    @Test
    fun `dragging right previews a forward jump and seeks on release`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.duration = 600_000L
        engine.currentPosition = 100_000L

        viewModel.startSeekDrag()
        viewModel.updateSeekDrag(0.25f)

        val preview = requireNotNull(viewModel.uiState.value.seekPreview)
        assertEquals(130_000L, preview.positionMs)
        assertEquals(30_000L, preview.deltaMs)

        viewModel.commitSeekDrag()

        assertEquals(130_000L, engine.seeks.last())
        assertNull(viewModel.uiState.value.seekPreview)

        viewModel.releaseResources()
    }

    @Test
    fun `dragging left rewinds and stops at the beginning`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.duration = 600_000L
        engine.currentPosition = 5_000L

        viewModel.startSeekDrag()
        viewModel.updateSeekDrag(-0.5f)

        val preview = requireNotNull(viewModel.uiState.value.seekPreview)
        assertEquals(0L, preview.positionMs)
        assertEquals(-5_000L, preview.deltaMs)

        viewModel.releaseResources()
    }

    @Test
    fun `cancelling a drag leaves the position untouched`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        engine.duration = 600_000L
        engine.currentPosition = 20_000L

        viewModel.startSeekDrag()
        viewModel.updateSeekDrag(0.4f)
        viewModel.cancelSeekDrag()

        assertNull(viewModel.uiState.value.seekPreview)
        assertTrue(engine.seeks.isEmpty())

        viewModel.releaseResources()
    }

    @Test
    fun `the drag span is two minutes and shrinks for short clips`() {
        assertEquals(120_000L, PlayerViewModel.dragSeekSpanMs(0L))
        assertEquals(120_000L, PlayerViewModel.dragSeekSpanMs(600_000L))
        assertEquals(90_000L, PlayerViewModel.dragSeekSpanMs(180_000L))
        assertEquals(30_000L, PlayerViewModel.dragSeekSpanMs(40_000L))
    }
}
