package com.grafizum.repio

import com.grafizum.repio.data.OverallLevelRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the letter tiers that sit on top of the overall level.
 *
 * The tiers are pure display — [com.grafizum.repio.data.OverallLevelCalculator]
 * is untouched — so what matters here is that the boundary a user lands on is
 * exactly the documented one, that the "X% to the next rank" figure is the
 * fraction of the tier actually covered, and that an unrated user gets no badge
 * rather than a fabricated one.
 */
class OverallLevelRankTest {

    private fun rank(level: Double) = OverallLevelRank.of(level)

    private fun letter(level: Double): String =
        requireNotNull(rank(level)) { "level $level must map to a rank" }.letter

    // --- The threshold table -------------------------------------------------

    @Test
    fun eachTierCoversItsDocumentedRange() {
        // Both ends of every band, so an off-by-one in a threshold fails here
        // rather than in the app three weeks later.
        val expected = listOf(
            0.0 to "E", 1.9 to "E",
            2.0 to "D", 2.9 to "D",
            3.0 to "C", 3.9 to "C",
            4.0 to "B", 4.9 to "B",
            5.0 to "A", 5.9 to "A",
            6.0 to "S", 6.9 to "S",
            7.0 to "SS", 7.9 to "SS",
            8.0 to "SS+", 12.0 to "SS+"
        )
        expected.forEach { (level, tier) ->
            assertEquals("level $level", tier, letter(level))
        }
    }

    @Test
    fun thresholdsAreStrictlyAscending() {
        val floors = OverallLevelRank.ORDERED.map { it.minLevel }
        assertEquals("floors must already be sorted", floors.sorted(), floors)
        assertEquals(
            "two tiers must not share a floor, got $floors",
            floors.distinct().size,
            floors.size
        )
    }

    @Test
    fun bandsAreContiguousWithNoGapOrOverlap() {
        // Each tier ends exactly 0.1 below the next one's floor: that is the
        // "6.0 – 6.9" label, and it is derived, not written out.
        OverallLevelRank.ORDERED.zipWithNext().forEach { (lower, upper) ->
            assertEquals(
                "${lower.label} must end just below ${upper.label}",
                upper.minLevel,
                requireNotNull(lower.rangeEnd) + 0.1,
                1e-9
            )
        }
        assertNull("the top tier is open-ended", OverallLevelRank.ORDERED.last().rangeEnd)
    }

    @Test
    fun theTopTierIsTheOnlyOpenOne() {
        assertEquals("SS+", OverallLevelRank.ORDERED.last().label)
        assertTrue(requireNotNull(rank(8.0)).isTop)
        assertNull("the top tier has no next rank", requireNotNull(rank(8.0)).nextLevel)
        assertNull(requireNotNull(rank(8.0)).nextLetter)
        assertEquals(1.0, requireNotNull(rank(9.9)).progressToNext, 1e-9)
    }

    @Test
    fun everyTierIsVisuallyDistinct() {
        // Two ranks sharing a metal would make the badge ambiguous — the whole
        // point of the letter is that it is readable at a glance.
        val tiers = OverallLevelRank.ORDERED.map { it.tier }
        assertEquals("tiers must be one per rank, got $tiers", tiers.distinct().size, tiers.size)

        val letters = OverallLevelRank.ORDERED.map { it.label }
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
    fun progressIsTheFractionOfTheTierCovered() {
        // S runs 6.0–6.9, so 6.4 is 40% of the way to SS at 7.0.
        val sixPointFour = requireNotNull(rank(6.4))
        assertEquals("S", sixPointFour.letter)
        assertEquals(0.4, sixPointFour.progressToNext, 1e-9)
        assertEquals("SS", sixPointFour.nextLetter)
        assertEquals(7.0, requireNotNull(sixPointFour.nextLevel), 1e-9)

        assertEquals(0.0, requireNotNull(rank(6.0)).progressToNext, 1e-9)
        assertEquals(0.9, requireNotNull(rank(6.9)).progressToNext, 1e-9)
    }

    @Test
    fun progressIsAlwaysAFraction() {
        var level = 0.0
        while (level <= 12.5) {
            val info = requireNotNull(rank(level)) { "level $level must map" }
            assertTrue(
                "level $level gave progress ${info.progressToNext}",
                info.progressToNext in 0.0..1.0
            )
            level = Math.round((level + 0.1) * 10.0) / 10.0
        }
    }

    @Test
    fun progressOnlyResetsWhenARankIsActuallyCrossed() {
        // Crossing a boundary is the only thing that may send progress back to
        // zero, and the tier must change on exactly that step.
        var level = 0.0
        var previous = requireNotNull(rank(level))
        while (level < 9.0) {
            level = Math.round((level + 0.1) * 10.0) / 10.0
            val current = requireNotNull(rank(level))
            if (current.progressToNext < previous.progressToNext) {
                assertTrue(
                    "progress fell at $level without changing tier (${previous.letter})",
                    current.letter != previous.letter
                )
                assertEquals("level $level must start its tier", 0.0, current.progressToNext, 1e-9)
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
        assertNull(OverallLevelRank.of(null as com.grafizum.repio.data.UserStatsEntity?))
    }

    @Test
    fun aCachedStatsRowIsRankedOnlyWhenReady() {
        val notReady = com.grafizum.repio.data.UserStatsEntity(
            overallStrengthLevel = 6.4,
            overallLevelReady = false,
            exercisesCountForLevel = 3
        )
        assertNull("an unready row must not be ranked", OverallLevelRank.of(notReady))

        val ready = notReady.copy(overallLevelReady = true)
        val info = OverallLevelRank.of(ready)
        assertNotNull(info)
        assertEquals("S", info!!.letter)
    }
}
