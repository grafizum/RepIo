package com.grafizum.repio.data

import java.util.Locale

/**
 * A localized display name and method description for one exercise in the built-in pool.
 *
 * @property name the exercise's name in this language, shown everywhere in the UI.
 * @property description the "how to perform it" text in this language, shown on the
 *   exercise card, in the add/edit dialog and as the replace-dialog hint.
 */
data class ExerciseText(val name: String, val description: String)

/** Shorthand for the translation tables in `ExerciseTextDe.kt` / `ExerciseTextIt.kt`. */
internal fun ex(name: String, description: String) = ExerciseText(name, description)

/**
 * Localization for the exercise pool.
 *
 * The database stores every pool exercise under its **canonical English name** ("Dumbbell
 * Curls"). That name is the key the equipment table, the primary-muscle table, the logged
 * sets and the templates all agree on, so it must never change: translating at *storage*
 * time would orphan every set a German user has already logged, and would silently split
 * "Dumbbell Curls" and "Kurzhantel-Curls" into two exercises.
 *
 * Instead the translations live in the tables referenced here and are applied at *display*
 * time, keyed by a slug derived from the canonical name (`dumbbell_curls`). One slug is the
 * join key between three things:
 *
 *  - the canonical English name (which the app already knows),
 *  - the localized name the user reads and types, and
 *  - the localized method description.
 *
 * That is also what makes the search work in the user's own language: [allNamesFor] returns
 * every name an exercise is known by, so typing "Kurzhantel-Curls" finds `Dumbbell Curls`,
 * which is still what gets stored. [canonicalFor] is the reverse lookup used to normalize
 * whatever the user typed (or picked) back to the canonical name.
 *
 * A locale with no table falls through to English untouched.
 */
object ExerciseLocalization {

    /** The canonical, untranslated language: the names the database stores. */
    const val ENGLISH = "en"

    /** Every language that has a translation table, keyed by ISO 639-1 code. */
    private val TABLES: Map<String, Map<String, ExerciseText>> = mapOf(
        "de" to EXERCISE_TEXT_DE,
        "it" to EXERCISE_TEXT_IT
    )

    /** The languages a table exists for (excluding [ENGLISH], which needs none). */
    val translatedLanguages: List<String> = TABLES.keys.sorted()

    /** Maps a configuration locale onto a table key, defaulting to [ENGLISH]. */
    fun languageOf(locale: Locale): String =
        when (locale.language.lowercase(Locale.ROOT)) {
            "de" -> "de"
            "it" -> "it"
            else -> ENGLISH
        }

    /**
     * Deterministic slug for an exercise name: ASCII letters and digits are kept, every
     * other run of characters collapses to a single underscore. So "Barbell Bench Presses"
     * becomes `barbell_bench_presses` and "Farmer's Walks" becomes `farmer_s_walks`.
     *
     * Deliberately ASCII-only (not `Char.isLetterOrDigit`), so an accented or CJK name
     * produces the same slug here as in the tooling that generated `tools/exercise-i18n`,
     * and a name it cannot represent simply resolves to no translation instead of to a
     * different entry.
     */
    fun slugify(name: String): String {
        val out = StringBuilder(name.length)
        for (ch in name.lowercase(Locale.ROOT)) {
            when {
                ch in 'a'..'z' || ch in '0'..'9' -> out.append(ch)
                out.isNotEmpty() && out.last() != '_' -> out.append('_')
            }
        }
        while (out.isNotEmpty() && out.last() == '_') out.deleteCharAt(out.length - 1)
        return out.toString()
    }

    /** The translated name + description for [canonicalName], or null when there is none. */
    fun textFor(canonicalName: String, language: String): ExerciseText? =
        textForSlug(slugify(canonicalName), language)

    /** The translated text stored under a slug, or null. */
    fun textForSlug(slug: String, language: String): ExerciseText? = TABLES[language]?.get(slug)

    /**
     * Every slug [language] has a translation for. Exposed so the coverage test (and any
     * future tooling) can compare a table against the pool instead of trusting it.
     */
    fun translatedSlugs(language: String): Set<String> = TABLES[language]?.keys ?: emptySet()

    /** The name shown to the user in [language], falling back to the canonical English one. */
    fun displayName(canonicalName: String, language: String): String =
        textFor(canonicalName, language)?.name ?: canonicalName

    /**
     * The localized method description, or null when this language has none for the
     * exercise — the caller then keeps the English description rather than showing nothing.
     */
    fun description(canonicalName: String, language: String): String? =
        textFor(canonicalName, language)?.description

    /**
     * Every name this exercise is known by — canonical English first, then one per
     * translation — so the search can match in any of the app's languages at once. A German
     * user who leaves the app in English can still type "Kurzhantel" and find the exercise.
     */
    fun allNamesFor(canonicalName: String): List<String> {
        val slug = slugify(canonicalName)
        if (slug.isEmpty()) return listOf(canonicalName)
        return listOf(canonicalName) + TABLES.values.mapNotNull { it[slug]?.name }
    }

    /**
     * Resolves any name — canonical English, a translated name, or whatever the user typed
     * on a previous build — back to the canonical English name the database stores. Returns
     * null for names outside the pool (custom exercises), which the caller leaves as typed.
     */
    fun canonicalFor(input: String): String? {
        val slug = slugify(input)
        if (slug.isEmpty()) return null
        return index[slug]
    }

    /**
     * Reverse index from every known name's slug to the canonical exercise name.
     *
     * Canonical names are inserted first and translations only fill the gaps, so a
     * translated name that happens to share a slug with *some* pool exercise can never
     * re-point that slug at a different exercise.
     */
    private val index: Map<String, String> by lazy {
        val pool = WorkoutTemplates.getAllExerciseNames()
        val map = HashMap<String, String>(pool.size * 3)
        for (canonical in pool) {
            val slug = slugify(canonical)
            if (slug.isNotEmpty()) map[slug] = canonical
        }
        for (canonical in pool) {
            val slug = slugify(canonical)
            if (slug.isEmpty()) continue
            for (table in TABLES.values) {
                val localized = table[slug]?.name ?: continue
                val localizedSlug = slugify(localized)
                // putIfAbsent needs API 24; minSdk is 23.
                if (localizedSlug.isNotEmpty() && localizedSlug !in map) map[localizedSlug] = canonical
            }
        }
        map
    }
}
