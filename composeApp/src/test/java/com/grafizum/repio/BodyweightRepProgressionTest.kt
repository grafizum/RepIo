package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rep progression for exercises that carry no external load (dashboard task
 * #4: "Crunches needs to be fixed, each level has to be more max reps. And when
 * switching level don't reduce the reps … instead show the reps").
 *
 * The bug was structural rather than a bad number: reps came only from the
 * Roman STAGE, so a bodyweight exercise had no way to ask for more work at a
 * higher level, and a level-up reset to the pattern floor ([8,8,8]) — a
 * visible rep DROP at the exact moment the user earned a level.
 *
 * These tests hold the two properties the owner asked for across the WHOLE
 * rep-progressed part of the pool, not just Crunches, because the fix is in
 * the engine and every bodyweight row inherits it.
 */
class BodyweightRepProgressionTest {

    /**
     * Every progression-ready row that progresses by reps rather than a load AND
     * carries a Roman pattern to scale. Duration rows with no pattern at all
     * (a plain hold) prescribe seconds, not reps, so there is nothing here to
     * hold — [ProgressionEngine.prescribedReps] returns null for them by design.
     */
    private fun repProgressedEntries() =
        ExerciseProgression.ENTRIES.filter {
            ProgressionEngine.isRepProgressed(it) &&
                ProgressionEngine.romanStageReps(it, 1) != null
        }

    @Test
    fun crunchesGainsRepsAtEveryLevel() {
        val entry = ProgressionEngine.entryFor("Crunches")
        assertNotNull("Crunches must be in the level system", entry)
        entry!!
        val max = ProgressionEngine.effectiveMaxLevel(entry)
        val firstSet = (1..max).map {
            ProgressionEngine.prescribedReps(entry, it, 1)!!.first()
        }
        println("Crunches reps by level: $firstSet")
        // Each level must ask for strictly more than the one below it.
        for (i in 1 until firstSet.size) {
            assertTrue(
                "Crunches level ${i + 1} ($firstSet[i]) must exceed level $i (${firstSet[i - 1]})",
                firstSet[i] > firstSet[i - 1]
            )
        }
    }

    @Test
    fun repsNeverDropWhenTheLevelChanges() {
        // The specific complaint: switching level must not reduce reps.
        for (entry in repProgressedEntries()) {
            val max = ProgressionEngine.effectiveMaxLevel(entry)
            for (level in 1 until max) {
                val lower = ProgressionEngine.prescribedReps(entry, level, 1)!!.max()
                val upper = ProgressionEngine.prescribedReps(entry, level + 1, 1)!!.max()
                assertTrue(
                    "${entry.id}: level ${level + 1} ($upper) dropped below level $level ($lower)",
                    upper >= lower
                )
            }
        }
    }

    @Test
    fun aLevelUpNeverDemotesTheReps() {
        // Walking the real level-up path from the top stage of each level must
        // not hand back fewer reps than the user had just completed.
        for (entry in repProgressedEntries()) {
            val max = ProgressionEngine.effectiveMaxLevel(entry)
            val lastStage = ProgressionEngine.patternLength(entry)
            for (level in 1 until max) {
                val before = ProgressionEngine.prescribedReps(entry, level, lastStage)!!.max()
                val up = ProgressionEngine.levelUp(entry, ProgressionEngine.ProgressionState(level, lastStage))
                    ?: continue
                assertTrue(
                    "${entry.id}: level-up from L$level dropped reps $before -> ${up.reps.max()}",
                    up.reps.max() >= before
                )
            }
        }
    }

    @Test
    fun repsStayInsideTheRowsOwnCeiling() {
        // Reps are clamped to the row's declared repMin..repMax: the level can
        // scale them, but never past what the data says the exercise allows.
        for (entry in repProgressedEntries()) {
            val min = entry.repMin
            val max = entry.repMax
            if (min == null || max == null) continue
            val levels = ProgressionEngine.effectiveMaxLevel(entry)
            for (level in 1..levels) {
                for (stage in 1..ProgressionEngine.patternLength(entry)) {
                    val reps = ProgressionEngine.prescribedReps(entry, level, stage) ?: continue
                    reps.forEach { rep ->
                        assertTrue("${entry.id} L$level S$stage rep $rep < repMin $min", rep >= min)
                        assertTrue("${entry.id} L$level S$stage rep $rep > repMax $max", rep <= max)
                    }
                }
            }
        }
    }

    @Test
    fun loadedExercisesKeepTheirStageRepsExactly() {
        // The change must not touch a single loaded row: there the LOAD is the
        // progression, and reps are the stage's own row.
        for (entry in ExerciseProgression.ENTRIES) {
            if (!ProgressionEngine.isProgressionReady(entry)) continue
            if (ProgressionEngine.valueKindOf(entry) != ProgressionEngine.ValueKind.LOAD) continue
            for (stage in 1..ProgressionEngine.patternLength(entry)) {
                assertEquals(
                    "${entry.id} stage $stage",
                    ProgressionEngine.romanStageReps(entry, stage),
                    ProgressionEngine.prescribedReps(entry, 3, stage)
                )
            }
        }
    }
}