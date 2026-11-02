package com.grafizum.repio

import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.ui.viewmodel.todaysRecapFromLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The day's recap as the UI shows it — every number comes out of the log table,
 * because it must outlive the process that drew it.
 *
 * Owner prompt, 2026-10-17: *"The workout completed doesnt permanently show the
 * recap of the day what was done what muscle groups were trained on today."*
 *
 * The failure mode this guards is the old in-memory accumulator: it died with
 * the process while the "completed" flag survived, so the card vanished on a
 * restart. These cases hold the three properties the replacement exists for:
 * the day gate (yesterday cannot impersonate today), the split counting sets
 * PLANNED to be trained — now counting sets ACTUALLY logged — and the exercise
 * rows naming only what a real log line supports. The builder is a pure
 * top-level function specifically so none of this needs an Android renderer.
 */
class TodaysRecapFromLogTest {

    private val startOfToday = 1_000_000L

    private fun set(
        name: String = "Push-Up",
        muscle: String = "Chest",
        ts: Long = startOfToday + 60_000L,
        reps: Int = 10,
        weight: Double = 0.0,
        setNumber: Int = 1,
    ) = LoggedSetEntity(
        timestamp = ts,
        dayOfWeek = "Monday",
        exerciseName = name,
        muscleGroup = muscle,
        setNumber = setNumber,
        weightKg = weight,
        reps = reps,
    )

    /** Nothing on the calendar day means no card at all — not an empty one. */
    @Test
    fun noLoggedSetsTodayMeansNoRecap() {
        assertNull(todaysRecapFromLog(emptyList(), startOfToday))
        // Last week's same-weekday session must not resurrect today's card.
        assertNull(
            "a set from before midnight can never fill today's card",
            todaysRecapFromLog(listOf(set(ts = startOfToday - 1L)), startOfToday)
        )
    }

    @Test
    fun countsTheDayAndDerivesVolumeFromWhatWasLogged() {
        val recap = todaysRecapFromLog(
            listOf(
                set(name = "Bench Press", ts = startOfToday + 1_000, weight = 40.0, reps = 8),
                set(name = "Bench Press", ts = startOfToday + 90_000, weight = 40.0, reps = 8, setNumber = 2),
                set(name = "Pull-Up", ts = startOfToday + 180_000, reps = 6),
            ),
            startOfToday
        )!!
        assertEquals(2, recap.exercises)
        assertEquals(3, recap.sets)
        assertEquals("volume is weight x reps summed over the logged sets", 40.0 * 8 + 40.0 * 8, recap.volumeKg!!, 0.001)
    }

    /** A bodyweight or timed session has no tonnage; "0 kg" would read as nothing done. */
    @Test
    fun volumeIsNullWhenNothingWasWeighted() {
        val recap = todaysRecapFromLog(listOf(set()), startOfToday)!!
        assertNull(recap.volumeKg)
    }

    /**
     * The split counts SETS ACTUALLY LOGGED per muscle, most-trained first —
     * not the routine's planned target sets, which credit work nobody did.
     */
    @Test
    fun theMuscleSplitCountsLoggedSetsAndSortsByLoad() {
        val recap = todaysRecapFromLog(
            listOf(
                set(name = "Bench Press", muscle = "Chest"),
                set(name = "Bench Press", muscle = "Chest", setNumber = 2),
                set(name = "Fly", muscle = "Chest", setNumber = 3),
                set(name = "Front Raise", muscle = "Shoulders"),
                set(name = "Plank", muscle = "  "),
            ),
            startOfToday
        )!!
        val split = recap.setsByMuscle
        assertEquals("three logged sets of chest work", 3, split["Chest"])
        assertTrue("a blank muscle must not vanish", split.containsKey("Other"))
        assertEquals("most-trained first", "Chest", split.keys.first())
        assertEquals(3, split.size)
    }

    /**
     * The rows are the "what was done" list: first-logged order (not the order
     * the list happened to arrive in), reps in set order, and only the loads a
     * set really carried — mixed loads are all kept so the UI cannot claim one
     * number covered a varying session.
     */
    @Test
    fun exerciseRowsDescribeExactlyWhatWasLogged() {
        val recap = todaysRecapFromLog(
            listOf(
                // Deliberately out of order on arrival: the row order is the
                // order the user TRAINED in, not the order Room answered in.
                set(name = "Squat", ts = startOfToday + 300_000, weight = 60.0, reps = 5, setNumber = 3),
                set(name = "Bench Press", ts = startOfToday + 1_000, reps = 8),
                set(name = "Squat", ts = startOfToday + 120_000, weight = 60.0, reps = 5),
                set(name = "Squat", ts = startOfToday + 240_000, weight = 62.5, reps = 4, setNumber = 2),
            ),
            startOfToday
        )!!
        assertEquals(listOf("Bench Press", "Squat"), recap.exerciseRows.map { it.exerciseName })

        val squat = recap.exerciseRows[1]
        assertEquals(3, squat.sets)
        assertEquals(listOf(5, 4, 5), squat.reps)
        assertEquals("both really-used loads, tagged as kilograms", listOf(60.0, 62.5), squat.weightsKg)

        val bench = recap.exerciseRows[0]
        assertEquals("an unweighted set carries no load at all", emptyList<Double>(), bench.weightsKg)
    }

    /**
     * Duration is the span of the logged sets. A restart cannot know the real
     * session clock, and a single set has no span — 0 keeps the tile hidden
     * rather than inventing a minute.
     */
    @Test
    fun durationIsTheSpanOfTodaysSets() {
        val recap = todaysRecapFromLog(
            listOf(
                set(ts = startOfToday + 10_000L),
                set(ts = startOfToday + 190_000L, setNumber = 2),
            ),
            startOfToday
        )!!
        assertEquals(180L, recap.durationSeconds)

        val single = todaysRecapFromLog(listOf(set()), startOfToday)!!
        assertEquals(0L, single.durationSeconds)
    }
}