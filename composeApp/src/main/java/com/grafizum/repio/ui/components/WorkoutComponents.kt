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
import androidx.compose.ui.graphics.RectangleShape
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
import com.grafizum.repio.data.ExerciseSearch
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
import androidx.compose.ui.window.DialogProperties

// Color mappings per muscle group
fun getMuscleGroupColor(muscleGroup: String): Color {
    return when (muscleGroup.lowercase().trim()) {
        "chest", "pectorals" -> Color(0xFFEF4444) // Red
        "abs", "abdominals" -> Color(0xFFF59E0B) // Amber
        "core", "obliques" -> Color(0xFFEAB308) // Yellow
        "legs", "quadriceps", "hamstrings", "glutes", "calves",
        "hip adductors", "hip abductors", "adductors", "abductors" -> Color(0xFF10B981) // Emerald
        "back", "lats", "upper back", "traps", "upper traps", "spinal erectors", "erectors" -> Color(0xFF3B82F6) // Blue
        "biceps" -> Color(0xFF8B5CF6) // Purple
        "triceps" -> Color(0xFF06B6D4) // Cyan
        "arms" -> Color(0xFF6366F1) // Indigo
        "forearms" -> Color(0xFFF97316) // Orange
        "shoulders", "deltoids", "front deltoids", "lateral deltoids", "rear deltoids" -> Color(0xFFEC4899) // Pink
        else -> Color(0xFF64748B) // Grey
    }
}

/**
 * [getMuscleGroupColor] tuned for the surface it is about to be drawn on. A mid-tone
 * hue that reads well on the dark themes is too washed out to serve as text or as a
 * small chip on the light ones (yellow most of all), so on a light surface it is
 * deepened one step. The hue — and therefore which body part it means — never
 * changes, only the depth.
 */
@Composable
fun muscleGroupColor(muscleGroup: String): Color {
    val base = getMuscleGroupColor(muscleGroup)
    return if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) deepenForLightSurface(base) else base
}

/** One step deeper, for legibility on a light background. */
private fun deepenForLightSurface(color: Color): Color = lerp(color, Color.Black, 0.3f)

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

