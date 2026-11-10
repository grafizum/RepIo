package com.grafizum.repio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.grafizum.repio.util.getString
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * THE 2D FORM DEMO.
 *
 * Owner, 2026-10-14: *"merge all 5 planned as 1 being (2d workout animation for
 * the app)"* — the merged row asked for a moving demonstration of the movement on
 * the exercise card. The render rig that was bought for it (SDXL + ControlNet
 * OpenPose, the 32-pattern squat render) produces STILLS on an external GPU box
 * and failed twice on the video path, and the owner never picked between stills
 * and a real clip.
 *
 * So this is the version that ships without either: a side-view figure drawn in
 * the app, animated through one rep. It needs no asset, no render step and no
 * network, it works for every one of the catalogue's 600+ rows through the name
 * classifier below, and it inherits the theme accent for free.
 *
 * The pose is PURE MATH ([formPoseAt]) — two authored keyframes per movement
 * family, interpolated by a rep depth, with both limbs solved by two-bone IK so
 * the knee bulges forward on a squat and the elbow folds on a pull-up instead of
 * the figure looking like a jointed ruler. That is what makes it testable: the
 * guard suite asserts the loop closes, the knee travels forward, the pull-up body
 * rises to a fixed bar and the push-up elbows fold, without a screenshot.
 */

/** One joint of the demo figure, in a 0..1 box: x to the right, y downward. */
data class FormJoint(val x: Float, val y: Float)

/** A whole side-view skeleton at one instant of a rep. */
data class FormPose(
    val hip: FormJoint,
    val shoulder: FormJoint,
    val head: FormJoint,
    val knee: FormJoint,
    val ankle: FormJoint,
    val elbow: FormJoint,
    val hand: FormJoint
)

/** The shape a movement has, which is all the demo needs to draw it. */
enum class MovementFamily { SQUAT, HINGE, LUNGE, PRESS, ROW, CURL, RAISE, PUSH_UP, PULL_UP, PLANK }

/** One end of a rep: where the hip, the hand and the foot sit, and how far the torso leans. */
private data class FormKeyframe(
    val hipX: Float,
    val hipY: Float,
    val handX: Float,
    val handY: Float,
    val ankleX: Float,
    val ankleY: Float,
    val leanDeg: Float
)

// Limb lengths in the same 0..1 box as the joints. They are what the IK solves
// against, so they are proportions of the figure rather than pixels: the drawing
// scales them with the square it is given.
private const val TORSO = 0.17f
private const val THIGH = 0.15f
private const val SHIN = 0.15f
private const val UPPER_ARM = 0.10f
private const val FOREARM = 0.10f
private const val HEAD_OFFSET = 0.075f

/** One rep of the demo, in milliseconds. */
const val FORM_DEMO_CYCLE_MS = 2000

/**
 * WHICH MOVEMENT THIS IS, from its name alone.
 *
 * Order matters and is the whole classifier: "push" is checked after the push-up
 * and the press so a Push-Up is not a standing press, "leg press" is caught by
 * the squat branch, and a pulldown is a pull-up because the body travels against
 * a fixed hand in both. Anything unrecognised still animates — as a squat cycle,
 * the movement the pool is built around — because a card that renders nothing is
 * worse than a card that renders the generic case.
 */
fun movementFamilyFor(exerciseName: String): MovementFamily {
    val name = exerciseName.lowercase()
    fun has(vararg keys: String) = keys.any { name.contains(it) }
    return when {
        has("push-up", "push up", "pushup", "press-up") -> MovementFamily.PUSH_UP
        has("pull-up", "pull up", "pullup", "chin-up", "chin up", "pulldown", "pull-down", "pulldowns") ->
            MovementFamily.PULL_UP
        has("plank", "wall sit", "dead hang", "hold") -> MovementFamily.PLANK
        has("lunge", "split squat", "step-up", "step up") -> MovementFamily.LUNGE
        has("squat", "leg press", "hack squat") -> MovementFamily.SQUAT
        has("deadlift", "romanian", "good morning", "hip thrust", "bridge", "swing") ->
            MovementFamily.HINGE
        has("row") -> MovementFamily.ROW
        has("curl", "pushdown", "skull", "extension") -> MovementFamily.CURL
        has("raise", "fly", "flye", "lateral", "shrug", "pullover") -> MovementFamily.RAISE
        has("press", "push", "dip", "bench") -> MovementFamily.PRESS
        else -> MovementFamily.SQUAT
    }
}

/**
 * How deep into the rep the figure is at [phase] (0 = the start of the rep, 1 =
 * the start of the next one).
 *
 * A half sine rather than a linear ramp, so the turn at each end is soft and the
 * loop closes exactly: the figure cannot snap from the bottom of a squat back to
 * standing. At phase 1 the depth is 0 again, which is what makes the cycle seamless.
 */
