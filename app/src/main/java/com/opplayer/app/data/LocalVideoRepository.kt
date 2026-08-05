package com.opplayer.app.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.opplayer.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalVideoRepository(private val context: Context) {

    suspend fun loadFolders(): List<VideoFolder> = withContext(Dispatchers.IO) {
        val videos = queryVideos()

        videos
            .groupBy { it.bucketId }
            .map { (bucketId, items) ->
                VideoFolder(
                    id = bucketId,
                    name = items.first().folderName,
                    videos = items.sortedByDescending { it.dateAddedSec }
                )
            }
            .sortedWith(compareByDescending<VideoFolder> { it.count }.thenBy { it.name })
    }

    private fun queryVideos(): List<LocalVideo> {
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
            null,
            null,
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
