package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE TWO MUSCLE VOCABULARIES ARE A CHOICE AND ITS REFINEMENTS, not two lists in one block.
 *
 * Owner, 2026-10-11: *"when selecting a muscle group in add exercise or whatever, make sure the
 * small 'muscle group: back' is shown also while scrolling through each precise muscle so that it
 * shows what these muscle are (from the back cathegory / group) and when selecting muscle group
 * back, then make sure that the precise muscles are shown only of that 'back' cathegory /
 * group"*.
 *
 * The rule that makes narrowing safe is a partition: the six broad parts must claim every precise
 * muscle exactly once. If one fell outside them, picking a group would make that muscle
 * unreachable - the same class of silent loss as the equipment filter's kit-free category - and if
 * two groups claimed the same muscle, "Back" and "Shoulders" would answer with the same chip twice.
 */
class MuscleFilterGroupingTest {

    private val broad: List<String> = WorkoutTemplates.getBroadMuscleGroups()
    private val precise: List<String> = WorkoutTemplates.getPreciseMuscleGroups()

    @Test
    fun theSixBroadPartsClaimEveryPreciseMuscleExactlyOnce() {
        val claimed = precise.filter { WorkoutTemplates.getTopicForMuscle(it) == "Other" }

        assertTrue(
            "these precise muscles belong to no broad group, so selecting any group hides them " +
                "and no single group can ever offer them: $claimed",
            claimed.isEmpty()
        )

        val union = broad.flatMap { WorkoutTemplates.getPreciseMuscleGroupsFor(it) }
        assertEquals(
            "the six groups do not add up to the precise list",
            precise.toSet(),
            union.toSet()
        )
        assertEquals(
            "the groups overlap: ${union.groupingBy { it }.eachCount().filterValues { it > 1 }}",
            precise.size,
            union.size
        )
    }

    @Test
    fun pickingBackOffersOnlyBackMuscles() {
        val back = WorkoutTemplates.getPreciseMuscleGroupsFor("Back")

        assertEquals(
            "the Back group must offer exactly its own muscles, in catalogue order",
            listOf("Lats", "Upper Back", "Upper Traps", "Spinal Erectors"),
            back
        )
        assertTrue(
            "a leg or arm muscle leaked into the Back list: $back",
            back.none { it in listOf("Quadriceps", "Hamstrings", "Biceps", "Pectorals") }
        )
    }

    /**
     * A PRECISE selection answers with its own GROUP's list, not with itself. The user tapped
     * "Lats" while looking at the Back muscles; a list of one, or a silent re-open of the other
     * five groups, would both lose the context the owner asked for.
     */
    @Test
    fun aPreciseSelectionKeepsItsGroupContext() {
        val fromPrecise = WorkoutTemplates.getPreciseMuscleGroupsFor("Lats")

        assertEquals(WorkoutTemplates.getPreciseMuscleGroupsFor("Back"), fromPrecise)
        assertEquals(
            "the caption's group label must come out as Back for Lats",
            "Back",
            WorkoutTemplates.getTopicForMuscle("Lats")
        )
    }

    @Test
    fun anySelectionOffersEveryPreciseMuscle() {
        assertEquals(precise, WorkoutTemplates.getPreciseMuscleGroupsFor(null))
        assertEquals(precise, WorkoutTemplates.getPreciseMuscleGroupsFor(""))
        assertEquals(precise, WorkoutTemplates.getPreciseMuscleGroupsFor("   "))
        // An unknown value falls back to everything rather than to an empty row: an empty filter
        // cannot be told apart from a broken one.
        assertEquals(precise, WorkoutTemplates.getPreciseMuscleGroupsFor("Spleen"))
    }
}
