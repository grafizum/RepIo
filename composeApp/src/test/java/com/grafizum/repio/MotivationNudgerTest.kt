package com.grafizum.repio

import com.grafizum.repio.util.MotivationNudger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the background nudge rules: the notification may only go out when the
 * app is closed, has been unused for a few days, and days were actually skipped.
 */
class MotivationNudgerTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun shouldNudge(
        lastActiveMillis: Long = now - MotivationNudger.INACTIVITY_THRESHOLD_MS - 1,
        skippedDays: Int = 1,
        enabled: Boolean = true,
        hasTrainingHistory: Boolean = true,
        appInForeground: Boolean = false,
        lastShownDayKey: Long = Long.MIN_VALUE
    ): Boolean = MotivationNudger.shouldNudge(
        now = now,
        lastActiveMillis = lastActiveMillis,
        skippedDays = skippedDays,
        enabled = enabled,
        hasTrainingHistory = hasTrainingHistory,
        appInForeground = appInForeground,
        lastShownDayKey = lastShownDayKey
    )

    @Test
    fun masterSwitchKeepsTheNudgeDormantUntilTheQuotesAreCleared() {
        // The whole feature is held off by one switch while we wait for permission
        // to use the athletes' quoted catchphrases, so no quote can reach a user.
        assertFalse(
            "NUDGES_ENABLED must stay off until the quotes are cleared for use",
            MotivationNudger.NUDGES_ENABLED
        )
        // The delivery rules themselves are untouched and still willing to fire,
        // so flipping that one switch back on restores the feature with no other
        // change needed anywhere.
        assertTrue(
            "the rules must still be intact behind the master switch",
            shouldNudge()
        )
    }

    @Test
    fun nudgesWhenClosedIdleForDaysAndDaysWereSkipped() {
        assertTrue(shouldNudge())
    }

    @Test
    fun neverNudgesWhileTheAppIsOpen() {
        assertFalse(shouldNudge(appInForeground = true))
    }

    @Test
    fun respectsTheDisabledSetting() {
        assertFalse(shouldNudge(enabled = false))
    }

    @Test
    fun needsAtLeastOneSkippedDay() {
        assertFalse(shouldNudge(skippedDays = 0))
    }

    @Test
    fun neverNudgesWithoutTrainingHistory() {
        assertFalse(shouldNudge(hasTrainingHistory = false))
    }

    @Test
    fun staysQuietWhenTheAppWasUsedRecently() {
        assertFalse(shouldNudge(lastActiveMillis = now - day))
    }

    @Test
    fun thresholdIsInclusive() {
        // Exactly at the threshold counts as "idle for days"; one ms less does not.
        assertTrue(shouldNudge(lastActiveMillis = now - MotivationNudger.INACTIVITY_THRESHOLD_MS))
        assertFalse(shouldNudge(lastActiveMillis = now - MotivationNudger.INACTIVITY_THRESHOLD_MS + 1))
    }

    @Test
    fun unknownLastUseIsTreatedAsFreshInstall() {
        assertFalse(shouldNudge(lastActiveMillis = 0L))
    }

    @Test
    fun noSecondNudgeOnTheSameDay() {
        assertFalse(shouldNudge(lastShownDayKey = now / day))
        assertTrue(shouldNudge(lastShownDayKey = now / day - 1))
    }
}
