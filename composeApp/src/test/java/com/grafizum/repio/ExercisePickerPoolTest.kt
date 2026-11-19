package com.grafizum.repio

import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.ExercisePopularity
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.defaultEquipmentFilter
import com.grafizum.repio.ui.components.exercisePickerPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The pool the level system's ADD-exercise picker offers, before any drawing.
 *
 * Extracted from the composable precisely so these rules can be checked at all: the
 * owner's request (2026-10-11) was *"exercise name that the user can input to search an
 * exercise and under it filter options ... but if the user only uses the filters and
 * doesnt search by name show al the possible exercises based on those filters"*, and
 * every failure mode of that sentence is a POOL question — the list is empty, or it is
 * everything regardless of the chips, or it offers a movement the user's kit cannot do.
 * A rule that lives inside a composable can only be checked by looking at the screen.
 *
 * The kit used throughout is the one the owner's own report was about: dumbbells and a
 * chair. It is the interesting kit because it is the one the coarse vocabulary cannot
 * describe — "Dumbbell" has no word for a chair, and "Bodyweight" reads as if every
 * bar-less movement were available — so a pool built from the chips alone offers
 * pull-ups and bench presses to someone who owns neither.
 */
class ExercisePickerPoolTest {

    /** Dumbbells, a floor to press from, and a chair. Deliberately NO bench and no bar. */
    private val kitTags: Set<String> = ExerciseEquipmentTags.allowedTagsFor(
        listOf(
            ExerciseEquipmentTags.DUMBBELLS_ONLY,
            ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR,
            ExerciseEquipmentTags.CHAIR,
        )
    )

    /** The same kit in the picker's chip vocabulary — what setup stored for this user. */
    private val kitCategories = listOf(
        WorkoutTemplates.EQUIPMENT_DUMBBELL,
        WorkoutTemplates.EQUIPMENT_BODYWEIGHT,
    )

    private fun pool(
        selectedEquipment: List<String> = defaultEquipmentFilter(kitCategories),
        selectedMuscleGroup: String? = null,
        excludedName: String? = null,
        grantedTags: Set<String>? = kitTags,
        categories: List<String> = kitCategories,
    ): List<String> = exercisePickerPool(
        equipmentCategories = categories,
        grantedTags = grantedTags,
        selectedEquipment = selectedEquipment,
        selectedMuscleGroup = selectedMuscleGroup,
        excludedName = excludedName,
    )

    // ------------------------------------------------------------- the kit is the ceiling

    /**
     * The list is useful the moment the card opens, with no query and no chip touched —
     * that is the "if the user only uses the filters ... show all the possible exercises"
     * half — and every row in it is a movement this kit can actually perform.
     */
    @Test
    fun theKitsOwnExercisesAreListedBeforeAnythingIsTouched() {
        val results = pool()

        assertTrue("the picker opened empty for a dumbbell + chair kit", results.isNotEmpty())
        assertTrue("expected plenty of movements for a dumbbell kit, got ${results.size}", results.size > 30)
        val shown = defaultEquipmentFilter(kitCategories)
        results.forEach { name ->
            assertTrue(
                "'$name' is offered to a dumbbells-and-chair kit that cannot perform it",
                ExerciseEquipmentTags.isAvailable(name, kitTags)
            )
            assertTrue(
                "'$name' is listed under a chip that is not showing",
                WorkoutTemplates.getEquipmentForExercise(name) in shown
            )
        }
    }

    /**
     * "Other" IS NOT KIT THE USER CAN LACK, and this is the regression that proved it: the
     * catalogue files a plank, a hollow hold, every stretch and pose - and a walk - under
     * `Other`, so opening the picker on the user's kit chips alone hid 115 of its movements and
     * no one who had ever answered the equipment question could add a Plank.
     */
    @Test
    fun kitlessMovementsRideAlongWithWhateverKitTheUserDeclared() {
        val shown = defaultEquipmentFilter(kitCategories)
        assertTrue("the picker does not show the no-equipment category", shown.contains(WorkoutTemplates.EQUIPMENT_OTHER))

        val results = pool()
        assertTrue("Plank is not offered to a dumbbell owner", results.contains("Plank"))
        assertTrue("a stretch is not offered to a dumbbell owner", results.contains("Butterfly Stretch"))

        // ...and it is still a CHIP: unticking Other drops them, so the row is honest about
        // what is filtering the list rather than quietly force-including a category.
        val withoutOther = pool(selectedEquipment = kitCategories)
        assertFalse("Other still showed after the chip was unticked", withoutOther.contains("Plank"))
    }

