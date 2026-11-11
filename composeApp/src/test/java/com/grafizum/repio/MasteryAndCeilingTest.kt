package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Guards the mastery layer: human ceilings cap the ladders, mastered
 * exercises expose their reason and branches, and the branch starting level
 * maps the user's relative depth onto the target exercise.
 */
class MasteryAndCeilingTest {

    private fun entry(name: String) = ProgressionEngine.entryFor(name)
        ?: error("'$name' must exist in the generated pool for this test")

    // --- Ceiling-aware ladders ----------------------------------------------

    @Test
    fun benchLadderEndsOnItsSourcedTop() {
        val e = entry("Barbell Bench Press")
        val effMax = ProgressionEngine.effectiveMaxLevel(e)
        // The last rung is the SOURCED top now - advanced bench at 1.25x an 80 kg
        // lifter is 100 kg - not the curated elite mark of 180 kg it used to stretch
        // to. So the ladder is 33 rungs of 2.5 kg from the bare bar, where it used to
        // be 65. Every displaced elite mark is listed in
        // docs/level-system-phase2-level-rebuild.md so one can be restored in a line.
        assertEquals("bench rungs at a constant 2.5 kg from 20 to 100", 33, effMax)
        // The ceiling load lands exactly on the effective max.
        assertEquals(e.ceiling!!.load!!, ProgressionEngine.valueAtLevelWithCeiling(e, effMax)!!, 0.001)
        // ... and never anywhere else.
        for (lvl in 1..effMax) {
            assertTrue(ProgressionEngine.valueAtLevelWithCeiling(e, lvl)!! <= e.ceiling!!.load!!)
        }
        // Below the top the ladder is ONE step per rung from level 1 (owner
        // 2026-10-13: "the increase of the weight should always be the same"), so
        // the bench's authored 60 kg top - which used to sit on level 10, after
        // steps of 2.5 kg four times, 5 kg four times and 10 kg - now sits on
        // level 17 and every rung below it is 2.5 kg heavier than the last. The
        // CSV sequence is no longer what the user climbs; the row's own step is.
        assertEquals(20.0, ProgressionEngine.valueAtLevelWithCeiling(e, 1)!!, 0.001)
        assertEquals(22.5, ProgressionEngine.valueAtLevelWithCeiling(e, 2)!!, 0.001)
        assertEquals(60.0, ProgressionEngine.valueAtLevelWithCeiling(e, 17)!!, 0.001)
        for (lvl in 2..17) {
            assertEquals(
                "bench rung $lvl must be exactly one 2.5 kg step above ${lvl - 1}",
                2.5,
                ProgressionEngine.valueAtLevelWithCeiling(e, lvl)!! -
                    ProgressionEngine.valueAtLevelWithCeiling(e, lvl - 1)!!,
                0.001
            )
        }
    }

    @Test
    fun aRepLadderClimbsToTheRepCeilingNotTheDeclaredRungCount() {
        // Owner ruling 2026-10-03: "changing the position of exercise i want
        // removed, instead increase the reps". A bodyweight row's declared max
        // level was the length of its VARIATION ladder - how many substitute
        // movements it had - which stops being the level's meaning the moment
        // the level counts reps. Its length is now the rep climb.
        val e = entry("Glute Bridge")
        assertTrue(ProgressionEngine.isRepLadder(e))
        assertEquals(
            ProgressionEngine.repRungs(e),
            ProgressionEngine.effectiveMaxLevel(e)
        )
        // The top rung lands within one step of the row's own ceiling - the
        // rungs step, so it does not have to land exactly on it.
        assertTrue(
            ProgressionEngine.repsAtLevel(e, ProgressionEngine.effectiveMaxLevel(e)) >=
                ProgressionEngine.repCeilingFor(e) - ProgressionEngine.repStepFor(e)
        )
    }

    @Test
    fun aRepLaddersTopLevelPrescribesTheCeilingRepsAndNotAVariation() {
        // The level names no movement any more, so there is no variation to
        // prescribe at the top of it - the reps are the whole answer.
        val e = entry("Pistol Squat")
        val top = ProgressionEngine.effectiveMaxLevel(e)
        assertNull(ProgressionEngine.prescribedValueAtLevel(e, top))
        assertEquals(
            ProgressionEngine.repsAtLevel(e, top),
            ProgressionEngine.prescribedReps(e, top, 1)!!.first()
        )
        assertTrue(
            ProgressionEngine.repsAtLevel(e, top) >=
                ProgressionEngine.repCeilingFor(e) - ProgressionEngine.repStepFor(e)
        )
    }

    // --- Mastery detection ---------------------------------------------------

