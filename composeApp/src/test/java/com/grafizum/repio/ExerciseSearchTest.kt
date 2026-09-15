package com.grafizum.repio

import com.grafizum.repio.data.ExerciseLocalization
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

    // --- Localized search -----------------------------------------------------------------
    //
    // The add-exercise field searches the pool by canonical name *and* by every translated
    // name, so a German user typing "Kurzhantel-Curls" finds the exercise stored as
    // "Dumbbell Curls". The canonical name is what gets handed back, so picking a translated
    // suggestion can never fork an exercise into a second record.

    private fun catalog(): List<ExerciseSearch.Candidate> =
        WorkoutTemplates.getAllExerciseNames().map { name ->
            ExerciseSearch.Candidate(name, ExerciseLocalization.allNamesFor(name))
        }

    @Test
    fun germanNameFindsTheExerciseAndReturnsTheCanonicalName() {
        val results = ExerciseSearch.searchCandidates(catalog(), "Kurzhantel-Curls")
        assertEquals("Dumbbell Curls", results.first().canonical)
    }

    @Test
    fun italianNameFindsTheExerciseAndReturnsTheCanonicalName() {
        val results = ExerciseSearch.searchCandidates(catalog(), "Curl con manubri")
        assertEquals("Dumbbell Curls", results.first().canonical)
    }

    @Test
    fun germanTypoStillFindsTheExercise() {
        // Fuzzy matching must work on the translated name too, not just exact matches.
        val results = ExerciseSearch.searchCandidates(catalog(), "Kurzhantel Kurles")
        assertTrue(
            "expected Dumbbell Curls, got ${results.take(5).map { it.canonical }}",
            results.any { it.canonical == "Dumbbell Curls" }
        )
    }

    @Test
    fun englishStillWorksAlongsideTheTranslations() {
        val results = ExerciseSearch.searchCandidates(catalog(), "Dumbbell Curls")
        assertEquals("Dumbbell Curls", results.first().canonical)
        val partial = ExerciseSearch.searchCandidates(catalog(), "bench press")
        assertTrue(partial.any { it.canonical == "Barbell Bench Presses" })
    }

    @Test
    fun translatedMatchesAreRankedAboveUnrelatedExercises() {
        val results = ExerciseSearch.searchCandidates(catalog(), "Kniebeugen")
        assertTrue("Kniebeugen should reach the squat family", results.isNotEmpty())
        assertTrue(
            "expected a squat, got ${results.take(3).map { it.canonical }}",
            results.take(3).any { it.canonical.contains("Squats") }
        )
    }

    @Test
    fun blankAndUnknownQueriesBehaveTheSameWithAliases() {
        assertTrue(ExerciseSearch.searchCandidates(catalog(), "   ").isEmpty())
        assertTrue(ExerciseSearch.searchCandidates(catalog(), "zzzzqqq").isEmpty())
    }

    @Test
    fun limitIsRespectedWithAliases() {
        assertTrue(ExerciseSearch.searchCandidates(catalog(), "e", limit = 3).size <= 3)
    }

    @Test
    fun searchWithoutAliasesIsUnchangedByTheCandidateApi() {
        // The plain search() overload must keep behaving exactly as before it gained aliases.
        assertEquals(
            ExerciseSearch.search(pool, "dumbell curls").first(),
            ExerciseSearch.searchCandidates(
                pool.map { ExerciseSearch.Candidate(it) },
                "dumbell curls"
            ).first().canonical
        )
    }
}
