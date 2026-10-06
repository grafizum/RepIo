package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The welcome screen's exercise maker is now the owner's pasted
 * template-engine prompt (2026-10-06). These cases pin the prompt's own
 * rules against the real pool — the split table, the 6-exercise / 20-set
 * day caps, the goal slot schemes, the exclusion filter, the equipment
 * gate, the no-same-muscle-consecutive rule and the compound-first /
 * isolation-last slot roles.
 */
class EngineProgramTest {

    private val week = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    private fun build(
        days: Int,
        goal: String = "goal_bodybuilding",
        duration: String = WorkoutTemplates.DURATION_STANDARD,
        equipment: List<String> = emptyList(),
        excluded: List<String> = emptyList(),
        intensity: String = "Medium"
    ) = WorkoutTemplates.generateEngineProgram(
        selectedDays = week.take(days),
        intensity = intensity,
        workoutGoal = goal,
        excludedExercises = excluded,
        equipment = equipment,
        duration = duration
    )

    @Test
    fun theSplitTableIsTheOwnersVerbatim() {
        assertEquals(listOf("full_body"), WorkoutTemplates.engineSplitFor(1))
        assertEquals(listOf("full_body", "full_body"), WorkoutTemplates.engineSplitFor(2))
        assertEquals(
            listOf("full_body", "full_body", "full_body"),
            WorkoutTemplates.engineSplitFor(3)
        )
        assertEquals(
            listOf("upper", "lower", "upper", "lower"),
            WorkoutTemplates.engineSplitFor(4)
        )
        assertEquals(
            listOf("push", "pull", "legs", "upper", "lower"),
            WorkoutTemplates.engineSplitFor(5)
        )
        assertEquals(
            listOf("push", "pull", "legs", "push", "pull", "legs"),
            WorkoutTemplates.engineSplitFor(6)
        )
        assertEquals(
            listOf("push", "pull", "legs", "upper", "lower", "arms", "core"),
            WorkoutTemplates.engineSplitFor(7)
        )
    }

    @Test
    fun everyDayCountAndGoalHonoursThePromptCaps() {
        for (days in 1..7) for (goal in WorkoutTemplates.allWorkoutGoals) {
            val program = build(days, goal)
            val byDay = program.exercises.groupBy { it.dayOfWeek }
            assertEquals("day count for $days / $goal", days, byDay.size)
            byDay.forEach { (day, exercises) ->
                assertTrue(
                    "$goal $day: ${exercises.size} exercises (cap 6)",
                    exercises.size in 2..6
                )
                val sets = exercises.sumOf { it.targetSets }
                assertTrue("$goal $day: $sets working sets (cap 20)", sets <= 20)
                exercises.forEach { ex ->
                    assertTrue(
                        "$goal $day: ${ex.name} not in catalog",
                        ExerciseCatalog.entryFor(ex.name) != null
                    )
                    assertTrue(
                        "$goal $day: ${ex.name} rest ${ex.restSeconds} not a template value",
                        ex.restSeconds in setOf(30, 45, 60, 90, 120)
                    )
                }
                assertEquals(
                    "$goal $day: orderIndex not contiguous",
                    exercises.map { it.orderIndex },
                    exercises.map { it.orderIndex }.sorted()
                )
            }
        }
    }

