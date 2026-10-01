package com.grafizum.repio

import com.grafizum.repio.data.DefaultData
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.equipmentTagShortLabelKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three equipment promises the owner raised on 2026-09-28, in one place.
 *
 * 1. A custom (hand-built) week is filtered by the tiles and the selection is
 *    persisted. The custom tab used to save without `equipment`, which wrote
 *    `setup_equipment = ""` — so from then on EVERY consumer read "no
 *    restriction" and the week ignored the icons for the rest of the install.
 * 2. Every tile draws something the user can name. The Bench tile drew a bench
 *    and was captioned with the generic "equipment" fallback, and a redundant
 *    "Dumbbell + Bench" tile sat next to it.
 * 3. A chair stands in for a bench on the movements that only need somewhere to
 *    sit, so "chair + dumbbells" is not a dead end.
 */
class CustomBuildEquipmentGateTest {

    private fun kit(vararg tags: String) =
        listOf(ExerciseEquipmentTags.PRESET_CUSTOM) + tags

    /**
     * What the custom tab saves is `DefaultData.routineExercises` walked
     * through the same gate the assisted path uses, so it is reproduced here
     * rather than asserted on the private ViewModel helper.
     */
    private fun gateCustomPrefill(equipment: List<String>): List<String> {
        val granted = ExerciseEquipmentTags.allowedTagsFor(equipment)
        return DefaultData.routineExercises.mapNotNull { ex ->
            if (ExerciseEquipmentTags.isAvailable(ex.name, granted)) ex.name
            else WorkoutTemplates.substituteExercise(ex.name, equipment)
        }
    }

    @Test
    fun theCustomWeekIsGatedForEveryKit() {
        val kits = mapOf(
            "bodyweight only" to listOf(ExerciseEquipmentTags.PRESET_BODYWEIGHT),
            "chair + dumbbells" to kit(ExerciseEquipmentTags.CHAIR, ExerciseEquipmentTags.DUMBBELLS_ONLY),
            "dumbbells + bench" to kit(ExerciseEquipmentTags.BENCH, ExerciseEquipmentTags.DUMBBELLS_ONLY),
            "chair + barbell" to kit(ExerciseEquipmentTags.CHAIR, ExerciseEquipmentTags.BARBELL_AND_RACK),
            "bands only" to listOf(ExerciseEquipmentTags.PRESET_BW_BANDS),
        )
        for ((label, equipment) in kits) {
            val granted = ExerciseEquipmentTags.allowedTagsFor(equipment)
            val kept = gateCustomPrefill(equipment)
            assertTrue("$label: the week must not be emptied by the gate", kept.isNotEmpty())
            val leaked = kept.filterNot { ExerciseEquipmentTags.isAvailable(it, granted) }
            assertEquals("$label: these rows need kit the user never ticked", emptyList<String>(), leaked)
        }
    }

    @Test
    fun aCustomBuildKeepsTheChairKitUseful() {
        // The point of the chair tile: with a chair and dumbbells the user still
        // trains the pressing and rowing patterns, seated on the chair.
        val granted = ExerciseEquipmentTags.allowedTagsFor(
            kit(ExerciseEquipmentTags.CHAIR, ExerciseEquipmentTags.DUMBBELLS_ONLY)
        )
        val kept = gateCustomPrefill(kit(ExerciseEquipmentTags.CHAIR, ExerciseEquipmentTags.DUMBBELLS_ONLY))
        assertTrue(
            "a chair must satisfy the seated dumbbell work, got $kept",
            ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", granted)
        )
        assertTrue(kept.isNotEmpty())
    }

    @Test
    fun everyTileHasItsOwnName() {
        // The regression: BENCH had no branch in equipmentTagShortLabelKey and
        // fell through to the generic "equipment" string, so a tile drawing a
        // bench was labelled with a word that names nothing.
        val tiles = ExerciseEquipmentTags.HOME_EQUIPMENT_TAGS +
            ExerciseEquipmentTags.GYM_EQUIPMENT_TAGS
        val generic = tiles.filter { equipmentTagShortLabelKey(it) == "equipment" }
        assertEquals("these tiles render the generic equipment caption", emptyList<String>(), generic)

        val labels = tiles.map { equipmentTagShortLabelKey(it) }
        assertEquals(
            "two tiles share one caption, so one of them is unreadable",
            labels.size,
            labels.toSet().size
        )
        assertNotEquals(
            equipmentTagShortLabelKey(ExerciseEquipmentTags.CHAIR),
            equipmentTagShortLabelKey(ExerciseEquipmentTags.BENCH)
        )
    }

    @Test
    fun everyStoredTagCanBeNamedBackToTheUser() {
        // The eight selectorized stations are not selectable tiles, so no test
        // covered them, and equipmentTagShortLabelKey answered "equipment" for
        // all eight. Settings ▸ Your equipment reads a STORED kit, renders one
        // chip per label and de-duplicates - so the whole seven-machine part of
        // a full-gym kit collapsed into a single chip reading "Equipment". The
        // card reported a kit the user did not have, which is how a
        // plate-loaded lateral raise stayed in a week the owner believed was
        // bodyweight: the app knew it granted the machine, the card hid it.
        val unnamed = ExerciseEquipmentTags.ALL_TAGS.filter {
            equipmentTagShortLabelKey(it) == "equipment"
        }
        assertEquals(
            "these stored tags have no readable name, so the equipment card cannot show them",
            emptyList<String>(),
            unnamed
        )
    }

    @Test
    fun theRedundantDumbbellPlusBenchTileIsGone() {
        assertFalse(
            "\"Dumbbell + Bench\" duplicated the Dumbbells and Bench tiles",
            ExerciseEquipmentTags.DUMBBELLS_AND_BENCH in ExerciseEquipmentTags.GYM_EQUIPMENT_TAGS
        )
        // A selection persisted under the retired tile still means what it said.
        val granted = ExerciseEquipmentTags.allowedTagsFor(
            listOf(ExerciseEquipmentTags.PRESET_CUSTOM, ExerciseEquipmentTags.DUMBBELLS_AND_BENCH)
        )
        assertTrue(ExerciseEquipmentTags.DUMBBELLS_ONLY in granted)
        assertTrue(ExerciseEquipmentTags.BENCH in granted)
        assertTrue(
            ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", granted)
        )
    }

    @Test
    fun noRequirementAsksForTheRetiredTileTag() {
        val offenders = WorkoutTemplates.getAllExerciseNames().filter { name ->
            ExerciseEquipmentTags.requirementsFor(name)
                .any { alternative -> ExerciseEquipmentTags.DUMBBELLS_AND_BENCH in alternative }
        }
        assertEquals(
            "these exercises still require the retired dumbbell+bench tile",
            emptyList<String>(),
            offenders
        )
    }
}
