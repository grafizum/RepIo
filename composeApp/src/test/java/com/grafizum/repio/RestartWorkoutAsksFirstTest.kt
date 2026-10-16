package com.grafizum.repio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RESTARTING A WORKOUT ASKS FIRST, AND BOTH RESTARTS ASK THE SAME WAY.
 *
 * [com.grafizum.repio.ui.viewmodel.WorkoutViewModel.resetWorkoutSession] is destructive in a way
 * nothing else on the workout screen is: it calls `repository.deleteTodayLogs()`, so every set
 * logged today is gone before the user can change their mind. Owner, 2026-10-16: *"Ask for
 * confirmation if the user wants to restart the workout"*.
 *
 * The screen already had one confirmation - the small restart button beside the progress row
 * (`restart_workout_button`) opens `showRestartDialog` - and the completion screen's
 * "START WORKOUT AGAIN" button did not: it called the reset straight from its `onClick`, so the
 * one restart a user is most likely to press, on a finished session, wiped the log on a single
 * tap. These cases hold the shape that fix put in place, because a Compose test here could only
 * see the dialog's pixels and not which writer ran behind it.
 *
 * What is pinned is the ORDER: exactly one reset call on the screen, and it is inside the
 * confirmation. A future edit that adds a second, direct call - on any button - fails here
 * rather than silently deleting a session.
 */
class RestartWorkoutAsksFirstTest {

    private val screen: String =
        File("src/main/java/com/grafizum/repio/ui/screens/WorkoutNowScreen.kt").readText()

    private val viewModel: String =
        File("src/main/java/com/grafizum/repio/ui/viewmodel/WorkoutViewModel.kt").readText()

    /** The reset itself, as the screen would call it: a call, not the prose that names it. */
    private val resetCall = Regex("""viewModel\.resetWorkoutSession\(\)""")

    /**
     * The dialog, from its guard to the screen's own layout column.
     *
     * Slicing on the next `Column(` and not on a brace count: the dialog contains lambdas and
     * nested blocks, and a brace matcher over a source string is a test that fails on unrelated
     * reformatting.
     */
    private fun confirmationBlock(): String {
        val guard = screen.indexOf("if (showRestartDialog)")
        assertTrue("the restart confirmation is gone from the workout screen", guard >= 0)
        val afterGuard = guard + "if (showRestartDialog)".length
        val columnIndex = screen.indexOf("\n    Column(", afterGuard)
        assertTrue("no Column( after the restart dialog - did the screen be laid out again?", columnIndex > 0)
        return screen.substring(afterGuard, columnIndex)
    }

    /**
     * One reset call on the whole screen, and it sits inside the confirmation.
     *
     * This is the case that would have caught the original bug: two call sites, one of them a
     * button's `onClick` with no dialog in front of it.
     */
    @Test
    fun theScreenOnlyResetsThroughTheConfirmation() {
        val calls = resetCall.findAll(screen).map { it.range.first }.toList()
        assertEquals(
            "the workout screen calls resetWorkoutSession ${calls.size} times - every one of them " +
                "has to be the confirmation's confirm button, and a direct call from a Button " +
                "deletes today's log on one tap",
            1,
            calls.size
        )

        val guard = screen.indexOf("if (showRestartDialog)")
        assertTrue(
            "the reset is called BEFORE the confirmation opens, so something resets the session " +
                "without asking",
            calls.single() > guard
        )

        val dialog = confirmationBlock()
        assertTrue(
            "the confirmation does not contain the reset call, so the dialog cannot be what " +
                "restarts the session",
            resetCall.containsMatchIn(dialog)
        )
    }

    /** The user has to be able to say no: the dialog declines, and declining writes nothing. */
    @Test
    fun theConfirmationCanBeDeclined() {
        val dialog = confirmationBlock()
        assertTrue(
            "the dialog has no dismiss handler, so backing out of it is undefined",
            dialog.contains("onDismissRequest")
        )
        assertTrue(
            "the dialog has no cancel button - the user cannot decline a restart",
            dialog.contains("""getString("cancel")""")
        )
    }

    /** The completion screen's restart opens the dialog instead of resetting on the spot. */
    @Test
    fun startWorkoutAgainAsksInsteadOfResetting() {
        val label = screen.indexOf("start_workout_again")
        assertTrue("the \"start workout again\" button is gone", label > 0)

        val handler = screen.substring(screen.lastIndexOf("onClick", label), label)
        assertTrue(
            "\"START WORKOUT AGAIN\" does not open the confirmation, so a finished workout is " +
                "still wiped by one tap",
            handler.contains("showRestartDialog = true")
        )
        assertFalse(
            "\"START WORKOUT AGAIN\" calls the reset directly again - the confirmation is not in " +
                "the path",
            resetCall.containsMatchIn(handler)
        )
    }

    /**
     * ...and the guard is worth having because the reset really does delete work.
     *
     * Without this the three cases above would still pass if `resetWorkoutSession` were made
     * harmless, and the dialog became a prompt about nothing.
     */
    @Test
    fun theGuardedResetIsDestructive() {
        val resetAt = viewModel.indexOf("fun resetWorkoutSession()")
        assertTrue("resetWorkoutSession is gone from the view model", resetAt > 0)
        val body = viewModel.substring(resetAt, viewModel.indexOf("\n    }", resetAt))
        assertTrue(
            "resetWorkoutSession no longer deletes today's logs, so the confirmation in front of " +
                "it no longer protects anything",
            body.contains("repository.deleteTodayLogs()")
        )
    }
}
