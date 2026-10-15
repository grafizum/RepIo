package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.ProgressionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt
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
        // The floor is a non-vacuity guard, not the invariant. It read 5_000
        // while the clamped rows padded their program with repeats of the
        // ceiling; ending those ladders at the rung that reaches the ceiling
        // (see noLevelOfALoadLadderRepeatsTheLoadBelowIt) removed those rungs
        // legitimately, so the real count is now ~4.4k.
        assertTrue("only $rungs rungs swept", rungs > 4_000)
    }

    /**
     * "Adapt it per exercise" — the stretch walks the increment the row itself
     * declares, so a barbell keeps its 2.5 kg jump and a dumbbell its per-hand
     * one. A row that declares none falls back to the step of its own authored
     * sequence, quantised, never to a fractional kilogram.
     */
    @Test
    fun everyRungWalksTheRowsOwnIncrementAndLandsOnALoadableGrid() {
        var checked = 0
        for (entry in loadLadders()) {
            val grid = ProgressionEngine.loadGridStep(entry)!!
            assertTrue("${entry.id} step $grid", grid >= 0.5)
            if (entry.increment != null) {
                assertEquals("${entry.id} ignored its own increment", entry.increment!!, grid, 1e-9)
            }
            // The walked step is the row's own increment, or FINER when the row's
            // authored ladder uses halves of it (a dumbbell authored 4, 5, 6, 8
            // under a declared 2 kg) - never coarser, so a constant ladder can
            // never remove a load the row used to reach.
            val (_, walked) = ProgressionEngine.constantStepLadder(entry)!!
            assertTrue(
                "${entry.id}: the ladder walks $walked, coarser than the row's own $grid",
                walked <= grid + 1e-9
            )
            // And every rung is a weight that exists: kilogram rows sit on the
            // half-kilo grid, including the researched ceiling that tops them.
            if (entry.unit.contains("kg")) {
                val top = ProgressionEngine.effectiveMaxLevel(entry)
                for (level in 1..top) {
                    val value = ProgressionEngine.valueAtLevelWithCeiling(entry, level)!!
                    assertEquals(
                        "${entry.id} level $level reads an off-grid $value",
                        value,
                        (value * 2).roundToInt() / 2.0,
                        1e-9
                    )
                }
            }
            checked++
        }
        // Every load ladder in the pool, not the ten whose researched ceiling sat
        // above an authored top: the constant-step rule made the whole pool walk
        // the row's own increment (see ConstantWeightStepTest).
        assertEquals("every load ladder is swept", loadLadders().size, checked)
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
     * prevent.
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

    /**
     * No load ladder repeats a load inside its own program — the CLAMP case.
     *
     * The mirror image of the stretch-zone case above and the one the owner
     * reported by name: 287 of the 338 load rows had their authored ladder cut
     * short by the researched ceiling while `effectiveMaxLevel` trusted the
     * CSV's declared max (30-40), so every level past the last loadable rung
     * repeated the ceiling — 4,684 duplicate levels in total (arnold-press
     * walked 30 levels of the same 30 kg, bent-over-row 20 levels of 32 kg).
     * `effectiveMaxLevel` now ends a clamped ladder at the rung that reaches the
     * ceiling, so this sweeps EVERY load row and requires a strictly rising load.
     */
    @Test
    fun noLevelOfALoadLadderRepeatsTheLoadBelowIt() {
        var rows = 0
        var duplicates = 0
        for (entry in loadLadders()) {
            val top = ProgressionEngine.effectiveMaxLevel(entry)
            var previous = Double.NEGATIVE_INFINITY
            for (level in 1..top) {
                val value = ProgressionEngine.valueAtLevelWithCeiling(entry, level)!!
                if (value <= previous) duplicates++
                assertEquals(
                    "${entry.id} repeats $previous at level $level",
                    true,
                    value > previous
                )
                previous = value
            }
            rows++
        }
        assertEquals("$rows load rows swept", rows, loadLadders().size)
        assertEquals("no level may repeat the load below it", 0, duplicates)
    }

    /**
     * A load ladder the level system can actually climb.
     *
     * Two rows shipped a ladder whose FIRST rung was already at (or past) the
     * researched ceiling: barbell-rear-delt-row carried a 20-60 kg ladder under a
     * 15 kg ceiling, barbell-front-raise a 20-60 one under a 20 kg ceiling. The
     * old engine hid that behind 30-40 levels all clamped to the ceiling; once
     * the program ends at the rung that reaches the ceiling it collapsed to a
     * SINGLE level, and a branch swap into the row had nothing left to climb
     * (EvolutionOfferTest caught it). The data was what was wrong — the ladder
     * must start below the ceiling — so both rows now derive theirs from their
     * own level-1 value and increment.
     */
    @Test
    fun everyLoadLaddersFirstRungSitsBelowItsCeiling() {
        val offenders = loadLadders().mapNotNull { entry ->
            val ceiling = entry.ceiling!!.load!!
            val first = entry.levelValues.firstOrNull() ?: return@mapNotNull null
            if (first >= ceiling) "${entry.slug}: first rung $first >= ceiling $ceiling" else null
        }
        assertEquals(emptyList<String>(), offenders)
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
