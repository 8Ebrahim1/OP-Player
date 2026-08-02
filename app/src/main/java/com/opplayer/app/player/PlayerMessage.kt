package com.opplayer.app.player

import androidx.annotation.StringRes

/**
 * A one-shot message the player wants to show, delivered exactly once.
 *
 * Lives in the player package (not the UI package) so playback collaborators
 * can raise messages without depending on Compose.
 */
data class PlayerMessage(
    @StringRes val textRes: Int,
    val argument: String? = null,
    val long: Boolean = false
)
