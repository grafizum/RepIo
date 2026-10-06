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

    /**
     * THE REAL PATH, and the one the cases above miss.
     *
     * A user finishes a level at its LAST Roman stage - the most reps that level
     * ever asks for - and the level-up resets the stage to I. Those two states
     * have to be compared, not two stage-I states: the Roman stage adds up to
     * four reps on top of the level's own count, so a level that steps by fewer
     * than its pattern climbs hands back FEWER reps the moment it advances.
     *
     * Owner report 2026-10-06: "when the rep is max of level 3 (lets say 12) and
     * level 4 comes, it jumps from 12 to 8 as if the weight would have been
     * increased, but since there is no weight to increase it makes no sense."
     *
     * Swept across every rep ladder in the pool, because the fix is in the
     * engine and a single exercise would prove nothing.
     */
    @Test
    fun levelingUpFromTheLastStageNeverDemotesTheReps() {
        var checked = 0
        for (entry in repProgressedEntries()) {
            if (!ProgressionEngine.isRepLadder(entry)) continue
            checked++
            val max = ProgressionEngine.effectiveMaxLevel(entry)
            val lastStage = ProgressionEngine.patternLength(entry)
            for (level in 1 until max) {
                val finishing = ProgressionEngine.prescribedReps(entry, level, lastStage)!!.max()
                val nextStart = ProgressionEngine.prescribedReps(entry, level + 1, 1)!!.max()
                assertTrue(
                    "${entry.slug}: L$level last stage asks $finishing reps but L${level + 1} " +
                        "opened on $nextStart - a level-up must never reduce the work",
                    nextStart >= finishing
                )
            }
        }
        assertTrue("expected rep ladders to check, got $checked", checked > 100)
    }

    /**
     * And the whole climb is monotonic, not just the level boundaries: walking
     * level by level and stage by stage, the reps never fall anywhere.
     */
    @Test
    fun theRepCountNeverFallsAnywhereAlongTheLadder() {
        for (entry in repProgressedEntries()) {
            if (!ProgressionEngine.isRepLadder(entry)) continue
            val max = ProgressionEngine.effectiveMaxLevel(entry)
            val stages = ProgressionEngine.patternLength(entry)
            var previous = 0
            for (level in 1..max) {
                for (stage in 1..stages) {
                    val reps = ProgressionEngine.prescribedReps(entry, level, stage)!!.max()
                    assertTrue(
                        "${entry.slug}: L$level S$stage asks $reps, below the previous step's $previous",
                        reps >= previous
                    )
                    previous = reps
                }
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
        // Reps run from the row's own floor up to the ladder's rep ceiling.
        //
        // NOT up to the row's repMax: that was the variation-era ceiling (12 for
        // most bodyweight rows) and the owner raised the ceiling on 2026-10-03
        // ("you can increase the count up to 25 or even 50"). What still has to
        // hold is the upper bound, because the Roman cascade adds up to four
        // reps to the set the user reaches last - without a cap, the top level
        // asked for more reps than the ladder's own ceiling says exist.
        for (entry in repProgressedEntries()) {
            val min = ProgressionEngine.repFloorOf(entry)
            val ceiling = ProgressionEngine.repCeilingFor(entry)
            val levels = ProgressionEngine.effectiveMaxLevel(entry)
            for (level in 1..levels) {
                for (stage in 1..ProgressionEngine.patternLength(entry)) {
                    val reps = ProgressionEngine.prescribedReps(entry, level, stage) ?: continue
                    reps.forEach { rep ->
                        assertTrue("${entry.id} L$level S$stage rep $rep < floor $min", rep >= min)
                        assertTrue("${entry.id} L$level S$stage rep $rep > ceiling $ceiling", rep <= ceiling)
                    }
                }
            }
        }
    }

    /**
     * The Roman stage DISTRIBUTES the level's reps across the sets - the shape
     * the pool already carries (extra reps walk forward, one set at a time) -
     * rather than being flattened into the same number three times.
     */
    @Test
    fun theRomanStageDistributesTheLevelsRepsAcrossTheSets() {
        val entry = ProgressionEngine.entryFor("Crunches")!!
        // Mid-pattern is where the cascade lives. The pattern's LAST stage is
        // [12,12,12] - flat - because by then every set has caught up, so the
        // top stage is not where a distribution is visible. Asserting it there
        // would have been asserting against the shape the data actually has.
        val atLevel5 = ProgressionEngine.prescribedReps(entry, 5, 5)!!
        // Crunches is core from a floor of 8, and its ladder now steps by 5 -
        // the Roman climb (4) plus one, so the level's band holds the whole
        // stage climb and a level-up can never demote the reps. Level 5 asks
        // 28 reps. The pattern's own stage-5 row [10,9,9] minus stage I's
        // [8,8,8] is [2,1,1] - the extra reps walking forward onto the first
        // set - so the sets read 30 / 29 / 29.
        val levelReps = ProgressionEngine.repsAtLevel(entry, 5)
        assertEquals(28, levelReps)
        // Every set is at least the level's own count: the stage adds work, it
        // never takes any away.
        assertTrue("stage 5 took reps away: $atLevel5", atLevel5.all { it >= levelReps })
        // And it is genuinely a DISTRIBUTION, not three identical numbers.
        assertEquals(listOf(30, 29, 29), atLevel5)
    }

    /**
     * The cascade's shape holds at EVERY stage of the pool's own pattern: the
     * sets never ascend left to right, because the growth front is a property
     * of the row and a pattern that inverted it would read as a typo.
     */
    @Test
    fun noStageAsksMoreOfALaterSetThanOfAnEarlierOne() {
        for (entry in repProgressedEntries()) {
            if (!ProgressionEngine.isRepLadder(entry)) continue
            for (stage in 1..ProgressionEngine.patternLength(entry)) {
                val reps = ProgressionEngine.prescribedReps(entry, 1, stage) ?: continue
                for (i in 0 until reps.size - 1) {
                    assertTrue(
                        "${entry.id} stage $stage ascends: $reps",
                        reps[i] >= reps[i + 1]
                    )
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