package com.grafizum.repio

import com.grafizum.repio.data.ExercisePopularity
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the popularity ranking that de-randomizes the recommendations: the
 * generator picks from the head of each pool and the pickers list their
 * suggestions in this order, so the table must stay in lockstep with the
 * pool — every entry a real catalog name, no pool exercise ranked twice.
 */
class ExercisePopularityTest {

    private val pool = WorkoutTemplates.getAllExerciseNames()

    @Test
    fun everyRankedNameIsAPoolExercise() {
        val poolLower = pool.map { it.lowercase() }.toSet()
        val strays = ExercisePopularity.rankedNames().filter { it.lowercase() !in poolLower }
        assertTrue("Ranked names that are not pool exercises: $strays", strays.isEmpty())
    }

    @Test
    fun noNameIsRankedTwice() {
        val names = ExercisePopularity.rankedNames().map { it.lowercase() }
        assertEquals(
            "duplicate ranked entries: ${
                names.groupBy { it }.filterValues { it.size > 1 }.keys
            }",
            names.size,
            names.toSet().size
        )
    }

    @Test
    fun everyEquipmentCategoryHasARankedStaple() {
        // A category with no ranked entry falls back to alphabetical order —
        // exactly the "random recommendations" bug this table exists to fix.
        WorkoutTemplates.allEquipmentCategories.forEach { category ->
            val staples = WorkoutTemplates.getAllExercisesForEquipment(category)
                .filter { ExercisePopularity.isRanked(it) }
            assertTrue(
                "'$category' has no ranked staple; its picker falls back to alphabetical order",
                staples.isNotEmpty()
            )
        }
    }

    @Test
    fun theHeadOfTheTableIsTheGlobalStaples() {
        val top = ExercisePopularity.rankedNames().take(8)
        assertEquals(
            listOf(
                "Barbell Bench Press", "Barbell Back Squat", "Barbell Deadlift",
                "Bent-Over Barbell Row", "Barbell Overhead Press",
                "Push-Up", "Pull-Up", "Romanian Deadlift"
            ),
            top
        )
    }

    @Test
    fun unrankedNamesSortAfterEveryRankedName() {
        val sorted = ExercisePopularity.sortedByPopularity(pool)
        val firstUnranked = sorted.indexOfFirst { !ExercisePopularity.isRanked(it) }
        if (firstUnranked == -1) return
        sorted.drop(firstUnranked).forEach { name ->
            assertTrue(
                "'$name' is ranked but sorted after the unranked tail",
                !ExercisePopularity.isRanked(name)
            )
        }
    }

    @Test
    fun sortingIsDeterministic() {
        assertEquals(
            ExercisePopularity.sortedByPopularity(pool),
            ExercisePopularity.sortedByPopularity(pool.shuffled())
        )
    }

    @Test
    fun generatorPicksComeFromTheRankedHead() {
        // The point of the whole table: a pool's head is the staples, not
        // whichever variation catalog order surfaced first. Bodyweight builds
        // draw from a small, fully-ranked pool — exactly where the rotation
        // offset starts.
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Chest", "Quadriceps", "Abs"),
            selectedDays = listOf("MONDAY", "WEDNESDAY"),
            intensity = "Easy",
            workoutGoal = "goal_calisthenics",
            equipment = listOf(WorkoutTemplates.EQUIPMENT_BODYWEIGHT)
        )
        assertTrue("expected rows in the build", program.exercises.isNotEmpty())
        // Push-Up is the ranked chest staple; a bodyweight chest day must open
        // with it rather than an obscure variation (Plyo, Wall, Deficit …).
        val chestRows = program.exercises.filter {
            WorkoutTemplates.getMuscleGroupForExercise(it.name) == "Pectorals"
        }
        assertTrue("expected chest rows, got ${program.exercises.map { it.name }}", chestRows.isNotEmpty())
        assertEquals("Push-Up", chestRows.first().name)
    }
}
