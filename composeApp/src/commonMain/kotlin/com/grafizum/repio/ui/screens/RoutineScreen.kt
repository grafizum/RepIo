package com.grafizum.repio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.grafizum.repio.data.DayOfWeekEnum
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplate
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.ExpandableExerciseCard
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.components.SearchableExerciseField
import com.grafizum.repio.ui.components.ThemedToast
import com.grafizum.repio.ui.components.getExerciseStaticDescription
import com.grafizum.repio.ui.components.getMissingMuscleGroups
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.components.translateMuscleGroup
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    viewModel: WorkoutViewModel,
    onBack: (() -> Unit)? = null,
    initialShowAddDialog: Boolean = false,
    onAddDialogDismissed: (() -> Unit)? = null,
    onCustomClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allExercises by viewModel.allRoutineExercises.collectAsState()
    val customWorkoutName by viewModel.customWorkoutName.collectAsState()
    val workoutState by viewModel.workoutNowState.collectAsState()
    val isDeveloper = LocalDeveloperSkin.current
    val today = remember { getTodayDayOfWeek() }
    val allAvailableExercises = remember { WorkoutTemplates.getAllExerciseNames() }

    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var replacingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var pendingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedExerciseIds = remember { mutableStateListOf<Long>() }
    var exerciseToDelete by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var showDeleteMultipleConfirmation by remember { mutableStateOf(false) }

    val exercisesForDay = allExercises.filter { it.dayOfWeek == workoutState.selectedDayOfWeek }

    val missingMuscleGroups = remember(allExercises) { getMissingMuscleGroups(allExercises) }

    androidx.compose.runtime.LaunchedEffect(initialShowAddDialog) {
        if (initialShowAddDialog) {
            showAddDialog = true
            onAddDialogDismissed?.invoke()
        }
    }

    if (showEditNameDialog) {
        var tempName by remember { mutableStateOf(customWorkoutName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(getString("edit_workout_program_name")) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { if (it.length <= 20) tempName = it },
                    label = { Text(getString("my_workout")) },
                    placeholder = {
                        Text(
                            text = getString("workout_program_name_placeholder"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    supportingText = {
                        Text(
                            text = if (tempName.length >= 20) getString("max_characters_reached") else "${tempName.length}/20",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = if (tempName.length >= 20) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    isError = tempName.length >= 20,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateWorkoutName(tempName)
                    showEditNameDialog = false
                }) {
                    Text(getString("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    if (showTemplatesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplatesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = getString("workout_templates"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = getString("switch_program_text"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(WorkoutTemplates.allTemplates) { template ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = getString(template.nameKey),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = getString(template.daysCountKey, template.daysCountArgs.first, template.daysCountArgs.second),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            pendingTemplate = template
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(getString("apply"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
                        Text(
                            text = getString("editor"),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showEditNameDialog = true }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onCustomClick != null) {
                        OutlinedButton(
                            onClick = onCustomClick,
                            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                            border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("action_back"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.resetFirstTimeSetup() },
                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                            contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(getString("new_action"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DAY SELECTION CHIPS
                item {
                    val allDays = DayOfWeekEnum.entries
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

                    Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(orderedDays) { dayEnum ->
                                val isSelected = dayEnum.name == workoutState.selectedDayOfWeek
                                val isToday = dayEnum.name == today
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectDayForWorkoutNow(dayEnum.name) },
                                    label = {
                                        val displayName = when (dayEnum) {
                                            DayOfWeekEnum.MONDAY -> getString("monday")
                                            DayOfWeekEnum.TUESDAY -> getString("tuesday")
                                            DayOfWeekEnum.WEDNESDAY -> getString("wednesday")
                                            DayOfWeekEnum.THURSDAY -> getString("thursday")
                                            DayOfWeekEnum.FRIDAY -> getString("friday")
                                            DayOfWeekEnum.SATURDAY -> getString("saturday")
                                            DayOfWeekEnum.SUNDAY -> getString("sunday")
                                        }
                                        Text(
                                            text = displayName,
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
                    }
                }

                // HEADER FOR SELECTED DAY
                item {
                    Card(
                        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val dayName = when (workoutState.selectedDayOfWeek.uppercase()) {
                                "MONDAY" -> getString("monday")
                                "TUESDAY" -> getString("tuesday")
                                "WEDNESDAY" -> getString("wednesday")
                                "THURSDAY" -> getString("thursday")
                                "FRIDAY" -> getString("friday")
                                "SATURDAY" -> getString("saturday")
                                "SUNDAY" -> getString("sunday")
                                else -> workoutState.selectedDayOfWeek
                            }

                            val focus = if (exercisesForDay.isNotEmpty()) {
                                exercisesForDay.map { WorkoutTemplates.getMuscleGroupForExercise(it.name) }
                                    .map { com.grafizum.repio.ui.components.translateMuscleGroup(it) }
                                    .distinct()
                                    .joinToString(", ")
                            } else {
                                getString("rest_recovery_day")
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = focus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // EXERCISES LIST
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isSelectionMode) getString("delete_selected", selectedExerciseIds.count())
                                   else getString("exercises_count", exercisesForDay.count()),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelectionMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isSelectionMode) {
                                TextButton(onClick = {
                                    if (selectedExerciseIds.size == exercisesForDay.size) {
                                        selectedExerciseIds.clear()
                                    } else {
                                        selectedExerciseIds.clear()
                                        selectedExerciseIds.addAll(exercisesForDay.map { it.id })
                                    }
                                }) {
                                    Text(if (selectedExerciseIds.size == exercisesForDay.size) getString("deselect_all") else getString("select_all"))
                                }

                                IconButton(
                                    onClick = { if (selectedExerciseIds.isNotEmpty()) showDeleteMultipleConfirmation = true },
                                    enabled = selectedExerciseIds.isNotEmpty()
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected", tint = if (selectedExerciseIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                }

                                IconButton(onClick = { 
                                    isSelectionMode = false
                                    selectedExerciseIds.clear()
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                                }
                            } else {
                                IconButton(onClick = { isSelectionMode = true }) {
                                    Icon(imageVector = Icons.Default.Checklist, contentDescription = "Selection Mode", tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(
                                    onClick = { showAddDialog = true },
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
                    }
                }

                if (exercisesForDay.isEmpty()) {
                    item {
                        Text(
                            text = getString("no_exercises_scheduled"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(exercisesForDay) { exercise ->
                        val isSelected = selectedExerciseIds.contains(exercise.id)
                        ExpandableExerciseCard(
                            exercise = exercise,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onSelectedChange = { checked ->
                                if (checked) selectedExerciseIds.add(exercise.id)
                                else selectedExerciseIds.remove(exercise.id)
                            },
                            onEdit = { editingExercise = exercise },
                            onDelete = { exerciseToDelete = exercise },
                            onReplace = { replacingExercise = exercise },
                            onSaveVideoUrl = { newUrl ->
                                viewModel.saveExercise(exercise.copy(videoUrl = newUrl))
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }

    // ADD / EDIT EXERCISE DIALOG
    val themeOption by viewModel.themeOption.collectAsState()

    if (showAddDialog || editingExercise != null) {
        ExerciseEditDialog(
            allAvailableExercises = allAvailableExercises,
            exercise = editingExercise,
            dayOfWeek = workoutState.selectedDayOfWeek,
            themeKey = themeOption,
            onDismiss = {
                showAddDialog = false
                editingExercise = null
            },
            onSave = { updatedExercise ->
                viewModel.saveExercise(updatedExercise)
                showAddDialog = false
                editingExercise = null
            }
        )
    }

    // DELETE SINGLE CONFIRMATION
    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text(getString("confirm_delete_title")) },
            text = { Text(getString("confirm_delete_msg")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExercise(exercise.id)
                        exerciseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(getString("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    // DELETE MULTIPLE CONFIRMATION
    if (showDeleteMultipleConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteMultipleConfirmation = false },
            title = { Text(getString("confirm_delete_multiple_title")) },
            text = { Text(getString("confirm_delete_multiple_msg", selectedExerciseIds.count())) },
            confirmButton = {
                Button(
                    onClick = {
                        selectedExerciseIds.forEach { id -> viewModel.deleteExercise(id) }
                        selectedExerciseIds.clear()
                        isSelectionMode = false
                        showDeleteMultipleConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(getString("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMultipleConfirmation = false }) {
                    Text(getString("cancel"))
                }
            }
        )
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
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            viewModel.saveExercise(exercise.copy(name = altName))
                                            replacingExercise = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = translateExerciseName(altName),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.weight(1f)
                                        )
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

    // RESET TO DEFAULT ROUTINE CONFIRMATION DIALOG
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(getString("reset_program_title")) },
            text = { Text(getString("reset_program_text")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaultProgram()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(getString("reset"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    // CONFIRM NEW TEMPLATE DIALOG
    pendingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingTemplate = null },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            ),
            title = { Text(getString("confirm_new_program_title")) },
            text = { Text(getString("confirm_new_program_message")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyWorkoutTemplate(template)
                        pendingTemplate = null
                        showTemplatesSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(getString("apply"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTemplate = null }) {
                    Text(getString("cancel"))
                }
            }
        )
    }
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
    var isTimeBased by remember { mutableStateOf(exercise?.targetReps?.contains("sec", ignoreCase = true) ?: false) }
    var isExpanded by remember { mutableStateOf(exercise != null) }
    
    var targetRepsValue by remember { mutableStateOf(exercise?.targetReps?.replace(" sec", "") ?: "") }
    var videoUrl by remember { mutableStateOf(exercise?.videoUrl ?: "") }

    var activeToastMessage by remember { mutableStateOf<String?>(null) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exercise == null) getString("add_exercise") else getString("edit_exercise")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Reps / Seconds Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val showReps = !isExpanded || !isTimeBased
                    val showTime = !isExpanded || isTimeBased

                    AnimatedVisibility(
                        visible = showReps,
                        modifier = Modifier.weight(1f),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        val isSelected = !isTimeBased
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    isTimeBased = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = getString("reps"),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showTime,
                        modifier = Modifier.weight(1f),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        val isSelected = isTimeBased
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    isTimeBased = true
                                    if (targetRepsValue.isEmpty() || !targetRepsValue.all { it.isDigit() }) {
                                        targetRepsValue = "30"
                                    }
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = getString("time_label"),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
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
                            targetSetsStr = ""
                            targetRepsValue = ""
                            muscleGroup = ""
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
                        if (targetSetsStr.isBlank()) {
                            targetSetsStr = com.grafizum.repio.data.WorkoutTemplates.getRecommendedSetsForExercise(selectedName).toString()
                        }
                        if (targetRepsValue.isBlank()) {
                            targetRepsValue = com.grafizum.repio.data.WorkoutTemplates.getRecommendedRepsForExercise(selectedName)
                        }
                    },
                    label = getString("exercise_name"),
                    placeholder = getString("exercise_name_placeholder")
                )

                // Muscle Group & Description (Visual indicator instead of text box)
                if (isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getString("trained_muscles") + ":",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MuscleGroupBadge(muscleGroup = muscleGroup)
                        }

                        // Simple description if available
                        val exerciseDesc = getExerciseStaticDescription(name)
                        if (exerciseDesc.isNotBlank()) {
                            Text(
                                text = exerciseDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val onlyNumbersMsg = getString("only_numbers_error")
                    OutlinedTextField(
                        value = targetSetsStr,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                targetSetsStr = it 
                            } else {
                                activeToastMessage = onlyNumbersMsg
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
                        val invalidCharMsg = getString("invalid_char_error")
                        OutlinedTextField(
                            value = targetRepsValue,
                            onValueChange = { newValue ->
                                // Allow only digits and "-"
                                val isCharValid = newValue.all { it.isDigit() || it == '-' || it == '–' }
                                if (isCharValid) {
                                    targetRepsValue = newValue
                                } else {
                                    activeToastMessage = invalidCharMsg
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
                    message = activeToastMessage ?: "",
                    themeKey = themeKey,
                    visible = activeToastMessage != null,
                    onDismiss = { activeToastMessage = null }
                )

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text(getString("demo_link_optional")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && targetRepsValue.isNotBlank()) {
                        val sets = targetSetsStr.toIntOrNull() ?: 3
                        val finalReps = if (isTimeBased) {
                            val digits = targetRepsValue.filter { it.isDigit() }
                            if (digits.isNotBlank()) "$digits sec" else "30 sec"
                        } else {
                            targetRepsValue.trim()
                        }
                        onSave(
                            RoutineExerciseEntity(
                                id = exercise?.id ?: 0L,
                                dayOfWeek = dayOfWeek,
                                name = name,
                                muscleGroup = muscleGroup,
                                targetSets = sets,
                                targetReps = finalReps,
                                videoUrl = videoUrl,
                                orderIndex = exercise?.orderIndex ?: 99
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && targetRepsValue.isNotBlank() && isRepsValid
            ) {
                Text(getString("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(getString("cancel"))
            }
        }
    )
}
