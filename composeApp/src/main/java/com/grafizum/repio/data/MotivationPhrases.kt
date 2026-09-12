package com.grafizum.repio.data

/**
 * A real person's catchphrase, reworked into a line that actually makes sense as
 * a "you skipped the gym" nudge.
 *
 * Lines are deliberately short — a notification gets a glance, not a read. Each
 * entry keeps the original [quote] and its [source] for attribution/reference;
 * only the [line], the [author] shown beneath it, and the skipped-day count are
 * surfaced in the notification.
 */
data class MotivationPhrase(
    /** Short, scannable line shown in the notification. */
    val line: String,
    /** The original catchphrase the line is built from. */
    val quote: String,
    val author: String,
    /** Where the catchphrase comes from, kept for attribution/reference. */
    val source: String
)

/**
 * Catchphrases from real athletes and coaches who motivated others or pushed
 * their own limits.
 *
 * EMPTY UNTIL PERMISSION IS GRANTED. The quoted lines were compiled into the
 * shipped binary even with the nudge switched off, so they were pulled out
 * entirely while the permission request to David Goggins' team is pending —
 * nothing third-party is in the APK, not even as unused constants.
 *
 * Re-enabling after permission is granted: paste the lines back into [all], flip
 * [com.grafizum.repio.util.MotivationNudger.NUDGES_ENABLED] back to true, and
 * update `MotivationPhrasesTest`, which currently asserts the catalogue is empty
 * on purpose.
 */
object MotivationPhrases {

    /**
     * Keep lines scannable: roughly one glance on a lock screen. Kept here so the
     * limit is already in place when the approved lines are pasted back.
     */
    const val MAX_LINE_LENGTH = 80

    val all: List<MotivationPhrase> = emptyList()

    /**
     * Deterministically picks a phrase for a given day so the same nudge keeps
     * the same line for the rest of the day (no reshuffling on recomposition).
     */
    fun forDay(dayNumber: Long): MotivationPhrase {
        if (all.isEmpty()) return MotivationPhrase("", "", "", "")
        val index = ((dayNumber % all.size) + all.size) % all.size
        return all[index.toInt()]
    }

    /** Skips longer dry spells deeper into the list so a repeat feels new. */
    fun forSkipCount(skippedDays: Int, dayNumber: Long): MotivationPhrase =
        forDay(dayNumber + skippedDays.toLong())
}
