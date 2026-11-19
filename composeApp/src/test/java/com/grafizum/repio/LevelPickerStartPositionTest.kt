package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.screens.levelPickerInitialIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards where the level picker OPENS, which is the whole of dashboard task #9:
 * "exercises below level 4 (starting level) are not even shown for the case that
 * level 4 is too high for the user."
 *
 * They were shown. The list opens on the current rung, and every weighted exercise
 * in the pool starts at level 4, so level 4 landed at the top of the viewport and
 * levels 1–3 were scrolled above the fold. That reads exactly like their being
 * absent — which is what the owner reported.
 *
 * The second half of the report ("some exercises still have a starting value which
 * is way too high") is the SAME defect seen from the data side: the CSV's Medium
 * rung is level 4 of every weighted ladder, which is 2.0–2.5x that row's own level
 * 1. The lower rungs are the answer, and they are now one tap away.
 */
class LevelPickerStartPositionTest {

    @Test
    fun theOpeningRowLeavesRungsAboveTheCurrentOneVisible() {
        // The real case: level 4 (index 3) with a full ladder. The list must open
        // high enough that level 1..3 are on screen, not at index 3.
        val openAt = levelPickerInitialIndex(currentIndex = 3, totalRows = 40)
        assertTrue(
            "opened at row $openAt, so the rungs below the start are off the top",
            openAt < 3
        )
        // Two rungs of lead-in: level 2 at the top, level 4 comfortably below it.
        assertEquals(1, openAt)
    }

    @Test
    fun aFirstSessionStartsAtTheTopBecauseThereIsNothingAboveIt() {
        assertEquals(0, levelPickerInitialIndex(currentIndex = 0, totalRows = 40))
    }

    @Test
    fun anUnknownCurrentRungFallsBackToTheTopRatherThanCrashing() {
        // -1 is what indexOfFirst returns when nothing is marked current.
        assertEquals(0, levelPickerInitialIndex(currentIndex = -1, totalRows = 40))
    }

    @Test
    fun anEmptyOrTinyLadderNeverOpensPastItsEnd() {
        assertEquals(0, levelPickerInitialIndex(currentIndex = 0, totalRows = 0))
        assertEquals(0, levelPickerInitialIndex(currentIndex = 5, totalRows = 1))
        assertEquals(0, levelPickerInitialIndex(currentIndex = 1, totalRows = 2))
    }

    @Test
    fun aRungNearTheTopOfALongLadderIsPinnedToTheTop() {
        assertEquals(0, levelPickerInitialIndex(currentIndex = 1, totalRows = 500))
    }

    /**
     * The premise the whole task rests on: the levels BELOW the prescribed start
     * really exist and are selectable. Without this, "open a bit higher" would be
     * scrolling into rows that prescribe nothing.
     */
    @Test
    fun everyWeightedExerciseHasSelectableRungsBelowItsStart() {
        var checked = 0
        for (name in WorkoutTemplates.getAllExerciseNames()) {
            val entry = ProgressionEngine.entryFor(name) ?: continue
            if (!ProgressionEngine.isProgressionReady(entry)) continue
            if (ProgressionEngine.valueKindOf(entry) != ProgressionEngine.ValueKind.LOAD) continue
            val start = ProgressionEngine.startingPrescription(
                name, ProgressionEngine.Difficulty.MEDIUM
            ) ?: continue
            // Level 1 is always reachable and always prescribes something.
            assertTrue(
                "$name has no prescription at level 1",
                ProgressionEngine.prescribedValueAtLevel(entry, 1) != null
            )
            if (start.level > 1) checked++
        }
        assertTrue("expected weighted exercises that start above level 1", checked > 50)
    }
}
