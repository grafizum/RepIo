package com.grafizum.repio

import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.OverallLevelCalculator
import com.grafizum.repio.data.OverallLevelRank
import com.grafizum.repio.data.WorkoutTemplates
import kotlin.math.ln
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the overall-level maths.
 *
 * The model changed on 2026-10-02 and BOTH halves of it are load-bearing here:
 *
 *  1. **Percent, not raw level.** Each exercise is normalised against its OWN
 *     ladder, because the pool's ladders run from 6 to 40 levels. Averaging raw
 *     levels compared a finished exercise to a barely-started one as equal.
 *  2. **Median, not mean.** The owner asked for the median, and it is the right
 *     statistic: one advanced lift among easy ones should not describe the user.
 *
 * The recency weight is kept inside the median (see `weightedMedian*` below), so
 * these tests also pin that a stale exercise cannot decide the outcome — the
 * half-life figures below are exact, not approximate, which is what makes the
 * weighted cases checkable by hand.
 */
class OverallLevelCalculatorTest {

    private val now = 1_700_000_000_000L
    private val oneDay = 24L * 60L * 60L * 1000L

    private fun daysAgo(days: Long): Long = now - days * oneDay

    /**
     * A sample with an EXPLICIT ladder length, which is what the model needs.
     * The [maxLevel] default equals the level (a one-rung ladder) would be a
     * degenerate reading of "100%", so tests that care about normalisation name
     * their ladder lengths.
     */
    private fun sample(
        name: String,
        level: Int,
        maxLevel: Int,
        at: Long = now,
        minLevel: Int = 1
    ) = OverallLevelCalculator.Sample(name, level, at, minLevel, maxLevel)

    /** Five exercises at the given levels, all on the same 20-level ladder, today. */
    private fun today(vararg levels: Int): List<OverallLevelCalculator.Sample> =
        levels.mapIndexed { index, level -> sample("Exercise $index", level, 20) }

    /**
     * A sample positioned to land on (approximately) [percent] of a [max]-rung
     * ladder. The level is rounded, so the resulting percent is the exact
     * quotient rather than the requested figure — the tests assert against
     * [percentOf] where that matters rather than against the input.
     */
    private fun atPercent(percent: Double, max: Int, name: String = "E", at: Long = now) =
        sample(name, Math.round(percent / 100.0 * (max - 1)).toInt(), max, at)

    // --- The recency weight (unchanged, still load-bearing) ------------------

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

    // --- Percent of the exercise's OWN ladder --------------------------------

    @Test
    fun theFirstRungIsZeroAndTheLastRungIsOneHundred() {
        assertEquals(0.0, OverallLevelCalculator.percentOf(sample("a", 1, 6)), 1e-12)
        assertEquals(100.0, OverallLevelCalculator.percentOf(sample("a", 6, 6)), 1e-12)
        assertEquals(0.0, OverallLevelCalculator.percentOf(sample("a", 1, 40)), 1e-12)
        assertEquals(100.0, OverallLevelCalculator.percentOf(sample("a", 40, 40)), 1e-12)
    }

    /**
     * THE defect the change exists to fix. The same raw level means completely
     * different things on a short and a long ladder, and under the old model both
     * scored the same number.
     */
    @Test
    fun theSameLevelOnAShortAndALongLadderAreNotTheSameProgress() {
        val onSix = OverallLevelCalculator.percentOf(sample("short", 5, 6))
        val onForty = OverallLevelCalculator.percentOf(sample("long", 5, 40))
        assertEquals("level 5 of 6 is nearly done", 80.0, onSix, 1e-9)
        assertEquals("level 5 of 40 has barely started", 10.256, onForty, 0.01)
        assertTrue("they must not score alike", onSix > onForty * 7)
    }

    @Test
    fun percentIsClampedRatherThanExtrapolated() {
        // The level picker offers rungs above the ladder's declared top; without a
        // clamp those would report over 100% and let one exercise exceed SS+.
        assertEquals(100.0, OverallLevelCalculator.percentOf(sample("a", 99, 6)), 1e-12)
        assertEquals(0.0, OverallLevelCalculator.percentOf(sample("a", 0, 6)), 1e-12)
        assertEquals(0.0, OverallLevelCalculator.percentOf(sample("a", -3, 6)), 1e-12)
    }

    @Test
    fun aOneRungLadderCountsAsCompleteRatherThanHoldingTheMedianDown() {
        // There is nothing to climb, so calling it 0% would report a finished
        // movement as untrained and drag the whole figure down forever.
        assertEquals(100.0, OverallLevelCalculator.percentOf(sample("a", 1, 1)), 1e-12)
        assertEquals(100.0, OverallLevelCalculator.percentOf(sample("a", 3, 2)), 1e-12)
    }

    @Test
    fun aNonStandardFirstRungIsRespected() {
        // minLevel is 1 for every pool row today, but the sample carries it so a
        // future ladder that starts above 1 normalises against its own first rung.
        val s = sample("a", 6, 10, minLevel = 5)
        assertEquals(20.0, OverallLevelCalculator.percentOf(s), 1e-9)
    }

