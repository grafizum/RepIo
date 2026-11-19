package com.grafizum.repio.data

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round

/**
 * The app-wide "Overall Level": one number describing how strong the user
 * currently is, computed as a **recency-weighted median of how far through its
 * own ladder each of their exercises sits**.
 *
 * ## Why a percentage, not a raw level
 *
 * The pool's ladders are not the same length. Measured over the 601 rows:
 * 116 run 43 levels (the rep ladders, whose rung count is the rep climb from
 * the row's floor to 50), 88 run 8, 31 run 10, 64 run 15, 57 run 20, 115 run 30
 * and 61 run 40. Averaging RAW levels therefore compared incomparable things —
 * a user at level 6 of a 6-level ladder (finished) sat level-for-level with a
 * user at level 6 of a 40-level ladder (barely started), and both were "6.0".
 * Every exercise is therefore normalised first:
 *
 *     percent = (level - minLevel) / (maxLevel - minLevel) × 100
 *
 * where `maxLevel` is [ProgressionEngine.effectiveMaxLevel], so a human ceiling
 * that shortens a ladder counts too. The bounds are the exercise's OWN first
 * and last rung, which is what makes 0% mean "you have done nothing of this
 * exercise's range" and 100% mean "you are at the top of it" on every row.
 *
 * ## Why a weighted MEDIAN, not a mean
 *
 * The owner asked for the median (2026-10-02). The reason it is the right
 * statistic here is robustness: a program built around one advanced lift — a
 * weighted pull-up, a pistol squat — alongside eight easy movements is
 * accurately described as "mostly working on the basics". A mean would be
 * dragged up by that single lift and describe a strength the user does not
 * have; the median reports the middle exercise, which is what the user is
 * actually experiencing.
 *
 * ## How recency is folded in WITHOUT turning it back into a mean
 *
 * The recency weight is kept (an exercise done today counts fully, one done
 * 60 days ago a quarter) by using the **weighted median** rather than the plain
 * one: sort the exercises by percent, then walk down accumulating their weights
 * and take the percent at which the cumulative weight first reaches half the
 * total. An exercise that has gone stale keeps its position in the ordering but
 * carries almost no weight, so it can no longer decide the outcome — which is
 * exactly the decay the old weighted mean provided, and exactly what a plain
 * median would have thrown away.
 *
 * This is a median in both senses: it is always one of the input percents (so
 * one outlier cannot invent a value that was never trained), and it is the
 * weight at which the centre of mass falls.
 *
 * ## The 0-100 figure is BACKEND ONLY
 *
 * The owner was explicit that users must not see the percentage or the
 * calculation behind it (2026-10-02). What the UI shows is the RANK letter,
 * which [OverallLevelRank] derives from this number. Nothing in the UI layer
 * formats a percent.
 *
 * ## Scope of a qualifying exercise
 *
 *  - it is flagged `include_in_strength_v1`, so cardio and mobility are out;
 *  - the level system can rate it, so manual-review and coach-review rows are out;
 *  - the user has logged at least one set of it, so untrained exercises are out.
 */
object OverallLevelCalculator {

    /** Below this many qualifying exercises there is no overall level to report. */
    const val MIN_EXERCISES = 5

    /** An exercise last trained this many days ago weighs 0.5. */
    const val HALF_LIFE_DAYS = 30.0

    /** λ = ln(2) / 30 per day — the decay constant the half-life implies. */
    val LAMBDA: Double = ln(2.0) / HALF_LIFE_DAYS

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    /**
     * One qualifying exercise.
     *
     * [level] is the user's current rung and [maxLevel] that exercise's own
     * ceiling-aware top rung; [minLevel] is its first. All three travel
     * together because a level is meaningless without its ladder — that is the
     * whole point of the normalisation.
     */
    data class Sample(
        val exerciseName: String,
        val level: Int,
        val lastDoneAt: Long,
        val minLevel: Int = 1,
        val maxLevel: Int = level
    )

    /**
     * The computed overall level: the recency-weighted median percent, or a
     * not-ready [Result] while there are fewer than [MIN_EXERCISES] samples.
     */
    data class Result(
        /** 0.0-100.0. Backend only — the UI shows the rank, not this. */
        val level: Double?,
        val ready: Boolean,
        val exerciseCount: Int
    )

    /**
     * The recency weight of an exercise: `exp(-λ · days)`, so 1.0 today, 0.5 at
     * the half-life and 0.25 at twice it. Negative day counts (a clock skewed
     * backwards) are treated as zero rather than amplifying the exercise.
     */
    fun weight(daysSinceLastDone: Long): Double =
        exp(-LAMBDA * daysSinceLastDone.coerceAtLeast(0L).toDouble())

    /** Whole days between [lastDoneAt] and [nowMillis]; never negative. */
    fun daysSince(lastDoneAt: Long, nowMillis: Long): Long =
        (nowMillis - lastDoneAt).coerceAtLeast(0L) / MILLIS_PER_DAY

