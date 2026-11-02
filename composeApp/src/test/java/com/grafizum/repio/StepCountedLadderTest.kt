package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.RepCeilings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the STEP-counted ladders, and the one rep ceiling the owner called out.
 *
 * Owner, 2026-10-24 (dashboard task_5u1n4l5m063l3m203045583y): *"Some exercises like heel to
 * toes walk have reps instead of steps."* and (task_6z0j2u394x6m07380t6x5805) *"For heel to
 * toes walk apparently the maxinum humanly 'possible' was reached at 30 steps ehich is
 * IMPOSSIBLE i could do 200 ... make sure the actually humanly possible amount for each
 * exercise is truly an actual value"*.
 *
 * The two walked drills now carry an authored ladder in STEPS (floor, ceiling, step) rather than
 * borrowing the rep ladder's ramp, and they are deliberately ABSENT from [RepCeilings] - one
 * exercise with two maxima is exactly how the numbers drift apart.
 */
class StepCountedLadderTest {

    private val heelToToe = ProgressionEngine.entryFor("Heel-to-Toe Walk")!!
    private val bearCrawl = ProgressionEngine.entryFor("Bear Crawl")!!
    private val pushUp = ProgressionEngine.entryFor("Push-Up")!!

    /** The unit word is a property of the EXERCISE, so a walked drill never says "Reps". */
    @Test
    fun theWalkedDrillsCountStepsAndNothingElseDoes() {
        assertTrue("Heel-to-Toe Walk should be counted in steps", ProgressionEngine.countsInSteps(heelToToe))
        assertTrue("Bear Crawl should be counted in steps", ProgressionEngine.countsInSteps(bearCrawl))
        assertFalse("a Push-Up is counted in reps", ProgressionEngine.countsInSteps(pushUp))

        assertEquals("steps", ProgressionEngine.repUnitKeyFor("Heel-to-Toe Walk"))
        assertEquals("steps", ProgressionEngine.repUnitKeyFor("Bear Crawl"))
        assertEquals("reps", ProgressionEngine.repUnitKeyFor("Push-Up"))
        // An exercise outside the pool (a user's own) keeps the ordinary word.
        assertEquals("reps", ProgressionEngine.repUnitKeyFor("My Own Movement"))
    }

    /**
     * 10 -> 100 in tens: ten rungs, and the number on a rung IS the step count - no rep ramp
     * underneath it, because a walk does not warm up from one pace to four.
     */
    @Test
    fun theWalkLadderRunsTenToAHundredInTens() {
        assertEquals(10, ProgressionEngine.repFloorOf(heelToToe))
        assertEquals(100, ProgressionEngine.repCeilingFor(heelToToe))
        assertEquals(10, ProgressionEngine.repStepFor(heelToToe))
        assertEquals(10, ProgressionEngine.repRungs(heelToToe))

        val rungs = (1..ProgressionEngine.repRungs(heelToToe)).map {
            ProgressionEngine.repsAtLevel(heelToToe, it)
        }
        assertEquals((10..100 step 10).toList(), rungs)
    }

    /** A crawl travels shorter passes, so its own ladder is 5 -> 50 in fives. */
    @Test
    fun theCrawlLadderRunsFiveToFiftyInFives() {
        assertEquals(5, ProgressionEngine.repFloorOf(bearCrawl))
        assertEquals(50, ProgressionEngine.repCeilingFor(bearCrawl))
        assertEquals(5, ProgressionEngine.repStepFor(bearCrawl))
        assertEquals(10, ProgressionEngine.repRungs(bearCrawl))
        assertEquals(5, ProgressionEngine.repsAtLevel(bearCrawl, 1))
        assertEquals(50, ProgressionEngine.repsAtLevel(bearCrawl, 10))
    }

    /**
     * ONE maximum per exercise. The step ladders are the only place these two numbers live: a
     * leftover row in [RepCeilings] would be a second answer no code reads, and the next palette
     * edit of the table would silently disagree with the ladder the app actually prescribes.
     */
    @Test
    fun aStepCountedRowCarriesNoSecondCeiling() {
        assertNull("Heel-to-Toe Walk must not also sit in RepCeilings", RepCeilings.forSlug(heelToToe.slug))
        assertNull("Bear Crawl must not also sit in RepCeilings", RepCeilings.forSlug(bearCrawl.slug))
        // ...and the coverage guard still sees them as authored rather than falling through.
        assertTrue(ProgressionEngine.hasAuthoredRepCeiling(heelToToe))
        assertTrue(ProgressionEngine.hasAuthoredRepCeiling(bearCrawl))
    }

    /** The rung's own set targets are step counts too, and never below the floor. */
    @Test
    fun theLevelCarriesItsStepsIntoTheSets() {
        val sets = ProgressionEngine.prescribedReps(heelToToe, 1, 1)!!
        assertEquals("the walk is written in three sets", 3, sets.size)
        assertTrue("every set asks for at least the ladder's floor: $sets", sets.all { it >= 10 })
    }

    /**
     * The complaint that started this, pinned as a number: 20 was below the AVERAGE diamond set
     * (strengthlevel's standards put it at 23), so the top rung is now 40 - still far from the
     * 122-in-a-minute record, because a record is speed work and not one set.
     */
    @Test
    fun diamondPushUpsNoLongerStopAtTwenty() {
        val diamond = ProgressionEngine.entryFor("Diamond Push Ups")!!
        assertEquals(40, ProgressionEngine.repCeilingFor(diamond))
        val top = ProgressionEngine.repsAtLevel(diamond, ProgressionEngine.repRungs(diamond))
        assertEquals(40, top)
    }
}
