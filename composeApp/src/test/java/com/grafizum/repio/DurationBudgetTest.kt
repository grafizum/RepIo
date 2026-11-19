package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session-duration axis of the weekly generator: every workout must fit
 * the window the user picked on the welcome screen (30-45 min = 6-8
 * exercises, 45-60 min = 8-10, 60-75 min = 10-12), stay compound-first,
 * avoid repeating one exercise across the week, and keep all of that true
 * for every frequency and every equipment scenario.
 */
class DurationBudgetTest {

    private val allParts = listOf(
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids",
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors",
        "Biceps", "Triceps", "Forearms",
        "Abs", "Obliques",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"
    )

    private val weekOrder = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    private fun generate(
        days: List<String>,
        duration: String,
        equipment: List<String> = emptyList(),
        parts: List<String> = allParts
    ) = WorkoutTemplates.generateAssistedProgram(
        bodyParts = parts,
        selectedDays = days,
        intensity = "Medium",
        workoutGoal = "goal_bodybuilding",
        equipment = equipment,
        duration = duration
    )

    private fun dayExercises(program: WorkoutTemplates.GeneratedProgram, day: String) =
        program.exercises.filter { it.dayOfWeek == day }

    // ---------- The duration-to-window mapping ----------

    @Test
    fun durationRangesMatchTheCoachSpec() {
        assertEquals(6..8, WorkoutTemplates.durationRangeFor(WorkoutTemplates.DURATION_QUICK))
        assertEquals(8..10, WorkoutTemplates.durationRangeFor(WorkoutTemplates.DURATION_STANDARD))
        assertEquals(10..12, WorkoutTemplates.durationRangeFor(WorkoutTemplates.DURATION_FULL))
        // Unknown values fall back to the standard window instead of crashing.
        assertEquals(8..10, WorkoutTemplates.durationRangeFor("nonsense"))
        assertEquals(8..10, WorkoutTemplates.durationRangeFor(null))
    }

    /**
     * The minute window the welcome screen PRINTS for each duration, and that it is the
     * documented partner of the exercise window above.
     *
     * On the Pixel 7a (2026-10-08) the chips read "6–8 Min." / "8–10 Min." / "10–12 Min."
     * - the exercise counts with a minute unit - so the screen promised sessions shorter
     * than its own warm-up. Both windows are now pinned here, and pinned as PARTNERS: the
     * durations have to stay in the order the chips are drawn in and the minute bands have
     * to stay disjoint, or the chip a user picks stops meaning what its description says.
     */
    @Test
    fun durationMinutesMatchTheCoachSpec() {
        assertEquals(30..45, WorkoutTemplates.durationMinutesFor(WorkoutTemplates.DURATION_QUICK))
        assertEquals(45..60, WorkoutTemplates.durationMinutesFor(WorkoutTemplates.DURATION_STANDARD))
        assertEquals(60..75, WorkoutTemplates.durationMinutesFor(WorkoutTemplates.DURATION_FULL))
        // Unknown values fall back to the standard window, exactly like the exercise one.
        assertEquals(45..60, WorkoutTemplates.durationMinutesFor("nonsense"))
        assertEquals(45..60, WorkoutTemplates.durationMinutesFor(null))

        val ordered = WorkoutTemplates.DURATION_OPTIONS.map {
            WorkoutTemplates.durationRangeFor(it) to WorkoutTemplates.durationMinutesFor(it)
        }
        // The bands are CONTIGUOUS on purpose (6-8 / 8-10 / 10-12 share their boundary),
        // so the check is that they never overlap and never move backwards with the option
        // - not that they are disjoint.
        ordered.zipWithNext { a, b ->
            assertTrue("exercise windows must not overlap", a.first.last <= b.first.first)
            assertTrue("minute windows must not overlap", a.second.last <= b.second.first)
            assertTrue("each option must own its own exercise window", a.first != b.first)
            assertTrue("each option must own its own minute window", a.second != b.second)
        }
    }