    @Test
    fun slotSchemesFollowTheGoalTemplate() {
        // Hypertrophy at the standard duration is exactly the prompt's five
        // hypertrophy slots — pinned literally, not recomputed from the code.
        val hypertrophy = build(5, "goal_bodybuilding")
        val expectedHypertrophy = listOf(
            Triple(4, "8–12", 90), Triple(3, "8–12", 90), Triple(3, "10–15", 60),
            Triple(3, "12–15", 45), Triple(2, "12–15", 45)
        )
        hypertrophy.exercises.groupBy { it.dayOfWeek }.forEach { (day, exercises) ->
            val actual = exercises.sortedBy { it.orderIndex }.map {
                Triple(it.targetSets, it.targetReps, it.restSeconds)
            }
            assertEquals("hypertrophy slots on $day", expectedHypertrophy, actual)
        }

        // Strength at the standard duration: four strength slots plus one
        // isolation top-up (the duration asks for five, the template has four).
        val strength = build(4, "goal_strength")
        val expectedStrength = listOf(
            Triple(5, "4–6", 120), Triple(4, "5–8", 90), Triple(3, "8–10", 60),
            Triple(3, "10–12", 45), Triple(3, "10–12", 45)
        )
        strength.exercises.groupBy { it.dayOfWeek }.forEach { (day, exercises) ->
            val actual = exercises.sortedBy { it.orderIndex }.map {
                Triple(it.targetSets, it.targetReps, it.restSeconds)
            }
            assertEquals("strength slots on $day", expectedStrength, actual)
        }

        // Endurance on the full duration: four endurance slots plus TWO
        // isolation top-ups, still inside the 6-exercise cap.
        val endurance = build(7, "goal_endurance", duration = WorkoutTemplates.DURATION_FULL)
        val expectedEndurance = listOf(
            Triple(3, "15–20", 30), Triple(3, "15–20", 30), Triple(3, "15–20", 30),
            Triple(2, "20–25", 30), Triple(2, "20–25", 30), Triple(2, "20–25", 30)
        )
        endurance.exercises.groupBy { it.dayOfWeek }.forEach { (day, exercises) ->
            val actual = exercises.sortedBy { it.orderIndex }.map {
                Triple(it.targetSets, it.targetReps, it.restSeconds)
            }
            assertEquals("endurance slots on $day", expectedEndurance, actual)
        }
    }

    @Test
    fun exclusionsAreHonouredCaseInsensitivelyAndPartially() {
        val withSquat = build(5)
        assertTrue(
            "non-vacuous: the unfiltered week must contain a squat somewhere",
            withSquat.exercises.any { it.name.contains("squat", ignoreCase = true) }
        )
        val withoutSquat = build(5, excluded = listOf("squat"))
        assertTrue(
            "an excluded name must not survive in any slot",
            withoutSquat.exercises.none { it.name.contains("squat", ignoreCase = true) }
        )
        // Partial match, case-insensitive: excluding one word of a family.
        val noDeadlift = build(5, excluded = listOf("deadlift", "barbell row"))
        assertTrue(
            noDeadlift.exercises.none { it.name.contains("deadlift", ignoreCase = true) }
        )
    }

    @Test
    fun theEquipmentGateIsStructural() {
        val granted = WorkoutTemplates.grantedTagsForPublic(listOf("Dumbbell"))
            ?: error("a Dumbbell selection must produce a gate, not 'unrestricted' (null)")
        for (goal in WorkoutTemplates.allWorkoutGoals) {
            val program = build(5, goal, equipment = listOf("Dumbbell"))
            program.exercises.forEach { ex ->
                assertTrue(
                    "$goal: ${ex.name} passes the dumbbell-only gate",
                    ExerciseEquipmentTags.isAvailable(ex.name, granted)
                )
            }
        }
    }

    @Test
    fun consecutiveDaysDoNotShareAPrimaryMuscle() {
        // The prompt's rule ("never train the same primary muscle group on
        // consecutive days") holds on every split whose day types differ.
        // 1-3 days are full_body by the prompt's OWN table and repeat the
        // majors by construction, so they are deliberately not asserted here.
        for (days in 4..7) for (goal in WorkoutTemplates.allWorkoutGoals) {
            val program = build(days, goal)
            val perDay = program.exercises.groupBy { it.dayOfWeek }
            val ordered = week.filter { it in perDay.keys }
            ordered.zipWithNext { a, b ->
                val partsA = perDay.getValue(a).map { it.muscleGroup }.toSet()
                val partsB = perDay.getValue(b).map { it.muscleGroup }.toSet()
                val overlap = partsA intersect partsB
                assertTrue(
                    "$goal: ${a.take(3)} -> ${b.take(3)} share ${overlap.joinToString()}",
                    overlap.isEmpty()
                )
            }
        }
    }

    @Test
    fun compoundLeadsTheFirstTwoSlotsWhereThePoolHasOne() {
        val program = build(5)
        program.exercises.groupBy { it.dayOfWeek }.forEach { (day, exercises) ->
            exercises.sortedBy { it.orderIndex }.take(2).forEach { ex ->
                val poolHasCompound = ExerciseCatalog.ENTRIES.any {
                    it.muscle == ex.muscleGroup && it.level == "compound"
                }
                if (poolHasCompound) {
                    assertEquals(
                        "$day slot ${ex.orderIndex}: ${ex.name} should be compound",
                        "compound",
                        ExerciseCatalog.entryFor(ex.name)?.level
                    )
                }
            }
        }
    }
}
