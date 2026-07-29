package com.opplayer.app.ui

data class VideoItem(
    val id: Long,
    val title: String,
    val url: String,
    val subtitleUrl: String = "",
    val positionMs: Long = 0L,
    val favorite: Boolean = false,
    val lastPlayedAt: Long = 0L
)