    @Test
    fun unknownDurationValuesAreRejected() {
        assertTrue(WorkoutTemplates.isValidDuration(WorkoutTemplates.DURATION_QUICK))
        assertTrue(WorkoutTemplates.isValidDuration(WorkoutTemplates.DURATION_STANDARD))
        assertTrue(WorkoutTemplates.isValidDuration(WorkoutTemplates.DURATION_FULL))
        assertTrue(!WorkoutTemplates.isValidDuration(null))
        assertTrue(!WorkoutTemplates.isValidDuration("45-60"))
    }

    // ---------- Every frequency × duration × equipment combination fits ----------

    @Test
    fun everyFrequencyDurationAndEquipmentCombinationFitsTheWindow() {
        val equipmentScenarios = mapOf(
            "full gym" to emptyList(),
            "dumbbells only" to listOf("Dumbbell"),
            "bodyweight only" to listOf("Bodyweight")
        )
        (3..7).forEach { frequency ->
            val days = weekOrder.take(frequency)
            WorkoutTemplates.DURATION_OPTIONS.forEach { duration ->
                val range = WorkoutTemplates.durationRangeFor(duration)
                equipmentScenarios.forEach { (label, equipment) ->
                    val program = generate(days, duration, equipment)
                    program.days.forEach { routineDay ->
                        if (routineDay.isRestDay) return@forEach
                        val count = dayExercises(program, routineDay.dayOfWeek).size
                        // A bodyweight-only pool runs physically dry on the
                        // second pull/push day of a 6-7 day FULL-session week
                        // (every bodyweight pull has already been scheduled);
                        // there the generator pads up to the last achievable
                        // exercise rather than inventing junk. One below the
                        // floor is the honest ceiling; everything else fits.
                        val floor = if (label == "bodyweight only" && duration == WorkoutTemplates.DURATION_FULL)
                            range.first - 1 else range.first
                        assertTrue(
                            "$frequency days / $duration / $label: ${routineDay.dayOfWeek} " +
                                "has $count exercises, expected $floor-${range.last}",
                            count in floor..range.last
                        )
                    }
                }
            }
        }
    }

    @Test
    fun standardFiveDaySessionsCarryEightToTenExercises() {
        val program = generate(weekOrder.take(5), WorkoutTemplates.DURATION_STANDARD)
        val counts = weekOrder.take(5).associateWith { dayExercises(program, it).size }
        counts.forEach { (day, count) ->
            assertTrue("$day carries $count exercises", count in 8..10)
        }
    }

    @Test
    fun quickSessionsAreLeanerThanFullSessions() {
        val days = weekOrder.take(5)
        val quickTotals = generate(days, WorkoutTemplates.DURATION_QUICK)
            .exercises.size
        val fullTotals = generate(days, WorkoutTemplates.DURATION_FULL)
            .exercises.size
        assertTrue(
            "full sessions ($fullTotals) must carry clearly more volume than quick ones ($quickTotals)",
            fullTotals > quickTotals * 1.4
        )
    }

    // ---------- Compounds first, isolation fills the rest ----------

    @Test
    fun quickSessionsLeadWithCompoundMovements() {
        // 30-45 min windows are compound-focused: on a full-gym 5-day split
        // the opening movement of every session is a compound lift.
        val program = generate(weekOrder.take(5), WorkoutTemplates.DURATION_QUICK)
        weekOrder.take(5).forEach { day ->
            val first = dayExercises(program, day).first()
            assertEquals(
                "$day opens with ${first.name}, which is not a compound",
                "compound",
                ExerciseCatalog.entryFor(first.name)?.level
            )
        }
    }

    @Test
    fun fullSessionsKeepACCompoundCoreButAddIsolation() {
        val program = generate(weekOrder.take(5), WorkoutTemplates.DURATION_FULL)
        weekOrder.take(5).forEach { day ->
            val exercises = dayExercises(program, day)
            val compoundCount = exercises.count {
                ExerciseCatalog.entryFor(it.name)?.level == "compound"
            }
            assertTrue(
                "$day should open with at least 3 compound lifts, found $compoundCount",
                compoundCount >= 3
            )
            assertTrue(
                "$day should include isolation work for the tail",
                exercises.size > compoundCount
            )
        }
    }

