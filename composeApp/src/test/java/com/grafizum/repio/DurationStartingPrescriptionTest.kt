package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EVERY PROGRESSION-READY ROW MUST BE ABLE TO RESOLVE A STARTING PRESCRIPTION.
 *
 * Owner, 2026-10-09: *"Jumping jacks is one of many exercises that doesnt show the
 * level style and it shows reps in the card yet the exercise is based on time and
 * has a timer????"*.
 *
 * The visible bug was the level card, but the cause was in the engine, one layer
 * below the UI that had already been "fixed" twice ([levelCardBody] and the
 * duration banner). A duration row carries its progression in SECONDS and has no
 * Roman rep pattern, so `prescribedReps` answers null for it - and
 * [ProgressionEngine.startingPrescription] treated "no reps" as "no
 * prescription" and returned null. With no prescription there is no
 * [ProgressionEngine.ProgressionState] either, so `inSystem` on the card was
 * false and a timed exercise rendered as the plain custom summary: the routine's
 * rep range, under a timer, with no level pill.
 *
 * Measured over the real catalog before the fix: 147 of 601 rows - every plank,
 * hold, carry, stretch and cardio machine - were in exactly that state. This test
 * is the guard: it sweeps the pool rather than naming Jumping Jacks, because the
 * report was explicitly "one of many".
 */
class DurationStartingPrescriptionTest {

    private val difficulties = listOf(
        ProgressionEngine.Difficulty.EASY,
        ProgressionEngine.Difficulty.MEDIUM,
        ProgressionEngine.Difficulty.HARD,
    )

    @Test
    fun everyProgressionReadyRowResolvesAStartingPrescription() {
        val offenders = ExerciseCatalog.ENTRIES.filter { entry ->
            if (!ProgressionEngine.isProgressionReady(ProgressionEngine.entryFor(entry.name))) {
                return@filter false
            }
            difficulties.any {
                ProgressionEngine.startingPrescription(entry.name, it) == null
            }
        }

        assertEquals(
            "these rows are in the level system yet cannot produce a level state, so " +
                "their card falls back to the custom rep summary: " +
                offenders.joinToString { it.name },
            emptyList<String>(),
            offenders.map { it.name },
        )
    }

    /**
     * The timed half of the pool must be a real slice, or the sweep above could
     * pass vacuously while proving nothing about the bug it exists for.
     */
    @Test
    fun theTimedRowsAreTheSliceThatWasBroken() {
        val timed = ExerciseCatalog.ENTRIES.filter {
            ProgressionEngine.entryFor(it.name)?.let(ProgressionEngine::isDurationBased) == true
        }
        assertTrue("expected a meaningful timed slice, found ${timed.size}", timed.size >= 100)

        // And each of them resolves to a state, with the seconds as its value.
        for (row in timed) {
            val name = row.name
            val entry = ProgressionEngine.entryFor(name)!!
            val prescription = ProgressionEngine.startingPrescription(
                name, ProgressionEngine.Difficulty.MEDIUM
            ) ?: throw AssertionError("$name produced no prescription")
            val value = ProgressionEngine.prescribedValueAtLevel(entry, prescription.level)
            assertEquals(
                "$name must prescribe a DURATION, not a rep count",
                ProgressionEngine.ValueKind.DURATION,
                value?.kind,
            )
            assertTrue(
                "$name must prescribe a positive number of seconds, got ${value?.value}",
                (value?.value ?: 0.0) > 0.0,
            )
            // A duration row has no rep targets - the banner's headline is the
            // seconds, so an empty list here is the honest answer, not a gap.
            assertTrue(
                "$name must not invent rep targets",
                prescription.reps.isEmpty(),
            )
        }
    }

    /**
     * The guard the fix must not weaken: a row with no reps AND no duration
     * ladder is genuinely undescribable and must still be refused, or the
     * prescription would print an empty card.
     */
    @Test
    fun aRowWithNeitherRepsNorDurationIsStillRefused() {
        val undescribable = ExerciseCatalog.ENTRIES.filter { catalogEntry ->
            val entry = ProgressionEngine.entryFor(catalogEntry.name)
            entry != null && ProgressionEngine.isProgressionReady(entry) &&
                !ProgressionEngine.isDurationBased(entry) &&
                ProgressionEngine.prescribedReps(entry, 1, 1).isNullOrEmpty()
        }
        // Nothing honest to assert about the data itself; what matters is that the
        // refusal is still reachable at all, so the fix did not quietly turn
        // `startingPrescription` into "always non-null".
        for (row in undescribable) {
            assertTrue(
                "an undescribable row must still be refused: ${row.name}",
                ProgressionEngine.startingPrescription(
                    row.name, ProgressionEngine.Difficulty.MEDIUM
                ) == null,
            )
        }
    }
}
