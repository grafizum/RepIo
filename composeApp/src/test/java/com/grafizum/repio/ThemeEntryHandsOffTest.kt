package com.grafizum.repio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE LAUNCHER ENTRIES MUST NOT BE THE APP, because the theme switch disables them.
 *
 * This is the property whose absence cost the owner two reports, and it is worth stating
 * exactly: a theme change enables one theme's launcher entry and disables the rest
 * ([com.grafizum.repio.util.IconSwitchHelper]), and disabling an entry takes down whatever was
 * started from it - the whole TASK whose base intent names it, not merely the ActivityRecord.
 * `DONT_KILL_APP` does not help; it keeps the PROCESS alive, not the window.
 *
 * MEASURED on emulator-5554, 2026-10-14, app launched through its theme entry as the
 * home-screen icon does:
 *
 *     IconSwitchHelper: Enabled component: …MainActivityOcean (process kept alive)
 *     pid 11603 -> 11603                       (the process really did survive)
 *     WindowManager: Force finishing ActivityRecord{…MainActivityRetro t150}
 *     task closed, the app visibly closed
 *
 * ...and again after the entries were made aliases pointing at an always-enabled target, since
 * the TASK's base intent still named the disabled entry:
 *
 *     WindowManagerShell: Transition CLOSE … baseIntent=Intent { cmp=…/LauncherOcean }
 *
 * So the fix is structural, not a flag: the entries hand off to [MainActivity] and finish (see
 * [ThemeLauncherEntry]), the UI runs in that always-enabled activity, and what a switch
 * disables is therefore only ever a momentary entry. These cases hold the two halves of that
 * shape, which a future edit could undo without any test failing on the pixels - the Compose
 * tests here have no way to see a package-manager write at all.
 */
class ThemeEntryHandsOffTest {

    private val manifest: String =
        File("src/main/AndroidManifest.xml").readText()

    private val variants: String =
        File("src/main/java/com/grafizum/repio/AppActivityVariants.kt").readText()

    /** The simple names of every `<activity>` that carries a MAIN/LAUNCHER intent filter. */
    private fun launcherEntryNames(): List<String> =
        // Split on the element name, not on "<activity ": in this manifest the element is
        // followed by a newline, so a split on the trailing space matched nothing at all - which
        // is exactly how the first version of this test failed, with "no launcher entry found".
        manifest.split(activityElement)
            .filter { it.contains("android.intent.category.LAUNCHER") }
            .mapNotNull { block ->
                Regex("""android:name="com\.grafizum\.repio\.(\w+)"""")
                    .find(block)?.groupValues?.get(1)
            }

    /**
     * Every launcher entry hands off; none of them IS the UI.
     *
     * The assertion is on the SUPERCLASS, not on a comment or a name: an entry declared
     * `: MainActivity()` again would put the app back in the entry, and every theme change
     * would close it exactly as before.
     */
    @Test
    fun everyLauncherEntryHandsOffInsteadOfHostingTheUi() {
        // The BASE CLASS is pinned too, and this is the assertion that matters: checking only
        // `class Entry : ThemeLauncherEntry()` one level deep would pass an edit that made
        // ThemeLauncherEntry itself extend MainActivity - the entries would be the app again,
        // with every case in this file still green.
        assertTrue(
            "ThemeLauncherEntry extends MainActivity, so the launcher entries ARE the app again: " +
                "the theme switch disables entries, and disabling the entry the app was launched " +
                "from closes it",
            !Regex("""class\s+ThemeLauncherEntry\s*:\s*MainActivity\s*\(""").containsMatchIn(variants)
        )
        assertTrue(
            "ThemeLauncherEntry no longer extends a plain Activity, so it is not the small " +
                "hand-off this design needs it to be",
            Regex("""class\s+ThemeLauncherEntry\s*:\s*Activity\s*\(""").containsMatchIn(variants)
        )

        val entries = launcherEntryNames()
        assertTrue(
            "no launcher entry found in the manifest - did the intent filters move?",
            entries.size >= 9
        )
        for (entry in entries) {
            assertTrue(
                "$entry is a launcher entry, so it must extend ThemeLauncherEntry: the theme " +
                    "switch disables launcher entries, and disabling one closes the app that " +
                    "was launched from it",
                Regex("""class\s+$entry\s*:\s*ThemeLauncherEntry\(\)""").containsMatchIn(variants)
            )
            assertTrue(
                "$entry extends MainActivity, so the app runs as a component the theme switch " +
                    "disables - which is what closed it on a theme change",
                !Regex("""class\s+$entry\s*:\s*MainActivity\(\)""").containsMatchIn(variants)
            )
        }
    }

    /**
     * ...and the thing they hand off TO is the one the switch cannot disable as an entry.
     *
     * Two halves again: the hand-off must actually start [MainActivity], and [MainActivity] must
     * not itself be a launcher entry (no MAIN/LAUNCHER filter), because an entry is exactly the
     * kind of component a switch disables.
     */
    @Test
    fun theHandOffTargetIsNeverALauncherEntry() {
        assertTrue(
            "ThemeLauncherEntry no longer starts MainActivity - what does it hand off to?",
            Regex("""Intent\(\s*this,\s*MainActivity::class\.java\s*\)""").containsMatchIn(variants)
        )

        val mainActivityBlock = manifest.split(activityElement)
            .firstOrNull { it.contains("com.grafizum.repio.MainActivity\"") }
        assertTrue(
            "the base MainActivity is not declared in the manifest any more",
            mainActivityBlock != null
        )
        assertTrue(
            "MainActivity carries a MAIN/LAUNCHER filter, which makes the UI host a launcher " +
                "entry - the very thing the theme switch disables",
            !mainActivityBlock!!.contains("android.intent.category.LAUNCHER")
        )
        assertEquals(
            "the UI host is expected to be exactly one activity",
            1,
            manifest.split(activityElement)
                .count { it.contains("""android:name="com.grafizum.repio.MainActivity"""") }
        )
    }

    private companion object {
        val activityElement = Regex("""<activity\b""")
    }
}
