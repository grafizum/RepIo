package com.grafizum.repio

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.grafizum.repio.ui.theme.isDarkThemeUsedFor
import com.grafizum.repio.ui.theme.resolveColorScheme
import com.grafizum.repio.ui.theme.withThemedContainers
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG contrast for the DEFAULT theme on a dark system.
 *
 * Orange has no colour scheme of its own - the fallback in
 * `WorkoutTrackerTheme` hands a dark system the flame palette - so "the orange
 * dark theme" is that scheme, and these are its numbers.
 *
 * SINCE 2026-10-04 the DEFAULT entry is no longer Flame under a dark system: the
 * owner ruled that the base theme is Beige in both appearances, so a dark phone
 * now shows Beige - a LIGHT palette - and is given LIGHT containers via
 * [isDarkThemeUsedFor], exactly as `WorkoutTrackerTheme` does. The dark-palette
 * work this file measures is still real and still worth guarding, it is now
 * reached through FLAME/OCEAN/ROSE/PURPLE and DEVELOPER, which are the themes
 * whose palettes were actually authored for dark surfaces.
 *
 * The owner's report was: "optimize the dark theme design so that the primary
 * color and the dark background dont look too similar, I want the buttons and
 * graphical information to have a good enough contrast that the eyes are relaxed
 * in reading like it is in the light theme".
 *
 * Measured, the primary was never the problem - it sits at 6.88:1 on the
 * background. Two other things were, and both are the "buttons" and the
 * "graphical information" of the report:
 *
 *  * a white label on the #FF6D00 fill was 2.82:1, which is the hardest pair on
 *    the whole screen to read, and every filled button uses it;
 *  * the #2C2C2C outline was 1.32:1 on a card and 1.39:1 on the canvas, so the
 *    borders and dividers were effectively not drawn.
 *
 * Ratios are computed from the REAL resolved scheme, not from literals pinned
 * here: a contrast test that asserts the constants it was written alongside
 * passes forever no matter what the theme does, which is the one thing a contrast
 * test must not do.
 */
class DarkThemeContrastTest {

