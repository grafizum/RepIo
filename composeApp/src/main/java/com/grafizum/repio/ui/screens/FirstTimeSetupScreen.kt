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
import com.grafizum.repio.ui.components.EquipmentIconImage
import com.grafizum.repio.ui.components.exerciseSetsSummaryLabel
import com.grafizum.repio.ui.components.equipmentTagIcon
import com.grafizum.repio.ui.components.equipmentTagShortLabelKey
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
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.ExerciseLocalization
import com.grafizum.repio.data.RoutineDayEntity
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplate
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.LocalWeightUnit
import com.grafizum.repio.ui.components.loadInputToKg
import com.grafizum.repio.ui.components.MissingBodyPartsWarningCard
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.components.ReplaceExerciseDialog
import com.grafizum.repio.ui.components.RepsTimeToggle
import com.grafizum.repio.ui.components.EditorLevelData
import com.grafizum.repio.ui.components.LevelPickerCard
import com.grafizum.repio.ui.components.levelEditorControls
import com.grafizum.repio.ui.components.SplitStyleSelector
import com.grafizum.repio.ui.components.RepsRangeField
import com.grafizum.repio.ui.components.CompactNumericField
import com.grafizum.repio.ui.components.SearchableExerciseField
import com.grafizum.repio.ui.components.ThemedToast
import com.grafizum.repio.ui.components.AppDialog
import com.grafizum.repio.ui.components.getExerciseDetails
import com.grafizum.repio.ui.components.balanceGroupsForParts
import com.grafizum.repio.ui.components.weeklyBalanceIssues
import com.grafizum.repio.ui.components.loadFieldLabel
import com.grafizum.repio.ui.components.loadFieldPlaceholder
import com.grafizum.repio.ui.components.loadFieldSuffix
import com.grafizum.repio.ui.components.loadFieldValueText
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
    // The default week is the premade 3-day pattern, so the screen starts on the same
    // answer the premade menu would produce (MON/WED/FRI) instead of a hand-typed list
    // that could drift away from it.
    val assistedSelectedDays = remember {
        mutableStateListOf<String>().also { it.addAll(WorkoutTemplates.premadeWeekFor(3)) }
    }
    // Whether the user is picking the days themselves. OFF is the premade week for the
    // chosen frequency (the recommended rest distribution); ON hands the day chips to the
    // user, which is what makes 1 and 7 days honest choices rather than refused ones.
    var assistedSpecificDays by remember { mutableStateOf(false) }
    // What each of those days IS. Separate from how many there are: a 5-day
    // week can be an upper/lower alternation or a push/pull/legs cycle, and the
    // day picker alone never said which one you were getting.
    var assistedSplitStyle by remember { mutableStateOf(WorkoutTemplates.SplitStyle.DEFAULT) }
    var assistedIntensity by remember { mutableStateOf("Medium") }
    var assistedDuration by remember { mutableStateOf(WorkoutTemplates.DURATION_STANDARD) }
    val assistedIncludedExercises = remember { mutableStateListOf<String>() }
    val assistedExcludedExercises = remember { mutableStateListOf<String>() }
    var preferencesExpanded by remember { mutableStateOf(false) }

    // Equipment types the build may use (empty = all).
    //
    // SEEDED FROM THE SAVED KIT. This started empty, so every time the user
    // reopened the build screen it showed a blank equipment picker and Save
    // wrote that blank back over `setup_equipment` — the user's kit was
    // invisible and one routine rebuild could wipe it. An empty list is still
    // a meaningful value ("no restriction"), so it is only used when nothing
    // has ever been saved.
    var buildEquipment by remember {
        mutableStateOf(viewModel.savedEquipmentSelection().ifEmpty { listOf<String>() })
    }
    // What the picker showed on open. Save compares against this so an
    // UNTOUCHED picker cannot overwrite the stored selection with a blank.
    val buildEquipmentOnOpen = remember { buildEquipment }

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
        // Never write a selection the user did not make. The equipment picker
        // is seeded from storage now, so an UNCHANGED picker means "keep what I
        // have" — passing it through anyway is what used to silently blank the
        // kit on a routine rebuild.
        val equipmentToSave =
            if (buildEquipment == buildEquipmentOnOpen) viewModel.savedEquipmentSelection()
            else buildEquipment
        if (selectedTab == 1) {
            // The hand-built routine is gated by the SAME kit the assisted tab
            // uses, and the selection is persisted with it. Saving a custom
            // build without `equipment` was the last remaining way the week
            // ignored the tiles: the prefill carries barbell staples, they were
            // written through unfiltered, AND `setup_equipment` was overwritten
            // with "" - so every later consumer (goal-change regeneration,
            // template application, the pickers) read "no restriction" for the
            // rest of the install.
            viewModel.completeFirstTimeSetup(
                days = customDays.toList(),
                exercises = customExercises.toList(),
                setupType = "CUSTOM",
                bodyParts = assistedBodyParts.toList(),
                selectedDays = assistedSelectedDays.toList(),
                intensity = assistedIntensity,
                equipment = equipmentToSave,
                workoutDuration = assistedDuration
            )
            return
        }
        val program = WorkoutTemplates.generateEngineProgram(
            bodyParts = assistedBodyParts.toList(),
            selectedDays = assistedSelectedDays.toList(),
            intensity = assistedIntensity,
            workoutGoal = selectedWorkoutGoal,
            includedExercises = assistedIncludedExercises.toList(),
            excludedExercises = assistedExcludedExercises.toList(),
            equipment = equipmentToSave,
            duration = assistedDuration,
            splitStyle = assistedSplitStyle
        )
        viewModel.completeFirstTimeSetup(
            days = program.days,
            exercises = program.exercises,
            setupType = "ASSISTED", 
            bodyParts = assistedBodyParts.toList(),
            selectedDays = assistedSelectedDays.toList(),
            intensity = assistedIntensity,
            equipment = equipmentToSave,
            workoutDuration = assistedDuration,
            splitStyle = assistedSplitStyle.id
        )
    }

    // The level ladder opened FROM the add dialog. Held at screen scope so the
    // dialog stays open behind it: a level chosen here lands, and the user lands
    // back on the half-filled form rather than losing it.
    var ladderExercise by remember { mutableStateOf<String?>(null) }

    // Add / Edit custom exercise dialog
    if (showAddCustomExerciseDialog || editingExercise != null) {
        AddExerciseDialog(
            allAvailableExercises = allAvailableExercises,
            exercise = editingExercise,
            initialDay = selectedDayInCustom,
            themeKey = themeOption,
            userEquipmentCategories = EquipmentFilterBlockCategories(buildEquipment),
            userGrantedTags = equipmentFilterBlockGrantedTags(buildEquipment, viewModel),
            // The level facts are resolved by NAME inside the dialog, so this editor
            // draws the same rungs for the same exercise as the routine editor.
            levelDataFor = { levelName, pageStart -> editorLevelData(viewModel, levelName, pageStart) },
            onLevelChange = { levelName, level ->
                viewModel.setExerciseLevelFromEditor(levelName, level)
            },
            onStageChange = { levelName, level, stage ->
                viewModel.setExerciseStateFromEditor(levelName, level, stage)
            },
            onOpenLevelLadder = { name -> ladderExercise = name },
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
        // The ladder stacked over the add dialog, so "See every level" reaches any
        // rung the fifteen-row page cannot.
        ladderExercise?.let { name ->
            LevelDetailsDialog(
                viewModel = viewModel,
                exerciseName = name,
                fromEditorEditPass = true,
                onDismiss = { ladderExercise = null }
            )
        }
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
                        tint = MaterialTheme.colorScheme.primary
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
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp),
                                    border = null) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
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
                        shape = RoundedCornerShape(18.dp),
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
                                color = MaterialTheme.colorScheme.primary,
                                // Radius = track radius (18dp) minus the thumbInset, so the two
                                // curves stay concentric and the thumb never pokes past the track.
                                shape = RoundedCornerShape(15.dp),
                                shadowElevation = 2.dp,
                                border = null) {}

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
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            border = null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                // 1a. HOW MANY DAYS — the menu the owner asked
                                                // for (2026-10-08). 1..7 so the two flagged
                                                // frequencies stay reachable; the label under
                                                // the row is what marks them as not recommended.
                                                DaysPerWeekMenu(
                                                    daysPerWeek = assistedSelectedDays.size,
                                                    onSelect = { count ->
                                                        assistedSelectedDays.clear()
                                                        assistedSelectedDays.addAll(
                                                            WorkoutTemplates.premadeWeekFor(count)
                                                        )
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                // 1b. WHICH DAYS — the week the count produces,
                                                // drawn as a strip so the pauses between
                                                // training days are visible at a glance.
                                                WeekStrip(selectedDays = assistedSelectedDays)
                                                // The weekday chips are NOT on screen by
                                                // default (owner, 2026-10-09: show the days
                                                // of the week "only if the user distinctively
                                                // opens it"). The specific-days switch below
                                                // is that deliberate opening; until then the
                                                // strip above carries the same information
                                                // without a grid of taps.
                                                if (assistedSpecificDays) {
                                                    Spacer(modifier = Modifier.height(12.dp))
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
                                                // The owner wanted the "not recommended"
                                                // marker "under or over the days of the week".
                                                // It sits directly under the chips, where the
                                                // count it refers to is visible.
                                                if (WorkoutTemplates.isNotRecommendedFrequency(assistedSelectedDays.size)) {
                                                    Text(
                                                        text = getString("days_not_recommended"),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier
                                                            .padding(top = 6.dp)
                                                            .semantics { testTag = "days_not_recommended" }
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 10.dp)
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = getString("choose_specific_days"),
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        // The hint names the state it applies to. It used to
                                                        // read "Off: the recommended week" in BOTH states, so
                                                        // the moment the user switched the specific days on,
                                                        // the line under the switch described the state they
                                                        // had just left - and "off" was simply untrue.
                                                        Text(
                                                            text = getString(
                                                                if (assistedSpecificDays) "premade_week_hint_on"
                                                                else "premade_week_hint"
                                                            ),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Switch(
                                                        checked = assistedSpecificDays,
                                                        onCheckedChange = { on ->
                                                            assistedSpecificDays = on
                                                            // Turning the switch OFF means
                                                            // "give me the recommended week
                                                            // for my frequency" — not "keep
                                                            // the days I picked by hand".
                                                            if (!on) {
                                                                val count = assistedSelectedDays.size.coerceIn(1, 7)
                                                                assistedSelectedDays.clear()
                                                                assistedSelectedDays.addAll(WorkoutTemplates.premadeWeekFor(count))
                                                            }
                                                        },
                                                        modifier = Modifier.semantics { testTag = "specific_days_switch" }
                                                    )
                                                }
                                                // 1x/week and 7x/week carry research-backed
                                                // warnings; the choice itself stays available.
                                                val warningKey = WorkoutTemplates.frequencyWarningKey(assistedSelectedDays.size)
                                                if (warningKey.isNotBlank()) {
                                                    Text(
                                                        text = getString(warningKey),
                                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 8.dp)
                                                    )
                                                }
                                                // 1b. WHAT EACH DAY IS - the other half of the
                                                // week. Sits right under the day picker
                                                // because it only makes sense against the
                                                // day count just chosen, and each option
                                                // spells out the run of days it produces so
                                                // the week can be counted against the days
                                                // picked above it.
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(
                                                        vertical = window.sectionSpacing
                                                    ),
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )
                                                // The title needs room to breathe before the
                                                // first card: it is a label for the block
                                                // below, not a caption on the divider.
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(
                                                        window.sectionSpacing / 2
                                                    )
                                                ) {
                                                    Text(
                                                        text = getString("split_style_title"),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    SplitStyleSelector(
                                                        selected = assistedSplitStyle,
                                                        daysPerWeek = assistedSelectedDays.size,
                                                        onSelect = { assistedSplitStyle = it }
                                                    )
                                                }
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

                                    // 3. WHAT TO TRAIN — the goal, not body parts.
                                    // Muscle-group picking moved under Advanced: the
                                    // generator distributes muscle groups itself from
                                    // the evidence-based split templates; beginners
                                    // picking muscles produces lopsided programs.
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OnboardingStepHeader(stepNumber = null, title = getString("choose_workout_goal"))
                                        WorkoutGoalBlock(
                                            selectedGoal = selectedWorkoutGoal,
                                            onGoalChange = { selectedWorkoutGoal = it },
                                            isDeveloper = isDeveloper
                                        )
                                    }

                                    // 4. HOW LONG — sets the per-session exercise budget
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OnboardingStepHeader(stepNumber = null, title = getString("choose_duration"))
                                        DurationSelectionBlock(assistedDuration) { assistedDuration = it }
                                    }

                                    // 5. HOW HARD — sets the target RPE
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
                                                // The spec removes muscle-group picking from the
                                                // default flow; power users keep it here.
                                                BodyPartsSelectionBlock(assistedBodyParts)
                                                PreferenceSection(getString("include_exercises"), allAvailableExercises, assistedIncludedExercises, MaterialTheme.colorScheme.primaryContainer)
                                                PreferenceSection(getString("exclude_exercises"), allAvailableExercises, assistedExcludedExercises, MaterialTheme.colorScheme.errorContainer)
                                            }
                                        }
                                    }

                                    // Warn about anything the chosen parts + equipment could not
                                    // actually cover, using the same build that will be saved.
                                    // The balance card judges the week against healthy
                                    // weekly ranges, scoped to the advanced tab's part
                                    // selection — which DEFAULTS to every part, so an
                                    // untouched selection checks every group.
                                    val buildIssues = weeklyBalanceIssues(
                                        WorkoutTemplates.generateEngineProgram(
                                            bodyParts = assistedBodyParts.toList(),
                                            selectedDays = assistedSelectedDays.toList(),
                                            intensity = assistedIntensity,
                                            workoutGoal = selectedWorkoutGoal,
                                            includedExercises = assistedIncludedExercises.toList(),
                                            excludedExercises = assistedExcludedExercises.toList(),
                                            equipment = buildEquipment,
                                            duration = assistedDuration,
                                            splitStyle = assistedSplitStyle
                                        ).exercises,
                                        balanceGroupsForParts(assistedBodyParts)
                                    )

                                    AnimatedContent(
                                        targetState = buildIssues,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500, delayMillis = 100)))
                                                .togetherWith(fadeOut(animationSpec = tween(200)))
                                        },
                                        label = "BuildWarning"
                                    ) { issues ->
                                        MissingBodyPartsWarningCard(issues = issues)
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
                                                            // Every gap here separates two pieces of text, so it
                                                            // comes from the window and not from a fixed dp - see
                                                            // [WindowSize.inlineGap]. The intensity used to be
                                                            // prefixed with a leading space instead of a gap, which
                                                            // is nothing at all once the font has grown.
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                                                MuscleGroupBadge(muscleGroup = exercise.muscleGroup, exerciseName = exercise.name)
                                                                Spacer(modifier = Modifier.width(window.inlineGap))
                                                                Text(
                                                                    // The exercise's own unit, not its stored row's
                                                                    // wording - see [exerciseSetsSummaryLabel]. Jumping
                                                                    // Jacks' "8–12" row is a 90-second exercise.
                                                                    text = exerciseSetsSummaryLabel(
                                                                        exerciseName = exercise.name,
                                                                        targetSets = exercise.targetSets,
                                                                        storedTarget = exercise.targetReps,
                                                                    ),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                if (exercise.targetIntensity.isNotBlank()) {
                                                                    val rpeExpl = com.grafizum.repio.ui.components.getRpeRirExplanation(exercise.targetIntensity)
                                                                    val displayIntensity = if (rpeExpl.isNotBlank()) "${exercise.targetIntensity} ($rpeExpl)" else exercise.targetIntensity
                                                                    Spacer(modifier = Modifier.width(window.inlineGap))
                                                                    Text(
                                                                        text = "• $displayIntensity",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                                if (exercise.tempo.isNotBlank()) {
                                                                    Spacer(modifier = Modifier.width(window.inlineGap))
                                                                    Text(
                                                                        text = "• ${com.grafizum.repio.ui.components.getTempoLabel(exercise.tempo)}",
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
                                            targetState = weeklyBalanceIssues(
                                                customExercises.toList(),
                                                balanceGroupsForParts(assistedBodyParts)
                                            ),
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500, delayMillis = 100)))
                                                    .togetherWith(fadeOut(animationSpec = tween(200)))
                                            },
                                            label = "CustomWarning"
                                        ) { missing ->
                                            MissingBodyPartsWarningCard(issues = missing)
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
        // True only when the user came here to replace a plan they already had.
        // On the very first launch there is no level system to lose, so the
        // warning stays out of that confirmation entirely.
        val replacingExistingPlan by viewModel.isManualReentry.collectAsState()
        SaveConfirmationDialog(
            levelResetWarning = replacingExistingPlan,
            onDismiss = { showFinalSaveConfirmation = false },
            onConfirm = {
                saveSelectedBuild()
                onComplete()
                showFinalSaveConfirmation = false
            }
        )
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
            // In the builder the chips read the selection the user is still
            // editing; before they touch equipment it defaults to the saved
            // build's kit so a no-barbell builder never sees barbell swaps.
            userEquipmentCategories = EquipmentFilterBlockCategories(buildEquipment)
                .ifEmpty { viewModel.userEquipmentCategories() },
            userGrantedTags = equipmentFilterBlockGrantedTags(buildEquipment, viewModel),
            onDismiss = { replacingExercise = null },
            onReplace = { name, muscleGroup ->
                // Drop the original and put the pick in its slot, keeping any demo
                // link already recorded for that exercise. The new row is
                // parameterized from the goal/difficulty this builder is still
                // editing, not the old exercise's sets/reps/rest.
                val knownUrl = customExercises
                    .firstOrNull { it.name == name && it.videoUrl.isNotBlank() }
                    ?.videoUrl ?: ""
                customExercises.removeAll {
                    it.dayOfWeek == exercise.dayOfWeek && it.name == exercise.name
                }
                customExercises.add(
                    anchorIndex,
                    WorkoutTemplates.parameterizeReplacement(
                        original = exercise,
                        newName = ExerciseLocalization.canonicalFor(name) ?: name,
                        newMuscleGroup = muscleGroup,
                        workoutGoal = selectedWorkoutGoal,
                        intensity = assistedIntensity.ifBlank { "Medium" },
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
/** The display label for one of the five canonical goal keys. */
@Composable
private fun goalLabelFor(goalKey: String): String = when (goalKey) {
    "goal_bodybuilding" -> getString("goal_bodybuilding")
    "goal_strength" -> getString("goal_strength")
    "goal_calisthenics" -> getString("goal_calisthenics")
    "goal_endurance" -> getString("goal_endurance")
    "goal_general" -> getString("goal_general")
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutGoalBlock(
    selectedGoal: String,
    onGoalChange: (String) -> Unit,
    isDeveloper: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    // The five canonical goals; legacy keys display under their canonical label.
    val goalKeys = WorkoutTemplates.allWorkoutGoals
    val selectedGoalLabel = goalLabelFor(WorkoutTemplates.canonicalGoal(selectedGoal))

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
                // The goal labels now carry the technical term in parentheses
                // ("Muscle Building (Hypertrophy)"), which does not fit one line
                // of a phone's width. An OutlinedTextField is single-line by
                // default and silently clips, so the chosen goal would read
                // "Muscle Building (Hypertr…". Two lines is enough for the
                // longest label at bodyMedium and still leaves the label
                // ("Workout Goal") and the trailing chevron their room.
                maxLines = 2,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                goalKeys.forEach { goalKey ->
                    val label = goalLabelFor(goalKey)
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

        val impact = when (WorkoutTemplates.canonicalGoal(selectedGoal)) {
            "goal_strength" -> getString("goal_strength_desc")
            "goal_bodybuilding" -> getString("goal_bodybuilding_desc")
            "goal_calisthenics" -> getString("goal_calisthenics_desc")
            "goal_endurance" -> getString("goal_endurance_desc")
            "goal_general" -> getString("goal_general_desc")
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

/**
 * The persisted build selection reduced to coarse categories ("Barbell",
 * "Dumbbell", …) — empty when it means "every category". The replace dialog
 * uses it to start its chips on the kit the builder is configured for.
 */
fun EquipmentFilterBlockCategories(selectedEquipment: List<String>): List<String> =
    ExerciseEquipmentTags.coarseCategoriesFor(selectedEquipment)

/**
 * The builder's selection as the pickers' equipment gate should read it.
 *
 * The builder owns the fine tags, so this is a direct expansion — no coarse
 * round trip, which would drop the chair/bench/bar grants the coarse chip
 * vocabulary cannot express. Null = the user has not picked anything yet, which
 * the gate reads as unrestricted.
 */
@Composable
private fun equipmentFilterBlockGrantedTags(
    selectedEquipment: List<String>,
    viewModel: com.grafizum.repio.ui.viewmodel.WorkoutViewModel
): Set<String>? =
    ExerciseEquipmentTags.allowedTagsFor(selectedEquipment)
        .takeIf { selectedEquipment.isNotEmpty() }
        ?: viewModel.userGrantedEquipmentTags()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentFilterBlock(
    selectedEquipment: List<String>,
    onEquipmentChange: (List<String>) -> Unit
) {
    // State model: [presetId, fineTag, fineTag, …]. The first element is one
    // of the ExerciseEquipmentTags presets (or PRESET_CUSTOM); everything
    // after it are fine-grained tags. Legacy persisted values ("Dumbbell",
    // "Bands", …) render through allowedTagsFor and normalize on first edit.
    val activePreset = selectedEquipment.firstOrNull {
        ExerciseEquipmentTags.isPreset(it) && it != ExerciseEquipmentTags.PRESET_CUSTOM
    }
    // The stored vocabulary is FINER than the tile grid: eight selectorized
    // stations sit behind the single "Machines" tile. They must NOT be folded
    // away (the stations gate different exercises from MACHINE_OTHER), but
    // they must not survive a tap either - see [ExerciseEquipmentTags.tagsCoveredByTile].
    val customTags: Set<String> = if (activePreset != null) {
        selectedEquipment.drop(1).filter { it in ExerciseEquipmentTags.ALL_TAGS }.toSet()
    } else {
        ExerciseEquipmentTags.allowedTagsFor(selectedEquipment)
    }
    // Checking a box while a preset is active carries the preset's tags into
    // Custom; unchecking everything in Custom honestly means "no equipment".
    fun toggleTag(tag: String) {
        val base = (if (activePreset != null) ExerciseEquipmentTags.presetTags(activePreset) else customTags)
            .toMutableSet()
        // Unticking a tile removes everything it stands for, not just the tile's
        // own tag: the eight stations have no tile of their own, so "remove the
        // tag the user can see" is the only way a machine the user no longer
        // owns ever leaves the kit. This is what stopped the owner removing the
        // machines he had never meant to grant.
        if (tag in base) {
            // Unticking removes the WHOLE family, not just the tile's own tag:
            // the eight stations have no tile of their own, so "remove the tag
            // the user can see" is the only way a machine the user no longer
            // owns ever leaves the kit. This is what stopped the owner removing
            // machines he had never meant to grant in the first place.
            base.removeAll(ExerciseEquipmentTags.tagsCoveredByTile(tag))
        } else {
            // Ticking ADDS what the tile stands for. Every tile now stands for its
            // own kit and nothing beside it - a rack no longer drags the barbell
            // tile along, which is the owner's ruling that the two are separate
            // switches - while still going through tagsGrantedByTicking rather than
            // the family, so ticking "Machines" cannot grant a leg press the user
            // never ticked.
            base.addAll(ExerciseEquipmentTags.tagsGrantedByTicking(tag))
        }
        val ordered = base.sortedBy { ExerciseEquipmentTags.ALL_TAGS.indexOf(it) }
        onEquipmentChange(listOf(ExerciseEquipmentTags.PRESET_CUSTOM) + ordered)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ---- Fine equipment as an ICON TILE GRID, not a checkbox list ----
        // The old sixteen rows of sentences forced reading before acting; the
        // grid turns the same data into a wall of equipment the user glances
        // at. The icon carries the shape, the short label only disambiguates.
        // A preset's tags show pre-checked (state, not just the chip), so the
        // grid doubles as the visual summary of what the preset grants.
        val presetGranted: Set<String> =
            if (activePreset != null) ExerciseEquipmentTags.presetTags(activePreset)
            else customTags

        EquipmentTileSection(
            title = getString("home_equipment_section"),
            tags = ExerciseEquipmentTags.HOME_EQUIPMENT_TAGS,
            isChecked = { tag -> presetGranted.any { it in ExerciseEquipmentTags.tagsCoveredByTile(tag) } },
            onToggle = { tag -> toggleTag(tag) }
        )

        EquipmentTileSection(
            title = getString("gym_equipment_section"),
            tags = ExerciseEquipmentTags.GYM_EQUIPMENT_TAGS,
            isChecked = { tag -> presetGranted.any { it in ExerciseEquipmentTags.tagsCoveredByTile(tag) } },
            onToggle = { tag -> toggleTag(tag) }
        )
    }
}

/**
 * One labelled section of the icon-tile grid: a small section header and the
 * tiles flowing beneath it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentTileSection(
    title: String,
    tags: List<String>,
    isChecked: (String) -> Boolean,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            tags.forEach { tag ->
                EquipmentIconTile(
                    tag = tag,
                    checked = isChecked(tag),
                    onToggle = { onToggle(tag) }
                )
            }
        }
    }
}

/**
 * One equipment tile of the grid: the tag's icon over its short label, the
 * whole tile the tap target. Selected tiles fill with the primary tint and
 * carry a primary rim — state reads from across the room, no checkbox needed.
 * A tile is at least 96dp wide so the grid lands at three tiles per row on a
 * 360dp phone and widens naturally on tablets (weight-like fill via weight on
 * the parent is not available in a FlowRow, so the min-width does the work).
 */
@Composable
private fun EquipmentIconTile(
    tag: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val isDeveloper = LocalDeveloperSkin.current
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        color = when {
            checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
                     1.dp,
                     if (checked) MaterialTheme.colorScheme.primary
                     else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                 ),
        modifier = Modifier.widthIn(min = 96.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            EquipmentIconImage(
                icon = equipmentTagIcon(tag),
                contentDescription = null,
                tint = when {
                    checked -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = getString(equipmentTagShortLabelKey(tag)),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = when {
                    checked -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
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
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) { Text(getString("add_pref"), style = MaterialTheme.typography.labelSmall) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            list.forEach { ex ->
                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(8.dp),
                    border = null,
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
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
                        shape = RoundedCornerShape(12.dp),
                        color = backgroundColor,
                        contentColor = contentColor,
                        border = if (isDeveloper && expanded) BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                                 else if (!expanded) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) 
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
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = if (isDeveloper && isSel) FilterChipDefaults.filterChipBorder(enabled = true, selected = true, selectedBorderColor = MaterialTheme.colorScheme.surface, selectedBorderWidth = 2.dp) else FilterChipDefaults.filterChipBorder(enabled = true, selected = isSel)
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
 * The setup stores the picker's own strings as its keys ("setup_intensity"), so
 * the bridge to the engine's enum lives next to its two callers rather than
 * being repeated inline.
 */
private fun difficultyOf(intensity: String): com.grafizum.repio.data.ProgressionEngine.Difficulty =
    when (intensity) {
        "Easy" -> com.grafizum.repio.data.ProgressionEngine.Difficulty.EASY
        "Hard" -> com.grafizum.repio.data.ProgressionEngine.Difficulty.HARD
        else -> com.grafizum.repio.data.ProgressionEngine.Difficulty.MEDIUM
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
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
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
                shape = RoundedCornerShape(7.dp),
                shadowElevation = 1.dp) {}

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
                            // What this choice actually sets: the rung each
                            // exercise starts on, not an RPE target.
                            Text(
                                text = getString(
                                    "intensity_level_value",
                                    com.grafizum.repio.data.ProgressionEngine.typicalStartingLevel(difficultyOf(level))
                                ),
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
                            text = getString(
                                "intensity_level_value",
                                com.grafizum.repio.data.ProgressionEngine.typicalStartingLevel(difficultyOf(selected))
                            ),
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

/**
 * The session length a build is budgeted for. The choice is the per-day
 * exercise window the generator trims/pads into: Quick = 6-8 (compound
 * focus), Standard = 8-10 (balanced), Full = 10-12 (full volume). Mirrors
 * the intensity block's segmented picker.
 */
@Composable
private fun DurationSelectionBlock(selected: String, onSelect: (String) -> Unit) {
    val options = WorkoutTemplates.DURATION_OPTIONS
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val isDeveloper = LocalDeveloperSkin.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selected == option
                // The chip answers the block's own header ("Sitzungslänge"), so it shows
                // MINUTES - the exercise window next to it is what the description below
                // is written in and what the generator enforces. Printing the exercise
                // count with the minute unit made every option claim to be shorter than
                // its own warm-up (measured on device 2026-10-08).
                val minutes = WorkoutTemplates.durationMinutesFor(option)
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "durationContentColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(option) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = getString("duration_${option.substringAfter("_")}_label"),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = contentColor
                        )
                        Text(
                            text = getString("duration_minutes", minutes.first, minutes.last),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = contentColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // What this choice actually sets: the per-session exercise budget.
        val description = when(selected) {
            WorkoutTemplates.DURATION_QUICK -> getString("duration_quick_desc")
            WorkoutTemplates.DURATION_FULL -> getString("duration_full_desc")
            else -> getString("duration_standard_desc")
        }

        AnimatedContent(
            targetState = description,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.92f, animationSpec = tween(500, delayMillis = 100)))
                    .togetherWith(fadeOut(animationSpec = tween(200)))
            },
            label = "durationDesc"
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
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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

@Composable
private fun SaveConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    levelResetWarning: Boolean = false
) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusManager.clearFocus() },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(getString("confirm_new_program_message"))
                if (levelResetWarning) {
                    // Saving replaces the routine, and replacing the routine
                    // clears every exercise's stored level and stage (see
                    // WorkoutViewModel.clearExerciseMemories). The logged sets
                    // survive, so the user only has to be told the levels restart.
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = getString("level_reset_warning_title"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = getString("level_reset_warning_body"),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
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

/**
 * "How many days a week" as a menu of the frequencies the app supports.
 *
 * Owner prompt 2026-10-08: *"i want to add a simple menu having the user select between 2 to
 * 6 days a week for a workout"* plus *"if the user chooses 1 or 7 days it will show a small
 * label ... saying (not recommended)"*. Both of those frequencies therefore stay SELECTABLE -
 * a menu that refuses them would leave a user who trains every day with no way to say so, and
 * the honest answer is to mark the choice rather than hide it.
 *
 * Selecting a count writes [WorkoutTemplates.premadeWeekFor] straight into the week, so the
 * user sees the actual shape the number produces (which days train, and where the pauses
 * fall) instead of a bare number.
 *
 * REDESIGNED, owner prompt 2026-10-09: *"seeing the days of week like that with the numbers on
 * top just looks very cheap and basic i want a different design"*. The seven separate numbered
 * tiles are gone. The frequencies now live in ONE segmented track that uses the same sliding
 * thumb as the build-mode switch at the top of this screen, so this control reads as part of
 * the app instead of as a row of boxes; the number is a label on the selected segment rather
 * than a tile of its own. The week the numbers produce is drawn UNDER the track as seven silent
 * blocks (see [WeekStrip]), and the weekday chips themselves stay off the screen until the user
 * opens the specific-days switch - the owner's *"show the days of the week ... only if the user
 * distinctively opens it"*.
 */
@Composable
private fun DaysPerWeekMenu(
    daysPerWeek: Int,
    onSelect: (Int) -> Unit
) {
    val segmentCount = 7
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        val segmentWidth = maxWidth / segmentCount
        val inset = 3.dp
        // The thumb is the chosen frequency. It rides the same spring the build-mode switch
        // uses, because the two controls sit on one screen and should move alike.
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * (daysPerWeek - 1) + inset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "daysPerWeekThumb"
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = inset)
                .width(segmentWidth - inset * 2)
                .height(42.dp - inset * 2)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp))
        )
        Row(modifier = Modifier.fillMaxSize()) {
            (1..segmentCount).forEach { count ->
                val isSelected = count == daysPerWeek
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(count) }
                        .semantics { testTag = "days_per_week_$count" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The week as a picture: one block per weekday, filled where the week trains.
 *
 * It keeps the half of the old numbered menu that was worth having - WHICH days train and
 * where the pauses fall - without printing the weekday names, which the owner asked to keep
 * off the screen until the specific-days switch is opened.
 */
@Composable
private fun WeekStrip(selectedDays: List<String>) {
    val week = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        week.forEach { day ->
            val trains = selectedDays.contains(day)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (trains) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
            )
        }
    }
}

@Composable
fun DaySelectionRow(
    selectedDays: List<String>,
    onDayToggle: (String) -> Unit,
    /**
     * False draws the row as the week the premade menu chose: still readable (the user must
     * be able to see where the pauses fall) but not editable, because the switch beside it
     * is what grants that. Defaults to true so the day pickers in the routine/editor screens
     * keep their old behaviour unchanged.
     */
    enabled: Boolean = true
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
            // Deliberately NOT `enabled = false` while the premade week is showing: Material's
            // disabled surface dims the whole row (and the project has already paid twice for
            // a control that "reads as disabled but is really just refused" - see
            // levelPillBlockedReason). The row keeps its real colours and the caller decides
            // what a tap means, which in the welcome screen is "open the specific-days
            // switch", so the tap explains itself instead of doing nothing.
            Surface(
                onClick = { onDayToggle(day) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                border = if (isSelected) {
                             BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                         } else BorderStroke(
                             1.dp,
                             MaterialTheme.colorScheme.outlineVariant.copy(
                                 // The premade week is an ANSWER ("this is your week"), the
                                 // editable row is a QUESTION. A fainter rim on the
                                 // unchosen days says which one the user is looking at
                                 // without dimming the week itself.
                                 alpha = if (enabled) 1f else 0.6f
                             )
                         ),
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
        "chest_title" -> "Chest"
        "back_title" -> "Back"
        "shoulders_title" -> "Shoulders"
        "arms_title" -> "Arms"
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
    onConfirm: (RoutineExerciseEntity) -> Unit,
    /**
     * The saved kit as coarse categories; the recommendation filters start on
     * it so a no-barbell setup never sees barbell suggestions by default.
     */
    userEquipmentCategories: List<String> = emptyList(),
    /**
     * The user's granted FINE tags when the host has them. The coarse list above
     * has no category for a chair, a bench or a pull-up bar, so re-expanding it
     * would silently drop grants the user actually ticked.
     */
    userGrantedTags: Set<String>? = null,
    /**
     * Resolves the level facts for the exercise this form is about RIGHT NOW,
     * keyed on its name.
     *
     * This is the same resolver contract the routine editor uses, and it is here
     * for the same reason: the welcome screen's add dialog is a SECOND editor, and
     * without it an exercise the user adds during setup still opened on the plain
     * custom boxes while the level system ran underneath it. Keyed on the name
     * because the name is typed inside the dialog - the host has no row to hand in.
     */
    levelDataFor: (@Composable (String, Int) -> EditorLevelData)? = null,
    /** Called with the exercise's name and the ABSOLUTE level the user picked. */
    onLevelChange: ((String, Int) -> Unit)? = null,
    /** A Roman numeral was picked: (name, level, stage), applied as ONE write. */
    onStageChange: ((String, Int, Int) -> Unit)? = null,
    /** Opens the full level ladder for the named exercise. */
    onOpenLevelLadder: ((String) -> Unit)? = null
) {
    var dayOfWeek by remember { mutableStateOf(exercise?.dayOfWeek ?: initialDay) }
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "Chest") }
    var targetSets by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "") }
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
    var targetIntensityStr by remember { mutableStateOf(exercise?.targetIntensity ?: "") }
    var restSecondsStr by remember { mutableStateOf(exercise?.restSeconds?.toString() ?: "60") }
    // The time toggle opens in the unit the EXERCISE measures in, not in whatever
    // its stored row says - see WorkoutTemplates.isExerciseTimeBased.
    var isTimeBased by remember { mutableStateOf(WorkoutTemplates.isExerciseTimeBased(exercise?.name.orEmpty())) }
    
    var targetRepsValue by remember { mutableStateOf(if (!isTimeBased) exercise?.targetReps ?: "" else "8-12") }
    var repsManuallyEdited by remember { mutableStateOf(exercise != null) }
    // See WorkoutTemplates.secondsInStoredTarget: a pre-level-system timed row
    // stores a rep range, which must not become the seconds field's value.
    var targetTimeValue by remember { mutableStateOf(if (isTimeBased) WorkoutTemplates.secondsInStoredTarget(exercise?.targetReps)?.toString() ?: "30" else "30") }
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

    // The muscle the NAME SUGGESTIONS are narrowed to, or null for every muscle.
    //
    // The suggestion filter popup was removed (dashboard task "The exercise
    // suggestions tab is still broken. Remove it completely") and what it left
    // behind was the one gate that is not a convenience: the user's own kit
    // (matching ExerciseEditDialog), because suggestions must never offer bar-less
    // moves to a dumbbells+chair setup.
    //
    // The muscle filter is back on the OWNER's instruction (2026-10-09: "allow the
    // user to filter which muscle too for when replacing an exercise"), and it is
    // deliberately the same compact row the replace dialog uses, so the two pickers
    // filter on the same vocabulary with the same control. It starts on ANY rather
    // than on the exercise's own muscle: this dialog is where a custom exercise is
    // NAMED, and someone renaming "Superman" to a row or a squat would be fighting a
    // filter they never asked for. The kit gate above stays ON either way.
    var selectedMuscleGroup by remember(exercise?.id) { mutableStateOf<String?>(null) }

    val filteredExercises = remember(
        allAvailableExercises, userEquipmentCategories, userGrantedTags, selectedMuscleGroup
    ) {
        com.grafizum.repio.ui.components.filteredExercisesForPickers(
            allAvailableExercises, selectedMuscleGroup, userEquipmentCategories,
            // Fine-tag gate matching ExerciseEditDialog: the coarse Bodyweight
            // chip alone offered bar-less moves to a dumbbells+chair setup.
            // Preferred over re-expanding the coarse list, which drops the
            // furniture and bar grants the chip vocabulary cannot express.
            WorkoutTemplates.pickerGrantedTags(userGrantedTags, userEquipmentCategories)
        )
    }

    // WHICH EXERCISE THIS FORM IS ABOUT, and therefore whose levels it shows. An
    // edit is about the row it was opened on; an add is about the name in the
    // search field, which only this dialog can see. Both resolve through the same
    // host resolver as the routine editor, so "add here" and "add in the workout"
    // are one code path with one definition of the level facts.
    val levelName = exercise?.name
        ?: ExerciseLocalization.canonicalFor(name.trim())
        ?: name.trim()
    var levelPageStart by remember(levelName) { mutableStateOf(0) }
    val levelData = levelDataFor?.invoke(levelName, levelPageStart) ?: EditorLevelData.NONE
    val levelControls = levelEditorControls(
        levelsManaged = levelData.levelsManaged,
        currentLevel = levelData.level,
        maxLevel = levelData.maxLevel,
        canWriteLevel = onLevelChange != null
    )
    val levelMode = levelControls.showLevelRow &&
        levelData.level != null &&
        onLevelChange != null
    // The rung and stage the list draws as chosen, seeded from the host and then
    // owned locally: a tap has to move the highlight and open the row in the SAME
    // frame. Keyed on the name so picking another exercise re-seeds instead of
    // carrying the previous one's level.
    var selectedLevel by remember(levelName) { mutableStateOf(levelData.level) }
    var selectedStage by remember(levelName) { mutableStateOf(levelData.stage) }

    AppDialog(
        onDismissRequest = onDismiss,
        title = getString(if (exercise == null) "add_custom_exercise" else "edit_exercise"),
        onTitleTap = { focusManager.clearFocus() },
        confirmLabel = getString(if (exercise == null) "add" else "save"),
        // In level mode the reps box is not on screen at all, so it cannot be the
        // thing that gates Save - the level list is what the user chose from. The
        // stored range is filled in on save from the exercise's recommendation
        // instead (see `finalReps`), because a blank range would clamp the level's
        // own prescription away at the next load.
        confirmEnabled = name.isNotBlank() &&
            (levelMode || (if (isTimeBased) targetTimeValue else targetRepsValue).isNotBlank()),
        onConfirm = {
            focusManager.clearFocus()
            val finalReps = if (isTimeBased) {
                val digits = targetTimeValue.filter { it.isDigit() }
                if (digits.isNotBlank()) "$digits sec" else "30 sec"
            } else {
                val typed = WorkoutTemplates.enforceRepRange(targetRepsValue.trim())
                when {
                    typed.isNotBlank() -> typed
                    // Level mode: an empty box means "the level owns this", not
                    // "the user cleared it". Store the range the exercise
                    // recommends so the level's prescription is not clamped away.
                    levelMode -> exercise?.targetReps?.takeIf { it.isNotBlank() }
                        ?: WorkoutTemplates.enforceRepRange(
                            WorkoutTemplates.getRecommendedRepsForExercise(levelName)
                        )
                    else -> ""
                }
            }
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
                targetWeightKg = loadInputToKg(name, targetWeightValue, weightUnit) ?: 0.0,
                restSeconds = restSecondsStr.toIntOrNull() ?: 60, 
                videoUrl = videoUrl, 
                orderIndex = exercise?.orderIndex ?: 99,
                // Kept from the generated build; tempo is not typed in by hand.
                tempo = exercise?.tempo ?: ""
            ))
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
                // The same compact muscle filter as the replace dialog: one line, with the
                // body parts and the precise muscles one tap away. See
                // [com.grafizum.repio.ui.components.MuscleGroupFilterCollapsible].
                com.grafizum.repio.ui.components.MuscleGroupFilterCollapsible(
                    selectedMuscleGroup = selectedMuscleGroup,
                    onSelect = { selectedMuscleGroup = it }
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
                    allExercises = filteredExercises,
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

                // THE LEVEL LIST, in level mode. It stands where the locked sets /
                // reps / weight boxes used to, exactly as in the routine editor -
                // otherwise an exercise added during setup shows the custom input
                // boxes while the level system runs underneath it (owner, 2026-10-09).
                if (levelMode) {
                    LevelPickerCard(
                        options = levelData.options,
                        pageRange = levelData.pageRange,
                        totalLevels = levelData.maxLevel,
                        onPreviousPage = levelData.previousPageStart
                            ?.let { start -> { levelPageStart = start } },
                        onNextPage = levelData.nextPageStart
                            ?.let { start -> { levelPageStart = start } },
                        selectedLevel = selectedLevel,
                        onSelect = { level ->
                            selectedLevel = level
                            selectedStage = 1
                            onLevelChange?.invoke(levelName, level)
                        },
                        onOpenLadder = { onOpenLevelLadder?.invoke(levelName) },
                        selectedStage = selectedStage,
                        onSelectStage = { level, stage ->
                            selectedLevel = level
                            selectedStage = stage
                            onStageChange?.invoke(levelName, level, stage)
                        }
                    )
                    if (levelData.caption != null) {
                        Text(
                            text = levelData.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // THE LEVEL-OWNED VALUES, IN CUSTOM MODE ONLY. When the level
                // system prescribes the exercise these fields are not shown at
                // all - the level list above IS their editor, and two editors for
                // one prescription is the duplication this change removes.
                if (!levelMode) {
                    RepsTimeToggle(
                        isTimeBased = isTimeBased,
                        onToggle = { newValue ->
                            isTimeBased = newValue
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Soft guidance only — see ExerciseEditDialog. Nothing rep-related
                    // blocks saving; a min>max range just gets a field-level hint.
                    val repsError = targetRepsValue.isNotEmpty() && !isRepsValid
                    val outsideRecommended = remember(targetRepsValue, name) {
                        name.isNotBlank() && targetRepsValue.isNotBlank() && !isTimeBased &&
                            WorkoutTemplates.isOutsideRecommendedRepRange(name, targetRepsValue)
                    }

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
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) targetWeightValue = it },
                            // The same three answers the routine editor gives, and
                            // for the same reason: a selectorized stack counts pins
                            // and a band counts tension, so "kg" here would ask the
                            // user for a number that does not exist on their machine.
                            label = loadFieldLabel(name),
                            placeholder = loadFieldPlaceholder(name),
                            suffix = { Text(loadFieldSuffix(name), fontSize = 12.sp) },
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
    }

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
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    border = null,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        if (isCollapsible) {
            Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
