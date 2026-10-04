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
    fun benchLadderIsStretchedToTheEliteCeiling() {
        val e = entry("Barbell Bench Press")
        val effMax = ProgressionEngine.effectiveMaxLevel(e)
        assertTrue("bench effective max ($effMax) must exceed the declared ladder", effMax > 40)
        // The ceiling load lands exactly on the effective max.
        assertEquals(e.ceiling!!.load!!, ProgressionEngine.valueAtLevelWithCeiling(e, effMax)!!, 0.001)
        // ... and never anywhere else.
        for (lvl in 1..effMax) {
            assertTrue(ProgressionEngine.valueAtLevelWithCeiling(e, lvl)!! <= e.ceiling!!.load!!)
        }
        // Below the explicit ladder's own top, the CSV values are unchanged
        // (the bench ships 10 exact rungs: 20 -> 60 kg).
        assertEquals(20.0, ProgressionEngine.valueAtLevelWithCeiling(e, 1)!!, 0.001)
        assertEquals(60.0, ProgressionEngine.valueAtLevelWithCeiling(e, 10)!!, 0.001)
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

    @Test
    fun levelUpReturnsNullPastTheEffectiveMax() {
        val e = entry("Barbell Bench Press")
        val effMax = ProgressionEngine.effectiveMaxLevel(e)
        val top = ProgressionEngine.ProgressionState(effMax, ProgressionEngine.patternLength(e))
        assertNull(ProgressionEngine.levelUp(e, top))
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
        // Clamped at the TOP - but not at the target's own maximum. Bench and
        // Front Squat are both 80-level ladders, and the plain relative-depth
        // mapping returns targetMax for a finished exercise, which would hand
        // over a ladder that is already complete: the swap would be over
        // before it began and the evolution popup would draw two identical
        // bars. An equal-length target has no more headroom to give, so it is
        // capped partway down instead. EvolutionOfferTest sweeps every branch
        // for this; this case pins the numbers.
        val atTop = ProgressionEngine.branchStartLevel(
            source, ProgressionEngine.ProgressionState(sourceMax, 1), target,
        )
        assertTrue(
            "a finished source landed on $atTop of $targetMax, which is the target's own cap",
            atTop < targetMax,
        )
        assertEquals((targetMax * 0.6).roundToInt(), atTop)
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
