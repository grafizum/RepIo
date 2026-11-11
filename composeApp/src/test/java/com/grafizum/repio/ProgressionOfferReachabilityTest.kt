package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The progression prompt must be REACHABLE for every exercise that has a
 * progression in the database (board, owner 2026-11-11: "when an exercise has
 * reached a certain level, then a progression should be asked based on the
 * overall database information").
 *
 * This pins the two states a real user can be in:
 *  - sitting ON the exercise's last rung with its top stage finished, and
 *  - sitting ABOVE it, which is what happens after a researched top is lowered
 *    for an exercise someone is already training past it.
 *
 * The second case is the one that silently lost the popup: `isMastered` used to
 * demand `level == max`, so a user above a lowered top never got an offer again.
 */
class ProgressionOfferReachabilityTest {

    @Test
    fun everyExerciseWithBranchesCanBeOfferedAtItsLastRung() {
        val problems = mutableListOf<String>()
        for (entry in ExerciseCatalog.ENTRIES) {
            val prog = ProgressionEngine.entryFor(entry.name) ?: continue
            if (!ProgressionEngine.isProgressionReady(prog)) continue
            if (ProgressionEngine.branchesFor(prog).isEmpty()) continue
            val top = ProgressionEngine.effectiveMaxLevel(prog)
            val stages = ProgressionEngine.patternLength(prog)
            val onTop = ProgressionEngine.ProgressionState(level = top, stage = stages)
            if (ProgressionEngine.masteryReason(prog, onTop) == null) {
                problems.add("${entry.name}: no offer on its last rung (level $top, stage $stages)")
            }
        }
        assertTrue(
            "every branched exercise must be offerable on its last rung:\n" +
                problems.take(20).joinToString("\n"),
            problems.isEmpty(),
        )
    }

    @Test
    fun aUserAboveALoweredTopIsStillOfferedProgression() {
        val problems = mutableListOf<String>()
        for (entry in ExerciseCatalog.ENTRIES) {
            val prog = ProgressionEngine.entryFor(entry.name) ?: continue
            if (!ProgressionEngine.isProgressionReady(prog)) continue
            if (ProgressionEngine.branchesFor(prog).isEmpty()) continue
            if (ProgressionEngine.isRepLadder(prog)) continue
            val past = ProgressionEngine.effectiveMaxLevel(prog) + 3
            val above = ProgressionEngine.ProgressionState(level = past, stage = 1)
            if (ProgressionEngine.masteryReason(prog, above) == null) {
                problems.add("${entry.name}: level $past is past its top and gets no offer")
            }
        }
        assertTrue(
            "a user training above a lowered top must still be offered progression:\n" +
                problems.take(20).joinToString("\n"),
            problems.isEmpty(),
        )
    }
}
