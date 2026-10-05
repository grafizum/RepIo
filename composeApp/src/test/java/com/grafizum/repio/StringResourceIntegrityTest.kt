package com.grafizum.repio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the string resources against the two failure modes that have actually shipped:
 *
 *  1. **A key that was never defined.** `util/Resources.kt` resolves string keys through
 *     `getIdentifier` and returns *the key itself* when the lookup misses, so a typo or a
 *     never-added resource does not crash — it quietly paints `incomplete_workout_routine`
 *     on screen, or makes TalkBack read a raw key. Three `ex_*` strings shipped that way, and
 *     so did `incomplete_workout_routine`, `incomplete_workout_desc` and
 *     `rank_badge_content_desc`. Nothing caught them.
 *
 *  2. **A translation that aapt2 silently dropped.** 53 German and 53 Italian entries were
 *     parked inside XML comment blocks, which *looks* present in a diff but is not compiled,
 *     so the whole level system fell back to English. This is only visible on a device, which
 *     is how it survived. Comparing key sets per locale turns it into a build failure.
 *
 *  3. **A translation that is a copy of the English.** See
 *     [noLocaleKeepsTheEnglishTextByAccident] below - this one defeated check 2, the
 *     placeholder check and key parity all at once.
 *
 * The locale comparison works because the keys are read from the source XML with comments
 * stripped first — a commented-out `<string>` must count as *missing*, which is the entire
 * point of test 2.
 */
class StringResourceIntegrityTest {

    private val resDir = locateResDir()

