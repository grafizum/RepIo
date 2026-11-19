package com.grafizum.repio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.grafizum.repio.ui.theme.THEME_CHANGE_MS
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.grafizum.repio.ui.theme.resolveColorScheme
import com.grafizum.repio.ui.theme.withThemedContainers
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A theme change MOVES, and it moves without rebuilding the app.
 *
 * Owner, 2026-10-15: *"add a small transition when changing theme so that its not instant but
 * still fast and snappy"*. Three things are easy to get wrong in a way no screenshot shows, and
 * this class is those three:
 *
 *  - **"not instant" is a frame between the palettes, not a duration.** Asserting that the clock
 *    ran for 220ms would pass on a build that snapped to the new palette on the first frame and
 *    then sat there doing nothing. So the assertion is on the COLOURS: the change is sampled on
 *    every frame it paints, and the middle of that run has to hold values that are neither the
 *    palette the app left nor the one it is arriving at. A snap leaves exactly two samples - old
 *    and new - which is why the count is asserted and not just the endpoint.
 *  - **the roles Material paints for the app move too.** The app names 23 roles in its own code,
 *    but the ones it never names by hand are the ones Material paints - `surfaceContainerHighest`
 *    is the fill of every Card. A scheme where the accent glides and the cards snap is worse than
 *    no animation at all, and it is what animating only the named roles would have produced.
 *  - **it must not be a rebuild.** The tempting implementation - crossfading the tree - animates
 *    the same pixels and throws away every `remember` under it, so a theme change would reset what
 *    the screen was showing. [aThemeChangeKeepsTheStateUnderneath] pins the state OBJECT, which is
 *    the difference between interpolating colours and re-composing the app.
 *
 * The clock is left on autoAdvance, so `waitForIdle` runs the transition to its end - which is
 * the same thing the phone does, and it means these samples are frames that were really painted
 * rather than a hand-cranked clock that Robolectric does not drive.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class ThemeChangeMotionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** The scheme a theme resolves to, through the same two steps the composable uses. */
    private fun target(option: String) =
        resolveColorScheme(option, isDark = false).withThemedContainers(isDark = true)

    @Test
    fun aPaletteSwapIsPaintedFrameByFrameInsteadOfSnapping() {
        val accents = mutableListOf<Color>()
        val cards = mutableListOf<Color>()
        var theme by mutableStateOf("BEIGE")

        composeTestRule.setContent {
            WorkoutTrackerTheme(themeOption = theme) {
                // Read through MaterialTheme, not from the resolver: this is the colour the app
                // actually paints with, so a scheme applied after the animation (or one the
                // animation skipped) fails here.
                accents += MaterialTheme.colorScheme.primary
                cards += MaterialTheme.colorScheme.surfaceContainerHighest
            }
        }
        composeTestRule.waitForIdle()

        val beigeAccent = accents.last()
        val beigeCard = cards.last()
        val flame = target("FLAME")

        theme = "FLAME"
        composeTestRule.waitForIdle()

        assertEquals(
            "the change never arrived at the theme that was picked",
            flame.primary,
            accents.last()
        )
        assertEquals(
            "the Card fill never arrived at the theme that was picked",
            flame.surfaceContainerHighest,
            cards.last()
        )

        // THE MIDDLE OF THE RUN, COUNTED RATHER THAN TIMED.
        //
        // A snap from one palette to the other paints exactly two colours - the old one and the
        // new one - and every frame in between them is the signature of an interpolation. The
        // frames equal to an end are simply not the evidence, so they are filtered out rather than
        // forbidden: the frame on which the target changed still holds the colour it is leaving,
        // which is what `animateColorAsState` starting from the current value looks like.
        val accentMidFrames = accents.filter { it != beigeAccent && it != flame.primary }.distinct()
        val cardMidFrames = cards
            .filter { it != beigeCard && it != flame.surfaceContainerHighest }
            .distinct()

        assertTrue(
            "the accent only ever painted the old palette and the new one, in " +
                "${accents.distinct().size} distinct colours over the whole change - the change " +
                "was instant and nothing was painted in between",
            accentMidFrames.size >= 3
        )
        assertTrue(
            "the Card fill only ever painted the old palette and the new one, so a role Material " +
                "paints was left out of the animation",
            cardMidFrames.size >= 3
        )

        // "FAST and snappy" as arithmetic rather than as taste: the frames this took, at the test
        // clock's 16ms, are the duration the phone will run. A single tween of the wrong length
        // (say two seconds) still passes every assertion above, and is exactly what "not instant"
        // would drift into.
        val paintedMs = accents.size * 16L
        assertTrue(
            "the change painted $paintedMs ms of frames, which is not the ~$THEME_CHANGE_MS ms " +
                "the transition is meant to take",
            paintedMs in THEME_CHANGE_MS / 2..THEME_CHANGE_MS.toLong() * 2
        )
    }

    /**
     * The screens underneath keep their state, which is what says this is a colour interpolation
     * and not a `Crossfade` of two trees.
     *
     * The assertion is on the IDENTITY of a `remember`ed state object rather than on its value: a
     * crossfade would give the incoming tree a fresh `remember` slot, and any object handed to the
     * test could still happen to hold the same number. Same instance, same screen.
     */
    @Test
    fun aThemeChangeKeepsTheStateUnderneath() {
        var theme by mutableStateOf("BEIGE")
        lateinit var counter: MutableState<Int>

        composeTestRule.setContent {
            WorkoutTrackerTheme(themeOption = theme) {
                counter = remember { mutableStateOf(0) }
            }
        }
        composeTestRule.waitForIdle()
        val before = counter
        counter.value = 7

        theme = "OCEAN"
        composeTestRule.waitForIdle()

        assertSame(
            "the theme change re-created the screen's remembered state, so the change is animated " +
                "by rebuilding the tree - every scroll position and open card resets",
            before,
            counter
        )
        assertEquals("the state's value was lost across the theme change", 7, counter.value)
    }
}
