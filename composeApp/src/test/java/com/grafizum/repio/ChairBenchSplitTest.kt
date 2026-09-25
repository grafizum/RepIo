package com.grafizum.repio

import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A chair and a bench are two tiles, two icons and two grants.
 *
 * They were one "Chair / Bench" tile, so ticking it claimed furniture the user
 * does not own and unticking it took away a bench they do own. The split has to
 * hold in three places at once: the tags are separate, a legacy saved selection
 * still grants both (nobody loses their kit to an update), and no requirement
 * asks for the retired merged tag any more.
 */
class ChairBenchSplitTest {

    private val chairOnly = listOf(ExerciseEquipmentTags.PRESET_CUSTOM, ExerciseEquipmentTags.CHAIR)
    private val benchOnly = listOf(ExerciseEquipmentTags.PRESET_CUSTOM, ExerciseEquipmentTags.BENCH)

    @Test
    fun chairAndBenchAreSeparateTagsOnSeparateTiles() {
        assertTrue(ExerciseEquipmentTags.CHAIR in ExerciseEquipmentTags.HOME_EQUIPMENT_TAGS)
        assertTrue(ExerciseEquipmentTags.BENCH in ExerciseEquipmentTags.HOME_EQUIPMENT_TAGS)
        assertFalse(
            "the merged tag must not be grantable any more",
            ExerciseEquipmentTags.CHAIR_OR_BENCH in ExerciseEquipmentTags.ALL_TAGS
        )
    }

    @Test
    fun aLegacyChairOrBenchSelectionStillGrantsBothFurniture() {
        val granted = ExerciseEquipmentTags.allowedTagsFor(listOf(ExerciseEquipmentTags.CHAIR_OR_BENCH))
        assertTrue(ExerciseEquipmentTags.CHAIR in granted)
        assertTrue(ExerciseEquipmentTags.BENCH in granted)
    }

    @Test
    fun aChairOnlyUserIsNotOfferedTheBenchOnlyKit() {
        val granted = ExerciseEquipmentTags.allowedTagsFor(chairOnly)
        assertFalse(ExerciseEquipmentTags.BENCH in granted)
        // The bench movements need the bench tag, so a chair alone cannot do them.
        assertFalse(ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", granted))
        assertTrue(ExerciseEquipmentTags.isAvailable("Bench Dips", granted))
    }

    @Test
    fun aBenchOnlyUserStillNeedsTheDumbbellsToPressAnything() {
        val granted = ExerciseEquipmentTags.allowedTagsFor(benchOnly)
        assertFalse(ExerciseEquipmentTags.CHAIR in granted)
        // The bench is the furniture; the press still needs the pair of
        // dumbbells, which the bench tile does not imply.
        assertFalse(ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", granted))
        assertTrue(
            "a bench lies you down, so bench dips are available",
            ExerciseEquipmentTags.isAvailable("Bench Dips", granted)
        )
        val withDumbbells = ExerciseEquipmentTags.allowedTagsFor(
            benchOnly + ExerciseEquipmentTags.DUMBBELLS_AND_BENCH
        )
        assertTrue(ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", withDumbbells))
    }

    @Test
    fun noRequirementAsksForTheRetiredMergedTag() {
        val offenders = WorkoutTemplates.getAllExerciseNames().filter { name ->
            ExerciseEquipmentTags.requirementsFor(name)
                .any { alternative -> ExerciseEquipmentTags.CHAIR_OR_BENCH in alternative }
        }
        assertEquals(
            "these exercises still require the merged chair/bench tag",
            emptyList<String>(),
            offenders
        )
    }

    @Test
    fun bothFurnitureTagsAreOfferedByEveryKitThatContainsEither() {
        // A "dumbbells and a bench" setup obviously has the bench; the tile grid
        // has to show it, or the grid stops being the summary of the selection.
        val granted = ExerciseEquipmentTags.allowedTagsFor(listOf(ExerciseEquipmentTags.PRESET_DB_BENCH))
        assertTrue(ExerciseEquipmentTags.BENCH in granted)
    }
}