    // --- The median ----------------------------------------------------------

    @Test
    fun fewerThanFiveExercisesIsNotReady() {
        for (count in 0..4) {
            val result = OverallLevelCalculator.compute(
                List(count) { sample("Exercise $it", 5, 20) },
                now
            )
            assertFalse("$count exercises must not unlock the level", result.ready)
            assertNull(result.level)
            assertEquals(count, result.exerciseCount)
        }
    }

    @Test
    fun fiveExercisesHalfwayUpTheirLadderScoreFifty() {
        val result = OverallLevelCalculator.compute(
            List(5) { sample("Exercise $it", 11, 21) },
            now
        )
        assertTrue(result.ready)
        assertEquals(50.0, result.level!!, 1e-12)
        assertEquals(5, result.exerciseCount)
    }

    @Test
    fun theMedianIsTheMiddleExerciseNotTheMean() {
        // 10 · 20 · 30 · 40 · 100. The mean is 40; the median is 30. A program
        // with one very advanced lift is described by its middle exercise.
        val percents = listOf(10.0, 20.0, 30.0, 40.0, 100.0)
        val samples = percents.mapIndexed { i, p -> atPercent(p, 20, "E$i") }
        assertEquals(26.3, OverallLevelCalculator.compute(samples, now).level!!, 0.05)
    }

    @Test
    fun oneAdvancedExerciseCannotDescribeTheWholeProgram() {
        // The case the median exists for: a weighted pull-up or a pistol squat
        // sitting alongside eight easy movements. The mean would read this as a
        // strong intermediate; the median says what the user mostly does.
        val percents = listOf(5.0, 8.0, 10.0, 12.0, 100.0, 100.0, 100.0, 100.0)
        val samples = percents.mapIndexed { i, p -> atPercent(p, 20, "E$i") }
        // On a 20-rung ladder these land at 0 · 5.3 · 5.3 · 5.3 · 94.7 ×4. The lower
        // middle of eight is the fourth, so the four advanced lifts at the top
        // are simply outvoted by the easy work the user mostly does.
        assertEquals(5.3, OverallLevelCalculator.compute(samples, now).level!!, 0.05)
    }

