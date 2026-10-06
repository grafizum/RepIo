package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.formatLoadValue
import com.grafizum.repio.ui.components.loadFieldPlaceholder
import com.grafizum.repio.ui.components.loadFieldValueText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the load UNIT an exercise is measured in.
 *
 * Dashboard task: "Lots of exercises have an increase of 'steps' instead of
 * weight. I don't know why but change it." The reason is that they are not
 * weights. A selectorized stack is counted in pin positions, and a pin means a
 * different resistance on every machine in every gym; a resistance band is
 * counted in tension and LOSES tension as it stretches, so a band step is not a
 * multiple of a weight either. Printing "4 steps" next to a prescription, and
 * labelling the field "kg", asked the user for a number that does not exist.
 *
 * These cannot be converted, only named honestly — so the test asserts they are
 * DISTINGUISHED from weight, and that nothing claims a step unit is a weight.
 */
class LoadUnitTest {

    @Test
    fun aSelectorizedStackIsAPinNotAKilogram() {
        assertEquals(
            ProgressionEngine.LoadUnit.STACK_STEP,
            ProgressionEngine.loadUnitFor("Cable Bent-Over Row")
        )
        assertEquals(
            ProgressionEngine.LoadUnit.STACK_STEP,
            ProgressionEngine.loadUnitFor("Leg Press")
        )
    }

    @Test
    fun aBandIsABandNotAKilogram() {
        assertEquals(
            ProgressionEngine.LoadUnit.RESISTANCE_STEP,
            ProgressionEngine.loadUnitFor("Banded Clamshell")
        )
    }

    @Test
    fun aFreeWeightIsStillWeight() {
        assertEquals(ProgressionEngine.LoadUnit.KG, ProgressionEngine.loadUnitFor("Barbell Back Squat"))
        assertEquals(
            ProgressionEngine.LoadUnit.KG_EACH,
            ProgressionEngine.loadUnitFor("Dumbbell Bicep Curl")
        )
    }

    /**
     * The step rows must actually EXIST in the pool, or the assertions above are
     * vacuously true on a null that would silently mean "no unit known".
     */
    @Test
    fun everyNonWeightUnitIsRealAndNoExerciseIsBoth() {
        var steps = 0
        var bands = 0
        var weights = 0
        for (name in WorkoutTemplates.getAllExerciseNames()) {
            val unit = ProgressionEngine.loadUnitFor(name)
            when (unit) {
                ProgressionEngine.LoadUnit.STACK_STEP -> steps++
                ProgressionEngine.LoadUnit.RESISTANCE_STEP -> bands++
                ProgressionEngine.LoadUnit.KG, ProgressionEngine.LoadUnit.KG_EACH -> weights++
                null -> {}
            }
        }
        assertEquals("no selectorized stack rows found", 64, steps)
        assertEquals("no resistance-band rows found", 17, bands)
        assertNotEquals("every exercise claims to be a weight", 0, weights)
    }

    /**
     * An exercise the user made up has no catalog row to ask, so it gets no unit
     * rather than a guessed one — the field then says "kg" as it always did.
     */
    @Test
    fun anUnknownExerciseHasNoUnitRatherThanAFalseOne() {
        assertNull(ProgressionEngine.loadUnitFor("Zorblax Deluxe Squatiser"))
        assertNull(ProgressionEngine.loadUnitFor(""))
    }

    /** A bodyweight row carries no load at all, so it is not "kg" either. */
    @Test
    fun aBodyweightExerciseCarriesNoLoadUnit() {
        assertNull(ProgressionEngine.loadUnitFor("Push-Up"))
    }

    /**
     * The starting weight is a KILOGRAM estimate, so a step row must return 0.0
     * rather than its pin count dressed as a weight — which is what would make a
     * "pin 3" render as "3 kg" on a machine where pin 3 is 30 kg.
     */
    @Test
    fun aStepRowStartsAtZeroKilogramsNotAtItsPinNumber() {
        assertEquals(0.0, WorkoutTemplates.getStartingWeightKg("Cable Bent-Over Row", "Medium"), 0.0)
        assertEquals(0.0, WorkoutTemplates.getStartingWeightKg("Leg Press", "Hard"), 0.0)
        assertEquals(0.0, WorkoutTemplates.getStartingWeightKg("Banded Clamshell", "Medium"), 0.0)
    }

    /**
     * The band ladder runs 1..10 and is rendered as five named tensions. The
     * boundaries matter: two rungs per band, and nothing outside 1..10, because a
     * clamped value would silently name a band the ladder never asked for.
     */
    @Test
    fun bandTensionBucketsTheWholeLadderAndNothingOutsideIt() {
        fun at(step: Double) = ProgressionEngine.bandTensionOf(step)
        assertEquals(ProgressionEngine.BandTension.VERY_LIGHT, at(1.0))
        assertEquals(ProgressionEngine.BandTension.VERY_LIGHT, at(2.0))
        assertEquals(ProgressionEngine.BandTension.LIGHT, at(3.0))
        assertEquals(ProgressionEngine.BandTension.LIGHT, at(4.0))
        assertEquals(ProgressionEngine.BandTension.MEDIUM, at(5.0))
        assertEquals(ProgressionEngine.BandTension.MEDIUM, at(6.0))
        assertEquals(ProgressionEngine.BandTension.HEAVY, at(7.0))
        assertEquals(ProgressionEngine.BandTension.HEAVY, at(8.0))
        assertEquals(ProgressionEngine.BandTension.VERY_HEAVY, at(9.0))
        assertEquals(ProgressionEngine.BandTension.VERY_HEAVY, at(10.0))
    }

