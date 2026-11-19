package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the exercise → primary-muscle data behind the "trained muscles" label
 * and the Analytics muscle focus. Since the v5 rebuild the classification comes
 * from the generated catalog (one precise muscle per exercise); these tests
 * pin the load-bearing classifications the UI relies on and the table
 * invariants around them.
 */
class ExercisePrimaryMuscleTest {

    private val broadTopics = setOf("Back", "Shoulders", "Legs", "Arms", "Core", "Chest")

    @Test
    fun everyPoolExerciseHasAnAuthoritativePrimaryMuscle() {
        val unmapped = WorkoutTemplates.getAllExerciseNames()
            .filter { WorkoutTemplates.getPrimaryMuscleForExercise(it).isEmpty() }
        assertTrue("Pool exercises missing from the primary-muscle table: $unmapped", unmapped.isEmpty())
    }

    @Test
    fun theTableHasNoStraysAndNoDuplicates() {
        val pool = WorkoutTemplates.getAllExerciseNames().map { it.lowercase() }.toSet()
        val table = WorkoutTemplates.getPrimaryMuscleTableNames()

        val strays = table.filter { it !in pool }
        assertTrue("Table entries that aren't pool exercises: $strays", strays.isEmpty())

        // toMap() silently collapses duplicates, so equal sizes prove uniqueness.
        assertEquals(
            "The table must name each pool exercise exactly once",
            pool.size,
            table.size
        )
    }

    @Test
    fun everyExerciseResolvesToAPreciseMuscleNotABroadTopic() {
        WorkoutTemplates.getAllExerciseNames().forEach { name ->
            val resolved = WorkoutTemplates.getMuscleGroupForExercise(name)
            assertFalse("'$name' resolved to the broad topic '$resolved'", resolved in broadTopics)
            assertTrue("'$name' has no topic", WorkoutTemplates.getTopicForMuscle(resolved) != "Other")
        }
    }

    // --- The specific classifications the UI depends on -----------------------

    @Test
    fun rowsResolveToBackMuscles() {
        listOf("Bent-Over Barbell Row", "Seated Cable Row", "Lat Pulldown").forEach { name ->
            assertEquals(name, "Back", WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(name)))
        }
        assertEquals("Lats", WorkoutTemplates.getMuscleGroupForExercise("Bent-Over Barbell Row"))
    }

    @Test
    fun pressingAndPushingExercisesAreChest() {
        listOf(
            "Push-Up", "Barbell Bench Press", "Machine Chest Press",
            "Incline Dumbbell Press", "Dumbbell Fly", "Cable Fly",
            "Machine Chest Fly", "Dumbbell Pullover"
        ).forEach { name ->
            assertEquals(name, "Pectorals", WorkoutTemplates.getMuscleGroupForExercise(name))
            assertEquals(
                name,
                "Chest",
                WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(name))
            )
        }
    }

    @Test
    fun planksAndCrunchesAreCore() {
        listOf("Plank", "Crunches").forEach { name ->
            assertEquals(name, "Abs", WorkoutTemplates.getMuscleGroupForExercise(name))
        }
        assertEquals("Spinal Erectors", WorkoutTemplates.getMuscleGroupForExercise("Back Extension"))
    }

    @Test
    fun hingesGoToHamstringsAndGlutes() {
        listOf("Romanian Deadlift", "Kettlebell Swing")
            .forEach { assertEquals(it, "Hamstrings", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Barbell Glute Bridge", "Barbell Hip Thrust")
            .forEach { assertEquals(it, "Glutes", WorkoutTemplates.getMuscleGroupForExercise(it)) }
    }

    @Test
    fun squatsAndLungesAreQuadriceps() {
        listOf("Barbell Back Squat", "Dumbbell Lunge", "Bulgarian Split Squat")
            .forEach { assertEquals(it, "Quadriceps", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        // The v5 CSV names glutes as the leg press's primary mover.
        assertEquals("Glutes", WorkoutTemplates.getMuscleGroupForExercise("Leg Press"))
    }

    @Test
    fun armsKeepTheirOwnGroups() {
        listOf("Dumbbell Bicep Curl", "Barbell Curl", "Dumbbell Hammer Curl", "Preacher Curl")
            .forEach { assertEquals(it, "Biceps", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Dumbbell Skull Crusher", "Dumbbell Tricep Extension", "Bench Dips", "Cable Tricep Pushdown")
            .forEach { assertEquals(it, "Triceps", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        // A chin-up is a pull first; the biceps are a secondary mover.
        assertEquals("Lats", WorkoutTemplates.getMuscleGroupForExercise("Chin-Ups"))
    }

    @Test
    fun pressingMovementsResolveToDeltoids() {
        listOf("Dumbbell Shoulder Press", "Arnold Press")
            .forEach { assertEquals(it, "Front Deltoids", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Dumbbell Lateral Raise")
            .forEach { assertEquals(it, "Lateral Deltoids", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Dumbbell Reverse Fly", "Cable Face Pull")
            .forEach { assertEquals(it, "Rear Deltoids", WorkoutTemplates.getMuscleGroupForExercise(it)) }
    }

    @Test
    fun shrugsAreTraps() {
        listOf("Barbell Shrug", "Dumbbell Shrug").forEach { name ->
            assertEquals(name, "Upper Traps", WorkoutTemplates.getMuscleGroupForExercise(name))
        }
    }
}
