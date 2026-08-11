package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    var workoutNameInput by remember { mutableStateOf("Workout") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Premade Templates, 1 = Build Custom

    var selectedTemplate by remember { mutableStateOf(WorkoutTemplates.template5DaySplit) }
    var previewTemplateModal by remember { mutableStateOf<WorkoutTemplate?>(null) }

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

    // Sheet for previewing full day breakdown of a template
    previewTemplateModal?.let { template ->
        ModalBottomSheet(
            onDismissRequest = { previewTemplateModal = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = stringResource(template.nameResId),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(template.daysCountResId, template.daysCountArgs.first, template.daysCountArgs.second),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            selectedTemplate = template
                            previewTemplateModal = null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.select))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(template.descriptionResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(template.dayTemplates) { day ->
                        val dayExercises = template.exercises.filter { it.dayOfWeek == day.dayOfWeek }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = stringResource(day.titleResId),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                if (day.isRestDay) {
                                    Text(
                                        text = stringResource(R.string.rest_and_recovery),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    dayExercises.forEach { ex ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.exercise_set_summary, com.example.ui.components.translateExerciseName(ex.name), ex.targetSets),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            MuscleGroupBadge(muscleGroup = ex.muscleGroup)
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

    // Add custom exercise dialog
    if (showAddCustomExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddCustomExerciseDialog = false },
            onAdd = { newEx ->
                customExercises.add(newEx)
                showAddCustomExerciseDialog = false
            }
        )
    }

    Column(
        modifier = modifier
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

                        OutlinedTextField(
                            value = workoutNameInput,
                            onValueChange = { workoutNameInput = it },
                            label = { Text(stringResource(R.string.program_name)) },
                            placeholder = { Text(stringResource(R.string.workout_program_name_placeholder)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { testTag = "onboarding_workout_name_input" }
                        )
                    }
                }
            }

            // STEP 2: CHOOSE TEMPLATE VS BUILD CUSTOM TAB ROW
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
                        // Option 1: Premade Templates
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (selectedTab == 0) {
                                CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                            } else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 0 }
                                .semantics { testTag = "tab_premade_templates" }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.premade_templates),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Option 2: Build Custom
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (selectedTab == 1) {
                                CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                            } else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 1 }
                                .semantics { testTag = "tab_build_custom" }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.build_custom),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (selectedTab == 1) {
                            Button(
                                onClick = { showAddCustomExerciseDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(82.dp)
                                    .width(70.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.new_action),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }
                    }
                }
            }

            // TAB CONTENT
            if (selectedTab == 0) {
                // PREMADE TEMPLATES LIST
                item {
                    Text(
                        text = stringResource(R.string.choose_recommended_template),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
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
                            .clickable { selectedTemplate = template }
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

            } else {
                // BUILD CUSTOM ROUTINE TAB
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.customize_exercises),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // LIVE MISSING BODY PART WARNING CARD FOR CUSTOM ROUTINE
                        val missingParts = getMissingMuscleGroups(customExercises)
                        MissingBodyPartsWarningCard(missingParts = missingParts)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.exercises_count, customExercises.size),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                items(customExercises) { exercise ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
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
                                        text = stringResource(R.string.sets_x_reps, exercise.targetSets, exercise.targetReps),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { customExercises.remove(exercise) }) {
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
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // SAVE & COMPLETE SETUP BUTTON (STICKY FOOTER)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // CLOSE BUTTON (Cancel without changing anything)
            OutlinedButton(
                onClick = onCancel,
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
                    val finalName = workoutNameInput.trim().ifEmpty { "Workout" }
                    if (selectedTab == 0) {
                        viewModel.applyWorkoutTemplate(selectedTemplate, finalName)
                    } else {
                        viewModel.completeFirstTimeSetup(finalName, customDays, customExercises)
                    }
                    onComplete()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (RoutineExerciseEntity) -> Unit
) {
    var dayOfWeek by remember { mutableStateOf("MONDAY") }
    var name by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf("Chest") }
    var targetSets by remember { mutableStateOf("3") }
    var targetReps by remember { mutableStateOf("8-12") }

    var expandedDay by remember { mutableStateOf(false) }
    var expandedMuscle by remember { mutableStateOf(false) }

    val daysOptions = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val muscleOptions = listOf("Chest", "Back", "Legs", "Shoulders", "Biceps", "Triceps", "Abs")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                }
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetSets,
                        onValueChange = { targetSets = it },
                        label = { Text(stringResource(R.string.target_sets)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetReps,
                        onValueChange = { targetReps = it },
                        label = { Text(stringResource(R.string.target_reps)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            RoutineExerciseEntity(
                                id = 0,
                                dayOfWeek = dayOfWeek,
                                name = name.trim(),
                                muscleGroup = muscleGroup,
                                targetSets = targetSets.toIntOrNull() ?: 3,
                                targetReps = targetReps.trim().ifEmpty { "8-12" },
                                videoUrl = "",
                                orderIndex = 99
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
