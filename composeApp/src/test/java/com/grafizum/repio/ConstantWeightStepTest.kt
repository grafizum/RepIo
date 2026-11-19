package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.ProgressionEngine.ValueKind
import com.grafizum.repio.data.ProgressionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * The weight increase is the SAME at every rung, and the max does not move.
 *
 * OWNER REPORT, 2026-10-13 (board task_4j6j3m5f49730n5c4b0g3g6l): *"And progressive
 * increase makes absolute no sense, the increase of the weight should always be the
 * same (add more levels since the max will be the same but the steps in between will
 * be more)"*.
 *
 * He was reading the authored ladders, and 295 of the 454 load ladders grew their
 * step. The bench is the pattern he would have hit first: 20, 22.5, 25, 27.5, 30, 35,
 * 40, 45, 50, 60 - 2.5 kg four times, then 5 kg four times, then 10 kg - so a user
 * met a 2.5 kg level-up and, ten levels later, a 10 kg one nobody had asked for.
 *
 * Measured across the whole pool, that is now one rule: every LOAD row walks a single
 * step from its own starting load to its researched ceiling. The ceiling - the max -
 * is unchanged, and the number of levels it takes to reach it grows (the bench: 58
 * rungs as the CSV authored them, and now 33 at a constant 2.5 kg up to its SOURCED
 * 100 kg top - the old curated 180 kg elite mark was displaced by the sourced table).
 *
 * The last-rung case is pinned separately and honestly: the ceiling is snapped to
 * exactly, so if the throw from the base is not a whole number of steps the FINAL rung
 * is the remainder - shorter than the row's step, never longer, and never a repeat.
 */
class ConstantWeightStepTest {

    private fun loadRows(): List<ProgressionEntry> =
        ExerciseProgression.ENTRIES.filter {
            ProgressionEngine.isProgressionReady(it) &&
                ProgressionEngine.valueKindOf(it) == ValueKind.LOAD
        }

    private fun value(entry: ProgressionEntry, level: Int): Double =
        ProgressionEngine.valueAtLevelWithCeiling(entry, level)!!

    /**
     * Every load ladder, every rung: one step above the one below it.
     *
     * The top rung is excluded here and checked in
     * [theTopRungIsTheResearchedCeilingAndTheRungsBelowItAreEven] because it is
     * snapped TO the ceiling rather than to the arithmetic.
     */
    @Test
    fun everyLoadLadderWalksOneStepPerRung() {
        var checked = 0
        for (entry in loadRows()) {
            val ladder = ProgressionEngine.constantStepLadder(entry)
            assertNotNull("${entry.id} is a load row with no constant step", ladder)
            val (base, step) = ladder!!
            assertTrue("${entry.id} step $step", step > 0.0)
            // Level 1 is the row's own starting load, untouched.
            assertEquals("${entry.id} level 1", base, value(entry, 1), 1e-9)

            val top = ProgressionEngine.effectiveMaxLevel(entry)
            for (level in 2 until top) {
                assertEquals(
                    "${entry.id} level $level is not one step above ${level - 1}",
                    step,
                    value(entry, level) - value(entry, level - 1),
                    1e-9
                )
            }
            checked++
        }
        assertEquals("every load row is swept", loadRows().size, checked)
        assertTrue("the pool must still have load ladders to sweep", checked > 300)
    }

    /** The max is the same research ceiling, and the rungs below it are even. */
    @Test
    fun theTopRungIsTheResearchedCeilingAndTheRungsBelowItAreEven() {
        var rowsWithACeiling = 0
        for (entry in loadRows()) {
            val ceiling = entry.ceiling?.load ?: continue
            val (_, step) = ProgressionEngine.constantStepLadder(entry)!!
            val top = ProgressionEngine.effectiveMaxLevel(entry)

            assertEquals(
                "${entry.id}: the top rung must BE the researched ceiling",
                ceiling,
                value(entry, top),
                1e-9
            )
            val finalStep = value(entry, top) - value(entry, top - 1)
            assertTrue("${entry.id}: the top rung repeats the one below it", finalStep > 0.0)
            assertTrue(
                "${entry.id}: the final step ($finalStep) overshoots the row's own step ($step)",
                finalStep <= step + 1e-9
            )
            rowsWithACeiling++
        }
        assertEquals("every load row carries a researched ceiling", loadRows().size, rowsWithACeiling)
    }

