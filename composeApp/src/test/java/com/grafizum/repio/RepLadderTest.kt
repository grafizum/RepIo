package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
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
     * "dont display 'dragon flag' or whatever, instead show the reps."
     *
     * A variation ladder past its last rung repeats the hardest movement and
     * clamps the reps, so every row past the end reads identically. The picker
     * used to offer 50 levels for a 6-level ladder, which meant 44 duplicate
     * rows all labelled "Dragon Flag". The ladder's own end is the honest limit
     * for a row with nothing to extrapolate.
     */
    @Test
    fun aVariationLadderEndsWhereItEndsRatherThanRepeatingItsLastMovement() {
        val maxLevel = ProgressionEngine.effectiveMaxLevel(crunches)
        val seen = mutableSetOf<String>()
        for (level in 1..maxLevel) {
            val variation = ProgressionEngine.variationAtLevel(crunches, level)!!
            assertTrue(
                "level $level repeats '$variation', which already appeared at a lower level",
                seen.add(variation)
            )
        }
        assertTrue(
            "expected the ladder to be longer than its single movement",
            seen.size > 1
        )
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