    /**
     * The load FIELD is the other half of the same contract, and it was the half
     * that still lied. The unit label and suffix were fixed, but the field was
     * seeded with `Double.toString()`, so a stack row opened as "Pin" over "20.0"
     * and a band row asked for a decimal place its ladder does not have.
     *
     * Found on the S22 Ultra, not by reading: the edit sheet for Machine Chest Fly
     * (a stack_step row) rendered label "Pin", no suffix, value "20.0".
     */
    @Test
    fun aPinFieldShowsThePinNotAFractionOfOne() {
        assertEquals("20", loadFieldValueText("Machine Chest Fly", 20.0))
        assertEquals("6", loadFieldValueText("Leg Press", 6.0))
        assertEquals("1", loadFieldValueText("Banded Clamshell", 1.0))
    }

    @Test
    fun aWeightFieldKeepsItsDecimalBecauseKilogramsHaveHalves() {
        // The whole point of the fix is that it is not a blanket "strip .0":
        // 2.5 kg is a real prescription and 62.5 kg is a real one.
        assertEquals("20.0", loadFieldValueText("Dumbbell Curl", 20.0))
        assertEquals("2.5", loadFieldValueText("Dumbbell Curl", 2.5))
        assertEquals("62.5", loadFieldValueText("Bench Press", 62.5))
    }

    @Test
    fun anUnsetOrNegativeLoadIsStillAnEmptyField() {
        for (name in listOf("Machine Chest Fly", "Dumbbell Curl")) {
            assertEquals("", loadFieldValueText(name, 0.0))
            assertEquals("", loadFieldValueText(name, -2.5))
        }
    }

    /** A user's own fraction in a pin field is kept, not rounded away. */
    @Test
    fun aFractionalCountIsPrintedInFullRatherThanRounded() {
        assertEquals("2.5", loadFieldValueText("Leg Press", 2.5))
    }

    /**
     * The engine's ceiling-stretch math spreads a ladder's remaining growth over
     * its extra rungs, so the levels above the ladder top carry values like
     * 40.45454545454545. Printed raw, the level picker showed all sixteen digits
     * (owner report 2026-10-06: "levels with an enormous amount of numbers after
     * the sign"). The display path trims them to two decimals and drops a
     * trailing ".0".
     *
     * Two decimals rather than one so a real 1.25 kg plate increment survives.
     */
    @Test
    fun anInterpolatedLoadIsTrimmedToSomethingReadable() {
        assertEquals("40.45", formatLoadValue(40.45454545454545))
        assertEquals("40.91", formatLoadValue(40.90909090909091))
        assertEquals("41.36", formatLoadValue(41.36363636363637))
        // A whole value loses its decimal entirely.
        assertEquals("40", formatLoadValue(40.0))
        // Real prescriptions are untouched.
        assertEquals("2.5", formatLoadValue(2.5))
        assertEquals("62.5", formatLoadValue(62.5))
        assertEquals("1.25", formatLoadValue(1.25))
    }

    /**
     * The same trim on the way INTO the edit field, so seeding a stretched level
     * cannot put "40.45454545454545" over the weight box either.
     */
    @Test
    fun theLoadFieldNeverShowsInterpolationNoise() {
        assertEquals("40.45", loadFieldValueText("Dumbbell Curl", 40.45454545454545))
        // The weight field still keeps one decimal when whole.
        assertEquals("40.0", loadFieldValueText("Dumbbell Curl", 40.0))
    }

    @Test
    fun thePlaceholderIsInTheSameUnitAsTheValue() {
        assertEquals("0", loadFieldPlaceholder("Machine Chest Fly"))
        assertEquals("0", loadFieldPlaceholder("Banded Clamshell"))
        assertEquals("0.0", loadFieldPlaceholder("Dumbbell Curl"))
        assertEquals("0.0", loadFieldPlaceholder("Bench Press"))
    }

    /**
     * Every step row must agree: label, suffix, seeded value and placeholder are
     * four answers to one question, and the second editor (first-time setup) was
     * found still hardcoding "kg" while the first had been fixed.
     */
    @Test
    fun everyStepRowInThePoolIsLabelledAndSeededAsACount() {
        var checked = 0
        for (name in WorkoutTemplates.getAllExerciseNames()) {
            val unit = ProgressionEngine.loadUnitFor(name) ?: continue
            if (unit == ProgressionEngine.LoadUnit.KG || unit == ProgressionEngine.LoadUnit.KG_EACH) continue
            assertEquals(
                "$name is a $unit row but its field placeholder asks for a decimal",
                "0",
                loadFieldPlaceholder(name)
            )
            assertEquals(
                "$name is a $unit row but a whole load seeds a fractional field",
                "12",
                loadFieldValueText(name, 12.0)
            )
            checked++
        }
        assertTrue("expected step rows in the pool, found none", checked > 50)
    }
}