    /** Every `<string>`/`<plurals>` name in a values dir, with XML comments removed. */
    private fun keysIn(dir: String): Map<String, Set<String>> {
        val file = File(resDir, "$dir/strings.xml")
        assertTrue("missing $file", file.isFile)
        val body = stripXmlComments(file.readText())
        val strings = Regex("""<string\s+name="([^"]+)"""").findAll(body).map { it.groupValues[1] }.toSet()
        val plurals = Regex("""<plurals\s+name="([^"]+)"""").findAll(body).map { it.groupValues[1] }.toSet()
        return mapOf("string" to strings, "plurals" to plurals)
    }

    /** Values inside an XML comment are not compiled, so they must not count as present. */
    private fun stripXmlComments(xml: String): String =
        Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL).replace(xml, "")

    private val kotlinSources: List<File> =
        File(resDir, "../java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /**
     * A call to the app's own `getString`/`getQuantityString` helpers, keyed by resource type.
     * The lookbehind keeps `prefs.getString("setup_days", …)` out — that is SharedPreferences
     * with a *default value*, not a resource lookup, and its keys must not be in strings.xml.
     *
     * Only keys shaped like a resource identifier are collected. A literal that interpolates
     * (`getString("intensity_${level.lowercase()}")`) builds its key at runtime, so the source
     * shows a prefix, not a resource; those are a different (also worth reviewing) concern and
     * are skipped here rather than reported as missing.
     */
    private fun literalKeyCalls(type: String): Map<String, List<String>> {
        val pattern = Regex("""(?<![.\w])get${type}String\(\s*"([^"]+)"[^)]*""")
        val identifier = Regex("""^[a-z][a-z0-9_]*$""")
        val hits = LinkedHashMap<String, MutableList<String>>()
        for (file in kotlinSources) {
            for (match in pattern.findAll(file.readText())) {
                val key = match.groupValues[1] ?: continue
                if (!identifier.matches(key)) continue
                hits.getOrPut(key) { mutableListOf() } += file.name
            }
        }
        return hits
    }

    private fun locateResDir(): File {
        val relative = "src/main/res"
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (File(candidate, "values/strings.xml").isFile) return candidate
            dir = dir.parentFile
        }
        throw AssertionError("could not locate $relative from ${System.getProperty("user.dir")}")
    }

    @Test
    fun everyStringKeyUsedInCodeIsDefined() {
        val defined = keysIn("values")["string"].orEmpty()
        val missing = literalKeyCalls("").keys - defined
        assertEquals(
            "these keys are passed to getString() but defined in no strings.xml, so the " +
                "app renders the key itself (add the resource):",
            emptySet<String>(),
            missing
        )
    }

    @Test
    fun everyPluralKeyUsedInCodeIsDefined() {
        val defined = keysIn("values")["plurals"].orEmpty()
        val missing = literalKeyCalls("Quantity").keys - defined
        assertEquals(
            "these keys are passed to getQuantityString() but defined in no <plurals> " +
                "resource, so the app renders the key itself:",
            emptySet<String>(),
            missing
        )
    }

    /**
     * A format string used with arguments must actually contain a placeholder. Without one
     * the arguments are silently dropped and the sentence reads as if nothing was missing.
     */
    @Test
    fun everyStringFormattedAtItsCallSiteHasAPlaceholder() {
        val body = stripXmlComments(File(resDir, "values/strings.xml").readText())
        val values = Regex("""<string\s+name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(body).associate { it.groupValues[1] to it.groupValues[2] }

        val pattern = Regex("""(?<![.\w])getString\(\s*"([^"]+)"\s*,""")
        // A full Java format specifier: optional argument index, flags, width, precision,
        // then the conversion. Covers %s, %d, %.1f and %1$.1f alike.
        val placeholder = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z]""")
        val offenders = pattern.findAll(kotlinSources.joinToString("\n") { it.readText() })
            .mapNotNull { it.groupValues[1] }
            .filter { key ->
                val value = values[key] ?: return@filter false
                !placeholder.containsMatchIn(value)
            }
            .distinct()
            .toSet()
        assertEquals(
            "these strings are formatted at their call site but declare no %1\$s placeholder, " +
                "so the substituted value is dropped:",
            emptySet<String>(),
            offenders
        )
    }

    /**
     * Locale parity. Android falls back to the base file, so a missing translation is not a
     * crash — it is an English sentence in a German app, which is exactly the bug that is
     * only visible on a device. Every locale must carry every key.
     */
    @Test
    fun everyLocaleTranslatesEveryKey() {
        val base = keysIn("values")
        for (localeDir in listOf("values-de", "values-it")) {
            val locale = keysIn(localeDir)
            for (type in listOf("string", "plurals")) {
                val missing = base[type].orEmpty() - locale[type].orEmpty()
                assertEquals(
                    "[$localeDir] these <$type> keys have no translation, so a $localeDir " +
                        "user sees the English text:",
                    emptySet<String>(),
                    missing
                )
                val extra = locale[type].orEmpty() - base[type].orEmpty()
                assertEquals(
                    "[$localeDir] these <$type> keys are translated but not in the base file, " +
                        "so they can never be shown:",
                    emptySet<String>(),
                    extra
                )
            }
        }
    }

    @Test
    fun theResourcesAreNotEmpty() {
        assertTrue("no <string> resources found", keysIn("values")["string"].orEmpty().size > 500)
    }

    /**
     * THE THIRD FAILURE MODE, AND THE ONE THAT SURVIVED THE OTHER TWO: a key that IS
     * defined in every locale but whose translation is a byte-for-byte copy of the
     * English. Every check above is satisfied by that — the key exists, it is not
     * inside a comment, it has its placeholder — and the result is an English
     * sentence in a German app, which is only visible on a device.
     *
     * The owner reported exactly this on the level system: the whole Overall Level
     * and rank block (44 German, 43 Italian keys) was present in all three files and
     * green in the build, while rendering in English. The level guide, the rank
     * tooltips, all eight rank descriptions and every "how your Overall Level works"
     * step had been copied across verbatim. Nothing in the build could see it,
     * because a copy and a translation are the same file content to every check
     * that compares key SETS.
     *
     * So this compares VALUES. Anything identical to the base text fails the build
     * unless it is in [IDENTICAL_BY_DESIGN], which is a list, not a suppression
     * switch: brand name, units, abbreviations and loanwords that the language
     * genuinely uses unchanged. A key belongs there only with a reason, and moving
     * one out of it means translating the string - not editing the list.
     */
    @Test
    fun noLocaleKeepsTheEnglishTextByAccident() {
        val base = valuesIn("values")
        for (localeDir in listOf("values-de", "values-it")) {
            val locale = valuesIn(localeDir)
            val offenders = (base.keys intersect locale.keys)
                .filter { base[it] == locale[it] }
                .filterNot { it in IDENTICAL_BY_DESIGN }
                .sorted()
            assertEquals(
                "[$localeDir] these keys carry the ENGLISH text verbatim, so the app " +
                    "renders English in a translated UI. Translate them, or move the " +
                    "key to IDENTICAL_BY_DESIGN if the language really uses the same " +
                    "word:",
                emptyList<String>(),
                offenders
            )
        }
    }

    /** Key -> value, comments stripped and inner whitespace collapsed. */
    private fun valuesIn(dir: String): Map<String, String> {
        val file = File(resDir, "$dir/strings.xml")
        assertTrue("missing $file", file.isFile)
        val body = stripXmlComments(file.readText())
        val pattern = Regex("""<(string|plurals)\s+name="([^"]+)"[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
        return pattern.findAll(body).associate { match ->
            match.groupValues[2] to Regex("""\s+""").replace(match.groupValues[3], " ").trim()
        }
    }

    private companion object {
        /**
         * Strings a German or Italian UI legitimately spells exactly like English.
         * Grouped by why, because "identical" is only allowed with a reason.
         */
        val IDENTICAL_BY_DESIGN: Set<String> = setOf(
            // Brand and universal UI tokens.
            "app_name", "ok", "language_auto_short", "editor", "input_demo", "start", "no",

            // Resistance bands are called "Band" in German and "pin" is a loanword
            // in both Italian and English gym talk, so these are unchanged.
            "load_unit_band", "load_unit_pin", "progression_load_pin",

            // Pure formats: no word in them at all.
            "day_title_format", "overall_level_format", "overall_level_none",
            "overall_level_weight_multiplier", "rank_range", "rank_range_open",
            "duration_minutes",

            // Units, where the symbol is the unit in every language.
            "weight_kg_intl", "progression_load_kg", "progression_duration_value",
            "recap_volume_kg",

            // The session recap's two pure formats. "kg" is the symbol in all
            // three, and "min" is the abbreviation Italian prints anyway - which
            // is exactly why the German one differs ("%1$d Min.") and its
            // absence here is evidence the block was translated, not copied.
            "recap_duration_format",

            // "Volume" is the Italian word for training volume, not a leftover:
            // Italian lifters say "volume" the way they say "scarico" for
            // unloading. The German entry is "Volumen", which is the different
            // word that check would otherwise force into the Italian file.
            "recap_volume",

            // Training vocabulary the German and Italian gym scenes use in English:
            // RPE/RIR are abbreviations, "Tempo", "Standard", "Kettlebell", and the
            // exercise names every gym in those countries calls by the English word.
            "rir_1", "rir_2", "rpe_rir_faq", "tempo_label", "duration_standard_label",
            "equipment_kettlebell", "equipment_tag_kettlebell", "gym_equipment_kettlebell",
            "goal_calisthenics", "goal_powerbuilding", "goal_crossfit", "split_style_c_title",
            "core", "ex_core", "ex_curls", "ex_crunches", "ex_plank", "ex_shrugs", "ex_dips",
            "lats_part",

            // "Smith machine" is the name of the apparatus in every language's
            // gym; German uses "Smith-Maschine" (hyphenated, translated) and
            // Italian uses the English words verbatim, as it does for Lat
            // machine and Leg press.
            "equipment_tag_smith",

            // "Level" is the word German lifters use, including this app's own guide.
            // The editor's own level-row heading joins them; Italian does translate
            // it ("Livello"), and the list is per KEY so it applies to both.
            "level_word_n", "adjust_levels_n", "intensity_level_value", "level_editor_title",

            // Weekday and split abbreviations: the two-letter forms are the local
            // convention in both languages, and PPL is the acronym everywhere.
            "monday_shortest", "monday_short", "friday_shortest", "saturday_shortest",
            "ppl_6day_name",

            // Italian keeps the English session names its own prose already uses
            // ("Giorni full body ..."), and Push/Pull are the standard PPL words.
            "smart_workout_assistant", "full_body_a_title", "full_body_b_title",
            "full_body_c_title", "session_push", "session_pull"
        )
    }
}
