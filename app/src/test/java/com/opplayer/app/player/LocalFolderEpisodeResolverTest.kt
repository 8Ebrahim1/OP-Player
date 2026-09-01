package com.opplayer.app.player

import com.opplayer.app.data.LocalVideo
import com.opplayer.app.data.VideoFolder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFolderEpisodeResolverTest {

    private fun video(id: Long, name: String) = LocalVideo(
        id = id,
        uri = "content://media/external/video/media/$id",
        name = name,
        durationMs = 60_000L,
        sizeBytes = 1_024L,
        bucketId = 7L,
        folderName = "Movies",
        dateAddedSec = id,
        mimeType = "video/mp4"
    )

    private val folder = VideoFolder(
        id = 7L,
        name = "Movies",
        videos = listOf(video(1, "first.mp4"), video(2, "second.mp4"), video(3, "third.mp4"))
    )

    private val resolver = LocalFolderEpisodeResolver { listOf(folder) }

    private fun request(uri: String, folderId: Long? = 7L) = PlaybackRequest(
        key = uri,
        title = "clip",
        uri = uri,
        source = PlaybackRequest.Source.DEVICE,
        folderId = folderId
    )

    @Test
    fun `the next file in the folder is returned`() = runTest {
        val result = resolver.resolve(request(folder.videos[0].uri), forward = true)

        assertTrue(result is EpisodeResolutionResult.Found)
        val target = (result as EpisodeResolutionResult.Found).target
        assertEquals(folder.videos[1].uri, target.url)
        assertEquals("second.mp4", target.label)
    }

    @Test
    fun `the previous file in the folder is returned`() = runTest {
        val result = resolver.resolve(request(folder.videos[2].uri), forward = false)

        assertEquals(
            folder.videos[1].uri,
            (result as EpisodeResolutionResult.Found).target.url
        )
    }

    @Test
    fun `the last file has no next one`() = runTest {
        assertEquals(
            EpisodeResolutionResult.NotFound,
            resolver.resolve(request(folder.videos[2].uri), forward = true)
        )
    }

    @Test
    fun `a request without a folder is located across all folders`() = runTest {
        val result = resolver.resolve(request(folder.videos[0].uri, folderId = null), forward = true)

        assertEquals(
            folder.videos[1].uri,
            (result as EpisodeResolutionResult.Found).target.url
        )
    }

    @Test
    fun `an unknown video without a folder is not resolved`() = runTest {
        assertEquals(
            EpisodeResolutionResult.NotFound,
            resolver.resolve(
                request("content://media/external/video/media/99", folderId = null),
                forward = true
            )
        )
    }

    @Test
    fun `a documents provider uri from another app matches the media store entry`() = runTest {
        val shared = "content://com.android.providers.media.documents/document/video:2"

        val result = resolver.resolve(request(shared, folderId = null), forward = false)

        assertEquals(
            folder.videos[0].uri,
            (result as EpisodeResolutionResult.Found).target.url
        )
    }

    @Test
    fun `a video from a foreign provider is matched by its display name`() = runTest {
        val shared = "content://com.example.gallery/shared/55"
        val named = PlaybackRequest(
            key = shared,
            title = "second.mp4",
            uri = shared,
            source = PlaybackRequest.Source.DEVICE
        )

        val result = resolver.resolve(named, forward = true)

        assertEquals(
            folder.videos[2].uri,
            (result as EpisodeResolutionResult.Found).target.url
        )
    }

    @Test
    fun `a failing media store read is not a crash`() = runTest {
        val failing = LocalFolderEpisodeResolver { error("media store unavailable") }

        assertEquals(
            EpisodeResolutionResult.NotFound,
            failing.resolve(request(folder.videos[0].uri), forward = true)
        )
    }
}
