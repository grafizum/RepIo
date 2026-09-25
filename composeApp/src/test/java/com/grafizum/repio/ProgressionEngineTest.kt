package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the v5 level-up system end to end, against the generated data:
 *
 *  - starting prescriptions (Easy/Medium/Hard) match the CSV's values,
 *  - stages advance one pattern row at a time with the load unchanged,
 *  - a completed top stage levels up: new load, reps reset per the CSV,
 *  - large jumps reset to the post-jump reps,
 *  - exercises flagged for coach review or failed validation never progress,
 *  - the UI never sees a mismatched stage/reps pair (data-error fallback).
 */
class ProgressionEngineTest {

    private fun entry(name: String) = ProgressionEngine.entryFor(name)
        ?: error("'$name' must exist in the generated pool for this test")

    // --- Starting prescriptions ---------------------------------------------

    @Test
    fun barbellBenchPressEasyStartsAtTheCsvValue() {
        val p = ProgressionEngine.startingPrescription("Barbell Bench Press", ProgressionEngine.Difficulty.EASY)
        assertNotNull(p)
        // v5 CSV: easy_main_level 1, easy_start_value 20 kg_total_barbell, [8,8,8]
        assertEquals(1, p!!.level)
        assertEquals(20.0, p.value!!, 0.001)
        assertEquals("kg_total_barbell", p.unit)
        assertEquals(listOf(8, 8, 8), p.reps)
        assertEquals(1, p.stage)
        assertEquals(3, p.setCount)
    }

    @Test
    fun barbellBenchPressHardStartsAtLevel7With40Kg() {
        val p = ProgressionEngine.startingPrescription("Barbell Bench Press", ProgressionEngine.Difficulty.HARD)
        assertNotNull(p)
        assertEquals(7, p!!.level)
        assertEquals(40.0, p.value!!, 0.001)
        assertEquals(listOf(8, 8, 8), p.reps)
    }

    @Test
    fun dumbbellBicepCurlMediumStartsAtTheCsvStackValue() {
        val name = "Dumbbell Bicep Curl"
        val p = ProgressionEngine.startingPrescription(name, ProgressionEngine.Difficulty.MEDIUM)
        assertNotNull("dumbbell curls must have a prescription", p)
        // Medium starts at the CSV's medium_main_level with the matching load.
        val entry = entry(name)
        assertEquals(entry.mediumValue!!, p!!.value!!, 0.001)
        assertEquals(4, p.level)
    }

    @Test
    fun machineStackExercisePrescribesStackSteps() {
        val p = ProgressionEngine.startingPrescription("Machine Assisted Dips", ProgressionEngine.Difficulty.EASY)
        assertNotNull(p)
        assertEquals("stack_step", p!!.unit)
        assertEquals(3.0, p.value!!, 0.001)
    }

    @Test
    fun airBikePrescribesDurationNotLoad() {
        // Cardio/duration exercise: the value column carries seconds.
        val easy = ProgressionEngine.startingSeconds("Air Bike", ProgressionEngine.Difficulty.EASY)
        val medium = ProgressionEngine.startingSeconds("Air Bike", ProgressionEngine.Difficulty.MEDIUM)
        val hard = ProgressionEngine.startingSeconds("Air Bike", ProgressionEngine.Difficulty.HARD)
        // v5 CSV: 30 / 90 / 240 seconds.
        assertEquals(30, easy)
        assertEquals(90, medium)
        assertEquals(240, hard)
    }

    // --- Roman-stage rep progression ------------------------------------------

    @Test
    fun stageOneIsEightEightEightAndAdvancesOneSetAtATime() {
        val entry = entry("Barbell Bench Press")
        assertEquals(listOf(8, 8, 8), ProgressionEngine.romanStageReps(entry, 1))
        assertEquals(listOf(9, 8, 8), ProgressionEngine.romanStageReps(entry, 2))
        assertEquals(listOf(9, 9, 8), ProgressionEngine.romanStageReps(entry, 3))
        assertEquals(listOf(12, 12, 12), ProgressionEngine.romanStageReps(entry, 13))
    }

    @Test
    fun stageProgressKeepsTheLoadAndAdvancesTheStage() {
        val entry = entry("Barbell Bench Press")
        val state = ProgressionEngine.ProgressionState(level = 1, stage = 5)
        val next = ProgressionEngine.levelUp(entry, state)
        assertNotNull(next)
        assertFalse(next!!.leveledUp)
        assertEquals(6, next.newState.stage)
        assertEquals(1, next.newState.level)
        assertEquals(20.0, next.value!!, 0.001) // level 1's load
        assertEquals(listOf(10, 10, 9), next.reps)
    }