@Composable
fun MuscleGroupBadge(
    muscleGroup: String,
    exerciseName: String = "",
    modifier: Modifier = Modifier
) {
    val effective = resolvePreciseMuscleGroup(exerciseName, muscleGroup)
    val topic = WorkoutTemplates.getTopicForMuscle(effective)
    val color = muscleGroupColor(topic)
    val isDeveloper = LocalDeveloperSkin.current
    val topicLabel = translateMuscleGroup(topic)
    val preciseLabel = translateMuscleGroup(effective)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Surface(
            color = if (isDeveloper) Color.Black else color.copy(alpha = 0.2f),
            contentColor = if (isDeveloper) Color.White else color,
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
            border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null
        ) {
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
    val lower = name.trim().lowercase()
    return when {
        lower == "row" || lower == "rows" -> getString("ex_row")
        lower == "pullover" || lower == "pullovers" -> getString("ex_dumbbell_pullover")
        lower == "curl" || lower == "curls" -> getString("ex_curls")
        lower == "hammer curl" || lower == "hammer curls" ||
            lower == "dumbbell hammer curl" || lower == "dumbbell hammer curls" -> getString("ex_hammer_curl")
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
        lower == "dumbbell lunge" || lower == "dumbbell lunges" -> getString("ex_dumbbell_lunge")
        lower == "barbell floor press" || lower == "barbell floor presses" -> getString("ex_barbell_floor_press")
        lower == "dumbbell floor press" || lower == "dumbbell floor presses" -> getString("ex_dumbbell_floor_press")

        else -> name
    }
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
    compact: Boolean = false
) {
    val isDeveloper = LocalDeveloperSkin.current
    val window = LocalWindowSize.current

    Card(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDeveloper) 0.dp else 4.dp),
        border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
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
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                                    .background(if (isDeveloper) Color.White else iconColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isDeveloper) Color.Black else iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
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
                                color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface
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
                                tint = if (isDeveloper) Color.White else iconColor,
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
                            color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface
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
                            .clip(if (isDeveloper) RoundedCornerShape(0.dp) else CircleShape)
                            .background(if (isDeveloper) Color.White else iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isDeveloper) Color.Black else iconColor,
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
                containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
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
                containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
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
                containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
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
    // (see ExerciseDescriptions.kt), so distinct movements never share text.
    var description = getExerciseDescription(name) ?: genericExerciseDescription(name, muscleGroup)

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
        cleanName.contains("cable crunch") -> getString("ex_cable_crunch_desc")
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
    val focusManager = LocalFocusManager.current

    Card(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isDeveloper) BorderStroke(2.dp, Color.White)
                 else if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                 else null,
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
                        tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = translateExerciseName(exercise.name),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = if (expanded) 2 else 1,
                                overflow = if (isDeveloper) TextOverflow.Clip else TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MuscleGroupBadge(muscleGroup = exercise.muscleGroup, exerciseName = exercise.name)
                        }
                        if (showActions) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (exercise.targetReps.contains("sec", ignoreCase = true))
                                        getQuantityString("sets_x_time_pl", exercise.targetSets, exercise.targetSets, exercise.targetReps)
                                    else getQuantityString("sets_x_reps_pl", exercise.targetSets, exercise.targetSets, exercise.targetReps),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (exercise.targetIntensity.isNotBlank()) {
                                    val rpeExpl = getRpeRirExplanation(exercise.targetIntensity)
                                    val displayIntensity = if (rpeExpl.isNotBlank()) "${exercise.targetIntensity} ($rpeExpl)" else exercise.targetIntensity
                                    Text(
                                        text = " • $displayIntensity",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDeveloper) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (exercise.tempo.isNotBlank()) {
                                    Text(
                                        text = " • ${getTempoLabel(exercise.tempo)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDeveloper) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
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
                        tint = if (isDeveloper) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
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
                                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
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
                                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
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
                                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.error),
                                        border = BorderStroke(1.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
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
                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCatExpanded || hasSelectedInCat) {
                        if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (hasSelectedInCat) {
                        if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary
                    } else if (isCatExpanded) {
                        if (isDeveloper) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else Color.Transparent
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
                                color = if (hasSelectedInCat) (if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurface
                            )
                            if (hasSelectedInCat && !isCatExpanded) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isCatExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
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
                                        if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
                                    } else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) {
                                        if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                                    } else MaterialTheme.colorScheme.onSurface,
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (isSelected) {
                                        if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
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
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
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
                    tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
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
        if (isDeveloper) {
            val index = overviewItems.indexOfFirst { it.first == key }
            val grey = (0.2f + (index.coerceAtLeast(0).toFloat() / overviewItems.size.coerceAtLeast(1)) * 0.7f).coerceIn(0f, 1f)
            Color(grey, grey, grey)
        } else {
            muscleGroupColor(WorkoutTemplates.getTopicForMuscle(key))
        }
    }

    fun entryColor(key: String): Color = groupColors[key] ?: Color(0xFF64748B)

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
                    // Same dimmed muscle-group color as the dots and arcs.
                    color = entryColor(key),
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
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
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
                        shape = if (isDeveloper) RectangleShape else RoundedCornerShape(10.dp)
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
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
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
                    // Broad body-part header chip with the section's total sets.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                sectionColor.copy(alpha = 0.12f),
                                if (isDeveloper) RectangleShape else RoundedCornerShape(8.dp)
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
                            color = sectionColor,
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
    val barRadius = if (isDeveloper) 0.dp else 3.dp
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
        mg.contains("chest") -> "chest"
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
            .clip(if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp))
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
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(7.dp),
            shadowElevation = if (isDeveloper) 0.dp else 1.dp
        ) {}

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

    // Typo-tolerant search: still finds "Dumbbell Curls" from "dumbell curls"
    // or "Bench Press" from "bnech pres". Exact/prefix matches stay on top.
    val filteredExercises = remember(value, allExercises) {
        ExerciseSearch.search(allExercises, value)
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredExercises.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
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
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(selection, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                // Avoid calling heavy functions inside Composable if possible, but this is a small list
                                val muscleGroup = remember(selection) { WorkoutTemplates.getMuscleGroupForExercise(selection) }
                                val (desc, _) = getExerciseDetails(selection, muscleGroup, "")
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
                            onValueChange(selection)
                            onItemSelected(selection)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}


fun getMissingMuscleGroups(exercises: List<RoutineExerciseEntity>): List<String> {
    val presentGroups = exercises.map { it.muscleGroup.lowercase().trim() }.toSet()

    val requiredGroups = listOf(
        "chest" to listOf("chest"),
        "back" to listOf("back", "lats", "upper back", "traps"),
        "legs" to listOf("legs", "quads", "quadriceps", "hamstrings", "calves", "glutes", "adductors", "abductors"),
        "shoulders" to listOf("shoulders", "delt", "deltoid"),
        "biceps" to listOf("biceps", "bicep"),
        "triceps" to listOf("triceps", "tricep"),
        "core" to listOf("abs", "core", "abdominals", "obliques", "erectors")
    )

    return requiredGroups.filter { (_, keywords) ->
        keywords.none { keyword -> 
            presentGroups.any { it.contains(keyword) } 
        }
    }.map { it.first }
}

@Composable
fun ThemedToast(
    message: String,
    themeKey: String,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (visible) {
        LaunchedEffect(visible) {
            delay(2500L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                // Placeholder for icon for now
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MissingBodyPartsWarningCard(
    missingParts: List<String>,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    if (missingParts.isEmpty()) {
        Card(
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDeveloper) Color.White else Color(0xFF10B981)
            ),
            border = if (isDeveloper) BorderStroke(2.dp, Color.Black) else null,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDeveloper) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = getString("balanced_workout_routine"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDeveloper) Color.Black else Color.White
                    )
                    Text(
                        text = getString("balanced_workout_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDeveloper) Color.Black else Color.White
                    )
                }
            }
        }
    } else {
        Card(
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDeveloper) Color.White
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            ),
            border = if (isDeveloper) BorderStroke(2.dp, Color.Black) else null,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = getString("incomplete_workout_routine"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    val translatedParts = mutableListOf<String>()
                    for (part in missingParts) {
                        translatedParts.add(translateMuscleGroup(part))
                    }

                    val warningText = getString("incomplete_workout_desc", translatedParts.joinToString(", "))

                    Text(
                        text = warningText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onErrorContainer
                    )
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
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    placeholder: String? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        interactionSource = interactionSource,
        enabled = true,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start, fontSize = 16.sp),
        keyboardOptions = keyboardOptions,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
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
                        enabled = true,
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
    supportingText: @Composable (() -> Unit)? = null
) {
    val parts = value.split("-", "–")
    val minVal = parts.getOrNull(0) ?: ""
    val maxVal = parts.getOrNull(1) ?: ""

    var minReps by remember(minVal) { mutableStateOf(minVal) }
    var maxReps by remember(maxVal) { mutableStateOf(maxVal) }

    val minFocusRequester = remember { FocusRequester() }
    val maxFocusRequester = remember { FocusRequester() }
    var isMinFocused by remember { mutableStateOf(false) }
    var isMaxFocused by remember { mutableStateOf(false) }

    val isAnyFocused = isMinFocused || isMaxFocused
    val hasContent = minReps.isNotEmpty() || maxReps.isNotEmpty()
    val isExpanded = isAnyFocused || hasContent || isError

    fun notifyChange(min: String, max: String) {
        val result = when {
            min.isNotBlank() && max.isNotBlank() -> "$min-$max"
            min.isNotBlank() -> min
            max.isNotBlank() -> max
            else -> ""
        }
        onValueChange(result)
    }

    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = if (isExpanded) " " else "",
        onValueChange = {},
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .pointerInput(Unit) {
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
                value = if (isExpanded) " " else "",
                innerTextField = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().fillMaxHeight().clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (!isExpanded) minFocusRequester.requestFocus()
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
                                ) { minFocusRequester.requestFocus() },
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
                                    if (minReps.isEmpty()) minFocusRequester.requestFocus()
                                    else maxFocusRequester.requestFocus()
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
                                ) { if (isExpanded) maxFocusRequester.requestFocus() else minFocusRequester.requestFocus() },
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
                enabled = true,
                isError = isError,
                singleLine = true,
                supportingText = supportingText,
                interactionSource = interactionSource,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(),
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
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
fun ExerciseEditDialog(
    allAvailableExercises: List<String>,
    exercise: RoutineExerciseEntity?,
    dayOfWeek: String,
    themeKey: String,
    onDismiss: () -> Unit,
    onSave: (RoutineExerciseEntity) -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "") }
    var targetSetsStr by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "") }
    var targetIntensityStr by remember { mutableStateOf(exercise?.targetIntensity ?: "") }
    var restSecondsStr by remember { mutableStateOf(exercise?.restSeconds?.toString() ?: "60") }
    var isTimeBased by remember { mutableStateOf(exercise?.targetReps?.contains("sec", ignoreCase = true) ?: false) }
    var isExpanded by remember { mutableStateOf(exercise != null) }
    
    var targetRepsValue by remember { mutableStateOf(if (!isTimeBased) exercise?.targetReps ?: "" else "8-12") }
    var repsManuallyEdited by remember { mutableStateOf(exercise != null) }
    var targetTimeValue by remember { mutableStateOf(if (isTimeBased) exercise?.targetReps?.replace(" sec", "") ?: "30" else "30") }
    var videoUrl by remember { mutableStateOf(exercise?.videoUrl ?: "") }
    var targetWeightValue by remember { mutableStateOf(if (exercise?.targetWeightKg == 0.0) "" else exercise?.targetWeightKg?.toString() ?: "") }

    var activeToastMessage by remember { mutableStateOf<String?>(null) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        // Full-width dialog: with the keyboard open on small phones (e.g. Pixel),
        // the default dialog + fixed content would clip the bottom fields entirely.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
            ) {
                Text(if (exercise == null) getString("add_exercise") else getString("edit_exercise"))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
            ) {
                RepsTimeToggle(
                    isTimeBased = isTimeBased,
                    onToggle = { newValue ->
                        isTimeBased = newValue
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                SearchableExerciseField(
                    allExercises = allAvailableExercises,
                    value = name,
                    onValueChange = { 
                        name = it
                        if (it.isBlank()) {
                            isExpanded = false
                            targetSetsStr = ""
                            targetRepsValue = ""
                            muscleGroup = ""
                        }
                        
                        val detected = WorkoutTemplates.getMuscleGroupForExercise(it)
                        if (detected != "Other") muscleGroup = detected
                    },
                    onItemSelected = { selectedName ->
                        name = selectedName
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
                    },
                    label = getString("exercise_name"),
                    placeholder = getString("exercise_name_placeholder")
                )

                if (isExpanded) {
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

                val repsError = targetRepsValue.isNotEmpty() && !isRepsValid
                val showSupportingSpacer = repsError

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    CompactNumericField(
                        value = targetSetsStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) targetSetsStr = it },
                        label = getString("sets"),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )

                    if (isTimeBased) {
                        var expandedTime by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedTime,
                            onExpandedChange = { expandedTime = !expandedTime },
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
                            ExposedDropdownMenu(expanded = expandedTime, onDismissRequest = { expandedTime = false }) {
                                listOf("30", "45", "60", "90", "120").forEach { t ->
                                    DropdownMenuItem(text = { Text("${t}s") }, onClick = { targetTimeValue = t; expandedTime = false })
                                }
                            }
                        }
                    } else {
                        RepsRangeField(
                            value = targetRepsValue,
                            onValueChange = { targetRepsValue = it; repsManuallyEdited = true },
                            label = getString("reps"),
                            isError = repsError,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    CompactNumericField(
                        value = targetWeightValue,
                        onValueChange = { targetWeightValue = it },
                        label = getString("weight"),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = "0.0",
                        suffix = { Text("kg", fontSize = 12.sp) }
                    )
                    CompactNumericField(
                        value = targetIntensityStr,
                        onValueChange = {
                            targetIntensityStr = it
                            // Re-adapt the auto-selected reps while the user hasn't typed their own
                            if (!repsManuallyEdited && !isTimeBased && name.isNotBlank()) {
                                val recommended = WorkoutTemplates.getRecommendedRepsForExercise(name)
                                if (recommended.isNotBlank()) {
                                    targetRepsValue = WorkoutTemplates.adaptRepsToIntensity(recommended, it)
                                }
                            }
                        },
                        label = getString("intensity_label"),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (name.isNotBlank() && (if (isTimeBased) targetTimeValue else targetRepsValue).isNotBlank()) {
                        val finalReps = if (isTimeBased) {
                            val digits = targetTimeValue.filter { it.isDigit() }
                            if (digits.isNotBlank()) "$digits sec" else "30 sec"
                        } else WorkoutTemplates.enforceRepRange(targetRepsValue.trim())
                        onSave(RoutineExerciseEntity(id = exercise?.id ?: 0L, dayOfWeek = dayOfWeek, name = name, muscleGroup = muscleGroup, targetSets = targetSetsStr.toIntOrNull() ?: 3, targetReps = finalReps, targetIntensity = targetIntensityStr, targetWeightKg = targetWeightValue.toDoubleOrNull() ?: 0.0, restSeconds = restSecondsStr.toIntOrNull() ?: 60, videoUrl = videoUrl, orderIndex = exercise?.orderIndex ?: 99, tempo = exercise?.tempo ?: ""))
                    }
                },
                enabled = name.isNotBlank() && (if (isTimeBased) targetTimeValue else targetRepsValue).isNotBlank() && (isTimeBased || isRepsValid)
            ) {
                Text(getString("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                focusManager.clearFocus()
                onDismiss()
            }) {
                Text(getString("cancel"))
            }
        }
    )
}

