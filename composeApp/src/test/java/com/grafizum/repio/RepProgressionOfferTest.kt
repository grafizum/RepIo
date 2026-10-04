package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WHEN a rep-only exercise is offered a harder movement, and what declining
 * that offer means.
 *
 * Owner 2026-10-03: "when a rep level is reached show a progression option,
 * progressions dont have to only show at max level could also show at the usual
 * level that is recommended base on online analytics and then if the user
 * chooses no, the exercise could go on with the level higher, lets say max is
 * 25 levels with reps 25, if the user then chooses the progression then change
 * the exercise, if he doesnt simply keep incrementing to maybe 50 and then a
 * show progression again".
 *
 * Three things are pinned here, and the third is the one that would be easiest
 * to break silently:
 *
 *  - the checkpoints are 12 / 25 / 50 reps, chosen from the literature rather
 *    than picked (see `REP_CHECKPOINTS`);
 *  - declining silences ONE checkpoint, not the exercise - the whole design is
 *    "not yet", and a boolean decline cannot express that;
 *  - EVERY rep row in the pool reaches its first checkpoint, so no bodyweight
 *    exercise is left with a ladder that never offers anything.
 *
 * The whole rule is one pure engine function (`isProgressionPending`) precisely
 * so it can be swept across the pool here instead of being asserted only for
 * Crunches.
 */
class RepProgressionOfferTest {

    private val crunches = ProgressionEngine.entryFor("Crunches")!!

    private fun stateAt(level: Int) = ProgressionEngine.ProgressionState(level, 1)

    @Test
    fun theCheckpointsAreTheResearchedRepCounts() {
        // 8-12 is the supported hypertrophy range (Schoenfeld et al. 2021), so 12
        // is where more reps stop being the better lever; 15-25 is ACSM's local
        // muscular endurance band and 50 is the ceiling for work with no load to
        // raise. Hard-coded here so a "just nudge this" edit has to be argued.
        assertEquals(listOf(12, 25, 50), ProgressionEngine.REP_CHECKPOINTS)
        assertEquals(50, ProgressionEngine.CORE_REP_CEILING)
        assertEquals(25, ProgressionEngine.STANDARD_REP_CEILING)
    }

    /**
     * The step is per-exercise and is NEVER one rep.
     *
     * Owner 2026-10-03: "make sure for the rep count being the level, that level
     * 1 doesnt start at rep 1 since 1 rep is basically nothing, and then
     * instead of 1 rep steps per level, adapt it to be 2 or 4 or whatever,
     * adapt it for each exercise of that kind".
     *
     * Two traps this pins, both of which were live:
     *  - a floor of 1 rep. Every rep row declares repMin = 8 and a row that
     *    declares none falls back to 8, so level 1 is never a single rep.
     *  - a step of 1 rep, which integer division produced. The step is the
     *    span divided by the rung count ROUNDED, and every ladder in the pool is
     *    checked here rather than one exercise being checked twice.
     */
    @Test
    fun noRepLadderStartsAtOneRepAndEveryLadderIsWalkable() {
        var checked = 0
        for (entry in ExerciseProgression.ENTRIES) {
            if (!ProgressionEngine.isRepLadder(entry)) continue
            checked++
            assertTrue(
                "${entry.id} starts at ${ProgressionEngine.repsAtLevel(entry, 1)} reps",
                ProgressionEngine.repsAtLevel(entry, 1) >= 8
            )
            // "One rep is basically nothing" holds wherever the ladder has ROOM
            // for it. It cannot hold for a skill row whose authored ceiling is
            // 12 against a floor of 8: a two-rep step there would leave three
            // rungs for a four-rep-wide range. So the rule is asserted against
            // the span, not against a flat 2.
            val span = ProgressionEngine.repCeilingFor(entry) - ProgressionEngine.repFloorOf(entry)
            val step = ProgressionEngine.repStepFor(entry)
            if (span >= 6) {
                assertTrue("${entry.id} steps by $step rep(s) per level over a $span span", step >= 2)
            }
            assertTrue("${entry.id} steps by $step, which is not progress", step >= 1)
            // And the ladder is a usable length: a rung is what the user sees.
            // Four is the floor a four-rep span can honestly produce; twelve is
            // the ceiling this ladder is built to.
            val rungs = ProgressionEngine.repRungs(entry)
            assertTrue("${entry.id} has $rungs rungs, too coarse or too long", rungs in 4..12)
        }
        assertTrue("expected the pool's rep rows to be checked, got $checked", checked > 100)
    }