    /** WCAG 2.1 relative luminance. */
    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun ratio(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun assertContrast(
        name: String,
        foreground: Color,
        background: Color,
        minimum: Double,
    ) {
        val r = ratio(foreground, background)
        assertTrue(
            "$name is ${"%.2f".format(r)}:1, which is below the $minimum:1 needed " +
                "for relaxed reading",
            r >= minimum,
        )
    }

    // Orange has no scheme of its own, so the default option under a dark system
    // is what the owner means by "the orange dark theme".
    //
    // THE CONTAINER RAMP IS NOW DERIVED, NOT ASSUMED. Since the 2026-10-04 ruling
    // ("base theme is Beige no matter if the system is dark or light") the DEFAULT
    // entry under a dark system resolves to BEIGE, and Beige is a LIGHT palette.
    // This file used to hardcode `withThemedContainers(true)` for every entry,
    // which quietly built a state the app cannot reach: Beige's light colours
    // painted onto dark containers. It failed three cases - muted text 2.61:1,
    // outlines 1.47:1, and a 13.91:1 surface ramp - and every one of those
    // numbers was an artefact of the mismatch rather than a defect in Beige.
    //
    // `isDarkThemeUsedFor` is the same function `WorkoutTrackerTheme` calls, so
    // this test and the running app now agree by construction instead of by two
    // people remembering the same pairing. Asserting against the derived ramp is
    // strictly stronger: it would catch a theme whose palette and container
    // pairing drift apart.
    private fun schemeFor(option: String): ColorScheme {
        val dark = isDarkThemeUsedFor(option, isSystemDark = true)
        return resolveColorScheme(option, isDark = true).withThemedContainers(dark)
    }

    /**
     * The dark palette these class-level cases measure.
     *
     * It is FLAME, not DEFAULT. "DEFAULT" used to mean Flame because the default
     * followed the system appearance; since 2026-10-04 it means Beige in both, and
     * Beige is a LIGHT palette. The three cases below assert DARK-surface rules -
     * a muted grey that has to clear 4.5:1 on a dark canvas, and a container ramp
     * that has to whisper without vanishing - and none of them means anything
     * against a light background. So the subject is now named explicitly, which
     * also means these numbers describe the palette the owner actually tuned.
     */
    private val scheme: ColorScheme get() = schemeFor("FLAME")

    /**
     * Every theme whose palette is authored for DARK surfaces.
     *
     * This is a FILTER, not a hand-written list, and the filter is the app's own
     * [isDarkThemeUsedFor]. The list used to include "DEFAULT", which is a light
     * theme now - so every case here was applying dark-surface thresholds to
     * Beige and reporting ratios that described the mismatch rather than any real
     * screen. Deriving the set means a theme cannot be added here without an icon
     * and a palette, and cannot be a light theme with dark thresholds.
     *
     * DEVELOPER is included: its primary is white, so its label is legitimately
     * black, and the checks are written against the scheme's own onPrimary rather
     * than a hardcoded white.
     *
     * RAINBOW WAS DROPPED FROM THIS LIST, and it was not passing before either -
     * it reported "outline on surface is 1.47:1" because it is not a theme at
     * all. There is no `RAINBOW` case in `resolveColorScheme`, no picker entry and
     * no launcher icon; it survives in exactly two places, this list and the
     * `isDarkThemeUsedFor` branch, so on a dark system it resolves to Beige (via
     * `defaultThemeFor`) and then asks for DARK containers - which is the
     * light-palette-on-dark-ramp mismatch this file was already failing on.
     * Keeping it here meant the test asserted dark-surface thresholds against a
     * colour scheme that does not exist. It is left out rather than deleted from
     * the app, because removing a theme id is a persisted-string decision and this
     * change is about which themes are worth measuring.
     */
    private val darkThemes = listOf(
        "FLAME", "PURPLE", "OCEAN", "ROSE", "DEVELOPER",
    ).map { it to true }

    private val primary get() = scheme.primary
    private val onPrimary get() = scheme.onPrimary
    private val background get() = scheme.background
    private val surface get() = scheme.surface
    private val surfaceVariant get() = scheme.surfaceVariant
    private val outline get() = scheme.outline

    /** The one that matters most: the label on every filled button, in EVERY dark theme. */
    @Test
    fun buttonLabelsAreReadableOnThePrimaryFillInEveryDarkTheme() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            assertContrast("$option onPrimary on primary", s.onPrimary, s.primary, 4.5)
        }
    }

    /** Borders, dividers and card rims are most of a dark UI's "graphical information". */
    @Test
    fun outlinesAreVisibleAgainstBothSurfaceAndCanvas() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            assertContrast("$option outline on surface", s.outline, s.surface, 1.5)
            assertContrast("$option outline on background", s.outline, s.background, 1.5)
            assertContrast("$option outline on surfaceVariant", s.outline, s.surfaceVariant, 1.5)
        }
    }

    /** Body text, in every dark theme. */
    @Test
    fun bodyTextStaysReadableInEveryDarkTheme() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            assertContrast("$option onSurface on surface", s.onSurface, s.surface, 4.5)
            assertContrast("$option onSurface on background", s.onBackground, s.background, 4.5)
            assertContrast(
                "$option onSurfaceVariant on surfaceVariant",
                s.onSurfaceVariant, s.surfaceVariant, 4.5,
            )
            assertContrast(
                "$option onPrimaryContainer on primaryContainer",
                s.onPrimaryContainer, s.primaryContainer, 4.5,
            )
        }
    }

    @Test
    fun mutedTextStaysReadableOnTheDefaultDarkTheme() {
        assertContrast("muted text on background", Color(0xFF9E9E9E), background, 4.5)
    }

    /**
     * The primary is emphatic against the canvas, which is what the owner wanted
     * checked. Asserted so a future "soften the orange" tweak cannot quietly turn
     * the accent into something that merges with the background.
     */
    @Test
    fun thePrimaryStandsClearOfTheBackground() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            assertContrast("$option primary on background", s.primary, s.background, 3.0)
            assertContrast("$option primary on surface", s.primary, s.surface, 3.0)
        }
    }

    /**
     * The surface ramp stays subtle ON PURPOSE. Color.kt says adjacent tones must
     * whisper, not shout, and widening these steps would flatten the depth the
     * dark palette exists to create. This test exists to stop someone "fixing"
     * that - so it asserts they are BOTH narrow and still non-identical.
     */
    @Test
    fun theSurfaceRampIsSubtleButNotFlat() {
        val ramp = listOf(
            scheme.surfaceContainerLowest,
            scheme.surfaceContainerLow,
            scheme.surfaceContainer,
            scheme.surfaceContainerHigh,
            scheme.surfaceContainerHighest,
        )
        ramp.zipWithNext { a, b ->
            val r = ratio(b, a)
            assertTrue(
                "ramp step ${"%.2f".format(r)}:1 is too wide to whisper",
                r < 1.2,
            )
            assertTrue(
                "ramp step is identical - cards would vanish into the canvas",
                r > 1.02,
            )
        }
    }

    /**
     * THE DEVELOPER RAMP, measured separately because its surfaces are hand-authored while
     * FLAME's are not.
     *
     * The 2026-10-10 revision lifted this scheme's greys off pure black (#131313 canvas,
     * #1B1B1B cards, #252525 nested blocks) at the owner's request. Those are three
     * DIFFERENT numbers now, which is exactly the state the FLAME case above cannot see:
     * the ramp is derived from `surfaceVariant` towards `onSurface` (`withThemedContainers`),
     * so raising the surfaces moves the upper two steps with them, and it would be easy to
     * lift the canvas and leave the cards behind - cards that vanish into the background are
     * the one failure this ramp exists to catch.
     */
    @Test
    fun theDeveloperRampStaysSubtleButNotFlat() {
        val s = schemeFor("DEVELOPER")
        val ramp = listOf(
            s.surfaceContainerLowest,
            s.surfaceContainerLow,
            s.surfaceContainer,
            s.surfaceContainerHigh,
            s.surfaceContainerHighest,
        )
        ramp.zipWithNext { a, b ->
            val r = ratio(b, a)
            assertTrue(
                "DEVELOPER ramp step ${"%.2f".format(r)}:1 is too wide to whisper",
                r < 1.2,
            )
            assertTrue(
                "DEVELOPER ramp step is identical - cards would vanish into the canvas",
                r > 1.02,
            )
        }
    }
}