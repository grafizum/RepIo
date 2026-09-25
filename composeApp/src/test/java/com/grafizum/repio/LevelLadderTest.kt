package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test/**
 * Guards the level picker's list against the generated v5 data.
 *
 *  - the list runs to the requested 500 rows, not to the exercise's own max,
 *  - every rung of the exercise's program keeps its real prescription,
 *  - rungs past that program are marked and carry nothing (the engine would
 *    otherwise hand back a clamped load and the row would read as a target),
 *  - a 500 jump cannot be stored: the level passes through clampLevel, which is
 *    what keeps the overall level and its E→SS+ bands inside the data's range.
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
        val max = ProgressionEngine.maxLevel(be)
        assertTrue("the tail must be non-empty for this test", max in 2..60)
        val program = ProgressionEngine.ladderRungs(be).take(max)

        assertEquals(max, program.size)
        assertTrue("no rung of a real program may be marked", program.none { it.beyondPlan })
        assertEquals(ProgressionEngine.romanStageReps(be, 1).orEmpty(), program.first().stageReps)
        program.forEach { rung ->
            assertNotNull("level ${rung.level} must keep its prescription", rung.prescribed)
            assertEquals(
                ProgressionEngine.prescribedValueAtLevel(be, rung.level),
                rung.prescribed
            )
        }
    }

    @Test
    fun theFirstRungPastTheProgramIsTheFirstMarkedOne() {
        val be = bench()
        val max = benchProgramLength()
        val rungs = ProgressionEngine.ladderRungs(be)

        assertTrue("level $max is the program's last rung", rungs[max - 1].beyondPlan.not())
        assertTrue(rungs[max].beyondPlan)
        assertTrue("the tail must run all the way", rungs.drop(max).all { it.beyondPlan })
    }

    @Test
    fun rungsPastTheProgramCarryNothingEvenThoughTheEngineCouldAnswer() {
        val be = bench()
        val max = benchProgramLength()
        val beyond = ProgressionEngine.ladderRungs(be).drop(max)

        assertTrue(beyond.isNotEmpty())
        assertTrue(beyond.all { it.prescribed == null && it.stageReps.isEmpty() })

        // The reason the row is explicit rather than "whatever the engine says":
        // valueAtLevel clamps, so level 300 still resolves to a load. Showing it
        // would present the level-30 weight as if it were a level-300 target.
        val level300 = ProgressionEngine.ladderRungs(be)[299]
        assertNull(level300.prescribed)
        assertNotNull(ProgressionEngine.prescribedValueAtLevel(be, 300))
    }

    @Test
    fun aJumpPastTheProgramCannotBeStored() {
        val be = bench()
        val max = benchProgramLength()

        assertEquals(max, ProgressionEngine.clampLevel(be, 500))
        assertEquals(max, ProgressionEngine.clampLevel(be, max + 1))
        assertEquals(1, ProgressionEngine.clampLevel(be, 0))
    }

    /**
     * Regression: the generated rows declare a max of 30-40 while shipping
     * only ~10 explicit values, and the engine used to CLAMP past them — so
     * every level from 11 up read the same load and the picker showed "level
     * 11 to level 19, same weight". The ladder now continues from the last
     * explicit value by the row's increment: the seam stays continuous and
     * every rung to the declared max carries its own (heavier) load.
     */
    @Test
    fun levelsPastTheExplicitSequenceKeepClimbingInsteadOfRepeating() {
        val be = bench()
        val n = be.levelValues.size
        assertTrue("this exercise must have the short-sequence shape", n in 2 until ProgressionEngine.maxLevel(be))

        val last = be.levelValues.last()
        val pastSeam = ProgressionEngine.valueAtLevel(be, n + 1)!!
        assertTrue("level ${n + 1} must continue the ladder upward", pastSeam > last)

        // Consecutive extrapolated levels must differ — no flat tail.
        assertNotEquals(
            ProgressionEngine.valueAtLevel(be, 11)!!,
            ProgressionEngine.valueAtLevel(be, 12)!!
        )

        // The declared top of the ladder must be heavier than the last
        // explicit rung, not a repeat of it.
        val top = ProgressionEngine.valueAtLevel(be, ProgressionEngine.maxLevel(be))!!
        assertTrue("the top rung must be heavier than the last explicit value", top > last)

        // The seam itself is exact: level n still reads the n-th explicit value.
        assertEquals(last, ProgressionEngine.valueAtLevel(be, n)!!, 0.0)
    }

    @Test
    fun aShorterListLengthNeverCutsOffTheProgram() {
        val be = bench()
        val max = benchProgramLength()

        // A floor, not a cap: asking for 3 rows must still yield the whole
        // program rather than truncating it.
        assertEquals(max, ProgressionEngine.ladderRungs(be, listLength = 3).size)
    }

    @Test
    fun entriesOutsideTheLevelSystemListNothing() {
        val notReady = ExerciseProgression.ENTRIES.first { !ProgressionEngine.isProgressionReady(it) }
        assertTrue(ProgressionEngine.ladderRungs(notReady).isEmpty())
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
        assertTrue(program.none { it.beyondPlan })
        assertEquals(
            ProgressionEngine.prescribedValueAtLevel(pushup, 1),
            program.first().prescribed
        )
        assertEquals("Incline Push-Up", program.first().prescribed?.variation)
    }
}
