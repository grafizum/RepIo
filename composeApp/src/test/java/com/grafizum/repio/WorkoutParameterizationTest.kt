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
 *   Difficulty  -> effort label + volume + suggested load
 *   Set count   -> universal constant (3) in Guided mode, owned by nobody
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
            val union = perDifficulty.flatMap { it.keys }.toSet()
            assertTrue("expected exercises for $goal", union.isNotEmpty())
            // With the rebuilt 1,470-exercise pool the selection is intensity-aware
            // by design (e.g. bodybuilding at Hard prefers barbell variants), so the
            // builds no longer nest. The invariant that matters: whatever an
            // exercise's reps are, they are identical at every difficulty it appears
            // at — the goal, not the difficulty, owns the rep range.
            val multiDifficulty = union.filter { name -> perDifficulty.count { name in it } >= 2 }
            assertTrue(
                "expected the Easy and Medium builds to share exercises for $goal",
                multiDifficulty.isNotEmpty()
            )
            multiDifficulty.forEach { name ->
                val ranges = perDifficulty.mapNotNull { it[name] }.distinct()
                assertEquals("$goal / $name changed reps with difficulty: $ranges", 1, ranges.size)
            }
            // And every emitted exercise's reps come from the goal's own scheme.
            perDifficulty.forEach { repsByName ->
                repsByName.forEach { (name, reps) ->
                    assertEquals(
                        "$goal / $name: emitted reps must be the goal's scheme",
                        WorkoutTemplates.getGoalReps(name, goal),
                        reps
                    )
                }
            }
        }
    }

    @Test
    fun repsDoDependOnTheGoal() {
        val bodybuilding = WorkoutTemplates.getGoalReps("Barbell Back Squat", "goal_bodybuilding")
        val strength = WorkoutTemplates.getGoalReps("Barbell Back Squat", "goal_strength")
        val endurance = WorkoutTemplates.getGoalReps("Barbell Back Squat", "goal_endurance")
        assertNotEquals(bodybuilding, strength)
        assertNotEquals(bodybuilding, endurance)
    }

    @Test
    fun repsMatchTheTextbookSchemePerGoal() {
        // Compounds keep the goal's own range instead of being widened into noise.
        assertEquals("1–5", WorkoutTemplates.getGoalReps("Barbell Back Squat", "goal_strength"))
        assertEquals("8–12", WorkoutTemplates.getGoalReps("Barbell Back Squat", "goal_bodybuilding"))
        assertEquals("12–20", WorkoutTemplates.getGoalReps("Barbell Back Squat", "goal_calisthenics"))
        // The overhead press family counts as a heavy compound even without the
        // word "barbell" in its name.
        assertEquals("1–5", WorkoutTemplates.getGoalReps("Military Press", "goal_strength"))
        assertEquals("1–5", WorkoutTemplates.getGoalReps("Barbell Bench Press", "goal_strength"))
        // Isolation work stays in a hypertrophy range even inside a strength block.
        assertEquals("8–12", WorkoutTemplates.getGoalReps("Dumbbell Bicep Curl", "goal_strength"))
        assertEquals("10–15", WorkoutTemplates.getGoalReps("Dumbbell Bicep Curl", "goal_bodybuilding"))
    }

    @Test
    fun setsAreUniversalAtThree() {
        // 3 sets for every exercise, at every goal × difficulty combination —
        // the set count is a Guided-mode constant, not a parameter.
        WorkoutTemplates.allWorkoutGoals.forEach { goal ->
            difficulties.forEach { intensity ->
                val exercises = build(goal, intensity).exercises
                assertTrue("expected exercises for $goal/$intensity", exercises.isNotEmpty())
                exercises.forEach { ex ->
                    assertEquals(
                        "$goal @ $intensity must keep the universal set count",
                        WorkoutTemplates.GUIDED_SETS_PER_EXERCISE,
                        ex.targetSets
                    )
                }
            }
        }
    }

    @Test
    fun restDependsOnTheGoal() {
        val strength = WorkoutTemplates.getGoalRestSeconds("goal_strength", "Barbell Back Squat")
        val bodybuilding = WorkoutTemplates.getGoalRestSeconds("goal_bodybuilding", "Barbell Back Squat")
        val conditioning = WorkoutTemplates.getGoalRestSeconds("goal_endurance", "Barbell Back Squat")
        assertTrue("strength rest must be the longest ($strength)", strength > bodybuilding)
        assertTrue("conditioning rest must be the shortest ($conditioning)", conditioning < bodybuilding)
    }

    @Test
    fun effortLabelTracksDifficultyAndIsNeverOverwritten() {
        assertEquals("Light", WorkoutTemplates.getGoalIntensity("goal_bodybuilding", "Easy", "Barbell Back Squat"))
        assertEquals("Moderate", WorkoutTemplates.getGoalIntensity("goal_bodybuilding", "Medium", "Barbell Back Squat"))
        assertEquals("To Failure", WorkoutTemplates.getGoalIntensity("goal_bodybuilding", "Hard", "Barbell Back Squat"))
        assertEquals("Very Heavy", WorkoutTemplates.getGoalIntensity("goal_strength", "Hard", "Barbell Back Squat"))
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
