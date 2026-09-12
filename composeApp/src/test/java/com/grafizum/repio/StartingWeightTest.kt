package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the starting weight suggested for a planned exercise: it must respond to
 * the chosen difficulty, stay on loadable steps, and never be invented for
 * movements that carry no load.
 *
 * The weight is only ever a fallback — a weight the plan set, or one the user has
 * already worked up to, wins over this estimate. That precedence lives in the
 * workout session state, not here.
 */
class StartingWeightTest {

    @Test
    fun exercisesWithoutLoadStartAtZero() {
        assertEquals(0.0, WorkoutTemplates.getStartingWeightKg("Push-ups", "Medium"), 0.0)
        assertEquals(0.0, WorkoutTemplates.getStartingWeightKg("Pull-ups", "Hard"), 0.0)
        // Timed holds carry no weight either.
        assertEquals(0.0, WorkoutTemplates.getStartingWeightKg("Planks", "Hard"), 0.0)
        assertEquals(0.0, WorkoutTemplates.getStartingWeightKg("", "Hard"), 0.0)
    }

    @Test
    fun harderDifficultyStartsHeavier() {
        val easy = WorkoutTemplates.getStartingWeightKg("Barbell Squats", "Easy")
        val medium = WorkoutTemplates.getStartingWeightKg("Barbell Squats", "Medium")
        val hard = WorkoutTemplates.getStartingWeightKg("Barbell Squats", "Hard")
        assertTrue("easy < medium ($easy vs $medium)", easy < medium)
        assertTrue("medium < hard ($medium vs $hard)", medium < hard)
    }

    @Test
    fun compoundsStartHeavierThanIsolation() {
        val compound = WorkoutTemplates.getStartingWeightKg("Dumbbell Shoulder Presses", "Medium")
        val isolation = WorkoutTemplates.getStartingWeightKg("Dumbbell Curls", "Medium")
        assertTrue("press > curl ($compound vs $isolation)", compound > isolation)

        val barbell = WorkoutTemplates.getStartingWeightKg("Barbell Squats", "Medium")
        val machine = WorkoutTemplates.getStartingWeightKg("Cable Flyes", "Medium")
        assertTrue("barbell squat > cable fly ($barbell vs $machine)", barbell > machine)
    }

    @Test
    fun everySuggestionLandsOnALoadableStep() {
        val names = listOf(
            "Barbell Squats", "Dumbbell Curls", "Dumbbell Shoulder Presses",
            "Cable Flyes", "Leg Press Machines", "Barbell Rows"
        )
        names.forEach { name ->
            listOf("Easy", "Medium", "Hard").forEach { intensity ->
                val kg = WorkoutTemplates.getStartingWeightKg(name, intensity)
                assertTrue("expected a load for $name", kg > 0.0)
                if (kg >= 10.0) {
                    val ratio = kg / 2.5
                    assertEquals("$name @$intensity = ${kg}kg is off the 2.5kg step", ratio, kotlin.math.round(ratio), 1e-9)
                } else {
                    assertEquals("$name @$intensity = ${kg}kg is not a whole kg", kg, kotlin.math.round(kg), 1e-9)
                }
            }
        }
    }

    @Test
    fun assistedBuildRepsFollowTheGoalNotTheDifficulty() {
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Chest"),
            selectedDays = listOf("MONDAY"),
            intensity = "Hard",
            workoutGoal = "goal_bodybuilding"
        )
        assertTrue("expected the assisted build to create exercises", program.exercises.isNotEmpty())
        program.exercises.forEach { ex ->
            assertEquals("Hard should build 5-set exercises", 5, ex.targetSets)
            assertEquals(
                "reps must match the goal scheme for ${ex.name}",
                WorkoutTemplates.getGoalReps(ex.name, "goal_bodybuilding"),
                ex.targetReps
            )
            // Training style is gone, so the generator never invents a tempo cue.
            assertEquals("generator must not set a tempo", "", ex.tempo)
        }
    }

    @Test
    fun assistedBuildLowersVolumeOnEasy() {
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Chest"),
            selectedDays = listOf("MONDAY"),
            intensity = "Easy",
            workoutGoal = "goal_bodybuilding"
        )
        program.exercises.forEach { ex ->
            assertEquals("Easy should build 3-set exercises", 3, ex.targetSets)
        }
    }
}
