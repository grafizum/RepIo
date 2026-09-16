package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards what a generated routine *stores* as the muscle group of a row.
 *
 * The assisted build picks exercises from a broad body part's pool, but the
 * pool is deliberately liberal: Push-ups sit under "Front Deltoids" and
 * "Shoulders" as secondary work while their authoritative primary muscle is
 * Pectorals. Storing the broad picker part made such a row display as a
 * shoulder exercise (day focus line, Home dot, Analytics bucket) while every
 * resolver — and the badge — said chest. The stored group must therefore be
 * the exercise's own precise primary muscle.
 */
class GeneratedProgramMuscleStorageTest {

    private fun build(bodyParts: List<String>, equipment: List<String>) =
        WorkoutTemplates.generateAssistedProgram(
            bodyParts = bodyParts,
            selectedDays = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            intensity = "Medium",
            equipment = equipment
        )

    @Test
    fun crossListedPushUpsAlwaysStoreAsPectorals() {
        // Regression: Push-ups sit in several bodyweight pools (they used to be
        // picked for shoulders parts and stored as a deltoid, which made rows
        // display as shoulders while every resolver said Chest). Storage must
        // always be the exercise's own precise primary muscle, Pectorals.
        // Includes match by topic, so Push-ups are forced in under the Chest
        // part — the storage rule is what this test pins.
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Chest", "Shoulders"),
            selectedDays = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            intensity = "Medium",
            workoutGoal = "goal_functional",
            includedExercises = listOf("Push-ups"),
            equipment = listOf(WorkoutTemplates.EQUIPMENT_BODYWEIGHT)
        )
        val pushUpRows = program.exercises.filter { it.name == "Push-ups" }
        assertTrue("expected Push-ups in the bodyweight build", pushUpRows.isNotEmpty())
        pushUpRows.forEach { row ->
            assertEquals(
                "Push-ups must be stored as Pectorals wherever they are offered",
                "Pectorals",
                row.muscleGroup
            )
        }
    }

    @Test
    fun bodyweightShouldersBuildSuggestsOnPartExercisesFirst() {
        // The generator ranks on-part exercises ahead of cross-listed ones, so
        // a default Easy build for the shoulders part suggests deltoid-primary
        // movements — not the chest-primary Push-ups that merely share the pool.
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Shoulders"),
            selectedDays = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            intensity = "Easy",
            equipment = listOf(WorkoutTemplates.EQUIPMENT_BODYWEIGHT)
        )
        assertTrue(program.exercises.isNotEmpty())
        program.exercises.forEach { row ->
            assertEquals(
                "'${row.name}' suggested for the shoulders part must be a deltoid exercise",
                "Shoulders",
                WorkoutTemplates.getTopicForMuscle(row.muscleGroup)
            )
        }
    }

    @Test
    fun everyStoredGroupIsTheExercisesOwnPrecisePrimaryMuscle() {
        val program = build(
            listOf("Chest", "Shoulders", "Back", "Legs", "Arms", "Core"),
            listOf(
                WorkoutTemplates.EQUIPMENT_BODYWEIGHT,
                WorkoutTemplates.EQUIPMENT_DUMBBELL,
                WorkoutTemplates.EQUIPMENT_BARBELL,
                WorkoutTemplates.EQUIPMENT_GYM
            )
        )
        assertTrue(program.exercises.isNotEmpty())
        program.exercises.forEach { row ->
            assertEquals(
                "'${row.name}' stored '${row.muscleGroup}' instead of its precise primary muscle",
                WorkoutTemplates.getMuscleGroupForExercise(row.name),
                row.muscleGroup
            )
        }
    }

    @Test
    fun storedGroupsNeverStayOnABroadTopic() {
        val program = build(listOf("Chest", "Shoulders", "Legs"), emptyList())
        program.exercises.forEach { row ->
            assertTrue(
                "'${row.name}' stored the broad topic '${row.muscleGroup}'",
                row.muscleGroup !in listOf("Chest", "Shoulders", "Back", "Legs", "Arms", "Core")
            )
        }
    }
}
