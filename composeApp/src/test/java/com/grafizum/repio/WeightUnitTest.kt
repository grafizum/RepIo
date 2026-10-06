package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WeightUnit
import com.grafizum.repio.ui.components.displayLoadForUnit
import com.grafizum.repio.ui.components.loadFieldValueText
import com.grafizum.repio.ui.components.loadInputToKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Guards the kilogram/pound view of a load.
 *
 * Storage stays kilograms; the unit decides only how a weight is written down
 * and read back. Two things can go wrong quietly, and each has a case here:
 * a pound step that is not a weight a gym stocks, and a conversion that turns
 * two kilogram rungs into ONE pound rung — a level-up with nothing to do.
 */
class WeightUnitTest {

    private fun kgLoadRows() = ExerciseProgression.ENTRIES.filter {
        ProgressionEngine.valueKindOf(it) == ProgressionEngine.ValueKind.LOAD &&
            it.unit.contains("kg")
    }

    @Test
    fun aKilogramIsTwoPointTwoPounds() {
        assertEquals(2.2046, WeightUnit.LBS.fromKg(1.0), 0.0001)
        assertEquals(220.462, WeightUnit.LBS.fromKg(100.0), 0.001)
        // And back, without drift: the factor is exact by definition.
        assertEquals(100.0, WeightUnit.LBS.toKg(WeightUnit.LBS.fromKg(100.0)), 1e-9)
        assertEquals(40.0, WeightUnit.KG.fromKg(40.0), 1e-9)
    }

    /** The pound step is the rack's smallest jump, not a converted number. */
    @Test
    fun thePoundGridIsTheEquipmentSmallestJump() {
        assertEquals(2.5, WeightUnit.LBS.poundGrid(1.0), 1e-9)
        assertEquals(5.0, WeightUnit.LBS.poundGrid(2.0), 1e-9)
        assertEquals(5.0, WeightUnit.LBS.poundGrid(2.5), 1e-9)
        assertEquals(10.0, WeightUnit.LBS.poundGrid(5.0), 1e-9)
        assertEquals(2.5, WeightUnit.LBS.poundGrid(null), 1e-9)
        // A kilogram is not on a pound ladder at all.
        assertEquals(2.5, WeightUnit.KG.poundGrid(1.0), 1e-9)
    }

    @Test
    fun thePhoneCountryDecidesTheDefault() {
        assertEquals(WeightUnit.LBS, WeightUnit.defaultFor(Locale("en", "US")))
        assertEquals(WeightUnit.LBS, WeightUnit.defaultFor(Locale("en", "LR")))
        assertEquals(WeightUnit.KG, WeightUnit.defaultFor(Locale("de", "DE")))
        // Stones exist in the UK, but gym plates do not, so kg is the safer default.
        assertEquals(WeightUnit.KG, WeightUnit.defaultFor(Locale("en", "GB")))
        assertEquals(WeightUnit.KG, WeightUnit.defaultFor(Locale("en", "CA")))
    }

    @Test
    fun aStoredUnitIdRoundTripsAndNothingElseDoes() {
        assertEquals(WeightUnit.LBS, WeightUnit.fromId("lbs"))
        assertEquals(WeightUnit.KG, WeightUnit.fromId("kg"))
        assertEquals(null, WeightUnit.fromId("pounds"))
        assertEquals(null, WeightUnit.fromId(null))
    }

    /**
     * Every pound rung is a weight the gym can set, none repeats, and the top
     * rung is the researched ceiling. Swept over the whole pool, because this is
     * the rule that failed twice against real data while being written.
     */
    @Test
    fun everyPoundRungIsALoadTheGymCanSetAndNoTwoAreTheSame() {
        var rungs = 0
        var rows = 0
        for (entry in kgLoadRows()) {
            val ladder = ProgressionEngine.poundLadder(entry) ?: continue
            var previous = Double.NEGATIVE_INFINITY
            for (level in 1..ladder.levels) {
                val pounds = ladder.at(level)
                assertEquals(
                    "${entry.slug} level $level is ${pounds}lb",
                    (pounds / ladder.stepLb).toLong().toDouble(),
                    pounds / ladder.stepLb,
                    1e-9
                )
                assertTrue("${entry.slug} level $level repeats ${previous}lb", pounds > previous)
                previous = pounds
                rungs++
            }
            assertEquals(
                "${entry.slug} top rung is not the ceiling",
                entry.ceiling!!.load!! * WeightUnit.LB_PER_KG,
                ladder.at(ladder.levels),
                ladder.stepLb
            )
            rows++
        }
        // 250 of the 338 load rows: the rest carry no ceiling or no starting load.
        assertTrue("only $rows rows swept", rows >= 240)
        assertTrue("only $rungs rungs swept", rungs > 3_000)
    }

