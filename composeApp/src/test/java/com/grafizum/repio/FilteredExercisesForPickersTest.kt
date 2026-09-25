package com.grafizum.repio

import com.grafizum.repio.data.ExercisePopularity
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.filteredExercisesForPickers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the filter lookup the add-exercise dialog shares with the replace
 * dialog: a broad body part ("Legs") must offer every exercise whose own body
 * part is Legs, an empty selection means everything, and the equipment chips
 * narrow the pool independently of the muscle filter.
 */
class FilteredExercisesForPickersTest {

    private val pool = WorkoutTemplates.getAllExerciseNames()

    @Test
    fun selectingLegsOffersOnlyLegExercises() {
        val results = filteredExercisesForPickers(pool, "Legs", emptyList())
        assertTrue("Legs filter returned nothing", results.isNotEmpty())
        assertTrue("Barbell Squat missing from the Legs filter", results.contains("Barbell Back Squat"))
        results.forEach { name ->
            assertEquals(
                "'$name' offered by the Legs filter is not a leg exercise",
                "Legs",
                WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(name))
            )
        }
    }

    @Test
    fun nullMuscleGroupOffersTheWholePool() {
        // Popularity order, not alphabetical: the staples the user would
        // actually pick scroll first, unranked names trail alphabetically.
        val results = filteredExercisesForPickers(pool, null, emptyList())
        assertEquals(pool.size, results.size)
        assertEquals("Barbell Bench Press", results.first())
        // The whole pool keeps a deterministic order: ranked block first
        // (popularity), then the unranked tail (alphabetical).
        val ranked = ExercisePopularity.rankedNames().toSet()
        assertEquals(
            results.filter { it in ranked },
            ExercisePopularity.sortedByPopularity(results.filter { it in ranked })
        )
        assertEquals(
            results.filter { it !in ranked },
            results.filter { it !in ranked }.sorted()
        )
    }

    @Test
    fun unrankedNamesSortAfterEveryRankedOne() {
        val results = filteredExercisesForPickers(pool, null, emptyList())
        val firstUnranked = results.indexOfFirst { it !in ExercisePopularity.rankedNames().toSet() }
        assertTrue("expected unranked names in the pool", firstUnranked >= 0)
        results.drop(firstUnranked).forEach { name ->
            assertTrue("'$name' is ranked but appears after the unranked tail", name !in ExercisePopularity.rankedNames().toSet())
        }
    }

    @Test
    fun equipmentChipsNarrowTheMuscleFilter() {
        val barbellLegs = filteredExercisesForPickers(
            pool,
            "Legs",
            listOf(WorkoutTemplates.EQUIPMENT_BARBELL)
        )
        assertTrue(barbellLegs.isNotEmpty())
        barbellLegs.forEach { name ->
            assertEquals(
                WorkoutTemplates.EQUIPMENT_BARBELL,
                WorkoutTemplates.getEquipmentForExercise(name)
            )
        }
    }

    @Test
    fun theResultStaysInsideTheGivenPool() {
        val small = listOf("Push-Up", "Barbell Back Squat")
        val results = filteredExercisesForPickers(small, null, emptyList())
        // Same members, popularity order (Barbell Back Squat is the top staple).
        assertEquals(small.size, results.size)
        assertEquals(listOf("Barbell Back Squat", "Push-Up"), results)
    }
}