fun formDepth(phase: Float): Float = sin(PI * phase.coerceIn(0f, 1f)).toFloat()

/** Frame data for one movement: [0] is the start of the rep, [1] its far end. */
private val FAMILY_KEYFRAMES: Map<MovementFamily, Pair<FormKeyframe, FormKeyframe>> = mapOf(
    // Feet planted, hips sink between them, hands travel with the chest.
    MovementFamily.SQUAT to (
        FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.60f, handY = 0.44f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 4f) to
            FormKeyframe(hipX = 0.42f, hipY = 0.66f, handX = 0.62f, handY = 0.56f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 18f)
        ),
    // Hips travel back, the torso folds forward, the hands run down the shin.
    MovementFamily.HINGE to (
        FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.48f, handY = 0.56f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 8f) to
            FormKeyframe(hipX = 0.41f, hipY = 0.55f, handX = 0.55f, handY = 0.70f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 62f)
        ),
    // The front foot steps out and the hips drop over it.
    MovementFamily.LUNGE to (
        FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.45f, handY = 0.62f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 10f) to
            FormKeyframe(hipX = 0.43f, hipY = 0.64f, handX = 0.45f, handY = 0.64f, ankleX = 0.66f, ankleY = 0.84f, leanDeg = 12f)
        ),
    // Racked at the shoulders, pressed straight overhead.
    MovementFamily.PRESS to (
        FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.48f, handY = 0.40f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 2f) to
            FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.48f, handY = 0.15f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 2f)
        ),
    // Torso held bent over, the hand pulls up to the ribs.
    MovementFamily.ROW to (
        FormKeyframe(hipX = 0.44f, hipY = 0.54f, handX = 0.52f, handY = 0.60f, ankleX = 0.44f, ankleY = 0.84f, leanDeg = 45f) to
            FormKeyframe(hipX = 0.44f, hipY = 0.54f, handX = 0.52f, handY = 0.46f, ankleX = 0.44f, ankleY = 0.84f, leanDeg = 45f)
        ),
    // Standing tall, the forearm folds up.
    MovementFamily.CURL to (
        FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.53f, handY = 0.56f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 0f) to
            FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.53f, handY = 0.36f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 0f)
        ),
    // The hand sweeps forward and up.
    MovementFamily.RAISE to (
        FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.49f, handY = 0.58f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 0f) to
            FormKeyframe(hipX = 0.45f, hipY = 0.54f, handX = 0.66f, handY = 0.40f, ankleX = 0.45f, ankleY = 0.84f, leanDeg = 0f)
        ),
    // Prone: hands planted, the whole body lowers and the elbows fold. The feet
    // stay on the same line as the hips drop, so the legs stay straight.
    MovementFamily.PUSH_UP to (
        FormKeyframe(hipX = 0.40f, hipY = 0.52f, handX = 0.60f, handY = 0.68f, ankleX = 0.18f, ankleY = 0.82f, leanDeg = 72f) to
            FormKeyframe(hipX = 0.40f, hipY = 0.62f, handX = 0.60f, handY = 0.68f, ankleX = 0.18f, ankleY = 0.92f, leanDeg = 72f)
        ),
    // Hanging: the hand is the fixed bar and the body rises to it.
    MovementFamily.PULL_UP to (
        FormKeyframe(hipX = 0.45f, hipY = 0.53f, handX = 0.45f, handY = 0.16f, ankleX = 0.45f, ankleY = 0.83f, leanDeg = 0f) to
            FormKeyframe(hipX = 0.45f, hipY = 0.42f, handX = 0.45f, handY = 0.16f, ankleX = 0.45f, ankleY = 0.72f, leanDeg = 0f)
        ),
    // A hold: barely anything moves, which is the point of a plank.
    MovementFamily.PLANK to (
        FormKeyframe(hipX = 0.40f, hipY = 0.52f, handX = 0.60f, handY = 0.68f, ankleX = 0.18f, ankleY = 0.82f, leanDeg = 72f) to
            FormKeyframe(hipX = 0.40f, hipY = 0.545f, handX = 0.60f, handY = 0.68f, ankleX = 0.18f, ankleY = 0.845f, leanDeg = 72f)
        )
)

/**
 * The skeleton at [phase] of a [family]'s rep.
 *
 * Authored keyframes first (hip, hand and foot targets plus the torso lean),
 * then the shoulder is placed down the torso, then the knee and the elbow are
 * SOLVED against their targets rather than authored, so the limb lengths are
 * respected and a bend appears exactly where a real one would.
 */