    /**
     * How far through its OWN ladder a level sits, as 0.0-100.0.
     *
     * Level 1 of 6 is 0% and level 6 of 6 is 100%; level 1 of 40 is 0% and
     * level 40 of 40 is 100%. A degenerate ladder (one rung) cannot be
     * climbed, so it reports 100% — the exercise has no further range, and
     * treating it as 0% would make a finished movement look untrained and
     * hold the whole median down forever.
     *
     * Out-of-range levels are clamped rather than extrapolated: the picker can
     * offer a rung above the ladder's declared top, and an unclamped value
     * would report more than 100%.
     */
    fun percentOf(sample: Sample): Double {
        val span = sample.maxLevel - sample.minLevel
        if (span <= 0) return 100.0
        val fraction = (sample.level - sample.minLevel).toDouble() / span
        return (fraction.coerceIn(0.0, 1.0) * 100.0)
    }

    /**
     * Whether an exercise may contribute to the overall level: it has to be a
     * strength exercise *and* one the level system is willing to rate.
     */
    fun isStrengthExercise(exerciseName: String): Boolean {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return false
        return entry.includeInStrengthV1 && ProgressionEngine.isProgressionReady(entry)
    }

    /**
     * Turns the logged history into one [Sample] per qualifying exercise.
     *
     * [levelFor] supplies the user's current level for an exercise (null when
     * the exercise is outside the level system); [maxLevelFor] supplies that
     * exercise's ceiling-aware top rung, and defaults to the engine's own
     * lookup so a caller cannot accidentally normalise against a different
     * ladder than the one the prescription came from. The last-done timestamp
     * is the newest logged set, so a session of three sets counts as one
     * training day.
     */
    fun qualifyingSamples(
        loggedSets: List<LoggedSetEntity>,
        levelFor: (String) -> Int?,
        isStrengthExercise: (String) -> Boolean = ::isStrengthExercise,
        maxLevelFor: (String, Int) -> Int = { name, level ->
            val entry = ProgressionEngine.entryFor(name)
            if (entry == null) level else ProgressionEngine.effectiveMaxLevel(entry)
        }
    ): List<Sample> =
        loggedSets
            .groupBy { it.exerciseName }
            .mapNotNull { (name, sets) ->
                if (!isStrengthExercise(name)) return@mapNotNull null
                val level = levelFor(name) ?: return@mapNotNull null
                val entry = ProgressionEngine.entryFor(name)
                val min = entry?.let { ProgressionEngine.minLevel(it) } ?: 1
                Sample(
                    exerciseName = name,
                    level = level,
                    lastDoneAt = sets.maxOf { it.timestamp },
                    minLevel = min,
                    maxLevel = maxLevelFor(name, level).coerceAtLeast(min)
                )
            }

    /**
     * The recency-weighted median of the per-exercise percentages, rounded to
     * one decimal — or a not-ready [Result] when there are fewer than
     * [MIN_EXERCISES] samples.
     *
     * See [weightedMedian] for the reduction itself.
     */
    fun compute(samples: List<Sample>, nowMillis: Long): Result {
        if (samples.size < MIN_EXERCISES) {
            return Result(level = null, ready = false, exerciseCount = samples.size)
        }

        val totalWeight = samples.sumOf { weight(daysSince(it.lastDoneAt, nowMillis)) }
        if (totalWeight <= 0.0) {
            return Result(level = null, ready = false, exerciseCount = samples.size)
        }

        return Result(
            level = round(weightedMedian(samples, nowMillis) * 10.0) / 10.0,
            ready = true,
            exerciseCount = samples.size
        )
    }

    /**
     * The weighted median of the samples' percentages.
     *
     * Sorted ascending by percent, each carrying its recency weight, the answer
     * is the percent at which the running weight first reaches half the total.
     * With equal weights this reduces to the ordinary median — including the
     * even-count case, where the lower of the two middle values is taken — so
     * this is a strict generalisation of it rather than a different statistic.
     *
     * Taking the LOWER middle value on an even count is deliberate: it is the
     * ordinary median definition, and it errs toward reporting the user as
     * slightly less advanced than they are, which is the safer direction for a
     * stat that gates an achievement-style letter.
     */
    fun weightedMedian(samples: List<Sample>, nowMillis: Long): Double {
        if (samples.isEmpty()) return 0.0
        val weighted = samples
            .map { percentOf(it) to weight(daysSince(it.lastDoneAt, nowMillis)) }
            .sortedBy { it.first }
        val total = weighted.sumOf { it.second }
        if (total <= 0.0) return weighted[weighted.size / 2].first

        val halfway = total / 2.0
        var running = 0.0
        for ((percent, w) in weighted) {
            running += w
            if (running >= halfway) return percent
        }
        // Unreachable: the weights sum to `total`, so the loop always returns.
        return weighted.last().first
    }
}