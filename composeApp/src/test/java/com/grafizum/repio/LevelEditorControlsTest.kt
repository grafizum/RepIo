package com.grafizum.repio

import com.grafizum.repio.ui.components.levelEditorControls
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The exercise editor's LEVEL row.
 *
 * Owner, 2026-10-05: "also the editing of an exercise, allow the user to edit
 * the level while in level system mode". Until then the editor greyed out every
 * value the level owns and offered exactly one way out — "switch to Custom" —
 * which reads as *this exercise cannot be edited here* rather than as *these
 * three numbers belong to the level*.
 *
 * These cases are about WHERE the row may appear and which of its three
 * affordances are honest, because that is where a level control fails quietly:
 * a row that appears in custom mode offers to prescribe a level that does not
 * exist, and an arrow that is drawn at the top of a ladder taps into a clamp and
 * looks broken.
 */
class LevelEditorControlsTest {

    /** The everyday case: level system on, existing exercise, level 4 of 12. */
    private fun managed(level: Int = 4, max: Int = 12, canWrite: Boolean = true) =
        levelEditorControls(
            levelsManaged = true,
            isExistingExercise = true,
            currentLevel = level,
            maxLevel = max,
            canWriteLevel = canWrite
        )

    @Test
    fun aLevelManagedExerciseGetsTheRowAndItsThreeControls() {
        val c = managed()
        assertTrue("the level is editable from the editor", c.showLevelRow)
        assertTrue("a level in the middle of the ladder steps down", c.canStepDown)
        assertTrue("a level in the middle of the ladder steps up", c.canStepUp)
        assertTrue("the whole ladder is one tap away", c.canOpenLadder)
    }

    /**
     * The floor and the ceiling are the two rungs where an arrow must go dead.
     *
     * `setExerciseLevel` clamps, so a live arrow at either end still "works" —
     * it just does nothing, which is precisely what a dead control looks like
     * from the user's side.
     */
    @Test
    fun theBottomAndTopRungsDisableTheArrowThatHasNowhereToGo() {
        val bottom = managed(level = 1, max = 12)
        assertFalse("level 1 is the bottom of every ladder", bottom.canStepDown)
        assertTrue("and it can still step up", bottom.canStepUp)

        val top = managed(level = 12, max = 12)
        assertFalse("the ceiling is the exercise's own humane end", top.canStepUp)
        assertTrue("and it can still step down", top.canStepDown)

        // The ladder line stays live at both ends: there is always something to
        // look at, and the picker is how a user reaches a level they mis-stepped.
        assertTrue(bottom.canOpenLadder)
        assertTrue(top.canOpenLadder)
    }

    /**
     * An UNKNOWN ceiling must not cost the user the "+".
     *
     * `maxLevel = 0` is what a host that does not know the ceiling passes. The
     * writer clamps to `progressionMaxLevel` regardless, so allowing the step is
     * honest; refusing it would leave a permanently dead arrow that looks like
     * the ceiling was reached.
     */
    @Test
    fun anUnknownCeilingStillAllowsSteppingUp() {
        val c = managed(level = 4, max = 0)
        assertTrue(c.showLevelRow)
        assertTrue(c.canStepUp)
    }

    /**
     * Custom mode has no level to edit. Showing the row there would promise a
     * prescription the app is not making — the same confusion as a level pill on
     * an exercise outside the system.
     */
    @Test
    fun customModeShowsNoLevelRow() {
        val c = levelEditorControls(
            levelsManaged = false,
            isExistingExercise = true,
            currentLevel = 4,
            maxLevel = 12,
            canWriteLevel = true
        )
        assertFalse(c.showLevelRow)
        assertFalse(c.canStepDown)
        assertFalse(c.canStepUp)
        assertFalse(c.canOpenLadder)
    }

    /**
     * A NEW exercise has nothing prescribed yet, so there is no level to move.
     * The editor already treats adding as never gated; the level row follows the
     * same rule rather than appearing over an empty prescription.
     */
    @Test
    fun addingAnExerciseShowsNoLevelRow() {
        val c = levelEditorControls(
            levelsManaged = true,
            isExistingExercise = false,
            currentLevel = null,
            maxLevel = 12,
            canWriteLevel = true
        )
        assertFalse(c.showLevelRow)
        assertFalse(c.canOpenLadder)
    }

    /**
     * An exercise the engine cannot rate has no level either — [currentLevel] is
     * null even though the system is on, and a row built from a null level would
     * print "Level null".
     */
    @Test
    fun anUnratedExerciseShowsNoLevelRow() {
        val c = levelEditorControls(
            levelsManaged = true,
            isExistingExercise = true,
            currentLevel = null,
            maxLevel = 12,
            canWriteLevel = true
        )
        assertFalse(c.showLevelRow)
    }

    /**
     * A host with no writer shows NOTHING, rather than a row whose taps go
     * nowhere. This is the level-pill lesson applied to a new control: the pill
     * looked pressable in every state and swallowed the tap, and the first
     * instinct for a level control would be the same one.
     */
    @Test
    fun aHostWithNoWriterShowsNoRow() {
        val c = managed(canWrite = false)
        assertFalse(c.showLevelRow)
        assertFalse(c.canStepDown)
        assertFalse(c.canStepUp)
        assertFalse(c.canOpenLadder)
    }

    /**
     * Nothing that is not a level-managed existing exercise may light the row up,
     * whichever single input is the odd one out. The tempting wrong version is
     * `show = currentLevel != null` alone, which puts the row in custom mode.
     */
    @Test
    fun everyInputIsLoadBearing() {
        assertTrue(managed().showLevelRow)
        assertFalse("no writer", managed(canWrite = false).showLevelRow)
        assertFalse("custom mode", levelEditorControls(false, true, 4, 12, true).showLevelRow)
        assertFalse("a new exercise", levelEditorControls(true, false, 4, 12, true).showLevelRow)
        assertFalse("no level", levelEditorControls(true, true, null, 12, true).showLevelRow)
    }
}
