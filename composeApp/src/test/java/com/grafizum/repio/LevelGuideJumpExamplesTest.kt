package com.grafizum.repio

import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.ProgressionEntry
import com.grafizum.repio.ui.screens.LevelSystemGuideContent
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The two jump cards must not be the same picture twice.
 *
 * OWNER REPORT, 2026-10-13: *"Between (level up and rep reset) and (big jumps smaller reps),
 * there is no real rep difference shown i want it displayed differently."*
 *
 * He was reading it right. The level-up card printed no load at all — so its caption's
 * "with more load" pointed at nothing — and then the big-jump card printed the same
 * "12 / 12 / 12" -> "8 / 8 / 8" underneath, which made the second card look like a copy of
 * the first. Both cards now draw an ordinary step and a big one respectively, every number
 * read from [ProgressionEngine] (the call the workout screen itself makes), so the two can
 * be told apart at a glance and neither can drift from the data it illustrates.
 *
 * The reset REPS really are the same on both cards, and that is the data's own answer, not
 * a drawing choice: every row of the v5 pool declares post-jump reps of 8 / 8 / 8, which is
 * exactly what the ordinary reset already uses. [theResetRepsAreTheDatasOwnAnswer] pins that
 * fact, so if the pool ever authors a genuinely lighter post-jump rung the test fails and
 * says to re-cut the card instead of quietly leaving the guide claiming something new.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class LevelGuideJumpExamplesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private companion object {
        const val LEVEL_UP_EXERCISE = "Barbell Bench Press"
        const val BIG_JUMP_EXERCISE = "Concentration Curl"
        /** The bench's ordinary step is read at its third rung... */
        const val RUNG = 3
        /** ...and the big jump at the bottom of the light dumbbell's ladder. */
        const val BIG_JUMP_RUNG = 1
    }

    private data class Step(val load: Double, val reps: List<Int>)

    /** The step the ENGINE makes off [rung]: the rung's last stage, and the level-up out of it. */
    private fun steps(exerciseName: String, rung: Int): Pair<Step, Step> {
        val entry = ProgressionEngine.entryFor(exerciseName)!!
        val top = ProgressionEngine.patternLength(entry)
        val first = Step(
            load = ProgressionEngine.valueAtLevel(entry, rung)!!,
            reps = ProgressionEngine.prescribedReps(entry, rung, top)!!
        )
        val up = ProgressionEngine.levelUp(
            entry,
            ProgressionEngine.ProgressionState(level = rung, stage = top)
        )!!
        val second = Step(
            load = up.value!!,
            reps = up.reps
        )
        return first to second
    }

    private fun renderGuide() {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                Surface(color = Color.White) { LevelSystemGuideContent() }
            }
        }
    }

    /**
     * The two cards are the engine's own arithmetic — the ordinary step and the big one —
     * and the second really is the bigger step of the two.
     */
    @Test
    fun theTwoJumpCardsShowTheEnginesOwnSteps() {
        val (plainFrom, plainTo) = steps(LEVEL_UP_EXERCISE, RUNG)
        val (bigFrom, bigTo) = steps(BIG_JUMP_EXERCISE, BIG_JUMP_RUNG)

        // The ordinary step is the smaller one — otherwise the second card would be
        // teaching the first card's case with different numbers.
        val plainPercent = (plainTo.load - plainFrom.load) / plainFrom.load
        val bigPercent = (bigTo.load - bigFrom.load) / bigFrom.load
        assertTrue(
            "the big-jump card must jump further than the level-up card " +
                "(${plainPercent * 100}% vs ${bigPercent * 100}%)",
            bigPercent > plainPercent
        )
        // ...and the engine agrees it is a big one, which is what the card's title claims.
        val bigEntry = ProgressionEngine.entryFor(BIG_JUMP_EXERCISE)!!
        assertEquals("large", ProgressionEngine.jumpClassForTransition(bigEntry, BIG_JUMP_RUNG))

        // The two cards cannot be the same picture: the loads they print differ.
        assertNotEquals(plainFrom.load to plainTo.load, bigFrom.load to bigTo.load)
        assertEquals(25.0, plainFrom.load, 0.001)
        assertEquals(27.5, plainTo.load, 0.001)
        assertEquals(2.0, bigFrom.load, 0.001)
        assertEquals(3.0, bigTo.load, 0.001)
    }

    /** What the reader sees: both cards' loads, level boxes and reps, from the engine. */
    @Test
    fun theGuidePrintsBothSteps() {
        renderGuide()

        // The level-up card's own pair, load and all — the load is what the old card left
        // out, which is why its caption's "with more load" was invisible.
        composeTestRule.onNodeWithText("25 kg — Level 3 | XIII").assertExists()
        composeTestRule.onNodeWithText("27.5 kg — Level 4 | I").assertExists()

        // The big-jump card's pair.
        composeTestRule.onNodeWithText("2 kg — Level 1 | XIII").assertExists()
        composeTestRule.onNodeWithText("3 kg — Level 2 | I").assertExists()

        // Both cards' reps are on screen (the reset appears on each card and on the max
        // card, so the count is what is worth pinning, not the presence).
        composeTestRule.onAllNodesWithText("12 / 12 / 12").assertCountEquals(3)
    }

    /**
     * The shared reset is the DATA's answer: the pool's post-jump reps are the standard
     * reset's own numbers, so no card can honestly print a difference the app does not make.
     */
    @Test
    fun theResetRepsAreTheDatasOwnAnswer() {
        for (name in listOf(LEVEL_UP_EXERCISE, BIG_JUMP_EXERCISE)) {
            val entry: ProgressionEntry = ProgressionEngine.entryFor(name)!!
            val standard = ProgressionEngine.romanStageReps(entry, 1)
            assertEquals(
                "$name: the pool declares post-jump reps (${entry.postJumpReps}) that differ " +
                    "from the ordinary reset ($standard) — the big-jump card is now allowed to " +
                    "show fewer reps than the level-up card, so re-cut it to do so",
                standard,
                entry.postJumpReps
            )
            assertEquals(listOf(8, 8, 8), standard)
        }
    }
}
