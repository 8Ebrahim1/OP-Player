package com.opplayer.app.player.subtitle

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Reads a subtitle file from any supported location and parses it into cues. */
object SubtitleLoader {

    private const val MAX_BYTES = 8 * 1024 * 1024
    private const val TIMEOUT_MS = 20_000

    suspend fun load(context: Context, uri: String): LoadedSubtitle? = withContext(Dispatchers.IO) {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return@withContext null
        val bytes = readBytes(context, parsed) ?: return@withContext null
        if (bytes.isEmpty()) return@withContext null

        val name = displayName(context, parsed)
        val cues = runCatching { SubtitleParser.parse(name, bytes) }.getOrDefault(emptyList())

        if (cues.isEmpty()) null else LoadedSubtitle(uri = uri, name = name, cues = cues)
    }

    private fun readBytes(context: Context, uri: Uri): ByteArray? {
        val scheme = uri.scheme?.lowercase()

        return when {
            scheme == "http" || scheme == "https" -> readHttp(uri)
            scheme == "content" -> runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readAtMost(MAX_BYTES) }
            }.getOrNull()

            scheme == "file" -> runCatching {
                uri.path?.let { File(it) }?.takeIf { it.canRead() }?.inputStream()
                    ?.use { it.readAtMost(MAX_BYTES) }
            }.getOrNull()

            else -> runCatching {
                File(uri.toString()).takeIf { it.canRead() }?.inputStream()
                    ?.use { it.readAtMost(MAX_BYTES) }
            }.getOrNull()
        }
    }

    private fun readHttp(uri: Uri): ByteArray? = runCatching {
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "OPPlayer (Android)")

        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            connection.inputStream.use { it.readAtMost(MAX_BYTES) }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun java.io.InputStream.readAtMost(limit: Int): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)

        while (true) {
            val read = read(chunk)
            if (read <= 0) break
            buffer.write(chunk, 0, read)
            if (buffer.size() >= limit) break
        }

        return buffer.toByteArray()
    }

    private fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme?.lowercase() == "content") {
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst() && cursor.columnCount > 0) {
                        cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { return it }
                    }
                }
            }
        }

        return uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "subtitle.srt"
    }
}
