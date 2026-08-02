package com.opplayer.app.player

/** Screen orientation requested by the player, free of Android framework constants. */
enum class PlayerOrientation { UNSPECIFIED, SENSOR_PORTRAIT, SENSOR_LANDSCAPE }

/**
 * Decides how the screen should be oriented for the video that is playing.
 *
 * Kept as pure functions so the rules can be unit tested without an Activity.
 */
object OrientationPolicy {

    /** Anything wider than this is treated as a landscape video. */
    const val LANDSCAPE_ASPECT_MIN = 1.2f

    /** Anything narrower than this is treated as a portrait video (stories, reels). */
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

    /** Display aspect ratio of a decoded frame, or 0 when the size is not known yet. */
    fun aspectOf(width: Int, height: Int, pixelWidthHeightRatio: Float): Float {
        val ratio = if (pixelWidthHeightRatio > 0f) pixelWidthHeightRatio else 1f
        val scaledWidth = width * ratio
        return if (scaledWidth > 0f && height > 0) scaledWidth / height else 0f
    }
}
