package com.opplayer.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.opplayer.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class LocalVideoRepository(private val context: Context) {

    suspend fun loadFolders(): List<VideoFolder> = withContext(Dispatchers.IO) {
        // The folder queue the episode resolver walks uses this same ordering, so next and
        // previous follow the order the user picked for the folder view.
        val sortOrder = runCatching {
            AppSettingsRepository(context).settings.first().videoSortOrder
        }.getOrDefault(VideoSortOrder.NAME_ASC)

        val videos = queryVideos()

        videos
            .groupBy { it.bucketId }
            .map { (bucketId, items) ->
                VideoFolder(
                    id = bucketId,
                    name = items.first().folderName,
                    videos = items.sortedWith(videoComparator(sortOrder))
                )
            }
            .sortedWith(compareByDescending<VideoFolder> { it.count }.thenBy { it.name })
    }

    private fun videoComparator(order: VideoSortOrder): Comparator<LocalVideo> = when (order) {
        VideoSortOrder.NAME_ASC -> compareBy(NaturalNameOrder) { it.name }
        VideoSortOrder.NAME_DESC -> compareByDescending(NaturalNameOrder) { it.name }
        VideoSortOrder.DATE_NEWEST ->
            compareByDescending<LocalVideo> { it.dateAddedSec }.thenBy(NaturalNameOrder) { it.name }
        VideoSortOrder.DATE_OLDEST ->
            compareBy<LocalVideo> { it.dateAddedSec }.thenBy(NaturalNameOrder) { it.name }
    }

    /**
     * Display name and size of a video another app shared with us, read through the granted
     * uri itself, so it works without the media permission.
     */
    fun readSharedVideoInfo(uri: Uri): SharedVideoInfo? {
        if (!uri.scheme.equals("content", ignoreCase = true)) return null

        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null

                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                SharedVideoInfo(
                    displayName = nameIndex.takeIf { it >= 0 }?.let { cursor.getString(it) },
                    sizeBytes = sizeIndex
                        .takeIf { it >= 0 && !cursor.isNull(it) }
                        ?.let { cursor.getLong(it) }
                )
            }
        }.getOrNull()
    }

    /**
     * Matches a video shared by another app against the media store by display name and size,
     * so progress and the folder queue work for galleries that do not hand out media store uris.
     */
    fun findVideo(displayName: String?, sizeBytes: Long?): LocalVideo? {
        if (displayName.isNullOrBlank()) return null

        if (sizeBytes != null && sizeBytes > 0L) {
            queryVideos(
                selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ? AND " +
                    "${MediaStore.Video.Media.SIZE} = ?",
                selectionArgs = arrayOf(displayName, sizeBytes.toString())
            ).firstOrNull()?.let { return it }
        }

        return queryVideos(
            selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
            selectionArgs = arrayOf(displayName)
        ).firstOrNull()
    }

    private fun queryVideos(
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): List<LocalVideo> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.BUCKET_ID)
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Video.Media.DATA)
            }
        }.toTypedArray()

        val result = mutableListOf<LocalVideo>()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            val bucketIdCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
            } else {
                -1
            }

            val bucketNameCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue

                val parentPath = if (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && bucketNameCol >= 0
                ) {
                    cursor.getString(bucketNameCol)?.let { File(it).parent }
                } else {
                    null
                }

                val folder = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        bucketNameCol.takeIf { it >= 0 }
                            ?.let { cursor.getString(it) }
                            ?: unknownFolderName()

                    else -> parentPath?.let { File(it).name } ?: unknownFolderName()
                }

                val bucketId = when {
                    bucketIdCol >= 0 && !cursor.isNull(bucketIdCol) -> cursor.getLong(bucketIdCol)
                    parentPath != null -> parentPath.lowercase().hashCode().toLong()
                    else -> UNKNOWN_BUCKET_ID
                }

                result += LocalVideo(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    name = name,
                    durationMs = cursor.getLong(durationCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    bucketId = bucketId,
                    folderName = folder,
                    dateAddedSec = cursor.getLong(dateCol),
                    mimeType = cursor.getString(mimeCol)
                )
            }
        }

        return result
    }

    /** Localized instead of a hardcoded Persian literal in the data layer. */
    private fun unknownFolderName(): String = context.getString(R.string.folder_other)

    private companion object {
        const val UNKNOWN_BUCKET_ID = -1L
    }
}
