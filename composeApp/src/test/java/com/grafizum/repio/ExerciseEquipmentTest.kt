package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the rebuilt exercise pool's equipment classification. The pool is
 * generated (ExerciseCatalog, from the v5 progression database), so the exact
 * entries are data-driven; what this file pins down is the *invariants*: every
 * pool exercise resolves to exactly one real category, the categories stay
 * disjoint, and the catalog data behind a handful of load-bearing movements is
 * what the pipeline decided.
 */
class ExerciseEquipmentTest {

    private val pool = WorkoutTemplates.getAllExerciseNames().map { it.lowercase() }.toSet()
    private val bodyweight = WorkoutTemplates.EQUIPMENT_BODYWEIGHT
    private val dumbbell = WorkoutTemplates.EQUIPMENT_DUMBBELL
    private val barbell = WorkoutTemplates.EQUIPMENT_BARBELL
    private val cable = WorkoutTemplates.EQUIPMENT_CABLE
    private val machine = WorkoutTemplates.EQUIPMENT_MACHINE
    private val kettlebell = WorkoutTemplates.EQUIPMENT_KETTLEBELL
    private val bands = WorkoutTemplates.EQUIPMENT_BANDS
    private val other = WorkoutTemplates.EQUIPMENT_OTHER

    @Test
    fun everyPoolExerciseHasExactlyOneEquipmentEntry() {
        val table = WorkoutTemplates.getEquipmentTableNames()
        val missing = pool - table.toSet()
        assertTrue("Pool exercises with no equipment entry: $missing", missing.isEmpty())

        val strays = table.filter { it !in pool }
        assertTrue("Equipment entries that aren't pool exercises: $strays", strays.isEmpty())

        assertEquals("Each pool exercise must be classified exactly once", pool.size, table.size)
    }

    @Test
    fun everyExerciseResolvesToAKnownCategory() {
        WorkoutTemplates.getAllExerciseNames().forEach { name ->
            val equipment = WorkoutTemplates.getEquipmentForExercise(name)
            assertTrue(
                "'$name' resolved to '$equipment', which is not a known equipment category",
                equipment in WorkoutTemplates.allEquipmentCategories
            )
        }
    }

    @Test
    fun theEquipmentFilterNeverLeaksAnotherCategory() {
        WorkoutTemplates.allEquipmentCategories.forEach { category ->
            WorkoutTemplates.getAllExercisesForEquipment(category).forEach { name ->
                assertEquals(
                    "'$name' is listed under '$category' but needs '${WorkoutTemplates.getEquipmentForExercise(name)}'",
                    category,
                    WorkoutTemplates.getEquipmentForExercise(name)
                )
            }
        }
    }

    @Test
    fun everyCategoryIsPopulated() {
        // A category the pipeline produced but that filters zero exercises is a
        // mapping bug waiting to be discovered in the UI.
        WorkoutTemplates.allEquipmentCategories.forEach { category ->
            assertTrue(
                "'$category' has no exercises in the rebuilt pool",
                WorkoutTemplates.getAllExercisesForEquipment(category).isNotEmpty()
            )
        }
    }

    @Test
    fun equipmentMatchesTheMovementInTheName() {
        // The equipment column comes from the v5 CSV through the pipeline's
        // taxonomy mapping; these are the movements whose category the app
        // actually relies on (starting weights, filters).
        assertEquals(barbell, WorkoutTemplates.getEquipmentForExercise("Barbell Deadlift"))
        assertEquals(barbell, WorkoutTemplates.getEquipmentForExercise("Barbell Back Squat"))
        assertEquals(barbell, WorkoutTemplates.getEquipmentForExercise("Romanian Deadlift"))
        assertEquals(dumbbell, WorkoutTemplates.getEquipmentForExercise("Dumbbell Hammer Curl"))
        assertEquals(dumbbell, WorkoutTemplates.getEquipmentForExercise("Dumbbell Bicep Curl"))
        assertEquals(dumbbell, WorkoutTemplates.getEquipmentForExercise("Dumbbell Fly"))
        // The CSV files the goblet squat as a kettlebell move, but the app gates
        // it on dumbbells_only (a dumbbell held at the chest) and filed it under
        // Kettlebell a dumbbells-only kit was never offered it.
        assertEquals(dumbbell, WorkoutTemplates.getEquipmentForExercise("Goblet Squat"))
        assertEquals(kettlebell, WorkoutTemplates.getEquipmentForExercise("Kettlebell Swing"))
        assertEquals(bodyweight, WorkoutTemplates.getEquipmentForExercise("Pull-Up"))
        assertEquals(bodyweight, WorkoutTemplates.getEquipmentForExercise("Push-Up"))
        assertEquals(bodyweight, WorkoutTemplates.getEquipmentForExercise("Bench Dips"))
        assertEquals(cable, WorkoutTemplates.getEquipmentForExercise("Lat Pulldown"))
        assertEquals(cable, WorkoutTemplates.getEquipmentForExercise("Cable Fly"))
        assertEquals(machine, WorkoutTemplates.getEquipmentForExercise("Machine Chest Press"))
        assertEquals(machine, WorkoutTemplates.getEquipmentForExercise("Machine Assisted Dips"))
    }

    @Test
    fun aCategoryOnlyContainsItsOwnMovements() {
        val dumb = WorkoutTemplates.getAllExercisesForEquipment(dumbbell)
        assertTrue(dumb.contains("Dumbbell Hammer Curl"))
        assertFalse(dumb.contains("Barbell Back Squat"))
        assertFalse(dumb.contains("Lat Pulldown"))

        val free = WorkoutTemplates.getAllExercisesForEquipment(bodyweight)
        assertTrue(free.contains("Pull-Up"))
        assertFalse(free.contains("Dumbbell Bicep Curl"))
    }

    @Test
    fun unknownExercisesHaveNoEquipment() {
        assertEquals("", WorkoutTemplates.getEquipmentForExercise("My Custom Leg Machine"))
        assertEquals("", WorkoutTemplates.getEquipmentForExercise(""))
    }
}