    @Test
    fun completingTheTopStageLevelsUpAndResetsReps() {
        val entry = entry("Barbell Bench Press")
        val state = ProgressionEngine.ProgressionState(level = 1, stage = 13)
        val next = ProgressionEngine.levelUp(entry, state)
        assertNotNull(next)
        assertTrue(next!!.leveledUp)
        assertEquals(2, next.newState.level)
        assertEquals(1, next.newState.stage)
        // Level 2 of the bench ladder: 22.5 kg, back to stage I [8,8,8].
        assertEquals(22.5, next.value!!, 0.001)
        assertEquals(listOf(8, 8, 8), next.reps)
    }

    @Test
    fun largeJumpsUseThePostJumpReps() {
        // Machine Assisted Dips declares rep_reset_policy large_jump_reset.
        val entry = entry("Machine Assisted Dips")
        // Jump from level 1 (3 steps) to level 2 (4 steps) is a large jump (~33%).
        val state = ProgressionEngine.ProgressionState(level = 1, stage = 13)
        val next = ProgressionEngine.levelUp(entry, state)
        assertNotNull(next)
        assertTrue(next!!.leveledUp)
        assertEquals(2, next.newState.level)
        // The jump class is derived from the ladder's own values.
        assertEquals("large", ProgressionEngine.jumpClassForTransition(entry, 1))
        // Large jump: reset to the post-jump reps (stage I: [8,8,8]).
        assertEquals(listOf(8, 8, 8), next.reps)
    }

    @Test
    fun theLadderEndsAtItsTopLevel() {
        val entry = entry("Barbell Bench Press")
        // The ceiling-aware ladder ends at the effective max: past it the
        // exercise is mastered and levelUp returns null (mastery takes over).
        val top = ProgressionEngine.ProgressionState(
            level = ProgressionEngine.effectiveMaxLevel(entry), stage = 13,
        )
        assertNull("the top of the ladder must not wrap around", ProgressionEngine.levelUp(entry, top))
    }

    // --- Data-error contract ---------------------------------------------------

    @Test
    fun coachReviewRowsParticipateButStartAtLevelOne() {
        // The v5 CSV flags the push-up variation ladder for coach review — its
        // note says only that the ladder ORDERING is heuristic. The owner asked
        // why push-ups had no levels at all, so these rows now participate;
        // only the CSV's guessed Easy/Medium/Hard start rungs are distrusted:
        // every difficulty starts at Level 1 (the easiest variation).
        val entry = entry("Push-Up")
        assertTrue(entry.requiresCoachReview)
        assertTrue(ProgressionEngine.isProgressionReady(entry))
        assertTrue(ProgressionEngine.isCoachReviewed(entry))

        for (difficulty in ProgressionEngine.Difficulty.entries) {
            val rx = ProgressionEngine.startingPrescription("Push-Up", difficulty)
            assertNotNull(rx)
            assertEquals("a coach-reviewed row must start at level 1", 1, rx!!.level)
        }

        // And the ladder itself climbs like any other exercise's.
        val up = ProgressionEngine.levelUp(entry, ProgressionEngine.ProgressionState(1, ProgressionEngine.patternLength(entry)))
        assertNotNull(up)
        assertTrue(up!!.leveledUp)
        assertEquals(2, up.newState.level)
    }

    @Test
    fun stageNameIsRomanButOnlyForTheEngine() {
        // The engine speaks Roman internally; the UI surfaces it only in the
        // developer skin through WorkoutViewModel.progressionLabel.
        assertEquals("I", ProgressionEngine.stageName(1))
        assertEquals("XIII", ProgressionEngine.stageName(13))
        assertEquals("", ProgressionEngine.stageName(14))
    }

    @Test
    fun stateSerializationRoundTrips() {
        val state = ProgressionEngine.ProgressionState(4, 7)
        assertEquals(state, ProgressionEngine.deserialize(ProgressionEngine.serialize(state)))
        assertNull(ProgressionEngine.deserialize(null))
        assertNull(ProgressionEngine.deserialize(""))
        assertNull(ProgressionEngine.deserialize("garbage"))
        assertNull(ProgressionEngine.deserialize("0,1")) // levels are 1-based
    }

