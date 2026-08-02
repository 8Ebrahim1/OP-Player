package com.opplayer.app.util

import java.util.Locale

/** Decimal separator used by Persian text. */
private const val DECIMAL_SEPARATOR_FA = '\u066b'

/**
 * Formats a duration as `mm:ss` or `h:mm:ss`.
 *
 * Digit shaping is opt in. The formatters used to always emit Persian digits,
 * which leaked Persian numerals into the English interface.
 */
fun formatDuration(durationMs: Long, persianDigits: Boolean = false): String {
    if (durationMs <= 0L) return "00:00".localizeDigits(persianDigits)

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val text = if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    return text.localizeDigits(persianDigits)
}

/** Formats a file size in MB or GB. Returns an empty string for unknown sizes. */
fun formatSize(sizeBytes: Long, persianDigits: Boolean = false): String {
    if (sizeBytes <= 0L) return ""

    val mb = sizeBytes / 1024.0 / 1024.0
    val text = if (mb >= 1024) {
        String.format(Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }

    return text.localizeDigits(persianDigits)
}

/** Formats a plain integer with the digits of the active interface language. */
fun formatCount(value: Int, persianDigits: Boolean = false): String =
    value.toString().localizeDigits(persianDigits)

/**
 * Converts Latin digits to Persian digits when [persian] is true, and also maps
 * the decimal point to the Persian decimal separator. Returns the receiver
 * untouched otherwise.
 */
fun String.localizeDigits(persian: Boolean): String {
    if (!persian) return this

    return map { ch ->
        when {
            ch in '0'..'9' -> persianDigitChars[ch - '0']
            ch == '.' -> DECIMAL_SEPARATOR_FA
            else -> ch
        }
    }.joinToString("")
}

private val persianDigitChars = charArrayOf(
    '\u06f0', '\u06f1', '\u06f2', '\u06f3', '\u06f4',
    '\u06f5', '\u06f6', '\u06f7', '\u06f8', '\u06f9'
)

fun String.toPersianDigits(): String = map { ch ->
    if (ch in '0'..'9') persianDigitChars[ch - '0'] else ch
}.joinToString("")

fun Int.toPersianDigits(): String = toString().toPersianDigits()

/**
 * Normalises Persian and Arabic digits back to Latin, including the Persian
 * decimal separator and thousands comma, so parsing user input never depends on
 * the interface language.
 */
fun String.toLatinDigits(): String = map { ch ->
    when (ch) {
        in '\u06f0'..'\u06f9' -> '0' + (ch - '\u06f0')
        in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
        DECIMAL_SEPARATOR_FA -> '.'
        '\u060c' -> ','
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
