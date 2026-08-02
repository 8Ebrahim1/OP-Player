package com.opplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoScaleModeTest {

    @Test
    fun `next cycles through every mode and wraps around`() {
        assertEquals(VideoScaleMode.FIT, VideoScaleMode.ZOOM.next())
        assertEquals(VideoScaleMode.FILL, VideoScaleMode.FIT.next())
        assertEquals(VideoScaleMode.ZOOM, VideoScaleMode.FILL.next())
    }

    @Test
    fun `cycling the whole enum returns to the starting mode`() {
        var mode = VideoScaleMode.ZOOM
        repeat(VideoScaleMode.entries.size) { mode = mode.next() }
        assertEquals(VideoScaleMode.ZOOM, mode)
    }

    @Test
    fun `every mode has a label`() {
        assertTrue(VideoScaleMode.entries.all { it.labelRes != 0 })
        assertEquals(
            VideoScaleMode.entries.size,
            VideoScaleMode.entries.map { it.labelRes }.distinct().size
        )
    }
}
