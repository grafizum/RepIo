package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The welcome screen's days-per-week menu lays out a WEEK by itself now
 * ([WorkoutTemplates.premadeWeekFor]), and the owner asked for those weeks to come from
 * "the usual professional sources" rather than from whatever fills the days in order.
 *
 * What the sources actually constrain is the SHAPE of the week, and that is what this test
 * pins - not the particular days, which are a choice:
 *
 *  - never more than three consecutive training days, for every frequency that can have a
 *    pause at all (3-on / 1-off / 2-on / the owner's own example for 5 days). 7 days is
 *    the exception the rule cannot cover, and it is the reason 7 is flagged instead;
 *  - the pattern ends at rest, so a week never runs its last first days into the next
 *    week's first session;
 *  - days are unique and in week order, because every downstream consumer (the chips, the
 *    generator, the routine rows) sorts by the same order;
 *  - 3 days stays MON/WED/FRI. That one is not a preference: it is the pattern this screen
 *    already shipped, the one the generator's own tests are written against, and the
 *    textbook full-body week.
 *
 * The 1 and 7 day choices are asserted to exist and to be FLAGGED rather than refused -
 * the owner asked for the label "(not recommended)", which only makes sense if the choice
 * can still be made.
 */
class PremadeWeekTest {

    private val order = WorkoutTemplates.WEEK_DAY_ORDER

    private fun consecutiveRuns(days: List<String>): List<Int> {
        val indices = days.map { order.indexOf(it) }.sorted()
        if (indices.isEmpty()) return emptyList()
        val runs = mutableListOf<Int>()
        var run = 1
        indices.zipWithNext { a, b ->
            if (b == a + 1) run++ else {
                runs.add(run); run = 1
            }
        }
        runs.add(run)
        return runs
    }

    @Test
    fun everyFrequencyGetsAWeekOfExactlyThatManyDays() {
        for (count in 1..7) {
            val week = WorkoutTemplates.premadeWeekFor(count)
            assertEquals("$count days must plan $count days", count, week.size)
            assertEquals("$count days must be unique", count, week.distinct().size)
            assertEquals("$count days must be in week order", week, week.sortedBy { order.indexOf(it) })
            assertEquals(week, WorkoutTemplates.premadeWeekFor(count))
            assertEquals(
                "the day count and the pattern must agree",
                week.size,
                WorkoutTemplates.premadeWeekDayCount(count)
            )
        }
        // Out of range is not an error state: it lands on the shipped 3-day week.
        assertEquals(WorkoutTemplates.premadeWeekFor(3), WorkoutTemplates.premadeWeekFor(0))
        assertEquals(WorkoutTemplates.premadeWeekFor(3), WorkoutTemplates.premadeWeekFor(9))
    }

    @Test
    fun theDefaultThreeDayWeekIsUnchanged() {
        assertEquals(listOf("MONDAY", "WEDNESDAY", "FRIDAY"), WorkoutTemplates.premadeWeekFor(3))
    }

    @Test
    fun noRecommendedWeekAsksForMoreThanThreeDaysInARow() {
        // 7 is deliberately OUT of this rule rather than exempted by accident: a week that
        // trains every day cannot contain a pause, which is exactly why the screen flags it
        // as not recommended. Asserting the rule over 1..6 and asserting the flag for 7 is
        // the honest pair - asserting the rule over 7 would only prove the test wrong.
        for (count in 1..6) {
            val longest = consecutiveRuns(WorkoutTemplates.premadeWeekFor(count)).max()
            assertTrue(
                "$count days asks for $longest consecutive training days",
                longest <= 3
            )
        }
        assertTrue(WorkoutTemplates.isNotRecommendedFrequency(7))
        assertEquals(7, consecutiveRuns(WorkoutTemplates.premadeWeekFor(7)).max())
    }

    @Test
    fun everyFrequencyThatNeedsAPauseHasOne() {
        // 4, 5 and 6 days have room for a pause inside the week, and the sources' rule
        // (never more than three in a row) means they must use it.
        for (count in 4..6) {
            val runs = consecutiveRuns(WorkoutTemplates.premadeWeekFor(count))
            assertTrue("$count days has no internal pause: $runs", runs.size >= 2)
            assertTrue("$count days runs $runs", runs.max() <= 3)
        }
    }

    @Test
    fun fiveDaysIsThePausePatternTheOwnerNamed() {
        // "5 days is 3 days workout 1 pause 2 workout 1 pause": MON/TUE/WED, pause THU,
        // FRI/SAT, pause SUN - three consecutive, then two.
        val week = WorkoutTemplates.premadeWeekFor(5)
        assertEquals(
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "FRIDAY", "SATURDAY"),
            week
        )
        assertEquals(listOf(3, 2), consecutiveRuns(week))
    }

    @Test
    fun theTwoDayWeekSpreadsBothSessionsApart() {
        val week = WorkoutTemplates.premadeWeekFor(2)
        val gap = order.indexOf(week[1]) - order.indexOf(week[0])
        assertTrue("the two sessions are $gap days apart - too close to recover", gap >= 2)
        // Both are inside the working week, so neither lands on a weekend by accident.
        assertFalse(week.contains("SATURDAY") || week.contains("SUNDAY"))
    }

    @Test
    fun onlyOneAndSevenDaysAreFlagged() {
        assertTrue(WorkoutTemplates.isNotRecommendedFrequency(1))
        assertTrue(WorkoutTemplates.isNotRecommendedFrequency(7))
        for (count in 2..6) {
            assertFalse(
                "$count days is inside the recommended band",
                WorkoutTemplates.isNotRecommendedFrequency(count)
            )
        }
    }
}
