package com.opplayer.app.player

import androidx.annotation.StringRes
import com.opplayer.app.R

enum class VideoScaleMode(@StringRes val labelRes: Int) {
    ZOOM(R.string.aspect_zoom),
    FIT(R.string.aspect_fit),
    FILL(R.string.aspect_fill);

    fun next(): VideoScaleMode {
        val modes = VideoScaleMode.entries
        return modes[(ordinal + 1) % modes.size]
    }
}