    /**
     * Core and non-core get DIFFERENT ladders, which is the whole point of
     * reading the ceiling off the row's own muscle rather than picking one
     * number for everybody.
     */
    @Test
    fun aShortLadderGetsFewerBiggerStepsThanALongOne() {
        val crunches = ProgressionEngine.entryFor("Crunches")!!
        val pushup = ProgressionEngine.entryFor("Push-Up")!!
        val planche = ProgressionEngine.entryFor("Pseudo Planche Push Ups")!!
        // Ceilings are per exercise now (RepCeilings), so this is no longer a
        // core-vs-rest rule: what has to hold is that a ladder climbing to 50
        // takes bigger steps than one climbing to 25, and that a short skill
        // ladder still advances rather than offering four identical rungs.
        assertEquals(50, ProgressionEngine.repCeilingFor(crunches))
        assertEquals(25, ProgressionEngine.repCeilingFor(pushup))
        assertEquals(12, ProgressionEngine.repCeilingFor(planche))

        // Bigger span, bigger step - the step is derived from the ceiling.
        assertTrue(ProgressionEngine.repStepFor(crunches) > ProgressionEngine.repStepFor(pushup))
        // And no ladder is ever a single rep per level.
        assertTrue(ProgressionEngine.repStepFor(planche) >= 1)
        // Every one of them stays short enough to read as a ladder.
        assertTrue(ProgressionEngine.repRungs(crunches) <= 12)
        assertTrue(ProgressionEngine.repRungs(pushup) <= 12)
        assertTrue(ProgressionEngine.repRungs(planche) <= 12)
    }

    @Test
    fun noOfferBeforeTheFirstCheckpointAndOneAtIt() {
        val firstLevel = ProgressionEngine.levelReachingReps(crunches, 12)
        assertFalse(
            "one level below the checkpoint is already owed an offer",
            ProgressionEngine.isProgressionPending(crunches, stateAt(firstLevel - 1), null)
        )
        assertTrue(
            ProgressionEngine.isProgressionPending(crunches, stateAt(firstLevel), null)
        )
    }

    @Test
    fun theOfferIsStillPendingHigherUpTheLadder() {
        // Reaching a checkpoint does not CONSUME it: the user who climbs past 12
        // without deciding keeps the offer, because 25 is the next one.
        val beyond = ProgressionEngine.levelReachingReps(crunches, 20)
        assertTrue(
            ProgressionEngine.isProgressionPending(crunches, stateAt(beyond), null)
        )
        assertEquals(
            12,
            ProgressionEngine.repCheckpointAtLevel(crunches, beyond)
        )
    }

    @Test
    fun decliningOneCheckpointSilencesItAndTheNextOneReArms() {
        val at12 = ProgressionEngine.levelReachingReps(crunches, 12)
        val at25 = ProgressionEngine.levelReachingReps(crunches, 25)
        val at50 = ProgressionEngine.levelReachingReps(crunches, 50)

        // Declined at 12: silent AT 12, which is the whole promise. The later
        // checkpoints stay armed, because the user only said "not yet" - not
        // "never".
        assertFalse(ProgressionEngine.isProgressionPending(crunches, stateAt(at12), 12))
        assertTrue(ProgressionEngine.isProgressionPending(crunches, stateAt(at25), 12))
        assertTrue(ProgressionEngine.isProgressionPending(crunches, stateAt(at50), 12))

        // Declined at 25 as well: silent from 25 until 50, then back.
        assertFalse(ProgressionEngine.isProgressionPending(crunches, stateAt(at25), 25))
        assertTrue(ProgressionEngine.isProgressionPending(crunches, stateAt(at50), 25))
    }

