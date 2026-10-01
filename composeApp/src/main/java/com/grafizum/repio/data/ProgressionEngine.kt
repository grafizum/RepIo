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

    /**
     * The per-set reps to prescribe at a [level]/[stage] of [entry].
     *
     * For a LOAD row this is exactly the Roman stage's own row — the load
     * already carries the progression, so the reps stay put.
     *
     * For a rep-progressed row (bodyweight variation, hold, cardio) the reps
     * RISE with the level, which is what makes a level mean anything when
     * there is no weight to add. Two properties this guarantees, both
     * requested by the owner and both previously untrue:
     *
     *  - reps never DROP when the level changes. Stage I is the pattern's
     *    floor ([8,8,8]), so a level-up used to reset a set of 12s back to
     *    8s — a demotion the user could see on the card.
     *  - each level's ceiling is strictly higher than the level below, up to
     *    the row's own [repMax] ceiling. [repMin] is the floor at level 1.
     *
     * The scale is linear between repMin and repMax across the ladder's
     * effective range, and the stage is still honoured *inside* a level: the
     * stage adds its own increment on top, then the result is clamped to
     * [repMax]. So a mid-level stage still asks for more than the level's
     * base, exactly as it does on a loaded row.
     *
     * Returns null only for the data-error case (no pattern row at the
     * stage), matching [romanStageReps] so every caller keeps its existing
     * "do not auto-progress on bad data" behaviour.
     */
    fun prescribedReps(entry: ProgressionEntry, level: Int, stage: Int): List<Int>? {
        val base = romanStageReps(entry, stage) ?: return null
        if (!isProgressionReady(entry) || valueKindOf(entry) == ValueKind.LOAD) return base

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

        // Coach-reviewed rows (the bodyweight_variation family) start at Level
        // 1: the CSV's Easy/Medium/Hard rungs there encode the guessed ladder
        // position (Push-Up: 1 / 6 / 12), which would hand a fresh beginner the
        // hardest variation. Level 1 IS the row's easiest rung, so it is the
        // safe start for every difficulty — the difficulty still chooses the
        // routine build, not the entry level here.
        val startLevel = if (isCoachReviewed(entry)) 1 else (level ?: 1)

        // The reps MUST be read at the level this prescription actually starts
        // on. They used to be read at the CSV's own rung, which for a
        // coach-reviewed row is NOT the level reported below — so Crunches came
        // back as "Level 1, 11 reps" while the level system prescribed 8 at
        // level 1. The routine was written with the higher number and the card
        // showed the lower one, which is exactly the demotion the owner
        // reported as "switching level reduces the reps". Read them together or
        // the pair is a lie.
        //
        // The CSV pairs each start stage with target reps; when they disagree
        // (a data error), prefer the stage's own pattern row so the UI can
        // never display a mismatched pair.
        val stageReps = prescribedReps(entry, startLevel, stage) ?: reps
        if (stageReps.isEmpty()) return null

        val startValue = if (isCoachReviewed(entry))
            valueAtLevelWithCeiling(entry, startLevel)
        else value

        return StartingPrescription(
            level = startLevel,
            value = startValue,
            unit = entry.unit,
            reps = stageReps,
            stage = stage.coerceAtLeast(1),
            setCount = stageReps.size,
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

    /** The load at a main level of an exercise, or null when the ladder is absent. */
    fun valueAtLevel(entry: ProgressionEntry, level: Int): Double? {
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
    fun clampLevel(entry: ProgressionEntry, level: Int): Int =
        level.coerceIn(minLevel(entry), effectiveMaxLevel(entry))

    /**
     * How many rungs the level picker lists. Deliberately far past every
     * exercise's own ladder — the pool tops out around level 30 — because the
     * rungs above a program's end are marked, not prescribed (see [LadderRung]).
     */
    const val PICKER_LEVEL_LIMIT = 500

    /**
     * The level list starts at the first [EXTENSION_CHUNK] levels and grows by
     * one chunk per "extend" tap (50 → 100 → … 500). The picker shows a chunk
     * at a time so the list stays scannable instead of wall-of-rows.
     */
    const val EXTENSION_CHUNK = 50

    /**
     * One row of the level picker: a rung of the exercise's own program, or —
     * past the end of that program — a reference row the app prescribes nothing
     * for.
     *
     * [beyondPlan] rows carry neither a [prescribed] value nor [stageReps], on
     * purpose. [valueAtLevel] would happily keep answering past the ladder (it
     * clamps to the last rung), so a level-300 row would otherwise show the
     * level-30 load and read as a real target. Nothing can be selected up there
     * either: the stored level passes through [clampLevel], which is what keeps
     * the overall level — and the E→SS+ bands measured against it — inside the
     * range the data actually supports.
     */
    data class LadderRung(
        val level: Int,
        val prescribed: PrescribedValue?,
        val stageReps: List<Int>,
        val beyondPlan: Boolean
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
            if (level > max) {
                LadderRung(
                    level = level,
                    prescribed = null,
                    stageReps = emptyList(),
                    beyondPlan = true
                )
            } else {
                LadderRung(
                    level = level,
                    prescribed = prescribedValueAtLevel(entry, level),
                    // Level-aware: a rep-progressed row shows MORE reps on a
                    // higher rung, so the picker says what each level asks for
                    // rather than repeating stage I on every row.
                    stageReps = prescribedReps(entry, level, 1).orEmpty(),
                    beyondPlan = false
                )
            }
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
            // ends at the ceiling-aware max: past it the exercise is mastered
            // and the mastery flow (not another level) takes over.
            val newLevel = state.level + 1
            if (newLevel > effectiveMaxLevel(entry)) return null
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

    private fun loadUnitOf(entry: ProgressionEntry): LoadUnit = when (entry.unit) {
        "kg_per_dumbbell" -> LoadUnit.KG_EACH
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
    fun prescribedValueAtLevel(entry: ProgressionEntry, level: Int): PrescribedValue? {
        return when (valueKindOf(entry)) {
            ValueKind.VARIATION -> variationAtLevel(entry, level)
                ?.let { PrescribedValue(ValueKind.VARIATION, null, it, null) }
            ValueKind.DURATION -> valueAtLevelWithCeiling(entry, level)
                ?.let { PrescribedValue(ValueKind.DURATION, it, null, null) }
            ValueKind.LOAD -> valueAtLevelWithCeiling(entry, level)
                ?.let { PrescribedValue(ValueKind.LOAD, it, null, loadUnitOf(entry)) }
        }
    }

    /**
     * True when the prescribed value at [level] differs from the one at
     * [fromLevel] — the level-up actually changed the load/variation.
     */
    fun prescriptionChangesAt(entry: ProgressionEntry, fromLevel: Int, level: Int): Boolean {
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
     * The load the ceiling defines at [level]. With an explicit ladder the
     * steps are preserved below its declared top and the stretch to the
     * ceiling is spread evenly over the extra [effectiveMaxLevel] rungs, so
     * the ceiling load lands exactly on the final level. Without a ladder the
     * load is interpolated from level 1 to the ceiling across the declared
     * max level. The result never exceeds the ceiling.
     */
    fun valueAtLevelWithCeiling(entry: ProgressionEntry, level: Int): Double? {
        val ceiling = entry.ceiling?.load
        val effMax = effectiveMaxLevel(entry)
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
        val declared = maxLevel(entry)
        if (level <= declared) return raw.coerceAtMost(ceiling)
        // Stretch zone (declared < level <= effMax): spread the remaining
        // growth (ladder top -> ceiling) evenly over the extra rungs.
        val ladderTop = valueAtLevel(entry, declared) ?: return raw
        val extra = effMax - declared
        if (extra <= 0) return raw.coerceAtMost(ceiling)
        val step = (ceiling - ladderTop) / extra
        return (ladderTop + step * (level - declared)).coerceAtMost(ceiling)
    }

    /**
     * The last meaningful level of the exercise:
     * - rows with an explicit ladder are RE-SCALED so the ceiling load lands
     *   on the final level — the ladder's shape (its steps) is preserved,
     *   its top is stretched/compressed to the researched ceiling. A bench
     *   ladder topping at 60 kg against a 180 kg elite ceiling keeps its
     *   20→22.5→… steps but reaches 180 at its last level.
     * - rows without a ladder keep their declared max level.
     * - rows whose ceiling is a variation cap (bodyweight ladders, ceiling
     *   == the ladder's own max value) keep their declared max level.
     */
    fun effectiveMaxLevel(entry: ProgressionEntry): Int {
        val declared = maxLevel(entry)
        val ceiling = entry.ceiling?.load ?: return declared
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
     */
    fun isMastered(entry: ProgressionEntry, state: ProgressionState): Boolean {
        if (!isProgressionReady(entry)) return false
        val effMax = effectiveMaxLevel(entry)
        if (state.level < effMax) return false
        if (state.level > effMax) return true
        // At the effective max: mastered only when there is genuinely no
        // further stage — i.e. the user completed the top stage, not when
        // they merely arrived at it.
        return state.stage >= patternLength(entry)
    }

    /** Why [entry] is mastered at [state]; [MasteryReason] or null if not mastered. */
    fun masteryReason(entry: ProgressionEntry, state: ProgressionState): MasteryReason? {
        if (!isMastered(entry, state)) return null
        val ceiling = entry.ceiling?.load
        val effMax = effectiveMaxLevel(entry)
        val reachedCeilingLoad = ceiling != null &&
            valueKindOf(entry) == ValueKind.LOAD &&
            valueAtLevelWithCeiling(entry, effMax)?.let { it >= ceiling } == true
        return if (reachedCeilingLoad) MasteryReason.CEILING_REACHED
        else MasteryReason.LADDER_COMPLETE
    }

    /** The exercise's branches, empty when none were curated. */
    fun branchesFor(entry: ProgressionEntry): List<ProgressionBranch> = entry.branches

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
        val branches = entry.branches
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
