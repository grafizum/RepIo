package com.grafizum.repio

import com.grafizum.repio.ui.screens.LevelPillBlocked
import com.grafizum.repio.ui.screens.LevelPillFill
import com.grafizum.repio.ui.screens.levelEditControls
import com.grafizum.repio.ui.screens.levelPillBlockedReason
import com.grafizum.repio.ui.screens.levelPillStyle
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

    /**
     * "Level button design needs to be changed so it shows its clickable WHEN
     * PRESSING ON THE PENCIL" — board task_2e5a3f4f5y6z515d3p6l3g0a, 2026-10-02.
     *
     * This corrects the 2026-10-01 fix rather than reporting a new bug. That work
     * raised the pill to 2dp in EVERY state because the owner had said it did not
     * look pressable, and it forced 0dp on the outlined one on the reasoning that
     * "an edit slot is not a button, the ring is the affordance". Both halves were
     * deliberate, and together they made the two states read EQUALLY — so the pill
     * was flattest at the exact moment the pencil was open and the user was about
     * to press it.
     *
     * The word doing the work is "when", and it is why this asserts a DIRECTION
     * rather than a difference. "Editing and idle are not the same" is satisfied by
     * an edit pill that sits BELOW idle, which would be a new flatness rather than
     * a fix; the requirement is that the affordance GROWS when the pencil opens.
     *
     * SUPERSEDED BY THE 2026-10-04 INVERSION, which reversed this direction: "no
     * i want the current edit design to be 'none edit' and the 'none edit' design
     * to be the edit design, invert them". The edit pill is now the SOLID one and
     * the committed pill is the ring. The assertion below pins the mapping in
     * both directions, and is kept rather than deleted because the trap is
     * precisely that both versions of this test are satisfied by "the states
     * differ" - only the direction tells them apart.
     *
     * AND BY THE 2026-10-05 RULING, which took the elevation out of both states
     * entirely - see [neitherPillStateCastsAShadow].
     */
    @Test
    fun theEditPillIsTheSolidOneAndTheCommittedPillTheOutlinedOne() {
        for (progressionAvailable in listOf(true, false)) {
            val idle = levelPillStyle(editing = false, progressionAvailable = progressionAvailable)
            val editing = levelPillStyle(editing = true, progressionAvailable = progressionAvailable)

            // FILL: the owner's inversion, stated as a mapping in both directions.
            assertEquals(
                "an OPEN edit pass is the solid fill (progressionAvailable=$progressionAvailable)",
                LevelPillFill.SOLID,
                editing.fill
            )
            assertEquals(
                "a COMMITTED level is the outlined ring (progressionAvailable=$progressionAvailable)",
                LevelPillFill.OUTLINED,
                idle.fill
            )

            // ELEVATION is not part of the distinction any more: it is 0 in both
            // states, because a Material3 Surface casts a shadow from the shape's
            // OUTLINE and the ring has no fill to lift. See
            // [neitherPillStateCastsAShadow] for the owner's report and the
            // two earlier rounds of this same parameter.
            assertEquals(
                "the committed ring casts no shadow (progressionAvailable=$progressionAvailable)",
                0,
                idle.shadowElevationDp
            )
            assertEquals(
                "the solid fill casts no shadow either (progressionAvailable=$progressionAvailable)",
                0,
                editing.shadowElevationDp
            )
        }
    }

    /**
     * The pill must not cast a shadow in EITHER state.
     *
     * Owner, on the phone: "the level button / label has a weird white line under
     * it and a shadow i want that fixed". That is not a styling preference, it is
     * two bugs in one report:
     *
     *  - a Material3 `Surface` turns `shadowElevation` into
     *    `Modifier.graphicsLayer(shadowElevation = ..., shape = shape)`, which
     *    casts from the shape's outline. The committed pill is a ring, so it threw
     *    a full rounded-rect shadow with nothing behind it to lift;
     *  - and the layer's ambient/spot colours default to black rather than to the
     *    theme, so even the solid pill's 2dp was not the app's own colour.
     *
     * So this asserts the ABSENCE, per state and per progression flag. A single
     * "elevation == 0" over the idle pill would have passed while the editing
     * pill still cast 2dp, which is the half the owner saw.
     */
    @Test
    fun neitherPillStateCastsAShadow() {
        for (editing in listOf(true, false)) {
            for (progressionAvailable in listOf(true, false)) {
                val style = levelPillStyle(editing = editing, progressionAvailable = progressionAvailable)
                assertEquals(
                    "a shadow under the level label is the defect being fixed " +
                        "(editing=$editing, progressionAvailable=$progressionAvailable)",
                    0,
                    style.shadowElevationDp
                )
            }
        }
    }

    /**
     * The two states must not be able to drift into looking alike again, which is
     * what the 2026-10-02 complaint was in the first place. Asserted on the
     * rendered surface properties rather than on the fill enum alone, because the
     * border is what actually differs on screen once both states are drawn.
     *
     * ELEVATION CAN NO LONGER CARRY ANY WEIGHT HERE - it is 0 in both states by
     * the 2026-10-05 ruling - so the two properties that still differ are fill
     * and ring. That is a real constraint on this test rather than a formality:
     * it used to assert `idle.shadow > editing.shadow`, which is now satisfied by
     * `0 > 0` being false, so leaving it would have quietly stopped testing
     * anything at all.
     */
    @Test
    fun theTwoPillStatesStayVisiblyDifferent() {
        val idle = levelPillStyle(editing = false, progressionAvailable = false)
        val editing = levelPillStyle(editing = true, progressionAvailable = false)

        assertEquals(
            "the committed level is the ring, the open edit pass is the fill",
            LevelPillFill.OUTLINED,
            idle.fill
        )
        assertEquals(
            "the committed level is the ring, the open edit pass is the fill",
            LevelPillFill.SOLID,
            editing.fill
        )
        assertTrue(
            "the committed ring must stay stronger than the editing hairline",
            idle.borderWidthDp > editing.borderWidthDp
        )
    }

    /**
     * The pending progression compacts the pill so it can stop taking the row's
     * whole vertical height. That must not quietly become a second way for the
     * pill to read as un-pressable — the compactness is padding and type, never
     * the shape or the ring, in BOTH states.
     */
    @Test
    fun aPendingProgressionCompactsThePillWithoutFlatteningIt() {
        for (editing in listOf(true, false)) {
            val roomy = levelPillStyle(editing = editing, progressionAvailable = false)
            val compact = levelPillStyle(editing = editing, progressionAvailable = true)

            assertEquals(
                "elevation is an affordance, not a compaction knob (editing=$editing)",
                roomy.shadowElevationDp,
                compact.shadowElevationDp
            )
            assertTrue(
                "the compact pill gives up padding (editing=$editing)",
                compact.verticalPaddingDp < roomy.verticalPaddingDp
            )
        }
    }
}
