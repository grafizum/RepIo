package com.grafizum.repio.data

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * The level-up system: the single source of truth for starting prescriptions
 * (Easy / Medium / Hard), rep progression inside a level, and level-ups —
 * including the rep reset the CSV prescribes when the next available load is
 * a large jump.
 *
 * Everything here reads the generated [ExerciseProgression] table (imported
 * from the v5 progression database, with all sequences pre-parsed by the
 * pipeline). The UI never sees a Roman numeral, a JSON sequence or a
 * validation flag; it sees [StartingPrescription] and [LevelProgression] only.
 *
 * Data-error contract: when a stored row is internally inconsistent (a stage
 * whose pattern row is missing, an empty sequence), the lookup helpers return
 * null and the callers fall back to a safe default — no auto-progress on bad
 * data, no crash, no Roman numerals on screen.
 */
object ProgressionEngine {

    /** The three difficulties a user picks in setup, mapped to the CSV columns. */
    enum class Difficulty { EASY, MEDIUM, HARD }

    /**
     * One exercise's state in the level system, persisted by the app
     * (WorkoutViewModel stores these as "level,stage" per exercise).
     */
    data class ProgressionState(
        val level: Int,          // main level (1-based)
        val stage: Int           // Roman stage index (1 = "I", 13 = "XIII")
    )

    /** What the setup flow turns the chosen difficulty into for one exercise. */
    data class StartingPrescription(
        val level: Int,
        val value: Double?,      // load in the exercise's unit (kg, stack step, …)
        val unit: String,
        val reps: List<Int>,     // per-set target reps, e.g. [8, 8, 8]
        val stage: Int,
        val setCount: Int,
        val restSeconds: Int
    )

    /** The next workout's suggestion after a stage advance or a level-up. */
    data class LevelProgression(
        val leveledUp: Boolean,
        val newState: ProgressionState,
        val value: Double?,      // the new level's load, null for bodyweight/duration
        val unit: String,
        val reps: List<Int>,     // the new stage's per-set targets
        val stage: Int
    )

    /** The progression entry for an exercise name, or null outside the pool. */
    fun entryFor(exerciseName: String): ProgressionEntry? =
        ExerciseProgression.forName(exerciseName)

    /**
     * Whether the exercise participates in the level system at all. Rows that
     * failed validation are excluded: the app falls back to the legacy
     * behavior for them instead of progressing on broken data.
     *
     * `requiresCoachReview` rows DO participate (owner decision 2026-09-24:
     * "Some exercises have no levels like pushups. Why?"). The flag's own
     * source note is only that the bodyweight variation ladder ORDERING is
     * heuristic — nothing unsafe hides in the numbers — and the 106-exercise
     * family it locked out (Push-Up, Chin-Ups, Dips, all the core holds and
     * stretches) was the entire visible gap in the level system. The CSV's
     * Easy/Medium/Hard start rungs are NOT trusted for these rows though:
     * [startingPrescription] falls back to Level 1 for them, so a fresh
     * exercise begins at its easiest variation rather than at the CSV's
     * guessed Hard entry point.
     */
    fun isProgressionReady(entry: ProgressionEntry?): Boolean {
        if (entry == null) return false
        if (entry.validationStatus != "valid" &&
            entry.validationStatus != "valid_but_review_recommended"
        ) return false
        return entry.levelValues.isNotEmpty() || entry.level1 != null
    }

    /**
     * True when [entry] passed validation but its ladder ordering is still
     * flagged for coach review (the bodyweight_variation family). The UI
     * treats these as fully level-managed; only the first-session start
     * derivation differs — see the comment on [isProgressionReady].
     */
    fun isCoachReviewed(entry: ProgressionEntry): Boolean = entry.requiresCoachReview

    /** Beginner premade builds only pick from this pool. */
    fun isBeginnerBuildEligible(exerciseName: String): Boolean =
        entryFor(exerciseName)?.beginnerBuildEligible == true

    // ------------------------------------------------------------------
    // Roman stages
    // ------------------------------------------------------------------

    private val ROMAN = listOf(
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII"
    )

    fun stageName(stage: Int): String = ROMAN.getOrElse(stage - 1) { "" }

    /**
     * The per-set reps the Roman pattern prescribes at [stage] (1-based), or
     * null when the pattern has no row there — the data-error fallback the
     * callers treat as "do not auto-progress".
     *
     * This is the STAGE's own row and nothing else. For an exercise with no
     * external load there is no other way to progress, so the level has to
     * carry reps too — see [prescribedReps], which is what the UI should
     * call. Kept as the raw lookup for the tests that assert the pattern
     * itself, and as [prescribedReps]'s own starting point.
     */
    fun romanStageReps(entry: ProgressionEntry, stage: Int): List<Int>? =
        entry.romanStages.getOrNull(stage - 1)?.takeIf { it.isNotEmpty() }

    /**
     * True when the exercise progresses by reps rather than by an external
     * load: bodyweight work (a variation ladder), holds and cardio. These are
     * the rows where "level 6" has to MEAN more work than "level 1", because
     * there is no weight on the bar to make it mean anything.
     */
    fun isRepProgressed(entry: ProgressionEntry): Boolean =
        isProgressionReady(entry) && valueKindOf(entry) != ValueKind.LOAD

    // ------------------------------------------------------------------
    // Rep ladders — the LEVEL is the rep count
    //
    // Owner ruling 2026-10-03: "changing the position of exercise i want
    // removed, instead increase the reps". A bodyweight row used to walk a
    // ladder of SUBSTITUTION MOVEMENTS — level 2 was a different exercise —
    // so the level said nothing about how much work the user was asked to do,
    // and the exercise changed without the user ever choosing to change it.
    //
    // Now the movement is fixed and the level counts reps, one per level, and
    // the row's variation ladder survives only as the BRANCH data the
    // progression offer draws from. That is the same thing the owner asked
    // for in the other direction: the exercise becomes something else only
    // when an offer is made and accepted.
    //
    // A REP LADDER is exactly a progression-ready row whose value is a
    // variation — the rows with no barbell and no clock. Duration rows (a
    // hold, a plank timed in seconds) keep their own ladder: their level
    // already means "longer hold", which is the same idea in another unit.
    // ------------------------------------------------------------------

    /** True when [entry]'s level counts reps rather than load, variation or seconds. */
    fun isRepLadder(entry: ProgressionEntry): Boolean =
        isProgressionReady(entry) && valueKindOf(entry) == ValueKind.VARIATION

    /**
     * The rep counts at which a rep ladder offers to become a harder movement.
     *
     * Grounded rather than picked: 8-12 is the range the hypertrophy
     * literature actually supports (Schoenfeld et al. 2021, the source of the
     * "8-12 hypertrophy zone"), so 12 is the first rung past which MORE REPS
     * stop being the best lever and a harder movement is the better one.
     * Above it ACSM's progression model puts local muscular endurance at high
     * repetitions - 15-25 - which is the second rung, and 50 is the ceiling
     * for work that has no load to raise at all.
     */
    val REP_CHECKPOINTS: List<Int> = listOf(12, 25, 50)
    /**
     * FALLBACK ceiling for a CORE rep ladder with no entry in [RepCeilings].
     * Every row the table covers has its own authored number; see
     * [repCeilingFor] for why the fallback still exists.
     */
    const val CORE_REP_CEILING = 50

    /**
     * FALLBACK ceiling for any other rep ladder with no entry in [RepCeilings].
     */
    const val STANDARD_REP_CEILING = 25

    /** Roughly how many rungs a rep ladder should have. See [repStepFor]. */
    private const val REP_LADDER_RUNGS = 10

    /**
     * A rung never adds fewer than this many reps ONCE THE LADDER HAS ROOM to
     * do it. See [repStepFor].
     */
    private const val MIN_REPS_PER_STEP = 2

    /**
     * The fewest rungs a rep ladder may be cut into. Below this it stops reading
     * as a ladder, which is why [repStepFor] will shrink a step rather than let
     * a thin row collapse to two rungs. See [repStepFor] and [repStepCeiling].
     */
    private const val MIN_REP_RUNGS = 4

    /**
     * Below this span a one-rep step is allowed, because there is no room for
     * anything else. A handstand push-up's authored ceiling is 12 against a
     * floor of 8: four reps of span cannot produce a five-rung ladder that also
     * steps by two. See [repStepFor].
     */
    private const val MIN_SPAN_FOR_A_BIGGER_STEP = MIN_REPS_PER_STEP * 3

    /**
     * THE FIRST RUNGS OF EVERY REP LADDER - the owner's ruling of 2026-10-16.
     *
     * Owner, 2026-10-16: *"Rep floor: start at 1 (4 for the hard variations), climb 4 to 8, then
     * 8-12"*.
     *
     * WHAT WAS WRONG, as a number: every rep ladder in the pool started at EIGHT. 430 rows carry
     * `rep_min = 8` in the CSV and the rest fell back to a constant that was also 8 - and 8 is the
     * BOTTOM OF THE HYPERTROPHY WINDOW, which is where a *working weight* lives, not where a
     * bodyweight ladder begins. A beginner arriving at a push-up ladder was told to do 8 reps at
     * level 1: for leverage work that is not a starting point, it is a target they may not reach,
     * and the ladder had nowhere left to climb below it.
     *
     * So the ladder now starts at the bottom of the movement and WARMS UP INTO the window:
     *
     *   1 -> 4 -> 8 -> 12, then the row's own step
     *
     * and a HARD VARIATION starts at 4 rather than 1, because one rep of a handstand push-up or a
     * one-arm row is not a prescription any more than eight were: the owner's own carve-out, and
     * the reason this is a list rather than a formula (the first rung of a ramp is a decision).
     *
     * The ramp's top is 12 for both - the top of the hypertrophy window, which is the number the
     * old floor was trying to express - so every ladder still arrives exactly where it used to
     * start, and every rung above it is unchanged in KIND (the row's own step, [repStepFor]).
     */
    private val ORDINARY_REP_RAMP = listOf(1, 4, 8, 12)
    private val HARD_VARIATION_REP_RAMP = listOf(4, 8, 12)

    /**
     * The ceiling at or below which a movement is a HARD VARIATION for the ramp above.
     *
     * Read off the row's OWN authored ceiling in [RepCeilings], never its name: a movement whose
     * ladder never leaves the hypertrophy window (12-15) is one whose difficulty is leverage rather
     * than load - a handstand push-up, a pistol squat, a one-arm row - and those are exactly the
     * rows where a single prescribed rep is meaningless. A row with room above 15 climbs like
     * ordinary work, so it starts from one.
     */
    private const val HARD_VARIATION_CEILING = 15

    fun isHardVariation(entry: ProgressionEntry): Boolean =
        repCeilingFor(entry) <= HARD_VARIATION_CEILING

    // ------------------------------------------------------------------
    // Step-counted ladders
    //
    // Owner, 2026-10-24 (dashboard task_5u1n4l5m063l3m203045583y): "Some exercises like heel
    // to toes walk have reps instead of steps."
    //
    // A walked drill is not counted in reps. What one rep of a heel-to-toe walk covers depends
    // on the room, and "12 reps" of it describes nothing a user can execute or count; the
    // balance drill is written and coached in STEPS. So these rows carry their OWN ladder, in the
    // same authored spirit as [RepCeilings]: a floor, a ceiling and a step, and the rungs are
    // linear because a step is a step (unlike a rep ladder, which warms up through
    // [ORDINARY_REP_RAMP] to reach the first real prescription).
    //
    // Only the rows whose work IS that count belong here. A walking lunge and a step-up are
    // prescribed per leg in reps and are NOT step counts; a row moves into this table when its
    // own movement is measured that way, never because its name says "walk".
    private data class StepLadder(val first: Int, val last: Int, val step: Int)

