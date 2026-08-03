package com.opplayer.app.player

enum class PlayerOrientation { UNSPECIFIED, SENSOR_PORTRAIT, SENSOR_LANDSCAPE }

object OrientationPolicy {

    const val LANDSCAPE_ASPECT_MIN = 1.2f

    const val PORTRAIT_ASPECT_MAX = 0.9f

    fun resolve(
        videoAspect: Float,
        isFullscreen: Boolean,
        autoRotateEnabled: Boolean,
        isInPip: Boolean
    ): PlayerOrientation = when {
        isInPip -> PlayerOrientation.UNSPECIFIED

        videoAspect > 0f &&
            videoAspect <= PORTRAIT_ASPECT_MAX &&
            (autoRotateEnabled || isFullscreen) -> PlayerOrientation.SENSOR_PORTRAIT

        isFullscreen -> PlayerOrientation.SENSOR_LANDSCAPE

        autoRotateEnabled &&
            videoAspect >= LANDSCAPE_ASPECT_MIN -> PlayerOrientation.SENSOR_LANDSCAPE

        else -> PlayerOrientation.UNSPECIFIED
    }

    fun aspectOf(width: Int, height: Int, pixelWidthHeightRatio: Float): Float {
        val ratio = if (pixelWidthHeightRatio > 0f) pixelWidthHeightRatio else 1f
        val scaledWidth = width * ratio
        return if (scaledWidth > 0f && height > 0) scaledWidth / height else 0f
    }
}
