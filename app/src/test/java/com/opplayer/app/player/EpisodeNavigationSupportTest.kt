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
    fun `the scheme check is case insensitive`() {
        assertTrue(request("HTTP://cdn.example.com/show/E05.mkv").supportsEpisodeNavigation())
    }
}