    private val STEP_LADDERS: Map<String, StepLadder> = mapOf(
        // 10 steps is the short end of a balance drill; 100 is past the point where the walking
        // stops being the practice. 10 a rung gives a ten-rung ladder that lands on 100 exactly.
        "heel_to_toe_walk" to StepLadder(first = 10, last = 100, step = 10),
        // A crawl travels in strides of a few paces: 5 is one short pass, 50 a long one.
        "bear_crawl" to StepLadder(first = 5, last = 50, step = 5),
    )

    private fun stepLadder(entry: ProgressionEntry): StepLadder? = STEP_LADDERS[entry.slug]

    /**
     * True when [entry]'s repeated work is counted in STEPS rather than reps.
     *
     * The display layer asks this through [repUnitKeyFor] so a walked drill says "10 / 10 / 10
     * steps" and never "Reps" - the word is a property of the exercise, and it is read here
     * rather than from a name so a rename cannot re-word a prescription.
     */
    fun countsInSteps(entry: ProgressionEntry): Boolean = stepLadder(entry) != null

    /** The string key for how [entry]'s repeated work is counted: "steps" or "reps". */
    fun repUnitKey(entry: ProgressionEntry): String = if (countsInSteps(entry)) "steps" else "reps"

    /** [repUnitKey] for a name, defaulting to reps for anything outside the pool. */
    fun repUnitKeyFor(exerciseName: String): String =
        entryFor(exerciseName)?.let { repUnitKey(it) } ?: "reps"

    /** The ladder's opening rungs, warm-up included: see [ORDINARY_REP_RAMP]. */
    fun repRamp(entry: ProgressionEntry): List<Int> =
        if (isHardVariation(entry)) HARD_VARIATION_REP_RAMP else ORDINARY_REP_RAMP

    /** The top of the ramp - where the stepped part of the ladder takes over. */
    private fun repRampTop(entry: ProgressionEntry): Int = repRamp(entry).last()

    /**
     * The rep count the ladder STARTS at: the first rung of the ramp.
     *
     * The row's declared `rep_min` is deliberately not consulted here any more. It is 8 wherever it
     * is set at all - the bottom of the hypertrophy window - so as a FLOOR it described the wrong
     * end of a beginner's ladder, which is the whole finding above. Nothing else read it: the
     * range-based rows ([prescribedReps]' non-ladder half) still walk `repMin` to `repMax`
     * themselves, and are untouched by this.
     */
    fun repFloorOf(entry: ProgressionEntry): Int = stepLadder(entry)?.first ?: repRamp(entry).first()

    /**
     * True when [entry] is core work, by the CATALOG's own primary muscle
     * through the app's existing broad-topic mapping, never by the name. "Plank"
     * and "Dead Bug" say nothing about the mover; the catalog does.
     */
    private fun isCoreRepRow(entry: ProgressionEntry): Boolean {
        val muscle = ExerciseCatalog.entryForId(entry.id)?.muscle ?: return false
        return WorkoutTemplates.getTopicForMuscle(muscle) == "Core"
    }

    /**
     * The most reps this exercise's ladder will ever ask for, and it is NOT the
     * same number for every row.
     *
     * [RepCeilings] carries an authored number for every one of the 116
     * bodyweight rep ladders, because a flat cap was wrong at both ends: it sent
     * a glute bridge to 25 reps when ordinary programming is 30-50, and a
     * handstand push-up to the same 25 when the movement itself ends near 12.
     *
     * The muscle fallback behind it is only a FALLBACK now - for a row added to
     * the pool after the table was written, or a user's own custom exercise,
     * which has no catalog row to ask. It keeps the old core/non-core split:
     * high-rep core work is how core training is ordinarily written, while 25
     * strict pull-ups is already an elite bodyweight standard. A row that falls
     * through is a row nobody authored a ceiling for, so the coverage test
     * fails the build rather than letting one ship silently.
     */
    fun repCeilingFor(entry: ProgressionEntry): Int =
        stepLadder(entry)?.last
            ?: RepCeilings.forSlug(entry.slug)
            ?: if (isCoreRepRow(entry)) CORE_REP_CEILING else STANDARD_REP_CEILING

    /**
     * True when [entry]'s ceiling came from the authored table rather than the
     * muscle fallback. Exposed so the coverage test can name the rows that
     * would silently get the flat cap.
     */
    fun hasAuthoredRepCeiling(entry: ProgressionEntry): Boolean =
        RepCeilings.forSlug(entry.slug) != null || countsInSteps(entry)

    /**
     * How many reps one level adds, which is not one for every row either.
     *
     * The rule is about the RUNG rather than the rep: the rung is what the user
     * sees, counts and navigates, and a ladder of 43 rungs that each add a
     * single rep is the defect that started this ("1 rep is basically
     * nothing"). So every rep ladder is built to roughly [REP_LADDER_RUNGS]
     * rungs and the step is whatever reaches the row's own ceiling in that
     * many: 2 for an ordinary 12-to-25 span, 4 for core's 12-to-50. A ladder
     * with no room for that rule - a skill row whose whole range is four reps
     * wide - steps by one rather than collapsing to three rungs. It is derived
     * from the row's own floor and its authored ceiling, never from its name.
     */
    /**
     * How many reps the exercise's Roman pattern adds WITHIN one level: the
     * largest climb any stage asks for over its stage-I row, across the sets.
     *
     * A property of the published pattern, never of the name. Crunches' pattern
     * runs [8,8,8] ... [12,12,12], so its climb is 4. It is the number that
     * decides whether a level's own band can hold the stage climb - see
     * [repStepFor] - which is what stops a level-up from demoting the reps.
     */
    fun stageClimb(entry: ProgressionEntry): Int {
        val first = romanStageReps(entry, 1) ?: return 0
        var climb = 0
        for (stage in 1..patternLength(entry)) {
            val row = romanStageReps(entry, stage) ?: continue
            row.forEachIndexed { i, reps ->
                val delta = reps - (first.getOrNull(i) ?: reps)
                if (delta > climb) climb = delta
            }
        }
        return climb
    }

    /**
     * The most reps a level may add and still leave a walkable ladder: one that
     * cuts the span into at least [MIN_REP_RUNGS] rungs. See [repStepFor].
     */
    private fun repStepCeiling(span: Int): Int =
        (span / (MIN_REP_RUNGS - 1)).coerceAtLeast(1)

    /**
     * How many reps one level adds, and it is not one number for every row.
     *
     * Two things set it now, and the first is the one that was missing.
     *
     * 1. **A level's band must hold the whole Roman climb** ([stageClimb]). The
     *    stage resets to I when the level advances, so a level that added fewer
     *    reps than its own pattern climbs would hand the user back FEWER reps on
     *    the level-up - a demotion at the exact moment they earned a level.
     *    Owner report 2026-10-06: a rep-only row finishing level 3 at its top
     *    stage dropped on level 4 "as if the weight would have been increased".
     *    The pattern is a climb of 4 for most rep rows, so a roomy ladder now
     *    steps by 5 - "1 level is 5 reps", the owner's own number.
     * 2. **The span still decides the rest**, rounded rather than floored so the
     *    ordinary 17-rep span does not become a step of 1 (17 / 10 == 1), which
     *    was the ladder of near-identical rungs this replaced.
     *
     * Where those two disagree - a four-rep-wide skill row cannot hold a
     * five-rep step AND four rungs - the step wins the ladder and the climb is
     * SCALED into the band instead; see [prescribedReps]. Every value here is
     * derived from the row's own floor, ceiling and pattern, never its name.
     */
    fun repStepFor(entry: ProgressionEntry): Int {
        // A STEP LADDER's rung is its own authored number and nothing else: a walk climbs by
        // steps, not by a rep step rescaled through a Roman pattern.
        stepLadder(entry)?.let { return it.step }
        // MEASURED FROM THE FLOOR, and deliberately NOT from the top of the ramp: the ramp moved
        // where the ladder STARTS, and the owner's 2026-10-16 ruling changes nothing about how far
        // apart the rungs are above it. Measuring from the ramp's top instead would have silently
        // re-cut every step in the pool (a 25-rep push-up's step fell from 5 to 3, and the "one
        // level is 5 reps" rule with it), which is a different change than the one that was asked
        // for. So the span stays as it was and only the rung VALUES below it changed.
        val span = (repCeilingFor(entry) - repFloorOf(entry)).coerceAtLeast(1)
        val spanStep = (span.toDouble() / REP_LADDER_RUNGS).roundToInt().coerceAtLeast(1)
        val spanBased = if (span >= MIN_SPAN_FOR_A_BIGGER_STEP) {
            spanStep.coerceAtLeast(MIN_REPS_PER_STEP)
        } else spanStep
        val climbStep = stageClimb(entry) + 1
        return maxOf(spanBased, climbStep)
            .coerceAtMost(repStepCeiling(span))
            .coerceAtLeast(1)
    }

    /**
     * The per-set rep target a rep ladder asks for at [level]: one STEP more
     * than the level below, from the row's own floor to [repCeilingFor].
     *
     * The floor is the row's declared minimum, and a row that declares none
     * starts at 8 rather than 1: one rep is not a prescription for anything,
     * and the supported range begins at the bottom of the hypertrophy window.
     */
    fun repsAtLevel(entry: ProgressionEntry, level: Int): Int {
        // STEPS FIRST: a walked drill's rung is floor + step x (level - 1), with no ramp and no
        // Roman climb doing the arithmetic. Past the top rung the count keeps climbing, the same
        // rule the rep ladders follow - the max is a marker, not a wall.
        stepLadder(entry)?.let { ladder ->
            val raw = ladder.first + ladder.step * (level - 1)
            val rungs = (ladder.last - ladder.first) / ladder.step + 1
            return if (level <= rungs) raw.coerceIn(ladder.first, ladder.last)
            else raw.coerceAtLeast(ladder.first)
        }
        val ramp = repRamp(entry)
        val floor = ramp.first()
        val top = repCeilingFor(entry)
        // THE RAMP FIRST, verbatim: 1 / 4 / 8 / 12 (or 4 / 8 / 12 on a hard variation). These are
        // the rungs the owner named, so they are written down rather than derived - a formula that
        // happened to produce them would be one edit away from not doing so.
        if (level <= ramp.size) return ramp[level - 1].coerceAtMost(top).coerceAtLeast(1)
        val raw = ramp.last() + repStepFor(entry) * (level - ramp.size)
        // Past the top rung the climb continues: the max is a marker, not a wall
        // (owner 2026-10-13). Below it the top still lands inside the ceiling.
        return if (level <= repRungs(entry)) raw.coerceIn(floor, top) else raw.coerceAtLeast(top)
    }

