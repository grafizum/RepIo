package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.ProgressionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToLong

/**
 * Guards that every rung of a LOAD ladder is a weight the gym can actually set.
 *
 * The level picker read `40.45454545454545 kg each` on a dumbbell curl (owner
 * report, 2026-10-06). Rounding the DISPLAY was not enough: the number the app
 * SUGGESTED was off-grid, so the user was asked to load a weight that does not
 * exist. The stretch zone above a ladder's authored top used to divide the
 * remaining growth to the human ceiling by a rung count taken from a log ratio,
 * which produced 1.61 kg rungs on a bench and 0.45 kg ones on a dumbbell curl.
 *
 * The step is now the row's OWN increment ([ProgressionEngine.loadGridStep]).
 * These cases sweep EVERY load ladder in the pool rather than the two rows that
 * were reported, because the defect was in the engine and not in the data.
 */
class LoadGridTest {

    private fun loadLadders(): List<ProgressionEntry> = ExerciseProgression.ENTRIES.filter {
        ProgressionEngine.valueKindOf(it) == ProgressionEngine.ValueKind.LOAD &&
            it.ceiling?.load != null
    }

    /** The sweep must not pass on an empty or shrunken pool. */
    @Test
    fun thePoolStillHasLoadLaddersToCheck() {
        assertTrue("only ${loadLadders().size} load ladders", loadLadders().size >= 100)
    }

    /**
     * The invariant the owner stated: a weight is x.0 or x.5, never anything
     * else. Anything finer is a number the equipment cannot be set to.
     */
    @Test
    fun everyRungOfALoadLadderIsAWeightTheGymCanSet() {
        var rungs = 0
        for (entry in loadLadders()) {
            val top = ProgressionEngine.effectiveMaxLevel(entry)
            for (level in 1..top) {
                val value = ProgressionEngine.valueAtLevelWithCeiling(entry, level)!!
                val halves = (value * 2.0).roundToLong()
                assertEquals("${entry.id} level $level", halves / 2.0, value, 1e-9)
                rungs++
            }
        }
        assertTrue("only $rungs rungs swept", rungs > 5_000)
    }

    /**
     * "Adapt it per exercise" — the stretch walks the increment the row itself
     * declares, so a barbell keeps its 2.5 kg jump and a dumbbell its per-hand
     * one. A row that declares none falls back to the step of its own authored
     * sequence, quantised, never to a fractional kilogram.
     */
    @Test
    fun theStretchZoneWalksTheRowsOwnIncrement() {
        var checked = 0
        for (entry in loadLadders()) {
            val declared = ProgressionEngine.maxLevel(entry)
            val top = ProgressionEngine.effectiveMaxLevel(entry)
            val step = ProgressionEngine.loadGridStep(entry)!!
            assertTrue("${entry.id} step $step", step >= 0.5)
            if (entry.increment != null) {
                assertEquals("${entry.id} ignored its own increment", entry.increment!!, step, 1e-9)
            }
            if (top <= declared) continue
            val ladderTop = ProgressionEngine.valueAtLevelWithCeiling(entry, declared)!!
            for (level in declared + 1 until top) {
                val expected = ladderTop + step * (level - declared)
                assertEquals(
                    "${entry.id} level $level",
                    expected,
                    ProgressionEngine.valueAtLevelWithCeiling(entry, level)!!,
                    1e-9
                )
            }
            checked++
        }
        // Ten rows in the current pool: the big barbell compounds and the two
        // arm curls, whose researched ceilings sit above their authored tops.
        assertTrue("only $checked ladders have a stretch zone", checked >= 9)
    }

    /** The researched ceiling is the ladder's final rung, not an overshoot. */
    @Test
    fun theTopRungIsTheResearchedCeiling() {
        for (entry in loadLadders()) {
            val top = ProgressionEngine.effectiveMaxLevel(entry)
            assertEquals(
                entry.id,
                entry.ceiling!!.load!!,
                ProgressionEngine.valueAtLevelWithCeiling(entry, top)!!,
                1e-9
            )
        }
    }

    /**
     * No dead rung in the zone this rule governs.
     *
     * A derived rung count can leave a level that changes nothing, and a
     * level-up with nothing to do is the failure the whole rule exists to
     * prevent. Scoped to the stretch rows on purpose: the ceiling CLAMP is the
     * mirror image of the same defect and is still open — 287 of the 338 load
     * rows have their authored ladder cut short by the researched ceiling,
     * which repeats the top load for 4,684 levels in total (arnold-press walks
     * 30 levels of 30 kg because its ceiling is 30). That is a rung-count
     * change like this one and is reported to the owner rather than slipped in
     * here.
     */
    @Test
    fun noLevelUpInTheStretchZoneIsARungWithNothingToDo() {
        var checked = 0
        for (entry in loadLadders()) {
            val declared = ProgressionEngine.maxLevel(entry)
            val top = ProgressionEngine.effectiveMaxLevel(entry)
            if (top <= declared) continue
            var previous = Double.NEGATIVE_INFINITY
            for (level in 1..top) {
                val value = ProgressionEngine.valueAtLevelWithCeiling(entry, level)!!
                assertTrue("${entry.id} level $level repeats $previous", value > previous)
                previous = value
            }
            checked++
        }
        assertTrue("only $checked stretch zones checked", checked >= 9)
    }

    /** The picker has to be able to render the ladder it is handed. */
    @Test
    fun everyLadderStaysWithinThePickerLimit() {
        for (entry in loadLadders()) {
            val top = ProgressionEngine.effectiveMaxLevel(entry)
            assertTrue(
                "${entry.id} has $top levels",
                top <= ProgressionEngine.PICKER_LEVEL_LIMIT
            )
        }
    }

    /** The reported row, pinned by name. */
    @Test
    fun theDumbbellCurlNoLongerInventsAnEleventhOfAKilo() {
        val curl = ProgressionEngine.entryFor("Dumbbell Bicep Curl")!!
        val step = ProgressionEngine.loadGridStep(curl)!!
        assertEquals(curl.increment!!, step, 1e-9)
        val top = ProgressionEngine.effectiveMaxLevel(curl)
        assertEquals(curl.ceiling!!.load!!, ProgressionEngine.valueAtLevelWithCeiling(curl, top)!!, 1e-9)
    }
}
