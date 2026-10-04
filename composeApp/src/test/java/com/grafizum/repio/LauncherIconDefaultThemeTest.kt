package com.grafizum.repio

import com.grafizum.repio.util.IconSwitchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A fresh install shows ONE icon, and which one is decided by two places that
 * have to agree: the manifest can enable only a single launcher activity, and
 * `RepIoApplication` decides whether to switch away from it.
 *
 * The owner's report was "it shows beige and dark, the default theme is light
 * beige which should make the white and beige icon". Two separate things were
 * true at once and neither was the plate bug:
 *
 *  1. The PHONE was in system dark mode (`cmd uimode night` -> `yes`). Android,
 *     not the app, picks the plate: it resolves `mipmap-night-anydpi-v26/` over
 *     `mipmap-anydpi-v26/`, so EVERY theme shows its dark-plate icon on a dark
 *     system. No theme choice can override that, and `LauncherIconNightVariantTest`
 *     already holds the two variants apart. So "beige and dark" was correct.
 *
 *  2. The launcher entry really WAS Beige, because the manifest enables Beige and
 *     `RepIoApplication` had its `switchIcon` call removed ("to prevent duplicate
 *     icons"). On a dark system it stored FLAME as the theme and then showed the
 *     Beige icon anyway - the stored theme and the visible icon disagreed. This
 *     test holds that fixed.
 */
class LauncherIconDefaultThemeTest {

    private val manifest = File("src/main/AndroidManifest.xml")

    /**
     * The launcher activities the manifest declares, with their enabled state and
     * icon. Exactly one may be enabled: two enabled LAUNCHER activities means two
     * icons on the home screen, which is the "duplicate icons" the old comment
     * was written about.
     */
    private data class LauncherEntry(
        val activity: String,
        val enabled: Boolean,
        val icon: String?,
    )

    private fun launcherEntries(): List<LauncherEntry> {
        val text = manifest.readText()
        val entries = mutableListOf<LauncherEntry>()
        val activity = Regex("<activity\\s[^>]*>(.*?)</activity>", RegexOption.DOT_MATCHES_ALL)
        for (match in activity.findAll(text)) {
            val block = match.groupValues[1]
            if (!block.contains("android.intent.category.LAUNCHER")) continue
            val name = Regex("""android:name="com\.grafizum\.repio\.(MainActivity\w*)"""")
                .find(match.value)?.groupValues?.get(1)
                ?: continue
            // `android:enabled` is absent on the base activity, which means enabled.
            val enabled = Regex("""android:enabled="(\w+)"""")
                .find(match.value)?.groupValues?.get(1)?.toBoolean() ?: true
            val icon = Regex("""android:icon="@mipmap/(\w+)"""")
                .find(match.value)?.groupValues?.get(1)
            entries += LauncherEntry(name, enabled, icon)
        }
        return entries
    }

    /** Exactly one launcher entry may be enabled, or the home screen shows two icons. */
    @Test
    fun exactlyOneLauncherActivityIsEnabled() {
        val entries = launcherEntries()
        assertTrue(
            "no LAUNCHER activity found; the manifest parse is wrong, not the manifest",
            entries.isNotEmpty(),
        )
        val enabled = entries.filter { it.enabled }
        assertEquals(
            "exactly one launcher activity may be enabled, otherwise the home " +
                "screen shows duplicate icons. Enabled: " +
                enabled.joinToString { it.activity },
            1,
            enabled.size,
        )
    }

    /**
     * `IconSwitchHelper.MANIFEST_DEFAULT_THEME` is a hardcoded string, and this is
     * what stops it from silently rotting. If someone flips the manifest's enabled
     * entry, the "only switch when it differs" guard starts skipping the switch
     * for the wrong theme - a bug with no symptom until someone reinstalls.
     */
    @Test
    fun theCodeAgreesWithTheManifestAboutTheEnabledEntry() {
        val enabled = launcherEntries().single { it.enabled }
        assertEquals(
            "IconSwitchHelper.MANIFEST_DEFAULT_THEME must be the theme whose " +
                "launcher activity the manifest enables, or the first-launch icon " +
                "switch is skipped for the wrong theme",
            enabled.activity.removePrefix("MainActivity").uppercase(),
            IconSwitchHelper.MANIFEST_DEFAULT_THEME,
        )
    }

    /**
     * The decision itself: Flame on a dark system, Beige on a light one.
     *
     * The light case must return the manifest default, because that is the entry
     * the manifest already enabled and switching to it is the duplicate-icon
     * mistake. That is the whole reason this is a named function and not an
     * inline `if` in `RepIoApplication`.
     */
    @Test
    fun theFirstLaunchThemeIsBeigeInBothSystemAppearances() {
        // Owner ruling 2026-10-04: "change the base theme of the app to be the
        // light beige one no matter if the system is dark or light". This test
        // used to assert the opposite - Flame on a dark system - which is the
        // correct assertion for the OLD rule and now pins the new one.
        assertEquals(
            "a dark system must ALSO start on Beige: the base theme no longer " +
                "follows the system appearance",
            IconSwitchHelper.MANIFEST_DEFAULT_THEME,
            IconSwitchHelper.defaultThemeFor(isSystemDark = true),
        )
        assertEquals(
            "a light system must start on the theme the manifest already enables",
            IconSwitchHelper.MANIFEST_DEFAULT_THEME,
            IconSwitchHelper.defaultThemeFor(isSystemDark = false),
        )
        // Stated as a property rather than left implied by the two asserts above:
        // the whole point of the ruling is that the argument does not matter, and
        // an equality pair still reads as two separate expectations.
        assertEquals(
            "the system appearance must not change the base theme",
            IconSwitchHelper.defaultThemeFor(isSystemDark = false),
            IconSwitchHelper.defaultThemeFor(isSystemDark = true),
        )
    }

    /**
     * The guard in `RepIoApplication` only switches when the target differs from
     * the manifest default. Asserted here as the property it must have, because
     * the call site is inside `Application.onCreate` and cannot be run in a unit
     * test.
     */
    @Test
    fun noSystemAppearanceNeedsAnIconSwitchAnymore() {
        // The REPLACES `onlyTheDarkSystemNeedsAnIconSwitch`, which asserted that a
        // dark system differs from the manifest default so RepIoApplication has to
        // switch the launcher component. That difference is gone by design.
        //
        // This is the property that makes the ruling SAFE rather than merely
        // different: RepIoApplication's guard is
        // `if (initialTheme != MANIFEST_DEFAULT_THEME) switchIcon(...)`, so a dark
        // phone now resolves to the theme the manifest ALREADY enables and takes
        // the early-return branch. The launcher component is therefore never
        // switched on either appearance, and the duplicate-icon mistake this guard
        // exists to prevent cannot happen.
        //
        // The flip side, and it is the reason Beige's own icon must be right: with
        // no switch, the enabled component stays whatever the manifest declares,
        // so the icon shown on a dark phone is the Beige icon the manifest points
        // at. `LauncherIconNightVariantTest` is what proves that plate is the
        // LIGHT one and cannot be overridden by system night mode.
        val light = IconSwitchHelper.defaultThemeFor(isSystemDark = false)
        val dark = IconSwitchHelper.defaultThemeFor(isSystemDark = true)
        assertEquals(
            "on a light system nothing should be switched: the manifest already " +
                "enables that entry",
            IconSwitchHelper.MANIFEST_DEFAULT_THEME,
            light,
        )
        assertEquals(
            "on a dark system the same holds now, because the base theme is Beige " +
                "in both appearances - so the launcher is never switched",
            IconSwitchHelper.MANIFEST_DEFAULT_THEME,
            dark,
        )
        assertFalse(
            "if this ever differs again, RepIoApplication will switch the launcher " +
                "component again and the icon must be re-checked",
            dark != IconSwitchHelper.MANIFEST_DEFAULT_THEME,
        )
    }
}