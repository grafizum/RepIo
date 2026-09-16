package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rep guidance is SOFT: a range outside the recommendation must never block
 * saving — it only drives an info chip in the add/edit dialogs. These tests
 * pin the pure classifier the chip reads.
 */
class RepRangeGuidanceTest {

    @Test
    fun insideTheRecommendedBandIsNotFlagged() {
        // Dumbbell Squats recommends 8–12.
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("Dumbbell Squats", "8-12"))
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("Dumbbell Squats", "10"))
    }

    @Test
    fun partialOverlapIsNotFlagged() {
        // The typed band overlaps the recommendation, so part of the work
        // happens in the optimal range — no nudge.
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("Dumbbell Squats", "8-20"))
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("Dumbbell Squats", "6-12"))
    }

    @Test
    fun fullyOutsideIsFlagged() {
        assertTrue(WorkoutTemplates.isOutsideRecommendedRepRange("Dumbbell Squats", "20-30"))
        assertTrue(WorkoutTemplates.isOutsideRecommendedRepRange("Dumbbell Squats", "1-3"))
    }

    @Test
    fun garbageAndBlanksNeverFlag() {
        // Unparseable input stays the field-validation problem, not the
        // recommendation chip's.
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("Dumbbell Squats", ""))
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("Dumbbell Squats", "abc"))
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("", "8-12"))
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("Unknown Custom Exercise", "8-12"))
    }

    @Test
    fun timeBasedExercisesAreNeverFlagged() {
        assertTrue(WorkoutTemplates.isExerciseTimeBased("Planks"))
        assertFalse(WorkoutTemplates.isOutsideRecommendedRepRange("Planks", "5-8"))
    }

    @Test
    fun everyPoolExerciseHasAUsableRecommendation() {
        val pool = WorkoutTemplates.getAllExerciseNames()
        assertTrue("pool should not be empty", pool.isNotEmpty())
        pool.forEach { name ->
            val rec = WorkoutTemplates.getRecommendedRepsForExercise(name)
            assertTrue(
                "no recommendation for $name",
                rec.isNotBlank() && WorkoutTemplates.parseRepRange(rec) != null || rec.endsWith(" sec")
            )
            assertFalse(
                "a parsed recommendation must never classify as outside itself: $name -> $rec",
                !rec.endsWith(" sec") && WorkoutTemplates.isOutsideRecommendedRepRange(name, rec)
            )
        }
    }
}
