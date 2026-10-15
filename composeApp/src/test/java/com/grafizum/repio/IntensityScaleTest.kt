package com.grafizum.repio

import com.grafizum.repio.data.rpeRirFor
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The intensity word on an exercise row, read as an RPE/RIR figure.
 *
 * Owner prompt, 2026-10-15: *"we have RPE and RIR inside the settings instructions ... i want it
 * to be integrated in the workout creation or exercises (just as labels for users nothing more)
 * find out a way to integrate it seemlessly without needing to change so much."*
 *
 * The seamlessness is that nothing new is authored: the templates already write an intensity word
 * on every row, and until this existed only a row that literally said "RPE 8" was explained. So
 * these cases hold two things at once — the words the app really writes map to a figure, and
 * anything else maps to NOTHING, because a label that invents a number for "Beast mode" is worse
 * than no label at all.
 */
class IntensityScaleTest {

    /**
     * The values [com.grafizum.repio.data.WorkoutTemplates.getGoalIntensity] returns, plus the
     * difficulty axis the setup flow stores before a goal is picked. If a new word is ever added
     * to that resolver, this case is where its missing figure shows up.
     */
    @Test
    fun everyWordTheAppWritesHasAFigure() {
        val words = mapOf(
            "To Failure" to 10,
            "Very Heavy" to 9,
            "Heavy" to 8,
            "High Intensity" to 8,
            "Moderate" to 7,
            "Light" to 6,
            "Easy" to 6,
            "Medium" to 7,
            "Hard" to 8
        )
        for ((word, rpe) in words) {
            val figure = rpeRirFor(word)
            assertTrue("the app writes \"$word\" but it has no figure", figure != null)
            assertEquals("\"$word\" means RPE $rpe", rpe, figure!!.low)
            assertEquals("\"$word\" is not a range", false, figure.isRange)
        }
    }

    /**
     * RPE and RIR are one scale read from both ends, so the reserve is DERIVED and the two halves
     * can never disagree. This is the assertion that would catch a table filled in by hand with a
     * second column of numbers.
     */
    @Test
    fun theReserveIsTheOtherEndOfTheSameScale() {
        assertEquals(3, rpeRirFor("Moderate")!!.rirLow)
        assertEquals(3, rpeRirFor("Moderate")!!.rirHigh)
        assertEquals(0, rpeRirFor("To Failure")!!.rirLow)
        assertEquals(1, rpeRirFor("Very Heavy")!!.rirLow)
        assertEquals(4, rpeRirFor("Light")!!.rirLow)
        // A band: RPE 8–9 is 1 to 2 reps in reserve, and the LOWER reserve belongs to the
        // HIGHER rpe - the ends swap, and swapping them is the easy mistake.
        val band = rpeRirFor("RPE 8-9")!!
        assertEquals(1, band.rirLow)
        assertEquals(2, band.rirHigh)
    }

    /** An unknown word is not a guess: it is the old behaviour, which is the word alone. */
    @Test
    fun aWordWithNoFigureIsUnknownRatherThanGuessed() {
        for (unknown in listOf("", "   ", "Beast mode", "Moderate-ish", "RPE", "reps in reserve", "all out")) {
            assertNull("\"$unknown\" must not be given a figure", rpeRirFor(unknown))
        }
        // ...while a reserve written loosely still counts, because it does state one: the
        // number and the word are what carry the meaning, not the punctuation around them.
        assertEquals(8, rpeRirFor("2 RIR reps?")!!.low)
    }

    @Test
    fun caseAndSurroundingSpaceAreForgotten() {
        assertEquals(7, rpeRirFor("  MODERATE  ")!!.low)
        assertEquals(8, rpeRirFor("Heavy")!!.low)
        assertEquals(10, rpeRirFor("to failure")!!.low)
    }

    /**
     * The other three ways the field is filled in by hand. "2 RIR" is the reserve end of the same
     * scale, so its RPE is 10 - 2, and a half step is refused rather than rounded down.
     */
    @Test
    fun theFieldCanAlsoBeWrittenAsAnRpeOrAReserve() {
        assertEquals(8, rpeRirFor("RPE 8")!!.low)
        assertEquals(8, rpeRirFor("rpe 8")!!.low)
        assertEquals(8, rpeRirFor("8 rpe")!!.low)
        assertEquals(8, rpeRirFor("2 RIR")!!.low)
        assertTrue("a reserve is not a range", !rpeRirFor("2 RIR")!!.isRange)
        assertEquals(7, rpeRirFor("3 reps in reserve")!!.low)

        // Refused rather than silently read as the whole number below it.
        assertNull("a half step has no whole figure", rpeRirFor("RPE 8.5"))
        assertNull("an inverted band is not a band", rpeRirFor("RPE 9-8"))
    }

    /**
     * The half a mapping test cannot hold: every label site still goes through the ONE helper.
     *
     * There are four places the intensity text is shown (the routine card, the setup preview, the
     * editor's hint and Home's schedule row), and the first ones were already two copies of one
     * expression before this change.
     * A future edit that re-inlines the old `${'$'}{word} (${'$'}explanation)` shape in one of
     * them would leave that screen printing a bare adjective again while the others printed the
     * figure — which is exactly the kind of drift the copies caused the first time.
     */
    @Test
    fun everyLabelSiteUsesTheOneHelper() {
        val rowSite = File("src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt").readText()
        val setupSite = File("src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt").readText()
        val scheduleSite = File("src/main/java/com/grafizum/repio/ui/screens/HomeScreen.kt").readText()

        assertTrue(
            "the exercise row no longer uses displayIntensityLabel, so it shows the bare word",
            rowSite.contains("val displayIntensity = displayIntensityLabel(exercise.targetIntensity)")
        )
        assertTrue(
            "the setup preview no longer uses displayIntensityLabel",
            setupSite.contains("displayIntensityLabel(exercise.targetIntensity)")
        )
        assertTrue(
            "Home's schedule row no longer uses displayIntensityLabel, so the list a user reads " +
                "before training shows the bare adjective again",
            scheduleSite.contains(
                "val displayIntensity = displayIntensityLabel(exercise.targetIntensity)"
            )
        )
        assertTrue(
            "Home's schedule row builds its own intensity label again instead of calling the helper",
            !scheduleSite.contains("getRpeRirExplanation")
        )
        // WHERE THE EXPLANATION IS ASKED FOR, and how many times. WorkoutComponents.kt is the
        // helper's own file, so exactly two mentions are right: the declaration and the call
        // inside it. A third is a caller that went back to building the label itself, which is
        // the shape that left the setup preview printing a bare word while the card printed the
        // figure. FirstTimeSetupScreen must not ask at all - it only renders the helper's answer.
        assertEquals(
            "WorkoutComponents.kt asks for the RPE explanation outside the one helper again, so a " +
                "label site is building its own text",
            2,
            Regex("""getRpeRirExplanation\(""").findAll(rowSite).count()
        )
        assertTrue(
            "the setup preview builds its own intensity label again instead of calling the helper",
            !setupSite.contains("getRpeRirExplanation")
        )
    }
}
