package com.grafizum.repio

import com.grafizum.repio.ui.screens.LevelCardBody
import com.grafizum.repio.ui.screens.levelCardBody
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * WHICH BODY THE LEVEL CARD WEARS, and the owner's complaint it answers:
 * "Time based exercises look like 'custon' exercises even though level system is
 * on, make sure all exercises have that new card design of the level system, i
 * want no exercise to dismiss the level system display card design" (board
 * task_5e4c3r1z1n3b3w211n205h02).
 *
 * A time-based exercise used to fall through to [LevelCardBody.SUMMARY] - the
 * plain custom-mode line - because the branch was gated on `!isTimeBased`. So a
 * plank, whose level system was running exactly as a bench press's was, rendered
 * in the one style the owner had explicitly read as "this exercise is not in the
 * system". `BANNER_DURATION` is that same banner wearing the unit the exercise
 * actually measures in.
 *
 * This is a pure three-case function that decides the whole look of the card, and
 * it had no test - so the one edit that put it back could have been reverted by
 * anything that touched the signature.
 */
class LevelCardBodyTest {

    @Test
    fun anExerciseOutsideTheSystemGetsThePlainSummary() {
        // The one case that IS a downgrade, and it is honest: there is no
        // prescription to banner.
        assertEquals(
            LevelCardBody.SUMMARY,
            levelCardBody(inSystem = false, isTimeBased = false, durationText = null),
        )
        assertEquals(
            LevelCardBody.SUMMARY,
            levelCardBody(inSystem = false, isTimeBased = true, durationText = "45 s"),
        )
    }

    @Test
    fun aTimeBasedExerciseGetsTheDurationBannerNotTheSummary() {
        // THE BUG. Both inputs say "in the level system" - only the unit differs,
        // and the unit must not decide which card design the user sees.
        assertEquals(
            "a time-based exercise in the system must not render as a custom exercise",
            LevelCardBody.BANNER_DURATION,
            levelCardBody(inSystem = true, isTimeBased = true, durationText = "45 s"),
        )
    }

    @Test
    fun aRepBasedExerciseGetsTheRepBanner() {
        assertEquals(
            LevelCardBody.BANNER,
            levelCardBody(inSystem = true, isTimeBased = false, durationText = null),
        )
    }

    /**
     * REVERSED 2026-10-04. This used to assert that a timed exercise with no
     * resolvable duration falls back to SUMMARY, on the reasoning that "a missing
     * value is not a downgrade of the card".
     *
     * That reasoning was wrong about what SUMMARY PRINTS. It renders
     * `primaryText ?: fallbackReps`, so for a timed exercise the fallback is the
     * routine's REP string - the owner's report was "some exercises dont show the
     * nice level card style and only show rep range". A plank printing "12 reps"
     * is not a missing value, it is a wrong prescription, and it is worse than an
     * empty headline because it looks correct.
     *
     * So an in-system timed exercise keeps its banner whether or not the engine
     * resolved a duration. The duration is a missing VALUE; the card is not a
     * downgrade of anything.
     */
    @Test
    fun aTimedExerciseWithNoResolvableDurationKeepsTheDurationBanner() {
        assertEquals(
            "an unresolved duration is a missing number, not a reason to print reps",
            LevelCardBody.BANNER_DURATION,
            levelCardBody(inSystem = true, isTimeBased = true, durationText = null),
        )
    }

    /**
     * Nothing in the level system may be shown the custom-mode card. Swept as a
     * statement rather than per-case, because the owner's wording was a blanket
     * one ("no exercise to dismiss the level system display card design") and a
     * per-case table would happily grow a fourth case nobody read.
     */
    @Test
    fun nothingInTheSystemEverWearsTheCustomPresentation() {
        for (timed in listOf(false, true)) {
            for (duration in listOf(null, "15 s", "45 s", "120 s")) {
                val body = levelCardBody(inSystem = true, isTimeBased = timed, durationText = duration)
                if (timed) {
                    assertEquals(
                        "a timed exercise wears the duration banner whatever its " +
                            "duration text is (duration=$duration)",
                        LevelCardBody.BANNER_DURATION,
                        body,
                    )
                } else {
                    assertEquals(
                        "a rep exercise wears the rep banner (duration=$duration)",
                        LevelCardBody.BANNER,
                        body,
                    )
                }
            }
        }
    }
}