    /** How many rungs the ladder has: the ramp's, then the stepped ones up to the ceiling. */
    fun repRungs(entry: ProgressionEntry): Int {
        stepLadder(entry)?.let { return (it.last - it.first) / it.step + 1 }
        val ramp = repRamp(entry)
        val top = repCeilingFor(entry)
        // A ladder whose ceiling is INSIDE the ramp (a handstand push-up tops out at 12) is just
        // the part of the ramp that fits, and must not claim rungs above its own ceiling.
        if (top <= ramp.last()) return ramp.count { it <= top }.coerceAtLeast(1)
        val step = repStepFor(entry)
        // CEILING, not floor: a remaining span of 7 with a step of 2 is four rungs by integer
        // division, whose top rung is 14 of a declared 15 - so the ladder stopped one rep short of
        // the ceiling it was built to reach, and the ceiling itself was therefore unreachable.
        // Rounding up makes the last rung land on it, which repsAtLevel's coerceIn then holds.
        return ramp.size + ((top - ramp.last() + step - 1) / step)
    }

    /**
     * The first level whose reps reach or pass [reps].
     *
     * This is the right inverse for a checkpoint, because the rungs are a STEP
     * apart and a checkpoint usually does not land on one: with core's step of
     * 4 the 25-rep checkpoint is first reached at 28. Asking for the NEAREST
     * rung instead would hand back the level below it, 24 reps, and report
     * that the checkpoint had been passed before it was.
     */
    fun levelReachingReps(entry: ProgressionEntry, reps: Int): Int {
        // THE INVERSE IS READ OFF THE LADDER ITSELF now, one rung at a time, instead of being
        // solved arithmetically. The arithmetic assumed the whole ladder was one arithmetic
        // sequence stepping up from the floor, and with a RAMP at the bottom (1 -> 4 -> 8 -> 12,
        // then the row's step) that is no longer true: the closed form answered level 4 for a
        // checkpoint of 8 reps, which is a rung the ramp reaches at level 3. A ladder of at most a
        // dozen rungs is cheaper to walk than to describe, and it cannot disagree with [repsAtLevel]
        // because it asks it.
        if (reps <= repsAtLevel(entry, 1)) return 1
        for (level in 2..repRungs(entry)) {
            if (repsAtLevel(entry, level) >= reps) return level
        }
        return repRungs(entry)
    }

    /**
     * The checkpoint this level reaches, or null when it reaches none.
     *
     * "Reaches" means the level's reps have reached or passed the checkpoint:
     * the user who declines at 12 and climbs on is owed the next one, not told
     * the offer expired.
     *
     * AT THE TOP RUNG the next checkpoint is owed even when the rungs stop short
     * of it, because that is where the user would otherwise be stuck with
     * nothing to climb to and nothing offered: a non-core ladder tops out at
     * 24 reps with the 25 checkpoint exactly one rung above it. This is also
     * what makes the last decline final, because declining at the top records
     * the last checkpoint and there is no rung above it to re-arm at.
     */
    fun repCheckpointAtLevel(entry: ProgressionEntry, level: Int): Int? {
        if (!isRepLadder(entry)) return null
        val reps = repsAtLevel(entry, level)
        val reached = REP_CHECKPOINTS.lastOrNull { reps >= it }
        if (level < repRungs(entry)) return reached
        // AT THE TOP RUNG the final checkpoint is what is owed, even when the
        // rungs stop short of it. Reporting the last one REACHED here would be
        // the stale answer - a user on core's 48-rep top rung would be told the
        // 25 checkpoint was still pending, long after they passed it - and it
        // would make declining at the top look repeatable when there is nothing
        // above to re-arm at.
        return REP_CHECKPOINTS.last()
    }

    /** The first checkpoint strictly above [reps], where a declined offer returns. */
    fun nextRepCheckpoint(afterReps: Int): Int? =
        REP_CHECKPOINTS.firstOrNull { it > afterReps }

    /**
     * Whether [state] on [entry] is currently OWED a progression offer.
     *
     * This is the whole decline lifecycle in one pure function, and it lives
     * here rather than in the view model because the rule is the engine's: the
     * offer appears at a checkpoint, and declining at one checkpoint only
     * silences THAT one - the next checkpoint re-arms it. The owner's words:
     * "progressions dont have to only show at max level... and then if the user
     * chooses no, the exercise could go on with the level higher, lets say max
     * is 25 levels with reps 25, if the user then chooses the progression then
     * change the exercise, if he doesnt simply keep incrementing to maybe 50
     * and then a show progression again".
     *
     * [declinedAtReps] is the rep count the user last declined, or null when
     * nothing was declined. It is deliberately NOT a boolean: a boolean cannot
     * express "not yet", which is the whole point of the re-arm.
     *
     * For a LOAD row [declinedAtReps] is ignored and the answer is simply
     * [isMastered] — its ladder genuinely ends, so there is nothing to re-arm.
     */
    fun isProgressionPending(
        entry: ProgressionEntry,
        state: ProgressionState,
        declinedAtReps: Int?,
    ): Boolean {
        if (!isMastered(entry, state)) return false
        val checkpoint = repCheckpointAtLevel(entry, state.level)
            ?: return declinedAtReps == null
        return declinedAtReps == null || declinedAtReps < checkpoint
    }

    /**
     * The per-set reps to prescribe at a [level]/[stage] of [entry].
     *
     * For a LOAD row this is exactly the Roman stage's own row — the load
     * already carries the progression, so the reps stay put.
     *
     * For a REP LADDER (a bodyweight row with no barbell) the LEVEL is the rep
     * count — one more rep per level — and the Roman stage distributes that
     * count across the sets using the row's own cascade shape. See
     * [isRepLadder] for why the level stopped naming a different movement.
     *
     * For the remaining rep-progressed rows (holds, cardio, anything measured
     * in seconds) the reps rise with the level on a linear scale between
     * repMin and repMax, and the stage adds its own increment on top. Two
     * properties this guarantees, both requested by the owner and both
     * previously untrue:
     *
     *  - reps never DROP when the level changes. Stage I is the pattern's
     *    floor ([8,8,8]), so a level-up used to reset a set of 12s back to
     *    8s — a demotion the user could see on the card.
     *  - each level's ceiling is strictly higher than the level below, up to
     *    the row's own [repMax] ceiling. [repMin] is the floor at level 1.
     *
     * Returns null only for the data-error case (no pattern row at the
     * stage), matching [romanStageReps] so every caller keeps its existing
     * "do not auto-progress on bad data" behaviour.
     */
    /**
     * The stage's per-set targets for a routine row carrying [setCount] sets.
     *
     * A published pattern is often narrower than the goal template's set
     * count — the bent-over dumbbell row's pattern is [8, 8, 8] while the
     * hypertrophy template stores 4 sets — and the workout card renders BOTH:
     * the set pills and "Satz n von 4" come from the routine row, the per-set
     * targets from the pattern. Beyond the pattern's width the last set's
     * target repeats, which is the same rule the set-target resolver applies
     * when it answers "what does set N ask" — so the banner that promises
     * every set's target lists all of them instead of leaving the extra sets
     * to be guessed. A [setCount] no larger than the pattern is returned
     * untouched: a narrower row keeps its own published shape.
     */
    fun stageTargetsForSets(
        entry: ProgressionEntry,
        level: Int,
        stage: Int,
        setCount: Int
    ): List<Int> {
        val reps = prescribedReps(entry, level, stage).orEmpty()
        if (reps.isEmpty() || setCount <= reps.size) return reps
        return reps + List(setCount - reps.size) { reps.last() }
    }

    /**
     * The reps a STAGE row shows in the level UI, which is [prescribedReps] with
     * one exception: a DURATION row shows none.
     *
     * A timed exercise carries a Roman rep pattern like every other row - that is
     * the shape its ladder was published with - but its work is measured in
     * SECONDS, so a stage list printing "8 / 8 / 8 reps" beside a jumping jack told
     * the user the wrong unit for the exercise the level system had just learned to
     * time (owner, 2026-10-09: "it shows reps in the card yet the exercise is based
     * on time and has a timer????"). The row's own seconds live on the LEVEL
     * ([prescribedValueAtLevel]), not on the stage, so an empty list is the honest
     * answer here and the stage row shows its numeral alone.
     */
    fun stageRowReps(entry: ProgressionEntry, level: Int, stage: Int): List<Int> =
        if (isDurationBased(entry)) emptyList() else prescribedReps(entry, level, stage).orEmpty()

    fun prescribedReps(entry: ProgressionEntry, level: Int, stage: Int): List<Int>? {
        val base = romanStageReps(entry, stage) ?: return null
        if (!isProgressionReady(entry) || valueKindOf(entry) == ValueKind.LOAD) return base

        // A REP LADDER: the level IS the rep count, and the Roman stage
        // distributes it across the sets using the row's OWN cascade shape —
        // the extra reps walk forward, one set at a time, so the set the user
        // reaches last is the one that grows. The shape is read off the stored
        // pattern (each stage's row minus stage I's row) rather than
        // re-derived by a formula, because the pattern is the published part
        // and a formula that quietly disagreed with it would be invisible.
        //
        // This is what the owner chose between two readings of "the roman
        // number changes the reps per set": cascading forward, the shape the
        // pool already carries.
        if (isRepLadder(entry)) {
            val floorRow = romanStageReps(entry, 1) ?: return base
            val target = repsAtLevel(entry, level)
            val floor = repFloorOf(entry)
            val top = repCeilingFor(entry)
            // The pattern's climb is SCALED into the level's own band, so the
            // band always fits inside one step: the top of level L sits one rep
            // below the start of level L+1, whatever the pattern gives. The
            // SHAPE is still the pattern's own - the extra reps walk forward,
            // one set at a time - only its magnitude is normalised to the step.
            // That is what keeps [repStepFor]'s no-demotion promise true even on
            // a row whose pattern climbs more than its step: a 25-rep-ceiling
            // push-up steps by 2, so its climb of 4 is compressed to fit.
            val band = (repStepFor(entry) - 1).coerceAtLeast(0)
            val climb = stageClimb(entry)
            val scale = if (climb > 0) band.toDouble() / climb else 0.0
            // The ceiling caps the rungs inside the ladder; past the top rung the
            // climb continues, so the picker's rung above the max shows a bigger
            // number instead of repeating the ceiling (owner 2026-10-13).
            //
            // INSIDE THE LADDER THE BAND IS CAPPED BY THE NEXT RUNG, taken from the ladder itself
            // rather than from the row's step. The stage climb rides on top of the level's target,
            // so the two have to fit in the gap between this rung and the next - or a level-up
            // DEMOTES the reps, which is the one thing this whole function promises not to do. That
            // gap was always one step wide and the step-sized [band] above matched it by
            // construction; the owner's 2026-10-16 ramp (1 -> 4 -> 8 -> 12) is NOT evenly stepped,
            // so the gap is no longer the step and the band has to ask the ladder what it is. A
            // back extension's level 2 opens on 4 reps, and its level 1 could reach 5 under the old
            // band - a demotion the moment the user earned a level.
            val upper = if (level <= repRungs(entry)) {
                minOf(top, repsAtLevel(entry, level + 1) - 1).coerceAtLeast(target)
            } else Int.MAX_VALUE
            return base.indices.map { i ->
                val delta = base[i] - (floorRow.getOrNull(i) ?: base[i])
                (target + (delta * scale).roundToInt()).coerceIn(floor, upper)
            }
        }

        val min = entry.repMin ?: base.minOrNull() ?: return base
        val max = entry.repMax ?: min
        if (max <= min) return base
        val effMax = effectiveMaxLevel(entry).coerceAtLeast(1)
        val span = (effMax - 1).coerceAtLeast(1)
        // How far up the rep range this level sits, 0 at level 1 and 1 at the
        // top of the ladder.
        val progress = (level - 1).toDouble() / span
        val levelBase = min + ((max - min) * progress)
        // The stage still counts inside the level, but capped at repMax so a
        // deep stage cannot ask for more reps than the row's ceiling allows.
        val stageBonus = (base.maxOrNull() ?: min) - min
        val target = (levelBase + stageBonus).roundToInt().coerceIn(min, max)
        // Never below what the stage alone asked for, and never below the
        // row's floor: reps must not fall when the level changes.
        val floor = (base.maxOrNull() ?: min).coerceIn(min, max)
        return base.map { target.coerceAtLeast(floor) }
    }

