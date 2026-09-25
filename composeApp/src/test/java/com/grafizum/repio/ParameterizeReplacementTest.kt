package com.grafizum.repio

import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replacing an exercise must hand the incoming one the defaults the generator
 * would have given it — goal owns reps and rest, set count is the universal
 * Guided-mode constant (3) — not the old row's sets/reps/rest, which made
 * "Squats → Planks" keep 8–12 reps and a barbell rest timer.
 */
class ParameterizeReplacementTest {

    private fun row(
        name: String,
        muscleGroup: String,
        sets: Int = 5,
        reps: String = "8–12",
        rest: Int = 240,
        weight: Double = 40.0,
        intensity: String = "Very Heavy",
        tempo: String = "explosive",
        url: String = "http://old.example"
    ) = RoutineExerciseEntity(
        id = 12L,
        dayOfWeek = "MONDAY",
        name = name,
        muscleGroup = muscleGroup,
        targetSets = sets,
        targetReps = reps,
        targetIntensity = intensity,
        targetWeightKg = weight,
        restSeconds = rest,
        transitionRestSeconds = 120,
        videoUrl = url,
        orderIndex = 3,
        tempo = tempo
    )

    @Test
    fun `time-based replacement does not keep the old rep scheme`() {
        val replacement = WorkoutTemplates.parameterizeReplacement(
            original = row("Barbell Back Squat", "Quadriceps", reps = "1–5", rest = 240),
            newName = "Planks",
            newMuscleGroup = "Abs",
            workoutGoal = "goal_strength",
            intensity = "Hard"
        )
        // Planks are time-based: reps must become a seconds value, not "1–5",
        // and rest comes from the *goal* (bodybuilding = 90s), not the old barbell row.
        assertTrue(replacement.targetReps.contains("sec"))
        assertEquals(90, replacement.restSeconds)
        assertEquals(0L, replacement.id)
        assertEquals("Planks", replacement.name)
        assertEquals("Abs", replacement.muscleGroup)
    }

    @Test
    fun `goal owns reps and rest for the new exercise`() {
        val replacement = WorkoutTemplates.parameterizeReplacement(
            original = row("Crunches", "Abs", sets = 2, reps = "15–20", rest = 45),
            newName = "Barbell Back Squat",
            newMuscleGroup = "Quadriceps",
            workoutGoal = "goal_strength",
            intensity = "Medium"
        )
        // Strength compound: 1–5 reps, 240s rest — regardless of the old row.
        assertEquals("1–5", replacement.targetReps)
        assertEquals(240, replacement.restSeconds)
        assertEquals("Moderate", replacement.targetIntensity)
    }

    @Test
    fun `sets are the universal Guided-mode constant`() {
        // 3 sets everywhere: regardless of goal, difficulty label, or what the
        // replaced row used to carry.
        listOf("Easy", "Medium", "Hard").forEach { intensity ->
            val replacement = WorkoutTemplates.parameterizeReplacement(
                original = row("Crunches", "Abs", sets = 2),
                newName = "Barbell Curls",
                newMuscleGroup = "Biceps",
                workoutGoal = "goal_bodybuilding",
                intensity = intensity
            )
            assertEquals("intensity $intensity must keep the universal set count", 3, replacement.targetSets)
        }
    }

    @Test
    fun `weight and tempo reset with the swap`() {
        val replacement = WorkoutTemplates.parameterizeReplacement(
            original = row("Barbell Back Squat", "Quadriceps", weight = 80.0, tempo = "explosive"),
            newName = "Dumbbell Bicep Curl",
            newMuscleGroup = "Biceps",
            workoutGoal = "goal_bodybuilding",
            intensity = "Medium"
        )
        assertEquals(0.0, replacement.targetWeightKg, 0.0)
        assertEquals("", replacement.tempo)
    }

    @Test
    fun `slot, day and order are kept`() {
        val replacement = WorkoutTemplates.parameterizeReplacement(
            original = row("Barbell Back Squat", "Quadriceps"),
            newName = "Dumbbell Bicep Curl",
            newMuscleGroup = "Biceps",
            workoutGoal = "goal_bodybuilding",
            intensity = "Medium"
        )
        assertEquals("MONDAY", replacement.dayOfWeek)
        assertEquals(3, replacement.orderIndex)
        // transition rest follows the generator's fixed 90s default, not the old row's 120s.
        assertEquals(90, replacement.transitionRestSeconds)
        assertNotEquals(12L, replacement.id)
        assertEquals(0L, replacement.id)
    }
}
