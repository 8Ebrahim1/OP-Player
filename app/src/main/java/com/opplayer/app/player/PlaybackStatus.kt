package com.opplayer.app.player

import androidx.annotation.StringRes

sealed interface PlaybackStatus {

    data object Idle : PlaybackStatus

    data object Preparing : PlaybackStatus

    data object Ready : PlaybackStatus

    data object Buffering : PlaybackStatus

    data object Ended : PlaybackStatus

    data class Error(@StringRes val messageRes: Int) : PlaybackStatus

    val isLoading: Boolean
        get() = this is Preparing || this is Buffering

    @get:StringRes
    val errorMessageRes: Int?
        get() = (this as? Error)?.messageRes
}
