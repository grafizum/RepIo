package com.grafizum.repio

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.EditorLevelData
import com.grafizum.repio.ui.components.ExerciseEditDialog
import com.grafizum.repio.ui.screens.AddExerciseDialog
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The level system's ADD-exercise card, as the owner meets it on the phone.
 *
 * Owner, 2026-10-11: *"i told you to remove the input boxes under the exercises and the 'exercise
 * (34)' label i want it to actually show all of those exercises in a scroll box kind of like
 * replacing exercise, not just a label but actually exercises i can select from"*.
 *
 * [ExercisePickerBodyTest] tests the picker as a component; this renders the DIALOG, because every
 * one of those three complaints was a fact about the dialog and not about the component:
 *
 *  1. the list was laid out at 0px - the card drew its search field, its filters and its count line
 *     and NOTHING to pick - because the picker's lazy list was handed an unbounded height by the
 *     dialog's scrolling body (the replace dialog, whose list works, passes `scrollableBody =
 *     false`). The component alone is measured in a bounded test column, which is exactly why the
 *     suite was green while the phone was not;
 *  2. the "Exercises (N)" caption is gone;
 *  3. the sets / reps / weight boxes no longer sit under the list while nothing is picked, and come
 *     back the moment a movement the level system has no ladder for is chosen.
 *
 * One deliberate stub: [EditorLevelData.NONE] is answered for every name, so the level list is
 * never drawn and the fallback below it is the thing under test. That is the honest way to reach
 * the fallback without depending on which pool movement happens to be ladder-less this month.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class AddExerciseCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private companion object {
        val KIT_TAGS: Set<String> = ExerciseEquipmentTags.allowedTagsFor(
            listOf(
                ExerciseEquipmentTags.DUMBBELLS_ONLY,
                ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR,
                ExerciseEquipmentTags.CHAIR,
            )
        )
        val KIT = listOf(
            WorkoutTemplates.EQUIPMENT_DUMBBELL,
            WorkoutTemplates.EQUIPMENT_BODYWEIGHT,
        )
        /** A staple of this kit, and a row of the list the card opens with. */
        const val STAPLE = "Push-Up"
    }

    private fun renderAddCard() {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                ExerciseEditDialog(
                    allAvailableExercises = emptyList(),
                    // An ADD: no row yet, which is what puts the card in picker mode.
                    exercise = null,
                    dayOfWeek = "MONDAY",
                    themeKey = "warm_beige",
                    onDismiss = {},
                    onSave = {},
                    levelDataFor = { _, _ -> EditorLevelData.NONE },
                    levelSystemOn = true,
                    userEquipmentCategories = KIT,
                    userGrantedTags = KIT_TAGS,
                )
            }
        }
    }

    /**
     * The SECOND host of the same card: the welcome/setup screen's own add dialog.
     *
     * It is a separate form that has already drifted from the routine editor once (it was the
     * editor that never got the level UI at all), and it hosts the picker the same way - so the
     * fix and the gate below have to hold in BOTH. Rendering it is the only way to know that, and
     * the ledger's own rule is that a change to one editor is a change to two.
     */
    private fun renderSetupCard() {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                AddExerciseDialog(
                    allAvailableExercises = emptyList(),
                    exercise = null,
                    initialDay = "MONDAY",
                    themeKey = "warm_beige",
                    onDismiss = {},
                    onConfirm = {},
                    levelDataFor = { _, _ -> EditorLevelData.NONE },
                    levelSystemOn = true,
                    userEquipmentCategories = KIT,
                    userGrantedTags = KIT_TAGS,
                )
            }
        }
    }

    /** The welcome screen's card: exercises to select, no caption, no empty boxes. */
    @Test
    fun theWelcomeScreenCardBehavesTheSame() {
        renderSetupCard()

        val row = composeTestRule.onNodeWithText(STAPLE).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the welcome screen's add card lays its rows out at ${row.height}px",
            row.height > 0f
        )
        composeTestRule.onNodeWithText("Exercises (", substring = true).assertDoesNotExist()
        for (label in listOf("Sets", "Weight")) {
            composeTestRule.onNodeWithText(label).assertDoesNotExist()
        }
    }

    /**
     * THE CARD OFFERS EXERCISES, which is the whole point of it.
     *
     * Asserted as a MEASUREMENT of a row's height rather than as `assertExists`: the failure the
     * owner reported was not a missing node - every row existed and every row was 0px tall, so an
     * existence check passes on a card with nothing on it.
     */
    @Test
    fun theCardShowsExercisesToSelect() {
        renderAddCard()

        assertTrue(
            "the card is not drawing the picker at all",
            composeTestRule.onNodeWithTag("exercise_picker_search", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot.height > 0f
        )
        val row = composeTestRule.onNodeWithText(STAPLE).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the card's rows have no height (${row.height}px), so the user sees a label and " +
                "nothing to pick from",
            row.height > 0f
        )
        composeTestRule.onNodeWithText(STAPLE).assertIsDisplayed()
    }

    /** The count caption the owner asked to have removed - it is not drawn, only spoken. */
    @Test
    fun theCardDoesNotPrintACountCaption() {
        renderAddCard()

        composeTestRule.onNodeWithText("Exercises (", substring = true).assertDoesNotExist()
        // The count still exists as the list's own accessibility description, which no sighted user
        // sees: the assertion above would otherwise be satisfied by deleting the information.
        assertTrue(
            "the list no longer states how many movements it holds",
            composeTestRule.onNodeWithTag("exercise_picker_list", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot.height > 0f
        )
    }

    /**
     * NEITHER THE COUNT LINE NOR RESET SITS ABOVE THE LIST.
     *
     * Owner, 2026-10-11: *"i want the 'reset' button gone over the list of exercises that are being
     * shown"* (the count label went in the same pass). A FILTER IS ACTIVATED FIRST on purpose: the
     * removed control only ever appeared once something was filtering, so an assertion made on the
     * untouched card would pass against the old code too - a test that cannot fail is not a test.
     */
    @Test
    fun nothingSitsBetweenTheFiltersAndTheListAnyMore() {
        renderAddCard()

        composeTestRule.onNodeWithTag("muscle_filter_toggle").performClick()
        composeTestRule.onNodeWithTag("muscle_chip_Chest").performClick()

        composeTestRule.onNodeWithText("Reset").assertDoesNotExist()
        composeTestRule.onNodeWithText("Exercises (", substring = true).assertDoesNotExist()
        // ...and the way back out is the filter itself, which must still be offered.
        composeTestRule.onNodeWithTag("muscle_chip_any").assertExists()
    }

    /**
     * THE GLYPH SITS ON THE CHOICE, NOT ON THE LIST.
     *
     * The muscle row's value is WORDS, and the body-part chip that picks it carries the glyph.
     * This is a correction of the 2026-10-11 placement, which put the glyph IN the row's value
     * slot: owner, 2026-10-12, on seeing it on the phone: *"the icons should be in the muscles
     * group filters not in the fucking exercises displaying"*. The row sits directly above the
     * exercise list inside the picker, so a lone body-part glyph there reads as a mark belonging
     * to the rows underneath rather than as a filter summary - which is exactly the complaint.
     *
     * The equipment chips already draw an 18dp leading glyph ([EquipmentFilterRow]), so putting
     * the muscle glyphs on their chips is also what makes the two filters one control family,
     * which was the original ask behind the icon work.
     *
     * Both halves are asserted: the row still NAMES the part that is filtering (in text, so it
     * survives translation), and the chip for that part draws a picture of it.
     */
    @Test
    fun theMuscleRowNamesItsGroupAndTheChipCarriesTheGlyph() {
        renderAddCard()

        composeTestRule.onNodeWithTag("muscle_filter_toggle").performClick()
        composeTestRule.onNodeWithTag("muscle_chip_Chest").performClick()

        // The row says the group in words.
        val value = composeTestRule.onNodeWithTag("muscle_filter_value", useUnmergedTree = true)
            .fetchSemanticsNode()
        assertEquals(
            "the muscle row no longer names the part that is filtering the list",
            "Chest",
            value.config.getOrNull(SemanticsProperties.Text)
                ?.joinToString("") { it.text }
        )

        // And the chip that picked it carries the picture, on top of its own label.
        // Read from the MERGED node: a FilterChip merges its descendants, so its label only
        // appears once merged (the unmerged tree answers with the bare wrapper).
        val chipText = composeTestRule.onNodeWithTag("muscle_chip_Chest")
            .fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }
            .orEmpty()
        assertTrue(
            "the Chest chip lost its label, so it is a picture nobody can name: \"$chipText\"",
            chipText.isNotBlank()
        )
        // The glyph itself, through the handle it carries — scoped to the CHEST chip, because
        // every part that has a glyph draws one (six in the "Any" state). An Icon with no content
        // description is dropped from the semantics tree, so this handle is the only way "does the
        // chip draw its body part" can be asked at all — and it is what makes the assertions above
        // more than label checks: removing the leadingIcon makes this node vanish and the test red.
        composeTestRule.onNode(
            hasTestTag("muscle_chip_Chest") and hasAnyDescendant(hasTestTag("muscle_chip_glyph")),
            useUnmergedTree = true
        ).assertIsDisplayed()

        // And "Any" draws no glyph of its own: nothing is chosen, so no body part can be named.
        composeTestRule.onNode(
            hasTestTag("muscle_chip_any") and hasAnyDescendant(hasTestTag("muscle_chip_glyph")),
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    /**
     * NO INPUT BOXES UNDER THE LIST UNTIL SOMETHING IS PICKED - and then they return for a movement
     * the level system has no ladder for, which is the fallback that keeps such a movement writable.
     * Both halves matter: hiding them unconditionally would leave a stretch or a pose with no way
     * to set a plan at all.
     */
    @Test
    fun theInputBoxesArriveWithAPickAndNotBefore() {
        renderAddCard()

        for (label in listOf("Sets", "Weight")) {
            composeTestRule.onNodeWithText(label).assertDoesNotExist()
        }

        composeTestRule.onNodeWithText(STAPLE).performClick()

        composeTestRule.onNodeWithText("Sets").assertExists()
        composeTestRule.onNodeWithText("Weight").assertExists()
    }
}
