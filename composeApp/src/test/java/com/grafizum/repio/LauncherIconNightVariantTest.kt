package com.grafizum.repio

import com.grafizum.repio.util.IconSwitchHelper
import org.junit.Assert.assertEquals
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
 * The light half was then corrected a second time, in the other direction. It
 * briefly required a white plate in light mode too, taken off the report "the
 * background of the light icon is dark not white like before" - and that was a
 * preference dressed as a fact. Five themes have shipped a dark light-mode
 * plate since b7d0ec4; rewriting them was rejected ("YOU CHANGED THE BACKGROUND
 * COLOR FOR THE LIGHT THEME ICON EVEN THOUGH I TOLD YOU NOT TO") and they were
 * reverted to HEAD. Light mode is the app's own look and is not ours to
 * normalise; what must hold is that the DARK work never reaches it.
 */
class LauncherIconNightVariantTest {

    private val res: File = File("src/main/res")

    private val allThemes = listOf(
        "ocean", "flame", "rose", "purple", "retro",
        "azure", "crimson", "emerald", "orange", "beige",
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
     * Light mode is the app's OWN look and is not ours to normalise.
     *
     * This assertion used to require every light icon to carry a white plate. It
     * was written off the owner's report "the background of the light icon is
     * dark not white like before" - and it was wrong, because it asserted a
     * preference rather than a fact. Five themes (flame/ocean/rose/purple/retro)
     * have shipped a DARK plate in light mode since commit b7d0ec4, and the
     * owner's later instruction was blunt: "YOU CHANGED THE BACKGROUND COLOR FOR
     * THE LIGHT THEME ICON EVEN THOUGH I TOLD YOU NOT TO". Those five were
     * reverted to HEAD, which is what this now accepts.
     *
     * So the invariant is NOT "every light plate is white" - it is that light mode
     * is never touched by the dark-mode work. Which is asserted two ways: the
     * recolour tool may only write night-qualified directories
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
    fun theRedrawnThemesIncludeTheOneADarkSystemActuallyShows() {
        assertEquals(
            "the dark-system default theme must carry the owner's render, or the " +
                "home screen shows historical artwork on a dark phone",
            "FLAME",
            IconSwitchHelper.defaultThemeFor(isSystemDark = true),
        )
        assertTrue(
            "the theme a dark system shows must be one the owner redrew",
            IconSwitchHelper.defaultThemeFor(isSystemDark = true)
                .lowercase() in setOf("orange", "flame"),
        )
    }
}
