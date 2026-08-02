package com.opplayer.app.ui.player

import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.fakes.FakeProgressSaver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressManagerTest {

    private val request = PlaybackRequest(
        key = "a",
        title = "A",
        uri = "https://cdn.test/a.mp4",
        source = PlaybackRequest.Source.LIBRARY
    )

    @Test
    fun `no saver means no write and no crash`() {
        val manager = ProgressManager()

        assertFalse(manager.hasSaver)
        manager.save(request, 10_000L, 60_000L)
        manager.saveExact(request, 10_000L)
    }

    @Test
    fun `a position in the middle is stored as is`() {
        val saver = FakeProgressSaver()
        val manager = ProgressManager(saver)

        manager.save(request, 25_000L, 60_000L)

        assertTrue(manager.hasSaver)
        assertEquals(1, saver.saved.size)
        assertEquals(request.key, saver.saved.first().first.key)
        assertEquals(25_000L, saver.lastPosition)
    }

    @Test
    fun `a position in the last seconds restarts the video`() {
        val saver = FakeProgressSaver()
        val manager = ProgressManager(saver)

        manager.save(request, 58_500L, 60_000L)

        assertEquals(0L, saver.lastPosition)
    }

    @Test
    fun `an unknown duration keeps the reported position`() {
        val saver = FakeProgressSaver()
        val manager = ProgressManager(saver)

        manager.save(request, 58_500L, 0L)

        assertEquals(58_500L, saver.lastPosition)
    }

    @Test
    fun `saveExact bypasses the end of video rule`() {
        val saver = FakeProgressSaver()
        val manager = ProgressManager(saver)

        manager.saveExact(request, 59_900L)

        assertEquals(59_900L, saver.lastPosition)
    }

    @Test
    fun `a negative position is clamped to zero`() {
        val saver = FakeProgressSaver()
        val manager = ProgressManager(saver)

        manager.saveExact(request, -5_000L)
        manager.save(request, -5_000L, 60_000L)

        assertEquals(listOf(0L, 0L), saver.saved.map { it.second })
    }

    @Test
    fun `a cleared saver is never called again`() {
        val saver = FakeProgressSaver()
        val manager = ProgressManager(saver)

        manager.clearSaver()
        manager.save(request, 25_000L, 60_000L)
        manager.saveExact(request, 25_000L)

        assertFalse(manager.hasSaver)
        assertTrue(saver.saved.isEmpty())
    }

    @Test
    fun `a replaced saver receives the next write`() {
        val first = FakeProgressSaver()
        val second = FakeProgressSaver()
        val manager = ProgressManager(first)

        manager.setSaver(second)
        manager.save(request, 25_000L, 60_000L)

        assertTrue(first.saved.isEmpty())
        assertEquals(25_000L, second.lastPosition)
    }
}
