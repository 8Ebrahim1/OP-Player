package com.opplayer.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.O)
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

        return PlaybackRequest(
            key = data.toString(),
            title = name,
            uri = data.toString(),
            source = PlaybackRequest.Source.DEVICE
        )
    }
}
