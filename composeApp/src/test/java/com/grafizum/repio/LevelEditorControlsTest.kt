package com.grafizum.repio

import com.grafizum.repio.ui.components.levelEditorControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    /** The everyday case: level system on, a resolved level, level 4 of 12. */
    private fun managed(level: Int = 4, max: Int = 12, canWrite: Boolean = true) =
        levelEditorControls(
            levelsManaged = true,
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
     * ADDING AN EXERCISE SHOWS THE LEVELS, which is the whole of the owner's
     * report (2026-10-09): "When adding an exercise it shows the custom input boxes
     * not the levels, redesign to fix it".
     *
     * A new exercise is not outside the level system: the engine answers a starting
     * level for every progression-ready exercise at the user's own difficulty
     * ([ProgressionEngine.startingPrescription], swept for every row by
     * DurationStartingPrescriptionTest), and that level is what the row will train
     * at the moment it is added. So the row appears over a REAL prescription, and
     * the old `isExistingExercise` gate was refusing it on the grounds that there
     * was nothing to move - which was never true for a pool exercise.
     */
    @Test
    fun addingAnExerciseWhoseLevelResolvesShowsTheRow() {
        val c = levelEditorControls(
            levelsManaged = true,
            currentLevel = 4,
            maxLevel = 12,
            canWriteLevel = true
        )
        assertTrue("the level the new row will start on is editable", c.showLevelRow)
        assertTrue("and the whole ladder is reachable from it", c.canOpenLadder)
    }

    /**
     * The other half, and the reason the gate is the LEVEL rather than the absence of
     * a row: an exercise the host cannot resolve a level for - a custom movement, or
     * a name the user is still typing - still gets the plain input boxes, because
     * there is genuinely nothing to list. Without this case the change above would
     * be indistinguishable from "the row is always on".
     */
    @Test
    fun anUnratedExerciseShowsNoLevelRow() {
        val c = levelEditorControls(
            levelsManaged = true,
            currentLevel = null,
            maxLevel = 12,
            canWriteLevel = true
        )
        assertFalse(c.showLevelRow)
        assertFalse(c.canOpenLadder)
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
        assertFalse("custom mode", levelEditorControls(false, 4, 12, true).showLevelRow)
        assertFalse("no level", levelEditorControls(true, null, 12, true).showLevelRow)
    }

    /**
     * THE HOST SIDE OF THE SAME REPORT. The rule above is only worth anything if the
     * editor is actually GIVEN the levels: every value the level UI draws used to be
     * passed in for `exercise`, and the add form has no exercise - it has a name in
     * its own search field. That is why the level data moved behind a name-keyed
     * resolver ([EditorLevelData]), and this case keeps a fourth host from opening
     * the editor without one, which would put the add form back to custom boxes with
     * nothing in the test suite noticing.
     */
    @Test
    fun everyHostThatOpensTheEditorResolvesTheLevelsByName() {
        val offenders = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            // The dialog's own declaration is not a call site.
            .filterNot { it.path.endsWith("ui/components/WorkoutComponents.kt") }
            .filter { it.readText().contains("ExerciseEditDialog(") }
            .filterNot { it.readText().contains("levelDataFor") }
            .map { it.path }
            .toList()
        assertEquals(
            "these call sites open the exercise editor without the name-keyed level " +
                "resolver, so their add form cannot show the level list: " +
                offenders.joinToString(),
            emptyList<String>(),
            offenders,
        )
    }
}
