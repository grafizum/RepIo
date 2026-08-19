package com.grafizum.repio.ui.screens

import androidx.compose.ui.layout.layout
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MeasureResult
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.grafizum.repio.data.DayOfWeekEnum
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.ExerciseVideoLinkButton
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.viewmodel.WorkoutNowState
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.getString
import com.grafizum.repio.ui.components.ExerciseEditDialog
import kotlinx.coroutines.delay

import com.grafizum.repio.ui.theme.LocalDeveloperSkin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutNowScreen(
    viewModel: WorkoutViewModel,
    onBack: (() -> Unit)? = null,
    onChangeRoutine: (() -> Unit)? = null,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val workoutState by viewModel.workoutNowState.collectAsState()
    val allExercises by viewModel.allRoutineExercises.collectAsState()
    val isDeveloper = LocalDeveloperSkin.current

    var showRestartDialog by remember { mutableStateOf(false) }
    var showCustomWeightDialog by remember { mutableStateOf(false) }
    var customWeightInput by remember { mutableStateOf("") }
    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    val allAvailableExercises = remember { WorkoutTemplates.getAllExerciseNames() }
    val themeKey by viewModel.themeOption.collectAsState()

    // Reset edit state when tab becomes inactive
    LaunchedEffect(isActive) {
        if (!isActive) {
            editingExercise = null
            showCustomWeightDialog = false
            showRestartDialog = false
        }
    }

    val currentDayExercises = allExercises.filter { it.dayOfWeek == workoutState.selectedDayOfWeek }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(getString("restart_workout_session_title")) },
            text = { Text(getString("restart_workout_session_text")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetWorkoutSession()
                        showRestartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(getString("restart"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    if (showCustomWeightDialog) {
        AlertDialog(
            onDismissRequest = { showCustomWeightDialog = false },
            title = { Text(getString("set_custom_weight_title")) },
            text = {
                Column {
                    Text(getString("enter_custom_weight_text"), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customWeightInput,
                        onValueChange = { customWeightInput = it },
                        label = { Text(getString("weight_in_kg")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customWeightInput.toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            viewModel.updateWeightAndReps(parsed, workoutState.currentReps)
                        }
                        showCustomWeightDialog = false
                    }
                ) {
                    Text(getString("save_weight"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomWeightDialog = false }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // COMPACT HEADER (FIXED)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(if (isDeveloper) RoundedCornerShape(0.dp) else CircleShape)
                        .background(if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dayOfWeekDisplayName(workoutState.selectedDayOfWeek),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    val trainingType = if (currentDayExercises.isNotEmpty()) {
                        currentDayExercises.map { it.muscleGroup }
                            .map { getString(com.grafizum.repio.ui.components.getUpperCategoryKey(it)) }
                            .distinct()
                            .joinToString(", ")
                    } else {
                        getString("rest_recovery_day")
                    }

                    if (trainingType.isNotBlank()) {
                        Text(
                            text = trainingType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onChangeRoutine != null) {
                    OutlinedButton(
                        onClick = onChangeRoutine,
                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                        border = if (isDeveloper) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDeveloper) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            contentColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .semantics { testTag = "change_workout_button" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = getString("workout"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val today = remember { getTodayDayOfWeek() }
        com.grafizum.repio.ui.components.WorkoutDayWheel(
            selectedDay = workoutState.selectedDayOfWeek,
            onDaySelected = { viewModel.selectDayForWorkoutNow(it) },
            today = today
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = workoutState.selectedDayOfWeek,
                transitionSpec = {
                    val allDaysEnum = DayOfWeekEnum.entries
                    val initialIndex = allDaysEnum.indexOfFirst { it.name == initialState }
                    val targetIndex = allDaysEnum.indexOfFirst { it.name == targetState }
                    
                    if (targetIndex > initialIndex) {
                        (slideInHorizontally { width -> width } + fadeIn(tween(300))).togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(tween(300))).togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(300)))
                    }.using(SizeTransform(clip = false))
                },
                label = "DayTransition"
            ) { targetDay ->
                val dayExercises = allExercises.filter { it.dayOfWeek == targetDay }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    if (dayExercises.isEmpty()) {
                        // Empty / Rest Day State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = getString("rest_recovery_day"),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = getString("rest_recovery_recommendation", dayOfWeekDisplayName(targetDay)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (workoutState.isWorkoutCompleted && targetDay == workoutState.selectedDayOfWeek) {
                        // WORKOUT COMPLETED CELEBRATION VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = getString("workout_completed_title"),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = getString("workout_completed_text", workoutState.totalSetsLoggedToday, dayOfWeekDisplayName(targetDay)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = { viewModel.resetWorkoutSession() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = getString("start_workout_again"),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ACTIVE WORKOUT SESSION VIEW
                        val exerciseIndex = workoutState.currentExerciseIndex.coerceIn(0, dayExercises.size - 1)
                        val currentExercise = dayExercises[exerciseIndex]
                        
                        val isTimeBased = currentExercise.targetReps.contains("sec", ignoreCase = true)
                        val isBodyweight = WorkoutTemplates.isBodyweightExercise(currentExercise.name)
                        val totalExercises = dayExercises.size

                        val nextExercise = if (exerciseIndex + 1 < dayExercises.size) {
                            dayExercises[exerciseIndex + 1]
                        } else null

                        // Pinned Top Progress
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val labelText = if (isDeveloper) {
                                    getString("exercise_x_of_y_simple", exerciseIndex + 1, totalExercises)
                                } else {
                                    getString("exercise_x_of_y", exerciseIndex + 1, totalExercises, workoutState.currentSetNumber, currentExercise.targetSets)
                                }
                                
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDeveloper) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                                )

                                Button(
                                    onClick = { showRestartDialog = true },
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .semantics { testTag = "restart_workout_button" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = getString("restart"),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        getString("restart"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val progress = ((exerciseIndex.toFloat() + (workoutState.currentSetNumber.toFloat() / currentExercise.targetSets)) / totalExercises.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(4.dp))
                                    .border(if (isDeveloper) BorderStroke(1.dp, Color.White) else BorderStroke(0.dp, Color.Transparent)),
                                color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable middle section
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(viewModel.workoutNowScrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // TIMER AREA
                            val isTimerActive = workoutState.isTimerActive && workoutState.restTimerSeconds > 0
                            val isExTimer = workoutState.isExerciseTimerActive

                            if (isTimerActive && !isExTimer) {
                                // Running Rest Timer Card (Only for Rest)
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                val label = if (workoutState.currentSetNumber == 1) {
                                                    getString("exercise_transition_label")
                                                } else {
                                                    getString("rest_timer")
                                                }
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = getString("seconds_remaining", workoutState.restTimerSeconds),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { viewModel.stopTimer() }) {
                                                Icon(
                                                    imageVector = Icons.Default.SkipNext,
                                                    contentDescription = "Skip Timer",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // CURRENT EXERCISE CARD
                            Card(
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isDeveloper) 0.dp else 4.dp),
                                border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            MuscleGroupBadge(muscleGroup = currentExercise.muscleGroup)
                                            
                                            val specificMuscle = currentExercise.muscleGroup
                                            val topic = WorkoutTemplates.getTopicForMuscle(specificMuscle)
                                            if (!specificMuscle.equals(topic, ignoreCase = true)) {
                                                Surface(
                                                    color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                                    contentColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.secondary,
                                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(4.dp),
                                                    border = BorderStroke(if (isDeveloper) 1.dp else 0.5.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                                ) {
                                                    Text(
                                                        text = com.grafizum.repio.ui.components.translateMuscleGroup(specificMuscle).uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 9.sp,
                                                            letterSpacing = 0.5.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Surface(
                                            color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            contentColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
                                            border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null
                                        ) {
                                            Text(
                                                text = getString("set_x_of_y", workoutState.currentSetNumber, currentExercise.targetSets),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    var showPeek by remember { mutableStateOf(false) }
                                    val (description, _) = com.grafizum.repio.ui.components.getExerciseDetails(currentExercise.name, currentExercise.muscleGroup, currentExercise.targetIntensity)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = com.grafizum.repio.ui.components.translateExerciseName(currentExercise.name),
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = if (isDeveloper) 2 else 1,
                                            overflow = if (isDeveloper) TextOverflow.Clip else TextOverflow.Ellipsis
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { showPeek = !showPeek },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (showPeek) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Quick Peek",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    AnimatedVisibility(visible = showPeek) {
                                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                                            Surface(
                                                color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                                                border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isTimeBased) getString("sets_x_time", currentExercise.targetSets, currentExercise.targetReps)
                                                       else getString("sets_x_reps", currentExercise.targetSets, currentExercise.targetReps),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (currentExercise.targetIntensity.isNotBlank()) {
                                                val rpeExpl = com.grafizum.repio.ui.components.getRpeRirExplanation(currentExercise.targetIntensity)
                                                val displayIntensity = if (rpeExpl.isNotBlank()) "${currentExercise.targetIntensity} ($rpeExpl)" else currentExercise.targetIntensity
                                                Text(
                                                    text = " • $displayIntensity",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ExerciseVideoLinkButton(
                                            videoUrl = currentExercise.videoUrl,
                                            onSaveDemoUrl = { newUrl ->
                                                viewModel.saveExercise(currentExercise.copy(videoUrl = newUrl))
                                            },
                                            modifier = Modifier.height(44.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Surface(
                                            color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            contentColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface,
                                            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                                            border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null,
                                            modifier = Modifier
                                                .height(44.dp)
                                                .weight(1f, fill = false)
                                                .semantics { testTag = "next_up_exercise_card" }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(start = 16.dp, end = 12.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = getString("next_up"),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = 0.5.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                if (nextExercise != null) {
                                                    Text(
                                                        text = com.grafizum.repio.ui.components.translateExerciseName(nextExercise.name),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                } else {
                                                    Text(
                                                        text = getString("next_up_none"),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // STEPPERS AND WEIGHT CUSTOMIZATION
                            Card(
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = getString("log_current_set_parameters"),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )

                                    // WEIGHT SECTION
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(
                                                text = getString("weight"),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isBodyweight) getString("bodyweight_label") else getString("weight_kg_intl", workoutState.currentWeightKg),
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isBodyweight) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                                )

                                                if (!isBodyweight) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                                            .clickable {
                                                                customWeightInput = workoutState.currentWeightKg.toString()
                                                                showCustomWeightDialog = true
                                                            }
                                                            .semantics { testTag = "modify_weight_button" }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Modify Weight",
                                                            modifier = Modifier.size(11.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (!isBodyweight) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val canDecrementWeight = workoutState.currentWeightKg > 0
                                                IconButton(
                                                    onClick = { viewModel.decrementWeight() },
                                                    enabled = canDecrementWeight,
                                                    modifier = Modifier.clip(CircleShape).background(if (canDecrementWeight) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                ) {
                                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease weight", tint = if (canDecrementWeight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                                }

                                                IconButton(
                                                    onClick = { viewModel.incrementWeight() },
                                                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                                ) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase weight", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        } else {
                                            // Inform user it's bodyweight
                                            Surface(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = getString("no_weight_needed"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // REPS or TIME (Constrained to plan range)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isTimeBased) getString("time_label") else getString("reps_completed"),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (isTimeBased) "${workoutState.currentExerciseSeconds}s" else getString("reps_count", workoutState.currentReps),
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isTimeBased) {
                                                val canDecreaseTime = workoutState.currentExerciseSeconds > workoutState.minExerciseSeconds
                                                IconButton(
                                                    onClick = { viewModel.updateExerciseSeconds(workoutState.currentExerciseSeconds - 5) },
                                                    enabled = canDecreaseTime,
                                                    modifier = Modifier.clip(CircleShape).background(if (canDecreaseTime) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                ) {
                                                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, tint = if (canDecreaseTime) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                                }

                                                val canIncreaseTime = workoutState.currentExerciseSeconds < workoutState.maxExerciseSeconds
                                                IconButton(
                                                    onClick = { viewModel.updateExerciseSeconds(workoutState.currentExerciseSeconds + 5) },
                                                    enabled = canIncreaseTime,
                                                    modifier = Modifier.clip(CircleShape).background(if (canIncreaseTime) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                ) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = if (canIncreaseTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                                }
                                            } else {
                                                val canDecreaseReps = workoutState.currentReps > workoutState.minReps
                                                IconButton(
                                                    onClick = { viewModel.updateWeightAndReps(workoutState.currentWeightKg, workoutState.currentReps - 1) },
                                                    enabled = canDecreaseReps,
                                                    modifier = Modifier.clip(CircleShape).background(if (canDecreaseReps) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                ) {
                                                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, tint = if (canDecreaseReps) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                                }

                                                val canIncreaseReps = workoutState.currentReps < workoutState.maxReps
                                                IconButton(
                                                    onClick = { viewModel.updateWeightAndReps(workoutState.currentWeightKg, workoutState.currentReps + 1) },
                                                    enabled = canIncreaseReps,
                                                    modifier = Modifier.clip(CircleShape).background(if (canIncreaseReps) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                ) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = if (canIncreaseReps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // PRIMARY ACTION (PINNED AT BOTTOM)
                        val isSelectedDayToday = targetDay == today
                        val isTimerActive = workoutState.isTimerActive && workoutState.restTimerSeconds > 0
                        val isExTimerRunning = isTimerActive && workoutState.isExerciseTimerActive
                        
                        if (isSelectedDayToday) {
                            Spacer(modifier = Modifier.height(16.dp))
                            if (isExTimerRunning) {
                                // COUNTER / SKIP BUTTON (Full Width)
                                Button(
                                    onClick = { viewModel.advanceAfterTimer(dayExercises) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null,
                                    modifier = Modifier.fillMaxWidth().height(64.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${workoutState.restTimerSeconds}s",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "(${getString("skip")})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { 
                                        if (isTimeBased) {
                                            viewModel.startExerciseTimerWithLog(dayExercises, workoutState.currentExerciseSeconds)
                                        } else {
                                            viewModel.logCurrentSetAndAdvance(dayExercises) 
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null,
                                    modifier = Modifier.fillMaxWidth().height(64.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon = if (isTimeBased) Icons.Default.Timer else Icons.Default.CheckCircle
                                        val text = if (isTimeBased) {
                                            getString("start_timer_x_and_log", workoutState.currentExerciseSeconds)
                                        } else {
                                            getString("log_set_x_next", workoutState.currentSetNumber)
                                        }
                                        
                                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // EXERCISE EDIT DIALOG
    editingExercise?.let { exercise ->
        ExerciseEditDialog(
            allAvailableExercises = allAvailableExercises,
            exercise = exercise,
            dayOfWeek = exercise.dayOfWeek,
            themeKey = themeKey,
            onDismiss = { editingExercise = null },
            onSave = { updated ->
                viewModel.saveExercise(updated)
                editingExercise = null
            }
        )
    }
}

@Composable
fun dayOfWeekDisplayName(dayOfWeek: String): String {
    return when (dayOfWeek.uppercase()) {
        "MONDAY" -> getString("monday")
        "TUESDAY" -> getString("tuesday")
        "WEDNESDAY" -> getString("wednesday")
        "THURSDAY" -> getString("thursday")
        "FRIDAY" -> getString("friday")
        "SATURDAY" -> getString("saturday")
        "SUNDAY" -> getString("sunday")
        else -> dayOfWeek
    }
}
