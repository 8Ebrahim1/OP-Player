package com.opplayer.app.player.subtitle

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Finds "sidecar" subtitle files that sit next to an offline video
 * (for example `Movie.mkv` + `Movie.fa.srt`).
 *
 * Offline videos come from MediaStore as `content://` URIs, so the plain file
 * system is usually not readable on Android 10+. We therefore search MediaStore
 * itself and only fall back to direct file access on older devices.
 */
object SubtitleLocator {

    private val EXTENSIONS = listOf("srt", "vtt", "ass", "ssa", "sub", "ttml", "dfxp", "smi")

    suspend fun findCandidates(context: Context, videoUri: String): List<SubtitleFileCandidate> =
        withContext(Dispatchers.IO) {
            val uri = runCatching { Uri.parse(videoUri) }.getOrNull()
                ?: return@withContext emptyList()

            val scheme = uri.scheme?.lowercase()
            if (scheme != null && scheme != "content" && scheme != "file") {
                return@withContext emptyList()
            }

            val info = videoInfo(context, uri) ?: return@withContext emptyList()
            val baseName = info.displayName.substringBeforeLast('.', info.displayName)

            val results = LinkedHashMap<String, SubtitleFileCandidate>()

            mediaStoreMatches(context, info, baseName).forEach { results[it.uri] = it }
            fileSystemMatches(info, baseName).forEach { results.putIfAbsent(it.uri, it) }

            results.values.sortedWith(
                compareByDescending<SubtitleFileCandidate> { exactMatchScore(it.name, baseName) }
                    .thenBy { it.name }
            )
        }

    suspend fun findBest(context: Context, videoUri: String): String? =
        findCandidates(context, videoUri).firstOrNull()?.uri

    private data class VideoInfo(
        val displayName: String,
        val relativePath: String?,
        val filePath: String?
    )

    private fun videoInfo(context: Context, uri: Uri): VideoInfo? {
        if (uri.scheme?.lowercase() == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            return VideoInfo(
                displayName = file.name,
                relativePath = null,
                filePath = path
            )
        }

        val projection = buildList {
            add(MediaStore.Video.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            }
            @Suppress("DEPRECATION")
            add(MediaStore.Video.Media.DATA)
        }.toTypedArray()

        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null

                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                @Suppress("DEPRECATION")
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                val name = nameCol.takeIf { it >= 0 }?.let { cursor.getString(it) }
                val data = dataCol.takeIf { it >= 0 }?.let { cursor.getString(it) }
                val display = name ?: data?.substringAfterLast('/') ?: return@use null

                VideoInfo(
                    displayName = display,
                    relativePath = pathCol.takeIf { it >= 0 }?.let { cursor.getString(it) },
                    filePath = data
                )
            }
        }.getOrNull()
    }

    private fun mediaStoreMatches(
        context: Context,
        info: VideoInfo,
        baseName: String
    ): List<SubtitleFileCandidate> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME
        )

        val selection: String
        val arguments: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.relativePath != null) {
            selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            arguments = arrayOf(info.relativePath, "$baseName%")
        } else {
            selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            arguments = arrayOf("$baseName%")
        }

        val results = mutableListOf<SubtitleFileCandidate>()

        runCatching {
            context.contentResolver.query(collection, projection, selection, arguments, null)
                ?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameCol) ?: continue
                        if (!isSubtitleName(name)) continue

                        val id = cursor.getLong(idCol)
                        results += SubtitleFileCandidate(
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            name = name
                        )
                    }
                }
        }

        return results
    }

    private fun fileSystemMatches(
        info: VideoInfo,
        baseName: String
    ): List<SubtitleFileCandidate> {
        val parent = info.filePath?.let { File(it).parentFile } ?: return emptyList()
        if (!parent.canRead()) return emptyList()

        val siblings = runCatching { parent.listFiles() }.getOrNull() ?: return emptyList()

        return siblings
            .filter { it.isFile && it.canRead() && isSubtitleName(it.name) }
            .filter { it.name.startsWith(baseName, ignoreCase = true) }
            .map { SubtitleFileCandidate(uri = Uri.fromFile(it).toString(), name = it.name) }
    }

    private fun isSubtitleName(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in EXTENSIONS

    private fun exactMatchScore(subtitleName: String, baseName: String): Int {
        val subtitleBase = subtitleName.substringBeforeLast('.', subtitleName)
        val extension = subtitleName.substringAfterLast('.', "").lowercase()

        var score = 0
        if (subtitleBase.equals(baseName, ignoreCase = true)) score += 10
        if (extension == "srt") score += 3
        if (extension == "vtt") score += 2
        if (subtitleName.contains("persian", true) ||
            subtitleName.contains("farsi", true) ||
            subtitleName.contains(".fa", true)
        ) {
            score += 4
        }

        return score
    }
}