    /** How many stages the exercise's Roman pattern defines. */
    fun patternLength(entry: ProgressionEntry): Int =
        entry.romanStages.count { it.isNotEmpty() }.coerceAtLeast(1)

    // ------------------------------------------------------------------
    // Starting prescriptions
    // ------------------------------------------------------------------

    /**
     * The starting prescription for an exercise at a difficulty, or null when
     * the exercise is not progression-ready (the caller falls back to the
     * legacy equipment-based estimate).
     */
    /**
     * WHERE A REP LADDER STARTS: the ramp rung its difficulty names - Easy 4, Medium 8, Hard 12.
     *
     * A rep ladder's level IS its rep count, so "which level do you start on" and "how many reps do
     * you start with" are one question - and the CSV answers it with a guessed ladder POSITION
     * (Push-Up: easy 1 / medium 6 / hard 12; Crunches 1 / 3 / 6; Glute Bridge 1 / 2 / 2) whose rep
     * targets are `[8,8,8]` at EVERY difficulty. That was harmless while level 1 WAS eight reps: any
     * start level landed the user on the bottom of the hypertrophy window.
     *
     * It stopped being harmless on 2026-10-16, when the owner put a warm-up ramp under every rep
     * ladder (*"Rep floor: start at 1 (4 for the hard variations), climb 4 to 8, then 8-12"*). Level
     * 2 of the ramp is four reps and level 1 is ONE, so a row that starts on its CSV rung would hand
     * a fresh user a 1-rep or 4-rep prescription on every bodyweight movement in their week - a
     * worse mistake than the one the old "trust the level, not the reps" rule was avoiding.
     *
     * So the difficulty picks the RUNG, and the three rungs it picks are the ramp's own working
     * rungs - the owner's own climb, read as a starting choice: 4 for Easy, 8 for Medium, 12 for
     * Hard. MEDIUM IS UNCHANGED WORK for every user: it lands on 8 reps, exactly where these rows
     * started before the ramp existed, because the ramp put rungs underneath the number that used to
     * BE level 1. What moved is the level INDEX (1 -> 3 on a standard ladder), and that the two
     * rungs below it now exist for a user who needs them.
     *
     * This used to apply to coach-reviewed rows only, on the reading that their CSV positions were
     * the untrustworthy ones. The measurement above says otherwise: EVERY rep row's difficulty
     * column is a guessed position over an identical `[8,8,8]`, so the rule is the ladder's, not the
     * review flag's.
     */
    fun repLadderStartLevel(entry: ProgressionEntry, difficulty: Difficulty): Int {
        val wanted = when (difficulty) {
            Difficulty.EASY -> EASY_START_REPS
            Difficulty.MEDIUM -> MEDIUM_START_REPS
            Difficulty.HARD -> HARD_START_REPS
        }
        val ramp = repRamp(entry)
        // The first rung that reaches the difficulty's own number. A hard variation's ramp is
        // [4, 8, 12], so its Easy start is 4 and its Hard start is 12 - the same three numbers, one
        // rung apart, which is why the ramp's last three rungs are the difficulty ladder.
        val index = ramp.indexOfFirst { it >= wanted }

        return (if (index >= 0) index + 1 else ramp.size).coerceAtLeast(1)
    }

    /** The reps each difficulty opens a bodyweight ramp on. See [repLadderStartLevel]. */
    private const val EASY_START_REPS = 4
    private const val MEDIUM_START_REPS = 8
    private const val HARD_START_REPS = 12

    fun startingPrescription(exerciseName: String, difficulty: Difficulty): StartingPrescription? {
        val entry = entryFor(exerciseName) ?: return null
        if (!isProgressionReady(entry)) return null

        val (level, value, stage, reps) = when (difficulty) {
            Difficulty.EASY ->
                Prescription(entry.easyLevel, entry.easyValue, entry.easyStage, entry.easyReps)
            Difficulty.MEDIUM ->
                Prescription(entry.mediumLevel, entry.mediumValue, entry.mediumStage, entry.mediumReps)
            Difficulty.HARD ->
                Prescription(entry.hardLevel, entry.hardValue, entry.hardStage, entry.hardReps)
        }

        // A BODYWEIGHT REP LADDER starts on the RAMP, at the rung the difficulty names: Easy 4,
        // Medium 8, Hard 12 - see [repLadderStartLevel] for the measurement that made its CSV rung
        // the wrong answer. Every other method (load, stack, resistance, variation) keeps the CSV's
        // own Easy/Medium/Hard level, where a level is a LOAD and the start is a real choice.
        val startLevel = if (isRepLadder(entry) || isCoachReviewed(entry)) {
            repLadderStartLevel(entry, difficulty)
        } else (level ?: 1)

        // The reps MUST be read at the level this prescription actually starts
        // on. They used to be read at the CSV's own rung, which for a
        // coach-reviewed row is NOT the level reported below — so Crunches came
        // back as "Level 1, 11 reps" while the level system prescribed 8 at
        // level 1. The routine was written with the higher number and the card
        // showed the lower one, which is exactly the demotion the owner
        // reported as "switching level reduces the reps". Read them together or
        // the pair is a lie.
        //
        // WHICH STAGE A FRESH PRESCRIPTION OPENS ON.
        //
        // A REP LADDER opens on stage I, and it has to: its level IS its rep count, so the
        // rung's number is the whole prescription and the Roman stage only spreads it across
        // the sets. The CSV's start stage is a guessed position (medium is stage II and hard
        // stage III on 74 rows), and pairing that stage with the ramp's rung produced a number
        // the level list never shows for the same level - measured 2026-11-02: Ball Pike level
        // 2 opened the add form on 9 / 9 / 9 while its level-list row read 8 / 8 / 8, and 73
        // rows disagreed the same way. That is the owner's report (dashboard,
        // task_2r6p1p3j2e405v701f2c186y: "If the exercise has 2 or 4 reps then it should show
        // the same amount in the level list"): one level, two numbers, one of them invented by
        // the stage.
        //
        // Every other method keeps the CSV's stage: on a LOAD row a stage is a documented part
        // of the ladder (a post-jump reset can genuinely restart mid-pattern), not a rep count.
        val startStage = if (isRepLadder(entry)) 1 else stage.coerceAtLeast(1)

        // The CSV pairs each start stage with target reps; when they disagree
        // (a data error), prefer the stage's own pattern row so the UI can
        // never display a mismatched pair.
        val stageReps = prescribedReps(entry, startLevel, startStage) ?: reps

        // A DURATION ROW HAS NO REP PATTERN, and demanding one refused its whole
        // prescription — which is how every timed exercise ended up looking like
        // a custom exercise with a rep range on it (owner, 2026-10-09: "Jumping
        // jacks is one of many exercises that doesnt show the level style and it
        // shows reps in the card yet the exercise is based on time and has a
        // timer????"). Measured over the real catalog: 147 of the 601 rows are
        // duration rows (planks, holds, carries, all the cardio machines, every
        // stretch) and NONE of them could resolve a state, because their ladder
        // lives in seconds and `prescribedReps` answers null for them. Without a
        // state [WorkoutViewModel.currentProgressionState] returns null, so the
        // card's `inSystem` was false and it fell to the plain custom summary,
        // printing the routine's stored rep string under a timer.
        //
        // An empty rep list is the TRUTHFUL answer for a duration row: the
        // prescription's number is its seconds, which
        // [prescribedValueAtLevel] already carries as a DURATION value, and
        // the card headlines with that. Only a row that has neither reps NOR
        // the duration ladder is genuinely undescribable.
        if (stageReps.isEmpty() && !isDurationBased(entry)) return null

        val startValue = if (isCoachReviewed(entry))
            valueAtLevelWithCeiling(entry, startLevel)
        else value

        return StartingPrescription(
            level = startLevel,
            value = startValue,
            unit = entry.unit,
            reps = stageReps,
            stage = startStage,
            // A duration row's set count is its routine row's, not its rep
            // pattern's: `stageReps` is empty there and 0 sets would be a
            // promise the card cannot keep. One set is the floor that says
            // "this prescription is one set of work" without inventing more.
            setCount = stageReps.size.coerceAtLeast(1),
            restSeconds = entry.restSeconds ?: 60
        )
    }

    /**
     * The starting level most exercises get at [difficulty].
     *
     * The CSV gives every exercise its own start rung, so there is no single
     * number: 304 of the 370 strength rows start at 1 / 4 / 7, but a few
     * variation ladders start at 1 / 5 / 9 and some never move at all. The
     * modal value is therefore described as "most exercises", and the picker
     * shows it instead of the old RPE label, which described a scale this app
     * does not use to choose a start.
     */
    fun typicalStartingLevel(difficulty: Difficulty): Int =
        startingLevels(difficulty).groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: 1

    /**
     * The distinct starting levels [difficulty] maps to across the pool, so the
     * "most exercises" claim above can be phrased as a range when the pool is
     * not uniform (e.g. 1..5 for Medium).
     */
    fun startingLevelRange(difficulty: Difficulty): IntRange {
        val levels = startingLevels(difficulty)
        if (levels.isEmpty()) return 1..1
        return levels.min()..levels.max()
    }

    /** The next level each progression-ready strength exercise starts at. */
    private fun startingLevels(difficulty: Difficulty): List<Int> =
        ExerciseProgression.ENTRIES
            .filter { includeInStrength(it) }
            .mapNotNull { entry ->
                when (difficulty) {
                    Difficulty.EASY -> entry.easyLevel
                    Difficulty.MEDIUM -> entry.mediumLevel
                    Difficulty.HARD -> entry.hardLevel
                }
            }

    /**
     * The level [exerciseName] would start at under [difficulty], regardless of
     * anything stored — i.e. the entry the difficulty picker is choosing. Null
     * for exercises outside the level system.
     */
    fun startingLevel(exerciseName: String, difficulty: Difficulty): Int? =
        startingPrescription(exerciseName, difficulty)?.level

    /**
     * Whether [entry] is part of the level system's strength pool: the same
     * filter [OverallLevelCalculator] uses, so "the exercises that count"
     * means one thing app-wide.
     */
    private fun includeInStrength(entry: ProgressionEntry): Boolean =
        isProgressionReady(entry) && entry.includeInStrengthV1

    private data class Prescription(
        val level: Int?, val value: Double?, val stage: Int, val reps: List<Int>
    )

    // ------------------------------------------------------------------
    // Level ladders
    // ------------------------------------------------------------------

