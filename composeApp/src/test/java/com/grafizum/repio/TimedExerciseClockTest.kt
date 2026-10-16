package com.grafizum.repio

import com.grafizum.repio.ui.screens.logSetEnabled
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * THE TIMED EXERCISE'S CLOCK AND ITS LOG BUTTON ARE TWO THINGS.
 *
 * Owner, 2026-10-16: *"small start/stop/resume/restart by the clock, log button grey to themed"*.
 *
 * The clock and the log button used to be ONE control: the primary button read "START 45s TIMER &
 * SAVE", logged the set on the tap and then counted the work interval, turning into a counter/skip
 * while it ran. So a timed set was on record before the work was done, the clock had no controls,
 * and the biggest button on the screen meant two different things depending on the clock's state.
 *
 * These cases hold the three halves of the fix that a Compose test could not see:
 *
 *  * the BUTTON's rule - live exactly when the clock is not running,
 *  * the CLOCK's four verbs - start, hold, resume, restart - existing on the view model and being
 *    driven by the screen,
 *  * and the ORDER of the two: logging a timed set now goes through the same call a rep-based set
 *    does, which is what makes it record SECONDS rather than a rep count.
 *
 * The last one is asserted against the source rather than by running the view model: the timed path
 * is inside one long suspend function that writes through the repository, the database and the
 * timer service, so a test that "ran" it would need the whole app - while the property that matters
 * here is which call the screen makes and which unit that call records, both of which are readable.
 */
class TimedExerciseClockTest {

    private val screen = File("src/main/java/com/grafizum/repio/ui/screens/WorkoutNowScreen.kt")
        .readText()
    private val viewModel = File("src/main/java/com/grafizum/repio/ui/viewmodel/WorkoutViewModel.kt")
        .readText()

    /** GREY WHILE THE CLOCK RUNS, THEMED WHEN IT STOPS. */
    @Test
    fun theLogButtonIsHeldBackOnlyWhileTheClockRuns() {
        assertFalse(
            "the log button must be disabled while the work interval is counting",
            logSetEnabled(clockRunning = true),
        )
        assertTrue(
            "the log button must be live the moment the clock stops",
            logSetEnabled(clockRunning = false),
        )
        // ...and the screen must actually ask the rule, not pass a literal.
        assertTrue(
            "the log button does not consult the clock rule, so its state is decoration",
            screen.contains("enabled = logSetEnabled(clockRunning = isClockRunning)"),
        )
    }

    /**
     * The clock has all four verbs, and the screen offers them beside the countdown.
     *
     * A "restart" that merely stopped the clock would leave the user to tap again to begin, and a
     * clock with no HOLD would make the only way to finish a timed set early the very button that
     * this change exists to keep out of the work interval.
     */
    @Test
    fun theClockHasStartHoldResumeAndRestart() {
        for (verb in listOf(
            "fun startExerciseClock(",
            "fun pauseExerciseClock(",
            "fun resumeExerciseClock(",
            "fun restartExerciseClock(",
        )) {
            assertTrue(
                "the clock has no $verb, so one of start/hold/resume/restart is missing",
                viewModel.contains(verb),
            )
        }
        // The three states of the one play/pause control, and the restart beside it.
        assertTrue(
            "the clock controls do not start a held or idle clock",
            screen.contains("else -> viewModel.startExerciseClock("),
        )
        assertTrue(
            "the clock controls do not hold a running clock",
            screen.contains("clockRunning -> viewModel.pauseExerciseClock()"),
        )
        assertTrue(
            "the clock controls do not resume a held clock",
            screen.contains("clockHeld -> viewModel.resumeExerciseClock()"),
        )
        assertTrue(
            "there is no way back to the full interval",
            screen.contains("viewModel.restartExerciseClock("),
        )
    }

    /**
     * A finished work interval does NOT consume a set by itself.
     *
     * This is the half that would quietly undo the change: if the expiry path still advanced the
     * session, the clock would take the set the user never logged - once per timed exercise, and
     * silently. It used to do exactly that, because the set had already been logged at the start.
     */
    @Test
    fun aFinishedClockDoesNotMoveTheSessionOn() {
        val finish = viewModel.substring(
            viewModel.indexOf("private fun onRestTimerFinished()"),
            viewModel.indexOf("fun checkTimerOnAppClose()"),
        )
        assertFalse(
            "the expiry path still advances the session, so a timed set can be consumed by a clock",
            finish.contains("advanceAfterTimer"),
        )
        assertTrue(
            "the expiry path does not retire the countdown, so the clock would sit at zero forever",
            finish.contains("stopTimer()"),
        )
    }

    /** A timed set is logged in SECONDS, through the same call a rep-based set uses. */
    @Test
    fun aTimedSetIsLoggedInSeconds() {
        assertTrue(
            "a timed set at the bottom button must log its seconds, not its reps",
            viewModel.contains(
                "val loggedValue = if (isTimeBased) currentState.currentExerciseSeconds else currentState.currentReps"
            ),
        )
        assertFalse(
            "the old log-at-start path is back, so a set is recorded before the work is done",
            viewModel.contains("fun startExerciseTimerWithLog("),
        )
        assertTrue(
            "the log button no longer logs a set at all",
            screen.contains("viewModel.logCurrentSetAndAdvance(dayExercises)"),
        )
    }
}