    @Test
    fun theResultIsAlwaysOneOfTheInputPercents() {
        // The defining property of a median: it can never invent a value the user
        // did not actually train. Swept across uneven, duplicated and sorted sets.
        val inputs = listOf(
            listOf(3.0, 91.0, 47.0, 12.0, 88.0),
            listOf(50.0, 50.0, 50.0, 50.0, 50.0),
            listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0),
            listOf(99.0, 100.0, 2.0, 64.0, 33.0, 71.0, 8.0)
        )
        inputs.forEach { percents ->
            val samples = percents.mapIndexed { i, p -> atPercent(p, 20, "E$i") }
            val level = OverallLevelCalculator.compute(samples, now).level!!
            // Compared against the ACTUAL percentages the rungs produce, not the
            // requested ones — [atPercent] rounds onto a rung, so a requested 50%
            // on a 20-rung ladder really is 47.4% and the median must be that.
            val actual = samples.map { OverallLevelCalculator.percentOf(it) }
            assertTrue(
                "median $level is not one of $actual",
                actual.any { Math.abs(it - level) < 0.05 }
            )
        }
    }

    @Test
    fun anEvenCountTakesTheLowerMiddleValue() {
        // The ordinary median definition, erring toward the user being slightly
        // less advanced than they are — the safer direction for a stat that gates
        // an achievement letter.
        //
        // Six samples, not four: the calculator refuses to report anything below
        // the five-exercise floor, so the even-count behaviour has to be shown on
        // a set that can actually reach the median.
        val percents = listOf(5.0, 10.0, 20.0, 30.0, 40.0, 50.0)
        val samples = percents.mapIndexed { i, p -> atPercent(p, 20, "E$i") }
        // These rungs land at 0 · 5.3 · 15.8 · 26.3 · 36.8 · 47.4. The running
        // weight crosses half (3.0) at the THIRD, so the lower of the two middle
        // values is taken — not the 26.3 an averaging median would report.
        assertEquals(15.8, OverallLevelCalculator.compute(samples, now).level!!, 0.05)
    }

    @Test
    fun theResultIsAlwaysAFractionOfTheScale() {
        val samples = listOf(1.0, 100.0, 55.0, 22.0, 78.0).mapIndexed { i, p ->
            atPercent(p, 40, "E$i")
        }
        val level = OverallLevelCalculator.compute(samples, now).level!!
        assertTrue("median $level left the 0-100 scale", level in 0.0..100.0)
    }

    // --- Recency folded into the median --------------------------------------

    @Test
    fun aStaleExerciseKeepsItsPlaceButLosesItsVote() {
        // Five exercises at 10/20/30/40/50, all trained 90 days ago: weights are
        // all 0.125, so they cancel and the plain median of 30 stands. This is
        // the "uniform gap leaves the answer alone" property.
        val samples = listOf(10.0, 20.0, 30.0, 40.0, 50.0).mapIndexed { i, p ->
            atPercent(p, 20, "E$i", at = daysAgo(90))
        }
        // 5.3 · 15.8 · 26.3 · 36.8 · 47.4 — the middle of the five.
        assertEquals(26.3, OverallLevelCalculator.compute(samples, now).level!!, 0.05)
    }

    @Test
    fun oneFreshExerciseOutweighsFourStaleOnes() {
        // The decay doing real work. Four exercises last trained 90 days ago sit
        // at 6.9% of a 30-rung ladder (weight 0.125 each); one trained today sits
        // at 93.1% (weight 1.0). Sorted, the four stale ones hold 0.5 of the
        // 1.5 total weight — still under half — so the answer follows the fresh
        // one. Recency moves the result in BOTH directions, which is what
        // "an abandoned lift must not prop the number up" needs.
        val stale = List(4) { sample("Stale $it", 3, 30, at = daysAgo(90)) }
        val samples = stale + sample("Fresh", 28, 30)
        val result = OverallLevelCalculator.compute(samples, now)
        assertTrue(result.ready)
        assertEquals(93.103, result.level!!, 0.01)
    }

    @Test
    fun aStaleHighExerciseCannotLiftTheMedian() {
        // The mirror case, and the one that would have failed under a mean: a
        // long-advanced lift nobody has touched in 90 days must not describe a
        // user who is currently working on their basics.
        val stale = listOf(
            sample("Stale advanced", 40, 40, at = daysAgo(120)),
            sample("Stale advanced 2", 38, 40, at = daysAgo(120))
        )
        val fresh = listOf(
            sample("Fresh 1", 3, 30),
            sample("Fresh 2", 4, 30),
            sample("Fresh 3", 2, 30),
            sample("Fresh 4", 5, 30),
            sample("Fresh 5", 3, 30)
        )
        val result = OverallLevelCalculator.compute(stale + fresh, now)
        assertTrue(result.ready)
        // Weights: five at 1.0 and two at ~0.031. Sorted percents the fresh ones
        // dominate the lower half, so the median is one of THEM.
        assertTrue(
            "a stale advanced lift leaked into the median: ${result.level}",
            result.level!! < 15.0
        )
    }

    @Test
    fun theLevelIsRoundedToOneDecimal() {
        val samples = listOf(10.0, 20.0, 33.0, 40.0, 50.0).mapIndexed { i, p ->
            atPercent(p, 30, "E$i")
        }
        val result = OverallLevelCalculator.compute(samples, now)
        assertTrue(result.ready)
        // Any median of these is a value with no more precision than one decimal,
        // which is what the stored figure promises.
        assertEquals(result.level!!, Math.round(result.level!! * 10.0) / 10.0, 1e-12)
    }

    // --- Qualifying samples --------------------------------------------------

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

    /**
     * The sample's ladder length must come from the engine, not from the level.
     * A caller that passed `maxLevel = level` for everything would silently
     * report every exercise as 100% complete.
     */
    @Test
    fun eachSampleCarriesItsOwnCeilingAwareLadderLength() {
        val name = WorkoutTemplates.getAllExerciseNames()
            .first { OverallLevelCalculator.isStrengthExercise(it) }
        val entry = requireNotNull(
            com.grafizum.repio.data.ProgressionEngine.entryFor(name)
        )
        val expectedMax = com.grafizum.repio.data.ProgressionEngine.effectiveMaxLevel(entry)

        val samples = OverallLevelCalculator.qualifyingSamples(
            listOf(loggedSet(name, now)),
            levelFor = { 1 }
        )

        assertEquals(1, samples.size)
        assertEquals(expectedMax, samples.first().maxLevel)
        assertEquals(com.grafizum.repio.data.ProgressionEngine.minLevel(entry), samples.first().minLevel)
        // Level 1 of a real ladder is the bottom, not the top.
        assertEquals(0.0, OverallLevelCalculator.percentOf(samples.first()), 1e-12)
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

    // --- The whole chain, end to end ----------------------------------------

    /**
     * The figure the calculator produces has to be the one the rank table reads,
     * or the badge on Home would describe a different number than the one stored.
     */
    @Test
    fun theComputedFigureRanksTheWayTheRankTableSaysItDoes() {
        val samples = List(5) { sample("E$it", 20, 21) }
        val result = OverallLevelCalculator.compute(samples, now)
        assertTrue(result.ready)
        // 20 of 21 levels is 95.2%, which is the top band and nowhere else.
        assertEquals(95.0, result.level!!, 0.05)
        assertEquals("SS+", requireNotNull(OverallLevelRank.of(result.level)).letter)

        val beginner = List(5) { sample("B$it", 1, 21) }
        val low = OverallLevelCalculator.compute(beginner, now)
        assertEquals("E", requireNotNull(OverallLevelRank.of(low.level)).letter)
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