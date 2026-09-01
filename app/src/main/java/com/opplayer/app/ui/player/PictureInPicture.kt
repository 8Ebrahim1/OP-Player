@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.opplayer.app.ui.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.media3.common.Player
import com.opplayer.app.MainActivity
import com.opplayer.app.R

private const val MIN_PIP_RATIO = 0.45f
private const val MAX_PIP_RATIO = 2.35f
private const val RATIO_PRECISION = 1000
private const val PIP_PLAY_PAUSE_REQUEST = 3101

fun supportsPip(packageManager: PackageManager): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

private fun aspectRatio(player: Player): Rational {
    val videoSize = player.videoSize
    if (videoSize.width <= 0 || videoSize.height <= 0) return Rational(16, 9)

    val raw = videoSize.width.toFloat() / videoSize.height.toFloat()
    val clamped = raw.coerceIn(MIN_PIP_RATIO, MAX_PIP_RATIO)
    return Rational((clamped * RATIO_PRECISION).toInt(), RATIO_PRECISION)
}

/**
 * The PiP window hides the player controls, so play/pause is published as a remote action and
 * handed back to the activity through a broadcast.
 */
private fun playPauseAction(activity: MainActivity, isPlaying: Boolean): RemoteAction {
    val pendingIntent = PendingIntent.getBroadcast(
        activity,
        PIP_PLAY_PAUSE_REQUEST,
        Intent(MainActivity.ACTION_PIP_PLAY_PAUSE).setPackage(activity.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val label = activity.getString(if (isPlaying) R.string.pause else R.string.play)
    val icon = Icon.createWithResource(
        activity,
        if (isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play
    )

    return RemoteAction(icon, label, label, pendingIntent)
}

private fun pipParams(
    activity: MainActivity,
    player: Player,
    sourceRect: Rect?
): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio(player))
        .setActions(listOf(playPauseAction(activity, player.isPlaying)))

    if (sourceRect != null && !sourceRect.isEmpty) {
        builder.setSourceRectHint(sourceRect)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(true)
    }

    return builder.build()
}

fun updatePipParams(activity: MainActivity?, player: Player, sourceRect: Rect?) {
    if (activity == null) return
    if (!supportsPip(activity.packageManager)) return

    runCatching { activity.setPictureInPictureParams(pipParams(activity, player, sourceRect)) }
}

fun enterPip(activity: MainActivity?, player: Player, sourceRect: Rect? = null) {
    if (activity == null) return
    if (!supportsPip(activity.packageManager)) return

    runCatching { activity.enterPictureInPictureMode(pipParams(activity, player, sourceRect)) }
}
