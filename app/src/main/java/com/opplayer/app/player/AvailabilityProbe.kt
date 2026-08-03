package com.opplayer.app.player

import com.opplayer.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
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
        when (val code = statusCode(url, "HEAD", range = false)) {
            in 200..299 -> AvailabilityResult.Available
            403, 405, 501 -> probeWithRange(url)
            else -> if (code < 0) probeWithRange(url) else AvailabilityResult.NotAvailable
        }
    } catch (error: IOException) {

        AvailabilityResult.NetworkUnavailable
    }

    private fun probeWithRange(url: String): AvailabilityResult = try {
        val code = statusCode(url, "GET", range = true)
        if (code in 200..299) AvailabilityResult.Available else AvailabilityResult.NotAvailable
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
