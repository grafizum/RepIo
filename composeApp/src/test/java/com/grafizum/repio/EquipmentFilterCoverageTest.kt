package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.defaultEquipmentFilter
import com.grafizum.repio.ui.components.exercisePickerPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WHICH MOVEMENTS A FILTER SETTING CAN NEVER SHOW, counted on the real catalogue.
 *
 * Owner, 2026-10-11: *"make sure all equipments that are from the welcome screen apply and the
 * (other) is not shown. And tell me if there is an exercise that wouldnt show at all if all
 * equipments are selected (simply say yes and no if there are any exercises and if yes how
 * many)"*.
 *
 * That question has one answer per vocabulary, and the three of them are NOT the same setting:
 *
 *   - the eight equipment CHIPS (the picker's filter row) cover the whole catalogue: 0 of the
 *     601 movements is unreachable with every chip on;
 *   - the welcome screen's sixteen TILES cover it as well, once each tile grants the family it
 *     stands for - 0 unreachable;
 *   - dropping the kit-free `Other` CATEGORY is what hides movements, and it hides 144 of them
 *     (601 -> 457). It is not equipment the user can lack, so the picker must keep filtering by
 *     it while drawing no chip for it.
 *
 * These are the assertions behind those numbers. They are written against the catalogue rather
 * than against a copy of it, so a movement added later cannot quietly become invisible: it will
 * either be reachable, or it will fail here.
 */
class EquipmentFilterCoverageTest {

    private val catalogue: List<String> = WorkoutTemplates.getAllExerciseNames()
    private val categories: List<String> = WorkoutTemplates.allEquipmentCategories
    private val other = WorkoutTemplates.EQUIPMENT_OTHER

    private fun poolFor(selected: List<String>, granted: Set<String>? = null): List<String> =
        exercisePickerPool(
            equipmentCategories = emptyList(),
            grantedTags = granted,
            selectedEquipment = selected,
            selectedMuscleGroup = null
        )

    @Test
    fun everyChipOnReachesTheWholeCatalogue() {
        val pool = poolFor(categories).toSet()

        assertEquals(
            "with every equipment chip selected the picker must offer the whole catalogue",
            catalogue.size,
            pool.size
        )
        assertTrue("the pool is not the catalogue: missing ${catalogue.toSet() - pool}", catalogue.toSet() == pool)
    }

    /**
     * Nothing is filed under a category the chips cannot select. This is the assumption the
     * assertion above rests on, and it is the one that broke before: a movement whose category is
     * outside [WorkoutTemplates.allEquipmentCategories] is unreachable no matter what the user
     * ticks, and no chip on screen can explain why.
     */
    @Test
    fun everyCatalogueMovementSitsInACategorySomeChipOffers() {
        val orphans = ExerciseCatalog.ENTRIES.filter { it.equipment !in categories }

        assertTrue(
            "these movements are filed under a category no chip offers, so no filter setting can " +
                "ever show them: ${orphans.map { "${it.name}/${it.equipment}" }}",
            orphans.isEmpty()
        )
    }

    /**
     * The fifteen tiles of the welcome screen, ticked the way the grid grants them, also reach
     * everything. Measured through [ExerciseEquipmentTags.tagsGrantedByTicking], because that is
     * what a tap on a tile writes - reading the tile's own tag alone under-counts the families
     * (the eight stations behind "Machines", `barbell_only` behind the Barbell tile) and reports
     * 59 unreachable movements that the real grid does grant.
     */
    @Test
    fun everyWelcomeScreenTileTickedReachesTheWholeCatalogue() {
        val granted = ExerciseEquipmentTags.SELECTABLE_TAGS
            .flatMap { ExerciseEquipmentTags.tagsGrantedByTicking(it) }
            .toSet()
        val blocked = catalogue.filterNot { ExerciseEquipmentTags.isAvailable(it, granted) }

        assertTrue(
            "ticking every tile on the welcome screen still hides ${blocked.size} movements: " +
                "$blocked",
            blocked.isEmpty()
        )
    }

    /**
     * WHY `Other` STAYS IN THE FILTER. This is the regression that produced the rule: the category
     * is not equipment, it is "needs no kit at all" - a plank, a walk, every stretch and pose -
     * and treating it as a chip the user did not tick hid 144 of the 601 movements from everyone
     * who had ever answered the equipment question.
     */
    @Test
    fun droppingTheKitFreeCategoryIsWhatHidesMovements() {
        val withoutOther = poolFor(categories.filter { it != other }).toSet()
        val hidden = catalogue.toSet() - withoutOther

        assertEquals(
            "the kit-free category is documented as protecting 144 movements; the catalogue now " +
                "counts ${hidden.size} behind it",
            144,
            hidden.size
        )
    }

    /**
     * The kit's own default filter carries `Other` along, and the row draws no chip for it. Both
     * halves matter: the first is what makes a plank reachable, the second is what the owner
     * asked for - the chip was not equipment he had chosen, and it appeared in the row of icons
     * that are.
     */
    @Test
    fun theDefaultFilterKeepsOtherWhileTheChipRowHidesIt() {
        val kit = listOf(WorkoutTemplates.EQUIPMENT_DUMBBELL, WorkoutTemplates.EQUIPMENT_BODYWEIGHT)

        val filter = defaultEquipmentFilter(kit)

        assertTrue("the kit's own categories must filter the list", filter.containsAll(kit))
        assertTrue(
            "the kit-free category must ride along or a plank cannot be added",
            filter.contains(other)
        )
        assertFalse(
            "the chip row must not offer a chip for the kit-free category",
            WorkoutTemplates.filterableEquipmentCategories.contains(other)
        )
        assertEquals(
            "the chip row is every category except the kit-free one",
            categories.filter { it != other },
            WorkoutTemplates.filterableEquipmentCategories
        )
        // And the pool that filter derives still contains those movements.
        assertTrue(
            "a plank must be reachable from the default filter",
            poolFor(filter).any { it.equals("Plank", ignoreCase = true) }
        )
    }

    /**
     * The welcome-screen rule, as a function: the builder's live selection while it has one, else
     * the kit the welcome screen saved. Both inputs are RAW stored lists, because the derived
     * category list is empty BOTH for "nothing chosen" and for "a full gym" - deciding on it is
     * what let a full-gym builder be narrowed back to the saved kit.
     */
    @Test
    fun theWelcomeScreenKitAppliesWhenTheBuilderHasNoSelectionYet() {
        val saved = listOf(ExerciseEquipmentTags.PRESET_DUMBBELLS, ExerciseEquipmentTags.CHAIR)

        assertEquals(
            "an untouched equipment step must open the picker on the kit the welcome screen saved",
            saved,
            WorkoutTemplates.pickerEquipmentSelection(emptyList(), saved)
        )
        assertEquals(
            "the builder's own live selection wins while it is being edited",
            listOf(ExerciseEquipmentTags.BENCH),
            WorkoutTemplates.pickerEquipmentSelection(listOf(ExerciseEquipmentTags.BENCH), saved)
        )
        assertEquals(
            "a full-gym builder selection is not the same as no selection",
            listOf(ExerciseEquipmentTags.PRESET_FULL_GYM),
            WorkoutTemplates.pickerEquipmentSelection(listOf(ExerciseEquipmentTags.PRESET_FULL_GYM), saved)
        )
        assertEquals(
            "nothing chosen anywhere stays unrestricted",
            emptyList<String>(),
            WorkoutTemplates.pickerEquipmentSelection(emptyList(), emptyList())
        )

        // The bug the old fallback had: a full-gym selection reduces to an EMPTY category list, so
        // "nothing derived" was read as "nothing chosen" and the saved kit narrowed the chips.
        assertEquals(
            "a full-gym selection is unrestricted, not a fallback to the saved kit",
            emptyList<String>(),
            ExerciseEquipmentTags.coarseCategoriesFor(listOf(ExerciseEquipmentTags.PRESET_FULL_GYM))
        )
        assertFalse(
            "the saved kit's categories must not replace a full-gym selection",
            ExerciseEquipmentTags.coarseCategoriesFor(saved).isEmpty()
        )
    }
}