fun formPoseAt(phase: Float, family: MovementFamily): FormPose {
    val (start, end) = FAMILY_KEYFRAMES.getValue(family)
    val depth = formDepth(phase).coerceIn(0f, 1f)
    fun mix(a: Float, b: Float) = a + (b - a) * depth

    val hip = FormJoint(mix(start.hipX, end.hipX), mix(start.hipY, end.hipY))
    val hand = FormJoint(mix(start.handX, end.handX), mix(start.handY, end.handY))
    val ankle = FormJoint(mix(start.ankleX, end.ankleX), mix(start.ankleY, end.ankleY))

    val leanRad = Math.toRadians(mix(start.leanDeg, end.leanDeg).toDouble()).toFloat()
    val shoulder = FormJoint(
        hip.x + TORSO * sin(leanRad),
        hip.y - TORSO * cos(leanRad)
    )
    val head = FormJoint(
        shoulder.x + HEAD_OFFSET * sin(leanRad),
        shoulder.y - HEAD_OFFSET * cos(leanRad)
    )

    return FormPose(
        hip = hip,
        shoulder = shoulder,
        head = head,
        knee = solveTwoBone(hip, ankle, THIGH, SHIN, bendForward = true),
        ankle = ankle,
        elbow = solveTwoBone(shoulder, hand, UPPER_ARM, FOREARM, bendForward = false),
        hand = hand
    )
}

/**
 * Where the middle joint of a two-segment limb sits, given both ends.
 *
 * Law of cosines on the triangle root-tip-middle: `along` is how far up the
 * root→tip line the joint drops, `rise` how far it bulges off it. The distance is
 * CLAMPED to what the two segments can span, so an authored target slightly out of
 * reach straightens the limb instead of detaching it.
 */
private fun solveTwoBone(
    root: FormJoint,
    tip: FormJoint,
    first: Float,
    second: Float,
    bendForward: Boolean
): FormJoint {
    val dx = tip.x - root.x
    val dy = tip.y - root.y
    val raw = hypot(dx, dy).coerceAtLeast(1e-4f)
    val reach = (first + second - 1e-3f)
    val span = raw.coerceIn(abs(first - second) + 1e-3f, reach)
    val along = (first * first - second * second + span * span) / (2f * span)
    val rise = sqrt((first * first - along * along).coerceAtLeast(0f))

    val ux = dx / raw
    val uy = dy / raw
    val px = if (bendForward) uy else -uy
    val py = if (bendForward) -ux else ux
    return FormJoint(
        x = root.x + ux * along + px * rise,
        y = root.y + uy * along + py * rise
    )
}

/**
 * The demo itself: one rep, looping, drawn with the theme's accent.
 *
 * The figure is drawn inside a SQUARE carved out of the space it is given, so the
 * body is never stretched by a wide card, and the ground line is the same line at
 * every phase — it is the fixed thing the movement happens on.
 */
@Composable
fun ExerciseFormDemo(
    exerciseName: String,
    modifier: Modifier = Modifier
) {
    val family = movementFamilyFor(exerciseName)
    val transition = rememberInfiniteTransition(label = "form_demo")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FORM_DEMO_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "form_demo_phase"
    )
    val pose = formPoseAt(phase, family)
    val limb = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val description = getString("form_demo_description")

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description }
    ) {
        val pad = 8.dp.toPx()
        val available = size.width - pad * 2
        val side = min(available, size.height - pad * 2)
        if (side <= 0f) return@Canvas
        val originX = pad + (available - side) / 2f
        val originY = pad + (size.height - pad * 2 - side) / 2f

        fun at(joint: FormJoint) = Offset(originX + joint.x * side, originY + joint.y * side)

        val hip = at(pose.hip)
        val shoulder = at(pose.shoulder)
        val head = at(pose.head)
        val knee = at(pose.knee)
        val ankle = at(pose.ankle)
        val elbow = at(pose.elbow)
        val hand = at(pose.hand)

        drawLine(
            color = muted.copy(alpha = 0.20f),
            start = Offset(originX, originY + 0.86f * side),
            end = Offset(originX + side, originY + 0.86f * side),
            strokeWidth = 1.dp.toPx()
        )

        val stroke = 4.dp.toPx()
        drawLine(limb, hip, shoulder, stroke, StrokeCap.Round)
        drawLine(limb, hip, knee, stroke, StrokeCap.Round)
        drawLine(limb, knee, ankle, stroke, StrokeCap.Round)
        drawLine(limb, shoulder, elbow, stroke, StrokeCap.Round)
        drawLine(limb, elbow, hand, stroke, StrokeCap.Round)
        drawCircle(limb, radius = 0.045f * side, center = head)

        val jointDot = 2.5.dp.toPx()
        listOf(hip, shoulder, knee, elbow).forEach {
            drawCircle(muted.copy(alpha = 0.85f), jointDot, it)
        }
        drawLine(muted, ankle, ankle + Offset(9.dp.toPx(), 0f), 3.dp.toPx(), StrokeCap.Round)
    }
}
