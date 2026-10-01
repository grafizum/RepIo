package com.grafizum.repio.data

/**
 * The letter tier shown beside the overall level: E to SS+.
 *
 * This is a *display* layer only — [OverallLevelCalculator] does the maths; this
 * object just reads its result and names it. The thresholds are one table in one
 * place on purpose, so they can be retuned from real data without hunting call
 * sites.
 *
 * Where the numbers come from. The overall level is the recency-weighted average
 * of the user's per-exercise levels, rounded to one decimal. Per-exercise levels
 * mostly run 1–12 (the CSV ladders), so:
 *
 *  - A user training five exercises at level 2 everywhere sits at 2.0 → D.
 *  - A committed lifter averaging level 6 across their routine sits at 6.0 → S.
 *  - 8.0+ is deliberately out of reach for anyone still climbing: it demands
 *    every trained exercise near the top of its ladder.
 *
 * SS (7.0) and SS+ (8.0) are therefore elite bands, not steps on the way —
 * Letters are not translated — "S" is "S" in every locale.
 */
object OverallLevelRank {

    /**
     * The tiers in order, lowest first. [minLevel] is the overall level (the
     * recency-weighted average, one decimal) at which the rank is reached. The
     * tier colours were removed: the letter itself, in the theme's primary
     * colour, carries the rank now.
     *
     * [label] is a literal rather than a string resource because letters are not
     * translated — "S" is "S" in every locale. [descriptionKey] *is* a resource
     * key, resolved through the dynamic `getString` bridge, so the motivating
     * line under each rank is localizable.
     */
    enum class Letter(
        val label: String,
        val minLevel: Double,
        val descriptionKey: String
    ) {
        E("E", 1.0, "rank_e_desc"),
        D("D", 2.0, "rank_d_desc"),
        C("C", 3.0, "rank_c_desc"),
        B("B", 4.0, "rank_b_desc"),
        A("A", 5.0, "rank_a_desc"),
        S("S", 6.0, "rank_s_desc"),
        SS("SS", 7.0, "rank_ss_desc"),
        SS_PLUS("SS+", 8.0, "rank_ss_plus_desc");

        /** The next rank up, or null for the open-ended top tier. */
        fun next(): Letter? = entries.getOrNull(ordinal + 1)

        /**
         * The inclusive top of this rank's range, for the "6.0 – 6.9" label, or
         * null for the top tier. Derived from the next rank's floor minus 0.1,
         * so the table cannot show a gap or an overlap.
         */
        val rangeEnd: Double? get() = next()?.minLevel?.minus(0.1)
    }

    /** Every rank, lowest first — what the rank table renders. */
    val ORDERED: List<Letter> get() = Letter.entries

    /**
     * A ranked result: which tier the level falls in, and how far through it.
     */
    data class Info(
        val rank: Letter,
        /** 0.0–1.0 through the current tier; 1.0 at the top tier. */
        val progressToNext: Double
    ) {
        val letter: String get() = rank.label
        val minLevel: Double get() = rank.minLevel
        val nextLevel: Double? get() = rank.next()?.minLevel

        /** The letter above this one, or null at the open top tier. */
        val nextLetter: String? get() = rank.next()?.label

        val descriptionKey: String get() = rank.descriptionKey

        /** True when there is nothing above this rank to progress to. */
        val isTop: Boolean get() = rank.next() == null
    }

    /**
     * The rank for an overall level (the one-decimal average), or null when
     * there is no level yet.
     *
     * Null rather than a default "E" on purpose: a user below the five-exercise
     * floor has not been *rated*, so handing them the bottom badge would claim a
     * measurement the app deliberately refuses to make. The UI keeps showing its
     * "train 5+ exercises" hint until there is a real number to rank.
     *
     * [progressToNext] is the fraction of the way from this rank's floor to the
     * next one, so level 6.4 in S (6.0–6.9) reads as 40% to SS.
     */
    fun of(overallLevel: Double?): Info? {
        if (overallLevel == null) return null
        val clamped = overallLevel.coerceAtLeast(1.0)
        val rank = Letter.entries.lastOrNull { clamped >= it.minLevel } ?: Letter.E
        val next = rank.next()
        val progress = if (next == null) {
            1.0
        } else {
            ((clamped - rank.minLevel) / (next.minLevel - rank.minLevel))
                .coerceIn(0.0, 1.0)
        }
        return Info(rank = rank, progressToNext = progress)
    }

    /** Convenience for callers holding the cached [UserStatsEntity] row. */
    fun of(stats: UserStatsEntity?): Info? =
        of(stats?.takeIf { it.overallLevelReady }?.overallStrengthLevel)
}
