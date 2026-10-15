package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "I want each exercise to have their cealing to be humane and nothing over
 * humanly possible" — owner, 2026-10-04.
 *
 * This is the one property in the level system that is a SAFETY property rather
 * than a design one, so it is asserted over the real pool instead of over a
 * couple of hand-picked rows.
 *
 * WHAT WAS WRONG, and it was not subtle once measured:
 *
 *  - `WorkoutViewModel.progressionMaxLevel` returned
 *    `maxOf(maxLevel, EXTENSION_CHUNK = 50, extensionLevelFor)`, so EVERY
 *    exercise reported at least 50 levels regardless of its own end.
 *  - The declared `maxLevel` is the CSV ladder's LENGTH — measured across the
 *    shipped pool as 6, 30, 4, 15, 40, 20, 10 — and bears no relation to when an
 *    exercise stops progressing. It is an artefact of how much data the row
 *    happened to ship.
 *  - `extendLevelList` grew that grid to `PICKER_LEVEL_LIMIT` (500) with the
 *    comment "with extrapolated ladders every level is genuinely selectable, so
 *    the added rows are real prescriptions". They were real AND past anything a
 *    human lifts: `valueAtLevel` extrapolates by the row's own increment forever.
 *  - `setExerciseLevel` clamped to THAT number rather than to the ceiling, so an
 *    impossible load could be selected and logged.
 *
 * A ceiling the app PRESCRIBES past, without being asked to, is not a ceiling.
 *
 * REVISED 2026-10-13, owner ruling: the max is a MARKER, not a wall. The ladder
 * carries real prescriptions above it and the user may climb on, so this suite
 * asserts the load ceiling over the range the evidence covers (levels 1..max,
 * which is also what the app prescribes by default) and separately asserts that
 * the max rung IS the row's own ceiling - the rung the researched number lands
 * on. Above it the climb is the user's own choice and is not a safety claim.
 *
 * WHAT REPLACES IT is evidence each row already carries: 478 of the 594 rows have
 * a sourced `HumanCeiling` ("elite dumbbell curl ~45-50 kg/arm"), and
 * `effectiveMaxLevel` maps the ladder onto it. The other 116 are `variation_step`
 * rows with no load at all, and for those the end is the rep climb.
 */
class HumaneCeilingTest {

    private val ready = ExerciseProgression.ENTRIES.filter { ProgressionEngine.isProgressionReady(it) }

    @Test
    fun theSweepIsNotVacuous() {
        assertTrue("expected progression-ready rows", ready.isNotEmpty())
        assertTrue(
            "expected the pool to carry sourced ceilings, found " +
                "${ready.count { it.ceiling?.load != null }}",
            ready.count { it.ceiling?.load != null } >= 400,
        )
    }

    /**
     * THE property: across the range the evidence covers - level 1 to the row's
     * own max, which is the whole of what the app prescribes by default - the
     * load never exceeds the row's recorded human ceiling.
     */
    @Test
    fun noPrescribedLoadEverExceedsItsHumanCeiling() {
        val offenders = mutableListOf<String>()

        for (row in ready) {
            val ceiling = row.ceiling?.load ?: continue
            if (!ProgressionEngine.isProgressionReady(row)) continue
            val max = ProgressionEngine.effectiveMaxLevel(row)

            // The first rung, the last, and both ends of the middle.
            val probes = listOf(1, max / 2, max - 1, max)
            for (level in probes.filter { it >= 1 }) {
                val load = ProgressionEngine.valueAtLevelWithCeiling(row, level) ?: continue
                if (load > ceiling + 1e-6) {
                    offenders += "${row.slug} level $level -> $load > ceiling $ceiling"
                }
            }
        }

        assertTrue(
            "these prescriptions exceed a humanly possible load: " +
                offenders.take(5).joinToString("; "),
            offenders.isEmpty(),
        )
    }

