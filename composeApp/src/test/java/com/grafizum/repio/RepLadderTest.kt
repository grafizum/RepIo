package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the rep ladder of a body-weight / variation exercise — the owner's
 * Crunches report: "each level has to be more max reps. And when switching level
 * dont reduce the reps (since this is a body exercise with no weights). So when
 * selecting the level dont display 'dragon flag' or whatever, instead show the
 * reps."
 *
 * Three claims, and the first two are about the same defect. A bodyweight
 * exercise has no weight to add, so the REP COUNT is the whole of the level —
 * which means the level number and the reps it prescribes are the same fact
 * stated twice, and a prescription that reports one while prescribing the other
 * is a lie the user can see on the card.
 */
class RepLadderTest {

    private val crunches = ProgressionEngine.entryFor("Crunches")!!

    @Test
    fun theCrunchesLadderRisesEveryLevel() {
        val maxLevel = ProgressionEngine.effectiveMaxLevel(crunches)
        var previous = 0
        for (level in 1..maxLevel) {
            val reps = ProgressionEngine.prescribedReps(crunches, level, 1)!!
            val ceiling = reps.max()
            assertTrue(
                "Crunches level $level asks for $ceiling reps, not more than level ${level - 1}'s $previous",
                ceiling > previous
            )
            previous = ceiling
        }
    }

    /**
     * THE BUG. The prescription used to read its reps at the CSV's own
     * Medium rung (level 3) while REPORTING level 1, because a coach-reviewed
     * row is forced to start at level 1. So the routine was written with 11
     * reps and the level card then prescribed 8 — a demotion the user could
     * watch happen on the first session.
     *
     * The prescription's own level and its own reps have to agree, so this
     * asserts the identity across EVERY rep-progressed row in the pool rather
     * than one exercise.
     */
    @Test
    fun aStartingPrescriptionNeverReportsALevelItDoesNotPrescribe() {
        for (name in WorkoutTemplates.getAllExerciseNames()) {
            val entry = ProgressionEngine.entryFor(name) ?: continue
            if (!ProgressionEngine.isProgressionReady(entry)) continue
            if (!ProgressionEngine.isRepProgressed(entry)) continue
            for (difficulty in ProgressionEngine.Difficulty.entries) {
                val p = ProgressionEngine.startingPrescription(name, difficulty) ?: continue
                val atItsOwnLevel = ProgressionEngine.prescribedReps(entry, p.level, p.stage)
                    ?: continue
                assertEquals(
                    "[$name/$difficulty] reports level ${p.level} with ${p.reps} reps, " +
                        "but level ${p.level} prescribes $atItsOwnLevel",
                    atItsOwnLevel,
                    p.reps
                )
            }
        }
    }

    /** And the concrete case the owner named, so a regression cannot hide in the general case. */
    @Test
    fun crunchesStartsAtLevelOneWithLevelOnesOwnReps() {
        val p = ProgressionEngine.startingPrescription("Crunches", ProgressionEngine.Difficulty.MEDIUM)!!
        assertEquals(1, p.level)
        assertEquals(ProgressionEngine.prescribedReps(crunches, 1, 1), p.reps)
        assertEquals(8, p.reps.max())
    }

    /**
     * The level of a rep ladder is a REP COUNT, not a rung of the variation
     * ladder. Owner ruling 2026-10-03: "changing the position of exercise i
     * want removed, instead increase the reps."
     *
     * The picker used to offer 50 levels for a 6-level ladder and label the 44
     * duplicate rows "Dragon Flag", which is the defect that prompted the
     * change: the level said nothing about the work and silently swapped the
     * movement under the user.
     */
    @Test
    fun aRepLaddersLevelsAreRepCountsAndNameNoMovement() {
        val maxLevel = ProgressionEngine.effectiveMaxLevel(crunches)
        assertEquals(
            ProgressionEngine.repRungs(crunches),
            maxLevel
        )
        for (level in 1..maxLevel) {
            assertNull(
                "level $level still prescribes a movement on a rep ladder",
                ProgressionEngine.prescribedValueAtLevel(crunches, level)
            )
        }
    }

    /**
     * Each level adds exactly the row's STEP, from the row's own floor, with
     * nothing skipped and nothing repeated.
     *
     * The step is per-exercise and is never 1 - see
     * [RepProgressionOfferTest.noRepLadderStartsAtOneRepAndEveryLadderIsWalkable],
     * which sweeps every row in the pool. This pins the arithmetic for one.
     *
     * The one thing that is NOT a plain step is the LAST rung, and it has to be
     * spelled out rather than waved through: a span that does not divide evenly
     * by its step reaches the top by rounding the rung count UP, so the final
     * rung overshoots and is held at the ceiling. Without the clamp that last
     * rung would ask for more reps than the exercise has - and with the old
     * floor-division rung count the ladder instead stopped one rep SHORT of the
     * ceiling it was built to reach.
     */
    @Test
    fun everyLevelOfARepLadderAddsExactlyOneStep() {
        val floor = ProgressionEngine.repFloorOf(crunches)
        val top = ProgressionEngine.repCeilingFor(crunches)
        val step = ProgressionEngine.repStepFor(crunches)
        assertTrue("a rep ladder must not step by a single rep", step >= 2)
        val maxLevel = ProgressionEngine.effectiveMaxLevel(crunches)
        val rungs = ProgressionEngine.repRungs(crunches)
        for (level in 1..maxLevel) {
            val expected = (floor + step * (level - 1)).coerceIn(floor, top)
            assertEquals(
                "level $level should ask $expected reps",
                expected,
                ProgressionEngine.repsAtLevel(crunches, level)
            )
            assertEquals(
                "level $level does not prescribe its own rep count",
                expected,
                ProgressionEngine.prescribedReps(crunches, level, 1)!!.first()
            )
            // Every rung but the last is an exact step - the clamp belongs to
            // the top of the ladder and nowhere else.
            if (level < rungs) {
                assertEquals(
                    "level $level of $rungs should be an unclamped step",
                    floor + step * (level - 1),
                    ProgressionEngine.repsAtLevel(crunches, level)
                )
            }
        }
        // And the last rung IS the ceiling - reachable, not one rep below it.
        assertEquals(
            "the ladder stops short of its own ceiling",
            top,
            ProgressionEngine.repsAtLevel(crunches, rungs)
        )
        // And level 1 is never a single rep.
        assertTrue(ProgressionEngine.repsAtLevel(crunches, 1) >= 8)
    }

    /** Every rep-progressed row's reps rise monotonically, not just Crunches. */
    @Test
    fun everyRepProgressedRowRisesMonotonically() {
        var checked = 0
        for (name in WorkoutTemplates.getAllExerciseNames()) {
            val entry = ProgressionEngine.entryFor(name) ?: continue
            if (!ProgressionEngine.isProgressionReady(entry)) continue
            if (!ProgressionEngine.isRepProgressed(entry)) continue
            val maxLevel = ProgressionEngine.effectiveMaxLevel(entry)
            var previous = 0
            for (level in 1..maxLevel) {
                val reps = ProgressionEngine.prescribedReps(entry, level, 1) ?: continue
                assertTrue(
                    "[$name] level $level ceiling ${reps.max()} is not above level ${level - 1}'s $previous",
                    reps.max() >= previous
                )
                previous = reps.max()
            }
            checked++
        }
        assertTrue("expected rep-progressed rows to check", checked > 50)
    }
}
