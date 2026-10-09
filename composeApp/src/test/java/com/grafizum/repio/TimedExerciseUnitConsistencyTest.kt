package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.screens.LevelCardBody
import com.grafizum.repio.ui.screens.levelCardBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ONE QUESTION, ONE ANSWER: "is this exercise measured in seconds?".
 *
 * Owner, 2026-10-09: *"Jumping jacks is one of many exercises that doesnt show the
 * level style and it shows reps in the card yet the exercise is based on time and
 * has a timer????"*
 *
 * The reason that report kept coming back is that the question had many answers.
 * Every surface that needed it - the live card, the timer button, the view model's
 * auto-selected seconds, the exercise editor's time toggle, and the "3 sets x ..."
 * summary line (written out by hand on the home schedule, the routine row, the
 * setup preview and the card) - decided it for itself, and the ones that did not
 * sniff the routine's stored rep string for "sec" simply always said "reps". That string is a
 * rendering of the prescription, not its source: every row built before the level
 * system stores "8-12" for a jumping jack, so a timed exercise read as a rep
 * exercise on every one of those surfaces while the timer ran underneath it.
 *
 * `WorkoutTemplates.isExerciseTimeBased` is the single definition now: the pool's
 * own `unit` first, and the name heuristic only for an exercise the pool does not
 * carry. [theShippedCodeHasNoSecondDefinition] is the part that keeps it single -
 * a behaviour test cannot see a NEW call site that sniffs the string again, and a
 * new call site is exactly how this came back the previous three times.
 */
class TimedExerciseUnitConsistencyTest {

    private val catalogRows = ExerciseCatalog.ENTRIES.mapNotNull { catalogEntry ->
        ProgressionEngine.entryFor(catalogEntry.name)?.let { catalogEntry.name to it }
    }

    @Test
    fun theUnitComesFromTheRowsOwnUnitWheneverThePoolCarriesTheRow() {
        val offenders = catalogRows.filter { (name, entry) ->
            WorkoutTemplates.isExerciseTimeBased(name) != ProgressionEngine.isDurationBased(entry)
        }
        assertEquals(
            "these rows are classified by a different rule than the engine's own unit: " +
                offenders.take(5).joinToString { it.first },
            emptyList<String>(),
            offenders.map { it.first },
        )
    }

    @Test
    fun theTimedSliceIsRealSoTheSweepsCannotPassVacuously() {
        val timed = catalogRows.filter { ProgressionEngine.isDurationBased(it.second) }
        assertTrue("expected a meaningful timed slice, found ${timed.size}", timed.size >= 100)
        // And the pool is what the sweeps below measure, not a substring of it.
        assertTrue("expected the whole catalog to resolve", catalogRows.size >= 500)
    }

    /**
     * The owner's own exercise, end to end: a level state exists, the prescription
     * is a DURATION, and the card that renders it wears the duration banner. Each
     * step here is one layer of the scan that produced the report.
     */
    @Test
    fun theNamedExerciseIsTimedAtEveryLayerTheReportTouched() {
        val name = "Jumping Jacks"
        val entry = ProgressionEngine.entryFor(name)
            ?: throw AssertionError("$name has no progression row")
        assertTrue("$name must be in the level system", ProgressionEngine.isProgressionReady(entry))
        assertTrue("$name must be timed", WorkoutTemplates.isExerciseTimeBased(name))

        for (difficulty in ProgressionEngine.Difficulty.entries) {
            val prescription = ProgressionEngine.startingPrescription(name, difficulty)
                ?: throw AssertionError("$name produced no $difficulty starting prescription")
            // inSystem on the card is `level != null`; a null state is what made the
            // card fall back to the routine's rep range.
            assertTrue("$name must resolve a level at $difficulty", prescription.level >= 1)

            val value = ProgressionEngine.prescribedValueAtLevel(entry, prescription.level)
                ?: throw AssertionError("$name prescribes nothing at ${prescription.level}")
            assertEquals(
                "$name must prescribe SECONDS, not reps",
                ProgressionEngine.ValueKind.DURATION,
                value.kind,
            )
            assertTrue("$name must prescribe a positive duration", (value.value ?: 0.0) > 0.0)

            // The card body is what the user actually sees, so it is asserted here
            // rather than assumed from the value kind above.
            assertEquals(
                LevelCardBody.BANNER_DURATION,
                levelCardBody(inSystem = true, isTimeBased = true, durationText = "${value.value?.toInt()} s"),
            )
        }
    }

    /**
     * The card's headline for a timed exercise is its seconds, so the seconds have
     * to exist at every rung the picker offers - a null there prints an empty
     * banner, which is the "no level style" half of the same report.
     */
    @Test
    fun everyTimedRowPrescribesSecondsAtEverySelectableLevel() {
        val timed = catalogRows.filter { ProgressionEngine.isDurationBased(it.second) }
        val offenders = mutableListOf<String>()
        for ((name, entry) in timed) {
            val max = ProgressionEngine.effectiveMaxLevel(entry).coerceAtLeast(1)
            val levels = setOf(1, (max + 1) / 2, max)
            for (level in levels) {
                val value = ProgressionEngine.prescribedValueAtLevel(entry, level)
                if (value == null || value.kind != ProgressionEngine.ValueKind.DURATION ||
                    (value.value ?: 0.0) <= 0.0
                ) {
                    offenders += "$name@$level"
                }
            }
        }
        assertEquals(
            "timed rows with no duration to headline at a selectable level: " +
                offenders.take(8).joinToString(),
            emptyList<String>(),
            offenders,
        )
    }

