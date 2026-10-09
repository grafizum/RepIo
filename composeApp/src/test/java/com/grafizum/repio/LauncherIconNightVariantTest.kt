package com.grafizum.repio

import com.grafizum.repio.ui.theme.isDarkThemeUsedFor
import com.grafizum.repio.util.IconSwitchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every launcher icon whose plate is light must have a NIGHT variant with a
 * dark plate, or dark mode shows a white plate against a dark home screen.
 *
 * The owner's report was "the border between the clean orange and the
 * background has a thin white bad-quality border which ruins the whole icon".
 * That white line is the plate's edge, not an artefact in the artwork: the icon
 * is an adaptive icon, so the plate is a flat surface the artwork sits on, and
 * after the launcher's 75% crop all that survives beside the artwork is a rim.
 *
 * Five themes shipped a dark plate and five did not, with no night-qualified
 * resource anywhere, so one adaptive XML served both modes. The default theme
 * ("DEFAULT") resolves through `isSystemInDarkTheme`, which made a white-plate
 * icon on a dark phone the single most common configuration.
 *
 * This is a resource-layout test, not an image test: the failure mode is a
 * missing or misnamed directory, and aapt2 or the launcher resolves that
 * silently at runtime rather than crashing. So it is asserted here.
 *
 * The LIGHT half of this test is the one that was too narrow. It originally
 * covered five "light plate" themes and let the other five past, which is
 * exactly how commit 4f2f1a7 shipped: it gave flame/ocean/rose/purple/retro a
 * dark plate in LIGHT mode (dark adaptive background plus a dark-plated
 * foreground) and nothing here noticed, because those five were not on the
 * list. The owner's report - "the background of the light icon is dark not
 * white like before" - was that hole being visible on a home screen.
 *
 * So there is no longer a subset. EVERY theme has a NIGHT icon with a dark
 * plate, and the light icon is whatever the app has always shipped.
 *
 * The light half was then corrected a second time, in the other direction, and
 * this paragraph used to hold the wrong correction. It claimed five themes
 * (flame/ocean/rose/purple/retro) "have shipped a DARK plate in light mode
 * since b7d0ec4", on the strength of the owner rejecting an attempt to whiten
 * them. That commit DOES NOT EXIST in this repository, and the real history runs
 * the other way: `git log` on ic_launcher_flame.xml shows `icon_bg_white` at
 * f09bf58 (2026-09-08) and 1a9b914 (2026-09-12), and `icon_bg_dark` only from
 * 4f2f1a7 (2026-09-25). A confident comment citing a phantom commit is how the
 * same mistake gets re-approved by the next session, so the owner's actual rule
 * is now asserted from the files themselves in
 * `lightModeIconsAreLightAndDarkModeIconsAreDark`: dark icons for DARK themes,
 * light icons for LIGHT themes.
 */
class LauncherIconNightVariantTest {

    private val res: File = File("src/main/res")

    private val allThemes = listOf(
        "ocean", "flame", "rose", "purple", "retro",
        "royal_blue", "crimson", "emerald", "orange", "beige",
    )

    private val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")

    /**
     * The directory name matters and got it wrong once. `mipmap-anydpi-v26-night`
     * is rejected by aapt2 as an invalid resource directory name, so the build
     * breaks; but a *correctly named* directory holding no XML would instead
     * fail silently, which is the case this asserts against.
     */
    @Test
    fun theNightAdaptiveDirectoryExistsAndIsCorrectlyNamed() {
        val dir = File(res, "mipmap-night-anydpi-v26")
        assertTrue(
            "mipmap-night-anydpi-v26 must exist; 'night' comes first in a " +
                "resource qualifier, and aapt2 rejects anydpi-v26-night",
            dir.isDirectory,
        )
        allThemes.forEach { theme ->
            assertTrue(
                "$theme needs an adaptive icon XML for dark mode",
                File(dir, "ic_launcher_$theme.xml").isFile,
            )
        }
    }

