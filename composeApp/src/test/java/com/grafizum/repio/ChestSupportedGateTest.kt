package com.grafizum.repio

import com.grafizum.repio.data.ExerciseEquipmentTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "chest supported" question, answered with the gate rather than with an opinion.
 *
 * Board task_6e584h5d2j1z56173p5m081a (2026-10-01): "Lot of exercises say 'chest
 * supported' but i wanted to generalize the terms so i dont understand what this
 * is supposed to change in comparison with the same exercises that doesnt have
 * 'chest supported' written in the front. Explain it and on my signal change it."
 *
 * The owner is told NOT to rename on this task, and the reason turns out to be in
 * the equipment gate: `requirementsFor` matches the word "chest-supported" to put
 * the row on a bench. The word IS the requirement. Strip it and the row becomes
 * dumbbells-only, which is the equipment-leak class this project has now paid for
 * three separate times (the chair/bench split, the coarse-tag round trip, the seven
 * mis-categorised catalog rows).
 *
 * So the generalization the owner asked for in September deliberately never touched
 * support/posture words - `generalize.py` moves APPARATUS words only - and this test
 * is the standing proof that the rule is load-bearing rather than conservative.
 *
 * It also caught a live hole: ONE of the five rows spells it "Chest Supported" with
 * a space. A `contains("chest-supported")` branch cannot see that spelling, so that
 * shrug was being offered to users who own no bench at all.
 */
class ChestSupportedGateTest {

    /** Dumbbells and nothing else: no bench, no chair. */
    private val dumbbellsOnly = setOf(ExerciseEquipmentTags.DUMBBELLS_ONLY)

    /** The same kit plus somewhere to lie on. */
    private val dumbbellsAndBench =
        ExerciseEquipmentTags.allowedTagsFor(
            listOf(ExerciseEquipmentTags.DUMBBELLS_ONLY, ExerciseEquipmentTags.BENCH)
        )

    @Test
    fun aChestSupportedRowNeedsASeatAndAPlainRowDoesNot() {
        assertFalse(
            "Chest-Supported Dumbbell Row must not be offered to a dumbbells-only kit",
            ExerciseEquipmentTags.isAvailable("Chest-Supported Dumbbell Row", dumbbellsOnly)
        )
        assertTrue(
            "and it must be offered once the user owns a bench",
            ExerciseEquipmentTags.isAvailable("Chest-Supported Dumbbell Row", dumbbellsAndBench)
        )
        assertTrue(
            "the plain row needs nothing but the dumbbells, which is the difference",
            ExerciseEquipmentTags.isAvailable("Bent-Over Dumbbell Row", dumbbellsOnly)
        )
    }

    /**
     * The bug this file was written for. One row is spelled with a SPACE, so the
     * hyphen-matching branch never saw it and it silently fell through to
     * dumbbells-only - a bench exercise shown to someone with no bench.
     */
    @Test
    fun theSpaceSpelledRowIsGatedLikeItsHyphenatedSiblings() {
        assertFalse(
            "'Chest Supported Dumbbell Shrug' (spaced) leaked to a dumbbells-only kit - " +
                "the gate matches the hyphenated spelling only",
            ExerciseEquipmentTags.isAvailable("Chest Supported Dumbbell Shrug", dumbbellsOnly)
        )
    }

    /**
     * Every pool row that says "chest supported" in any spelling must ask for a
     * seat. This is the sweep that would have caught the spaced row on the day it
     * was written, and it is written so a future rename that reintroduces the
     * inconsistency fails here instead of in a user's picker.
     */
    @Test
    fun everyChestSupportedRowInThePoolAsksForASeat() {
        val rows = com.grafizum.repio.data.ExerciseCatalog.ENTRIES
            .filter { it.name.contains("chest supported", ignoreCase = true) }
        assertTrue("expected chest-supported rows in the pool", rows.isNotEmpty())
        rows.forEach { entry ->
            assertFalse(
                "${entry.name} supports the chest but is offered to a dumbbells-only kit, " +
                    "so the word is not reaching the equipment gate",
                ExerciseEquipmentTags.isAvailable(entry.name, dumbbellsOnly)
            )
        }
    }

    /**
     * And the same sweep for the other support spellings the pool actually uses, so
     * the fix is not a patch for one name but for the class.
     */
    @Test
    fun everySupportSpellingThePoolUsesIsGated() {
        val spellings = listOf(
            "Chest-Supported Dumbbell Row",
            "Chest Supported Dumbbell Shrug",
            "Single-Arm Chest-Supported Dumbbell Row",
            "Chest-Supported Kettlebell Row",
            "Chest-Supported Smith Machine Row"
        )
        spellings.forEach { name ->
            assertFalse(
                "$name is a chest-supported row and must need a seat",
                ExerciseEquipmentTags.isAvailable(name, dumbbellsOnly)
            )
        }
    }
}
