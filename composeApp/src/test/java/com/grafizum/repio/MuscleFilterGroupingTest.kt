package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.muscleGroupIconRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    /**
     * ONE GLYPH PER BROAD PART, AND NO GLYPH FOR ANYTHING THAT IS NOT ONE.
     *
     * Owner, 2026-10-11: *"i want the equipment way of displaying ... to also have the same style in
     * muscle group 'show more' label. And instead of the muscle group label name it shows also the
     * icon."* The BODY-PART CHIP draws its glyph from [muscleGroupIconRes] (the collapsed row's
     * value went back to plain text on 2026-10-12 — see [MuscleGlyphFamilyTest]), which reduces
     * whatever the filter holds - a broad part or a precise muscle - through the SAME topic function
     * the partition above and the badge's colour use, so the glyph cannot disagree with the rest of
     * the app about which part a muscle belongs to.
     *
     * Distinctness is the assertion that matters as much as the lookup: six parts, six drawings. Two
     * parts sharing one glyph would make a row filtering by Back show the shape of a leg.
     */
    @Test
    fun everyBroadPartHasItsOwnGlyphAndNothingElseHasOne() {
        val glyphs = broad.associateWith { muscleGroupIconRes(it) }

        assertTrue(
            "a broad part has no glyph to draw: $glyphs",
            glyphs.values.all { it != null }
        )
        assertEquals(
            "two broad parts share one drawing: " +
                "${glyphs.entries.groupBy({ it.value }, { it.key }).filterValues { it.size > 1 }}",
            broad.size,
            glyphs.values.filterNotNull().toSet().size
        )

        // A PRECISE selection draws its GROUP's glyph: the row holds "Lats" the moment that muscle
        // is tapped, and it must still read as Back rather than losing its icon.
        precise.forEach { muscle ->
            assertEquals(
                "the glyph for '$muscle' is not its group's",
                glyphs[WorkoutTemplates.getTopicForMuscle(muscle)],
                muscleGroupIconRes(muscle)
            )
        }

        // "Any" means every muscle, which is not a body part - null is what makes the row fall back
        // to the word, the same way the equipment row falls back to "All equipment".
        assertNull("the Any state must not draw a body part", muscleGroupIconRes(null))
        assertNull(muscleGroupIconRes(""))
        assertNull(muscleGroupIconRes("   "))
        assertNull(
            "a group the app cannot place must not draw a guessed body part",
            muscleGroupIconRes("Spleen")
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
