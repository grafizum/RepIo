package com.grafizum.repio

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A ROUTINE ROW SHOWS THE LEVEL THE USER HAS — AND "NEW" WHEN THERE ISN'T ONE.
 *
 * Owner, 2026-10-16, two reports in one breath: *"In the exervises edit tab i want to see levels of
 * the exercise instead of the reps range (or show new if a level hasnt been yet selected because
 * the exercise wasnt made yet)"* and *"When editing or replacing exercises the levels selected
 * dont stay fix it immediately"* — asked which of three symptoms he meant, he picked *"It shows
 * the setup default, not mine"*.
 *
 * MEASURED ON THE DEVICE (SM-S906B, `com.grafizum.repio.debug` 1.7.5/v57, 2026-10-16) before the
 * change: 23 exercises had logged sets; only 8 of them had a `progression_state_<name>` entry.
 * `Concentration Curl` — six sets, the sixth row of his Friday — had `cache_reps_` but no level
 * at all, so the app prescribed and printed the level its setup difficulty ("Medium") would start
 * it at. That is the number he was reading as his own.
 *
 * The split these cases hold: a level the user HAS is stored state
 * ([com.grafizum.repio.ui.viewmodel.WorkoutViewModel.storedProgressionLevel]), while
 * [com.grafizum.repio.ui.viewmodel.WorkoutViewModel.progressionDisplayLevel] also answers with a
 * derived starting prescription. A label may print the first; printing the second as "your level"
 * is the defect. The reads are one word apart and both compile, so this cannot be caught by the
 * type checker and the Compose tests here have no way to see which value builder ran.
 */
class RoutineRowShowsTheLevelTest {

    private val routine: String =
        File("src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt").readText()

    private val components: String =
        File("src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt").readText()

    private val viewModel: String =
        File("src/main/java/com/grafizum/repio/ui/viewmodel/WorkoutViewModel.kt").readText()

    /** The routine row's own call into the card, which is the surface this was reported on. */
    private fun rowCall(): String {
        val at = routine.indexOf("ExpandableExerciseCard(")
        assertTrue("the routine list no longer draws an ExpandableExerciseCard", at > 0)
        return routine.substring(at, routine.indexOf("modifier = (if (isDragging)", at))
    }

    /** The body of a top-level function, up to its closing brace at column 0. */
    private fun bodyOf(source: String, declaration: String, indent: String): String {
        val at = source.indexOf(declaration)
        assertTrue("$declaration is gone", at > 0)
        return source.substring(at, source.indexOf("\n$indent}", at))
    }

    /**
     * The row reads the level the user HAS, not the one the system would start them at.
     *
     * This is the case that would have failed before the change: the row printed the reps range
     * and never asked the level system anything at all.
     */
    @Test
    fun theRoutineRowReadsTheStoredLevel() {
        val call = rowCall()
        assertTrue(
            "the routine row does not show a level, so the edit tab still describes the exercise " +
                "with its reps range",
            call.contains("exerciseLevelSummaryLabel") || call.contains("level = viewModel.")
        )
        assertTrue(
            "the routine row does not read the STORED level",
            call.contains("storedProgressionLevel")
        )
        assertFalse(
            "the routine row reads progressionDisplayLevel, which also answers with the level the " +
                "setup difficulty would start the exercise at - the \"it shows the setup default, " +
                "not mine\" report",
            call.contains("progressionDisplayLevel")
        )
        assertTrue(
            "the row does not ask whether the level system prescribes this exercise at all, so a " +
                "custom-mode row (or a movement with no ladder) loses its reps range",
            call.contains("isExerciseLevelManaged")
        )
    }

    /** The card prints the level line when the system prescribes the row, and keeps reps when not. */
    @Test
    fun theCardSwitchesOnTheLevelSystemAndNotOnABlank() {
        val at = components.indexOf("fun ExpandableExerciseCard(")
        assertTrue("ExpandableExerciseCard is gone", at > 0)
        val card = components.substring(at, components.indexOf("private fun ", at).let {
            if (it == -1) components.length else it
        })
        assertTrue(
            "the card no longer takes the level, so nothing can print it",
            card.contains("levelManaged: Boolean = false") && card.contains("level: Int? = null")
        )
        assertTrue(
            "the card never calls exerciseLevelSummaryLabel, so the row still prints the reps range",
            card.contains("exerciseLevelSummaryLabel(exercise.targetSets, level)")
        )
        assertTrue(
            "the card does not keep the reps line for the rows the level system does not " +
                "prescribe - a custom-mode exercise has no level to show",
            card.contains("exerciseSetsSummaryLabel(")
        )
    }

    /**
     * A level nobody has chosen is not a number.
     *
     * These pin the "New" half, and that the label cannot reach for the derived value: a fallback
     * to `progressionDisplayLevel` inside it would restore the reported behaviour silently, with
     * every other case here still green.
     */
    @Test
    fun anUnchosenLevelSaysNewInsteadOfTheSetupDefault() {
        val label = bodyOf(components, "fun exerciseLevelSummaryLabel(", "")
        assertTrue(
            "the level line does not say \"New\" for an unchosen level: it prints a number the " +
                "user never picked",
            label.contains("""getString("level_new")""")
        )
        assertTrue(
            "the level line does not use the one level format the rest of the app uses",
            label.contains("""getString("level_word_n"""")
        )
        assertFalse(
            "the level line falls back to a DERIVED level, which is the setup default wearing the " +
                "user's name again",
            label.contains("progressionDisplayLevel") || label.contains("currentProgressionState")
        )
    }

    /**
     * ...and the read behind the label really does answer null when nothing is stored.
     *
     * A false here would be silent: a `storedProgressionLevel` that fell through to
     * `startingPrescription` would print "Level 5" and every case above would still pass.
     */
    @Test
    fun theStoredReadDoesNotFallBackToAStartingPrescription() {
        val body = bodyOf(viewModel, "fun storedProgressionLevel(", "    ")
        assertTrue(
            "storedProgressionLevel no longer reads the stored prefs entry",
            body.contains("progressionKey(exerciseName)") && body.contains("deserialize(")
        )
        assertFalse(
            "storedProgressionLevel falls back to a starting prescription, so it answers with the " +
                "setup default for an exercise the user has never levelled - the exact value this " +
                "read exists to keep out of the label",
            body.contains("startingPrescription") || body.contains("Difficulty")
        )
    }
}
