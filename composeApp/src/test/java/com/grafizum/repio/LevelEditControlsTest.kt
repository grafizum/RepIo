package com.grafizum.repio

import com.grafizum.repio.ui.screens.LevelPillBlocked
import com.grafizum.repio.ui.screens.levelEditControls
import com.grafizum.repio.ui.screens.levelPillBlockedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pencil's contract, in the owner's own words.
 *
 * Board task_3o3c023w5k073f573g0h2g29 (2026-10-01): "I want a pencil option
 * instead of the lock option while in the my workout tab, I want the pencil
 * button to edit current workout level and when pressed it shall switch to a
 * tick that will be ticked to kind of (lock in) the changed workout and save the
 * changes made. While in edit mode the + and - are shown next to the roman level
 * number but hidden once locked in. And the level card (the small level) will be
 * changed to kind of show that during edit mode it can be pressed and when lock
 * in then not anymore."
 *
 * That is four rules, and as composable code they were four independent boolean
 * expressions in four places - which is how the pill, the stepper and the pencil
 * end up disagreeing about whether the exercise is being edited. The rules now
 * live in [levelEditControls], and this is the test of them.
 *
 * The flow itself is: pencil -> `onBeginEdit` (clears routine_exercises.locked,
 * enters edit mode) -> + / - walk the Roman stages -> tick -> `onCommitEdit`
 * (sets the pin again, saving the level). The PERSISTED state is still that one
 * pin, so this is a change to the flow around it, not a new storage.
 */
class LevelEditControlsTest {

    /** An exercise inside the level system, not yet locked - the ordinary state. */
    private fun idle() = levelEditControls(adjustable = true, locked = false, editing = false)

    @Test
    fun anUnlockedIdleExerciseShowsThePencilAndNothingElse() {
        val c = idle()
        assertTrue("there would be no way into edit mode", c.showPencil)
        assertFalse("the + and - must be hidden until the pencil is pressed", c.stepperEnabled)
        assertFalse("the level pill must be inert until the pencil is pressed", c.levelPillEnabled)
    }

    @Test
    fun thePencilOpensTheEditPassAndTheTickClosesIt() {
        val editing = levelEditControls(adjustable = true, locked = false, editing = true)
        assertTrue("edit mode must show the tick that locks the level in", editing.showPencil)
        assertTrue("edit mode must show the roman-stage stepper", editing.stepperEnabled)
        assertTrue("edit mode must make the level card pressable", editing.levelPillEnabled)

        // And back again: committed, everything but the pencil goes inert.
        val committed = levelEditControls(adjustable = true, locked = true, editing = false)
        assertTrue("the pencil must stay reachable once committed", committed.showPencil)
        assertFalse("+ and - are hidden once locked in", committed.stepperEnabled)
        assertFalse("the level card is not pressable once locked in", committed.levelPillEnabled)
    }

    /**
     * The one that is easy to get wrong and impossible to notice by reading: a
     * committed level must not be adjustable THROUGH the back door of a stale
     * edit flag. `editing` is local UI state; `locked` is the saved truth, and
     * the saved truth wins.
     */
    @Test
    fun aLockedExerciseStaysInertEvenIfAnEditFlagSurvives() {
        val stale = levelEditControls(adjustable = true, locked = true, editing = true)
        assertFalse("the stepper must not walk a locked level", stale.stepperEnabled)
        assertFalse("the level card must not open for a locked level", stale.levelPillEnabled)
        assertTrue("but the pencil is still there to unlock it", stale.showPencil)
    }

    /**
     * An exercise the level system cannot rate grows no stepper and no pressable
     * pill - but it KEEPS the pencil while it is locked, and that exception is the
     * whole point.
     *
     * `locked` is a column on the routine row, not a property of the level system:
     * the evolution-deny path writes it, and so does the user's own hand. So an
     * exercise can be locked while carrying no level at all, and hiding the pencil
     * then would make that lock PERMANENT - the one control that clears it is the
     * one that disappeared. The first version of this test asserted the tidy rule
     * ("adjustable=false never grows chrome") and it was wrong in exactly that
     * direction.
     */
    @Test
    fun anExerciseOutsideTheLevelSystemHasNoStepperButALockedOneKeepsItsPencil() {
        val unlocked = levelEditControls(adjustable = false, locked = false, editing = false)
        assertFalse("nothing to edit, so nothing to offer", unlocked.showPencil)
        assertFalse(unlocked.stepperEnabled)
        assertFalse(unlocked.levelPillEnabled)

        val lockedOutside = levelEditControls(adjustable = false, locked = true, editing = false)
        assertTrue(
            "a locked exercise with no level would be permanently locked",
            lockedOutside.showPencil
        )
        assertFalse("there is no level to step", lockedOutside.stepperEnabled)
        assertFalse("there is no level pill to press", lockedOutside.levelPillEnabled)
    }

    /**
     * Not decoration: the stepper and the pill are two different controls and the
     * owner described them in the same breath, so they must never drift apart.
     */
    @Test
    fun theStepperAndTheLevelPillAlwaysAgree() {
        for (adjustable in listOf(true, false)) {
            for (locked in listOf(true, false)) {
                for (editing in listOf(true, false)) {
                    val c = levelEditControls(adjustable, locked, editing)
                    assertTrue(
                        "stepper=${c.stepperEnabled} pill=${c.levelPillEnabled} for " +
                            "adjustable=$adjustable locked=$locked editing=$editing",
                        c.stepperEnabled == c.levelPillEnabled
                    )
                }
            }
        }
    }

    /** The pencil is the only way back in, so it can never be hidden by state. */
    @Test
    fun thePencilIsReachableFromEveryStateTheAppCanBeIn() {        for (adjustable in listOf(true, false)) {
            for (locked in listOf(true, false)) {
                for (editing in listOf(true, false)) {
                    // The only combination that hides it is an exercise outside the
                    // level system that is neither locked nor being edited - and that
                    // combination is not reachable from the UI, because the pencil is
                    // the only control that sets `editing`.
                    val c = levelEditControls(adjustable, locked, editing)
                    if (adjustable || locked || editing) {
                        assertTrue(
                            "pencil unreachable: adjustable=$adjustable locked=$locked editing=$editing",
                            c.showPencil
                        )
                    }
                }
            }
        }
    }

    /**
     * The pill is drawn pressable in EVERY state (the owner reported it as "doesnt
     * look like its pressabel" when it was flat and greyed), so a press it refuses
     * has to explain itself instead of doing nothing silently.
     *
     * And the explanation must be right: an exercise that was NEVER locked is
     * still blocked while idle, because the ladder opens only inside an edit pass.
     * Keying the message off `locked` — the obvious implementation, and the one
     * written first — tells exactly those users to "switch to custom", which is
     * not why their tap did nothing.
     */
    @Test
    fun aBlockedPillAsksForThePencilWheneverTheLevelSystemIsRunning() {
        // Never locked, level system on: the pill still refuses (not editing), and
        // the honest answer is the pencil, not the custom mode.
        assertEquals(
            LevelPillBlocked.NEEDS_EDIT_PASS,
            levelPillBlockedReason(locked = false, levelSystemOn = true)
        )
        // Locked in: same answer.
        assertEquals(
            LevelPillBlocked.NEEDS_EDIT_PASS,
            levelPillBlockedReason(locked = true, levelSystemOn = true)
        )
        // Only a genuinely custom-mode app gets the other message.
        assertEquals(
            LevelPillBlocked.NEEDS_CUSTOM_MODE,
            levelPillBlockedReason(locked = false, levelSystemOn = false)
        )
    }
}
