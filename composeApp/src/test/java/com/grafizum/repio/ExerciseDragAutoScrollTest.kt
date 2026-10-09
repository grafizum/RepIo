package com.grafizum.repio

import com.grafizum.repio.ui.screens.DragBandHold
import com.grafizum.repio.ui.screens.exerciseDragAutoScrollDelta
import com.grafizum.repio.ui.screens.exerciseDragAutoScrollEngaged
import com.grafizum.repio.ui.screens.exerciseDragBandHold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Edge auto-scroll in the edit-day exercise list.
 *
 * Owner report, 2026-10-08: *"moving an exercise doesnt scroll up or down when grabbing the
 * exercise and going to a border so that the user goes up the ladder or down in case there is
 * too many exercises"*.
 *
 * The shipped version compared `dragOffsetY` — the distance travelled since the card was
 * grabbed — against the scroll margin. So the trigger was not the finger's position at all:
 *
 *  - a 60px pull on the FIRST card (whose finger sits in the middle of the screen, nowhere
 *    near a border) started scrolling the list, and
 *  - a 20px pull on the LAST visible card, whose finger really is sitting on the bottom
 *    border, did nothing, because 20px of travel never crosses a 56px margin.
 *
 * That pair is the whole bug, so both directions are pinned here in terms of the FINGER'S
 * POSITION rather than in terms of travel: the same drag distance must produce opposite
 * answers depending on where the finger is on screen. A test written against `dragOffsetY`
 * cannot express that, which is why the decision was extracted from the composable.
 *
 * All numbers are the real viewport geometry of a 411x891dp phone: viewport 0..2400px and a
 * 72dp margin (198px at 2.75x density), and the cap is one viewport per 25 ticks (96px).
 *
 * SECOND OWNER REPORT, 2026-10-09: *"the auto scroll up and down is too sensible you need to
 * make it more natural and less sensible"*. The rule above was right about WHERE and wrong
 * about HOW MUCH: it was linear in depth (a tenth of the way in was a tenth of top speed) and
 * it answered the instant the finger crossed the band's inner edge, with a top speed of six
 * viewports a second. So this class now also pins the two properties that make it feel like a
 * bed the finger settles into instead of a switch it trips — the squared entry, and the dwell
 * a hold has to serve before anything moves at all.
 */
class ExerciseDragAutoScrollTest {

    private val viewportTop = 0f
    private val viewportBottom = 2400f
    private val margin = 198f
    /** One viewport per 25 ticks, the caller's cap. */
    private val maxSpeed = 2400f / 25f

    private fun delta(
        fingerY: Float?,
        canScrollBackward: Boolean = true,
        canScrollForward: Boolean = true
    ) = exerciseDragAutoScrollDelta(
        fingerYInViewport = fingerY,
        viewportTop = viewportTop,
        viewportBottom = viewportBottom,
        margin = margin,
        maxSpeed = maxSpeed,
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward
    )

    @Test
    fun aFingerInsideTheListAsksForNoScroll() {
        // Middle of the screen: the common case, and the one where a scroll would be a bug
        // rather than a help. These are the positions the OLD rule scrolled from.
        for (fingerY in listOf(199f, 300f, 1200f, 2200f)) {
            assertEquals("finger at $fingerY is inside the list", 0f, delta(fingerY), 0.001f)
        }
    }

    @Test
    fun aFingerAtTheTopBorderScrollsUpAndOneAtTheBottomScrollsDown() {
        assertTrue("a finger above the top margin must pull the list down", delta(50f) < 0f)
        assertTrue("a finger below the bottom margin must pull the list up", delta(2350f) > 0f)
    }

    @Test
    fun theSameDragDistanceAnswersDifferentlyByFingerPosition() {
        // 20px of travel. Grabbed in the middle of the list -> no scroll; held at the bottom
        // border -> scrolls. This is the assertion the old implementation fails in one
        // direction and passes wrongly in the other, and it is the reason the rule is about
        // position, not distance.
        assertEquals("20px in the middle of the list is not a border hold", 0f, delta(1800f), 0.001f)
        assertTrue("20px from the bottom edge is a border hold", delta(2390f) > 0f)
    }

    @Test
    fun theSpeedRampsWithDepthIntoTheMarginAndIsCapped() {
        // Just inside the band: slow. Deeper: faster. Off the edge: capped, never flinging.
        val just = delta(190f)
        val deeper = delta(100f)
        val offTheEdge = delta(-500f)
        assertTrue("just inside the band must move less than deeper in", just > deeper)
        assertTrue("deeper in must move less than the cap", deeper > offTheEdge)
        assertEquals("the cap is a tenth of the viewport", -maxSpeed, offTheEdge, 0.001f)
        assertTrue("and never beyond it", offTheEdge >= -maxSpeed)
    }

    @Test
    fun aBorderWithNothingBehindItAsksForNoScroll() {
        // At the very top of the list there is nothing above, so asking for a scroll there
        // would fight the viewport every frame; the loop then sees a raw delta of zero and
        // the notice is that it should stop asking.
        assertEquals(0f, delta(10f, canScrollBackward = false), 0.001f)
        assertEquals(0f, delta(2399f, canScrollForward = false), 0.001f)
        // The opposite border still answers, so a single blocked end does not disable both.
        assertTrue(delta(10f, canScrollForward = false) < 0f)
        assertTrue(delta(2399f, canScrollBackward = false) > 0f)
    }

