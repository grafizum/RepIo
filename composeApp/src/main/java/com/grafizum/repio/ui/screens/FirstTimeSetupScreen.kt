package com.grafizum.repio.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grafizum.repio.data.DefaultData
import com.grafizum.repio.data.ExerciseLocalization
import com.grafizum.repio.data.RoutineDayEntity
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplate
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.MissingBodyPartsWarningCard
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.components.ReplaceExerciseDialog
import com.grafizum.repio.ui.components.RepsTimeToggle
import com.grafizum.repio.ui.components.RepsRangeField
import com.grafizum.repio.ui.components.CompactNumericField
import com.grafizum.repio.ui.components.SearchableExerciseField
import com.grafizum.repio.ui.components.ThemedToast
import com.grafizum.repio.ui.components.getExerciseDetails
import com.grafizum.repio.ui.components.getMissingMuscleGroups
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.components.translateMuscleGroup
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getString
import com.grafizum.repio.util.getQuantityString
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun FirstTimeSetupScreen(
    viewModel: WorkoutViewModel,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDeveloper = LocalDeveloperSkin.current
    val window = LocalWindowSize.current

    // Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    var showNotificationSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showNotificationSettingsDialog = true
            }
        }
    }

    if (showNotificationSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationSettingsDialog = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("allow_notifications"))
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
                    Text(getString("notification_channel_desc"))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        showNotificationSettingsDialog = false
                        try {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            } else {
                                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text(getString("settings"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showNotificationSettingsDialog = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }
    // The two ways to build a routine: 0 = the assisted (auto-generated) build,
    // 1 = the manual custom build. The switch below flips between them.
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedWorkoutGoal by remember { mutableStateOf("goal_bodybuilding") }

    // Assisted Build State
    val assistedBodyParts = remember { mutableStateListOf(
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids",
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors",
        "Biceps", "Triceps", "Forearms",
        "Abs", "Obliques",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"
    ) }
    val assistedSelectedDays = remember { mutableStateListOf("MONDAY", "WEDNESDAY", "FRIDAY") }
    var assistedIntensity by remember { mutableStateOf("Medium") }
    var assistedMinSets by remember { mutableIntStateOf(0) }
    var assistedMaxSets by remember { mutableIntStateOf(0) }
    val assistedIncludedExercises = remember { mutableStateListOf<String>() }
    val assistedExcludedExercises = remember { mutableStateListOf<String>() }
    var preferencesExpanded by remember { mutableStateOf(false) }

    // Equipment types the build may use (empty = all)
    var buildEquipment by remember { mutableStateOf(listOf<String>()) }

    val allAvailableExercises by viewModel.allAvailableExercises.collectAsState()

    var selectedDayInCustom by remember { mutableStateOf("MONDAY") }
    var showFinalSaveConfirmation by remember { mutableStateOf(false) }
    val isManualReentry by viewModel.isManualReentry.collectAsState()

    BackHandler(enabled = isManualReentry) {
        onCancel()
    }

    val isLightTheme = MaterialTheme.colorScheme.onSurface.luminance() < 0.5f

    // State for Custom Routine Builder
    val customDays = remember {
        mutableStateListOf(
            RoutineDayEntity("MONDAY", "Monday — Workout", "Custom session", false),
            RoutineDayEntity("TUESDAY", "Tuesday — Workout", "Custom session", false),
            RoutineDayEntity("WEDNESDAY", "Wednesday — Rest", "Recovery day", true),
            RoutineDayEntity("THURSDAY", "Thursday — Workout", "Custom session", false),
            RoutineDayEntity("FRIDAY", "Friday — Workout", "Custom session", false),
            RoutineDayEntity("SATURDAY", "Saturday — Rest", "Recovery day", true),
            RoutineDayEntity("SUNDAY", "Sunday — Rest", "Recovery day", true)
        )
    }

    val customExercises = remember {
        mutableStateListOf<RoutineExerciseEntity>().apply {
            addAll(DefaultData.routineExercises)
        }
    }
    
    val customSelectedExercises = remember { mutableStateListOf<RoutineExerciseEntity>() }
    var isCustomSelectionMode by remember { mutableStateOf(false) }
    var showDeleteMultipleConfirmationCustom by remember { mutableStateOf(false) }

    var showAddCustomExerciseDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var customExerciseToDelete by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var replacingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }

    // Copy Day feature: copy the selected day's exercises to another day
    var showCopyDayDialog by remember { mutableStateOf(false) }
    var copyTargetDay by remember { mutableStateOf("TUESDAY") }
    var copyReplaceExercises by remember { mutableStateOf(true) }
    val themeOption by viewModel.themeOption.collectAsState()

    // Both the Save button and the final confirmation dialog go through here, so the
    // generated build and the manual one can't drift apart.
    //   0 = assisted: generate the program from the chosen parts and settings.
    //   1 = custom: save the day-by-day routine built by hand.
    fun saveSelectedBuild() {
        viewModel.setWorkoutGoal(selectedWorkoutGoal)
        if (selectedTab == 1) {
            viewModel.completeFirstTimeSetup(
                days = customDays.toList(),
                exercises = customExercises.toList(),
                setupType = "CUSTOM"
            )
            return
        }
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = assistedBodyParts.toList(),
            selectedDays = assistedSelectedDays.toList(),
            intensity = assistedIntensity,
            workoutGoal = selectedWorkoutGoal,
            includedExercises = assistedIncludedExercises.toList(),
            excludedExercises = assistedExcludedExercises.toList(),
            minSets = assistedMinSets,
            maxSets = assistedMaxSets,
            equipment = buildEquipment
        )
        viewModel.completeFirstTimeSetup(
            days = program.days,
            exercises = program.exercises,
            setupType = "ASSISTED",
            bodyParts = assistedBodyParts.toList(),
            selectedDays = assistedSelectedDays.toList(),
            intensity = assistedIntensity,
            minSets = assistedMinSets,
            maxSets = assistedMaxSets,
            equipment = buildEquipment
        )
    }

    // Add / Edit custom exercise dialog
    if (showAddCustomExerciseDialog || editingExercise != null) {
        AddExerciseDialog(
            allAvailableExercises = allAvailableExercises,
            exercise = editingExercise,
            initialDay = selectedDayInCustom,
            themeKey = themeOption,
            onDismiss = { 
                showAddCustomExerciseDialog = false
                editingExercise = null
            },
            onConfirm = { ex ->
                var syncedEx = ex
                // Sync with existing exercises in the current building list
                if (ex.videoUrl.isBlank()) {
                    val existing = customExercises.find { it.name == ex.name && it.videoUrl.isNotBlank() }
                    if (existing != null) syncedEx = ex.copy(videoUrl = existing.videoUrl)
                }

                if (editingExercise != null) {
                    val index = customExercises.indexOf(editingExercise)
                    if (index != -1) {
                        customExercises[index] = syncedEx
                    }
                } else {
                    customExercises.add(syncedEx)
                }

                // Propagate videoUrl to all other exercises with the same name in the building list
                if (syncedEx.videoUrl.isNotBlank()) {
                    customExercises.indices.forEach { i ->
                        val item = customExercises[i]
                        if (item.name == syncedEx.name && item.videoUrl != syncedEx.videoUrl) {
                            customExercises[i] = item.copy(videoUrl = syncedEx.videoUrl)
                        }
                    }
                }

                showAddCustomExerciseDialog = false
                editingExercise = null
            }
        )
    }

    Surface(
        color = if (isLightTheme) Color.White else Color(0xFF1E1E1E),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = window.screenPadding)
        ) {
            if (isManualReentry) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = getString("action_back"),
                        tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
                verticalArrangement = Arrangement.spacedBy(window.sectionSpacing)
            ) {
                // HEADER HERO BANNER
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLightTheme) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (isLightTheme) 0.08f else 0.2f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = if (isLightTheme) 0.05f else 0.15f)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = getString("welcome_title"),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = getString("welcome_text"),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                            }
                        }
                    }
                }

                item {
                    // ONE two-way switch for the whole build: the left half is the
                    // assisted (auto-generated) build, the right half is the manual
                    // custom build. Both halves are the control — tap one, or drag
                    // across, and the thumb slides between them.
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp)
                                .pointerInput(selectedTab) {
                                    var totalDrag = 0f
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (totalDrag > 50) {
                                                if (selectedTab < 1) selectedTab++
                                            } else if (totalDrag < -50) {
                                                if (selectedTab > 0) selectedTab--
                                            }
                                            totalDrag = 0f
                                        },
                                        onHorizontalDrag = { _, dragAmount ->
                                            totalDrag += dragAmount
                                        }
                                    )
                                }
                        ) {
                            val tabWidth = this.maxWidth / 2
                            val density = LocalDensity.current
                            val swipeThreshold = with(density) { 40.dp.toPx() }

                            // The thumb is the "on" side of the switch. It is inset so the
                            // track stays visible around it as it slides between halves.
                            val thumbInset = 3.dp
                            val indicatorPosition by animateDpAsState(
                                targetValue = tabWidth * selectedTab + thumbInset,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "routineTabIndicator"
                            )

                            Surface(
                                modifier = Modifier
                                    .width(tabWidth - thumbInset * 2)
                                    .height(44.dp)
                                    .graphicsLayer {
                                        translationX = with(density) { indicatorPosition.toPx() }
                                    },
                                color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                                // Radius = track radius (18dp) minus the thumbInset, so the two
                                // curves stay concentric and the thumb never pokes past the track.
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(15.dp),
                                shadowElevation = if (isDeveloper) 0.dp else 2.dp,
                                border = if (isDeveloper) BorderStroke(2.dp, Color.Black) else null
                            ) {}

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(selectedTab) {
                                        var totalDrag = 0f
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (totalDrag > swipeThreshold) {
                                                    if (selectedTab < 1) selectedTab++
                                                } else if (totalDrag < -swipeThreshold) {
                                                    if (selectedTab > 0) selectedTab--
                                                }
                                                totalDrag = 0f
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                totalDrag += dragAmount
                                            }
                                        )
                                    },
                                horizontalArrangement = Arrangement.spacedBy(0.0.dp)
                            ) {
                                val tabs = listOf(
                                    0 to (Icons.Default.FitnessCenter to "assisted_build"),
                                    1 to (Icons.Default.Edit to "build_custom")
                                )
                                tabs.forEach { (index, info) ->
                                    val isSelected = selectedTab == index
                                    
                                    val contentColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "tabContentColor"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { selectedTab = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = info.first,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = contentColor
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = getString(info.second),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = contentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB CONTENT
                item {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "TabContentAnimation"
                    ) { targetTab ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            when (targetTab) {
                                0 -> {
                                    // ASSISTED BUILD — the only generated build on offer.
                                    // Everything it needs is here, and the options most
                                    // people leave alone live under Advanced.

                                    // 1. HOW OFTEN — the quickest choice, so it leads.
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OnboardingStepHeader(stepNumber = null, title = getString("choose_days"))
                                        Card(
                                            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                DaySelectionRow(
                                                    selectedDays = assistedSelectedDays,
                                                    onDayToggle = { day ->
                                                        if (assistedSelectedDays.contains(day)) {
                                                            // Always keep at least one training day.
                                                            if (assistedSelectedDays.size > 1) assistedSelectedDays.remove(day)
                                                        } else assistedSelectedDays.add(day)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // 2. WHAT THE BUILD MAY USE
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OnboardingStepHeader(stepNumber = null, title = getString("equipment"))
                                        EquipmentFilterBlock(
                                            selectedEquipment = buildEquipment,
                                            onEquipmentChange = { buildEquipment = it }
                                        )
                                    }

                                    // 3. WHAT TO TRAIN
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OnboardingStepHeader(stepNumber = null, title = getString("choose_body_parts"))
                                        BodyPartsSelectionBlock(assistedBodyParts)
                                    }

                                    // 4. HOW HARD — sets the target RPE
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OnboardingStepHeader(stepNumber = null, title = getString("choose_intensity"))
                                        IntensitySelectionBlock(assistedIntensity) { assistedIntensity = it }
                                    }

                                    // ADVANCED (collapsible) — fine-tuning most users leave alone
                                    Column {
                                        OnboardingStepHeader(
                                            stepNumber = null,
                                            title = getString("additional_preferences"),
                                            isCollapsible = true,
                                            isExpanded = preferencesExpanded,
                                            onToggle = { preferencesExpanded = !preferencesExpanded }
                                        )
                                        AnimatedVisibility(visible = preferencesExpanded) {
                                            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                SetsAllocationBlock(
                                                    minSets = assistedMinSets,
                                                    maxSets = assistedMaxSets,
                                                    onMinChange = { assistedMinSets = it },
                                                    onMaxChange = { assistedMaxSets = it }
                                                )
                                                WorkoutGoalBlock(
                                                    selectedGoal = selectedWorkoutGoal,
                                                    onGoalChange = { selectedWorkoutGoal = it },
                                                    isDeveloper = isDeveloper
                                                )
                                                PreferenceSection(getString("include_exercises"), allAvailableExercises, assistedIncludedExercises, MaterialTheme.colorScheme.primaryContainer)
                                                PreferenceSection(getString("exclude_exercises"), allAvailableExercises, assistedExcludedExercises, MaterialTheme.colorScheme.errorContainer)
                                            }
                                        }
                                    }

                                    // Warn about anything the chosen parts + equipment could not
                                    // actually cover, using the same build that will be saved.
                                    val buildMissing = getMissingMuscleGroups(
                                        WorkoutTemplates.generateAssistedProgram(
                                            bodyParts = assistedBodyParts.toList(),
                                            selectedDays = assistedSelectedDays.toList(),
                                            intensity = assistedIntensity,
                                            workoutGoal = selectedWorkoutGoal,
                                            includedExercises = assistedIncludedExercises.toList(),
                                            excludedExercises = assistedExcludedExercises.toList(),
                                            minSets = assistedMinSets,
                                            maxSets = assistedMaxSets,
                                            equipment = buildEquipment
                                        ).exercises
                                    )

                                    AnimatedContent(
                                        targetState = buildMissing,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500, delayMillis = 100)))
                                                .togetherWith(fadeOut(animationSpec = tween(200)))
                                        },
                                        label = "BuildWarning"
                                    ) { missing ->
                                        MissingBodyPartsWarningCard(missingParts = missing)
                                    }
                                }
                                else -> {
                                    // CUSTOM CONTENT
                                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                                            DaySelectionRow(
                                                selectedDays = listOf(selectedDayInCustom),
                                                onDayToggle = { selectedDayInCustom = it }
                                            )

                                            val fullDayName = when (selectedDayInCustom) {
                                                "MONDAY" -> getString("monday")
                                                "TUESDAY" -> getString("tuesday")
                                                "WEDNESDAY" -> getString("wednesday")
                                                "THURSDAY" -> getString("thursday")
                                                "FRIDAY" -> getString("friday")
                                                "SATURDAY" -> getString("saturday")
                                                "SUNDAY" -> getString("sunday")
                                                else -> selectedDayInCustom
                                            }

                                            Text(
                                                text = fullDayName,
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    textAlign = TextAlign.Center
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        val exercisesForSelectedDay = customExercises.filter { it.dayOfWeek == selectedDayInCustom }
                                        
                                        // SELECTION / COUNT ROW
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isCustomSelectionMode) {
                                                Surface(
                                                    onClick = {
                                                        if (customSelectedExercises.size == exercisesForSelectedDay.size) {
                                                            customSelectedExercises.clear()
                                                        } else {
                                                            customSelectedExercises.clear()
                                                            customSelectedExercises.addAll(exercisesForSelectedDay)
                                                        }
                                                    },
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                                ) {
                                                    val labelText = if (customSelectedExercises.size == exercisesForSelectedDay.size) getString("deselect_all") else getString("select_all")
                                                    Text(
                                                        text = "$labelText (${customSelectedExercises.size})",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    IconButton(
                                                        onClick = { if (customSelectedExercises.isNotEmpty()) showDeleteMultipleConfirmationCustom = true },
                                                        enabled = customSelectedExercises.isNotEmpty()
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected", tint = if (customSelectedExercises.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                                    }

                                                    IconButton(onClick = { 
                                                        isCustomSelectionMode = false
                                                        customSelectedExercises.clear()
                                                    }) {
                                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = getString("exercises_count", exercisesForSelectedDay.count()),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    IconButton(onClick = { isCustomSelectionMode = true }) {
                                                        Icon(imageVector = Icons.Default.Checklist, contentDescription = "Selection Mode", tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            focusManager.clearFocus()
                                                            copyTargetDay = if (selectedDayInCustom == "MONDAY") "TUESDAY" else "MONDAY"
                                                            showCopyDayDialog = true
                                                        },
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                                    }
                                                    IconButton(onClick = { focusManager.clearFocus(); showAddCustomExerciseDialog = true }, modifier = Modifier.size(40.dp)) {
                                                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                                    }
                                                }
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                            exercisesForSelectedDay.forEach { exercise ->
                                                val isSelected = customSelectedExercises.contains(exercise)
                                                Card(
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (isCustomSelectionMode) {
                                                                if (isSelected) customSelectedExercises.remove(exercise)
                                                                else customSelectedExercises.add(exercise)
                                                            }
                                                        }
                                                ) {
                                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        if (isCustomSelectionMode) {
                                                            Checkbox(
                                                                checked = isSelected,
                                                                onCheckedChange = { checked ->
                                                                    if (checked) customSelectedExercises.add(exercise)
                                                                    else customSelectedExercises.remove(exercise)
                                                                },
                                                                modifier = Modifier.padding(end = 8.dp)
                                                            )
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = translateExerciseName(exercise.name), 
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                                                MuscleGroupBadge(muscleGroup = exercise.muscleGroup, exerciseName = exercise.name)
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                    text = if (exercise.targetReps.contains("sec")) getQuantityString("sets_x_time_pl", exercise.targetSets, exercise.targetSets, exercise.targetReps)
                                                                    else getQuantityString("sets_x_reps_pl", exercise.targetSets, exercise.targetSets, exercise.targetReps),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                if (exercise.targetIntensity.isNotBlank()) {
                                                                    val rpeExpl = com.grafizum.repio.ui.components.getRpeRirExplanation(exercise.targetIntensity)
                                                                    val displayIntensity = if (rpeExpl.isNotBlank()) "${exercise.targetIntensity} ($rpeExpl)" else exercise.targetIntensity
                                                                    Text(
                                                                        text = " • $displayIntensity",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                                if (exercise.tempo.isNotBlank()) {
                                                                    Text(
                                                                        text = " • ${com.grafizum.repio.ui.components.getTempoLabel(exercise.tempo)}",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.weight(1f, fill = false),
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        if (!isCustomSelectionMode) {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                IconButton(onClick = { replacingExercise = exercise }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                                                                IconButton(onClick = { focusManager.clearFocus(); editingExercise = exercise }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                                                                IconButton(onClick = { customExerciseToDelete = exercise }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        AnimatedContent(
                                            targetState = getMissingMuscleGroups(customExercises.toList()),
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500, delayMillis = 100)))
                                                    .togetherWith(fadeOut(animationSpec = tween(200)))
                                            },
                                            label = "CustomWarning"
                                        ) { missing ->
                                            MissingBodyPartsWarningCard(missingParts = missing)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // FOOTER
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isManualReentry) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) { Text(getString("close"), style = MaterialTheme.typography.titleSmall) }
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (isManualReentry) showFinalSaveConfirmation = true 
                        else {
                            saveSelectedBuild()
                            onComplete()
                        }
                    },
                    // The assisted build needs at least one training day; the custom
                    // builder seeds its own days so it is always saveable.
                    enabled = if (selectedTab == 0) assistedSelectedDays.isNotEmpty() else true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(getString("save_and_start"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    // DIALOGS
    if (showFinalSaveConfirmation) {
        SaveConfirmationDialog(onDismiss = { showFinalSaveConfirmation = false }, onConfirm = {
            saveSelectedBuild()
            onComplete()
            showFinalSaveConfirmation = false
        })
    }

    if (showDeleteMultipleConfirmationCustom) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { showDeleteMultipleConfirmationCustom = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("confirm_delete_multiple_title"))
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
                    Text(getQuantityString("confirm_delete_multiple_msg", customSelectedExercises.count()))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        customExercises.removeAll(customSelectedExercises)
                        customSelectedExercises.clear()
                        isCustomSelectionMode = false
                        showDeleteMultipleConfirmationCustom = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(getString("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showDeleteMultipleConfirmationCustom = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    customExerciseToDelete?.let { exercise ->
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { customExerciseToDelete = null },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("confirm_delete_title"))
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
                    Text(getString("confirm_delete_msg"))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        customExercises.remove(exercise)
                        customExerciseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(getString("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    customExerciseToDelete = null
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    if (showCopyDayDialog) {
        val focusManager = LocalFocusManager.current
        val sourceExercises = customExercises.filter { it.dayOfWeek == selectedDayInCustom }
        val sourceDayName = dayOfWeekDisplayName(selectedDayInCustom)
        AlertDialog(
            onDismissRequest = { showCopyDayDialog = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("copy_day_title", sourceDayName))
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
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = getString("copy_day_desc", sourceDayName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        DaySelectionRow(
                            selectedDays = listOf(copyTargetDay),
                            onDayToggle = { copyTargetDay = it }
                        )

                        // Replace vs append mode
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = copyReplaceExercises,
                                onClick = { copyReplaceExercises = true },
                                label = { Text(getString("copy_day_replace")) }
                            )
                            FilterChip(
                                selected = !copyReplaceExercises,
                                onClick = { copyReplaceExercises = false },
                                label = { Text(getString("copy_day_append")) }
                            )
                        }

                        if (sourceExercises.isEmpty()) {
                            Text(
                                text = getString("no_exercises_scheduled"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (sourceExercises.isNotEmpty() && copyTargetDay != selectedDayInCustom) {
                            if (copyReplaceExercises) {
                                customExercises.removeAll { it.dayOfWeek == copyTargetDay }
                            }
                            val maxOrder = customExercises
                                .filter { it.dayOfWeek == copyTargetDay }
                                .maxOfOrNull { it.orderIndex } ?: 0
                            val copies = sourceExercises.sortedBy { it.orderIndex }.mapIndexed { index, ex ->
                                ex.copy(
                                    id = 0L,
                                    dayOfWeek = copyTargetDay,
                                    orderIndex = maxOrder + index + 1
                                )
                            }
                            customExercises.addAll(copies)

                            // Also update the target day entity so it is a workout day
                            val dayIndex = customDays.indexOfFirst { it.dayOfWeek == copyTargetDay }
                            if (dayIndex != -1) {
                                val sourceDay = customDays.firstOrNull { it.dayOfWeek == selectedDayInCustom }
                                customDays[dayIndex] = customDays[dayIndex].copy(
                                    isRestDay = false,
                                    title = sourceDay?.title ?: customDays[dayIndex].title
                                )
                            }
                        }
                        showCopyDayDialog = false
                    },
                    enabled = sourceExercises.isNotEmpty() && copyTargetDay != selectedDayInCustom
                ) {
                    Text(getString("copy"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showCopyDayDialog = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    replacingExercise?.let { exercise ->
        val anchorIndex = remember(exercise) {
            customExercises.indexOfFirst { it.name == exercise.name && it.dayOfWeek == exercise.dayOfWeek }.coerceAtLeast(0)
        }
        ReplaceExerciseDialog(
            exercise = exercise,
            onDismiss = { replacingExercise = null },
            onReplace = { name, muscleGroup ->
                // Drop the original and put the pick in its slot, keeping any demo
                // link already recorded for that exercise.
                val knownUrl = customExercises
                    .firstOrNull { it.name == name && it.videoUrl.isNotBlank() }
                    ?.videoUrl ?: ""
                customExercises.removeAll {
                    it.dayOfWeek == exercise.dayOfWeek && it.name == exercise.name
                }
                customExercises.add(
                    anchorIndex,
                    exercise.copy(
                        id = 0L,
                        name = ExerciseLocalization.canonicalFor(name) ?: name,
                        muscleGroup = muscleGroup,
                        videoUrl = knownUrl
                    )
                )
            }
        )
    }

}

/**
 * The primary training goal. It is NOT a duplicate of difficulty or training
 * style: difficulty sets the target RPE, style sets the tempo, while the goal
 * drives the rep ranges (1–5 for strength vs 12–20 for conditioning), the rest
 * times and the set counts. Kept out of the main flow but still selectable —
 * without it every routine would silently be built as bodybuilding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutGoalBlock(
    selectedGoal: String,
    onGoalChange: (String) -> Unit,
    isDeveloper: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val goalKeys = listOf(
        "goal_bodybuilding", "goal_strength", "goal_calisthenics",
        "goal_functional", "goal_olympic", "goal_powerbuilding",
        "goal_crossfit", "goal_athletic"
    )
    val selectedGoalLabel = when (selectedGoal) {
        "goal_bodybuilding" -> getString("goal_bodybuilding")
        "goal_strength" -> getString("goal_strength")
        "goal_calisthenics" -> getString("goal_calisthenics")
        "goal_functional" -> getString("goal_functional")
        "goal_olympic" -> getString("goal_olympic")
        "goal_powerbuilding" -> getString("goal_powerbuilding")
        "goal_crossfit" -> getString("goal_crossfit")
        "goal_athletic" -> getString("goal_athletic")
        else -> ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedGoalLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(getString("workout_goal")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDeveloper) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onSurface)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                goalKeys.forEach { goalKey ->
                    val label = when (goalKey) {
                        "goal_bodybuilding" -> getString("goal_bodybuilding")
                        "goal_strength" -> getString("goal_strength")
                        "goal_calisthenics" -> getString("goal_calisthenics")
                        "goal_functional" -> getString("goal_functional")
                        "goal_olympic" -> getString("goal_olympic")
                        "goal_powerbuilding" -> getString("goal_powerbuilding")
                        "goal_crossfit" -> getString("goal_crossfit")
                        "goal_athletic" -> getString("goal_athletic")
                        else -> ""
                    }
                    DropdownMenuItem(
                        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onGoalChange(goalKey)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        val impact = when (selectedGoal) {
            "goal_strength", "goal_olympic" -> getString("goal_strength_desc")
            "goal_bodybuilding", "goal_powerbuilding" -> getString("goal_bodybuilding_desc")
            "goal_calisthenics", "goal_crossfit" -> getString("goal_calisthenics_desc")
            "goal_functional", "goal_athletic" -> getString("goal_functional_desc")
            else -> ""
        }
        if (impact.isNotBlank()) {
            Text(
                text = impact,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentFilterBlock(
    selectedEquipment: List<String>,
    onEquipmentChange: (List<String>) -> Unit
) {
    val equipmentOptions = listOf(
        WorkoutTemplates.EQUIPMENT_BODYWEIGHT to getString("bodyweight_label"),
        WorkoutTemplates.EQUIPMENT_DUMBBELL to getString("equipment_dumbbell"),
        WorkoutTemplates.EQUIPMENT_BARBELL to getString("equipment_barbell"),
        WorkoutTemplates.EQUIPMENT_GYM to getString("equipment_gym")
    )

    // "All" is not a fifth equipment type — see WorkoutTemplates.toggleEquipmentSelection.
    val optionKeys = equipmentOptions.map { it.first }
    fun toggle(key: String) {
        onEquipmentChange(WorkoutTemplates.toggleEquipmentSelection(selectedEquipment, key, optionKeys))
    }

    // The section title comes from the caller's OnboardingStepHeader.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            EquipmentChip(
                label = getString("equipment_all"),
                selected = selectedEquipment.isEmpty(),
                modifier = Modifier.weight(1f)
            ) { toggle(WorkoutTemplates.EQUIPMENT_ALL) }
            EquipmentChip(
                label = equipmentOptions[0].second,
                selected = selectedEquipment.contains(equipmentOptions[0].first),
                modifier = Modifier.weight(1f)
            ) { toggle(equipmentOptions[0].first) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            EquipmentChip(
                label = equipmentOptions[1].second,
                selected = selectedEquipment.contains(equipmentOptions[1].first),
                modifier = Modifier.weight(1f)
            ) { toggle(equipmentOptions[1].first) }
            EquipmentChip(
                label = equipmentOptions[2].second,
                selected = selectedEquipment.contains(equipmentOptions[2].first),
                modifier = Modifier.weight(1f)
            ) { toggle(equipmentOptions[2].first) }
        }
        EquipmentChip(
            label = equipmentOptions[3].second,
            selected = selectedEquipment.contains(equipmentOptions[3].first),
            modifier = Modifier.fillMaxWidth()
        ) { toggle(equipmentOptions[3].first) }
    }
}

/** One equipment chip, stretched to its grid cell so the rows line up symmetrically. */
@Composable
private fun EquipmentChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        },
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceSection(
    title: String,
    allExercises: List<String>,
    list: MutableList<String>,
    containerColor: Color
) {
    var input by remember { mutableStateOf("") }
    val isDeveloper = LocalDeveloperSkin.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SearchableExerciseField(
                    allExercises = allExercises,
                    value = input,
                    onValueChange = { input = it },
                    onItemSelected = { input = it },
                    label = "",
                    placeholder = "Search..."
                )
            }
            Button(
                onClick = { if (input.isNotBlank()) { list.add(input.trim()); input = "" } },
                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) { Text(getString("add_pref"), style = MaterialTheme.typography.labelSmall) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            list.forEach { ex ->
                Surface(
                    color = containerColor,
                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null,
                    modifier = Modifier.clickable { list.remove(ex) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(translateExerciseName(ex), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BodyPartsSelectionBlock(selected: MutableList<String>) {
    val coroutineScope = rememberCoroutineScope()
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val isDeveloper = LocalDeveloperSkin.current

    val categories = listOf(
        getString("arms") to listOf("Biceps", "Triceps", "Forearms"),
        getString("legs") to listOf("Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"),
        getString("back") to listOf("Lats", "Upper Back", "Upper Traps", "Spinal Erectors"),
        getString("chest") to listOf("Chest"),
        getString("shoulders") to listOf("Front Deltoids", "Lateral Deltoids", "Rear Deltoids"),
        getString("abs") to listOf("Abs", "Obliques")
    )

    val balancedPreset = listOf(
        "Chest", "Lats", "Upper Back", 
        "Quadriceps", "Hamstrings", "Glutes", "Calves",
        "Front Deltoids", "Lateral Deltoids",
        "Biceps", "Triceps", "Abs"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getString("select_body_parts_desc"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // The preset button on the right keeps its intrinsic size; a long
                    // translated description wraps inside the leftover width instead.
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Surface(
                    onClick = {
                        selected.clear()
                        selected.addAll(balancedPreset)
                    },
                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(getString("balanced_preset"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Category Buttons in 2 rows
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                categories.forEach { (cat, parts) ->
                    val expanded = expandedCategory == cat
                    val count = parts.count { selected.contains(it) }

                    val backgroundColor by animateColorAsState(
                        targetValue = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        label = "catBackground"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        label = "catContent"
                    )

                    Surface(
                        onClick = {
                            if (expanded) {
                                expandedCategory = null
                            } else {
                                expandedCategory = cat
                                coroutineScope.launch {
                                    delay(200)
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                        color = backgroundColor,
                        contentColor = contentColor,
                        border = if (isDeveloper && expanded) BorderStroke(2.dp, Color.Black)
                                 else if (!expanded) BorderStroke(1.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.outlineVariant) 
                                 else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (count > 0) {
                                    Surface(
                                        color = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        modifier = Modifier.padding(start = 6.dp).size(14.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Shared Dropdown Area (Expands the box)
            AnimatedVisibility(
                visible = expandedCategory != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AnimatedContent(
                    targetState = expandedCategory,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.92f, animationSpec = tween(500, delayMillis = 100)))
                            .togetherWith(fadeOut(animationSpec = tween(200)))
                    },
                    label = "BodyPartsContent"
                ) { targetCat ->
                    val currentParts = categories.find { it.first == targetCat }?.second ?: emptyList()
                    
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentParts.forEach { part ->
                                val isSel = selected.contains(part)
                                FilterChip(
                                    selected = isSel,
                                    onClick = { 
                                        if (isSel) selected.remove(part) 
                                        else selected.add(part) 
                                    },
                                    label = { Text(translateMuscleGroup(part), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = if (isDeveloper && isSel) FilterChipDefaults.filterChipBorder(enabled = true, selected = true, selectedBorderColor = Color.Black, selectedBorderWidth = 2.dp) else FilterChipDefaults.filterChipBorder(enabled = true, selected = isSel)
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
private fun IntensitySelectionBlock(selected: String, onSelect: (String) -> Unit) {
    val levels = listOf("Easy", "Medium", "Hard")
    val selectedIndex = levels.indexOf(selected).coerceAtLeast(0)
    val isDeveloper = LocalDeveloperSkin.current
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val density = LocalDensity.current
        val swipeThreshold = with(density) { 40.dp.toPx() }
        
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp))
                .padding(4.dp)
                .pointerInput(selected) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalDrag > swipeThreshold) {
                                if (selectedIndex < levels.size - 1) onSelect(levels[selectedIndex + 1])
                            } else if (totalDrag < -swipeThreshold) {
                                if (selectedIndex > 0) onSelect(levels[selectedIndex - 1])
                            }
                            totalDrag = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        }
                    )
                }
        ) {
            val tabWidth = this.maxWidth / levels.size
            val indicatorPosition by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "intensityIndicator"
            )

            // Sliding Indicator
            val density = LocalDensity.current
            Surface(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = with(density) { indicatorPosition.toPx() }
                    },
                color = MaterialTheme.colorScheme.primary,
                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(7.dp),
                shadowElevation = if (isDeveloper) 0.dp else 1.dp
            ) {}

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                levels.forEach { level ->
                    val isSelected = selected == level
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "intensityContentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSelect(level)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = getString("intensity_${level.lowercase()}"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = contentColor
                            )
                            Text(
                                text = getString("intensity_rpe_value", WorkoutTemplates.getRpeForIntensity(level)),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = contentColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Description Section
        val description = when(selected) {
            "Easy" -> getString("intensity_easy_desc")
            "Medium" -> getString("intensity_medium_desc")
            "Hard" -> getString("intensity_hard_desc")
            else -> ""
        }

        AnimatedContent(
            targetState = description,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.92f, animationSpec = tween(500, delayMillis = 100)))
                    .togetherWith(fadeOut(animationSpec = tween(200)))
            },
            label = "intensityDesc"
        ) { desc ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = getString("intensity_rpe_value", WorkoutTemplates.getRpeForIntensity(selected)),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
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
                Text(getString("confirm_new_program_title"))
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
                Text(getString("confirm_new_program_message"))
            }
        },
        confirmButton = {
            Button(onClick = {
                focusManager.clearFocus()
                onConfirm()
            }) { Text(getString("confirm")) }
        },
        dismissButton = {
            TextButton(onClick = {
                focusManager.clearFocus()
                onDismiss()
            }) { Text(getString("cancel")) }
        }
    )
}

@Composable
fun DaySelectionRow(
    selectedDays: List<String>,
    onDayToggle: (String) -> Unit
) {
    val allDays = listOf(
        "MONDAY" to getString("monday_shortest"),
        "TUESDAY" to getString("tuesday_shortest"),
        "WEDNESDAY" to getString("wednesday_shortest"),
        "THURSDAY" to getString("thursday_shortest"),
        "FRIDAY" to getString("friday_shortest"),
        "SATURDAY" to getString("saturday_shortest"),
        "SUNDAY" to getString("sunday_shortest")
    )
    val isDeveloper = LocalDeveloperSkin.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        allDays.forEach { (day, label) ->
            val isSelected = selectedDays.contains(day)
            Surface(
                onClick = { onDayToggle(day) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                shape = if (isDeveloper) RoundedCornerShape(0.dp) else CircleShape,
                border = if (isSelected) {
                             BorderStroke(2.dp, if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary)
                         } else BorderStroke(1.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

fun remapTemplateToDays(
    template: WorkoutTemplate,
    targetDays: List<String>
): Pair<List<RoutineDayEntity>, List<RoutineExerciseEntity>> {
    val weekDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val logicalWorkoutDays = template.dayTemplates.filter { !it.isRestDay }
    
    val routineDays = mutableListOf<RoutineDayEntity>()
    val routineExercises = mutableListOf<RoutineExerciseEntity>()
    
    // Sort targetDays according to week order
    val sortedTargetDays = targetDays.sortedBy { weekDays.indexOf(it) }
    
    weekDays.forEach { day ->
        val targetIndex = sortedTargetDays.indexOf(day)
        if (targetIndex != -1) {
            // This is a workout day
            val logicalDay = logicalWorkoutDays.getOrNull(targetIndex % logicalWorkoutDays.size)
            if (logicalDay != null) {
                routineDays.add(RoutineDayEntity(
                    dayOfWeek = day,
                    title = day.substring(0, 1) + day.substring(1).lowercase() + " — " + getEnglishTitleFromKey(logicalDay.titleKey),
                    description = logicalDay.descKey,
                    isRestDay = false
                ))
                
                // Map exercises
                template.exercises.filter { it.dayOfWeek == logicalDay.dayOfWeek }.forEach { ex ->
                    routineExercises.add(ex.copy(dayOfWeek = day))
                }
            }
        } else {
            // This is a rest day
            routineDays.add(RoutineDayEntity(
                dayOfWeek = day,
                title = day.substring(0, 1) + day.substring(1).lowercase() + " — Rest",
                description = "rest_desc",
                isRestDay = true
            ))
        }
    }
    
    return routineDays to routineExercises
}

fun getEnglishTitleFromKey(key: String): String {
    return when(key) {
        "pull_title" -> "Pull (Back & Biceps)"
        "push_title" -> "Push (Chest, Shoulders, Triceps)"
        "legs_core_title" -> "Legs & Core"
        "upper_power_title" -> "Upper Body Power"
        "lower_power_title" -> "Lower Body & Core"
        "upper_hypertrophy_title" -> "Upper Body Hypertrophy"
        "lower_hypertrophy_title" -> "Lower Body Hypertrophy"
        "full_body_a_title" -> "Full Body A"
        "full_body_b_title" -> "Full Body B"
        "full_body_c_title" -> "Full Body C"
        else -> key
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    allAvailableExercises: List<String>,
    exercise: RoutineExerciseEntity? = null,
    initialDay: String = "MONDAY",
    themeKey: String,
    onDismiss: () -> Unit,
    onConfirm: (RoutineExerciseEntity) -> Unit
) {
    var dayOfWeek by remember { mutableStateOf(exercise?.dayOfWeek ?: initialDay) }
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "Chest") }
    var targetSets by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "") }
    var targetWeightValue by remember { mutableStateOf(if (exercise?.targetWeightKg == 0.0) "" else exercise?.targetWeightKg?.toString() ?: "") }
    var targetIntensityStr by remember { mutableStateOf(exercise?.targetIntensity ?: "") }
    var restSecondsStr by remember { mutableStateOf(exercise?.restSeconds?.toString() ?: "60") }
    var isTimeBased by remember { mutableStateOf(exercise?.targetReps?.contains("sec", ignoreCase = true) ?: false) }
    
    var targetRepsValue by remember { mutableStateOf(if (!isTimeBased) exercise?.targetReps ?: "" else "8-12") }
    var repsManuallyEdited by remember { mutableStateOf(exercise != null) }
    var targetTimeValue by remember { mutableStateOf(if (isTimeBased) exercise?.targetReps?.replace(" sec", "") ?: "30" else "30") }
    var videoUrl by remember { mutableStateOf(exercise?.videoUrl ?: "") }

    val focusManager = LocalFocusManager.current
    var showInvalidCharToast by remember { mutableStateOf(false) }

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
            } else parts.size == 1 && parts[0].toIntOrNull() != null
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

    var expandedDay by remember { mutableStateOf(false) }
    val daysOptions = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")

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
                Text(getString(if (exercise == null) "add_custom_exercise" else "edit_exercise"))
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

                ExposedDropdownMenuBox(
                    expanded = expandedDay,
                    onExpandedChange = { 
                        focusManager.clearFocus()
                        expandedDay = !expandedDay 
                    }
                ) {
                    OutlinedTextField(
                        value = dayOfWeek,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(getString("day_of_week")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDay,
                        onDismissRequest = { expandedDay = false }
                    ) {
                        daysOptions.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d, style = MaterialTheme.typography.bodySmall) },
                                onClick = { 
                                    focusManager.clearFocus()
                                    dayOfWeek = d; expandedDay = false 
                                }
                            )
                        }
                    }
                }

                SearchableExerciseField(
                    allExercises = allAvailableExercises,
                    value = name,
                    onValueChange = { 
                        name = it
                        // Resolve a name typed in another language first, so a German name still
                        // auto-detects its muscle group instead of falling through to "Other".
                        val detected = WorkoutTemplates.getMuscleGroupForExercise(
                            ExerciseLocalization.canonicalFor(it) ?: it
                        )
                        if (detected != "Other") muscleGroup = detected
                    },
                    onItemSelected = { selectedName ->
                        focusManager.clearFocus()
                        isTimeBased = WorkoutTemplates.isExerciseTimeBased(selectedName)
                        if (name != selectedName) videoUrl = ""
                        if (targetSets.isBlank()) targetSets = WorkoutTemplates.getRecommendedSetsForExercise(selectedName).toString()
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

                if (name.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                val repsError = targetRepsValue.isNotEmpty() && !isRepsValid
                val showSupportingSpacer = repsError

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    CompactNumericField(
                        value = targetSets,
                        onValueChange = { if (it.all { char -> char.isDigit() }) targetSets = it },
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
                            val digits = targetTimeValue.filter { it.isDigit() }
                            val interactionSource = remember { MutableInteractionSource() }
                            BasicTextField(
                                value = if (digits.isNotBlank()) "${digits}s" else "30s",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().defaultMinSize(minHeight = 56.dp).fillMaxHeight(),
                                interactionSource = interactionSource,
                                enabled = true,
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start, fontSize = 16.sp),
                                decorationBox = { innerTextField ->
                                    OutlinedTextFieldDefaults.DecorationBox(
                                        value = if (digits.isNotBlank()) "${digits}s" else "30s",
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
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            supportingText = if (repsHasRangeError) {
                                {
                                    Text(getString("min_greater_than_max"), color = MaterialTheme.colorScheme.error, maxLines = 1)
                                }
                            } else null
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
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) targetWeightValue = it },
                        label = getString("target_weight"),
                        placeholder = "20.0",
                        suffix = { Text("kg", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                        placeholder = "RPE 8",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }


                // Tempo is not a manual choice — it is never touched here, so an
                // existing value on the exercise is kept as-is.
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text(getString("demo_link_optional")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ThemedToast(message = getString("invalid_char_error"), themeKey = themeKey, visible = showInvalidCharToast, onDismiss = { showInvalidCharToast = false })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val finalReps = if (isTimeBased) {
                        val digits = targetTimeValue.filter { it.isDigit() }
                        if (digits.isNotBlank()) "$digits sec" else "30 sec"
                    } else WorkoutTemplates.enforceRepRange(targetRepsValue.trim())
                    onConfirm(RoutineExerciseEntity(
                        id = exercise?.id ?: 0, 
                        dayOfWeek = dayOfWeek, 
                        // Stored canonically when the name is a pool exercise, so equipment and
                        // muscle lookups keep working whatever language it was typed in.
                        name = ExerciseLocalization.canonicalFor(name) ?: name.trim(), 
                        muscleGroup = muscleGroup, 
                        targetSets = targetSets.toIntOrNull() ?: 3, 
                        targetReps = finalReps, 
                        targetIntensity = targetIntensityStr, 
                        targetWeightKg = targetWeightValue.toDoubleOrNull() ?: 0.0,
                        restSeconds = restSecondsStr.toIntOrNull() ?: 60, 
                        videoUrl = videoUrl, 
                        orderIndex = exercise?.orderIndex ?: 99,
                        // Kept from the generated build; tempo is not typed in by hand.
                        tempo = exercise?.tempo ?: ""
                    ))
                },
                enabled = name.isNotBlank() && (if (isTimeBased) targetTimeValue else targetRepsValue).isNotBlank() && (isTimeBased || isRepsValid)
            ) { Text(getString(if (exercise == null) "add" else "save")) }
        },
        dismissButton = {
            TextButton(onClick = {
                focusManager.clearFocus()
                onDismiss()
            }) { Text(getString("cancel")) }
        }
    )
}

                @Composable
private fun SetsAllocationBlock(
    minSets: Int,
    maxSets: Int,
    onMinChange: (Int) -> Unit,
    onMaxChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = getString("sets_allocation"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SetsInput(
                label = getString("min_sets"),
                value = minSets,
                onValueChange = onMinChange,
                modifier = Modifier.weight(1f)
            )
            SetsInput(
                label = getString("max_sets"),
                value = maxSets,
                onValueChange = onMaxChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SetsInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(if (value == 0) "" else value.toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            if (newValue.isEmpty()) {
                textValue = ""
                onValueChange(0)
            } else if (newValue.all { it.isDigit() }) {
                textValue = newValue
                newValue.toIntOrNull()?.let { onValueChange(it) }
            }
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = { Text(getString("no_limit"), style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier
    )
}

@Composable
fun OnboardingStepHeader(
    title: String,
    modifier: Modifier = Modifier,
    stepNumber: Int? = null,
    isCollapsible: Boolean = false,
    isExpanded: Boolean = false,
    onToggle: (() -> Unit)? = null
) {
    val isDeveloper = LocalDeveloperSkin.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .then(if (isCollapsible && onToggle != null) Modifier.clickable { onToggle() } else Modifier)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (stepNumber != null) {
                Surface(
                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else CircleShape,
                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Surface(
                color = if (isDeveloper) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        if (isCollapsible) {
            Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