    @Test
    fun theTopLevelStopsOfferingBecauseThereIsNowhereLeftToClimb() {
        val top = ProgressionEngine.effectiveMaxLevel(crunches)
        val topReps = ProgressionEngine.repsAtLevel(crunches, top)
        // The rungs step by 4 from a floor of 8, so the top rung lands within
        // one step of the ceiling rather than exactly on it - which is why the
        // offer at the top is owed even though no rung reads 50.
        assertTrue("top rung asks $topReps, not within a step of 50", topReps >= 48)
        assertTrue(ProgressionEngine.isProgressionPending(crunches, stateAt(top), null))
        assertEquals(
            ProgressionEngine.CORE_REP_CEILING,
            ProgressionEngine.repCheckpointAtLevel(crunches, top)
        )
        assertNull(ProgressionEngine.nextRepCheckpoint(ProgressionEngine.CORE_REP_CEILING))
    }

    @Test
    fun theLadderCanBeClimbedPastADecline() {
        // The reason a decline is a rep count and not a cap: the level-up path
        // must keep working at the checkpoint, or "keep incrementing to maybe 50"
        // is unimplementable.
        //
        // From the TOP STAGE: levelUp walks the Roman stages first and only then
        // rolls into the next level, so a mid-pattern state would return a stage
        // bump and prove nothing about the ladder.
        val at12 = ProgressionEngine.levelReachingReps(crunches, 12)
        val topStage = ProgressionEngine.patternLength(crunches)
        val next = ProgressionEngine.levelUp(
            crunches, ProgressionEngine.ProgressionState(at12, topStage)
        )
        assertTrue("a declined checkpoint must not end the ladder", next != null)
        assertEquals(at12 + 1, next!!.newState.level)
        assertEquals(
            ProgressionEngine.repStepFor(crunches),
            ProgressionEngine.repsAtLevel(crunches, next.newState.level) -
                ProgressionEngine.repsAtLevel(crunches, at12)
        )
    }

    /**
     * EVERY rep row in the pool, not one exercise: a row that can never reach a
     * checkpoint has a ladder that ends without ever offering anything, which is
     * the failure the whole feature is supposed to remove.
     */
    @Test
    fun everyRepRowInThePoolReachesItsFirstCheckpoint() {
        var checked = 0
        for (entry in ExerciseProgression.ENTRIES) {
            if (!ProgressionEngine.isRepLadder(entry)) continue
            checked++
            val top = ProgressionEngine.effectiveMaxLevel(entry)
            val firstCheckpoint = ProgressionEngine.REP_CHECKPOINTS.first()
            assertTrue(
                "${entry.id} tops out below its first checkpoint",
                ProgressionEngine.repsAtLevel(entry, top) >= firstCheckpoint
            )
            assertTrue(
                "${entry.id} is offered nothing at its own top level",
                ProgressionEngine.isProgressionPending(entry, stateAt(top), null)
            )
            // And the ladder starts at the row's own floor, which is the bottom
            // of the supported range rather than a made-up number.
            assertTrue(
                "${entry.id} starts below the supported rep floor",
                ProgressionEngine.repsAtLevel(entry, 1) >= 8
            )
        }
        assertTrue("expected the pool's rep rows to be checked, got $checked", checked > 100)
    }

    /** A LOAD row is unaffected: its offer still fires only at the ladder's end. */
    @Test
    fun aLoadedRowStillOffersOnlyAtTheEndOfItsLadder() {
        val bench = ProgressionEngine.entryFor("Barbell Bench Press")!!
        val top = ProgressionEngine.effectiveMaxLevel(bench)
        val topStage = ProgressionEngine.patternLength(bench)
        assertFalse(
            ProgressionEngine.isProgressionPending(bench, stateAt(1), null)
        )
        // Arriving at the top is not the same as finishing it: a LOAD row is
        // offered only once the top stage is done, exactly as before.
        assertFalse(
            ProgressionEngine.isProgressionPending(bench, stateAt(top), null)
        )
        assertTrue(
            ProgressionEngine.isProgressionPending(
                bench, ProgressionEngine.ProgressionState(top, topStage), null
            )
        )
        assertNull(ProgressionEngine.repCheckpointAtLevel(bench, top))
    }
}