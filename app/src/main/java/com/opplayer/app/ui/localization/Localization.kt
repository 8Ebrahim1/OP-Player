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

/**
 * Applies the in-app language and layout direction to everything inside
 * [content].
 *
 * The app runs on a plain [androidx.activity.ComponentActivity] and does not
 * depend on AppCompat, so `AppCompatDelegate.setApplicationLocales` is not an
 * option here. Instead the whole Compose tree receives a localized
 * [android.content.Context], a localized [Configuration] and an explicit
 * [LayoutDirection]. Every `stringResource` call, and every `context.getString`
 * call that reads `LocalContext`, then resolves against the chosen locale, and
 * switching the language takes effect immediately without recreating the
 * activity.
 */
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

/**
 * The language and direction picked in the app settings.
 *
 * Needed because a dialog or a bottom sheet lives in its own window with its
 * own `AndroidComposeView`, and Compose re-provides [LocalContext] and
 * [LocalConfiguration] there from that view. The values published by
 * [AppLocalization] on the main window are therefore lost and those surfaces
 * fall back to the device locale. A custom composition local is not re-provided
 * by Compose, so it survives the jump and lets [LocalizedWindow] restore the
 * choice inside the new window.
 */
@Immutable
data class AppLocaleChoice(
    val language: AppLanguage,
    val layoutDirection: AppLayoutDirection
)

val LocalAppLocaleChoice = compositionLocalOf<AppLocaleChoice?> { null }

/**
 * Re-applies the app language inside a dialog or bottom sheet window.
 *
 * Wrap the content of every surface that Compose hosts in a separate window,
 * otherwise its strings follow the device locale instead of the in-app setting.
 * Falls back to plain content when no choice has been published yet, which
 * keeps previews and tests working.
 */
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

/**
 * True when the locale currently applied to the UI is Persian.
 *
 * Number formatting reads this instead of converting unconditionally, otherwise
 * the English interface would still show Persian digits.
 */
@Composable
@ReadOnlyComposable
fun usePersianDigits(): Boolean =
    LocalConfiguration.current.firstLocale().language == PERSIAN_LANGUAGE

/**
 * Serves localized resources while keeping the Activity reachable.
 *
 * `createConfigurationContext` returns a bare context that is no longer part of
 * the wrapper chain leading back to the Activity. Publishing that context as
 * [LocalContext] breaks every lookup that walks the chain: `findActivity()`
 * returns null, and `rememberLauncherForActivityResult` throws
 * "No ActivityResultRegistryOwner was provided". Wrapping the original context
 * instead keeps `getBaseContext()` pointing at the Activity, while resources
 * and assets are answered by the localized context so `stringResource` and
 * `context.getString` still resolve against the chosen language.
 */
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
    // Uses the platform's own script information, so an Arabic or Hebrew system
    // locale is handled correctly too, not just the two languages we ship.
    AppLayoutDirection.AUTO ->
        if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
}