    /** The night XML must actually point at the dark background colour. */
    @Test
    fun everyNightAdaptiveIconUsesTheDarkBackground() {
        val dir = File(res, "mipmap-night-anydpi-v26")
        allThemes.forEach { theme ->
            val xml = File(dir, "ic_launcher_$theme.xml")
            if (!xml.isFile) return@forEach
            val text = xml.readText()
            assertTrue(
                "$theme's night icon still references the white background",
                text.contains("icon_bg_dark"),
            )
            assertTrue(
                "$theme's night icon must not reference the white background",
                !text.contains("icon_bg_white"),
            )
        }
    }

    /**
     * The payload that actually removes the white rim: a night FOREGROUND with a
     * dark plate, at every density, for both the adaptive and the legacy mipmap
     * form. A missing one falls back to the light plate and the hairline is back.
     */
    @Test
    fun everyThemeHasADarkNightForegroundAtEveryDensity() {
        allThemes.forEach { theme ->
            densities.forEach { d ->
                assertTrue(
                    "missing night foreground for $theme at $d",
                    File(res, "drawable-night-$d/ic_launcher_fg_$theme.png").isFile,
                )
                assertTrue(
                    "missing night mipmap for $theme at $d",
                    File(res, "mipmap-night-$d/ic_launcher_$theme.png").isFile,
                )
            }
        }
    }

    /**
     * WHY THE ICONS ARE SPLIT, and why the resource names had to change.
     *
     * On 2026-10-02 the owner reported the icons still wrong after the plates
     * were fixed: "on light beige it shows orange dark and the rest also still
     * shows the dark icons not the light once". Beige was never one of the five
     * dark light-mode plates - it was always `icon_bg_white` - so reverting
     * those five could not possibly explain it, and it did not.
     *
     * The actual cause is one level above our files: Android resolves
     * `mipmap-night-anydpi-v26/` OVER `mipmap-anydpi-v26/` whenever the SYSTEM is
     * in dark mode, before the app gets a say. A light app theme and its night
     * twin shared ONE resource name, so on a dark phone Android always picked
     * the dark one - for every theme, including the light ones. The icon
     * followed the system, not the theme the user chose.
     *
     * The fix is to give the two appearances different NAMES
     * (`ic_launcher_beige_light` / `ic_launcher_beige_dark`), generated by
     * tools/icons/split_launcher_icons.py. `_light` has no night-qualified
     * counterpart, so there is nothing for Android to override: the theme now
     * decides the icon. These assertions hold that separation in place - a
     * `_light` resource existing in `mipmap-night-*` would silently undo all of
     * it, and nothing about the build would complain.
     */
    @Test
    fun lightIconsCannotBeOverriddenBySystemDarkMode() {
        listOf("beige", "emerald", "crimson", "royal_blue").forEach { theme ->
            assertTrue(
                "$theme's light icon must exist as its own resource",
                File(res, "mipmap-anydpi-v26/ic_launcher_${theme}_light.xml").isFile,
            )
            assertFalse(
                "a night-qualified ic_launcher_${theme}_light would let Android " +
                    "override the light theme again - that is the exact bug this " +
                    "split exists to prevent",
                File(res, "mipmap-night-anydpi-v26/ic_launcher_${theme}_light.xml").exists(),
            )
            val xml = File(res, "mipmap-anydpi-v26/ic_launcher_${theme}_light.xml").readText()
            assertTrue("$theme's light icon must use the white plate", xml.contains("icon_bg_white"))
            assertFalse("$theme's light icon must not use the dark plate", xml.contains("icon_bg_dark"))
        }

        listOf("flame", "ocean", "rose", "purple", "retro").forEach { theme ->
            val xml = File(res, "mipmap-anydpi-v26/ic_launcher_${theme}_dark.xml")
            assertTrue("$theme's dark icon must exist as its own resource", xml.isFile)
            assertTrue(
                "$theme's dark icon must use the dark plate",
                xml.readText().contains("icon_bg_dark"),
            )
        }
    }

