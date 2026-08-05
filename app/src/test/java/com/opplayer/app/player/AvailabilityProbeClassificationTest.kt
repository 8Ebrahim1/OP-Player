package com.opplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The status mapping decides whether a missing episode is really missing or just a temporary
 * server problem, so it is pinned down here instead of only being exercised over the network.
 */
class AvailabilityProbeClassificationTest {

    @Test
    fun `a success means the episode exists`() {
        assertEquals(AvailabilityResult.Available, HttpAvailabilityProbe.classify(200))
        assertEquals(AvailabilityResult.Available, HttpAvailabilityProbe.classify(206))
        assertEquals(AvailabilityResult.Available, HttpAvailabilityProbe.classify(299))
    }

    @Test
    fun `a client rejection means the episode is missing`() {
        listOf(400, 404, 410, 451).forEach { code ->
            assertEquals(
                "status $code",
                AvailabilityResult.NotAvailable,
                HttpAvailabilityProbe.classify(code)
            )
        }
    }

    @Test
    fun `server errors and throttling are network problems, not missing episodes`() {
        listOf(408, 425, 429, 500, 502, 503, 504, 599).forEach { code ->
            assertEquals(
                "status $code",
                AvailabilityResult.NetworkUnavailable,
                HttpAvailabilityProbe.classify(code)
            )
        }
    }

    @Test
    fun `hosts that refuse a HEAD request ask for a ranged retry`() {
        listOf(403, 405, 501, -1).forEach { code ->
            assertNull("status $code", HttpAvailabilityProbe.classify(code))
        }
    }
}
