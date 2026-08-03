@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.opplayer.app.ui.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.media3.common.Player
import com.opplayer.app.MainActivity

private const val MIN_PIP_RATIO = 0.45f
private const val MAX_PIP_RATIO = 2.35f
private const val RATIO_PRECISION = 1000

fun supportsPip(packageManager: PackageManager): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

fun enterPip(activity: MainActivity?, player: Player) {
    if (activity == null) return
    if (!supportsPip(activity.packageManager)) return

    val videoSize = player.videoSize
    val ratio = if (videoSize.width > 0 && videoSize.height > 0) {
        val raw = videoSize.width.toFloat() / videoSize.height.toFloat()
        val clamped = raw.coerceIn(MIN_PIP_RATIO, MAX_PIP_RATIO)
        Rational((clamped * RATIO_PRECISION).toInt(), RATIO_PRECISION)
    } else {
        Rational(16, 9)
    }

    runCatching {
        activity.enterPictureInPictureMode(
            PictureInPictureParams.Builder().setAspectRatio(ratio).build()
        )
    }
}
