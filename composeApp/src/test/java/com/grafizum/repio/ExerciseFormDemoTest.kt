package com.grafizum.repio

import com.grafizum.repio.ui.components.FormPose
import com.grafizum.repio.ui.components.MovementFamily
import com.grafizum.repio.ui.components.formDepth
import com.grafizum.repio.ui.components.formPoseAt
import com.grafizum.repio.ui.components.movementFamilyFor
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE 2D FORM DEMO, PINNED WITHOUT A SCREENSHOT.
 *
 * Owner, 2026-10-14 (the merged "2D workout animation for the app" row): the
 * moving demonstration on the exercise card. The animation is drawn from pure
 * pose math, so the things that can silently go wrong are arithmetic, not pixels:
 * a rep that does not loop, a knee that bends backwards, a pull-up whose bar
 * drifts, an arm that stretches instead of folding, or a figure that walks out of
 * its own frame. Each of those is a case below.
 */
class ExerciseFormDemoTest {

    // ------------------------------------------------------------ the classifier

    @Test
    fun theCatalogueNamesLandOnTheRightMovementFamily() {
        assertEquals(MovementFamily.PUSH_UP, movementFamilyFor("Push-Up"))
        assertEquals(MovementFamily.PUSH_UP, movementFamilyFor("Diamond Push Ups"))
        assertEquals(MovementFamily.PULL_UP, movementFamilyFor("Pull-Up"))
        assertEquals(MovementFamily.PULL_UP, movementFamilyFor("Wide Grip Chin-Up"))
        assertEquals(MovementFamily.PULL_UP, movementFamilyFor("Lat Pulldown"))
        assertEquals(MovementFamily.PLANK, movementFamilyFor("Plank"))
        assertEquals(MovementFamily.SQUAT, movementFamilyFor("Barbell Back Squat"))
        assertEquals(MovementFamily.SQUAT, movementFamilyFor("Leg Press"))
        assertEquals(MovementFamily.LUNGE, movementFamilyFor("Walking Lunge"))
        assertEquals(MovementFamily.HINGE, movementFamilyFor("Dumbbell Romanian Deadlift"))
        assertEquals(MovementFamily.ROW, movementFamilyFor("Seated Cable Row"))
        assertEquals(MovementFamily.CURL, movementFamilyFor("Barbell Curl"))
        assertEquals(MovementFamily.RAISE, movementFamilyFor("Lateral Raise"))
        assertEquals(MovementFamily.PRESS, movementFamilyFor("Barbell Bench Press"))
    }

    @Test
    fun aNameTheClassifierDoesNotKnowStillAnimates() {
        // A card with no figure is worse than a card with the generic cycle.
        assertEquals(MovementFamily.SQUAT, movementFamilyFor("Zercher Carry"))
    }

    // ------------------------------------------------------------ the rep cycle

    @Test
    fun theRepStartsAndEndsAtTheSamePoseSoTheLoopIsSeamless() {
        MovementFamily.entries.forEach { family ->
            val start = formPoseAt(0f, family)
            val end = formPoseAt(1f, family)
            assertSamePose("$family does not close its loop", start, end)
        }
    }

    @Test
    fun theDepthIsZeroAtBothEndsAndFullAtTheMiddle() {
        assertEquals(0f, formDepth(0f), 1e-5f)
        assertEquals(1f, formDepth(0.5f), 1e-5f)
        assertEquals(0f, formDepth(1f), 1e-5f)
    }

    @Test
    fun everyFamilyStaysInsideItsOwnFrameAtEveryPhase() {
        var phase = 0f
        while (phase <= 1.0001f) {
            MovementFamily.entries.forEach { family ->
                val pose = formPoseAt(phase, family)
                allJoints(pose).forEach { (name, joint) ->
                    assertTrue(
                        "$family/$name left the frame at phase $phase: (${joint.x}, ${joint.y})",
                        joint.x in -0.05f..1.05f && joint.y in -0.05f..1.05f
                    )
                }
            }
            phase += 0.05f
        }
    }

    // ------------------------------------------------------------ the movements

    @Test
    fun aSquatSinksTheHipsAndDrivesTheKneeForward() {
        val standing = formPoseAt(0f, MovementFamily.SQUAT)
        val bottom = formPoseAt(0.5f, MovementFamily.SQUAT)
        assertTrue("the hips must drop", bottom.hip.y > standing.hip.y + 0.05f)
        assertTrue(
            "the knee must travel forward of the hip, not backwards",
            bottom.knee.x > standing.knee.x + 0.02f
        )
    }

    @Test
    fun aPullUpRisesToAHandThatDoesNotMove() {
        val hanging = formPoseAt(0f, MovementFamily.PULL_UP)
        val top = formPoseAt(0.5f, MovementFamily.PULL_UP)
        assertEquals("the bar must not drift", hanging.hand.y, top.hand.y, 1e-5f)
        assertTrue("the body must rise to the bar", top.hip.y < hanging.hip.y - 0.04f)
    }

    @Test
    fun aPushUpFoldsTheElbowInsteadOfStretchingTheArm() {
        val top = formPoseAt(0f, MovementFamily.PUSH_UP)
        val bottom = formPoseAt(0.5f, MovementFamily.PUSH_UP)
        val reachTop = hypot(top.hand.x - top.shoulder.x, top.hand.y - top.shoulder.y)
        val reachBottom = hypot(bottom.hand.x - bottom.shoulder.x, bottom.hand.y - bottom.shoulder.y)
        assertTrue("the chest must come down to the hands", reachBottom < reachTop - 0.02f)
    }

    // ------------------------------------------------------------ helpers

    private fun allJoints(pose: FormPose) = listOf(
        "hip" to pose.hip,
        "shoulder" to pose.shoulder,
        "head" to pose.head,
        "knee" to pose.knee,
        "ankle" to pose.ankle,
        "elbow" to pose.elbow,
        "hand" to pose.hand
    )

    private fun assertSamePose(message: String, a: FormPose, b: FormPose) {
        allJoints(a).zip(allJoints(b)).forEach { (first, second) ->
            assertEquals("$message (${first.first}.x)", first.second.x, second.second.x, 1e-4f)
            assertEquals("$message (${first.first}.y)", first.second.y, second.second.y, 1e-4f)
        }
    }
}
