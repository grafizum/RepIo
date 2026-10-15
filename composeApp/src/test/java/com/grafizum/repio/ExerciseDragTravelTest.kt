package com.grafizum.repio

import com.grafizum.repio.ui.screens.exerciseDragTravel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE HELD CARD TRAVELS, and it travels for BOTH reasons a finger can move relative to the list.
 *
 * Owner report, 2026-10-15: *"moving an exercise wih the auto scrolling up and down just places
 * the exercise somewhere along the way even though i didnt lift the finger."*
 *
 * Measured on emulator-5554, long-pressing the first of Monday's nine exercises, walking the
 * finger into the bottom band and then sending no input events at all:
 *
 *     move  idx=4 off=-3.6 reading=1625          the card under the finger, index 4
 *     tick  wanted=62.2 held=548  first=0 -> 3 -> 5   the list glided ~5 rows under a parked finger
 *     tick  wanted=0    held=0    idx=4               the card was still at index 4
 *     end   idx=4                                  so the lift committed it 5 rows adrift
 *
 * The glide scrolled the LIST and nothing scrolled the CARD, because the reorder was driven by
 * `dragOffsetY` - the distance the finger has travelled - and a parked finger travels nowhere.
 * That is the defect these cases hold: the step rule is now one function
 * ([exerciseDragTravel]) fed by both the finger's deltas and the glide's scroll, and the glide's
 * call site is pinned too, since a pure case cannot see whether the loop still calls it.
 *
 * Geometry is the emulator's: rows about 200px, the 16dp gap about 40px, so one step is 240px.
 */
class ExerciseDragTravelTest {

    private val row = 200
    private val spacing = 40f

    /** One step of the rule, with both neighbours measured (`row` px each). */
    private fun step(index: Int, travel: Float, listSize: Int = 9, nextSize: Int? = row, prevSize: Int? = row) =
        exerciseDragTravel(
            index = index,
            travel = travel,
            listSize = listSize,
            nextSize = nextSize,
            prevSize = prevSize,
            spacing = spacing
        )

    @Test
    fun aStepIsTakenPastHalfTheNextRowAndTheRestIsKeptAsTheOffset() {
        // Half of a row + gap is the threshold the drag has always used.
        val justShort = step(index = 2, travel = 119f)
        assertEquals("under half a row the card has not earned the next slot", 2, justShort.index)
        assertEquals("and nothing is consumed", 119f, justShort.offset, 0.001f)

        val past = step(index = 2, travel = 121f)
        assertEquals("past half a row it takes the next slot", 3, past.index)
        assertEquals("and the row it took comes off the offset", -119f, past.offset, 0.001f)

        val up = step(index = 2, travel = -121f)
        assertEquals("and it works the same way upwards", 1, up.index)
        assertEquals(119f, up.offset, 0.001f)
    }

    /**
     * The property the owner could see: however far the list has glided, the card is still within
     * a row of the finger. A step that consumed less than a row would let the card drift away a
     * little per tick — which is exactly how it ended up five rows behind.
     */
    @Test
    fun thePaintedOffsetNeverExceedsOneRowSoTheCardStaysUnderTheFinger() {
        var index = 0
        var offset = 0f
        var travelled = 0f
        // A glide of five rows in the loop's own ticks: one viewport per 25 ticks is ~62px here.
        while (travelled < 5 * (row + spacing)) {
            val next = step(index = index, travel = offset + 62f)
            index = next.index
            offset = next.offset
            travelled += 62f
            assertTrue(
                "the card drifted $offset px from the finger after $travelled px of glide",
                kotlin.math.abs(offset) <= row + spacing
            )
        }
    }

    /**
     * The list's movement and the finger's movement are the same event, so the card must keep up
     * with a glide at the rate it keeps up with a finger: about one slot per row.
     */
    @Test
    fun theCardKeepsUpWithTheGlideAtOneSlotPerRow() {
        fun slotsFor(total: Float, chunk: Float): Int {
            var index = 0
            var offset = 0f
            var done = 0f
            while (done < total) {
                val next = step(index = index, travel = offset + chunk)
                index = next.index
                offset = next.offset
                done += chunk
            }
            return index
        }

        val rowsScrolled = 5
        val scroll = rowsScrolled * (row + spacing)
        val slots = slotsFor(scroll, 62f)
        assertEquals(
            "five rows of glide must walk the card five slots, not leave it where it was",
            rowsScrolled,
            slots
        )
        // And the answer does not depend on how finely the glide is chopped up, which is the
        // property that makes the finger's deltas and the glide's scroll one rule: the same
        // travel in bigger ticks must land the card in the same slot.
        assertEquals("two-tick steps land it in the same place", slots, slotsFor(scroll, 120f))
        assertEquals(
            "and so does one delta the size of the whole glide, which steps ONCE - the rule is " +
                "applied per tick by the loop, not re-derived from scratch",
            1,
            slotsFor(scroll, scroll)
        )
    }

    @Test
    fun theEndsAndAnUnmeasuredNeighbourRefuseTheStepRatherThanGuessing() {
        assertEquals("nothing below the last row to take", 8, step(index = 8, travel = 900f).index)
        assertEquals("nothing above the first", 0, step(index = 0, travel = -900f).index)
        assertEquals(
            "a neighbour that is not composed has no height to halve",
            3,
            step(index = 3, travel = 190f, nextSize = null).index
        )
        assertEquals(
            "and the same upwards",
            3,
            step(index = 3, travel = -190f, prevSize = null).index
        )
        assertEquals("an empty list is inert", 0, step(index = 0, travel = 900f, listSize = 0).index)
        assertEquals("and so is a card that is not in the list", -1, step(index = -1, travel = 900f).index)
    }

    /**
     * The half no pure case can hold: the glide must actually FEED the rule its scroll.
     *
     * The defect was a missing call, not a wrong number — `dispatchRawDelta` moved the list and
     * nothing moved the card — so a test of the rule alone would have stayed green while the
     * owner's bug was present. This reads the source the way `ThemeEntryHandsOffTest` does, for
     * the same reason: no Compose test here can drive the glide loop and watch a row.
     */
    @Test
    fun everyScrollTheGlideAppliesTravelsTheHeldCard() {
        val source = File("src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt").readText()

        assertTrue(
            "the glide scrolls the list but nothing travels the held card, so the card is left " +
                "behind the finger and the lift commits it somewhere along the way",
            Regex("""dispatchRawDelta\(wanted\)[\s\S]{0,600}?travelDraggedItem\(it, wanted\)""")
                .containsMatchIn(source)
        )
        // The threshold math is the RULE's, not the composable's: if a copy lives beside the
        // gesture again, the two can be edited apart and this whole class of bug comes back.
        val rule = source.substringAfter("fun exerciseDragTravel").substringBefore("\n}\n")
        assertTrue("the travel rule is gone from RoutineScreen", rule.contains("val threshold"))
        assertEquals(
            "a reorder threshold has a second home outside the one rule, so the glide and the " +
                "finger can drift apart again",
            0,
            Regex("""val threshold\s*=""").findAll(source.replace(rule, "")).count()
        )
        assertEquals(
            "the card travels from more than one rule",
            1,
            Regex("""fun travelDraggedItem""").findAll(source).count()
        )
    }
}
