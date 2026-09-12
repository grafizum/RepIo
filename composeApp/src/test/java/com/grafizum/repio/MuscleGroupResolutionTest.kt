package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.resolvePreciseMuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the Analytics "Muscle Group Focus" resolution: every exercise in the
 * built-in pool must resolve to a precise muscle (Lats, Quadriceps, Front
 * Deltoids, …) instead of staying on its broad topic (Back, Legs, Shoulders,
 * …), using the pool as the real data for what each exercise trains.
 */
class MuscleGroupResolutionTest {

    private val broadTopics = setOf("Back", "Shoulders", "Legs", "Arms", "Core")

    @Test
    fun everyTemplateExerciseResolvesToAPreciseMuscle() {
        WorkoutTemplates.allTemplates.forEach { template ->
            template.exercises.forEach { ex ->
                val resolved = resolvePreciseMuscleGroup(ex.name, ex.muscleGroup)
                assertTrue(
                    "'${ex.name}' (stored '${ex.muscleGroup}') still resolves to the broad topic '$resolved'",
                    resolved !in broadTopics
                )
            }
        }
    }

    @Test
    fun rowsResolveToLatsInsteadOfGettingStuckOnBack() {
        // Regression: the pool lists rows under Rear Deltoids / Lateral Deltoids
        // too, and the old first-match lookup made the resolver give up and keep
        // the broad "Back" group.
        assertEquals("Lats", resolvePreciseMuscleGroup("Barbell Rows", "Back"))
        assertEquals("Lats", resolvePreciseMuscleGroup("Dumbbell Rows", "Back"))
        assertEquals("Lats", resolvePreciseMuscleGroup("Dumbbell Pullovers", "Back"))
    }

    @Test
    fun broadStoredGroupsResolveToTheirPreciseMuscle() {
        assertEquals("Quadriceps", resolvePreciseMuscleGroup("Barbell Squats", "Legs"))
        assertEquals("Hamstrings", resolvePreciseMuscleGroup("Barbell Romanian Deadlifts", "Legs"))
        assertEquals("Quadriceps", resolvePreciseMuscleGroup("Dumbbell Lunges", "Legs"))
        assertEquals("Front Deltoids", resolvePreciseMuscleGroup("Dumbbell Shoulder Presses", "Shoulders"))
        assertEquals("Lateral Deltoids", resolvePreciseMuscleGroup("Dumbbell Lateral Raises", "Shoulders"))
        assertEquals("Biceps", resolvePreciseMuscleGroup("Dumbbell Curls", "Biceps"))
        assertEquals("Abs", resolvePreciseMuscleGroup("Crunches", "Abs"))
    }

    @Test
    fun poolWinsWhenTheStoredGroupDisagrees() {
        // Dumbbell Flyes are a chest exercise in the pool, even though the old
        // default routine stored them under "Shoulders".
        assertEquals("Pectorals", resolvePreciseMuscleGroup("Dumbbell Flyes", "Shoulders"))
        assertEquals("Pectorals", resolvePreciseMuscleGroup("Dumbbell Floor Presses", "Chest"))
    }

    @Test
    fun preciseStoredGroupsAreKeptWhenThePoolAgrees() {
        assertEquals("Biceps", resolvePreciseMuscleGroup("Dumbbell Curls", "Biceps"))
        assertEquals("Triceps", resolvePreciseMuscleGroup("Dips", "Triceps"))
        assertEquals("Quadriceps", resolvePreciseMuscleGroup("Barbell Squats", "Quadriceps"))
        assertEquals("Pectorals", resolvePreciseMuscleGroup("Push-ups", "Chest"))
    }

    @Test
    fun unknownExercisesKeepTheirStoredGroup() {
        // No real data exists for custom exercises -> never guess.
        assertEquals("Legs", resolvePreciseMuscleGroup("My Custom Leg Machine", "Legs"))
        assertEquals("Chest", resolvePreciseMuscleGroup("", "Chest"))
        assertEquals("Back", resolvePreciseMuscleGroup("Something Made Up", "Back"))
    }
}