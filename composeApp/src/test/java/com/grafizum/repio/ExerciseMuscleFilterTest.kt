package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the muscle filter in the replace dialog, which offers the six broad body
 * parts rather than the nineteen precise muscles.
 *
 * A broad part is defined as "every exercise whose authoritative primary muscle
 * belongs to it". That makes the six parts a partition of the pool: no exercise
 * can be missing from its own body part, none can leak into another, and adding
 * a precise muscle later cannot silently drop movements out of the filter.
 */
class ExerciseMuscleFilterTest {

    private val all = WorkoutTemplates.EQUIPMENT_ALL

    @Test
    fun theFilterOffersOnlyBroadBodyParts() {
        assertEquals(
            listOf("Arms", "Back", "Chest", "Core", "Legs", "Shoulders"),
            WorkoutTemplates.getBroadMuscleGroups()
        )
        // Every offered part must be its own topic, or the dropdown would show a
        // label that the rest of the app groups somewhere else.
        WorkoutTemplates.getBroadMuscleGroups().forEach { part ->
            assertEquals(part, WorkoutTemplates.getTopicForMuscle(part))
        }
    }

    @Test
    fun everyBodyPartReturnsExercises() {
        WorkoutTemplates.getBroadMuscleGroups().forEach { part ->
            val pool = WorkoutTemplates.getExercisesForEquipment(all, part)
            assertTrue("'$part' has no exercises at all", pool.isNotEmpty())
        }
    }

    @Test
    fun aBodyPartIsExactlyTheExercisesWhosePrimaryMuscleIsInIt() {
        WorkoutTemplates.getBroadMuscleGroups().forEach { part ->
            val expected = WorkoutTemplates.getAllExerciseNames()
                .filter { WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(it)) == part }
                .sorted()
            assertEquals(
                "Filtering by '$part' does not match its own primary muscles",
                expected,
                WorkoutTemplates.getExercisesForEquipment(all, part)
            )
        }
    }

    @Test
    fun theBodyPartsPartitionThePoolWithoutOverlap() {
        val seen = mutableMapOf<String, String>()
        WorkoutTemplates.getBroadMuscleGroups().forEach { part ->
            WorkoutTemplates.getExercisesForEquipment(all, part).forEach { name ->
                val previous = seen.put(name, part)
                assertEquals("'$name' is offered by both '$previous' and '$part'", null, previous)
            }
        }
        assertEquals(
            "The six body parts together must cover the whole pool",
            WorkoutTemplates.getAllExerciseNames().sorted(),
            seen.keys.sorted()
        )
    }

    @Test
    fun aBodyPartOffersEveryMovementItClassifies() {
        // The badge beside an exercise comes from its own body part, so the filter
        // has to agree with it: nothing the app labels "Legs" may be unreachable by
        // filtering "Legs". Checked per equipment category, so the muscle filter
        // and the equipment filter stay independent.
        WorkoutTemplates.allEquipmentCategories.forEach { category ->
            val categoryPool = WorkoutTemplates.getAllExercisesForEquipment(category)
            WorkoutTemplates.getBroadMuscleGroups().forEach { part ->
                val expected = categoryPool
                    .filter {
                        WorkoutTemplates.getTopicForMuscle(
                            WorkoutTemplates.getMuscleGroupForExercise(it)
                        ) == part
                    }
                    .sorted()
                assertEquals(
                    "Filtering '$part' under '$category' disagrees with the body part the app labels those exercises with",
                    expected,
                    WorkoutTemplates.getExercisesForEquipment(category, part)
                )
            }
        }
    }

    @Test
    fun aPreciseGroupStillAnswersWithItsOwnPool() {
        // The precise groups are still addressable (templates and the primary-muscle
        // tests rely on them) and keep their long-standing pool semantics.
        assertTrue(
            "Lats should still offer Reverse Snow Angels through the raw pool",
            WorkoutTemplates.getExercisesForEquipment(all, "Lats").contains("Reverse Snow Angels")
        )
        assertTrue(
            WorkoutTemplates.getExercisesForEquipment(all, "Triceps").contains("Dumbbell Floor Presses")
        )
    }
}