/**
 * Gap between two day chips, as a fraction of one chip's slot width. A ratio rather than a number
 * of dp, so the wheel keeps the same visual rhythm on a 320dp phone and on a tablet.
 */
private const val DayWheelGapRatio = 0.08f

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
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(chipSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                allDays.forEach { dayEnum ->
                    val isToday = dayEnum.name == today
                    val isSelected = dayEnum.name == selectedDay

                    val backgroundColor = when {
                        isSelected -> if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
                        else -> if (isDeveloper) Color.Black else MaterialTheme.colorScheme.surfaceVariant
                    }

                    val contentColor = when {
                        isSelected -> if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                        else -> if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val borderStroke = when {
                        isToday -> BorderStroke(2.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary)
                        isDeveloper && isSelected -> BorderStroke(2.dp, Color.White)
                        else -> null
                    }

                    Surface(
                        onClick = { onDaySelected(dayEnum.name) },
                        color = backgroundColor,
                        contentColor = contentColor,
                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
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

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // The day label is centred in the chip - both ways - on every chip, so
                            // all seven share one baseline. AutoScaleText instead of a clipped 12sp
                            // label keeps "Mi"/"Mer"/"Mie" fitting the same slot in every language.
                            // Its fillMaxWidth box is wider than the text and AutoScaleText centres
                            // the text inside it; without that the label sat against the left edge.
                            AutoScaleText(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                minScale = 0.7f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Today's chip carries its tag pinned to the top edge instead of above
                            // the label, so the label itself never has to leave the centre to make
                            // room for it - which is what used to make today's label sit lower than
                            // the other six.
                            if (isToday) {
                                Text(
                                    text = getString("today").uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 2.dp)
                                )
                            }
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
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
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
            
            val repsDetail = if (isTime) {
                if (isBW) "${firstSet.reps}s" else "${firstSet.weightKg}kg x ${firstSet.reps}s"
            } else {
                if (isBW) "${firstSet.reps}" else "${firstSet.weightKg}kg x ${firstSet.reps}"
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

/** How long the picked card's "+" takes to turn into a tick before the swap is applied. */
private const val REPLACE_TICK_MILLIS = 220L

/**
 * A beat between applying the swap and closing the dialog: it gives the new exercise time to
 * reach the list behind the scrim, so the change is part of the transition rather than a
 * jump the instant the dialog disappears.
 */
private const val REPLACE_SETTLE_MILLIS = 160L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReplaceExerciseDialog(
    exercise: RoutineExerciseEntity,
    onDismiss: () -> Unit,
    onReplace: (name: String, muscleGroup: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val allMuscleGroups = remember { WorkoutTemplates.getAllMuscleGroups() }

    val equipmentOptions = listOf(
        WorkoutTemplates.EQUIPMENT_BODYWEIGHT to getString("bodyweight_label"),
        WorkoutTemplates.EQUIPMENT_DUMBBELL to getString("equipment_dumbbell"),
        WorkoutTemplates.EQUIPMENT_BARBELL to getString("equipment_barbell"),
        WorkoutTemplates.EQUIPMENT_GYM to getString("equipment_gym")
    )

    val anyMuscleGroupLabel = getString("any_muscle_group")

    // Auto-select the replaced exercise's equipment & muscle group. An empty
    // equipment selection means "every category" (replaces the old "All" chip).
    var selectedEquipment by remember(exercise.name) {
        mutableStateOf(
            listOf(WorkoutTemplates.getEquipmentForExercise(exercise.name)).filter { it.isNotEmpty() }
        )
    }
    var selectedMuscleGroup by remember(exercise.name) {
        mutableStateOf(exercise.muscleGroup.ifBlank { allMuscleGroups.firstOrNull() ?: "Chest" })
    }
    var muscleMenuExpanded by remember { mutableStateOf(false) }

    // The card whose acknowledgement is playing: its "+" turns into a tick while the swap is
    // held back for [REPLACE_TICK_MILLIS]. Nothing else can be picked meanwhile, so a second
    // tap can't swap a different exercise in behind the first one.
    var pickedName by remember { mutableStateOf<String?>(null) }
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
        val pool = if (selectedMuscleGroup == ANY_MUSCLE_GROUP_KEY) {
            equipmentFilter.flatMap { WorkoutTemplates.getAllExercisesForEquipment(it) }
        } else {
            equipmentFilter.flatMap { WorkoutTemplates.getExercisesForEquipment(it, selectedMuscleGroup) }
        }
        pool.distinct().filter { it.lowercase() != exercise.name.lowercase() }.sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
            ) {
                Text(getString("replace_exercise"))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
            ) {
                Text(
                    text = getString("replace_exercise_desc", translateExerciseName(exercise.name)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // EQUIPMENT FILTER (all options visible, no horizontal scrolling)
                Text(
                    text = getString("equipment"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    equipmentOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedEquipment.contains(key),
                            onClick = {
                                selectedEquipment = if (selectedEquipment.contains(key)) {
                                    selectedEquipment - key
                                } else {
                                    selectedEquipment + key
                                }
                            },
                            label = { Text(label, maxLines = 1) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // MUSCLE GROUP FILTER
                ExposedDropdownMenuBox(
                    expanded = muscleMenuExpanded,
                    onExpandedChange = { muscleMenuExpanded = !muscleMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedMuscleGroup == ANY_MUSCLE_GROUP_KEY) anyMuscleGroupLabel else translateMuscleGroup(selectedMuscleGroup),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(getString("muscle_group")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = muscleMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = muscleMenuExpanded,
                        onDismissRequest = { muscleMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(anyMuscleGroupLabel, fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedMuscleGroup = ANY_MUSCLE_GROUP_KEY
                                muscleMenuExpanded = false
                            }
                        )
                        HorizontalDivider()
                        allMuscleGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(translateMuscleGroup(group)) },
                                onClick = {
                                    selectedMuscleGroup = group
                                    muscleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = getString("exercises_count", alternatives.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (alternatives.isEmpty()) {
                    Text(
                        text = getString("no_exercises_for_filters"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(alternatives) { _, altName ->
                            val altMuscleGroup = if (selectedMuscleGroup == ANY_MUSCLE_GROUP_KEY) {
                                WorkoutTemplates.getMuscleGroupForExercise(altName)
                            } else {
                                selectedMuscleGroup
                            }
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
                                            Text(
                                                text = altDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                focusManager.clearFocus()
                onDismiss()
            }) {
                Text(getString("cancel"))
            }
        }
    )
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
