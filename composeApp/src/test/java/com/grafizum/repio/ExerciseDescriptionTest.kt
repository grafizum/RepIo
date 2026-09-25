package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.getExerciseDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the exercise description pool. Since the rebuild, the descriptions
 * come from the dataset through the generated catalog, so coverage is a
 * *data property* (asserted against the catalog itself) while the app-facing
 * contract stays: every exercise that carries a dataset text resolves through
 * the delegate, routine exercises always do, and the delegate agrees with the
 * catalog exactly.
 */
class ExerciseDescriptionTest {

    @Test
    fun everyPoolExerciseWithATextResolvesThroughTheDelegate() {
        val names = WorkoutTemplates.getAllExerciseNames()
        assertTrue("exercise pool should not be empty", names.isNotEmpty())

        val missing = names.filter {
            ExerciseCatalog.entryFor(it)?.description?.isNotBlank() == true &&
                getExerciseDescription(it) == null
        }
        assertEquals(
            "every catalog entry carrying a text must resolve through the delegate",
            emptyList<String>(),
            missing
        )
    }

    @Test
    fun theDelegateAgreesWithTheCatalog() {
        // Blank dataset rows are allowed by the pipeline (rating-only entries);
        // beyond that the delegate must agree with the catalog exactly — no
        // drift, no gaps of its own.
        val expected = ExerciseCatalog.ENTRIES.count { it.description.isNotBlank() }
        val actual = WorkoutTemplates.getAllExerciseNames().count { getExerciseDescription(it) != null }
        assertEquals(expected, actual)
    }

    @Test
    fun everyCatalogEntryCarriesADescription() {
        // The v5 CSV carries no descriptions; the pipeline merges them from the
        // previous catalog where names reconciled (208 of 601 rows) and fills
        // the rest from the hand-written table in
        // tools/exercise-db/data/hand_descriptions.py. The pool is now fully
        // described — a new pipeline row must come with a text of its own.
        val blank = ExerciseCatalog.ENTRIES.filter { it.description.isBlank() }
        assertTrue(
            "every catalog entry must carry a description, but these are blank: " +
                blank.joinToString { it.name },
            blank.isEmpty()
        )
    }

    @Test
    fun userFacingRoutineExercisesResolve() {
        val routineNames = listOf(
            "Bent-Over Barbell Row", "Bent-Over Dumbbell Row", "Dumbbell Pullover",
            "Dumbbell Bicep Curl", "Dumbbell Hammer Curl", "Dumbbell Shoulder Press",
            "Dumbbell Lateral Raise", "Crunches", "Barbell Bench Press",
            "Barbell Curl", "Hanging Leg Raise",
            "Barbell Back Squat", "Romanian Deadlift", "Dumbbell Lunge",
            "Plank", "Dumbbell Fly"
        )
        routineNames.forEach { name ->
            assertNotNull("no description for '$name'", getExerciseDescription(name))
        }
    }

    @Test
    fun curlVariantsDescribeTheirOwnMovement() {
        val dumbbellCurls = getExerciseDescription("Dumbbell Bicep Curl")
        val hammerCurls = getExerciseDescription("Dumbbell Hammer Curl")
        val preacherCurls = getExerciseDescription("Preacher Curl")
        assertTrue(dumbbellCurls != null && hammerCurls != null && preacherCurls != null)
        assertEquals(
            "distinct curl variants must not share one text",
            3,
            setOf(dumbbellCurls, hammerCurls, preacherCurls).size
        )
    }
}
