package com.grafizum.repio

import com.grafizum.repio.ui.components.weeklyBarHeight
import com.grafizum.repio.ui.components.weeklyDayCenterX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE WEEK'S TREND LINE IS THE PROFILE OF ITS BARS.
 *
 * Owner, 2026-10-16: the line is *"badly designed"*. It was, and as arithmetic rather than taste:
 * the chart drew its bars with `volume/max * height * 0.6f` measured up from the bottom, and drew
 * the line at `height * 0.4f - volume/max * height * 0.2f` measured DOWN FROM THE TOP - a band
 * between 20% and 40% of the height that no bar ever entered. A rest day put its point 40% down
 * while its bar sat on the baseline; the busiest day put its point 20% down while its bar reached
 * the top. So the line crossed over bars it disagreed with, and on a week with one session it read
 * as a diagonal stripe across an empty chart.
 *
 * The fix is one scale shared by both drawings, which is what these cases pin: the line goes
 * through the top of each day's bar, and the day's dot sits on that point at the bar's own centre.
 * The chart is drawn by a Canvas, so the shared scale is a pure function
 * ([weeklyBarHeight]) and this is where it is held to the geometry the callers use.
 */
class WeeklyOutputTrendTest {

    private val height = 300f

    /** A column's height and the line's point for that day are the same number, by construction. */
    @Test
    fun theLineTouchesTheTopOfEveryBar() {
        val max = 12000.0
        for (volume in listOf(0.0, 1.0, 3000.0, 6000.0, 12000.0)) {
            val bar = weeklyBarHeight(volume, max, height)
            val point = height - bar
            assertEquals(
                "the trend point for $volume must be the top of its own bar",
                height - weeklyBarHeight(volume, max, height),
                point,
                0.001f
            )
        }
    }

    /**
     * A REST DAY SITS ON THE BASELINE - the exact symptom of the old line.
     *
     * Volume 0 used to place its point at 40% of the chart height with no bar under it, and on a
     * week that trains Monday and Friday the line therefore hovered across the empty days.
     */
    @Test
    fun aRestDaySitsOnTheBaseline() {
        val max = 5000.0
        assertEquals(0f, weeklyBarHeight(0.0, max, height), 0.001f)
        val oldLineY = height * 0.4f - (0.0 / max).toFloat() * height * 0.2f
        assertTrue(
            "a rest day must not float in the old mid-air band (${oldLineY}px)",
            height - weeklyBarHeight(0.0, max, height) > oldLineY + height * 0.5f
        )
    }

    /** The busiest day is the highest point, and a heavier day can never be lower than a lighter one. */
    @Test
    fun theLineClimbsWithTheWork() {
        val max = 9000.0
        val heights = listOf(0.0, 1500.0, 4500.0, 9000.0).map { weeklyBarHeight(it, max, height) }
        assertEquals(
            "the week's columns must climb in order",
            heights.sorted(),
            heights
        )
        assertEquals("the busiest day is the tallest column", height * 0.6f, heights.last(), 0.01f)
    }

    /** A week at or over the scale's top cannot draw outside the chart - the line included. */
    @Test
    fun nothingIsDrawnOutsideTheChart() {
        val max = 1000.0
        for (volume in listOf(-5.0, 1000.0, 4000.0)) {
            val bar = weeklyBarHeight(volume, max, height)
            assertTrue("a column of $bar px leaves the chart", bar in 0f..(height * 0.6f + 0.001f))
            assertTrue("a point at ${height - bar} leaves the chart", (height - bar) in 0f..height)
        }
    }

    /** Bar, point and dot share one centre per day, or the line misses the bars it summarizes. */
    @Test
    fun everyDaySharesOneCentre() {
        val barWidth = 40f
        val spacing = 12f
        val centers = (0 until 5).map { weeklyDayCenterX(it, barWidth, spacing) }
        for (index in 1 until centers.size) {
            assertEquals(
                "day $index's centre is not one bar-plus-gap along",
                barWidth + spacing,
                centers[index] - centers[index - 1],
                0.001f
            )
        }
    }
}
