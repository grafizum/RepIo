package com.grafizum.repio.ui.components

import com.grafizum.repio.util.openUrl
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import com.grafizum.repio.data.DayOfWeekEnum
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import com.grafizum.repio.util.getString
import com.grafizum.repio.util.getQuantityString
import com.grafizum.repio.util.rememberCurrentLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.ExerciseLocalization
import com.grafizum.repio.data.ExercisePopularity
import com.grafizum.repio.data.ExerciseSearch
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WeightUnit
import com.grafizum.repio.data.WorkoutTemplates
import java.util.Calendar
import kotlin.math.roundToInt
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import com.grafizum.repio.ui.theme.BrightLime
import com.grafizum.repio.ui.theme.FlameOrange
import com.grafizum.repio.ui.theme.NeonCyan

import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import com.grafizum.repio.ui.theme.LocalLightTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * The bar between a level and its stage in the level box: `"Level 10 | IV"`.
 *
 * Owner, 2026-10-08: *"i want the card to show 'level 10 | IV' for example as a whole small
 * box instead of having the romand number on the right"*. The level and the stage it sits on
 * are one answer read twice, so the live card draws them inside one surface around this
 * separator - and the guide's example rows read it from here too, so the app cannot end up
 * teaching one notation while showing another.
 */
const val LEVEL_STAGE_SEPARATOR = " | "

/**
 * How thick the rule between a level and its Roman stage is drawn.
 *
 * Owner, 2026-10-09: *"make the line that separates level with roman number, thinner so it
 * doesnt look like a number or letter"*. What it replaces was the [LEVEL_STAGE_SEPARATOR]
 * CHARACTER printed between "Level 10" and "IV": a full `|` glyph, in the same weight, the
 * same colour and the same height as the digits on either side of it - which is exactly why
 * it read as one more numeral, or as a stray `l` from a lost word, rather than as a divider.
 * A 1dp rule cannot be misread that way: it has no glyph to read.
 *
 * The four numbers live in a spec rather than in literals inside the composable so that a
 * test can pin the property the owner actually asked for — it is THIN, never a second ring
 * — and so that the guide's copy of this box cannot quietly keep the old thickness. It is
 * the same arrangement as [com.grafizum.repio.ui.screens.LevelPillStyle] on the card: the
 * look is a value with a name, not a number buried where only pixels can report it.
 */
data class StageDividerSpec(
    val widthDp: Int,
    val heightDp: Int,
    val horizontalPaddingDp: Int,
    val alpha: Float,
)

/**
 * One hairline, no taller than the cap height it sits between.
 *
 * 14dp against the label's own type, and 0.55 alpha so it groups the two readings without
 * competing with either of them — the pipe it replaces was full-strength text, which is half
 * of why it read as text.
 */
fun stageDividerSpec(): StageDividerSpec = StageDividerSpec(
    widthDp = 1,
    heightDp = 14,
    horizontalPaddingDp = 6,
    alpha = 0.55f,
)

/** The divider's test handle — see [LevelStageDivider]. */
const val LEVEL_STAGE_DIVIDER_TAG = "level_chip_divider"

/**
 * The rule itself, DRAWN rather than typed.
 *
 * Owner, 2026-10-09. Drawing it is the fix, not the styling: a character can be given a
 * lighter weight and a smaller size and still be read as a character whenever the reader is
 * scanning for one, but a 1dp bar has no glyph to resolve. Everything about it comes from
 * [stageDividerSpec] so the guide's example of this box and the live card draw the same line.
 */
@Composable
fun LevelStageDivider(color: Color) {
    val spec = stageDividerSpec()
    Box(
        modifier = Modifier
            .padding(horizontal = spec.horizontalPaddingDp.dp)
            .width(spec.widthDp.dp)
            .height(spec.heightDp.dp)
            .background(color.copy(alpha = spec.alpha))
            // Tagged like the box's other two parts ("level_chip", "level_chip_value"), so a
            // test can measure the bar that was actually laid out instead of trusting the
            // spec - the owner's complaint was about what the phone draws, not about a
            // constant. `LevelStageLabelRenderTest` reads this node's bounds.
            .semantics { testTag = LEVEL_STAGE_DIVIDER_TAG }
    )
}

/**
 * The level box's reading — "Level 10 ⏐ IV" — drawn ONCE, for both surfaces that print it.
 *
 * It exists because there ARE two of them, and they drifted the moment the card's bar stopped
 * being a glyph: the guide's `LevelLockExample` shows this box to teach the card, and it is
 * copied from the card by hand. Two hand-copied labels is one label plus one future lie — and
 * this one is copied from a surface the owner reads on the phone, so the lie would be the
 * first thing a reader compares against the real card.
 *
 * [trailing] carries the stage dots, which only the card draws: they are the level's own
 * lattice, and the guide explains them in the caption beside this box rather than inside it.
 * [style] is a parameter because the card compacts the label when a progression is pending
 * while the guide's example is always the roomy one; the SIZE may differ, the reading cannot.
 */
@Composable
fun LevelStageLabel(
    level: Int,
    stage: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = getString("level_word_n", level),
            style = style,
            color = color,
            maxLines = 1
        )
        LevelStageDivider(color)
        // The Roman numeral as a READOUT, not a control. The -/+ that walked the stages was
        // removed on the owner's instruction (2026-10-08): "instead of having that '+' and '-'
        // to change roman number, we will open the full exercise edit screen like normal".
        Text(
            text = ProgressionEngine.stageName(stage),
            style = style,
            color = color,
            modifier = Modifier.semantics { testTag = "level_chip_value" },
            maxLines = 1
        )
        trailing?.invoke(this)
    }
}

// Color mappings per muscle group
/**
 * One light colour per muscle group. Owner's call: the mid-tones read as "dark"
 * next to the app's surfaces, so every hue was lifted one step lighter (the
 * 400-step of the same hue, never a different hue) — Chest stays red, Back stays
 * blue, only the depth changed. Keep the hue-to-body-part mapping stable: the
 * Analytics donut, the legend and the Home dots all identify a body part by it.
 */
/**
 * One colour per BROAD BODY PART (owner request): Chest, Back, Legs, Shoulders,
 * Arms and Core each carry their own hue, and every precise muscle group
 * (Biceps, Triceps, Quadriceps, Front Deltoids…) inherits the hue of the body
 * part it belongs to. [WorkoutTemplates.getTopicForMuscle] is the authoritative
 * grouping — the same mapping the badges use — so a muscle can never drift
 * away from its part's colour. Unknown/custom names fall back to neutral grey.
 */
fun getMuscleGroupColor(muscleGroup: String): Color {
    if (muscleGroup.isBlank()) return Color(0xFF94A3B8) // Grey (light)
    return when (WorkoutTemplates.getTopicForMuscle(muscleGroup)) {
        "Chest" -> Color(0xFFF87171)     // Red (light)
        "Back" -> Color(0xFF60A5FA)      // Blue (light)
        "Legs" -> Color(0xFF34D399)      // Emerald (light)
        "Shoulders" -> Color(0xFFF472B6) // Pink (light)
        "Arms" -> Color(0xFF818CF8)      // Indigo (light)
        "Core" -> Color(0xFFFACC15)      // Yellow (light)
        else -> Color(0xFF94A3B8)        // Grey (light)
    }
}

/**
 * The palette colour read as TEXT on a light surface. Yellow-400 on white is
 * ~1.6:1 contrast — unreadable — so text uses the deeper 600-step of the same
 * hue on light themes; dark themes keep the light palette as-is. Fills (donut
 * arcs, dots, badge chips) still use the light palette so the "light colours"
 * the owner asked for are preserved.
 */
@Composable
fun muscleGroupTextColor(muscleGroup: String): Color {
    if (!LocalLightTheme.current) return getMuscleGroupColor(muscleGroup)
    if (muscleGroup.isBlank()) return Color(0xFF64748B) // Grey 500
    return when (WorkoutTemplates.getTopicForMuscle(muscleGroup)) {
        "Chest" -> Color(0xFFDC2626)     // Red 600
        "Back" -> Color(0xFF2563EB)      // Blue 600
        "Legs" -> Color(0xFF059669)      // Emerald 600
        "Shoulders" -> Color(0xFFDB2777) // Pink 600
        "Arms" -> Color(0xFF4F46E5)      // Indigo 600
        "Core" -> Color(0xFFCA8A04)      // Yellow 600
        else -> Color(0xFF64748B)        // Grey 500
    }
}

/**
 * [getMuscleGroupColor] as-is, on every surface. It used to deepen colours one
 * step on light themes for contrast; the owner asked for lighter muscle-group
 * colours instead, so the palette itself is light and no surface-dependent
 * deepening happens any more. Kept as a function (not a straight alias) so a
 * future surface-aware tweak still has its single choke point.
 */
@Composable
fun muscleGroupColor(muscleGroup: String): Color = getMuscleGroupColor(muscleGroup)

/**
 * Refines a stored muscle group into the most precise well-known group for an
 * exercise when possible, using the built-in pool as the authoritative real
 * data for what the exercise trains. Exercises stored under a broad category
 * ("Legs", "Shoulders", "Back"…) resolve to their specific subcategory
 * (Quadriceps, Front Deltoids, Lats…, Pectorals for the chest). Never guesses:
 * custom/unknown exercises keep their stored group.
 */
fun resolvePreciseMuscleGroup(exerciseName: String, storedGroup: String): String {
    if (exerciseName.isBlank()) return storedGroup
    val storedTopic = WorkoutTemplates.getTopicForMuscle(storedGroup)
    val precise = WorkoutTemplates.getPrimaryMuscleGroupForExercise(exerciseName, storedTopic)
    if (precise.isBlank() || precise.equals("Other", ignoreCase = true)) return storedGroup
    return precise
}

/**
 * Combined label for a muscle group: the broad body part ("Legs", "Arms", "Core"…)
 * stays as the colored chip, with the precise group ("Quadriceps", "Biceps", "Obliques"…)
 * shown right next to it. Falls back to a single label when both are the same.
 */
@Composable
fun formatMuscleGroup(muscleGroup: String, exerciseName: String = ""): String {
    val effective = resolvePreciseMuscleGroup(exerciseName, muscleGroup)
    val topic = WorkoutTemplates.getTopicForMuscle(effective)
    val topicLabel = translateMuscleGroup(topic)
    val preciseLabel = translateMuscleGroup(effective)
    return if (preciseLabel.equals(topicLabel, ignoreCase = true)) preciseLabel
           else "$topicLabel · $preciseLabel"
}

/**
 * The precise muscle group an exercise is *displayed* under: the stored group
 * refined through the built-in pool when the pool knows better. Push-ups stored
 * under "Shoulders" by an older build resolve to "Pectorals", so everything
 * derived from the stored value — dot colours, day focus lines, Analytics
 * buckets — agrees with the badge that already resolves this way.
 */
fun effectiveMuscleGroup(exerciseName: String, storedGroup: String): String =
    resolvePreciseMuscleGroup(exerciseName, storedGroup)

