package com.opplayer.app.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opplayer.app.R
import com.opplayer.app.data.AppLanguage
import com.opplayer.app.data.AppLayoutDirection
import com.opplayer.app.data.AppSettings
import com.opplayer.app.data.SubtitleStyleSettings
import com.opplayer.app.ui.components.GlassBackground
import com.opplayer.app.ui.components.SubtitleOverlay
import com.opplayer.app.ui.components.backgroundColorName
import com.opplayer.app.ui.components.textColorName
import com.opplayer.app.ui.localization.usePersianDigits
import com.opplayer.app.util.formatCount
import kotlinx.coroutines.launch

private const val PAGE_WELCOME = 0
private const val PAGE_LANGUAGE = 1
private const val PAGE_DIRECTION = 2
private const val PAGE_LIBRARY = 3
private const val PAGE_DEVICE = 4
private const val PAGE_PLAYER = 5
private const val PAGE_SUBTITLE_COLOR = 6
private const val PAGE_SUBTITLE_BACKGROUND = 7
private const val PAGE_READY = 8
private const val PAGE_COUNT = 9

/**
 * First run tour.
 *
 * The tour is not a passive slideshow: the language, the layout direction and
 * the subtitle colours are chosen on the slide that introduces them, and every
 * choice is applied immediately, so the following slides are already shown the
 * way the user asked for.
 */
@Composable
fun OnboardingScreen(
    settings: AppSettings,
    subtitleStyle: SubtitleStyleSettings,
    onLanguageChange: (AppLanguage) -> Unit,
    onLayoutDirectionChange: (AppLayoutDirection) -> Unit,
    onSubtitleTextColorChange: (Long) -> Unit,
    onSubtitleBackgroundChange: (Long) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val persianDigits = usePersianDigits()

    Box(modifier = modifier.fillMaxSize()) {
        GlassBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.onboarding_step,
                        formatCount(pagerState.currentPage + 1, persianDigits),
                        formatCount(PAGE_COUNT, persianDigits)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onFinish) {
                    Text(text = stringResource(R.string.onboarding_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        PAGE_WELCOME -> SlideHeader(
                            icon = Icons.Default.PlayArrow,
                            title = stringResource(R.string.onboarding_welcome_title),
                            body = stringResource(R.string.onboarding_welcome_body)
                        )

                        PAGE_LANGUAGE -> {
                            SlideHeader(
                                icon = Icons.Default.Language,
                                title = stringResource(R.string.onboarding_language_title),
                                body = stringResource(R.string.onboarding_language_body)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            ChoiceGroup(
                                options = listOf(
                                    AppLanguage.SYSTEM to stringResource(R.string.language_system),
                                    AppLanguage.PERSIAN to stringResource(R.string.language_persian),
                                    AppLanguage.ENGLISH to stringResource(R.string.language_english)
                                ),
                                selected = settings.language,
                                onSelect = onLanguageChange
                            )
                        }

                        PAGE_DIRECTION -> {
                            SlideHeader(
                                icon = Icons.Default.SwapHoriz,
                                title = stringResource(R.string.onboarding_direction_title),
                                body = stringResource(R.string.onboarding_direction_body)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            ChoiceGroup(
                                options = listOf(
                                    AppLayoutDirection.AUTO to stringResource(R.string.direction_auto),
                                    AppLayoutDirection.RTL to stringResource(R.string.direction_rtl),
                                    AppLayoutDirection.LTR to stringResource(R.string.direction_ltr)
                                ),
                                selected = settings.layoutDirection,
                                onSelect = onLayoutDirectionChange
                            )
                        }

                        PAGE_LIBRARY -> SlideHeader(
                            icon = Icons.Default.Link,
                            title = stringResource(R.string.onboarding_library_title),
                            body = stringResource(R.string.onboarding_library_body)
                        )

                        PAGE_DEVICE -> SlideHeader(
                            icon = Icons.Default.Smartphone,
                            title = stringResource(R.string.onboarding_device_title),
                            body = stringResource(R.string.onboarding_device_body)
                        )

                        PAGE_PLAYER -> SlideHeader(
                            icon = Icons.Default.Subtitles,
                            title = stringResource(R.string.onboarding_player_title),
                            body = stringResource(R.string.onboarding_player_body)
                        )

                        PAGE_SUBTITLE_COLOR -> {
                            SlideHeader(
                                icon = Icons.Default.Palette,
                                title = stringResource(R.string.onboarding_subtitle_color_title),
                                body = stringResource(R.string.onboarding_subtitle_color_body)
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            SubtitlePreview(subtitleStyle)
                            Spacer(modifier = Modifier.height(18.dp))

                            ColorChoiceRow(
                                colors = SubtitleStyleSettings.TEXT_COLORS,
                                selected = subtitleStyle.textColorArgb,
                                onSelect = onSubtitleTextColorChange,
                                nameOf = { textColorName(it) }
                            )
                        }

                        PAGE_SUBTITLE_BACKGROUND -> {
                            SlideHeader(
                                icon = Icons.Default.Style,
                                title = stringResource(R.string.onboarding_subtitle_background_title),
                                body = stringResource(R.string.onboarding_subtitle_background_body)
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            SubtitlePreview(subtitleStyle)
                            Spacer(modifier = Modifier.height(18.dp))

                            ColorChoiceRow(
                                colors = SubtitleStyleSettings.BACKGROUND_COLORS,
                                selected = subtitleStyle.backgroundArgb,
                                onSelect = onSubtitleBackgroundChange,
                                nameOf = { backgroundColorName(it) }
                            )
                        }

                        else -> SlideHeader(
                            icon = Icons.Default.TaskAlt,
                            title = stringResource(R.string.onboarding_ready_title),
                            body = stringResource(R.string.onboarding_ready_body)
                        )
                    }
                }
            }

            PageIndicator(
                count = PAGE_COUNT,
                current = pagerState.currentPage,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > PAGE_WELCOME) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.onboarding_previous))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (pagerState.currentPage >= PAGE_READY) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (pagerState.currentPage >= PAGE_READY) {
                            stringResource(R.string.onboarding_finish)
                        } else {
                            stringResource(R.string.onboarding_next)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideHeader(
    icon: ImageVector,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Live preview so a colour choice can be judged before it is confirmed. */
@Composable
private fun SubtitlePreview(settings: SubtitleStyleSettings) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Color(0xFF101014), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        SubtitleOverlay(
            text = stringResource(R.string.subtitle_preview_text),
            settings = settings.copy(enabled = true, bottomMarginDp = 0f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun <T> ChoiceGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { (value, label) ->
            val isSelected = value == selected

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ColorChoiceRow(
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit,
    nameOf: @Composable (Long) -> String
) {
    val selectedLabel = stringResource(R.string.color_selected)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { value ->
            val isSelected = value == selected
            val name = nameOf(value)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(value.toInt()), CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) }
                    )
                    .semantics {
                        contentDescription = name
                        if (isSelected) {
                            stateDescription = selectedLabel
                        }
                    }
            )
        }
    }
}

@Composable
private fun PageIndicator(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateFloatAsState(
                targetValue = if (active) 22f else 8f,
                label = "indicatorWidth"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(8.dp)
                    .width(width.dp)
                    .background(
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}
