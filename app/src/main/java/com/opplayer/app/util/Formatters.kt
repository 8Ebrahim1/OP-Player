package com.opplayer.app.util

import java.util.Locale

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }.toPersianDigits()
}

fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0L) return ""
    val mb = sizeBytes / 1024.0 / 1024.0
    return if (mb >= 1024) {
        String.format(Locale.US, "%.1f GB", mb / 1024.0).toPersianDigits()
    } else {
        String.format(Locale.US, "%.0f MB", mb).toPersianDigits()
    }
}

private val persianDigits = charArrayOf(
    '\u06f0', '\u06f1', '\u06f2', '\u06f3', '\u06f4',
    '\u06f5', '\u06f6', '\u06f7', '\u06f8', '\u06f9'
)

fun String.toPersianDigits(): String = map { ch ->
    if (ch in '0'..'9') persianDigits[ch - '0'] else ch
}.joinToString("")

fun Int.toPersianDigits(): String = toString().toPersianDigits()

fun String.toLatinDigits(): String = map { ch ->
    when (ch) {
        in '\u06f0'..'\u06f9' -> '0' + (ch - '\u06f0')
        in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
        else -> ch
    }
}.joinToString("")

fun isValidMediaUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return false

    val allowedSchemes = listOf("http://", "https://", "rtsp://", "content://", "file://")
    if (allowedSchemes.none { trimmed.startsWith(it, ignoreCase = true) }) return false

    return runCatching { android.net.Uri.parse(trimmed).scheme != null }.getOrDefault(false)
}
