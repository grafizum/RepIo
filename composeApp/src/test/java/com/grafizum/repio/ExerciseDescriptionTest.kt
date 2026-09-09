package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.getExerciseDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the exercise description pool: every built-in exercise must have its
 * own description, and distinct exercises must never share one (the regression
 * where "Dumbbell Curls" and "Concentration Curls" showed identical text).
 */
class ExerciseDescriptionTest {

    @Test
    fun everyPoolExerciseHasItsOwnDescription() {
        val names = WorkoutTemplates.getAllExerciseNames()
        assertTrue("exercise pool should not be empty", names.isNotEmpty())

        val missing = names.filter { getExerciseDescription(it) == null }
        assertEquals("every pool exercise must resolve to a description", emptyList<String>(), missing)

        val descriptions = names.map { getExerciseDescription(it)!! }
        assertEquals(
            "distinct exercises must not share a description",
            descriptions.size,
            descriptions.distinct().size
        )
    }

    @Test
    fun userFacingRoutineExercisesResolve() {
        val routineNames = listOf(
            "Barbell Rows", "Dumbbell Rows", "Dumbbell Pullovers", "Dumbbell Curls",
            "Dumbbell Hammer Curls", "Dumbbell Shoulder Presses", "Dumbbell Lateral Raises",
            "Crunches", "Barbell Bench Presses", "Dumbbell Bench Presses", "Barbell Curls",
            "Leg Raises", "Barbell Squats", "Barbell Romanian Deadlifts", "Dumbbell Lunges",
            "Planks", "Dumbbell Flyes", "Dumbbell Tricep Extensions", "Concentration Curls",
            "Hack Squats", "Sumo Squats", "Seated Rows", "Incline Dumbbell Curls"
        )
        routineNames.forEach { name ->
            assertNotNull("no description for '$name'", getExerciseDescription(name))
        }
    }

    @Test
    fun curlsVariantsDescribeTheirOwnMovement() {
        val dumbbellCurls = getExerciseDescription("Dumbbell Curls")
        val concentrationCurls = getExerciseDescription("Concentration Curls")
        val preacherCurls = getExerciseDescription("Preacher Curls")
        assertTrue(dumbbellCurls != null && concentrationCurls != null && preacherCurls != null)
        assertEquals(
            setOf(dumbbellCurls, concentrationCurls, preacherCurls).size,
            3
        )
        // Concentration curls must mention the braced-arm setup that defines them.
        assertTrue(concentrationCurls!!.contains("inner thigh", ignoreCase = true))
    }
}