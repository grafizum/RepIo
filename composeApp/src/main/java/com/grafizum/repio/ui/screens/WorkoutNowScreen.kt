package com.grafizum.repio.ui.screens

import androidx.compose.ui.layout.layout
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.MeasureResult
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.grafizum.repio.data.DayOfWeekEnum
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.ExerciseVideoLinkButton
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.ScreenHeader
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.viewmodel.MasteryCardData
import com.grafizum.repio.ui.viewmodel.WorkoutNowState
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.getString
import com.grafizum.repio.util.getQuantityString
import com.grafizum.repio.ui.components.ExerciseEditDialog
import kotlinx.coroutines.delay

import com.grafizum.repio.ui.theme.LocalDeveloperSkin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutNowScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onChangeRoutine: (() -> Unit)? = null,
    onAdjustLevels: () -> Unit = {},
    isActive: Boolean = true
) {
    val workoutState by viewModel.workoutNowState.collectAsState()
    val allExercises by viewModel.allRoutineExercises.collectAsState()
    val isDeveloper = LocalDeveloperSkin.current
    val focusManager = LocalFocusManager.current
    val window = LocalWindowSize.current

    var showRestartDialog by remember { mutableStateOf(false) }
    // Exercise whose level-details dialog is open (name, or null). Opened by
    // tapping the level in the card's header — the full ladder plus the picker
    // that jumps straight to a level.
    var levelDetailsFor by remember { mutableStateOf<String?>(null) }
    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    val allAvailableExercises by viewModel.allAvailableExercises.collectAsState()
    val themeKey by viewModel.themeOption.collectAsState()

    val context = LocalContext.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset edit state when tab becomes inactive
    LaunchedEffect(isActive) {
        if (!isActive) {
            editingExercise = null
            showRestartDialog = false
        }
    }

    val currentDayExercises = remember(allExercises, workoutState.selectedDayOfWeek) {
        allExercises.filter { it.dayOfWeek == workoutState.selectedDayOfWeek }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("restart_workout_session_title"))
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("restart_workout_session_text"))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
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
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showRestartDialog = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = window.screenPadding)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Spacer(modifier = Modifier.height(window.sectionSpacing))

        // COMPACT HEADER — the day title and the "Workout" action share this row. ScreenHeader
        // keeps the action at its intrinsic size while the title wraps inside the leftover
        // width, so long day names (DE/IT) can no longer squeeze the button or clip mid-glyph
        // on narrow phones (e.g. Pixel 8). No subtitle: the per-exercise muscle badges
        // already say what the day trains.
        ScreenHeader(
            title = dayOfWeekDisplayName(workoutState.selectedDayOfWeek),
            icon = Icons.Default.FitnessCenter,
            trailing = {
                // Both header actions are siblings in one Row, so the gap between
                // them is structural (spacedBy) rather than a start-padding bolted
                // onto one of them — that is what keeps "Levels" clear of
                // "Workout" in every locale and at every font scale.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(window.headerIconSpacing)
                ) {
                    // "Levels": re-opens the guide as a compact popup so the
                    // system stays explainable at any time.
                    HeaderActionPill(
                        label = getString("levels_menu_title"),
                        icon = Icons.Default.FormatListNumbered,
                        onClick = onAdjustLevels
                    )
                    if (onChangeRoutine != null) {
                        HeaderActionPill(
                            label = getString("workout"),
                            icon = Icons.Default.Edit,
                            onClick = onChangeRoutine,
                            modifier = Modifier.semantics { testTag = "change_workout_button" }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(window.sectionSpacing))

        val today = remember { getTodayDayOfWeek() }
        com.grafizum.repio.ui.components.WorkoutDayWheel(
            selectedDay = workoutState.selectedDayOfWeek,
            onDaySelected = { viewModel.selectDayForWorkoutNow(it) },                            today = today
        )

        Spacer(modifier = Modifier.height(window.sectionSpacing))

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
                                // Without weight(1f), Arrangement.Center + scrolling clips
                                // the TOP of over-tall content on short screens.
                                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
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
                                // Without weight(1f), Arrangement.Center + scrolling clips
                                // the TOP of over-tall content on short screens.
                                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
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
                                        text = getQuantityString("workout_completed_text", workoutState.totalSetsLoggedToday, workoutState.totalSetsLoggedToday, dayOfWeekDisplayName(targetDay)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))

                                    if (targetDay == today) {
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

                            // YOUR PROGRESSION OPPORTUNITIES — the Mastery &
                            // Branch hub. Only present when at least one exercise
                            // finished its ladder or reached its researched human
                            // ceiling this workout.
                            if (workoutState.masteredExercises.isNotEmpty()) {
                                MasterySection(
                                    cards = workoutState.masteredExercises,
                                    onAcceptBranch = { source, target -> viewModel.acceptBranch(source, target) },
                                    onKeepAtCap = { viewModel.keepExerciseAtCap(it) }
                                )
                            }
                        }
                    } else {
                        // ACTIVE WORKOUT SESSION VIEW
                        val exerciseIndex = workoutState.currentExerciseIndex.coerceIn(0, dayExercises.size - 1)
                        val currentExercise = dayExercises[exerciseIndex]
                        
                        val isTimeBased = currentExercise.targetReps.contains("sec", ignoreCase = true)
                        val totalExercises = dayExercises.size

                        val nextExercise = if (exerciseIndex + 1 < dayExercises.size) {
                            dayExercises[exerciseIndex + 1]
                        } else null

                        // v5 level system: what to do THIS set. Everything shown
                        // comes from WorkoutNowState — the UI never touches the
                        // engine, and internal fields never reach it. Computed once
                        // here because both cards below read it: the exercise card
                        // stays clean (what to do), the LEVEL card under it carries
                        // the prescription (which level, which weight, which reps).
                        val progLevel = workoutState.progressionLevel
                        val progStage = workoutState.progressionStage
                        val prescribed = workoutState.progressionPrescribed
                        val variationOrDuration = prescribed?.let { rx ->
                            when (rx.kind) {
                                ProgressionEngine.ValueKind.VARIATION -> rx.variation
                                ProgressionEngine.ValueKind.DURATION -> prescriptionDisplayText(rx)
                                ProgressionEngine.ValueKind.LOAD -> null
                            }
                        }
                        val loadText = when {
                            prescribed?.kind == ProgressionEngine.ValueKind.LOAD ->
                                prescriptionDisplayText(prescribed)
                            workoutState.currentWeightKg > 0.0 ->
                                getString("weight_kg_intl", workoutState.currentWeightKg)
                            else -> null
                        }

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
                                    color = if (isDeveloper) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (targetDay == today) {
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
                                            .heightIn(min = 32.dp)
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
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        focusManager.clearFocus()
                                    })
                                }
                                .verticalScroll(viewModel.workoutNowScrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // TIMER AREA (only shown on the day the timer belongs to)
                            val isTimerActive = workoutState.isTimerActive && workoutState.restTimerSeconds > 0
                            val isExTimer = workoutState.isExerciseTimerActive
                            val isTimerForThisDay = isTimerActive && targetDay == workoutState.timerDayOfWeek

                            if (isTimerForThisDay && !isExTimer) {
                                // Running Rest Timer Card (Only for Rest)
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
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
                                            if (!notificationsEnabled) {
                                                IconButton(
                                                    onClick = {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            try {
                                                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (_: Exception) {
                                                                val intent = Intent(Settings.ACTION_SETTINGS)
                                                                context.startActivity(intent)
                                                            }
                                                        } else {
                                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                                data = Uri.fromParts("package", context.packageName, null)
                                                            }
                                                            context.startActivity(intent)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsOff,
                                                        contentDescription = getString("allow_notifications"),
                                                        tint = MaterialTheme.colorScheme.error
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
                            }

                            // CURRENT EXERCISE CARD — the hero of the screen. Painted in
                            // the same language as the level card below it: the theme's
                            // primary tint over the surface with a hairline primary rim,
                            // so the two cards read as one family instead of a grey box
                            // above a tinted one. Developer skin keeps its flat look.
                            Card(
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDeveloper) Color.Black
                                                     else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = if (isDeveloper) BorderStroke(1.dp, Color.White)
                                         else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    var showPeek by remember { mutableStateOf(false) }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Badge shows the broad body part + precise muscle group together.
                                        MuscleGroupBadge(
                                            muscleGroup = currentExercise.muscleGroup,
                                            exerciseName = currentExercise.name
                                        )
                                        

                                        // Card actions: edit the exercise and peek at the
                                        // method description, parked beside the badge so the
                                        // name below gets the full width. The set count lives
                                        // in the level card's banner — no duplicate up here.
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { editingExercise = currentExercise },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = getString("edit_routine"),
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { showPeek = !showPeek },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (showPeek) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = getString("description"),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val (description, _) = com.grafizum.repio.ui.components.getExerciseDetails(currentExercise.name, currentExercise.muscleGroup, currentExercise.targetIntensity)

                                    // The name leads, full width now that the actions
                                    // live beside the badge: the user has to know WHAT
                                    // they are about to do before a level or a load
                                    // means anything.
                                    Text(
                                        text = com.grafizum.repio.ui.components.translateExerciseName(currentExercise.name),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = if (showPeek) 2 else 1,
                                        overflow = if (isDeveloper) TextOverflow.Clip else TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    AnimatedVisibility(visible = showPeek) {
                                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                                            // Nested well: one shade of the primary IN — the same
                                            // recipe as the level card's banner — so the description
                                            // reads as recessed into the card family.
                                            Surface(
                                                color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                                                border = if (isDeveloper) BorderStroke(1.dp, Color.White)
                                                         else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
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

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Tools + context row. The demo button renders nothing
                                    // when the exercise has no video link, so the SPACER
                                    // carries the weight here: "next up" stays pinned to the
                                    // trailing edge whether or not a demo button is present,
                                    // instead of drifting to the left when there is none.
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ExerciseVideoLinkButton(
                                            videoUrl = currentExercise.videoUrl,
                                            onSaveDemoUrl = { newUrl ->
                                                viewModel.saveExercise(currentExercise.copy(videoUrl = newUrl))
                                            },
                                            modifier = Modifier.height(44.dp)
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        // "Next up" as a quiet right-aligned caption —
                                        // no chip surface, so the row reads as one
                                        // modern typographic line with the demo button.
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .semantics { testTag = "next_up_exercise_card" }
                                        ) {
                                            Text(
                                                text = getString("next_up"),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = nextExercise?.let { com.grafizum.repio.ui.components.translateExerciseName(it.name) }
                                                       ?: getString("next_up_none"),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // LEVEL CARD — the second card under the exercise card.
                            // The prescription lives HERE and only here: which level,
                            // which stage, and the weight and reps to reach, with the
                            // active set's rep bold. The exercise card above stays a
                            // clean "what am I doing" view. Exercises outside the level
                            // system (custom mode, non-ready rows) get the same card
                            // without the level header, showing the routine's own reps.
                            Spacer(modifier = Modifier.height(12.dp))
                            LevelPrescriptionCard(
                                exerciseName = currentExercise.name,
                                level = progLevel,
                                stage = progStage,
                                lastStage = viewModel.patternStageCount(currentExercise.name),
                                maxLevel = viewModel.progressionMaxLevel(currentExercise.name),
                                adjustable = progLevel != null && progStage != null &&
                                    viewModel.isLevelAdjustable(currentExercise.name) &&
                                    !currentExercise.locked,
                                locked = currentExercise.locked,
                                developerLabel = if (isDeveloper && progLevel != null) viewModel.progressionLabel(currentExercise.name) else null,
                                isFirstSession = workoutState.progressionIsFirstSession,
                                targetSets = currentExercise.targetSets,
                                repTargets = workoutState.progressionStageTargetReps,
                                currentSetNumber = workoutState.currentSetNumber,
                                fallbackReps = currentExercise.targetReps,
                                primaryText = variationOrDuration,
                                loadText = loadText,
                                isTimeBased = isTimeBased,
                                onStageStep = { delta ->
                                    if (delta > 0) viewModel.advanceStageOrLevel(currentExercise.name)
                                    else viewModel.retreatStageOrLevel(currentExercise.name)
                                },
                                onToggleLock = { viewModel.setExerciseLocked(currentExercise.name, !currentExercise.locked) },
                                onOpenDetails = { levelDetailsFor = currentExercise.name }
                            )

                        }

                        // PRIMARY ACTION (PINNED AT BOTTOM)
                        val isSelectedDayToday = targetDay == today
                        val isTimerActive = workoutState.isTimerActive && workoutState.restTimerSeconds > 0
                        val isExTimerRunning = isTimerActive && workoutState.isExerciseTimerActive && targetDay == workoutState.timerDayOfWeek
                        
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
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${workoutState.restTimerSeconds}s",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
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
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon = if (isTimeBased) Icons.Default.Timer else Icons.Default.CheckCircle
                                        val text = if (isTimeBased) {
                                            getString("start_timer_x_and_log", workoutState.currentExerciseSeconds)
                                        } else {
                                            getString("log_set_x_next", workoutState.currentSetNumber)
                                        }
                                        
                                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.navigateToExercise(workoutState.currentExerciseIndex - 1, dayExercises.size) },
                                    enabled = workoutState.currentExerciseIndex > 0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(getString("previous"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Button(
                                    onClick = { viewModel.navigateToExercise(workoutState.currentExerciseIndex + 1, dayExercises.size) },
                                    enabled = workoutState.currentExerciseIndex < dayExercises.size - 1,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    Text(getString("next"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // LEVEL DETAILS DIALOG — jump levels with full weight/reps overview.
    levelDetailsFor?.let { name ->
        LevelDetailsDialog(
            viewModel = viewModel,
            exerciseName = name,
            onDismiss = { levelDetailsFor = null }
        )
    }

    // EXERCISE EDIT DIALOG
    editingExercise?.let { exercise ->
        // Touching a level-owned field (sets/reps/weight/intensity) offers the
        // switch to Custom — never silently switches the mode.
        var confirmCustom by remember { mutableStateOf(false) }
        ExerciseEditDialog(
            allAvailableExercises = allAvailableExercises,
            exercise = exercise,
            dayOfWeek = exercise.dayOfWeek,
            themeKey = themeKey,
            levelsManaged = viewModel.isExerciseLevelManaged(exercise.name),
            userEquipmentCategories = viewModel.userEquipmentCategories(),
            onRequestCustomMode = { confirmCustom = true },
            onDismiss = { editingExercise = null },
            onSave = { updated ->
                viewModel.saveExercise(updated)
                editingExercise = null
            }
        )
        if (confirmCustom) {
            AlertDialog(
                onDismissRequest = { confirmCustom = false },
                title = { Text(getString("custom_mode_title")) },
                text = { Text(getString("custom_mode_body")) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setLevelSystemEnabled(false)
                        confirmCustom = false
                    }) { Text(getString("custom_mode_confirm")) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmCustom = false }) {
                        Text(getString("cancel"))
                    }
                }
            )
        }
    }
}

/**
 * Renders an engine [ProgressionEngine.PrescribedValue] as localized UI text:
 * "8 kg each", "12 kg", "4 steps", "Incline Push-Up", or "30 s".
 */
@Composable
fun prescriptionDisplayText(rx: ProgressionEngine.PrescribedValue): String = when (rx.kind) {
    ProgressionEngine.ValueKind.VARIATION -> rx.variation ?: ""
    ProgressionEngine.ValueKind.DURATION -> getString("progression_duration_value", rx.value?.toInt() ?: 0)
    ProgressionEngine.ValueKind.LOAD -> {
        val v = rx.value ?: 0.0
        val clean = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
        when (rx.loadUnit) {
            ProgressionEngine.LoadUnit.KG_EACH -> getString("progression_load_each", clean)
            ProgressionEngine.LoadUnit.KG -> getString("progression_load_kg", clean)
            ProgressionEngine.LoadUnit.STACK_STEP -> getString("progression_load_steps", clean)
            ProgressionEngine.LoadUnit.RESISTANCE_STEP -> getString("progression_load_steps", clean)
            null -> ""
        }
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

/**
 * One pill in the workout header. Both header actions ("Levels" and "Workout")
 * go through this, so they always match in height, shape and colour — the gap
 * between them belongs to the Row that holds them, not to one button.
 */
@Composable
private fun HeaderActionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    OutlinedButton(
        onClick = onClick,
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDeveloper) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            contentColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = modifier.heightIn(min = 40.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Where one set of the current stage sits: already logged, now, or still to come. */

/**
 * The LEVEL card — the second card under the exercise card.
 *
 * It carries everything the level system prescribes, and only it does: the
 * level and stage on top, then the weight and the per-set reps to reach. The
 * exercise card above stays a clean "what am I doing" view, so the two never
 * repeat each other.
 *
 * The card is painted from the theme's own palette (a primary tint over the
 * surface, a primary border) rather than a neutral one, so it reads as part of
 * the app's colour system instead of a detached grey box.
 *
 * The level pill between the arrows is an actual raised button — tonal fill,
 * border, chevron — so "this opens the level picker" is visible instead of a
 * hint the user has to be told. The set the user is on renders bold (and
 * filled); sets already saved are tinted, the rest outlined.
 *
 * [level]/[stage] null (exercise outside the level system: custom mode, or a
 * row the engine cannot rate) drop the header and stage dots; the card then
 * stands alone as the routine's own "this set" row.
 */
@OptIn(ExperimentalMaterial3Api::class) // Surface(onClick) for the level pill
@Composable
private fun LevelPrescriptionCard(
    exerciseName: String,
    level: Int?,
    stage: Int?,
    lastStage: Int,
    maxLevel: Int,
    adjustable: Boolean,
    locked: Boolean,
    developerLabel: String?,
    isFirstSession: Boolean,
    targetSets: Int,
    repTargets: List<Int>,
    currentSetNumber: Int,
    fallbackReps: String,
    primaryText: String?,
    loadText: String?,
    isTimeBased: Boolean,
    onStageStep: (Int) -> Unit,
    onToggleLock: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val isDeveloper = LocalDeveloperSkin.current
    val inSystem = level != null && stage != null
    // The per-set action banner is LEVEL-MODE ONLY: it narrates the system's
    // prescription. Custom mode keeps the classic read — one big target line,
    // plan summary beside it — so the two modes look and feel different.
    val showChips = inSystem && !isTimeBased && primaryText == null && repTargets.size in 1..3

    Surface(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
        // Level mode wears the primary tint; custom mode a neutral surface so
        // the card visually confirms which mode is driving the workout. Both
        // get a hairline rim so they hold their edge against the background.
        color = if (isDeveloper) Color.Black
                else if (inSystem) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isDeveloper) BorderStroke(1.dp, Color.White)
                 else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (inSystem) 0.35f else 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---- Header: level pill anchored LEFT with the Roman stage as a
            // side note OUTSIDE the pill's background; the stepper (− level +)
            // rides at the far right. Skipped for exercises outside the level
            // system — those get a plain "this set" strip with no level chrome.
            if (level != null && stage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // The clickable level pill: a raised tonal button rather than bare
                // text, so the tap target is discoverable without a caption.
                Surface(
                    onClick = onOpenDetails,
                    enabled = adjustable,
                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                    color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                    contentColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onPrimary,
                    border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null,
                    shadowElevation = if (isDeveloper || !adjustable) 0.dp else 2.dp,
                    modifier = Modifier.semantics { testTag = "level_chip" }
                ) {
                    Text(
                        text = getString("level_word_n", level),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        maxLines = 1
                    )
                }

                // Stage progress as dots between pill and stepper; the Roman
                // numeral itself lives in the stepper now, so a second static
                // copy would say the same thing twice.
                if (lastStage in 2..8) {
                    StageDots(total = lastStage, current = stage)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Compact stepper: − stage +. The arrows walk the Roman stages
                // of the current level one at a time; at the LAST stage the +
                // rolls over into the next level's stage I (and − into the
                // previous level's last stage). A locked exercise pins the
                // stepper completely: disabled clicks, fully grayed icons.
                val canStepDown = adjustable && ((stage ?: 1) > 1 || (level ?: 1) > 1)
                val canStepUp = adjustable &&
                    ((stage ?: 1) < lastStage || (level ?: 1) < maxLevel)
                Surface(
                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                    color = if (isDeveloper) Color.White.copy(alpha = 0.10f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = canStepDown) { onStageStep(-1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = getString("stage_down_desc"),
                                    tint = when {
                                        !canStepDown -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        isDeveloper -> Color.White
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = ProgressionEngine.stageName(stage ?: 1),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 2.dp)
                                .semantics { testTag = "level_chip_value" },
                            maxLines = 1
                        )
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = canStepUp) { onStageStep(+1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = getString("stage_up_desc"),
                                    tint = when {
                                        !canStepUp -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        isDeveloper -> Color.White
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // The lock rides at the row's far end, always visible and
                // tappable — INCLUDING while locked, or there would be no way
                // back. Locked persists per exercise in the routine table and
                // pins this exercise's level: the stepper grays out completely.
                if (adjustable || locked) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleLock),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = getString(if (locked) "exercise_lock_desc" else "exercise_unlock_desc"),
                                tint = when {
                                    isDeveloper -> Color.White
                                    locked -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            }

            if (showChips) {
                // ---- Action banner: the ONE thing to do right now ----
                // "SET 2 OF 3 — do 10 reps" at display size in a tinted strip,
                // with one progress pill per set underneath. The current target
                // must read at arm's length mid-set; finished sets collapse to
                // filled ticks, upcoming ones stay visible but quiet.
                val activeSet = currentSetNumber.coerceIn(1, targetSets)
                val activeReps = repTargets.getOrNull(activeSet - 1)
                if (activeReps != null) {
                    Surface(
                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp),
                        color = if (isDeveloper) Color.White.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = if (isDeveloper) null
                                 else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = getString("set_progress_format", activeSet, targetSets).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = activeReps.toString(),
                                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = " " + getString("reps"),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDeveloper) Color.White.copy(alpha = 0.7f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                // One pill per set: filled = done, wide = current,
                                // faint = upcoming. The week's shape at a glance.
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(targetSets) { index ->
                                        val done = index < currentSetNumber - 1
                                        val current = index == currentSetNumber - 1
                                        Box(
                                            modifier = Modifier
                                                .size(width = if (current) 18.dp else 8.dp, height = 8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when {
                                                        isDeveloper -> Color.White.copy(alpha = if (current || done) 1f else 0.35f)
                                                        current -> MaterialTheme.colorScheme.primary
                                                        done -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                    }
                                                )
                                        )
                                    }
                                }
                                if (loadText != null) {
                                    Text(
                                        text = loadText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                // Small label with EVERY set's target, so the stage's
                                // whole shape ("9 / 8 / 8") reads next to the pills and
                                // nobody has to guess what the remaining sets will ask.
                                if (repTargets.size > 1) {
                                    Text(
                                        text = repTargets.joinToString(" / ") + " " + getString("reps"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDeveloper) Color.White.copy(alpha = 0.7f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Fallback view (time-based / mixed-rep prescriptions): the
                // full plan summary beside the target, no banner needed.
                Text(
                    text = if (repTargets.isEmpty()) getQuantityString("sets_planned_pl", targetSets)
                           else getQuantityString("sets_x_reps_pl", targetSets, targetSets, repTargets.joinToString(" / ")),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // weight(fill = false): the target can shrink (and ellipsize) so the
                    // load beside it is never squeezed off the card.
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = primaryText ?: fallbackReps,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (loadText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = loadText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            developerLabel?.let { label ->
                Text(
                    text = label + if (isFirstSession) " (fresh)" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** One dot per Roman stage: filled up to the current one. */
@Composable
private fun StageDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

// ----------------------------------------------------------------------
// Mastery & Branch — the post-workout progression hub.
//
// Celebratory but grounded: the exercise's track is drawn as a node road
// (completed, current, locked), the load line names the ceiling that was
// reached, and each branch is a real card with its own mini track — not a
// bare button. A true ceiling (no branches) shows the acknowledgement card
// instead. All animations are subtle fades; nothing bounces.
// ----------------------------------------------------------------------

@Composable
private fun MasterySection(
    cards: List<MasteryCardData>,
    onAcceptBranch: (sourceName: String, targetName: String) -> Unit,
    onKeepAtCap: (sourceName: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = getString("mastery_section_title"),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = getString("mastery_section_subtitle"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        cards.forEach { card -> MasteryCard(card, onAcceptBranch, onKeepAtCap) }
    }
}

@Composable
private fun MasteryCard(
    card: MasteryCardData,
    onAcceptBranch: (String, String) -> Unit,
    onKeepAtCap: (String) -> Unit
) {
    val isDeveloper = LocalDeveloperSkin.current
    Card(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        border = if (isDeveloper) BorderStroke(1.dp, Color.White)
                 else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // --- Identity + load line ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translateExerciseName(card.exerciseName),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = listOf(card.muscleGroup, card.equipment).filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Progress track: filled bar + "MAX" pill ---
            val loadLine = card.load?.let {
                val clean = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                getString("progression_load_kg", clean)
            } ?: ""
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "MAX",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getString("mastery_level_line", card.level, card.stageRoman) +
                        if (loadLine.isNotBlank()) " – $loadLine" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when (card.reason) {
                    ProgressionEngine.MasteryReason.CEILING_REACHED ->
                        card.ceiling?.basis ?: getString("mastery_ceiling_default")
                    ProgressionEngine.MasteryReason.LADDER_COMPLETE ->
                        getString("mastery_ladder_complete")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // --- Branches, or the true-ceiling acknowledgement ---
            if (card.branches.isNotEmpty()) {
                card.branches.forEach { branch ->
                    BranchCard(card.exerciseName, branch, onAcceptBranch)
                }
                Text(
                    text = getString("mastery_switch_later"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HumanLimitCard(card, onKeepAtCap)
            }

            // "Stay at cap" is available for branch exercises too (decline).
            if (card.branches.isNotEmpty()) {
                TextButton(onClick = { onKeepAtCap(card.exerciseName) }) {
                    Text(getString("mastery_keep_current"), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun BranchCard(
    sourceName: String,
    branch: com.grafizum.repio.data.ProgressionBranch,
    onAcceptBranch: (String, String) -> Unit
) {
    val isDeveloper = LocalDeveloperSkin.current
    Surface(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = translateExerciseName(branch.entry),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (branch.tag == "advanced") MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (branch.tag == "advanced") MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = if (branch.tag == "advanced") getString("mastery_tag_advanced")
                               else getString("mastery_tag_new"),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = branch.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Mini progress track: the branch's future nodes, next one highlighted.
            if (branch.nodes.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    branch.nodes.take(3).forEachIndexed { index, node ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (index == 0) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (index == 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = translateExerciseName(node),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (index < branch.nodes.take(3).lastIndex) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { onAcceptBranch(sourceName, branch.entry) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(getString("mastery_choose_path"), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun HumanLimitCard(
    card: MasteryCardData,
    onKeepAtCap: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getString("mastery_human_limit"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = getString("mastery_human_limit_text", translateExerciseName(card.exerciseName)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (card.suggestions.isNotEmpty()) {
            val suggestionList = card.suggestions.map { translateExerciseName(it) }.joinToString(", ")
            Text(
                text = getString("mastery_focus_on") + " " + suggestionList,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = { onKeepAtCap(card.exerciseName) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(getString("mastery_acknowledge"), style = MaterialTheme.typography.labelLarge)
        }
    }
}


