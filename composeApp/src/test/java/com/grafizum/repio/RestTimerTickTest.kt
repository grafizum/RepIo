package com.grafizum.repio

import com.grafizum.repio.util.RestTimerTick
import com.grafizum.repio.util.remainingRestSeconds
import com.grafizum.repio.util.restTimerTick
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rest timer is driven from the end time stored in the session, so the background
 * service and the in-app timer always agree on what is left — and neither can end a rest
 * the user has already skipped, replaced or reset in the meantime.
 */
class RestTimerTickTest {

    private val now = 1_000_000L

    @Test
    fun countsDownWhileTheTimerRuns() {
        assertEquals(RestTimerTick.Count(42), restTimerTick(now + 42_000L, now))
    }

    @Test
    fun roundsUpSoTheLastSecondIsShownInFull() {
        assertEquals(RestTimerTick.Count(1), restTimerTick(now + 1L, now))
        assertEquals(RestTimerTick.Count(2), restTimerTick(now + 1_001L, now))
    }

    @Test
    fun finishesOnceTheEndTimeHasPassed() {
        assertEquals(RestTimerTick.Finish, restTimerTick(now, now))
        assertEquals(RestTimerTick.Finish, restTimerTick(now - 5_000L, now))
    }

    @Test
    fun aClearedTimerIsAbandonedRatherThanFinished() {
        // Skipped from the notification, replaced, or reset: nothing left to ring for.
        assertEquals(RestTimerTick.Abandon, restTimerTick(0L, now))
        assertEquals(RestTimerTick.Abandon, restTimerTick(-1L, now))
    }

    @Test
    fun remainingSecondsNeverGoNegative() {
        assertEquals(0, remainingRestSeconds(now, now))
        assertEquals(0, remainingRestSeconds(now - 1L, now))
    }
}
