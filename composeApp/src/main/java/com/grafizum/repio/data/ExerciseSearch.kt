package com.grafizum.repio.data

/**
 * Typo-tolerant exercise name search.
 *
 * Plain `contains` matching fails as soon as the user mistypes a letter, doubles
 * one, drops one or swaps two — which is exactly what happens on a phone
 * keyboard. [search] ranks matches so the exact / prefix / substring hits still
 * come first, then falls back to an edit-distance comparison for near misses.
 *
 * Matching is intentionally conservative: short queries (<= 2 chars) are never
 * fuzzily matched, and the allowed edit distance grows with query length, so the
 * result list does not fill up with unrelated exercises.
 *
 * An exercise can also be searched under names other than the canonical English one it is
 * stored as — see [Candidate] — so a German or Italian user finds "Dumbbell Curls" by
 * typing "Kurzhantel-Curls" or "Curl con manubri".
 */
object ExerciseSearch {

    /** Maximum number of suggestions returned for one query. */
    const val DEFAULT_LIMIT = 40

    /**
     * One searchable exercise.
     *
     * @property canonical the name the database stores and the UI applies; this is what a
     *   selection hands back to the caller, so a translated query still saves the canonical
     *   name and cannot fork the exercise into two records.
     * @property aliases every other name the same exercise answers to (its translations).
     *   Matched exactly like [canonical], and the best of them ranks the exercise.
     */
    data class Candidate(val canonical: String, val aliases: List<String> = emptyList())

    /**
     * One name, pre-normalised for matching.
     *
     * WHY THIS EXISTS. The owner asked whether the whole exercise database is loaded
     * on every search, and the answer is that search never touches the database at
     * all - the pool is a compiled-in constant, and the matcher is pure in-memory
     * work over it. But measuring the matcher found real waste anyway.
     *
     * Every keystroke ran `lowercase()`, `compact()` and `split(NON_ALPHANUMERIC)`
     * over all 2,624 searchable names (601 exercises plus their de/it and
     * pre-rename aliases). None of that depends on the query. Measured on the JVM:
     * 5.26ms of a 7-10ms keystroke was that repeated normalisation, and building the
     * whole prepared set costs 20ms ONCE. On a phone those numbers are several
     * times worse, and this runs on the main thread inside composition, on every
     * character typed.
     *
     * So the query-independent half is hoisted into [PreparedIndex] and built once.
     * The query-dependent half (the comparisons and edit distances) is unchanged and
     * still runs per keystroke - it genuinely cannot be hoisted, since it depends on
     * what was typed.
     */
    data class Prepared(
        val candidate: Candidate,
        val lower: String,
        val compactName: String,
        val words: List<String>
    )

    /**
     * A candidate list with its normalisation already done.
     *
     * Build once per pool (it is `remember`-able on the pool list), then call
     * [searchPrepared] per keystroke. [searchCandidates] remains for callers that
     * search rarely and would not benefit from the setup.
     *
     * The ranking is identical to [searchCandidates] - see
     * `ExerciseSearchPreparedIndexTest`, which asserts the two agree on every query
     * in the pool, because an optimisation that quietly changed which exercises a
     * search surfaces would be far worse than the 5ms it saved.
     */
    class PreparedIndex private constructor(
        private val entries: List<List<Prepared>>
    ) {
        /**
         * Best (lowest) score for one exercise across its canonical name and aliases.
         *
         * Aliases come before the canonical name, exactly as [scoreCandidate] did, so
         * ties resolve identically.
         */
        fun scoreOf(
            prepared: List<Prepared>,
            query: String,
            compactQuery: String,
            tokens: List<String>
        ): Double? = prepared
            .asSequence()
            .mapNotNull {
                scoreAgainst(it.lower, it.compactName, it.words, query, compactQuery, tokens)
            }
            .minOrNull()

        /** Every exercise, in pool order, with its prepared names. */
        fun all(): List<List<Prepared>> = entries

        companion object {
            /** Precompute the normalisation for every name in [candidates]. */
            fun build(candidates: List<Candidate>): PreparedIndex =
                PreparedIndex(
                    candidates.map { candidate ->
                        // Aliases first, then canonical - the order [scoreCandidate] used.
                        (candidate.aliases + candidate.canonical).map { name ->
                            val lower = name.lowercase()
                            Prepared(
                                candidate = candidate,
                                lower = lower,
                                compactName = compact(lower),
                                words = lower.split(NON_ALPHANUMERIC).filter { it.isNotEmpty() }
                            )
                        }
                    }
                )
        }
    }

