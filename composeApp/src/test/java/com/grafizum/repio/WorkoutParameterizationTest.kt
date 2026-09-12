package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The build exposes exactly two controls, and each one owns exactly one axis of
 * the prescription. These tests pin that down so a future change cannot quietly
 * reintroduce the double-dipping (e.g. difficulty moving reps *and* sets, or a
 * second control overwriting the effort label).
 *
 *   Goal        -> rep range + rest
 *   Difficulty  -> effort label + sets + volume + suggested load
 */
class WorkoutParameterizationTest {

    private val difficulties = listOf("Easy", "Medium", "Hard")

    private fun build(goal: String, intensity: String) = WorkoutTemplates.generateAssistedProgram(
        bodyParts = listOf("Chest", "Lats", "Quadriceps"),
        selectedDays = listOf("MONDAY", "WEDNESDAY"),
        intensity = intensity,
        workoutGoal = goal
    )

    /** name -> reps for one goal at one difficulty. */
    private fun repsByExercise(goal: String, intensity: String): Map<String, String> =
        build(goal, intensity).exercises.associate { it.name to it.targetReps }

    @Test
    fun repsDependOnGoalAndNotOnDifficulty() {
        WorkoutTemplates.allWorkoutGoals.forEach { goal ->
            val perDifficulty = difficulties.map { repsByExercise(goal, it) }
            val shared = perDifficulty.map { it.keys }.reduce { a, b -> a intersect b }
            assertTrue("expected overlapping exercises for $goal", shared.isNotEmpty())
            shared.forEach { name ->
                val ranges = perDifficulty.map { it.getValue(name) }.distinct()
                assertEquals("$goal / $name changed reps with difficulty: $ranges", 1, ranges.size)
            }
        }
    }

    @Test
    fun repsDoDependOnTheGoal() {
        val bodybuilding = WorkoutTemplates.getGoalReps("Barbell Squats", "goal_bodybuilding")
        val strength = WorkoutTemplates.getGoalReps("Barbell Squats", "goal_strength")
        val crossfit = WorkoutTemplates.getGoalReps("Barbell Squats", "goal_crossfit")
        assertNotEquals(bodybuilding, strength)
        assertNotEquals(bodybuilding, crossfit)
    }

    @Test
    fun repsMatchTheTextbookSchemePerGoal() {
        // Compounds keep the goal's own range instead of being widened into noise.
        assertEquals("1–5", WorkoutTemplates.getGoalReps("Barbell Squats", "goal_strength"))
        assertEquals("8–12", WorkoutTemplates.getGoalReps("Barbell Squats", "goal_bodybuilding"))
        assertEquals("12–20", WorkoutTemplates.getGoalReps("Barbell Squats", "goal_calisthenics"))
        // The overhead press family counts as a heavy compound even without the
        // word "barbell" in its name.
        assertEquals("1–5", WorkoutTemplates.getGoalReps("Military Presses", "goal_strength"))
        assertEquals("1–5", WorkoutTemplates.getGoalReps("Barbell Bench Press", "goal_strength"))
        // Isolation work stays in a hypertrophy range even inside a strength block.
        assertEquals("8–12", WorkoutTemplates.getGoalReps("Dumbbell Curls", "goal_strength"))
        assertEquals("10–15", WorkoutTemplates.getGoalReps("Dumbbell Curls", "goal_bodybuilding"))
    }

    @Test
    fun setsDependOnDifficultyAlone() {
        val expected = mapOf("Easy" to 3, "Medium" to 4, "Hard" to 5)
        WorkoutTemplates.allWorkoutGoals.forEach { goal ->
            difficulties.forEach { intensity ->
                val exercises = build(goal, intensity).exercises
                assertTrue("expected exercises for $goal/$intensity", exercises.isNotEmpty())
                exercises.forEach { ex ->
                    assertEquals(
                        "$goal @ $intensity should use ${expected.getValue(intensity)} sets",
                        expected.getValue(intensity),
                        ex.targetSets
                    )
                }
            }
        }
    }

    @Test
    fun restDependsOnTheGoal() {
        val strength = WorkoutTemplates.getGoalRestSeconds("goal_strength", "Barbell Squats")
        val bodybuilding = WorkoutTemplates.getGoalRestSeconds("goal_bodybuilding", "Barbell Squats")
        val conditioning = WorkoutTemplates.getGoalRestSeconds("goal_crossfit", "Barbell Squats")
        assertTrue("strength rest must be the longest ($strength)", strength > bodybuilding)
        assertTrue("conditioning rest must be the shortest ($conditioning)", conditioning < bodybuilding)
    }

    @Test
    fun effortLabelTracksDifficultyAndIsNeverOverwritten() {
        assertEquals("Light", WorkoutTemplates.getGoalIntensity("goal_bodybuilding", "Easy", "Barbell Squats"))
        assertEquals("Moderate", WorkoutTemplates.getGoalIntensity("goal_bodybuilding", "Medium", "Barbell Squats"))
        assertEquals("To Failure", WorkoutTemplates.getGoalIntensity("goal_bodybuilding", "Hard", "Barbell Squats"))
        assertEquals("Very Heavy", WorkoutTemplates.getGoalIntensity("goal_strength", "Hard", "Barbell Squats"))
    }

    @Test
    fun generatorLeavesLoadToTheDifficultyEstimate() {
        // The plan never bakes a weight in for a generated exercise, so the
        // difficulty-driven estimate is the single source of the starting load.
        build("goal_bodybuilding", "Medium").exercises.forEach { ex ->
            assertEquals("generated plans must not hard-code a weight", 0.0, ex.targetWeightKg, 0.0)
        }
    }
}
