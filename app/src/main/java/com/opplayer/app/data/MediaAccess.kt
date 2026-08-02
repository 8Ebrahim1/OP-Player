package com.opplayer.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * How much of the device media library the app may read right now.
 *
 * Android 14 introduced partial access: the user can grant a hand picked set of
 * items instead of the whole library. Treating that as "denied" hid videos the
 * user had explicitly shared, and treating it as "granted" hid the button that
 * lets them widen the selection, so it needs its own state.
 */
enum class MediaAccess {
    FULL,
    PARTIAL,
    DENIED;

    val canReadAnything: Boolean get() = this != DENIED
}

/** The runtime permission to request for full video access on this API level. */
val fullMediaPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/**
 * Permissions to ask for in one request.
 *
 * On Android 14+ the visual user selected permission is requested alongside the
 * full one, which is what makes the system offer the "select photos and videos"
 * option.
 */
val mediaPermissionRequest: Array<String>
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else {
        arrayOf(fullMediaPermission)
    }

/** Reads the current access level. Cheap enough to call on every ON_RESUME. */
fun Context.currentMediaAccess(): MediaAccess {
    if (isGranted(fullMediaPermission)) return MediaAccess.FULL

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    ) {
        return MediaAccess.PARTIAL
    }

    return MediaAccess.DENIED
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
