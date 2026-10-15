package com.grafizum.repio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.windowSizeFor
import com.grafizum.repio.ui.screens.GUIDE_DIFFICULTY_EASY_TAG
import com.grafizum.repio.ui.screens.GUIDE_DIFFICULTY_HARD_TAG
import com.grafizum.repio.ui.screens.GUIDE_DIFFICULTY_MEDIUM_TAG
import com.grafizum.repio.ui.screens.LevelSystemGuideExamples
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * THE STARTING LEVEL OF EACH DIFFICULTY, taught in the guide.
 *
 * Owner board task_5f5u1p0x5v1z5p366g4r5455, 2026-10-13: *"With starting difficulty
 * (still in the level instructions) show also the starting level based on what
 * difficulty choose"*. The three options in that card now carry the level they open an
 * exercise on, read from [ProgressionEngine.startingPrescription] - the same call the
 * setup screen makes - so the numbers cannot drift from what the app actually does.
 *
 * The checks are the owner's two halves: the levels must RISE with the difficulty (the
 * property the card is teaching) and they must actually be DRAWN on the options.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class GuideDifficultyLevelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val window = windowSizeFor(widthDp = 411)

    private fun level(difficulty: ProgressionEngine.Difficulty): Int =
        ProgressionEngine.startingPrescription("Barbell Bench Press", difficulty)?.level
            ?: error("the guide's sample exercise must carry a starting prescription")

    private val easy = level(ProgressionEngine.Difficulty.EASY)
    private val medium = level(ProgressionEngine.Difficulty.MEDIUM)
    private val hard = level(ProgressionEngine.Difficulty.HARD)

    /** The property the card exists to teach: a harder start is a higher rung. */
    @Test
    fun aHarderDifficultyStartsOnAHigherLevel() {
        assertTrue("easy $easy, medium $medium", easy < medium)
        assertTrue("medium $medium, hard $hard", medium < hard)
    }

    @Test
    fun theOptionsDrawTheLevelEachDifficultyStartsOn() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalWindowSize provides window) {
                WorkoutTrackerTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LevelSystemGuideExamples()
                    }
                }
            }
        }

        // Each option states ITS OWN level - looked up underneath that option's tag, because
        // other cards in this guide print "Level N" rows too (the easy start of 1 already
        // collides with one), so an unscoped text search would pass on the wrong card.
        listOf(
            GUIDE_DIFFICULTY_EASY_TAG to easy,
            GUIDE_DIFFICULTY_MEDIUM_TAG to medium,
            GUIDE_DIFFICULTY_HARD_TAG to hard
        ).forEach { (tag, startLevel) ->
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
            composeTestRule.onNode(
                hasText("Level $startLevel") and hasAnyAncestor(hasTestTag(tag)),
                useUnmergedTree = true
            ).assertExists()
        }
    }
}
