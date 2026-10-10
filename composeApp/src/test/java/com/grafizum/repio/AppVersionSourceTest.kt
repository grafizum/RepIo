package com.grafizum.repio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

/**
 * THE APP HAS ONE VERSION, AND IT IS `version.properties`.
 *
 * AGENTS.md states the rule plainly: *"`version.properties` at the repo root is the single
 * source of truth for `VERSION_NAME` and `VERSION_CODE`. Never hardcode a version anywhere
 * else."* It was written down because the alternative had already shipped: this suite was
 * added after finding `SupportManager.getAppVersion()` returning a hardcoded `"1.0.0"`, so
 * every support ticket a user sent reported a version that has never existed (the app's
 * first release was 1.4.3). Nothing failed - the field was simply a string, and a string
 * that happens to look like a version compiles exactly like one that does not.
 *
 * A version is the one value in the app that no code path can validate at runtime: the
 * device cannot know what the "right" number is, so a stale literal is invisible until a
 * support report quotes it back. These cases are therefore about the SOURCE, in the same
 * spirit as ThemeRenameTest ("no shipped source file still says azure").
 *
 * The unit tests run with the `composeApp` module as the working directory, so the repo
 * root is one level up.
 */
class AppVersionSourceTest {

    private val repoRoot = File("..")

    private fun versionProperties(): Properties {
        val props = Properties()
        File(repoRoot, "version.properties").inputStream().use { props.load(it) }
        return props
    }

    /** Strips comments so prose that merely mentions a version cannot fail the scan. */
    private fun codeOnly(source: String): String =
        source
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .lineSequence()
            .joinToString("\n") { line ->
                val idx = line.indexOf("//")
                if (idx >= 0) line.substring(0, idx) else line
            }

    /**
     * A version.properties that is missing or malformed must fail HERE rather than at the
     * first release, where the config-time bump writes into it and the bundle claims
     * whatever Play will accept.
     */
    @Test
    fun versionPropertiesIsWellFormedAndIsTheOnlySource() {
        val props = versionProperties()

        val name = props.getProperty("VERSION_NAME")
        assertTrue(
            "version.properties has no VERSION_NAME (it is the single source of truth)",
            name != null,
        )
        assertTrue(
            "VERSION_NAME '$name' is not MAJOR.MINOR.PATCH; the root bumpVersion task " +
                "parses it with this shape",
            Regex("""^\d+\.\d+\.\d+$""").matches(name!!),
        )

        val code = props.getProperty("VERSION_CODE")?.toIntOrNull()
        assertTrue(
            "VERSION_CODE '${props.getProperty("VERSION_CODE")}' is not a positive integer; " +
                "Play rejects a bundle without one",
            code != null && code > 0,
        )
    }

    /**
     * The build has to READ that file rather than restate it. If the android block ever
     * grows a literal versionName again, the APK and the file disagree and the developer
     * tab would show the literal.
     */
    @Test
    fun theBuildDerivesBothVersionFieldsFromTheFile() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(
            "the build no longer reads version.properties",
            gradle.contains("rootProject.file(\"version.properties\")"),
        )
        assertTrue(
            "versionName is not taken from the file's VERSION_NAME",
            gradle.contains("versionName = appVersion"),
        )
        assertTrue(
            "versionCode is not taken from the file's VERSION_CODE",
            gradle.contains("versionCode = playStoreVersion"),
        )
    }

    /**
     * NO SHIPPED SOURCE MAY HARDCODE A VERSION.
     *
     * The scan looks for a semver-shaped string LITERAL in main sources, which is exactly
     * the shape the defect had. It is deliberately narrow - a bare `1.7.5` in a comment or
     * a KDoc is stripped before the scan, because prose about a version is how the rule is
     * explained and must not read as a violation.
     *
     * This is NON-VACUOUS by construction: `SupportManager.getAppVersion()` returned
     * `"1.0.0"` when this case was written, and restoring that literal fails it.
     *
     * ALLOWLIST IS EMPTY ON PURPOSE. There is no legitimate reason for shipped code to
     * name a version: the one place that must is `BuildConfig`, which is generated from
     * version.properties. If a day comes when something genuinely needs a literal, add it
     * here with its reason rather than widening the pattern.
     */
    @Test
    fun noShippedSourceHardcodesAVersion() {
        val versionLiteral = Regex("""["']v?\d+\.\d+\.\d+["']""")
        val offenders = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { versionLiteral.containsMatchIn(codeOnly(it.readText())) }
            .map { it.path }
            .toList()
        assertEquals(
            "these shipped sources hardcode a version instead of reading BuildConfig " +
                "(version.properties is the single source of truth): " +
                offenders.joinToString(),
            emptyList<String>(),
            offenders,
        )
    }

    /**
     * THE DEVELOPER TAB SHOWS THE BUILD'S OWN VERSION.
     *
     * The Creator screen's footer label is what the owner checks a build against, so it has
     * to come from `BuildConfig` - a literal there would be the same defect in the one place
     * a person actually reads the number. Asserted as a source fact because the label is a
     * late-bound string in a Compose tree and no unit test renders the screen.
     */
    @Test
    fun theDeveloperTabPrintsTheBuildsOwnVersion() {
        val screen = File("src/main/java/com/grafizum/repio/ui/screens/EasterEggScreen.kt")
        assertTrue("the Creator/developer screen is gone", screen.exists())
        val code = codeOnly(screen.readText())
        assertTrue(
            "the developer tab no longer prints BuildConfig.VERSION_NAME",
            code.contains("BuildConfig.VERSION_NAME"),
        )
    }
}
