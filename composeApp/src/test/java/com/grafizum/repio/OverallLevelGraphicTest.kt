package com.grafizum.repio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.grafizum.repio.ui.screens.OverallLevelExample
import com.grafizum.repio.ui.screens.OverallLevelExampleGraphic
import com.grafizum.repio.ui.screens.OverallLevelExplainer
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Renders the Overall Level explainer on a narrow phone and checks it lays out
 * with real copy.
 *
 * This is not a pixel test — it guards the two failures that a graphic like this
 * actually suffers from:
 *
 *  - the app resolves every string through `getString("some_key")` and
 *    `Resources.getIdentifier`, which silently falls back to rendering the raw
 *    key when a resource is missing. A typo would ship a card that literally says
 *    "overall_level_step_measure_title", so the raw keys are asserted absent.
 *  - the illustration is a fixed-width chip row and three label/value bars. If it
 *    outgrows a 320dp card the captions get clipped off, so every step's title is
 *    asserted displayed inside that width.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class OverallLevelGraphicTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** The 320dp-class card the graphic has to survive inside. */
    private fun renderNarrow() {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                Box(modifier = Modifier.width(320.dp).padding(12.dp)) {
                    OverallLevelExampleGraphic()
                }
            }
        }
    }

    @Test
    fun everyStepAndTheIllustrationsCarryRealCopy() {
        renderNarrow()

        // Resolved through the static resource, so a value that drifts between
        // the dynamic bridge and the resources file fails here.
        listOf(
            R.string.overall_level_step_measure_title,
            R.string.overall_level_step_recency_title,
            R.string.overall_level_step_where_title,
            R.string.overall_level_recency_today,
            R.string.overall_level_recency_month,
            R.string.overall_level_recency_two_months,
        ).forEach { resId ->
            composeTestRule
                .onNodeWithText(context.getString(resId))
                .assertIsDisplayed()
        }
    }

    @Test
    fun noRawStringKeyIsEverRendered() {
        renderNarrow()

        // The keys the graphic asks for. If any of them reached the screen the
        // dynamic lookup failed, which no other test would catch.
        listOf(
            "overall_level_step_measure_title",
            "overall_level_step_measure_caption",
            "overall_level_step_recency_title",
            "overall_level_step_recency_caption",
            "overall_level_step_where_title",
            "overall_level_step_where_caption",
            "overall_level_recency_today",
            "overall_level_recency_month",
            "overall_level_recency_two_months",
            "overall_level_weight_multiplier",
            "overall_level_label",
            "overall_level_format",
        ).forEach { key ->
            composeTestRule.onNodeWithText(key, substring = true).assertDoesNotExist()
        }
    }

    @Test
    fun theHomeMiniatureShowsTheFormattedLevel() {
        renderNarrow()

        // "L.4.6" is the whole point of step 3: it has to show the same format the
        // Home stat uses, not a bare number.
        val expected = context.getString(R.string.overall_level_format, OverallLevelExample.average)
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun theWorkedExampleAddsUpAndItsStepsAgree() {
        // Step 1 claims "these five levels average into this number", so the five
        // levels have to be the ones the graphic prints and their mean has to be
        // the number beside them. The average is derived, so this pins the example
        // itself: editing the levels without re-checking the caption fails here.
        assertEquals(listOf(5, 4, 5, 3, 6), OverallLevelExample.levels)
        assertEquals(4.6, OverallLevelExample.average, 1e-9)

        renderNarrow()

        // Step 1's result and step 3's Home card are the same number, printed once
        // bare and once in the app's "L.x" format — they read one shared value, so
        // they can never drift apart.
        composeTestRule.onNodeWithText(OverallLevelExample.average.toString()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.overall_level_format, OverallLevelExample.average))
            .assertIsDisplayed()
    }

    // --- The prose that now sits above the graphic ---------------------------

    private fun renderExplainer(detailed: Boolean) {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                Column(modifier = Modifier.padding(12.dp)) {
                    OverallLevelExplainer(detailed = detailed)
                }
            }
        }
    }

    @Test
    fun theLongExplanationQuotesTheSameExampleAsTheGraphic() {
        renderExplainer(detailed = true)

        // The prose and step 1 have to state the same number: a reader can add the
        // chips up, and if the sentence disagreed with them the illustration would
        // be teaching something false. Both read OverallLevelExample.average.
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.overall_level_gpa_example, OverallLevelExample.average)
            )
            .assertExists()

        // Used with assertExists rather than assertIsDisplayed throughout: prose plus
        // a three-step graphic is taller than the test window, and a node below the
        // fold is still genuinely rendered.
        listOf(
            R.string.overall_level_gpa_lead,
            R.string.overall_level_gpa_step_1,
            R.string.overall_level_gpa_step_2,
            R.string.overall_level_gpa_step_3,
            R.string.overall_level_gpa_outro
        ).forEach { resId ->
            composeTestRule.onNodeWithText(context.getString(resId)).assertExists()
        }
    }

    @Test
    fun theShortFormIsUsedWhereTheLongOneWouldBuryThePictures() {
        renderExplainer(detailed = false)

        composeTestRule
            .onNodeWithText(context.getString(R.string.overall_level_short))
            .assertExists()
        // The walkthrough is already a wall of graphics; the five-line version is
        // Settings-only, and this is what keeps it there.
        composeTestRule
            .onNodeWithText(context.getString(R.string.overall_level_gpa_lead))
            .assertDoesNotExist()
    }

    @Test
    fun theProseRendersNoRawStringKey() {
        renderExplainer(detailed = true)

        listOf(
            "overall_level_short",
            "overall_level_gpa_lead",
            "overall_level_gpa_step_1",
            "overall_level_gpa_step_2",
            "overall_level_gpa_step_3",
            "overall_level_gpa_example",
            "overall_level_gpa_outro"
        ).forEach { key ->
            composeTestRule.onNodeWithText(key, substring = true).assertDoesNotExist()
        }
    }
}
