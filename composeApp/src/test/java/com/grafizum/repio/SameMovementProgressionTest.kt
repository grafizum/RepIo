package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE OFFER MUST BE THE SAME MOVEMENT, DONE HARDER.
 *
 * Owner, 2026-10-17: *"When doing cross body crunch to max level, it offers
 * standing side bend which is a completely different movement plane. Why tf is
 * this given as progression if its more of a regression. Fix this for all
 * exercises, i cant have progression exercises be shown that arent even
 * progressing the exercise before. Fix that immediately thats a huge bug."*
 *
 * The CSV's curated branches are the app's offer, and one of them named a
 * neighbouring exercise rather than a harder version of the same one — with a
 * note claiming it "keeps the same movement", which was the part that made it a
 * lie rather than a taste question. These tests hold the FILTER, not the row:
 * a data pass can fix the note, but only the filter stops the next such row
 * from being offered.
 */
class SameMovementProgressionTest {

    @Test
    fun theReportedCaseIsNotOffered() {
        val crunch = ProgressionEngine.entryFor("Cross-Body Crunch")
            ?: error("Cross-Body Crunch must stay in the pool")
        val sideBend = ProgressionEngine.entryFor("Standing Side Bend")
            ?: error("Standing Side Bend must stay in the pool")

        assertFalse(
            "a bodyweight strength rotation is not progressed by a mobility stretch",
            ProgressionEngine.isSameMovement(crunch, sideBend)
        )
        assertNull(
            "the crunch must have no offer at all — the owner asked for no bad offer, not a worse one",
            ProgressionEngine.preferredBranch(crunch)
        )
        assertTrue(
            "with no same-movement branch the exercise reads as a ceiling, which is honest",
            ProgressionEngine.branchesFor(crunch).isEmpty()
        )
    }

    @Test
    fun aRealProgressionIsStillOffered() {
        val curl = ProgressionEngine.entryFor("Cross Body Hammer Curl")
            ?: error("Cross Body Hammer Curl must stay in the pool")

        val offer = ProgressionEngine.preferredBranch(curl)
        assertNotNull(
            "a biceps hammer ladder must still offer its next rung",
            offer
        )
        assertEquals(
            "Incline Hammer Curl is the same movement, asked heavier",
            "Incline Hammer Curl",
            offer!!.entry
        )
    }

    @Test
    fun theFilterKeepsTheCuratedOfferSystemIntact() {
        val withBranches = ExerciseProgression.ENTRIES.filter { it.branches.isNotEmpty() }
        val surviving = withBranches.count { ProgressionEngine.branchesFor(it).isNotEmpty() }
        assertTrue(
            "the same-movement rule kept $surviving of ${withBranches.size} curated offers; " +
                "a filter that removes most of them would quietly end the feature",
            surviving * 2 >= withBranches.size
        )
    }

    @Test
    fun theTwoHalvesOfTheRuleArePinnedOnTheirOwn() {
        val crunch = ProgressionEngine.entryFor("Cross-Body Crunch")!!
        val sideBend = ProgressionEngine.entryFor("Standing Side Bend")!!

        // Same muscle, different training family — the half that catches the report.
        assertEquals(
            "both are filed under Obliques",
            ExerciseCatalog.entryForId(crunch.id)!!.muscle,
            ExerciseCatalog.entryForId(sideBend.id)!!.muscle
        )
        assertFalse(ProgressionEngine.isSameMovement(crunch, sideBend))

        // The families themselves: a barbell ladder and its bodyweight variation
        // are one family, a stretch is not one of them.
        assertEquals("strength", ProgressionEngine.trainingFamily("strength_weighted_or_machine"))
        assertEquals("strength", ProgressionEngine.trainingFamily("strength_bodyweight"))
        assertEquals("mobility", ProgressionEngine.trainingFamily("mobility_stretching"))
        assertEquals("cardio", ProgressionEngine.trainingFamily("cardio_conditioning"))
    }

    @Test
    fun anExerciseTheCatalogDoesNotCarryIsNotSilentlyUnoffered() {
        val curl = ProgressionEngine.entryFor("Cross Body Hammer Curl")!!
        // A target the catalog cannot judge keeps its curated branch rather than
        // losing it to a lookup miss: the filter is about movement, not about data.
        val unknown = curl.copy(id = "not-in-the-catalog", branches = curl.branches)
        assertTrue(
            ProgressionEngine.isSameMovement(unknown, curl)
        )
    }
}
