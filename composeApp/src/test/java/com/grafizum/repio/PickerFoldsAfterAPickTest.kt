package com.grafizum.repio

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE PICKER FOLDS AWAY ONCE SOMETHING IS PICKED, AND THE NAME STAYS ON SCREEN.
 *
 * Owner, 2026-10-16: *"When selecting an exercise to add or replace, minimize the exercise scrolling
 * window (and allow the user to reopen it if wanted) and then under it where the level shows upon
 * selecting an exercise, i want you to display the exervise name fully as a title above the levels
 * so that the user still can see what he chose."*
 *
 * Both halves are about the SAME moment - the tap that picks a movement - and they only work
 * together. Folding the list without a title would leave the dialog showing a level list with no
 * statement of which exercise it belongs to; the title without the fold would title a list the user
 * is still scrolling. So they are pinned together, and in this order: the fold first, the title
 * after it.
 *
 * These are source-level assertions on purpose: `ExercisePickerBody` draws its results as cards
 * addressed by their own text, and a Compose case that tapped "whichever row is first" would be
 * asserting on the pool's popularity order rather than on the fold.
 */
class PickerFoldsAfterAPickTest {

    private val picker: String =
        File("src/main/java/com/grafizum/repio/ui/components/ExercisePicker.kt").readText()

    private val components: String =
        File("src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt").readText()

    /**
     * The list is BEHIND the fold, not merely next to it.
     *
     * The ordering assertion is the one that matters: the collapsed row is drawn first and the
     * `LazyColumn` only in the branch below it, so an edit that put the list back unconditionally -
     * leaving the collapsed row beside a visible list - fails here instead of shipping.
     */
    @Test
    fun thePickerFoldsTheExerciseListWhenSomethingIsPicked() {
        val folded = picker.indexOf("exercise_picker_collapsed")
        val list = picker.indexOf("\"exercise_picker_list\"")
        assertTrue("the picker has no folded state", folded > 0)
        assertTrue("the exercise list is gone from the picker entirely", list > 0)
        assertTrue(
            "the folded row is not drawn before the list, so the list is not what the fold hides",
            folded < list
        )
        assertTrue(
            "the fold is not keyed on the pick, so the list never opens again or never closes",
            picker.contains("remember(selectedName) { mutableStateOf(selectedName == null) }")
        )
        assertTrue(
            "the folded picker offers no way back into the list, which the owner asked for " +
                "(\"and allow the user to reopen it if wanted\")",
            picker.contains("exercise_picker_expand") && picker.contains("""getString("show_more")""")
        )
    }

    /** The folded row names what was picked, in the language the list was read in. */
    @Test
    fun theFoldedRowShowsTheChosenExercise() {
        val folded = picker.indexOf("exercise_picker_collapsed")
        val expand = picker.indexOf("exercise_picker_expand")
        val block = picker.substring(folded, expand)
        assertTrue(
            "the folded row does not name the picked exercise, so folding the list loses the " +
                "answer the user just gave",
            block.contains("translateExerciseName(") && block.contains("selectedName")
        )
    }

    /**
     * ...and the editor titles the movement above its levels, where the choice has to be readable.
     */
    @Test
    fun theEditorTitlesTheChosenExerciseAboveItsLevels() {
        val title = components.indexOf("editor_level_exercise_title")
        // THE CALL, NOT THE DEFINITION. `LevelPickerCard(` also matches the composable's own
        // declaration, which sits far above the editor in this file - comparing against that made
        // this case fail on a source that was correct.
        val levels = components.indexOf(
            "LevelPickerCard(\n                        options = levelData.options"
        )
        assertTrue("the editor no longer titles the exercise above its levels", title > 0)
        assertTrue(
            "the editor does not call LevelPickerCard with the level data any more, so this case " +
                "cannot tell where the title sits relative to the levels",
            levels > 0
        )
        assertTrue(
            "the exercise title is drawn BELOW the level list, where it does not say what the " +
                "levels belong to",
            title < levels
        )
        // Sliced from the title's GUARD, not from its test tag: the tag is written at the END of
        // the Text call, so a slice that started there would exclude the very line it looks for.
        val guard = components.indexOf("if (levelMode && name.isNotBlank()) {")
        assertTrue("the title is not gated on a chosen exercise any more", guard > 0)
        val block = components.substring(guard, levels)
        assertTrue(
            "the title does not read the exercise's own name",
            block.contains("translateExerciseName(name)")
        )
    }
}
