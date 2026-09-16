package com.grafizum.repio

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
        assertTrue("Squats missing from the Legs filter", results.contains("Squats"))
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
        assertEquals(
            pool.sorted(),
            filteredExercisesForPickers(pool, null, emptyList())
        )
    }

    @Test
    fun equipmentChipsNarrowTheMuscleFilter() {
        val dumbbellLegs = filteredExercisesForPickers(
            pool,
            "Legs",
            listOf(WorkoutTemplates.EQUIPMENT_DUMBBELL)
        )
        assertTrue(dumbbellLegs.isNotEmpty())
        dumbbellLegs.forEach { name ->
            assertEquals(
                WorkoutTemplates.EQUIPMENT_DUMBBELL,
                WorkoutTemplates.getEquipmentForExercise(name)
            )
        }
    }

    @Test
    fun theResultStaysInsideTheGivenPool() {
        val small = listOf("Squats", "Push-ups")
        val results = filteredExercisesForPickers(small, null, emptyList())
        assertEquals(small.sorted(), results)
    }
}
