package com.opplayer.app.player

import androidx.annotation.StringRes
import com.opplayer.app.R

/**
 * How the video is scaled inside the player surface.
 *
 * The UI layer maps these to the media3 `AspectRatioFrameLayout` constants, so
 * the player state stays free of view classes.
 */
enum class VideoScaleMode(@StringRes val labelRes: Int) {
    ZOOM(R.string.aspect_zoom),
    FIT(R.string.aspect_fit),
    FILL(R.string.aspect_fill);

    /** Next mode for the aspect-ratio button in the top bar. */
    fun next(): VideoScaleMode {
        val modes = VideoScaleMode.entries
        return modes[(ordinal + 1) % modes.size]
    }
}
