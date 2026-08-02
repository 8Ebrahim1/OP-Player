package com.opplayer.app.player

import com.opplayer.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of asking whether one URL can be played.
 *
 * The boolean this replaces could not tell "the server says no" apart from
 * "there is no network", so an offline device was reported to the user as
 * "this was the last episode".
 */
sealed interface AvailabilityResult {
    data object Available : AvailabilityResult
    data object NotAvailable : AvailabilityResult
    data object NetworkUnavailable : AvailabilityResult
}

/**
 * Checks whether a media URL exists.
 *
 * An interface so that episode navigation can be unit tested without a network,
 * and so the blocking HTTP implementation can be swapped later.
 */
fun interface AvailabilityProbe {
    suspend fun probe(url: String): AvailabilityResult
}

/**
 * Default probe: a HEAD request, falling back to a one byte ranged GET for the
 * servers that refuse HEAD.
 *
 * Cancellation is honoured for real. [HttpURLConnection.getResponseCode] is a
 * blocking call that ignores coroutine cancellation, so the work runs inside
 * [runInterruptible] and the connection is disconnected from a `finally` block.
 * That is what makes the eight second deadline in `EpisodeController` an actual
 * deadline instead of a hopeful one.
 */
class HttpAvailabilityProbe(
    private val timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val userAgent: String = DEFAULT_USER_AGENT
) : AvailabilityProbe {

    override suspend fun probe(url: String): AvailabilityResult {
        if (!url.startsWith("http", ignoreCase = true)) return AvailabilityResult.NotAvailable

        return withContext(Dispatchers.IO) {
            runInterruptible { probeBlocking(url) }
        }
    }

    private fun probeBlocking(url: String): AvailabilityResult = try {
        when (val code = statusCode(url, "HEAD", range = false)) {
            in 200..299 -> AvailabilityResult.Available
            403, 405, 501 -> probeWithRange(url)
            else -> if (code < 0) probeWithRange(url) else AvailabilityResult.NotAvailable
        }
    } catch (error: IOException) {
        // Kept separate from "not found": the episode may well exist.
        AvailabilityResult.NetworkUnavailable
    }

    private fun probeWithRange(url: String): AvailabilityResult = try {
        val code = statusCode(url, "GET", range = true)
        if (code in 200..299) AvailabilityResult.Available else AvailabilityResult.NotAvailable
    } catch (error: IOException) {
        AvailabilityResult.NetworkUnavailable
    }

    /**
     * Opens, reads and always closes one connection.
     *
     * Returns -1 when the server answered nothing useful; every I/O failure is
     * rethrown so the caller can report it as a network problem.
     */
    private fun statusCode(url: String, method: String, range: Boolean): Int {
        var connection: HttpURLConnection? = null

        return try {
            connection = openConnection(url, method)
            if (range) connection.setRequestProperty("Range", "bytes=0-0")

            val code = connection.responseCode

            if (range && code in 200..299) {
                connection.inputStream.use { it.read() }
            }

            code
        } catch (interrupted: InterruptedException) {
            // runInterruptible turns cancellation into a thread interrupt.
            throw CancellationException("Availability probe was cancelled")
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    private fun openConnection(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "*/*")
        }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 8_000
        val DEFAULT_USER_AGENT: String = "OPPlayer/" + BuildConfig.VERSION_NAME + " (Android)"
    }
}
