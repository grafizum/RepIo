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
}
