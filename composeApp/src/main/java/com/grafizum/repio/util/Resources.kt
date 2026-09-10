package com.grafizum.repio.util

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
@Suppress("DiscouragedApi") // Dynamic string keys are an intentional design of this app
fun getString(key: String): String {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else key
}

@Composable
@Suppress("DiscouragedApi") // Dynamic string keys are an intentional design of this app
fun getString(key: String, vararg args: Any): String {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
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
    val resId = context.resources.getIdentifier(key, "plurals", context.packageName)
    return if (resId != 0) context.resources.getQuantityString(resId, count, count) else key
}

@Composable
@Suppress("DiscouragedApi") // Dynamic string keys are an intentional design of this app
fun getQuantityString(key: String, count: Int, vararg args: Any): String {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(key, "plurals", context.packageName)
    return if (resId != 0) context.resources.getQuantityString(resId, count, *args) else key
}
