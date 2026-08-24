package com.grafizum.repio.ui.components

import com.grafizum.repio.util.openUrl
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import com.grafizum.repio.data.DayOfWeekEnum
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import com.grafizum.repio.util.getString
import kotlinx.coroutines.delay
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.theme.BrightLime
import com.grafizum.repio.ui.theme.FlameOrange
import com.grafizum.repio.ui.theme.NeonCyan

import com.grafizum.repio.ui.theme.LocalDeveloperSkin

// Color mappings per muscle group
fun getMuscleGroupColor(muscleGroup: String): Color {
    return when (muscleGroup.lowercase().trim()) {
        "back", "lats", "upper back", "traps", "spinal erectors" -> Color(0xFF3B82F6) // Blue
        "chest" -> Color(0xFFEF4444) // Red
        "legs", "quadriceps", "hamstrings", "glutes", "calves" -> Color(0xFF10B981) // Emerald
        "shoulders", "deltoids", "front deltoids", "lateral deltoids", "rear deltoids" -> Color(0xFFEC4899) // Pink
        "biceps" -> Color(0xFF8B5CF6) // Purple
        "triceps" -> Color(0xFF06B6D4) // Cyan
        "arms" -> Color(0xFF6366F1) // Indigo
        "abs" -> Color(0xFFF59E0B) // Amber
        "core", "obliques" -> Color(0xFFD946EF) // Fuchsia
        "forearms" -> Color(0xFFF97316) // Orange
        else -> Color(0xFF64748B) // Grey
    }
}

