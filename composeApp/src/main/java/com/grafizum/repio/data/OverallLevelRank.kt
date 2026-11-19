package com.grafizum.repio.data

import kotlin.math.roundToInt

/**
 * The letter tier shown beside the overall level: E to SS+.
 *
 * This is a *display* layer only — [OverallLevelCalculator] does the maths and
 * produces a 0-100 figure; this object only reads that figure and names it.
 * Nothing here formats a percentage for the user: the letter is the whole of
 * what is shown, and the 0-100 value stays backend-only (owner, 2026-10-02).
 *
 * ## The thresholds
 *
 * The owner fixed both ends: **0% is E** and **95%+ is SS+**, with the seven
 * tiers between them spaced evenly. Eight tiers over 0-95 gives a step of
 * 95/7 ≈ 13.571:
 *
 *     E 0.0 · D 13.6 · C 27.1 · B 40.7 · A 54.3 · S 67.9 · SS 81.4 · SS+ 95.0
 *
 * The scale deliberately does NOT run to 100. The top band is the widest claim
 * the app makes — "you are within 5% of the ceiling on the exercise in the
 * middle of your routine" — and reserving the last five points means SS+ is a
 * genuine ceiling band rather than something a rounding difference hands out.
 *
 * Letters are not translated — "S" is "S" in every locale — so [Letter.label] is
 * a literal rather than a string resource. [Letter.descriptionKey] IS a
 * resource key, resolved through the dynamic `getString` bridge, so the
 * motivating line under each rank is localizable.
 */
object OverallLevelRank {

    /**
     * Where the top tier starts. The scale runs 0 to this, not 0 to 100.
     */
    const val TOP_TIER_PERCENT = 95.0

    /**
     * The evenly-spaced band width: the 0-[TOP_TIER_PERCENT] range divided by
     * the seven steps between eight tiers. Derived, not typed, so changing the
     * number of tiers cannot leave the table with a gap or an overlap.
     */
    private val STEP = TOP_TIER_PERCENT / (Letter.entries.size - 1)

    /**
     * Where the figure saturates. Not a rank and never shown — it exists so the
     * open top band has a real width and so a figure above 100 (an exercise set
     * past its ceiling) has somewhere defined to be clamped.
     */
    const val SCALE_CEILING = 100.0

    /**
     * The tiers in order, lowest first. [minPercent] is the overall percent at
     * which the rank is reached; it is computed from [STEP] rather than written
     * out, which is what keeps the "evenly spaced" claim true after an edit.
     *
     * The enum cannot declare `minPercent` from a companion's `STEP`, so each
     * entry declares its ordinal position and the percentage is derived — see
     * [minPercent]. The declared values below are therefore a cross-check, not
     * the source of truth.
     */
    enum class Letter(
        val label: String,
        val descriptionKey: String
    ) {
        E("E", "rank_e_desc"),
        D("D", "rank_d_desc"),
        C("C", "rank_c_desc"),
        B("B", "rank_b_desc"),
        A("A", "rank_a_desc"),
        S("S", "rank_s_desc"),
        SS("SS", "rank_ss_desc"),
        SS_PLUS("SS+", "rank_ss_plus_desc");

        /** The next rank up, or null for the open-ended top tier. */
        fun next(): Letter? = entries.getOrNull(ordinal + 1)

        /**
         * The overall percent at which this tier begins: E at 0, SS+ at 95, and
         * `ordinal × STEP` in between. Derived from the ordinal so the spacing
         * is exactly even and the last entry lands on [TOP_TIER_PERCENT].
         */
        val minPercent: Double get() = ordinal * STEP

        /**
         * The inclusive top of this tier, for the "13.6 – 27.1" label, or null
         * for the open top tier. Derived from the next tier's floor rather than
         * written out, so the table cannot show a gap or an overlap.
         */
        val rangeEnd: Double? get() = next()?.minPercent

        /**
         * How many percentage points wide this tier is.
         *
         * The open top tier runs to [SCALE_CEILING], not to [TOP_TIER_PERCENT] —
         * both are 95 at its floor, so measuring it against TOP_TIER_PERCENT
         * would report a width of zero, which is not a band, it is a bug. The
         * band is real: 95 up to the point where the figure saturates.
         */
        val width: Double get() = (rangeEnd ?: SCALE_CEILING) - minPercent
    }

