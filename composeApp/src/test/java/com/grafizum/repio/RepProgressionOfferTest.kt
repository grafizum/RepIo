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
     * EVERY LADDER STARTS ON THE RAMP, and is walkable from there.
     *
     * TWO OWNER RULINGS ON THIS, and they disagree - which is why both are recorded here, because
     * the older one is quoted in the KDoc of the rule it was about:
     *
     *  - 2026-10-03: *"make sure for the rep count being the level, that level 1 doesnt start at rep
     *    1 since 1 rep is basically nothing, and then instead of 1 rep steps per level, adapt it to
     *    be 2 or 4 or whatever"*. That is where the flat 8-rep floor and the never-one-rep STEP came
     *    from, and the step half of it still stands (see the span assertion below).
     *  - 2026-10-16: *"Rep floor: start at 1 (4 for the hard variations), climb 4 to 8, then
     *    8-12"*. This is the NEWER ruling and it replaces the floor half: every rep ladder now opens
     *    on a fixed ramp - 1 -> 4 -> 8 -> 12, or 4 -> 8 -> 12 when the movement is a hard variation
     *    whose own ceiling never leaves the hypertrophy window - and only the rungs ABOVE 12 follow
     *    the row's step. The 8-rep floor was the bottom of the hypertrophy window standing in for
     *    the bottom of a LADDER, so a beginner's push-up row began at a number a beginner may not
     *    reach.
     *
     * What the older ruling was protecting is still protected: 12 is still on every ladder, one
     * rep per step is still refused wherever the span has room for more, and the ramp's rungs are
     * written down in the engine rather than derived, so an edit to the step arithmetic cannot move
     * them.
     */
    @Test
    fun everyRepLadderStartsOnTheRampAndIsWalkable() {
        var checked = 0
        for (entry in ExerciseProgression.ENTRIES) {
            if (!ProgressionEngine.isRepLadder(entry)) continue
            checked++
            val ramp = ProgressionEngine.repRamp(entry)
            assertEquals(
                "${entry.id} does not open on the first rung of its ramp",
                ramp.first(),
                ProgressionEngine.repsAtLevel(entry, 1)
            )
            // The ramp itself, rung by rung - the numbers the owner named.
            val expected = if (ProgressionEngine.isHardVariation(entry)) {
                listOf(4, 8, 12)
            } else {
                listOf(1, 4, 8, 12)
            }
            assertEquals("${entry.id} does not climb 4 -> 8 -> 12", expected, ramp)
            assertTrue(
                "${entry.id} starts above the ramp it is supposed to open on",
                ProgressionEngine.repsAtLevel(entry, 1) <= 4
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
            // Three rungs is a legitimate ladder now: a hard variation whose own ceiling is the top
            // of the ramp (a handstand push-up stops at 12) IS its ramp, 4 -> 8 -> 12.
            val rungs = ProgressionEngine.repRungs(entry)
            assertTrue("${entry.id} has $rungs rungs, too coarse or too long", rungs in 3..12)
        }
        assertTrue("expected the pool's rep rows to be checked, got $checked", checked > 100)
    }

    /**
     * Every ladder steps by enough to hold its OWN Roman climb.
     *
     * This replaced "a bigger span takes a bigger step", and the reason is the
     * owner's 2026-10-06 report: the stage resets to I when the level advances,
     * so the step has to cover the climb or a level-up demotes the reps. The
     * climb is a property of the PATTERN, not the span, which is why Crunches
     * and Push-Up now step alike - they carry the same pattern - and why the
     * span only decides the rest.
     */
    @Test
    fun everyLadderStepsByEnoughToHoldItsOwnRomanClimb() {
        val crunches = ProgressionEngine.entryFor("Crunches")!!
        val pushup = ProgressionEngine.entryFor("Push-Up")!!
        val planche = ProgressionEngine.entryFor("Pseudo Planche Push Ups")!!
        // Ceilings stay per exercise (RepCeilings).
        assertEquals(50, ProgressionEngine.repCeilingFor(crunches))
        assertEquals(25, ProgressionEngine.repCeilingFor(pushup))
        assertEquals(12, ProgressionEngine.repCeilingFor(planche))

        // "1 level is 5 reps": the pattern's climb of 4 plus one, for every row
        // with room for it.
        assertEquals(5, ProgressionEngine.repStepFor(crunches))
        assertEquals(5, ProgressionEngine.repStepFor(pushup))
        assertTrue(
            "a roomy ladder must step PAST its own climb",
            ProgressionEngine.repStepFor(crunches) > ProgressionEngine.stageClimb(crunches)
        )
        // The skill row's whole range is four reps wide, so it cannot hold a
        // five-rep step AND four rungs: its step shrinks and the climb is scaled
        // into the band instead (see prescribedReps).
        assertTrue(
            "a four-rep-wide ladder must step smaller than a roomy one",
            ProgressionEngine.repStepFor(planche) < ProgressionEngine.repStepFor(crunches)
        )
        assertTrue(ProgressionEngine.repStepFor(planche) >= 1)
        // Every one of them stays short enough to read as a ladder. The floor is THREE now, not
        // four: the planche's ceiling (12) is the top of the ramp itself, so its whole ladder is
        // 4 -> 8 -> 12 and every one of those rungs is a real step of four reps.
        for (entry in listOf(crunches, pushup, planche)) {
            assertTrue(
                "${entry.slug} has ${ProgressionEngine.repRungs(entry)} rungs",
                ProgressionEngine.repRungs(entry) in 3..12
            )
        }
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
            // And the ladder starts on the ramp's first rung, and climbs from there - see
            // [everyRepLadderStartsOnTheRampAndIsWalkable] for why the floor is 1 (4 on a hard
            // variation) rather than the 8 the old rule asked for.
            assertTrue(
                "${entry.id} starts above the ramp it is supposed to open on",
                ProgressionEngine.repsAtLevel(entry, 1) == ProgressionEngine.repRamp(entry).first()
            )
            assertTrue(
                "${entry.id} starts above the ramp it is supposed to open on",
                ProgressionEngine.repsAtLevel(entry, 1) in listOf(1, 4)
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