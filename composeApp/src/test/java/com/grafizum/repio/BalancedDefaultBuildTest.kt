package com.grafizum.repio

import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.BALANCE_GROUP_KEYS
import com.grafizum.repio.ui.components.WeeklyBalanceIssue
import com.grafizum.repio.ui.components.balanceGroupsForParts
import com.grafizum.repio.ui.components.weeklyBalanceIssues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The setup screen's balance card judges the week the welcome screen is about
 * to SAVE — [WorkoutTemplates.generateEngineProgram], the same generator
 * `FirstTimeSetupScreen.saveSelectedBuild` calls — against a healthy weekly
 * RANGE per muscle group, scoped to the advanced tab's part selection.
 *
 * Owner, 2026-10-06: the old card cried "Unbalanced Workout Routine" whenever
 * a group had ZERO exercises, which fired on every engine week under seven
 * days (the engine's full-body table has no direct core) even when the user
 * had filtered nothing out. The verdict is now numbers — group, exercises per
 * week, days trained, healthy range — and the ranges are anchored on the
 * generator itself: a floor is never higher than the thinnest week the app's
 * own templates produce, so the app never red-flags its own default output,
 * while a hand-built week that skips a major group (or piles junk volume onto
 * one) still goes red.
 */
class BalancedDefaultBuildTest {

    private val week = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    /** The advanced tab's body-part vocabulary; its DEFAULT selection is all of it. */
    private val allParts = listOf(
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids",
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors",
        "Biceps", "Triceps", "Forearms",
        "Abs", "Obliques",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"
    )

    private fun engine(
        days: Int = 3,
        goal: String = "goal_bodybuilding",
        duration: String = WorkoutTemplates.DURATION_STANDARD,
        intensity: String = "Medium",
        equipment: List<String> = emptyList()
    ) = WorkoutTemplates.generateEngineProgram(
        selectedDays = week.take(days),
        intensity = intensity,
        workoutGoal = goal,
        equipment = equipment,
        duration = duration
    )

    /** A hand-built row for the custom-tab cases: the checker reads day + muscle only. */
    private fun row(day: String, muscle: String, order: Int = 0) = RoutineExerciseEntity(
        dayOfWeek = day,
        name = "Test Row",
        muscleGroup = muscle,
        targetSets = 3,
        targetReps = "8–12",
        orderIndex = order
    )

    private fun report(issues: List<WeeklyBalanceIssue>): String =
        issues.joinToString("; ") {
            "${it.groupKey}=${it.perWeek} on ${it.days} (healthy ${it.healthyMin}..${it.healthyMax})"
        }

    @Test
    fun everyDefaultEngineWeekIsBalanced() {
        // The whole surface the welcome screen can produce with untouched
        // filters: every day count, session window, goal and difficulty. If
        // ANY of these goes red, the app is crying "unbalanced" about a week
        // it generated itself with no user filtering — the owner's report.
        for (days in 1..7)
            for (duration in WorkoutTemplates.DURATION_OPTIONS)
                for (goal in WorkoutTemplates.allWorkoutGoals)
                    for (intensity in listOf("Easy", "Medium", "Hard")) {
                        val issues =
                            weeklyBalanceIssues(engine(days, goal, duration, intensity).exercises)
                        assertTrue(
                            "$days d / $duration / $goal / $intensity: ${report(issues)}",
                            issues.isEmpty()
                        )
                    }
    }

    /**
     * Kit families that may go red when ONLY the equipment changes. Measured
     * at the default shape: every kit still fills the floor-bearing groups
     * (chest 2+, back 2+, legs 6+ at 3-day quick; double that at standard),
     * and the two gaps that remain — bodyweight/bands have no loaded curl
     * and no bar for chin-ups — land on groups whose floor is 0, because the
     * generator's own thin weeks contain no direct arms either. So NOTHING
     * goes red for equipment alone at the default shape; the old binary
     * checker's documented biceps gap is visible only as a NUMBER inside the
     * healthy band now, not as a verdict.
     */
    private val documentedEquipmentGaps: Map<String, Set<String>> = emptyMap()

    @Test
    fun changingOnlyEquipmentKeepsTheDefaultBuildBalanced() {
        val shapes = listOf(
            WorkoutTemplates.DURATION_STANDARD to "Medium",
            WorkoutTemplates.DURATION_QUICK to "Easy"
        )
        for (equipment in WorkoutTemplates.allEquipmentCategories.map(::listOf))
            for ((duration, intensity) in shapes) {
                val issues = weeklyBalanceIssues(
                    engine(duration = duration, intensity = intensity, equipment = equipment).exercises
                )
                val expected = documentedEquipmentGaps[equipment.single()] ?: emptySet()
                assertEquals(
                    "$intensity build with $equipment ($duration): ${report(issues)}",
                    expected,
                    issues.map { it.groupKey }.toSet()
                )
            }
    }

    @Test
    fun theCheckCatchesAWeekThatSkipsAMajorGroup() {
        // Custom-tab shape: the user picked only chest and back rows. The
        // floor-bearing groups must be named with their zero, not hidden.
        val chestAndBack = List(5) { i -> row(week[i % 3], "Pectorals", i) } +
            List(5) { i -> row(week[i % 3], "Lats", i) }
        val issues = weeklyBalanceIssues(chestAndBack)
        assertTrue(
            "legs must be flagged on a chest+back-only week: ${report(issues)}",
            "legs" in issues.map { it.groupKey }
        )
        assertTrue(
            "chest itself must NOT be flagged on a chest-heavy week (in range): ${report(issues)}",
            "chest" !in issues.map { it.groupKey }
        )
        // Mirror case: legs and back, no chest — the chest line must appear.
        val legsAndBack = List(5) { i -> row(week[i % 3], "Quadriceps", i) } +
            List(5) { i -> row(week[i % 3], "Lats", i) }
        assertTrue(
            "chest must be flagged on a legs+back-only week: ${report(weeklyBalanceIssues(legsAndBack))}",
            "chest" in weeklyBalanceIssues(legsAndBack).map { it.groupKey }
        )
    }

    @Test
    fun theCheckCatchesJunkVolumeOnOneGroup() {
        // The mirror case: one muscle piled past its ceiling is junk volume,
        // and the card must say so with the count and the range.
        val junkChest = List(12) { i -> row(week[i % 3], "Pectorals", i) }
        val issues = weeklyBalanceIssues(junkChest)
        val chest = issues.firstOrNull { it.groupKey == "chest" }
        assertTrue(
            "12 chest rows in a week must be over-trained: ${report(issues)}",
            chest != null && chest.isOvertrained
        )
    }

    @Test
    fun theScopeIsTheAdvancedTabsPartSelection() {
        // The DEFAULT advanced-tab selection is every part, which maps to
        // every balance group — "all unless the user changes it".
        assertEquals(BALANCE_GROUP_KEYS.toSet(), balanceGroupsForParts(emptyList()))
        assertEquals(BALANCE_GROUP_KEYS.toSet(), balanceGroupsForParts(allParts))
        // Narrowing the tab narrows the check to the matching groups.
        assertEquals(setOf("chest"), balanceGroupsForParts(listOf("Chest")))
        assertEquals(setOf("core"), balanceGroupsForParts(listOf("Abs", "Obliques")))
        assertEquals(
            setOf("chest", "back"),
            balanceGroupsForParts(listOf("Chest", "Lats", "Upper Traps", "Spinal Erectors"))
        )
        // Forearms have no balance group, so a forearms-only selection judges nothing.
        assertEquals(emptySet<String>(), balanceGroupsForParts(listOf("Forearms")))

        // And the scope actually filters what gets reported: a chest-only
        // scope on a legs-less week reports nothing about the missing legs.
        val chestOnlyScope = listOf("Chest")
        val noLegs = List(3) { i -> row(week[i], "Pectorals", i) }
        assertEquals(
            emptyList<WeeklyBalanceIssue>(),
            weeklyBalanceIssues(noLegs, balanceGroupsForParts(chestOnlyScope))
        )
        // The same week under the default (all-groups) scope still names them.
        assertTrue(weeklyBalanceIssues(noLegs).isNotEmpty())
    }

    /**
     * AN EMPTY BUILDER IS NOT AN UNBALANCED ONE.
     *
     * Owner, 2026-10-12: *"in the custom build show the days empty not filled with already
     * exercises"*. The custom tab now opens with zero exercises, so it necessarily passes an
     * empty list to [weeklyBalanceIssues]. Without this test that case would only be checked by
     * reading the source: a regression that reports every group as undertrained on a blank week
     * would greet a first-time user with six red warnings for a routine they have not built yet.
     */
    @Test
    fun anEmptyBuilderIssuesNoVerdict() {
        assertEquals(
            "a week with no exercises must not be reported as unbalanced",
            emptyList<WeeklyBalanceIssue>(),
            weeklyBalanceIssues(emptyList()),
        )
    }
}
