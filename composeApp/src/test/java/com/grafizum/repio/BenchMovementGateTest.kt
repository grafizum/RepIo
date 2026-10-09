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
        "Incline Dumbbell Curl",
        "Incline Dumbbell Fly",
        "Incline Dumbbell Press",
        "Incline Hammer Curl",
        "Single-Arm Chest-Supported Dumbbell Row",
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
            val isBenchFamily = listOf("incline", "decline", "chest-supported", "chest supported")
                .any { n.contains(it) }
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
}
