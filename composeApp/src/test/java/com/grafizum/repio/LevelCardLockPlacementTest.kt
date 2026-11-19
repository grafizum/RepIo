package com.grafizum.repio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.windowSizeFor
import com.grafizum.repio.ui.screens.LevelPrescriptionCard
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
 * WHERE THE LEVEL LOCK CHIP SITS IN THE CARD'S HEADER ROW, measured on the real card.
 *
 * Owner, 2026-10-09: *"the level lock button is a bit too centered in my opinion, it needs to
 * be better positioned and maybe also changed the style slightly so it fits with the rest of
 * the content in that card"*, then, on being asked where it should go: *"make it far right
 * together with the pencil but also make sure that the user can distinguish them and make it
 * look good"*.
 *
 * That first pass made the chip and the pencil their own row so the weight spacer could carry
 * the pair to the edge - and the owner came back on 2026-10-11 with the same complaint in
 * sharper words: *"change the positioning a bit of the level lock to the right so that it
 * doesnt look like its randomly floating and not anchored somehow or aligned somewhere"*.
 *
 * WHAT WAS STILL WRONG, as a number. Measured on the phone (SM-S906B, 1080px @ 420dpi), the
 * chip's ring ended at x=852 while the card's content column - the line the body's values
 * (`27.5 kg`, `8 / 8 / 8 / 8 Reps`) and `NEXT UP` end on - is x=983. So the chip's right edge
 * finished 50dp short of the one line the card is built on, 112dp of empty card sat behind its
 * left edge, and 25dp of the pencil's INVISIBLE padding sat in front of it. Reaching the right
 * edge is not the same as being anchored to it: parked left of the pencil, the chip could only
 * ever land on the pencil's padding, never on the card.
 *
 * So the ORDER changed - pencil first, chip last - which is what this test pins down. The
 * numbers it prints are dp, converted from the layout's own pixels, so an assertion means the
 * same thing on any qualifier, and it measures the pencil's GLYPH as well as its 48dp target:
 * the target's own layout gap was already 2dp when the owner reported 25dp of air, so the
 * target alone cannot prove the cluster reads as one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class LevelCardLockPlacementTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** The card is rendered at a phone width, exactly as the workout screen does. */
    private val window = windowSizeFor(widthDp = 411)

    /**
     * The row's own spacing between the pencil and the chip. It is deliberately not the
     * header's 8dp: the pencil's 48dp target is mostly invisible padding, so this is the only
     * gap the pair needs on top of the pencil's own +6dp shift toward the chip.
     */
    private val pairSpacingDp = 2f

    /** Renders the card at a phone width, exactly as the workout screen does. */
    private fun renderCard(locked: Boolean) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalWindowSize provides window) {
                WorkoutTrackerTheme {
                    Box(modifier = Modifier.fillMaxSize().testTag("card_host")) {
                        LevelPrescriptionCard(
                            exerciseName = "Bench Press",
                            level = 10,
                            stage = 4,
                            lastStage = 13,
                            adjustable = true,
                            locked = locked,
                            targetSets = 3,
                            repTargets = listOf(10, 10, 10),
                            currentSetNumber = 2,
                            fallbackReps = "10",
                            primaryText = null,
                            loadText = "60 kg",
                            isTimeBased = false,
                            onOpenEditor = {},
                            onOpenDetails = {},
                            onBlockedTap = {},
                            onToggleLock = {},
                            progressionAvailable = false,
                            progressionTarget = null
                        )
                    }
                }
            }
        }
    }

    private fun Density.dp(px: Float) = px / density

    /**
     * [left, right] edges of a tagged control, in dp, in root coordinates.
     *
     * `unmerged` is for the pencil's GLYPH: it is a child of a clickable box, so its semantics
     * are merged into the target and it only exists as a node of its own in the unmerged tree.
     */
    private fun horizontalBounds(tag: String, unmerged: Boolean = false): Pair<Float, Float> {
        val node = if (unmerged) {
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
        } else {
            composeTestRule.onNodeWithTag(tag)
        }
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val density = composeTestRule.density
        return with(density) { dp(bounds.left) to dp(bounds.right) }
    }

    private val rootRightDp: Float
        get() = with(composeTestRule.density) {
            dp(composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot.right)
        }

    /** The card's own inner right edge: the column its content is built on. */
    private val contentRightDp: Float
        get() = rootRightDp - window.cardPadding.value

    /**
     * THE FIX, and the measurement that proves it.
     *
     *   - the chip's RIGHT edge lands on the card's content column, which is the "anchored
     *     somehow or aligned somewhere" the owner asked for - previously 50dp short of it;
     *   - the pencil sits BEHIND it, separated by exactly the row's own spacing, so the pair
     *     is still one cluster at the edge rather than two controls that drifted apart;
     *   - the air the eye actually reads between the glyph and the chip's ring stays a cluster
     *     gap (4..12dp), which is the part the target's bounds cannot show;
     *   - and the chip is past the card's midline, which is the 2026-10-09 complaint stated as
     *     a number.
     */
    private fun assertTheChipClosesTheHeader(locked: Boolean) {
        val (chipLeft, chipRight) = horizontalBounds("level_lock_toggle")
        val (glyphLeft, glyphRight) = horizontalBounds("level_edit_glyph", unmerged = true)
        val (pencilLeft, pencilRight) = horizontalBounds("level_edit")
        val contentRight = contentRightDp

        val airDp = chipLeft - glyphRight
        println(
            "locked=$locked: chip [$chipLeft, $chipRight] (${chipRight - chipLeft}dp wide), " +
                "pencil glyph [$glyphLeft, $glyphRight] in target [$pencilLeft, $pencilRight], " +
                "air glyph->ring ${airDp}dp, chip right edge ${contentRight - chipRight}dp from " +
                "the card's content column ($contentRight), " +
                "chip left at ${"%.0f".format(100f * chipLeft / rootRightDp)}% of the card"
        )

        assertEquals(
            "locked=$locked: the chip must END the row on the card's own content column, not on " +
                "the pencil's invisible padding - its right edge is " +
                "${contentRight - chipRight}dp short of $contentRight, so nothing on the card " +
                "aligns with it",
            contentRight,
            chipRight,
            1.5f
        )
        assertTrue(
            "locked=$locked: the pencil is behind the chip, so the chip starts after the " +
                "pencil's target ends (chip left $chipLeft, pencil target right $pencilRight)",
            chipLeft >= pencilRight
        )
        assertEquals(
            "locked=$locked: only the row's own spacing may separate the pair",
            pairSpacingDp,
            chipLeft - pencilRight,
            1.5f
        )
        assertTrue(
            "locked=$locked: the visible air between the pencil's glyph and the chip's ring is " +
                "${airDp}dp - a cluster gap, not the 20dp+ the eye read as two stray controls",
            airDp in 4f..12f
        )
        assertTrue(
            "locked=$locked: the chip must not sit on the card's midline - its left edge is " +
                "at ${100f * chipLeft / rootRightDp}% of the width",
            chipLeft > rootRightDp / 2f
        )
    }

    @Test
    fun theChipClosesTheHeaderOnTheCardsContentColumn() {
        renderCard(locked = false)
        assertTheChipClosesTheHeader(locked = false)
    }

    /**
     * The arrangement holds in BOTH lock states. The label gets shorter when the exercise is
     * unlocked ("Level lock" vs "Locked"), so the chip's width changes while its anchor must
     * not: its right edge stays on the content column and the pencil stays behind it.
     */
    @Test
    fun theArrangementHoldsWhenTheExerciseIsLocked() {
        renderCard(locked = true)
        assertTheChipClosesTheHeader(locked = true)
    }

    /**
     * The other half of the 2026-10-09 instruction: they stay DISTINGUISHABLE, now that they
     * share the corner and are 2dp apart on paper. What separates them is shape and wording
     * rather than distance - a labelled pill against a bare circular glyph - so the chip must
     * be visibly wider than the pencil's target, and the pencil must stay a 48dp square (its
     * tap target is not the thing that moved).
     */
    @Test
    fun theChipAndThePencilRemainDistinguishable() {
        renderCard(locked = false)

        val (chipLeft, chipRight) = horizontalBounds("level_lock_toggle")
        val (pencilLeft, pencilRight) = horizontalBounds("level_edit")

        assertEquals(
            "the pencil keeps its 48dp minimum touch target",
            48f,
            pencilRight - pencilLeft,
            1.5f
        )
        assertTrue(
            "the chip must read as the labelled control beside the icon, not a second icon " +
                "(chip ${chipRight - chipLeft}dp wide vs the pencil's ${pencilRight - pencilLeft}dp)",
            chipRight - chipLeft > pencilRight - pencilLeft + 20f
        )
        assertTrue(
            "the pencil stays inside the card: its target ends at $pencilRight and the card's " +
                "content column is $contentRightDp",
            pencilRight <= contentRightDp + 1.5f
        )
    }
}
