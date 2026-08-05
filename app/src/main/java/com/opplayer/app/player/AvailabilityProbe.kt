package com.opplayer.app.player

import com.opplayer.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

sealed interface AvailabilityResult {
    data object Available : AvailabilityResult
    data object NotAvailable : AvailabilityResult
    data object NetworkUnavailable : AvailabilityResult
}

fun interface AvailabilityProbe {
    suspend fun probe(url: String): AvailabilityResult
}

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
        classify(statusCode(url, "HEAD", range = false)) ?: probeWithRange(url)
    } catch (error: IOException) {

        AvailabilityResult.NetworkUnavailable
    }

    private fun probeWithRange(url: String): AvailabilityResult = try {
        classify(statusCode(url, "GET", range = true)) ?: AvailabilityResult.NotAvailable
    } catch (error: IOException) {
        AvailabilityResult.NetworkUnavailable
    }

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
            Thread.currentThread().interrupt()
            throw CancellationException("Availability probe was cancelled")
        } catch (interrupted: InterruptedIOException) {

            if (interrupted is SocketTimeoutException || !Thread.interrupted()) throw interrupted

            Thread.currentThread().interrupt()
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
        const val DEFAULT_TIMEOUT_MS = 5_000

        private val RETRYABLE_CODES = setOf(408, 425, 429)

        private val RANGE_FALLBACK_CODES = setOf(403, 405, 501)

        /**
         * Maps an HTTP status onto an availability answer. `null` means the answer is
         * inconclusive, so the same URL has to be retried with a ranged GET.
         *
         * Server-side or throttling failures must never be reported as "episode does not
         * exist", otherwise a temporary 5xx/429 looks like the end of a series.
         */
        internal fun classify(code: Int): AvailabilityResult? = when {
            code in 200..299 -> AvailabilityResult.Available
            code < 0 || code in RANGE_FALLBACK_CODES -> null
            code in 500..599 || code in RETRYABLE_CODES -> AvailabilityResult.NetworkUnavailable
            else -> AvailabilityResult.NotAvailable
        }

        val DEFAULT_USER_AGENT: String = "OPPlayer/" + BuildConfig.VERSION_NAME + " (Android)"
    }
}
