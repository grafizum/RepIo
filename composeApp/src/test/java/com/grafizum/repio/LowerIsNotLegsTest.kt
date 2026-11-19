package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "A day training (Lower) body, is a day full of legs which im fairly sure is wrong."
 * — owner report 2026-10-08.
 *
 * He was right. `enginePartsFor("lower")` returned ENGINE_LEG_PARTS verbatim, so in
 * a 4-day upper/lower week BOTH lower days trained the same four leg muscles, and
 * the "lower" day was indistinguishable from the dedicated "legs" day. A lower day
 * is the lower-body half of an upper/lower split and carries the posterior chain
 * and the trunk; a legs day stays quad-dominant.
 *
 * The assertion is deliberately the OBSERVABLE one - what the generated week
 * actually contains - rather than the private part list, so it fails if either
 * mapping regresses, however it is reached.
 */
class LowerIsNotLegsTest {

    private fun groupsFor(days: Int, dayIndex: Int): Set<String> {
        val dayNames = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        val program = WorkoutTemplates.generateEngineProgram(
            selectedDays = dayNames.take(days),
            workoutGoal = "goal_bodybuilding",
            equipment = listOf(WorkoutTemplates.EQUIPMENT_ALL),
        )
        val day = dayNames[dayIndex]
        // muscleGroup is the precise group the generator trained that slot on.
        return program.exercises.filter { it.dayOfWeek == day }.map { it.muscleGroup }.toSet()
    }

    /** The 4-day split is exactly upper / lower / upper / lower. */
    @Test
    fun theTwoLowerDaysOfAFourDayWeekAreNotASecondLegsDay() {
        val lower = groupsFor(4, 1)
        assertTrue("expected the lower day to train something: $lower", lower.isNotEmpty())
        val legsOnly = setOf("Quadriceps", "Hamstrings", "Glutes", "Calves")
        assertTrue(
            "the lower day trains only leg muscles, so it is the legs day again: $lower",
            lower.any { it !in legsOnly }
        )
    }

    @Test
    fun theLowerDayCarriesThePosteriorChainAndTheTrunk() {
        val lower = groupsFor(4, 1)
        val trunkOrHinge = setOf("Spinal Erectors", "Abs", "Obliques")
        assertTrue(
            "a lower day must carry the trunk/hip-hinge work an upper day never touches: $lower",
            lower.any { it in trunkOrHinge }
        )
    }

    @Test
    fun anUpperDayNeverTrainsALegOrTrunkMuscle() {
        // The other half of the split, so the two days cannot drift into each other.
        val upper = groupsFor(4, 0)
        val forbidden = setOf("Quadriceps", "Hamstrings", "Glutes", "Calves", "Spinal Erectors", "Abs")
        assertTrue("an upper day leaked a lower-body muscle: $upper", upper.none { it in forbidden })
    }
}