@Composable
fun MuscleGroupBadge(
    muscleGroup: String,
    exerciseName: String = "",
    modifier: Modifier = Modifier
) {
    val effective = resolvePreciseMuscleGroup(exerciseName, muscleGroup)
    val topic = WorkoutTemplates.getTopicForMuscle(effective)
    val color = muscleGroupColor(topic)
    // Text must stay readable on light surfaces, where the light palette dips
    // below legible contrast (yellow-400 on white is ~1.6:1).
    val textColor = muscleGroupTextColor(topic)
    val isDeveloper = LocalDeveloperSkin.current
    val topicLabel = translateMuscleGroup(topic)
    val preciseLabel = translateMuscleGroup(effective)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Surface(
            color = color.copy(alpha = 0.2f),
            contentColor = textColor,
            shape = RoundedCornerShape(8.dp),
            border = null) {
            Text(
                text = topicLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        if (!preciseLabel.equals(topicLabel, ignoreCase = true)) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = preciseLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Composable
fun translateMuscleGroup(muscleGroup: String): String {
    return when (muscleGroup.lowercase().trim()) {
        "back" -> getString("back")
        "biceps" -> getString("biceps")
        "shoulders" -> getString("shoulders")
        "chest" -> getString("chest")
        "pectorals" -> getString("pectorals")
        "legs" -> getString("legs")
        "abs" -> getString("abs")
        "triceps" -> getString("triceps")
        "arms" -> getString("arms")
        "core" -> getString("core")
        "front deltoids" -> getString("front_delts")
        "lateral deltoids" -> getString("lateral_delts")
        "rear deltoids" -> getString("rear_delts")
        "lats" -> getString("lats_part")
        "upper back" -> getString("upper_back")
        "upper traps" -> getString("upper_traps")
        "spinal erectors" -> getString("spinal_erectors")
        "forearms" -> getString("forearms")
        "obliques" -> getString("obliques")
        "quadriceps" -> getString("quadriceps_part")
        "hamstrings" -> getString("hamstrings_part")
        "glutes" -> getString("glutes_part")
        "calves" -> getString("calves_part")
        "hip adductors" -> getString("hip_adductors")
        "hip abductors" -> getString("hip_abductors")
        "other" -> getString("other")
        else -> muscleGroup
    }
}

@Composable
fun translateExerciseName(name: String): String {
    // Pool exercises are translated from the tables in ExerciseLocalization. Those tables
    // cover 121 of the 601 pool exercises per language so far — anything they miss falls back
    // to its canonical English name, which is correct, just untranslated. Everything outside
    // the pool — the generic movement names some templates still carry ("TRX Row", "Curls") and
    // user-created customs — falls through to the legacy mapping below.
    val canonical = ExerciseLocalization.canonicalFor(name)
    if (canonical != null) {
        return ExerciseLocalization.displayName(
            canonical,
            ExerciseLocalization.languageOf(rememberCurrentLocale())
        )
    }
    return translateGenericExerciseName(name)
}

/**
 * Name mapping for exercises outside the built-in pool: the handful of generic movement
 * names the built-in templates use, plus whatever a user typed for a custom exercise (which
 * is returned unchanged).
 */
@Composable
private fun translateGenericExerciseName(name: String): String {
    val lower = name.trim().lowercase()
    return when {
        lower == "row" || lower == "rows" -> getString("ex_row")
        lower == "pullover" || lower == "pullovers" -> getString("ex_dumbbell_pullover")
        lower == "curl" || lower == "curls" -> getString("ex_curls")
        lower == "hammer curl" || lower == "hammer curls" ||
            lower == "hammer curl (dumbbell)" || lower == "dumbbell hammer curls" -> getString("ex_hammer_curl")
        lower == "shoulder press" || lower == "shoulder presses" -> getString("ex_shoulder_press")
        lower == "lateral raise" || lower == "lateral raises" -> getString("ex_lateral_raise")
        lower == "crunch" || lower == "crunches" -> getString("ex_crunches")
        lower == "bicycle crunch" || lower == "bicycle crunches" -> getString("ex_bicycle_crunches")
        lower == "bench press" || lower == "bench presses" || lower == "floor press" || lower == "floor presses" || lower == "chest press" || lower == "chest presses" -> getString("ex_bench_press")
        lower == "leg raise" || lower == "leg raises" -> getString("ex_leg_raise")
        lower == "squat" || lower == "squats" -> getString("ex_squat")
        lower == "romanian deadlift" || lower == "romanian deadlifts" -> getString("ex_romanian_deadlift")
        lower == "deadlift" || lower == "deadlifts" -> getString("ex_deadlift")
        lower == "lunge" || lower == "lunges" -> getString("ex_lunge")
        lower == "plank" || lower == "planks" -> getString("ex_plank")
        lower == "tricep pushdown" || lower == "tricep pushdowns" || lower == "pushdown" || lower == "pushdowns" -> getString("ex_tricep_pushdown")
        lower == "face pull" || lower == "face pulls" -> getString("ex_face_pull")
        lower == "side raise" || lower == "side raises" -> getString("ex_side_raise")
        lower == "push-up" || lower == "push-ups" -> getString("ex_push_up")
        lower == "pull-up" || lower == "pull-ups" -> getString("ex_pull_up")
        lower == "chin-up" || lower == "chin-ups" -> getString("ex_chin_up")
        lower == "military press" || lower == "military presses" -> getString("ex_military_press")
        lower == "lat pulldown" || lower == "lat pulldowns" -> getString("ex_lat_pulldown")
        lower == "seated row" || lower == "seated rows" -> getString("ex_seated_row")
        lower == "leg press" || lower == "leg presses" -> getString("ex_leg_press")
        lower == "leg curl" || lower == "leg curls" -> getString("ex_leg_curl")
        lower == "leg extension" || lower == "leg extensions" -> getString("ex_leg_extension")
        lower == "calf raise" || lower == "calf raises" -> getString("ex_calf_raise")
        lower == "hip thrust" || lower == "hip thrusts" -> getString("ex_hip_thrust")
        lower == "glute bridge" || lower == "glute bridges" -> getString("ex_glute_bridge")
        lower == "back extension" || lower == "back extensions" -> getString("ex_back_extension")
        lower == "tricep dip" || lower == "tricep dips" || lower == "tricep dips / extensions" -> getString("ex_tricep_dips_extensions")
        lower == "overhead tricep extension" || lower == "overhead tricep extensions" -> getString("ex_overhead_tricep_extension")
        lower == "dumbbell overhead extension" || lower == "dumbbell overhead extensions" -> getString("ex_dumbbell_overhead_extension")
        lower == "rear delt fly" || lower == "rear delt flies" || lower == "rear delt flyes" || lower == "rear-delt fly" -> getString("ex_rear_delt_fly")
        lower == "shrug" || lower == "shrugs" -> getString("ex_shrugs")
        
        // Specific ones
        lower == "dumbbell curl" || lower == "dumbbell curls" -> getString("ex_dumbbell_curl")
        lower == "dumbbell row" || lower == "dumbbell rows" -> getString("ex_dumbbell_row")
        lower == "barbell row" || lower == "barbell rows" -> getString("ex_barbell_row")
        lower == "barbell squat" || lower == "barbell squats" -> getString("ex_barbell_squat")
        lower == "lunge (dumbbell)" || lower == "dumbbell lunges" -> getString("ex_dumbbell_lunge")
        lower == "barbell floor press" || lower == "barbell floor presses" -> getString("ex_barbell_floor_press")
        lower == "floor press (dumbbell)" || lower == "dumbbell floor presses" -> getString("ex_dumbbell_floor_press")

        else -> name
    }
}

/**
 * THE ONE SUMMARY LINE for a planned exercise row: "3 sets × 90 s" or "3 sets ×
 * 8–12 reps".
 *
 * Owner, 2026-10-09: *"Jumping jacks is one of many exercises that doesnt show the
 * level style and it shows reps in the card yet the exercise is based on time and
 * has a timer????"* - and the same lie sat on the HOME schedule, the routine row
 * and the setup preview, each written as its own `sets_x_reps_pl` call. A timed
 * exercise therefore read as a rep exercise in three places that had nothing to do
 * with the card that was being fixed.
 *
 * The unit comes from the exercise ([WorkoutTemplates.isExerciseTimeBased]) and the
 * NUMBER comes from the level system when the caller can supply it: a jumping jack
 * stores "8–12" from before the level system, so printing the stored string under a
 * seconds label would only move the confusion. [prescribed] is the row's
 * prescription at the level it is on - the same value the workout card headlines -
 * so the schedule and the card cannot disagree.
 */
@Composable
fun exerciseSetsSummaryLabel(
    exerciseName: String,
    targetSets: Int,
    storedTarget: String,
    prescribed: ProgressionEngine.PrescribedValue? = null,
): String {
    if (!WorkoutTemplates.isExerciseTimeBased(exerciseName)) {
        return getQuantityString("sets_x_reps_pl", targetSets, targetSets, storedTarget)
    }
    val seconds = prescribed
        ?.takeIf { it.kind == ProgressionEngine.ValueKind.DURATION }
        ?.value?.toInt()
        ?: WorkoutTemplates.secondsInStoredTarget(storedTarget)
    val value = seconds?.let { getString("progression_duration_value", it) } ?: storedTarget
    return getQuantityString("sets_x_time_pl", targetSets, targetSets, value)
}

/**
 * Short, localized label for a stored tempo cue ("explosive", "slow" or "" = none).
 * Kept tiny so it fits on the exercise info line next to sets/reps.
 */
@Composable
fun getTempoLabel(tempo: String): String = when(tempo) {
    "explosive" -> getString("tempo_explosive")
    "slow" -> getString("tempo_slow")
    else -> ""
}

@Composable
fun translateDayTitle(title: String): String {
    return when {
        title.contains("Push (Chest, Shoulders, Triceps)") -> title.replace("Push (Chest, Shoulders, Triceps)", getString("push_title"))
        title.contains("Pull (Back & Biceps)") -> title.replace("Pull (Back & Biceps)", getString("pull_title"))
        title.contains("Legs & Core") -> title.replace("Legs & Core", getString("legs_core_title"))
        title.contains("Upper Body Power") -> title.replace("Upper Body Power", getString("upper_power_title"))
        title.contains("Lower Body & Core") -> title.replace("Lower Body & Core", getString("lower_power_title"))
        title.contains("Upper Body Hypertrophy") -> title.replace("Upper Body Hypertrophy", getString("upper_hypertrophy_title"))
        title.contains("Lower Body Hypertrophy") -> title.replace("Lower Body Hypertrophy", getString("lower_hypertrophy_title"))
        title.contains("Full Body A") -> title.replace("Full Body A", getString("full_body_a_title"))
        title.contains("Full Body B") -> title.replace("Full Body B", getString("full_body_b_title"))
        title.contains("Full Body C") -> title.replace("Full Body C", getString("full_body_c_title"))
        title.contains("Pull & Back Focus") -> title.replace("Pull & Back Focus", getString("pull_title"))
        title.contains("Upper Body Push & Pull") -> title.replace("Upper Body Push & Pull", getString("upper_power_title"))
        title.contains("Legs, Shoulders & Abs") -> title.replace("Legs, Shoulders & Abs", getString("legs_core_title"))
        title.contains("Upper Body Volume") -> title.replace("Upper Body Volume", getString("upper_hypertrophy_title"))
        title.contains("Shoulders, Arms & Back") -> title.replace("Shoulders, Arms & Back", getString("pull_title"))
        title.contains("Rest & Recovery") -> title.replace("Rest & Recovery", getString("rest_recovery_day"))
        else -> title
    }
}

@Composable
fun translateDayDescription(description: String): String {
    val trimmed = description.trim()
    return when (trimmed) {
        "push_desc" -> getString("push_desc")
        "pull_desc" -> getString("pull_desc")
        "legs_desc" -> getString("legs_desc")
        "upper_power_desc" -> getString("upper_power_desc")
        "lower_power_desc" -> getString("lower_power_desc")
        "rest_desc" -> getString("rest_desc")
        "full_body_a_desc" -> getString("full_body_a_desc")
        "full_body_b_desc" -> getString("full_body_b_desc")
        "full_body_c_desc" -> getString("full_body_c_desc")
        
        "Heavy chest pressing and overhead shoulder work" -> getString("push_desc")
        "Heavy rows, pulldowns, and bicep hypertrophy" -> getString("pull_desc")
        "Squats, lunges, leg curls, and core stability" -> getString("legs_desc")
        "Heavy chest, back, shoulders, and arm pressing" -> getString("upper_power_desc")
        "Squats, deadlifts, and abs" -> getString("lower_power_desc")
        "Rest and mobility work" -> getString("rest_desc")
        "Active rest, foam rolling, and mobility" -> getString("rest_desc")
        "Active rest, walking, or light stretching" -> getString("rest_desc")
        "Rest, foam rolling, and mobility work" -> getString("rest_desc")
        "Volume chest flyes, rows, lateral raises, and biceps" -> getString("upper_power_desc")
        "Lunges, Romanian deadlifts, calves, and core" -> getString("lower_power_desc")
        "Squat, Press, Row, Core" -> getString("full_body_a_desc")
        "Lunge, Shoulder Press, Pullover, Biceps" -> getString("full_body_b_desc")
        "Romanian Deadlift, Chest Press, Arms, Core" -> getString("full_body_c_desc")
        "Back, biceps, rear delts, and abs" -> getString("pull_desc")
        "Chest, back, biceps, and core" -> getString("push_desc")
        "Squats, lunges, shoulder press, and core" -> getString("legs_desc")
        "Rows, curls, lateral raises, and rear delts" -> getString("pull_desc")
        "Shoulder press, curls, extensions, and rows" -> getString("push_desc")
        "Volume pressing and lateral raise volume" -> getString("push_desc")
        "Rowing variations, rear delts, and bicep curls" -> getString("pull_desc")
        "Deadlifts, Romanian deadlifts, calves, and abs" -> getString("legs_desc")
        else -> description
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector?,
    iconColor: Color,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    compact: Boolean = false,
    /**
     * Extra content for the badge position on the right of the card — used by
     * the rank badge. Rendered in the compact and hero layouts, which are the
     * two that have a badge slot; the standard (subtitle) layout has none.
     */
    trailing: (@Composable () -> Unit)? = null,
    /**
     * Replaces the leading icon INSIDE its chip: same 40dp rounded tile, same
     * tinted background, but the content is the caller's. The Overall Level card
     * passes the rank letter here so the rank sits on the LEFT of the number in
     * the slot the trophy used to own, rather than trailing the number as a
     * badge of its own. Preferred over [icon]; when both are given, this wins.
     */
    leading: (@Composable () -> Unit)? = null
) {
    val isDeveloper = LocalDeveloperSkin.current
    val window = LocalWindowSize.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(
                when {
                    subtitle.isNotBlank() -> 16.dp
                    compact -> 12.dp
                    else -> 24.dp
                }
            )
        ) {
            if (subtitle.isBlank()) {
                if (compact) {
                    // COMPACT HERO (Analytics tab): one lean row — icon chip,
                    // label + big number, badge on the right. Keeps the whole
                    // Analytics screen on one page without scrolling.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (leading != null || icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(iconColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (leading != null) {
                                    leading()
                                } else {
                                    Icon(
                                        imageVector = icon!!,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (badgeText != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (trailing != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            trailing()
                        }
                    }
                } else {
                    // HERO MODE: headline stat with optional badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // weight: a long title (or a long translation) wraps inside the leftover
                        // width instead of squeezing the badge off the card.
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (badgeText != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (trailing != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            trailing()
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(44.dp).align(Alignment.CenterVertically)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Shrink the hero number on narrow phones so it never clips mid-glyph.
                        val isCompact = window.isNarrow
                        Text(
                            text = value,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = if (isCompact) 32.sp else 44.sp,
                                lineHeight = if (isCompact) 32.sp else 44.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // STANDARD MODE: Split layout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ExerciseVideoLinkButton(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onSaveDemoUrl: ((String) -> Unit)? = null,
    isEditMode: Boolean = false
) {
    val isDeveloper = LocalDeveloperSkin.current
    var showInputDialog by remember { mutableStateOf(false) }
    var inputUrlText by remember { mutableStateOf(videoUrl) }

    val focusManager = LocalFocusManager.current
    if (isEditMode && onSaveDemoUrl != null) {
        Button(
            onClick = {
                focusManager.clearFocus()
                inputUrlText = videoUrl
                showInputDialog = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp),
            modifier = modifier
        ) {
            val icon = if (videoUrl.isNotBlank()) Icons.Default.Edit else Icons.Default.Add
            val textKey = if (videoUrl.isNotBlank()) "edit_demo" else "input_demo"
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = getString(textKey),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false
            )
        }
    } else if (videoUrl.isNotBlank()) {
        Button(
            onClick = {
                focusManager.clearFocus()
                openUrl(videoUrl)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp),
            modifier = modifier.semantics { testTag = "watch_demo_button" }
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircleFilled,
                contentDescription = getString("watch_demo_video"),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = getString("watch_demo"),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false
            )
        }
    } else if (onSaveDemoUrl != null) {
        Button(
            onClick = {
                focusManager.clearFocus()
                inputUrlText = ""
                showInputDialog = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp),
            modifier = modifier.semantics { testTag = "input_demo_button" }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = getString("input_demo_video"),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = getString("input_demo"),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false
            )
        }
    }

    if (showInputDialog && onSaveDemoUrl != null) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("demo_video_link"))
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(
                        text = getString("enter_video_url_desc"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = inputUrlText,
                        onValueChange = { inputUrlText = it },
                        label = { Text(getString("video_url")) },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSaveDemoUrl(inputUrlText.trim())
                        showInputDialog = false
                    }
                ) {
                    Text(getString("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showInputDialog = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }
}

@Composable
fun getRpeRirExplanation(intensity: String): String {
    if (intensity.isBlank()) return ""
    
    // Regex for "RPE X" or "RPE X-Y"
    val rpeRegex = Regex("RPE\\s*(\\d+)(?:-(\\d+))?", RegexOption.IGNORE_CASE)
    val match = rpeRegex.find(intensity) ?: return ""
    
    val low = match.groups[1]?.value?.toIntOrNull() ?: return ""
    val high = match.groups[2]?.value?.toIntOrNull()
    
    return if (high != null) {
        val rirLow = (10 - high).coerceAtLeast(0)
        val rirHigh = (10 - low).coerceAtLeast(0)
        if (rirLow == rirHigh) {
             getString("rpe_explanation_rir_$rirLow")
        } else {
             getString("rpe_explanation_rir_range", rirLow, rirHigh)
        }
    } else {
        val rir = (10 - low).coerceAtLeast(0)
        getString("rpe_explanation_rir_$rir")
    }
}

@Composable
fun getExerciseDetails(name: String, muscleGroup: String, intensity: String = ""): Pair<String, String> {
    // Every built-in exercise has its own accurate, mimic-able description
    // (see ExerciseDescriptions.kt), so distinct movements never share text. In German and
    // Italian the same text comes from the translation table instead; a language without a
    // table, or an exercise outside the pool, keeps the English one rather than showing
    // nothing. `name` is normalized first so an exercise saved under a translated name still
    // resolves to its table entry.
    val canonicalName = ExerciseLocalization.canonicalFor(name) ?: name
    val language = ExerciseLocalization.languageOf(rememberCurrentLocale())
    var description = ExerciseLocalization.description(canonicalName, language)
        ?: getExerciseDescription(name)
        ?: getExerciseDescription(canonicalName)
        ?: genericExerciseDescription(name, muscleGroup)

    if (intensity.contains("RPE", ignoreCase = true)) {
        description += "\n\n" + getString("intensity_label") + ": " + intensity
    }

    return description to ""
}

/**
 * Fallback description for exercises outside the built-in pool (user-created
 * customs). Built-in names never reach here because [getExerciseDescription]
 * matches them exactly.
 */
@Composable
private fun genericExerciseDescription(name: String, muscleGroup: String): String {
    val cleanName = name.trim().lowercase()
    return when {
        cleanName.contains("bench press") || cleanName.contains("floor press") -> getString("ex_bench_press_desc")
        cleanName.contains("incline") && cleanName.contains("press") -> getString("ex_incline_press_desc")
        cleanName.contains("decline") && cleanName.contains("press") -> getString("ex_decline_press_desc")
        cleanName.contains("goblet squat") -> getString("ex_goblet_squat_desc")
        cleanName.contains("squat") && !cleanName.contains("goblet") && !cleanName.contains("split") -> getString("ex_squats_desc")
        cleanName.contains("romanian") || cleanName.contains("rdl") -> getString("ex_romanian_deadlift_desc")
        cleanName.contains("deadlift") -> getString("ex_deadlifts_desc")
        cleanName.contains("pull-up") || cleanName.contains("pullup") || cleanName.contains("chin-up") -> getString("ex_pull_ups_desc")
        cleanName.contains("pulldown") -> getString("ex_pulldown_desc")
        cleanName.contains("pullover") -> getString("ex_pullover_desc")
        cleanName.contains("seated row") || cleanName.contains("cable row") -> getString("ex_seated_row_desc")
        cleanName.contains("shoulder press") || cleanName.contains("military press") || cleanName.contains("overhead press") || cleanName.contains("arnold") -> getString("ex_shoulder_press_desc")
        cleanName.contains("front raise") -> getString("ex_front_raise_desc")
        cleanName.contains("lateral raise") || cleanName.contains("side raise") -> getString("ex_side_raise_desc")
        cleanName.contains("hammer curl") -> getString("ex_hammer_curl_desc")
        cleanName.contains("curl") -> getString("ex_bicep_curl_desc")
        cleanName.contains("row") -> getString("ex_rows_desc")
        cleanName.contains("bird-dog") -> getString("ex_bird_dog_desc")
        cleanName.contains("superman") -> getString("ex_superman_desc")
        cleanName.contains("plank") -> getString("ex_plank_desc")
        cleanName.contains("mountain climber") -> getString("ex_mountain_climbers_desc")
        cleanName.contains("hollow") && cleanName.contains("hold") -> getString("ex_hollow_hold_desc")
        cleanName.contains("pushdown") -> getString("ex_tricep_pushdown_desc")
        cleanName.contains("extension") && (cleanName.contains("tricep") || cleanName.contains("overhead") || cleanName.contains("skullcrusher")) -> getString("ex_tricep_extension_desc")
        cleanName.contains("lunge") || cleanName.contains("step-up") || cleanName.contains("split squat") -> getString("ex_lunges_desc")
        cleanName.contains("side lying leg raise") -> getString("ex_side_lying_leg_raise_desc")
        cleanName.contains("clamshell") -> getString("ex_clamshell_desc")
        cleanName.contains("crunch") -> getString("ex_cable_crunch_desc")
        cleanName.contains("crunch") -> getString("ex_crunches_desc")
        cleanName.contains("leg raise") -> getString("ex_leg_raises_desc")
        cleanName.contains("russian twist") -> getString("ex_russian_twist_desc")
        cleanName.contains("abs") -> getString("ex_abs_desc")
        cleanName.contains("fly") -> getString("ex_flyes_desc")
        cleanName.contains("shrug") -> getString("ex_shrugs_desc")
        cleanName.contains("face pull") -> getString("ex_face_pulls_desc")
        cleanName.contains("good morning") -> getString("ex_good_morning_desc")
        cleanName.contains("extension") && (cleanName.contains("back") || cleanName.contains("hyper")) -> getString("ex_back_extensions_desc")
        cleanName.contains("woodchopper") || cleanName.contains("twist") -> getString("ex_woodchoppers_desc")
        cleanName.contains("push-up") || cleanName.contains("pushup") -> getString("ex_push_ups_desc")
        cleanName.contains("dip") && !cleanName.contains("pushdown") -> getString("ex_dips_desc")
        cleanName.contains("leg extension") -> getString("ex_leg_extension_desc")
        cleanName.contains("leg curl") -> getString("ex_leg_curl_desc")
        cleanName.contains("leg press") -> getString("ex_leg_press_desc")
        cleanName.contains("calf raise") -> getString("ex_calf_raises_desc")
        cleanName.contains("thrust") || cleanName.contains("bridge") -> getString("ex_hip_thrust_desc")
        cleanName.contains("kickback") -> getString("ex_kickbacks_desc")
        cleanName.contains("carry") || cleanName.contains("walk") -> getString("ex_carry_desc")
        cleanName.contains("rollout") -> getString("ex_rollout_desc")
        cleanName.contains("clean") || cleanName.contains("snatch") || cleanName.contains("thruster") -> getString("ex_explosive_desc")
        else -> {
            val mg = muscleGroup.lowercase().trim()
            when {
                mg.contains("chest") -> getString("ex_desc_fallback_chest")
                mg.contains("back") || (mg.contains("lat") && !mg.contains("lateral")) || mg.contains("trap") || mg.contains("erector") -> getString("ex_desc_fallback_back")
                mg.contains("shoulder") || mg.contains("delt") -> getString("ex_desc_fallback_shoulders")
                mg.contains("leg") || mg.contains("quad") || mg.contains("hamstring") || mg.contains("glute") || mg.contains("calf") || mg.contains("adductor") || mg.contains("abductor") -> getString("ex_desc_fallback_legs")
                mg.contains("bicep") -> getString("ex_desc_fallback_biceps")
                mg.contains("tricep") -> getString("ex_desc_fallback_triceps")
                mg.contains("abs") || mg.contains("core") || mg.contains("oblique") -> getString("ex_desc_fallback_abs")
                else -> getString("ex_desc_fallback_generic", translateMuscleGroup(muscleGroup))
            }
        }
    }
}

@Composable
fun ExpandableExerciseCard(
    exercise: RoutineExerciseEntity,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReplace: (() -> Unit)? = null,
    onSaveVideoUrl: ((String) -> Unit)? = null,
    showDescription: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    isExpanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    showActions: Boolean = true
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = isExpanded ?: internalExpanded
    val toggleExpand = {
        val nextValue = !expanded
        if (isExpanded != null && onExpandedChange != null) {
            onExpandedChange(nextValue)
        } else {
            internalExpanded = nextValue
        }
    }

    val (description, _) = getExerciseDetails(exercise.name, exercise.muscleGroup, exercise.targetIntensity)
    val isDeveloper = LocalDeveloperSkin.current
    // The gap between the sets, the intensity and the tempo is a text gap, so it
    // comes from the window rather than a fixed dp - see [WindowSize.inlineGap].
    val window = LocalWindowSize.current
    val inlineGap = window.inlineGap
    val focusManager = LocalFocusManager.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant
        ),
        // Hairline rim in the idle state: the routine rows hold a visible edge
        // against the background instead of dissolving into it. Selection keeps
        // its stronger primary border.
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { 
                if (isSelectionMode) {
                    onSelectedChange?.invoke(!isSelected)
                } else {
                    toggleExpand()
                }
            }
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Drag handle is always visible: drag it in selection mode, or
                    // long-press the card to start dragging outside selection mode
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .then(dragHandleModifier)
                    )
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = onSelectedChange,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Column {
                        // THE NAME GETS ITS OWN LINE WHEN THE TEXT IS BIG.
                        //
                        // Measured on the 1.8x emulator: side by side, the muscle
                        // badge (two chips, 430px of the row) left "Dumbbell Bench
                        // Press" 212px, so the name of the exercise rendered as
                        // "Dumbb..." - the exercise was abbreviated to make room
                        // for its own muscle group. Stacked, the name reads in full
                        // and the badge keeps a line it does not have to share.
                        //
                        // Only at a large font scale: the badge's share of the row
                        // is a FRACTION, so at 1.0x there is room for both and the
                        // compact one-line header is the better header.
                        if (window.isLargeText) {
                            Text(
                                text = translateExerciseName(exercise.name),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = if (expanded) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(if (expanded) 6.dp else 2.dp))
                            MuscleGroupBadge(muscleGroup = exercise.muscleGroup, exerciseName = exercise.name)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = translateExerciseName(exercise.name),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = if (expanded) 2 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(inlineGap))
                                MuscleGroupBadge(muscleGroup = exercise.muscleGroup, exerciseName = exercise.name)
                            }
                        }
                        if (showActions) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    // The unit comes from the EXERCISE, not from the routine
                                    // string stored on the row - see
                                    // [exerciseSetsSummaryLabel]. Jumping Jacks' row carries
                                    // "8–12" from before the level system, so this line used
                                    // to call a timed exercise a rep exercise.
                                    text = exerciseSetsSummaryLabel(
                                        exerciseName = exercise.name,
                                        targetSets = exercise.targetSets,
                                        storedTarget = exercise.targetReps,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (exercise.targetIntensity.isNotBlank()) {
                                    val rpeExpl = getRpeRirExplanation(exercise.targetIntensity)
                                    val displayIntensity = if (rpeExpl.isNotBlank()) "${exercise.targetIntensity} ($rpeExpl)" else exercise.targetIntensity
                                    // A real gap, not the single space this used to
                                    // carry in front of the bullet. At font scale 1.8
                                    // on the 411dp phone that space rendered as ~7px
                                    // between two 26px-tall words: the owner's
                                    // "pressed together labeling and text".
                                    Spacer(modifier = Modifier.width(inlineGap))
                                    Text(
                                        text = "• $displayIntensity",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (exercise.tempo.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(inlineGap))
                                    Text(
                                        text = "• ${getTempoLabel(exercise.tempo)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isSelectionMode) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
            }

            if (expanded && !isSelectionMode) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Description & Detailed Muscles
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showDescription) {
                            Row {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(getString("description"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    Text(description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        val detailedMuscle = WorkoutTemplates.getMuscleGroupForExercise(exercise.name)
                        
                        Row {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(getString("trained_muscles"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Text(translateMuscleGroup(detailedMuscle), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // ACTION BUTTONS GRID (2x2 for symmetry)
                    if (showActions) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (onReplace != null) {
                                    OutlinedButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            onReplace()
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(getString("replace"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                if (onEdit != null) {
                                    OutlinedButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            onEdit()
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(getString("edit"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (onSaveVideoUrl != null) {
                                    ExerciseVideoLinkButton(
                                        videoUrl = exercise.videoUrl,
                                        onSaveDemoUrl = onSaveVideoUrl,
                                        isEditMode = true,
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    )
                                } else if (exercise.videoUrl.isNotBlank()) {
                                    ExerciseVideoLinkButton(
                                        videoUrl = exercise.videoUrl,
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    )
                                } else {
                                    // Spacer to maintain grid symmetry if no demo button
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                if (onDelete != null) {
                                    OutlinedButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            onDelete()
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(getString("delete"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MuscleGroupSelector(
    selectedMuscle: String,
    onMuscleSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    val focusManager = LocalFocusManager.current
    val categories = listOf(
        getString("arms") to listOf("Biceps", "Triceps", "Forearms"),
        getString("legs") to listOf("Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"),
        getString("back") to listOf("Lats", "Upper Back", "Upper Traps", "Spinal Erectors"),
        getString("chest") to listOf("Chest"),
        getString("shoulders") to listOf("Front Deltoids", "Lateral Deltoids", "Rear Deltoids"),
        getString("abs") to listOf("Abs", "Obliques")
    )

    var expandedCategory by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = getString("muscle_group_body_part"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        categories.forEach { (catName, parts) ->
            val isCatExpanded = expandedCategory == catName
            val hasSelectedInCat = parts.any { it.equals(selectedMuscle, ignoreCase = true) }

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCatExpanded || hasSelectedInCat) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (hasSelectedInCat) {
                        MaterialTheme.colorScheme.primary
                    } else if (isCatExpanded) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)} else Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                expandedCategory = if (isCatExpanded) null else catName 
                            }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (hasSelectedInCat) FontWeight.Bold else FontWeight.Medium),
                                color = if (hasSelectedInCat) (MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurface
                            )
                            if (hasSelectedInCat && !isCatExpanded) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isCatExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isCatExpanded) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth()
                        ) {
                            parts.forEach { part ->
                                val isSelected = part.equals(selectedMuscle, ignoreCase = true)
                                val displayName = translateMuscleGroup(part)
                                
                                Surface(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable { 
                                        focusManager.clearFocus()
                                        onMuscleSelected(part)
                                        expandedCategory = null 
                                    }
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TimersMergedCard(
    restValue: Int,
    transitionValue: Int,
    onRestSelected: (Int) -> Unit,
    onTransitionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    val focusManager = LocalFocusManager.current
    var expandedType by remember { mutableStateOf<String?>(null) } // "REST", "TRANSITION", or null

    val restOptions = listOf(30, 45, 60, 90, 120, 180)
    val transitionOptions = listOf(60, 90, 120, 150, 180, 210)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = getString("timer_durations"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getString("timer_settings_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two Buttons in a Row (exactly like muscle group dropdown style in onboarding)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimerSelectionButton(
                    label = getString("resting_timer_duration"),
                    value = formatTimerValue(restValue),
                    isSelected = expandedType == "REST",
                    onClick = { 
                        focusManager.clearFocus()
                        expandedType = if (expandedType == "REST") null else "REST" 
                    },
                    modifier = Modifier.weight(1f)
                )
                TimerSelectionButton(
                    label = getString("exercise_transition_timer"),
                    value = formatTimerValue(transitionValue),
                    isSelected = expandedType == "TRANSITION",
                    onClick = { 
                        focusManager.clearFocus()
                        expandedType = if (expandedType == "TRANSITION") null else "TRANSITION" 
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Shared Dropdown Area (Expands the box)
            AnimatedVisibility(
                visible = expandedType != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val options = if (expandedType == "REST") restOptions else transitionOptions
                val current = if (expandedType == "REST") restValue else transitionValue
                val onSelect = if (expandedType == "REST") onRestSelected else onTransitionSelected

                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEach { opt ->
                            val isSel = opt == current
                            Surface(
                                onClick = { 
                                    focusManager.clearFocus()
                                    onSelect(opt)
                                    expandedType = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = if (!isSel) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                            ) {
                                Text(
                                    text = formatTimerValue(opt),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerSelectionButton(
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // AutoScaleText keeps the label legible on narrow screens instead of
                    // collapsing to an unreadable 7sp when selected.
                    AutoScaleText(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        minScale = 0.6f
                    )
                    if (isSelected) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

fun formatTimerValue(seconds: Int): String {
    return when {
        seconds == 90 -> "1.5m"
        seconds == 150 -> "2.5m"
        seconds == 210 -> "3.5m"
        seconds >= 60 && seconds % 60 == 0 -> "${seconds / 60}m"
        seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}



/**
 * Interactive Donut Chart for Muscle Group Distribution
 */
@Composable
fun MuscleGroupDonutChart(
    distribution: Map<String, Int>, // "Back" -> 12 sets
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    val total = distribution.values.sum().coerceAtLeast(1)

    // Overview shows the broad body parts (Arms, Legs, Back…) by default; a button
    // in the card opens the full breakdown with every precise muscle group.
    // No synthetic "Other" slice — genuine ungrouped sets keep their own bucket.
    val otherLabel = getString("other")
    val normalized: Map<String, Int> = remember(distribution) {
        val map = mutableMapOf<String, Int>()
        distribution.forEach { (key, value) ->
            val k = if (key.equals("Other", ignoreCase = true)) otherLabel else key
            map[k] = (map[k] ?: 0) + value
        }
        map
    }
    val topicDistribution: Map<String, Int> = remember(normalized) {
        val map = mutableMapOf<String, Int>()
        normalized.forEach { (key, value) ->
            val topic = WorkoutTemplates.getTopicForMuscle(key)
            map[topic] = (map[topic] ?: 0) + value
        }
        map
    }
    var showDetailDialog by remember { mutableStateOf(false) }

    // Overview: broad body parts (Arms, Legs, Back…) sorted by volume,
    // most-trained first.
    val overviewItems: List<Pair<String, Int>> = topicDistribution.entries
        .sortedByDescending { it.value }
        .map { it.key to it.value }

    // Detailed breakdown: precise muscle groups grouped into sections per
    // broad body part. Sections are ordered by trained volume (most first),
    // not by a fixed group-name list; rows inside each section are sorted by
    // volume too. Shown in the detail dialog.
    val detailedSections: List<Pair<String, List<Pair<String, Int>>>> = remember(normalized) {
        normalized.entries
            .groupBy { WorkoutTemplates.getTopicForMuscle(it.key) }
            .map { (topic, members) ->
                topic to members.sortedByDescending { it.value }.map { it.key to it.value }
            }
            .sortedByDescending { (_, members) -> members.sumOf { it.second } }
    }

    // Whole-number legend percentages that always sum to exactly 100%
    // (largest-remainder method): floor every share, then give the leftover
    // percentage points to the entries with the largest fractional remainders.
    val percentages: Map<String, Int> = run {
        val raw = overviewItems.map { (key, value) ->
            key to (value.toFloat() / total.toFloat()) * 100f
        }
        val floored = raw.associate { (key, value) -> key to value.toInt() }.toMutableMap()
        var leftover = 100 - floored.values.sum()
        raw.sortedByDescending { it.second - it.second.toInt() }
            .forEach { (key, value) ->
                if (leftover > 0) {
                    floored[key] = floored.getValue(key) + 1
                    leftover--
                }
            }
        floored
    }

    // Palette: one color per broad body part from the app's usual muscle-group
    // mapping, drawn at full strength so the arcs, dots and legend labels stay
    // light and vibrant on the card surface. Keyed by group so the arcs and
    // legend dots always stay in sync.
    // Colours are resolved once, in a composable context, so the Canvas, the arcs
    // and the legend all use the exact same tone. Using the theme-aware
    // [muscleGroupColor] keeps them identical to the muscle chips on Home and My
    // Workout instead of a lighter, washed-out variant on a light theme.
    val groupColors: Map<String, Color> = overviewItems.map { it.first }.distinct().associateWith { key ->
        muscleGroupColor(WorkoutTemplates.getTopicForMuscle(key))
    }

    fun entryColor(key: String): Color = groupColors[key] ?: getMuscleGroupColor("Other")

    @Composable
    fun LegendRow(
        key: String,
        modifier: Modifier = Modifier,
        showDot: Boolean = true,
        label: String? = null
    ) {
        val percentage = percentages[key] ?: 0
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showDot) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(entryColor(key))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = label ?: formatMuscleGroup(key),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    // Same hue family as the dot beside it, but read as TEXT:
                    // Home's chips use the deep 600-step on light themes for
                    // legibility (yellow-400 text on white is ~1.6:1) — the
                    // legend now matches them hue-for-hue and shade-for-shade,
                    // which is the consistency the owner asked for.
                    color = muscleGroupTextColor(key),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = null,
        modifier = modifier.fillMaxWidth()
    ) {
        // Fills the card's (possibly stretched) height so the content can
        // distribute itself across the full card instead of hugging the top.
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header mirrors the Week Metrics card: small primary label above a
            // big bold title, with a compact action on the right (there: week
            // navigator, here: the detail-view opener — top placement keeps it
            // always visible instead of clipped at the card's bottom edge).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = getString("muscle_group_focus"),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = getString("muscle_distribution"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (distribution.isNotEmpty()) {
                    Surface(
                        onClick = { showDetailDialog = true },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = getString("detailed_view"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (distribution.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getString("no_sets_logged_full"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Chart row — expands into the card's leftover height so the
                // extra room from the stretched card is absorbed here: the
                // header stays top-anchored and the donut/legend can breathe.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                ) {
                    // Donut Canvas — scales with the space the taller card
                    // provides (capped so it never dwarfs the legend).
                    BoxWithConstraints(contentAlignment = Alignment.Center) {
                        val donutSize = minOf(maxHeight, maxWidth).coerceIn(96.dp, 150.dp)
                        Box(
                            modifier = Modifier
                                .size(donutSize)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var startAngle = -90f
                                val strokeWidth = 20.dp.toPx()
                                // Inset the ring by half the stroke so the arcs
                                // always stay inside the canvas — no overhang
                                // that a parent clip can slice mid-stroke.
                                val inset = strokeWidth / 2f
                                val arcTopLeft = Offset(inset, inset)
                                val arcSize = Size(
                                    this.size.width - strokeWidth,
                                    this.size.height - strokeWidth
                                )

                                overviewItems.forEach { (key, value) ->
                                    val sweepAngle = (value.toFloat() / total.toFloat()) * 360f

                                    drawArc(
                                        color = entryColor(key),
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        topLeft = arcTopLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweepAngle
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$total",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = getString("sets_label"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Legend — vertically centered so it lines up with the
                    // donut; rows gain even spacing and scroll if they ever
                    // exceed the available height.
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        overviewItems.forEach { (key, _) ->
                            LegendRow(key)
                        }
                    }
                }
            }
        }
    }

    if (showDetailDialog) {
        MuscleGroupDetailDialog(
            detailedSections = detailedSections,
            total = total,
            onDismiss = { showDetailDialog = false }
        )
    }
}

/**
 * Full tree-style breakdown of every precise muscle group, opened from the
 * Muscle Group Focus card's detail button. Sections are broad body parts in
 * the shared group order; rows inside each section are precise muscles with
 * their real per-group colors (same mapping the workout screens use), not
 * theme-matched shades.
 *
 * The rows are the chart: each precise muscle carries a bar of its own, so the
 * breakdown reads as a bar chart without the numbers having to move out of the
 * tree. A donut is deliberately not repeated here — the overview card already
 * answers "how do the body parts split up", while bar length answers "which
 * muscles carry that volume, and by how much".
 */
@Composable
private fun MuscleGroupDetailDialog(
    detailedSections: List<Pair<String, List<Pair<String, Int>>>>,
    total: Int,
    onDismiss: () -> Unit
) {
    val isDeveloper = LocalDeveloperSkin.current

    // Position of each muscle in the flattened list, used to draw the bars in top-to-bottom.
    val rowRanks = remember(detailedSections) {
        detailedSections
            .flatMap { (_, members) -> members }
            .mapIndexed { index, (muscleKey, _) -> muscleKey to index }
            .toMap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = getString("muscle_group_focus"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = getString("sets_label") + ": $total",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                detailedSections.forEachIndexed { index, (_, members) ->
                    val headerKey = members.first().first
                    val sectionColor = muscleGroupColor(WorkoutTemplates.getTopicForMuscle(headerKey))
                    // Same contrast rule as the badge: the header text sits on a
                    // light tint, so on light themes it reads in the deeper tone.
                    val sectionTextColor = muscleGroupTextColor(WorkoutTemplates.getTopicForMuscle(headerKey))
                    // Broad body-part header chip with the section's total sets.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                sectionColor.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(sectionColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = translateMuscleGroup(WorkoutTemplates.getTopicForMuscle(headerKey)).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = sectionTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    members.forEach { (muscleKey, sets) ->
                        MuscleVolumeRow(
                            muscleKey = muscleKey,
                            sets = sets,
                            total = total,
                            rank = rowRanks[muscleKey] ?: 0
                        )
                    }
                    if (index < detailedSections.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = getString("close"))
            }
        }
    )
}

/** Stagger between two bars drawing in, and the ceiling that keeps the cascade brief. */
private const val MUSCLE_BAR_STAGGER_MILLIS = 22
private const val MUSCLE_BAR_MAX_STAGGER_MILLIS = 260

/** How long a single bar takes to reach its length. */
private const val MUSCLE_BAR_GROW_MILLIS = 420

private val MUSCLE_BAR_HEIGHT = 5.dp

/**
 * One muscle in the detailed breakdown: its name and share, over a bar as long as its share
 * of [total] — the same 100% the percentage next to it is taken from, so the longest bar is
 * the biggest share and a full track always means the whole breakdown. The bar makes the
 * volume readable at a glance where two percentages a point apart would not, and keeps the
 * exact sets and percentage in place next to it.
 */
@Composable
private fun MuscleVolumeRow(
    muscleKey: String,
    sets: Int,
    total: Int,
    rank: Int
) {
    val isDeveloper = LocalDeveloperSkin.current
    val muscleColor = muscleGroupColor(muscleKey)
    val barRadius = 3.dp
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)

    // Bars cascade from the top so the shape of the breakdown arrives before the eye has to
    // scan for it, without holding the numbers back: they are on screen from the start.
    var grown by remember(muscleKey) { mutableStateOf(false) }
    LaunchedEffect(muscleKey) {
        delay((rank * MUSCLE_BAR_STAGGER_MILLIS).coerceAtMost(MUSCLE_BAR_MAX_STAGGER_MILLIS).toLong())
        grown = true
    }
    val fraction by animateFloatAsState(
        targetValue = if (grown && total > 0) {
            (sets.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = tween(durationMillis = MUSCLE_BAR_GROW_MILLIS, easing = FastOutSlowInEasing),
        label = "muscleVolume"
    )
    val share = if (total > 0) (sets.toFloat() / total.toFloat() * 100f).roundToInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(muscleColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = translateMuscleGroup(muscleKey),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$sets · $share%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Track plus fill, drawn rather than re-laid out: the growing fraction is read in the
        // draw pass, so animating 25 bars costs redraws instead of a recomposition a frame.
        // The track holds the full scale open, so a short bar still reads as a small share
        // rather than as a missing row.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(MUSCLE_BAR_HEIGHT)
        ) {
            val radius = barRadius.toPx()
            val corners = CornerRadius(radius, radius)
            drawRoundRect(color = trackColor, cornerRadius = corners)
            val filled = size.width * fraction
            if (filled > 0f) {
                drawRoundRect(
                    color = muscleColor,
                    size = Size(filled, size.height),
                    cornerRadius = corners
                )
            }
        }
    }
}

fun getUpperCategoryKey(muscleGroup: String): String {
    val mg = muscleGroup.lowercase().trim()
    return when {
        // "pectorals" is the precise group the primary-muscle table stores; it must
        // land on the same broad key as "chest" or a chest day reads as "Other".
        mg.contains("chest") || mg.contains("pectoral") -> "chest"
        mg.contains("back") || (mg.contains("lat") && !mg.contains("lateral")) || mg.contains("trap") || mg.contains("erector") -> "back"
        mg.contains("leg") || mg.contains("quad") || mg.contains("hamstring") || mg.contains("glute") || mg.contains("calve") || mg.contains("adductor") || mg.contains("abductor") -> "legs"
        mg.contains("shoulder") || mg.contains("delt") -> "shoulders"
        mg.contains("bicep") || mg.contains("tricep") || mg.contains("arm") || mg.contains("forearm") -> "arms"
        mg.contains("abs") || mg.contains("core") || mg.contains("oblique") || mg.contains("abdominal") -> "core"
        else -> "other"
    }
}

@Composable
fun RepsTimeToggle(
    isTimeBased: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 40.dp.toPx() }

    val focusManager = LocalFocusManager.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .pointerInput(isTimeBased) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDrag > swipeThreshold) {
                            if (!isTimeBased) onToggle(true)
                        } else if (totalDrag < -swipeThreshold) {
                            if (isTimeBased) onToggle(false)
                        }
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                )
            }
    ) {
        val tWidth = this.maxWidth / 2
        val indicatorPosition by animateDpAsState(
            targetValue = if (isTimeBased) tWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "indicatorPosition"
        )

        // Sliding Indicator
        val density = LocalDensity.current
        Surface(
            modifier = Modifier
                .width(tWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = with(density) { indicatorPosition.toPx() }
                },
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(7.dp),
            shadowElevation = 1.dp) {}

        Row(modifier = Modifier.fillMaxSize()) {
            // Reps Tab
            val repsColor by animateColorAsState(
                targetValue = if (!isTimeBased) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "repsColor"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        onToggle(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getString("reps"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = repsColor
                )
            }

            // Time Tab
            val timeColor by animateColorAsState(
                targetValue = if (isTimeBased) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "timeColor"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        onToggle(true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getString("time_label"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = timeColor
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableExerciseField(
    allExercises: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    onItemSelected: (String) -> Unit = {},
    label: String,
    placeholder: String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    // Every pool exercise is searchable under its canonical name *and* its translations, so
    // a German user typing "Kurzhantel-Curls" finds the exercise that is stored as
    // "Dumbbell Curls". The candidate keeps that canonical name, which is what a selection
    // hands back — the query language can never fork an exercise into two records.
    val candidates = remember(allExercises) {
        allExercises.map { name ->
            ExerciseSearch.Candidate(name, ExerciseLocalization.allNamesFor(name))
        }
    }

    // The normalisation of every searchable name (lowercase / compact / split) does
    // not depend on what is typed, so it is done once per pool instead of once per
    // keystroke. Measured 19ms -> 8ms per keystroke on the JVM; on a phone that is
    // several times more, and it runs on the main thread inside composition.
    // Ranking is unchanged - `thePreparedIndexReturnsExactlyWhatTheDirectSearchDoes`
    // asserts the two agree name-for-name and order-for-order.
    val searchIndex = remember(candidates) { ExerciseSearch.PreparedIndex.build(candidates) }

    // Typo-tolerant search: still finds "Dumbbell Curls" from "dumbell curls"
    // or "Bench Press" from "bnech pres". Exact/prefix matches stay on top.
    // A BLANK query shows the pool itself (popularity-ordered, already filtered
    // by the caller): the owner's "suggestions have to ACTUALLY SUGGEST AND
    // SHOW EXERCISES" — the dropdown used to stay empty until a letter was
    // typed, because searchCandidates treats an empty query as "no matches".
    val filteredExercises = remember(value, searchIndex) {
        if (value.isBlank()) candidates
        else ExerciseSearch.searchPrepared(searchIndex, value)
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredExercises.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            // Shown localized, stored canonical: picking "Kniebeugen" from the German list
            // leaves the field reading "Kniebeugen" rather than flipping to "Squats". While
            // typing this is a no-op, because the text only resolves once it names a known
            // exercise - and then to its own name in the current language.
            value = translateExerciseName(value),
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            singleLine = true,
            // Opening on FOCUS is what makes the box suggest anything: a blank
            // query already resolves to the whole pool, but the menu only ever
            // opened from onValueChange, so a user who taps the field and looks
            // for suggestions saw an empty box and concluded there were none.
            // Losing focus closes it again, so a tap elsewhere in the dialog
            // does not leave the list hanging over the form.
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    expanded = if (state.isFocused) filteredExercises.isNotEmpty() else false
                }
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(10.dp)
        )

        if (filteredExercises.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .heightIn(max = 280.dp)
            ) {
                filteredExercises.forEach { selection ->
                    // The row shows the localized name and description in the current language,
                    // while `onItemSelected` still receives the canonical name.
                    val canonical = selection.canonical
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    translateExerciseName(canonical),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                // Avoid calling heavy functions inside Composable if possible, but this is a small list
                                val muscleGroup = remember(canonical) { WorkoutTemplates.getMuscleGroupForExercise(canonical) }
                                val (desc, _) = getExerciseDetails(canonical, muscleGroup, "")
                                if (desc.isNotBlank()) {
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        onClick = {
                            onValueChange(canonical)
                            onItemSelected(canonical)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}


/**
 * One muscle group's standing in the generated week, against its healthy
 * weekly RANGE. Owner, 2026-10-06: the card used to cry "Unbalanced Workout
 * Routine" whenever a group had zero exercises — which fired on every
 * engine week under seven days (no direct core) even when the user had
 * filtered nothing out. The verdict is now a range with numbers and days,
 * not a binary presence check.
 *
 * [days] holds the day-of-week keys the group actually trains on, so the
 * message can say which days as well as how many exercises.
 */
data class WeeklyBalanceIssue(
    val groupKey: String,
    val perWeek: Int,
    val days: List<String>,
    val healthyMin: Int,
    val healthyMax: Int
) {
    val isUndertrained: Boolean get() = perWeek < healthyMin
    val isOvertrained: Boolean get() = perWeek > healthyMax
}

/**
 * The seven balance groups: key, the muscleGroup spellings that count, and the
 * healthy weekly range of DIRECT exercises. The ranges are anchored on the
 * generator itself, because the owner's 2026-10-06 report was a FALSE
 * "unbalanced" verdict on a week nothing had been filtered out of. Measured
 * over all 315 no-filter engine weeks (1-7 days x three session windows x
 * five goals x three difficulties): the thinnest week the templates produce
 * is 1 chest / 1 back / 3 legs and 0 shoulders, arms and core — the prompt's
 * full-body table has no direct core, and a short strength session fits no
 * arm isolation — so those are the floors. The ceiling sits just above the
 * THICKEST template week (6 chest / 12 back / 24 legs / 18 shoulders / 12
 * arms / 8 core, at 6-7 days x full window); past it is junk volume the
 * evidence (~10-20 hard sets per muscle per week, Schoenfeld; ~3 working
 * sets per exercise) does not support.
 *
 * A floor of 0 is honest, not a shrug: a default week may legitimately
 * contain none of those groups, so "none" alone is not a defect — a pile of
 * them past the ceiling still is. The multi-head aggregates (legs = quads +
 * hams + glutes + calves + hips; shoulders = three heads) carry the widest
 * ranges because the count is over the aggregate, not per head.
 */
private val BALANCE_GROUPS: List<Triple<String, List<String>, IntRange>> = listOf(
    // "pectoral" covers the precise "Pectorals" the primary-muscle table stores.
    Triple("chest", listOf("chest", "pectoral"), 1..9),
    // "erectors" sits with BACK here, matching the advanced tab's own taxonomy
    // (Spinal Erectors is filed under Back there), so scope and count agree.
    Triple("back", listOf("back", "lats", "upper back", "traps", "erectors"), 1..14),
    Triple("legs", listOf("legs", "quads", "quadriceps", "hamstrings", "calves", "glutes", "adductors", "abductors"), 3..24),
    Triple("shoulders", listOf("shoulders", "delt", "deltoid"), 0..20),
    Triple("biceps", listOf("biceps", "bicep"), 0..12),
    Triple("triceps", listOf("triceps", "tricep"), 0..12),
    Triple("core", listOf("abs", "core", "abdominals", "obliques"), 0..10)
)

/** Every balance-group key — the DEFAULT scope ("all unless the user narrows it"). */
val BALANCE_GROUP_KEYS: List<String> = BALANCE_GROUPS.map { it.first }

/** Day-of-week key -> the abbreviation the balance lines print (locale-neutral, like the weekday chips). */
private val DAY_ABBREV = mapOf(
    "MONDAY" to "Mon", "TUESDAY" to "Tue", "WEDNESDAY" to "Wed", "THURSDAY" to "Thu",
    "FRIDAY" to "Fri", "SATURDAY" to "Sat", "SUNDAY" to "Sun"
)

/**
 * Maps the advanced tab's body-part selection onto balance-group keys. The
 * DEFAULT selection is every part, which maps to every group — so callers
 * pass the selection straight through and "narrowed by the user" falls out
 * of the data rather than a separate touched-flag. Forearms have no balance
 * group and are ignored; an empty selection means unrestricted → all groups.
 */
fun balanceGroupsForParts(parts: Collection<String>): Set<String> {
    if (parts.isEmpty()) return BALANCE_GROUP_KEYS.toSet()
    val lowered = parts.map { it.lowercase().trim() }.toSet()
    return BALANCE_GROUPS.filter { (_, keywords, _) ->
        keywords.any { kw -> lowered.any { it == kw || it.contains(kw) } }
    }.map { it.first }.toSet()
}

/**
 * The weekly balance report: for every group in [scopeGroups] (null = all),
 * the exercises per week and the days they land on, kept only when the
 * count falls OUTSIDE the group's healthy range. In-range groups are
 * silence — the green card is what "everything fits" looks like. See
 * [BALANCE_GROUPS] for where the ranges come from and why a floor of 0 is
 * the honest number for four of the seven groups.
 */
fun weeklyBalanceIssues(
    exercises: List<RoutineExerciseEntity>,
    scopeGroups: Collection<String>? = null
): List<WeeklyBalanceIssue> {
    val scope = (scopeGroups ?: BALANCE_GROUP_KEYS).toSet()
    return BALANCE_GROUPS.mapNotNull { (key, keywords, range) ->
        if (key !in scope) return@mapNotNull null
        val hits = exercises.filter { ex ->
            val muscle = ex.muscleGroup.lowercase().trim()
            keywords.any { muscle.contains(it) }
        }
        WeeklyBalanceIssue(
            groupKey = key,
            perWeek = hits.size,
            days = hits.map { it.dayOfWeek }.distinct().sortedBy { DAY_ORDER.indexOf(it) },
            healthyMin = range.first,
            healthyMax = range.last
        ).takeIf { it.isUndertrained || it.isOvertrained }
    }
}

private val DAY_ORDER = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")

/**
 * How long a toast stays up, and how wide it may get.
 *
 * The owner asked for 1.5 s and for a SMALL popup rather than the full-width
 * banner this used to be: `fillMaxWidth()` made a two-word refusal ("press the
 * pencil first") arrive as a full-screen slab that reflowed the card it was
 * reporting on. `TOAST_WIDTH_FRACTION` caps it so a long sentence wraps inside
 * a popup instead of stretching edge to edge on a tablet.
 */
private const val TOAST_VISIBLE_MS = 1500L
private const val TOAST_WIDTH_FRACTION = 0.8f

@Composable
fun ThemedToast(
    message: String,
    themeKey: String,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (visible) {
        LaunchedEffect(visible) {
            delay(TOAST_VISIBLE_MS)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(10.dp),
                // A visible red rim is the point: this is a refusal, not an
                // informational note, and at 20% opacity the old border read as
                // a soft grey outline on the error container.
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(TOAST_WIDTH_FRACTION)
                    .widthIn(max = 360.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        // Decorative: the toast's own text carries the meaning,
                        // so a screen reader must not read the icon as a second
                        // "error" on top of it.
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MissingBodyPartsWarningCard(
    issues: List<WeeklyBalanceIssue>,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    if (issues.isEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF10B981)),
            border = null,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = getString("balanced_workout_routine"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = getString("balanced_workout_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
            border = null,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = getString("incomplete_workout_routine"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Text(
                        text = getString("incomplete_workout_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    // One line per out-of-range group: the number of exercises,
                    // the days it lands on, and the healthy range — the owner's
                    // "healthy amount of exercises per week on which days and
                    // which muscle groups", said with numbers instead of a
                    // scare headline. A group with NO exercises has no days to
                    // name, so it takes the shorter format rather than a
                    // dangling placeholder.
                    issues.forEach { issue ->
                        val text = if (issue.days.isEmpty()) {
                            getString(
                                "balance_issue_line_no_days",
                                translateMuscleGroup(issue.groupKey),
                                issue.perWeek,
                                issue.healthyMin,
                                issue.healthyMax
                            )
                        } else {
                            getString(
                                "balance_issue_line",
                                translateMuscleGroup(issue.groupKey),
                                issue.perWeek,
                                issue.days.joinToString(", ") { DAY_ABBREV[it] ?: it },
                                issue.healthyMin,
                                issue.healthyMax
                            )
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    /**
     * False greys the field out and blocks input: used by the routine editor when
     * the level system prescribes this exercise and the user must switch to
     * custom mode to change the value.
     */
    enabled: Boolean = true,
    /**
     * Called when a DISABLED field is tapped. A disabled text field swallows the
     * tap, so without this the greyed box just does nothing and the user is left
     * guessing; the editor passes a short notice here instead.
     */
    onLockedTap: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    placeholder: String? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .pointerInput(enabled) {
                if (enabled) return@pointerInput
                detectTapGestures { onLockedTap?.invoke() }
            },
        interactionSource = interactionSource,
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start, fontSize = 16.sp),
        keyboardOptions = keyboardOptions,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                interactionSource = interactionSource,
                isError = isError,
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                placeholder = if (placeholder != null) { { Text(placeholder, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) } } else null,
                suffix = suffix,
                supportingText = supportingText,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    start = 16.dp,
                    end = 12.dp
                ),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        focusedBorderThickness = 2.dp,
                        unfocusedBorderThickness = 1.dp
                    )
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepsRangeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    /**
     * False greys the field out and blocks both focus and input: used by the
     * routine editor when the level system prescribes this exercise.
     */
    enabled: Boolean = true,
    /** See [CompactNumericField]. */
    onLockedTap: (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null
) {
    val parts = value.split("-", "–")
    val minVal = parts.getOrNull(0) ?: ""
    val maxVal = parts.getOrNull(1) ?: ""

    var minReps by remember(minVal) { mutableStateOf(minVal) }
    var maxReps by remember(maxVal) { mutableStateOf(maxVal) }

    // What the LOCKED field prints, decided once so the label can never leak
    // into the value slot.
    //
    // The owner's report was "fix its format so that the reps placeholder also
    // moves away and doesnt stay glitched inside the input box". Two things
    // were wrong and they are one bug: the locked branch printed
    // `value.ifBlank { getString("reps") }`, so a row with no prescribed reps
    // rendered the word "Reps" AS ITS VALUE - the label was being used as
    // content, and it sat in the middle of the box instead of floating on the
    // border where Material puts a label. And the DecorationBox below was
    // handed `""` whenever the field was not expanded, so the label never
    // floated at all: "Reps" appeared twice, once parked mid-box and once as
    // the outline label.
    //
    // So: the locked row shows the VALUE or an em dash, never the label, and
    // the DecorationBox is handed a non-blank string whenever there is
    // something to show, which is what makes Material float the label.
    val lockedDisplay = repsLockedDisplay(value)

    val minFocusRequester = remember { FocusRequester() }
    val maxFocusRequester = remember { FocusRequester() }
    var isMinFocused by remember { mutableStateOf(false) }
    var isMaxFocused by remember { mutableStateOf(false) }

    val isAnyFocused = isMinFocused || isMaxFocused
    val hasContent = minReps.isNotEmpty() || maxReps.isNotEmpty()
    val isExpanded = (isAnyFocused || hasContent || isError) && enabled

    fun notifyChange(min: String, max: String) {
        if (!enabled) return
        val result = when {
            min.isNotBlank() && max.isNotBlank() -> "$min-$max"
            min.isNotBlank() -> min
            max.isNotBlank() -> max
            else -> ""
        }
        onValueChange(result)
    }

    val interactionSource = remember { MutableInteractionSource() }

    // Material floats the label as soon as the DecorationBox's `value` is
    // non-blank. It was `if (isExpanded) " " else ""`, which is blank whenever
    // the field is locked - so the label stayed parked mid-box over the value.
    // One predicate now decides it: show SOMETHING whenever there is something
    // to show.
    val decorationValue = if (enabled) (if (isExpanded) " " else "") else " "

    BasicTextField(
        value = decorationValue,
        onValueChange = {},
        // enabled=false greys the decoration out and stops the pointerInput below
        // from handing out focus, so a disabled field cannot be typed into.
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .pointerInput(enabled) {
                if (!enabled) {
                    // Locked: the tap cannot open the editor, but it is not
                    // swallowed either - the notice is the whole point of
                    // greying the box out.
                    detectTapGestures { onLockedTap?.invoke() }
                    return@pointerInput
                }
                detectTapGestures { offset ->
                    if (isExpanded) {
                        if (offset.x < size.width / 2) minFocusRequester.requestFocus()
                        else maxFocusRequester.requestFocus()
                    } else {
                        minFocusRequester.requestFocus()
                    }
                }
            },
        readOnly = true,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Start,
            fontSize = 16.sp
        ),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = decorationValue,
                innerTextField = {
                    // A LOCKED reps field shows the WHOLE value, as one line.
                    //
                    // `isExpanded` is forced false when the field is disabled
                    // (it gates the min/max editor), and the max half and the
                    // "-" separator were gated on `isExpanded` too - so greying
                    // a "8-12" field out rendered just "8". The value the level
                    // system prescribes was therefore lost from a box whose
                    // whole job is to show it. Editing is what needs the split;
                    // reading does not, so the split is only for editing.
                    if (!enabled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                        ) {
                            Text(
                                text = lockedDisplay,
                                style = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontSize = 16.sp,
                                    // Material greys a disabled decoration; the
                                    // value has to follow it or a locked field
                                    // reads as ENABLED and looks editable.
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        return@DecorationBox
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().fillMaxHeight().clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (enabled && !isExpanded) minFocusRequester.requestFocus()
                        }
                    ) {
                        // MIN FIELD
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { if (enabled) minFocusRequester.requestFocus() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (minReps.isEmpty() && isExpanded) {
                                Text(
                                    text = "min",
                                    style = LocalTextStyle.current.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            BasicTextField(
                                value = minReps,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) {
                                        minReps = it
                                        notifyChange(it, maxReps)
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontSize = 16.sp
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(minFocusRequester)
                                    .onFocusChanged { isMinFocused = it.isFocused }
                            )
                        }

                        // SEPARATOR
                        if (isExpanded) {
                            Text(
                                text = "-",
                                style = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    // Tap on separator focuses the min field if empty, or max if min has content
                                    if (enabled) {
                                        if (minReps.isEmpty()) minFocusRequester.requestFocus()
                                        else maxFocusRequester.requestFocus()
                                    }
                                }
                            )
                        }

                        // MAX FIELD
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { if (isExpanded) maxFocusRequester.requestFocus() else if (enabled) minFocusRequester.requestFocus() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (maxReps.isEmpty() && isExpanded) {
                                Text(
                                    text = "max",
                                    style = LocalTextStyle.current.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            if (isExpanded) {
                                BasicTextField(
                                    value = maxReps,
                                    onValueChange = {
                                        if (it.all { c -> c.isDigit() }) {
                                            maxReps = it
                                            notifyChange(minReps, it)
                                        }
                                    },
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(maxFocusRequester)
                                        .onFocusChanged { isMaxFocused = it.isFocused }
                                )
                            }
                        }
                    }
                },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                enabled = enabled,
                isError = isError,
                singleLine = true,
                supportingText = supportingText,
                interactionSource = interactionSource,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(),
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        focusedBorderThickness = 2.dp,
                        unfocusedBorderThickness = 1.dp
                    )
                }
            )
        }
    )
}

/**
 * What a LOCKED reps field prints inside its box.
 *
 * The rule is that the LABEL is never the value. A greyed-out field with no
 * prescribed reps used to print the word "Reps" as though the user had been
 * prescribed eighty reps, and because the DecorationBox was handed a blank
 * value at the same time, that word also stayed parked in the middle of the box
 * instead of floating onto the outline - the owner's "the reps placeholder
 * also moves away and doesnt stay glitched inside the input box".
 *
 * An em dash is the honest answer for "nothing prescribed here": it is what the
 * rest of the app prints for an absent measurement (`overall_level_none` is
 * exactly this character), so the box reads as empty rather than as a value.
 *
 * Pure, and in [WorkoutComponents.kt] rather than at the call site, because a
 * Compose field's rendering is not reachable from a unit test - this is the part
 * that can be.
 */
fun repsLockedDisplay(value: String): String {
    val clean = value.replace('-', '–').trim()
    // "No digits" rather than "blank text": a value that is just a separator
    // ("-" or "–") is not blank, so a blank check would print the dash INSIDE
    // the box as if it were a measurement. There is no rep count in it.
    return if (clean.none { it.isDigit() }) "—" else clean
}

/**
 * The band a `resistance_step` value names, in the user's language. Bands are sold
 * by colour and felt, never by number, so the ladder's 1..10 is rendered as the
 * five tensions a user can actually pick up.
 */
@Composable
fun bandTensionLabel(tension: ProgressionEngine.BandTension): String = when (tension) {
    ProgressionEngine.BandTension.VERY_LIGHT -> getString("band_tension_very_light")
    ProgressionEngine.BandTension.LIGHT -> getString("band_tension_light")
    ProgressionEngine.BandTension.MEDIUM -> getString("band_tension_medium")
    ProgressionEngine.BandTension.HEAVY -> getString("band_tension_heavy")
    ProgressionEngine.BandTension.VERY_HEAVY -> getString("band_tension_very_heavy")
}

/**
 * The label the load field carries for an exercise. Most are loaded with weight,
 * so the default is "Weight"/"kg" — but a selectorized stack and a resistance
 * band are NOT weights: a pin means a different resistance on every machine and a
 * band loses tension as it stretches. Labelling those rows "kg" asked the user to
 * invent a number (dashboard task "lots of exercises have steps instead of weight").
 */
@Composable
fun loadFieldLabel(exerciseName: String): String =
    when (ProgressionEngine.loadUnitFor(exerciseName)) {
        ProgressionEngine.LoadUnit.STACK_STEP -> getString("load_unit_pin")
        ProgressionEngine.LoadUnit.RESISTANCE_STEP -> getString("load_unit_band")
        else -> getString("weight")
    }

/** The unit printed inside the load field. Empty for the step units: they are a count, not a measure. */
@Composable
fun loadFieldSuffix(exerciseName: String): String =
    when (ProgressionEngine.loadUnitFor(exerciseName)) {
        ProgressionEngine.LoadUnit.STACK_STEP,
        ProgressionEngine.LoadUnit.RESISTANCE_STEP -> ""
        else -> if (LocalWeightUnit.current == WeightUnit.KG) "kg" else "lb"
    }

/**
 * The unit the user reads and enters weights in.
 *
 * A CompositionLocal rather than a parameter because a load becomes a string in
 * a dozen places — the level picker, the live card, both exercise editors, the
 * logged-set summary, the evolution popup — and threading a unit through all of
 * them by hand is how one of them ends up printing kilograms to a US user.
 * Storage stays KILOGRAMS, like the exercise pool storing canonical English
 * names: this decides only how a weight is written down and read back.
 */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }

/** The row's own kilogram increment, or null for a row outside the level system. */
private fun loadStepKg(exerciseName: String): Double? =
    ProgressionEngine.entryFor(exerciseName)?.increment

/**
 * True for a row whose load is a weight; a pin or a band rung is a count.
 *
 * An unknown row (loadUnitFor returns null) counts as a WEIGHT: that is the
 * default a user's own custom exercise gets, and reading it as a count is how a
 * field stops showing "20.0".
 */
private fun isWeightLoaded(exerciseName: String): Boolean =
    when (ProgressionEngine.loadUnitFor(exerciseName)) {
        ProgressionEngine.LoadUnit.STACK_STEP,
        ProgressionEngine.LoadUnit.RESISTANCE_STEP -> false
        else -> true
    }

/**
 * A stored KILOGRAM load as [unit] should read it, snapped onto that unit's own
 * grid: 40 kg is 88 lb, and a 2.5 kg barbell row moves in 5 lb rungs rather than
 * 5.51 — the pounds a US gym actually stocks, taken from the row's own
 * increment. A pin or a band rung is not a weight and is returned untouched,
 * because "pin 6" is not thirteen pounds.
 */
fun displayLoadForUnit(exerciseName: String, kg: Double, unit: WeightUnit): Double =
    if (unit == WeightUnit.KG || !isWeightLoaded(exerciseName)) kg
    else unit.display(kg, loadStepKg(exerciseName))

/**
 * The KILOGRAM value to store for what the user typed in their own unit, or null
 * when the field is not a number. The stored rows never change unit, so the
 * conversion happens exactly once, here.
 */
fun loadInputToKg(exerciseName: String, text: String, unit: WeightUnit): Double? {
    val typed = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (unit == WeightUnit.KG || !isWeightLoaded(exerciseName)) return typed
    return unit.toKg(typed)
}

/**
 * A load value as the user should read it: rounded to at most two decimals, with
 * a trailing ".0" dropped.
 *
 * The engine's ceiling-stretch math spreads a ladder's remaining growth over its
 * extra rungs, so it produces values like 40.45454545454545. Printing the raw
 * Double put sixteen digits into the level picker — and the same noise into the
 * live card and every logged set — for a number no user can act on. Two decimals
 * keeps a real prescription intact (2.5, 62.5 and 1.25 kg all survive) while
 * collapsing the interpolation noise to something readable.
 *
 * This is the ONE place a load becomes a string. A count (a pin, a band) and a
 * weight share it so the card, the picker and the log cannot disagree.
 */
fun formatLoadValue(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

/**
 * The stored load as the load field should print it.
 *
 * The field was seeded with `Double.toString()`, which is right for weight ("2.5")
 * and wrong for a COUNT: pin 20 arrived as "20.0", and the placeholder said "0.0"
 * too, so a stack row asked for a number with a decimal point on a machine whose
 * plate stack has no such thing. [loggedLoadSummary] already dropped the ".0" on
 * the way out; this is the same rule on the way in, and the two now agree because
 * a field that reads back differently from the card that printed it is its own
 * small lie.
 *
 * The value is formatted through [formatLoadValue], so a fraction the user typed
 * is preserved up to two decimals and only the engine's interpolation noise is
 * trimmed. A kg field keeps one decimal even when whole ("20.0" is what a weight
 * field shows); a count does not.
 */
fun loadFieldValueText(
    exerciseName: String,
    value: Double,
    unit: WeightUnit = WeightUnit.KG
): String {
    if (value <= 0.0) return ""
    val trimmed = formatLoadValue(displayLoadForUnit(exerciseName, value, unit))
    if (!isWeightLoaded(exerciseName)) return trimmed
    // A weight field keeps its decimal: "20.0", not "20".
    return if (trimmed.contains('.')) trimmed else "$trimmed.0"
}

/** The empty-field hint, in the exercise's own unit: a count is not written "0.0". */
fun loadFieldPlaceholder(exerciseName: String): String =
    when (ProgressionEngine.loadUnitFor(exerciseName)) {
        ProgressionEngine.LoadUnit.STACK_STEP,
        ProgressionEngine.LoadUnit.RESISTANCE_STEP -> "0"
        else -> "0.0"
    }

/**
 * How a logged set's own load reads, in the exercise's own unit: "20 kg", "Pin 6",
 * "Medium band". The value stored is whatever the user typed, so a stack's pin
 * number is printed back as a pin, not as a kilogram.
 */
@Composable
fun loggedLoadSummary(exerciseName: String, value: Double): String {
    if (value <= 0.0) return ""
    val unit = LocalWeightUnit.current
    val clean = formatLoadValue(displayLoadForUnit(exerciseName, value, unit))
    return when (ProgressionEngine.loadUnitFor(exerciseName)) {
        ProgressionEngine.LoadUnit.STACK_STEP -> getString("progression_load_pin", clean)
        ProgressionEngine.LoadUnit.RESISTANCE_STEP -> getString(
            "progression_load_band",
            bandTensionLabel(ProgressionEngine.bandTensionOf(value))
        )
        else -> getString(
            if (unit == WeightUnit.KG) "progression_load_kg" else "progression_load_lb",
            clean
        )
    }
}

/**
 * What the exercise editor's LEVEL row is allowed to do.
 *
 * Owner, 2026-10-05: "also the editing of an exercise, allow the user to edit
 * the level while in level system mode". Until then the editor greyed out every
 * value the level owns and offered exactly one way out — "switch to Custom" —
 * which reads as *this exercise cannot be edited here* rather than as *these
 * three numbers belong to the level*. The level itself is the one thing in that
 * dialog the user is entitled to move, so it gets its own control.
 *
 * It is one pure function for the same reason [levelEditControls] is: the row's
 * visibility and its three affordances were four conditions, and four conditions
 * in a composable are four chances for the row to appear with its arrows dead.
 */
data class LevelEditorControls(
    val showLevelRow: Boolean,
    val canStepDown: Boolean,
    val canStepUp: Boolean,
    val canOpenLadder: Boolean
)

fun levelEditorControls(
    levelsManaged: Boolean,
    currentLevel: Int?,
    maxLevel: Int,
    canWriteLevel: Boolean
): LevelEditorControls {
    // ADDING AN EXERCISE IS NOT OUTSIDE THE LEVEL SYSTEM, and this function used to
    // say it was. `isExistingExercise` gated the row on `exercise != null`, whose
    // stated reason was "a NEW exercise has no level yet (there is nothing
    // prescribed to move)" - but there IS: [ProgressionEngine.startingPrescription]
    // answers a starting level for every progression-ready exercise at the user's
    // own difficulty, which is the level the row will train at the moment it is
    // added. Owner, 2026-10-09: *"When adding an exercise it shows the custom input
    // boxes not the levels, redesign to fix it"*.
    //
    // So the gate is the LEVEL's own existence, not the row's: an exercise the host
    // can resolve a level for gets the level UI, whether it is already in the
    // routine or about to be. A custom-mode editor (level system off) and an
    // exercise outside the pool still resolve no level and still show the plain
    // input boxes - which is the owner's other half of the same rule. A host that
    // gives no writer shows nothing rather than a row whose taps go nowhere - the
    // same lesson as the pill that looked pressable and refused.
    val level = currentLevel
    val show = levelsManaged && level != null && canWriteLevel
    return LevelEditorControls(
        showLevelRow = show,
        // Level 1 is the bottom of every ladder, so "down" has no rung below it.
        canStepDown = show && level!! > 1,
        // maxLevel <= 0 means the host does not know the ceiling; the writer
        // clamps to [WorkoutViewModel.progressionMaxLevel] anyway, so stepping
        // up is honest there and a permanently dead "+" would not be.
        canStepUp = show && (maxLevel <= 0 || level!! < maxLevel),
        canOpenLadder = show
    )
}

/**
 * Everything the exercise editor's level UI needs about ONE exercise, resolved by
 * the host for whichever exercise the form is currently editing.
 *
 * IT IS A NAMED BUNDLE RATHER THAN SIX PARAMETERS, and that is the fix for the
 * editor's second bug: while the six values were passed in separately, both hosts
 * derived them from `editingExercise`, so the ADD form - which has no exercise yet,
 * because the name is typed inside the dialog - could only pass nulls and empty
 * lists, and the level list had nothing to draw. A resolver keyed on the NAME means
 * the same six answers exist for a row the user has not added yet, which is the
 * whole difference between "levels while editing" and "levels while adding".
 *
 * [level] and [stage] are the exercise's own stored level, or - for an exercise that
 * has none yet - the level the system WOULD start it at, which
 * [WorkoutViewModel.currentProgressionState] already answers for any pool exercise.
 */
data class EditorLevelData(
    /** True when the level system prescribes this exercise at all. */
    val levelsManaged: Boolean = false,
    val level: Int? = null,
    val stage: Int? = null,
    /** "Stage III · 20 kg": what [level] asks for, or null when there is no level. */
    val caption: String? = null,
    /** The exercise's ceiling rung; 0 when the host does not know it. */
    val maxLevel: Int = 0,
    /**
     * ONE PAGE of the ladder - 15 rungs, not the whole ladder. The dialog body is a
     * scrolling column and a 500-rung list in it is not a list, so the page is the
     * unit both the host and the list think in.
     */
    val options: List<EditorLevelOption> = emptyList(),
    /**
     * The level the PREVIOUS page of rungs starts at, or null on page one.
     *
     * The host answers this rather than the dialog deriving it, because the page
     * arithmetic belongs with the page size: a list that worked out "15 before my
     * first row" itself would be a second copy of the rule that decides what a page
     * is, and the two would drift the first time the size changed.
     */
    val previousPageStart: Int? = null,
    /** The level the NEXT page of rungs starts at, or null on the last page. */
    val nextPageStart: Int? = null,
) {
    /** The rungs [options] covers, e.g. 1..15. */
    val pageRange: IntRange?
        get() = options.firstOrNull()?.level?.let { first ->
            first..(options.lastOrNull()?.level ?: first)
        }

    /** What a host passes for a name it cannot resolve at all (typed, not picked). */
    companion object {
        val NONE = EditorLevelData()
    }
}

/**
 * One level offered by the exercise editor's level list.
 *
 * [loadText] is already LOCALIZED ("20 kg", "Pin 6", "Medium band", "30 s",
 * "Incline Push-Up") because it is built by the host through the same
 * `prescriptionDisplayText` the live card and the ladder dialog use - a level
 * list that quoted a different number from the card for the same rung would make
 * the whole editor feel broken.
 */
data class EditorLevelOption(
    val level: Int,
    val isCurrent: Boolean,
    val loadText: String,
    /**
     * The level's stages in Roman numerals, lowest first, each with the reps it
     * asks for. This is the level's "expansion": see [LevelPickerCard].
     */
    val stages: List<EditorLevelStage> = emptyList()
)

/**
 * One stage of one level: its Roman numeral, its ordinal (1-based, in the
 * exercise's own pattern), the reps it asks for, and whether it is the stage the
 * exercise is actually on right now.
 *
 * [isCurrent] travels with the row rather than being recomputed in the card
 * because "which stage am I on" is a fact about the stored progression state, not
 * about what the list happens to be showing.
 */
data class EditorLevelStage(
    val stage: Int,
    val roman: String,
    val reps: List<Int> = emptyList(),
    val isCurrent: Boolean = false
)

/**
 * The exercise editor's LEVEL LIST — what stands where the locked sets / reps /
 * weight fields used to.
 *
 * THE OWNER'S RULING (2026-10-08): "the edit card with each single input box will
 * be replaced by the level list (only shown if level system is one, if custom
 * would be one then show the normal input boxes and no levels) ... the user
 * basically select the level and the level expands and the roman number is
 * displayed".
 *
 * So this is not a second copy of the ladder picker:
 *
 *  - the ROW is the choice. Tapping one level selects it, immediately - one tap,
 *    not select-then-confirm - and the host writes it through the same
 *    `setExerciseLevelFromEditor` path the ladder uses (which also clears a
 *    committed lock, so a locked exercise is editable here rather than through a
 *    screen that refuses every tap).
 *  - the SELECTED row EXPANDS to its Roman stages. That is the owner's "the
 *    level expands and the roman number is displayed": the numerals answer "what
 *    am I actually doing at this level", which is the whole reason a level is
 *    being chosen.
 *  - [onOpenLadder] keeps every rung reachable. The list only carries a window of
 *    levels around the current one - a 500-rung ladder inside a scrolling form is
 *    not a list, and the dialog body CANNOT hold a lazy list (it is measured
 *    inside a scrolling column). "See every level" opens the full picker for the
 *    jump this window cannot offer.
 *
 * The labels of the LEVEL and the STAGES are deliberately the same strings the
 * rest of the level system uses, so nothing here can drift into a second
 * vocabulary for the same two objects.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LevelPickerCard(
    options: List<EditorLevelOption>,
    selectedLevel: Int?,
    onSelect: (Int) -> Unit,
    onOpenLadder: (() -> Unit)?,
    /** The Roman stage the exercise is on, so the list can mark it. */
    selectedStage: Int? = null,
    /**
     * A Roman numeral was picked: (level, stage). Owner, 2026-10-08 — "the list
     * shall have multiple levels under the level based on how many roman
     * exercises that level offers ... and when he selects it directly apply it".
     */
    onSelectStage: ((Int, Int) -> Unit)? = null,
    /**
     * WHICH FIFTEEN ARE ON SCREEN, and how to reach the next fifteen.
     *
     * Owner, 2026-10-09: *"Editing a level exercise should show 15 levels at one
     * with min 1 level so dont hide level 1 only when going from 15+ to 30 as a
     * second screen"*. The list used to hold a three-each-way window around the
     * current rung, so a long ladder simply never showed level 1 again. The block
     * is now a page aligned from level 1, and these two controls are how the rest
     * of the ladder is reached — [onOpenLadder] ("See every level") stays as the
     * way to any rung directly.
     *
     * They are null at the ends of the ladder rather than drawn-but-dead: an arrow
     * that cannot move the list is the "pressable control that refuses" this
     * project has already paid for twice.
     */
    pageRange: IntRange? = null,
    totalLevels: Int = 0,
    onPreviousPage: (() -> Unit)? = null,
    onNextPage: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Every gap inside this card separates text from text or from an icon, so
    // none of them is a fixed dp - see [WindowSize.inlineGap]. Measured on the
    // 1.8x emulator, "Level 1" and its "4 lb each" load met with 0px between
    // them, because an 8dp spacer written before the font grew is 8dp after it.
    val window = LocalWindowSize.current
    val inlineGap = window.inlineGap
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .semantics { testTag = "exercise_level_list" }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = getString("level_editor_title").uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (onOpenLadder != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenLadder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(inlineGap))
                        Text(
                            text = getString("level_editor_open_ladder"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // WHICH FIFTEEN ARE THESE, AND HOW TO THE NEXT FIFTEEN.
            //
            // The range is printed because "fifteen rungs" says nothing about WHERE
            // in the ladder they sit: page one and page two of a 30-rung exercise
            // are otherwise the same picture. Each arrow is drawn only where a page
            // exists, so neither is ever a control that cannot move the list.
            if (pageRange != null && totalLevels > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "level_page_controls" }
                ) {
                    if (onPreviousPage != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onPreviousPage)
                                .semantics { testTag = "level_page_previous" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = getString("previous"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = getString(
                            "levels_page_range",
                            pageRange.first,
                            pageRange.last,
                            totalLevels
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (onNextPage != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onNextPage)
                                .semantics { testTag = "level_page_next" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = getString("next"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            options.forEach { option ->
                val selected = option.level == selectedLevel
                Surface(
                    // Tapping a level APPLIES it (level, stage I) and opens its Roman
                    // list beneath it. Tapping the level that is already applied does
                    // NOTHING, deliberately: re-applying it writes stage I over the
                    // stage the user just chose under it, which reads as the list
                    // resetting itself. The way back to stage I is its own row.
                    onClick = { if (!selected) onSelect(option.level) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "editor_level_row_${option.level}" }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        // THE LOAD DROPS TO ITS OWN LINE WHEN THE TEXT IS BIG.
                        //
                        // `isLargeText` is a STRUCTURAL flag (see WindowSize), and
                        // this is a structural question: "Level 4", "current" and
                        // "8 lb each" cannot share one line once the text has grown.
                        // Measured on the 1.8x emulator, that row gave the level
                        // name SIXTEEN pixels - the label the row exists to print,
                        // crushed to a sliver so its load could keep its place.
                        // Stacked, the name owns its line and the load keeps the
                        // word "current" beside it. Under the threshold nothing
                        // changes: one line, load pushed right, exactly as before.
                        val stackLoad = window.isLargeText && option.loadText.isNotBlank()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getString("adjust_levels_n", option.level),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                // fill = false: the name hugs its own text instead
                                // of claiming the whole row, and shrinks - only
                                // then ellipsizing - when the row is genuinely
                                // full, whichever line it is on.
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selected && !stackLoad) {
                                Spacer(modifier = Modifier.width(inlineGap))
                                Text(
                                    text = getString("level_details_current"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                            if (option.loadText.isNotBlank() && !stackLoad) {
                                Spacer(modifier = Modifier.width(inlineGap))
                                Text(
                                    text = option.loadText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // The chevron is what says "this row opens". Without it
                            // the Roman list under the selected level reads as
                            // belonging to the card rather than to that rung.
                            Spacer(modifier = Modifier.width(inlineGap))
                            Icon(
                                imageVector = if (selected) Icons.Default.ExpandLess
                                              else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (stackLoad) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = option.loadText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (selected) {
                                    Spacer(modifier = Modifier.width(inlineGap))
                                    Text(
                                        text = getString("level_details_current"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // THE ROMAN LIST UNDER THE LEVEL.
                        //
                        // Owner, 2026-10-08: "i want the roman numbers inside the
                        // edit exercise to show as a list under the level, the list
                        // shall have multiple [entries] under the level based on how
                        // many roman [stages] that level offers so that the user
                        // doesnt just see all the roman numbers but can also select
                        // which level / roman number and when he selects it directly
                        // apply it".
                        //
                        // So every stage is its OWN row and its own tap target, and
                        // picking one writes the level AND the stage in one go (see
                        // WorkoutViewModel.setExerciseStateFromEditor). A read-only
                        // chip grid said what the stages were; this says which one
                        // you are on and lets you move to another.
                        //
                        // One row per stage, opened for the SELECTED level only. A
                        // rep ladder can run past a dozen stages, so opening every
                        // level at once would put ~90 rows in a form whose body
                        // scrolls - the levels are the list, and each level opens
                        // into its own stages.
                        //
                        // The numeral hugs its own glyphs and the reps take the
                        // rest of the row, with a real gap between them. Every
                        // fixed-width attempt at this column has failed on the
                        // 1.8x emulator: 28dp clips "XIII", and a weighted column
                        // steals the room the reps need (see the note on the rows
                        // themselves).
                        if (selected && option.stages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                option.stages.forEach { stage ->
                                    // The LOCAL choice wins over the reported one while
                                    // both are in flight: the host's `isCurrent` still
                                    // points at the stage the user just left until its
                                    // recomposition lands, and two rows lit at once
                                    // would say the write had not taken.
                                    val stageSelected = if (selectedStage != null &&
                                        selectedLevel == option.level
                                    ) {
                                        stage.stage == selectedStage
                                    } else {
                                        stage.isCurrent
                                    }
                                    Surface(
                                        onClick = { onSelectStage?.invoke(option.level, stage.stage) },
                                        enabled = onSelectStage != null,
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (stageSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (stageSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics {
                                                testTag = "editor_level_stage_${stage.roman}"
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            // The numeral takes what its own
                                            // glyphs need and no more - a weighted
                                            // column claimed 26% of the row for a
                                            // word that is one to four characters
                                            // wide, and the reps paid for it.
                                            // Measured on the 1.8x emulator: with
                                            // the column weighted, the row that
                                            // carries the current-stage tick had
                                            // only ~350px left, so "8 / 8 / 8 Reps"
                                            // WRAPPED onto two lines and that row
                                            // stood 143px tall in a 67px list.
                                            Text(
                                                text = stage.roman,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.width(inlineGap))
                                            Text(
                                                text = if (stage.reps.isEmpty()) ""
                                                       else stage.reps.joinToString(" / ") + " " + getString("reps"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                // One line, always: the reps take
                                                // every pixel the numeral and the
                                                // tick do not, and a ladder whose
                                                // numbers are longer than the row
                                                // ellipsizes instead of growing a
                                                // second line that no other row has.
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            // A TICK, not the word "current". Measured
                                            // on the 1.8x emulator: the word took
                                            // ~183px of the row, which pushed the reps
                                            // column down to ~350px and made
                                            // "9 / 9 / 8 Reps" WRAP onto two lines.
                                            // The tick says the same thing in 47px and
                                            // needs no translation, so the reps now sit
                                            // on one line with room to spare.
                                            if (stageSelected) {
                                                Spacer(modifier = Modifier.width(inlineGap))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = getString("level_details_current"),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditDialog(
    allAvailableExercises: List<String>,
    exercise: RoutineExerciseEntity?,
    dayOfWeek: String,
    themeKey: String,
    onDismiss: () -> Unit,
    onSave: (RoutineExerciseEntity) -> Unit,
    /**
     * Resolves the level facts for the exercise the form is editing RIGHT NOW,
     * keyed on its name.
     *
     * Keyed on the name rather than handed in for `exercise`, because the ADD form
     * picks its exercise inside the dialog: before this was a resolver, both hosts
     * derived the level data from `editingExercise`, so an add passed nulls and the
     * level list could not exist. See [EditorLevelData].
     * * The host supplies it (the dialog holds no view model), and a name it cannot
 * resolve returns [EditorLevelData.NONE] — a half-typed name simply has no level
 * list, which is the honest reading of "this is not an exercise yet".
 *
 * The Int is the level the requested PAGE starts at, or 0 for "the page the
 * exercise's own level is on": the owner's ruling is that the list shows fifteen
 * rungs starting at level 1, with the following fifteen as a second screen
 * (2026-10-09: *"show 15 levels at one with min 1 level so dont hide level 1 only
 * when going from 15+ to 30 as a second screen"*).
 */
    levelDataFor: (@Composable (String, Int) -> EditorLevelData)? = null,
    /**
     * Called when the user touches a level-owned field while the level system
     * prescribes the exercise. The host shows a confirm dialog offering the
     * switch to Custom; returning true unlocks the fields in place.
     */
    onRequestCustomMode: (() -> Unit)? = null,
    /**
     * A Roman numeral was picked under a level: (name, level, stage), applied as ONE
     * write. Owner, 2026-10-08 — "and when he selects it directly apply it".
     *
     * Separate from [onLevelChange] on purpose: that one means "this level, from
     * its first stage", which is what tapping a level row does, and collapsing
     * the two would make picking "V, stage III" impossible to express.
     *
     * THE NAME IS THE FIRST ARGUMENT because the dialog is the only component that
     * knows which exercise is being edited — for an add that is the name in the
     * search field, which the host cannot see.
     */
    onStageChange: ((String, Int, Int) -> Unit)? = null,
    /** Called with the exercise's name and the ABSOLUTE level the user picked. */
    onLevelChange: ((String, Int) -> Unit)? = null,
    /** Opens the full level ladder for the named exercise. */
    onOpenLevelLadder: ((String) -> Unit)? = null,
    /**
     * The equipment the user said they have, as coarse categories. The
     * recommendation filters START on it — an unrestricted-feeling "all
     * suggestions" list that includes kit the user does not own is exactly
     * how the equipment setup read as "the filter doesn't work".
     */
    userEquipmentCategories: List<String> = emptyList(),
    /**
     * The user's granted FINE tags, when the host has them.
     *
     * [userEquipmentCategories] is deliberately coarse — the chip vocabulary is
     * "Barbell", "Dumbbell", … and has NO category for a chair, a bench or a
     * pull-up bar, so those are already gone by the time it arrives. Rebuilding
     * the gate from it cannot restore them: `allowedTagsFor` on the coarse list
     * yields dumbbells/floor/bands only, and the furniture and bar grants the
     * user actually ticked are silently dropped (measured: chair, bench and
     * pull_up_bar all lost). Prefer this when non-null; fall back to the coarse
     * list only for a host that has no fine tags to give.
     */
    userGrantedTags: Set<String>? = null,
    /**
     * True when this exercise's evolution offer was declined and it is locked
     * at its top level. The badge is the ONLY way a declined offer comes back,
     * so it has to be visible here rather than remembered.
     */
    evolutionLocked: Boolean = false,
    /** Opens the evolution popup again for this locked exercise. */
    onUpgrade: (() -> Unit)? = null,
    /**
     * True when the level system prescribes exercises app-wide.
     *
     * It is NOT the same question as [EditorLevelData.levelsManaged], which the
     * host answers per NAME: an ADD form has no name yet, so it gets NONE and the
     * old form fell back to the custom boxes until the user picked something. This
     * flag says "the system is on", which is what decides whether a new exercise is
     * picked through the level system's picker (owner, 2026-10-11) or typed into the
     * classic field. An edit never uses the picker: it is about the row it was
     * opened on.
     */
    levelSystemOn: Boolean = false,
) {
    // Only an edit of an existing, level-managed row is locked; a fresh add
    // always keeps its inputs open. Touching a locked field (while enabled,
    // i.e. NOT visually greyed) asks the host to offer the custom switch.
    // Settled below, once the form knows which exercise it is editing.
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "") }
    var targetSetsStr by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "") }
    var targetIntensityStr by remember { mutableStateOf(exercise?.targetIntensity ?: "") }
    var restSecondsStr by remember { mutableStateOf(exercise?.restSeconds?.toString() ?: "60") }

    // WHICH EXERCISE THIS FORM IS ABOUT, and therefore whose levels it shows.
    //
    // An edit is about the row it was opened on; an ADD is about the name in the
    // search field, which only this dialog can see. Both resolve through the same
    // host resolver, so "add" and "edit" are one code path with one definition of
    // the level facts - the alternative was six nulls on the add path, which is
    // exactly why adding an exercise showed the custom input boxes instead of the
    // levels (owner, 2026-10-09).
    // A typed name resolves through [ExerciseLocalization.canonicalFor] first, so a
    // name typed in another language or with different spacing still finds its pool
    // row - and therefore its levels - instead of silently falling back to the
    // custom boxes. The raw text is the last resort, which is what makes a name the
    // pool does not carry resolve to [EditorLevelData.NONE].
    val levelName = exercise?.name
        ?: ExerciseLocalization.canonicalFor(name.trim())
        ?: name.trim()
    // WHICH BLOCK OF FIFTEEN. 0 means "the page the exercise's level is on", which
    // is what opening the form asks for; the list's own next/previous controls set
    // a real start level. Keyed on the name so picking another exercise starts
    // again from that exercise's own level instead of carrying a page across.
    var levelPageStart by remember(levelName) { mutableStateOf(0) }
    val levelData = levelDataFor?.invoke(levelName, levelPageStart) ?: EditorLevelData.NONE

    // Only an edit of an existing, level-managed row is locked; a fresh add
    // always keeps its inputs open. Touching a locked field (while enabled,
    // i.e. NOT visually greyed) asks the host to offer the custom switch.
    val inputsLocked = levelData.levelsManaged && exercise != null
    // THE TIME TOGGLE OPENS IN THE MEASURE THE EXERCISE ACTUALLY USES. It used to
    // open a timed exercise with the toggle OFF and a rep range in the reps field
    // whenever its stored row did not literally spell "sec" - which is every row
    // built before the level system, Jumping Jacks' "8–12" included. The editor is
    // where the user confirms the plan, so showing "reps" there for an exercise the
    // workout runs on a timer is the same bug the card had, one screen over.
    var isTimeBased by remember { mutableStateOf(WorkoutTemplates.isExerciseTimeBased(exercise?.name.orEmpty())) }
    var isExpanded by remember { mutableStateOf(exercise != null) }
    
    var targetRepsValue by remember { mutableStateOf(if (!isTimeBased) exercise?.targetReps ?: "" else "8-12") }
    var repsManuallyEdited by remember { mutableStateOf(exercise != null) }
    // Seeded through secondsInStoredTarget rather than by stripping " sec" off the
    // row: a pre-level-system timed row stores a rep range ("8-12"), which would
    // open the SECONDS field at "8" and - because the save path strips non-digits -
    // write back "812 sec". A row with no plain duration opens at 30.
    var targetTimeValue by remember { mutableStateOf(if (isTimeBased) WorkoutTemplates.secondsInStoredTarget(exercise?.targetReps)?.toString() ?: "30" else "30") }
    var videoUrl by remember { mutableStateOf(exercise?.videoUrl ?: "") }
    // Seeded through loadFieldValueText, not Double.toString(): a pin count is a
    // count, so it must not open the sheet as "20.0" (see the function's note).
    // Read outside the remember: `remember`'s lambda is not a composable scope,
    // so the unit has to be resolved here and captured.
    val weightUnit = LocalWeightUnit.current
    var targetWeightValue by remember {
        mutableStateOf(
            exercise?.targetWeightKg?.let { loadFieldValueText(name, it, weightUnit) } ?: ""
        )
    }

    // The suggestion filter popup was removed (dashboard task
    // "The exercise suggestions tab is still broken. Remove it completely").
    // What is left is the ONE gate that is not a convenience: the user's own
    // kit. Suggestions must never offer a barbell row to a no-barbell setup
    // before the user has touched a single control, so the equipment
    // restriction starts ON (from the user's declared equipment) and the muscle
    // restriction starts off. There is no UI to change either one any more.
    val filteredExercises = remember(allAvailableExercises, userEquipmentCategories, userGrantedTags) {
        filteredExercisesForPickers(
            allAvailableExercises, null, userEquipmentCategories,
            // Fine-tag gate: coarse chips alone leaked bar-less moves (Pull-Ups)
            // into dumbbells+chair suggestions through the Bodyweight chip.
            WorkoutTemplates.pickerGrantedTags(userGrantedTags, userEquipmentCategories)
        )
    }

    var activeToastMessage by remember { mutableStateOf<String?>(null) }

    // The rung the level list draws as chosen. Seeded from the host, then owned
    // locally: a tap has to move the highlight and open the row in the SAME frame,
    // while the host's own level arrives through a different path (a prefs write
    // and a recomposition) that is not guaranteed to land first.
    // Keyed on [levelName], not on the exercise id: in the add form the name
    // CHANGES as the user picks a different exercise, and the highlight has to
    // re-seed to that exercise's own level rather than keep the last one's.
    var selectedLevel by remember(levelName) { mutableStateOf(levelData.level) }

    // The Roman numeral the list draws as chosen, seeded from the host and then
    // owned locally for the same reason as [selectedLevel]: the highlight has to
    // move on the tap, not one recomposition later.
    var selectedStage by remember(levelName) { mutableStateOf(levelData.stage) }

    // One notice for every locked field: the level system prescribes the value,
    // so the box is read-only and says why in one short line instead of silently
    // ignoring the tap.
    //
    // It goes to the APP-LEVEL top banner rather than a toast inside this card.
    // Rendered here it sat in a scrolling Box, so on a long editor the notice
    // could be off-screen when the user tapped, which is the "dont have it
    // inside the popup card but show it at the top" the owner asked for. See
    // AppEventBus.showTopBanner.
    val lockedFieldNoticeText = getString("level_fields_locked_toast")
    val lockedFieldNotice: () -> Unit = {
        com.grafizum.repio.util.AppEventBus.showTopBanner(lockedFieldNoticeText)
    }

    val isRepsValid = remember(targetRepsValue, isTimeBased) {
        if (isTimeBased) true
        else if (targetRepsValue.isBlank()) true
        else {
            val validChars = targetRepsValue.all { it.isDigit() || it == '-' || it == '–' || it == ' ' }
            if (!validChars) return@remember false
            val clean = targetRepsValue.replace(" ", "")
            if (clean.startsWith('-') || clean.endsWith('-') || 
                clean.startsWith('–') || clean.endsWith('–')) return@remember false
            val parts = clean.split('-', '–').filter { it.isNotBlank() }
            if (parts.size == 2) {
                val v1 = parts[0].toIntOrNull()
                val v2 = parts[1].toIntOrNull()
                if (v1 != null && v2 != null) v1 <= v2 else false
            } else if (parts.size == 1) {
                parts[0].toIntOrNull() != null
            } else false
        }
    }

    val repsHasRangeError = remember(targetRepsValue, isTimeBased) {
        if (isTimeBased || targetRepsValue.isBlank()) false
        else {
            val clean = targetRepsValue.replace(" ", "")
            val parts = clean.split('-', '–').filter { it.isNotBlank() }
            if (parts.size == 2) {
                val v1 = parts[0].toIntOrNull()
                val v2 = parts[1].toIntOrNull()
                v1 != null && v2 != null && v1 > v2
            } else false
        }
    }

    val focusManager = LocalFocusManager.current

    // WHICH EDITOR THIS FORM IS, resolved before the shell so the confirm button
    // and the body agree on it. Level mode means the level system prescribes this
    // exercise — whether the row exists yet or not: its sets, reps, weight, hold
    // time and intensity ARE the level's values, so the form shows the LEVEL LIST in
    // their place. Custom mode keeps the classic boxes and shows no level list at
    // all — the owner's two halves of one rule, 2026-10-08: "the edit card with each
    // single input box will be replaced by the level list (only shown if level
    // system is one, if custom would be one then show the normal input boxes and no
    // levels)".
    //
    // ADDING USED TO FALL OUT HERE, which is the report this changed for: the add
    // form passed no level, so the whole screen was custom mode and the level list
    // was unreachable. See [levelEditorControls] for why the gate is the LEVEL's
    // existence and not the row's.
    val levelControls = levelEditorControls(
        levelsManaged = levelData.levelsManaged,
        currentLevel = levelData.level,
        maxLevel = levelData.maxLevel,
        canWriteLevel = onLevelChange != null
    )
    val levelMode = levelControls.showLevelRow &&
        levelData.level != null &&
        selectedLevel != null &&
        onLevelChange != null

    AppDialog(
        onDismissRequest = onDismiss,
        title = if (exercise == null) getString("add_exercise") else getString("edit_exercise"),
        // With the keyboard open on small phones (e.g. Pixel) the form used to
        // clip its bottom fields; the shell pads for the keyboard and scrolls the
        // body, so every field stays reachable.
        onTitleTap = { focusManager.clearFocus() },
        confirmLabel = getString("save"),
        // In level mode the reps box is not on screen at all, so it cannot be the
        // thing that gates Save: a level-managed row whose stored reps happen to be
        // blank would otherwise show a permanently disabled button with nothing on
        // the form to fix. The stored values are preserved on save instead - see
        // `finalReps` below.
        confirmEnabled = name.isNotBlank() &&
            (levelMode || (if (isTimeBased) targetTimeValue else targetRepsValue).isNotBlank()),
        onConfirm = {
            focusManager.clearFocus()
            if (name.isNotBlank() && (levelMode || (if (isTimeBased) targetTimeValue else targetRepsValue).isNotBlank())) {
                val finalReps = if (isTimeBased) {
                    val digits = targetTimeValue.filter { it.isDigit() }
                    if (digits.isNotBlank()) "$digits sec" else "30 sec"
                } else {
                    val typed = WorkoutTemplates.enforceRepRange(targetRepsValue.trim())
                    when {
                        typed.isNotBlank() -> typed
                        // In level mode an empty box means "the level owns this", not
                        // "the user cleared it". An EDIT keeps the stored range; an
                        // ADD has none yet and takes the range the exercise
                        // recommends, because the level list is what the user chose
                        // from and there is no reps box on screen to fill. A row saved
                        // with a blank range would also clamp the level's own
                        // prescription away at the next load (`setTarget in
                        // minVal..maxVal`), which is how a level ends up ignored.
                        levelMode -> exercise?.targetReps?.takeIf { it.isNotBlank() }
                            ?: WorkoutTemplates.enforceRepRange(
                                WorkoutTemplates.getRecommendedRepsForExercise(levelName)
                            )
                        else -> ""
                    }
                }
                // Whatever the user typed or picked, the row is stored under the canonical
                // pool name when it is a pool exercise: that keeps the equipment table, the
                // muscle table and previously logged sets all pointing at the same exercise.
                onSave(RoutineExerciseEntity(id = exercise?.id ?: 0L, dayOfWeek = dayOfWeek, name = ExerciseLocalization.canonicalFor(name) ?: name, muscleGroup = muscleGroup, targetSets = targetSetsStr.toIntOrNull() ?: 3, targetReps = finalReps, targetIntensity = targetIntensityStr, targetWeightKg = loadInputToKg(name, targetWeightValue, weightUnit) ?: 0.0, restSeconds = restSecondsStr.toIntOrNull() ?: 60, videoUrl = videoUrl, orderIndex = exercise?.orderIndex ?: 99, tempo = exercise?.tempo ?: ""))
            }
        },
        dismissLabel = getString("cancel"),
        onDismiss = {
            focusManager.clearFocus()
            onDismiss()
        }
    ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
            ) {
                // The lock the user set by declining an evolution. Declining
                // is permanent, so without a visible way back it would read as
                // "the app decided this exercise was finished".
                if (evolutionLocked && exercise != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = getString("evolution_locked_badge"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = getString("evolution_decline_warns"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            TextButton(
                                onClick = { onUpgrade?.invoke() },
                                enabled = onUpgrade != null,
                            ) {
                                Text(getString("evolution_locked_upgrade"))
                            }
                        }
                    }
                }

                // The lock the user set by declining an evolution. Declining
                // is permanent, so without a visible way back it would read as
                // "the app decided this exercise was finished".
                if (evolutionLocked && exercise != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = getString("evolution_locked_badge"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = getString("evolution_decline_warns"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            TextButton(
                                onClick = { onUpgrade?.invoke() },
                                enabled = onUpgrade != null,
                            ) {
                                Text(getString("evolution_locked_upgrade"))
                            }
                        }
                    }
                }

                // THE LEVEL IS EDITABLE HERE, even in level-system mode. Owner,
                // 2026-10-05: "the editing of an exercise, allow the user to edit
                // the level while in level system mode".
                //
                // It sits ABOVE the locked-fields notice on purpose. That notice
                // says sets/reps/weight belong to the level system, which is true
                // and is why they stay greyed — but read on its own it says the
                // exercise cannot be edited here at all, which is now false: the
                // one value that decides what those numbers WILL BE is a control
                // on this same screen.
                // THE EXERCISE ITSELF COMES FIRST, ABOVE THE LEVELS.
                //
                // Owner, 2026-10-08: "and in the edit screen have the exercise name
                // above the levels". It is also the honest order: the level list is
                // the level OF something, so the something has to be readable before
                // the rungs are.
                //
                // It stays in BOTH modes - none of it is prescribed by the level
                // system, and hiding the name field in level mode would leave no way
                // to correct a mis-picked movement.
                //
                // IN LEVEL MODE A NEW EXERCISE IS PICKED, NOT TYPED. Owner,
                // 2026-10-11: "adding an exercise card ... needs to stay as it is for
                // custom but changed for when level system is active, instead of all
                // the reps sets weight etc input boxes, i want to have exercise name
                // that the user can input to search an exercise and under it filter
                // options for the searched exercise, but if the user only uses the
                // filters and doesnt search by name show al the possible exercises
                // based on those filters". The picker draws exactly that: one search
                // field, the two filters, and a list of graphical rows. Custom mode
                // keeps the classic field and boxes, untouched.
                val pickerMode = exercise == null && levelSystemOn

                // ONE definition of "this is the exercise now", shared by the typed
                // field and the picker. The picker has no keystrokes to hook, so
                // without it the two paths would set different defaults for the same
                // pick - and the muscle group would only ever be set by typing, which
                // the picker makes optional.
                val adoptExercise: (String, String?) -> Unit = { selectedName, preciseMuscle ->
                    name = selectedName
                    if (!preciseMuscle.isNullOrBlank()) muscleGroup = preciseMuscle
                    isTimeBased = WorkoutTemplates.isExerciseTimeBased(selectedName)
                    isExpanded = true
                    videoUrl = ""
                    if (targetSetsStr.isBlank()) targetSetsStr = WorkoutTemplates.getRecommendedSetsForExercise(selectedName).toString()
                    if (isTimeBased) {
                        if (targetTimeValue.isBlank()) targetTimeValue = WorkoutTemplates.getRecommendedRepsForExercise(selectedName).replace(" sec", "")
                    } else {
                        if (targetRepsValue.isBlank() || targetRepsValue == "8-12") {
                            targetRepsValue = WorkoutTemplates.adaptRepsToIntensity(WorkoutTemplates.getRecommendedRepsForExercise(selectedName), targetIntensityStr)
                            repsManuallyEdited = false
                        }
                    }
                }

                if (pickerMode) {
                    ExercisePickerBody(
                        equipmentCategories = userEquipmentCategories,
                        grantedTags = userGrantedTags,
                        allAvailableExercises = allAvailableExercises,
                        // The picked exercise stays visible as a tinted row: the level
                        // list below belongs to it, and a list that cannot say which
                        // row it is about is how "did my tap register?" starts.
                        selectedName = name.takeIf { it.isNotBlank() },
                        onPick = { pick -> adoptExercise(pick.name, pick.muscleGroup) }
                    )
                } else {
                SearchableExerciseField(
                    allExercises = filteredExercises,
                    value = name,
                    onValueChange = { 
                        name = it
                        if (it.isBlank()) {
                            isExpanded = false
                            targetSetsStr = ""
                            targetRepsValue = ""
                            muscleGroup = ""
                        }
                    
                        // Resolve a name typed in another language first, so "Kniebeugen" still
                        // auto-detects Legs instead of falling through to "Other".
                        val detected = WorkoutTemplates.getMuscleGroupForExercise(
                            ExerciseLocalization.canonicalFor(it) ?: it
                        )
                        if (detected != "Other") muscleGroup = detected
                    },
                    onItemSelected = { selectedName -> adoptExercise(selectedName, null) },
                    label = getString("exercise_name"),
                    placeholder = getString("exercise_name_placeholder")
                )
                }

                // The method text is CUSTOM MODE'S context. In picker mode the level
                // list is the information the user came for, and a two-line paragraph
                // between the search and the rungs is the "bloat text" the owner asked
                // to be rid of.
                if (isExpanded && !pickerMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = getString("trained_muscles") + ":",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                // "Trainierte Muskeln:" is far longer than "Trained muscles:", so give
                                // the label its own shrinking column instead of squeezing the badge.
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MuscleGroupBadge(muscleGroup = muscleGroup, exerciseName = name)
                        }
                    
                        val (exerciseDesc, _) = getExerciseDetails(name, muscleGroup, targetIntensityStr)
                        if (exerciseDesc.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp)) {
                                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(exerciseDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }


                if (levelMode) {
                    LevelPickerCard(
                        options = levelData.options,
                        // THE NEXT FIFTEEN ARE A SECOND SCREEN, not a scroll: the
                        // buttons move the whole block, and they only exist where
                        // there is a block to move to.
                        pageRange = levelData.pageRange,
                        totalLevels = levelData.maxLevel,
                        onPreviousPage = levelData.previousPageStart
                            ?.let { start -> { levelPageStart = start } },
                        onNextPage = levelData.nextPageStart
                            ?.let { start -> { levelPageStart = start } },
                        selectedLevel = selectedLevel,
                        onSelect = { level ->
                            // A LEVEL row means "this level from its first stage":
                            // applyExerciseLevel writes stage I, so the local highlight
                            // has to follow it rather than keep pointing at whatever
                            // stage the previous level was on.
                            selectedLevel = level
                            selectedStage = 1
                            onLevelChange?.invoke(levelName, level)
                        },
                        // The name travels with every level write: in the add form it
                        // is the exercise the user is ABOUT to add, and the host has
                        // no other way to know which row this list belongs to.
                        onOpenLadder = { onOpenLevelLadder?.invoke(levelName) },
                        selectedStage = selectedStage,
                        onSelectStage = { level, stage ->
                            selectedLevel = level
                            selectedStage = stage
                            onStageChange?.invoke(levelName, level, stage)
                        }
                    )
                }

                // One line of context under the list, for the same reason the old
                // level row had one: "LEVEL 5" alone does not say what level 5 does.
                if (levelMode && levelData.caption != null) {
                    Text(
                        text = levelData.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // When the level system prescribes this exercise, the values it
                // owns are shown read-only: the prescription is authoritative, so
                // the fields are greyed out rather than hidden, and one line
                // explains where the switch that unlocks them lives.
                if (inputsLocked && !levelMode) {
                    Surface(
                        onClick = { onRequestCustomMode?.invoke() },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getString("level_fields_locked_hint"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // THE LEVEL-OWNED VALUES, IN CUSTOM MODE ONLY. These fields
                // are what the level system prescribes when it is on, so in
                // level mode they are not shown at all - the level list above
                // IS their editor, and two editors for one prescription is the
                // duplication this whole change exists to remove.
                //
                // AND THE PICKER HOLDS THEM BACK UNTIL SOMETHING IS PICKED. Owner, 2026-10-11:
                // *"remove the input boxes under the exercises"*. At the moment the card opens,
                // nothing is chosen, so the boxes could only ever be EMPTY - and empty boxes under
                // a list of exercises read as "fill these in", i.e. as a second, wrong way to say
                // which movement this is. A picker card is a list until a row is tapped; once one
                // is, the level list takes over for a laddered exercise and these fields return for
                // a movement the level system has no ladder for (a stretch), which is the fallback
                // that keeps such a movement writable at all.
                if (!levelMode && (!pickerMode || name.isNotBlank())) {
                    RepsTimeToggle(
                        isTimeBased = isTimeBased,
                        onToggle = { newValue ->
                            if (!inputsLocked) isTimeBased = newValue
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // SOFT rep guidance, never a gate: the field only flags a
                    // structurally invalid range (min > max), and even that keeps the
                    // save button enabled — the user is always free to save what they
                    // typed. If the range sits entirely outside the exercise's
                    // recommended band, an info chip says so without blocking.
                    val repsError = targetRepsValue.isNotEmpty() && !isRepsValid
                    val outsideRecommended = remember(targetRepsValue, name) {
                        name.isNotBlank() && targetRepsValue.isNotBlank() && !isTimeBased &&
                            WorkoutTemplates.isOutsideRecommendedRepRange(name, targetRepsValue)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    ) {                        CompactNumericField(
                            value = targetSetsStr,
                            onValueChange = { newValue ->
                                if (!newValue.all { char -> char.isDigit() }) return@CompactNumericField
                                if (inputsLocked) { onRequestCustomMode?.invoke(); return@CompactNumericField }
                                targetSetsStr = newValue
                            },
                            label = getString("sets"),
                            enabled = !inputsLocked,
                            onLockedTap = lockedFieldNotice,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )

                        if (isTimeBased) {
                            var expandedTime by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedTime,
                                // The anchor kept opening its menu while locked: the
                                // dropdown then closed again on the very next
                                // composition, so a tap on a greyed field looked
                                // like nothing happened at all.
                                onExpandedChange = {
                                    if (inputsLocked) lockedFieldNotice()
                                    else expandedTime = !expandedTime
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                val digitsOnly = targetTimeValue.filter { it.isDigit() }
                                val interactionSource = remember { MutableInteractionSource() }
                                BasicTextField(
                                    value = if (digitsOnly.isNotBlank()) "${digitsOnly}s" else "30s",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().defaultMinSize(minHeight = 56.dp).fillMaxHeight(),
                                    interactionSource = interactionSource,
                                    enabled = true,
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start, fontSize = 16.sp),
                                    decorationBox = { innerTextField ->
                                        OutlinedTextFieldDefaults.DecorationBox(
                                            value = if (digitsOnly.isNotBlank()) "${digitsOnly}s" else "30s",
                                            innerTextField = innerTextField,
                                            enabled = true,
                                            singleLine = true,
                                            visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                                            interactionSource = interactionSource,
                                            isError = false,
                                            label = { Text(getString("time_label"), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTime) },
                                            contentPadding = OutlinedTextFieldDefaults.contentPadding(start = 16.dp, end = 12.dp),
                                            container = {
                                                OutlinedTextFieldDefaults.Container(
                                                    enabled = true,
                                                    isError = false,
                                                    interactionSource = interactionSource,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                                    ),
                                                    shape = RoundedCornerShape(12.dp),
                                                    focusedBorderThickness = 2.dp,
                                                    unfocusedBorderThickness = 1.dp
                                                )
                                            }
                                        )
                                    }
                                )
                                // Greyed out with the rest of the prescribed values; the
                                // menu anchor still renders, but nothing opens it.
                                if (inputsLocked) expandedTime = false
                                ExposedDropdownMenu(expanded = expandedTime && !inputsLocked, onDismissRequest = { expandedTime = false }) {
                                    listOf("30", "45", "60", "90", "120").forEach { t ->
                                        DropdownMenuItem(text = { Text("${t}s") }, onClick = { targetTimeValue = t; expandedTime = false })
                                    }
                                }
                            }
                        } else {
                            RepsRangeField(
                                value = targetRepsValue,
                                onValueChange = { newValue ->
                                    if (inputsLocked) { onRequestCustomMode?.invoke(); return@RepsRangeField }
                                    targetRepsValue = newValue
                                    repsManuallyEdited = true
                                },
                                label = getString("reps"),
                                isError = repsError,
                                enabled = !inputsLocked,
                                onLockedTap = lockedFieldNotice,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }

                    // Attention, not enforcement: the save button stays enabled, this
                    // only explains that there is a recommended band.
                    if (outsideRecommended) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = getString("reps_outside_recommended"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    ) {
                        CompactNumericField(
                            value = targetWeightValue,
                            onValueChange = { newValue ->
                                if (inputsLocked) { onRequestCustomMode?.invoke(); return@CompactNumericField }
                                targetWeightValue = newValue
                            },
                            // Not every exercise is loaded with weight: a selectorized
                            // stack counts pins and a band counts tension, and a "kg"
                            // label on those rows asks a question with no honest answer.
                            label = loadFieldLabel(name),
                            enabled = !inputsLocked,
                            onLockedTap = lockedFieldNotice,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = loadFieldPlaceholder(name),
                            suffix = { Text(loadFieldSuffix(name), fontSize = 12.sp) }
                        )
                        CompactNumericField(
                            value = targetIntensityStr,
                            onValueChange = { newValue ->
                                if (inputsLocked) { onRequestCustomMode?.invoke(); return@CompactNumericField }
                                targetIntensityStr = newValue
                                // Re-adapt the auto-selected reps while the user hasn't typed their own
                                if (!repsManuallyEdited && !isTimeBased && name.isNotBlank()) {
                                    val recommended = WorkoutTemplates.getRecommendedRepsForExercise(name)
                                    if (recommended.isNotBlank()) {
                                        targetRepsValue = WorkoutTemplates.adaptRepsToIntensity(recommended, newValue)
                                    }
                                }
                            },
                            label = getString("intensity_label"),
                            enabled = !inputsLocked,
                            onLockedTap = lockedFieldNotice,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    }
                }


                // Tempo is not a manual choice — it is never touched here, so an
                // existing value on the exercise is kept as-is.
                ThemedToast(activeToastMessage ?: "", themeKey, activeToastMessage != null) { activeToastMessage = null }
                OutlinedTextField(
                    value = videoUrl, 
                    onValueChange = { videoUrl = it }, 
                    label = { Text(getString("demo_link_optional")) }, 
                    singleLine = true, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )

                // THE WAY OUT OF LEVEL MODE, at the END of the form, for the user who
                // came here to change something the level system owns.
                //
                // The owner asked for it in as many words (2026-10-08): "if level is
                // on then show a small label at the end saying 'switch to custom for
                // full exercise edit'". It has to be here rather than nowhere because
                // the fields it refers to are no longer ON this screen - without it,
                // a user looking for the weight box would reasonably conclude the app
                // had stopped letting them edit the exercise at all.
                //
                // It fires the same switch the old locked-field notice fired, and the
                // host still confirms it: turning the level system off is a
                // whole-app decision, not a per-exercise one.
                if (levelMode) {
                    Text(
                        text = getString("level_switch_to_custom_edit"),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onRequestCustomMode != null) {
                                onRequestCustomMode?.invoke()
                            }
                            .padding(vertical = 4.dp)
                            .semantics { testTag = "level_switch_to_custom" }
                    )
                }
            }
    }

    }

/**
 * Gap between two day chips, as a fraction of one chip's slot width. A ratio rather than a number
 * of dp, so the wheel keeps the same visual rhythm on a 320dp phone and on a tablet.
 */
private const val DayWheelGapRatio = 0.08f

/**
 * How many day chips share a row once the text is too large for seven.
 *
 * Seven equal columns are what make the wheel a wheel, and they are also its ceiling:
 * measured at font scale 1.8 on the owner's 411dp emulator, dividing the wheel into
 * seven slots with its own gap ratio left each chip 68px for a label that needs 85px,
 * so AutoScaleText quietly shrank the day letters to about 73% of the platform size -
 * 62px of text against the owner's phone at 47px, i.e. 1.32x, where every other string
 * on that screen measured 1.80x. That one row was the last thing still "pressed
 * together" (the owner's word) after the frame was fixed. Splitting seven into four and
 * three doubles the width each label gets while KEEPING equal chips - the short row
 * fills its remainder with invisible weights, so the row stays centred - and no day is
 * hidden off-screen the way a horizontally scrolling strip would hide it.
 */
private const val DayWheelLargeTextRowSize = 4

/**
 * Distance from the chip's top edge to the TODAY tag. The tag overlays the
 * headroom above the centred day label and consumes no layout space, so the
 * label's centre - not a slot+label composite - defines the chip's optical
 * balance on all seven chips.
 */
private val DayWheelTodayTagTopPadding = 3.dp

@Composable
fun WorkoutDayWheel(
    selectedDay: String,
    onDaySelected: (String) -> Unit,
    today: String,
    modifier: Modifier = Modifier
) {
    val allDays = DayOfWeekEnum.entries
    val isDeveloper = LocalDeveloperSkin.current
    val window = LocalWindowSize.current
    var totalDrag by remember { mutableFloatStateOf(0f) }

    // Strict gesture lock to prevent main app tab switching while swiping on the wheel
    val lockConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.x != 0f) Offset(x = available.x, y = 0f) else Offset.Zero
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(lockConnection)
            .pointerInput(selectedDay) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        val currentIndex = allDays.indexOfFirst { it.name == selectedDay }
                        if (totalDrag > 120f) { // Swipe Right -> Next Day
                            val nextIndex = (currentIndex + 1) % 7
                            onDaySelected(allDays[nextIndex].name)
                        } else if (totalDrag < -120f) { // Swipe Left -> Previous Day
                            val prevIndex = (currentIndex - 1 + 7) % 7
                            onDaySelected(allDays[prevIndex].name)
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            }
    ) {
        // Divide whatever width the wheel is actually given into seven slots and take the gap as
        // a fraction of one slot. The same code therefore yields a tight wheel on a 320dp phone
        // and a roomy one on a tablet, continuously, with no per-device table to maintain.
        val chipSpacing = if (maxWidth.value.isFinite() && maxWidth > 0.dp) {
            (maxWidth / allDays.size) * DayWheelGapRatio
        } else {
            4.dp
        }

        // Seven equal columns cannot all keep Material's 48dp *width* minimum on a phone: 7 x 48dp
        // fills a 336dp content column completely, erasing the gaps and turning the wheel into one
        // solid bar. The chips below opt out of the width minimum - they keep a >= 48dp *height*,
        // so their tap targets stay comfortable - and each one really gets its weight(1f) share.
        // One row on a normal phone, two once the labels need the room (see
        // DayWheelLargeTextRowSize). The gap between chips rides the user's font scale at
        // that size - the same `inlineGap` token the rest of the app uses between two
        // pieces of text - instead of staying a fraction of the wheel's width while the
        // letters inside it grow.
        val wheelRows = if (window.isLargeText) allDays.chunked(DayWheelLargeTextRowSize)
                        else listOf(allDays)
        val wheelGap = if (window.isLargeText) window.inlineGap else chipSpacing

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Column(
            verticalArrangement = Arrangement.spacedBy(wheelGap),
            modifier = Modifier.fillMaxWidth()
        ) {
            wheelRows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(wheelGap),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { dayEnum ->
                    val isToday = dayEnum.name == today
                    val isSelected = dayEnum.name == selectedDay

                    // Unselected days sit on the LOW container — one shade below
                    // surfaceVariant — so the filled primary chip reads as clearly
                    // raised against its siblings instead of one of seven equal tiles.
                    val backgroundColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    }

                    val contentColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val borderStroke = when {
                        isToday -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        isDeveloper && isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                        else -> null
                    }

                    Surface(
                        onClick = { onDaySelected(dayEnum.name) },
                        color = backgroundColor,
                        contentColor = contentColor,
                        shape = RoundedCornerShape(12.dp),
                        border = borderStroke,
                        // A *definite* height, not a minimum. Two reasons: a minimum lets each
                        // chip wrap its own content, so the selected day (which carries the extra
                        // "TODAY" tag) ends up taller than its neighbours and the wheel looks
                        // ragged; and an unbounded chip let all seven stretch to every dp the
                        // screen could spare - 388dp on a 360x640dp phone - which ate the whole
                        // column and pushed the day's exercise list below down to zero height.
                        // The height itself comes from the window's own height, so a short window
                        // gets slimmer chips rather than a per-device number.
                        modifier = Modifier
                            .weight(1f)
                            .height(window.dayChipHeight)
                    ) {
                        // Safe to fill, unlike before: the chip's height is definite now, so this
                        // box is exactly [WindowSize.dayChipHeight] and cannot grow to the window's.
                        val dayLabel = when (dayEnum) {
                            DayOfWeekEnum.MONDAY -> getString("monday_shortest")
                            DayOfWeekEnum.TUESDAY -> getString("tuesday_shortest")
                            DayOfWeekEnum.WEDNESDAY -> getString("wednesday_shortest")
                            DayOfWeekEnum.THURSDAY -> getString("thursday_shortest")
                            DayOfWeekEnum.FRIDAY -> getString("friday_shortest")
                            DayOfWeekEnum.SATURDAY -> getString("saturday_shortest")
                            DayOfWeekEnum.SUNDAY -> getString("sunday_shortest")
                        }

                        // Every chip reserves the same fixed-height slot above its day label:
                        // today's chip fills it with the TODAY tag, the other six leave it
                        // empty. Because the slot exists on all seven, every label keeps
                        // exactly the same baseline (the old overlay shoved today's label
                        // off-centre, and pinning the tag to the top edge afterwards left it
                        // cramped against the border - this placement fixes both).
                        // Centering is done EXPLICITLY, not trusted to the Surface:
                        // a bordered Surface's drawn pill can sit a few px left of the
                        // slot on some devices (the border stroke and the tonal overlay
                        // resolve around the measured size differently), which is exactly
                        // the ragged look this wheel had. fillMaxSize + a column centred
                        // in the WHOLE slot keeps label and pill dead-centre on every
                        // screen width; the 2dp border stroke straddles the edge.
                        // The day label is centred in the FULL chip - both axes - on every
                        // chip, so all seven share one baseline AND sit at the pill's
                        // optical centre. (Reserving a layout slot above the label for the
                        // TODAY tag centred the slot+label composite instead, which left
                        // six labels hanging below centre with dead space above - the
                        // ragged look. The tag now overlays the top headroom and consumes
                        // no layout space; chips keep a >= 48dp height, so the ~10dp tag
                        // always fits in the headroom above the centred label.)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // AutoScaleText instead of a clipped 12sp label keeps
                            // "Mi"/"Mer"/"Mie" fitting the same slot in every language.
                            // Its fillMaxWidth box is wider than the text and AutoScaleText
                            // centres the text inside it; without that the label sat
                            // against the left edge.
                            AutoScaleText(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                minScale = 0.7f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isToday) {
                                AutoScaleText(
                                    text = getString("today").uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    minScale = 0.5f,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .padding(top = DayWheelTodayTagTopPadding)
                                )
                            }
                        }
                    }
                }
                    // A short row keeps the FULL row's chips the same width, centred,
                    // by padding itself out with invisible weights.
                    repeat(DayWheelLargeTextRowSize - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            }
        }
    }
}

/**
 * Workout History Calendar - Redesigned to "Training Output" unified card
 */
@Composable
fun WorkoutCalendar(
    loggedSets: List<LoggedSetEntity>,
    selectedDayKey: String?,
    onDaySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    val colorScheme = MaterialTheme.colorScheme
    
    // State for the selected week
    var weekStartCalendar by remember { 
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
        }) 
    }

    val sdf = SimpleDateFormat("MMM dd", rememberCurrentLocale())
    val weekEndCalendar = (weekStartCalendar.clone() as Calendar).apply { add(Calendar.DATE, 6) }
    val weekRangeText = "${sdf.format(weekStartCalendar.time)} - ${sdf.format(weekEndCalendar.time)}"

    // Filter data for the selected week
    val weekSets = remember(loggedSets, weekStartCalendar) {
        val startTs = weekStartCalendar.timeInMillis
        val endTs = weekEndCalendar.timeInMillis + 86400000L
        loggedSets.filter { it.timestamp in startTs until endTs }
    }

    // Process weekly metrics
    val monShort = getString("monday_short")
    val tueShort = getString("tuesday_short")
    val wedShort = getString("wednesday_short")
    val thuShort = getString("thursday_short")
    val friShort = getString("friday_short")
    val satShort = getString("saturday_short")
    val sunShort = getString("sunday_short")
    val dayKeys = listOf(monShort, tueShort, wedShort, thuShort, friShort, satShort, sunShort)

    val dailyDataMap = remember(weekSets, dayKeys) {
        val map = mutableMapOf<String, Double>()
        weekSets.forEach { set ->
            val shortDay = when (set.dayOfWeek.uppercase()) {
                "MONDAY" -> monShort
                "TUESDAY" -> tueShort
                "WEDNESDAY" -> wedShort
                "THURSDAY" -> thuShort
                "FRIDAY" -> friShort
                "SATURDAY" -> satShort
                "SUNDAY" -> sunShort
                else -> monShort
            }
            map[shortDay] = (map[shortDay] ?: 0.0) + 1.0 // Sets logged per day
        }
        map
    }

    val sessionsCount = dailyDataMap.size
    val totalSets = weekSets.size
    
    val totalDurationMin = remember(weekSets) {
        weekSets.groupBy { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.DAY_OF_YEAR)
        }.values.sumOf { daySets ->
            if (daySets.isEmpty()) 0L
            else {
                val minTs = daySets.minOf { it.timestamp }
                val maxTs = daySets.maxOf { it.timestamp }
                val diff = (maxTs - minTs) / 60000L
                maxOf(diff + 15, 30L)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        border = null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = getString("week_metrics"),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = getString("training_output"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = colorScheme.onSurface
                    )
                }
                
                // Week Navigator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val newCal = weekStartCalendar.clone() as Calendar
                            newCal.add(Calendar.DATE, -7)
                            weekStartCalendar = newCal
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = weekRangeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    IconButton(
                        onClick = {
                            val newCal = weekStartCalendar.clone() as Calendar
                            newCal.add(Calendar.DATE, 7)
                            weekStartCalendar = newCal
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    value = "$sessionsCount/7",
                    label = getString("sessions_label"),
                    color = colorScheme.primary
                )
                MetricItem(
                    value = totalDurationMin.toString(),
                    label = getString("min_logged_label"),
                    color = colorScheme.primary
                )
                MetricItem(
                    value = totalSets.toString(),
                    label = getString("total_sets_short"),
                    color = colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Weekly Output Chart
            WeeklyOutputChart(
                dailyData = dailyDataMap,
                dayKeys = dayKeys,
                selectedKey = selectedDayKey,
                onKeySelected = onDaySelected,
                primaryColor = colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
            
            // 4. Embedded Day Details
            if (selectedDayKey != null) {
                val daySets = remember(weekSets, selectedDayKey) {
                    weekSets.filter { set ->
                        val shortDay = when (set.dayOfWeek.uppercase()) {
                            "MONDAY" -> monShort
                            "TUESDAY" -> tueShort
                            "WEDNESDAY" -> wedShort
                            "THURSDAY" -> thuShort
                            "FRIDAY" -> friShort
                            "SATURDAY" -> satShort
                            "SUNDAY" -> sunShort
                            else -> monShort
                        }
                        shortDay == selectedDayKey
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                if (daySets.isNotEmpty()) {
                    DayDetailContent(
                        dayName = selectedDayKey,
                        sets = daySets,
                        isDeveloper = isDeveloper
                    )
                } else {
                    Text(
                        text = getString("no_workouts_for_day").replace("%1\$s", selectedDayKey),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayDetailContent(
    dayName: String,
    sets: List<LoggedSetEntity>,
    isDeveloper: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getString("workout_on_day").replace("%1\$s", dayName),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${sets.size} ${getString("sets_label")}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        sets.groupBy { it.exerciseName }.forEach { (exerciseName, exerciseSets) ->
            val firstSet = exerciseSets.firstOrNull() ?: return@forEach
            val isTime = WorkoutTemplates.isExerciseTimeBased(firstSet.exerciseName)
            val isBW = WorkoutTemplates.isBodyweightExercise(firstSet.exerciseName)
            
            // A selectorized stack and a band are not loaded in kg; showing "kg"
            // there is the same lie as labelling the field, so the summary reads
            // the exercise's own unit.
            val loadDetail = loggedLoadSummary(firstSet.exerciseName, firstSet.weightKg)
            val repsDetail = if (isTime) {
                if (isBW) "${firstSet.reps}s" else "$loadDetail x ${firstSet.reps}s"
            } else {
                if (isBW) "${firstSet.reps}" else "$loadDetail x ${firstSet.reps}"
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = translateExerciseName(exerciseName),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${exerciseSets.size} × $repsDetail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    value: String,
    label: String,
    color: Color
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun WeeklyOutputChart(
    dailyData: Map<String, Double>,
    dayKeys: List<String>,
    selectedKey: String?,
    onKeySelected: (String?) -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVolume = remember(dailyData) {
        val max = dailyData.values.maxOrNull() ?: 1.0
        max * 1.2
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dayKeys) {
                    detectTapGestures { offset ->
                        val barStep = size.width / dayKeys.size
                        val index = (offset.x / barStep).toInt().coerceIn(0, dayKeys.size - 1)
                        val tappedKey = dayKeys[index]
                        onKeySelected(if (tappedKey == selectedKey) null else tappedKey)
                    }
                }
        ) {
            val width = size.width
            val height = size.height - 24.dp.toPx()
            val barWidth = width / 12
            val spacing = (width - (barWidth * dayKeys.size)) / (dayKeys.size + 1)
            
            // Trend Line
            val linePath = Path()
            dayKeys.forEachIndexed { index, key ->
                val x = spacing + (barWidth / 2) + index * (barWidth + spacing)
                val volume = dailyData[key] ?: 0.0
                val y = height * 0.4f - (volume / maxVolume).toFloat() * height * 0.2f
                if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            
            drawPath(
                path = linePath,
                color = primaryColor.copy(alpha = 0.2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Bars
            dayKeys.forEachIndexed { index, key ->
                val x = spacing + index * (barWidth + spacing)
                val volume = dailyData[key] ?: 0.0
                val barHeight = (volume / maxVolume).toFloat() * height * 0.6f
                val cornerRadius = 4.dp.toPx()
                val isSelected = key == selectedKey
                
                if (volume > 0) {
                    val barColor = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.6f)
                    
                    // Glow
                    if (isSelected) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent)
                            ),
                            topLeft = Offset(x - 4.dp.toPx(), height - barHeight - 20.dp.toPx()),
                            size = Size(barWidth + 8.dp.toPx(), barHeight + 4.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius * 2)
                        )
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.7f))
                        ),
                        topLeft = Offset(x, height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                    )
                } else {
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.1f),
                        topLeft = Offset(x, height - 8.dp.toPx()),
                        size = Size(barWidth, 8.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayKeys.forEach { key ->
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (key == selectedKey) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = if (key == selectedKey) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Dialog for replacing an exercise. The exercise pool is pre-filtered to the
 * replaced exercise's equipment and muscle group, but the user can change both
 * to browse the complete pool.
 *
 * Tapping anywhere on an exercise replaces it and closes the dialog — the "+" is the
 * action itself, not a selection to be confirmed later. The tap first plays a short
 * "+" -> tick acknowledgement on that card, and only then does the swap land, so the
 * list doesn't jump out from under the finger.
 */
// Sentinel used for the "Any" muscle group option in the replace dialog.
private const val ANY_MUSCLE_GROUP_KEY = "__any_muscle_group__"

/**
 * How much of the window an EXPANDED filter row may take, before it starts scrolling inside itself.
 *
 * The filter rows sit above the exercise list in a dialog whose body is a fixed height, so an
 * unbounded expansion does not "push" anything - it overflows. Measured on the Pixel-class
 * emulator with the six body parts (104dp) plus the nineteen precise muscles (328dp): the
 * "Exercises (N)" count line ended up 828dp BELOW the dialog's own Cancel button, which is the
 * whole reason this cap exists rather than a hope that the vocabulary is small.
 *
 * A share of the window rather than a dp constant, like everything else measured in this file. The
 * number is not a guess: the block it caps is a 40dp row, a 104dp body-part grid and a 29dp caption
 * — 173dp of things the user must see — so the share has to leave roughly two rows of the precise
 * muscles (80dp) below them, or the vocabulary the filter exists for stays off the fold. At 891dp
 * that is 249dp of chips, and the list keeps 190dp: two exercise cards while the filter is open,
 * against 401dp when it is closed. Inert until a filter is opened, since the collapsed row is 40dp.
 */
private const val EXPANDED_FILTER_MAX_WINDOW_SHARE = 0.28f

/** How long the picked card's "+" takes to turn into a tick before the swap is applied. */
private const val REPLACE_TICK_MILLIS = 220L

/**
 * A beat between applying the swap and closing the dialog: it gives the new exercise time to
 * reach the list behind the scrim, so the change is part of the transition rather than a
 * jump the instant the dialog disappears.
 */
private const val REPLACE_SETTLE_MILLIS = 160L

/**
 * The exercises matching an add-dialog filter selection — the same lookup the
 * replace dialog runs: empty muscle selection = every group, empty equipment
 * selection = every category, and the muscle filter is a broad body part that
 * answers with the exercises whose authoritative primary muscle belongs to it
 * (selecting "Legs" offers every leg exercise, whatever precise muscle it
 * names). Returns the pool sorted, ready to feed [SearchableExerciseField].
 */
fun filteredExercisesForPickers(
    allExercises: List<String>,
    selectedMuscleGroup: String?,
    selectedEquipment: List<String>,
    /**
     * The user's granted FINE tags, when known. The coarse chips group every
     * bodyweight movement under one "Bodyweight" chip — without this gate a
     * dumbbells+chair setup was offered Pull-Ups and Muscle-Ups through it.
     * The generator already gates builds this way; the pickers now match.
     * Null = unrestricted.
     */
    grantedTags: Set<String>? = null
): List<String> {
    val equipmentFilter = selectedEquipment.ifEmpty { WorkoutTemplates.allEquipmentCategories }
    val pool = when (selectedMuscleGroup) {
        null, "", ANY_MUSCLE_GROUP_KEY ->
            equipmentFilter.flatMap { WorkoutTemplates.getAllExercisesForEquipment(it) }
        else ->
            equipmentFilter.flatMap { WorkoutTemplates.getExercisesForEquipment(it, selectedMuscleGroup) }
    }
    val gated = if (grantedTags != null) pool.filter { ExerciseEquipmentTags.isAvailable(it, grantedTags) } else pool
    // Popularity order (not alphabetical): the suggestions the user scrolls
    // past first are the movements programs actually use, not "Ab Wheel
    // Rollout" because it sorts first.
    return ExercisePopularity.sortedByPopularity(
        gated.distinct().filter { it in allExercises.toSet() || allExercises.isEmpty() }
    )
}

/**
 * The equipment filter row from the replace dialog: one chip per category,
 * none selected meaning "every category". Shared with the add dialog so both
 * pickers narrow the list by the same keys.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquipmentFilterRow(
    selectedEquipment: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // "Other" is not offered: it is not equipment the user can be said to lack, so a chip for it
    // is a control they never chose sitting among the ones they did. It stays in the FILTER
    // (see [WorkoutTemplates.filterableEquipmentCategories]) - only its chip is gone.
    val equipmentOptions = WorkoutTemplates.filterableEquipmentCategories.map { category ->
        category to getString(WorkoutTemplates.equipmentLabelKey(category))
    }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        equipmentOptions.forEach { (key, label) ->
            FilterChip(
                selected = selectedEquipment.contains(key),
                onClick = { onToggle(key) },
                leadingIcon = {
                    // Same drawing as the setup's tile grid, so "Barbell" reads
                    // as the same object everywhere equipment is offered.
                    EquipmentIconImage(
                        icon = equipmentCategoryIcon(key),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { Text(label, maxLines = 1) },
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

/**
 * How many equipment glyphs the COLLAPSED summary draws before it stops and
 * counts the rest. Four is what fits beside the toggle at the widest font scale
 * the app supports without the row wrapping.
 */
private const val MAX_COLLAPSED_EQUIPMENT_ICONS = 4

/**
 * The equipment filter as a COLLAPSED summary, with the full chip grid one tap
 * away.
 *
 * WHY THIS EXISTS. The replace dialog used to lay out all eight equipment chips
 * unconditionally. On a phone whose font scale is large that is not a filter row
 * at all: measured on the Pixel-class emulator at 1.8x, one labelled chip is
 * 466px of a 738px content row, so the FlowRow chipped ONE PER LINE, eight lines
 * deep — 1300px of a 2256px dialog — and the exercise list every user opens this
 * dialog for was pushed off the bottom of the screen entirely. The owner's report
 * was exactly that: "the user cant even see exercises to replace".
 *
 * So the equipment already filtering the list is drawn as GLYPHS (the same
 * drawings the setup's tile grid uses, so "Barbell" is the same object
 * everywhere), which cost a fixed 22dp regardless of the font scale, and the
 * grid of labels appears only when the user asks for it. Collapsed is the
 * default because the filter is already applied: the common case is reading the
 * list, not re-choosing the equipment.
 *
 * [initiallyExpanded] exists for the one case where the choice IS the task: a
 * caller with no equipment selected yet (nothing to summarise) can open the grid
 * up front.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquipmentFilterCollapsible(
    selectedEquipment: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val window = LocalWindowSize.current
    Column(
        // The same cap as the muscle row's (see [EXPANDED_FILTER_MAX_WINDOW_SHARE]): one rule for
        // both filters, so whichever is open cannot eat the list the dialog exists to show.
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = (window.windowHeightDp * EXPANDED_FILTER_MAX_WINDOW_SHARE).dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // The word "Equipment" is the first thing to go at a large font scale:
            // it costs ~200px of a 738px row on the emulator's 1.8x, which is more
            // than the glyphs it labels. The row still reads as "the equipment
            // filter" from the glyphs and the chevron, and the accessible name is
            // on the toggle either way.
            if (!window.isLargeText) {
                Text(
                    text = getString("equipment"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            if (selectedEquipment.isEmpty()) {
                // Nothing selected means every category is included, which is a
                // fact worth stating rather than an empty row to puzzle over.
                Text(
                    text = getString("equipment_all"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Only the categories the UI is allowed to draw a chip for: "Other" filters the
                // list (a plank is a real exercise) but is not a piece of kit, so it is not shown
                // and cannot be unticked - see [WorkoutTemplates.filterableEquipmentCategories].
                val drawn = selectedEquipment.filter { it in WorkoutTemplates.filterableEquipmentCategories }
                if (drawn.isEmpty()) {
                    // The kit-free category is the ONLY thing filtering - which is what replacing a
                    // plank or a stretch starts from for a user who never restricted their kit.
                    // It has no glyph of its own, so the row states the fact in words rather than
                    // leaving an empty line where the icons belong.
                    Text(
                        text = getString("equipment_tag_none_required"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    drawn.take(MAX_COLLAPSED_EQUIPMENT_ICONS).forEach { key ->
                        // The label moves to the content description, where it costs no
                        // width: this row has to survive the 1.8x font scale that made
                        // the labelled version unreadable.
                        //
                        // THE ACCENT, not the inherited text colour. Owner, 2026-10-11: *"make
                        // sure the minimized icons ... is not pitch black but a theme color
                        // maybe a bit more acented than the usual theme primary color"*. They
                        // inherited `LocalContentColor` before, which is the near-black body
                        // colour on the light themes and the muted grey on the dark ones - the
                        // row of glyphs that says "this is the kit filtering your list" was the
                        // quietest thing in the dialog. [equipmentGlyphTint] is the single place
                        // that decision lives.
                        EquipmentIconImage(
                            icon = equipmentCategoryIcon(key),
                            contentDescription = getString(WorkoutTemplates.equipmentLabelKey(key)),
                            tint = equipmentGlyphTint(),
                            // Tagged per category so a test can assert WHICH kit glyphs this row
                            // draws - including that it never draws one for "Other".
                            modifier = Modifier
                                .size(22.dp)
                                .semantics { testTag = "equipment_glyph_$key" }
                        )
                    }
                    if (drawn.size > MAX_COLLAPSED_EQUIPMENT_ICONS) {
                        Text(
                            text = "+${drawn.size - MAX_COLLAPSED_EQUIPMENT_ICONS}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            if (window.isLargeText) {
                // Icon-only: the chevron is what tells the user the row opens, and
                // at this font scale the words would push the glyphs off the row.
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { testTag = "equipment_filter_toggle" }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = getString(if (expanded) "show_less" else "show_more"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.semantics { testTag = "equipment_filter_toggle" }
                ) {
                    Text(
                        text = getString(if (expanded) "show_less" else "show_more"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (expanded) {
            EquipmentFilterRow(selectedEquipment = selectedEquipment, onToggle = onToggle)
        }
    }
}

/**
 * The muscle filter, as the SAME compact row as the equipment filter.
 *
 * Owner, 2026-10-09: *"allow the user to filter which muscle too for when replacing an
 * exercise, make sure the filter still stays nicely spacious yet compact and small"*.
 *
 * It used to be a full-width `OutlinedTextField` dropdown — a form field, with a floating
 * label, 56dp of height and a chevron - sitting directly under the one-row equipment summary.
 * Two filters, two visual languages, and the heavier one belonged to the simpler choice:
 * there are six body parts and nineteen muscles against eight equipment categories. The
 * owner's instruction was not to add a filter (this dialog had one) but to make the muscle
 * one read like the equipment one: a single line showing what is filtering the list, with the
 * choices one tap away, and every dp it does not spend going to the exercise list, which is
 * what the dialog is for.
 *
 * SO THE SHAPE IS DELIBERATELY THE EQUIPMENT ROW'S: label, current selection, "Show more" /
 * "Show less". The words do the work the equipment row gives to glyphs, because a muscle has
 * no icon in this app and inventing nineteen would be a vocabulary of its own. The label
 * yields at a large font scale exactly as the equipment row's does — the row then reads as
 * "selected value + chevron", which still says "this is a filter and it is openable".
 *
 * EXPANDED, IT IS TWO ROWS RATHER THAN ONE, and that is the "nicely spacious" half: body
 * parts first (the predictable partition, and what the filter STARTS on), then the precise
 * muscles under their own caption, because the two answer different questions — "another back
 * movement" against "something else for my lats". A single mixed grid of 25 chips would make
 * the user hunt for the six they actually use.
 *
 * SINGLE-SELECT, unlike equipment: the pool lookup answers one muscle at a time
 * ([WorkoutTemplates.getExercisesForEquipment]), and "Legs OR Chest" is what the Any chip is
 * for. Tapping the selected chip again keeps it selected rather than clearing it — clearing is
 * what the Any chip and the dialog's Reset are for, and a chip that silently unsets itself is
 * how a filter looks broken.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuscleGroupFilterCollapsible(
    selectedMuscleGroup: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val window = LocalWindowSize.current
    val anyLabel = getString("any_muscle_group")

    Column(
        // The CAP and the SCROLL now live on the expanded block, not here: the row that says which
        // group is filtering has to survive the scroll of the muscles under it (owner, 2026-10-11).
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Same surrender as the equipment row at a large font scale: the word costs
            // more of the row than the value it labels.
            if (!window.isLargeText) {
                Text(
                    text = getString("muscle_group"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            // The current selection, drawn as a chip rather than as text: the equipment row
            // shows glyphs, and a filter's value reading as a value (not as another label) is
            // what keeps the two rows legible side by side.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selectedMuscleGroup == null) Color.Transparent
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                border = BorderStroke(
                    1.dp,
                    if (selectedMuscleGroup == null) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                ),
            ) {
                // The tag sits on the TEXT rather than on the Surface around it, so a test can
                // read the value the row is showing ("Back") instead of only proving a box is
                // on screen — the difference between "the row exists" and "the row is honest".
                Text(
                    text = if (selectedMuscleGroup == null) anyLabel
                           else translateMuscleGroup(selectedMuscleGroup),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selectedMuscleGroup == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .semantics { testTag = "muscle_filter_value" }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // The toggle is the same control in both states — a text button on a roomy
            // window, an icon button at a large font scale, where the words would push the
            // value off the row.
            if (window.isLargeText) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { testTag = "muscle_filter_toggle" }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = getString(if (expanded) "show_less" else "show_more"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.semantics { testTag = "muscle_filter_toggle" }
                ) {
                    Text(
                        text = getString(if (expanded) "show_less" else "show_more"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (expanded) {
            // THE CHIPS SCROLL, THIS ROW DOES NOT. Owner, 2026-10-11: *"make sure the small
            // 'muscle group: back' is shown also while scrolling through each precise muscle so
            // that it shows what these muscle are (from the back cathegory / group)"*. The row
            // above carries that value, so it stays put while the muscles below it are read.
            //
            // CAPPED AND SCROLLABLE, because nothing about a chip list bounds it: see
            // [EXPANDED_FILTER_MAX_WINDOW_SHARE]. The six body parts fit; the nineteen precise
            // muscles do not, so they scroll inside the block instead of pushing the exercise list
            // out of the dialog — which is what the measurement recorded when this was unbounded.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (window.windowHeightDp * EXPANDED_FILTER_MAX_WINDOW_SHARE).dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BODY PARTS — the same six the filter has always offered, with Any first so
                // "no muscle preference" is a visible choice rather than the absence of one.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "muscle_filter_broad_row" }
                ) {
                    FilterChip(
                        selected = selectedMuscleGroup == null,
                        onClick = { onSelect(null) },
                        label = { Text(anyLabel, maxLines = 1) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.semantics { testTag = "muscle_chip_any" }
                    )
                    WorkoutTemplates.getBroadMuscleGroups().forEach { group ->
                        FilterChip(
                            selected = selectedMuscleGroup == group,
                            onClick = { onSelect(group) },
                            label = { Text(translateMuscleGroup(group), maxLines = 1) },
                            shape = RoundedCornerShape(10.dp),
                            // A handle per chip, so "does picking this muscle narrow the list" can
                            // be asked of the chip itself rather than of a label that a card's
                            // description might also contain.
                            modifier = Modifier.semantics { testTag = "muscle_chip_$group" }
                        )
                    }
                }

                // PRECISE MUSCLES — under their own caption, so the nineteen do not read as a
                // longer version of the six. These are the groups the catalog actually stores on
                // each exercise, so each one answers with the movements that train it.
                //
                // AND THEY ARE THE CHOSEN GROUP'S OWN MUSCLES. Owner, 2026-10-11: *"when
                // selecting muscle group back, then make sure that the precise muscles are shown
                // only of that 'back' cathegory / group"*. The six broad chips partition the
                // nineteen exactly, so narrowing loses nothing - and the caption NAMES the group,
                // because a list that silently shrank cannot say why it shrank, and "Lats" on
                // its own does not tell the user it came from Back. Both halves answer the same
                // question the owner asked: WHICH GROUP AM I LOOKING AT.
                val preciseMuscles = WorkoutTemplates.getPreciseMuscleGroupsFor(selectedMuscleGroup)
                val preciseGroupLabel = selectedMuscleGroup
                    ?.let { WorkoutTemplates.getTopicForMuscle(it) }
                    ?.let { translateMuscleGroup(it) }
                Text(
                    text = if (preciseGroupLabel == null) getString("muscle_filter_precise")
                           else getString("muscle_filter_precise_group", preciseGroupLabel),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { testTag = "muscle_filter_precise_caption" }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "muscle_filter_precise_row" }
                ) {
                    preciseMuscles.forEach { muscle ->
                        FilterChip(
                            selected = selectedMuscleGroup == muscle,
                            onClick = { onSelect(muscle) },
                            label = { Text(translateMuscleGroup(muscle), maxLines = 1) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.semantics { testTag = "muscle_chip_precise_$muscle" }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReplaceExerciseDialog(
    exercise: RoutineExerciseEntity,
    onDismiss: () -> Unit,
    onReplace: (name: String, muscleGroup: String) -> Unit,
    /**
     * The equipment the user said they have, as coarse categories (the saved
     * fine tags reduced to "Barbell", "Dumbbell", …). Empty = unrestricted —
     * the chips then start from the replaced exercise's own category, as
     * before.
     */
    userEquipmentCategories: List<String> = emptyList(),
    /** The user's granted FINE tags — see [ExerciseEditorDialog]'s note. */
    userGrantedTags: Set<String>? = null
) {
    val focusManager = LocalFocusManager.current
    val broadMuscleGroups = remember { WorkoutTemplates.getBroadMuscleGroups() }

    // The filter starts on what the user can actually use. When they declared
    // their kit, the replaced exercise's own category is irrelevant — offering
    // barbell alternatives for a barbell row in a no-barbell setup is how the
    // leak read as "the barbell filter doesn't work". Unrestricted users keep
    // the old default: the replaced exercise's own category.
    var selectedEquipment by remember(exercise.name) {
        mutableStateOf(
            userEquipmentCategories.ifEmpty {
                listOf(WorkoutTemplates.getEquipmentForExercise(exercise.name))
                    .filter { it.isNotEmpty() }
            }
        )
    }
    var selectedMuscleGroup by remember(exercise.name) {
        // The filter offers broad body parts, so the exercise's own muscle group is
        // reduced to its topic ("Lats" and "Upper Back" both start the filter on
        // "Back") — the precise group still labels the replaced exercise itself.
        mutableStateOf(
            WorkoutTemplates
                .getTopicForMuscle(resolvePreciseMuscleGroup(exercise.name, exercise.muscleGroup))
                .takeIf { it != "Other" }
                ?: broadMuscleGroups.first()
        )
    }

    // The card whose acknowledgement is playing: its "+" turns into a tick while the swap is
    // held back for [REPLACE_TICK_MILLIS]. Nothing else can be picked meanwhile, so a second
    // tap can't swap a different exercise in behind the first one.
    var pickedName by remember { mutableStateOf<String?>(null) }

    // A card's method text starts clamped to two lines and can be opened in place. Kept as
    // a set rather than one slot, so comparing two alternatives does not collapse the first
    // the moment the second is opened.
    var expandedAlternatives by remember { mutableStateOf(emptySet<String>()) }

    val scope = rememberCoroutineScope()

    fun pick(name: String, muscleGroup: String) {
        if (pickedName != null) return
        pickedName = name
        scope.launch {
            delay(REPLACE_TICK_MILLIS)
            onReplace(name, muscleGroup)
            delay(REPLACE_SETTLE_MILLIS)
            onDismiss()
        }
    }

    val alternatives = remember(exercise.name, selectedEquipment, selectedMuscleGroup) {
        val equipmentFilter = selectedEquipment.ifEmpty { WorkoutTemplates.allEquipmentCategories }
        var pool = if (selectedMuscleGroup == ANY_MUSCLE_GROUP_KEY) {
            equipmentFilter.flatMap { WorkoutTemplates.getAllExercisesForEquipment(it) }
        } else {
            equipmentFilter.flatMap { WorkoutTemplates.getExercisesForEquipment(it, selectedMuscleGroup) }
        }
        // Fine-tag gate (same as the generator): the coarse Bodyweight chip
        // covers every bar-less movement, so without it a dumbbells+chair setup
        // was offered Pull-Ups and Muscle-Ups. Null when unrestricted.
        val granted = WorkoutTemplates.pickerGrantedTags(userGrantedTags, userEquipmentCategories)
        if (granted != null) pool = pool.filter { ExerciseEquipmentTags.isAvailable(it, granted) }
        ExercisePopularity.sortedByPopularity(
            pool.distinct().filter { it.lowercase() != exercise.name.lowercase() }
        )
    }

    // Search over the filtered pool. The same typo-tolerant matcher the
    // add-exercise field uses, and it matches canonical AND translated names —
    // a German user typing "kniebeuge" finds "Barbell Back Squat". A blank
    // query leaves the popularity-ordered list untouched.
    var searchQuery by remember(exercise.name) { mutableStateOf("") }
    val searchCandidates = remember(alternatives) {
        alternatives.map { name ->
            ExerciseSearch.Candidate(name, ExerciseLocalization.allNamesFor(name))
        }
    }
    // Same precomputation as the add-exercise field: the per-name normalisation is
    // query-independent, so paying it per keystroke was pure waste on the main
    // thread. See ExerciseSearch.PreparedIndex.
    val searchIndex = remember(searchCandidates) {
        ExerciseSearch.PreparedIndex.build(searchCandidates)
    }
    val visibleAlternatives = remember(searchQuery, searchIndex, alternatives) {
        if (searchQuery.isBlank()) alternatives
        else ExerciseSearch.searchPrepared(searchIndex, searchQuery).map { it.canonical }
    }

    val window = LocalWindowSize.current

    // Every popup in the app renders through [AppDialog] now, so the replace
    // dialog stopped being "the one with its own surface": same container colour,
    // same corner radius, same width cap as the add/edit/filter popups. What it
    // still asks for is the whole window, because its body is a lazy list of
    // alternatives that must not be squeezed to one card - that was the original
    // "you only see one at a time and have to scroll a tiny space" complaint, and
    // the shell keeps the fix (the list takes every dp the header leaves) while
    // the look matches its siblings.
    AppDialog(
        onDismissRequest = onDismiss,
        heightFraction = 0.94f,
        scrollableBody = false
    ) {
                Text(
                    text = getString("replace_exercise"),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // A structural branch, not a per-device number: on a window too short to give
                // the list a decent share, the header drops to what the user actually needs —
                // which exercise is being replaced, and the filters — and every dp it saves
                // goes to the alternatives. On a tall window nothing is hidden.
                //
                // [WindowSize.isLargeText] joins it because a large font scale does to this
                // dialog exactly what a short window does: it makes the chrome taller than the
                // list. Measured on the Pixel-class emulator at 1.8x, the notice alone was
                // 219px of a 2256px dialog. The rule is the same in both cases — the list is
                // what the dialog is FOR, so the notice yields.
                val compactHeader = window.isShort || window.isLargeText

                Text(
                    text = getString("replace_exercise_desc", translateExerciseName(exercise.name)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compactHeader) 1 else 3,
                    overflow = TextOverflow.Ellipsis
                )

                // SEARCH — narrows the list beneath the filters so a known target
                // never needs scrolling to reach. The ✕ clears without closing the
                // dialog, so refining a mistyped query is one tap.
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = getString("replace_search_hint"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = getString("close")
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // EQUIPMENT FILTER — collapsed to the glyphs of what is already
                // filtering the list, with the labelled grid one tap away.
                //
                // This is the fix for "the user can't even see exercises to
                // replace": the eight labelled chips used to be laid out
                // unconditionally, which at a large font scale is one chip per
                // line and 1300px of the dialog spent before the list begins.
                // Collapsed occupies ONE row whatever the font scale is.
                EquipmentFilterCollapsible(
                    selectedEquipment = selectedEquipment,
                    onToggle = { key ->
                        selectedEquipment = if (selectedEquipment.contains(key)) {
                            selectedEquipment - key
                        } else {
                            selectedEquipment + key
                        }
                    }
                )

                // MUSCLE FILTER — the equipment row's shape, because it is the same kind of
                // control: one line of summary while the list below is the thing being read,
                // and the body parts + precise muscles only when the user is choosing one.
                MuscleGroupFilterCollapsible(
                    selectedMuscleGroup = selectedMuscleGroup.takeIf { it != ANY_MUSCLE_GROUP_KEY },
                    onSelect = { selectedMuscleGroup = it ?: ANY_MUSCLE_GROUP_KEY }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = getString("exercises_count", visibleAlternatives.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    // Reset widens the view back to everything: clear the query and
                    // both filters, mirroring the welcome screen's filter dialog.
                    if (searchQuery.isNotBlank() || selectedEquipment.isNotEmpty() ||
                        selectedMuscleGroup != ANY_MUSCLE_GROUP_KEY
                    ) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedEquipment = emptyList()
                                selectedMuscleGroup = ANY_MUSCLE_GROUP_KEY
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(getString("reset"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (visibleAlternatives.isEmpty()) {
                    Text(
                        text = getString("no_exercises_for_filters"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        // weight(1f): the list takes whatever the compact header leaves,
                        // instead of a fixed cap a short window cannot honour.
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(visibleAlternatives) { _, altName ->
                            // Always store the exercise's own precise muscle, whichever way it
                            // was found: the filter is a broad body part, but the data behind
                            // the label (and the Analytics split) stays precise.
                            val altMuscleGroup = WorkoutTemplates.getMuscleGroupForExercise(altName)
                                .ifBlank { selectedMuscleGroup }
                            val (altDesc, _) = getExerciseDetails(altName, altMuscleGroup, exercise.targetIntensity)

                            // Each card runs its own acknowledgement off the same animation, so
                            // only the picked one turns its "+" into a tick and lifts its tint.
                            val confirmProgress by animateFloatAsState(
                                targetValue = if (pickedName == altName) 1f else 0f,
                                animationSpec = tween(
                                    durationMillis = REPLACE_TICK_MILLIS.toInt(),
                                    easing = FastOutSlowInEasing
                                ),
                                label = "replaceConfirm"
                            )

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = lerp(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.primaryContainer,
                                        confirmProgress
                                    )
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        val lift = 1f + 0.02f * confirmProgress
                                        scaleX = lift
                                        scaleY = lift
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        focusManager.clearFocus()
                                        pick(altName, altMuscleGroup)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ReplaceActionMark(progress = confirmProgress)

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = translateExerciseName(altName),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (altDesc.isNotBlank()) {
                                            val isExpanded = altName in expandedAlternatives
                                            var isClamped by remember(altName) { mutableStateOf(false) }
                                            val descriptionInteraction = remember { MutableInteractionSource() }
                                            val toggleInteraction = remember { MutableInteractionSource() }

                                            // A method text short enough to fit two lines is not a
                                            // toggle at all: every tap on it must stay the tap that
                                            // replaces the exercise. Only text the card actually cut
                                            // off (or one already open) takes the press, and it takes
                                            // it before the card can, so opening a description never
                                            // swaps the exercise by accident.
                                            val canToggle = isExpanded || isClamped
                                            val toggle: () -> Unit = {
                                                expandedAlternatives =
                                                    if (isExpanded) expandedAlternatives - altName
                                                    else expandedAlternatives + altName
                                            }

                                            Text(
                                                text = altDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                                overflow = TextOverflow.Ellipsis,
                                                onTextLayout = { layout ->
                                                    if (!isExpanded) isClamped = layout.hasVisualOverflow
                                                },
                                                modifier = Modifier.clickable(
                                                    enabled = canToggle,
                                                    interactionSource = descriptionInteraction,
                                                    indication = null,
                                                    onClick = toggle
                                                )
                                            )

                                            if (canToggle) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clickable(
                                                            interactionSource = toggleInteraction,
                                                            indication = null,
                                                            onClick = toggle
                                                        )
                                                        .padding(top = 2.dp)
                                                ) {
                                                    Text(
                                                        text = getString(if (isExpanded) "show_less" else "show_more"),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                                                      else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = {
                        focusManager.clearFocus()
                        onDismiss()
                    }) {
                        Text(getString("cancel"))
                    }
                }
    }
}

/**
 * The replace action on an alternative-exercise card, as a mark that leads the row: a "+"
 * that turns into a tick as [progress] runs from 0 to 1. It exists so a tap is visible
 * before the swap lands — the card would otherwise be replaced under the finger with no
 * acknowledgement that anything was picked.
 */
@Composable
private fun ReplaceActionMark(progress: Float) {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = getString("replace"),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    // Swung away to the left, as if the tick were turned over into place.
                    alpha = 1f - progress
                    val shrink = 1f - 0.45f * progress
                    scaleX = shrink
                    scaleY = shrink
                    rotationZ = -90f * progress
                }
        )
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    alpha = progress
                    val grow = 0.4f + 0.6f * progress
                    scaleX = grow
                    scaleY = grow
                }
        )
    }
}
