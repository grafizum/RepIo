package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner, 2026-11-11 (board task_240k4p5w583755053t162c4b): "For some exercises
 * (each) on weights makes no sense when its only one weight."
 *
 * The "each" suffix is not typed anywhere - it comes from the row's unit
 * (`kg_per_dumbbell` -> LoadUnit.KG_EACH -> "25 kg each"). A one-bell movement
 * must therefore stop declaring a per-hand load, and a two-bell movement must
 * keep it; both halves are asserted here so neither can drift back.
 *
 * The list is authored in [ProgressionEngine.SINGLE_IMPLEMENT_SLUGS]. Naming
 * movements here (rather than in the engine) is the point: if a slug in that
 * set stops resolving, this test fails instead of silently losing a fix.
 */
class SingleImplementLoadLabelTest {

    /** Movements performed with one dumbbell - the "each" suffix is wrong on each. */
    private val oneBell = listOf(
        "Dumbbell Pullover",
        "Dumbbell Svend Press",
        "Single Dumbbell Svend Press",
        "Concentration Curl",
        "Dumbbell Side Bend",
        "Dumbbell Windmill",
        "One-Arm Dumbbell Push Press",
        "One-Arm Dumbbell Swing",
        "Single-Arm Dumbbell Row",
        "Single-Arm Hammer Curl",
        "Single-Arm Chest-Supported Dumbbell Row",
        "Single-Arm Dumbbell Overhead Tricep Extension",
        "One-Arm Single-Leg Dumbbell Romanian Deadlift",
        "Dumbbell Pistol Squat",
        "Jefferson Curl",
        "Cross Body Hammer Curl",
        "Dumbbell Tricep Kickback",
    )

    /** Movements that genuinely hold a bell in each hand - these keep "each". */
    private val twoBells = listOf(
        "Dumbbell Bench Press",
        "Dumbbell Bicep Curl",
        "Dumbbell Fly",
        "Dumbbell Lateral Raise",
        "Bent-Over Dumbbell Row",
    )

    @Test
    fun oneBellMovementsDoNotSayEach() {
        for (name in oneBell) {
            val entry = ProgressionEngine.entryFor(name)
            assertNotNull("the pool must still contain $name", entry)
            val prescribed = ProgressionEngine.prescribedValueAtLevel(entry!!, 1)
            assertNotNull("$name must prescribe a load at level 1", prescribed)
            assertEquals(
                "$name is held in one hand, so its weight must not read 'each'",
                ProgressionEngine.LoadUnit.KG,
                prescribed!!.loadUnit,
            )
        }
    }

    @Test
    fun twoBellMovementsKeepEach() {
        for (name in twoBells) {
            val entry = ProgressionEngine.entryFor(name)
            assertNotNull("the pool must still contain $name", entry)
            val prescribed = ProgressionEngine.prescribedValueAtLevel(entry!!, 1)
            assertNotNull("$name must prescribe a load at level 1", prescribed)
            assertEquals(
                "$name uses a bell in each hand, so its weight must still read 'each'",
                ProgressionEngine.LoadUnit.KG_EACH,
                prescribed!!.loadUnit,
            )
        }
    }

    @Test
    fun theWordingChangeDoesNotMoveAnyNumber() {
        // The fix is a label, so the ladder itself must be untouched: level 1 of a
        // one-bell row is the same weight it always was.
        val entry = ProgressionEngine.entryFor("Dumbbell Pullover")!!
        val levelOne = ProgressionEngine.prescribedValueAtLevel(entry, 1)!!.value
        assertTrue("the pullover's level 1 must still carry a real load", levelOne!! > 0.0)
        assertEquals(
            "the level-1 load is the row's own, not something the label change picked",
            ProgressionEngine.valueAtLevelWithCeiling(entry, 1),
            levelOne,
        )
    }
}