    /**
     * As [searchCandidates], over an index built once by [PreparedIndex.build].
     *
     * Same results, same order, same limit - it only skips re-normalising the names.
     */
    fun searchPrepared(
        index: PreparedIndex,
        query: String,
        limit: Int = DEFAULT_LIMIT
    ): List<Candidate> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val compactQuery = compact(trimmed)
        val tokens = trimmed.split(NON_ALPHANUMERIC).filter { it.isNotEmpty() }

        return index.all()
            .mapNotNull { prepared ->
                // Every Prepared in the list belongs to the same exercise, so the
                // head carries it - no positional lookup needed (an indexOf here
                // would be O(n^2) per keystroke and would break on duplicate lists).
                index.scoreOf(prepared, trimmed, compactQuery, tokens)
                    ?.let { prepared.first().candidate to it }
            }
            .sortedWith(compareBy({ it.second }, { it.first.canonical.lowercase() }))
            .map { it.first }
            .take(limit)
    }

    /**
     * Returns the exercise names matching [query], best match first. A blank
     * query yields an empty list (the search field shows nothing until typing).
     */
    fun search(
        allExercises: List<String>,
        query: String,
        limit: Int = DEFAULT_LIMIT
    ): List<String> = searchCandidates(
        // Aliases included, exactly as the pickers build them: a translated
        // name and a pre-generalization name ("Smith Machine Squat") are how
        // people still ask for an exercise, and this overload must not answer
        // a narrower set of queries than the UI does.
        allExercises.map { Candidate(it, ExerciseLocalization.allNamesFor(it)) },
        query,
        limit
    ).map { it.canonical }

    /**
     * As [search], but over [Candidate]s: an exercise matches when *any* of its names does,
     * and it is ranked by its best-matching name. Returning the candidates (rather than bare
     * strings) lets the caller keep showing the canonical name while the user typed a
     * translated one.
     */
    fun searchCandidates(
        candidates: List<Candidate>,
        query: String,
        limit: Int = DEFAULT_LIMIT
    ): List<Candidate> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val compactQuery = compact(trimmed)
        val tokens = trimmed.split(NON_ALPHANUMERIC).filter { it.isNotEmpty() }

        return candidates
            .mapNotNull { candidate ->
                scoreCandidate(candidate, trimmed, compactQuery, tokens)
                    ?.let { candidate to it }
            }
            .sortedWith(compareBy({ it.second }, { it.first.canonical.lowercase() }))
            .map { it.first }
            .take(limit)
    }/** Best (lowest) score across the candidate's canonical name and all its aliases. */
    private fun scoreCandidate(
        candidate: Candidate,
        query: String,
        compactQuery: String,
        tokens: List<String>
    ): Double? = candidate.aliases
        .asSequence()
        .plus(candidate.canonical)
        .mapNotNull { score(it, query, compactQuery, tokens) }
        .minOrNull()

    /**
     * Lower is better; `null` means the name is not a match at all.
     *
     * Score bands (lower = stronger):
     *  0–5   exact / prefix / substring of the raw or compact form
     *  10+   whole-string fuzzy match (edit distance)
     *  20+   every query token fuzzy-matches a word of the name
     */
    private fun score(
        name: String,
        query: String,
        compactQuery: String,
        tokens: List<String>
    ): Double? {
        val lower = name.lowercase()
        return scoreAgainst(lower, compact(lower), lower.split(NON_ALPHANUMERIC).filter { it.isNotEmpty() }, query, compactQuery, tokens)
    }

    /**
     * The matching itself, over an already-normalised name.
     *
     * Split out from [score] so [PreparedIndex] can pass the precomputed
     * `lower` / `compactName` / `words` instead of rebuilding them per keystroke.
     * The bodies are identical, which is what keeps the two paths in agreement.
     */
    private fun scoreAgainst(
        lower: String,
        compactName: String,
        words: List<String>,
        query: String,
        compactQuery: String,
        tokens: List<String>
    ): Double? {

        if (lower == query) return 0.0
        if (compactName == compactQuery) return 1.0
        if (lower.startsWith(query)) return 2.0
        if (compactName.startsWith(compactQuery)) return 3.0
        if (lower.contains(query)) return 4.0
        if (compactName.contains(compactQuery)) return 5.0

        // Whole-string typo tolerance ("benchpres" -> "bench press").
        val wholeDistance = damerau(
            compactName,
            compactQuery,
            maxDistance = allowedDistance(compactQuery.length)
        )
        val wholeFuzzy = if (wholeDistance <= allowedDistance(compactQuery.length)) {
            10.0 + wholeDistance
        } else null

        // Token tolerance: every typed word must find a close word, so
        // "bnech press" still reaches "Barbell Bench Presses".
        var tokenScore: Double? = null
        if (tokens.isNotEmpty()) {
            var total = 0.0
            var matchedAll = true
            for (token in tokens) {
                val best = bestTokenScore(token, words, compactName)
                if (best == null) {
                    matchedAll = false
                    break
                }
                total += best
            }
            if (matchedAll) tokenScore = 20.0 + total / tokens.size
        }

        return listOfNotNull(wholeFuzzy, tokenScore).minOrNull()
    }

    private fun bestTokenScore(token: String, words: List<String>, compactName: String): Double? {
        var best: Double? = null

        for (word in words) {
            val score = when {
                word == token -> 0.0
                word.startsWith(token) -> 1.0
                token.startsWith(word) && word.length >= 3 -> 2.0
                else -> {
                    val distance = damerau(word, token, maxDistance = allowedDistance(token.length))
                    if (distance <= allowedDistance(token.length)) 3.0 + distance else null
                }
            }
            if (score != null) best = if (best == null) score else minOf(best, score)
        }

        // A single token can also span several words ("benchpres").
        if (compactName.contains(token)) {
            best = if (best == null) 4.0 else minOf(best, 4.0)
        } else {
            val distance = damerau(compactName, token, maxDistance = allowedDistance(token.length))
            if (distance <= allowedDistance(token.length)) {
                val score = 6.0 + distance
                best = if (best == null) score else minOf(best, score)
            }
        }

        return best
    }

    /**
     * How many single-character edits (insert / delete / replace / adjacent swap)
     * are tolerated for a query of [length]. Kept tight for short words so a
     * three-letter query doesn't pull in half the pool.
     */
    private fun allowedDistance(length: Int): Int = when {
        length <= 2 -> 0
        length <= 4 -> 1
        length <= 8 -> 2
        else -> 3
    }

    /** Letters and digits only, so "Pull-up" and "Pullup" compare as one word. */
    private fun compact(value: String): String {
        val sb = StringBuilder(value.length)
        for (ch in value) {
            if (ch.isLetterOrDigit()) sb.append(ch)
        }
        return sb.toString()
    }

    /**
     * Optimal string alignment (Damerau-Levenshtein restricted to adjacent
     * transpositions) — the common phone-typing mistakes. Bails out early once
     * the distance provably exceeds [maxDistance].
     */
    private fun damerau(a: String, b: String, maxDistance: Int): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > maxDistance) return maxDistance + 1

        var previousPrevious: IntArray? = null
        var previous = IntArray(b.length + 1) { it }

        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i

            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = minOf(
                    previous[j] + 1,        // deletion
                    current[j - 1] + 1,     // insertion
                    previous[j - 1] + cost  // substitution
                )
                if (i > 1 && j > 1 &&
                    a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]
                ) {
                    value = minOf(value, previousPrevious!![j - 2] + 1) // adjacent swap
                }
                current[j] = value
            }

            previousPrevious = previous
            previous = current
        }

        return previous[b.length]
    }

    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
}
