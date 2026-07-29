package com.opplayer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OpBackground = Color(0xFF090B13)
val OpSurface = Color(0xFF111420)
val OpGlass = Color(0xFF182033)
val OpPrimary = Color(0xFF83E7FF)
val OpSecondary = Color(0xFFA8B6FF)
val OpTertiary = Color(0xFFCFB5FF)
val OpAccentPink = Color(0xFFFF7E9C)

private val OpColorScheme = darkColorScheme(
    primary = OpPrimary,
    onPrimary = Color(0xFF001F27),
    secondary = OpSecondary,
    onSecondary = Color(0xFF0A0F1E),
    tertiary = OpTertiary,
    background = OpBackground,
    onBackground = Color(0xFFE7EAF5),
    surface = OpSurface,
    onSurface = Color(0xFFE7EAF5),
    surfaceVariant = Color(0xFF1A2032),
    onSurfaceVariant = Color(0xFFB6BFD6),
    outline = Color(0xFF2A3350),
    error = Color(0xFFFF6B7A)
)

@Composable
fun OPPlayerTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OpColorScheme,
        typography = OpTypography,
        content = content
    )
}
