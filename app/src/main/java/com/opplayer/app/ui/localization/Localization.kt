package com.opplayer.app.ui.localization

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.opplayer.app.data.AppLanguage
import com.opplayer.app.data.AppLayoutDirection
import java.util.Locale

private const val PERSIAN_LANGUAGE = "fa"

@Composable
fun AppLocalization(
    language: AppLanguage,
    layoutDirection: AppLayoutDirection,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val systemLocale = remember(configuration) { configuration.firstLocale() }
    val locale = remember(language, systemLocale) { language.toLocale(systemLocale) }

    val localizedConfiguration = remember(configuration, locale) {
        Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }

    val localizedContext = remember(context, localizedConfiguration) {
        LocalizedContextWrapper(
            base = context,
            localized = context.createConfigurationContext(localizedConfiguration)
        )
    }

    val direction = remember(layoutDirection, locale) {
        layoutDirection.toComposeDirection(locale)
    }

    val choice = remember(language, layoutDirection) {
        AppLocaleChoice(language, layoutDirection)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalLayoutDirection provides direction,
        LocalAppLocaleChoice provides choice,
        content = content
    )
}

@Immutable
data class AppLocaleChoice(
    val language: AppLanguage,
    val layoutDirection: AppLayoutDirection
)

val LocalAppLocaleChoice = compositionLocalOf<AppLocaleChoice?> { null }

@Composable
fun LocalizedWindow(content: @Composable () -> Unit) {
    val choice = LocalAppLocaleChoice.current

    if (choice == null) {
        content()
    } else {
        AppLocalization(
            language = choice.language,
            layoutDirection = choice.layoutDirection,
            content = content
        )
    }
}

@Composable
@ReadOnlyComposable
fun usePersianDigits(): Boolean =
    LocalConfiguration.current.firstLocale().language == PERSIAN_LANGUAGE

private class LocalizedContextWrapper(
    base: Context,
    private val localized: Context
) : ContextWrapper(base) {
    override fun getResources(): Resources = localized.resources

    override fun getAssets(): AssetManager = localized.assets
}

private fun Configuration.firstLocale(): Locale =
    locales.takeIf { !it.isEmpty }?.get(0) ?: Locale.getDefault()

private fun AppLanguage.toLocale(systemLocale: Locale): Locale = when (this) {
    AppLanguage.SYSTEM -> systemLocale
    AppLanguage.PERSIAN -> Locale(PERSIAN_LANGUAGE)
    AppLanguage.ENGLISH -> Locale.ENGLISH
}

private fun AppLayoutDirection.toComposeDirection(locale: Locale): LayoutDirection = when (this) {
    AppLayoutDirection.RTL -> LayoutDirection.Rtl
    AppLayoutDirection.LTR -> LayoutDirection.Ltr

    AppLayoutDirection.AUTO ->
        if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
}
