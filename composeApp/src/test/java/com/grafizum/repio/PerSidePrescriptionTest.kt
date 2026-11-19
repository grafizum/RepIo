package com.grafizum.repio

import androidx.compose.ui.test.junit4.createComposeRule
import com.grafizum.repio.data.ExerciseCatalog
import java.io.File
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.exerciseSetsSummaryLabel
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * WHOSE REPS THE PRESCRIPTION IS.
 *
 * Owner, 2026-10-13: *"When giving one arm exercises define that its 8 reps on
 * each arm (care that exercises like bicycle crunch also have 'on each side'
 * type of exercise so the same applies to those too)"*.
 *
 * The ladders already count PER SIDE (a one-arm row's 8 means 8 per arm), so
 * this is a display fact: the surfaces that show a prescription must say whose
 * reps they are, or "8-12 reps" on a Single-Arm Row reads as 8-12 total, which
 * half-trains whoever believes it.
 *
 * Two things are pinned here, and the second one is the regression: the NAME
 * classifier (which exercises claim a limb, and which must stay silent), and
 * the summary line actually wearing the suffix - a label no surface renders
 * would pass every data assertion while the user still reads the old lie.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerSidePrescriptionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ---------------------------------------------------------------- the classifier

    @Test
    fun oneArmRowsNameTheArm() {
        for (name in listOf(
            "Single-Arm Chest-Supported Dumbbell Row",
            "One Arm Kettlebell Shoulder Press",
            "Single Arm Machine Shoulder Press",
        )) {
            assertEquals(
                "$name must say its reps are per arm",
                "per_side_arm",
                WorkoutTemplates.perSideLabelKey(name),
            )
        }
    }

    @Test
    fun oneLegRowsNameTheLeg() {
        for (name in listOf(
            "Single Leg Extension",
            "Single Leg Lying Leg Curl",
            "Single Leg Press",
        )) {
            assertEquals(
                "$name must say its reps are per leg",
                "per_side_leg",
                WorkoutTemplates.perSideLabelKey(name),
            )
        }
    }

    @Test
    fun alternatingOneSideWorkSaysEachSide() {
        // The owner named the bicycle crunch specifically; the side plank and
        // mountain climber are the same family (one side at a time, both sides
        // inside one set).
        for (name in listOf("Bicycle Crunch", "Side Plank", "Mountain Climber")) {
            assertEquals(
                "$name must say its reps are per side",
                "per_side_side",
                WorkoutTemplates.perSideLabelKey(name),
            )
        }
    }

    @Test
    fun bilateralWorkStaysSilent() {
        // A suffix on a barbell press would be a different lie from the one it
        // fixes, so the classifier must answer null for both-sided movements.
        for (name in listOf(
            "Barbell Bench Press",
            "Push-Up",
            "Plank",
            "Barbell Back Squat",
            "Dumbbell Curl",
        )) {
            assertNull(
                "$name works both sides together and must carry no per-side note",
                WorkoutTemplates.perSideLabelKey(name),
            )
        }
    }

    @Test
    fun theClassifierNamesExercisesTheCatalogueActuallyHolds() {
        // A rename would silently drop the note, so every keyed example must
        // still exist in the built-in pool.
        val pool = ExerciseCatalog.NAMES.map { it.lowercase() }.toSet()
        for (name in listOf(
            "Bicycle Crunch", "Side Plank", "Single Leg Extension",
            "One Arm Kettlebell Shoulder Press", "Single-Arm Chest-Supported Dumbbell Row",
        )) {
            assertTrue("$name is keyed by the classifier but is not in the pool", name.lowercase() in pool)
        }
    }

    // ---------------------------------------------------------------- the surface

    /**
     * THE BANNER WRITES THE UNIT AND THE NOTE AS ONE LABEL.
     *
     * Owner, 2026-10-31 (dashboard task_6a5w1h175j1q0l0j1t18352w): *"The '12 reps each arm' is
     * not the same alignment / size / label style."* It was two Text runs: `" reps"` at
     * titleMedium and `" each arm"` one type step below at labelMedium, so the phrase arrived in
     * three sizes and the qualifier sat on its own baseline. They are one Text now, which makes
     * the sizes and the baseline agree by construction - this pins that shape, because the split
     * is invisible to every data assertion above (the words are the same either way).
     */
    @Test
    fun theBannerWritesTheUnitAndThePerSideNoteAsOneLabel() {
        val code = File("src/main/java/com/grafizum/repio/ui/screens/WorkoutNowScreen.kt")
            .readText()
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
            .replace(Regex("//[^\\n]*"), "")
            .replace(Regex("\\s+"), "")
        assertTrue(
            "the banner must append the per-side words to the unit label itself",
            code.contains("getString(ProgressionEngine.repUnitKeyFor(exerciseName))+perSide")
        )
        assertFalse(
            "the per-side note is back on its own Text, which is the size/baseline mismatch",
            code.contains("text=\" \"+getString(key)")
        )
    }

    @Test
    fun theSummaryLineWearsThePerSideNote() {
        var withArm = ""
        var bilateral = ""
        composeTestRule.setContent {
            WorkoutTrackerTheme {
                withArm = exerciseSetsSummaryLabel(
                    exerciseName = "Single-Arm Chest-Supported Dumbbell Row",
                    targetSets = 3,
                    storedTarget = "8-12",
                )
                bilateral = exerciseSetsSummaryLabel(
                    exerciseName = "Barbell Bench Press",
                    targetSets = 3,
                    storedTarget = "8-12",
                )
            }
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "the summary line for a one-arm row does not say whose reps they are: \"$withArm\"",
            withArm.contains("each arm"),
        )
        assertTrue(
            "the bilateral summary line grew a per-side note it must not have: \"$bilateral\"",
            !bilateral.contains("each arm") && !bilateral.contains("each side") && !bilateral.contains("each leg"),
        )
    }
}
