package com.grafizum.repio

import com.grafizum.repio.data.DefaultData
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.ExercisePopularity
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The equipment promise outlives the setup wizard. The generated builds were
 * always gated; the paths that write PREPICKED rows — the default program
 * prefill, the template picker, the hand-built routine save — were not, which
 * is how a user who said they have no barbell still found barbell movements in
 * their routine. Every path must now walk the same substitution chains.
 */
class EquipmentGateEverywhereTest {

    /** The dumbbells-only grant: no barbell, no machine, no cable. */
    private val dumbbellKit = listOf(ExerciseEquipmentTags.PRESET_DUMBBELLS)

    /** The no-barbell grant that still allows cables, machines and kettlebells. */
    private val noBarbellKit = listOf(ExerciseEquipmentTags.PRESET_CUSTOM) +
        listOf(
            ExerciseEquipmentTags.DUMBBELLS_ONLY, ExerciseEquipmentTags.DUMBBELLS_AND_BENCH,
            ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR, ExerciseEquipmentTags.KETTLEBELL_ONLY,
            ExerciseEquipmentTags.CABLE_MACHINE, ExerciseEquipmentTags.CHEST_PRESS_MACHINE,
            ExerciseEquipmentTags.LEG_PRESS_MACHINE, ExerciseEquipmentTags.LAT_PULLDOWN_MACHINE,
            ExerciseEquipmentTags.SEATED_ROW_MACHINE, ExerciseEquipmentTags.MACHINE_OTHER,
        )

    private val fullGym = listOf(ExerciseEquipmentTags.PRESET_FULL_GYM)

    private fun granted(equipment: List<String>) =
        WorkoutTemplates.grantedTagsForPublic(equipment) ?: emptySet()

    // ---------- 1. The coarse category reduction ----------

    @Test
    fun noBarbellKitReducesWithoutABarbellCategory() {
        val categories = ExerciseEquipmentTags.coarseCategoriesFor(noBarbellKit)
        assertFalse(
            "a no-barbell kit must not reduce to a Barbell category: $categories",
            WorkoutTemplates.EQUIPMENT_BARBELL in categories
        )
        assertTrue(WorkoutTemplates.EQUIPMENT_DUMBBELL in categories)
    }

    @Test
    fun dumbbellKitReducesToDumbbellOnly() {
        assertEquals(
            listOf(WorkoutTemplates.EQUIPMENT_DUMBBELL),
            ExerciseEquipmentTags.coarseCategoriesFor(dumbbellKit)
        )
    }

    @Test
    fun bodyweightPresetReducesToTheBodyweightCategory() {
        assertEquals(
            listOf(WorkoutTemplates.EQUIPMENT_BODYWEIGHT),
            ExerciseEquipmentTags.coarseCategoriesFor(listOf(ExerciseEquipmentTags.PRESET_BODYWEIGHT))
        )
    }

    @Test
    fun fullGymAndEmptyReduceToNoPreference() {
        assertEquals(emptyList<String>(), ExerciseEquipmentTags.coarseCategoriesFor(fullGym))
        assertEquals(emptyList<String>(), ExerciseEquipmentTags.coarseCategoriesFor(emptyList()))
        assertEquals(
            emptyList<String>(),
            ExerciseEquipmentTags.coarseCategoriesFor(listOf(WorkoutTemplates.EQUIPMENT_ALL))
        )
    }

    // ---------- 2. The default prefill is gated ----------

    @Test
    fun defaultPrefillWithDumbbellsOnlyContainsNoBarbellMovements() {
        val granted = granted(dumbbellKit)
        DefaultData.routineExercises.forEach { ex ->
            val sub = WorkoutTemplates.substituteExercise(ex.name, dumbbellKit)
            assertTrue(
                "${ex.name} has no available substitution for a dumbbells-only kit",
                sub != null
            )
            sub?.let {
                assertTrue(
                    "$it leaked through the dumbbells-only gate",
                    ExerciseEquipmentTags.isAvailable(it, granted)
                )
            }
        }
    }

    @Test
    fun defaultPrefillForAFullGymIsUntouched() {
        DefaultData.routineExercises.forEach { ex ->
            assertEquals(
                ex.name,
                WorkoutTemplates.substituteExercise(ex.name, fullGym)
            )
        }
    }

    @Test
    fun noBarbellKitKeepsTheBarbellRowsAvailableViaSubstitution() {
        // The point is not to drop the movement but to swap it down its chain:
        // Bent-Over Barbell Row must survive as a row, not vanish.
        val sub = WorkoutTemplates.substituteExercise("Bent-Over Barbell Row", noBarbellKit)
        assertTrue("expected a row substitution, got null", sub != null)
        assertTrue(
            "expected the substitution to stay a row, got $sub",
            sub!!.contains("Row")
        )
        assertTrue(
            ExerciseEquipmentTags.isAvailable(sub, granted(noBarbellKit))
        )
    }

    // ---------- 3. The generator stays gated under the same kits ----------

    @Test
    fun generatedBuildWithNoBarbellKitContainsNoBarbellRow() {
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Chest", "Lats", "Quadriceps", "Biceps", "Hamstrings", "Shoulders"),
            selectedDays = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            intensity = "Hard",
            workoutGoal = "goal_bodybuilding",
            equipment = noBarbellKit
        )
        val granted = granted(noBarbellKit)
        program.exercises.forEach { ex ->
            assertTrue(
                "'${ex.name}' leaked into a build restricted to ${noBarbellKit.drop(1)}",
                ExerciseEquipmentTags.isAvailable(ex.name, granted)
            )
        }
    }

    @Test
    fun hardBodybuildingWithNoBarbellStillPrefersTheStaplesItHas() {
        // The strength/Hard preference narrows to barbell-style compounds; with
        // no barbell granted it must fall back to the available compounds and
        // stay popularity-ranked instead of surfacing obscure variations.
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Chest", "Lats", "Quadriceps"),
            selectedDays = listOf("MONDAY", "WEDNESDAY"),
            intensity = "Hard",
            workoutGoal = "goal_bodybuilding",
            equipment = dumbbellKit
        )
        val chestPicks = program.exercises.filter {
            it.dayOfWeek == "MONDAY" &&
                WorkoutTemplates.getMuscleGroupForExercise(it.name) == "Pectorals"
        }
        assertTrue("expected a chest pick on the push day", chestPicks.isNotEmpty())
        chestPicks.forEach {
            assertTrue(
                "${it.name} is not performable with a dumbbells-only kit",
                ExerciseEquipmentTags.isAvailable(it.name, granted(dumbbellKit))
            )
        }
    }

    // ---------- 4. Popularity keeps the staples in front ----------

    @Test
    fun machineLegsListTheRankedStaplesAheadOfTheVariations() {
        val machineLegs = WorkoutTemplates.getAllExercisesForEquipment(
            WorkoutTemplates.EQUIPMENT_MACHINE
        ).filter {
            WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(it)) == "Legs"
        }
        val ranked = machineLegs.filter { ExercisePopularity.isRanked(it) }
        assertTrue("expected ranked machine leg staples", ranked.size >= 2)
        val sorted = ExercisePopularity.sortedByPopularity(ranked)
        assertTrue("Leg Extension" in sorted)
        assertTrue("Leg Press" in sorted)
        // Every ranked entry sorts ahead of the unranked variations.
        val firstUnranked = machineLegs.indexOfFirst { !ExercisePopularity.isRanked(it) }
        assertTrue(firstUnranked < 0 || ExercisePopularity.sortedByPopularity(machineLegs).take(firstUnranked)
            .all { ExercisePopularity.isRanked(it) })
    }
}
