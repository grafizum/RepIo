package com.grafizum.repio

import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.OverallLevelCalculator
import com.grafizum.repio.data.WorkoutTemplates
import kotlin.math.ln
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the overall-strength-level maths: a recency-weighted average of the
 * per-exercise levels, with a 30-day half-life and a five-exercise floor.
 *
 * The numbers below are the specification, not a snapshot of the implementation —
 * a weight is 1.0 today, exactly 0.5 at 30 days and exactly 0.125 at 90 days
 * (exp(-3·ln2)), which makes every expected level here exact rather than
 * approximately right.
 */
class OverallLevelCalculatorTest {

    private val now = 1_700_000_000_000L
    private val oneDay = 24L * 60L * 60L * 1000L

    private fun daysAgo(days: Long): Long = now - days * oneDay

    /** Five exercises at the given levels, all trained today. */
    private fun today(vararg levels: Int): List<OverallLevelCalculator.Sample> =
        levels.mapIndexed { index, level ->
            OverallLevelCalculator.Sample("Exercise $index", level, now)
        }

    @Test
    fun theHalfLifeIsThirtyDays() {
        assertEquals(30.0, OverallLevelCalculator.HALF_LIFE_DAYS, 0.0)
        assertEquals(ln(2.0) / 30.0, OverallLevelCalculator.LAMBDA, 1e-12)
    }

    @Test
    fun theWeightHalvesEveryHalfLife() {
        assertEquals(1.0, OverallLevelCalculator.weight(0), 1e-12)
        assertEquals(0.5, OverallLevelCalculator.weight(30), 1e-12)
        assertEquals(0.25, OverallLevelCalculator.weight(60), 1e-12)
        assertEquals(0.125, OverallLevelCalculator.weight(90), 1e-12)
    }

    @Test
    fun theWeightNeverGrowsForANegativeDayCount() {
        // A clock that moved backwards must not make an exercise count for more
        // than one trained today.
        assertEquals(1.0, OverallLevelCalculator.weight(-5), 0.0)
    }

    @Test
    fun daysAreCountedWholeAndNeverNegative() {
        assertEquals(0L, OverallLevelCalculator.daysSince(daysAgo(0), now))
        assertEquals(0L, OverallLevelCalculator.daysSince(now - oneDay + 1, now))
        assertEquals(1L, OverallLevelCalculator.daysSince(daysAgo(1), now))
        assertEquals(90L, OverallLevelCalculator.daysSince(daysAgo(90), now))
        assertEquals(0L, OverallLevelCalculator.daysSince(now + oneDay, now))
    }

    @Test
    fun fewerThanFiveExercisesIsNotReady() {
        for (count in 0..4) {
            val result = OverallLevelCalculator.compute(
                List(count) { OverallLevelCalculator.Sample("Exercise $it", 5, now) },
                now
            )
            assertFalse("$count exercises must not unlock the level", result.ready)
            assertNull(result.level)
            assertEquals(count, result.exerciseCount)
        }
    }

    @Test
    fun fiveExercisesAtLevelFiveDoneTodayScoreFive() {
        val result = OverallLevelCalculator.compute(today(5, 5, 5, 5, 5), now)
        assertTrue(result.ready)
        assertEquals(5.0, result.level!!, 1e-12)
        assertEquals(5, result.exerciseCount)
    }

    @Test
    fun equallyRecentExercisesAreAveraged() {
        // Levels 3..7 average to 5.0.
        val result = OverallLevelCalculator.compute(today(3, 4, 5, 6, 7), now)
        assertEquals(5.0, result.level!!, 1e-12)
    }

    @Test
    fun theDocumentedExampleScoresSix() {
        // The number the label promises: levels 8, 6, 5, 4 and 7, all trained
        // recently, read as L.6.0 — the user trains at an average level of 6.
        val result = OverallLevelCalculator.compute(today(8, 6, 5, 4, 7), now)
        assertTrue(result.ready)
        assertEquals(6.0, result.level!!, 1e-12)
    }

