package com.opplayer.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.opplayer.app.data.LocalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object ThumbnailLoader {

    private val cache = object : LruCache<String, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun loadThumbnail(context: Context, video: LocalVideo): Bitmap? {
        cache.get(video.uri)?.let { return it }

        val bitmap = withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { runCatching { signal.cancel() } }

                val result = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(
                            Uri.parse(video.uri),
                            Size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT),
                            signal
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver,
                            video.id,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    }
                }.getOrNull()

                if (continuation.isActive) continuation.resume(result)
            }
        }

        return bitmap?.also { cache.put(video.uri, it) }
    }

    private const val THUMBNAIL_WIDTH = 320
    private const val THUMBNAIL_HEIGHT = 180
}

@Composable
fun rememberVideoThumbnail(video: LocalVideo): State<ImageBitmap?> {
    val context = LocalContext.current.applicationContext

    return produceState<ImageBitmap?>(initialValue = null, video.uri) {
        value = ThumbnailLoader.loadThumbnail(context, video)?.asImageBitmap()
    }
}
