package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test/**
 * Guards the level picker's list against the generated v5 data.
 *
 *  - the list runs to the requested 500 rows, not to the exercise's own max,
 *  - every rung keeps its real prescription, INCLUDING the ones past the max
 *    (owner 2026-10-13: the max is a marker, not a wall - the ladder climbs on),
 *  - the rung the max lands on is the one marked [LadderRung.isMax], and it is
 *    the only one,
 *  - a 500 jump is stored as 500: clampLevel's bound is the picker's own limit
 *    rather than the exercise's max.
 *
 * Since the human-ceiling work the picker's "program" is the ceiling-aware
 * ladder: the bench's 40 declared levels stretch to 80 so the researched
 * elite ceiling (180 kg) lands on the final rung — see MasteryAndCeilingTest.
 */
class LevelLadderTest {

    private fun entry(name: String) = ProgressionEngine.entryFor(name)
        ?: error("'$name' must exist in the generated pool for this test")

    /**
     * A progression-ready strength row with a real ladder. The pool's
     * `requiresCoachReview` rows (Back Extension, Push-Up …) are excluded by
     * design, and have no rungs at all — their own case is asserted below.
     */
    private fun bench() = entry("Barbell Bench Press").also {
        assertEquals("valid", it.validationStatus)
        assertTrue(ProgressionEngine.isProgressionReady(it))
    }

    /** The picker's program length is the ceiling-aware effective max. */
    private fun benchProgramLength() = ProgressionEngine.effectiveMaxLevel(bench())

    @Test
    fun theListRunsToTheRequested500Rows() {
        assertEquals(500, ProgressionEngine.PICKER_LEVEL_LIMIT)

        val rungs = ProgressionEngine.ladderRungs(bench())
        assertEquals(ProgressionEngine.PICKER_LEVEL_LIMIT, rungs.size)
        assertEquals(1, rungs.first().level)
        assertEquals(500, rungs.last().level)
        // Level numbers are the 1-based row position, with no gaps to skip.
        rungs.forEachIndexed { index, rung -> assertEquals(index + 1, rung.level) }
    }

    @Test
    fun everyRungOfTheProgramKeepsItsRealPrescription() {
        val be = bench()
        // The PROGRAM is the ceiling-aware ladder, not the CSV's declared max:
        // the declared number is the variation count (40) while the ladder the
        // app walks runs to the researched ceiling (58).
        val max = benchProgramLength()
        assertTrue("the tail must be non-empty for this test", max in 2..200)
        val program = ProgressionEngine.ladderRungs(be).take(max)

        assertEquals(max, program.size)
        assertTrue(
            "only the program's own last rung is the max",
            program.last().isMax && program.dropLast(1).none { it.isMax }
        )
        assertEquals(ProgressionEngine.romanStageReps(be, 1).orEmpty(), program.first().stageReps)
        program.forEach { rung ->
            assertNotNull("level ${rung.level} must keep its prescription", rung.prescribed)
            assertEquals(
                ProgressionEngine.prescribedValueAtLevel(be, rung.level),
                rung.prescribed
            )
        }
    }

    /**
     * The max is a MARKER: exactly one rung carries it, and the ladder does not
     * stop there (owner 2026-10-13: "labels the max ... but continues climbing,
     * the max is just a mark").
     */
    @Test
    fun theMaxIsMarkedOnOneRungAndTheLadderClimbsPastIt() {
        val be = bench()
        val max = benchProgramLength()
        val rungs = ProgressionEngine.ladderRungs(be)

        val marked = rungs.filter { it.isMax }
        assertEquals("exactly one rung is the max", 1, marked.size)
        assertEquals(max, marked.single().level)

        // Past the max every rung is still a real prescription that keeps rising:
        // this is what the picker shows the user, so a repeat or a null here is the
        // "level that changes nothing" defect in the rows above the max.
        val beyond = rungs.drop(max)
        assertTrue(beyond.isNotEmpty())
        assertTrue(beyond.all { it.prescribed != null })
        val loads = (max..max + 5).map { ProgressionEngine.valueAtLevelWithCeiling(be, it)!! }
        assertEquals("the rungs above the max must keep climbing", loads.sorted(), loads)
        loads.zipWithNext { lower, upper ->
            assertTrue("level above $max must be heavier than the one below", upper > lower)
        }
        assertNotEquals(
            ProgressionEngine.prescribedValueAtLevel(be, max)!!.value,
            ProgressionEngine.prescribedValueAtLevel(be, max + 1)!!.value
        )
    }

    /** A LEVEL jump is stored as itself all the way to the picker's own limit. */
    @Test
    fun aJumpPastTheMaxIsStoredAsTheLevelAskedFor() {
        val be = bench()
        val max = benchProgramLength()

        assertEquals(max + 1, ProgressionEngine.clampLevel(be, max + 1))
        assertEquals(ProgressionEngine.PICKER_LEVEL_LIMIT, ProgressionEngine.clampLevel(be, 500))
        assertEquals(1, ProgressionEngine.clampLevel(be, 0))
    }

