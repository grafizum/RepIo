package com.grafizum.repio

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.ReplaceExerciseDialog
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
import java.io.File

/**
 * The replace dialog's muscle filter, after the owner asked for it to be a FILTER rather than a
 * form field.
 *
 * Owner, 2026-10-09: *"allow the user to filter which muscle too for when replacing an exercise,
 * make sure the filter still stays nicely spacious yet compact and small"*.
 *
 * The dialog already HAD a muscle filter — a full-width `OutlinedTextField` dropdown with a
 * floating label, sitting directly under the one-row equipment summary. So the deliverable is not
 * "a control exists"; it is four properties that can each regress on their own:
 *
 *  1. COMPACT while the list is being read — one line naming what is filtering it, choices behind
 *     a toggle, the shape the equipment row beside it already had;
 *  2. SPACIOUS when opened — every body part AND every precise muscle, the precise ones under
 *     their own caption so the six do not have to be found again among twenty-five;
 *  3. HONEST — picking one actually narrows the pool. The counts here are compared against
 *     [WorkoutTemplates.getExercisesForEquipment], the same lookup the dialog is supposed to be
 *     feeding, so a chip that renders correctly and passes the wrong muscle through (or none)
 *     fails;
 *  4. REVERSIBLE — "Any" widens it back, and the collapsed row always shows the live choice.
 *
 * The chips are addressed by tag rather than by their label: a card's description can contain the
 * word "lats", and a test that clicked whichever node happened to say it would pass for the wrong
 * reason.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class ReplaceMuscleFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private companion object {
        /** A bodyweight spinal-erector movement, so the dialog opens on its own kit and part. */
        const val REPLACED = "Superman"
        const val REPLACED_MUSCLE = "Spinal Erectors"
        /** What [WorkoutTemplates.getTopicForMuscle] reduces that muscle to. */
        const val REPLACED_TOPIC = "Back"
    }

    private fun render() {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                ReplaceExerciseDialog(
                    exercise = RoutineExerciseEntity(
                        id = 1,
                        dayOfWeek = "MONDAY",
                        name = REPLACED,
                        muscleGroup = REPLACED_MUSCLE,
                        targetSets = 3,
                        targetReps = "8-12",
                        targetIntensity = "medium",
                        targetWeightKg = 0.0,
                        restSeconds = 60,
                        orderIndex = 0,
                        videoUrl = ""
                    ),
                    onDismiss = {},
                    onReplace = { _, _ -> }
                )
            }
        }
    }

    /** The number the dialog's own header prints in "Exercises (N)". */
    private fun shownCount(): Int {
        val node = composeTestRule
            .onNodeWithText("Exercises (", substring = true)
            .fetchSemanticsNode()
        val text = node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }
            .orEmpty()
        val match = Regex("\\((\\d+)\\)").find(text)
        assertTrue("the list printed no count: \"$text\"", match != null)
        return match!!.groupValues[1].toInt()
    }

    /** The muscle the collapsed row is showing. */
    private fun shownMuscle(): String =
        composeTestRule.onNodeWithTag("muscle_filter_value")
            .fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }
            .orEmpty()

    /**
     * The pool the dialog should be offering for [muscle], derived the way the dialog derives it:
     * the replaced row's own equipment category seeds the equipment filter, and "no muscle" means
     * the whole equipment category rather than an empty list. The replaced exercise itself is
     * never offered back, which is the only subtraction.
     */
    private fun expectedPool(muscle: String?): Int {
        val equipment = listOf(WorkoutTemplates.getEquipmentForExercise(REPLACED))
            .filter { it.isNotEmpty() }
            .ifEmpty { WorkoutTemplates.allEquipmentCategories }
        val pool = if (muscle == null) {
            equipment.flatMap { WorkoutTemplates.getAllExercisesForEquipment(it) }
        } else {
            equipment.flatMap { WorkoutTemplates.getExercisesForEquipment(it, muscle) }
        }
        return pool.distinct().filterNot { it.equals(REPLACED, ignoreCase = true) }.size
    }

    /**
     * Open or close the filter. The row is scrolled to first because the capped block can have
     * scrolled it out of the viewport, and a tap at a scrolled-out node's coordinates lands on
     * whatever is drawn there instead — which is how a test "clicks the toggle" and changes
     * nothing.
     */
    private fun openFilter() = chip("muscle_filter_toggle").performScrollTo().performClick()

    private fun chip(tag: String) = composeTestRule.onNodeWithTag(tag)

    /**
     * Tap a chip the way a user has to: the expanded block is capped and scrolls, so the precise
     * muscles below the fold must be scrolled to before they can be tapped.
     */
    private fun pick(tag: String) = chip(tag).performScrollTo().performClick()

    // ---------------------------------------------------------------- compact

    /**
     * Collapsed, the filter is ONE row: a value and a toggle, and none of the choices. This is the
     * "compact and small" half — the old dropdown spent 56dp plus a floating label on a control
     * whose whole job is to say one word.
     */
    @Test
    fun theFilterIsOneRowThatNamesWhatItIsFilteringBy() {
        render()

        chip("muscle_filter_toggle").assertExists()
        chip("muscle_filter_value").assertIsDisplayed()
        assertEquals(
            "the collapsed row does not name the muscle the list is filtered to",
            REPLACED_TOPIC, shownMuscle()
        )
        // Every choice is behind the toggle: nothing of the vocabulary is on screen yet.
        chip("muscle_filter_precise_row").assertDoesNotExist()
        chip("muscle_chip_any").assertDoesNotExist()
        chip("muscle_chip_precise_Lats").assertDoesNotExist()
        composeTestRule.onNodeWithText("Precise muscles").assertDoesNotExist()
    }

    /**
     * The shape the two words in the request describe, MEASURED rather than eyeballed: *"nicely
     * spacious yet compact and small"*.
     *
     * Compact is the collapsed row being ONE line — the value and the toggle share a baseline band
     * instead of stacking — and spacious is the two vocabularies being separated vertically when it
     * opens, so the precise nineteen sit under their own caption rather than continuing the six.
     * Both are geometric claims, so they are asked of the laid-out boxes; a screenshot would show
     * them today and prove nothing after the next edit.
     */
    @Test
    fun theCollapsedRowIsOneLineAndTheTwoVocabulariesStack() {
        render()
        val density = composeTestRule.density.density

        val value = chip("muscle_filter_value").fetchSemanticsNode().boundsInRoot
        val toggle = chip("muscle_filter_toggle").fetchSemanticsNode().boundsInRoot
        val lineGap = kotlin.math.abs(value.center.y - toggle.center.y) / density
        assertTrue(
            "the collapsed filter is not one line: the value's centre and the toggle's centre " +
                "are ${"%.1f".format(lineGap)}dp apart",
            lineGap <= 3f
        )
        // One line also means the whole row stays short: the taller of the two halves is the row.
        val rowHeightDp = maxOf(value.height, toggle.height) / density
        assertTrue(
            "the collapsed muscle row is ${"%.1f".format(rowHeightDp)}dp tall, which is a field " +
                "rather than a line",
            rowHeightDp <= 44f
        )

        openFilter()
        val broad = chip("muscle_chip_$REPLACED_TOPIC").fetchSemanticsNode().boundsInRoot
        val precise = chip("muscle_filter_precise_row").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the precise muscles are not stacked below the body parts: the body-part chip ends " +
                "at ${"%.1f".format(broad.bottom / density)}dp and the precise block starts at " +
                "${"%.1f".format(precise.top / density)}dp, so the nineteen read as a longer " +
                "version of the six",
            precise.top >= broad.bottom
        )
    }

    // --------------------------------------------------------------- spacious

    /**
     * Opened, it offers BOTH vocabularies — the six body parts the filter always had, and the
     * precise muscles under their own caption. The caption is the owner's "spacious" half: the
     * nineteen are the useful answer to "something else for my lats", but they must not read as a
     * longer version of the six they sit under.
     */
    @Test
    fun openingItOffersTheBodyPartsAndThePreciseMusclesSeparately() {
        render()
        openFilter()

        WorkoutTemplates.getBroadMuscleGroups().forEach { group ->
            chip("muscle_chip_$group").assertExists()
        }
        chip("muscle_filter_precise_row").assertExists()
        WorkoutTemplates.getPreciseMuscleGroups().forEach { muscle ->
            chip("muscle_chip_precise_$muscle").assertExists()
        }
        // "Any" is a visible choice rather than the absence of one.
        chip("muscle_chip_any").assertExists()
        // The filter STARTS on the replaced exercise's own body part, and says so.
        chip("muscle_chip_$REPLACED_TOPIC").assertIsSelected()
    }

    // ----------------------------------------------------------------- honest

    /**
     * The part no screenshot can show: the choice reaches the pool lookup.
     *
     * Each expectation is the count [WorkoutTemplates.getExercisesForEquipment] produces for that
     * muscle, which is what makes this a test of the WIRING rather than of the chip's looks — a
     * filter that renders beautifully and passes the replaced exercise's own muscle every time
     * would fail on the very first assertion.
     */
    @Test
    fun pickingAMuscleNarrowsThePoolToThatMuscle() {
        render()
        openFilter()

        assertEquals(
            "the dialog does not open on the replaced exercise's own body part",
            expectedPool(REPLACED_TOPIC), shownCount()
        )
        assertTrue("the back pool is empty", expectedPool(REPLACED_TOPIC) > 0)

        pick("muscle_chip_precise_Lats")
        assertEquals(
            "picking Lats did not narrow the list to the lat pool",
            expectedPool("Lats"), shownCount()
        )
        assertTrue("the lat pool is empty", expectedPool("Lats") > 0)
        assertTrue(
            "the lat pool should be a strict subset of the back pool",
            expectedPool("Lats") < expectedPool(REPLACED_TOPIC)
        )

        pick("muscle_chip_precise_Quadriceps")
        assertEquals(
            "picking Quadriceps did not narrow the list to the quad pool",
            expectedPool("Quadriceps"), shownCount()
        )
    }

    /** A broad part still filters after a precise muscle: both vocabularies stay live. */
    @Test
    fun aBodyPartStillFiltersAfterAPreciseMuscleWasChosen() {
        render()
        openFilter()
        pick("muscle_chip_precise_Lats")
        pick("muscle_chip_Legs")

        assertEquals(
            "the body parts stopped filtering once a precise muscle had been picked",
            expectedPool("Legs"), shownCount()
        )
    }

    /**
     * The measured shape of the thing the request asked to keep small, written where it can be
     * read without a phone: how much of the dialog the filters cost in each state.
     *
     * It is the audit-style report the filter tests in this project already write (see
     * `FilterConsistencyAuditTest.report`), and it is not decoration — "compact" has to be a
     * number to be checkable, because the previous control was compact-looking in code and 56dp
     * of form field on the screen. Regenerate with:
     *
     *   ./gradlew :composeApp:testDebugUnitTest --tests "*ReplaceMuscleFilterTest*"
     *
     * and read `evidence/verify-muscle-filter/layout.txt`.
     */
    @Test
    fun reportTheFilterHeights() {
        render()
        val density = composeTestRule.density.density
        fun dp(px: Float) = "%.1f".format(px / density)
        fun box(tag: String) = chip(tag).fetchSemanticsNode().boundsInRoot

        val collapsed = box("muscle_filter_value").let { value ->
            val toggle = box("muscle_filter_toggle")
            maxOf(value.height, toggle.height)
        }
        fun text(t: String) = composeTestRule.onNodeWithText(t).fetchSemanticsNode().boundsInRoot
        fun countBox() = composeTestRule.onNodeWithText("Exercises (", substring = true)
            .fetchSemanticsNode().boundsInRoot
        // Re-fetched per state: the count line and the list room both MOVE when the filter opens,
        // and reusing a stale box is how a report ends up claiming the layout did not change.
        fun chrome() = countBox().bottom - text("Replace Exercise").top
        // The Cancel button is pinned below the body, so count-line-to-Cancel is exactly the room
        // the alternatives get — the number the whole layout exists to protect.
        fun listRoom() = text("Cancel").top - countBox().bottom

        val equipment = box("equipment_filter_toggle")
        val chromeCollapsed = chrome()
        val listRoomCollapsed = listRoom()

        openFilter()
        val broad = box("muscle_filter_broad_row")
        val precise = box("muscle_filter_precise_row")
        val chromeExpanded = chrome()
        val listRoomExpanded = listRoom()

        val lines = buildString {
            appendLine("REPLACE DIALOG — FILTER HEIGHTS (Pixel7Pro ${density}x, so 1dp = ${density}px)")
            appendLine()
            appendLine("COLLAPSED (the list is what the dialog is for):")
            appendLine("  muscle filter row:          ${dp(collapsed)}dp")
            appendLine("  equipment filter row:       ${dp(equipment.height)}dp")
            appendLine("  chrome above the list:      ${dp(chromeCollapsed)}dp")
            appendLine("  room for the alternatives:  ${dp(listRoomCollapsed)}dp")
            appendLine()
            appendLine("EXPANDED (the choices, one tap away):")
            appendLine("  body-part chip block:       ${dp(broad.height)}dp")
            appendLine("  caption + gap:              ${dp(precise.top - broad.bottom)}dp")
            appendLine("  precise-muscle chip block:  ${dp(precise.height)}dp")
            appendLine("  the two blocks together:    ${dp(precise.bottom - broad.top)}dp")
            appendLine("  chrome above the list:      ${dp(chromeExpanded)}dp (was ${dp(chromeCollapsed)}dp)")
            appendLine("  room for the alternatives:  ${dp(listRoomExpanded)}dp (was ${dp(listRoomCollapsed)}dp)")
            appendLine()
            appendLine("Owner, 2026-10-09: the filter must stay 'nicely spacious yet compact and small'.")
            appendLine("Compact is the one collapsed line; spacious is the two vocabularies staying apart,")
            appendLine("body parts first and the nineteen precise muscles under their own caption.")
        }
        val dir = java.io.File("../evidence/verify-muscle-filter").apply { mkdirs() }
        java.io.File(dir, "layout.txt").writeText(lines)

        assertTrue(File(dir, "layout.txt").length() > 0)
    }

    // ------------------------------------------------------------- reversible

    /** "Any" is the way back out, and it must widen the pool again. */
    @Test
    fun anyClearsTheMuscleFilter() {
        render()
        openFilter()
        pick("muscle_chip_precise_Lats")
        assertEquals(expectedPool("Lats"), shownCount())

        pick("muscle_chip_any")
        assertEquals(
            "Any did not widen the pool back to every muscle",
            expectedPool(null), shownCount()
        )
        assertTrue(
            "the whole bodyweight pool should be wider than the lat pool",
            expectedPool(null) > expectedPool("Lats")
        )
    }

    /** The collapsed row follows the choice, so the one line it shows never lies about the list. */
    @Test
    fun theCollapsedRowShowsTheChosenMuscle() {
        render()
        openFilter()
        pick("muscle_chip_precise_Triceps")
        openFilter() // collapse again

        assertEquals(
            "the collapsed row did not pick up the muscle that is filtering the list",
            "Triceps", shownMuscle()
        )
        chip("muscle_filter_precise_row").assertDoesNotExist()
    }
}
