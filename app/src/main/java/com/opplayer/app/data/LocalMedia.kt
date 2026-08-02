package com.opplayer.app.data

/** One video file found on the device. */
data class LocalVideo(
    val id: Long,
    val uri: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    /** MediaStore bucket id; the only reliable identity of the containing folder. */
    val bucketId: Long,
    val folderName: String,
    val dateAddedSec: Long,
    val mimeType: String?
) {
    val extension: String
        get() = name.substringAfterLast('.', "").uppercase()
}

/**
 * A device folder.
 *
 * Grouping used to happen by display name, so two different folders that happen
 * to be called "Videos" were merged into one and produced duplicate keys in the
 * lazy list. [id] is the MediaStore bucket id and is unique per folder.
 */
data class VideoFolder(
    val id: Long,
    val name: String,
    val videos: List<LocalVideo>
) {
    val count: Int get() = videos.size
    val totalDurationMs: Long get() = videos.sumOf { it.durationMs }
}
