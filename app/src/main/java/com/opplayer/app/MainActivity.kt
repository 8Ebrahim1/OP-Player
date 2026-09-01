package com.opplayer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.opplayer.app.data.DeviceVideoKeys
import com.opplayer.app.data.LocalVideoRepository
import com.opplayer.app.data.currentMediaAccess
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.ui.OPPlayerApp
import com.opplayer.app.ui.theme.OPPlayerTheme

class MainActivity : ComponentActivity() {
    val isInPipMode: MutableState<Boolean> = mutableStateOf(false)

    var onUserLeaveAction: (() -> Unit)? = null

    var onPipPlayPauseAction: (() -> Unit)? = null

    private val pendingRequest: MutableState<PlaybackRequest?> = mutableStateOf(null)

    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_PIP_PLAY_PAUSE) return
            onPipPlayPauseAction?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ContextCompat.registerReceiver(
            this,
            pipActionReceiver,
            IntentFilter(ACTION_PIP_PLAY_PAUSE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

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

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipActionReceiver) }
        super.onDestroy()
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

        // Providers that only hand out a numbered uri are still asked for the real file name,
        // so the player top bar shows it instead of the uri's trailing id.
        val repository = LocalVideoRepository(this)
        val shared = repository.readSharedVideoInfo(data)
        val displayName = shared?.displayName?.takeIf { it.isNotBlank() } ?: data.lastPathSegment
        val name = displayName ?: getString(R.string.default_video_name)

        // Matching the shared file against the media store keys progress and the folder queue
        // to the same video no matter which gallery app handed it over.
        val match = if (currentMediaAccess().canReadAnything) {
            runCatching {
                repository.findVideo(displayName, shared?.sizeBytes)
            }.getOrNull()
        } else {
            null
        }

        return PlaybackRequest(
            key = match?.uri ?: DeviceVideoKeys.canonical(data.toString()),
            title = name,
            uri = data.toString(),
            source = PlaybackRequest.Source.DEVICE,
            folderId = match?.bucketId
        )
    }

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.opplayer.app.action.PIP_PLAY_PAUSE"
    }
}
