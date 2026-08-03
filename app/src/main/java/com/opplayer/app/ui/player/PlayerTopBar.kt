package com.opplayer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.opplayer.app.R

@Composable
fun PlayerTopBar(
    title: String,
    isFullscreen: Boolean,
    showEpisodeButton: Boolean,
    isResolvingEpisode: Boolean,
    onBack: () -> Unit,
    onNextEpisode: () -> Unit,
    onCycleScale: () -> Unit,
    onPip: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSubtitles: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White
            )
        }

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        if (showEpisodeButton) {
            if (isResolvingEpisode) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onNextEpisode) {
                    Icon(

                        imageVector = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                            Icons.Filled.KeyboardDoubleArrowLeft
                        } else {
                            Icons.Filled.KeyboardDoubleArrowRight
                        },
                        contentDescription = stringResource(R.string.next_episode),
                        tint = Color.White
                    )
                }
            }
        }

        IconButton(onClick = onCycleScale) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = stringResource(R.string.resize_mode),
                tint = Color.White
            )
        }

        IconButton(onClick = onPip) {
            Icon(
                imageVector = Icons.Default.PictureInPictureAlt,
                contentDescription = stringResource(R.string.pip),
                tint = Color.White
            )
        }

        IconButton(onClick = onToggleFullscreen) {
            Icon(
                imageVector = if (isFullscreen) {
                    Icons.Default.FullscreenExit
                } else {
                    Icons.Default.Fullscreen
                },
                contentDescription = if (isFullscreen) {
                    stringResource(R.string.exit_fullscreen)
                } else {
                    stringResource(R.string.fullscreen)
                },
                tint = Color.White
            )
        }

        IconButton(onClick = onSubtitles) {
            Icon(
                imageVector = Icons.Default.Subtitles,
                contentDescription = stringResource(R.string.subtitle_settings),
                tint = Color.White
            )
        }

        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = Color.White
            )
        }

        IconButton(onClick = onHelp) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = stringResource(R.string.help),
                tint = Color.White
            )
        }
    }
}
