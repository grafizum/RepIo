package com.grafizum.repio

import com.grafizum.repio.data.ExerciseSearch
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the typo-tolerant exercise search behind the "SearchableExerciseField":
 * the exercise the user means must still surface when they drop, double or swap a
 * letter (or type words in a different order), while unrelated exercises stay out.
 */
class ExerciseSearchTest {

    private val pool = listOf(
        "Barbell Bench Presses",
        "Dumbbell Bench Presses",
        "Barbell Rows",
        "Dumbbell Curls",
        "Dumbbell Lateral Raises",
        "Push-ups",
        "Dips",
        "Crunches",
        "Barbell Squats",
        "Pull-ups"
    )

    @Test
    fun exactNameComesFirst() {
        assertEquals("Dumbbell Curls", ExerciseSearch.search(pool, "Dumbbell Curls").first())
    }

    @Test
    fun missingLetterStillMatches() {
        assertTrue(ExerciseSearch.search(pool, "dumbell curls").contains("Dumbbell Curls"))
    }

    @Test
    fun extraLetterStillMatches() {
        assertTrue(ExerciseSearch.search(pool, "dumbbbell curls").contains("Dumbbell Curls"))
    }

    @Test
    fun swappedLettersStillMatch() {
        assertTrue(
            ExerciseSearch.search(pool, "bnech presses").any { it.contains("Bench Presses") }
        )
    }

    @Test
    fun wordOrderDoesNotMatter() {
        assertTrue(
            ExerciseSearch.search(pool, "press bench barbell").any { it == "Barbell Bench Presses" }
        )
    }

    @Test
    fun prefixStillWorks() {
        assertTrue(ExerciseSearch.search(pool, "dumb").any { it == "Dumbbell Curls" })
    }

    @Test
    fun hyphenAndSpacingAreEquivalent() {
        assertTrue(ExerciseSearch.search(pool, "pushups").contains("Push-ups"))
        assertTrue(ExerciseSearch.search(pool, "pull ups").contains("Pull-ups"))
    }

    @Test
    fun nonsenseQueryReturnsNothing() {
        assertTrue(ExerciseSearch.search(pool, "zzzzqqq").isEmpty())
    }

    @Test
    fun blankQueryReturnsNothing() {
        assertTrue(ExerciseSearch.search(pool, "   ").isEmpty())
    }

    @Test
    fun shortQueriesDoNotFuzzyMatch() {
        // Two-letter queries must stay literal so they don't drag in the whole pool.
        val results = ExerciseSearch.search(pool, "ab")
        assertTrue(results.all { it.lowercase().contains("ab") })
    }

    @Test
    fun limitIsRespected() {
        assertTrue(ExerciseSearch.search(pool, "e", limit = 3).size <= 3)
    }

    @Test
    fun realPoolFindsCommonTypos() {
        val all = WorkoutTemplates.getAllExerciseNames()
        assertTrue(ExerciseSearch.search(all, "barbel squat").any { it.contains("Squat") })
        assertTrue(ExerciseSearch.search(all, "dumbell row").any { it.contains("Row") })
        assertTrue(ExerciseSearch.search(all, "dumbell curl").any { it.contains("Curl") })
        assertTrue(ExerciseSearch.search(all, "bnech press").any { it.contains("Bench Press") })
        assertTrue(ExerciseSearch.search(all, "pullup").any { it.contains("Pull-up") })
    }
}