    /** Every rank, lowest first — what the rank table renders. */
    val ORDERED: List<Letter> get() = Letter.entries

    /**
     * A ranked result: which tier the percent falls in, and how far through it.
     */
    data class Info(
        val rank: Letter,
        /** 0.0–1.0 through the current tier; 1.0 at the top tier. */
        val progressToNext: Double
    ) {
        val letter: String get() = rank.label
        val nextLetter: String? get() = rank.next()?.label

        val descriptionKey: String get() = rank.descriptionKey

        /** True when there is nothing above this rank to progress to. */
        val isTop: Boolean get() = rank.next() == null
    }

    /**
     * The rank for an overall percent (the weighted median from
     * [OverallLevelCalculator]), or null when there is no level yet.
     *
     * Null rather than a default "E" on purpose: a user below the five-exercise
     * floor has not been *rated*, so handing them the bottom badge would claim a
     * measurement the app deliberately refuses to make. The UI keeps showing its
     * "train 5+ exercises" hint until there is a real number to rank.
     *
     * [progressToNext] is the fraction of the way from this tier's floor to the
     * next one, so 70% in S reads as 19% of the way to SS.
     */
    fun of(overallPercent: Double?): Info? {
        if (overallPercent == null) return null
        val clamped = overallPercent.coerceIn(0.0, SCALE_CEILING)
        val rank = Letter.entries.lastOrNull { clamped >= it.minPercent } ?: Letter.E
        val next = rank.next()
        val progress = if (next == null) {
            1.0
        } else {
            ((clamped - rank.minPercent) / (next.minPercent - rank.minPercent))
                .coerceIn(0.0, 1.0)
        }
        return Info(rank = rank, progressToNext = progress)
    }

    /** Convenience for callers holding the cached [UserStatsEntity] row. */
    fun of(stats: UserStatsEntity?): Info? =
        of(stats?.takeIf { it.overallLevelReady }?.overallStrengthLevel)

    /** The percent rounded to a whole number, for the rank table's range text. */
    fun displayPercent(percent: Double): String = percent.roundToInt().toString()

    /**
     * The overall LEVEL as a whole number, 1..100 — what the Home card's
     * headline prints instead of the rank word.
     *
     * The owner's ruling: "In the overall level i want to show the actual level
     * with only the rank symbol next to it (from: E -> Rank E, to: E -> Lvl.
     * 50)". So the number becomes the headline and the letter becomes a badge
     * beside it, rather than the letter carrying a word of its own.
     *
     * WHY 1 AND NOT 0. The stored figure is a percent of the way to the ceiling
     * across the routine, and 0% is a real, reachable state — the very first
     * rated session lands on it. Printing "Lvl. 0" reads as "no level at all"
     * rather than "the bottom of the scale", and the bottom tier is a tier like
     * any other, so the displayed range is 1..100.
     *
     * The two are NOT redundant: the number moves one unit at a time while the
     * letter only changes every ~13.6 points, so the number is what gives the
     * user a sense of movement between ranks and the letter is what tells them
     * which band they are in.
     */
    fun displayLevel(percent: Double): Int =
        percent.roundToInt().coerceIn(1, 100)

    /**
     * The span of LEVELS a rank covers, or [last] null for the open top band.
     *
     * A band is stored as a PERCENT (see [Letter.minPercent]); a user reads
     * LEVELS, because that is the number the Home card prints and the number
     * that moves every session. This is the one translation between the two,
     * and it is DERIVED, so adding a rank cannot leave the scale with a gap or
     * an overlap — `OverallLevelRankTest` sweeps for exactly that.
     */
    data class LevelRange(val first: Int, val last: Int?) {
        val isOpen: Boolean get() = last == null
    }

    /** The levels a rank covers: its own floor up to the level below the next rank's. */
    fun levelRangeOf(letter: Letter): LevelRange {
        val first = displayLevel(letter.minPercent)
        val end = letter.rangeEnd ?: return LevelRange(first, null)
        return LevelRange(first, (displayLevel(end) - 1).coerceAtLeast(first))
    }
}