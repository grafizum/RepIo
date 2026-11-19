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
 *
 * Since the v5 rebuild the canonical pool carries the progression database's
 * names (Push-Up, Barbell Back Squat, …). The localization tables were retired
 * with the old pool (clean slate, refillable later), so the localized-search
 * tests here assert the *mechanism* against a synthetic alias candidate rather
 * than specific translations.
 */
class ExerciseSearchTest {

    private val pool = listOf(
        "Barbell Bench Press",
        "Dumbbell Bench Press",
        "Bent-Over Barbell Row",
        "Dumbbell Bicep Curl",
        "Dumbbell Lateral Raise",
        "Push-Up",
        "Bench Dips",
        "Crunches",
        "Barbell Back Squat",
        "Pull-Up"
    )

    @Test
    fun exactNameComesFirst() {
        assertEquals("Dumbbell Bicep Curl", ExerciseSearch.search(pool, "Dumbbell Bicep Curl").first())
    }

    @Test
    fun missingLetterStillMatches() {
        assertTrue(ExerciseSearch.search(pool, "dumbell bicep").contains("Dumbbell Bicep Curl"))
    }

    @Test
    fun extraLetterStillMatches() {
        assertTrue(ExerciseSearch.search(pool, "dumbbbell bicep").contains("Dumbbell Bicep Curl"))
    }

    @Test
    fun swappedLettersStillMatch() {
        assertTrue(
            ExerciseSearch.search(pool, "bnech press").any { it.contains("Bench Press") }
        )
    }

    @Test
    fun wordOrderDoesNotMatter() {
        assertTrue(
            ExerciseSearch.search(pool, "press bench barbell").any { it == "Barbell Bench Press" }
        )
    }

    @Test
    fun prefixStillWorks() {
        assertTrue(ExerciseSearch.search(pool, "dumb").any { it == "Dumbbell Bicep Curl" })
    }

    @Test
    fun hyphenAndSpacingAreEquivalent() {
        assertTrue(ExerciseSearch.search(pool, "pushups").contains("Push-Up"))
        assertTrue(ExerciseSearch.search(pool, "pull ups").contains("Pull-Up"))
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
        // "Literal" means one of the exercise's names really contains the query:
        // a canonical name alone is too narrow now that an exercise also answers
        // to its translation and to the name it had before the generalization -
        // "Barbell Back Squat" also answers to "Barbell Back Squat".
        val results = ExerciseSearch.search(pool, "be")
        assertTrue("the query should match the bench presses", results.isNotEmpty())
        assertTrue(
            "a two-letter query must not reach an exercise with no such literal name",
            results.all { name ->
                ExerciseLocalization.allNamesFor(name).any { it.lowercase().contains("be") }
            }
        )
        // And it must not behave like a fuzzy matcher: nothing without the
        // literal reaches the results, so the pool stays small.
        assertTrue("two letters must not drag in the whole pool", results.size < pool.size)
    }

    @Test
    fun limitIsRespected() {
        assertTrue(ExerciseSearch.search(pool, "e", limit = 3).size <= 3)
    }

    @Test
    fun realPoolFindsCommonTypos() {
        val all = WorkoutTemplates.getAllExerciseNames()
        assertTrue(ExerciseSearch.search(all, "barbel squat").any { it.contains("Squat") })
        assertTrue(ExerciseSearch.search(all, "dumbell row").any { it.contains("Dumbbell Row") })
        assertTrue(ExerciseSearch.search(all, "dumbell curl").any { it.contains("Curl") })
        assertTrue(ExerciseSearch.search(all, "bnech press").any { it.contains("Bench Press") })
        assertTrue(ExerciseSearch.search(all, "pullup").any { it.contains("Pull-Up") })
        assertTrue(ExerciseSearch.search(all, "air bike").any { it == "Air Bike" })
    }

