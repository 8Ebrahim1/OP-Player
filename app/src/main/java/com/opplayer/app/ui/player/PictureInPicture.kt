@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.opplayer.app.ui.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.media3.common.Player
import com.opplayer.app.MainActivity

private const val MIN_PIP_RATIO = 0.45f
private const val MAX_PIP_RATIO = 2.35f
private const val RATIO_PRECISION = 1000

fun supportsPip(packageManager: PackageManager): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

private fun aspectRatio(player: Player): Rational {
    val videoSize = player.videoSize
    if (videoSize.width <= 0 || videoSize.height <= 0) return Rational(16, 9)

    val raw = videoSize.width.toFloat() / videoSize.height.toFloat()
    val clamped = raw.coerceIn(MIN_PIP_RATIO, MAX_PIP_RATIO)
    return Rational((clamped * RATIO_PRECISION).toInt(), RATIO_PRECISION)
}

private fun pipParams(player: Player, sourceRect: Rect?): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder().setAspectRatio(aspectRatio(player))

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

    runCatching { activity.setPictureInPictureParams(pipParams(player, sourceRect)) }
}

fun enterPip(activity: MainActivity?, player: Player, sourceRect: Rect? = null) {
    if (activity == null) return
    if (!supportsPip(activity.packageManager)) return

    runCatching { activity.enterPictureInPictureMode(pipParams(player, sourceRect)) }
}
