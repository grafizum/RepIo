package com.grafizum.repio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.grafizum.repio.ui.components.SearchableExerciseField
import com.grafizum.repio.ui.components.ThemedToast
import com.grafizum.repio.ui.components.getExerciseDetails
import com.grafizum.repio.ui.components.getMissingMuscleGroups
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.components.translateMuscleGroup
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getString

import com.grafizum.repio.ui.theme.LocalDeveloperSkin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FirstTimeSetupScreen(
    viewModel: WorkoutViewModel,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var workoutNameInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Premade, 1 = Assisted, 2 = Custom

    // Request notification permission for Android 13+ (API 33+)
    /* val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    } */

    // Assisted Build State
    val assistedEquipment = remember { mutableStateListOf("Gym") }
    val assistedBodyParts = remember { mutableStateListOf(
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids",
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors",
        "Biceps", "Triceps", "Forearms",
        "Abs", "Obliques",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"
    ) }
    val assistedSelectedDays = remember { mutableStateListOf("MONDAY", "WEDNESDAY", "FRIDAY") }
    var assistedIntensity by remember { mutableStateOf("Medium") }
    val assistedIncludedExercises = remember { mutableStateListOf<String>() }
    val assistedExcludedExercises = remember { mutableStateListOf<String>() }
    var preferencesExpanded by remember { mutableStateOf(false) }

    val allAvailableExercises = remember { WorkoutTemplates.getAllExerciseNames() }

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
    val isDeveloper = LocalDeveloperSkin.current

    // Platform back handling placeholder

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

    var showAddCustomExerciseDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
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
                if (editingExercise != null) {
                    val index = customExercises.indexOf(editingExercise)
                    if (index != -1) {
                        customExercises[index] = ex
                    }
                } else {
                    customExercises.add(ex)
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                            }
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isLightTheme) 0.08f else 0.2f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = if (isLightTheme) 0.05f else 0.15f)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = getString("welcome_title"),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = getString("welcome_text"),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // STEP 1: CUSTOM WORKOUT NAME
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = getString("name_workout_program"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = getString("name_workout_program_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        var isFocused by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = workoutNameInput,
                            onValueChange = { if (it.length <= 20) workoutNameInput = it },
                            label = { Text(getString("my_workout")) }, // Changed label to R.string.my_workout
                            placeholder = { 
                                if (!isFocused) {
                                    Text(
                                        text = getString("my_workout"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            supportingText = {
                                Text(
                                    text = if (workoutNameInput.length >= 20) getString("max_characters_reached") else "${workoutNameInput.length}/20",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = if (workoutNameInput.length >= 20) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            isError = workoutNameInput.length >= 20,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isFocused = it.isFocused }
                                .semantics { testTag = "onboarding_workout_name_input" }
                        )
                    }
                }
            }

            // STEP 2: CHOOSE TEMPLATE VS ASSISTED VS CUSTOM BUILD TAB ROW
            item {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = getString("select_routine_type"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Option 1: Premade Templates (Show if NOT selected)
                        if (selectedTab != 0) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { 
                                        focusManager.clearFocus()
                                        selectedTab = 0 
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = getString("premade_templates"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Option 2: Assisted Build (Show if NOT selected)
                        if (selectedTab != 1) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { 
                                        focusManager.clearFocus()
                                        selectedTab = 1 
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = getString("assisted_build"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Option 3: Custom Build (Show if NOT selected)
                        if (selectedTab != 2) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { 
                                        focusManager.clearFocus()
                                        selectedTab = 2 
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = getString("build_custom"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MODE TITLE
                    val currentModeTitle = when(selectedTab) {
                        0 -> getString("premade_templates")
                        1 -> getString("assisted_build")
                        else -> getString("build_custom")
                    }
                    Text(
                        text = currentModeTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // TAB CONTENT
            if (selectedTab == 0) {
                // PREMADE TEMPLATES LIST
                item {
                    Text(
                        text = getString("choose_recommended_template"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(WorkoutTemplates.allTemplates) { template ->
                    val isSelected = selectedTemplate.id == template.id
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                focusManager.clearFocus()
                                selectedTemplate = template 
                            }
                            .semantics { testTag = "template_card_${template.id}" }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedTemplate = template }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = getString(template.nameKey),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = getString(template.daysCountKey, template.daysCountArgs.second),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = getString(template.badgeKey),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = getString(template.descriptionKey),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { previewTemplateModal = template }) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(getString("preview_breakdown"))
                                }

                                if (isSelected) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = getString("selected"),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TEMPLATE MISSING BODY PARTS STATUS (Templates are balanced!)
                item {
                    MissingBodyPartsWarningCard(missingParts = getMissingMuscleGroups(selectedTemplate.exercises))
                }

                // STEP 3: CUSTOMIZE DAYS FOR TEMPLATE
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "3. Choose Your Workout Days",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            
                            Text(
                                text = "Pick exactly ${selectedTemplate.daysCountArgs.first} days for this routine:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            DaySelectionRow(
                                selectedDays = templateSelectedDays,
                                onDayToggle = { day ->
                                    if (templateSelectedDays.contains(day)) {
                                        templateSelectedDays.remove(day)
                                    } else {
                                        // Optional: restrict to max count if you want hard enforcement here, 
                                        // but it's better to allow and show validation on Save.
                                        templateSelectedDays.add(day)
                                    }
                                }
                            )
                            
                            if (templateSelectedDays.size != selectedTemplate.daysCountArgs.first) {
                                Text(
                                    text = "Please select ${selectedTemplate.daysCountArgs.first} days (currently ${templateSelectedDays.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

            } else if (selectedTab == 1) {
                // ASSISTED BUILD CONTENT
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. Equipment
                            Text(getString("choose_equipment"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Gym", "Bodyweight", "Barbell", "Dumbbell").forEach { eq ->
                                    val isEqSelected = assistedEquipment.contains(eq)
                                    val displayName = when(eq) {
                                        "Gym" -> getString("full_gym")
                                        "Bodyweight" -> getString("bodyweight_only")
                                        "Barbell" -> getString("barbell_only")
                                        else -> getString("dumbbells_only")
                                    }
                                    Surface(
                                        color = if (isEqSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        contentColor = if (isEqSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { 
                                            if (eq == "Gym") {
                                                // If Full Gym is pressed, it clears everything else and becomes the only selection
                                                assistedEquipment.clear()
                                                assistedEquipment.add("Gym")
                                            } else {
                                                if (isEqSelected) {
                                                    // Prevent removing the last item if you want to ensure at least one is selected,
                                                    // or just remove it.
                                                    if (assistedEquipment.size > 1) {
                                                        assistedEquipment.remove(eq)
                                                    }
                                                } else {
                                                    // If any other equipment is pressed, remove "Full Gym" and add this one
                                                    assistedEquipment.remove("Gym")
                                                    assistedEquipment.add(eq)
                                                }
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Body Parts (Expandable Categories)
                            Text(getString("choose_body_parts"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            
                            val categories = listOf(
                                getString("arms") to listOf("Biceps", "Triceps", "Forearms"),
                                getString("legs") to listOf("Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"),
                                getString("back") to listOf("Lats", "Upper Back", "Upper Traps", "Spinal Erectors"),
                                getString("chest") to listOf("Chest"),
                                getString("shoulders") to listOf("Front Deltoids", "Lateral Deltoids", "Rear Deltoids"),
                                getString("abs") to listOf("Abs", "Obliques")
                            )

                            var expandedCategory by remember { mutableStateOf<String?>(null) }

                            categories.forEach { (catName, parts) ->
                                val isCatExpanded = expandedCategory == catName
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCatExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (isCatExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { expandedCategory = if (isCatExpanded) null else catName }
                                                .padding(14.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val selectedInCat = parts.count { assistedBodyParts.contains(it) }
                                                Text(
                                                    text = catName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                if (selectedInCat > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text("$selectedInCat", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                            Icon(
                                                imageVector = if (isCatExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (isCatExpanded) {
                                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    parts.forEach { part ->
                                                        val isPartSelected = assistedBodyParts.contains(part)
                                                        val displayName = when(part) {
                                                            "Chest" -> getString("chest")
                                                            "Back" -> getString("back")
                                                            "Legs" -> getString("legs")
                                                            "Shoulders" -> getString("shoulders")
                                                            "Biceps" -> getString("biceps")
                                                            "Triceps" -> getString("triceps")
                                                            "Front Deltoids" -> getString("front_delts")
                                                            "Lateral Deltoids" -> getString("lateral_delts")
                                                            "Rear Deltoids" -> getString("rear_delts")
                                                            "Lats" -> getString("lats_part")
                                                            "Upper Back" -> getString("upper_back")
                                                            "Upper Traps" -> getString("upper_traps")
                                                            "Spinal Erectors" -> getString("spinal_erectors")
                                                            "Forearms" -> getString("forearms")
                                                            "Obliques" -> getString("obliques")
                                                            "Quadriceps" -> getString("quadriceps_part")
                                                            "Hamstrings" -> getString("hamstrings_part")
                                                            "Glutes" -> getString("glutes_part")
                                                            "Calves" -> getString("calves_part")
                                                            "Hip Adductors" -> getString("hip_adductors")
                                                            "Hip Abductors" -> getString("hip_abductors")
                                                            else -> getString("abs")
                                                        }
                                                        
                                                        Column {
                                                            Surface(
                                                                color = if (isPartSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                                                contentColor = if (isPartSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                shape = RoundedCornerShape(8.dp),
                                                                modifier = Modifier
                                                                    .border(
                                                                        width = if (isPartSelected) 2.dp else 1.dp,
                                                                        color = if (isPartSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                                        shape = RoundedCornerShape(8.dp)
                                                                    )
                                                                    .clickable(
                                                                        interactionSource = remember { MutableInteractionSource() },
                                                                        indication = null
                                                                    ) { 
                                                                        if (isPartSelected) assistedBodyParts.remove(part) else assistedBodyParts.add(part)
                                                                    }
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = displayName,
                                                                        style = MaterialTheme.typography.labelSmall
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 3. Days
                            Text("3. Choose Workout Days", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            DaySelectionRow(
                                selectedDays = assistedSelectedDays,
                                onDayToggle = { day ->
                                    if (assistedSelectedDays.contains(day)) {
                                        if (assistedSelectedDays.size > 1) assistedSelectedDays.remove(day)
                                    } else {
                                        assistedSelectedDays.add(day)
                                    }
                                }
                            )

                            // 4. Exercise Preferences
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { preferencesExpanded = !preferencesExpanded }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    getString("exercise_preferences"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Icon(
                                    imageVector = if (preferencesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = preferencesExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Include Section
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(getString("include_exercises"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        var includeInput by remember { mutableStateOf("") }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                SearchableExerciseField(
                                                    allExercises = allAvailableExercises,
                                                    value = includeInput,
                                                    onValueChange = { includeInput = it },
                                                    label = "",
                                                    placeholder = "Search or type..."
                                                )
                                            }
                                            Button(
                                                onClick = { if (includeInput.isNotBlank()) { assistedIncludedExercises.add(includeInput.trim()); includeInput = "" } },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(50.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp)
                                            ) {
                                                Text(getString("add_pref"), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            assistedIncludedExercises.forEach { ex ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { assistedIncludedExercises.remove(ex) }
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                        Text(translateExerciseName(ex), style = MaterialTheme.typography.labelSmall)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Exclude Section
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(getString("exclude_exercises"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        var excludeInput by remember { mutableStateOf("") }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                SearchableExerciseField(
                                                    allExercises = allAvailableExercises,
                                                    value = excludeInput,
                                                    onValueChange = { excludeInput = it },
                                                    label = "",
                                                    placeholder = "Search or type..."
                                                )
                                            }
                                            Button(
                                                onClick = { if (excludeInput.isNotBlank()) { assistedExcludedExercises.add(excludeInput.trim()); excludeInput = "" } },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(50.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp)
                                            ) {
                                                Text(getString("add_pref"), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            assistedExcludedExercises.forEach { ex ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.errorContainer,
                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { assistedExcludedExercises.remove(ex) }
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                        Text(translateExerciseName(ex), style = MaterialTheme.typography.labelSmall)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Intensity
                            Text(getString("choose_intensity"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Easy", "Medium", "Difficult").forEach { level ->
                                    val isIntSelected = assistedIntensity == level
                                    val (name, desc) = when(level) {
                                        "Easy" -> getString("intensity_easy") to getString("intensity_easy_desc")
                                        "Medium" -> getString("intensity_medium") to getString("intensity_medium_desc")
                                        else -> getString("intensity_difficult") to getString("intensity_difficult_desc")
                                    }
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                        containerColor = if (isIntSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isIntSelected) 2.dp else 1.dp,
                                            color = if (isIntSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { assistedIntensity = level }
                                ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = isIntSelected, onClick = { assistedIntensity = level })
                                            Column {
                                                Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val program = WorkoutTemplates.generateAssistedProgram(
                        equipment = assistedEquipment, 
                        bodyParts = assistedBodyParts.toList(), 
                        selectedDays = assistedSelectedDays.toList(),
                        intensity = assistedIntensity,
                        includedExercises = assistedIncludedExercises.toList(),
                        excludedExercises = assistedExcludedExercises.toList()
                    )
                    MissingBodyPartsWarningCard(
                        missingParts = getMissingMuscleGroups(program.exercises),
                        equipmentLimitedParts = program.unsatisfiedParts
                    )
                }

            } else {
                // CUSTOM BUILD ROUTINE TAB
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "3. Build Your Routine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (customExercises.isNotEmpty()) {
                                    TextButton(
                                        onClick = { customExercises.clear() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(getString("delete_all"), style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                IconButton(
                                    onClick = { 
                                        focusManager.clearFocus()
                                        showAddCustomExerciseDialog = true 
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = getString("add_exercise"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        DaySelectionRow(
                            selectedDays = listOf(selectedDayInCustom),
                            onDayToggle = { selectedDayInCustom = it }
                        )

                        val exercisesForSelectedDay = customExercises.filter { it.dayOfWeek == selectedDayInCustom }
                        
                        Text(
                            text = if (exercisesForSelectedDay.isEmpty()) "No exercises for this day" 
                                   else getString("exercises_count", exercisesForSelectedDay.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        // LIVE MISSING BODY PART WARNING CARD FOR CUSTOM ROUTINE
                        val missingParts = getMissingMuscleGroups(customExercises)
                        MissingBodyPartsWarningCard(missingParts = missingParts)
                    }
                }

                val exercisesForSelectedDay = customExercises.filter { it.dayOfWeek == selectedDayInCustom }
                items(exercisesForSelectedDay) { exercise ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = exercise.dayOfWeek.take(3),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = translateExerciseName(exercise.name),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MuscleGroupBadge(muscleGroup = exercise.muscleGroup)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (exercise.targetReps.contains("sec", ignoreCase = true)) 
                                               getString("sets_x_time", exercise.targetSets, exercise.targetReps)
                                               else getString("sets_x_reps", exercise.targetSets, exercise.targetReps),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = { replacingExercise = exercise },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = getString("replace"),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { 
                                        focusManager.clearFocus()
                                        editingExercise = exercise 
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { 
                                        focusManager.clearFocus()
                                        customExercises.remove(exercise) 
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // SAVE & COMPLETE SETUP BUTTON (STICKY FOOTER)
            Spacer(modifier = Modifier.height(10.dp))

            if (showFinalSaveConfirmation) {
                AlertDialog(
                    onDismissRequest = { showFinalSaveConfirmation = false },
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnClickOutside = true,
                        dismissOnBackPress = true
                    ),
                    title = { Text(getString("confirm_new_program_title")) },
                    text = { Text(getString("confirm_new_program_message")) },
                    confirmButton = {
                        val fallbackName = getString("my_workout")
                        Button(
                            onClick = {
                                val finalName = workoutNameInput.trim().ifEmpty { fallbackName }
                                if (selectedTab == 0) {
                                    val mapped = remapTemplateToDays(selectedTemplate, templateSelectedDays.toList())
                                    viewModel.completeFirstTimeSetup(finalName, mapped.first, mapped.second)
                                } else if (selectedTab == 1) {
                                    // Generate and apply assisted program
                                    val program = WorkoutTemplates.generateAssistedProgram(
                                        equipment = assistedEquipment,
                                        bodyParts = assistedBodyParts.toList(),
                                        selectedDays = assistedSelectedDays.toList(),
                                        intensity = assistedIntensity,
                                        includedExercises = assistedIncludedExercises.toList(),
                                        excludedExercises = assistedExcludedExercises.toList()
                                    )
                                    viewModel.completeFirstTimeSetup(finalName, program.days, program.exercises)
                                } else {
                                    viewModel.completeFirstTimeSetup(finalName, customDays, customExercises)
                                }
                                onComplete()
                                showFinalSaveConfirmation = false
                            }
                        ) {
                            Text(getString("confirm"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFinalSaveConfirmation = false }) {
                            Text(getString("cancel"))
                        }
                    }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // CLOSE BUTTON (Cancel without changing anything)
                if (isManualReentry) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isLightTheme) Color(0xFFF5F5F5) else Color(0xFF2C2C2C),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text(
                            text = getString("close"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // SAVE BUTTON
                Button(
                    onClick = { 
                        focusManager.clearFocus()
                        if (isManualReentry) {
                            showFinalSaveConfirmation = true 
                        } else {
                            // Direct save if it's the very first time
                            val fallbackName = "My Workout"
                            val finalName = workoutNameInput.trim().ifEmpty { fallbackName }
                            if (selectedTab == 0) {
                                val mapped = remapTemplateToDays(selectedTemplate, templateSelectedDays.toList())
                                viewModel.completeFirstTimeSetup(finalName, mapped.first, mapped.second)
                            } else if (selectedTab == 1) {
                                val program = WorkoutTemplates.generateAssistedProgram(
                                    equipment = assistedEquipment,
                                    bodyParts = assistedBodyParts.toList(),
                                    selectedDays = assistedSelectedDays.toList(),
                                    intensity = assistedIntensity,
                                    includedExercises = assistedIncludedExercises.toList(),
                                    excludedExercises = assistedExcludedExercises.toList()
                                )
                                viewModel.completeFirstTimeSetup(finalName, program.days, program.exercises)
                            } else {
                                viewModel.completeFirstTimeSetup(finalName, customDays, customExercises)
                            }
                            onComplete()
                        }
                    },
                    enabled = when(selectedTab) {
                        0 -> templateSelectedDays.size == selectedTemplate.daysCountArgs.first
                        1 -> assistedSelectedDays.isNotEmpty()
                        else -> true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .semantics { testTag = "complete_onboarding_button" }
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getString("save_and_start"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // REPLACE EXERCISE DIALOG
    replacingExercise?.let { exercise ->
        val alternatives = remember(exercise.name, exercise.muscleGroup) {
            WorkoutTemplates.getAlternativeExercises(exercise.name, exercise.muscleGroup)
        }

        AlertDialog(
            onDismissRequest = { replacingExercise = null },
            title = { Text(getString("alternative_exercises")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = getString("select_alternative_desc", translateMuscleGroup(exercise.muscleGroup)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (alternatives.isEmpty()) {
                        Text(
                            text = "No other exercises found for this muscle group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(alternatives) { altName ->
                                var expanded by remember { mutableStateOf(false) }
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            val index = customExercises.indexOf(exercise)
                                            if (index != -1) {
                                                customExercises[index] = exercise.copy(name = altName)
                                            }
                                            replacingExercise = null
                                        }
                                ) {
                                    val (altDesc, _) = getExerciseDetails(altName, exercise.muscleGroup)
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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
                                                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (altDesc.isNotBlank()) {
                                            IconButton(onClick = { expanded = !expanded }) {
                                                Icon(
                                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { replacingExercise = null }) {
                    Text(getString("cancel"))
                }
            }
        )
    }
}

@Composable
fun DaySelectionRow(
    selectedDays: List<String>,
    onDayToggle: (String) -> Unit
) {
    val allDays = listOf(
        "MONDAY" to "M",
        "TUESDAY" to "T",
        "WEDNESDAY" to "W",
        "THURSDAY" to "T",
        "FRIDAY" to "F",
        "SATURDAY" to "S",
        "SUNDAY" to "S"
    )

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
                shape = CircleShape,
                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
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

private fun getEnglishTitleFromKey(key: String): String {
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
private fun AddExerciseDialog(
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
    var isTimeBased by remember { mutableStateOf(exercise?.targetReps?.contains("sec", ignoreCase = true) ?: false) }
    var isExpanded by remember { mutableStateOf(exercise != null) }
    
    var targetRepsValue by remember { mutableStateOf(exercise?.targetReps?.replace(" sec", "") ?: "") }

    var showInvalidCharToast by remember { mutableStateOf(false) }

    val isRepsValid = remember(targetRepsValue, isTimeBased) {
        if (isTimeBased) true
        else if (targetRepsValue.isBlank()) true
        else {
            // Check if it only contains digits and '-'
            val validChars = targetRepsValue.all { it.isDigit() || it == '-' || it == '–' }
            if (!validChars) return@remember false
            
            // Cannot start or end with '-'
            if (targetRepsValue.startsWith('-') || targetRepsValue.endsWith('-') || 
                targetRepsValue.startsWith('–') || targetRepsValue.endsWith('–')) return@remember false
            
            val parts = targetRepsValue.split('-', '–').filter { it.isNotBlank() }
            if (parts.size == 2) {
                val v1 = parts[0].toIntOrNull()
                val v2 = parts[1].toIntOrNull()
                if (v1 != null && v2 != null) v1 <= v2 else false
            } else if (parts.size == 1) {
                parts[0].toIntOrNull() != null
            } else {
                false
            }
        }
    }

    var expandedDay by remember { mutableStateOf(false) }

    val daysOptions = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(getString(if (exercise == null) "add_custom_exercise" else "edit_exercise")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Reps / Seconds Sliding Toggle
                RepsTimeToggle(
                    isTimeBased = isTimeBased,
                    onToggle = { newValue: Boolean ->
                        isTimeBased = newValue
                        if (newValue && (targetRepsValue.isEmpty() || !targetRepsValue.all { it.isDigit() })) {
                            targetRepsValue = "30"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Day of Week Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDay,
                    onExpandedChange = { expandedDay = !expandedDay }
                ) {
                    OutlinedTextField(
                        value = dayOfWeek,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(getString("day_of_week")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDay,
                        onDismissRequest = { expandedDay = false },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        daysOptions.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    dayOfWeek = d
                                    expandedDay = false
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Exercise Name
                SearchableExerciseField(
                    allExercises = allAvailableExercises,
                    value = name,
                    onValueChange = { 
                        name = it
                        if (it.isBlank()) {
                            isExpanded = false
                            targetSets = ""
                            targetRepsValue = ""
                        }
                        // Auto-update muscle group
                        val detected = WorkoutTemplates.getMuscleGroupForExercise(it)
                        if (detected != "Other") {
                            muscleGroup = detected
                        }
                    },
                    onItemSelected = { selectedName ->
                        isTimeBased = com.grafizum.repio.data.WorkoutTemplates.isExerciseTimeBased(selectedName)
                        isExpanded = true
                        
                        // Auto-fill recommendations if fields are empty (likely new exercise)
                        if (targetSets.isBlank()) {
                            targetSets = com.grafizum.repio.data.WorkoutTemplates.getRecommendedSetsForExercise(selectedName).toString()
                        }
                        if (targetRepsValue.isBlank()) {
                            targetRepsValue = com.grafizum.repio.data.WorkoutTemplates.getRecommendedRepsForExercise(selectedName)
                        }
                    },
                    label = getString("exercise_name"),
                    placeholder = getString("exercise_name_placeholder")
                )

                // Muscle Group & Description (Visual indicator instead of text box)
                if (name.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getString("trained_muscles") + ":",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MuscleGroupBadge(muscleGroup = muscleGroup)
                        }
                        
                        // Detailed description in a prominent box
                        val (exerciseDesc, _) = getExerciseDetails(name, muscleGroup)
                        if (exerciseDesc.isNotBlank()) {
                            val isDeveloper = LocalDeveloperSkin.current
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
                                        text = exerciseDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetSets,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                targetSets = it 
                            }
                        },
                        label = { Text(getString("sets")) },
                        placeholder = { Text(getString("sets")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (isTimeBased) {
                        var expandedTime by remember { mutableStateOf(false) }
                        val timeOptions = listOf("30", "45", "60", "90", "120")
                        ExposedDropdownMenuBox(
                            expanded = expandedTime,
                            onExpandedChange = { expandedTime = !expandedTime },
                            modifier = Modifier.weight(1f)
                        ) {
                            val digitsOnly = targetRepsValue.filter { it.isDigit() }
                            val displayValue = if (digitsOnly.isNotBlank()) "${digitsOnly}s" else "30s"
                            OutlinedTextField(
                                value = displayValue,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(getString("time_label")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTime) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTime,
                                onDismissRequest = { expandedTime = false },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                timeOptions.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text("${t}s", style = MaterialTheme.typography.bodySmall) },
                                        onClick = {
                                            targetRepsValue = t
                                            expandedTime = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = targetRepsValue,
                            onValueChange = { newValue ->
                                // Allow only digits and "-"
                                val isCharValid = newValue.all { it.isDigit() || it == '-' || it == '–' }
                                if (isCharValid) {
                                    targetRepsValue = newValue
                                } else {
                                    showInvalidCharToast = true
                                }
                            },
                            label = { Text(getString("reps")) },
                            placeholder = { Text(getString("reps")) },
                            isError = targetRepsValue.isNotEmpty() && !isRepsValid,
                            supportingText = if (targetRepsValue.isNotEmpty() && !isRepsValid) {
                                { Text(getString("rep_format_error")) }
                            } else null,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ThemedToast(
                    message = getString("invalid_char_error"),
                    themeKey = themeKey,
                    visible = showInvalidCharToast,
                    onDismiss = { showInvalidCharToast = false }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && targetRepsValue.isNotBlank()) {
                        val finalReps = if (isTimeBased == true) {
                            val digits = targetRepsValue.filter { it.isDigit() }
                            if (digits.isNotBlank()) "$digits sec" else "30 sec"
                        } else {
                            targetRepsValue.trim()
                        }
                        onConfirm(
                            RoutineExerciseEntity(
                                id = exercise?.id ?: 0,
                                dayOfWeek = dayOfWeek,
                                name = name.trim(),
                                muscleGroup = muscleGroup,
                                targetSets = targetSets.toIntOrNull() ?: 3,
                                targetReps = finalReps,
                                videoUrl = exercise?.videoUrl ?: "",
                                orderIndex = exercise?.orderIndex ?: 99
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && targetRepsValue.isNotBlank() && isRepsValid
            ) {
                Text(
                    text = getString(if (exercise == null) "add" else "save")
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(getString("cancel"))
            }
        }
    )
}
