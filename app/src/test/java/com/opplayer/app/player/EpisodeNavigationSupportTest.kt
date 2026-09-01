package com.opplayer.app.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeNavigationSupportTest {

    private fun request(uri: String) = PlaybackRequest(
        key = uri,
        title = "clip",
        uri = uri,
        source = PlaybackRequest.Source.LIBRARY
    )

    @Test
    fun `an http link with a season and episode marker supports navigation`() {
        assertTrue(request("https://cdn.example.com/show/S01E02.mkv").supportsEpisodeNavigation())
    }

    @Test
    fun `an http link without a marker does not support navigation`() {
        assertFalse(
            request("https://cdn.example.com/movies/interstellar.mkv").supportsEpisodeNavigation()
        )
    }

    @Test
    fun `a device video never supports navigation`() {
        assertFalse(request("content://media/external/video/media/1204").supportsEpisodeNavigation())
        assertFalse(
            request("file:///storage/emulated/0/Movies/S01E02.mkv").supportsEpisodeNavigation()
        )
    }

    @Test
    fun `a device video opened from a folder supports navigation`() {
        val uri = "content://media/external/video/media/1204"
        val request = PlaybackRequest(
            key = uri,
            title = "clip",
            uri = uri,
            source = PlaybackRequest.Source.DEVICE,
            folderId = 42L
        )

        assertTrue(request.supportsEpisodeNavigation())
    }

    @Test
    fun `a local video shared from another app supports navigation without a folder`() {
        val content = PlaybackRequest(
            key = "content://com.example.gallery/shared/video/1204",
            title = "clip",
            uri = "content://com.example.gallery/shared/video/1204",
            source = PlaybackRequest.Source.DEVICE
        )
        val mediaStore = PlaybackRequest(
            key = "content://media/external/video/media/1204",
            title = "clip",
            uri = "content://media/external/video/media/1204",
            source = PlaybackRequest.Source.DEVICE
        )
        val file = content.copy(
            key = "file:///storage/emulated/0/a.mkv",
            uri = "file:///storage/emulated/0/a.mkv"
        )

        assertTrue(content.supportsEpisodeNavigation())
        assertTrue(mediaStore.supportsEpisodeNavigation())
        assertTrue(file.supportsEpisodeNavigation())
    }

    @Test
    fun `a remote stream opened as a device video does not support navigation`() {
        val uri = "https://cdn.example.com/live/stream"
        val request = PlaybackRequest(
            key = uri,
            title = "clip",
            uri = uri,
            source = PlaybackRequest.Source.DEVICE
        )

        assertFalse(request.supportsEpisodeNavigation())
    }

    @Test
    fun `the scheme check is case insensitive`() {
        assertTrue(request("HTTP://cdn.example.com/show/E05.mkv").supportsEpisodeNavigation())
    }
}
