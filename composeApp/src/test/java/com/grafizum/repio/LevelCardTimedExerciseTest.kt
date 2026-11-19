package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.ui.screens.LevelCardBody
import com.grafizum.repio.ui.screens.levelCardBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WHY THE LEVEL CARD DECIDES "IS THIS A HOLD?" FROM THE ENGINE.
 *
 * Owner, 2026-10-04: "some exercises dont show the nice level card style and only
 * show rep range, thats wrong fix it".
 *
 * There were TWO independent causes, and only the second was visible in the
 * function under test:
 *
 *  1. `WorkoutNowScreen` derived it as
 *     `currentExercise.targetReps.contains("sec", ignoreCase = true)` - a sniff of
 *     the ROUTINE'S OWN REP STRING. That string is a rendering of the
 *     prescription, not its source, so a timed exercise whose routine row did not
 *     literally spell "sec" was rendered as a rep exercise, with rep pills. It
 *     now reads `ProgressionEngine.isDurationBased`, which knows from the row's own
 *     `unit`.
 *  2. `levelCardBody` returned SUMMARY for a timed exercise whose duration the
 *     engine could not resolve - and SUMMARY prints `primaryText ?: fallbackReps`,
 *     i.e. THE REP RANGE. That is the literal "only show rep range" of the report,
 *     and it is what [LevelCardBodyTest] now pins the other way.
 *
 * Cause 1 lived in a composable and could not be unit tested, which is why this
 * file exists: it sweeps the real pool so the classification the UI now reads is
 * asserted against actual rows rather than against the handful of names someone
 * thought to type into a test.
 */
class LevelCardTimedExerciseTest {

    /** Every pool row the engine classifies as a timed hold. */
    private val timedRows = ExerciseProgression.ENTRIES.filter { ProgressionEngine.isDurationBased(it) }

    @Test
    fun theEngineClassifiesEveryTimedRowInThePool() {
        // If this were zero the sweep below would pass vacuously, having proved
        // nothing about a change to how the UI asks the question.
        assertTrue(
            "expected real timed exercises in the pool, found ${timedRows.size}",
            timedRows.isNotEmpty(),
        )
        assertTrue(
            "expected the timed rows to be a meaningful slice, found ${timedRows.size}",
            timedRows.size >= 50,
        )
    }

    /**
     * A row whose `unit` says seconds but whose ladder is a rep ladder (or the
     * reverse) would make the two sources of truth disagree, and the UI can only
     * follow one of them.
     */
    @Test
    fun timingComesFromTheRowNotFromItsDisplayName() {
        for (row in timedRows) {
            assertEquals(
                "a row the engine calls timed must report a DURATION value kind " +
                    "(slug=${row.slug}, unit=${row.unit})",
                ProgressionEngine.ValueKind.DURATION,
                ProgressionEngine.valueKindOf(row),
            )
            // The converse: a LOAD row must never be classified as a hold, or the
            // rep banner would start printing seconds.
            if (!ProgressionEngine.isDurationBased(row)) {
                assertTrue(
                    "slug=${row.slug} unit=${row.unit} is not timed",
                    ProgressionEngine.valueKindOf(row) != ProgressionEngine.ValueKind.DURATION,
                )
            }
        }
    }

    /**
     * The owner's blanket wording: "i want no exercise to dismiss the level system
     * display card design". Swept over the REAL pool rather than over a
     * hand-written table of inputs, because the bug lived in the call site and a
     * table would not have reached it.
     */
    @Test
    fun noProgressionReadyExerciseInThePoolFallsToTheCustomSummary() {
        val ready = ExerciseProgression.ENTRIES.filter { ProgressionEngine.isProgressionReady(it) }
        assertTrue("expected progression-ready rows", ready.isNotEmpty())

        val offenders = ready.filter { row ->
            // An exercise with no resolved level is genuinely outside the system
            // (custom mode), which is the one honest SUMMARY. The card body is
            // asked with inSystem = "this exercise has a level", so a ready row
            // always is.
            levelCardBody(
                inSystem = true,
                isTimeBased = ProgressionEngine.isDurationBased(row),
                durationText = null,
            ) == LevelCardBody.SUMMARY
        }

        assertEquals(
            "these rows are in the level system yet would render the custom card: " +
                offenders.take(5).joinToString { it.slug },
            emptyList<String>(),
            offenders.map { it.slug },
        )
    }
}