    /**
     * The starting load of a LOAD ladder and the ONE step it repeats from there,
     * or null for every other row (a variation, a duration, or a row with no
     * increment to walk).
     *
     * OWNER REPORT, 2026-10-13 (board task_4j6j3m5f49730n5c4b0g3g6l): *"And
     * progressive increase makes absolute no sense, the increase of the weight
     * should always be the same (add more levels since the max will be the same
     * but the steps in between will be more)"*.
     *
     * He is reading the AUTHORED ladders, and 295 of the 454 load ladders grow
     * their step. The bench is the pattern: 20, 22.5, 25, 27.5, 30, 35, 40, 45,
     * 50, 60 - 2.5 kg four times, then 5 kg four times, then 10 kg - so the user
     * meets a 2.5 kg level-up and, ten levels later, a 10 kg one it did not ask
     * for. A ladder now walks ONE step from its starting load to its researched
     * top, so the increase is identical at every rung and the top does not move;
     * what grows is the number of levels it takes to reach it (the bench goes
     * from 58 rungs to 65 at a constant 2.5 kg). That is also exactly what the
     * POUND ladder has always done, so the two units now state one rule.
     *
     * The step is the SMALLEST the row itself declares - its own increment, or
     * the finest authored step when that is finer - so a constant ladder can
     * never be coarser than the one it replaces and never drops a load the row
     * used to be able to reach (a dumbbell authored 4, 5, 6, 8 ... with a 2 kg
     * declared increment keeps a 1 kg step). Kilogram rows are quantised to a
     * half-kilo, exactly as [loadGridStep] already does, so a rung is always a
     * weight the gym can set; pin and band counts stay whole.
     */
    fun constantStepLadder(entry: ProgressionEntry): Pair<Double, Double>? {
        if (valueKindOf(entry) != ValueKind.LOAD) return null
        val base = entry.level1 ?: entry.levelValues.firstOrNull() ?: return null
        if (base <= 0.0) return null
        val declared = entry.increment?.takeIf { it > 0.0 }
        val finestAuthored = entry.levelValues
            .zipWithNext { a, b -> b - a }
            .filter { it > 0.0 }
            .minOrNull()
        val raw = listOfNotNull(declared, finestAuthored).minOrNull() ?: return null
        val step = if (entry.unit.contains("kg")) {
            ((raw / 0.5).roundToInt() * 0.5).coerceAtLeast(0.5)
        } else {
            raw
        }
        return base to step
    }

    /**
     * The VALUE a LOAD ladder climbs to, when the row declares one: the
     * researched human ceiling, else the row's own maximum progression value.
     * Null for a row with neither, which is the signal to keep the declared rung
     * count rather than invent a top.
     */
    private fun ladderTopValue(entry: ProgressionEntry): Double? =
        entry.ceiling?.load?.takeIf { it > 0.0 }
            ?: entry.maxValue?.takeIf { it > 0.0 }

    /** The load at a main level of an exercise, or null when the ladder is absent. */
    fun valueAtLevel(
        entry: ProgressionEntry,
        level: Int,
        unit: WeightUnit = WeightUnit.KG
    ): Double? {
        if (unit == WeightUnit.LBS) poundLadder(entry)?.let { return it.at(level) }
        // A plate ladder walks ONE step from level 1 (see [constantStepLadder]).
        constantStepLadder(entry)?.let { (base, step) ->
            return base + step * (level - 1)
        }
        if (entry.levelValues.isNotEmpty()) {
            val n = entry.levelValues.size
            if (level <= n) {
                val clamped = level.coerceIn(1, n)
                return entry.levelValues[clamped - 1]
            }
            // The generated data ships only ~10 explicit values per row while
            // declaring a max of 30-40 (the sequence's own steps also grow:
            // bench 20→22.5→…→50→60). Clamping here made every level past the
            // sequence read the SAME load — level 11 looked identical to the
            // top rung. Instead, continue the ladder from the last explicit
            // value by the row's declared increment (or by the sequence's own
            // final step when no increment is declared). The seam stays
            // continuous: level n still reads exactly levelValues[n - 1].
            val last = entry.levelValues[n - 1]
            val step = entry.increment
                ?: entry.levelValues.takeLast(2).let { if (it.size == 2) it[1] - it[0] else null }
                ?: return last
            return last + step * (level - n)
        }
        // No explicit sequence: linear ladder from level_1_value by the increment.
        val base = entry.level1 ?: return null
        val inc = entry.increment ?: return base
        return base + inc * (level - 1)
    }

    /** The number of main levels the exercise supports. */
    fun maxLevel(entry: ProgressionEntry): Int =
        entry.maxLevel
            ?: entry.levelValues.size.takeIf { it > 0 }
            ?: 1

    /** The lowest selectable level (the ladder's first rung). */
    fun minLevel(entry: ProgressionEntry): Int = 1

    /** Clamps a user-chosen level into the exercise's (ceiling-aware) range. */
    fun clampLevel(entry: ProgressionEntry, level: Int, unit: WeightUnit = WeightUnit.KG): Int =
        // The max is a marker, not a wall: levels keep going past it, so the bound
        // is the picker's own limit rather than [effectiveMaxLevel].
        level.coerceIn(minLevel(entry), PICKER_LEVEL_LIMIT)

    /**
     * How many rungs the level picker lists. Deliberately far past every
     * exercise's own ladder — the pool tops out around level 30 — because the
     * rungs keep going past a program's end, and the rung its max lands on is
     * marked "max" (see [LadderRung]).
     */
    const val PICKER_LEVEL_LIMIT = 500

    /**
     * The level list starts at the first [EXTENSION_CHUNK] levels and grows by
     * one chunk per "extend" tap (50 → 100 → … 500). The picker shows a chunk
     * at a time so the list stays scannable instead of wall-of-rows.
     */
    const val EXTENSION_CHUNK = 50

    /**
     * One row of the level picker. Every rung carries a real prescription,
     * including the ones above [effectiveMaxLevel] — [isMax] marks the rung the
     * researched ceiling lands on, which the picker labels "max" rather than
     * stopping at it (owner 2026-10-13).
     */
    data class LadderRung(
        val level: Int,
        val prescribed: PrescribedValue?,
        val stageReps: List<Int>,
        /** True on [effectiveMaxLevel]'s rung, so the UI can label it "max". */
        val isMax: Boolean
    )

    /**
     * The picker's rows: the exercise's own rungs 1..[maxLevel], then reference
     * rows up to [listLength]. Empty for an entry outside the level system,
     * exactly like every other helper here.
     *
     * [listLength] is a floor, not a cap: an exercise whose own program is longer
     * than it keeps every one of its rungs. Truncating the real ladder to fit a
     * display number would hide levels the data does support.
     */
    fun ladderRungs(entry: ProgressionEntry, listLength: Int = PICKER_LEVEL_LIMIT): List<LadderRung> {
        if (!isProgressionReady(entry)) return emptyList()
        val max = effectiveMaxLevel(entry)
        return (1..listLength.coerceAtLeast(max)).map { level ->
            LadderRung(
                level = level,
                prescribed = prescribedValueAtLevel(entry, level),
                // Level-aware: a rep-progressed row shows MORE reps on a
                // higher rung, so the picker says what each level asks for
                // rather than repeating stage I on every row.
                stageReps = prescribedReps(entry, level, 1).orEmpty(),
                isMax = level == max
            )
        }
    }

    // ------------------------------------------------------------------
    // Rep progression inside a level, and level-ups
    // ------------------------------------------------------------------

    /**
     * Called when the user completes every set of the current stage at the
     * required load (the CSV's `meet_or_exceed_every_set_at_required_load`).
     *
     * Within the pattern the stage advances (I [8,8,8] -> II [9,8,8] -> …);
     * at the top stage the level goes up, the load moves to the new level's
     * sequence value, and the reps reset per the CSV: small/moderate jumps
     * restart the pattern at stage I, large/very-large jumps use the row's
     * post-jump reps (also stage I in the standard pattern, but the explicit
     * data row is authoritative and may differ).
     *
     * Returns null when the ladder is complete (top level at top stage) or
     * when the data is inconsistent — never auto-progress on bad data.
     */
    fun levelUp(entry: ProgressionEntry, state: ProgressionState): LevelProgression? {
        if (!isProgressionReady(entry)) return null

        val maxStage = patternLength(entry)

        return if (state.stage < maxStage) {
            // Simple stage progression, load unchanged.
            val nextStage = state.stage + 1
            val nextReps = prescribedReps(entry, state.level, nextStage) ?: return null
            LevelProgression(
                leveledUp = false,
                newState = ProgressionState(state.level, nextStage),
                value = valueAtLevelWithCeiling(entry, state.level),
                unit = entry.unit,
                reps = nextReps,
                stage = nextStage
            )
        } else {
            // Level-up: advance the load, reset reps per the CSV. The ladder
            // does NOT end at the ceiling-aware max: that rung is a MARKER, and
            // the levels after it keep climbing (owner 2026-10-13: "add more
            // levels after it ... past-max levels must be selectable"). The only
            // bound left is the picker's own limit, which is also what
            // [clampLevel] writes to - so a level the app can list is a level it
            // can level up into.
            val newLevel = state.level + 1
            if (newLevel > PICKER_LEVEL_LIMIT) return null
            // A rep-progressed row (bodyweight/hold/cardio) must NOT reset to
            // the pattern floor here: there is no heavier load to compensate,
            // so a reset would silently DROP the user's reps at the moment
            // they earned a level. prescribedReps scales them up with the new
            // level instead, and never below what the level below prescribed.
            val resetReps = if (isRepProgressed(entry)) {
                // Scale up with the level, but never BELOW the reps the user
                // just completed. Scaling alone is not enough: the user earns
                // this level from the LAST stage of the one below, which asks
                // for more reps than that same level's stage I does. Without
                // the floor, archer-pull-ups went 12 reps at L1's final stage
                // -> 9 at L2 — a rep DROP at the moment a level was earned,
                // which is the exact thing the owner asked to stop.
                val scaled = prescribedReps(entry, newLevel, 1)
                val floor = prescribedReps(entry, state.level, state.stage)
                when {
                    scaled == null -> floor
                    floor == null -> scaled
                    else -> scaled.map { maxOf(it, floor.max()) }
                }
            } else {
                val jumpClass = jumpClassForTransition(entry, state.level)
                if (jumpClass == "large" || jumpClass == "very_large") {
                    entry.postJumpReps.takeIf { it.isNotEmpty() }
                } else {
                    null
                } ?: romanStageReps(entry, 1)
            } ?: return null
            LevelProgression(
                leveledUp = true,
                newState = ProgressionState(newLevel, 1),
                value = valueAtLevelWithCeiling(entry, newLevel),
                unit = entry.unit,
                reps = resetReps,
                stage = 1
            )
        }
    }

    /**
     * The jump class ("small"/"moderate"/"large"/"very_large") of the
     * level -> level+1 transition, derived from the ladder's own values so the
     * classification always agrees with the loads the app will actually use.
     *
     * The boundaries mirror the v5 CSV's own jump-class sequence, which the
     * import measured across the pool: small < 10%, moderate 10-30%, large
     * 30-75%, very_large beyond (a 2x step or bigger).
     */
    fun jumpClassForTransition(entry: ProgressionEntry, fromLevel: Int): String {
        val current = valueAtLevel(entry, fromLevel)
        val next = valueAtLevel(entry, fromLevel + 1)
        if (current == null || next == null || current <= 0.0) return "moderate"
        val ratio = (next - current) / current
        return when {
            ratio >= 0.75 -> "very_large"
            ratio >= 0.30 -> "large"
            ratio >= 0.10 -> "moderate"
            else -> "small"
        }
    }

    // ------------------------------------------------------------------
    // Persistence helpers (state <-> "level,stage" for SharedPreferences)
    // ------------------------------------------------------------------

