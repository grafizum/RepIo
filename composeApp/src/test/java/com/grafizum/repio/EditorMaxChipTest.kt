package com.grafizum.repio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.grafizum.repio.ui.components.EditorLevelOption
import com.grafizum.repio.ui.components.LevelPickerCard
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.windowSizeFor
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * THE "max" MARK ON THE EDITOR'S LEVEL LIST.
 *
 * Owner ruling 2026-10-13: keep each exercise's max as it is, add more levels after it, and
 * give the max rung *"a small max label so it is distinguishable"*. The engine half of that
 * (the rung the researched ceiling lands on is flagged `isMax`, the levels above it are real
 * and selectable) is pinned by LevelLadderTest; what this case pins is the half the user
 * actually sees - the chip is drawn on the marked rung and on no other - which no pure
 * function can show.
 *
 * It renders the real card at the phone's width, so a future edit that drops the chip, moves
 * it onto every row or loses it under a larger font scale fails here rather than on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class EditorMaxChipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val window = windowSizeFor(widthDp = 411)

    /** Five rungs with the max on the third, exactly as the host builds them. */
    private fun render(maxLevel: Int = 3, rows: IntRange = 1..5) {
        val options = rows.map { level ->
            EditorLevelOption(
                level = level,
                isCurrent = level == 1,
                loadText = "20 kg",
                isMax = level == maxLevel
            )
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalWindowSize provides window) {
                WorkoutTrackerTheme {
                    Box(modifier = Modifier.fillMaxSize().testTag("editor_host")) {
                        LevelPickerCard(
                            options = options,
                            selectedLevel = 1,
                            onSelect = {},
                            onOpenLadder = null,
                            pageRange = rows.first..rows.last,
                            totalLevels = rows.last
                        )
                    }
                }
            }
        }
    }

    @Test
    fun theMarkedRungCarriesTheChipAndTheOthersDoNot() {
        render(maxLevel = 3)

        // Exactly ONE chip on the list, not one per row...
        composeTestRule.onAllNodesWithTag("editor_level_max_chip", useUnmergedTree = true)
            .assertCountEquals(1)

        // ...on the marked rung...
        composeTestRule.onNode(
            hasTestTag("editor_level_row_3") and hasAnyDescendant(hasTestTag("editor_level_max_chip")),
            useUnmergedTree = true
        ).assertExists()

        // ...and not on the rows beside it.
        for (level in listOf(1, 2, 4, 5)) {
            composeTestRule.onNode(
                hasTestTag("editor_level_row_$level") and
                    hasAnyDescendant(hasTestTag("editor_level_max_chip")),
                useUnmergedTree = true
            ).assertDoesNotExist()
        }

        // The chip says what the marker MEANS, so it is readable without the guide: the label
        // is the shared level_max string, which is "max" in the base locale.
        composeTestRule.onNodeWithText("max", useUnmergedTree = true).assertExists()
    }

    /** A list whose max is off-screen carries no chip at all - the mark is per rung. */
    @Test
    fun aRowWithoutTheMaxDrawsNoChip() {
        render(maxLevel = 9)
        composeTestRule.onAllNodesWithTag("editor_level_max_chip", useUnmergedTree = true)
            .assertCountEquals(0)
    }
}
