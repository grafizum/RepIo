package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.getMissingMuscleGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The setup's balance card must be GREEN for the default suggestion at every
 * difficulty — an "incomplete routine" warning must never appear by default,
 * and changing ONLY the equipment must not flip a green build red.
 *
 * The old failure mode: at Easy difficulty one exercise is picked per part,
 * and an unsorted pool could offer a cross-listed movement (Dips under
 * "Chest") whose stored PRIMARY muscle is another group — so the checker saw
 * no chest/pectoral row and turned the card red with no user-visible cause.
 */
class BalancedDefaultBuildTest {
    private val allParts = listOf(
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids",
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors",
        "Biceps", "Triceps", "Forearms",
        "Abs", "Obliques",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"
    )
    private val defaultDays = listOf("MONDAY", "WEDNESDAY", "FRIDAY")

    private fun build(intensity: String, equipment: List<String> = emptyList()) =
        WorkoutTemplates.generateAssistedProgram(
            bodyParts = allParts,
            selectedDays = defaultDays,
            intensity = intensity,
            workoutGoal = "goal_bodybuilding",
            equipment = equipment
        )

    @Test
    fun defaultBuildIsBalancedAtEveryDifficulty() {
        listOf("Easy", "Medium", "Hard").forEach { intensity ->
            val missing = getMissingMuscleGroups(build(intensity).exercises)
            assertEquals(
                "default $intensity build must be balanced, was missing $missing",
                emptyList<String>(),
                missing
            )
        }
    }

    @Test
    fun changingOnlyEquipmentKeepsEasyBuildBalanced() {
        listOf(
            listOf(WorkoutTemplates.EQUIPMENT_BODYWEIGHT),
            listOf(WorkoutTemplates.EQUIPMENT_DUMBBELL),
            listOf(WorkoutTemplates.EQUIPMENT_BARBELL),
            listOf(WorkoutTemplates.EQUIPMENT_GYM)
        ).forEach { equipment ->
            val missing = getMissingMuscleGroups(build("Easy", equipment).exercises)
            assertEquals(
                "Easy build with $equipment must stay balanced, was missing $missing",
                emptyList<String>(),
                missing
            )
        }
    }

    @Test
    fun everyPickedExerciseBelongsToAPickedPartTopic() {
        // The invariant that keeps the card green: every stored row's primary
        // muscle resolves to a broad part the user actually picked.
        val pickedTopics = allParts.map { WorkoutTemplates.getTopicForMuscle(it) }.toSet()
        build("Easy").exercises.forEach { ex ->
            val topic = WorkoutTemplates.getTopicForMuscle(ex.muscleGroup)
            assertTrue(
                "${ex.name} stored as ${ex.muscleGroup} ($topic) but that part was not picked",
                topic in pickedTopics || topic == "Other"
            )
        }
    }
}
