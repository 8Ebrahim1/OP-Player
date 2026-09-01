package com.opplayer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceVideoKeysTest {

    @Test
    fun `a media store uri is already canonical`() {
        val uri = "content://media/external/video/media/1204"

        assertEquals(uri, DeviceVideoKeys.canonical(uri))
    }

    @Test
    fun `another volume canonicalizes to the aggregate video uri`() {
        assertEquals(
            "content://media/external/video/media/1204",
            DeviceVideoKeys.canonical("content://media/external_primary/video/media/1204")
        )
    }

    @Test
    fun `a media documents uri canonicalizes to the video uri`() {
        assertEquals(
            "content://media/external/video/media/1204",
            DeviceVideoKeys.canonical(
                "content://com.android.providers.media.documents/document/video:1204"
            )
        )
    }

    @Test
    fun `an encoded documents uri canonicalizes to the video uri`() {
        assertEquals(
            "content://media/external/video/media/1204",
            DeviceVideoKeys.canonical(
                "content://com.android.providers.media.documents/document/video%3A1204"
            )
        )
    }

    @Test
    fun `a trailing query does not leak into the id`() {
        assertEquals(
            "content://media/external/video/media/1204",
            DeviceVideoKeys.canonical("content://media/external/video/media/1204?foo=bar")
        )
    }

    @Test
    fun `non media store uris are kept as they are`() {
        val file = "file:///storage/emulated/0/Movies/a.mkv"
        val provider = "content://com.example.gallery/shared/video/1204"
        val documentImage = "content://com.android.providers.media.documents/document/image:1204"
        val mediaRoot = "content://media/external/video/media"

        assertEquals(file, DeviceVideoKeys.canonical(file))
        assertEquals(provider, DeviceVideoKeys.canonical(provider))
        assertEquals(documentImage, DeviceVideoKeys.canonical(documentImage))
        assertEquals(mediaRoot, DeviceVideoKeys.canonical(mediaRoot))
    }

    @Test
    fun `media store detection matches canonicalization`() {
        assertTrue(DeviceVideoKeys.isMediaStoreVideo("content://media/external/video/media/1"))
        assertTrue(
            DeviceVideoKeys.isMediaStoreVideo(
                "content://com.android.providers.media.documents/document/video:1"
            )
        )
        assertFalse(DeviceVideoKeys.isMediaStoreVideo("file:///storage/emulated/0/a.mkv"))
        assertFalse(DeviceVideoKeys.isMediaStoreVideo("content://media/external/video/media"))
    }
}