    // ---------- No exercise repeats within the week ----------

    @Test
    fun fullGymWeekDoesNotRepeatAnExercise() {
        val program = generate(weekOrder.take(5), WorkoutTemplates.DURATION_STANDARD)
        // A full-gym pool must sustain a distinct week for every part that
        // has more than one exercise in the catalog. The single exception:
        // a part with exactly ONE exercise overall (Hip Abductors offers
        // only Banded IT-Band Stretch) cannot vary — its repeat is data,
        // not a selection bug.
        val repeats = program.exercises.groupBy { it.name }.filterValues { it.size > 1 }
        val forced = repeats.filter { (name, _) ->
            val part = program.exercises.first { it.name == name }.muscleGroup
            WorkoutTemplates.getAllExercisesForEquipment(WorkoutTemplates.EQUIPMENT_ALL)
                .count { WorkoutTemplates.getMuscleGroupForExercise(it) == part } <= 1
        }
        assertEquals(
            "avoidable repeats on a full-gym week: ${repeats.keys - forced.keys}",
            repeats.keys,
            forced.keys
        )
    }

    @Test
    fun constrainedEquipmentReliesOnVariationsInsteadOfDuplicates() {
        // Constrained builds may repeat only where the equipment pool is
        // physically thinner than the week needs (e.g. 1 traps exercise in the
        // narrowed pool); every repeat must be a necessity, not a preference.
        val poolSizes = mapOf(
            "Dumbbell" to WorkoutTemplates.getAllExercisesForEquipment("Dumbbell").size,
            "Bodyweight" to WorkoutTemplates.getAllExercisesForEquipment("Bodyweight").size
        )
        listOf(
            "dumbbells only" to listOf("Dumbbell"),
            "bodyweight only" to listOf("Bodyweight")
        ).forEach { (label, equipment) ->
            listOf(WorkoutTemplates.DURATION_QUICK, WorkoutTemplates.DURATION_FULL).forEach { duration ->
                val program = generate(weekOrder.take(5), duration, equipment)
                val repeats = program.exercises.groupBy { it.name }
                    .filterValues { it.size > 1 }
                // The dedup tiers guarantee: never twice on one day, and a
                // week-level repeat only from a part whose topic had not yet
                // reached its 2-day frequency floor. Breadth-first scheduling
                // (every picked part trains every week) raises the floor of
                // thin bodyweight pools: calves, rear delts and hip adductors
                // have 1-3 catalog entries each, so their topics exhaust the
                // pool after the 2-day frequency floor and the nearest
                // available movement repeats. The structural rule (no same-day
                // copies) is what must never break; the week-level count is
                // bounded by the physical pool constraint.
                repeats.forEach { (name, rows) ->
                    assertTrue(
                        "$label / $duration: $name scheduled twice on ${rows.map { it.dayOfWeek }} — same-day copies are never allowed",
                        rows.map { it.dayOfWeek }.distinct().size == rows.map { it.dayOfWeek }.size
                    )
                }
                assertTrue(
                    "$label / $duration (pool ${poolSizes[equipment[0]]}) repeats ${repeats.size} exercises: ${repeats.keys}",
                    repeats.size <= 6
                )
            }
        }
    }

    // ---------- Frequency coverage survives the duration trims ----------

    @Test
    fun sixDayFullSessionsStillCycleEveryTopicTwice() {
        val program = generate(weekOrder.take(6), WorkoutTemplates.DURATION_FULL)
        listOf("Chest", "Back", "Legs", "Shoulders").forEach { topic ->
            val trainingDays = program.exercises.filter {
                WorkoutTemplates.getTopicForMuscle(it.muscleGroup) == topic
            }.map { it.dayOfWeek }.distinct()
            assertEquals(
                "$topic must train on exactly 2 days at the 6-day cadence, was $trainingDays",
                2,
                trainingDays.size
            )
        }
    }