    // --- Beginner-build eligibility ---------------------------------------------

    @Test
    fun beginnerBuildEligibilityFollowsTheCsvFlags() {
        // Eligibility reads the CSV's own beginnerBuildEligible flag, which is
        // independent of the level gate: the push-up stays out of beginner
        // builds (its variation ladder ordering is unreviewed) even though it
        // now carries levels.
        assertTrue(WorkoutTemplates.isBeginnerBuildEligible("Barbell Bench Press"))
        assertFalse(WorkoutTemplates.isBeginnerBuildEligible("Push-Up"))
        // Custom exercises keep the old behavior (eligible).
        assertTrue(WorkoutTemplates.isBeginnerBuildEligible("My Custom Exercise"))
    }

    @Test
    fun clampLevelRestrictsToTheLadderRange() {
        val be = entry("Back Extension")
        assertEquals(1, ProgressionEngine.minLevel(be))
        assertEquals(6, ProgressionEngine.maxLevel(be))
        assertEquals(1, ProgressionEngine.clampLevel(be, -3))
        assertEquals(1, ProgressionEngine.clampLevel(be, 0))
        assertEquals(4, ProgressionEngine.clampLevel(be, 4))
        assertEquals(6, ProgressionEngine.clampLevel(be, 9))
    }

    // --- Display helpers (the My-Workout UI path) ------------------------------

    @Test
    fun prescribedValueClassifiesLoadsVariationsAndDurations() {
        // Load (barbell, total kg).
        val bench = entry("Barbell Bench Press")
        val benchRx = ProgressionEngine.prescribedValueAtLevel(bench, 1)!!
        assertEquals(ProgressionEngine.ValueKind.LOAD, benchRx.kind)
        assertEquals(ProgressionEngine.LoadUnit.KG, benchRx.loadUnit)
        assertEquals(20.0, benchRx.value!!, 0.001)

        // Load (per dumbbell).
        val curl = entry("Dumbbell Bicep Curl")
        val curlRx = ProgressionEngine.prescribedValueAtLevel(curl, 1)!!
        assertEquals(ProgressionEngine.LoadUnit.KG_EACH, curlRx.loadUnit)

        // Equipment steps (machine stack).
        val dips = entry("Machine Assisted Dips")
        val dipsRx = ProgressionEngine.prescribedValueAtLevel(dips, 1)!!
        assertEquals(ProgressionEngine.LoadUnit.STACK_STEP, dipsRx.loadUnit)
        assertEquals(3.0, dipsRx.value!!, 0.001)

        // Variation ladder.
        val pushup = entry("Push-Up")
        val pushupRx = ProgressionEngine.prescribedValueAtLevel(pushup, 1)!!
        assertEquals(ProgressionEngine.ValueKind.VARIATION, pushupRx.kind)
        assertEquals("Incline Push-Up", pushupRx.variation)

        // Duration (cardio).
        val bike = entry("Air Bike")
        val bikeRx = ProgressionEngine.prescribedValueAtLevel(bike, 1)!!
        assertEquals(ProgressionEngine.ValueKind.DURATION, bikeRx.kind)
        assertEquals(30.0, bikeRx.value!!, 0.001)
    }

    @Test
    fun variationAtLevelReadsTheLadderAndRejectsOutOfRangeLevels() {
        val be = entry("Back Extension")
        assertEquals("Back Extension", ProgressionEngine.variationAtLevel(be, 1))
        assertEquals("Superman", ProgressionEngine.variationAtLevel(be, 3))
        // Past the named ladder the hardest variation repeats (max_level 6 > 3 names).
        assertEquals("Superman", ProgressionEngine.variationAtLevel(be, 6))
        assertNull(ProgressionEngine.variationAtLevel(be, 0))
    }

    @Test
    fun prescriptionChangesDetectsWhetherALevelUpAltersTheValue() {
        val be = entry("Back Extension")
        // Ladder levels 1→2→3 all change the variation.
        assertTrue(ProgressionEngine.prescriptionChangesAt(be, 1, 2))
        // The top of the ladder repeats the last variation.
        assertFalse(ProgressionEngine.prescriptionChangesAt(be, 3, 4))
        // A 1-step load ladder always changes.
        val bench = entry("Barbell Bench Press")
        assertTrue(ProgressionEngine.prescriptionChangesAt(bench, 1, 2))
    }

