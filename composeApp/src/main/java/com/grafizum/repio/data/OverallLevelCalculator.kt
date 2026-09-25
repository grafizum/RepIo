package com.grafizum.repio.data

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round

/**
 * The app-wide "Overall Level": one number describing how strong the user
 * currently is, as the **recency-weighted average of their per-exercise
 * levels**, rounded to one decimal.
 *
 * A flat average of raw levels is exactly what the stat should read as: "you
 * train at an average level of 6". An exercise done today counts fully; one
 * done 30 days ago counts half; one done 60 days ago a quarter. That keeps a
 * strong but abandoned lift from propping the number up forever, and it means
 * the value decays on its own even when the user logs nothing. A uniform gap
 * across every exercise leaves the plain average intact (the common weight
 * cancels); only *differing* recency moves the number.
 *
 * The calculator is pure: it takes the samples (level + last-done timestamp per
 * exercise) and the current time, and returns a [Result]. Everything the caller
 * needs to gather — logged sets, per-exercise progression state — goes in
 * through [qualifyingSamples], which keeps the arithmetic testable without a
 * database or a preferences file.
 *
 * Scope of a qualifying exercise (all three must hold):
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

    /** One qualifying exercise: its current level and when it was last trained. */
    data class Sample(
        val exerciseName: String,
        val level: Int,
        val lastDoneAt: Long
    )

    /**
     * The computed overall level: the recency-weighted mean rounded to one
     * decimal, or a not-ready [Result] while there are fewer than
     * [MIN_EXERCISES] samples.
     */
    data class Result(
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
     * the exercise is outside the level system); [isStrengthExercise] defaults
     * to the pool-wide rule above. The last-done timestamp is the newest logged
     * set, so a session of three sets counts as one training day.
     */
    fun qualifyingSamples(
        loggedSets: List<LoggedSetEntity>,
        levelFor: (String) -> Int?,
        isStrengthExercise: (String) -> Boolean = ::isStrengthExercise
    ): List<Sample> =
        loggedSets
            .groupBy { it.exerciseName }
            .mapNotNull { (name, sets) ->
                if (!isStrengthExercise(name)) return@mapNotNull null
                val level = levelFor(name) ?: return@mapNotNull null
                Sample(name, level, sets.maxOf { it.timestamp })
            }

    /**
     * The recency-weighted mean of the per-exercise levels, rounded to one
     * decimal — or a not-ready [Result] when there are fewer than
     * [MIN_EXERCISES] samples.
     */
    fun compute(samples: List<Sample>, nowMillis: Long): Result {
        if (samples.size < MIN_EXERCISES) {
            return Result(level = null, ready = false, exerciseCount = samples.size)
        }

        var numerator = 0.0
        var denominator = 0.0
        samples.forEach { sample ->
            val w = weight(daysSince(sample.lastDoneAt, nowMillis))
            numerator += w * sample.level
            denominator += w
        }
        // Every weight is positive, so this only trips on an empty list, which
        // the count check above already ruled out.
        if (denominator <= 0.0) {
            return Result(level = null, ready = false, exerciseCount = samples.size)
        }

        return Result(
            level = round((numerator / denominator) * 10.0) / 10.0,
            ready = true,
            exerciseCount = samples.size
        )
    }
}
