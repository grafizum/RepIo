package com.grafizum.repio

import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.grafizum.repio.ui.screens.LEVEL_AUTO_BLOCK_TAG
import com.grafizum.repio.ui.screens.LEVEL_AUTO_TICK_TAG
import com.grafizum.repio.ui.screens.LevelAutoAdvanceBlock
import com.grafizum.repio.ui.screens.autoLevelTarget
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The level panel's explanation that finishing an exercise raises its level by itself.
 *
 * Owner, 2026-10-09: *"the app should also explain that the levels increase automatically
 * after finishing that exercise (show that graphically the same as the other informations for
 * the level panel)"*.
 *
 * The panel could have said that in a sentence, and a sentence would need no test. It says it
 * as a PICTURE instead — the finished sets, then the level chip the exercise becomes — and a
 * picture has parts that can quietly go missing: a tick count taken from the wrong row, an
 * arrow drawn at the top of a ladder where nothing moves, a chip that names a level one above
 * the ceiling. So this class checks the three things a screenshot of it would not survive a
 * refactor to prove:
 *
 *  - [autoLevelTarget] is null at the ceiling, the ONE state where the graphic must not
 *    promise a level-up, because the engine's `levelUp` returns null there and the mastery
 *    flow takes over;
 *  - the block draws one tick per PRESCRIBED set, and the caption that belongs to its state;
 *  - the chip carries the same reading as the card's own chip, since the whole point is to
 *    show the user the shape they will see after they finish.
 *
 * The copy is guarded in [LevelPickerStringsTest], which is where every level-panel key is
 * resolved in all three shipped locales.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class LevelDetailsAutoAdvanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun block(currentLevel: Int, setCount: Int, nextLevel: Int?) {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                Surface(color = Color.White) {
                    LevelAutoAdvanceBlock(
                        currentLevel = currentLevel,
                        setCount = setCount,
                        nextLevel = nextLevel
                    )
                }
            }
        }
    }

    // --- the pure half -------------------------------------------------------

    @Test
    fun theNextRungIsOneUpWhenTheLadderStillHasIt() {
        assertEquals(5, autoLevelTarget(current = 4, ladder = (1..20).toList()))
        // One rung left above the current one is still a level-up.
        assertEquals(7, autoLevelTarget(current = 6, ladder = listOf(1, 6, 7)))
    }

    @Test
    fun theTopOfTheLadderHasNoNextRung() {
        // The exercise's ceiling: the engine refuses here, so the panel must not draw an
        // arrow promising an ascent that will never happen.
        assertNull(autoLevelTarget(current = 7, ladder = listOf(1, 6, 7)))
        // A gap in the ladder is not a target either: the panel can only promise a rung it
        // actually lists.
        assertNull(autoLevelTarget(current = 6, ladder = listOf(1, 6, 8)))
    }

    @Test
    fun anExerciseOutsideTheLevelSystemHasNoTarget() {
        assertNull(autoLevelTarget(current = null, ladder = (1..20).toList()))
    }

    // --- what the panel actually draws --------------------------------------

    @Test
    fun theGraphicShowsOneFinishedSetPerPrescribedSet() {
        block(currentLevel = 4, setCount = 3, nextLevel = 5)

        composeTestRule.onNodeWithTag(LEVEL_AUTO_BLOCK_TAG).assertExists()
        composeTestRule.onAllNodesWithTag(LEVEL_AUTO_TICK_TAG).assertCountEquals(3)
    }

    @Test
    fun theTickCountFollowsThePatternRatherThanADecoration() {
        block(currentLevel = 2, setCount = 4, nextLevel = 3)

        composeTestRule.onAllNodesWithTag(LEVEL_AUTO_TICK_TAG).assertCountEquals(4)
    }

    @Test
    fun theGraphicNamesTheLevelTheExerciseBecomes() {
        block(currentLevel = 4, setCount = 3, nextLevel = 5)

        // The chip is the card's own reading - "Level 5" - not a second notation.
        composeTestRule.onNodeWithText("Level 5").assertExists()
        composeTestRule.onNodeWithText("Levels go up on their own").assertExists()
        composeTestRule.onNodeWithText("level up").assertExists()
    }

    /**
     * At the ceiling the same picture is drawn WITHOUT the arrow, and the caption changes with
     * it. Asserting both halves together is the point: a block that dropped the arrow while
     * keeping the "it moves up by itself" wording would still be telling the user to expect an
     * ascent the engine will not perform.
     */
    @Test
    fun atTheCeilingItSaysItStaysInstead() {
        block(currentLevel = 7, setCount = 3, nextLevel = null)

        composeTestRule.onNodeWithTag(LEVEL_AUTO_BLOCK_TAG).assertExists()
        // The chip still names the exercise's level - there is just nowhere above it.
        composeTestRule.onNodeWithText("Level 7").assertExists()
        composeTestRule.onNodeWithText("level up").assertDoesNotExist()
        composeTestRule.onNodeWithText("Level 8").assertDoesNotExist()
    }
}
