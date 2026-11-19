package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.ProgressionEntry
import com.grafizum.repio.data.RepCeilings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards [RepCeilings], the per-exercise rep ceiling the owner authorised
 * reading names for on 2026-10-04.
 *
 * The flat cap it replaced (25 for everything, 50 for core) was wrong at BOTH
 * ends, and both halves of that are asserted here as named anchors rather than
 * as a distribution: a glute bridge is written 30-50 everywhere and a
 * handstand push-up ends near 12, and if the table ever collapses back to one
 * number per muscle these anchors are what notices.
 */
class RepCeilingCoverageTest {

    private val repLadders: List<ProgressionEntry> =
        ExerciseProgression.ENTRIES.filter { ProgressionEngine.isRepLadder(it) }

    /** Every rep-ladder row must have an authored number, or it silently gets the flat cap. */
    @Test
    fun everyRepLadderInThePoolHasAnAuthoredCeiling() {
        assertTrue("expected rep ladders to exist", repLadders.isNotEmpty())
        val missing = repLadders
            .filter { !ProgressionEngine.hasAuthoredRepCeiling(it) }
            .map { it.slug }
            .sorted()
        assertEquals(
            "these rep ladders fall through to the flat cap: ${missing.joinToString()}",
            emptyList<String>(), missing,
        )
    }

    /** And the table must hold nothing that is not a real pool row. */
    @Test
    fun everySlugInTheTableIsARealPoolExercise() {
        val poolSlugs = ExerciseProgression.ENTRIES.map { it.slug }.toSet()
        val fiction = RepCeilings.slugs.filter { it !in poolSlugs }.sorted()
        assertEquals(
            "RepCeilings names exercises the pool does not have: ${fiction.joinToString()}",
            emptyList<String>(), fiction,
        )
    }

    /**
     * The band itself. 12 is the floor because it is the first
     * REP_CHECKPOINTS rung - a ladder that stopped below it could never offer a
     * progression, which is the feature the ladder exists for. 50 is the top
     * because that is where work with no load left to raise simply ends.
     */
    @Test
    fun everyAuthoredCeilingSitsInsideTheHonestBand() {
        // The band is a REP band (12..50 reps), so the step-counted rows are not in it: their
        // ceiling is a number of STEPS and is authored in ProgressionEngine's step ladders.
        for (row in repLadders.filter { !ProgressionEngine.countsInSteps(it) }) {
            val top = ProgressionEngine.repCeilingFor(row)
            assertTrue(
                "slug=${row.slug} has a ceiling of $top, outside 12..50",
                top in 12..50,
            )
        }
    }

    /**
     * The two ends of the correction, pinned by name. These are the whole
     * reason the table exists, so they are asserted as facts about specific
     * exercises rather than as a range the whole set happens to satisfy.
     */
    @Test
    fun theCheapestAndTheHardestMovementsNoLongerShareOneNumber() {
        val gluteBridge = ProgressionEngine.entryFor("Glute Bridge")!!
        val handstandPushUp = ProgressionEngine.entryFor("Handstand Push Ups")!!
        val pushUp = ProgressionEngine.entryFor("Push-Up")!!
        val pullUp = ProgressionEngine.entryFor("Pull-Up")!!

        // A glute bridge is 30-50 rep programming everywhere; the old flat cap
        // stopped it at 25 and called that the top of the movement.
        assertEquals(50, ProgressionEngine.repCeilingFor(gluteBridge))
        // A handstand push-up ends near 12; the old flat cap asked for 25.
        assertEquals(12, ProgressionEngine.repCeilingFor(handstandPushUp))
        // A strict push-up tops out around 25 and a strict pull-up lower still -
        // which is the whole argument for a per-exercise table.
        assertEquals(25, ProgressionEngine.repCeilingFor(pushUp))
        assertEquals(15, ProgressionEngine.repCeilingFor(pullUp))
        assertTrue(
            "the ladder must still be a ladder, not one rung",
            ProgressionEngine.repCeilingFor(handstandPushUp) <
                ProgressionEngine.repCeilingFor(pushUp),
        )
    }

    /**
     * Levers within one family still have to be ordered: a wall incline is a
     * 40 rep movement and a pseudo-planche is a skill. A blanket per-muscle
     * number would pass the two anchor cases above and fail this one.
     */
    @Test
    fun leverageIsStillOrderedWithinAFamily() {
        val wall = ProgressionEngine.entryFor("Wall Push Ups")!!
        val normal = ProgressionEngine.entryFor("Push-Up")!!
        val decline = ProgressionEngine.entryFor("Decline Push-Up")!!
        val planche = ProgressionEngine.entryFor("Pseudo Planche Push Ups")!!

        val ceilings = listOf(wall, normal, decline, planche).map {
            ProgressionEngine.repCeilingFor(it)
        }
        // STRICTLY decreasing, not merely sorted: an all-equal list is sorted
        // too, so a blanket per-muscle number would sail through a plain
        // ordering check. That is exactly how the flat cap survived this long.
        for (i in 1 until ceilings.size) {
            assertTrue(
                "push-up leverage must be strictly ordered, got $ceilings",
                ceilings[i - 1] > ceilings[i],
            )
        }
    }

    /**
     * The authored ceilings produced one class of ladder the flat cap never
     * had: a SKILL row whose whole range is four reps wide (a pseudo-planche
     * push-up, 8 to 12). The step rule is asserted against the span rather than
     * against a flat 2, because forcing two reps per level there would leave
     * three rungs for a movement nobody does twelve of anyway.
     */
    @Test
    fun everyLadderIsWalkableAndNeverStepsByNothing() {
        for (row in repLadders) {
            val span = ProgressionEngine.repCeilingFor(row) - ProgressionEngine.repFloorOf(row)
            val step = ProgressionEngine.repStepFor(row)
            assertTrue(
                "slug=${row.slug} steps by $step rep per level, which is not progress",
                step >= 1,
            )
            if (span >= 6) {
                assertTrue(
                    "slug=${row.slug} steps by $step over a $span span; one rep is nothing",
                    step >= 2,
                )
            }
            // THREE rungs is a real ladder since the owner's 2026-10-16 floor ruling: a hard
            // variation whose whole range is the ramp - 4 -> 8 -> 12 - has exactly three, and each
            // of them is a step of four reps. Four was the floor when level 1 was 8 reps and every
            // ladder therefore had to climb above the ramp to reach its ceiling.
            val rungs = ProgressionEngine.repRungs(row)
            assertTrue(
                "slug=${row.slug} climbs to $rungs rungs, too coarse or too long",
                rungs in 3..12,
            )
            // And the top rung is the ceiling itself, never a step past it.
            assertEquals(
                "slug=${row.slug} stops short of its own ceiling",
                ProgressionEngine.repCeilingFor(row),
                ProgressionEngine.repsAtLevel(row, ProgressionEngine.repRungs(row)),
            )
        }
    }
}