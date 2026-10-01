package com.grafizum.repio

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
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
    private val scheme: ColorScheme =
        resolveColorScheme(themeOption = "DEFAULT", isDark = true).withThemedContainers(true)

    /**
     * Every dark theme, plus the two entries that reach a dark palette by
     * another route: DEFAULT (orange, which has no scheme of its own) and
     * DEVELOPER (whose primary is white, so its label is legitimately black).
     *
     * The fix was applied to all of them after the owner saw the default one and
     * said "apply it to the other dark themes too". Asserting each one matters
     * because the failure was NOT uniform before: white on a saturated primary
     * ranged from 2.38:1 (ocean) to 3.14:1 (purple), so a single shared check
     * could have passed one theme while another stayed unreadable.
     */
    private val darkThemes = listOf(
        "DEFAULT" to true,
        "FLAME" to true,
        "PURPLE" to true,
        "OCEAN" to true,
        "ROSE" to true,
        "RAINBOW" to true,
        "DEVELOPER" to true,
    )

    private fun schemeFor(option: String): ColorScheme =
        resolveColorScheme(option, isDark = true).withThemedContainers(true)

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
}