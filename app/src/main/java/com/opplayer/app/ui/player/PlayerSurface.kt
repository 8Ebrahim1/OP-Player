@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.opplayer.app.ui.player

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.opplayer.app.player.VideoScaleMode

private const val CONTROLLER_TIMEOUT_MS = 4_000

fun VideoScaleMode.toResizeMode(): Int = when (this) {
    VideoScaleMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    VideoScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    VideoScaleMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
}

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

@Composable
fun PlayerSurface(
    player: Player,
    scaleMode: VideoScaleMode,
    controlsEnabled: Boolean,
    showEpisodeButtons: Boolean,
    controls: PlayerControlsState,
    touchListener: View.OnTouchListener?,
    onNextEpisode: () -> Unit,
    onPreviousEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val next by rememberUpdatedState(onNextEpisode)
    val previous by rememberUpdatedState(onPreviousEpisode)

    // The controller draws its next/previous buttons right beside the 15 second seek buttons, so
    // episode navigation is published as a playlist command instead of living in the top bar.
    val navigablePlayer = remember(player) {
        object : ForwardingPlayer(player) {

            override fun getAvailableCommands(): Player.Commands =
                super.getAvailableCommands()
                    .buildUpon()
                    .addAll(
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                    )
                    .build()

            // ForwardingPlayer delegates this straight to the wrapped player, bypassing the
            // commands added above, so the episode buttons could be left disabled without it.
            override fun isCommandAvailable(command: Int): Boolean =
                command == Player.COMMAND_SEEK_TO_NEXT ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                    command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                    super.isCommandAvailable(command)

            override fun hasNextMediaItem(): Boolean = true

            override fun hasPreviousMediaItem(): Boolean = true

            override fun seekToNext() = next()

            override fun seekToNextMediaItem() = next()

            override fun seekToPrevious() = previous()

            override fun seekToPreviousMediaItem() = previous()
        }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                // The controller mirrors in RTL locales; pin it LTR so previous stays on the
                // left and next on the right.
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                this.player = navigablePlayer
                useController = true
                controllerAutoShow = true
                controllerShowTimeoutMs = CONTROLLER_TIMEOUT_MS
                keepScreenOn = true

                subtitleView?.visibility = View.GONE
                setShowNextButton(showEpisodeButtons)
                setShowPreviousButton(showEpisodeButtons)
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
            view.setShowNextButton(showEpisodeButtons)
            view.setShowPreviousButton(showEpisodeButtons)
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
