package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Core closes every session.
 *
 * The core transfers to the rest of the workout by bracing the spine, not by
 * being worked to failure first, so freshness is worth least there — and
 * training it first would leave the loaded work after it bracing poorly. The
 * sort key therefore has to outrank the compound rule, or a "compound" core
 * movement (a loaded carry, a cable chop) gets promoted to lead the session.
 *
 * Note what this test does NOT assert: that order grows muscle. The evidence
 * (Nunes et al. 2021, pooled across 11 studies) is that order makes no
 * measurable difference to hypertrophy. This is about session structure, not
 * an adaptation claim.
 */
class CoreLastInSessionTest {

    private val week = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    @Test
    fun everyGeneratedDayPutsCoreLast() {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        val parts = WorkoutTemplates.getBroadMuscleGroups()
        for (duration in listOf("Short", "Standard", "Long")) {
            val program = WorkoutTemplates.generateAssistedProgram(
                bodyParts = parts,
                selectedDays = days,
                intensity = "Medium",
                workoutGoal = "goal_bodybuilding",
                duration = duration
            )
            for (day in days) {
                val dayExercises = program.exercises.filter { it.dayOfWeek == day }
                if (dayExercises.isEmpty()) continue

                val firstCore = dayExercises.indexOfFirst {
                    WorkoutTemplates.getTopicForMuscle(it.muscleGroup) == "Core"
                }
                if (firstCore < 0) continue

                val lastNonCore = dayExercises.indexOfLast {
                    WorkoutTemplates.getTopicForMuscle(it.muscleGroup) != "Core"
                }
                assertTrue(
                    "$duration, $day: core work starts at index $firstCore but " +
                        "non-core work continues to $lastNonCore — " +
                        "core must close the session: " +
                        dayExercises.map { "${it.muscleGroup}: ${it.name}" },
                    firstCore > lastNonCore
                )
            }
        }
    }

    @Test
    fun aCoreDayStillTrainsCore() {
        // The rule reorders, it must not quietly drop the muscle: a day whose
        // topics include Core has to come out with core exercises in it.
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = WorkoutTemplates.getBroadMuscleGroups(),
            selectedDays = days,
            intensity = "Medium",
            workoutGoal = "goal_bodybuilding"
        )
        val coreExercises = program.exercises.filter {
            WorkoutTemplates.getTopicForMuscle(it.muscleGroup) == "Core"
        }
        assertTrue("a 5-day build must still train core", coreExercises.isNotEmpty())
    }

    @Test
    fun theCoreDayItselfEndsInCore() {
        // On Option A's 5-day week the fifth session IS arms & core, so this is
        // the case the owner asked about: the day must not open with core work
        // just because the session is mostly core-adjacent.
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = WorkoutTemplates.getBroadMuscleGroups(),
            selectedDays = days,
            intensity = "Medium",
            workoutGoal = "goal_bodybuilding"
        )
        val lastDay = days.last()
        val dayExercises = program.exercises.filter { it.dayOfWeek == lastDay }
        if (dayExercises.isEmpty()) return

        val firstCore = dayExercises.indexOfFirst {
            WorkoutTemplates.getTopicForMuscle(it.muscleGroup) == "Core"
        }
        if (firstCore < 0) return
        assertTrue(
            "$lastDay opens with core at index $firstCore: " +
                dayExercises.map { "${it.muscleGroup}: ${it.name}" },
            firstCore > 0
        )
    }
}