    /** A pin and a band rung are counts: "pin 6" is not thirteen pounds. */
    @Test
    fun aPinIsNotAWeight() {
        assertEquals(6.0, displayLoadForUnit("Leg Press", 6.0, WeightUnit.LBS), 1e-9)
        assertEquals("6", loadFieldValueText("Leg Press", 6.0, WeightUnit.LBS))
        assertEquals(6.0, loadInputToKg("Leg Press", "6", WeightUnit.LBS)!!, 1e-9)
    }

    /** What the user types comes back exactly as they typed it. */
    @Test
    fun whatTheUserTypesComesBackAsTheyTypedIt() {
        val name = "Dumbbell Bicep Curl"
        val stored = loadInputToKg(name, "25", WeightUnit.LBS)!!
        assertEquals(11.34, stored, 0.01)
        assertEquals("25.0", loadFieldValueText(name, stored, WeightUnit.LBS))
        // The kilogram path is untouched, including its one-decimal habit.
        assertEquals(12.5, loadInputToKg(name, "12.5", WeightUnit.KG)!!, 1e-9)
        assertEquals("12.5", loadFieldValueText(name, 12.5, WeightUnit.KG))
        // A comma decimal separator is accepted, as a German keyboard sends it.
        assertEquals(12.5, loadInputToKg(name, "12,5", WeightUnit.KG)!!, 1e-9)
    }

    /** The unit reaches the LADDER the UI reads, not just the formatter. */
    @Test
    fun thePickerReadsTheLadderInTheUsersUnit() {
        val curl = ProgressionEngine.entryFor("Dumbbell Bicep Curl")!!
        val ladder = ProgressionEngine.poundLadder(curl)!!
        assertEquals(ladder.levels, ProgressionEngine.effectiveMaxLevel(curl, WeightUnit.LBS))
        val top = ProgressionEngine.prescribedValueAtLevel(curl, ladder.levels, WeightUnit.LBS)!!
        assertEquals(ProgressionEngine.ValueKind.LOAD, top.kind)
        assertEquals(ladder.at(ladder.levels), top.value!!, 1e-9)
        // And the kilogram ladder is untouched by the pound one.
        assertTrue(ProgressionEngine.effectiveMaxLevel(curl) != ladder.levels)
        // A machine's pin ladder is not a weight ladder at all.
        assertEquals(null, ProgressionEngine.poundLadder(ProgressionEngine.entryFor("Leg Press")!!))
    }

    /** Switching units is a view: the kilogram value underneath never moves. */
    @Test
    fun theKilogramViewIsUnchangedByThePoundOne() {
        assertEquals(40.0, displayLoadForUnit("Dumbbell Bicep Curl", 40.0, WeightUnit.KG), 1e-9)
        // 40 kg is 88.18 lb, which lands on 87.5 on that row's 2.5 lb grid.
        assertEquals(87.5, displayLoadForUnit("Dumbbell Bicep Curl", 40.0, WeightUnit.LBS), 1e-9)
    }

    /**
     * The row whose authored ladder steps by 1 kg at the bottom: a grid taken
     * from its declared 2 kg increment let levels 1 and 2 both read 10 lb, which
     * the pool sweep caught. Pinned by name so the coarse-grid shortcut cannot
     * come back.
     */
    @Test
    fun aLadderWithFinerBottomStepsStillGetsDistinctPoundRungs() {
        val arnold = kgLoadRows().first { it.slug == "arnold_press" }
        val ladder = ProgressionEngine.poundLadder(arnold)!!
        // Rounded onto a grid this row collided at levels 1 and 2 on a 2.5 lb
        // grid and at level 11 on a 1.25 lb one; generated, it cannot.
        assertEquals(5.0, ladder.stepLb, 1e-9)
        assertTrue(ladder.at(2) > ladder.at(1))
        assertTrue(ladder.levels >= 10)
        for (level in 2..ladder.levels) {
            assertTrue("level $level", ladder.at(level) > ladder.at(level - 1))
        }
    }
}
