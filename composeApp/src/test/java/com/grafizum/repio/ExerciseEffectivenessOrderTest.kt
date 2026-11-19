package com.grafizum.repio

import com.grafizum.repio.data.ExerciseEffectiveness
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE SELECTION IS A RANKING, NOT A DRAW.
 *
 * Owner, 2026-10-22 (dashboard task_0z6e4u5i5h2k0e1p2m4e226m): the day's exercises must not be
 * random, must be ordered from most to least effective per muscle, must respect the equipment the
 * user has, and must not repeat.
 *
 * The four claims are tested on the same generator the welcome screen calls, because that is where
 * the order becomes a plan - a ranking helper nobody consults would pass every unit case here and
 * change nothing the user sees.
 */
class ExerciseEffectivenessOrderTest {

    private val allDays = listOf("MONDAY", "WEDNESDAY", "FRIDAY")

    private fun build(
        days: List<String> = allDays,
        equipment: List<String> = emptyList(),
        duration: String = WorkoutTemplates.DURATION_STANDARD
    ) = WorkoutTemplates.generateEngineProgram(
        selectedDays = days,
        workoutGoal = "goal_bodybuilding",
        equipment = equipment,
        duration = duration
    )

    /** 1. THE ORDER ITSELF: the compound, loadable movement leads its muscle's list. */
    @Test
    fun theMuscleListRunsFromTheLoadedCompoundToTheLightIsolation() {
        val ranked = ExerciseEffectiveness.rankedForMuscle(
            listOf("Push-Up", "Dumbbell Fly", "Barbell Bench Press", "Machine Chest Fly", "Dumbbell Bench Press")
        )
        assertEquals(
            "a barbell compound leads, and isolation may not outrank a loaded compound",
            listOf("Barbell Bench Press", "Dumbbell Bench Press", "Push-Up", "Dumbbell Fly", "Machine Chest Fly"),
            ranked
        )
        assertTrue(
            "compound before isolation holds regardless of kit",
            ranked.indexOf("Push-Up") < ranked.indexOf("Dumbbell Fly")
        )
    }

    /** 2. THE PICK SKIPS WHAT THE WEEK HAS USED, in rank order rather than by alphabet. */
    @Test
    fun thePickTakesTheBestMovementTheWeekHasNotUsed() {
        val pool = listOf("Barbell Bench Press", "Dumbbell Bench Press", "Push-Up")
        val first = ExerciseEffectiveness.bestUnused(pool, slotIndex = 0, slotCount = 4, usedThisWeek = emptySet())
        assertEquals("Barbell Bench Press", first)
        val second = ExerciseEffectiveness.bestUnused(pool, 0, 4, usedThisWeek = setOf(first!!))
        assertEquals("Dumbbell Bench Press", second)
        val third = ExerciseEffectiveness.bestUnused(pool, 0, 4, usedThisWeek = setOf(first, second!!))
        assertEquals("Push-Up", third)
    }

    /** 3. A REPEAT IS THE LAST RESORT, AND NEVER INSIDE ONE DAY. */
    @Test
    fun aRepeatOnlyHappensWhenTheDayHasNothingElseAndNeverTwiceInOneDay() {
        val pool = listOf("Barbell Bench Press", "Push-Up")
        assertEquals(
            "every movement used this week, but one is free today: take it rather than leave the slot empty",
            "Barbell Bench Press",
            ExerciseEffectiveness.bestUnused(pool, 0, 3, usedThisWeek = pool.toSet())
        )
        assertNull(
            "a movement already used today may not be repeated in the same session",
            ExerciseEffectiveness.bestUnused(
                pool, 0, 3,
                usedThisWeek = pool.toSet(),
                usedToday = pool.toSet()
            )
        )
    }

    /** 4. THE GENERATED WEEK IS DETERMINISTIC and never repeats a movement while it has options. */
    @Test
    fun aGeneratedWeekDoesNotRepeatWhileThePoolHasUnusedOptions() {
        val program = build()
        val names = program.exercises.map { it.name }
        assertTrue("the build produced no exercises", names.size >= 6)
        assertEquals(
            "the same answers must produce the same week, or the picks are a draw again",
            names,
            build().exercises.map { it.name }
        )
        assertEquals(
            "a pool this deep must not repeat a movement inside one week: " + names,
            names.distinct().size,
            names.size
        )
    }

    /** 5. COMPOUNDS LEAD THE SESSION, which is what the slot role is for. */
    @Test
    fun theFirstSlotOfADayLeadsWithACompound() {
        val program = build(days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"))
        val firstByDay = program.exercises
            .groupBy { it.dayOfWeek }
            .mapValues { (_, rows) -> rows.minBy { it.orderIndex }.name }
        assertTrue("no days were built", firstByDay.isNotEmpty())
        firstByDay.forEach { (day, name) ->
            assertTrue(
                "$day opens on '$name', which the catalogue does not call a compound",
                ExerciseEffectiveness.isCompound(name)
            )
        }
    }

    /** 6. THE KIT DECIDES THE POOL: a build never hands the user a movement they cannot do. */
    @Test
    fun thePicksStayInsideTheKitTheUserHas() {
        val dumbbellsOnly = build(equipment = listOf("preset_dumbbells"))
        assertFalse("the dumbbells-only build is empty", dumbbellsOnly.exercises.isEmpty())
        val forbidden = listOf("Barbell", "Machine", "Cable")
        dumbbellsOnly.exercises.forEach { ex ->
            val equipment = WorkoutTemplates.getEquipmentForExercise(ex.name)
            assertFalse(
                "'${ex.name}' ($equipment) cannot be done with a pair of dumbbells",
                equipment in forbidden
            )
        }
    }

    /** 7. A SHORT DECK IS STILL FILLED: no part is dropped when only the repetition rule bites. */
    @Test
    fun aBodyweightOnlyWeekStillFillsEveryDay() {
        val program = build(equipment = listOf("preset_bw_pullup_bar"), days = allDays)
        val holes = program.unsatisfiedParts
            .filter { it.contains("no exercise left in the gated pool") }
            .map { it.substringBefore(" (") }
            .distinct()
        assertEquals(
            "the only kit gap a pull-up bar leaves is the known one (no bodyweight biceps " +
                "movement exists); anything else is a part the repetition rule starved: $holes",
            listOf("Biceps"),
            holes
        )
        assertNotNull(program.exercises.firstOrNull())
    }
}
