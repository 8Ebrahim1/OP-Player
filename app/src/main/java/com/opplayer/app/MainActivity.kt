package com.opplayer.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.ui.OPPlayerApp
import com.opplayer.app.ui.theme.OPPlayerTheme

class MainActivity : ComponentActivity() {
    val isInPipMode: MutableState<Boolean> = mutableStateOf(false)

    var onUserLeaveAction: (() -> Unit)? = null

    private val pendingRequest: MutableState<PlaybackRequest?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            pendingRequest.value = intentToPlaybackRequest(intent)
        }

        setContent {
            OPPlayerTheme {
                OPPlayerApp(
                    initialRequest = pendingRequest.value,
                    onInitialRequestHandled = { pendingRequest.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        intentToPlaybackRequest(intent)?.let { request ->
            pendingRequest.value = request
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        onUserLeaveAction?.invoke()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode.value = isInPictureInPictureMode
    }

    private fun intentToPlaybackRequest(intent: Intent?): PlaybackRequest? {
        val data = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
        val name = data.lastPathSegment ?: getString(R.string.default_video_name)

        // A one-shot grant dies with the process, which breaks resume after process death.
        // Persist it when the sender allows it; ignore the failure otherwise.
        if (data.scheme.equals("content", ignoreCase = true)) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    data,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        return PlaybackRequest(
            key = data.toString(),
            title = name,
            uri = data.toString(),
            source = PlaybackRequest.Source.DEVICE
        )
    }
}
