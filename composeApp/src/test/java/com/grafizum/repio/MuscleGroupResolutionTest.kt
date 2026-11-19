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
    fun rowsResolveToTheirPreciseMuscleInsteadOfGettingStuckOnBack() {
        // Regression: rows stored under the broad "Back" group stayed there when
        // the resolver could not refine them.
        assertEquals("Lats", resolvePreciseMuscleGroup("Bent-Over Barbell Row", "Back"))
        assertEquals("Lats", resolvePreciseMuscleGroup("Bent-Over Dumbbell Row", "Back"))
    }

    @Test
    fun broadStoredGroupsResolveToTheirPreciseMuscle() {
        assertEquals("Quadriceps", resolvePreciseMuscleGroup("Barbell Back Squat", "Legs"))
        assertEquals("Hamstrings", resolvePreciseMuscleGroup("Romanian Deadlift", "Legs"))
        assertEquals("Quadriceps", resolvePreciseMuscleGroup("Dumbbell Lunge", "Legs"))
        assertEquals("Front Deltoids", resolvePreciseMuscleGroup("Dumbbell Shoulder Press", "Shoulders"))
        assertEquals("Lateral Deltoids", resolvePreciseMuscleGroup("Dumbbell Lateral Raise", "Shoulders"))
        assertEquals("Biceps", resolvePreciseMuscleGroup("Dumbbell Bicep Curl", "Biceps"))
        assertEquals("Abs", resolvePreciseMuscleGroup("Crunches", "Abs"))
    }

    @Test
    fun poolWinsWhenTheStoredGroupDisagrees() {
        // A pull-up stored (wrongly) as a shoulder is still a back exercise;
        // push-ups stored under "Chest" stay Pectorals.
        assertEquals("Lats", resolvePreciseMuscleGroup("Pull-Up", "Shoulders"))
        assertEquals("Pectorals", resolvePreciseMuscleGroup("Push-Up", "Chest"))
        assertEquals("Pectorals", resolvePreciseMuscleGroup("Dumbbell Fly", "Shoulders"))
    }

    @Test
    fun preciseStoredGroupsAreKeptWhenThePoolAgrees() {
        assertEquals("Biceps", resolvePreciseMuscleGroup("Dumbbell Bicep Curl", "Biceps"))
        assertEquals("Triceps", resolvePreciseMuscleGroup("Bench Dips", "Triceps"))
        assertEquals("Quadriceps", resolvePreciseMuscleGroup("Barbell Back Squat", "Quadriceps"))
        assertEquals("Pectorals", resolvePreciseMuscleGroup("Push-Up", "Chest"))
    }

    @Test
    fun unknownExercisesKeepTheirStoredGroup() {
        // No real data exists for custom exercises -> never guess. The resolver
        // contract after the rebuild: names outside the pool keep the stored
        // group (which is exactly what a log already carries).
        assertEquals("Legs", resolvePreciseMuscleGroup("My Custom Leg Machine", "Legs"))
        assertEquals("Chest", resolvePreciseMuscleGroup("", "Chest"))
        assertEquals("Back", resolvePreciseMuscleGroup("Something Made Up", "Back"))
        assertEquals("Whatever Was Stored", resolvePreciseMuscleGroup("Totally Unknown", "Whatever Was Stored"))
    }
}
