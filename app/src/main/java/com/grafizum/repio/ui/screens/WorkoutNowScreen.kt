package com.grafizum.repio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.grafizum.repio.R
import com.grafizum.repio.data.DayOfWeekEnum
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.ui.components.ExerciseVideoLinkButton
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.viewmodel.WorkoutNowState
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.ui.viewmodel.getTodayDayOfWeek
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutNowScreen(
    viewModel: WorkoutViewModel,
    onBack: (() -> Unit)? = null,
    onChangeRoutine: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val workoutState by viewModel.workoutNowState.collectAsState()
    val allExercises by viewModel.allRoutineExercises.collectAsState()

    var showRestartDialog by remember { mutableStateOf(false) }
    var showCustomWeightDialog by remember { mutableStateOf(false) }
    var customWeightInput by remember { mutableStateOf("") }

    val currentDayExercises = allExercises.filter { it.dayOfWeek == workoutState.selectedDayOfWeek }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.restart_workout_session_title)) },
            text = { Text(stringResource(R.string.restart_workout_session_text)) },
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
                    Text(stringResource(R.string.restart))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCustomWeightDialog) {
        AlertDialog(
            onDismissRequest = { showCustomWeightDialog = false },
            title = { Text(stringResource(R.string.set_custom_weight_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.enter_custom_weight_text), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customWeightInput,
                        onValueChange = { customWeightInput = it },
                        label = { Text(stringResource(R.string.weight_in_kg)) },
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
                    Text(stringResource(R.string.save_weight))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomWeightDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // COMPACT HEADER (Matches HomeScreen)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                            .map { stringResource(com.grafizum.repio.ui.components.getUpperCategoryResId(it)) }
                            .distinct()
                            .joinToString(", ")
                    } else {
                        stringResource(R.string.rest_recovery_day)
                    }

                    if (trainingType.isNotBlank()) {
                        Text(
                            text = trainingType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onChangeRoutine != null) {
                    OutlinedButton(
                        onClick = onChangeRoutine,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .semantics { testTag = "change_workout_button" }
                    ) {
                        Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.workout), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val today = remember { getTodayDayOfWeek() }
            val allDays = DayOfWeekEnum.values()
            val todayIndex = allDays.indexOfFirst { it.name == today }.coerceAtLeast(0)
            val orderedDays = allDays.drop(todayIndex) + allDays.take(todayIndex)

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        return Offset(x = available.x, y = 0f)
                    }
                }
            }

            // DAY SELECTOR CHIPS
            Box(
                modifier = Modifier
                    .nestedScroll(nestedScrollConnection)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(orderedDays) { dayEnum ->
                        val isSelected = dayEnum.name == workoutState.selectedDayOfWeek
                        val isToday = dayEnum.name == today
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectDayForWorkoutNow(dayEnum.name) },
                            label = {
                                val shortName = when (dayEnum) {
                                    DayOfWeekEnum.MONDAY -> stringResource(R.string.monday_short)
                                    DayOfWeekEnum.TUESDAY -> stringResource(R.string.tuesday_short)
                                    DayOfWeekEnum.WEDNESDAY -> stringResource(R.string.wednesday_short)
                                    DayOfWeekEnum.THURSDAY -> stringResource(R.string.thursday_short)
                                    DayOfWeekEnum.FRIDAY -> stringResource(R.string.friday_short)
                                    DayOfWeekEnum.SATURDAY -> stringResource(R.string.saturday_short)
                                    DayOfWeekEnum.SUNDAY -> stringResource(R.string.sunday_short)
                                }
                                Text(
                                    text = shortName,
                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                borderWidth = if (isToday && !isSelected) 2.dp else 1.dp,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                selectedBorderWidth = if (isToday) 2.dp else 1.dp
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 4.dp)
                )
            }

            if (currentDayExercises.isEmpty()) {
                // Empty / Rest Day State
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                            text = stringResource(R.string.rest_recovery_day),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.rest_recovery_recommendation, dayOfWeekDisplayName(workoutState.selectedDayOfWeek)),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (workoutState.isWorkoutCompleted) {
                // WORKOUT COMPLETED CELEBRATION VIEW
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                            text = stringResource(R.string.workout_completed_title),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.workout_completed_text, workoutState.totalSetsLoggedToday, dayOfWeekDisplayName(workoutState.selectedDayOfWeek)),
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
                                text = stringResource(R.string.start_workout_again),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            } else {
                // ACTIVE WORKOUT SESSION VIEW
                val exerciseIndex = workoutState.currentExerciseIndex.coerceIn(0, currentDayExercises.size - 1)
                val currentExercise = currentDayExercises[exerciseIndex]
                val isTimeBased = currentExercise.targetReps.contains("sec", ignoreCase = true)
                val totalExercises = currentDayExercises.size
                val nextExercise = if (exerciseIndex + 1 < currentDayExercises.size) {
                    currentDayExercises[exerciseIndex + 1]
                } else null

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.exercise_x_of_y, exerciseIndex + 1, totalExercises, workoutState.currentSetNumber, currentExercise.targetSets),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = { showRestartDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .semantics { testTag = "restart_workout_button" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.restart),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.restart),
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
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

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
                                        stringResource(R.string.exercise_transition_label)
                                    } else {
                                        stringResource(R.string.rest_timer)
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.seconds_remaining, workoutState.restTimerSeconds),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

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

                // CURRENT EXERCISE CARD
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MuscleGroupBadge(muscleGroup = currentExercise.muscleGroup)
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.set_x_of_y, workoutState.currentSetNumber, currentExercise.targetSets),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = com.grafizum.repio.ui.components.translateExerciseName(currentExercise.name),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isTimeBased) stringResource(R.string.sets_x_time, currentExercise.targetSets, currentExercise.targetReps)
                                       else stringResource(R.string.sets_x_reps, currentExercise.targetSets, currentExercise.targetReps),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                }
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.semantics { testTag = "next_up_exercise_card" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = stringResource(R.string.next_up),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (nextExercise != null) {
                                        Text(
                                            text = stringResource(R.string.next_up_exercise, com.grafizum.repio.ui.components.translateExerciseName(nextExercise.name), nextExercise.targetSets),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.next_up_none),
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
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.log_current_set_parameters),
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
                                    text = stringResource(R.string.weight),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.weight_kg_intl, workoutState.currentWeightKg),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.decrementWeight() },
                                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.background)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease weight")
                                }

                                IconButton(
                                    onClick = { viewModel.incrementWeight() },
                                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase weight", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // REPS or TIME
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = if (isTimeBased) stringResource(R.string.time_label) else stringResource(R.string.reps_completed),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isTimeBased) "${workoutState.currentExerciseSeconds}s" else stringResource(R.string.reps_count, workoutState.currentReps),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isTimeBased) {
                                    // Time selectors next to time label
                                    listOf(30, 45, 60).forEach { secs ->
                                        val isSelected = workoutState.currentExerciseSeconds == secs
                                        Surface(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.clickable { viewModel.updateExerciseSeconds(secs) }
                                        ) {
                                            Text(
                                                text = "${secs}s",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                } else {
                                    IconButton(
                                        onClick = { viewModel.updateWeightAndReps(workoutState.currentWeightKg, workoutState.currentReps - 1) },
                                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.background)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                                    }

                                    IconButton(
                                        onClick = { viewModel.updateWeightAndReps(workoutState.currentWeightKg, workoutState.currentReps + 1) },
                                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                // PRIMARY ACTION
                val isSelectedDayToday = workoutState.selectedDayOfWeek == today
                val isExTimerRunning = isTimerActive && isExTimer
                
                if (isSelectedDayToday) {
                    if (isExTimerRunning) {
                        // COUNTER / SKIP BUTTON (Full Width)
                        Button(
                            onClick = { viewModel.advanceAfterTimer(currentDayExercises) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
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
                                    text = "(${stringResource(R.string.skip)})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { 
                                if (isTimeBased) {
                                    viewModel.startExerciseTimerWithLog(currentDayExercises, workoutState.currentExerciseSeconds)
                                } else {
                                    viewModel.logCurrentSetAndAdvance(currentDayExercises) 
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().height(64.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = if (isTimeBased) Icons.Default.Timer else Icons.Default.CheckCircle
                                val text = if (isTimeBased) {
                                    stringResource(R.string.start_timer_x_and_log, workoutState.currentExerciseSeconds)
                                } else {
                                    stringResource(R.string.log_set_x_next, workoutState.currentSetNumber)
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun dayOfWeekDisplayName(dayOfWeek: String): String {
    return when (dayOfWeek.uppercase()) {
        "MONDAY" -> stringResource(R.string.monday)
        "TUESDAY" -> stringResource(R.string.tuesday)
        "WEDNESDAY" -> stringResource(R.string.wednesday)
        "THURSDAY" -> stringResource(R.string.thursday)
        "FRIDAY" -> stringResource(R.string.friday)
        "SATURDAY" -> stringResource(R.string.saturday)
        "SUNDAY" -> stringResource(R.string.sunday)
        else -> dayOfWeek
    }
}
