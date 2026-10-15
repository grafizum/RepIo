package com.grafizum.repio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.ui.components.EditorLevelOption
import com.grafizum.repio.ui.components.EditorLevelStage
import com.grafizum.repio.ui.components.LevelPickerCard
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.windowSizeFor
import com.grafizum.repio.ui.screens.roundedStageGroups
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import androidx.test.core.app.ApplicationProvider
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
 * Every Roman row can be opened, and what it opens is the climb to the next one.
 *
 * OWNER, 2026-10-14: *"i want to be able to have each roman section (9 9 9) or (10 10 10)
 * have an arrow dropdown which will then show the other roman level steps underneath it
 * which is from one (9 9 9) to the next (10 10 10)"*.
 *
 * The rows the list shows are already ROUNDED (owner ruling 2026-10-09: "round them up so
 * each number shown is the same reps"), so on the standard pattern it draws I 8/8/8,
 * IV 9/9/9, VII 10/10/10, X 11/11/11, XIII 12/12/12 and the two stages between each pair
 * were gone entirely. They are now carried on the row above them and drawn when its arrow
 * is tapped.
 *
 * Picking a revealed stage WRITES it, like any other row - the owner's answer when asked,
 * and the honest one: they are real stages the engine climbs through, and the workout
 * card has always drawn their dots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class LevelStageDropdownTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val window = windowSizeFor(widthDp = 411)

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val bench = ProgressionEngine.entryFor("Barbell Bench Press")!!

    /** The stages of one level, built exactly as the editor host builds them. */
    private fun stagesOf(level: Int): List<EditorLevelStage> =
        (1..ProgressionEngine.patternLength(bench)).map { stage ->
            EditorLevelStage(
                stage = stage,
                roman = ProgressionEngine.stageName(stage),
                reps = ProgressionEngine.stageRowReps(bench, level, stage)
            )
        }

    private fun groupsOf(level: Int) = roundedStageGroups(stagesOf(level))

    /**
     * The rows, and the climb under each one.
     *
     * The bench's pattern is the pool's standard one: 8/8/8 climbing to 12/12/12 with one
     * rep moving on one set at a time, so the round rows are I, IV, VII, X, XIII and every
     * step between them is a mixture (II 9/8/8, III 9/9/8 ...).
     */
    @Test
    fun theShownRowsCarryTheStepsTheyHide() {
        val groups = groupsOf(1)

        assertEquals(
            "the round rows, in order",
            listOf("I", "IV", "VII", "X", "XIII"),
            groups.map { it.shown.roman }
        )
        assertEquals(listOf("II", "III"), groups[0].hidden.map { it.roman })
        assertEquals(listOf("V", "VI"), groups[1].hidden.map { it.roman })
        assertEquals(listOf("VIII", "IX"), groups[2].hidden.map { it.roman })
        assertEquals(listOf("XI", "XII"), groups[3].hidden.map { it.roman })
        assertTrue("the last row has no next step to climb to", groups[4].hidden.isEmpty())

        // The revealed rows are the pattern's own, not invented: II is a mixed row.
        assertEquals(listOf(9, 8, 8), groups[0].hidden[0].reps)
        assertEquals(listOf(9, 9, 8), groups[0].hidden[1].reps)

        // Nothing is lost between them: shown + revealed is the whole pattern.
        val covered = groups.flatMap { listOf(it.shown.stage) + it.hidden.map { h -> h.stage } }
        assertEquals((1..13).toList(), covered.sorted())
    }

    /** A flat ladder grows no arrows at all, rather than rows of the same numbers. */
    @Test
    fun aRowWithNothingUnderItGetsNoArrow() {
        val flat = (1..13).map { stage ->
            EditorLevelStage(stage = stage, roman = ProgressionEngine.stageName(stage), reps = listOf(12, 12, 12))
        }
        val groups = roundedStageGroups(flat)
        assertEquals(1, groups.size)
        assertTrue("a flat pattern has nothing to reveal", groups.single().hidden.isEmpty())
    }

    private fun render(level: Int, onStage: (Int, Int) -> Unit = { _, _ -> }) {
        val options = listOf(
            EditorLevelOption(
                level = level,
                isCurrent = true,
                loadText = "20 kg",
                stageGroups = groupsOf(level)
            )
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalWindowSize provides window) {
                WorkoutTrackerTheme {
                    Surface(color = Color.White) {
                        Box(modifier = Modifier.fillMaxSize().testTag("editor_host")) {
                            LevelPickerCard(
                                options = options,
                                selectedLevel = level,
                                onSelect = {},
                                onOpenLadder = null,
                                selectedStage = 1,
                                onSelectStage = { l, s -> onStage(l, s) }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * The real host, shrunk to one level: picking a stage moves the CURRENT stage, which
     * re-rounds the list (the current stage is always drawn - see [roundedStages]). That
     * recomputation is the whole reason the open dropdown used to snap shut, so the test
     * has to reproduce it rather than render a frozen list.
     */
    private fun renderLive(initialStage: Int) {
        val current = mutableStateOf(initialStage)
        composeTestRule.setContent {
            val stage = current.value
            val stages = (1..ProgressionEngine.patternLength(bench)).map {
                EditorLevelStage(
                    stage = it,
                    roman = ProgressionEngine.stageName(it),
                    reps = ProgressionEngine.stageRowReps(bench, 1, it),
                    isCurrent = it == stage
                )
            }
            CompositionLocalProvider(LocalWindowSize provides window) {
                WorkoutTrackerTheme {
                    Surface(color = Color.White) {
                        Box(modifier = Modifier.fillMaxSize().testTag("editor_host")) {
                            LevelPickerCard(
                                options = listOf(
                                    EditorLevelOption(
                                        level = 1,
                                        isCurrent = true,
                                        loadText = "20 kg",
                                        stageGroups = roundedStageGroups(stages)
                                    )
                                ),
                                selectedLevel = 1,
                                onSelect = {},
                                onOpenLadder = null,
                                selectedStage = stage,
                                onSelectStage = { _, s -> current.value = s }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun rect(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /**
     * A revealed row IS the same box as the rows above it - size included, not just edges.
     *
     * OWNER, 2026-10-14: *"the smaller in between roman numbers ... keep their edges /
     * borders the same shape as the big one ... dont minimize anything on selection and
     * make sure the left starting position of the box is the same as the bigger box"*.
     *
     * Measured, not asserted by eye, and measured in all three directions a box has: the
     * same left edge, the same right edge AND the same height.
     *
     * The rest of that ruling was REVERSED later the same day - see
     * [theInBetweenNumeralsAreSmallerAndSetFurtherRight] - but the BOX half of it stands,
     * which is what this test is: the numeral inside the row recedes, the row does not.
     *
     * The height is the one that was wrong: a shown row stands 48dp for its 32dp arrow
     * target plus 8dp of padding
     * twice, so the rows a dropdown revealed - which carry nothing - came out 32dp, a
     * thinner box under a thicker one. See [LevelPickerCard]'s `stageRowHeight`, which is
     * now a MINIMUM on every row for exactly that reason.
     */
    @Test
    fun aRevealedRowIsTheSameBoxAsAShownOne() {
        render(level = 1)
        val shown = rect("editor_level_stage_IV")
        composeTestRule.onNodeWithTag("editor_stage_dropdown_I").performClick()
        val revealed = rect("editor_level_stage_II")

        assertEquals("same left edge", shown.left, revealed.left, 0.5f)
        assertEquals("same right edge", shown.right, revealed.right, 0.5f)
        assertEquals("same height", shown.height, revealed.height, 0.5f)
    }

    /**
     * The in-between rows RECEDE: smaller numerals, set further right than a big row's.
     *
     * OWNER, 2026-10-14 (later the same day): *"Reverse how the levels between the roman big
     * step numbers are shown, the smaller roman numbers make them smaller again and make
     * sure that they are slightly more to the right positioned so i can see that they are
     * treated as in between roman numbers."*
     *
     * That reverses the NUMERAL half of the ruling the test above pins — the box half still
     * stands — so both halves are measured here rather than asserted in the abstract: the
     * numeral of a revealed row lays out SHORTER than the numeral of a big row, and starts
     * further right.
     *
     * Read out of the UNMERGED tree on purpose. A row is a clickable Surface, so it merges
     * its children: a merged lookup answers a numeral with the whole row's box - which is
     * the one box the two kinds of row still share - and this test would then pass with the
     * change absent.
     */
    @Test
    fun theInBetweenNumeralsAreSmallerAndSetFurtherRight() {
        render(level = 1)
        val density = composeTestRule.density.density
        // "IV" against "II": both are two characters, so a difference in the laid-out box is
        // the type size and nothing else. Height rather than width, because a single line's
        // height comes from its style - a line box does not vary with the glyphs inside it.
        val bigNumeral = composeTestRule.onNodeWithTag(
            "editor_stage_numeral_IV",
            useUnmergedTree = true
        ).fetchSemanticsNode().boundsInRoot
        val bigRow = rect("editor_level_stage_IV")

        composeTestRule.onNodeWithTag("editor_stage_dropdown_I").performClick()
        val inBetweenNumeral = composeTestRule.onNodeWithTag(
            "editor_stage_numeral_II",
            useUnmergedTree = true
        ).fetchSemanticsNode().boundsInRoot
        val inBetweenRow = rect("editor_level_stage_II")

        assertTrue(
            "the in-between numeral laid out " + inBetweenNumeral.height +
                "px against the big row's " + bigNumeral.height +
                "px - the owner asked for these to be smaller again",
            inBetweenNumeral.height < bigNumeral.height - 0.5f
        )

        // ...and inboard of it, by a SLIGHT step: further right than the big row sets its own
        // numerals, but still a numeral in the row rather than a column of its own.
        val stepDp = (inBetweenNumeral.left - bigNumeral.left) / density
        assertTrue(
            "the in-between numeral starts " + stepDp +
                "dp right of the big row's - that is level with it, not in between",
            stepDp >= 4f
        )
        assertTrue(
            "the in-between numeral starts " + stepDp +
                "dp right of the big row's - that is a column of its own, not an in-between step",
            stepDp <= 24f
        )

        // WHAT MOVED WAS THE NUMERAL: the two boxes still start at the same left edge, and the
        // numeral is still inside its own row.
        assertEquals("same left edge", bigRow.left, inBetweenRow.left, 0.5f)
        assertTrue(
            "the in-between numeral ran past its own row's right edge",
            inBetweenNumeral.right < inBetweenRow.right
        )
    }

    /**
     * "Dont minimize anything on selection": the picked row keeps that box exactly.
     *
     * The height is the interesting one, because the selection adds a tick to a row that
     * already has content: if the row were free to size itself, picking it would make it
     * grow and the list would jump under the finger that just tapped it.
     */
    @Test
    fun selectingARevealedRowDoesNotResizeItsBox() {
        renderLive(initialStage = 1)
        composeTestRule.onNodeWithTag("editor_stage_dropdown_I").performClick()
        val before = rect("editor_level_stage_II")
        val shown = rect("editor_level_stage_IV")

        composeTestRule.onNodeWithTag("editor_level_stage_II").performClick()
        val after = rect("editor_level_stage_II")

        assertEquals("same left edge", before.left, after.left, 0.5f)
        assertEquals("same right edge", before.right, after.right, 0.5f)
        assertEquals("same height", before.height, after.height, 0.5f)
        // ...and the box it keeps is the one the arrow rows have.
        assertEquals("the big row's height", shown.height, after.height, 0.5f)
    }

    /**
     * Selecting one of them leaves the dropdown OPEN, with the pick marked.
     *
     * This is the case that used to break: picking stage II makes it the current stage, the
     * current stage is drawn as a row of its own, and the list under the arrow emptied -
     * so the dropdown snapped shut the instant the user used it. The revealed rows are
     * remembered as opened now, so the list stays and the pick carries the tick.
     */
    @Test
    fun pickingARevealedRowLeavesTheDropdownOpenAndMarked() {
        renderLive(initialStage = 1)

        composeTestRule.onNodeWithTag("editor_stage_dropdown_I").performClick()
        composeTestRule.onNodeWithTag("editor_level_stage_II").assertExists()
        composeTestRule.onNodeWithTag("editor_level_stage_III").assertExists()

        composeTestRule.onNodeWithTag("editor_level_stage_II").performClick()

        // Still open after the write: II and III are on screen, and I - now a rung with
        // nothing left under it - still shows its arrow, so it can be closed again.
        composeTestRule.onNodeWithTag("editor_level_stage_II").assertExists()
        composeTestRule.onNodeWithTag("editor_level_stage_III").assertExists()
        composeTestRule.onNodeWithTag("editor_stage_dropdown_I").assertExists()

        // II is drawn ONCE. It is both the pick (the open list) and a shown row now (the
        // current stage is always drawn), and two rows for one stage is the defect this
        // caught on the first run.
        composeTestRule.onAllNodesWithTag("editor_level_stage_II").assertCountEquals(1)

        // ...and the pick is the marked one. Counted by the TICK's content description,
        // because the selected level's row prints the same word as text.
        composeTestRule.onAllNodesWithContentDescription(context.getString(R.string.level_details_current))
            .assertCountEquals(1)

        // Closing again drops the open list: III goes, because nothing between II and IV is
        // drawn any more...
        composeTestRule.onNodeWithTag("editor_stage_dropdown_I").performClick()
        composeTestRule.onAllNodesWithTag("editor_level_stage_III").assertCountEquals(0)
        // ...while II stays: it is the current stage, and the list always draws that.
        composeTestRule.onNodeWithTag("editor_level_stage_II").assertExists()
    }

    /** Closed: the hidden stages are NOT in the tree. Open: they are, and they select. */
    @Test
    fun theArrowOpensTheHiddenStagesAndARevealedOneWrites() {
        var picked: Pair<Int, Int>? = null
        render(level = 1) { l, s -> picked = l to s }

        // The rounded rows are the list until asked.
        composeTestRule.onNodeWithTag("editor_level_stage_I").assertExists()
        composeTestRule.onNodeWithTag("editor_level_stage_IV").assertExists()
        composeTestRule.onAllNodesWithTag("editor_level_stage_II").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("editor_level_stage_III").assertCountEquals(0)

        // The last row has no arrow - a control that opens nothing is not drawn.
        composeTestRule.onAllNodesWithTag("editor_stage_dropdown_XIII").assertCountEquals(0)

        composeTestRule.onNodeWithTag("editor_stage_dropdown_I").performClick()
        composeTestRule.onNodeWithTag("editor_level_stage_II").assertExists()
        composeTestRule.onNodeWithTag("editor_level_stage_III").assertExists()

        // A revealed row is a real choice: tapping it writes level AND stage.
        composeTestRule.onNodeWithTag("editor_level_stage_III").performClick()
        assertEquals(1 to 3, picked)

        // And the arrow closes it again.
        composeTestRule.onNodeWithTag("editor_stage_dropdown_I").performClick()
        composeTestRule.onAllNodesWithTag("editor_level_stage_II").assertCountEquals(0)
    }
}