    @Test
    fun notMasteredAtTheBottomOfTheLadder() {
        val e = entry("Barbell Bench Press")
        assertFalse(ProgressionEngine.isMastered(e, ProgressionEngine.ProgressionState(1, 1)))
        assertNull(ProgressionEngine.masteryReason(e, ProgressionEngine.ProgressionState(1, 1)))
    }

    @Test
    fun masteredAtTheCeilingTopWithCeilingReason() {
        val e = entry("Barbell Bench Press")
        val effMax = ProgressionEngine.effectiveMaxLevel(e)
        val state = ProgressionEngine.ProgressionState(effMax, ProgressionEngine.patternLength(e))
        assertTrue(ProgressionEngine.isMastered(e, state))
        assertEquals(ProgressionEngine.MasteryReason.CEILING_REACHED, ProgressionEngine.masteryReason(e, state))
    }

    /**
     * The max is a MARKER, not a wall: from the top stage of the max rung the
     * level-up goes PAST it rather than stopping (owner 2026-10-13: "add more
     * levels after it ... past-max levels must be selectable").
     */
    @Test
    fun levelUpClimbsPastTheEffectiveMax() {
        val e = entry("Barbell Bench Press")
        val effMax = ProgressionEngine.effectiveMaxLevel(e)
        val top = ProgressionEngine.ProgressionState(effMax, ProgressionEngine.patternLength(e))
        val up = ProgressionEngine.levelUp(e, top)
        assertNotNull("the level-up at the max must not be blocked", up)
        assertTrue(up!!.leveledUp)
        assertEquals(effMax + 1, up.newState.level)
        // ...and it is a real prescription: the rung above the max is heavier.
        assertTrue(up.value!! > ProgressionEngine.valueAtLevelWithCeiling(e, effMax)!!)
    }

    @Test
    fun bodyweightLadderCompletesWithLadderReason() {
        // A rep ladder is "at the offer" from its first checkpoint, not only at
        // the top: at the top there is nothing left to climb, so the reason is
        // LADDER_COMPLETE (no load ceiling was crossed).
        val e = entry("Glute Bridge")
        val max = ProgressionEngine.effectiveMaxLevel(e)
        val state = ProgressionEngine.ProgressionState(max, ProgressionEngine.patternLength(e))
        assertEquals(ProgressionEngine.MasteryReason.LADDER_COMPLETE, ProgressionEngine.masteryReason(e, state))
    }

    // --- Branches ------------------------------------------------------------

    @Test
    fun curatedBranchesExistForTheKeyLifts() {
        // The pull-up branch routes through the pulldown family (the only
        // branch targets the level system can carry — see MasteryHubTest).
        assertTrue(ProgressionEngine.branchesFor(entry("Knee Push Ups")).isNotEmpty())
        assertTrue(ProgressionEngine.branchesFor(entry("Incline Push-Up")).isNotEmpty())
        assertTrue(ProgressionEngine.branchesFor(entry("Barbell Back Squat")).size >= 1)
        assertTrue(ProgressionEngine.branchesFor(entry("Dumbbell Bicep Curl")).size >= 2)
        assertTrue(ProgressionEngine.branchesFor(entry("Pull-Up")).size >= 1)
        // Isolation work used to be the one family with no branches at all -
        // it was hand-written data, and a curl genuinely does not "become"
        // another exercise. The ladder tables (2026-10-02) gave the isolation
        // families their published orderings, so Preacher Curl now has both a
        // destination (Preacher Hammer Curl) and its own path forward. What
        // stays true is that a row with NO rung above it in any ladder has no
        // branches; the top of every ladder is that row.
        assertTrue(ProgressionEngine.branchesFor(entry("Preacher Curl")).isNotEmpty())
        assertTrue(ProgressionEngine.branchesFor(entry("Front Squat")).isNotEmpty())
    }

    @Test
    fun branchEntriesAndNodesAreAllCatalogExercises() {
        val withBranches = com.grafizum.repio.data.ExerciseProgression.ENTRIES.filter { it.branches.isNotEmpty() }
        assertTrue(withBranches.isNotEmpty())
        for (e in withBranches) {
            for (b in e.branches) {
                assertNotNull(
                    "branch entry '${b.entry}' of '${e.slug}' must be a catalog exercise",
                    com.grafizum.repio.data.ExerciseCatalog.entryFor(b.entry),
                )
                for (n in b.nodes) {
                    assertNotNull(
                        "branch node '$n' of '${e.slug}' must be a catalog exercise",
                        com.grafizum.repio.data.ExerciseCatalog.entryFor(n),
                    )
                }
                assertTrue("tag must be new or advanced: ${b.tag}", b.tag == "new" || b.tag == "advanced")
                assertTrue(b.note.isNotBlank())
            }
        }
    }

