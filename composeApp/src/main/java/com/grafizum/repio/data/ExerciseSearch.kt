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
     * Returns the exercise names matching [query], best match first. A blank
     * query yields an empty list (the search field shows nothing until typing).
     */
    fun search(
        allExercises: List<String>,
        query: String,
        limit: Int = DEFAULT_LIMIT
    ): List<String> = searchCandidates(
        allExercises.map { Candidate(it) },
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
    }

    /** Best (lowest) score across the candidate's canonical name and all its aliases. */
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
        val compactName = compact(lower)

        if (lower == query) return 0.0
        if (compactName == compactQuery) return 1.0
        if (lower.startsWith(query)) return 2.0
        if (compactName.startsWith(compactQuery)) return 3.0
        if (lower.contains(query)) return 4.0
        if (compactName.contains(compactQuery)) return 5.0

        val words = lower.split(NON_ALPHANUMERIC).filter { it.isNotEmpty() }

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
