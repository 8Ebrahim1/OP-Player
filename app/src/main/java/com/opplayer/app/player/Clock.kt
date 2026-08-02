package com.opplayer.app.player

/**
 * Wall clock, injected so that time dependent logic stays deterministic in tests.
 */
fun interface Clock {

    fun currentTimeMillis(): Long

    companion object {
        /** The real device clock. */
        val SYSTEM: Clock = Clock { System.currentTimeMillis() }
    }
}
