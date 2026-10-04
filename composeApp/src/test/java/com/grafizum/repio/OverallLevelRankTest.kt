package com.grafizum.repio

import com.grafizum.repio.data.OverallLevelRank
import com.grafizum.repio.data.UserStatsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the letter tiers that sit on top of the overall level.
 *
 * The scale changed on 2026-10-02 from "raw average level" to "percent of the
 * whole range", and the owner fixed both ends of it: **0% is E** and **95%+ is
 * SS+**, with the seven tiers between them evenly spaced. What matters here is
 * that the boundary a user lands on is exactly that, that the spacing really is
 * even, and that an unrated user gets no badge rather than a fabricated one.
 *
 * The "95, not 100" is the part most likely to be "tidied up" later by someone
 * reading it as an off-by-one, so it is asserted explicitly.
 */
class OverallLevelRankTest {

    private fun rank(percent: Double) = OverallLevelRank.of(percent)

    private fun letter(percent: Double): String =
        requireNotNull(rank(percent)) { "percent $percent must map to a rank" }.letter

    /** 95 / 7 — the width of each of the seven bands between the eight tiers. */
    private val step = 95.0 / 7.0

    // --- The threshold table -------------------------------------------------

    @Test
    fun eachTierCoversItsEvenlySpacedBand() {
        // Both ends of every band, so an off-by-one in a threshold fails here
        // rather than in the app three weeks later.
        val labels = listOf("E", "D", "C", "B", "A", "S", "SS", "SS+")
        labels.forEachIndexed { i, tier ->
            val floor = i * step
            val justBelowNext = if (i == labels.lastIndex) 99.0 else (i + 1) * step - 0.01
            assertEquals("floor of $tier", tier, letter(floor))
            assertEquals("top of $tier", tier, letter(justBelowNext))
        }
    }

    @Test
    fun theScaleStartsAtZeroAndEndsAtNinetyFive() {
        assertEquals("E owns zero", "E", letter(0.0))
        assertEquals("SS+ starts at 95", "SS+", letter(95.0))
        assertEquals("SS+ holds above 95", "SS+", letter(99.9))
        assertEquals(
            "the scale must stop at 95, not run to 100",
            95.0,
            OverallLevelRank.TOP_TIER_PERCENT,
            1e-9
        )
    }

    @Test
    fun ssIsStillReachableJustBelowTheTop() {
        // If the top band started too low, SS would be a tier nobody ever sees.
        assertEquals("SS", letter(step * 6))
        assertEquals("SS", letter(94.99))
    }

    @Test
    fun theBandsAreEvenlySpaced() {
        // The seven CLOSED bands are one step wide each. The top band is not: it
        // is open-ended and runs from 95 to the 100 the scale stops short of, so
        // it is the one deliberately-narrower-looking-but-actually-wider band and
        // is excluded here. That is the whole reason the scale stops at 95.
        val closed = OverallLevelRank.ORDERED.dropLast(1)
        closed.forEach { rank ->
            assertEquals("${rank.label} must be one step wide", step, rank.width, 1e-9)
        }
        val top = OverallLevelRank.ORDERED.last()
        assertEquals("SS+ starts at 95", 95.0, top.minPercent, 1e-9)
        assertEquals(
            "the top band runs to the end of the scale, wider than one step",
            5.0,
            top.width,
            1e-9
        )
    }

    @Test
    fun thresholdsAreStrictlyAscending() {
        val floors = OverallLevelRank.ORDERED.map { it.minPercent }
        assertEquals("floors must already be sorted", floors.sorted(), floors)
        assertEquals(
            "two tiers must not share a floor, got $floors",
            floors.distinct().size,
            floors.size
        )
    }

    @Test
    fun bandsAreContiguousWithNoGapOrOverlap() {
        // Each tier ends exactly where the next one begins: that is the range
        // label, and it is derived from the next floor rather than written out.
        OverallLevelRank.ORDERED.zipWithNext().forEach { (lower, upper) ->
            assertEquals(
                "${lower.label} must end where ${upper.label} begins",
                upper.minPercent,
                requireNotNull(lower.rangeEnd),
                1e-9
            )
        }
        assertNull("the top tier is open-ended", OverallLevelRank.ORDERED.last().rangeEnd)
    }

    @Test
    fun theTopTierIsTheOnlyOpenOne() {
        assertEquals("SS+", OverallLevelRank.ORDERED.last().label)
        assertTrue(requireNotNull(rank(95.0)).isTop)
        assertNull("the top tier has no next rank", requireNotNull(rank(95.0)).nextLetter)
        assertEquals(1.0, requireNotNull(rank(99.0)).progressToNext, 1e-9)
    }

    @Test
    fun everyTierIsVisuallyDistinct() {
        // The badge is the LETTER in the theme's primary colour — there is no
        // colour ramp behind it any more (the owner had the drawn crest removed),
        // so the letters themselves are the only thing telling two ranks apart.
        val letters = OverallLevelRank.ORDERED.map { it.label }
        assertEquals("letters must be one per rank, got $letters", letters.distinct().size, letters.size)
        assertEquals(listOf("E", "D", "C", "B", "A", "S", "SS", "SS+"), letters)
    }

