package com.opplayer.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.opplayer.app.data.LocalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailLoader {
    private val cache = object : LruCache<String, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun loadThumbnail(context: Context, video: LocalVideo): Bitmap? =
        withContext(Dispatchers.IO) {
            cache.get(video.uri)?.let { return@withContext it }

            val bitmap = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        Uri.parse(video.uri),
                        Size(320, 180),
                        null
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

            bitmap?.also { cache.put(video.uri, it) }
        }
}

@Composable
fun rememberVideoThumbnail(video: LocalVideo): State<ImageBitmap?> {
    val context = LocalContext.current

    return produceState<ImageBitmap?>(initialValue = null, video.uri) {
        value = ThumbnailLoader.loadThumbnail(context, video)?.asImageBitmap()
    }
}