    @Test
    fun everyGeneratedEntryCarriesAConsistentRomanTable() {
        // The data-error contract as a data property: no generated row may pair
        // a stage with reps that disagree with its own pattern table — the
        // pipeline validated it at import, this pins it from the app side.
        ExerciseProgression.ENTRIES.forEach { entry ->
            val pattern = entry.romanStages
            val nonEmpty = pattern.filter { it.isNotEmpty() }
            // All defined stages must have the same set count (the pattern's width).
            if (nonEmpty.isNotEmpty()) {
                val width = nonEmpty.first().size
                assertTrue(
                    "${entry.id}: ragged Roman pattern",
                    nonEmpty.all { it.size == width }
                )
                // Stage I must start at rep_min (the pattern's floor).
                val stage1 = pattern.firstOrNull()
                if (stage1 != null && stage1.isNotEmpty() && entry.repMin != null) {
                    assertEquals("${entry.id}: stage I below rep_min", entry.repMin, stage1.minOrNull())
                }
                // Guided mode is 3 sets universally: every defined stage row
                // must be exactly 3 targets wide.
                if (nonEmpty.isNotEmpty()) {
                    assertEquals("${entry.id}: stage row is not 3 sets wide", 3, width)
                }
            }
        }
    }

    // --- Difficulty -> starting level ---------------------------------------

    /**
     * The assisted picker used to print an RPE number, which described a scale
     * the app does not use to choose a start. It now prints the rung the choice
     * sets, and what it prints is the pool's own mode — so this failing means
     * either the data moved or the printed number stopped matching it.
     */
    @Test
    fun typicalStartingLevelIsThePoolsMostCommonStart() {
        assertEquals(1, ProgressionEngine.typicalStartingLevel(ProgressionEngine.Difficulty.EASY))
        assertEquals(4, ProgressionEngine.typicalStartingLevel(ProgressionEngine.Difficulty.MEDIUM))
        assertEquals(7, ProgressionEngine.typicalStartingLevel(ProgressionEngine.Difficulty.HARD))
    }

    @Test
    fun startsNeverFallAsDifficultyRises() {
        val starts = ProgressionEngine.Difficulty.entries.map { difficulty ->
            ProgressionEngine.typicalStartingLevel(difficulty)
        }
        assertEquals(
            "EASY < MEDIUM < HARD must hold, got $starts",
            starts.sorted(),
            starts
        )
        assertTrue("Easy and Hard must differ, got $starts", starts.first() < starts.last())
    }

    /** The pill prints the modal start, so the advertised range has to contain it. */
    @Test
    fun startingLevelRangeContainsTheTypicalLevel() {
        ProgressionEngine.Difficulty.entries.forEach { difficulty ->
            val typical = ProgressionEngine.typicalStartingLevel(difficulty)
            val range = ProgressionEngine.startingLevelRange(difficulty)
            assertTrue("$difficulty: typical $typical falls outside $range", typical in range)
        }
    }

    /**
     * The picker is not decorative: each choice resolves to its own rung on an
     * exercise whose ladder has room, and the rung it reports is the one the
     * workout actually starts on (the same [ProgressionEngine.startingPrescription]
     * the session reads).
     */
    @Test
    fun eachDifficultyResolvesToItsOwnStartingLevel() {
        val name = "Barbell Back Squat"
        val easy = requireNotNull(
            ProgressionEngine.startingLevel(name, ProgressionEngine.Difficulty.EASY)
        )
        val medium = requireNotNull(
            ProgressionEngine.startingLevel(name, ProgressionEngine.Difficulty.MEDIUM)
        )
        val hard = requireNotNull(
            ProgressionEngine.startingLevel(name, ProgressionEngine.Difficulty.HARD)
        )

        assertEquals(
            ProgressionEngine.startingPrescription(name, ProgressionEngine.Difficulty.MEDIUM)?.level,
            medium
        )
        assertTrue("easy=$easy medium=$medium hard=$hard", easy < medium && medium < hard)
    }

    /** Exercises outside the level system have no rung, so the picker shows none. */
    @Test
    fun exercisesOutsideTheLevelSystemHaveNoStartingLevel() {
        assertNull(ProgressionEngine.startingLevel("Ab Wheel Rollout", ProgressionEngine.Difficulty.MEDIUM))
        assertNull(ProgressionEngine.startingLevel("", ProgressionEngine.Difficulty.HARD))
    }
}
