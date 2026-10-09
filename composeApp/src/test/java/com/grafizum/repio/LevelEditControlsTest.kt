package com.grafizum.repio

import com.grafizum.repio.ui.screens.LevelPillBlocked
import com.grafizum.repio.ui.screens.levelEditControls
import com.grafizum.repio.ui.screens.levelPillBlockedReason
import com.grafizum.repio.ui.screens.levelPillStyle
import com.grafizum.repio.ui.components.stageDividerSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The level card's two controls, and what each of them may do.
 *
 * REWRITTEN 2026-10-08. The owner changed the card and this test follows the change
 * rather than being deleted with the code it used to cover:
 *
 *   "instead of having that '+' and '-' to change roman number, we will open the
 *    full exercise edit screen like normal ... instead of the pencil icon in the top
 *    card where the exercise is shown, we should only show the pencil in the bottom
 *    card where sets and reps are".
 *
 * So the Roman-stage stepper and the two-step edit pass (pencil -> edit mode -> tick)
 * are gone. What survives is two rules, and they are asserted here because they are
 * the two ways this card can lie to the user:
 *
 *   - the PENCIL is always on the card. It is now the ONLY route into the exercise
 *     editor from a live session, so hiding it — for any exercise, level-managed or
 *     not — would leave that exercise uneditable from the workout screen.
 *   - the level PILL answers to the persisted `routine_exercises.locked` pin, which
 *     is the user's own "stay on this level". Unlocked and inside the level system it
 *     opens the ladder directly; anything else refuses and explains itself through
 *     [levelPillBlockedReason].
 *
 * The superseded 2026-10-01 contract ("while in edit mode the + and - are shown next
 * to the roman level number but hidden once locked in") is deliberately NOT kept as a
 * dead parameter: `levelEditControls` no longer takes an `editing` flag, so no future
 * caller can reintroduce a third state by passing one.
 */
class LevelEditControlsTest {

    /** An exercise inside the level system, unlocked - the ordinary state. */
    private fun idle() = levelEditControls(adjustable = true, locked = false)

    @Test
    fun anUnlockedExerciseOffersBothThePencilAndAPressablePill() {
        val c = idle()
        assertTrue("the pencil is the only way into the editor now", c.showPencil)
        assertTrue("an unlocked level must be reachable from the card", c.levelPillEnabled)
    }

    @Test
    fun aCommittedLevelRefusesThePillButKeepsThePencil() {
        val committed = levelEditControls(adjustable = true, locked = true)
        assertTrue("the pencil must stay reachable once committed", committed.showPencil)
        assertFalse("the level card is not pressable once locked in", committed.levelPillEnabled)
    }

    @Test
    fun thePencilIsReachableFromEveryStateTheAppCanBeIn() {
        for (adjustable in listOf(true, false)) {
            for (locked in listOf(true, false)) {
                val c = levelEditControls(adjustable, locked)
                assertTrue(
                    "pencil unreachable: adjustable=$adjustable locked=$locked",
                    c.showPencil
                )
            }
        }
    }

    /**
     * An exercise the level system cannot rate has no pill to press - there is no
     * ladder behind it - but it KEEPS the pencil, and that exception is the whole
     * point of asserting it.
     *
     * `locked` is a column on the routine row, not a property of the level system:
     * the evolution-deny path writes it, and so does the user's own hand. So an
     * exercise can be locked while carrying no level at all, and hiding the pencil
     * then would make that lock PERMANENT - the one control that clears it is the one
     * that disappeared. (The same reasoning now applies to custom-mode exercises,
     * which is why the pencil is unconditional.)
     */
    @Test
    fun anExerciseOutsideTheLevelSystemKeepsItsPencil() {
        val unlocked = levelEditControls(adjustable = false, locked = false)
        assertTrue("custom mode still edits name, demo link and its own reps", unlocked.showPencil)
        assertFalse("there is no level to jump to", unlocked.levelPillEnabled)

        val lockedOutside = levelEditControls(adjustable = false, locked = true)
        assertTrue(
            "a locked exercise with no level would be permanently locked",
            lockedOutside.showPencil
        )
        assertFalse("there is no level pill to press", lockedOutside.levelPillEnabled)
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
     * SUPERSEDED TWICE, AND THE SECOND TIME IN THE OPPOSITE DIRECTION.
     *
     * The 2026-10-04 inversion made an open edit pass the SOLID pill and a
     * committed level the ring, and this case pinned that mapping in both
     * directions ("the states differ" cannot tell the two versions apart, only the
     * direction can). The 2026-10-09 ruling retires the whole idea of a
     * state-dependent level label:
     *
     *   *"dont change the level label when locking or unlocking an exercise. Keep
     *   the label as a thin border around the level instead."*
     *
     * So the old case is DELETED rather than weakened, and its replacement asserts
     * the opposite property: no state may change the label at all. Leaving a
     * "they must differ" assertion in place would have been asserting the exact
     * behaviour the owner just forbade. The lock is still visible - it is the lock
     * CHIP beside the label that fills when it is on - and this test is what stops
     * the next editor from making the label carry it again.
     */
    @Test
    fun theLevelLabelLooksTheSameLockedAndUnlocked() {
        for (progressionAvailable in listOf(true, false)) {
            val unlocked = levelPillStyle(editing = true, progressionAvailable = progressionAvailable)
            val locked = levelPillStyle(editing = false, progressionAvailable = progressionAvailable)

            assertEquals(
                "the ring must not move with the lock (progressionAvailable=$progressionAvailable)",
                unlocked.borderWidthDp,
                locked.borderWidthDp
            )
            assertEquals(
                "nor the horizontal padding",
                unlocked.horizontalPaddingDp,
                locked.horizontalPaddingDp
            )
            assertEquals(
                "nor the vertical padding",
                unlocked.verticalPaddingDp,
                locked.verticalPaddingDp
            )
            assertEquals(
                "nor a shadow",
                unlocked.shadowElevationDp,
                locked.shadowElevationDp
            )
        }
    }

    /**
     * It is a THIN border - one hairline, in every combination of states.
     *
     * The ring used to be 2dp whenever it was the committed level, which is the
     * "heavy" reading of a label the owner has now asked to be thin. Both flags
     * are swept because the failure this guards is a future editor reintroducing a
     * branch: any branch that changes the width would have to change it for at
     * least one of these four calls.
     */
    @Test
    fun theLevelLabelIsAThinBorderInEveryState() {
        for (editing in listOf(true, false)) {
            for (progressionAvailable in listOf(true, false)) {
                assertEquals(
                    "owner: \"keep the label as a thin border around the level\" " +
                        "(editing=$editing, progressionAvailable=$progressionAvailable)",
                    1,
                    levelPillStyle(editing = editing, progressionAvailable = progressionAvailable).borderWidthDp
                )
            }
        }
    }

/**
 * The bar between the level and its Roman stage is a HAIRLINE, and it is never typed.
 *
 * Owner, 2026-10-09: *"make the line that separates level with roman number, thinner so it
 * doesnt look like a number or letter"*. The reading used to be assembled as a String - 
 * `"Level 10" + LEVEL_STAGE_SEPARATOR + "IV"` - which put a full `|` glyph between two
 * bold numerals at the same height as them, so it resolved as one more digit rather than
 * as a divider. The fix is that the bar is DRAWN ([LevelStageDivider], fed by this spec)
 * and that the label is one composable shared by the card and the guide's example of it.
 *
 * This pins the property the owner asked for, on the value rather than on the pixels: one
 * dp, under the cap height of the text around it, and translucent enough to group the two
 * readings instead of competing with them. A branch that made the bar 2dp "to match the
 * ring" would have to fail here first.
 */
@Test
fun theStageDividerIsAHairlineAndNotACharacter() {
    val spec = stageDividerSpec()
    assertEquals("a 1dp rule is the whole fix: it has no glyph to read", 1, spec.widthDp)
    assertTrue(
        "the bar must not stand taller than the text it divides (was ${spec.heightDp}dp)",
        spec.heightDp <= 14
    )
    assertTrue(
        "a full-strength bar reads as text again (was ${spec.alpha})",
        spec.alpha < 1f
    )
    assertTrue(
        "the bar needs breathing room or it fuses with the numerals (was " +
            "${spec.horizontalPaddingDp}dp)",
        spec.horizontalPaddingDp >= 4
    )
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

    /*
     * DELETED, 2026-10-09: `theTwoPillStatesStayVisiblyDifferent`.
     *
     * Its premise was that the two states MUST be told apart by the label (ring
     * against fill, ring weight against hairline), which is the opposite of the
     * owner's ruling that locking must not change the label at all. Its intent -
     * "the user must be able to see the lock" - is carried by the card's lock chip,
     * not by this pure style function, so the coverage moves rather than weakens.
     */

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
            assertEquals(
                "and the thin ring is not a compaction knob (editing=$editing)",
                roomy.borderWidthDp,
                compact.borderWidthDp
            )
        }
    }
}
