package com.grafizum.repio

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.grafizum.repio.ui.screens.LevelGuideOnboardingScreen
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The one-time walkthrough that greets a user with both explainers at once.
 *
 * It has to teach two systems in one sitting and still be easy to leave, so this
 * covers the two things that make it a welcome rather than a nag: both subjects
 * are actually present, and it can be dismissed from the top corner without
 * scrolling to the bottom button.
 *
 * The graphic content asserts `assertExists` rather than `assertIsDisplayed`,
 * because the page is a scrolling column — everything below the fold is composed
 * but legitimately off-screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class GuideOnboardingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun render(
        onDismiss: () -> Unit = {},
        appLanguage: String = "system",
        onLanguageSelected: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                LevelGuideOnboardingScreen(
                    onDismiss = onDismiss,
                    appLanguage = appLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }
        }
    }

    @Test
    fun bothExplainersAreOnTheWalkthrough() {
        render()

        // The headline and the level-system section are at the top of the page.
        composeTestRule
            .onNodeWithText(context.getString(R.string.level_guide_onboarding_title))
            .assertIsDisplayed()

        // Both section headings, and a card from each graphic. Existence, not
        // visibility: the page scrolls, so the lower half is composed off-screen.
        composeTestRule
            .onNodeWithText(context.getString(R.string.level_system_guide_title))
            .assertExists()
        composeTestRule
            .onNodeWithText(context.getString(R.string.overall_level_how_title))
            .assertExists()

        // One real card from each system, proving both graphics rendered and not
        // just their headings.
        composeTestRule
            .onNodeWithText(context.getString(R.string.level_example_title))
            .assertExists()
        composeTestRule
            .onNodeWithText(context.getString(R.string.overall_level_step_measure_title))
            .assertExists()
    }

    @Test
    fun theCornerControlShowsTheCurrentLanguageCompactly() {
        render(appLanguage = "de")

        // Findable by what it does, and labelled with the language's own two
        // letters rather than a full name the corner cannot afford.
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.change_app_language))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("DE").assertIsDisplayed()
    }

    @Test
    fun followingTheDeviceReadsAsAutoRatherThanAnIsoCode() {
        // "system" has no code to show; it must not render as the raw word "system".
        render(appLanguage = "system")

        composeTestRule
            .onNodeWithText(context.getString(R.string.language_auto_short))
            .assertIsDisplayed()
    }

    @Test
    fun pickingALanguageReportsThePreferenceValue() {
        val picked = mutableListOf<String>()
        render(appLanguage = "en", onLanguageSelected = { picked += it })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.change_app_language))
            .performClick()

        // The menu carries the full localized names, and reports the same
        // preference value Settings uses ("de"), not the label it displayed.
        composeTestRule
            .onNodeWithText(context.getString(R.string.german))
            .performClick()

        assertEquals(listOf("de"), picked)
    }

    @Test
    fun itCanBePressedAwayFromTheTopWithoutScrolling() {
        var dismissals = 0
        render(onDismiss = { dismissals++ })

        // The close affordance sits above the fold, so leaving never requires
        // scrolling through the whole walkthrough first.
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.close))
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, dismissals)
    }
}
