package com.opplayer.app.player

import android.content.Context
import androidx.media3.common.MimeTypes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * On-device checks for the pieces that only behave realistically on Android:
 * engine construction, media item resolution and teardown.
 *
 * Playback of real streams is covered by the manual matrix in
 * `docs/DEVICE_TEST_PLAN.md`, because it depends on the network.
 */
@RunWith(AndroidJUnit4::class)
class PlayerEngineInstrumentationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    private fun <T> onMainThread(block: () -> T): T {
        var result: T? = null
        var failure: Throwable? = null
        val latch = CountDownLatch(1)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                result = block()
            } catch (error: Throwable) {
                failure = error
            } finally {
                latch.countDown()
            }
        }

        assertTrue("Main thread work timed out", latch.await(30, TimeUnit.SECONDS))
        failure?.let { throw it }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun request(uri: String) = PlaybackRequest(
        key = "instrumentation",
        title = "Instrumentation",
        uri = uri,
        source = PlaybackRequest.Source.LIBRARY
    )

    @Test
    fun openingAndClosingThePlayerRepeatedlyReleasesIt() {
        repeat(5) {
            onMainThread {
                val engine = DefaultPlayerFactory.create(context)
                engine.prepare(request("file:///android_asset/missing.mp4"))
                engine.pause()
                engine.release()
                // Releasing twice must stay a no-op.
                engine.release()
            }
        }
    }

    @Test
    fun releasedEngineStopsReportingProgress() {
        onMainThread {
            val engine = DefaultPlayerFactory.create(context)
            engine.release()

            assertEquals(0L, engine.currentPosition)
            assertEquals(0L, engine.duration)
            assertFalse(engine.isPlaying)
        }
    }

    @Test
    fun listenerIsDetachedOnRelease() {
        onMainThread {
            val engine = DefaultPlayerFactory.create(context)
            engine.setListener(object : PlayerEngineListener {})
            engine.release()
            engine.setListener(null)
        }
    }

    @Test
    fun adaptiveStreamsGetTheRightMimeType() {
        val hls = MediaFactory.buildMediaItem(request("https://cdn.test/stream.m3u8"))
        val dash = MediaFactory.buildMediaItem(request("https://cdn.test/stream.mpd"))
        val rtsp = MediaFactory.buildMediaItem(request("rtsp://cdn.test/live"))
        val local = MediaFactory.buildMediaItem(request("file:///storage/emulated/0/a.mp4"))

        assertEquals(MimeTypes.APPLICATION_M3U8, hls.localConfiguration?.mimeType)
        assertEquals(MimeTypes.APPLICATION_MPD, dash.localConfiguration?.mimeType)
        assertNotNull(rtsp.localConfiguration)
        assertNull(local.localConfiguration?.mimeType)
    }

    @Test
    fun externalSubtitleIsAttachedToTheMediaItem() {
        val item = MediaFactory.buildMediaItem(
            request("https://cdn.test/video.mp4").copy(subtitleUrl = "https://cdn.test/video.srt")
        )

        assertEquals(1, item.localConfiguration?.subtitleConfigurations?.size)
    }

    @Test
    fun pictureInPictureSupportIsQueryable() {
        // Must not throw on devices without the PiP feature.
        com.opplayer.app.ui.player.supportsPip(context.packageManager)
    }
}
