package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker equipment gate must be driven by the user's FINE tags, not by the
 * coarse category list the chips speak.
 *
 * `coarseCategoriesFor` reduces a selection to `["Bodyweight", "Dumbbell",
 * "Bands", …]`. That vocabulary has **no category for a chair, a bench or a
 * pull-up bar** — the furniture home users actually tick — so any gate rebuilt
 * from the coarse list has already lost them. Measured on a
 * dumbbells+chair+bench+bands+bar selection, the round trip through
 * `allowedTagsFor(coarse)` dropped `chair`, `bench` and `pull_up_bar` outright.
 *
 * The direction of that defect matters and is the reason it survived: the gate
 * only ever REMOVES rows, so a lost grant does not look like a leak in casual
 * use. What it does is shrink the pool — and because the generator substitutes
 * into that same shrunken pool, a user who ticked "pull-up bar" was quietly
 * pushed toward bodyweight and dumbbell substitutes for movements they own the
 * kit for. That is the same class of bug as the equipment leak the gate was
 * added to fix, pointed the other way.
 */
class CoarseRoundTripTagLossTest {

    /** A realistic home kit: dumbbells, a chair, a bench, bands and a bar. */
    private val homeKit = listOf(
        ExerciseEquipmentTags.PRESET_CUSTOM,
        ExerciseEquipmentTags.DUMBBELLS_ONLY,
        ExerciseEquipmentTags.CHAIR,
        ExerciseEquipmentTags.BENCH,
        ExerciseEquipmentTags.RESISTANCE_BANDS,
        ExerciseEquipmentTags.PULL_UP_BAR,
    )

    private fun granted(equipment: List<String>) =
        WorkoutTemplates.grantedTagsForPublic(equipment) ?: emptySet()

    // ---------- 1. The coarse vocabulary cannot express the kit ----------

    /**
     * The premise of the whole test. If a future coarse category ever covers
     * the furniture, these two assertions should be revisited — but until then
     * they are what makes the loss below inevitable rather than accidental.
     */
    @Test
    fun theCoarseVocabularyHasNoCategoryForHomeFurniture() {
        val coarse = ExerciseEquipmentTags.coarseCategoriesFor(homeKit)
        assertTrue(
            "expected no coarse category naming the chair, bench or bar, got $coarse",
            coarse.none { it.contains("chair", true) || it.contains("bench", true) || it.contains("bar", true) },
        )
    }

    /** The round trip really does drop the grants the user ticked. */
    @Test
    fun rebuildingTheGateFromTheCoarseListLosesTheFurniture() {
        val coarse = ExerciseEquipmentTags.coarseCategoriesFor(homeKit)
        val roundTripped = ExerciseEquipmentTags.allowedTagsFor(coarse)

        val lost = granted(homeKit) - roundTripped
        assertTrue(
            "expected chair/bench/pull_up_bar to be lost in the coarse round trip, lost=$lost",
            lost.containsAll(
                setOf(
                    ExerciseEquipmentTags.CHAIR,
                    ExerciseEquipmentTags.BENCH,
                    ExerciseEquipmentTags.PULL_UP_BAR,
                )
            ),
        )
    }

    // ---------- 2. The gate the pickers use keeps them ----------

    /**
     * The fix: the dialogs take the granted FINE tags. This walks the same
     * reduction the UI used to do and asserts nothing the user owns is lost.
     */
    @Test
    fun theFineTagGateKeepsEveryGrantTheUserTicked() {
        val fine = granted(homeKit)
        listOf(
            ExerciseEquipmentTags.CHAIR,
            ExerciseEquipmentTags.BENCH,
            ExerciseEquipmentTags.PULL_UP_BAR,
            ExerciseEquipmentTags.DUMBBELLS_ONLY,
            ExerciseEquipmentTags.RESISTANCE_BANDS,
        ).forEach { tag ->
            assertTrue("$tag should be granted by the user's own selection", tag in fine)
        }
    }

    /** The user-facing consequence: kit they own must actually be reachable. */
    @Test
    fun exercisesForTheFurnitureTheUserOwnsAreNotGatedOut() {
        val fine = granted(homeKit)
        listOf("Pull-Up", "Bench Dips", "Decline Push-Up").forEach { name ->
            assertTrue(
                "$name should be available to a user who ticked the bar/chair/bench",
                ExerciseEquipmentTags.isAvailable(name, fine),
            )
        }
    }

    // ---------- 3. And the gate still refuses kit they do NOT own ----------

    /**
     * The point of threading the fine tags is to make the gate TIGHTER, not
     * looser. A dropped grant must never become a free pass, so a kit with no
     * machine grant must still see no machine row.
     */
    @Test
    fun theFineTagGateStillRefusesUnownedKit() {
        val fine = granted(homeKit)
        assertFalse(
            "a home kit owns no smith machine, so no smith row may pass",
            ExerciseEquipmentTags.isAvailable("Smith Machine Squat", fine),
        )
        ExerciseCatalog.ENTRIES
            .filter { ExerciseEquipmentTags.requirementsFor(it.name)
                .any { req -> ExerciseEquipmentTags.SMITH_MACHINE in req } }
            .forEach { entry ->
                assertFalse(
                    "${entry.name} requires a smith machine and must not pass a home kit",
                    ExerciseEquipmentTags.isAvailable(entry.name, fine),
                )
            }
    }

    /** Unrestricted stays unrestricted — a null gate is not an empty one. */
    @Test
    fun anUnrestrictedUserKeepsTheWholePool() {
        assertTrue(
            WorkoutTemplates.grantedTagsForPublic(listOf(ExerciseEquipmentTags.PRESET_FULL_GYM))
                ?.contains(ExerciseEquipmentTags.SMITH_MACHINE) ?: false
        )
    }

    // ---------- 4. The resolver the dialogs actually run ----------

    /**
     * This is the assertion that protects the fix. The two dialogs call
     * [WorkoutTemplates.pickerGrantedTags], so a host that hands over its fine
     * tags keeps the chair/bench/bar grants — this fails the moment either
     * dialog is wired back to the lossy coarse round trip.
     */
    @Test
    fun theDialogResolverPrefersTheFineTagsTheHostSupplies() {
        val fine = granted(homeKit)
        val coarse = ExerciseEquipmentTags.coarseCategoriesFor(homeKit)

        val resolved = WorkoutTemplates.pickerGrantedTags(fine, coarse)

        assertEquals(
            "the dialog gate must be the user's fine tags, not a coarse re-expansion",
            fine,
            resolved,
        )
        listOf(
            ExerciseEquipmentTags.CHAIR,
            ExerciseEquipmentTags.BENCH,
            ExerciseEquipmentTags.PULL_UP_BAR,
        ).forEach { tag ->
            assertTrue("$tag must survive into the dialog gate", tag in resolved!!)
        }
    }

    /** A host with no fine tags still gets the old behaviour, not a crash. */
    @Test
    fun theDialogResolverFallsBackToTheCoarseList() {
        val coarse = ExerciseEquipmentTags.coarseCategoriesFor(homeKit)
        val resolved = WorkoutTemplates.pickerGrantedTags(null, coarse)
        assertNotNull("a restricted host must still get a gate", resolved)
        assertEquals(ExerciseEquipmentTags.allowedTagsFor(coarse), resolved)
    }

    /** Null in, null out: an unrestricted user is gated by nothing. */
    @Test
    fun theDialogResolverKeepsUnrestrictedNull() {
        assertNull(WorkoutTemplates.pickerGrantedTags(null, emptyList()))
    }
}