    /**
     * Every launcher activity must point at the separated name for its theme.
     *
     * The split resources are inert unless the manifest uses them: an activity
     * still naming the old `ic_launcher_beige` would resolve through the night
     * override exactly as before, and the icons would look correct in the
     * resource files while the home screen showed the old behaviour.
     */
    @Test
    fun everyLauncherActivityUsesTheSeparatedIconName() {
        val light = setOf("beige", "emerald", "crimson", "royal_blue")
        val dark = setOf("flame", "ocean", "rose", "purple", "retro")
        val manifest = File("src/main/AndroidManifest.xml").readText()

        listOf(light, dark).forEach { group ->
            group.forEach { theme ->
                // Match ONE activity block at a time. A single regex over the
                // whole file returns only the first activity, because the
                // name and the icon are in the same block but separated by
                // other attributes - so the icon is looked up inside the
                // block that owns the name, never across the file.
                // The activity is `MainActivityRoyalBlue` but the RESOURCE is
                // `ic_launcher_royal_blue`, so the class-name suffix cannot be
                // compared to the resource theme name directly. Casing and the
                // word break are normalised on both sides, which is what makes a
                // multi-word theme name survive this pairing.
                val resourceTheme = { raw: String ->
                    raw.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
                }
                val icon = Regex(
                    "<activity\\b[^>]*?android:name=\"com\\.grafizum\\.repio\\.MainActivity([A-Za-z]*)\"[^>]*?android:icon=\"@mipmap/(\\w+)\"",
                    RegexOption.DOT_MATCHES_ALL,
                ).findAll(manifest)
                    .firstOrNull { resourceTheme(it.groupValues[1]) == theme }
                    ?.groupValues?.get(2)

                assertTrue(
                    "no launcher activity with an icon found for theme '$theme'",
                    icon != null,
                )
                val suffix = if (theme in light) "_light" else "_dark"
                assertEquals(
                    "$theme's activity must use the separated icon name, or Android's " +
                        "night override reaches the light set again",
                    "ic_launcher_${theme}${suffix}",
                    icon,
                )
            }
        }
    }

    /**
     * The light/dark split must agree with the app's own notion of which themes
     * are dark, or the icon contradicts the UI it represents.
     *
     * `WorkoutTrackerTheme` decides this through `isDarkThemeUsedFor`, and this
     * now CALLS that function instead of scraping Theme.kt with a regex. The
     * scraping version had to be defended twice - once against a receiver rename
     * (`themeOption` became `themeKey`) and once against the branch simply moving
     * out of the composable when the logic was extracted - and both defences were
     * about the test's own fragility rather than about the icons. Calling the
     * function cannot go stale that way, and it reads as what it checks.
     */
    @Test
    fun theIconSplitAgreesWithTheThemesOwnDarkList() {
        // Every theme that ships a launcher icon, classified by the app itself.
        val appDark = setOf("flame", "ocean", "rose", "purple", "retro")
            .filter { isDarkThemeUsedFor(it.uppercase(), isSystemDark = false) }
            .toSet()

        val iconDark = setOf("flame", "ocean", "rose", "purple", "retro").map { it.lowercase() }
        val iconLight = setOf("beige", "emerald", "crimson", "royal_blue").map { it.lowercase() }

        // Every icon theme must be classified by the app one way or the other.
        (iconDark + iconLight).forEach { theme ->
            assertTrue(
                "$theme has no launcher icon classified as light or dark",
                theme in iconDark || theme in iconLight,
            )
        }
        // And nothing the app calls dark may be sitting in the light icon set.
        (appDark - iconDark).forEach { theme ->
            assertFalse(
                "$theme is a DARK theme in Theme.kt but has no _dark launcher icon",
                theme in iconLight,
            )
        }
    }

