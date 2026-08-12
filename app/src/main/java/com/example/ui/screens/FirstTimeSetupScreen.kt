package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RoutineDayEntity
import com.example.data.RoutineExerciseEntity
import com.example.data.WorkoutTemplate
import com.example.data.WorkoutTemplates
import com.example.ui.components.MissingBodyPartsWarningCard
import com.example.ui.components.MuscleGroupBadge
import com.example.ui.components.getMissingMuscleGroups
import com.example.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FirstTimeSetupScreen(
    viewModel: WorkoutViewModel,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var workoutNameInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Premade, 1 = Assisted, 2 = Custom

    // Assisted Build State
    val assistedEquipment = remember { mutableStateListOf("Gym") }
    val assistedBodyParts = remember { mutableStateListOf(
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids",
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors",
        "Biceps", "Triceps", "Forearms",
        "Abs", "Obliques",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"
    ) }
    var assistedDays by remember { mutableIntStateOf(3) }
    var assistedIntensity by remember { mutableStateOf("Medium") }

    BackHandler {
        if (selectedTab != 0) {
            selectedTab = 0
        } else {
            onCancel()
        }
    }

    var selectedTemplate by remember { mutableStateOf(WorkoutTemplates.template5DaySplit) }
    var previewTemplateModal by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showFinalSaveConfirmation by remember { mutableStateOf(false) }

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
        mutableStateListOf(
            RoutineExerciseEntity(0, "MONDAY", "Bench Press / Floor Press", "Chest", 4, "8-12", "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Shoulder Press", "Shoulders", 3, "10-12", "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell Rows", "Back", 4, "8-12", "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Bicep Curls", "Biceps", 3, "10-12", "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Barbell Squats", "Legs", 4, "8-12", "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Overhead Tricep Extension", "Triceps", 3, "10-15", "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Crunches / Leg Raises", "Abs", 3, "15-20", "", 2)
        )
    }

    var showAddCustomExerciseDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }

    // Add / Edit custom exercise dialog
    if (showAddCustomExerciseDialog || editingExercise != null) {
        AddExerciseDialog(
            exercise = editingExercise,
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
                                text = stringResource(R.string.welcome_title),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.welcome_text),
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
                                text = stringResource(R.string.name_workout_program),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.name_workout_program_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        var isFocused by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = workoutNameInput,
                            onValueChange = { if (it.length <= 20) workoutNameInput = it },
                            label = { Text(stringResource(R.string.program_name)) },
                            placeholder = { 
                                if (!isFocused) {
                                    Text(
                                        text = stringResource(R.string.my_workout),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            supportingText = {
                                Text(
                                    text = if (workoutNameInput.length >= 20) stringResource(R.string.max_characters_reached) else "${workoutNameInput.length}/20",
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
                            text = stringResource(R.string.select_routine_type),
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
                                        text = stringResource(R.string.premade_templates),
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
                                        text = stringResource(R.string.assisted_build),
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
                                        text = stringResource(R.string.build_custom),
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
                        0 -> stringResource(R.string.premade_templates)
                        1 -> stringResource(R.string.assisted_build)
                        else -> stringResource(R.string.build_custom)
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
                        text = stringResource(R.string.choose_recommended_template),
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
                                            text = stringResource(template.nameResId),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(template.daysCountResId, template.daysCountArgs.first, template.daysCountArgs.second),
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
                                        text = stringResource(template.badgeResId),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(template.descriptionResId),
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
                                    Text(stringResource(R.string.preview_breakdown))
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
                                                text = stringResource(R.string.selected),
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
                            Text(stringResource(R.string.choose_equipment), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Gym", "Bodyweight", "Barbell", "Dumbbell").forEach { eq ->
                                    val isEqSelected = assistedEquipment.contains(eq)
                                    val displayName = when(eq) {
                                        "Gym" -> stringResource(R.string.full_gym)
                                        "Bodyweight" -> stringResource(R.string.bodyweight_only)
                                        "Barbell" -> stringResource(R.string.barbell_only)
                                        else -> stringResource(R.string.dumbbells_only)
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
                            Text(stringResource(R.string.choose_body_parts), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            
                            val categories = listOf(
                                stringResource(R.string.arms) to listOf("Biceps", "Triceps", "Forearms"),
                                stringResource(R.string.legs) to listOf("Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"),
                                stringResource(R.string.back) to listOf("Lats", "Upper Back", "Upper Traps", "Spinal Erectors"),
                                stringResource(R.string.chest) to listOf("Chest"),
                                stringResource(R.string.shoulders) to listOf("Front Deltoids", "Lateral Deltoids", "Rear Deltoids"),
                                stringResource(R.string.abs) to listOf("Abs", "Obliques")
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
                                                            "Chest" -> stringResource(R.string.chest)
                                                            "Back" -> stringResource(R.string.back)
                                                            "Legs" -> stringResource(R.string.legs)
                                                            "Shoulders" -> stringResource(R.string.shoulders)
                                                            "Biceps" -> stringResource(R.string.biceps)
                                                            "Triceps" -> stringResource(R.string.triceps)
                                                            "Front Deltoids" -> stringResource(R.string.front_delts)
                                                            "Lateral Deltoids" -> stringResource(R.string.lateral_delts)
                                                            "Rear Deltoids" -> stringResource(R.string.rear_delts)
                                                            "Lats" -> stringResource(R.string.lats_part)
                                                            "Upper Back" -> stringResource(R.string.upper_back)
                                                            "Upper Traps" -> stringResource(R.string.upper_traps)
                                                            "Spinal Erectors" -> stringResource(R.string.spinal_erectors)
                                                            "Forearms" -> stringResource(R.string.forearms)
                                                            "Obliques" -> stringResource(R.string.obliques)
                                                            "Quadriceps" -> stringResource(R.string.quadriceps_part)
                                                            "Hamstrings" -> stringResource(R.string.hamstrings_part)
                                                            "Glutes" -> stringResource(R.string.glutes_part)
                                                            "Calves" -> stringResource(R.string.calves_part)
                                                            "Hip Adductors" -> stringResource(R.string.hip_adductors)
                                                            "Hip Abductors" -> stringResource(R.string.hip_abductors)
                                                            else -> stringResource(R.string.abs)
                                                        }
                                                        
                                                        var showPartInfo by remember { mutableStateOf(false) }

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
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Icon(
                                                                        imageVector = Icons.Default.Info,
                                                                        contentDescription = "Science Info",
                                                                        modifier = Modifier
                                                                            .size(14.dp)
                                                                            .clickable { showPartInfo = !showPartInfo },
                                                                        tint = if (isPartSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }

                                                            if (showPartInfo) {
                                                                Text(
                                                                    text = WorkoutTemplates.volumeRecommendations[part] ?: "",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.secondary,
                                                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp).widthIn(max = 200.dp)
                                                                )
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
                            Text(stringResource(R.string.choose_days), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (assistedDays > 1) assistedDays-- },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "$assistedDays",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )

                                IconButton(
                                    onClick = { if (assistedDays < 7) assistedDays++ },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // 4. Intensity
                            Text(stringResource(R.string.choose_intensity), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Easy", "Medium", "Difficult").forEach { level ->
                                    val isIntSelected = assistedIntensity == level
                                    val (name, desc) = when(level) {
                                        "Easy" -> stringResource(R.string.intensity_easy) to stringResource(R.string.intensity_easy_desc)
                                        "Medium" -> stringResource(R.string.intensity_medium) to stringResource(R.string.intensity_medium_desc)
                                        else -> stringResource(R.string.intensity_difficult) to stringResource(R.string.intensity_difficult_desc)
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
                        getApplication = viewModel.getApplication(), 
                        equipment = assistedEquipment, 
                        bodyParts = assistedBodyParts.toList(), 
                        daysCount = assistedDays,
                        intensity = assistedIntensity
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
                            text = stringResource(R.string.exercises_count, customExercises.size),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        IconButton(
                            onClick = { 
                                focusManager.clearFocus()
                                showAddCustomExerciseDialog = true 
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_exercise),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                        // LIVE MISSING BODY PART WARNING CARD FOR CUSTOM ROUTINE
                        val missingParts = getMissingMuscleGroups(customExercises)
                        MissingBodyPartsWarningCard(missingParts = missingParts)
                    }
                }

                items(customExercises) { exercise ->
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
                                        text = com.example.ui.components.translateExerciseName(exercise.name),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MuscleGroupBadge(muscleGroup = exercise.muscleGroup)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (exercise.targetReps.contains("sec", ignoreCase = true)) 
                                               stringResource(R.string.sets_x_time, exercise.targetSets, exercise.targetReps)
                                               else stringResource(R.string.sets_x_reps, exercise.targetSets, exercise.targetReps),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { 
                                    focusManager.clearFocus()
                                    editingExercise = exercise 
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { 
                                    focusManager.clearFocus()
                                    customExercises.remove(exercise) 
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
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
                    title = { Text(stringResource(R.string.confirm_new_program_title)) },
                    text = { Text(stringResource(R.string.confirm_new_program_message)) },
                    confirmButton = {
                        val fallbackName = stringResource(R.string.my_workout)
                        Button(
                            onClick = {
                                val finalName = workoutNameInput.trim().ifEmpty { fallbackName }
                                if (selectedTab == 0) {
                                    viewModel.applyWorkoutTemplate(selectedTemplate, finalName)
                                } else if (selectedTab == 1) {
                                    // Generate and apply assisted program
                                    val program = WorkoutTemplates.generateAssistedProgram(
                                        getApplication = viewModel.getApplication(),
                                        equipment = assistedEquipment,
                                        bodyParts = assistedBodyParts.toList(),
                                        daysCount = assistedDays,
                                        intensity = assistedIntensity
                                    )
                                    viewModel.completeFirstTimeSetup(finalName, program.days, program.exercises)
                                } else {
                                    viewModel.completeFirstTimeSetup(finalName, customDays, customExercises)
                                }
                                onComplete()
                                showFinalSaveConfirmation = false
                            }
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFinalSaveConfirmation = false }) {
                            Text(stringResource(R.string.cancel))
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
                        text = stringResource(R.string.close),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // SAVE BUTTON
                Button(
                    onClick = { 
                        focusManager.clearFocus()
                        showFinalSaveConfirmation = true 
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
                        text = stringResource(R.string.save_and_start),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    exercise: RoutineExerciseEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (RoutineExerciseEntity) -> Unit
) {
    var dayOfWeek by remember { mutableStateOf(exercise?.dayOfWeek ?: "MONDAY") }
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "Chest") }
    var targetSets by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "3") }
    var isTimeBased by remember { mutableStateOf(exercise?.targetReps?.contains("sec", ignoreCase = true) ?: false) }
    
    var targetRepsValue by remember { mutableStateOf(exercise?.targetReps?.replace(" sec", "") ?: "8-12") }

    var expandedDay by remember { mutableStateOf(false) }
    var expandedMuscle by remember { mutableStateOf(false) }
    var isConfirming by remember { mutableStateOf(false) }

    val daysOptions = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val muscleOptions = listOf("Chest", "Back", "Legs", "Shoulders", "Biceps", "Triceps", "Abs")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (exercise == null) R.string.add_custom_exercise else R.string.edit_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Reps / Seconds Switch
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(false to stringResource(R.string.reps), true to stringResource(R.string.time_label)).forEach { (isTime, label) ->
                        val isSelected = isTimeBased == isTime
                        Surface(
                            modifier = Modifier.weight(1f).clickable { 
                                isTimeBased = isTime 
                                if (isTime) targetRepsValue = "30"
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Day of Week Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDay,
                    onExpandedChange = { expandedDay = !expandedDay }
                ) {
                    OutlinedTextField(
                        value = dayOfWeek,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.day_of_week)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDay,
                        onDismissRequest = { expandedDay = false }
                    ) {
                        daysOptions.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d) },
                                onClick = {
                                    dayOfWeek = d
                                    expandedDay = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // Exercise Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
                    placeholder = { Text(stringResource(R.string.exercise_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Muscle Group Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedMuscle,
                    onExpandedChange = { expandedMuscle = !expandedMuscle }
                ) {
                    OutlinedTextField(
                        value = muscleGroup,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.muscle_group_body_part)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMuscle) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMuscle,
                        onDismissRequest = { expandedMuscle = false }
                    ) {
                        muscleOptions.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    muscleGroup = m
                                    expandedMuscle = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetSets,
                        onValueChange = { targetSets = it },
                        label = { Text(stringResource(R.string.sets)) },
                        singleLine = true,
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
                                label = { Text(stringResource(R.string.time_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTime) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTime,
                                onDismissRequest = { expandedTime = false }
                            ) {
                                timeOptions.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text("${t}s") },
                                        onClick = {
                                            targetRepsValue = t
                                            expandedTime = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = targetRepsValue,
                            onValueChange = { targetRepsValue = it },
                            label = { Text(stringResource(R.string.reps)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isConfirming) {
                    Text(
                        text = stringResource(R.string.confirm_action_question),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            val isRepsValid = remember(targetRepsValue, isTimeBased) {
                if (isTimeBased) true
                else {
                    val parts = targetRepsValue.split('-', '–').filter { it.isNotBlank() }
                    if (parts.size == 2) {
                        val v1 = parts[0].filter { it.isDigit() }.toIntOrNull()
                        val v2 = parts[1].filter { it.isDigit() }.toIntOrNull()
                        if (v1 != null && v2 != null) v1 <= v2 else true
                    } else true
                }
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && targetRepsValue.isNotBlank()) {
                        if (!isConfirming && exercise == null) {
                            isConfirming = true
                        } else {
                            val finalReps = if (isTimeBased) {
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
                            isConfirming = false
                        }
                    }
                },
                enabled = name.isNotBlank() && targetRepsValue.isNotBlank() && isRepsValid
            ) {
                Text(
                    text = if (isConfirming) stringResource(R.string.confirm) 
                           else stringResource(if (exercise == null) R.string.add else R.string.save)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