    fun serialize(state: ProgressionState): String = "${state.level},${state.stage}"

    fun deserialize(raw: String?): ProgressionState? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(",")
        val level = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val stage = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (level < 1 || stage < 1) return null
        return ProgressionState(level, stage)
    }

    // ------------------------------------------------------------------
    // Duration methods (cardio, holds)
    // ------------------------------------------------------------------

    /** True when the exercise progresses via seconds rather than load/reps. */
    fun isDurationBased(entry: ProgressionEntry): Boolean =
        entry.method == "duration_progression" ||
            entry.method == "distance_or_interval_progression" ||
            entry.unit == "seconds"

    /** The starting seconds for a duration exercise at a difficulty. */
    fun startingSeconds(exerciseName: String, difficulty: Difficulty): Int? {
        val entry = entryFor(exerciseName) ?: return null
        if (!isProgressionReady(entry) || !isDurationBased(entry)) return null
        return when (difficulty) {
            Difficulty.EASY -> entry.easyValue?.toInt()
            Difficulty.MEDIUM -> entry.mediumValue?.toInt()
            Difficulty.HARD -> entry.hardValue?.toInt()
        }
    }

    // ------------------------------------------------------------------
    // Display helpers — the only path the UI uses to describe a
    // prescription. No raw sequences or validation flags leave this object;
    // the value comes pre-typed so the UI can localize labels itself.
    // ------------------------------------------------------------------

    /** What kind of value a prescription describes, so the UI can label it correctly. */
    enum class ValueKind { LOAD, VARIATION, DURATION }

    /** How a LOAD prescription counts — kg plate math or equipment steps. */
    enum class LoadUnit { KG_EACH, KG, STACK_STEP, RESISTANCE_STEP }

    /**
     * The prescribed value at a main level, pre-typed for display. Exactly one
     * of [variation] / [value] is populated, depending on [kind]; [loadUnit]
     * is set only for LOAD.
     */
    data class PrescribedValue(
        val kind: ValueKind,
        val value: Double?,
        val variation: String?,
        val loadUnit: LoadUnit?
    )

    /** The kind of starting value an exercise's prescription carries. */
    fun valueKindOf(entry: ProgressionEntry): ValueKind = when {
        isDurationBased(entry) -> ValueKind.DURATION
        entry.unit == "variation_step" -> ValueKind.VARIATION
        else -> ValueKind.LOAD
    }

    /**
     * Movements held in ONE hand or with ONE bell, where "each" is simply untrue.
     *
     * Owner, 2026-11-11 (board task_240k4p5w583755053t162c4b): *"For some exercises
     * (each) on weights makes no sense when its only one weight."* The word was never
     * typed by hand - it followed the row's unit (`kg_per_dumbbell` -> [LoadUnit.KG_EACH]
     * -> "25 kg each") - and a per-hand unit is only honest for a movement that really
     * holds a bell in each hand.
     *
     * AUTHORED, never read off the name. The project's rule is that a movement's
     * properties come from data, because a rename must not re-word a prescription, and
     * "Pullover" or "Svend Press" say nothing about how many bells are in it. This is
     * the list of rows whose implement count is one; every other `kg_per_dumbbell` row
     * keeps "each" because it genuinely uses a pair (bench press, fly, row, curl...).
     *
     * It changes the WORDING only. No load, rung, level or overall level moves: the
     * numbers still come from the row's own ladder, and [LoadUnit.KG] prints the same
     * weight without the suffix.
     */
    private val SINGLE_IMPLEMENT_SLUGS: Set<String> = setOf(
        "dumbbell_pullover",                                 // Dumbbell Pullover
        "dumbbell_svend_press",                              // Dumbbell Svend Press
        "single_dumbbell_svend_press",                       // Single Dumbbell Svend Press
        "concentration_curl",                                // Concentration Curl
        "dumbbell_side_bend",                                // Dumbbell Side Bend
        "dumbbell_windmill",                                 // Dumbbell Windmill
        "one_arm_dumbbell_push_press",                       // One-Arm Dumbbell Push Press
        "one_arm_dumbbell_swing",                            // One-Arm Dumbbell Swing
        "single_arm_dumbbell_row",                           // Single-Arm Dumbbell Row
        "single_arm_hammer_curl",                            // Single-Arm Hammer Curl
        "single_arm_chest_supported_dumbbell_row",           // Single-Arm Chest-Supported Dumbbell Row
        "single_arm_dumbbell_overhead_tricep_extension",     // Single-Arm Dumbbell Overhead Tricep Extension
        "one_arm_single_leg_dumbbell_romanian_deadlift",     // One-Arm Single-Leg Dumbbell RDL
        "dumbbell_pistol_squat",                             // Dumbbell Pistol Squat
        "jefferson_curl",                                    // Jefferson Curl
        "cross_body_hammer_curl",                            // Cross Body Hammer Curl
        "dumbbell_tricep_kickback",                          // Dumbbell Tricep Kickback

    )

    /** True when [entry] is performed with one implement, so its load is not "each". */
    fun isSingleImplement(entry: ProgressionEntry): Boolean =
        entry.slug.replace('-', '_') in SINGLE_IMPLEMENT_SLUGS

    private fun loadUnitOf(entry: ProgressionEntry): LoadUnit = when (entry.unit) {
        // A per-hand unit on a ONE-bell movement reads "25 kg each" beside a single
        // bell, so those rows answer with the plain kilogram unit instead.
        "kg_per_dumbbell" -> if (isSingleImplement(entry)) LoadUnit.KG else LoadUnit.KG_EACH
        "stack_step" -> LoadUnit.STACK_STEP
        "resistance_step" -> LoadUnit.RESISTANCE_STEP
        else -> LoadUnit.KG
    }

    /**
     * How an exercise's load is counted, or null when the exercise has no
     * progression row (a user's own custom exercise) or carries no load at all
     * (a variation or duration ladder).
     *
     * The UI needs this for the *field*, not only for the prescription text: a
     * selectorized stack and a resistance band have a number that is NOT a
     * weight, so a field labelled "kg" on those rows asks the user a question
     * they cannot answer honestly. Dashboard task "steps instead of weight".
     */
    fun loadUnitFor(exerciseName: String): LoadUnit? {
        val entry = entryFor(exerciseName) ?: return null
        if (!isProgressionReady(entry)) return null
        if (valueKindOf(entry) != ValueKind.LOAD) return null
        return loadUnitOf(entry)
    }

    /** Where a resistance step sits on the light-to-heavy band ladder. */
    enum class BandTension { VERY_LIGHT, LIGHT, MEDIUM, HEAVY, VERY_HEAVY }

    /**
     * The band a `resistance_step` value names. The ladder runs 1..10, which is
     * five real bands with two rungs each: bands are bought in colours and
     * labelled by feel, not by number, so a raw "step 4" asks the user to know
     * a numbering their gym does not use.
     *
     * Bands also LOSE tension as they stretch, so a step number is not a
     * multiple of a weight either — which is why this exists instead of a
     * conversion.
     */
    fun bandTensionOf(step: Double): BandTension = when {
        step < 3 -> BandTension.VERY_LIGHT
        step < 5 -> BandTension.LIGHT
        step < 7 -> BandTension.MEDIUM
        step < 9 -> BandTension.HEAVY
        else -> BandTension.VERY_HEAVY
    }

    /**
     * The variation name at a main level of a variation-laddered exercise.
     * Levels past the named ladder repeat the hardest variation (the CSV's
     * max_level can exceed the ladder — e.g. 6 levels over 3 variations),
     * mirroring how [valueAtLevel] clamps loads. Null when the exercise has
     * no variation ladder or the level is below 1.
     *
     * NOT a rep ladder's identity any more: a rep ladder's level counts reps
     * and the movement only changes when the user accepts a progression offer,
     * so nothing may read a variation off a level. It stays readable as the
     * row's own sequence — that is the data the offer draws its destinations
     * from.
     */
    fun variationAtLevel(entry: ProgressionEntry, level: Int): String? {
        if (entry.variations.isEmpty() || level < 1) return null
        return entry.variations[(level - 1).coerceAtMost(entry.variations.size - 1)]
    }

    /**
     * The prescribed value at the START of a level: a load (kg or step count),
     * a variation name, or a duration in seconds. Null when the exercise
     * carries no value at that level (data error for a progression-ready row).
     */
    fun prescribedValueAtLevel(
        entry: ProgressionEntry,
        level: Int,
        unit: WeightUnit = WeightUnit.KG
    ): PrescribedValue? {
        return when (valueKindOf(entry)) {
            // A rep ladder's level no longer names a movement, so it has no
            // separate value to prescribe: the reps ARE the prescription and
            // they come from [prescribedReps]. Rendering a variation here
            // would put "Dragon Flag" back next to a level that now only means
            // "one more rep", which is exactly the confusion this removed.
            ValueKind.VARIATION -> if (isRepLadder(entry)) null
                else variationAtLevel(entry, level)
                    ?.let { PrescribedValue(ValueKind.VARIATION, null, it, null) }
            ValueKind.DURATION -> valueAtLevelWithCeiling(entry, level, unit)
                ?.let { PrescribedValue(ValueKind.DURATION, it, null, null) }
            ValueKind.LOAD -> valueAtLevelWithCeiling(entry, level, unit)
                ?.let { PrescribedValue(ValueKind.LOAD, it, null, loadUnitOf(entry)) }
        }
    }

    /**
     * True when the prescribed value at [level] differs from the one at
     * [fromLevel] — the level-up actually changed the load/variation.
     *
     * A REP LADDER is measured on its REPS, not on a value: its
     * [prescribedValueAtLevel] is null at every level (the reps are the whole
     * prescription), so comparing values would report that no level-up ever
     * changed anything — which is the one answer this function must never
     * give, because the level-up did happen.
     */
    fun prescriptionChangesAt(entry: ProgressionEntry, fromLevel: Int, level: Int): Boolean {
        if (isRepLadder(entry)) {
            return prescribedReps(entry, fromLevel, 1) != prescribedReps(entry, level, 1)
        }
        val before = prescribedValueAtLevel(entry, fromLevel)
        val after = prescribedValueAtLevel(entry, level)
        return before != after
    }

    // ------------------------------------------------------------------
    // Mastery, human ceilings and progression branches
    //
    // A variation has two honest endpoints: its own ladder ends (the
    // variation sequence runs out) or its load reaches the exercise's
    // realistic human ceiling from the research sidecar. Past either the
    // exercise does NOT level up — it is mastered, and the mastery UI offers
    // the coach-curated branches instead.
    // ------------------------------------------------------------------

    /** Why an exercise has stopped progressing. */
    enum class MasteryReason { LADDER_COMPLETE, CEILING_REACHED }
    /**
     * The step a LOAD ladder walks in its stretch zone, in the row's own unit.
     *
     * This is the row's OWN declared increment — barbell 2.5 kg total, dumbbell
     * 1-2 kg per hand, one pin, one band rung — because that is the smallest
     * change the equipment can actually make. The stretch zone used to divide
     * the remaining growth by a rung count taken from a log ratio, so a bench
     * got 1.61 kg rungs and a dumbbell curl 0.45 kg ones: numbers no gym can
     * set, which is the owner's report that "40.43 kg should not exist".
     *
     * Kilograms are quantised to a half-kilo, so a rung is always x.0 or x.5.
     * That is a FLOOR, not a target: a 2.5 kg barbell row keeps 2.5, because a
     * 1 kg jump is not the lever that row progresses by. Pin and band counts
     * are left whole — a fraction of a pin position does not exist.
     */
    /**
     * The pound ladder of a row whose load is a weight, or null for every other
     * row: a pin and a band rung are counts and do not convert, and a bodyweight
     * row has no load to convert.
     *
     * Rungs are GENERATED by adding the row's own pound increment, never by
     * rounding a kilogram value onto a grid. That is the whole point, and it is
     * measured rather than assumed: an arnold press steps 1 kg at its bottom
     * (2.2 lb), so rounding put its levels 1 and 2 both on 10 lb with a 2.5 lb
     * grid — a level-up that changes nothing — and a finer 1.25 lb grid merely
     * moved the repeat to level 11. A generated rung cannot repeat, because each
     * one is the one below plus exactly one increment.
     */
    data class PoundLadder(val baseLb: Double, val stepLb: Double, val topLb: Double) {
        /** Rungs from the row's own starting load to its researched ceiling. */
        val levels: Int = maxOf(1, ((topLb - baseLb) / stepLb).roundToInt() + 1)

        /** The load at a rung. The last rung IS the ceiling, not an overshoot. */
        fun at(level: Int): Double {
            val l = level.coerceAtLeast(1)
            // Past the ladder's own end it keeps climbing (see valueAtLevelWithCeiling).
            return if (l >= levels) topLb + stepLb * (l - levels) else baseLb + stepLb * (l - 1)
        }
    }

    fun poundLadder(entry: ProgressionEntry): PoundLadder? {
        if (!entry.unit.contains("kg")) return null
        val baseKg = entry.level1 ?: return null
        val ceilingKg = entry.ceiling?.load ?: return null
        val step = WeightUnit.LBS.poundGrid(entry.increment)
        val base = WeightUnit.LBS.snapToGrid(baseKg * WeightUnit.LB_PER_KG, step)
        val top = WeightUnit.LBS.snapToGrid(ceilingKg * WeightUnit.LB_PER_KG, step)
        if (top < base) return null
        return PoundLadder(base, step, top)
    }

    fun loadGridStep(entry: ProgressionEntry): Double? {
        val declared = entry.increment?.takeIf { it > 0.0 }
        val fromSequence = entry.levelValues.takeLast(2).let {
            if (it.size == 2) (it[1] - it[0]).takeIf { step -> step > 0.0 } else null
        }
        val step = declared ?: fromSequence ?: return null
        return if (entry.unit.contains("kg")) {
            ((step / 0.5).roundToInt() * 0.5).coerceAtLeast(0.5)
        } else {
            step
        }
    }

    /**
     * The load the ceiling defines at [level]. With an explicit ladder the
     * steps are preserved below its declared top and the stretch to the
     * ceiling walks the row's own increment (see [loadGridStep]), so every rung
     * is a load the gym can set and the ceiling load lands on the final level.
     * Without a ladder the load is interpolated from level 1 to the ceiling
     * across the declared max level. The result never exceeds the ceiling.
     */
    fun valueAtLevelWithCeiling(
        entry: ProgressionEntry,
        level: Int,
        unit: WeightUnit = WeightUnit.KG
    ): Double? {
        // A pound ladder is generated, not rounded, so it does not go through
        // the ceiling-stretch arithmetic at all (see [poundLadder]).
        if (unit == WeightUnit.LBS) poundLadder(entry)?.let { return it.at(level) }
        val ceiling = entry.ceiling?.load
        val effMax = effectiveMaxLevel(entry)
        // Above the max the ladder keeps climbing at the row's own rate, so the
        // max is a marker rather than a wall (owner 2026-10-13: "both climb like
        // usual based on the normal climbing until max level was").
        if (level > effMax) {
            val atMax = valueAtLevelWithCeiling(entry, effMax, unit) ?: return null
            val below = if (effMax > 1) valueAtLevelWithCeiling(entry, effMax - 1, unit) else null
            val step = loadGridStep(entry)
                ?: (atMax - (below ?: atMax)).takeIf { it > 0.0 }
                ?: return atMax
            return atMax + step * (level - effMax)
        }
        if (ceiling != null && level >= effMax) return ceiling
        val raw = valueAtLevel(entry, level)
        if (ceiling == null) return raw
        if (raw == null) {
            // No explicit ladder: interpolate level 1 -> ceiling over maxLevel.
            val base = entry.level1 ?: return null
            val max = maxLevel(entry)
            if (max <= 1 || ceiling <= base) return ceiling
            return (base + (ceiling - base) * (level - 1) / (max - 1)).coerceAtMost(ceiling)
        }
        // A CONSTANT-STEP ladder needs no stretch zone at all: [valueAtLevel]
        // already walks the row's own step from level 1 to the top, so the
        // stretch arithmetic below would fight it. Measured before this guard
        // existed: arnold-press read 37 kg at level 32 and then 36 kg at 33, a
        // ladder that went DOWN between two rungs, because the stretch zone was
        // still re-spreading a growth that had already been spread.
        if (constantStepLadder(entry) != null) return raw.coerceAtMost(ceiling)
        val declared = maxLevel(entry)
        if (level <= declared) return raw.coerceAtMost(ceiling)
        // Stretch zone (declared < level <= effMax). A LOAD ladder grows by the
        // row's own increment so every rung stays loadable; a DURATION ladder
        // has no increment to obey and keeps its even spread over the rungs.
        val ladderTop = valueAtLevel(entry, declared) ?: return raw
        val extra = effMax - declared
        if (extra <= 0) return raw.coerceAtMost(ceiling)
        val gridStep = if (valueKindOf(entry) == ValueKind.LOAD) loadGridStep(entry) else null
        val step = gridStep ?: ((ceiling - ladderTop) / extra)
        return (ladderTop + step * (level - declared)).coerceAtMost(ceiling)
    }

    /**
     * The last meaningful level of the exercise:
     * - a LOAD ladder keeps its authored rungs and then continues by the row's
     *   OWN increment (see [loadGridStep]) until the researched ceiling, which
     *   is the final rung. A bench ladder topping at 60 kg against a 180 kg
     *   elite ceiling gains 48 rungs of 2.5 kg, so every rung is a weight the
     *   gym can set.
     * - a DURATION ladder is re-scaled so the ceiling lands on the final level,
     *   its rungs being seconds rather than plates and having no increment.
     * - rows without a ladder keep their declared max level.
     * - rows whose ceiling is a variation cap (bodyweight ladders, ceiling
     *   == the ladder's own max value) keep their declared max level.
     * - a LOAD ladder whose ceiling is reached before the declared max ends at
     *   the rung that reaches it, so no level repeats a load (see the branch
     *   below; the declared max there is a variation count, not a rung count).
     */
    fun effectiveMaxLevel(entry: ProgressionEntry, unit: WeightUnit = WeightUnit.KG): Int {
        // A pound ladder's length is its own: 5 lb rungs to the same ceiling are
        // a different number of steps than 2.5 kg ones, which is honest — a
        // pound-based programme genuinely is a different programme.
        if (unit == WeightUnit.LBS) poundLadder(entry)?.let { return it.levels }
        // A REP LADDER's length is the number of rungs its rep climb has, not the
        // CSV's declared rung count: that was the variation ladder's length,
        // which is a different thing entirely now that the level no longer
        // selects a movement.
        if (isRepLadder(entry)) return repRungs(entry)

        val declared = maxLevel(entry)
        val ceiling = entry.ceiling?.load ?: return declared

        // A LOAD ladder grows in rungs the gym can set: continue the row's own
        // increment from its top to the researched ceiling, so the ceiling is
        // the last rung and every rung below it is loadable. The log-ratio
        // spread below is kept for DURATION ladders, whose rungs are seconds
        // rather than plates and which have no increment to obey.
        if (valueKindOf(entry) == ValueKind.LOAD) {
            // CONSTANT STEP (owner 2026-10-13, board task_4j6j3m5f49730n5c4b0g3g6l):
            // the step is the row's own, so the RUNG COUNT follows from it rather
            // than the step following from the CSV's rung count. The bench's 58
            // rungs of a growing 2.5/5/10 kg step become 65 rungs of 2.5 kg to
            // the same 180 kg ceiling, which is precisely "more levels, same max".
            constantStepLadder(entry)?.let { (base, step) ->
                val top = ladderTopValue(entry) ?: return@let
                if (top <= base) return@let
                return (((top - base) / step).roundToInt() + 1).coerceAtLeast(1)
            }
            val step = loadGridStep(entry) ?: return declared
            val ladderTop = valueAtLevel(entry, declared) ?: return declared
            if (ceiling <= ladderTop) {
                // The authored rungs already reach the ceiling within the declared
                // max, so the declared max is NOT the number of loads that exist:
                // it is the CSV's variation count (30-40) while only ~10 distinct
                // weights are authored. Trusting it repeated the ceiling for every
                // level above the last loadable rung — arnold-press walked 30
                // levels of the same 30 kg, bent-over-row 20 levels of 32 kg —
                // which is the owner's 2026-10-09 report ("from level 11 to level
                // 30 the exact same weight and reps … fix this for every
                // exercise"). The program ends at the rung that reaches the
                // ceiling: every authored load below it, then the ceiling itself.
                val loads = entry.levelValues
                if (loads.isEmpty()) return declared
                return (loads.count { it < ceiling } + 1).coerceAtLeast(1)
            }
            val extra = ((ceiling - ladderTop) / step).roundToInt()
            return declared + extra.coerceAtLeast(0)
        }

        val base = entry.levelValues.firstOrNull()
        if (base == null || base <= 0.0 || ceiling <= base) return declared
        // The ladder's own shape decides the length: its step grows with the
        // load (bench: 2.5 -> 5 -> 10 kg), so map the growth from level 1 to
        // the ceiling against the growth from level 1 to the ladder's own
        // last explicit rung — the larger stretch, not the increment tail.
        val ladderTop = entry.levelValues.last()
        if (ceiling <= ladderTop) return declared
        val growth = ladderTop / base
        val ceilingGrowth = ceiling / base
        val nExplicit = entry.levelValues.size
        val scaled = declared * (ln(ceilingGrowth) / ln(growth))
        return scaled.roundToInt().coerceAtLeast(nExplicit.coerceAtLeast(declared))
    }

    /**
     * True when [level] is the exercise's last: the ladder has run out or the
     * ceiling load has been reached. The caller (mastery flow) then shows the
     * branch/ceiling UI instead of another level-up.
     *
     * A REP LADDER is the exception, and it is the owner's ruling: it is
     * "at the offer" from the first checkpoint, not only at the top. More reps
     * stop being the better lever once the level passes the hypertrophy range,
     * so that is where a harder movement is offered — and declining it does
     * NOT end the exercise, it lets the level keep climbing to the next
     * checkpoint (see [repCheckpointAtLevel]).
     */
    fun isMastered(
        entry: ProgressionEntry,
        state: ProgressionState,
        unit: WeightUnit = WeightUnit.KG
    ): Boolean {
        if (!isProgressionReady(entry)) return false
        if (isRepLadder(entry)) return repCheckpointAtLevel(entry, state.level) != null
        val effMax = effectiveMaxLevel(entry, unit)
        // The offer is due ON the max rung, and that rung only. A level PAST it is
        // not mastery any more - the ladder keeps climbing there (owner
        // 2026-10-13: the max is a marker, not the end), so the mastery card
        // fires once where the researched evidence ends, and the user who
        // ignores it carries on training the same movement.
        if (state.level != effMax) return false
        // On the max rung: mastered only when there is genuinely no further
        // stage — i.e. the user completed the top stage, not when they merely
        // arrived at it.
        return state.stage >= patternLength(entry)
    }

    /** Why [entry] is mastered at [state]; [MasteryReason] or null if not mastered. */
    fun masteryReason(
        entry: ProgressionEntry,
        state: ProgressionState,
        unit: WeightUnit = WeightUnit.KG
    ): MasteryReason? {
        if (!isMastered(entry, state, unit)) return null
        // A rep ladder reaching a checkpoint is the ladder's own end for this
        // stretch of it — there is no load ceiling to have hit.
        if (isRepLadder(entry)) return MasteryReason.LADDER_COMPLETE
        val ceiling = entry.ceiling?.load
        val effMax = effectiveMaxLevel(entry, unit)
        // In pounds the ladder is GENERATED to end on the ceiling, so reaching
        // its top rung is reaching the ceiling by construction; in kilograms the
        // top rung is compared against it.
        val reachedCeilingLoad = ceiling != null &&
            valueKindOf(entry) == ValueKind.LOAD &&
            (
                unit == WeightUnit.LBS ||
                    valueAtLevelWithCeiling(entry, effMax)?.let { it >= ceiling } == true
                )
        return if (reachedCeilingLoad) MasteryReason.CEILING_REACHED
        else MasteryReason.LADDER_COMPLETE
    }

    /**
     * The exercise's branches, empty when none were curated — or when none of
     * the curated ones is the SAME MOVEMENT.
     *
     * Owner, 2026-10-17: "When doing cross body crunch to max level, it offers
     * standing side bend which is a completely different movement plane. Why tf
     * is this given as progression if its more of a regression. Fix this for all
     * exercises, i cant have progression exercises be shown that arent even
     * progressing the exercise before."
     *
     * The CSV's coach-curated branches are the app's offer, and a branch can
     * name a neighbouring exercise rather than a harder version of the same
     * one — Cross-Body Crunch (a bodyweight strength rotation) was offered
     * Standing Side Bend (a mobility stretch) with the note claiming it "keeps
     * the same movement", which is exactly the lie the owner is pointing at.
     *
     * So an offer must survive [isSameMovement]. An exercise with no surviving
     * branch is not offered at all: [WorkoutNowScreen] already reads an empty
     * branch list as a ceiling card, which is the honest picture when there is
     * no harder version of this movement to hand.
     */
    fun branchesFor(entry: ProgressionEntry): List<ProgressionBranch> =
        entry.branches.filter { isSameMovement(entry, entryFor(it.entry)) }

    /**
     * Whether [target] is the same movement as [source] closely enough to be
     * called its PROGRESSION rather than a different exercise.
     *
     * Two facts decide it, both from the catalog both exercises are already in:
     *
     *  - the TRAINING FAMILY of [ExerciseCatalogEntry.type]. A strength ladder
     *    ends where a strength ladder ends, not in a stretch or a cardio row —
     *    this is the half that catches the reported case, since Cross-Body
     *    Crunch is `strength_bodyweight` and Standing Side Bend is
     *    `mobility_stretching` while both are filed under Obliques.
     *  - the PRIMARY MUSCLE. "Progression" means the same thing the exercise
     *    was doing, done harder; a branch onto a different muscle is a new
     *    exercise, which is what the evolution picker is for rather than the
     *    level ladder's end.
     *
     * An exercise the catalog does not carry is neither filtered nor judged:
     * returning true keeps the curated offer rather than deleting it on a
     * lookup miss.
     */
    fun isSameMovement(source: ProgressionEntry, target: ProgressionEntry?): Boolean {
        if (target == null) return false
        val from = ExerciseCatalog.entryForId(source.id) ?: return true
        val to = ExerciseCatalog.entryForId(target.id) ?: return true
        if (trainingFamily(from.type) != trainingFamily(to.type)) return false
        return from.muscle.equals(to.muscle, ignoreCase = true)
    }

    /**
     * The coarse class a training type belongs to, for [isSameMovement].
     *
     * `strength_weighted_or_machine` and `strength_bodyweight` are one family:
     * a barbell ladder ending in its bodyweight variation is still the same
     * movement. Public so the rule can be pinned on its own.
     */
    fun trainingFamily(type: String): String = when {
        type.startsWith("strength") -> "strength"
        type.startsWith("cardio") -> "cardio"
        type.startsWith("mobility") -> "mobility"
        type.startsWith("isometric") -> "hold"
        else -> type
    }

    // ------------------------------------------------------------------
    // The evolution offer: "what it was" and "what it can become"
    //
    // The mastery section already lists branches as buttons. The evolution
    // popup is the same decision made legible: both ladders drawn as their
    // REAL numbers, side by side, so the choice is "I am here, I could be
    // here" rather than a list of names. Everything it shows is computed
    // from the same engine the prescriptions come from, so the popup cannot
    // promise a start level the next session would not actually give.
    // ------------------------------------------------------------------

    /**
     * One side of the offer. [level] is where the user stands, [maxLevel]
     * how far this ladder runs, [reps] what the current level asks for and
     * [nodes] the future path names (only the "after" side has a future).
     */
    data class EvolutionSide(
        val exerciseName: String,
        val level: Int,
        val maxLevel: Int,
        val stageRoman: String,
        val load: Double?,
        val unit: String,
        val reps: List<Int>,
        val nodes: List<String>,
    ) {
        /**
         * How far along this side is, 0f..1f. The "before" side is always
         * 1f by construction — that is the whole premise of the offer — so
         * this is really only meaningful for the "after" side, where it is
         * what makes the arrow a gain rather than a sideways move.
         */
        val progressFraction: Float
            get() = if (maxLevel <= 1) 1f else (level.toFloat() / maxLevel).coerceIn(0f, 1f)
    }

    /** The full before/after pair the popup draws. */
    data class EvolutionOffer(
        val from: EvolutionSide,
        val to: EvolutionSide,
        val targetName: String,
        val note: String,
        val tag: String,
    )

    /**
     * Builds the offer for [sourceName] -> [targetName], or null when either
     * side is missing or the target has no progression of its own (an offer
     * to start from nothing is not an upgrade, it is a restart).
     */
    fun evolutionOffer(
        sourceName: String,
        targetName: String,
        state: ProgressionState,
    ): EvolutionOffer? {
        val sourceEntry = entryFor(sourceName) ?: return null
        val targetEntry = entryFor(targetName) ?: return null
        if (!isProgressionReady(targetEntry)) return null
        val branch = sourceEntry.branches.firstOrNull { it.entry == targetName } ?: return null
        val startLevel = branchStartLevel(sourceEntry, state, targetEntry)
        return EvolutionOffer(
            from = EvolutionSide(
                exerciseName = sourceName,
                level = state.level,
                // The "before" ladder is drawn complete: it has been climbed.
                maxLevel = effectiveMaxLevel(sourceEntry),
                stageRoman = stageName(state.stage),
                load = valueAtLevelWithCeiling(sourceEntry, state.level),
                unit = sourceEntry.unit,
                reps = prescribedReps(sourceEntry, state.level, state.stage).orEmpty(),
                nodes = emptyList(),
            ),
            to = EvolutionSide(
                exerciseName = targetName,
                level = startLevel,
                maxLevel = effectiveMaxLevel(targetEntry),
                stageRoman = stageName(1),
                load = valueAtLevelWithCeiling(targetEntry, startLevel),
                unit = targetEntry.unit,
                reps = prescribedReps(targetEntry, startLevel, 1).orEmpty(),
                nodes = branch.nodes,
            ),
            targetName = targetName,
            note = branch.note,
            tag = branch.tag,
        )
    }

    /**
     * The branch the popup opens on when the user has not picked one: the
     * "advanced" branch if the coach curated one, else the first. An evolution
     * that offers a WEAKER exercise than the one being left behind is not an
     * evolution, so a branch whose effective max is shorter than the source's
     * is skipped when a longer one exists.
     */
    fun preferredBranch(entry: ProgressionEntry): ProgressionBranch? {
        // Filtered, not raw: the caption beside the level pill quotes this
        // target, so an off-movement branch here is the same false promise the
        // owner reported — see [branchesFor].
        val branches = branchesFor(entry)
        if (branches.isEmpty()) return null
        val sourceMax = effectiveMaxLevel(entry)
        val longer = branches.filter { candidate ->
            entryFor(candidate.entry)?.let { effectiveMaxLevel(it) >= sourceMax } ?: false
        }
        val pool = longer.ifEmpty { branches }
        return pool.firstOrNull { it.tag == "advanced" } ?: pool.first()
    }

    /**
     * The starting level for a branch the user just picked: the user's
     * relative depth into their current exercise maps onto the target
     * exercise's effective range, clamped inside it.
     *
     * The one adjustment is the case the plain mapping gets wrong. The user
     * only ever gets here at 100% depth (that is what "finished" means), so a
     * target ladder that is LONGER than the source lands them partway up and
     * keeps climbing - correct. But a target ladder of the SAME length lands
     * them at its own maximum: Crunches -> Hanging Knee Raise (6 levels ->
     * 6 levels) would hand over level 6 of 6, so the swap would be instantly
     * finished too and the before/after popup would draw two identical bars.
     * A ladder the same length has no more headroom to give, so it is capped
     * at [PEER_LADDER_START_RATIO] of its range - the swap changes the
     * movement and still leaves something to climb.
     */
    fun branchStartLevel(sourceEntry: ProgressionEntry, sourceState: ProgressionState, target: ProgressionEntry): Int {
        val sourceMax = effectiveMaxLevel(sourceEntry).coerceAtLeast(1)
        val ratio = (sourceState.level.toDouble() / sourceMax).coerceIn(0.0, 1.0)
        val targetMax = effectiveMaxLevel(target)
        val mapped = (ratio * targetMax).roundToInt().coerceIn(1, targetMax)
        if (targetMax > sourceMax) return mapped
        // Same length or shorter: never hand back a maxed-out ladder.
        val capped = (targetMax * PEER_LADDER_START_RATIO).roundToInt().coerceAtLeast(1)
        return minOf(mapped, capped).coerceAtLeast(1)
    }

    /**
     * Where a swap onto an equal-length ladder starts, as a fraction of that
     * ladder. See [branchStartLevel].
     */
    private const val PEER_LADDER_START_RATIO = 0.6

    /**
     * The suggested alternatives shown on a "Human Limit Reached" card when
     * no branch exists: same primary muscle, same or simpler equipment,
     * excluding the exercise itself. Falls back to the pattern-mates when the
     * muscle pool is empty.
     */
    fun suggestedAlternatives(exerciseName: String, limit: Int = 3): List<String> {
        val entry = entryFor(exerciseName) ?: return emptyList()
        val catalogEntry = ExerciseCatalog.entryForId(entry.id) ?: return emptyList()
        val muscle = catalogEntry.muscle
        val sameMuscle = ExerciseCatalog.ENTRIES.filter { other ->
            other.id != entry.id && other.muscle == muscle
        }
        if (sameMuscle.isEmpty()) return emptyList()
        // Prefer progression-ready rows so the suggestion is usable.
        val usable = sameMuscle.filter { other ->
            isProgressionReady(ExerciseProgression.forId(other.id))
        }
        val pool = usable.ifEmpty { sameMuscle }
        return pool.take(limit).map { it.name }
    }
}