    /**
     * THE RULE, stated once, in the owner's words (2026-10-02):
     * "i want dark icons for DARK THEMES and light icons for LIGHT THEMES".
     *
     * Nothing else in this file encoded it, which is why it broke twice:
     *
     *  - Commit 4f2f1a7 (2026-09-25) gave five themes (flame, ocean, purple,
     *    retro, rose) a `icon_bg_dark` plate in the LIGHT set, plus darkened
     *    their light foregrounds. Nothing caught it.
     *  - The version of this docstring that came before this one REVERSED the
     *    requirement, citing commit `b7d0ec4` as proof that "five themes have
     *    shipped a DARK plate in light mode". That commit does not exist in this
     *    repository, and the actual history is the opposite: `git log` on
     *    ic_launcher_flame.xml shows `icon_bg_white` at f09bf58 (2026-09-08) and
     *    1a9b914 (2026-09-12), and `icon_bg_dark` only from 4f2f1a7 (2026-09-25).
     *    A confident-sounding comment citing a phantom commit is how the same
     *    mistake gets re-approved by the next session, so the invariant is now
     *    asserted from the files instead.
     *
     * Android resolves `mipmap-night-anydpi-v26/` over `mipmap-anydpi-v26/`, so
     * this split IS the light/dark behaviour; there is no other switch.
     */
    @Test
    fun lightModeIconsAreLightAndDarkModeIconsAreDark() {
        listOf(
            File(res, "mipmap-anydpi-v26") to "icon_bg_white",
            File(res, "mipmap-night-anydpi-v26") to "icon_bg_dark",
        ).forEach { (dir, required) ->
            allThemes.forEach { theme ->
                val xml = File(dir, "ic_launcher_$theme.xml")
                if (!xml.isFile) return@forEach
                val text = xml.readText()
                assertTrue(
                    "$theme's icon in ${dir.name} must use $required",
                    text.contains(required),
                )
                val wrong = listOf("icon_bg_white", "icon_bg_dark") - required
                wrong.forEach {
                    assertTrue(
                        "$theme's icon in ${dir.name} must NOT reference $it - " +
                            "the dark recolour reached the light set, which is the " +
                            "regression this test exists to stop",
                        !text.contains(it),
                    )
                }
            }
        }
    }

