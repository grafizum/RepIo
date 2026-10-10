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
import androidx.compose.ui.unit.dp
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
 * The defect was structural, not a value: the chip used to be a plain SIBLING of the pencil, so
 * the header's own 8dp spacing and a fixed 4dp spacer both landed between them, while the
 * weight spacer above had already eaten the free width - 26dp of air between the two controls,
 * and the chip's left edge consequently on the card's midline. Nothing about that is expressible
 * as a pure function, so this test lays the card out with Robolectric and reads the two
 * controls' actual bounds. It is also what stops the fix from being undone by a later edit that
 * re-adds a spacer: the chips come apart and [theChipAndPencilAreOneCluster] fails.
 *
 * The numbers printed by each test are dp, converted from the layout's own pixels, so an
 * assertion means the same thing on any qualifier.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class LevelCardLockPlacementTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * How far the pencil's GLYPH sits inside its 48dp target, on its left: a 20dp icon centred
     * in the box and then shifted 6dp toward the card's edge by the card's own `offset`. The
     * target's padding is what gives the chip and the pencil their visible breathing room
     * without the row having to add any, which is why the assertions are stated against the
     * pencil's TARGET and not against its glyph.
     */
    private val pencilLeadInDp = 14f

    /** The 6dp the card shifts the pencil's target toward the edge (owner's earlier request). */
    private val pencilInsetShiftDp = 6f

    /** Renders the card at a phone width, exactly as the workout screen does. */
    private fun renderCard(locked: Boolean) {
        composeTestRule.setContent {
            val window = windowSizeFor(widthDp = 411)
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

    /** [left, right] edges of a tagged control, in dp, in root coordinates. */
    private fun horizontalBounds(tag: String): Pair<Float, Float> {
        val bounds = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val density = composeTestRule.density
        return with(density) { dp(bounds.left) to dp(bounds.right) }
    }

    private val rootRightDp: Float
        get() = with(composeTestRule.density) {
            dp(composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot.right)
        }

    /**
     * THE FIX, and the measurement that proves it. The chip's right edge stops 2dp short of the
     * pencil's 48dp target - the cluster's own spacing - where the old sibling arrangement left
     * 26dp (8dp + 4dp spacer + 8dp + the pencil's 6dp shift) between them. The chip's left edge
     * also has to be past the card's midline, which is the complaint stated as a number.
     *
     * The 2dp is intentionally smaller than the 6dp [pencilInsetShiftDp] that the pencil's own
     * target adds to the measured gap: the gap this asserts is the LAYOUT gap, so the pencil's
     * shift is subtracted back out rather than being allowed to explain a loose chip.
     */
    private fun assertTheChipAndPencilAreOneCluster(locked: Boolean) {
        val (chipLeft, chipRight) = horizontalBounds("level_lock_toggle")
        val (pencilLeft, pencilRight) = horizontalBounds("level_edit")

        val layoutGap = pencilLeft - chipRight - pencilInsetShiftDp
        println(
            "locked=$locked: chip [$chipLeft, $chipRight] (${chipRight - chipLeft}dp wide), " +
                "pencil target [$pencilLeft, $pencilRight], layout gap ${layoutGap}dp, " +
                "chip left at ${"%.0f".format(100f * chipLeft / rootRightDp)}% of the card, " +
                "pencil ends ${rootRightDp - pencilRight}dp from its right edge"
        )

        assertTrue(
            "locked=$locked: the lock chip must sit immediately LEFT of the pencil, not " +
                "somewhere mid-row (chip right edge $chipRight, pencil target starts at " +
                "$pencilLeft)",
            chipLeft < chipRight && chipRight <= pencilLeft
        )
        assertTrue(
            "locked=$locked: chip and pencil are one cluster, so only the cluster's own " +
                "spacing may separate them - measured ${layoutGap}dp (the old sibling layout " +
                "gave 20dp plus the pencil's shift)",
            layoutGap <= 3f
        )
        assertTrue(
            "locked=$locked: the chip must not sit on the card's midline - its left edge is " +
                "at ${100f * chipLeft / rootRightDp}% of the width",
            chipLeft > rootRightDp / 2f
        )
        assertTrue(
            "locked=$locked: the pencil keeps the card's corner, so its target must end " +
                "within its own padding of the edge - it ends " +
                "${rootRightDp - pencilRight}dp short",
            rootRightDp - pencilRight <= pencilLeadInDp + pencilInsetShiftDp
        )
    }

    @Test
    fun theChipAndPencilAreOneCluster() {
        renderCard(locked = false)
        assertTheChipAndPencilAreOneCluster(locked = false)
    }

    /**
     * The pair holds in BOTH lock states. The label gets shorter when the exercise is unlocked
     * ("Level lock" vs "Locked"), so the chip's width changes while its anchor must not: it
     * still stops against the pencil, and the pencil still owns the corner.
     */
    @Test
    fun theClusterHoldsWhenTheExerciseIsLocked() {
        renderCard(locked = true)
        assertTheChipAndPencilAreOneCluster(locked = true)
    }

    /**
     * The other half of the instruction: they stay DISTINGUISHABLE, since they now share a
     * corner. What separates them is shape and wording rather than distance - a labelled pill
     * against a bare circular glyph - so the chip must be visibly wider than the pencil's
     * target, and the pencil must stay a 48dp square.
     */
    @Test
    fun theChipAndThePencilRemainDistinguishable() {
        renderCard(locked = false)

        val (chipLeft, chipRight) = horizontalBounds("level_lock_toggle")
        val (pencilLeft, pencilRight) = horizontalBounds("level_edit")

        assertEquals(
            "the pencil keeps the card's corner at its 48dp minimum touch target",
            48f,
            pencilRight - pencilLeft,
            1.5f
        )
        assertTrue(
            "the chip must read as the labelled control beside the icon, not a second icon " +
                "(chip ${chipRight - chipLeft}dp wide vs the pencil's ${pencilRight - pencilLeft}dp)",
            chipRight - chipLeft > pencilRight - pencilLeft + 20f
        )
    }
}
