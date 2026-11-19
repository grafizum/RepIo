package com.grafizum.repio

import com.grafizum.repio.data.OverallLevelCalculator
import com.grafizum.repio.data.OverallLevelRank
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE RANK MUST SURVIVE A REBUILT LADDER.
 *
 * Owner requirement 2026-10-08, after the level minimums and maximums were
 * re-authored: "verify the overall rank that it's adapted correctly since we made
 * min and max changes (it should automatically adapt so that if i made all my
 * exercises at level 1 it will display lowest rank and if i did them all at max
 * level it displays max rank (with a small buffer before max max) and of course
 * everything in between)."
 *
 * It adapts because the calculator normalises against each exercise's OWN ladder
 * - `percent = (level - minLevel) / (maxLevel - minLevel) x 100` - rather than
 * against a raw level, so a rebuilt ceiling moves every rung's meaning with it and
 * the two ends stay pinned at 0% and 100%. This test pins those ends against the
 * REAL engine bounds, so re-authoring the data can never silently desync the rank.
 */
class OverallRankAdaptsToLadderTest {

    private val now = 1_800_000_000_000L

    private fun samplesAt(level: Int, max: Int) =
        (1..6).map { OverallLevelCalculator.Sample("Exercise $it", level, now, 1, max) }

    private fun rankAt(percent: Double) =
        OverallLevelRank.of(percent)!!.rank

    @Test
    fun everyExerciseOnItsOwnFirstRungIsTheLowestRank() {
        val result = OverallLevelCalculator.compute(samplesAt(level = 1, max = 40), now)
        assertEquals(0.0, result.level!!, 0.0001)
        assertEquals(OverallLevelRank.Letter.entries.first(), rankAt(result.level!!))
    }

    @Test
    fun everyExerciseAtItsOwnCeilingIsTheTopRank() {
        val result = OverallLevelCalculator.compute(samplesAt(level = 40, max = 40), now)
        assertEquals(100.0, result.level!!, 0.0001)
        assertEquals(OverallLevelRank.Letter.entries.last(), rankAt(result.level!!))
    }

    @Test
    fun theTopRankHasABufferSoItDoesNotNeedExactlyOneHundredPercent() {
        val top = OverallLevelRank.Letter.entries.last()
        assertTrue(
            "the top tier must start below 100%, so a maxed-out program is not the " +
                "only way to reach it (it starts at ${top.minPercent})",
            top.minPercent < OverallLevelRank.SCALE_CEILING
        )
    }

    @Test
    fun theRankNeverFallsAsLevelsRiseThroughTheSameLadder() {
        var previous = -1
        for (level in 1..40) {
            val percent = OverallLevelCalculator.compute(samplesAt(level, 40), now).level!!
            val ordinal = rankAt(percent).ordinal
            assertTrue(
                "rank went DOWN from level ${level - 1} to $level ($previous -> $ordinal)",
                ordinal >= previous
            )
            previous = ordinal
        }
    }

    @Test
    fun theTwoEndsHoldOnARealRebuiltLadder() {
        // End to end against the shipped data: Barbell Curl was one of the rows whose
        // floor (20 -> 10 kg) and ceiling (60 -> 40 kg) were both re-authored.
        val entry = ProgressionEngine.entryFor("Barbell Curl")!!
        val min = ProgressionEngine.minLevel(entry)
        val max = ProgressionEngine.effectiveMaxLevel(entry)
        assertTrue("a rebuilt ladder must still be climbable (min=$min max=$max)", max > min)

        val atFloor = (1..6).map {
            OverallLevelCalculator.Sample("Barbell Curl", min, now, min, max)
        }
        val atCeiling = (1..6).map {
            OverallLevelCalculator.Sample("Barbell Curl", max, now, min, max)
        }
        assertEquals(0.0, OverallLevelCalculator.compute(atFloor, now).level!!, 0.0001)
        assertEquals(100.0, OverallLevelCalculator.compute(atCeiling, now).level!!, 0.0001)
    }
}