    @Test
    fun aUniformTrainingGapLeavesThePlainAverageIntact() {
        // Weighting only moves the number when the exercises' recency differs.
        // If every one of them was trained 90 days ago, each weight is the same
        // 0.125 and the common factor cancels: still 6.0, just older.
        val samples = listOf(8, 6, 5, 4, 7).mapIndexed { index, level ->
            OverallLevelCalculator.Sample("Exercise $index", level, daysAgo(90))
        }
        assertEquals(6.0, OverallLevelCalculator.compute(samples, now).level!!, 1e-12)
    }

    @Test
    fun aRecentExerciseDominatesStaleOnes() {
        // One exercise at level 10 done today, four at level 2 done 90 days ago.
        // Weights: 1.0 and four times 0.125.
        // (1.0·10 + 0.5·2) / (1.0 + 0.5) = 11 / 1.5 = 7.333… → 7.3
        val samples = listOf(
            OverallLevelCalculator.Sample("Fresh", 10, now)
        ) + List(4) { OverallLevelCalculator.Sample("Stale $it", 2, daysAgo(90)) }

        val result = OverallLevelCalculator.compute(samples, now)
        assertTrue(result.ready)
        assertEquals(7.3, result.level!!, 1e-12)
    }

    @Test
    fun theLevelIsRoundedToOneDecimal() {
        val samples = listOf(
            OverallLevelCalculator.Sample("Recent", 2, now),
            OverallLevelCalculator.Sample("Old", 3, daysAgo(30)),
            OverallLevelCalculator.Sample("A", 2, now),
            OverallLevelCalculator.Sample("B", 2, now),
            OverallLevelCalculator.Sample("C", 2, now)
        )
        val result = OverallLevelCalculator.compute(samples, now)
        // (2 + 2 + 2 + 2 + 3·0.5) / (4 + 0.5) = 9.5 / 4.5 = 2.111… → 2.1
        assertEquals(2.1, result.level!!, 1e-12)
    }

    @Test
    fun onlyTrainedStrengthExercisesTheLevelSystemCanRateQualify() {
        val pool = WorkoutTemplates.getAllExerciseNames()
        val strength = pool.filter { OverallLevelCalculator.isStrengthExercise(it) }
        val notStrength = pool.filterNot { OverallLevelCalculator.isStrengthExercise(it) }
        assertTrue("the pool should offer strength exercises", strength.size >= 5)
        assertTrue("the pool should offer cardio/mobility/manual-review rows", notStrength.isNotEmpty())

        val logged = (strength.take(5) + notStrength.first() + "Not An Exercise At All")
            .map { name -> loggedSet(name, now) }

        val samples = OverallLevelCalculator.qualifyingSamples(logged, levelFor = { 7 })

        assertEquals(
            strength.take(5).toSet(),
            samples.map { it.exerciseName }.toSet()
        )
    }

    @Test
    fun anExerciseTheLevelSystemCannotRateIsSkippedEvenWhenTrained() {
        val name = WorkoutTemplates.getAllExerciseNames()
            .first { OverallLevelCalculator.isStrengthExercise(it) }

        // levelFor returning null is how the caller reports "no level for this
        // exercise", e.g. a row the level system does not cover.
        val samples = OverallLevelCalculator.qualifyingSamples(
            listOf(loggedSet(name, now)),
            levelFor = { null }
        )

        assertTrue(samples.isEmpty())
    }

    @Test
    fun everySetOfASessionCollapsesIntoOneTrainingDay() {
        val name = WorkoutTemplates.getAllExerciseNames()
            .first { OverallLevelCalculator.isStrengthExercise(it) }
        val first = now - 3 * oneDay
        val logged = listOf(
            loggedSet(name, first, reps = 8),
            loggedSet(name, first + 90_000, reps = 8),
            loggedSet(name, first + 180_000, reps = 8)
        )

        val samples = OverallLevelCalculator.qualifyingSamples(logged, levelFor = { 4 })

        assertEquals(1, samples.size)
        assertEquals(first + 180_000, samples.first().lastDoneAt)
    }

    private fun loggedSet(
        exerciseName: String,
        timestamp: Long,
        reps: Int = 10
    ) = LoggedSetEntity(
        timestamp = timestamp,
        dayOfWeek = "MONDAY",
        exerciseName = exerciseName,
        muscleGroup = "Chest",
        setNumber = 1,
        weightKg = 20.0,
        reps = reps
    )
}
