package com.opplayer.app.data

data class SharedVideoInfo(
    val displayName: String?,
    val sizeBytes: Long?
)

data class LocalVideo(
    val id: Long,
    val uri: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,

    val bucketId: Long,
    val folderName: String,
    val dateAddedSec: Long,
    val mimeType: String?
) {
    val extension: String
        get() = name.substringAfterLast('.', "").uppercase()
}

data class VideoFolder(
    val id: Long,
    val name: String,
    val videos: List<LocalVideo>
) {
    val count: Int get() = videos.size
    val totalDurationMs: Long get() = videos.sumOf { it.durationMs }
}