    /**
     * The three concrete rows the coarse chips get wrong, each for a different reason:
     * a pull-up needs a bar the user does not have, an inverted row needs one at height,
     * and a dumbbell bench press needs a flat bench rather than a kitchen chair.
     *
     * These are the project's own past reports, kept as guards: the inverted row in
     * particular was filed by the owner against exactly this kit.
     */
    @Test
    fun furnitureAndBarsTheUserDoesNotOwnAreNotOffered() {
        val results = pool()

        assertFalse("Pull-Up offered without a bar", results.contains("Pull-Up"))
        assertFalse("Inverted Row offered without a bar to hang from", results.contains("Inverted Row"))
        assertFalse(
            "Dumbbell Bench Press offered without a bench",
            results.contains("Dumbbell Bench Press")
        )
        // ...while the row that only needs the floor and the dumbbells IS there, so the
        // exclusions above cannot be satisfied by emptying the list.
        assertTrue("Dumbbell Floor Press missing from a floor-press kit", results.contains("Dumbbell Floor Press"))
    }

    /** Granting the bench adds the bench movement and nothing else moves. */
    @Test
    fun addingTheMissingKitWidensTheList() {
        val withoutBench = pool()
        val withBench = pool(
            grantedTags = kitTags + ExerciseEquipmentTags.BENCH,
        )

        assertTrue("the bench kit still cannot bench", withBench.contains("Dumbbell Bench Press"))
        assertTrue(
            "the bench grant removed rows it should not have touched",
            withBench.containsAll(withoutBench)
        )
    }

    // ------------------------------------------------------- chips narrow, never re-grant

    /** A chip restricts the list to its category; unticking it puts those rows back. */
    @Test
    fun theEquipmentChipsNarrowAndWidenInBothDirections() {
        val dumbbellOnly = pool(selectedEquipment = listOf(WorkoutTemplates.EQUIPMENT_DUMBBELL))

        assertTrue("the Dumbbell chip left nothing to pick", dumbbellOnly.isNotEmpty())
        dumbbellOnly.forEach { name ->
            assertEquals(
                "'$name' survived the Dumbbell-only chip",
                WorkoutTemplates.EQUIPMENT_DUMBBELL,
                WorkoutTemplates.getEquipmentForExercise(name)
            )
        }
        assertFalse("Push-Up survived a Dumbbell-only selection", dumbbellOnly.contains("Push-Up"))

        val both = pool(selectedEquipment = kitCategories)
        assertTrue("re-selecting Bodyweight did not bring the bodyweight rows back", both.contains("Push-Up"))
        assertTrue("the wider selection lost rows", both.containsAll(dumbbellOnly))
    }

    /**
     * A chip cannot grant kit the user never declared. Ticking "Barbell" for a dumbbell
     * owner offers nothing rather than a list of movements they cannot do — the grid is
     * there to re-narrow what is possible, and the empty list is the honest answer.
     */
    @Test
    fun aChipCannotGrantKitTheUserDoesNotOwn() {
        val barbell = pool(selectedEquipment = listOf(WorkoutTemplates.EQUIPMENT_BARBELL))

        assertTrue("a dumbbell owner was offered barbell movements: $barbell", barbell.isEmpty())
    }

    // ------------------------------------------------------------------ muscle filtering

