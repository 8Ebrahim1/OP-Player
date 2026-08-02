package com.opplayer.app.ui.player

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.opplayer.app.player.VideoScaleMode

private const val CONTROLLER_TIMEOUT_MS = 4_000

/** media3 resize mode for a [VideoScaleMode]. */
fun VideoScaleMode.toResizeMode(): Int = when (this) {
    VideoScaleMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    VideoScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    VideoScaleMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
}

/**
 * Visibility of the media3 transport controls, hoisted out of the view.
 *
 * The screen needs to know whether the controls are on screen (the custom top
 * bar follows them) and needs to be able to show or toggle them from gestures.
 */
@Stable
class PlayerControlsState {

    var visible by mutableStateOf(true)
        internal set

    internal var view: PlayerView? = null

    fun show() {
        view?.showController()
    }

    fun toggle() {
        val target = view ?: return
        if (target.isControllerFullyVisible) target.hideController() else target.showController()
    }
}

@Composable
fun rememberPlayerControlsState(): PlayerControlsState = remember { PlayerControlsState() }

/** The video surface plus the built in media3 transport controls. */
@Composable
fun PlayerSurface(
    player: Player,
    scaleMode: VideoScaleMode,
    controlsEnabled: Boolean,
    controls: PlayerControlsState,
    touchListener: View.OnTouchListener?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                controllerAutoShow = true
                controllerShowTimeoutMs = CONTROLLER_TIMEOUT_MS
                keepScreenOn = true
                // Cues are drawn by SubtitleOverlay so styling and sync stay adjustable.
                subtitleView?.visibility = View.GONE
                setShowNextButton(false)
                setShowPreviousButton(false)
                setShowRewindButton(true)
                setShowFastForwardButton(true)
                setResizeMode(scaleMode.toResizeMode())
                setOnTouchListener(touchListener)
                setControllerVisibilityListener(
                    PlayerView.ControllerVisibilityListener { visibility ->
                        controls.visible = visibility == View.VISIBLE
                    }
                )
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                controls.view = this
                showController()
            }
        },
        update = { view ->
            view.resizeMode = scaleMode.toResizeMode()
            view.useController = controlsEnabled
            if (!controlsEnabled) view.hideController()
        },
        onRelease = { view ->
            view.setOnTouchListener(null)
            view.player = null
            controls.view = null
        },
        modifier = modifier
    )
}
