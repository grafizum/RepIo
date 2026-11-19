package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the mastery hub's data plumbing (the graphical cards are thin over
 * [MasteryCardData], so the data contract is what matters):
 *
 *  - mastered exercises resolve a reason and carry honest card data,
 *  - every curated branch points at real catalog exercises with usable notes,
 *  - the branch start level maps the user's depth and clamps,
 *  - a true-ceiling exercise suggests alternatives instead of branches.
 */
class MasteryHubTest {

    private fun entry(name: String) = ProgressionEngine.entryFor(name)
        ?: error("'$name' must exist in the generated pool for this test")

    @Test
    fun benchAtCeilingProducesACompleteCard() {
        val e = entry("Barbell Bench Press")
        val effMax = ProgressionEngine.effectiveMaxLevel(e)
        val state = ProgressionEngine.ProgressionState(effMax, ProgressionEngine.patternLength(e))
        assertEquals(ProgressionEngine.MasteryReason.CEILING_REACHED, ProgressionEngine.masteryReason(e, state))
        // The card's load line is the ceiling itself.
        assertEquals(e.ceiling!!.load!!, ProgressionEngine.valueAtLevelWithCeiling(e, effMax)!!, 0.001)
        // The bench has a curated branch to continue on.
        assertTrue(ProgressionEngine.branchesFor(e).isNotEmpty())
    }

    @Test
    fun trueCeilingRowSuggestsAlternatives() {
        // Every suggestion must be a real exercise the user could actually train.
        val suggestions = ProgressionEngine.suggestedAlternatives("Barbell Bench Press")
        assertTrue(suggestions.isNotEmpty())
        suggestions.forEach { name ->
            assertNotNull("suggested '$name' must be a catalog exercise", com.grafizum.repio.data.ExerciseCatalog.entryFor(name))
        }
        // ... and never the exercise itself.
        assertTrue("Barbell Bench Press" !in suggestions)
    }

    @Test
    fun branchStartLevelStaysInsideTheTargetLadder() {
        val source = entry("Barbell Bench Press")
        val target = entry("Dumbbell Hammer Curl")
        val sourceMax = ProgressionEngine.effectiveMaxLevel(source)
        val targetMax = ProgressionEngine.effectiveMaxLevel(target)
        for (sourceLevel in 1..sourceMax) {
            val start = ProgressionEngine.branchStartLevel(
                source, ProgressionEngine.ProgressionState(sourceLevel, 1), target,
            )
            assertTrue("start level $start outside 1..$targetMax", start in 1..targetMax)
        }
    }

    @Test
    fun masteredRowWithBranchesOffersThemOnItsCard() {
        // The romanian deadlift is ready, curated, and its branch target too —
        // the full accept-a-branch flow must be reachable for it.
        val rdl = entry("Romanian Deadlift")
        assertTrue(ProgressionEngine.isProgressionReady(rdl))
        assertTrue(ProgressionEngine.branchesFor(rdl).isNotEmpty())
        ProgressionEngine.branchesFor(rdl).forEach { b ->
            val target = com.grafizum.repio.data.ExerciseProgression.forName(b.entry)
            assertTrue("'${b.entry}' ready", target != null && ProgressionEngine.isProgressionReady(target))
        }
    }

    @Test
    fun everyBranchEntryIsProgressionReady() {
        // Accepting a branch stores a progression state for the TARGET; the
        // acceptance must land on an exercise the level system can actually
        // carry. (Nodes are display-only roadmap labels — no stored state.)
        val blocked = com.grafizum.repio.data.ExerciseProgression.ENTRIES
            .filter { it.branches.isNotEmpty() }
            .flatMap { it.branches }
            .mapNotNull { b ->
                val target = com.grafizum.repio.data.ExerciseProgression.forName(b.entry)
                if (target != null && ProgressionEngine.isProgressionReady(target)) null else b.entry
            }
        assertTrue("branch targets outside the level system: $blocked", blocked.isEmpty())
    }
}
