package com.opplayer.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.opplayer.app.R

/*
 * Colour swatches are drawn as bare circles. Without a name they are announced
 * as an unlabeled control, so every offered value is mapped to a translated
 * name here and exposed through the semantics of the swatch.
 */

@StringRes
private fun textColorNameRes(argb: Long): Int = when (argb) {
    0xFFFFFFFFL -> R.string.color_white
    0xFFFFEB3BL -> R.string.color_yellow
    0xFF00E5FFL -> R.string.color_cyan
    0xFF69F0AEL -> R.string.color_green
    0xFFFFAB40L -> R.string.color_orange
    0xFFFF80ABL -> R.string.color_pink
    0xFF000000L -> R.string.color_black
    else -> R.string.subtitle_text_color
}

@StringRes
private fun backgroundColorNameRes(argb: Long): Int = when (argb) {
    0x00000000L -> R.string.color_transparent
    0x59000000L -> R.string.color_black_light
    0x99000000L -> R.string.color_black_medium
    0xE0000000L -> R.string.color_black_strong
    0xB3FFFFFFL -> R.string.color_white_box
    else -> R.string.subtitle_background
}

/** Translated name of a subtitle text colour. */
@Composable
@ReadOnlyComposable
fun textColorName(argb: Long): String = stringResource(textColorNameRes(argb))

/** Translated name of a subtitle background colour. */
@Composable
@ReadOnlyComposable
fun backgroundColorName(argb: Long): String = stringResource(backgroundColorNameRes(argb))