    /**
     * Entering the band is no longer almost the same as parking in it.
     *
     * The old linear rule gave a tenth of the way in a tenth of the top speed — 24px per tick on
     * this geometry, most of the way to a fling the moment a finger came near a border. Squared,
     * the same entry is 1% of the speed: the difference between the two is this test.
     */
    @Test
    fun enteringTheBandIsAlmostNothingAndOnlyAParkReachesTheCap() {
        // 10% into the band: 20px past the inner edge.
        val shallow = kotlin.math.abs(delta(viewportTop + margin - margin / 10f))
        // Halfway in: 99px past the inner edge.
        val half = kotlin.math.abs(delta(viewportTop + margin - margin / 2f))
        // Parked exactly on the border, and then out past it.
        val atTheBorder = kotlin.math.abs(delta(viewportTop))
        val offTheEdge = kotlin.math.abs(delta(viewportTop - 400f))

        // 0.01 x 96px: the old linear rule asked for 24px at this exact position, and 24px of a
        // 2400px viewport every 16ms is most of the fling the owner was complaining about.
        assertEquals("10% into the band is 1% of the speed", maxSpeed * 0.01f, shallow, 0.01f)
        assertEquals("halfway into the band is a quarter of it", maxSpeed * 0.25f, half, 0.01f)
        assertEquals("the border itself is the cap", maxSpeed, atTheBorder, 0.01f)
        assertEquals(
            "past the border the speed saturates rather than accelerating into a fling",
            atTheBorder, offTheEdge, 0.01f
        )
        // The cap is still a real glide — 96px every 16ms is about two and a half viewports a
        // second — so parking at the border crosses the day quickly instead of creeping.
        assertEquals("the cap is one viewport per 25 ticks", 2400f / 25f, atTheBorder, 0.01f)
        assertTrue("and it must not be slower than a usable glide", atTheBorder > 60f)
    }

    /**
     * The dwell: no glide until the finger has rested in the band.
     *
     * This is the half of "less sensitive" that a position-only rule cannot express — the same
     * finger position is a pass-through at 100ms and a hold at 250ms, and only the second may
     * move the list.
     */
    @Test
    fun nothingGlidesUntilTheFingerHasRestedInTheBand() {
        assertFalse("a finger that has just arrived has not earned a glide", exerciseDragAutoScrollEngaged(0L))
        assertFalse("nor at 100ms, which is a pass-through", exerciseDragAutoScrollEngaged(100L))
        assertFalse("nor one millisecond short of the dwell", exerciseDragAutoScrollEngaged(249L))
        assertTrue("the moment the dwell is served it starts", exerciseDragAutoScrollEngaged(250L))
        assertTrue("and it keeps going while the finger stays there", exerciseDragAutoScrollEngaged(3000L))
    }

    /**
     * The dwell is per HOLD, not per drag: crossing from one band to the other starts it over.
     *
     * Without this rule a finger that had spent a quarter second in the top band would begin
     * gliding the instant it reached the bottom band — the same twitch the dwell exists to remove,
     * one band over. The clearing rule is the same idea in the other direction: back inside the
     * list, the next hold earns its own quarter second instead of inheriting a spent one.
     */
    @Test
    fun theDwellRestartsOnANewBandAndIsClearedWhenTheFingerLeaves() {
        // Parked in the top band from t=1000.
        val top = exerciseDragBandHold(direction = -1, nowMs = 1000L, previous = DragBandHold(0, 0L))
        assertEquals("the hold starts when the finger arrives", 1000L, top.sinceMs)
        assertEquals("and it is still running a second later", 1000L, top.heldMs(2000L))

        // The same band on the next tick does not restart the clock: that clock IS the dwell.
        val stillTop = exerciseDragBandHold(direction = -1, nowMs = 1016L, previous = top)
        assertEquals("a continuing hold keeps its original timestamp", top, stillTop)
        assertEquals("so it can be served", 250L, stillTop.heldMs(1250L))

        // Crossing to the bottom band at t=2000 starts a NEW hold: no glide before 2250.
        val bottom = exerciseDragBandHold(direction = 1, nowMs = 2000L, previous = stillTop)
        assertEquals("the other band is a new hold", 2000L, bottom.sinceMs)
        assertEquals("which has not been served yet", 0L, bottom.heldMs(2000L))
        assertFalse(
            "and must not inherit the top band's quarter second",
            exerciseDragAutoScrollEngaged(bottom.heldMs(2000L))
        )
        assertTrue("but it is served a quarter second later", exerciseDragAutoScrollEngaged(bottom.heldMs(2250L)))

        // Back inside the list: the hold is gone rather than paused.
        val inside = exerciseDragBandHold(direction = 0, nowMs = 3000L, previous = bottom)
        assertEquals("leaving the band clears the hold", DragBandHold(0, 0L), inside)
        assertEquals("a cleared hold has served nothing", 0L, inside.heldMs(9999L))
    }

    @Test
    fun anUnknownFingerPositionAsksForNoScroll() {
        // The dragged row can be missing from the visible list on the frame the list is
        // still composing it. Guessing a position there would scroll on a frame nobody
        // asked about.
        assertEquals(0f, delta(null), 0.001f)
    }

    @Test
    fun aDegenerateViewportOrMarginIsInertRatherThanInverted() {
        // A negative margin would put the band OUTSIDE the viewport and turn every position
        // into a scroll; a zero cap would ask for an un-throttled one. Both must be inert.
        assertEquals(
            0f,
            exerciseDragAutoScrollDelta(
                fingerYInViewport = 0f,
                viewportTop = viewportTop,
                viewportBottom = viewportBottom,
                margin = -10f,
                maxSpeed = maxSpeed,
                canScrollBackward = true,
                canScrollForward = true
            ),
            0.001f
        )
        assertEquals(
            0f,
            exerciseDragAutoScrollDelta(
                fingerYInViewport = 0f,
                viewportTop = viewportTop,
                viewportBottom = viewportBottom,
                margin = margin,
                maxSpeed = 0f,
                canScrollBackward = true,
                canScrollForward = true
            ),
            0.001f
        )
    }
}