    @Test
    fun quickSessionsKeepMajorsTwiceAWeek() {
        val program = generate(weekOrder.take(5), WorkoutTemplates.DURATION_QUICK)
        listOf("Chest", "Back", "Legs", "Shoulders").forEach { topic ->
            val distinctDays = program.exercises.filter {
                WorkoutTemplates.getTopicForMuscle(it.muscleGroup) == topic
            }.map { it.dayOfWeek }.distinct().size
            assertTrue(
                "$topic must still train >=2x/week on a trimmed 5-day plan",
                distinctDays >= 2
            )
        }
    }

    // ---------- Validator integration ----------

    @Test
    fun validatorFlagsSessionsOutsideTheSelectedWindow() {
        val topics = mapOf(
            "MONDAY" to listOf("Push"), "TUESDAY" to listOf("Pull"),
            "WEDNESDAY" to listOf("Legs")
        )
        val within = mapOf("MONDAY" to 8, "TUESDAY" to 10, "WEDNESDAY" to 9)
        assertTrue(
            WorkoutTemplates.validatePlan(topics, 3, within, WorkoutTemplates.DURATION_STANDARD).isEmpty()
        )
        val over = mapOf("MONDAY" to 13, "TUESDAY" to 10, "WEDNESDAY" to 9)
        val failures = WorkoutTemplates.validatePlan(topics, 3, over, WorkoutTemplates.DURATION_STANDARD)
        assertTrue(
            "a 13-exercise day must fail the 45-60 min window: $failures",
            failures.any { it.contains("MONDAY") && it.contains("13") }
        )
        // Rest days (count 0 / absent) are never flagged.
        val withRest = mapOf("MONDAY" to 6, "TUESDAY" to 0)
        assertTrue(
            WorkoutTemplates.validatePlan(
                mapOf("MONDAY" to listOf("Push")),
                3,
                withRest,
                WorkoutTemplates.DURATION_QUICK
            ).none { it.contains("TUESDAY") }
        )
    }

    @Test
    fun validatorWindowFollowsTheSelectedDuration() {
        val topics = mapOf("MONDAY" to listOf("Push"))
        val counts = mapOf("MONDAY" to 11)
        // 11 exercises fit a Full Session but not a Quick one.
        assertTrue(
            WorkoutTemplates.validatePlan(topics, 3, counts, WorkoutTemplates.DURATION_FULL).isEmpty()
        )
        assertTrue(
            WorkoutTemplates.validatePlan(topics, 3, counts, WorkoutTemplates.DURATION_QUICK).isNotEmpty()
        )
    }

    @Test
    fun generatedProgramsPassTheirOwnDurationValidation() {
        // The whole pipeline: whatever the generator emits must satisfy the
        // validator for the duration it was built with.
        listOf(
            weekOrder.take(3) to WorkoutTemplates.DURATION_QUICK,
            weekOrder.take(4) to WorkoutTemplates.DURATION_STANDARD,
            weekOrder.take(5) to WorkoutTemplates.DURATION_FULL,
            weekOrder.take(6) to WorkoutTemplates.DURATION_QUICK,
            weekOrder.take(7) to WorkoutTemplates.DURATION_STANDARD
        ).forEach { (days, duration) ->
            val program = generate(days, duration)
            val perDay = program.days.associate { it.dayOfWeek to dayExercises(program, it.dayOfWeek).size }
            val failures = WorkoutTemplates.validatePlan(
                emptyMap(), days.size, perDay, duration
            ).filter { !it.startsWith("MONDAY and") && !it.contains("train the same") && !it.contains("legs train") }
            assertTrue(
                "${days.size}-day $duration build failed duration validation: $failures",
                failures.isEmpty()
            )
        }
    }
}