    /**
     * Regression, and its SUPERSESSION: the generated rows declare a max of 30-40
     * while shipping only ~10 explicit values, and the engine used to CLAMP past
     * them, so every level from 11 up read the same load and the picker showed
     * "level 11 to level 19, same weight". The ladder then continued from the
     * last explicit value by the row's increment.
     *
     * It now walks ONE step from level 1 instead (owner 2026-10-13: *"And
     * progressive increase makes absolute no sense, the increase of the weight
     * should always be the same (add more levels since the max will be the same
     * but the steps in between will be more)"*), so the authored sequence is no
     * longer a path the ladder passes through: the bench's 60 kg now sits on
     * level 17 rather than level 10, and every rung is exactly one 2.5 kg step
     * above the one below it. What this test exists for is unchanged — no rung
     * may repeat the load below it, and no rung may go flat.
     */
    @Test
    fun everyRungIsExactlyOneStepAboveTheOneBelowIt() {
        val be = bench()
        val step = ProgressionEngine.constantStepLadder(be)!!.second
        assertEquals("the bench walks its own 2.5 kg", 2.5, step, 1e-9)

        // The declared max is still a rung the ladder reaches, and it is heavier
        // than every authored load rather than a repeat of one.
        val top = ProgressionEngine.valueAtLevel(be, ProgressionEngine.maxLevel(be))!!
        assertTrue("the top rung must be heavier than the last explicit value", top > be.levelValues.last())

        // No flat tail, and no surprise step: EVERY rung is one step above its
        // neighbour, all the way to the ceiling (not just past the seam).
        val effMax = ProgressionEngine.effectiveMaxLevel(be)
        for (level in 2..effMax) {
            assertEquals(
                "bench level $level",
                step,
                ProgressionEngine.valueAtLevel(be, level)!! - ProgressionEngine.valueAtLevel(be, level - 1)!!,
                1e-9
            )
        }
    }

    @Test
    fun aShorterListLengthNeverCutsOffTheProgram() {
        val be = bench()
        val max = benchProgramLength()

        // A floor, not a cap: asking for 3 rows must still yield the whole
        // program rather than truncating it.
        assertEquals(max, ProgressionEngine.ladderRungs(be, listLength = 3).size)
    }

    /**
     * A rep ladder keeps climbing above its own top rung too, and the picker
     * shows the bigger number rather than repeating the ceiling.
     */
    @Test
    fun aRepLadderKeepsClimbingPastItsTopRung() {
        val crunches = entry("Crunches")
        assertTrue(ProgressionEngine.isRepLadder(crunches))
        val rungs = ProgressionEngine.repRungs(crunches)
        val top = ProgressionEngine.repsAtLevel(crunches, rungs)
        // Where the top rung lands is repCeilingFor's own business (the rungs
        // step, so it need not land on the ceiling exactly - MasteryAndCeilingTest
        // pins that); what this case is about is that it is not the end.
        assertTrue(top >= ProgressionEngine.repCeilingFor(crunches) - ProgressionEngine.repStepFor(crunches))
        assertTrue(
            "a rung above the max asks for more reps",
            ProgressionEngine.repsAtLevel(crunches, rungs + 1) > top
        )
        // The row the picker draws agrees with the engine's own climb.
        val rungAbove = ProgressionEngine.ladderRungs(crunches)[rungs]
        assertEquals(rungs + 1, rungAbove.level)
        assertEquals(
            ProgressionEngine.repsAtLevel(crunches, rungs + 1),
            rungAbove.stageReps.max()
        )
    }

    @Test
    fun entriesOutsideTheLevelSystemListNothing() {
        // The pool used to carry seven rows outside the system and this test
        // sampled one of them. All seven were fixed on 2026-10-06 ("give me a
        // number as to how many and then fix them all"), so:
        //  1. NOTHING may be outside any more - the fix, pinned;
        //  2. the engine's defensive half is still proved on a synthetic row,
        //     because the property "outside lists nothing" must hold even
        //     though no real row exercises it.
        val outside = ExerciseProgression.ENTRIES.filter { !ProgressionEngine.isProgressionReady(it) }
        assertEquals("rows outside the level system (the seven were fixed 2026-10-06)", 0, outside.size)

        val synthetic = ExerciseProgression.ENTRIES.first()
            .copy(validationStatus = "manual_review_required")
        assertFalse(ProgressionEngine.isProgressionReady(synthetic))
        assertTrue(ProgressionEngine.ladderRungs(synthetic).isEmpty())
    }

    /**
     * The coach-reviewed bodyweight family (Push-Up …) used to be excluded
     * from the level system entirely — the owner's "why do pushups have no
     * levels?". It now lists real rungs: its own ladder from level 1, with
     * the variation name as the prescription.
     */
    @Test
    fun coachReviewedRowsListTheirOwnLadder() {
        val pushup = entry("Push-Up")
        assertTrue(pushup.requiresCoachReview)
        assertTrue(ProgressionEngine.isProgressionReady(pushup))

        val rungs = ProgressionEngine.ladderRungs(pushup)
        assertEquals(ProgressionEngine.PICKER_LEVEL_LIMIT, rungs.size)
        val program = rungs.take(ProgressionEngine.effectiveMaxLevel(pushup))
        assertTrue(program.last().isMax && program.dropLast(1).none { it.isMax })
        // A coach-reviewed bodyweight row is a REP LADDER, so every rung of its
        // program prescribes REPS and none of them names a movement. It used to
        // open on "Incline Push-Up" - the level was a rung of the variation
        // ladder, which is the thing the owner removed on 2026-10-03.
        assertEquals(
            ProgressionEngine.repFloorOf(pushup),
            program.first().stageReps.max()
        )
        assertTrue(program.none { it.prescribed?.variation != null })
    }
}