    // --- Localized search -----------------------------------------------------------------
    //
    // The add-exercise field searches the pool by canonical name *and* by every translated
    // name, so a German user typing "Kurzhantel-Curls" finds the exercise stored as
    // "Dumbbell Bicep Curl". The canonical name is what gets handed back, so picking a translated
    // suggestion can never fork an exercise into a second record. The rebuilt pool has no
    // translations yet (clean slate), so these tests use a synthetic German alias.

    private fun localizedCatalog(): List<ExerciseSearch.Candidate> =
        listOf(
            ExerciseSearch.Candidate(
                "Barbell Back Squat",
                aliases = listOf("Kniebeugen", "Hackenschmidt-Kniebeuge")
            ),
            ExerciseSearch.Candidate("Dumbbell Bicep Curl", aliases = listOf("Kurzhantel-Curls"))
        ) + WorkoutTemplates.getAllExerciseNames().map { ExerciseSearch.Candidate(it) }

    @Test
    fun germanNameFindsTheExerciseAndReturnsTheCanonicalName() {
        val results = ExerciseSearch.searchCandidates(localizedCatalog(), "Kniebeugen")
        assertEquals("Barbell Back Squat", results.first().canonical)
    }

    @Test
    fun germanTypoStillFindsTheExercise() {
        // Fuzzy matching must work on the translated name too, not just exact matches.
        val results = ExerciseSearch.searchCandidates(localizedCatalog(), "Kurzhantel Kurles")
        assertTrue(
            "expected Dumbbell Bicep Curl, got ${results.take(5).map { it.canonical }}",
            results.any { it.canonical == "Dumbbell Bicep Curl" }
        )
    }

    @Test
    fun englishStillWorksAlongsideTheTranslations() {
        val results = ExerciseSearch.searchCandidates(localizedCatalog(), "Dumbbell Bicep Curl")
        assertEquals("Dumbbell Bicep Curl", results.first().canonical)
        val partial = ExerciseSearch.searchCandidates(localizedCatalog(), "bench press")
        assertTrue(partial.any { it.canonical == "Barbell Bench Press" })
    }

    @Test
    fun translatedMatchesAreRankedAboveUnrelatedExercises() {
        val results = ExerciseSearch.searchCandidates(localizedCatalog(), "Kniebeugen")
        assertTrue("Kniebeugen should reach the squat family", results.isNotEmpty())
        assertEquals(
            "the alias hit must rank first",
            "Barbell Back Squat",
            results.first().canonical
        )
    }

    @Test
    fun blankAndUnknownQueriesBehaveTheSameWithAliases() {
        assertTrue(ExerciseSearch.searchCandidates(localizedCatalog(), "   ").isEmpty())
        assertTrue(ExerciseSearch.searchCandidates(localizedCatalog(), "zzzzqqq").isEmpty())
    }

    @Test
    fun limitIsRespectedWithAliases() {
        assertTrue(ExerciseSearch.searchCandidates(localizedCatalog(), "e", limit = 3).size <= 3)
    }

    @Test
    fun searchWithoutAliasesIsUnchangedByTheCandidateApi() {
        // The plain search() overload must keep behaving exactly as before it gained aliases.
        assertEquals(
            ExerciseSearch.search(pool, "dumbell bicep").first(),
            ExerciseSearch.searchCandidates(
                pool.map { ExerciseSearch.Candidate(it) },
                "dumbell bicep"
            ).first().canonical
        )
    }

    @Test
    fun emptyTranslationTablesLeaveSearchFullyEnglish() {
        // Integrity guard for the clean-slate state: with the localization tables
        // retired, every pool search is canonical-English — and must stay that way.
        val catalog = WorkoutTemplates.getAllExerciseNames().map { ExerciseSearch.Candidate(it) }
        assertTrue(ExerciseSearch.searchCandidates(catalog, "dumbell curl").any { it.canonical == "Dumbbell Bicep Curl" })
    }
}
