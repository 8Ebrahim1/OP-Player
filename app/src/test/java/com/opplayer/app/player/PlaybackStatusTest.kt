package com.opplayer.app.player

import com.opplayer.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStatusTest {

    @Test
    fun `only preparing and buffering show a spinner`() {
        assertTrue(PlaybackStatus.Preparing.isLoading)
        assertTrue(PlaybackStatus.Buffering.isLoading)

        assertFalse(PlaybackStatus.Idle.isLoading)
        assertFalse(PlaybackStatus.Ready.isLoading)
        assertFalse(PlaybackStatus.Ended.isLoading)
        assertFalse(PlaybackStatus.Error(R.string.error_network).isLoading)
    }

    @Test
    fun `only the error state carries a message`() {
        assertNull(PlaybackStatus.Idle.errorMessageRes)
        assertNull(PlaybackStatus.Preparing.errorMessageRes)
        assertNull(PlaybackStatus.Ready.errorMessageRes)
        assertNull(PlaybackStatus.Buffering.errorMessageRes)
        assertNull(PlaybackStatus.Ended.errorMessageRes)

        assertEquals(
            R.string.error_network,
            PlaybackStatus.Error(R.string.error_network).errorMessageRes
        )
    }

    @Test
    fun `a failed state is never a loading state at the same time`() {
        val failed = PlaybackStatus.Error(R.string.error_unknown)

        assertFalse(failed.isLoading)
        assertEquals(R.string.error_unknown, failed.errorMessageRes)
    }

    @Test
    fun `errors with the same message are equal`() {
        assertEquals(
            PlaybackStatus.Error(R.string.error_format),
            PlaybackStatus.Error(R.string.error_format)
        )
    }
}