@Composable
fun MuscleGroupBadge(
    muscleGroup: String,
    modifier: Modifier = Modifier
) {
    val topic = WorkoutTemplates.getTopicForMuscle(muscleGroup)
    val color = getMuscleGroupColor(topic)
    val isDeveloper = LocalDeveloperSkin.current
    Surface(
        color = if (isDeveloper) Color.Black else color.copy(alpha = 0.2f),
        contentColor = if (isDeveloper) Color.White else color,
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
        border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null,
        modifier = modifier
    ) {
        Text(
            text = translateMuscleGroup(topic),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun translateMuscleGroup(muscleGroup: String): String {
    return when (muscleGroup.lowercase().trim()) {
        "back" -> getString("back")
        "biceps" -> getString("biceps")
        "shoulders" -> getString("shoulders")
        "chest" -> getString("chest")
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
        else -> muscleGroup
    }
}

@Composable
fun translateExerciseName(name: String): String {
    val clean = name.trim()
    val lower = clean.lowercase()
    
    return when {
        lower == "push-ups" -> getString("ex_push_ups")
        lower == "pull-ups" -> getString("ex_pull_ups")
        lower == "chin-ups" -> getString("ex_chin_ups")
        lower == "bench press" -> getString("ex_bench_press")
        lower == "squats" -> getString("ex_squats")
        lower == "deadlifts" -> getString("ex_deadlifts")
        lower == "military press" -> getString("ex_military_press")
        lower == "lat pulldown" -> getString("ex_lat_pulldown")
        lower == "seated row" -> getString("ex_seated_row")
        lower == "leg press" -> getString("ex_leg_press")
        lower == "leg curls" -> getString("ex_leg_curls")
        lower == "leg extensions" -> getString("ex_leg_extensions")
        lower == "calf raises" -> getString("ex_calf_raises")
        lower == "hip thrusts" -> getString("ex_hip_thrusts")
        lower == "glute bridges" -> getString("ex_glute_bridges")
        lower == "tricep pushdowns" -> getString("ex_tricep_pushdowns")
        lower == "face pulls" -> getString("ex_face_pulls")
        lower == "side raises" -> getString("ex_side_raises")
        
        clean == "Barbell bent-over row" -> getString("ex_barbell_bent_over_row")
        clean == "One-arm dumbbell row" -> getString("ex_one_arm_dumbbell_row")
        clean == "Dumbbell pullover" -> getString("ex_dumbbell_pullover")
        clean == "Dumbbell curls" -> getString("ex_dumbbell_curls")
        clean == "Hammer curls" -> getString("ex_hammer_curls")
        clean == "Dumbbell shoulder press" -> getString("ex_dumbbell_shoulder_press")
        clean == "Dumbbell lateral raises" -> getString("ex_dumbbell_lateral_raises")
        clean == "Crunches (sit-ups)" -> getString("ex_crunches")
        clean == "Crunches" -> getString("ex_crunches")
        clean == "Barbell floor press" -> getString("ex_barbell_floor_press")
        clean == "Dumbbell floor press" -> getString("ex_dumbbell_floor_press")
        clean == "Leg raises" -> getString("ex_leg_raises")
        clean == "Barbell squats" -> getString("ex_barbell_squats")
        clean == "Romanian deadlifts" -> getString("ex_romanian_deadlifts")
        clean == "Dumbbell lunges" -> getString("ex_dumbbell_lunges")
        clean == "Plank" -> getString("ex_plank")
        lower == "barbell row" -> getString("ex_barbell_row")
        clean == "Tricep dips / extensions" -> getString("ex_tricep_dips_extensions")
        clean == "Overhead tricep extension" -> getString("ex_overhead_tricep_extension")
        clean == "Overhead dumbbell extension" -> getString("ex_overhead_dumbbell_extension")
        clean == "Bench Press / Floor Press" -> getString("ex_bench_press_floor_press")
        clean == "Barbell Rows" -> getString("ex_barbell_rows_generic")
        clean == "Bicep Curls" -> getString("ex_bicep_curls_generic")
        clean == "Rear-delt dumbbell fly" -> getString("ex_rear_delt_dumbbell_fly")
        clean == "Rear-delt fly" -> getString("ex_rear_delt_fly")
        clean == "Calves" -> getString("ex_calves")
        clean == "Abs" -> getString("ex_abs")
        clean == "Press" -> getString("ex_press")
        clean == "Row" -> getString("ex_row")
        clean == "Lunge" -> getString("ex_lunge")
        clean == "Biceps" -> getString("ex_biceps_generic")
        clean == "Chest Press" -> getString("ex_chest_press")
        clean == "Arms" -> getString("ex_arms")
        clean == "Core" -> getString("ex_core")
        clean == "Triceps" -> getString("ex_triceps_generic")
        clean == "Shoulders" -> getString("ex_shoulders_generic")
        clean == "Back" -> getString("ex_back_generic")
        clean == "Legs" -> getString("ex_legs_generic")
        clean == "Flyes" -> getString("ex_flyes")
        else -> name
    }
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
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    Card(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDeveloper) 0.dp else 2.dp),
        border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(if (isDeveloper) RoundedCornerShape(0.dp) else CircleShape)
                        .background(if (isDeveloper) Color.White else iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isDeveloper) Color.Black else iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ExerciseVideoLinkButton(
    videoUrl: String,
    onSaveDemoUrl: ((String) -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    var showInputDialog by remember { mutableStateOf(false) }
    var inputUrlText by remember { mutableStateOf(videoUrl) }

    if (isEditMode && onSaveDemoUrl != null) {
        Button(
            onClick = {
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
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text(getString("demo_video_link")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        onSaveDemoUrl(inputUrlText.trim())
                        showInputDialog = false
                    }
                ) {
                    Text(getString("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
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
    val cleanName = name.trim().lowercase()
    var description = when {
        cleanName.contains("bench press") || cleanName.contains("floor press") -> getString("ex_bench_press_desc")
        cleanName.contains("incline") && cleanName.contains("press") -> getString("ex_incline_press_desc")
        cleanName.contains("decline") && cleanName.contains("press") -> getString("ex_decline_press_desc")
        cleanName.contains("squat") && !cleanName.contains("goblet") && !cleanName.contains("split") -> getString("ex_squats_desc")
        cleanName.contains("goblet squat") -> getString("ex_goblet_squat_desc")
        cleanName.contains("deadlift") && !cleanName.contains("romanian") && !cleanName.contains("rdl") -> getString("ex_deadlifts_desc")
        cleanName.contains("romanian") || cleanName.contains("rdl") -> getString("ex_romanian_deadlift_desc")
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
        cleanName.contains("crunch") && !cleanName.contains("cable") -> getString("ex_crunches_desc")
        cleanName.contains("cable crunch") -> getString("ex_cable_crunch_desc")
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
                mg.contains("back") || mg.contains("lat") || mg.contains("trap") || mg.contains("erector") -> getString("ex_desc_fallback_back")
                mg.contains("shoulder") || mg.contains("delt") -> getString("ex_desc_fallback_shoulders")
                mg.contains("leg") || mg.contains("quad") || mg.contains("hamstring") || mg.contains("glute") || mg.contains("calf") || mg.contains("adductor") || mg.contains("abductor") -> getString("ex_desc_fallback_legs")
                mg.contains("bicep") -> getString("ex_desc_fallback_biceps")
                mg.contains("tricep") -> getString("ex_desc_fallback_triceps")
                mg.contains("abs") || mg.contains("core") || mg.contains("oblique") -> getString("ex_desc_fallback_abs")
                else -> getString("ex_desc_fallback_generic", translateMuscleGroup(muscleGroup))
            }
        }
    }

    if (intensity.contains("RPE", ignoreCase = true)) {
        description += "\n\n" + getString("intensity_label") + ": " + intensity
    }

    val equipment = when {
        cleanName.contains("barbell") ||
        cleanName.contains("military press") ||
        cleanName.contains("deadlift") ||
        cleanName.contains("clean & press") ||
        cleanName.contains("pendlay row") ||
        cleanName.contains("upright row") ||
        cleanName.contains("front squat") ||
        cleanName.contains("good morning") ||
        cleanName.contains("rollout") -> getString("ex_equipment_barbell")

        cleanName.contains("dumbbell") ||
        cleanName.contains("db") ||
        cleanName.contains("lateral raise") ||
        cleanName.contains("front raise") ||
        cleanName.contains("hammer curl") ||
        cleanName.contains("kickback") ||
        cleanName.contains("goblet squat") ||
        cleanName.contains("thruster") ||
        cleanName.contains("incline curl") ||
        cleanName.contains("overhead extension") -> getString("ex_equipment_dumbbell")

        cleanName.contains("cable") -> getString("ex_equipment_cables")

        cleanName.contains("push-up") ||
        (cleanName.contains("crunch") && !cleanName.contains("cable") && !cleanName.contains("machine")) ||
        (cleanName.contains("leg raise") && !cleanName.contains("hanging") && !cleanName.contains("captain's chair") && !cleanName.contains("machine")) ||
        cleanName.contains("russian twist") ||
        cleanName.contains("clamshell") ||
        cleanName.contains("bridge") ||
        (cleanName.contains("squat") && !cleanName.contains("barbell") && !cleanName.contains("dumbbell") && !cleanName.contains("machine") && !cleanName.contains("hack") && !cleanName.contains("v-squat")) ||
        (cleanName.contains("dip") && (cleanName.contains("chair") || cleanName.contains("bench"))) ||
        cleanName.contains("plank") -> getString("ex_equipment_bodyweight")

        else -> getString("ex_equipment_gym")
    }

    return description to equipment
}

@Composable
fun ExpandableExerciseCard(
    exercise: RoutineExerciseEntity,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReplace: (() -> Unit)? = null,
    onSaveVideoUrl: ((String) -> Unit)? = null,
    showDescription: Boolean = false,
    modifier: Modifier = Modifier,
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

    val (description, equipment) = getExerciseDetails(exercise.name, exercise.muscleGroup, exercise.targetIntensity)
    val isDeveloper = LocalDeveloperSkin.current

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
                    if (isSelectionMode) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Reorder",
                            tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .then(dragHandleModifier)
                        )
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
                                maxLines = if (isDeveloper) 2 else 1,
                                overflow = if (isDeveloper) TextOverflow.Clip else TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MuscleGroupBadge(muscleGroup = exercise.muscleGroup)
                        }
                        if (showActions) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (exercise.targetReps.contains("sec", ignoreCase = true))
                                        getString("sets_x_time", exercise.targetSets, exercise.targetReps)
                                    else getString("sets_x_reps", exercise.targetSets, exercise.targetReps),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (exercise.targetIntensity.isNotBlank()) {
                                    val rpeExpl = getRpeRirExplanation(exercise.targetIntensity)
                                    val displayIntensity = if (rpeExpl.isNotBlank()) "${exercise.targetIntensity} ($rpeExpl)" else exercise.targetIntensity
                                    Text(
                                        text = " • $displayIntensity",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDeveloper) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
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
                    // Description & Equipment & Detailed Muscles
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

                        Row {
                            Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(getString("equipment"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Text(equipment, style = MaterialTheme.typography.bodySmall)
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
                                        onClick = onReplace,
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
                                        onClick = onEdit,
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
                                        onClick = onDelete,
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
                            .clickable { expandedCategory = if (isCatExpanded) null else catName }
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
                    onClick = { expandedType = if (expandedType == "REST") null else "REST" },
                    modifier = Modifier.weight(1f)
                )
                TimerSelectionButton(
                    label = getString("exercise_transition_timer"),
                    value = formatTimerValue(transitionValue),
                    isSelected = expandedType == "TRANSITION",
                    onClick = { expandedType = if (expandedType == "TRANSITION") null else "TRANSITION" },
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
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (isSelected) 7.sp else 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
 * Interactive Weekly Volume / Sets Bar Chart
 */
@Composable
fun InteractiveVolumeBarChart(
    dailyData: Map<String, Double>, // "Mon" -> volume
    selectedKey: String?,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        getString("monday_short"),
        getString("tuesday_short"),
        getString("wednesday_short"),
        getString("thursday_short"),
        getString("friday_short"),
        getString("saturday_short"),
        getString("sunday_short")
    )
    val isDeveloper = LocalDeveloperSkin.current
    val maxVal = (dailyData.values.maxOrNull() ?: 100.0).coerceAtLeast(100.0)

    val primaryColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
    val highlightColor = if (isDeveloper) Color.LightGray else MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = getString("volume_progress_kg"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (selectedKey != null && dailyData.containsKey(selectedKey)) {
                            getString("volume_label") + " $selectedKey: ${dailyData[selectedKey]?.toInt()} kg"
                        } else {
                            getString("tap_bar_to_inspect")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedKey != null) highlightColor else labelColor
                    )
                }
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val barWidth = size.width / keys.size
                                val index = (offset.x / barWidth).toInt().coerceIn(0, keys.size - 1)
                                onKeySelected(keys[index])
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height - 30.dp.toPx() // Reserve space for labels
                    val barWidth = (width / keys.size) * 0.5f

                    // Draw subtle grid lines
                    for (i in 1..3) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = labelColor.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    keys.forEachIndexed { index, key ->
                        val value = dailyData[key] ?: 0.0
                        val barHeight = ((value / maxVal) * height).toFloat()
                        val xCenter = (width / keys.size) * (index + 0.5f)
                        val xLeft = xCenter - (barWidth / 2f)
                        val yTop = height - barHeight

                        val isSelected = key == selectedKey
                        val gradient = if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(highlightColor, highlightColor.copy(alpha = 0.7f))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.5f))
                            )
                        }

                        // Draw Bar
                        if (barHeight > 0) {
                            drawRoundRect(
                                brush = gradient,
                                topLeft = Offset(xLeft, yTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                            )
                        } else {
                            // Empty bar indicator
                            drawRoundRect(
                                color = labelColor.copy(alpha = 0.15f),
                                topLeft = Offset(xLeft, height - 8.dp.toPx()),
                                size = Size(barWidth, 8.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                // X-Axis labels
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(top = 4.dp)
                ) {
                    keys.forEach { key ->
                        Text(
                            text = key,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (key == selectedKey) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (key == selectedKey) highlightColor else labelColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
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
    val sortedItems = distribution.entries.sortedByDescending { it.value }

    Card(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = getString("muscle_group_focus"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (distribution.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val strokeWidth = 24.dp.toPx()

                            sortedItems.forEachIndexed { index, entry ->
                                val sweepAngle = (entry.value.toFloat() / total.toFloat()) * 360f
                                val color = if (isDeveloper) {
                                    val grey = (0.2f + (index.toFloat() / sortedItems.size) * 0.7f).coerceIn(0f, 1f)
                                    Color(grey, grey, grey)
                                } else {
                                    getMuscleGroupColor(entry.key)
                                }

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
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

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        sortedItems.take(5).forEachIndexed { index, entry ->
                            val color = if (isDeveloper) {
                                val grey = (0.2f + (index.toFloat() / sortedItems.size) * 0.7f).coerceIn(0f, 1f)
                                Color(grey, grey, grey)
                            } else {
                                getMuscleGroupColor(entry.key)
                            }
                            val percentage = ((entry.value.toFloat() / total.toFloat()) * 100).toInt()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = translateMuscleGroup(entry.key),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getUpperCategoryKey(muscleGroup: String): String {
    val mg = muscleGroup.lowercase().trim()
    return when {
        mg.contains("chest") -> "chest"
        mg.contains("back") || mg.contains("lat") || mg.contains("trap") || mg.contains("erector") -> "back"
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
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
                    ) { onToggle(false) },
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
                    ) { onToggle(true) },
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

    val filteredExercises = remember(value) {
        if (value.isBlank()) emptyList()
        else {
            allExercises.filter { it.contains(value, ignoreCase = true) }
                .sortedWith(compareByDescending<String> { it.equals(value, ignoreCase = true) }
                    .thenByDescending { it.startsWith(value, ignoreCase = true) }
                    .thenBy { it.lowercase() }
                )
                .take(5)
        }
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
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(10.dp)
        )

        if (filteredExercises.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                filteredExercises.forEach { selection ->
                    DropdownMenuItem(
                        text = { 
                            Column {
                                Text(selection, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                val (desc, _) = getExerciseDetails(selection, WorkoutTemplates.getMuscleGroupForExercise(selection), "")
                                if (desc.isNotBlank()) {
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
    equipmentLimitedParts: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    if (missingParts.isEmpty() && equipmentLimitedParts.isEmpty()) {
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
        val isEquipmentIssue = equipmentLimitedParts.isNotEmpty()
        Card(
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDeveloper) Color.White
                                else if (isEquipmentIssue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
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
                        text = if (isEquipmentIssue) getString("equipment_limited_routine") else getString("incomplete_workout_routine"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    val translatedParts = mutableListOf<String>()
                    if (isEquipmentIssue) {
                        for (part in equipmentLimitedParts) {
                            translatedParts.add(translateMuscleGroup(part))
                        }
                    } else {
                        for (part in missingParts) {
                            translatedParts.add(translateMuscleGroup(part))
                        }
                    }

                    val warningText = if (isEquipmentIssue) {
                        getString("equipment_limited_desc", translatedParts.joinToString(", "))
                    } else {
                        getString("incomplete_workout_desc", translatedParts.joinToString(", "))
                    }

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

    // Use a Box to container everything and handle the click to focus the first field
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                minFocusRequester.requestFocus()
            }
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = if (isAnyFocused || hasContent) " " else "",
            innerTextField = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // MIN FIELD
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (minReps.isEmpty() && (isAnyFocused || hasContent)) {
                            Text(
                                text = "min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                        BasicTextField(
                            value = minReps,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() }) {
                                    minReps = it
                                    notifyChange(it, maxReps)
                                    if (it.length >= 2) {
                                        maxFocusRequester.requestFocus()
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(minFocusRequester)
                                .onFocusChanged { isMinFocused = it.isFocused }
                        )
                    }

                    // SEPARATOR
                    if (isAnyFocused || hasContent) {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // MAX FIELD
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (maxReps.isEmpty() && (isAnyFocused || hasContent)) {
                            Text(
                                text = "max",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                        if (isAnyFocused || hasContent) {
                            BasicTextField(
                                value = maxReps,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) {
                                        maxReps = it
                                        notifyChange(minReps, it)
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            label = { Text(label) },
            enabled = true,
            isError = isError,
            singleLine = true,
            supportingText = supportingText,
            interactionSource = interactionSource,
            visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
            container = {
                OutlinedTextFieldDefaults.ContainerBox(
                    enabled = true,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = OutlinedTextFieldDefaults.colors(),
                    shape = RoundedCornerShape(12.dp),
                    focusedBorderThickness = 2.dp,
                    unfocusedBorderThickness = 1.dp
                )
            }
        )
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
    onSave: (RoutineExerciseEntity) -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "") }
    var targetSetsStr by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "") }
    var targetIntensityStr by remember { mutableStateOf(exercise?.targetIntensity ?: "") }
    var restSecondsStr by remember { mutableStateOf(exercise?.restSeconds?.toString() ?: "60") }
    var isTimeBased by remember { mutableStateOf(exercise?.targetReps?.contains("sec", ignoreCase = true) ?: false) }
    var isExpanded by remember { mutableStateOf(exercise != null) }
    
    var targetRepsValue by remember { mutableStateOf(exercise?.targetReps?.replace(" sec", "") ?: "") }
    var videoUrl by remember { mutableStateOf(exercise?.videoUrl ?: "") }

    var activeToastMessage by remember { mutableStateOf<String?>(null) }

    val isRepsValid = remember(targetRepsValue, isTimeBased) {
        if (isTimeBased) true
        else if (targetRepsValue.isBlank()) true
        else {
            val validChars = targetRepsValue.all { it.isDigit() || it == '-' || it == '–' }
            if (!validChars) return@remember false
            if (targetRepsValue.startsWith('-') || targetRepsValue.endsWith('-') || 
                targetRepsValue.startsWith('–') || targetRepsValue.endsWith('–')) return@remember false
            val parts = targetRepsValue.split('-', '–').filter { it.isNotBlank() }
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
            val parts = targetRepsValue.split('-', '–').filter { it.isNotBlank() }
            if (parts.size == 2) {
                val v1 = parts[0].toIntOrNull()
                val v2 = parts[1].toIntOrNull()
                v1 != null && v2 != null && v1 > v2
            } else false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exercise == null) getString("add_exercise") else getString("edit_exercise")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RepsTimeToggle(
                    isTimeBased = isTimeBased,
                    onToggle = { newValue ->
                        isTimeBased = newValue
                        if (newValue && (targetRepsValue.isEmpty() || !targetRepsValue.all { it.isDigit() })) {
                            targetRepsValue = "30"
                        }
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
                        isTimeBased = WorkoutTemplates.isExerciseTimeBased(selectedName)
                        isExpanded = true
                        // Reset videoUrl if name changed via selection to allow repository inheritance
                        if (name != selectedName) videoUrl = ""
                        if (targetSetsStr.isBlank()) targetSetsStr = WorkoutTemplates.getRecommendedSetsForExercise(selectedName).toString()
                        if (targetRepsValue.isBlank()) targetRepsValue = WorkoutTemplates.getRecommendedRepsForExercise(selectedName)
                    },
                    label = getString("exercise_name"),
                    placeholder = getString("exercise_name_placeholder")
                )

                if (isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(getString("trained_muscles") + ":", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            MuscleGroupBadge(muscleGroup = muscleGroup)
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = targetSetsStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) targetSetsStr = it },
                        label = { Text(getString("sets")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        supportingText = if (repsHasRangeError) { { Text("") } } else null
                    )

                    if (isTimeBased) {
                        var expandedTime by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedTime,
                            onExpandedChange = { expandedTime = !expandedTime },
                            modifier = Modifier.weight(1f)
                        ) {
                            val digitsOnly = targetRepsValue.filter { it.isDigit() }
                            OutlinedTextField(
                                value = if (digitsOnly.isNotBlank()) "${digitsOnly}s" else "30s",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(getString("time_label")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTime) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedTime, onDismissRequest = { expandedTime = false }) {
                                listOf("30", "45", "60", "90", "120").forEach { t ->
                                    DropdownMenuItem(text = { Text("${t}s") }, onClick = { targetRepsValue = t; expandedTime = false })
                                }
                            }
                        }
                    } else {
                        RepsRangeField(
                            value = targetRepsValue,
                            onValueChange = { targetRepsValue = it },
                            label = getString("reps"),
                            isError = repsHasRangeError || (targetRepsValue.isNotEmpty() && !isRepsValid),
                            modifier = Modifier.weight(1f),
                            supportingText = if (repsHasRangeError) {
                                { Text(getString("min_greater_than_max"), color = MaterialTheme.colorScheme.error) }
                            } else null
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = restSecondsStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) restSecondsStr = it },
                        label = { Text(getString("rest_timer_label")) },
                        suffix = { Text("s") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetIntensityStr,
                        onValueChange = { targetIntensityStr = it },
                        label = { Text(getString("intensity_label")) },
                        placeholder = { Text("RPE 8") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                ThemedToast(activeToastMessage ?: "", themeKey, activeToastMessage != null) { activeToastMessage = null }
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text(getString("demo_link_optional")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && targetRepsValue.isNotBlank()) {
                        val finalReps = if (isTimeBased) {
                            val digits = targetRepsValue.filter { it.isDigit() }
                            if (digits.isNotBlank()) "$digits sec" else "30 sec"
                        } else targetRepsValue.trim()
                        onSave(RoutineExerciseEntity(id = exercise?.id ?: 0L, dayOfWeek = dayOfWeek, name = name, muscleGroup = muscleGroup, targetSets = targetSetsStr.toIntOrNull() ?: 3, targetReps = finalReps, targetIntensity = targetIntensityStr, restSeconds = restSecondsStr.toIntOrNull() ?: 60, videoUrl = videoUrl, orderIndex = exercise?.orderIndex ?: 99))
                    }
                },
                enabled = name.isNotBlank() && targetRepsValue.isNotBlank() && isRepsValid
            ) { Text(getString("save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(getString("cancel")) } }
    )
}

@Composable
fun WorkoutDayWheel(
    selectedDay: String,
    onDaySelected: (String) -> Unit,
    today: String,
    modifier: Modifier = Modifier
) {
    val allDays = DayOfWeekEnum.entries
    val isDeveloper = LocalDeveloperSkin.current
    var totalDrag by remember { mutableStateOf(0f) }

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

    Box(
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isToday) {
                                Text(
                                    text = getString("today").uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 6.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                            }

                            val dayLabel = when (dayEnum) {
                                DayOfWeekEnum.MONDAY -> getString("monday_shortest")
                                DayOfWeekEnum.TUESDAY -> getString("tuesday_shortest")
                                DayOfWeekEnum.WEDNESDAY -> getString("wednesday_shortest")
                                DayOfWeekEnum.THURSDAY -> getString("thursday_shortest")
                                DayOfWeekEnum.FRIDAY -> getString("friday_shortest")
                                DayOfWeekEnum.SATURDAY -> getString("saturday_shortest")
                                DayOfWeekEnum.SUNDAY -> getString("sunday_shortest")
                            }

                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