    @Test
    fun branchStartLevelMapsRelativeDepthAndClamps() {
        val source = entry("Barbell Bench Press")
        val target = entry("Front Squat")
        val sourceMax = ProgressionEngine.effectiveMaxLevel(source)
        val targetMax = ProgressionEngine.effectiveMaxLevel(target)
        // Halfway through the source maps to roughly halfway through the target.
        val half = ProgressionEngine.branchStartLevel(
            source, ProgressionEngine.ProgressionState(sourceMax / 2, 1), target,
        )
        assertTrue(half in 1..targetMax)
        assertTrue(half >= targetMax / 2 - 1 && half <= targetMax / 2 + 1)
        // Clamped at the bottom.
        assertEquals(
            1, ProgressionEngine.branchStartLevel(source, ProgressionEngine.ProgressionState(1, 1), target),
        )
        // Clamped at the TOP, and WHICH arm of the rule applies is a fact about the
        // two ladders' lengths - so this asks the data instead of assuming:
        //
        //  - an EQUAL-length target has no headroom to give a finished user, so the
        //    mapping is capped partway down (the swap would otherwise be over before
        //    it began, and the popup would draw two identical bars);
        //  - a LONGER target takes the plain relative depth and lands on its top,
        //    which is the point of a longer ladder. That arm has its own sweep in
        //    [aLongerTargetStillTakesThePlainRelativeDepth].
        //
        // This used to hard-code "Bench and Front Squat are both 80-level ladders".
        // They are not any more: the sourced tops (2026-11-10) put the bench on 33
        // rungs and the front squat on 49, which is the longer-target case.
        val atTop = ProgressionEngine.branchStartLevel(
            source, ProgressionEngine.ProgressionState(sourceMax, 1), target,
        )
        if (targetMax > sourceMax) {
            assertEquals("a longer target must give the whole ladder", targetMax, atTop)
        } else {
            assertTrue(
                "a finished source landed on $atTop of $targetMax, which is the target's own cap",
                atTop < targetMax,
            )
            assertEquals((targetMax * 0.6).roundToInt(), atTop)
        }
    }

    @Test
    fun aLongerTargetStillTakesThePlainRelativeDepth() {
        // The cap above is for EQUAL-length ladders only. A longer target
        // exists precisely so the user arrives partway up with room left, and
        // capping it too would throw that away. Swept over the REAL branches,
        // because branchStartLevel only ever runs for those.
        var longerFound = 0
        ExerciseProgression.ENTRIES.forEach { source ->
            val sourceMax = ProgressionEngine.effectiveMaxLevel(source)
            source.branches.forEach { branch ->
                val target = ProgressionEngine.entryFor(branch.entry) ?: return@forEach
                val targetMax = ProgressionEngine.effectiveMaxLevel(target)
                if (targetMax <= sourceMax) return@forEach
                val mapped = ProgressionEngine.branchStartLevel(
                    source, ProgressionEngine.ProgressionState(sourceMax, 1), target,
                )
                // Relative depth 1.0 on a longer ladder lands on its top,
                // which IS the point: the swap gives a whole new ladder.
                assertEquals(
                    "a longer target was capped, so the swap would restart the user near the bottom",
                    targetMax,
                    mapped,
                )
                longerFound++
            }
        }
        assertTrue(
            "no longer-than-source branch existed in the pool, so the cap is untested",
            longerFound > 0,
        )
    }

    // --- Ceiling data sanity ---------------------------------------------------

    @Test
    fun noDumbbellCurlLadderExceedsTheResearchCeiling() {
        // The spec's guardrail: no 100 kg dumbbell curls, ever.
        val curl = entry("Dumbbell Bicep Curl")
        val ceiling = curl.ceiling!!.load!!
        assertEquals(45.0, ceiling, 0.001)
        val effMax = ProgressionEngine.effectiveMaxLevel(curl)
        for (lvl in 1..effMax) {
            val v = ProgressionEngine.valueAtLevelWithCeiling(curl, lvl)
            assertTrue(v == null || v <= ceiling + 0.001)
        }
    }

    @Test
    fun everyLoadRowCarriesABasis() {
        val ceilings = com.grafizum.repio.data.ExerciseProgression.ENTRIES.mapNotNull { it.ceiling }
        assertTrue(ceilings.isNotEmpty())
        for (c in ceilings) {
            assertTrue("ceiling basis must name its source: ${c.basis}", c.basis.isNotBlank())
        }
    }
}
