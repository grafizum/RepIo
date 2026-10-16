package com.grafizum.repio

import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.ui.components.EditorLevelStage
import com.grafizum.repio.ui.screens.LevelSystemGuideContent
import com.grafizum.repio.ui.screens.roundedStages
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * "What is a stage?" must show the stages the app actually shows.
 *
 * OWNER REPORT, 2026-10-09: *"it shows \"I\" and right after \"II\" although the display
 * should be from \"I\" to \"IV\" since those are the only ones that have the same reps for
 * each set"*.
 *
 * The guide drew the RAW pattern — I 8/8/8, II 9/8/8, III 9/9/8 — which is one rep moving on
 * one set at a time. The exercise editor had already stopped listing those rows on the same
 * day's ruling ("round them up so each number shown is the same reps"), so the guide was
 * left as the only surface teaching a ladder no other screen offers: a reader would look for
 * a "II" that is not in the list they can actually pick from.
 *
 * This class pins the fix from both ends:
 *
 *  - the example's two rows are the engine's OWN first two rounded stages of the standard
 *    pattern, so the illustrative numbers cannot drift from the data they illustrate;
 *  - the composed guide contains no standalone mixed numeral, which is the property the
 *    owner saw violated.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class LevelGuideStageDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderGuide() {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                Surface(color = Color.White) { LevelSystemGuideContent() }
            }
        }
    }

    /**
     * The guide's illustrative pair, against the engine.
     *
     * Glute Bridge is the representative row: it carries the standard three-set pattern
     * (8/8/8 climbing to 12/12/12) that every example in the guide quotes.
     *
     * READ AT THE RUNG A MEDIUM USER STARTS ON, not at level 1. The guide teaches the stage mechanic
     * with the numbers a user actually sees, and since the owner's 2026-10-16 rep-floor ruling the
     * bodyweight ladders open on a warm-up ramp (1/4/8/12) - so level 1 IS one rep per set now, and
     * a guide built on it would teach "I 1/1/1 -> IV 1/1/1". The MEDIUM start is the same eight reps
     * this card has always shown (the ramp put three rungs underneath the number that used to be
     * level 1), so the card's pixels do not move; only the rung it is derived from does.
     */
    @Test
    fun theExamplesRowsAreTheEnginesOwnFirstTwoRoundedStages() {
        val entry = ProgressionEngine.entryFor("Glute Bridge")!!
        val level = ProgressionEngine.startingPrescription(
            "Glute Bridge", ProgressionEngine.Difficulty.MEDIUM
        )!!.level
        val stages = ProgressionEngine.patternLength(entry)
        val all = (1..stages).map { stage ->
            EditorLevelStage(
                stage = stage,
                roman = ProgressionEngine.stageName(stage),
                reps = ProgressionEngine.prescribedReps(entry, level, stage).orEmpty()
            )
        }
        val shown = roundedStages(all)

        // The two rows the guide draws are I 8/8/8 and IV 9/9/9 - not two adjacent stages.
        assertEquals(listOf(1, 4), shown.take(2).map { it.stage })
        assertEquals(listOf(8, 8, 8), shown[0].reps)
        assertEquals(listOf(9, 9, 9), shown[1].reps)

        // And the rule that produced them is the one the owner stated: every set alike.
        assertTrue(
            "the shown rows must ask the same reps of every set",
            shown.all { it.reps.distinct().size <= 1 }
        )
        // The raw neighbours really are mixed - otherwise this test would be pinning a
        // distinction that does not exist.
        val rawSecond = ProgressionEngine.prescribedReps(entry, level, 2).orEmpty()
        assertEquals(
            "stage II is a mixed row (${rawSecond.joinToString("/")}), which is why it is not shown",
            2,
            rawSecond.distinct().size
        )
    }

    @Test
    fun theGuideDrawsTheRoundedStagesAndNeverTheMixedOnes() {
        renderGuide()

        // The example's own two rows exist. "I" is checked by count because every other
        // stage reading on the page is inside a "Level N | XIII" line rather than a box of
        // its own, and "9 / 9 / 9" is the second row's reps - a string no other example
        // quotes (they use 12 / 12 / 12 and 8 / 8 / 8).
        composeTestRule.onAllNodesWithText("I").assertCountEquals(1)
        composeTestRule.onNodeWithText("9 / 9 / 9").assertExists()

        // ...and the rows the owner objected to are gone from the whole guide. A standalone
        // "II" or "III" can only come from this example: every other stage reading on the
        // page is part of a "Level N | XIII" line, not a box of its own.
        composeTestRule.onNodeWithText("II").assertDoesNotExist()
        composeTestRule.onNodeWithText("III").assertDoesNotExist()
    }
}
