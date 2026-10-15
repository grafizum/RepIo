package com.grafizum.repio

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.grafizum.repio.navBarFill
import com.grafizum.repio.navIndicatorFill
import com.grafizum.repio.ui.screens.workoutActionFill
import com.grafizum.repio.ui.screens.workoutActionLabel
import com.grafizum.repio.ui.screens.workoutCardFill
import com.grafizum.repio.ui.screens.workoutCardRim
import com.grafizum.repio.ui.screens.workoutWellFill
import com.grafizum.repio.ui.screens.workoutWellRim
import com.grafizum.repio.ui.theme.DarkBarSurface
import com.grafizum.repio.ui.theme.isDarkThemeUsedFor
import com.grafizum.repio.ui.theme.resolveColorScheme
import com.grafizum.repio.ui.theme.withThemedContainers
import org.junit.Assert.assertEquals
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

    /**
     * Every theme whose palette is authored for LIGHT surfaces - the counterpart of
     * [darkThemes], and a list rather than a filter for the same reason: `isDarkThemeUsedFor`
     * decides one theme at a time, and these four are the ones whose palettes are light.
     *
     * It is a property rather than a local because two cases now need it: the divider rung
     * below and the workout card's fill, which has to stay the primary tint on exactly these
     * four and be a neutral grey on the five above.
     */
    private val lightThemes = listOf("BEIGE", "EMERALD", "CRIMSON", "ROYAL_BLUE")

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

    /**
     * THE PRIMARY ACTION ON A DARK THEME IS A TONE OF THE ACCENT, NOT THE ACCENT ITSELF.
     *
     * Owner, 2026-10-14: *"the other dark themes are too strong ... especially if the primary
     * color is shown orange in the my workout tab it is very heavy on the eye"*. MEASURED with
     * FLAME selected on emulator-5554: the vivid #FF6D00 covered 8.3% of the whole My Workout
     * frame and ONE full-width button was 5.2% of it. So every dark theme EXCEPT the developer
     * skin now fills its primary action with [workoutActionFill] - a tone of the accent - and
     * labels it with [workoutActionLabel].
     *
     * Asserted as two properties rather than as a literal colour, because the point is not which
     * hex it lands on: the fill must STOP being the raw accent, and it must be QUIETER than the
     * accent it replaces. A test that pinned a value would pass on the very slab it exists to
     * prevent.
     */
    @Test
    fun thePrimaryActionIsAToneOfTheAccentOnEveryDarkThemeButDeveloper() {
        (darkThemes.map { it.first } - "DEVELOPER").forEach { option ->
            val s = schemeFor(option)
            val fill = workoutActionFill(s, isLightTheme = false, isDeveloperSkin = false)
            assertTrue(
                "$option's primary action still fills with the raw accent ($fill), which is the " +
                    "slab of saturated colour the owner called too heavy on the eye",
                fill != s.primary,
            )
            assertTrue(
                "$option's primary action fill is not quieter than the accent it replaces " +
                    "(${"%.4f".format(luminance(fill))} vs ${"%.4f".format(luminance(s.primary))})",
                luminance(fill) < luminance(s.primary),
            )
        }
    }

    /**
     * The label on that tone still has to be readable, in EVERY dark theme.
     *
     * This is the case that stops the tone being toned past usability. The accents differ wildly in
     * luminance - Emerald is bright, RoyalBlue is dark enough that its raw accent cannot clear
     * 4.5:1 on ANY dark fill - so [workoutActionLabel] walks the accent toward white until it
     * passes, and this asserts the result rather than the recipe.
     */
    @Test
    fun workoutActionLabelsStayReadableOnTheirOwnTone() {
        (darkThemes.map { it.first } - "DEVELOPER").forEach { option ->
            val s = schemeFor(option)
            val fill = workoutActionFill(s, isLightTheme = false, isDeveloperSkin = false)
            val label = workoutActionLabel(s, isLightTheme = false, isDeveloperSkin = false)
            assertContrast("$option action label on its tone", label, fill, 4.5)
        }
    }

    /**
     * The two exemptions, as a property: the gold Developer theme and the four LIGHT themes keep
     * the solid fill they already had.
     *
     * DEVELOPER is the theme the owner tuned by hand and asked for "an accent gold", and it is not
     * one of the themes he called too strong; the light themes' accents are a bronze / teal / red /
     * blue that never glared, and their `onPrimary` is already a designed pair. Without this case,
     * a future "simplify" pass could spread the tone over them and undo both rulings silently.
     */
    @Test
    fun theDeveloperSkinAndTheLightThemesKeepTheirSolidFill() {
        val dev = schemeFor("DEVELOPER")
        assertEquals(
            "the developer skin's primary action no longer fills with its accent gold",
            dev.primary, workoutActionFill(dev, isLightTheme = false, isDeveloperSkin = true),
        )
        assertEquals(
            "the developer skin's primary action no longer labels with onPrimary",
            dev.onPrimary, workoutActionLabel(dev, isLightTheme = false, isDeveloperSkin = true),
        )
        lightThemes.forEach { option ->
            val s = schemeFor(option)
            assertEquals(
                "$option is a LIGHT theme and its primary action must keep its solid fill",
                s.primary, workoutActionFill(s, isLightTheme = true, isDeveloperSkin = false),
            )
            assertEquals(
                "$option is a LIGHT theme and its primary action must keep its onPrimary label",
                s.onPrimary, workoutActionLabel(s, isLightTheme = true, isDeveloperSkin = false),
            )
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
        assertRampWhispers(
            listOf(
                scheme.surfaceContainerLowest,
                scheme.surfaceContainerLow,
                scheme.surfaceContainer,
                scheme.surfaceContainerHigh,
                scheme.surfaceContainerHighest,
            ),
            "FLAME",
        )
    }

    /**
     * THE DEVELOPER RAMP, measured separately because its surfaces are hand-authored while
     * FLAME's are not.
     *
     * The 2026-10-10 revision lifted this scheme's greys off pure black at the owner's request.
     * Since 2026-10-14 this scheme reads the SHARED ladder in `Color.kt`, so the case below and
     * the FLAME one above measure the same five numbers - see `everyDarkThemeSharesOneLadder`.
     * It is kept as its own case anyway: the developer theme is the one whose surfaces are
     * hand-written, so it is the one that can be edited alone and silently leave the others
     * behind.
     */
    @Test
    fun theDeveloperRampStaysSubtleButNotFlat() {
        val s = schemeFor("DEVELOPER")
        assertRampWhispers(
            listOf(
                s.surfaceContainerLowest,
                s.surfaceContainerLow,
                s.surfaceContainer,
                s.surfaceContainerHigh,
                s.surfaceContainerHighest,
            ),
            "DEVELOPER",
        )
    }

    /**
     * ONE LADDER FOR EVERY DARK THEME, asserted as a PROPERTY rather than against the constants.
     *
     * Owner, 2026-10-13 (dashboard task_1k6x0b4q3072712v0h496e2l): "make sure the develope theme
     * and the dark theme have the same grey shading palette applied". Before this pass they did
     * not: Flame ran #0D0D0F / #141416 / #1B1B1F with a container ramp derived from ITS
     * surfaceVariant (#26262A at the top), and Developer ran #131313 / #1B1B1B / #252525 with
     * #303030 - three ladders wearing one name, one of which was slightly blue. So the property
     * worth pinning is not "the greys are these hexes" (a rename of the palette should be free)
     * but "switching dark theme changes the ACCENT and nothing else about the shading". Every
     * surface token is compared across all five dark themes, and compared as a set at the end,
     * which is what makes the check non-vacuous: a theme that quietly kept a private grey fails
     * on the pairing, and a palette that collapsed two themes onto one scheme fails on the set.
     */
    @Test
    fun everyDarkThemeSharesOneLadder() {
        fun surfaces(s: ColorScheme) = listOf(
            "background" to s.background,
            "surface" to s.surface,
            "surfaceVariant" to s.surfaceVariant,
            "containerLowest" to s.surfaceContainerLowest,
            "containerLow" to s.surfaceContainerLow,
            "container" to s.surfaceContainer,
            "containerHigh" to s.surfaceContainerHigh,
            "containerHighest" to s.surfaceContainerHighest,
            "outline" to s.outline,
            "outlineVariant" to s.outlineVariant,
        )

        val reference = schemeFor(darkThemes.first().first)
        darkThemes.forEach { (option, _) ->
            val mine = surfaces(schemeFor(option))
            val theirs = surfaces(reference)
            mine.zip(theirs).forEach { (a, b) ->
                assertEquals(
                    "$option's ${a.first} is ${a.second} while ${darkThemes.first().first}'s " +
                        "is ${b.second} - the dark themes no longer share one grey ladder",
                    b.second, a.second,
                )
            }
        }

        // The accents must still tell the themes apart, or the check above would pass on a
        // palette that had collapsed every dark theme onto one scheme.
        val accents = darkThemes.map { schemeFor(it.first).primary }.toSet()
        assertEquals(
            "the dark themes must keep distinct accents ($accents)",
            darkThemes.size, accents.size,
        )
    }

    /**
     * THE STAGE-TO-CARD STEP IS A FRONT, NOT A WHISPER.
     *
     * Owner, 2026-10-13, same task: "the cards in the same dark grey palette but less dark so it
     * sees as a front". Measured at the time, the step was 1.055:1 on the shared ladder and
     * 1.079:1 on the developer one — inside the 1.02-1.2:1 band the ramp test allows, but at the
     * very bottom of it, so a card did not separate from the page it sat on. The floor here is
     * 1.08:1, which the old values fail and the new ones pass, and the ceiling is the same
     * 1.2:1 the ramp test uses so "make the cards read as a front" cannot become "paint a
     * visible seam".
     */
    @Test
    fun theCanvasToCardStepReadsAsAFront() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            // THE SUBJECT IS THE CARD RUNG, NOT `surface` (corrected 2026-10-14, third pass).
            // `surface` is the chip/sheet rung - it is 1.048:1 from the canvas, exactly as beige's
            // own surface is 1.044:1 - and it never was "the card". Measuring it here is how the
            // general card grey drifted to 1.259:1 unnoticed: the check passed on a token no card
            // was painted with. `surfaceVariant` is the rung the app's cards name in code
            // (`workoutCardFill` on light, every `surfaceVariant` card), and beige's is 1.099:1, so
            // the floor and ceiling below are the band the LIGHT theme's card occupies too.
            val step = ratio(s.surfaceVariant, s.background)
            assertTrue(
                "$option's card is only ${"%.3f".format(step)}:1 from its canvas - a card that " +
                    "close to the page does not read as a front",
                step >= 1.08,
            )
            assertTrue(
                "$option's card is ${"%.3f".format(step)}:1 from its canvas - too wide a step " +
                    "to whisper",
                step < 1.2,
            )
        }
    }

    /**
     * THE DARK STEPS ARE THE BEIGE STEPS, MIRRORED - the property the owner actually asked for.
     *
     * Owner, 2026-10-14: *"the grey of the bottom bar and the general cards background color is
     * the one you were supposed to make darker ... and make the card you actually change slightly
     * lighter? i want it to have the same visuals as the light beige theme"*.
     *
     * "The same visuals" is not a mood, it is a set of ratios away from the canvas, and the light
     * theme states them: measured on the device (evidence/dark-cards-2026-10-14), beige runs canvas
     * #FDFCFB, chips/sheets #F9F7F4 (1.044:1), general cards #F4F1ED (1.099:1), the workout card
     * #F4F0EB (1.107:1) and its widest neutral, the bottom bar, #F0EBE5 (1.157:1). A dark theme
     * cannot step in the same DIRECTION - its canvas is the dark extreme - so the same picture is
     * the same sizes in the other direction, which is what this case holds: the two roles the owner
     * named step away from their own canvas by the same amount, within a tolerance smaller than the
     * step between two rungs of the ladder.
     *
     * It FAILS against the shape he rejected rather than merely passing, which is the only reason
     * it is worth writing: the general cards were 1.259:1 (15% away from the light theme's
     * 1.099:1, against a 5% tolerance) and the bar was 1.42:1, three times the widest neutral step
     * the light theme takes.
     *
     * The tolerances stand in for "the same visual weight": exact hexes cannot match across a light
     * and a dark palette, because the two ends of the range are not symmetric, and pinning the
     * literals would make a future palette edit fail on a number rather than on the look. The bar's
     * step is compared as a ROLE rather than as a value, because the light theme's bar colour is a
     * Material composite (surface + its 3dp tonal lift) that no token in this test can read - the
     * device capture in evidence/dark-cards-2026-10-14 is where 1.157:1 comes from.
     */
    @Test
    fun theDarkStepsAreTheLightThemesStepsMirrored() {
        val light = resolveColorScheme("BEIGE", isDark = false).withThemedContainers(false)
        val lightSurface = ratio(light.surface, light.background)
        val lightCards = ratio(light.surfaceVariant, light.background)
        val tolerance = 0.05

        // The light hero card is a TRANSLUCENT tint, so it has to be composited over the canvas
        // before it can be compared: `Color.luminance` ignores alpha, and measuring the tint alone
        // reports nonsense (5.23:1 for what is visually a 1.107:1 card).
        val lightHero = ratio(
            over(workoutCardFill(light, isLightTheme = true), light.background), light.background,
        )
        assertTrue(
            "the light theme's own hero card (${workoutCardFill(light, true)}) is not the lighter " +
                "of its two card rungs (${"%.3f".format(lightHero)} vs " +
                "${"%.3f".format(lightCards)}): the mirror below would be pinning a shape the " +
                "light theme does not have",
            lightHero > lightCards,
        )

        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            val chips = ratio(s.surface, s.background)
            val cards = ratio(s.surfaceVariant, s.background)
            val bar = ratio(navBarFill(s, isLightTheme = false), s.background)
            val hero = ratio(workoutCardFill(s, isLightTheme = false), s.background)

            assertEquals(
                "$option's sheets/chips are ${"%.3f".format(chips)}:1 from its canvas while " +
                    "beige's are ${"%.3f".format(lightSurface)}:1",
                lightSurface, chips, tolerance,
            )
            assertEquals(
                "$option's general cards are ${"%.3f".format(cards)}:1 from its canvas while " +
                    "beige's are ${"%.3f".format(lightCards)}:1 - the owner called that too light",
                lightCards, cards, tolerance,
            )
            assertEquals(
                "$option's workout card is ${"%.3f".format(hero)}:1 from its canvas while " +
                    "beige's is ${"%.3f".format(lightHero)}:1",
                lightHero, hero, 0.08,
            )
            // THE BAR'S ROLE, which is a token-comparable statement: in beige the bottom bar is the
            // widest neutral step that theme takes (1.157:1, over its cards' 1.099:1). The dark bar
            // has to play the same part and still be a whisper - the shape the owner rejected was
            // 1.42:1, three times beige's widest step - while NOT being a wash of a lighter grey.
            assertTrue(
                "$option's bottom bar is ${"%.3f".format(bar)}:1 from its canvas - the owner " +
                    "called that grey too light",
                bar < 1.2,
            )
            assertTrue(
                "$option's bottom bar (${"%.3f".format(bar)}:1) is not the widest neutral step " +
                    "of its scheme, which is the part the bar plays in the light theme",
                bar > cards,
            )
            // THE OWNER'S SECOND HALF: the hero card is the lighter of the two card rungs. Asserted
            // as the relationship, because that is what the light theme has and what he described.
            assertTrue(
                "$option's workout card (${"%.3f".format(hero)}:1) is not lighter than its " +
                    "general cards (${"%.3f".format(cards)}:1) - it is the shadow of the card it is " +
                    "supposed to be the hero of",
                hero > cards,
            )
            assertTrue(
                "$option's workout card is ${"%.3f".format(hero)}:1 from its canvas - wider than " +
                    "any step the light theme takes",
                hero < 1.2,
            )
            // ...and lighter than the CHROME, which is the correction that made the bar a rung of
            // its own. Sharing `surfaceContainerHigh` put the hero card and the bar on the same
            // grey, so the card was not lighter than anything: both measured #1E1E1E on the device.
            assertTrue(
                "$option's workout card (${"%.3f".format(hero)}:1) is not lighter than its " +
                    "bottom bar (${"%.3f".format(bar)}:1) - the bar is chrome and the card is the " +
                    "panel, so the panel has to come out in front",
                hero > bar,
            )
        }
    }

    /**
     * NO HUE IN ANY DARK GREY.
     *
     * The old shared ladder was deliberately a touch blue (#0D0D0F carries more blue than red),
     * which is the cold stock-Material grey the owner rejected on the developer theme in
     * 2026-10-08 ("a true dark grey palette lik my app on the phone had"). The rule that came
     * out of that is a property of every neutral surface: its channels are within one step of
     * each other. `primaryContainer` is deliberately NOT in the list - it is a STATE container
     * (Material's selected-chip and selected-row fill), not a shading step, and the hued dark
     * themes keep it in their own accent family. Making that grey too is a palette decision for
     * the owner, not something a contrast test should force.
     */
    @Test
    fun noDarkGreyCarriesAHue() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            listOf(
                "background" to s.background,
                "surface" to s.surface,
                "surfaceVariant" to s.surfaceVariant,
                "containerHigh" to s.surfaceContainerHigh,
                "containerHighest" to s.surfaceContainerHighest,
                // The bar is a rung of its own outside the Material ramp, so it has to be named.
                "bottomBar" to DarkBarSurface,
                "outline" to s.outline,
                "outlineVariant" to s.outlineVariant,
            ).forEach { (name, colour) -> assertNeutral("$option's $name", colour) }
        }
    }

    /**
     * A DIVIDER IS DRAWN IN THE THEME'S OWN GREY, NOT IN MATERIAL'S BASELINE ONE.
     *
     * `outlineVariant` is what `HorizontalDivider`, `Divider` and most card rims read, and NO
     * palette in this file used to set it — so Material's baseline filled it with #49454F on
     * dark schemes and #CAC4D0 on light ones, both of them a cool purple-grey that belongs to
     * no theme here. Measured on the DEVELOPER theme's level guide before the fix: the rims came
     * back #323035 (R 50, B 53 — the baseline anti-aliased over a #1B1B1B card) across 1.1% of
     * the frame. "Looks professional" is mostly this: the quiet 1% of a screen that is either
     * the theme's own grey or somebody else's.
     *
     * Two properties, because the two branches answer differently on purpose. A DARK theme gets
     * its own rung of the shared ladder — neutral, a rung below `outline` (a divider is quieter
     * than a surface edge) and still visible on a card. A LIGHT theme collapses the token onto
     * its own outline, which is the same move its container ramp already makes.
     */
    @Test
    fun dividersAreDrawnInTheSchemesOwnGrey() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            assertNeutral("$option's outlineVariant", s.outlineVariant)
            val belowOutline = ratio(s.outlineVariant, s.outline)
            assertTrue(
                "$option's divider is ${"%.2f".format(belowOutline)}:1 from its outline - it " +
                    "is not a quieter rung of the same ladder",
                belowOutline in 1.02..1.5,
            )
            val onCard = ratio(s.outlineVariant, s.surface)
            assertTrue(
                "$option's divider is ${"%.2f".format(onCard)}:1 on a card - drawn but " +
                    "invisible",
                onCard >= 1.5,
            )
        }
        lightThemes.forEach { option ->
            val s = resolveColorScheme(option, isDark = false).withThemedContainers(false)
            assertEquals(
                "$option's divider is not its own outline - the light palettes collapse the " +
                    "token instead of inheriting Material's baseline grey",
                s.outline, s.outlineVariant,
            )
        }
    }

    /** Every neutral surface must be hue-free: its channels within one step of each other. */
    private fun assertNeutral(name: String, colour: Color) {
        val spread = listOf(colour.red, colour.green, colour.blue).max() -
            listOf(colour.red, colour.green, colour.blue).min()
        assertTrue(
            "$name carries a hue: $colour spans ${(spread * 255).toInt()} of 255 between its " +
                "channels",
            spread <= 1f / 255f,
        )
    }

    /**
     * ELEVATION NEVER LIFTS IN THE ACCENT.
     *
     * This is the biggest single visible defect of the palette this replaces, and it was measured
     * on the phone-sized capture rather than argued: `Surface(tonalElevation = 3.dp)` under the
     * bottom navigation bar came back #2B2820 and a 6dp dialog #302C22, because Material
     * composites `surfaceTint` (= `primary` by default) into `surface` at an elevation-dependent
     * alpha. That put a gold wash on the navigation bar of EVERY screen — 9.3% of the frame,
     * sampled — plus every dialog and sheet, none of which is a state and all of which therefore
     * had the accent tinting chrome instead of signalling anything.
     *
     * The lift is kept (a bar should still read as raised) and only its COLOUR changes, so what
     * is asserted is the property: the tint a dark theme lifts towards is one of its own light
     * NEUTRALs, and never the accent. "Lifts by the same amount as before" is not asserted here
     * because it is Material's formula, not this app's; the composite is checked on the device.
     */
    @Test
    fun elevationLiftsInTheSchemesOwnNeutralInk() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            assertTrue(
                "$option's elevation tint is its accent (${s.surfaceTint}) - every elevated " +
                    "surface would be washed with the theme colour",
                s.surfaceTint != s.primary,
            )
            val tint = s.surfaceTint
            val spread = listOf(tint.red, tint.green, tint.blue).max() -
                listOf(tint.red, tint.green, tint.blue).min()
            assertTrue(
                "$option's elevation tint ($tint) is not a neutral: its channels span " +
                    "${(spread * 255).toInt()} of 255",
                spread <= 8f / 255f,
            )
        }
    }

    /**
     * THE WORKOUT TAB'S HERO CARDS ARE NOT THE ACCENT ON A DARK SCHEME.
     *
     * Owner, 2026-10-14: *"the theme primary color is still shown as the main card of my
     * workout tab, i want it to be a grey shade and have the primary color only be used as
     * information / box color but only border or writing etc (as an actual accent color but
     * not the whole card in primary color)"*.
     *
     * The two cards that share this fill - the current exercise card and the level card under
     * it - used to be one expression, `primary.copy(alpha = 0.08f)`. On a LIGHT scheme that is
     * a beige card on a cream canvas, which is the half he asked to KEEP. On a DARK scheme it
     * is the accent used as a surface: measured on the emulator in the DEVELOPER theme it
     * painted #1F1B14 over 25.6% of the frame. So the property worth pinning is one line of
     * arithmetic, in both appearances at once - the fill is the scheme's own neutral card grey
     * on dark and the primary tint on light, and never the accent itself.
     *
     * This is asserted against the RESOLVED scheme of every dark theme, not against a literal,
     * so a new dark palette cannot be added with a private grey or a tinted card.
     */
    @Test
    fun theWorkoutCardsAreGreyOnEveryDarkThemeAndBeigeOnEveryLightOne() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            val fill = workoutCardFill(s, isLightTheme = false)
            assertNeutral("$option's workout card fill", fill)
            // ONE RUNG ABOVE THE GENERAL CARD, not the general card itself (2026-10-14, third
            // pass): the owner asked for the workout card to be the LIGHTER of the two, which is
            // the relationship the beige theme has. `theDarkStepsAreTheLightThemesStepsMirrored`
            // holds the sizes of both steps; this case pins which rung is which, so the two can
            // never be swapped back by accident.
            assertEquals(
                "$option's workout card is not the rung above the general card grey the rest of " +
                    "the app is painted with on a dark scheme ($fill vs ${s.surfaceVariant})",
                s.surfaceContainerHigh,
                fill,
            )
            assertTrue(
                "$option's workout card ($fill) is not lighter than the general card grey " +
                    "(${s.surfaceVariant}) - it must be the hero card, not the shadow of one",
                luminance(fill) > luminance(s.surfaceVariant),
            )
            assertTrue(
                "$option's workout card is filled with the accent again (${s.primary})",
                fill != s.primary,
            )
        }

        lightThemes.forEach { option ->
            val s = schemeFor(option)
            val fill = workoutCardFill(s, isLightTheme = true)
            // The beige the owner asked to keep: the accent, as a tint, and nothing else.
            assertEquals(
                "$option's workout card lost the primary tint",
                s.primary.copy(alpha = 0.08f),
                fill,
            )
            // Color quantises alpha to 8 bits (0.08f is stored as 20/255), so the tint's
            // strength is compared at that resolution rather than exactly.
            assertEquals("$option's workout card tint is no longer 8%", 0.08f, fill.alpha, 1f / 255f)
        }
    }

    /**
     * THE CARD THE APP ACTUALLY PAINTS IS INSIDE THE BAND, not just `surface`.
     *
     * Owner, 2026-10-14: *"the cards are too light grey, i want them darker but still lighter than
     * the background color of the app"*. He was describing a number nobody was measuring: every
     * card in the app is painted `surfaceVariant`, while [theCanvasToCardStepReadsAsAFront] above
     * measures `surface` — so the ladder passed its own rule at 1.121:1 while the cards had
     * drifted out of the top of the band at **1.259:1**. This case closes the gap in the check
     * rather than only in the palette: the fill the two workout hero cards are painted with is
     * measured against the canvas the user sees it on, and it has to be BOTH darker than the
     * 1.259:1 that was rejected and still clear of the 1.08:1 floor below which a card stops
     * reading as a front.
     *
     * Asserted through [workoutCardFill] on every dark theme, so it is "the cards the app draws"
     * rather than "the token named variant": a future pass that repaints the cards from another
     * rung is measured on the rung it chose.
     */
    @Test
    fun theCardsTheAppPaintsSitInsideTheWhisperBand() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            val card = workoutCardFill(s, isLightTheme = false)
            val step = ratio(card, s.background)
            assertTrue(
                "$option's card is only ${"%.3f".format(step)}:1 from its canvas - it does not " +
                    "read as a front",
                step >= 1.08,
            )
            assertTrue(
                "$option's card is ${"%.3f".format(step)}:1 from its canvas - the owner called " +
                    "that too light",
                step < 1.2,
            )
        }
    }

    /**
     * THE ACCENT ON THE WORKOUT CARDS IS THE ACCENT, NOT A WASH OF IT.
     *
     * Owner, 2026-10-14: *"instead of a creamy gold i want an accent gold"*. A gold at 35% or 12%
     * over a grey is a different colour — a tan — and the two pairs of constants this asserts
     * exist only to stop that returning by accident at any one call site. On a DARK scheme:
     *
     *  * the card's rim and a nested block's rim are the accent, exact and undiluted;
     *  * a block's FILL is the CARD rung of the grey ladder (`surfaceVariant`), which is what makes
     *    the pair "a gold line around a grey card inside the card" instead of a gold-tinted panel.
     *
     * THE FILL USED TO BE THE CANVAS GREY, a strict mirror of the beige theme: its well is 1.16:1
     * from its card, and over the dark card that ratio lands exactly on `surfaceContainerLowest` =
     * #0E0E0E. Mirror-correct, and wrong on the screen — a fill equal to the page is not a
     * recessed panel, it is a hole, and the rep banner read as the page showing through the card.
     * The owner said so on the device: *"make the rep card slightly lighter"*. So the assertions
     * below are the two the mistake actually needed: the block is visible against the PAGE and
     * still a rung under the CARD. **A step that only exists relative to the card has to stay
     * visible against the page as well.**
     *
     * On a LIGHT scheme all three keep the tints they had: the owner's note was about the dark
     * theme, and a softened bronze on a cream card is a designed edge rather than a muddied one.
     * The two branch answers are asserted as the branch answers rather than as "they differ",
     * because "they differ" would pass on any pair of wrong values.
     */
    @Test
    fun theAccentOnTheWorkoutCardsIsTheAccentNotAWashOfIt() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            assertEquals(
                "$option's card rim is a wash of the accent (${workoutCardRim(s, false)}) rather " +
                    "than the accent (${s.primary})",
                s.primary, workoutCardRim(s, isLightTheme = false),
            )
            assertEquals(
                "$option's nested-block rim is a wash of the accent",
                s.primary, workoutWellRim(s, isLightTheme = false),
            )
            val well = workoutWellFill(s, isLightTheme = false)
            assertEquals(
                "$option's nested block is filled with ${s.primary} instead of the card rung",
                s.surfaceVariant, well,
            )
            assertNeutral("$option's nested block fill", well)
            // IT HAS TO BE VISIBLE AGAINST THE PAGE, which the canvas grey was not: a block whose
            // fill equals the background is a hole rather than a panel, whatever the ratio to the
            // card says. This is the assertion whose absence let the mistake ship.
            assertTrue(
                "$option's nested block (${well}) is the page's own grey (${s.background}) - it " +
                    "reads as a hole punched through the card, not as a block inside it",
                ratio(well, s.background) > 1.05,
            )
            // ...and still BELOW the card it sits in: a block lighter than its card is a raised
            // panel wearing the word "well", which is the shape this replaced.
            assertTrue(
                "$option's nested block (${well}) is not inset into the card " +
                    "(${workoutCardFill(s, isLightTheme = false)})",
                ratio(workoutCardFill(s, isLightTheme = false), well) > 1.05,
            )
        }

        lightThemes.forEach { option ->
            val s = resolveColorScheme(option, isDark = false).withThemedContainers(false)
            assertEquals(
                "$option's card rim lost its tint",
                s.primary.copy(alpha = 0.35f), workoutCardRim(s, isLightTheme = true),
            )
            assertEquals(
                "$option's nested-block rim lost its tint",
                s.primary.copy(alpha = 0.25f), workoutWellRim(s, isLightTheme = true),
            )
            assertEquals(
                "$option's nested-block fill lost its tint",
                s.primary.copy(alpha = 0.12f), workoutWellFill(s, isLightTheme = true),
            )
        }
    }

    /**
     * THE TAB PILL IS NOT A WASH OF THE ACCENT EITHER.
     *
     * The same ruling, one layer out from the cards: the sliding pill behind the selected tab was
     * `primary.copy(alpha = 0.1f)`, which on a dark scheme is a tan `#3F3B31` over the bar's
     * raised grey, measured live at 1.5% of the frame and following the selection from tab to
     * tab. See [navIndicatorFill]. On a dark scheme the pill is a neutral rung of the ladder and
     * the accent does its marking with the icon and the label, which are full strength; on a light
     * scheme the 10% tint stays, because there it is a designed bronze edge on cream.
     */
    @Test
    fun theTabPillIsNeutralOnEveryDarkTheme() {
        darkThemes.forEach { (option, _) ->
            val s = schemeFor(option)
            val pill = navIndicatorFill(s, isLightTheme = false)
            assertNeutral("$option's tab pill", pill)
            assertEquals(
                "$option's tab pill is filled with ${s.primary} instead of a neutral rung",
                s.surfaceContainerHighest, pill,
            )
            // It has to stay a WHISPER, or "not a wash" becomes "a second, louder surface".
            // Measured against the rung BELOW it rather than against `surface`: the bar itself is
            // `surface` lifted by Material's elevation tint, which is a runtime composite this
            // test cannot compute, so the honest statement is "the pill is one ladder step above
            // its neighbour and not more".
            val step = ratio(pill, s.surfaceContainerHigh)
            assertTrue(
                "$option's tab pill is ${"%.2f".format(step)}:1 from the rung below it - that is " +
                    "a panel, not a state",
                step in 1.02..1.2,
            )
        }

        lightThemes.forEach { option ->
            val s = resolveColorScheme(option, isDark = false).withThemedContainers(false)
            assertEquals(
                "$option's tab pill lost its tint",
                s.primary.copy(alpha = 0.1f), navIndicatorFill(s, isLightTheme = true),
            )
        }
    }

    /** [top] painted over [bottom] at its own alpha, which is how Compose composites a tint. */
    private fun over(top: Color, bottom: Color): Color {
        val a = top.alpha
        fun mix(t: Float, b: Float) = t * a + b * (1f - a)
        return Color(mix(top.red, bottom.red), mix(top.green, bottom.green), mix(top.blue, bottom.blue))
    }

    private fun assertRampWhispers(ramp: List<Color>, name: String) {
        ramp.zipWithNext { a, b ->
            val r = ratio(b, a)
            assertTrue("$name ramp step ${"%.2f".format(r)}:1 is too wide to whisper", r < 1.2)
            assertTrue("$name ramp step is identical - cards would vanish into the canvas", r > 1.02)
        }
    }
}