    /**
     * The bench, as the owner would have read it: no step grows, and the max is
     * where it was while the levels it takes to get there are more than the CSV's
     * ten authored rungs.
     */
    @Test
    fun theBenchWalksItsOwnIncrementAllTheWayToItsSourcedTop() {
        val bench = ProgressionEngine.entryFor("Barbell Bench Press")!!
        val (base, step) = ProgressionEngine.constantStepLadder(bench)!!
        assertEquals(20.0, base, 1e-9)
        assertEquals("the bench's own barbell increment", 2.5, step, 1e-9)

        val top = ProgressionEngine.effectiveMaxLevel(bench)
        // The researched top for this lifter is 100 kg (advanced bench, 1.25x an 80 kg
        // bodyweight). The old 180 kg elite mark is listed as displaced in
        // docs/level-system-phase2-level-rebuild.md and comes back in a line if wanted.
        assertEquals("the sourced top is the last rung", 100.0, value(bench, top), 1e-9)
        assertEquals("(100 - 20) / 2.5 + 1 rungs", 33, top)
        // More levels for the same max, exactly as asked: the CSV authored ten
        // values and the old engine stretched those to 58 growing rungs.
        assertTrue("the ladder must be longer than the authored list", top > bench.levelValues.size)
        // And the authored 60 kg - once level 10, at the end of a 10 kg step - is
        // now rung 17 at the same 2.5 kg: the load did not move, its LEVEL did.
        assertEquals(60.0, value(bench, 17), 1e-9)
        assertEquals(17, ((60.0 - base) / step).roundToInt() + 1)
    }

    /**
     * The step is the row's OWN and never coarser than the finest step it
     * authored: a constant ladder may add rungs, never remove a load the row used
     * to reach. A dumbbell authored 4, 5, 6, 8 ... under a declared 2 kg
     * increment keeps its 1 kg step.
     */
    @Test
    fun theStepIsTheRowsOwnAndNeverCoarserThanItsAuthoredFinest() {
        var dumbbells = 0
        for (entry in loadRows()) {
            val (_, step) = ProgressionEngine.constantStepLadder(entry)!!
            val authored = entry.levelValues.filter { it > 0.0 }
            val finest = authored.zipWithNext { a, b -> b - a }.filter { it > 0.0 }.minOrNull()
            if (finest != null) {
                assertTrue(
                    "${entry.id}: constant step $step is coarser than its finest authored step $finest",
                    step <= finest + 1e-9
                )
            }
            if (entry.unit == "kg_per_dumbbell" && finest != null && finest < (entry.increment ?: 0.0)) {
                dumbbells++
            }
        }
        assertTrue("the pool must still hold sub-increment dumbbell ladders", dumbbells > 0)
    }

    /**
     * A big jump still exists, and the guide's two cards depend on both cases
     * being real: every load ladder now has ONE step, so the size of a step in
     * percent shrinks as the load grows, and a "large" jump (>=30%) survives only
     * at the bottom of a light ladder.
     */
    @Test
    fun aBigJumpSurvivesOnlyAtTheBottomOfALightLadder() {
        val big = loadRows().filter {
            ProgressionEngine.jumpClassForTransition(it, 1) in setOf("large", "very_large")
        }
        assertTrue("no load row can make a big jump any more", big.isNotEmpty())

        val bench = ProgressionEngine.entryFor("Barbell Bench Press")!!
        val benchFirst = ProgressionEngine.jumpClassForTransition(bench, 1)
        assertTrue(
            "the bench's first step must be an ordinary one, not a big jump (was $benchFirst)",
            benchFirst == "small" || benchFirst == "moderate"
        )

        // The guide's big-jump card reads the concentration curl at level 1.
        val curl = ProgressionEngine.entryFor("Concentration Curl")!!
        assertEquals("large", ProgressionEngine.jumpClassForTransition(curl, 1))
        assertEquals(2.0, value(curl, 1), 1e-9)
        assertEquals(3.0, value(curl, 2), 1e-9)
    }
}
