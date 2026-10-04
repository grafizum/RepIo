package com.grafizum.repio

import com.grafizum.repio.ui.components.repsLockedDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A LOCKED reps field must never print its own LABEL as its value.
 *
 * The owner's report (board task_1d1x205z): "fix its format so that the reps
 * placeholder also moves away and doesnt stay glitched inside the input box".
 * That is one bug with two halves, and both halves are in this file:
 *
 *  1. The locked branch rendered `value.ifBlank { getString("reps") }` - so a row
 *     with NO prescribed reps displayed the word "Reps" as though it were the
 *     prescription. The label was doing the value's job.
 *  2. The `DecorationBox` was handed `value = if (isExpanded) " " else ""`, and
 *     `isExpanded` is forced false whenever the field is disabled. A blank value
 *     is exactly what stops Material floating the label, so the word sat parked
 *     in the MIDDLE of the box, on top of the border rather than on it.
 *
 * Both are silent: nothing throws, the build is green, and the field still
 * *contains* the right information - it is just drawn in the wrong place with the
 * wrong word. So the rule is pinned here, in the one function that decides it.
 */
class LockedRepsFieldTest {

    @Test
    fun aLockedFieldShowsThePrescribedRange() {
        assertEquals("8–12", repsLockedDisplay("8-12"))
    }

    @Test
    fun theEnDashSpellingIsPreserved() {
        // The pool and the picker both write "–" as well as "-", so the locked
        // read must not normalise one into the other and change the digits the
        // user is looking at.
        assertEquals("8–12", repsLockedDisplay("8–12"))
    }

    /**
     * THE BUG. The label must never become the value: a greyed box with nothing
     * prescribed is EMPTY, and "Reps" inside it reads as "you should do Reps".
     */
    @Test
    fun anEmptyValuePrintsAnEmDashAndNeverTheLabel() {
        for (blank in listOf("", "   ", "-", "–")) {
            val shown = repsLockedDisplay(blank)
            assertEquals("a blank value must print the em dash, not $shown", "—", shown)
            assertFalse(
                "the field label leaked into the value slot: \"$shown\"",
                shown.equals("Reps", ignoreCase = true) || shown.equals("Wiederholungen", ignoreCase = true)
            )
        }
    }

    /** The em dash is the app's own "no measurement" glyph, not an arbitrary one. */
    @Test
    fun theEmptyGlyphMatchesTheRestOfTheApp() {
        // `overall_level_none` is exactly this character. Two screens inventing
        // two different glyphs for "nothing to show" is how they drift apart.
        assertEquals("—", repsLockedDisplay(""))
    }

    /**
     * The whole-value rule. The locked read shows the WHOLE range, never one
     * half of it: the split min/max editor is for editing, and a locked "8-12"
     * that rendered just "8" would understate the prescription.
     */
    @Test
    fun theWholeRangeSurvivesLocking() {
        val shown = repsLockedDisplay("8-12")
        assertTrue("the max half was lost: \"$shown\"", shown.contains("12"))
        assertTrue("the min half was lost: \"$shown\"", shown.contains("8"))
    }

    /** Surrounding whitespace is layout noise, not part of the value. */
    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals("8–12", repsLockedDisplay("  8-12  "))
    }
}