    @Test
    fun everyTierCarriesItsOwnDescriptionKey() {
        val keys = OverallLevelRank.ORDERED.map { it.descriptionKey }
        keys.forEach { assertTrue("blank key in $keys", it.isNotBlank()) }
        assertEquals("descriptions must not be shared", keys.distinct().size, keys.size)
        // The dynamic getString bridge echoes an unknown key straight to the
        // screen, so the naming is pinned here and resolved in RankStringsTest.
        keys.forEach { assertTrue("unexpected key shape: $it", it.matches(Regex("rank_[a-z_]+_desc"))) }
    }

    // --- Progress to the next rank ------------------------------------------

    @Test
    fun progressIsTheFractionOfTheBandCovered() {
        // The halfway point of a band is halfway to the next rank.
        val mid = requireNotNull(rank(step * 1.5))
        assertEquals("D", mid.letter)
        assertEquals(0.5, mid.progressToNext, 1e-9)
        assertEquals("C", mid.nextLetter)

        assertEquals(0.0, requireNotNull(rank(step)).progressToNext, 1e-9)
        // 0.01 below the next floor is not exactly 100% through the band, and
        // asserting 1.0 there would be asserting a rounding the code does not do.
        val justUnder = requireNotNull(rank(step * 2 - 0.01))
        assertTrue(
            "just under the boundary must read as nearly complete, got ${justUnder.progressToNext}",
            justUnder.progressToNext > 0.99
        )
    }

    @Test
    fun progressIsAlwaysAFraction() {
        // Stepping by an INTEGER and deriving the percent, never by adding a
        // fraction to a running float. The accumulating form silently hangs:
        // `Math.round((2.3 + 0.05) * 10.0) / 10.0` is 23.499999999999996 ->
        // 23 -> 2.3, so the counter stops advancing at 2.3 and the loop never
        // ends. A sweep that hangs is a test that reports nothing, which is how
        // the whole suite sat at 0 failures while never finishing.
        for (tenths in 0..999) {
            val percent = tenths / 10.0
            val info = requireNotNull(rank(percent)) { "percent $percent must map" }
            assertTrue(
                "percent $percent gave progress ${info.progressToNext}",
                info.progressToNext in 0.0..1.0
            )
        }
    }

    @Test
    fun progressOnlyResetsWhenARankIsActuallyCrossed() {
        // Crossing a boundary is the only thing that may send progress back to
        // zero, and the tier must change on exactly that step.
        // Integer stepping again - see the note in progressIsAlwaysAFraction.
        // This is the loop that actually hung the suite: the +0.05 version
        // stalled at 2.3 forever.
        var previous = requireNotNull(rank(0.0))
        for (fiftieths in 1..1980) {
            val percent = fiftieths / 20.0
            val current = requireNotNull(rank(percent))
            if (current.progressToNext < previous.progressToNext) {
                assertTrue(
                    "progress fell at $percent without changing tier (${previous.letter})",
                    current.letter != previous.letter
                )
                // "Just entered the band", NOT "exactly on its floor". The bands
                // are 95/7 = 13.571... wide, so a 0.05 sweep step never lands on
                // a boundary: the first sample inside C is 13.6, which is 0.0286
                // past the floor and therefore 0.0021 of the way through the
                // band. Asserting an exact 0.0 would only pass on a sweep whose
                // step happened to divide 95/7, and it was wrong before this
                // loop ever ran - the old accumulating version hung at 2.3, so
                // this assertion had never been evaluated at all.
                // One step's worth of the band width is the honest bound.
                assertTrue(
                    "percent $percent is ${current.progressToNext} into ${current.letter}, " +
                        "further than one 0.05 sweep step past its floor",
                    current.progressToNext <= 0.05 / step
                )
            }
            previous = current
        }
    }

    // --- The unrated case ----------------------------------------------------

    @Test
    fun anUnratedLevelGetsNoRank() {
        // Below the five-exercise floor the calculator reports no level at all,
        // and handing out "E" would claim a measurement the app refuses to make.
        assertNull(OverallLevelRank.of(null as Double?))
        assertNull(OverallLevelRank.of(null as UserStatsEntity?))
    }

    @Test
    fun aCachedStatsRowIsRankedOnlyWhenReady() {
        val notReady = UserStatsEntity(
            overallStrengthLevel = 70.0,
            overallLevelReady = false,
            exercisesCountForLevel = 3
        )
        assertNull("an unready row must not be ranked", OverallLevelRank.of(notReady))

        val ready = notReady.copy(overallLevelReady = true)
        val info = OverallLevelRank.of(ready)
        assertNotNull(info)
        assertEquals("S", info!!.letter)
    }

    /**
     * The whole point of the change: a figure that used to mean "level 7.0" on a
     * 1-12 scale must not still be read the same way now that the scale is
     * 0-100. 70% is a strong S, and under the old thresholds 7.0 was also an S,
     * so this pins the coincidence away deliberately.
     */
    @Test
    fun theScaleIsPercentNotTheOldRawLevel() {
        assertEquals("0 is E, not an unrated user", "E", letter(0.0))
        // A user who has just started sits at the bottom, which the old scale
        // could not express: level 1.0 was the LOWEST possible raw level and
        // also the E floor, so there was no room below a starter.
        assertEquals("a beginner's figure ranks E", "E", letter(4.0))
        assertEquals("a finished ladder is the top band", "SS+", letter(100.0))
    }
}