    /** A body part filter keeps only that part, and is genuinely narrower than no filter. */
    @Test
    fun theMuscleFilterKeepsOnlyThatBodyPart() {
        val unfiltered = pool()
        val chest = pool(selectedMuscleGroup = "Chest")

        assertTrue("the Chest filter returned nothing", chest.isNotEmpty())
        assertTrue(
            "the Chest filter (${chest.size}) is not narrower than no filter (${unfiltered.size})",
            chest.size < unfiltered.size
        )
        chest.forEach { name ->
            assertEquals(
                "'$name' is offered under the Chest filter but trains something else",
                "Chest",
                WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(name))
            )
            assertTrue("'$name' is outside the kit's pool", unfiltered.contains(name))
        }
    }

    /**
     * A precise muscle is answered by its own pool, so it must still name the exercise it
     * is the filter for instead of dropping it — the edge the broad-part partition does not
     * cover.
     */
    @Test
    fun aPreciseMuscleKeepsTheExerciseItIsNamedAfter() {
        val head = pool().first()
        val precise = WorkoutTemplates.getPrimaryMuscleGroupForExercise(
            head,
            WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(head))
        )
        assumeTrue("expected a precise primary muscle for $head", precise.isNotBlank())

        val results = pool(selectedMuscleGroup = precise)

        assertTrue(
            "the precise filter '$precise' dropped $head, the exercise it names",
            results.contains(head)
        )
        assertTrue(
            "the precise filter is not constrained by the unfiltered pool",
            pool().containsAll(results)
        )
    }

    // ------------------------------------------------------------------- order and hygiene

    @Test
    fun theListIsPopularityOrderedAndHasNoRepeats() {
        val results = pool()

        assertEquals("the same exercise is listed twice", results.distinct(), results)
        assertEquals(
            "the popularity order was lost (filtering after sorting?)",
            ExercisePopularity.sortedByPopularity(results),
            results
        )
        // The staples the owner would actually pick scroll first: a dumbbell kit's list
        // that opens on an obscure variation is the reason this sort exists.
        assertTrue(
            "the kit's list does not open on a staple: ${results.take(3)}",
            results.take(5).any { it.contains("Goblet Squat") || it.contains("Push-Up") }
        )
    }

    /** The row being replaced is never offered back, however it was capitalised. */
    @Test
    fun theRowBeingReplacedIsNotOfferedBack() {
        val victim = pool().first()
        val excluded = pool(excludedName = victim)
        val excludedLower = pool(excludedName = victim.lowercase())

        assertFalse("$victim was offered back to be replaced by itself", excluded.contains(victim))
        assertFalse("the exclusion is case-sensitive", excludedLower.contains(victim))
        assertEquals("the exclusion dropped more than the one row", pool().size - 1, excluded.size)
    }

    /**
     * A user who declared no equipment at all is not caged: the picker answers the whole
     * catalogue. This is the same null-vs-empty distinction the generator makes — null
     * tags mean "never restricted the kit", while an empty set is the strict
     * bodyweight-only preset and stays strict.
     */
    @Test
    fun anUndeclaredKitOffersTheWholeCatalogue() {
        // No chips ticked, no tags and no declared kit: the user is not caged at all.
        val all = pool(grantedTags = null, categories = emptyList(), selectedEquipment = emptyList())

        assertEquals(
            "the unrestricted picker does not list the whole catalogue",
            WorkoutTemplates.getAllExerciseNames().toSet(),
            all.toSet()
        )
        assertEquals("the unrestricted list is not popularity ordered", ExercisePopularity.sortedByPopularity(all), all)

        // The strict preset is the other half of that distinction: an EMPTY tag set is a
        // bodyweight-only user, so the bar movements inside their own category stay out.
        val bodyweightOnly = pool(grantedTags = emptySet(), categories = listOf(WorkoutTemplates.EQUIPMENT_BODYWEIGHT))
        assertTrue("the bodyweight preset offers nothing at all", bodyweightOnly.isNotEmpty())
        assertTrue("the bodyweight preset offers a bar movement", bodyweightOnly.none { it == "Pull-Up" })
        assertTrue("the bodyweight preset offers barbell work", bodyweightOnly.none { it.contains("Barbell") })
    }
}
