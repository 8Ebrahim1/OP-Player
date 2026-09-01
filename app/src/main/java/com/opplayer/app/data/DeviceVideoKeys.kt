package com.opplayer.app.data

/**
 * The same device video reaches the player through different uri shapes depending on where it
 * was opened from: the in-app listing and most galleries hand out media store uris, while the
 * system picker and some file managers wrap the same video in a media documents uri. Progress
 * and folder matching are keyed by the canonical media store video uri — the exact form the
 * in-app listing builds with ContentUris.withAppendedId — so every entry point reads and
 * writes the same record.
 *
 * Pure string parsing on purpose: this also runs inside local unit tests where the Android
 * framework is stubbed out.
 */
object DeviceVideoKeys {

    private const val MEDIA_AUTHORITY = "media"
    private const val MEDIA_DOCUMENTS_AUTHORITY = "com.android.providers.media.documents"
    private const val CANONICAL_PREFIX = "content://media/external/video/media/"

    fun canonical(uri: String): String {
        val id = mediaStoreVideoId(uri) ?: return uri
        return CANONICAL_PREFIX + id
    }

    fun isMediaStoreVideo(uri: String): Boolean = mediaStoreVideoId(uri) != null

    private fun mediaStoreVideoId(uri: String): Long? {
        if (!uri.startsWith("content://", ignoreCase = true)) return null

        val authority = uri
            .substringAfter("://", "")
            .substringBefore('/')
            .lowercase()
        val lastSegment = uri
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('/')

        return when (authority) {
            // content://media/<volume>/video/media/<id>
            MEDIA_AUTHORITY -> lastSegment.toLongOrNull()

            // content://com.android.providers.media.documents/document/video:<id>
            MEDIA_DOCUMENTS_AUTHORITY -> lastSegment
                .replace("%3A", ":", ignoreCase = true)
                .substringAfter("video:", "")
                .toLongOrNull()

            else -> null
        }
    }
}
