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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.delay
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.WorkoutTemplates
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
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
    val lower = name.trim().lowercase()
    return when {
        lower == "row" || lower == "rows" -> getString("ex_row")
        lower == "pullover" || lower == "pullovers" -> getString("ex_dumbbell_pullover")
        lower == "curl" || lower == "curls" -> getString("ex_curls")
        lower == "hammer curl" || lower == "hammer curls" -> getString("ex_hammer_curl")
        lower == "shoulder press" || lower == "shoulder presses" -> getString("ex_shoulder_press")
        lower == "lateral raise" || lower == "lateral raises" -> getString("ex_lateral_raise")
        lower == "crunch" || lower == "crunches" -> getString("ex_crunches")
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
    customPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    numericValue: Double? = null,
    workoutDaysCount: Int? = null
) {
    val isDeveloper = LocalDeveloperSkin.current
    val goldColor = Color(0xFFD4AF37) // Soft Metallic Gold
    
    val pointsColor = if (numericValue != null) {
        when {
            numericValue < 1000 -> Color(0xFF2C2C2E) // Darkish black
            numericValue < 10000 -> Color(0xFF007AFF) // 1k+ (4 digits)
            numericValue < 100000 -> Color(0xFF34C759) // 10k+ (5 digits)
            numericValue < 1000000 -> Color(0xFF8B5CF6) // 100k+ (6 digits)
            else -> Color(0xFFFF9500) // 1M+ (7 digits)
        }
    } else {
        MaterialTheme.colorScheme.primary
    }

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
            modifier = Modifier.padding(if (subtitle.isBlank()) 24.dp else 16.dp)
        ) {
            if (subtitle.isBlank()) {
                // HERO MODE: Repio Points
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (workoutDaysCount != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Consecutive training days: $workoutDaysCount",
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
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 44.sp,
                            lineHeight = 44.sp
                        ),
                        color = pointsColor
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = getString("pts_abbr"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = pointsColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    if (customPainter != null) {
                        Image(
                            painter = customPainter,
                            contentDescription = null,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(goldColor),
                            modifier = Modifier.size(52.dp).align(Alignment.CenterVertically)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = goldColor,
                            modifier = Modifier.size(44.dp).align(Alignment.CenterVertically)
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
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
    onSaveDemoUrl: ((String) -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
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

    return description to ""
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
                                maxLines = if (expanded) 2 else 1,
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

fun formatRepioPoints(points: Double): String {
    return if (points > 999999) {
        val k = points / 1000.0
        val whole = k.toInt()
        val decimal = ((k - whole) * 10).toInt()
        if (decimal > 0) "${whole}.${decimal}k" else "${whole}k"
    } else {
        points.toInt().toString()
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
    onCalendarClick: (() -> Unit)? = null,
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
                        text = getString("weekly_progress"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (selectedKey != null && dailyData.containsKey(selectedKey)) {
                            getString("volume_label") + " $selectedKey: ${formatRepioPoints(dailyData[selectedKey] ?: 0.0)} " + getString("total_volume")
                        } else {
                            getString("tap_bar_to_inspect")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedKey != null) highlightColor else labelColor
                    )
                }
                
                if (onCalendarClick != null) {
                    IconButton(
                        onClick = onCalendarClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Show Full History",
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Y-Axis Labels
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(bottom = 30.dp), // Match Canvas height reserve
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 4 downTo 0) {
                            Text(
                                text = formatRepioPoints(maxVal * (i / 4f)),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = labelColor.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                            for (i in 0..4) {
                                val y = height * (1f - i / 4f)
                                drawLine(
                                    color = labelColor.copy(alpha = 0.1f),
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

    val filteredExercises = remember(value, allExercises) {
        if (value.isBlank()) emptyList()
        else {
            val query = value.lowercase().trim()
            allExercises.filter { it.lowercase().contains(query) }
                .sortedWith(
                    compareByDescending<String> { it.equals(value, ignoreCase = true) }
                        .thenByDescending { it.startsWith(value, ignoreCase = true) }
                        .thenBy { it.lowercase() }
                )
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
                supportingText = {
                    if (supportingText != null) supportingText()
                    else Text(" ", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                },
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    start = 16.dp,
                    end = 12.dp
                ),
                container = {
                    OutlinedTextFieldDefaults.ContainerBox(
                        enabled = true,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                supportingText = {
                    if (supportingText != null) supportingText()
                    else Text(" ", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                },
                interactionSource = interactionSource,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(),
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                container = {
                    OutlinedTextFieldDefaults.ContainerBox(
                        enabled = true,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                            if (targetRepsValue.isBlank() || targetRepsValue == "8-12") targetRepsValue = WorkoutTemplates.getRecommendedRepsForExercise(selectedName)
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
                            Text(getString("trained_muscles") + ":", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                val repsError = targetRepsValue.isNotEmpty() && !isRepsValid
                val showSupportingSpacer = repsError

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                        supportingText = { Text(" ", style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                                        contentPadding = OutlinedTextFieldDefaults.contentPadding(start = 16.dp, end = 12.dp),
                                        container = {
                                            OutlinedTextFieldDefaults.ContainerBox(
                                                enabled = true,
                                                isError = false,
                                                interactionSource = interactionSource,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                            onValueChange = { targetRepsValue = it },
                            label = getString("reps"),
                            isError = repsError,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                        onValueChange = { targetIntensityStr = it },
                        label = getString("intensity_label"),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }


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
                        } else targetRepsValue.trim()
                        onSave(RoutineExerciseEntity(id = exercise?.id ?: 0L, dayOfWeek = dayOfWeek, name = name, muscleGroup = muscleGroup, targetSets = targetSetsStr.toIntOrNull() ?: 3, targetReps = finalReps, targetIntensity = targetIntensityStr, targetWeightKg = targetWeightValue.toDoubleOrNull() ?: 0.0, restSeconds = restSecondsStr.toIntOrNull() ?: 60, videoUrl = videoUrl, orderIndex = exercise?.orderIndex ?: 99))
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

    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
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
            val points = WorkoutTemplates.calculateRepioPoints(set.exerciseName, set.weightKg, set.reps)
            map[shortDay] = (map[shortDay] ?: 0.0) + points
        }
        map
    }

    val sessionsCount = dailyDataMap.size
    val totalVolume = dailyDataMap.values.sum()
    val avgLoad = if (sessionsCount > 0) totalVolume / sessionsCount else 0.0
    
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
                    value = avgLoad.toInt().toString(),
                    label = getString("avg_load_label"),
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
    val totalPoints = sets.sumOf { 
        WorkoutTemplates.calculateRepioPoints(it.exerciseName, it.weightKg, it.reps)
    }
    
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
                text = formatRepioPoints(totalPoints) + " " + getString("pts_abbr"),
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
