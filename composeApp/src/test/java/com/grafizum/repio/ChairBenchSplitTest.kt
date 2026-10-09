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
        // The pressing work is the two tiles the user actually sees: the Bench
        // tile and the Dumbbells tile. The retired "Dumbbell + Bench" tile no
        // longer exists, so the grant comes from the pair.
        val withDumbbells = ExerciseEquipmentTags.allowedTagsFor(
            benchOnly + ExerciseEquipmentTags.DUMBBELLS_ONLY
        )
        assertTrue(ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", withDumbbells))
    }

    @Test
    fun aChairIsNotABenchButTheChairTileStillOpensChairWork() {
        // REVERSED 2026-10-08. This used to assert that a chair stands in for a
        // bench ("a chair really is a seat"), and the owner reported the leak that
        // assumption caused: "I only have chair and dumbell and yet the system asks
        // me to do an inclinde dumbell press which doesnt work without a bench". A
        // chair has no adjustable back angle and no pad to brace against, so the
        // bench-angle work now requires a BENCH.
        val chairAndDumbbells = ExerciseEquipmentTags.allowedTagsFor(
            listOf(ExerciseEquipmentTags.PRESET_CUSTOM, ExerciseEquipmentTags.CHAIR, ExerciseEquipmentTags.DUMBBELLS_ONLY)
        )
        assertFalse(ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", chairAndDumbbells))
        assertFalse(ExerciseEquipmentTags.isAvailable("Incline Dumbbell Press", chairAndDumbbells))
        // The tile is still worth ticking: a chair genuinely supports the work that
        // only needs somewhere to put a foot or a hand.
        assertTrue(ExerciseEquipmentTags.isAvailable("Bulgarian Split Squat", chairAndDumbbells))
        // ...and the same movements are NOT handed to a user with no weights.
        val chairOnly2 = ExerciseEquipmentTags.allowedTagsFor(chairOnly)
        assertFalse(ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", chairOnly2))
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
