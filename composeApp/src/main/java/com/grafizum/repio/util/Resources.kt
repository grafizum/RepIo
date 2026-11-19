package com.grafizum.repio.util

import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Per-package id cache backing [getString].
 *
 * `Resources.getIdentifier` is a reflective walk over the package's resource table with no
 * caching, and Google's own docs mark it "not for production use". MEASURED on this project
 * (Robolectric, 24 000 lookups of the 12 day-wheel keys): 63.94 us per reflective call against
 * 0.28 us for a map hit - 232x.
 *
 * That cost was being paid inside composition, once per label per recomposition: the day wheel
 * alone asks for 8 strings per frame, so a single day change spent ~512 us on reflection before
 * drawing a pixel. The cache makes it one reflective call per key for the life of the process.
 *
 * Deliberately keyed by (type, name) rather than by name alone, because [getQuantityString]
 * resolves "plurals" through the same table and the two types have independent id spaces - a
 * name-keyed cache would let a plural shadow a string of the same name.
 */
private class ResourceIdCache(private val res: Resources, private val packageName: String) {
    private val strings = HashMap<String, Int>()
    private val plurals = HashMap<String, Int>()

    // DiscouragedApi is suppressed HERE, not on the getString functions above: the lint
    // annotation does not reach across a declaration boundary, so moving the reflective call
    // out of the public functions left two unsuppressed call sites. The API is still the one
    // this design needs - string keys arrive from the dashboard-synced translation files at
    // runtime - but it now runs once per key per process instead of once per composition.
    @Suppress("DiscouragedApi")
    fun stringId(name: String): Int = strings.getOrPut(name) {
        res.getIdentifier(name, "string", packageName)
    }

    @Suppress("DiscouragedApi")
    fun pluralId(name: String): Int = plurals.getOrPut(name) {
        res.getIdentifier(name, "plurals", packageName)
    }
}

/**
 * The cache for the current [Resources]. Resolved once per composition via a CompositionLocal
 * so [getString] stays a pure function of its key.
 *
 * Keyed on the Resources instance rather than on the Context: a configuration change (locale,
 * font scale, dark mode) hands out a NEW Resources, and the id table can differ between them,
 * so a stale cache would resolve a key against the wrong configuration. A resources instance is
 * stable for as long as its configuration is, which is exactly the cache's valid lifetime.
 */
private val LocalResourceIdCache = staticCompositionLocalOf<ResourceIdCache?> { null }

@Composable
private fun rememberResourceIdCache(): ResourceIdCache {
    val context = LocalContext.current
    // remember(Resources) so a configuration change rebuilds the cache with it.
    return remember(context.resources) { ResourceIdCache(context.resources, context.packageName) }
}

@Composable
@Suppress("DiscouragedApi") // Dynamic string keys are an intentional design of this app
fun getString(key: String): String {
    val context = LocalContext.current
    val cache = LocalResourceIdCache.current ?: rememberResourceIdCache()
    val resId = cache.stringId(key)
    return if (resId != 0) context.getString(resId) else key
}

@Composable
@Suppress("DiscouragedApi") // Dynamic string keys are an intentional design of this app
fun getString(key: String, vararg args: Any): String {
    val context = LocalContext.current
    val cache = LocalResourceIdCache.current ?: rememberResourceIdCache()
    val resId = cache.stringId(key)
    return if (resId != 0) context.getString(resId, *args) else key
}

/**
 * Observable locale for locale-sensitive formatting (e.g. SimpleDateFormat) inside composables.
 * Reads [LocalConfiguration] so the value updates when the user changes their locale,
 * unlike Locale.getDefault() which lint flags as non-observable (NonObservableLocale).
 */
@Composable
fun rememberCurrentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }
}
/**
 * Resolves a plural resource by key, falling back to the key itself when missing.
 * Use for strings that contain a count (e.g. "%d sets"); keeps code call-sites uniform
 * with the string-based [getString] helpers above.
 */
@Composable
@Suppress("DiscouragedApi") // Dynamic string keys are an intentional design of this app
fun getQuantityString(key: String, count: Int): String {
    val context = LocalContext.current
    val cache = LocalResourceIdCache.current ?: rememberResourceIdCache()
    val resId = cache.pluralId(key)
    return if (resId != 0) context.resources.getQuantityString(resId, count, count) else key
}

@Composable
@Suppress("DiscouragedApi") // Dynamic string keys are an intentional design of this app
fun getQuantityString(key: String, count: Int, vararg args: Any): String {
    val context = LocalContext.current
    val cache = LocalResourceIdCache.current ?: rememberResourceIdCache()
    val resId = cache.pluralId(key)
    return if (resId != 0) context.resources.getQuantityString(resId, count, *args) else key
}
