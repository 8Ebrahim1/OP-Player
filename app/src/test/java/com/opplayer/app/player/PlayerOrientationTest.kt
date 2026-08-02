package com.opplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerOrientationTest {

    @Test
    fun `picture in picture never requests an orientation`() {
        assertEquals(
            PlayerOrientation.UNSPECIFIED,
            OrientationPolicy.resolve(
                videoAspect = 1.78f,
                isFullscreen = true,
                autoRotateEnabled = true,
                isInPip = true
            )
        )
    }

    @Test
    fun `portrait video rotates to portrait when auto rotate is on`() {
        assertEquals(
            PlayerOrientation.SENSOR_PORTRAIT,
            OrientationPolicy.resolve(
                videoAspect = 0.56f,
                isFullscreen = false,
                autoRotateEnabled = true,
                isInPip = false
            )
        )
    }

    @Test
    fun `portrait video stays portrait in fullscreen even without auto rotate`() {
        assertEquals(
            PlayerOrientation.SENSOR_PORTRAIT,
            OrientationPolicy.resolve(
                videoAspect = 0.56f,
                isFullscreen = true,
                autoRotateEnabled = false,
                isInPip = false
            )
        )
    }

    @Test
    fun `fullscreen locks landscape for a wide video`() {
        assertEquals(
            PlayerOrientation.SENSOR_LANDSCAPE,
            OrientationPolicy.resolve(
                videoAspect = 1.78f,
                isFullscreen = true,
                autoRotateEnabled = false,
                isInPip = false
            )
        )
    }

    @Test
    fun `fullscreen locks landscape before the video size is known`() {
        assertEquals(
            PlayerOrientation.SENSOR_LANDSCAPE,
            OrientationPolicy.resolve(
                videoAspect = 0f,
                isFullscreen = true,
                autoRotateEnabled = false,
                isInPip = false
            )
        )
    }

    @Test
    fun `auto rotate follows a wide video without fullscreen`() {
        assertEquals(
            PlayerOrientation.SENSOR_LANDSCAPE,
            OrientationPolicy.resolve(
                videoAspect = 1.78f,
                isFullscreen = false,
                autoRotateEnabled = true,
                isInPip = false
            )
        )
    }

    @Test
    fun `nothing is forced when auto rotate is off`() {
        assertEquals(
            PlayerOrientation.UNSPECIFIED,
            OrientationPolicy.resolve(
                videoAspect = 1.78f,
                isFullscreen = false,
                autoRotateEnabled = false,
                isInPip = false
            )
        )
    }

    @Test
    fun `nearly square video is left to the system`() {
        assertEquals(
            PlayerOrientation.UNSPECIFIED,
            OrientationPolicy.resolve(
                videoAspect = 1.0f,
                isFullscreen = false,
                autoRotateEnabled = true,
                isInPip = false
            )
        )
    }

    @Test
    fun `aspect uses square pixels by default`() {
        assertEquals(1.777f, OrientationPolicy.aspectOf(1920, 1080, 1f), 0.01f)
    }

    @Test
    fun `aspect applies the pixel ratio of anamorphic video`() {
        assertEquals(1.777f, OrientationPolicy.aspectOf(1440, 1080, 1.333f), 0.01f)
    }

    @Test
    fun `aspect falls back to square pixels for an invalid ratio`() {
        assertEquals(1.777f, OrientationPolicy.aspectOf(1920, 1080, 0f), 0.01f)
    }

    @Test
    fun `aspect is zero while the size is unknown`() {
        assertEquals(0f, OrientationPolicy.aspectOf(0, 0, 1f), 0.0001f)
    }
}
