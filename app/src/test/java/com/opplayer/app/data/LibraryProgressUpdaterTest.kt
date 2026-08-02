package com.opplayer.app.data

import com.opplayer.app.player.PlaybackRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryProgressUpdaterTest {

    private val pattern = EpisodePattern(
        prefix = "https://cdn.test/s01e",
        suffix = ".mp4",
        episode = 2,
        pad = 2
    )

    private val item = VideoItem(
        id = "item-1",
        title = "Show",
        url = "https://cdn.test/s01e01.mp4",
        positionMs = 1_000L,
        lastPlayedAt = 10L,
        currentLabel = "E01"
    )

    private fun request(
        uri: String,
        pattern: EpisodePattern? = null,
        label: String? = null
    ) = PlaybackRequest(
        key = "item-1",
        title = "Show",
        uri = uri,
        source = PlaybackRequest.Source.LIBRARY,
        pattern = pattern,
        episodeLabel = label
    )

    @Test
    fun `keeps episode metadata when the same episode is still playing`() {
        val updated = LibraryProgressUpdater.apply(
            item = item,
            request = request(item.url),
            positionMs = 5_000L,
            nowMs = 99L
        )

        assertEquals(5_000L, updated.positionMs)
        assertEquals(99L, updated.lastPlayedAt)
        assertEquals(item.url, updated.currentUrl)
        assertEquals("E01", updated.currentLabel)
    }

    @Test
    fun `drops stale metadata when the episode changed`() {
        val updated = LibraryProgressUpdater.apply(
            item = item,
            request = request("https://cdn.test/s01e02.mp4"),
            positionMs = 0L,
            nowMs = 120L
        )

        assertEquals("https://cdn.test/s01e02.mp4", updated.currentUrl)
        assertNull(updated.currentLabel)
        assertNull(updated.currentPattern)
    }

    @Test
    fun `takes the pattern and label from the request when present`() {
        val updated = LibraryProgressUpdater.apply(
            item = item,
            request = request("https://cdn.test/s01e02.mp4", pattern, "E02"),
            positionMs = 250L,
            nowMs = 130L
        )

        assertEquals(pattern, updated.currentPattern)
        assertEquals("E02", updated.currentLabel)
    }

    @Test
    fun `never stores a negative position`() {
        val updated = LibraryProgressUpdater.apply(item, request(item.url), -42L, 1L)

        assertEquals(0L, updated.positionMs)
    }

    @Test
    fun `updates only the matching item in the list`() {
        val other = item.copy(id = "item-2", title = "Other")

        val updated = LibraryProgressUpdater.applyTo(
            items = listOf(item, other),
            request = request(item.url),
            positionMs = 7_000L,
            nowMs = 55L
        )

        assertEquals(7_000L, updated.first().positionMs)
        assertEquals(other, updated.last())
    }

    @Test
    fun `reset clears progress and episode metadata`() {
        val played = item.copy(
            positionMs = 9_000L,
            currentUrl = "https://cdn.test/s01e03.mp4",
            currentPattern = pattern,
            currentLabel = "E03"
        )

        val reset = LibraryProgressUpdater.reset(listOf(played), played.id).first()

        assertEquals(0L, reset.positionMs)
        assertNull(reset.currentUrl)
        assertNull(reset.currentPattern)
        assertNull(reset.currentLabel)
    }
}
