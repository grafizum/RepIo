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
import com.grafizum.repio.data.RoutineDayEntity
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplate
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.MissingBodyPartsWarningCard
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.components.RepsTimeToggle
import com.grafizum.repio.ui.components.RepsRangeField
import com.grafizum.repio.ui.components.SearchableExerciseField
import com.grafizum.repio.ui.components.ThemedToast
import com.grafizum.repio.ui.components.getExerciseDetails
import com.grafizum.repio.ui.components.getMissingMuscleGroups
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.components.translateMuscleGroup
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getString
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Premade, 1 = Assisted, 2 = Custom
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

    val allAvailableExercises by viewModel.allAvailableExercises.collectAsState()

    var selectedTemplate by remember { mutableStateOf(WorkoutTemplates.template5DaySplit) }
    val templateSelectedDays = remember { mutableStateListOf<String>() }
    var selectedDayInCustom by remember { mutableStateOf("MONDAY") }

    // Sync template days when template changes
    LaunchedEffect(selectedTemplate) {
        templateSelectedDays.clear()
        templateSelectedDays.addAll(
            selectedTemplate.dayTemplates
                .filter { !it.isRestDay }
                .map { it.dayOfWeek }
        )
    }

    var previewTemplateModal by remember { mutableStateOf<WorkoutTemplate?>(null) }
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
    val themeOption by viewModel.themeOption.collectAsState()

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
                .padding(horizontal = 24.dp)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

                                Spacer(modifier = Modifier.height(16.dp))

                                // GOAL SELECTION
                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Adjust,
                                            contentDescription = null,
                                            tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = getString("workout_goal"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    var expanded by remember { mutableStateOf(false) }
                                    val selectedGoalLabel = when(selectedWorkoutGoal) {
                                        "goal_bodybuilding" -> getString("goal_bodybuilding")
                                        "goal_strength" -> getString("goal_strength")
                                        "goal_calisthenics" -> getString("goal_calisthenics")
                                        "goal_functional" -> getString("goal_functional")
                                        "goal_olympic" -> getString("goal_olympic")
                                        "goal_powerbuilding" -> getString("goal_powerbuilding")
                                        "goal_crossfit" -> getString("goal_crossfit")
                                        "goal_athletic" -> getString("goal_athletic")
                                        else -> getString("goal_bodybuilding")
                                    }

                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = !expanded },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = selectedGoalLabel,
                                            onValueChange = {},
                                            readOnly = true,
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
                                            listOf(
                                                "goal_bodybuilding", "goal_strength", "goal_calisthenics",
                                                "goal_functional", "goal_olympic", "goal_powerbuilding",
                                                "goal_crossfit", "goal_athletic"
                                            ).forEach { goalKey ->
                                                val label = when(goalKey) {
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
                                                        selectedWorkoutGoal = goalKey
                                                        expanded = false
                                                    },
                                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                )
                                            }
                                        }
                                    }

                                    // GOAL IMPACT SUMMARY
                                    val goalImpactText = when(selectedWorkoutGoal) {
                                        "goal_strength", "goal_olympic" -> getString("goal_strength_desc")
                                        "goal_bodybuilding", "goal_powerbuilding" -> getString("goal_bodybuilding_desc")
                                        "goal_calisthenics", "goal_crossfit" -> getString("goal_calisthenics_desc")
                                        "goal_functional", "goal_athletic" -> getString("goal_functional_desc")
                                        else -> ""
                                    }
                                    
                                    if (goalImpactText.isNotBlank()) {
                                        AnimatedContent(
                                            targetState = goalImpactText,
                                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                                            label = "GoalImpact"
                                        ) { text ->
                                            Text(
                                                text = text,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .pointerInput(selectedTab) {
                                    var totalDrag = 0f
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (totalDrag > 50) {
                                                if (selectedTab < 2) selectedTab++
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
                            val tabWidth = this.maxWidth / 3
                            val density = LocalDensity.current
                            val swipeThreshold = with(density) { 40.dp.toPx() }

                            val indicatorPosition by animateDpAsState(
                                targetValue = tabWidth * selectedTab,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "routineTabIndicator"
                            )

                            // Sliding Indicator
                            Surface(
                                modifier = Modifier
                                    .width(tabWidth)
                                    .height(44.dp)
                                    .graphicsLayer {
                                        translationX = with(density) { indicatorPosition.toPx() }
                                    },
                                color = MaterialTheme.colorScheme.primary,
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                                shadowElevation = if (isDeveloper) 0.dp else 1.dp,
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
                                                    if (selectedTab < 2) selectedTab++
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
                                    0 to (Icons.Default.FitnessCenter to "premade_templates"),
                                    1 to (Icons.Default.Info to "assisted_build"),
                                    2 to (Icons.Default.Edit to "build_custom")
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
                                    // PREMADE CONTENT
                                    WorkoutTemplates.allTemplates.forEach { template ->
                                        val isSelected = selectedTemplate.id == template.id
                                        Card(
                                            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            ),
                                            border = if (isDeveloper) BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.Black else Color.White)
                                                     else if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                                     else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    focusManager.clearFocus()
                                                    selectedTemplate = template 
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        RadioButton(
                                                            selected = isSelected,
                                                            onClick = { selectedTemplate = template },
                                                            colors = RadioButtonDefaults.colors(
                                                                selectedColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                                                                unselectedColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.outline
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Column {
                                                            Text(
                                                                text = getString(template.nameKey),
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                            )
                                                            Text(
                                                                text = getString(template.daysCountKey, template.daysCountArgs.first),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
                                                        border = if (isDeveloper) BorderStroke(1.dp, if (isSelected) Color.Black else Color.White) else null
                                                    ) {
                                                        Text(
                                                            text = getString(template.badgeKey),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = getString(template.descriptionKey),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )

                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                                ) {
                                                    TextButton(
                                                        onClick = { previewTemplateModal = template },
                                                        contentPadding = PaddingValues(0.dp),
                                                        colors = ButtonDefaults.textButtonColors(
                                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                                        )
                                                    ) {
                                                        Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(getString("preview_breakdown"), style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    if (isSelected) {
                                                        Icon(Icons.Default.CheckCircle, null, tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Card(
                                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            DaySelectionRow(
                                                selectedDays = templateSelectedDays,
                                                onDayToggle = { day ->
                                                    if (templateSelectedDays.contains(day)) templateSelectedDays.remove(day)
                                                    else templateSelectedDays.add(day)
                                                }
                                            )
                                            if (templateSelectedDays.size != selectedTemplate.daysCountArgs.first) {
                                                Text(
                                                    text = "Select exactly ${selectedTemplate.daysCountArgs.first} days",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }

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
                                                PreferenceSection(getString("include_exercises"), allAvailableExercises, assistedIncludedExercises, MaterialTheme.colorScheme.primaryContainer)
                                                PreferenceSection(getString("exclude_exercises"), allAvailableExercises, assistedExcludedExercises, MaterialTheme.colorScheme.errorContainer)
                                            }
                                        }
                                    }

                                    AnimatedContent(
                                        targetState = getMissingMuscleGroups(selectedTemplate.exercises),
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500, delayMillis = 100)))
                                                .togetherWith(fadeOut(animationSpec = tween(200)))
                                        },
                                        label = "PremadeWarning"
                                    ) { missing ->
                                        MissingBodyPartsWarningCard(missingParts = missing)
                                    }
                                }
                                1 -> {
                                    // ASSISTED CONTENT
                                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            OnboardingStepHeader(stepNumber = 1, title = getString("choose_body_parts"))
                                            BodyPartsSelectionBlock(assistedBodyParts)
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            OnboardingStepHeader(stepNumber = 2, title = getString("choose_days"))
                                            DaySelectionRow(
                                                selectedDays = assistedSelectedDays,
                                                onDayToggle = { day ->
                                                    if (assistedSelectedDays.contains(day)) {
                                                        if (assistedSelectedDays.size > 1) assistedSelectedDays.remove(day)
                                                    } else assistedSelectedDays.add(day)
                                                }
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            OnboardingStepHeader(stepNumber = 3, title = getString("choose_intensity"))
                                            IntensitySelectionBlock(assistedIntensity) { assistedIntensity = it }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            OnboardingStepHeader(
                                                stepNumber = 4,
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
                                                    PreferenceSection(getString("include_exercises"), allAvailableExercises, assistedIncludedExercises, MaterialTheme.colorScheme.primaryContainer)
                                                    PreferenceSection(getString("exclude_exercises"), allAvailableExercises, assistedExcludedExercises, MaterialTheme.colorScheme.errorContainer)
                                                }
                                            }
                                        }

                                        val program = WorkoutTemplates.generateAssistedProgram(
                                            bodyParts = assistedBodyParts.toList(),
                                            selectedDays = assistedSelectedDays.toList(),
                                            intensity = assistedIntensity,
                                            workoutGoal = selectedWorkoutGoal,
                                            includedExercises = assistedIncludedExercises.toList(),
                                            excludedExercises = assistedExcludedExercises.toList()
                                        )

                                        val missing = getMissingMuscleGroups(program.exercises)
                                        
                                        AnimatedContent(
                                            targetState = missing,
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500, delayMillis = 100)))
                                                    .togetherWith(fadeOut(animationSpec = tween(200)))
                                            },
                                            label = "AssistedWarning"
                                        ) { m ->
                                            MissingBodyPartsWarningCard(
                                                missingParts = m
                                            )
                                        }
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
                                                                MuscleGroupBadge(exercise.muscleGroup)
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                    text = if (exercise.targetReps.contains("sec")) getString("sets_x_time", exercise.targetSets, exercise.targetReps)
                                                                    else getString("sets_x_reps", exercise.targetSets, exercise.targetReps),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                if (exercise.targetIntensity.isNotBlank()) {
                                                                    val rpeExpl = com.grafizum.repio.ui.components.getRpeRirExplanation(exercise.targetIntensity)
                                                                    val displayIntensity = if (rpeExpl.isNotBlank()) "${exercise.targetIntensity} ($rpeExpl)" else exercise.targetIntensity
                                                                    Text(
                                                                        text = " • $displayIntensity",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = MaterialTheme.colorScheme.primary
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
                            viewModel.setWorkoutGoal(selectedWorkoutGoal)
                            when(selectedTab) {
                                0 -> {
                                    val mapped = remapTemplateToDays(selectedTemplate, templateSelectedDays.toList())
                                    val mappedExercises = mapped.second.map { 
                                        var sets = it.targetSets
                                        if (assistedMinSets > 0) sets = kotlin.math.max(sets, assistedMinSets)
                                        if (assistedMaxSets > 0) sets = kotlin.math.min(sets, assistedMaxSets)
                                        
                                        it.copy(
                                            targetSets = sets,
                                            targetReps = WorkoutTemplates.getGoalReps(it.name, selectedWorkoutGoal, sets),
                                            restSeconds = WorkoutTemplates.getGoalRestSeconds(selectedWorkoutGoal, it.name)
                                        ) 
                                    }
                                    viewModel.completeFirstTimeSetup(
                                        days = mapped.first,
                                        exercises = mappedExercises,
                                        setupType = "PREMADE",
                                        selectedDays = templateSelectedDays.toList(),
                                        templateId = selectedTemplate.id,
                                        minSets = assistedMinSets,
                                        maxSets = assistedMaxSets
                                    )
                                }
                                1 -> {
                                    val program = WorkoutTemplates.generateAssistedProgram(
                                        bodyParts = assistedBodyParts.toList(),
                                        selectedDays = assistedSelectedDays.toList(),
                                        intensity = assistedIntensity,
                                        workoutGoal = selectedWorkoutGoal,
                                        includedExercises = assistedIncludedExercises.toList(),
                                        excludedExercises = assistedExcludedExercises.toList(),
                                        minSets = assistedMinSets,
                                        maxSets = assistedMaxSets
                                    )
                                    viewModel.completeFirstTimeSetup(
                                        days = program.days,
                                        exercises = program.exercises,
                                        setupType = "ASSISTED",
                                        bodyParts = assistedBodyParts.toList(),
                                        selectedDays = assistedSelectedDays.toList(),
                                        intensity = assistedIntensity,
                                        minSets = assistedMinSets,
                                        maxSets = assistedMaxSets
                                    )
                                }
                                else -> viewModel.completeFirstTimeSetup(
                                    days = customDays.toList(),
                                    exercises = customExercises.toList(),
                                    setupType = "CUSTOM"
                                )
                            }
                            onComplete()
                        }
                    },
                    enabled = when(selectedTab) {
                        0 -> templateSelectedDays.size == selectedTemplate.daysCountArgs.first
                        1 -> assistedSelectedDays.isNotEmpty()
                        else -> true
                    },
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
            viewModel.setWorkoutGoal(selectedWorkoutGoal)
            when(selectedTab) {
                0 -> {
                    val mapped = remapTemplateToDays(selectedTemplate, templateSelectedDays.toList())
                    val mappedExercises = mapped.second.map { 
                        var sets = it.targetSets
                        if (assistedMinSets > 0) sets = kotlin.math.max(sets, assistedMinSets)
                        if (assistedMaxSets > 0) sets = kotlin.math.min(sets, assistedMaxSets)
                        
                        it.copy(
                            targetSets = sets,
                            targetReps = WorkoutTemplates.getGoalReps(it.name, selectedWorkoutGoal, sets),
                            restSeconds = WorkoutTemplates.getGoalRestSeconds(selectedWorkoutGoal)
                        ) 
                    }
                    viewModel.completeFirstTimeSetup(
                        days = mapped.first,
                        exercises = mappedExercises,
                        setupType = "PREMADE",
                        selectedDays = templateSelectedDays.toList(),
                        templateId = selectedTemplate.id,
                        minSets = assistedMinSets,
                        maxSets = assistedMaxSets
                    )
                }
                1 -> {
                    val program = WorkoutTemplates.generateAssistedProgram(
                        bodyParts = assistedBodyParts.toList(),
                        selectedDays = assistedSelectedDays.toList(),
                        intensity = assistedIntensity,
                        workoutGoal = selectedWorkoutGoal,
                        includedExercises = assistedIncludedExercises.toList(),
                        excludedExercises = assistedExcludedExercises.toList(),
                        minSets = assistedMinSets,
                        maxSets = assistedMaxSets
                    )
                    viewModel.completeFirstTimeSetup(
                        days = program.days,
                        exercises = program.exercises,
                        setupType = "ASSISTED",
                        bodyParts = assistedBodyParts.toList(),
                        selectedDays = assistedSelectedDays.toList(),
                        intensity = assistedIntensity,
                        minSets = assistedMinSets,
                        maxSets = assistedMaxSets
                    )
                }
                else -> viewModel.completeFirstTimeSetup(
                    days = customDays.toList(),
                    exercises = customExercises.toList(),
                    setupType = "CUSTOM"
                )
            }
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
                    Text(getString("confirm_delete_multiple_msg", customSelectedExercises.count()))
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

    replacingExercise?.let { exercise ->
        AlternativeExercisesDialog(
            exercise = exercise,
            onDismiss = { replacingExercise = null },
            onSelect = { altName ->
                val index = customExercises.indexOf(exercise)
                if (index != -1) {
                    val existing = customExercises.find { it.name == altName && it.videoUrl.isNotBlank() }
                    customExercises[index] = exercise.copy(name = altName, videoUrl = existing?.videoUrl ?: "")
                }
                replacingExercise = null
            }
        )
    }

    previewTemplateModal?.let { template ->
        TemplatePreviewDialog(
            template = template,
            goal = selectedWorkoutGoal,
            minSets = assistedMinSets,
            maxSets = assistedMaxSets,
            onDismiss = { previewTemplateModal = null }
        )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text(
                            text = getString("intensity_${level.lowercase()}"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = contentColor
                        )
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
private fun TemplatePreviewDialog(
    template: WorkoutTemplate,
    goal: String,
    minSets: Int,
    maxSets: Int,
    onDismiss: () -> Unit
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
                Text(getString(template.nameKey))
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
                    text = "Goal Adapted: ${goal.replace("goal_", "").replace("_", " ").replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    val logicalDays = template.dayTemplates.filter { !it.isRestDay }
                    logicalDays.forEach { day ->
                        item {
                            Text(
                                text = getEnglishTitleFromKey(day.titleKey),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        val dayExercises = template.exercises.filter { it.dayOfWeek == day.dayOfWeek }
                        items(dayExercises) { ex ->
                            var sets = ex.targetSets
                            if (minSets > 0) sets = kotlin.math.max(sets, minSets)
                            if (maxSets > 0) sets = kotlin.math.min(sets, maxSets)
                            val reps = WorkoutTemplates.getGoalReps(ex.name, goal, sets)
                            val rest = WorkoutTemplates.getGoalRestSeconds(goal, ex.name)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { focusManager.clearFocus() },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(translateExerciseName(ex.name), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (reps.contains("sec")) "${sets}x $reps | ${rest}s Rest" else "${sets}x $reps reps | ${rest}s Rest",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (ex.targetIntensity.isNotBlank()) {
                                                val rpeExpl = com.grafizum.repio.ui.components.getRpeRirExplanation(ex.targetIntensity)
                                                val displayIntensity = if (rpeExpl.isNotBlank()) "${ex.targetIntensity} ($rpeExpl)" else ex.targetIntensity
                                                Text(
                                                    text = " • $displayIntensity",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
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
        },
        confirmButton = {
            Button(onClick = {
                focusManager.clearFocus()
                onDismiss()
            }) { Text(getString("close")) }
        }
    )
}

@Composable
private fun AlternativeExercisesDialog(
    exercise: RoutineExerciseEntity,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val alternatives = remember(exercise.name, exercise.muscleGroup) { 
        WorkoutTemplates.getAlternativeExercises(exercise.name, exercise.muscleGroup) 
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
                Text(getString("alternative_exercises"))
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
                Text(getString("select_alternative_desc", translateMuscleGroup(exercise.muscleGroup)), style = MaterialTheme.typography.bodySmall)
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(alternatives) { alt ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                                focusManager.clearFocus()
                                onSelect(alt) 
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(translateExerciseName(alt), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
    var targetWeightValue by remember { mutableStateOf(exercise?.targetWeightKg?.toString() ?: "") }
    var targetIntensityStr by remember { mutableStateOf(exercise?.targetIntensity ?: "") }
    var restSecondsStr by remember { mutableStateOf(exercise?.restSeconds?.toString() ?: "60") }
    var isTimeBased by remember { mutableStateOf(exercise?.targetReps?.contains("sec", ignoreCase = true) ?: false) }
    
    var targetRepsValue by remember { mutableStateOf(if (!isTimeBased) exercise?.targetReps ?: "" else "8-12") }
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        val detected = WorkoutTemplates.getMuscleGroupForExercise(it)
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
                            if (targetRepsValue.isBlank() || targetRepsValue == "8-12") targetRepsValue = WorkoutTemplates.getRecommendedRepsForExercise(selectedName)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    OutlinedTextField(
                        value = targetSets,
                        onValueChange = { if (it.all { char -> char.isDigit() }) targetSets = it },
                        label = { Text(getString("sets"), maxLines = 1) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        supportingText = if (showSupportingSpacer) { { Text(" ", maxLines = 1) } } else null
                    )

                    if (isTimeBased) {
                        var expandedTime by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedTime,
                            onExpandedChange = { expandedTime = !expandedTime },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            val digits = targetTimeValue.filter { it.isDigit() }
                            OutlinedTextField(
                                value = if (digits.isNotBlank()) "${digits}s" else "30s",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(getString("time_label"), maxLines = 1) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTime) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().fillMaxHeight(),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                supportingText = if (showSupportingSpacer) { { Text(" ", maxLines = 1) } } else null
                            )
                            ExposedDropdownMenu(expanded = expandedTime, onDismissRequest = { expandedTime = false }) {
                                listOf("30", "45", "60", "90", "120").forEach { t ->
                                    DropdownMenuItem(text = { Text("${t}s") }, onClick = { targetTimeValue = t; expandedTime = false })
                                }
                            }
                        }
                    } else {
                        com.grafizum.repio.ui.components.RepsRangeField(
                            value = targetRepsValue,
                            onValueChange = { targetRepsValue = it },
                            label = getString("reps"),
                            isError = repsError,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            supportingText = if (showSupportingSpacer) {
                                {
                                    if (repsHasRangeError) {
                                        Text(getString("min_greater_than_max"), color = MaterialTheme.colorScheme.error, maxLines = 1)
                                    } else {
                                        Text(" ", maxLines = 1)
                                    }
                                }
                            } else null
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    OutlinedTextField(
                        value = targetWeightValue,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) targetWeightValue = it },
                        label = { Text(getString("target_weight"), maxLines = 1) },
                        placeholder = { Text("20.0") },
                        suffix = { Text("kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        supportingText = if (showSupportingSpacer) { { Text(" ", maxLines = 1) } } else null
                    )
                    OutlinedTextField(
                        value = targetIntensityStr,
                        onValueChange = { targetIntensityStr = it },
                        label = { Text(getString("intensity_label"), maxLines = 1) },
                        placeholder = { Text("RPE 8") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        supportingText = if (showSupportingSpacer) { { Text(" ", maxLines = 1) } } else null
                    )
                }


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
                    } else targetRepsValue.trim()
                    onConfirm(RoutineExerciseEntity(
                        id = exercise?.id ?: 0, 
                        dayOfWeek = dayOfWeek, 
                        name = name.trim(), 
                        muscleGroup = muscleGroup, 
                        targetSets = targetSets.toIntOrNull() ?: 3, 
                        targetReps = finalReps, 
                        targetIntensity = targetIntensityStr, 
                        targetWeightKg = targetWeightValue.toDoubleOrNull() ?: 0.0,
                        restSeconds = restSecondsStr.toIntOrNull() ?: 60, 
                        videoUrl = videoUrl, 
                        orderIndex = exercise?.orderIndex ?: 99
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
