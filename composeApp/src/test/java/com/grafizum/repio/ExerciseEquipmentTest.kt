package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the authoritative equipment table behind the equipment filter.
 *
 * The filter used to be guessed from the exercise name and from whichever muscle
 * lists mentioned the exercise, so an exercise could appear under two categories
 * at once (and the same movement could be listed twice under different names —
 * "Hammer Curls" *and* "Dumbbell Hammer Curls"). This file pins down that every
 * pool exercise is assigned exactly one real category and that the duplicate
 * aliases stay out of the pool.
 */
class ExerciseEquipmentTest {

    private val pool = WorkoutTemplates.getAllExerciseNames().map { it.lowercase() }.toSet()
    private val bodyweight = WorkoutTemplates.EQUIPMENT_BODYWEIGHT
    private val dumbbell = WorkoutTemplates.EQUIPMENT_DUMBBELL
    private val barbell = WorkoutTemplates.EQUIPMENT_BARBELL
    private val gym = WorkoutTemplates.EQUIPMENT_GYM

    @Test
    fun everyPoolExerciseHasExactlyOneEquipmentCategory() {
        val table = WorkoutTemplates.getEquipmentTableNames()
        val missing = pool - table.toSet()
        assertTrue("Pool exercises with no equipment entry: $missing", missing.isEmpty())

        val strays = table.filter { it !in pool }
        assertTrue("Equipment entries that aren't pool exercises: $strays", strays.isEmpty())

        assertEquals("Each pool exercise must be classified exactly once", pool.size, table.size)
    }

    @Test
    fun everyExerciseResolvesToAKnownCategory() {
        WorkoutTemplates.getAllExerciseNames().forEach { name ->
            val equipment = WorkoutTemplates.getEquipmentForExercise(name)
            assertTrue(
                "'$name' resolved to '$equipment', which is not a known equipment category",
                equipment in WorkoutTemplates.allEquipmentCategories
            )
        }
    }

    @Test
    fun theEquipmentFilterNeverLeaksAnotherCategory() {
        WorkoutTemplates.allEquipmentCategories.forEach { category ->
            WorkoutTemplates.getAllExercisesForEquipment(category).forEach { name ->
                assertEquals(
                    "'$name' is listed under '$category' but needs '${WorkoutTemplates.getEquipmentForExercise(name)}'",
                    category,
                    WorkoutTemplates.getEquipmentForExercise(name)
                )
            }
        }
    }

    @Test
    fun equipmentIsReadFromTheMovementNotFromTheNameOrTheMuscleList() {
        // Hammer curls are dumbbells, not the bodyweight/gym entries they used to be
        // filed under, and barbell movements stay barbell even though the gym lists
        // mention them for those muscles.
        assertEquals(dumbbell, WorkoutTemplates.getEquipmentForExercise("Dumbbell Hammer Curls"))
        assertEquals(dumbbell, WorkoutTemplates.getEquipmentForExercise("Farmer's Walks"))
        assertEquals(dumbbell, WorkoutTemplates.getEquipmentForExercise("Goblet Squats"))
        assertEquals(barbell, WorkoutTemplates.getEquipmentForExercise("Barbell Deadlifts"))
        assertEquals(barbell, WorkoutTemplates.getEquipmentForExercise("Military Presses"))
        assertEquals(barbell, WorkoutTemplates.getEquipmentForExercise("Landmine Presses"))
        assertEquals(bodyweight, WorkoutTemplates.getEquipmentForExercise("Pull-ups"))
        assertEquals(bodyweight, WorkoutTemplates.getEquipmentForExercise("Push-ups"))
        assertEquals(bodyweight, WorkoutTemplates.getEquipmentForExercise("Glute Bridges"))
        assertEquals(gym, WorkoutTemplates.getEquipmentForExercise("Lat Pulldowns"))
        assertEquals(gym, WorkoutTemplates.getEquipmentForExercise("Pec Decks"))
        assertEquals(gym, WorkoutTemplates.getEquipmentForExercise("Cable Face Pulls"))
    }

    @Test
    fun aCategoryOnlyContainsItsOwnMovements() {
        val dumb = WorkoutTemplates.getAllExercisesForEquipment(dumbbell)
        assertTrue(dumb.contains("Dumbbell Hammer Curls"))
        assertFalse(dumb.contains("Barbell Squats"))
        assertFalse(dumb.contains("Lat Pulldowns"))

        val free = WorkoutTemplates.getAllExercisesForEquipment(bodyweight)
        assertTrue(free.contains("Pull-ups"))
        assertFalse(free.contains("Dumbbell Rows"))
        assertFalse(free.contains("Leg Presses"))
    }

    @Test
    fun duplicateAliasesHaveBeenMergedAway() {
        // Each of these was a second name for an exercise that is already in the
        // pool under its canonical name, so the picker showed the same movement twice.
        listOf(
            "hammer curls", "deadlifts", "romanian deadlifts", "bridges", "skullcrushers",
            "face pulls", "rope face pulls", "side raises", "dips (bars)", "chest dips (bars)",
            "good mornings", "inverted curls (bodyweight)", "reverse hypers (glute focus)",
            "reverse curls", "hip thrusts", "pullovers", "presses", "australian pull-ups (lats)",
            "lying leg curls", "calf press on leg press"
        ).forEach { alias ->
            assertFalse("'$alias' is a duplicate alias that must not be in the pool", alias in pool)
        }
    }

    @Test
    fun theCanonicalSurvivorsAreStillThere() {
        listOf(
            "Dumbbell Hammer Curls", "Barbell Deadlifts", "Barbell Romanian Deadlifts",
            "Glute Bridges", "Barbell Skullcrushers", "Cable Face Pulls",
            "Dumbbell Lateral Raises", "Dips", "Barbell Good Mornings", "Inverted Curls",
            "Reverse Hypers", "Barbell Reverse Curls", "Barbell Hip Thrusts", "Inverted Rows"
        ).forEach { canonical ->
            assertTrue("'$canonical' must stay in the pool", canonical.lowercase() in pool)
        }
    }

    @Test
    fun unknownExercisesHaveNoEquipment() {
        assertEquals("", WorkoutTemplates.getEquipmentForExercise("My Custom Leg Machine"))
        assertEquals("", WorkoutTemplates.getEquipmentForExercise(""))
    }
}
