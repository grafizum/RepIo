package com.grafizum.repio

import com.grafizum.repio.ui.theme.isRetiredThemeOption
import com.grafizum.repio.ui.theme.retiredThemeOptions
import com.grafizum.repio.ui.theme.resolveColorScheme
import com.grafizum.repio.ui.theme.migrateThemeOption
import com.grafizum.repio.util.IconSwitchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The AZURE retirement failsafe (2026-10-02).
 *
 * A theme id is a PERSISTED STRING (`theme_option`) and its launcher activity is a
 * PERSISTED COMPONENT STATE. Retiring `AZURE` therefore leaves two broken things
 * on the device: the stored id resolves to nothing, AND `MainActivityAzure` - which
 * was the ENABLED component - no longer exists, so no launcher activity resolves at
 * all.
 *
 * The owner's instruction was to send those users back to the DEFAULT theme. That
 * is not the same as translating `AZURE` to its successor `ROYAL_BLUE`, and the
 * difference is the whole point of these cases: a translation would keep the user
 * on a theme they never picked, and - being a pure preference rewrite - would do
 * nothing at all about the missing icon. A retired id must resolve to *no stored
 * choice* so it re-enters the fresh-install path, which is the only path that both
 * picks the system default and calls `switchIcon`.
 */
class ThemeRenameTest {

    @Test
    fun `a retired id resolves to the system default, not to a surviving theme`() {
        // The owner's ask: "revert to the default theme". The default is whatever a
        // fresh install gets - which since 2026-10-04 is Beige in BOTH system
        // appearances, so this no longer has to reason about the appearance at all.
        // It still sweeps both values rather than deleting the loop, because the
        // retired-id path calls the same function with whichever appearance the
        // device happens to be in, and both must land on the fresh-install default.
        // The owner's ask: "revert to the default theme". The default is whatever a
        // fresh install gets - which since 2026-10-04 is Beige in BOTH system
        // appearances, so this no longer has to reason about the appearance at all.
        // It still sweeps both values rather than deleting the loop, because the
        // retired-id path calls the same function with whichever appearance the
        // device happens to be in, and both must land on the fresh-install default.
        listOf(false, true).forEach { isDark ->
            val expected = IconSwitchHelper.defaultThemeFor(isDark)
            assertEquals(
                "retired id must land on the fresh-install default (isDark=$isDark)",
                resolveColorScheme(expected, isDark),
                resolveColorScheme("AZURE", isDark)
            )
        }
    }

    @Test
    fun `a retired id does NOT resolve to the theme that replaced it`() {
        // The tempting implementation: `mapOf("AZURE" to "ROYAL_BLUE")`. It passes
        // the test above on a light system, because the light default IS Beige and
        // a careless assertion would not notice. This case is what forbids it.
        listOf(false, true).forEach { isDark ->
            assertNotEquals(
                "a retired id must not silently move the user onto a theme they never chose",
                resolveColorScheme("ROYAL_BLUE", isDark),
                resolveColorScheme("AZURE", isDark)
            )
        }
    }

    @Test
    fun `the retired set holds the dead id only, never a mapping to its successor`() {
        // A map would translate rather than reset. `mapOf("AZURE" to "ROYAL_BLUE")`
        // is the version that looked tidier and did the wrong thing: it kept the
        // user on a theme they never picked, and being a pure preference rewrite it
        // did nothing for the missing launcher icon.
        assertEquals(setOf("AZURE"), retiredThemeOptions())
    }

    @Test
    fun `a retired id is recognised as retired whatever its casing`() {
        // The stored value is uppercased at every comparison site, so a lowercased
        // write must still be caught - otherwise the failsafe misses exactly the
        // case it exists for.
        assertTrue(isRetiredThemeOption("AZURE"))
        assertTrue(isRetiredThemeOption("azure"))
        assertTrue(isRetiredThemeOption("Azure"))
    }

    @Test
    fun `no LIVE theme id is ever treated as retired`() {
        // The failsafe must be surgical. If it fired for a theme a user can still
        // pick, it would throw away a valid choice on every launch.
        listOf("BEIGE", "FLAME", "EMERALD", "CRIMSON", "ROYAL_BLUE", "OCEAN", "ROSE", "PURPLE", "DEVELOPER", "DEFAULT")
            .forEach {
                assertFalse("$it is live and must not be treated as retired", isRetiredThemeOption(it))
                assertFalse("$it is live and must not be treated as retired", isRetiredThemeOption(it.lowercase()))
                // A live id is also passed through untouched, so nothing rewrites it.
                assertEquals(it, migrateThemeOption(it))
            }
    }

    @Test
    fun `a retired id migrates to null, which is what routes it to the default`() {
        // null is the load-bearing value here: "no stored choice". It is what makes
        // resolveColorScheme fall to its `else`, and what lets RepIoApplication
        // treat the device as never having chosen a theme.
        assertNull(migrateThemeOption("AZURE"))
        assertNull(migrateThemeOption("azure"))
    }

    @Test
    fun `unknown and null ids pass through untouched`() {
        // The caller's own defaulting stays the single place that decides what an
        // unrecognised value means, so migration must not invent an answer - and
        // must not claim an id it does not know is retired.
        assertNull(migrateThemeOption(null))
        assertEquals("NOT_A_THEME", migrateThemeOption("NOT_A_THEME"))
        assertFalse(isRetiredThemeOption("NOT_A_THEME"))
        assertFalse(isRetiredThemeOption(null))
    }

    @Test
    fun `every live theme resolves to its own distinct scheme`() {
        // The other way a rename goes wrong: both ids parse, both compile, both
        // look right in Settings, and two themes collapse onto one scheme.
        val light = listOf("BEIGE", "EMERALD", "CRIMSON", "ROYAL_BLUE")
        val dark = listOf("FLAME", "OCEAN", "ROSE", "PURPLE")
        val schemes = (light + dark).map { resolveColorScheme(it, isDark = it in dark) }

        assertEquals(
            "two live theme ids resolve to the same colour scheme",
            schemes.size,
            schemes.distinct().size
        )
    }

    @Test
    fun `the retired id survives only as a retired marker, never as live shipped code`() {
        // What must not survive is the retired id used AS AN IDENTIFIER in shipped
        // code - a theme key, a resource name, an activity class, a tool list.
        // Prose that says WHY the id was retired is the opposite of a problem: it
        // is the record that stops the next session re-deriving the same wrong
        // assumption (#007AFF is hue 211, not azure's 210). So comments are
        // stripped first and only CODE is asserted on.
        //
        // Scope is src/main + tools, deliberately NOT src/test: this file has to
        // name the retired id in code in order to assert on it at all, so
        // including tests would make the case fail on its own assertions. The
        // cases above still pin the behaviour; this one pins the codebase.
        val offenders = listOf("src/main", "../tools").flatMap { root ->
            File(root).walkTopDown()
                .filter { it.isFile && it.extension in setOf("kt", "xml", "py") }
                .filter { !it.path.contains("/build/") && !it.path.contains("__pycache__") }
                .filter { file ->
                    val code = stripComments(file.readText())
                    // The only place the id may appear as code is the retired set.
                    val allowed = file.path.endsWith("ui/theme/Theme.kt")
                    !allowed && code.contains("azure", ignoreCase = true)
                }
                .map { it.path }
        }
        assertTrue("retired id still used as live shipped code: $offenders", offenders.isEmpty())
    }

    /** Drops `//` line comments and the inside of `/* ... */` blocks. */
    private fun stripComments(source: String): String = source
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("//[^\\n]*"), " ")
}