package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A CHAIR IS NOT A BENCH.
 *
 * The owner: "I only have chair and dumbell and yet the system asks me to do an
 * inclinde dumbell press which doesnt work without a bench." The gate used to
 * answer `chairOrBenchWith(DUMBBELLS_ONLY)` for every bench-family Dumbbell
 * movement - a chair satisfied an incline press - because "a chair or a bench"
 * was treated as one piece of furniture.
 *
 * It is not. An incline or a decline needs an adjustable back angle, and a
 * chest-supported row needs a pad to brace against; a kitchen chair offers
 * neither. Movements where the furniture is only something to put a hand or a
 * foot on (bench dips, incline/decline push-ups) still accept a chair and are
 * deliberately NOT in this list.
 */
class BenchMovementGateTest {

    /** The angle/support movements this gate must protect. */
    private val benchOnly = listOf(
        "Chest Supported Dumbbell Shrug",
        "Chest-Supported Dumbbell Row",
        "Close-Grip Dumbbell Bench Press",
        "Decline Bench Press",
        "Decline Dumbbell Fly",
        "Dumbbell Bench Press",
        "Dumbbell Bench Pull",
        "Dumbbell Face Pull",
        "Dumbbell Fly",
        "Incline Dumbbell Curl",
        "Incline Dumbbell Fly",
        "Incline Dumbbell Press",
        "Incline Hammer Curl",
        "Single-Arm Chest-Supported Dumbbell Row",
        "Spider Curl",
    )

    private val dumbbellsOnly = setOf(
        ExerciseEquipmentTags.DUMBBELLS_ONLY,
        ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR,
    )
    private val dumbbellsAndChair = dumbbellsOnly + ExerciseEquipmentTags.CHAIR
    private val dumbbellsAndBench = dumbbellsOnly + ExerciseEquipmentTags.BENCH

    @Test
    fun everyBenchMovementRejectsAChair() {
        for (name in benchOnly) {
            assertFalse(
                "$name needs a bench's angle or pad; a chair must not satisfy it",
                ExerciseEquipmentTags.isAvailable(name, dumbbellsAndChair),
            )
        }
    }

    @Test
    fun everyBenchMovementAcceptsARealBench() {
        for (name in benchOnly) {
            assertTrue(
                "$name must still be available to a dumbbells + bench kit",
                ExerciseEquipmentTags.isAvailable(name, dumbbellsAndBench),
            )
        }
    }

    /**
     * The counterpart, so the fix cannot over-tighten: movements whose furniture
     * is only a hand/foot support keep working on a chair.
     */
    @Test
    fun chairMovementWorkIsNotBroken() {
        for (name in listOf("Bulgarian Split Squat", "Bench Dips", "Incline Push-Up", "Decline Push-Up")) {
            assertTrue(
                "$name works on a chair and must stay available to a chair-only kit",
                ExerciseEquipmentTags.isAvailable(name, dumbbellsAndChair),
            )
        }
    }

    /**
     * THE SWEEP, not the list: this is the actual defect class. Any future pool
     * row that is a Dumbbell angle/support movement must be bench-gated even if
     * nobody remembers to add it above.
     */
    @Test
    fun noBenchFamilyDumbbellRowIsReachableByAChairOnlyKit() {
        for (entry in ExerciseCatalog.ENTRIES) {
            if (entry.equipment != "Dumbbell") continue
            val n = entry.name.lowercase()
            // "face pull" and "spider" joined this list on 2026-11-10: the owner
            // sent a photo of a dumbbell face pull braced on a bench and asked for
            // it across the board. A spider curl is the same demand from the other
            // end of the body - the upper arms hang off an incline pad - and it was
            // dumbbells-only while its EZ-bar sibling already required a bench.
            val isBenchFamily = listOf(
                "incline", "decline", "chest-supported", "chest supported",
                "face pull", "spider",
            ).any { n.contains(it) }
            // `push-up`/`crunch`/`dip` furniture work legitimately takes a chair.
            val chairIsFine = listOf("push-up", "pushup", "crunch", "dip").any { n.contains(it) }
            if (!isBenchFamily || chairIsFine) continue
            assertFalse(
                "${entry.name} is a bench-family incline/decline/support row " +
                    "but a chair-only kit can reach it",
                ExerciseEquipmentTags.isAvailable(entry.name, dumbbellsAndChair),
            )
        }
    }

    /**
     * The softer half of the same class: a movement performed SITTING needs
     * something to sit on, and a chair is enough. These four were dumbbells-only,
     * so a kit with two bells and no furniture at all was still offered them.
     */
    @Test
    fun seatedDumbbellWorkNeedsSomethingToSitOn() {
        for (name in listOf(
            "Seated Dumbbell Curl",
            "Seated Dumbbell Lateral Raise",
            "Seated Dumbbell Shoulder Press",
            "Seated Dumbbell Tricep Extension",
        )) {
            assertFalse(
                "$name is performed seated; a kit with no furniture cannot do it",
                ExerciseEquipmentTags.isAvailable(name, dumbbellsOnly),
            )
            assertTrue(
                "$name works on a chair and must stay available to a chair kit",
                ExerciseEquipmentTags.isAvailable(name, dumbbellsAndChair),
            )
        }
    }

    /**
     * The owner's card in its own words: "not only based on the name". The
     * library's own instructions are evidence, so a dumbbell row whose method text
     * puts the lifter on a bench must be gated even when its name says nothing.
     * The three exceptions name a no-bench setup in the same breath (a thigh, the
     * floor), and gating those would take away work the user can really do.
     */
    @Test
    fun instructionsThatNameABenchAreEnough() {
        val honestWithoutABench = setOf(
            "Dumbbell Pullover", "Single-Arm Dumbbell Row", "Lying Tricep Extension",
        )
        for (entry in ExerciseCatalog.ENTRIES) {
            if (entry.equipment != "Dumbbell") continue
            if (entry.name in honestWithoutABench) continue
            val method = entry.description.lowercase()
            val saysBench = listOf("on the bench", "on a bench").any { method.contains(it) }
            if (!saysBench) continue
            assertFalse(
                "${entry.name}'s own instructions put the lifter on a bench, " +
                    "but a dumbbells-only kit can reach it",
                ExerciseEquipmentTags.isAvailable(entry.name, dumbbellsOnly),
            )
        }
    }

    /** A chair seats a curl; it does not seat a fly. */
    @Test
    fun aChairSeatsWhatItCanAndNoMore() {
        assertFalse(
            "Concentration Curl is performed sitting, so a kit with no furniture cannot do it",
            ExerciseEquipmentTags.isAvailable("Concentration Curl", dumbbellsOnly),
        )
        assertTrue(
            "Concentration Curl works on a chair",
            ExerciseEquipmentTags.isAvailable("Concentration Curl", dumbbellsAndChair),
        )
        assertFalse(
            "Dumbbell Fly needs the bench under the back, not a chair",
            ExerciseEquipmentTags.isAvailable("Dumbbell Fly", dumbbellsAndChair),
        )
    }

    /**
     * The other guard, so "every exercise" cannot be satisfied by tightening until
     * the pool is empty: dumbbells plus a bench must still reach EVERY dumbbell row.
     */
    @Test
    fun aDumbbellsAndBenchKitReachesEveryDumbbellRow() {
        for (entry in ExerciseCatalog.ENTRIES) {
            if (entry.equipment != "Dumbbell") continue
            assertTrue(
                "${entry.name} must be reachable with dumbbells + a bench",
                ExerciseEquipmentTags.isAvailable(entry.name, dumbbellsAndBench),
            )
        }
    }
}
