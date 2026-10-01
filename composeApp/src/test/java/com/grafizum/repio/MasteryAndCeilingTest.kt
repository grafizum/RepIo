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
    fun ladderWithNoCeilingIsUnchanged() {
        // A ready variation-laddered bodyweight row: no load ceiling to
        // stretch, so the effective max is the declared one. (The headline
        // variation rows like Push-Up / Pistol Squat are coach-review-flagged
        // and never progress — their own case is asserted elsewhere.)
        val e = entry("Glute Bridge")
        assertEquals(ProgressionEngine.maxLevel(e), ProgressionEngine.effectiveMaxLevel(e))
    }

    @Test
    fun variationCapCeilingKeepsDeclaredMax() {
        // Pistol Squat's ceiling is the variation cap (its own ladder top), so
        // the effective max stays the declared one.
        val e = entry("Pistol Squat")
        assertEquals(ProgressionEngine.maxLevel(e), ProgressionEngine.effectiveMaxLevel(e))
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
        // Glute Bridge tops its 2-step variation ladder at level 6; at the top
        // stage the reason is LADDER_COMPLETE (no load ceiling was crossed).
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
        // A random isolation exercise has none.
        assertTrue(ProgressionEngine.branchesFor(entry("Preacher Curl")).isEmpty())
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
