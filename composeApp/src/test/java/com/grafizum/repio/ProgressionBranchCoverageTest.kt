package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the progression-branch data, which grew from 29 hand-written rows to
 * 462 derived from the ladder tables in tools/exercise-db/data/.
 *
 * Two things here are load-bearing rather than descriptive:
 *
 *  1. DIRECTION. A branch's `entry` is the exercise the user moves TO and the
 *     row it hangs off is the one they leave. `ProgressionEngine.evolutionOffer`
 *     resolves the popup with `sourceEntry.branches.firstOrNull { it.entry ==
 *     targetName }`, so a branch pointing at its own source renders as
 *     "you have mastered X -> try X". That is the one sentence this popup must
 *     never produce, and it is exactly what a mis-ordered ladder produces, so
 *     it is asserted over every row rather than eyeballed.
 *
 *  2. NODES. `nodes` is the DESTINATION's own future path, not more options at
 *     this level. Listing the entry among its own nodes would draw the same
 *     exercise twice in one card.
 *
 * The coverage floor is not "every exercise has a branch" and cannot be: the
 * pool holds 341 machine rows and 66 stretches/poses, which progress by load or
 * by time rather than by becoming another exercise - the level ladder already
 * carries those. The floor is set below the measured value so an accidental
 * deletion of a whole ladder is caught, while a legitimate pool change that
 * removes rows does not fail the build on its own.
 */
class ProgressionBranchCoverageTest {

    private companion object {
        /** Measured 65% of free-weight rows carry a branch; the floor sits below. */
        const val MIN_FREE_WEIGHT_COVERAGE_PERCENT = 60
    }

    private val entries = ExerciseProgression.ENTRIES
    private val withBranches = entries.filter { it.branches.isNotEmpty() }
    private val allBranches = entries.flatMap { it.branches }

    @Test
    fun coverageStaysAboveTheFloorSoDeletingALadderIsCaught() {
        assertTrue(
            "only ${withBranches.size} of ${entries.size} exercises have a " +
                "progression branch; the ladder tables were probably not run",
            withBranches.size >= 300,
        )
    }

    @Test
    fun everyBranchPointsAtADifferentExerciseThanTheOneItHangsOff() {
        val selfPointing = entries.flatMap { e -> e.branches.filter { it.entrySlug == e.slug } }
        assertTrue("a branch must never point at its own source", selfPointing.isEmpty())
    }

    @Test
    fun noBranchListsItsOwnDestinationInItsNodes() {
        val repeated = allBranches.filter { it.entry in it.nodes }
        assertTrue(
            "nodes are the path BEYOND the entry, not the entry again: " +
                repeated.joinToString { "${it.entry} -> ${it.nodes}" },
            repeated.isEmpty(),
        )
    }

    @Test
    fun nodesStayShortEnoughToRenderAsAPath() {
        val longest = allBranches.maxOfOrNull { it.nodes.size } ?: 0
        assertTrue("a node list of $longest stops being a path and becomes a wall of text", longest <= 3)
    }

    /**
     * Two branches off the SAME source may not name the same destination.
     *
     * This is not a style rule, it is a reachability one.
     * `ProgressionEngine.evolutionOffer` resolves a branch with
     * `branches.firstOrNull { it.entry == targetName }`, so where a source has
     * N rows sharing one entry, only the FIRST is ever reachable - the rest are
     * dead rows that no user action can select. Any picker would still render
     * them, showing the same exercise two or three times as if they were
     * different choices.
     *
     * It happened because a rung legitimately belongs to more than one ladder
     * (a dumbbell bench press is the bottom of both the press ladder and the
     * floor-press ladder), and each ladder emitted its own branch for it. The
     * pipeline's dedup was keyed on `(entry, nodes)` and so treated differing
     * node lists as meaningfully different branches - a distinction the engine
     * cannot act on. Measured before the fix: 28 of 334 exercises carried a
     * repeated entry, and Dumbbell Shoulder Press offered "Barbell Overhead
     * Press" three times. `merge_branches` now merges by entry and unions the
     * nodes.
     */
    @Test
    fun noExerciseOffersTheSameDestinationTwice() {
        val repeated = entries.mapNotNull { e ->
            val entries_ = e.branches.map { it.entry }
            val dupes = entries_.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            if (dupes.isEmpty()) null else "${e.id}: ${dupes.joinToString()}"
        }
        assertTrue(
            "the same destination offered more than once (the extras are unreachable, " +
                "since evolutionOffer resolves by entry and takes the first):\n" +
                repeated.joinToString("\n"),
            repeated.isEmpty(),
        )
    }

    @Test
    fun everyBranchNamesAnExerciseThatActuallyExistsInThePool() {
        val known = ExerciseCatalog.ENTRIES.map { it.name }.toSet()
        val unknown = allBranches.flatMap { listOf(it.entry) + it.nodes }.filterNot { it in known }
        assertTrue("branch names outside the pool: ${unknown.distinct()}", unknown.isEmpty())
    }

    @Test
    fun everyBranchCarriesACoachNoteAndAKnownTag() {
        val blank = allBranches.filter { it.note.isBlank() }
        assertTrue("a branch with no note says nothing: ${blank.size} of them", blank.isEmpty())
        val badTag = allBranches.filter { it.tag != "new" && it.tag != "advanced" }
        assertTrue("unexpected tags: ${badTag.map { it.tag }.distinct()}", badTag.isEmpty())
    }

    /**
     * The popup opens on `preferredBranch`, which prefers an "advanced" branch
     * whose target ladder is at least as long as the source's. If no exercise in
     * the pool could satisfy that, the popup would have nothing sensible to
     * open on - so the set of exercises that CAN has to stay non-trivial.
     */
    @Test
    fun thePopupCanAlwaysOpenOnAnAdvancedBranch() {
        val openable = entries.count { ProgressionEngine.preferredBranch(it)?.tag == "advanced" }
        assertTrue("only $openable exercises can open the popup on an advanced branch", openable >= 50)
    }

    /**
     * The owner's scope decision, asserted so it cannot quietly reverse: every
     * bodyweight and free-weight row in the pool is reachable as a progression
     * OR is the top of its ladder. Machines and stretches are excluded by design.
     */
    @Test
    fun mostFreeWeightRowsAreEitherAStartingPointOrATopOfTheLadder() {
        // The catalog is joined by exercise NAME (ExerciseCatalog.ENTRIES carries no
        // slug), and ProgressionEngine.entryFor is the app's own name -> entry
        // resolver, so this goes through the same path the popup does rather
        // than a private lookup that could drift from it.
        val freeWeight = ExerciseCatalog.ENTRIES.mapNotNull { catalogEntry ->
            val equipment = catalogEntry.equipment
            if (equipment == "Machine" || equipment == "Cable") {
                null
            } else {
                ProgressionEngine.entryFor(catalogEntry.name)
            }
        }
        val covered = freeWeight.count { it.branches.isNotEmpty() }
        // A FLOOR, not an equality: adding a ladder is a win and must not fail
        // this test, while deleting one wholesale is the failure it guards.
        assertTrue(
            "free-weight coverage dropped to $covered of ${freeWeight.size}; " +
                "the ladder tables were probably not run",
            covered * 100 >= freeWeight.size * MIN_FREE_WEIGHT_COVERAGE_PERCENT,
        )
    }
}