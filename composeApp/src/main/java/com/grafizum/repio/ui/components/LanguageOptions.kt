package com.grafizum.repio.ui.components

import androidx.compose.runtime.Composable
import com.grafizum.repio.util.getString

/**
 * The languages the app ships, as (preference value, localized label) pairs.
 *
 * Defined once so every surface that offers a language choice shows the same set
 * with the same labels — and, more importantly, so the `"system"` sentinel keeps
 * one meaning. MainActivity maps it to an empty [androidx.core.os.LocaleListCompat],
 * which is how AppCompat is told "follow the device"; spelling it differently in
 * one place would quietly pin the app to a language.
 */
@Composable
fun appLanguageOptions(): List<Pair<String, String>> = listOf(
    "system" to getString("system_default"),
    "en" to getString("english"),
    "de" to getString("german"),
    "it" to getString("italian")
)

/**
 * The compact tag for the current language, for controls too small to carry a
 * full label: an ISO code the user recognises ("EN", "DE", "IT").
 *
 * Codes are not translated — they are the language's own name in two letters — so
 * only `"system"`, which has no code, needs copy.
 */
@Composable
fun appLanguageShortTag(languageCode: String): String =
    if (languageCode == APP_LANGUAGE_SYSTEM) getString("language_auto_short")
    else languageCode.uppercase()

/** The preference value meaning "follow the device locale". */
const val APP_LANGUAGE_SYSTEM = "system"
