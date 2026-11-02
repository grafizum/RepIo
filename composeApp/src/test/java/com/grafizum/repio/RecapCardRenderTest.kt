package com.grafizum.repio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.grafizum.repio.ui.components.WorkoutRecapCard
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.grafizum.repio.ui.viewmodel.RecapExerciseRow
import com.grafizum.repio.ui.viewmodel.WorkoutRecap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The session recap card — Double-Bezel Console layout with the finished
 * label as its header — rendered on the dark ladder.
 *
 * Owner prompt, 2026-10-17 (second correction): *"i want the 'all exercises
 * finished for today' box not be separate and instead be inside the session
 * recap and instead of showing session recap show that boxed label (changed to
 * be shorter) and make it slightly bigger i want it to be better visible"*.
 *
 * This exists because the physical phone disconnected mid-pass, and the thing
 * under review is exactly the kind of change a text dump cannot judge: is the
 * header big enough, boxed, and legible on the real surface rungs? The
 * [GreetingScreenshotTest] precedent is this project's way of keeping a visual
 * record without a device.
 *
 * DARK COMES FROM THE THEME FAMILY, not from the system: `resolveColorScheme`
 * sends "DEFAULT" to Beige in BOTH appearances (the owner's 2026-04 ruling),
 * and the owner's phone runs the Developer scheme — so the render passes
 * `themeOption = "DEVELOPER"`, which is also how the real screen gets its dark
 * surface ladder. The first attempts at forcing night through Robolectric
 * qualifiers and through composition-level configuration overrides came back
 * beige for exactly this reason; they are not the mechanism.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [34])
class RecapCardRenderTest {

    @get:Rule val composeTestRule = createComposeRule()

    /** The real Saturday session from the owner's debug device, 22 sets. */
    private val sampleRecap = WorkoutRecap(
        exercises = 9,
        sets = 22,
        volumeKg = 1056.0,
        durationSeconds = 4440L,
        setsByMuscle = linkedMapOf(
            "Hamstrings" to 6,
            "Glutes" to 3,
            "Spinal Erectors" to 3,
            "Obliques" to 3,
            "Lats" to 2,
            "Quadriceps" to 2,
            "Calves" to 2,
            "Back" to 1,
        ),
        exerciseRows = listOf(
            RecapExerciseRow("Dumbbell Deadlift", 4, listOf(12, 12, 12, 12), listOf(8.0)),
            RecapExerciseRow("Glute Bridge", 3, listOf(12, 12, 12), emptyList()),
            RecapExerciseRow("Pilates Leg Pull Back", 1, listOf(12), emptyList()),
            RecapExerciseRow("Bent-Over Dumbbell Row", 3, listOf(12, 12, 12), listOf(8.0)),
        ),
    )

    @Test
    fun recapCardConsoleHeader() {
        composeTestRule.setContent {
            WorkoutTrackerTheme(themeOption = "DEVELOPER") {
                // The card never sits on pure black: give it the app's canvas.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0E0E0E))
                        .padding(16.dp)
                ) {
                    WorkoutRecapCard(recap = sampleRecap)
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recap_card_console.png")
    }
}
