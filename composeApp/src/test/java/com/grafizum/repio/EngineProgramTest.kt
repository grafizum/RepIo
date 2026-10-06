package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The welcome screen's exercise maker is the owner's pasted template-engine
 * prompt (2026-10-06), and the owner's correction the same day — "the session
 * length the user inputs is not correctly applied" — turned Step 4 into a TIME
 * rule: slots are added while the running cost is under the picked window's
 * midpoint and never past its end, with a window-scaled working-sets guard.
 * These cases pin that contract against the real pool.
 */
class EngineProgramTest {

    private val week = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    /** The prompt's goal templates, pinned literally (en-dash, like the app). */
    private val templates = mapOf(
        "goal_bodybuilding" to listOf(
            Triple(4, "8–12", 90), Triple(3, "8–12", 90), Triple(3, "10–15", 60),
            Triple(3, "12–15", 45), Triple(2, "12–15", 45)
        ),
        "goal_strength" to listOf(
            Triple(5, "4–6", 120), Triple(4, "5–8", 90),
            Triple(3, "8–10", 60), Triple(3, "10–12", 45)
        ),
        "goal_endurance" to listOf(
            Triple(3, "15–20", 30), Triple(3, "15–20", 30),
            Triple(3, "15–20", 30), Triple(2, "20–25", 30)
        ),
        "goal_general" to listOf(
            Triple(3, "8–12", 90), Triple(3, "8–12", 60),
            Triple(3, "10–15", 60), Triple(2, "12–15", 45)
        )
    )

    /** The goal's isolation top-up row, pinned literally. */
    private val topUps = mapOf(
        "goal_bodybuilding" to Triple(2, "12–15", 45),
        "goal_strength" to Triple(3, "10–12", 45),
        "goal_endurance" to Triple(2, "20–25", 30),
        "goal_general" to Triple(2, "12–15", 45)
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

    private fun daySlots(goal: String, duration: String, days: Int = 5): List<List<Triple<Int, String, Int>>> =
        build(days, goal, duration).exercises
            .groupBy { it.dayOfWeek }
            .map { (_, ex) -> ex.sortedBy { it.orderIndex }.map { Triple(it.targetSets, it.targetReps, it.restSeconds) } }

    /** Calisthenics folds into the engine's "general" family (see engineGoalKey). */
    private fun family(goal: String): String =
        if (goal == "goal_calisthenics") "goal_general" else goal

    private fun costOf(slots: List<Triple<Int, String, Int>>): Int =
        slots.sumOf { (sets, reps, rest) -> WorkoutTemplates.engineSlotCostSeconds(sets, reps, rest) }

    @Test
    fun theSplitTableIsTheOwnersVerbatim() {
        assertEquals(listOf("full_body"), WorkoutTemplates.engineSplitFor(1))
        assertEquals(listOf("full_body", "full_body"), WorkoutTemplates.engineSplitFor(2))
        assertEquals(
            listOf("full_body", "full_body", "full_body"),
            WorkoutTemplates.engineSplitFor(3)
        )
        assertEquals(listOf("upper", "lower", "upper", "lower"), WorkoutTemplates.engineSplitFor(4))
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
    fun everyDayHonoursTheGuardAndStaysReal() {
        for (days in 1..7) for (goal in WorkoutTemplates.allWorkoutGoals) {
            for (duration in WorkoutTemplates.DURATION_OPTIONS) {
                val program = build(days, goal, duration)
                val byDay = program.exercises.groupBy { it.dayOfWeek }
                assertEquals("day count for $days / $goal", days, byDay.size)
                val guard = WorkoutTemplates.engineSetsGuard(duration)
                byDay.forEach { (day, exercises) ->
                    assertTrue("$goal $day: ${exercises.size} exercises (min 2)", exercises.size >= 2)
                    val sets = exercises.sumOf { it.targetSets }
                    assertTrue("$goal $day: $sets working sets (guard $guard)", sets <= guard)
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
                        exercises.map { it.orderIndex }.sorted(),
                        exercises.map { it.orderIndex }
                    )
                }
            }
        }
    }

    @Test
    fun theTemplatePrefixIsLiteralAndTheWindowIsFilledByTime() {
        for (goal in WorkoutTemplates.allWorkoutGoals) {
            val template = templates.getValue(family(goal))
            val topUp = topUps.getValue(family(goal))
            for (duration in WorkoutTemplates.DURATION_OPTIONS) {
                val window = WorkoutTemplates.engineWindowMinutes(duration)
                val target = (window.first + window.last) * 30
                val end = window.last * 60
                val guard = WorkoutTemplates.engineSetsGuard(duration)
                daySlots(goal, duration).forEach { slots ->
                    // The prompt's goal table is the PREFIX of every day —
                    // top-ups append, they never reorder the template.
                    val prefix = slots.take(template.size)
                    assertEquals("$goal $duration: template prefix", template, prefix)
                    // And every slot past the prefix is the goal's isolation row.
                    slots.drop(template.size).forEach {
                        assertEquals("$goal $duration: top-up row", topUp, it)
                    }
                    val cost = costOf(slots)
                    assertTrue("$goal $duration: $cost s overruns the window end $end", cost <= end)
                    val sets = slots.sumOf { it.first }
                    val setsBound = sets + topUp.first > guard
                    if (!setsBound) {
                        // Not guard-bound, so the fill must reach the window's
                        // midpoint within one slot — the duration was applied.
                        val maxSlot = slots.maxOf { costOf(listOf(it)) }
                        assertTrue(
                            "$goal $duration: $cost s is under-filled (target $target)",
                            cost >= target - maxSlot
                        )
                    } else {
                        assertTrue(
                            "$goal $duration: guard-bound at only $sets sets (guard $guard)",
                            sets >= guard - 4
                        )
                    }
                }
            }
        }
    }

    @Test
    fun thePickedDurationVisiblyChangesTheWeek() {
        // The owner's defect was three durations producing the same week.
        // Counts must rise strictly with the window for every goal.
        for (goal in WorkoutTemplates.allWorkoutGoals) {
            val counts = WorkoutTemplates.DURATION_OPTIONS.map { duration ->
                daySlots(goal, duration).map { it.size }
            }
            counts[0].zip(counts[1]).forEachIndexed { i, (quick, standard) ->
                assertTrue("$goal day $i: quick=$quick not below standard=$standard", quick < standard)
            }
            counts[1].zip(counts[2]).forEachIndexed { i, (standard, full) ->
                assertTrue("$goal day $i: standard=$standard not below full=$full", standard < full)
            }
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
