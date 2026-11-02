package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WorkoutTemplates
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ONE LEVEL, ONE REP NUMBER - across every surface that names it.
 *
 * Owner, 2026-10-31 (dashboard task_2r6p1p3j2e405v701f2c186y): *"If the exercise has 2 or 4 reps
 * then it should show the same amount in the level list."* Two defects sat behind that one line,
 * and this class holds both of them down.
 *
 * 1. **The add form and the level list disagreed about the same rung.** A rep ladder's level IS its
 *    rep count, so a prescription at level 2 is whatever the ladder says at 2 - but
 *    [ProgressionEngine.startingPrescription] paired the ramp's rung with the CSV's own start
 *    STAGE, and that stage is II or III on 74 rows. Ball Pike level 2 therefore opened the add
 *    form on 9 / 9 / 9 while the level list's row for level 2 read 8 / 8 / 8, and the same
 *    disagreement covered the 73 other rows. A rep ladder now opens on stage I, where its
 *    difficulty's rung is the whole prescription.
 *
 * 2. **A loaded rung printed its plates and not its reps.** A clean's level 1 is "20 kg" AND
 *    "2 / 1 / 1 reps"; the list printed the load alone, so the row's own rep pattern - the 2 the
 *    owner was looking at - was unreachable from the screen that exists to say what a rung costs.
 *    The list now prints both, read from the same [ProgressionEngine.prescribedReps] the card uses.
 */
class LevelListRepsMatchTest {

    private val dialogSource =
        File("src/main/java/com/grafizum/repio/ui/screens/LevelDetailsDialog.kt").readText()

    /** Comments stripped, so a rule mentioned in prose is not mistaken for a rule in code. */
    private val dialogCode = dialogSource
        .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        .replace(Regex("//[^\\n]*"), "")

    private val difficulties = listOf(
        ProgressionEngine.Difficulty.EASY,
        ProgressionEngine.Difficulty.MEDIUM,
        ProgressionEngine.Difficulty.HARD
    )

    /**
     * THE REPORTED DEFECT, as a sweep: for every row in the pool and every difficulty, the reps the
     * ADD FORM opens on and the reps the LEVEL LIST shows for that same level must be the same
     * list. Measured before the fix: 74 disagreements, every one of them the add form's stage.
     */
    @Test
    fun theAddFormAndTheLevelListPrescribeTheSameRepsForTheSameLevel() {
        val mismatches = mutableListOf<String>()
        for (difficulty in difficulties) {
            for (name in WorkoutTemplates.getAllExerciseNames()) {
                val entry = ProgressionEngine.entryFor(name) ?: continue
                if (!ProgressionEngine.isProgressionReady(entry)) continue
                val start = ProgressionEngine.startingPrescription(name, difficulty) ?: continue
                val rung = ProgressionEngine.ladderRungs(entry)
                    .firstOrNull { it.level == start.level } ?: continue
                if (start.reps != rung.stageReps) {
                    mismatches.add(
                        "$difficulty $name level ${start.level}: " +
                            "add form ${start.reps}, level list ${rung.stageReps}"
                    )
                }
            }
        }
        assertEquals(
            "one level may not prescribe two different rep amounts:\n" +
                mismatches.joinToString("\n"),
            emptyList<String>(),
            mismatches
        )
    }

    /** A fresh rep-ladder prescription starts on stage I, so its rung's number IS its number. */
    @Test
    fun aRepLadderOpensOnStageOne() {
        for (difficulty in difficulties) {
            for (name in WorkoutTemplates.getAllExerciseNames()) {
                val entry = ProgressionEngine.entryFor(name) ?: continue
                if (!ProgressionEngine.isProgressionReady(entry)) continue
                if (!ProgressionEngine.isRepLadder(entry)) continue
                val start = ProgressionEngine.startingPrescription(name, difficulty) ?: continue
                assertEquals(
                    "$name ($difficulty) must open on the ladder's own rung, not a stage above it",
                    1,
                    start.stage
                )
            }
        }
    }

    /**
     * THE 2-REP HALF: every loaded rung must HAVE reps for the list to print. A load-only sweep is
     * what makes the surface claim honest rather than a promise a dialog comment makes.
     */
    @Test
    fun everyLoadedRungCarriesTheRepsTheListPrints() {
        val missing = mutableListOf<String>()
        for (name in WorkoutTemplates.getAllExerciseNames()) {
            val entry = ProgressionEngine.entryFor(name) ?: continue
            if (!ProgressionEngine.isProgressionReady(entry)) continue
            for (rung in ProgressionEngine.ladderRungs(entry)) {
                val prescribed = rung.prescribed ?: continue
                if (prescribed.kind != ProgressionEngine.ValueKind.LOAD) continue
                if (rung.stageReps.isEmpty()) missing.add("$name level ${rung.level}")
            }
        }
        assertEquals(
            "a loaded rung without reps leaves the 2-or-4 unreachable:\n" +
                missing.joinToString("\n"),
            emptyList<String>(),
            missing
        )
    }

    /**
     * And the row must actually DRAW them: the composition is private Compose, so this pins the
     * one call site that joins a LOAD prescription to the rung's reps.
     */
    @Test
    fun theLoadedRowJoinsItsRepsToTheLoad() {
        assertTrue(
            "the LOAD branch of the level row must reach the rung's own reps",
            dialogCode.contains("ValueKind.LOAD") &&
                dialogCode.contains("prescriptionDisplayText(row.prescribed) + \" · \"") &&
                dialogCode.contains("row.stageReps.joinToString")
        )
    }
}
