package com.opplayer.app.player

import android.content.Context

/**
 * Creates the [PlayerEngine] used by a player screen.
 *
 * Injecting this instead of calling [MediaFactory.createPlayer] directly is what
 * lets `PlayerViewModel` run under plain JUnit with a fake engine.
 */
fun interface PlayerFactory {
    fun create(context: Context): PlayerEngine
}

/** Production factory: a real ExoPlayer wrapped in [ExoPlayerEngine]. */
object DefaultPlayerFactory : PlayerFactory {
    override fun create(context: Context): PlayerEngine =
        ExoPlayerEngine(MediaFactory.createPlayer(context))
}
