package com.opplayer.app.player

import androidx.annotation.StringRes

data class PlayerMessage(
    @StringRes val textRes: Int,
    val argument: String? = null,
    val long: Boolean = false
)