    /**
     * ...and the max rung is exactly the ceiling, not an overshoot: the marker
     * sits on the researched number.
     */
    @Test
    fun theMarkedRungIsTheCeilingItself() {
        val offenders = mutableListOf<String>()
        for (row in ready) {
            val ceiling = row.ceiling?.load ?: continue
            if (ProgressionEngine.valueKindOf(row) != ProgressionEngine.ValueKind.LOAD) continue
            val max = ProgressionEngine.effectiveMaxLevel(row)
            val atMax = ProgressionEngine.valueAtLevelWithCeiling(row, max) ?: continue
            if (kotlin.math.abs(atMax - ceiling) > 1e-6) {
                offenders += "${row.slug} level $max -> $atMax, ceiling $ceiling"
            }
        }
        assertTrue(
            "the max rung must land on the researched ceiling: " +
                offenders.take(5).joinToString("; "),
            offenders.isEmpty(),
        )
    }

    /**
     * Past the max, both kinds of ladder keep climbing (owner ruling 2026-10-13:
     * the max is a mark, not a stop) - one rung above it is a different, bigger
     * number in every row, because a level that repeats the one below it is the
     * defect that put this whole rule in the engine.
     */
    @Test
    fun everyRungAboveTheMaxKeepsClimbing() {
        val flat = mutableListOf<String>()
        for (row in ready) {
            val max = ProgressionEngine.effectiveMaxLevel(row)
            val probe = max + 1
            if (ProgressionEngine.valueKindOf(row) == ProgressionEngine.ValueKind.VARIATION) continue
            if (ProgressionEngine.isRepLadder(row)) {
                if (ProgressionEngine.repsAtLevel(row, probe) <= ProgressionEngine.repsAtLevel(row, max)) {
                    flat += "${row.slug} reps at $probe do not climb"
                }
            } else {
                val above = ProgressionEngine.valueAtLevelWithCeiling(row, probe) ?: continue
                val atMax = ProgressionEngine.valueAtLevelWithCeiling(row, max) ?: continue
                if (above <= atMax) flat += "${row.slug} load at $probe is $above, not above $atMax"
            }
        }
        assertTrue("these rungs do not climb past the max: " + flat.take(5).joinToString("; "), flat.isEmpty())
    }

    /**
     * Every row's own end, not a shared floor. This is the owner's other half:
     * "make the max level of each exercise realistic", i.e. different per
     * exercise rather than 50 for everything.
     */
    @Test
    fun everyRowEndsWhereItsOwnEvidenceSaysItEnds() {
        for (row in ready) {
            val max = ProgressionEngine.effectiveMaxLevel(row)
            assertTrue(
                "slug=${row.slug} has no end at all",
                max >= 1,
            )
            // A row whose ladder reaches past its own ceiling by a wide margin is
            // still asking for impossible work, whatever its maxLevel says.
            val ceiling = row.ceiling?.load
            if (ceiling != null && ProgressionEngine.valueKindOf(row) == ProgressionEngine.ValueKind.LOAD) {
                val atEnd = ProgressionEngine.valueAtLevelWithCeiling(row, max) ?: continue
                assertTrue(
                    "slug=${row.slug} tops out at $atEnd but its ceiling is $ceiling",
                    atEnd <= ceiling + 1e-6,
                )
            }
        }
    }

    /**
     * The rep ladders have no load, so their ceiling is the rep climb - and the
     * climb must actually terminate somewhere a person could reach. Guards against
     * the regression that would re-open this: an unbounded rep climb is the same
     * defect as an unbounded load ladder.
     */
    @Test
    fun repLaddersTopOutAtAReachableRepCount() {
        val repLadders = ready.filter { ProgressionEngine.isRepLadder(it) }
        assertTrue("expected rep ladders", repLadders.isNotEmpty())

        for (row in repLadders) {
            val top = ProgressionEngine.repCeilingFor(row)
            assertTrue(
                "slug=${row.slug} climbs to $top reps, which is not a humane end",
                top in 12..100,
            )
            // The last rung must equal the ceiling, not exceed it - the ladder is
            // built from the floor to this number, so a rung past it would be an
            // exercise the ceiling is supposed to have stopped.
            val lastRung = ProgressionEngine.repsAtLevel(row, ProgressionEngine.repRungs(row))
            assertTrue(
                "slug=${row.slug} last rung asks for $lastRung reps, past its ceiling of $top",
                lastRung <= top,
            )
        }
    }
}