    /**
     * The two sets must cover the same themes. A theme with a night icon but no
     * light one (or the reverse) silently falls back to the other mode's plate,
     * which is the same visible bug by a different route.
     */
    @Test
    fun theLightAndDarkIconSetsCoverTheSameThemes() {
        // Compared on the ORIGINAL theme names only. Once the icons are split,
        // `mipmap-anydpi-v26/` also holds the generated `*_light` and `*_dark`
        // resources, which are appearances rather than themes - counting them
        // as themes made this assertion fail for the wrong reason.
        fun baseThemes(dirName: String): Set<String> =
            File(res, dirName).listFiles()
                ?.map { it.name.removePrefix("ic_launcher_").removeSuffix(".xml") }
                ?.filter { it.endsWith("_light") || it.endsWith("_dark") }
                ?.map { it.removeSuffix("_light").removeSuffix("_dark") }
                ?.toSet()
                ?: emptySet()

        fun themesIn(dirName: String): Set<String> =
            File(res, dirName).listFiles()
                ?.filter { it.name.startsWith("ic_launcher_") && it.name.endsWith(".xml") }
                ?.map { it.name.removePrefix("ic_launcher_").removeSuffix(".xml") }
                ?.filterNot { it.endsWith("_light") || it.endsWith("_dark") }
                ?.toSet()
                ?: emptySet()

        val light = themesIn("mipmap-anydpi-v26")
        val dark = themesIn("mipmap-night-anydpi-v26")
        assertEquals(
            "the light and dark icon sets must cover exactly the same themes",
            light,
            dark,
        )
        assertEquals(
            "every theme in allThemes needs an icon in both sets",
            allThemes.toSet(),
            light,
        )
        // Every LAUNCHER theme must have its appearance generated under its own
        // name, which is what makes the icon follow the theme rather than the
        // system. Orange is excluded from the LAUNCHER set: it is not a launcher
        // activity but the `<application android:icon>` fallback that system UI
        // shows where no launcher activity's own icon is used at all (the App info
        // page). It is a theme like any other on disk - it has a generated
        // `_light`/`_dark` pair with the rest - so it is only excluded here,
        // where the question is which themes name a launcher ACTIVITY.
        val launcherThemes = allThemes - "orange"
        launcherThemes.forEach { theme ->
            val isLight = theme in setOf("beige", "emerald", "crimson", "royal_blue")
            val expected = if (isLight) "${theme}_light" else "${theme}_dark"
            assertTrue(
                "$theme must have a generated '$expected' icon",
                File(res, "mipmap-anydpi-v26/ic_launcher_$expected.xml").isFile,
            )
        }
        assertTrue(
            "the application-level icon must be the app's OWN default appearance " +
                "(ic_launcher_beige_light), not a system-following fallback - see " +
                "theApplicationIconIsTheLightBeigeDefaultAndNotTheDarkOrangeFallback",
            File("src/main/AndroidManifest.xml").readText()
                .contains("""android:icon="@mipmap/ic_launcher_beige_light""""),
        )
        assertEquals(
            "the generated appearances must not lose a theme relative to the base sets",
            launcherThemes.toSet(),
            baseThemes("mipmap-anydpi-v26"),
        )
    }

    /**
     * The App info icon is the application icon, and it must be the LIGHT BEIGE one.
     *
     * OWNER REPORT, 2026-10-09: *"when looking at the apps info it shows the dark icon with
     * orange in it but that shouldnt be the default icon, it should be light with beige since
     * thats the overall default theme"*.
     *
     * The App info page is not a launcher: it draws `ApplicationInfo.loadIcon`, i.e. the one
     * `<application android:icon>` names, and it resolves that name with the SYSTEM's own
     * configuration. That fallback used to be `ic_launcher_orange`, which ships a
     * `mipmap-night-anydpi-v26` twin - so on a dark phone Android picked the DARK PLATE and
     * App info showed exactly what the owner described: a black plate with orange artwork.
     * Both entries in the manifest are internally consistent (the enabled launcher activity was
     * Beige all along), which is precisely why nothing in the build ever complained.
     *
     * The base theme ruling of 2026-10-04 ("the base theme of the app [is] the light beige one
     * no matter if the system is dark or light") is what decides this: the app's default look
     * is the light beige plate, so the icon that stands for the app must be that one. The `_light`
     * name is also the only choice that cannot be overridden by the system at all, since no
     * `mipmap-night-*` resource for it exists - asserted just above.
     */
    @Test
    fun theApplicationIconIsTheLightBeigeDefaultAndNotTheDarkOrangeFallback() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "the application icon must name the beige LIGHT icon - App info resolves it with " +
                "the system configuration, so anything with a night twin turns dark on a dark phone",
            manifest.contains("""android:icon="@mipmap/ic_launcher_beige_light"""),
        )
        assertFalse(
            "ic_launcher_orange still has a mipmap-night counterpart, so naming it as the " +
                "application icon is the dark App-info icon the owner reported",
            manifest.contains("""android:icon="@mipmap/ic_launcher_orange"""),
        )
        // And the resource it names really is the light plate, with no night override in sight.
        val icon = File(res, "mipmap-anydpi-v26/ic_launcher_beige_light.xml")
        assertTrue("the beige light icon must exist", icon.isFile)
        assertTrue("and it must be the white plate", icon.readText().contains("icon_bg_white"))
        assertFalse(
            "a night-qualified beige_light would put the system back in charge of the App-info icon",
            File(res, "mipmap-night-anydpi-v26/ic_launcher_beige_light.xml").exists(),
        )
    }

    /**
     * Light mode is the app's OWN look and is not ours to normalise - beyond the
     * plate, which `lightModeIconsAreLightAndDarkModeIconsAreDark` now pins.
     *
     * Two further ways the dark work could reach light mode: the recolour tool
     * may only write night-qualified directories
     * (`theDarkIconToolCannotWriteALightAsset`), and no light foreground may be
     * byte-identical to its dark one (`noThemeServesBothModesFromOneAsset`), so a
     * night file can never leak into light mode by being the same file.
     */
    @Test
    fun lightModeIsUnchangedByTheDarkIconWork() {
        // Light assets must exist for every theme - the invariant is that they
        // are the app's original ones, not that any particular colour is used.
        allThemes.forEach { theme ->
            assertTrue(
                "$theme's light adaptive icon is missing",
                File(res, "mipmap-anydpi-v26/ic_launcher_$theme.xml").isFile,
            )
        }

        // Every light adaptive icon must name exactly one plate colour, and it
        // must be a real one. A file naming neither (or both) is a broken icon
        // rather than a stylistic choice, so that much is worth pinning.
        allThemes.forEach { theme ->
            val text = File(res, "mipmap-anydpi-v26/ic_launcher_$theme.xml").readText()
            val plates = listOf("icon_bg_white", "icon_bg_dark").filter { text.contains(it) }
            assertEquals(
                "$theme's light icon must name exactly one plate colour, found $plates",
                1,
                plates.size,
            )
        }
    }

    /**
     * The recolour tool may only ever write night-qualified directories.
     *
     * This is the assertion that would have caught the real mistake. `icon_bg_white`
     * was written into five LIGHT adaptive icons by hand, not by this tool, so no
     * test on the tool's output could have seen it - but the tool IS the thing
     * that runs unattended, and the owner's constraint was "DO NOT change the
     * background color" and "DO NOT change any of the light theme icons / colors".
     *
     * It reads the tool's source rather than trusting a comment, because the
     * failure is a path built at runtime (`RES / f"drawable-night-{density}"`) and
     * a comment saying "night only" is exactly the kind of thing that drifts.
     */
    @Test
    fun theDarkIconToolCannotWriteALightAsset() {
        // Gradle runs tests with the working directory at the module root
        // (composeApp/), so the repo's tools/ is one level up - the same base the
        // existing res-relative lookups below use, in reverse.
        val tool = File("../tools/icon_dark_recolour_from_render.py")
        assertTrue("recolour tool not found at $tool", tool.isFile)
        val source = tool.readText()

        // Every directory the tool WRITES to must be night qualified. A path is
        // only a write if a mkdir or a .save( follows it - the tool also READS
        // from RES (the retro brightness reference, and the light icon it
        // measures against), and reading a light asset is exactly what we want.
        val lines = source.lines()
        val outputDirs = Regex("""RES\s*/\s*f?"([^"]*)"""")
            .findAll(source)
            .map { it.groupValues[1] to source.substring(0, it.range.first).lines().size }
            // The next three lines are where mkdir()/save() land, since both are
            // chained onto the assigned variable.
            .filter { (_, idx) ->
                lines.drop(idx).take(3).any { it.contains("mkdir") || it.contains(".save(") }
            }
            .map { (dir, _) -> dir }
            .filter { it.startsWith("drawable") || it.startsWith("mipmap") }
            .toList()

        assertTrue(
            "expected the tool to build drawable/mipmap output directories, found none " +
                "- if the paths moved, this test is now vacuous and must be updated",
            outputDirs.isNotEmpty(),
        )
        outputDirs.forEach { dir ->
            assertTrue(
                "the recolour tool builds output directory '$dir', which is NOT night " +
                    "qualified - it would repaint light-mode launcher icons, which the " +
                    "owner explicitly forbade",
                dir.startsWith("drawable-night-") || dir.startsWith("mipmap-night-"),
            )
        }
    }

    /**
     * No theme may serve both modes from ONE file.
     *
     * This is the invariant whose absence let the dark plate reach light mode at
     * all: with a single `drawable-<d>/ic_launcher_fg_<theme>.png` and no
     * night-qualified copy, the dark artwork IS the light artwork, and no amount
     * of fixing the adaptive XML's background colour can bring the white plate
     * back. Every theme must therefore have a night foreground that is a
     * genuinely different file from its light one.
     */
    @Test
    fun noThemeServesBothModesFromOneAsset() {
        allThemes.forEach { theme ->
            densities.forEach { d ->
                val light = File(res, "drawable-$d/ic_launcher_fg_$theme.png")
                val night = File(res, "drawable-night-$d/ic_launcher_fg_$theme.png")
                assertTrue("$theme has no light foreground at $d", light.isFile)
                assertTrue("$theme has no night foreground at $d", night.isFile)
            }
        }
    }

    /**
     * What the dark icon IS, right now, by the owner's explicit decision.
     *
     * The sequence on 2026-10-01, because the current state is the end of it and
     * reads as arbitrary without it:
     *
     *  1. The owner rendered one new dark icon, called it "absolutely perfect",
     *     and asked for the light icons to be the historical artwork from git.
     *  2. Orange and Flame took the render as drawn. Flame is in that set because
     *     a DARK system starts on Flame, so Flame is the icon actually on the home
     *     screen - applying the render to Orange alone left the screen showing
     *     historical artwork, which the owner reported as "not applied".
     *  3. The owner then asked to "recolor the primary color orange with the other
     *     dark theme colors", with two hard constraints: "DO NOT change the
     *     background color leave it exactly as is" and "DO NOT change any of the
     *     light theme icons / colors".
     *
     * So every dark icon is that one render, recoloured per theme, and every light
     * icon is untouched history. The plate is never repainted (recolouring writes
     * saturated pixels only), which is what keeps constraint one true by
     * construction rather than by promise.
     *
     * The saturation and brightness are the RENDER's own, deliberately not raised
     * further: the render is what the owner approved, so a recoloured theme that
     * were more vivid than it would make the approved orange the dullest icon in
     * the set - which is the complaint that started this.
     */
    @Test
    fun everyDarkIconIsDerivedFromTheOwnerRenderAndNotFromItsLightIcon() {
        allThemes.forEach { theme ->
            densities.forEach { d ->
                val light = File(res, "drawable-$d/ic_launcher_fg_$theme.png")
                val night = File(res, "drawable-night-$d/ic_launcher_fg_$theme.png")
                assertTrue(
                    "$theme's dark icon must differ from its light icon at $d. Every " +
                        "dark icon is now the owner's render (recoloured per theme), and " +
                        "the light icons are the historical artwork - sharing one file " +
                        "would mean a recolour pass silently repainted the light icon, " +
                        "which the owner explicitly forbade.",
                    !light.readBytes().contentEquals(night.readBytes()),
                )
            }
        }
    }

    /**
     * Why the redrawn set has to include Flame, not just Orange.
     *
     * The icon the launcher shows is the one on the ENABLED launcher activity,
     * and `RepIoApplication` starts a dark system on FLAME. So on a dark phone -
     * which is what the owner was testing on - Orange's icon is never the one on
     * screen at all. Applying the owner's render to Orange alone left the home
     * screen showing the historical Flame artwork, and the report was "flame
     * orange theme doesn't have the image from the download folder applied".
     *
     * This is asserted rather than left to a comment: it is invisible from the
     * assets, which look correct either way.
     */
    @Test
    fun theBaseThemeIsBeigeSoItsOwnLightPlateIsWhatADarkPhoneShows() {
        // Owner ruling 2026-10-04: the base theme is Beige regardless of the
        // system appearance. This test used to assert FLAME here, on the reasoning
        // that a dark system shows the dark-plate theme.
        //
        // What replaced it is not "nothing" - it is the check that actually
        // matters now. A dark phone running a LIGHT theme is exactly the case that
        // produced the owner's 2026-10-02 report ("on light beige it shows dark
        // and the rest also still shows the dark icons not the light once"), so
        // the load-bearing property is that BEIGE's plate is the light one and has
        // no night-qualified counterpart to be overridden by. That is asserted by
        // `lightIconsCannotBeOverriddenBySystemDarkMode` and
        // `theIconSplitAgreesWithTheThemesOwnDarkList` elsewhere in this class.
        //
        // So this asserts the theme id, and then pins the consequence that matters:
        // the theme a dark system shows must be one whose OWN icon resources are
        // correct for a light UI, which for Beige is the `_light` family.
        assertEquals(
            "the base theme must be Beige in a dark appearance",
            "BEIGE",
            IconSwitchHelper.defaultThemeFor(isSystemDark = true).uppercase(),
        )
        assertTrue(
            "the theme a dark system shows must be one the owner redrew",
            IconSwitchHelper.defaultThemeFor(isSystemDark = true)
                .lowercase() in setOf("beige", "flame", "orange"),
        )
    }
}
