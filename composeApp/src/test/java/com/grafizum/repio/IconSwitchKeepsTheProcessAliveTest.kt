package com.grafizum.repio

import android.content.pm.PackageManager
import com.grafizum.repio.util.IconSwitchHelper
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE LAUNCHER-ICON SWITCH MUST NOT COST THE USER A RESTART.
 *
 * `setComponentEnabledSetting` takes a flags argument, and the difference between the two
 * possibilities is the whole feature: `0` tells the system it MAY kill the process to apply the
 * change, `DONT_KILL_APP` asks it not to. `IconSwitchHelper` used to pass
 * `if (killApp) 0 else DONT_KILL_APP`, and every caller took the 0 -
 * `PlatformUtils.applyAppIconChange` left the parameter at its `true` default.
 *
 * MEASURED ON emulator-5554, 2026-10-14, before this change: tapping a theme and pressing OK on
 * the "Restart Required" dialog left `pidof com.grafizum.repio.debug` EMPTY, and the launcher
 * entry only then moved to the new theme's activity. So changing the app's colour cost the user a
 * close-and-reopen, and a whole dialog existed to explain a restart the app was causing itself.
 * That is the owner's report: *"can we change the theme and icon without needing to close and
 * reopen the app manually"*.
 *
 * THE FLAG WAS NECESSARY AND NOT SUFFICIENT, which is the second half of this story and the
 * reason the owner had to report the same thing twice. With `DONT_KILL_APP` in place the process
 * really did survive every switch - measured, pid 11603 -> 11603 - while the app still closed,
 * because the home-screen icon launches the app THROUGH its theme entry and disabling that entry
 * takes the whole TASK whose base intent names it:
 *
 *     WindowManager: Force finishing ActivityRecord{…MainActivityRetro t150}
 *
 * This test can only hold the flag, and this note is here so nobody reads a green run as "the
 * close is fixed". That half lives in [com.grafizum.repio.ThemeLauncherEntry] and is guarded by
 * [ThemeEntryHandsOffTest]: the entries hand off to the always-enabled `MainActivity`, so what a
 * switch disables is never what the user is looking at.
 *
 * WHY THESE CASES AND NOT A RENDER TEST. There is nothing to render: the switch is a
 * PackageManager call, and a unit test has no PackageManager. What IS testable is the flag the
 * call is built from, and the absence of the two shapes that turned it back into a kill - which
 * is exactly what the bug was. Case 1 fails if the flag regresses; cases 2 and 3 fail if a kill
 * switch or a literal-`0` flags argument comes back.
 */
class IconSwitchKeepsTheProcessAliveTest {

    /**
     * Case 1. The flag is `DONT_KILL_APP` and is emphatically not 0.
     *
     * Asserted as BOTH, because they answer different questions: the equality pins the intent, and
     * `assertNotEquals(0)` is the one that would have caught the shipped bug - `0` is a perfectly
     * valid flags value that compiles, runs and kills the app. A test that only asserted "it is an
     * Int" would have passed on every version of this code.
     */
    @Test
    fun theIconSwitchDoesNotAskTheSystemToKillTheApp() {
        val flags = IconSwitchHelper.iconSwitchFlags()
        assertEquals(
            "the icon switch no longer asks for DONT_KILL_APP, so the system is free to kill the " +
                "process - which is what closed the app on a theme change",
            PackageManager.DONT_KILL_APP, flags,
        )
        assertNotEquals(
            "the icon switch passes flags 0, which is permission for the system to kill the " +
                "process; the app must change its icon without closing",
            0, flags,
        )
    }

    /**
     * Case 2. NO CALLER CAN ASK FOR A KILL, and no component write passes a literal 0.
     *
     * A source-level assertion, deliberately: the helper's three writes are the disable, the
     * enable and the base-activity repair, and a fourth added later by hand would otherwise be
     * free to pass a literal and reopen the bug without touching this test's subject. Comments are
     * stripped first so the doc block above - which quotes `killApp` on purpose - cannot make the
     * absence checks pass or fail by accident; only real code counts.
     *
     * The repo already reads its own sources from a unit test for exactly this reason
     * (`StringResourceIntegrityTest` scans for `getString` calls and strips XML comments), so the
     * idiom is established rather than invented here.
     */
    @Test
    fun noComponentWriteCanAskForAKill() {
        val code = stripComments(
            File(locateMainJava(), "com/grafizum/repio/util/IconSwitchHelper.kt").readText()
        )

        assertTrue(
            "no setComponentEnabledSetting call found - did the helper move or get renamed?",
            Regex("""setComponentEnabledSetting\(""").containsMatchIn(code),
        )
        assertTrue(
            "IconSwitchHelper still exposes a kill switch, so a caller can pass a flag that kills " +
                "the app - the decision this test exists to keep out",
            !code.contains("killApp"),
        )
        assertTrue(
            "IconSwitchHelper passes a literal 0 as the flags argument of a component-state " +
                "write, which is what let the system kill the process on a theme change; every " +
                "write must name iconSwitchFlags()",
            !Regex("""COMPONENT_ENABLED_STATE_\w+,\s*0\s*[,)]""").containsMatchIn(code),
        )
        assertTrue(
            "IconSwitchHelper no longer routes its component-state writes through " +
                "iconSwitchFlags()",
            code.contains("iconSwitchFlags()"),
        )
    }

    /** Line and block comments removed, so an absence check cannot be satisfied by prose. */
    private fun stripComments(source: String): String =
        source
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""//.*"""), "")

    private fun locateMainJava(): File {
        val relative = "src/main/java"
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (File(candidate, "com/grafizum/repio/util/IconSwitchHelper.kt").isFile) return candidate
            dir = dir.parentFile
        }
        throw AssertionError("could not locate $relative from ${System.getProperty("user.dir")}")
    }
}
