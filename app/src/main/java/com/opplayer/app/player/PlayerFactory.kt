package com.opplayer.app.player

import android.content.Context

fun interface PlayerFactory {
    fun create(context: Context): PlayerEngine
}

object DefaultPlayerFactory : PlayerFactory {
    override fun create(context: Context): PlayerEngine =
        ExoPlayerEngine(MediaFactory.createPlayer(context))
}
