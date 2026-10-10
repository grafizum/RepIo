package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
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
        // The SHARED duration ladder (owner ruling 2026-10-04): every
        // seconds-based row snaps to 15/30/45/60/90/120, and Air Bike is a
        // REPEATED interval rather than a hold, so its own 180-600s rungs are
        // kept on top of that floor rather than truncated away.
        //   rungs = 15, 30, 45, 60, 90, 120, 180, 240, 300, 420, 600
        // Easy is the floor rung, Medium the 30s dose, Hard the top.
        assertEquals(15, easy)
        assertEquals(30, medium)
        assertEquals(600, hard)
    }

    /**
     * THE SHARED DURATION LADDER, and the invariant behind the 2026-10-04
     * normalization pass: the same level means the same number of seconds on
     * every hold row. Before it, 147 rows already had a seconds ladder and
     * they disagreed with each other - a Plank climbed 30/45/60/90, a Back Lever
     * 20/30/40 - so the number on the level card was not comparable across the
     * pool.
     *
     * The ladder is asserted as DATA rather than as a constant imported from
     * the pipeline, because the failure being guarded is the pipeline drifting
     * away from itself. A row may carry rungs ABOVE the shared floor only when
     * its own ceiling goes past it: 120s is a ceiling for anything HELD and
     * only a floor for anything REPEATED, so Air Bike legitimately keeps its
     * 180-600s intervals.
     */
    @Test
    fun everyHoldRowWalksTheSameDurationLadder() {
        val shared = listOf(15.0, 30.0, 45.0, 60.0, 90.0, 120.0)
        val holds = ExerciseProgression.ENTRIES.filter { it.unit == "seconds" }
        assertTrue(
            "no seconds-based rows in the pool, so this test would pass vacuously",
            holds.size > 100,
        )
        for (hold in holds) {
            assertTrue("${hold.slug} is not ascending", hold.levelValues == hold.levelValues.sorted())
            for (rung in hold.levelValues) {
                if (rung <= shared.last()) {
                    assertTrue(
                        "${hold.slug} has rung $rung, which is not on the shared ladder " +
                            "$shared - two rows would mean different things by level",
                        rung in shared,
                    )
                } else {
                    assertTrue(
                        "${hold.slug} runs to $rung, past the shared floor, without a " +
                            "ceiling of its own to justify it (120s is a CEILING for a " +
                            "hold and only a FLOOR for a repeated movement)",
                        (hold.maxValue ?: 0.0) >= rung,
                    )
                }
            }
            // The Easy/Medium/Hard starts are stored as a level INDEX, so a
            // ladder rewritten underneath them leaves them naming a rung that no
            // longer exists - silently, because the value just reads wrong.
            for ((label, level) in listOf(
                "easy" to hold.easyLevel, "medium" to hold.mediumLevel, "hard" to hold.hardLevel,
            )) {
                assertTrue(
                    "${hold.slug} $label start points at level $level, outside its own " +
                        "ladder of ${hold.levelValues.size}",
                    level == null || level in 1..hold.levelValues.size,
                )
            }
        }
    }

    /** A hold is a hold: no row in the pool prescribes reps for one. */
    @Test
    fun aHoldCarriesNoRomanPattern() {
        for (hold in ExerciseProgression.ENTRIES.filter { it.unit == "seconds" }) {
            if (hold.romanStages.isNotEmpty()) {
                continue // Olympic lifts keep a pattern - they are trained at 1-5 reps.
            }
            assertTrue(
                "${hold.slug} is a hold and must not prescribe per-set reps",
                hold.easyReps.isEmpty() && hold.mediumReps.isEmpty() && hold.hardReps.isEmpty(),
            )
        }
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
        // The engine speaks Roman internally and the UI surfaces it in the level
        // box, the editor's level list and this name.
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
        // Back Extension is a REP LADDER now, so its ceiling is the rep climb
        // (floor 8 -> 50 = 43 levels), not the 6 rungs its variation ladder had.
        // The clamp has to land on THAT or a user could park on a level the app
        // has no data for.
        val top = ProgressionEngine.effectiveMaxLevel(be)
        assertEquals(ProgressionEngine.repRungs(be), top)
        assertEquals(top, ProgressionEngine.clampLevel(be, 999))
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

        // Variation ladder: a REP LADDER prescribes no variation, because its level
        // counts reps and no longer selects a movement. The reps are its value.
        val pushup = entry("Push-Up")
        assertTrue(ProgressionEngine.isRepLadder(pushup))
        assertNull(ProgressionEngine.prescribedValueAtLevel(pushup, 1))

        // Duration (cardio).
        val bike = entry("Air Bike")
        val bikeRx = ProgressionEngine.prescribedValueAtLevel(bike, 1)!!
        assertEquals(ProgressionEngine.ValueKind.DURATION, bikeRx.kind)
        assertEquals(15.0, bikeRx.value!!, 0.001)
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
        // A rep ladder has no value to compare - its level is reps, so every
        // level-up INSIDE its ladder changes the prescription. Reading it off
        // the value would report "nothing changed" for every level in the pool.
        //
        // Walked against the row's own length rather than two fixed numbers:
        // the ladder's step (and so its rung count) is derived from its ceiling
        // and its Roman climb, and a fixed 8 -> 9 pair silently stops meaning
        // anything the moment the step grows and the ladder gets shorter.
        val be = entry("Back Extension")
        val top = ProgressionEngine.effectiveMaxLevel(be)
        for (level in 1 until top) {
            assertTrue(
                "Back Extension did not change from level $level to ${level + 1} of $top",
                ProgressionEngine.prescriptionChangesAt(be, level, level + 1)
            )
        }
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

    /**
     * The level-system gap, locked down on both sides.
     *
     * This test USED to assert that "Ab Wheel Rollout" had no starting level —
     * it was written to pin the gap, and it is the assertion the owner's
     * 2026-10-02 report ("lots of exercises have no level system") was really
     * about. The pipeline now installs a curated ladder on the 109 rows the v5
     * CSV left at `manual_review`, so that assertion is the bug.
     *
     * The documented remainder — Box Jump, Jump Squat, Plyo Lunge, Plyo
     * Push-Up, Nordic Hamstring Curl, Rope Climb, Wrist Roller — was closed
     * on the owner's instruction on 2026-10-06 ("give me a number as to how
     * many and then fix them all"; the number was seven, and the Chin Tuck
     * Hold that prompted it already had a 15s→120s duration ladder). Each now
     * counts its own reps — contacts, slow eccentrics, climbs, revolutions —
     * with the implement's height, tempo or load left to the user.
     *
     * So the pool sweep below is the WHOLE assertion: every row is inside the
     * system, and a gap reopening on any row at all fails here, named.
     */
    @Test
    fun exercisesOutsideTheLevelSystemHaveNoStartingLevel() {
        // The gap the owner reported is closed: a stretch is a hold in seconds,
        // a skill hold is a hold in seconds, and an implement-weighted row is a
        // kg ladder. None of them prescribe reps any more.
        assertNull(ProgressionEngine.startingLevel("", ProgressionEngine.Difficulty.HARD))

        assertTrue(
            "Ab Wheel Rollout should be level-managed now",
            ProgressionEngine.startingLevel("Ab Wheel Rollout", ProgressionEngine.Difficulty.MEDIUM) != null,
        )
        val wrist = requireNotNull(ProgressionEngine.entryFor("Kneeling Wrist Stretch"))
        assertEquals("a stretch has no load, so its measure is hold seconds", wrist.unit, "seconds")
        assertTrue(ProgressionEngine.isDurationBased(wrist))

        // The seven that used to be listed here as unfixable now prescribe a
        // starting level like everything else — named first, so a regression
        // says WHICH one reopened the hole the owner had closed.
        val formerGap = listOf(
            "Box Jump", "Jump Squat", "Plyo Lunge", "Plyo Push-Up",
            "Nordic Hamstring Curl", "Rope Climb", "Wrist Roller",
        )
        for (name in formerGap) {
            assertTrue(
                "$name is one of the seven the owner had fixed and must have a starting level",
                ProgressionEngine.startingLevel(name, ProgressionEngine.Difficulty.MEDIUM) != null,
            )
        }
        // ...and nothing at all is quietly outside the system.
        for (catalogEntry in ExerciseCatalog.ENTRIES) {
            val name = catalogEntry.name
            if (name.isBlank()) continue
            val entry = ProgressionEngine.entryFor(name)
            assertTrue("$name lost its level system", ProgressionEngine.isProgressionReady(entry))
        }
    }

    // --- Set-target banner vs the row's set count ---------------------------

    /**
     * The workout card renders BOTH sources: the pills and "Satz n von 4"
     * come from the routine row, the per-set targets from the published
     * pattern — which is often narrower. Seen on the device (2026-10-06):
     * the bent-over dumbbell row showed FOUR pills over "8 / 8 / 8 Wdh.",
     * because its pattern is [8, 8, 8] while the hypertrophy template stores
     * 4 sets. The padded list must equal what each set actually asks (the
     * resolver repeats the last value past the pattern), or set 4 is a guess.
     */
    @Test
    fun theSetTargetBannerListsEverySetTheRowAsksFor() {
        val row = entry("Bent-Over Dumbbell Row")
        val pattern = ProgressionEngine.prescribedReps(row, 1, 1)
            ?: error("the row must carry a stage-1 pattern")
        assertEquals("non-vacuous: this row's pattern is 3-wide", listOf(8, 8, 8), pattern)

        for (setCount in pattern.size..pattern.size + 2) {
            val padded = ProgressionEngine.stageTargetsForSets(row, 1, 1, setCount)
            assertEquals("one target per set", setCount, padded.size)
            padded.forEachIndexed { i, reps ->
                // The resolver's own rule: set N takes pattern[N-1], and
                // beyond the pattern the LAST set's target repeats.
                assertEquals(
                    "set ${i + 1} of $setCount",
                    pattern[i.coerceAtMost(pattern.size - 1)],
                    reps
                )
            }
        }
        // A count that matches the pattern is returned untouched.
        assertEquals(pattern, ProgressionEngine.stageTargetsForSets(row, 1, 1, pattern.size))
    }

    /**
     * The same rule over every progression-ready row: the published shape is
     * never rewritten, and two extra template sets append the last value
     * twice. Fails vacuously-proof: some row must actually need padding.
     */
    @Test
    fun everyRowsSetTargetsPadWithoutRewritingThePattern() {
        var paddedRows = 0
        for (e in ExerciseProgression.ENTRIES.filter { ProgressionEngine.isProgressionReady(it) }) {
            val pattern = ProgressionEngine.prescribedReps(e, 1, 1) ?: continue
            if (pattern.isEmpty()) continue
            val padded = ProgressionEngine.stageTargetsForSets(e, 1, 1, pattern.size + 2)
            assertEquals("${e.slug}: one target per set", pattern.size + 2, padded.size)
            assertEquals("${e.slug}: the published shape survives", pattern, padded.take(pattern.size))
            assertEquals("${e.slug}: past the pattern the last target repeats", pattern.last(), padded.last())
            if (padded.size > pattern.size) paddedRows++
        }
        assertTrue("non-vacuous: some row's template is wider than its pattern", paddedRows > 0)
    }
}
