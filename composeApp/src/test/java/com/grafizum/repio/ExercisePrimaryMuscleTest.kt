package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the exercise → primary-muscle data behind the "trained muscles" label
 * and the Analytics muscle focus. The pool lists every muscle an exercise works
 * (rows sit under Lats, Upper Back and the deltoids), so classification must not
 * depend on the order those lists are scanned — Dumbbell Rows being shown as a
 * shoulder exercise is the regression this file exists to prevent.
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

    @Test
    fun primaryMuscleIsAlwaysOneOfTheGroupsThePoolListsTheExerciseUnder() {
        val groups = WorkoutTemplates.getAllMuscleGroups()
        val poolByGroup = groups.associateWith { g ->
            WorkoutTemplates.getExercisesForEquipment(WorkoutTemplates.EQUIPMENT_ALL, g)
                .map { it.lowercase() }
                .toSet()
        }
        WorkoutTemplates.getAllExerciseNames().forEach { name ->
            val primary = WorkoutTemplates.getMuscleGroupForExercise(name)
            val containing = groups.filter { primary in (poolByGroup[it] ?: emptySet()) || it == primary }
            assertTrue(
                "'$name' is classified as '$primary', which the pool never lists it under",
                containing.isNotEmpty()
            )
        }
    }

    // --- The specific misclassifications this change fixes -------------------

    @Test
    fun rowsArePullMusclesNotShoulders() {
        // The reported bug: Dumbbell Rows showed as a shoulder (Lateral Delts).
        listOf("Dumbbell Rows", "Barbell Rows", "Hammer Strength Rows", "Seated Rows").forEach { name ->
            assertEquals(name, "Lats", WorkoutTemplates.getMuscleGroupForExercise(name))
            assertEquals(name, "Back", WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(name)))
        }
        assertEquals("Upper Back", WorkoutTemplates.getMuscleGroupForExercise("T-Bar Rows"))
        assertEquals("Upper Back", WorkoutTemplates.getMuscleGroupForExercise("Inverted Rows"))
        assertEquals("Lats", WorkoutTemplates.getMuscleGroupForExercise("Dumbbell Pullovers"))
    }

    @Test
    fun shrugsAreTrapsNotAGenericUpperBack() {
        listOf("Barbell Shrugs", "Dumbbell Shrugs", "Cable Shrugs", "Machine Shrugs").forEach { name ->
            assertEquals(name, "Upper Traps", WorkoutTemplates.getMuscleGroupForExercise(name))
        }
    }

    @Test
    fun facePullsStayOnTheRearDelts() {
        listOf("Cable Face Pulls", "Dumbbell Face Pulls", "Barbell Face Pulls").forEach { name ->
            assertEquals(name, "Rear Deltoids", WorkoutTemplates.getMuscleGroupForExercise(name))
        }
    }

    @Test
    fun pressingAndPushingExercisesAreChestNotShouldersOrTriceps() {
        listOf(
            "Push-ups", "Dumbbell Floor Presses", "Barbell Floor Presses",
            "Barbell Bench Presses", "Incline Dumbbell Presses", "Cable Flyes",
            "Pec Decks", "Chest Press Machines"
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
    fun planksAreCoreNotSpinalErectors() {
        assertEquals("Abs", WorkoutTemplates.getMuscleGroupForExercise("Planks"))
        assertEquals("Abs", WorkoutTemplates.getMuscleGroupForExercise("Mountain Climbers"))
        assertEquals("Spinal Erectors", WorkoutTemplates.getMuscleGroupForExercise("Supermans"))
        assertEquals("Spinal Erectors", WorkoutTemplates.getMuscleGroupForExercise("Barbell Deadlifts"))
    }

    @Test
    fun hipExercisesGoToTheRightHipGroup() {
        // Side-lying work abducts the hip; it used to fall through to Hip Adductors.
        listOf(
            "Side Lying Leg Raises", "Side Plank Leg Raises", "Clamshells",
            "Abductor Machines", "Dumbbell Side Lunges", "Side Steps"
        ).forEach { name ->
            assertEquals(name, "Hip Abductors", WorkoutTemplates.getMuscleGroupForExercise(name))
        }
        listOf("Adductor Machines", "Sumo Squats", "Copenhagen Planks", "Cossack Squats").forEach { name ->
            assertEquals(name, "Hip Adductors", WorkoutTemplates.getMuscleGroupForExercise(name))
        }
    }

    @Test
    fun hingesAndBridgesGoToHamstringsGlutesAndErectors() {
        listOf("Barbell Romanian Deadlifts", "Dumbbell Romanian Deadlifts", "Stiff Leg Barbell Deadlifts")
            .forEach { assertEquals(it, "Hamstrings", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Glute Bridges", "Barbell Glute Bridges", "Dumbbell Hip Thrusts", "Fire Hydrants")
            .forEach { assertEquals(it, "Glutes", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Barbell Deadlifts", "Barbell Rack Pulls", "Back Extensions")
            .forEach { assertEquals(it, "Spinal Erectors", WorkoutTemplates.getMuscleGroupForExercise(it)) }
    }

    @Test
    fun rotationAndSideBendWorkIsObliques() {
        listOf("Russian Twists", "Dumbbell Side Bends", "Side Planks", "Cable Woodchoppers", "Landmine Twists")
            .forEach { assertEquals(it, "Obliques", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Crunches", "Hanging Leg Raises", "Ab Wheels", "V-Ups")
            .forEach { assertEquals(it, "Abs", WorkoutTemplates.getMuscleGroupForExercise(it)) }
    }

    @Test
    fun armsKeepTheirOwnGroups() {
        listOf("Dumbbell Curls", "Barbell Curls", "Preacher Curls", "Dumbbell Hammer Curls")
            .forEach { assertEquals(it, "Biceps", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Tricep Pushdowns", "Barbell Skullcrushers", "Dips", "Overhead Cable Extensions")
            .forEach { assertEquals(it, "Triceps", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Barbell Wrist Curls", "Farmer's Walks", "Barbell Reverse Curls")
            .forEach { assertEquals(it, "Forearms", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        // A chin-up is a pull first; the biceps are a secondary mover.
        assertEquals("Lats", WorkoutTemplates.getMuscleGroupForExercise("Chin-ups"))
    }

    @Test
    fun pressingMovementsAllResolveSomewhereAndAreNotGenericShoulders() {
        listOf("Military Presses", "Dumbbell Shoulder Presses", "Arnold Presses", "Landmine Presses")
            .forEach { assertEquals(it, "Front Deltoids", WorkoutTemplates.getMuscleGroupForExercise(it)) }
        listOf("Dumbbell Lateral Raises", "Machine Lateral Raises", "Upright Barbell Rows")
            .forEach { assertEquals(it, "Lateral Deltoids", WorkoutTemplates.getMuscleGroupForExercise(it)) }
    }
}
