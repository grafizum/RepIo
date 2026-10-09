package com.grafizum.repio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grafizum.repio.ui.components.LEVEL_STAGE_DIVIDER_TAG
import com.grafizum.repio.ui.components.LevelStageLabel
import com.grafizum.repio.ui.components.stageDividerSpec
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * The level box's reading, MEASURED through a real composition rather than asserted in the
 * abstract.
 *
 * [stageDividerSpec] says the bar is 1dp wide and 14dp tall, and [LevelEditControlsTest]
 * pins those two numbers — but a spec is a promise about what a composable SHOULD draw. The
 * owner's complaint was about what the phone actually showed (*"make the line that separates
 * level with roman number, thinner so it doesnt look like a number or letter"*), so this
 * class composes [LevelStageLabel] and asks the composed tree three things:
 *
 *  - the two halves are two texts, and the `|` CHARACTER is not among them any more;
 *  - the bar between them is one node, laid out one hairline wide and 14dp tall, sitting in
 *    the gap between the two texts - that is the difference between a rule and a glyph;
 *  - the `trailing` slot still composes (the card's stage dots ride in it).
 *
 * The last one is not decoration. The card's stage dots moved from a hand-written block
 * inside the card into that slot, and a slot that quietly stopped being invoked would show
 * up only as missing dots on a phone — the one thing no spec can answer.
 *
 * The pixels themselves are checked on the device (three screenshots in `evidence/verify-label`)
 * rather than here: Compose's `captureToImage()` needs a real window, which Robolectric does
 * not provide. What this test adds is the part a screenshot cannot: that it is still true
 * after the next edit.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class LevelStageLabelRenderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun label(withTrailing: Boolean = false) {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                Surface(color = Color.White) {
                    LevelStageLabel(
                        level = 10,
                        stage = 4,
                        style = MaterialTheme.typography.labelLarge
                            .copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        trailing = if (!withTrailing) null else {
                            {
                                // A probe standing in for the card's stage dots: same size, same
                                // place in the row, and tagged so its absence is a failure
                                // rather than a missing decoration nobody notices.
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .testTag(TRAILING_TAG)
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    @Test
    fun theReadingIsTwoTextsAndNotOneStringWithAPipeInIt() {
        label()

        composeTestRule.onNodeWithText("Level 10").assertExists()
        composeTestRule.onNodeWithText("IV").assertExists()
        // The glyph the owner rejected as "looking like a number or letter" is gone from the
        // tree outright, which is the structural half of the fix: no text node can carry it.
        composeTestRule.onNodeWithText("Level 10 | IV").assertDoesNotExist()
        composeTestRule.onNodeWithText("Level 10 | IV", substring = true).assertDoesNotExist()
    }

    /**
     * The bar is in the gap, and its measured box is the hairline the spec names.
     *
     * A width assertion is the one that matters: the pipe glyph was as TALL as the numerals
     * around it (that is what made it read as another numeral), and the only thing that
     * separates the two is that this mark is a fraction of a character's width. So the
     * tolerance here is tight on purpose — under two dp — while the height may drift.
     */
    @Test
    fun theBarBetweenThemIsAOneDpHairlineAndNotAGlyph() {
        label()

        val density = composeTestRule.density.density
        val divider = composeTestRule.onNodeWithTag(LEVEL_STAGE_DIVIDER_TAG)
            .fetchSemanticsNode().boundsInRoot
        val left = composeTestRule.onNodeWithText("Level 10").fetchSemanticsNode().boundsInRoot
        val right = composeTestRule.onNodeWithText("IV").fetchSemanticsNode().boundsInRoot

        // boundsInRoot is in pixels, so dp is recovered with the composition's own density.
        val widthDp = divider.width / density
        val heightDp = divider.height / density
        val spec = stageDividerSpec()

        assertTrue(
            "the bar laid out ${"%.2f".format(widthDp)}dp wide (${divider.width}px at " +
                "${"%.2f".format(density)}x); the owner asked for the line to be thinner so it " +
                "stops reading as a number or letter (spec: ${spec.widthDp}dp)",
            abs(widthDp - spec.widthDp) <= 0.9f
        )
        assertTrue(
            "the bar laid out ${"%.2f".format(heightDp)}dp tall against the spec's " +
                "${spec.heightDp}dp — a mark taller than its rule is how the glyph started " +
                "reading as a numeral",
            abs(heightDp - spec.heightDp) <= 1.5f
        )
        assertTrue(
            "the bar is not joining the two halves: it sits at " +
                "${"%.1f".format(divider.left)}..${"%.1f".format(divider.right)} while the " +
                "level ends at ${"%.1f".format(left.right)} and the stage begins at " +
                "${"%.1f".format(right.left)}",
            divider.left >= left.right - 1f && divider.right <= right.left + 1f
        )
    }

    @Test
    fun theTrailingSlotStillComposes() {
        label(withTrailing = true)

        // The card's stage dots live in this slot. If the slot stopped being invoked, a level
        // with a short lattice would simply be missing its dots on the phone.
        composeTestRule.onNodeWithTag(TRAILING_TAG).assertExists()
    }

    private companion object {
        const val TRAILING_TAG = "level_stage_label_trailing_probe"
    }
}
