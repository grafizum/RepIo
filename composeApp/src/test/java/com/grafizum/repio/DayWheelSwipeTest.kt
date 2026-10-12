package com.grafizum.repio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.grafizum.repio.ui.components.WorkoutDayWheel
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * THE DAY WHEEL'S SWIPE READS THE LIVE DAY, NOT THE DAY IT STARTED ON.
 *
 * The wheel installs its drag detector ONCE (`pointerInput(Unit)`) so that changing the day
 * does not tear down and re-install the gesture that changed it - the micro-lag reported as
 * "lagging slightly when changing training days". A handler that never restarts also never
 * refreshes its captures, so [selectedDay] reaches it through `rememberUpdatedState`: without
 * that, two swipes right would BOTH compute from the day the wheel first composed with, and
 * the user would sit on Tuesday after asking twice for the day after Monday.
 *
 * This is the regression test for that exact trap, so it swipes TWICE without re-setting
 * content between the gestures. A version of the wheel with a plain captured `selectedDay`
 * fails the second assertion.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class DayWheelSwipeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** A wheel tracking its own day, the way both screens drive it: state in, callback out. */
    private fun renderWheel(initialDay: String, onDay: (String) -> Unit) {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                // HOISTED, as the real screens do (the day lives in WorkoutViewModel's state
                // flow, not in a local). A plain `var day = initialDay` here would be recreated
                // on every recomposition and would never reach the wheel as a new value - the
                // wheel would sit on the initial day forever and every swipe would answer the
                // same thing, which is a bug in the harness rather than in the wheel.
                val day = remember { mutableStateOf(initialDay) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    WorkoutDayWheel(
                        selectedDay = day.value,
                        onDaySelected = { next ->
                            day.value = next
                            onDay(next)
                        },
                        today = "MONDAY",
                    )
                }
            }
        }
    }

    /** One swipe right advances exactly one day - the gesture still works at all. */
    @Test
    fun oneSwipeRightMovesOneDayForward() {
        val seen = mutableListOf<String>()
        renderWheel("MONDAY", onDay = { seen.add(it) })

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertEquals("a single swipe right did not move one day forward: $seen", listOf("TUESDAY"), seen)
    }

    /**
     * THE POINT OF THE SUITE: the second swipe must be computed from the day the FIRST swipe
     * selected. With a stale capture both swipes answer "TUESDAY", and the user cannot walk
     * the wheel forward at all.
     */
    @Test
    fun consecutiveSwipesKeepAdvancingFromTheLatestDay() {
        val seen = mutableListOf<String>()
        renderWheel("MONDAY", onDay = { seen.add(it) })

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertEquals(
            "consecutive swipes did not keep advancing - the handler is reading a stale day: $seen",
            listOf("TUESDAY", "WEDNESDAY", "THURSDAY"),
            seen,
        )
    }

    /** And backwards the same way, including the wrap at the start of the week. */
    @Test
    fun consecutiveSwipesLeftWalkBackwardsAndWrap() {
        val seen = mutableListOf<String>()
        renderWheel("TUESDAY", onDay = { seen.add(it) })

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(
            "backwards swipes did not walk the wheel: $seen",
            listOf("MONDAY", "SUNDAY"),
            seen,
        )
    }
}
