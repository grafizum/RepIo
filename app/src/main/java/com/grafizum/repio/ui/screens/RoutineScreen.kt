package com.grafizum.repio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.stringResource
import com.grafizum.repio.R
import com.grafizum.repio.data.DayOfWeekEnum
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplate
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.ExerciseVideoLinkButton
import com.grafizum.repio.ui.components.MissingBodyPartsWarningCard
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.components.getMissingMuscleGroups
import com.grafizum.repio.ui.components.SearchableExerciseField
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.ui.viewmodel.getTodayDayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    viewModel: WorkoutViewModel,
    onBack: (() -> Unit)? = null,
    onNavigateToWorkoutNow: ((String) -> Unit)? = null,
    initialShowAddDialog: Boolean = false,
    onAddDialogDismissed: (() -> Unit)? = null,
    onCustomClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val routineDays by viewModel.allRoutineDays.collectAsState()
    val allExercises by viewModel.allRoutineExercises.collectAsState()
    val customWorkoutName by viewModel.customWorkoutName.collectAsState()
    val workoutState by viewModel.workoutNowState.collectAsState()
    val today = remember { getTodayDayOfWeek() }
    val allAvailableExercises = remember { WorkoutTemplates.getAllExerciseNames() }

    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var pendingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }

    val dayEntity = routineDays.find { it.dayOfWeek == workoutState.selectedDayOfWeek }
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
            title = { Text(stringResource(R.string.edit_workout_program_name)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { if (it.length <= 20) tempName = it },
                    label = { Text(stringResource(R.string.my_workout)) },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.workout_program_name_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    supportingText = {
                        Text(
                            text = if (tempName.length >= 20) stringResource(R.string.max_characters_reached) else "${tempName.length}/20",
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
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
                    text = stringResource(R.string.workout_templates),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(R.string.switch_program_text),
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
                                            text = stringResource(template.nameResId),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = stringResource(template.daysCountResId, template.daysCountArgs.first, template.daysCountArgs.second),
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
                                        Text(stringResource(R.string.apply))
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
                            text = customWorkoutName,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.clickable { showEditNameDialog = true }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onCustomClick != null) {
                        OutlinedButton(
                            onClick = onCustomClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_back), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.resetFirstTimeSetup() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.new_action), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
                                            DayOfWeekEnum.MONDAY -> stringResource(R.string.monday)
                                            DayOfWeekEnum.TUESDAY -> stringResource(R.string.tuesday)
                                            DayOfWeekEnum.WEDNESDAY -> stringResource(R.string.wednesday)
                                            DayOfWeekEnum.THURSDAY -> stringResource(R.string.thursday)
                                            DayOfWeekEnum.FRIDAY -> stringResource(R.string.friday)
                                            DayOfWeekEnum.SATURDAY -> stringResource(R.string.saturday)
                                            DayOfWeekEnum.SUNDAY -> stringResource(R.string.sunday)
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
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val dayName = when (workoutState.selectedDayOfWeek.uppercase()) {
                                "MONDAY" -> stringResource(R.string.monday)
                                "TUESDAY" -> stringResource(R.string.tuesday)
                                "WEDNESDAY" -> stringResource(R.string.wednesday)
                                "THURSDAY" -> stringResource(R.string.thursday)
                                "FRIDAY" -> stringResource(R.string.friday)
                                "SATURDAY" -> stringResource(R.string.saturday)
                                "SUNDAY" -> stringResource(R.string.sunday)
                                else -> workoutState.selectedDayOfWeek
                            }

                            val dayTitle = if (exercisesForDay.isNotEmpty()) {
                                val focus = exercisesForDay.map { it.muscleGroup }
                                    .map { stringResource(com.grafizum.repio.ui.components.getUpperCategoryResId(it)) }
                                    .distinct()
                                    .joinToString(", ")
                                "$dayName — $focus"
                            } else {
                                "$dayName — ${stringResource(R.string.rest_recovery_day)}"
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = dayTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayEntity?.let { com.grafizum.repio.ui.components.translateDayDescription(it.description) } ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (dayEntity != null && workoutState.selectedDayOfWeek == today) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onNavigateToWorkoutNow?.invoke(workoutState.selectedDayOfWeek) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.start_this_workout_now),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
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
                            text = stringResource(R.string.exercises_count, exercisesForDay.size),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        IconButton(
                            onClick = { showAddDialog = true },
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
                }

                if (exercisesForDay.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_exercises_scheduled),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(exercisesForDay) { exercise ->
                        com.grafizum.repio.ui.components.ExpandableExerciseCard(
                            exercise = exercise,
                            onEdit = { editingExercise = exercise },
                            onDelete = { viewModel.deleteExercise(exercise.id) },
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
    if (showAddDialog || editingExercise != null) {
        ExerciseEditDialog(
            allAvailableExercises = allAvailableExercises,
            exercise = editingExercise,
            dayOfWeek = workoutState.selectedDayOfWeek,
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

    // RESET TO DEFAULT ROUTINE CONFIRMATION DIALOG
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_program_title)) },
            text = { Text(stringResource(R.string.reset_program_text)) },
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
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
            title = { Text(stringResource(R.string.confirm_new_program_title)) },
            text = { Text(stringResource(R.string.confirm_new_program_message)) },
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
                    Text(stringResource(R.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTemplate = null }) {
                    Text(stringResource(R.string.cancel))
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
    onDismiss: () -> Unit,
    onSave: (RoutineExerciseEntity) -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "Back") }
    var targetSetsStr by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "3") }
    var isTimeBased by remember { mutableStateOf(exercise?.targetReps?.contains("sec", ignoreCase = true) ?: false) }
    
    var targetRepsValue by remember { mutableStateOf(exercise?.targetReps?.replace(" sec", "") ?: "8-12") }
    var videoUrl by remember { mutableStateOf(exercise?.videoUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exercise == null) stringResource(R.string.add_exercise) else stringResource(R.string.edit_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                // Exercise Name
                SearchableExerciseField(
                    allExercises = allAvailableExercises,
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.exercise_name),
                    placeholder = stringResource(R.string.exercise_name_placeholder)
                )

                OutlinedTextField(
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    label = { Text(stringResource(R.string.muscle_group)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetSetsStr,
                        onValueChange = { targetSetsStr = it },
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

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text(stringResource(R.string.demo_link_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
