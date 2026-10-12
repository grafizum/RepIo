package com.grafizum.repio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.ExercisePick
import com.grafizum.repio.ui.components.ExercisePickerBody
import com.grafizum.repio.ui.components.defaultEquipmentFilter
import com.grafizum.repio.ui.components.equipmentGlyphTint
import com.grafizum.repio.ui.components.exercisePickerPool
import com.grafizum.repio.ui.components.resolvePreciseMuscleGroup
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The picker the level system's ADD-exercise card draws, as the user meets it.
 *
 * [ExercisePickerPoolTest] proves the rules; this proves the WIRING, which is where the owner's
 * request can still fail while every rule holds: the two halves of *\"exercise name that the user
 * can input to search an exercise and under it filter options ... but if the user only uses the
 * filters and doesnt search by name show al the possible exercises based on those filters\"* are
 * one list, and a list that is only ever the search results (or only ever the filters') satisfies
 * neither sentence. The count the card's list carries is read back and compared with
 * the pool the shared function derives, so a picker that renders correctly and shows the wrong
 * rows fails.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class ExercisePickerBodyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private companion object {
        /** Dumbbells and a chair, with no bench and no bar. */
        val KIT_TAGS: Set<String> = ExerciseEquipmentTags.allowedTagsFor(
            listOf(
                ExerciseEquipmentTags.DUMBBELLS_ONLY,
                ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR,
                ExerciseEquipmentTags.CHAIR,
            )
        )
        val KIT_CATEGORIES = listOf(
            WorkoutTemplates.EQUIPMENT_DUMBBELL,
            WorkoutTemplates.EQUIPMENT_BODYWEIGHT,
        )
        /** A staple of this kit, and the one row of the kit that matches this query. */
        const val STAPLE = "Push-Up"
        const val QUERY = "romanian"
        const val MATCH = "Dumbbell Romanian Deadlift"
    }

    private var picked: ExercisePick? = null

    private fun render() {
        picked = null
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    ExercisePickerBody(
                        equipmentCategories = KIT_CATEGORIES,
                        grantedTags = KIT_TAGS,
                        onPick = { picked = it }
                    )
                }
            }
        }
    }

    /**
     * The card hosted the way BOTH add-cards actually host it: inside a body that scrolls.
     *
     * This is the case the first version shipped without, and it is where the owner met the
     * card - *"not just a label but actually exercises i can select from"*. The picker's list is
     * a lazy list, and a lazy list handed an unbounded height (which is exactly what a scrolling
     * parent gives it) cannot lay out, so the card showed its search field, its filters, its
     * count line and NOTHING to pick.
     */
    private fun renderInScrollingBody() {
        picked = null
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    ExercisePickerBody(
                        equipmentCategories = KIT_CATEGORIES,
                        grantedTags = KIT_TAGS,
                        onPick = { picked = it }
                    )
                }
            }
        }
    }

    /**
     * How many movements the card's list holds.
     *
     * Read from the LIST rather than from a caption: the visible "Exercises (N)" line was removed
     * on the owner's instruction (2026-10-11: *"remove ... the 'exercise (34)' label"*), and the
     * count it carried moved to the list's accessibility description - spoken, never drawn. The
     * assertion it feeds is the one worth keeping: the card must show exactly the pool the shared
     * function derives, not merely a plausible-looking list.
     */
    private fun shownCount(): Int {
        val node = composeTestRule.onNodeWithTag("exercise_picker_list").fetchSemanticsNode()
        val text = node.config.getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString("") { it }
            .orEmpty()
        val match = Regex("\\((\\d+)\\)").find(text)
        assertTrue("the picker's list carries no count: \"$text\"", match != null)
        return match!!.groupValues[1].toInt()
    }

    /** The pool the card should be showing, derived the way the card derives it. */
    private fun expectedCount(muscle: String? = null): Int = exercisePickerPool(
        equipmentCategories = KIT_CATEGORIES,
        grantedTags = KIT_TAGS,
        selectedEquipment = defaultEquipmentFilter(KIT_CATEGORIES),
        selectedMuscleGroup = muscle,
    ).size

    /**
     * THE LIST SURVIVES A SCROLLING DIALOG BODY, which is the only way it is ever hosted.
     *
     * Both add-cards draw the picker inside `AppDialog`'s default (scrolling) body; the replace
     * dialog, which the owner names as the one that works, passes `scrollableBody = false` and so
     * gives its list a bounded height. Asserted as a MEASUREMENT rather than an existence check,
     * because the failure mode is not a missing node - it is a row laid out at zero height, which
     * `assertExists` happily passes.
     */
    @Test
    fun theListSurvivesAScrollingDialogBody() {
        renderInScrollingBody()

        val row = composeTestRule.onNodeWithText(STAPLE)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the card's rows have no height inside a scrolling dialog body (${row.height}px), so " +
                "the user sees a count line and nothing to pick from",
            row.height > 0f
        )
    }

    /**
     * OPENING THE CARD IS ALREADY USEFUL. The filters alone answer with everything they allow -
     * no query needed - and "everything" here is the kit the user declared, in popularity order,
     * so the first row is a movement they would actually program.
     */
    @Test
    fun theFiltersAloneListEverythingTheKitAllows() {
        render()

        assertEquals(
            "the opening list is not the pool the filters allow",
            expectedCount(),
            shownCount()
        )
        composeTestRule.onNodeWithText(STAPLE).assertIsDisplayed()
    }

    /**
     * THE OTHER HALF: a query narrows that same list, and it goes through the equipment filter
     * rather than around it - a search for a movement the user's kit cannot do offers nothing.
     */
    @Test
    fun aQueryNarrowsTheFilteredList() {
        render()
        val unfiltered = shownCount()

        composeTestRule.onNodeWithTag("exercise_picker_search").performTextInput(QUERY)

        val narrowed = shownCount()
        assertTrue(
            "the search left the list at $narrowed of $unfiltered rows",
            narrowed in 1 until unfiltered
        )
        composeTestRule.onNodeWithText(MATCH).assertIsDisplayed()
        composeTestRule.onNodeWithText(STAPLE).assertDoesNotExist()
    }

    /**
     * THE MUSCLE FILTER IS ON THE CARD, not only in the pool function behind it: opening it and
     * picking a body part moves the list to exactly what the shared lookup allows for that part,
     * and the row goes on saying which part it is filtering by.
     */
    @Test
    fun theMuscleFilterNarrowsTheListFromTheCard() {
        render()
        val unfiltered = shownCount()

        // No `performScrollTo` here any more: the muscle block scrolls only when its chips outgrow
        // the 28%-of-the-window cap, and the chest selection is a single precise muscle. Scrolling
        // was incidental to reaching a visible chip, never part of what this case asserts.
        composeTestRule.onNodeWithTag("muscle_filter_toggle").performClick()
        composeTestRule.onNodeWithTag("muscle_chip_Chest").performClick()

        val chest = shownCount()
        assertEquals(
            "the card does not show the pool the Chest filter allows",
            expectedCount("Chest"),
            chest
        )
        assertTrue(
            "the Chest filter did not narrow the list ($chest of $unfiltered rows)",
            chest < unfiltered
        )
        composeTestRule.onNodeWithTag("muscle_chip_Chest").assertIsSelected()
    }

    /**
     * THE CHIP ROW SHOWS THE KIT, AND NO CHIP FOR THE KIT-FREE CATEGORY.
     *
     * Owner, 2026-10-11: *"make sure all equipments that are from the welcome screen apply and the
     * (other) is not shown"*. Both halves are asserted here against the drawn row rather than
     * against the vocabulary behind it: the kit's own glyphs are there, the kit-free category's is
     * not, and the list underneath it STILL contains kit-free movements - because "not shown" was
     * never meant to mean "not offered", and the category carries 144 of the catalogue's movements.
     */
    @Test
    fun theFilterRowDrawsTheKitsGlyphsAndNoneForTheKitFreeCategory() {
        render()

        composeTestRule.onNodeWithTag("equipment_glyph_${WorkoutTemplates.EQUIPMENT_DUMBBELL}")
            .assertExists()
        composeTestRule.onNodeWithTag("equipment_glyph_${WorkoutTemplates.EQUIPMENT_BODYWEIGHT}")
            .assertExists()
        composeTestRule.onNodeWithTag("equipment_glyph_${WorkoutTemplates.EQUIPMENT_OTHER}")
            .assertDoesNotExist()

        val pool = exercisePickerPool(
            equipmentCategories = KIT_CATEGORIES,
            grantedTags = KIT_TAGS,
            selectedEquipment = defaultEquipmentFilter(KIT_CATEGORIES),
            selectedMuscleGroup = null,
        )
        assertTrue(
            "the kit-free category stopped filtering, so a plank, a stretch or a walk is no " +
                "longer offered at all",
            pool.any { WorkoutTemplates.getEquipmentForExercise(it) == WorkoutTemplates.EQUIPMENT_OTHER }
        )
    }

    /**
     * AND THE GLYPHS WEAR THE THEME'S ACCENT, not the inherited body colour.
     *
     * Owner, 2026-10-11: *"make sure the minimized icons ... is not pitch black but a theme color
     * maybe a bit more acented than the usual theme primary color"*. The row inherited
     * `LocalContentColor` before, which on the light themes is the near-black body colour - so this
     * pins the intent as a fact the theme can be checked against: the tint IS the accent, and the
     * accent is not the colour body text is written in.
     */
    @Test
    fun theGlyphsWearTheThemeAccentAndNotTheBodyColour() {
        var tint: androidx.compose.ui.graphics.Color? = null
        var accent: androidx.compose.ui.graphics.Color? = null
        var body: androidx.compose.ui.graphics.Color? = null
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                tint = equipmentGlyphTint()
                accent = MaterialTheme.colorScheme.primary
                body = MaterialTheme.colorScheme.onSurface
            }
        }

        assertEquals("the glyph tint must be the theme's accent", accent, tint)
        assertTrue(
            "the glyph tint is the body text colour, which is what the owner reported as " +
                "pitch black (tint=$tint, onSurface=$body)",
            tint != body
        )
    }

    /**
     * THE GROUP'S OWN MUSCLES, WITH THE GROUP STILL ON SCREEN.
     *
     * Owner, 2026-10-11: *"make sure the small 'muscle group: back' is shown also while scrolling
     * through each precise muscle ... and when selecting muscle group back, then make sure that the
     * precise muscles are shown only of that 'back' cathegory / group"*.
     *
     * Both halves are asserted on the card: picking Back leaves exactly Back's muscles (a leg
     * muscle's chip is gone, not merely off-screen), the caption above them NAMES the group, and
     * the row carrying the value survives a scroll to the LAST muscle of the block - which is the
     * point of the request, because a list that scrolls its own label away is nineteen unlabelled
     * muscles again.
     */
    @Test
    fun pickingAGroupNarrowsThePreciseMusclesAndNamesTheGroup() {
        render()

        composeTestRule.onNodeWithTag("muscle_filter_toggle").performClick()
        composeTestRule.onNodeWithTag("muscle_chip_Back").performClick()

        composeTestRule.onNodeWithTag("muscle_chip_precise_Lats").assertExists()
        composeTestRule.onNodeWithTag("muscle_chip_precise_Upper Back").assertExists()
        composeTestRule.onNodeWithTag("muscle_chip_precise_Upper Traps").assertExists()
        composeTestRule.onNodeWithTag("muscle_chip_precise_Spinal Erectors").assertExists()
        composeTestRule.onNodeWithTag("muscle_chip_precise_Quadriceps").assertDoesNotExist()
        composeTestRule.onNodeWithTag("muscle_chip_precise_Pectorals").assertDoesNotExist()
        composeTestRule.onNodeWithTag("muscle_chip_precise_Biceps").assertDoesNotExist()

        val captionText = composeTestRule.onNodeWithTag("muscle_filter_precise_caption")
            .fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }
            .orEmpty()
        assertTrue(
            "the caption above the muscles does not name the group they came from: \"$captionText\"",
            captionText.contains("Back")
        )
        // The row names the group through the glyph's accessible name now, not through a text chip
        // (owner, 2026-10-11: *"instead of the muscle group label name it shows also the icon"*).
        assertEquals(
            "the row no longer names the group it is filtering by",
            "Back",
            composeTestRule.onNodeWithTag("muscle_filter_value", useUnmergedTree = true)
                .fetchSemanticsNode()
                .config.getOrNull(SemanticsProperties.ContentDescription)
                ?.joinToString("") { it }
        )
    }

    /**
     * THE ROW THAT NAMES THE GROUP DOES NOT SCROLL WITH THE MUSCLES.
     *
     * Owner, 2026-10-11: *"make sure the small 'muscle group: back' is shown also while scrolling
     * through each precise muscle"*. Measured the only way that means anything: the chip block is
     * scrolled to its LAST muscle, and the row carrying the value must not have moved a pixel doing
     * it. This is the state with all nineteen muscles - the block only scrolls when the muscles
     * outgrow it, which is exactly the situation the owner described.
     *
     * Before the change the row lived INSIDE the scrolled block, so scrolling to the last muscle
     * carried the label off the top with it and this assertion would have failed.
     */
    @Test
    fun theGroupRowStaysPutWhileTheMuscleListScrolls() {
        render()
        composeTestRule.onNodeWithTag("muscle_filter_toggle").performClick()

        val before = composeTestRule.onNodeWithTag("muscle_filter_value")
            .fetchSemanticsNode().boundsInRoot.top

        composeTestRule.onNodeWithTag("muscle_chip_precise_Pectorals").performScrollTo()

        val after = composeTestRule.onNodeWithTag("muscle_filter_value")
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            "the group row moved by ${after - before}px while the muscles scrolled: it is inside " +
                "the scrollable block instead of above it",
            kotlin.math.abs(after - before) <= 1f
        )
        composeTestRule.onNodeWithTag("muscle_chip_precise_Pectorals").assertIsDisplayed()
        composeTestRule.onNodeWithTag("muscle_filter_value").assertIsDisplayed()
    }

    /**
     * Picking a row hands the host the movement AND its precise muscle, which is the part the
     * host cannot work out for itself: the filter vocabulary is broad ("Chest"), the stored row
     * has to keep the precise label its badge and the analytics split read.
     */
    @Test
    fun pickingARowHandsTheHostThePreciseMuscle() {
        render()

        composeTestRule.onNodeWithText(STAPLE).performClick()

        val result = picked
        assertTrue("tapping a card picked nothing", result != null)
        assertEquals(STAPLE, result!!.name)
        assertEquals(
            "the picked muscle is not the exercise's precise one",
            resolvePreciseMuscleGroup(STAPLE, WorkoutTemplates.getMuscleGroupForExercise(STAPLE)),
            result.muscleGroup
        )
    }
}
