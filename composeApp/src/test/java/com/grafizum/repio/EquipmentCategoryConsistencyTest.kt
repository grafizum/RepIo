package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The catalog's `equipment` column and the fine-tag requirements must agree.
 *
 * The pickers filter by the category ("Kettlebell" chip = the Kettlebell rows)
 * and the generator gates by the fine tags, so a row whose category names gear
 * its own requirements do not ask for is the one defect both layers cannot see:
 * the chip puts it under the wrong drawer, and the gate lets it through. That is
 * how a bodyweight + dumbbell kit was offered a "Kettlebell" goblet squat and a
 * "Barbell" inverted row — the owner read it as the filter leaking machines and
 * kettlebells.
 *
 * The rule: an exercise that requires nothing (pure bodyweight, yoga, cardio) may
 * sit in any category, but an exercise that DOES require gear must be filed
 * under the family that gear belongs to.
 */
class EquipmentCategoryConsistencyTest {

    /** The fine tags each catalog category is allowed to require. */
    private val tagsByCategory: Map<String, Set<String>> = mapOf(
        "Bodyweight" to setOf(
            ExerciseEquipmentTags.NONE_REQUIRED, ExerciseEquipmentTags.CHAIR,
            ExerciseEquipmentTags.BENCH,
            ExerciseEquipmentTags.WALL, ExerciseEquipmentTags.STAIRS,
            ExerciseEquipmentTags.BACKPACK_OR_WEIGHTED_BAG, ExerciseEquipmentTags.TOWEL,
            ExerciseEquipmentTags.PULL_UP_BAR, ExerciseEquipmentTags.DIP_BARS,
            ExerciseEquipmentTags.GYMNASTIC_RINGS, ExerciseEquipmentTags.RESISTANCE_BANDS,
        ),
        "Dumbbell" to setOf(
            ExerciseEquipmentTags.DUMBBELLS_ONLY, ExerciseEquipmentTags.DUMBBELLS_AND_BENCH,
            ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR,
            ExerciseEquipmentTags.CHAIR, ExerciseEquipmentTags.BENCH,
        ),
        "Barbell" to setOf(
            ExerciseEquipmentTags.BARBELL_ONLY, ExerciseEquipmentTags.BARBELL_AND_FLOOR,
            ExerciseEquipmentTags.BARBELL_AND_RACK,
            ExerciseEquipmentTags.CHAIR, ExerciseEquipmentTags.BENCH,
        ),
        "Kettlebell" to setOf(
            ExerciseEquipmentTags.KETTLEBELL_ONLY, ExerciseEquipmentTags.KETTLEBELL_AND_BENCH,
            ExerciseEquipmentTags.CHAIR, ExerciseEquipmentTags.BENCH,
        ),
        "Bands" to setOf(ExerciseEquipmentTags.RESISTANCE_BANDS),
        "Cable" to setOf(ExerciseEquipmentTags.CABLE_MACHINE),
        "Machine" to (ExerciseEquipmentTags.ALL_TAGS - setOf(
            ExerciseEquipmentTags.DUMBBELLS_ONLY, ExerciseEquipmentTags.DUMBBELLS_AND_BENCH,
            ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR, ExerciseEquipmentTags.BARBELL_ONLY,
            ExerciseEquipmentTags.BARBELL_AND_FLOOR, ExerciseEquipmentTags.BARBELL_AND_RACK,
            ExerciseEquipmentTags.KETTLEBELL_ONLY, ExerciseEquipmentTags.KETTLEBELL_AND_BENCH,
            ExerciseEquipmentTags.RESISTANCE_BANDS,
        )).toSet(),
        "Other" to ExerciseEquipmentTags.ALL_TAGS.toSet(),
    )

    @Test
    fun noExerciseIsFiledUnderGearItsOwnRequirementsDoNotAskFor() {
        val offenders = ExerciseCatalog.ENTRIES.mapNotNull { entry ->
            val allowed = tagsByCategory[entry.equipment] ?: return@mapNotNull null
            // A requirement SET is an OR of ANDs: the row passes when at least
            // one alternative stays inside its own family.
            val asks = ExerciseEquipmentTags.requirementsFor(entry.name)
                .map { set -> set.toSet() }
                .filter { it.isNotEmpty() }
            if (asks.isEmpty()) return@mapNotNull null
            if (asks.any { alternative -> alternative.all { it in allowed } }) return@mapNotNull null
            "${entry.name} (${entry.equipment}) needs $asks"
        }
        assertEquals(
            "these exercises sit under an equipment category their own requirements " +
                "contradict, so the picker files them with gear the user does not have: " +
                offenders,
            emptyList<String>(),
            offenders
        )
    }
}
