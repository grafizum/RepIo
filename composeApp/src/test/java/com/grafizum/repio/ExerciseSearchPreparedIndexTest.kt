package com.grafizum.repio

import com.grafizum.repio.data.ExerciseLocalization
import com.grafizum.repio.data.ExerciseSearch
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The prepared search index must be an OPTIMISATION and nothing else.
 *
 * The owner asked whether the whole exercise database gets loaded on every search.
 * It does not - search never touches Room; the pool is a compiled-in constant. But
 * measuring the matcher found real waste: every keystroke re-normalised all 2,624
 * searchable names (601 exercises plus de/it and pre-rename aliases) with
 * `lowercase()`, `compact()` and `split()`, none of which depend on the query.
 * Measured 5.26ms of a 7-10ms keystroke, on the main thread, per character typed.
 *
 * `PreparedIndex` hoists that work out. The risk is not that it breaks - it is that
 * it works *slightly differently*, which no crash or assertion would reveal. Search
 * is how a user finds an exercise at all; an index that quietly drops a translated
 * match, or reorders two equally-good hits, is far worse than the 5ms it saved.
 *
 * So this asserts EQUALITY, not "it works". Every query below is compared as an
 * ordered list of names, because the order IS the ranking.
 */
class ExerciseSearchPreparedIndexTest {

    private val pool = WorkoutTemplates.getAllExerciseNames()
    private val candidates =
        pool.map { ExerciseSearch.Candidate(it, ExerciseLocalization.allNamesFor(it)) }
    private val index = ExerciseSearch.PreparedIndex.build(candidates)

    /**
     * Queries chosen to hit every branch of the matcher, not to be representative.
     *
     * A representative sample would only prove the common path. These cover: empty
     * and whitespace (early return), below and at the fuzzy-length thresholds
     * (`allowedDistance` changes at 2/4/8 chars), exact and prefix matches, compact
     * vs. punctuated forms, transpositions, whole-word typos, multi-token queries,
     * a query matching only a TRANSLATION, a query matching only a PRE-RENAME name,
     * and a query that matches nothing at all.
     */
    private val queries = listOf(
        "",
        "   ",
        "b",
        "be",
        "ben",
        "benc",
        "bench",
        "bench press",
        "BENCH PRESS",
        "bnech press",
        "benchpres",
        "dumbell curl",
        "dumbbell curls",
        "knee",
        "squat",
        "kurzhantel curls",      // German translation only
        "curl con manubri",      // Italian translation only
        "smith machine squat",   // pre-generalization name only
        "leg press",
        "plank",
        "xyzzy",
        "z",
        "a",
        "dead bug",
        "lat pulldown",
        "overhead press barbell",
    )

    /** The whole point: identical names, identical order, for every query. */
    @Test
    fun thePreparedIndexReturnsExactlyWhatTheDirectSearchDoes() {
        queries.forEach { query ->
            val direct = ExerciseSearch.searchCandidates(candidates, query).map { it.canonical }
            val prepared = ExerciseSearch.searchPrepared(index, query).map { it.canonical }
            assertEquals(
                "prepared and direct search disagree for query '$query'.\n" +
                    "  direct:   ${direct.take(8)}\n" +
                    "  prepared: ${prepared.take(8)}",
                direct,
                prepared,
            )
        }
    }

    /** Guard against the comparison above passing because both sides return nothing. */
    @Test
    fun theQueriesActuallyProduceResults() {
        val withResults = queries.filter { query ->
            ExerciseSearch.searchCandidates(candidates, query).isNotEmpty()
        }
        assertTrue(
            "none of the probe queries matched anything, so the equality test above " +
                "would have passed vacuously",
            withResults.size >= 15,
        )
    }

    /**
     * The limit must still be honoured, and still be applied AFTER sorting.
     *
     * Taking the first N before sorting would quietly return the wrong N - a bug that
     * looks like a slightly wrong dropdown rather than an obvious failure.
     */
    @Test
    fun theLimitIsAppliedAfterRanking() {
        // "b" matches far more exercises than the limit allows.
        val atLimit = ExerciseSearch.searchPrepared(index, "b", limit = 5)
        val full = ExerciseSearch.searchPrepared(index, "b", limit = 40)
        assertEquals(5, atLimit.size)
        assertEquals(
            "a limited search must be a prefix of the unlimited one - that is what " +
                "'sorted then take' means",
            full.take(5),
            atLimit,
        )
    }

    /**
     * Building the index must be cheaper than the work it replaces, or it is not
     * worth having. The setup is query-independent, so it should be paid once.
     */
    @Test
    fun buildingTheIndexPaysForItself() {
        fun timeIt(block: () -> Unit): Long {
            block() // warm up, so JIT does not land in the measurement
            val start = System.nanoTime()
            repeat(5) { block() }
            return (System.nanoTime() - start) / 5 / 1_000_000
        }

        val build = timeIt { ExerciseSearch.PreparedIndex.build(candidates) }

        // One keystroke, both ways.
        val direct = timeIt {
            ExerciseSearch.searchCandidates(candidates, "bench press")
        }
        val prepared = timeIt {
            ExerciseSearch.searchPrepared(index, "bench press")
        }

        println(
            "build=${build}ms  directKeystroke=${direct}ms  preparedKeystroke=${prepared}ms"
        )
        assertTrue(
            "the prepared keystroke ($prepared ms) is not faster than the direct one " +
                "($direct ms); the index is pure overhead",
            prepared < direct,
        )
    }

    /**
     * An index built for one pool must not be silently reused for another.
     *
     * `PreparedIndex` holds the pool it was built from, so a caller passing a stale
     * index would get results from the wrong pool. This pins that the index reports
     * the pool it actually covers.
     */
    @Test
    fun theIndexCoversThePoolItWasBuiltFrom() {
        val small = candidates.take(5)
        val smallIndex = ExerciseSearch.PreparedIndex.build(small)
        assertEquals(5, smallIndex.all().size)
        assertEquals(pool.size, index.all().size)
        assertEquals(
            small.map { it.canonical },
            smallIndex.all().map { it.first().candidate.canonical },
        )
    }
}