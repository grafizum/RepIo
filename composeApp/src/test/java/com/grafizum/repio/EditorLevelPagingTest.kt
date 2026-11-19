package com.grafizum.repio

import com.grafizum.repio.ui.screens.EDITOR_LEVEL_PAGE
import com.grafizum.repio.ui.screens.levelPageRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WHICH RUNGS THE EXERCISE EDITOR'S LEVEL LIST PUTS ON ONE SCREEN.
 *
 * Owner, 2026-10-09: *"Editing a level exercise should show 15 levels at one with min
 * 1 level so dont hide level 1 only when going from 15+ to 30 as a second screen"*.
 *
 * The list used to hold a three-each-way WINDOW around the current rung, so an
 * exercise sitting on level 20 opened on 17..23 and level 1 was not on the screen at
 * all - reachable only through "See every level", which is not the list. The pages are
 * now aligned from level 1 (1..15, 16..30, ...), because "dont hide level 1" is a
 * statement about which rungs the first screen contains, and the next fifteen are the
 * second screen.
 *
 * These cases pin the rule rather than the drawing: the page decides what a user can
 * CHOOSE, and a page that quietly drops a rung makes a level reachable only through
 * another screen, which is exactly the bug this replaced.
 */
class EditorLevelPagingTest {

    @Test
    fun thePageSizeIsTheNumberTheOwnerAskedFor() {
        assertEquals("fifteen levels at once", 15, EDITOR_LEVEL_PAGE)
    }

    /** The first screen starts at level 1 whatever rung the exercise is on. */
    @Test
    fun theFirstPageAlwaysStartsAtLevelOne() {
        for (current in listOf(1, 2, 7, 14, 15)) {
            assertEquals(
                "an exercise on level $current opens on page one",
                1..15,
                levelPageRange(currentLevel = current, maxLevel = 30),
            )
        }
    }

    /** Going past the fifteenth rung is the second screen, exactly as asked. */
    @Test
    fun goingFromTheFifteenthToTheThirtiethIsTheSecondScreen() {
        for (current in listOf(16, 20, 29, 30)) {
            assertEquals(
                "an exercise on level $current opens on the second screen",
                16..30,
                levelPageRange(currentLevel = current, maxLevel = 30),
            )
        }
    }

    /**
     * A page REQUEST (the list's own next/previous control) wins over the current
     * level, and is aligned to its page boundary rather than trusted: paging must land
     * on whole blocks, or the blocks would slide and some rung would be skipped.
     */
    @Test
    fun aRequestedPageIsAlignedToItsBoundary() {
        assertEquals("the next block", 16..30, levelPageRange(12, 40, requestedStart = 16))
        assertEquals("a start mid-block snaps back", 1..15, levelPageRange(12, 40, requestedStart = 5))
        assertEquals("the third block", 31..45, levelPageRange(12, 60, requestedStart = 31))
    }

    /** A ladder shorter than a page simply shows all of it - no paging, no blank rows. */
    @Test
    fun aLadderShorterThanAPageShowsWhole() {
        assertEquals(1..12, levelPageRange(currentLevel = 4, maxLevel = 12))
        assertEquals(1..1, levelPageRange(currentLevel = 1, maxLevel = 1))
    }

    /** No page may start below level 1, or end past the ladder's own ceiling. */
    @Test
    fun aPageNeverLeavesTheLadder() {
        assertEquals("a zero level is level 1", 1..15, levelPageRange(0, 30))
        assertEquals("a negative level is level 1", 1..15, levelPageRange(-5, 30))
        assertEquals("a level past the end is the last page", 16..30, levelPageRange(99, 30))
        assertEquals("a ladder with no rungs", 1..1, levelPageRange(1, 0))
        assertEquals("a page that would run past the end is clamped", 16..21, levelPageRange(16, 21))
    }

    /**
     * THE SWEEP: paging forward from the first screen must tile the whole ladder
     * exactly once. This is what "only when going from 15+ to 30 as a second screen"
     * means as an invariant - one rung belongs to one page, and none is missing, so a
     * user who only ever pages never loses access to a level.
     */
    @Test
    fun consecutivePagesTileEveryLadderWithoutGapsOrOverlaps() {
        for (max in 1..40) {
            val visited = mutableListOf<Int>()
            var requestedStart = 0
            var guard = 0
            while (guard++ < 100) {
                val page = levelPageRange(
                    currentLevel = 1,
                    maxLevel = max,
                    requestedStart = requestedStart,
                )
                visited += page.toList()
                if (page.last >= max) break
                requestedStart = page.last + 1
            }
            assertEquals(
                "the pages of a $max-rung ladder must cover it exactly once",
                (1..max).toList(),
                visited,
            )
        }
    }
}