    /**
     * THE STRUCTURAL GUARD. Behaviour tests cannot see a seventh call site that
     * sniffs the stored rep string, and that is how this bug shipped four times.
     * So the shipped code is scanned: the only place allowed to read `sec` off a
     * stored rep string is `WorkoutTemplates.parseRepsRange`, which is PARSING a
     * user's own reps field (it must refuse "30 sec" as a rep range) rather than
     * deciding what an exercise measures.
     */
    @Test
    fun theShippedCodeHasNoSecondDefinition() {
        val offenders = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.path.contains("/build/") }
            .filter { file ->
                val code = stripComments(file.readText())
                // The owner's name for this whole class of bug, in one expression.
                val sniffsTheRepString = code.contains("targetReps", ignoreCase = false) &&
                    code.contains(".contains(\"sec\"")
                sniffsTheRepString && !file.path.endsWith("data/WorkoutTemplates.kt")
            }
            .map { it.path }
            .toList()

        assertEquals(
            "these files decide what an exercise measures from the routine's stored " +
                "rep string; route them through WorkoutTemplates.isExerciseTimeBased: " +
                offenders.joinToString(),
            emptyList<String>(),
            offenders,
        )
    }

    /**
     * THE LEVEL UI'S STAGE ROWS. A timed exercise still carries a Roman rep pattern
     * (that is the shape its ladder was published with), so the editor's stage list
     * printed "8 / 8 / 8 reps" under a jumping jack. [stageRowReps] answers none for
     * a duration row, and the seconds the level asks for are printed on the level
     * itself, not invented per stage.
     */
    @Test
    fun aTimedRowsStagesDoNotClaimReps() {
        val timed = catalogRows.filter { ProgressionEngine.isDurationBased(it.second) }
        assertTrue("expected a timed slice to sweep", timed.size >= 100)
        for ((name, entry) in timed) {
            assertTrue(
                "$name must show no rep targets on its stage rows",
                ProgressionEngine.stageRowReps(entry, 1, 1).isEmpty(),
            )
        }
        // The other direction, so this cannot pass by returning empty for everyone:
        // a load row keeps the rep pattern it has always shown.
        val loadRow = catalogRows.firstOrNull { (name, entry) ->
            !ProgressionEngine.isDurationBased(entry) &&
                !ProgressionEngine.stageRowReps(entry, 1, 1).isEmpty() &&
                ProgressionEngine.prescribedReps(entry, 1, 1)?.isNotEmpty() == true
        } ?: throw AssertionError("expected at least one rep-based row in the pool")
        assertEquals(
            ProgressionEngine.prescribedReps(loadRow.second, 1, 1),
            ProgressionEngine.stageRowReps(loadRow.second, 1, 1),
        )
    }

    /**
     * THE EDITOR'S SECONDS FIELD. Once the time toggle opens in the exercise's own
     * unit, its VALUE has to come from the same place: a pre-level-system timed row
     * stores a rep range, and the editor's save path strips non-digits, so reading
     * "8-12" into the time field would have written back "812 sec".
     */
    @Test
    fun aStoredRepRangeIsNeverReadAsSeconds() {
        assertNull(WorkoutTemplates.secondsInStoredTarget("8–12"))
        assertNull(WorkoutTemplates.secondsInStoredTarget("8-12"))
        assertNull(WorkoutTemplates.secondsInStoredTarget("10 each leg"))
        assertNull(WorkoutTemplates.secondsInStoredTarget(""))
        assertNull(WorkoutTemplates.secondsInStoredTarget(null))
        assertEquals(30, WorkoutTemplates.secondsInStoredTarget("30 sec"))
        assertEquals(90, WorkoutTemplates.secondsInStoredTarget("90 s"))
        assertEquals(45, WorkoutTemplates.secondsInStoredTarget("45"))
    }

    /**
     * THE SAME GUARD FOR THE LABEL. "3 sets x 8–12 reps" was written out by hand on
     * the home schedule, the routine row, the setup preview and the workout card, so
     * fixing the card left three screens still calling a timed exercise a rep
     * exercise. The label now has ONE definition ([exerciseSetsSummaryLabel]); any
     * file that renders it must also be asking the one unit question, which is the
     * cheapest way for a shell to catch a fifth hand-written copy.
     */
    @Test
    fun everyFileThatPrintsTheSetsByRepsLabelAsksTheOneDefinition() {
        val offenders = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.path.contains("/build/") }
            .filter { file ->
                val code = stripComments(file.readText())
                code.contains("sets_x_reps_pl") &&
                    !code.contains("isExerciseTimeBased") &&
                    !code.contains("exerciseSetsSummaryLabel")
            }
            .map { it.path }
            .toList()

        assertEquals(
            "these files choose a reps label without asking the exercise's own " +
                "unit; call exerciseSetsSummaryLabel: " + offenders.joinToString(),
            emptyList<String>(),
            offenders,
        )
    }

    /** Drops `//` line comments and the inside of `/* ... */` blocks. */
    private fun stripComments(source: String): String = source
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("//[^\\n]*"), " ")
}
