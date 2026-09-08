package com.grafizum.repio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.grafizum.repio.data.DayOfWeekEnum
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplate
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.ExpandableExerciseCard
import com.grafizum.repio.ui.components.ExerciseEditDialog
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.components.ReplaceExerciseDialog
import com.grafizum.repio.ui.components.SearchableExerciseField
import com.grafizum.repio.ui.components.ThemedToast
import com.grafizum.repio.ui.components.WorkoutDayWheel
import com.grafizum.repio.ui.components.getExerciseDetails
import com.grafizum.repio.ui.components.getMissingMuscleGroups
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.components.translateMuscleGroup
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onBack: (() -> Unit)? = null,
    initialShowAddDialog: Boolean = false,
    onAddDialogDismissed: (() -> Unit)? = null,
    onCustomClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allExercises by viewModel.allRoutineExercises.collectAsState()
    val workoutState by viewModel.workoutNowState.collectAsState()
    val isDeveloper = LocalDeveloperSkin.current
    val today = remember { getTodayDayOfWeek() }
    val allAvailableExercises by viewModel.allAvailableExercises.collectAsState()

    val dayExercises = remember(allExercises, workoutState.selectedDayOfWeek) {
        allExercises.filter { it.dayOfWeek == workoutState.selectedDayOfWeek }
            .sortedBy { it.orderIndex }
    }

    // Local state for reordering
    var reorderedList by remember { mutableStateOf(dayExercises) }
    
    // Track dragging state
    var draggingItemId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Sync local list when database exercises change, but NOT while dragging
    LaunchedEffect(dayExercises) {
        if (draggingItemId == null) {
            reorderedList = dayExercises
        }
    }

    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var replacingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var pendingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var expandedExerciseId by remember { mutableStateOf<Long?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedExerciseIds = remember { mutableStateListOf<Long>() }
    var exerciseToDelete by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    var showDeleteMultipleConfirmation by remember { mutableStateOf(false) }

    val missingMuscleGroups = remember(allExercises) { getMissingMuscleGroups(allExercises) }
    val focusManager = LocalFocusManager.current

    androidx.compose.runtime.LaunchedEffect(workoutState.selectedDayOfWeek) {
        expandedExerciseId = null
    }

    androidx.compose.runtime.LaunchedEffect(initialShowAddDialog) {
        if (initialShowAddDialog) {
            showAddDialog = true
            onAddDialogDismissed?.invoke()
        }
    }

    // While in selection mode, the system back button exits selection mode
    // instead of navigating away from the edit screen.
    BackHandler(enabled = isSelectionMode) {
        focusManager.clearFocus()
        isSelectionMode = false
        selectedExerciseIds.clear()
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
                    modifier = Modifier.height(380.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        },
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
                                            text = getString(template.daysCountKey, template.daysCountArgs.first),
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
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // COMPACT HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(if (isDeveloper) RoundedCornerShape(0.dp) else CircleShape)
                        .background(if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = getString("edit"),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onCustomClick != null) {
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            onCustomClick()
                        },
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
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.resetFirstTimeSetup() 
                    },
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

        WorkoutDayWheel(
            selectedDay = workoutState.selectedDayOfWeek,
            onDaySelected = { viewModel.selectDayForWorkoutNow(it) },
            today = today
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                val dayExercisesList = reorderedList
                val listState = rememberLazyListState()
                val density = LocalDensity.current

                // Shared drag-to-reorder logic (used by the selection-mode handle drag
                // and by long-press drag outside selection mode)
                fun startExerciseDrag(exerciseId: Long) {
                    draggingItemId = exerciseId
                }

                fun moveExerciseByDrag(exerciseId: Long, dragDelta: Float) {
                    dragOffsetY += dragDelta

                    val currentIndex = reorderedList.indexOfFirst { it.id == exerciseId }
                    if (currentIndex == -1) return
                    val spacing = with(density) { 16.dp.toPx() }

                    // Move DOWN
                    if (dragOffsetY > 0 && currentIndex < reorderedList.size - 1) {
                        val nextItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == reorderedList[currentIndex + 1].id }
                        if (nextItemInfo != null) {
                            val threshold = (nextItemInfo.size + spacing) / 2
                            if (dragOffsetY > threshold) {
                                val newList = reorderedList.toMutableList()
                                val item = newList.removeAt(currentIndex)
                                newList.add(currentIndex + 1, item)
                                reorderedList = newList
                                dragOffsetY -= (nextItemInfo.size + spacing)
                            }
                        }
                    }
                    // Move UP
                    else if (dragOffsetY < 0 && currentIndex > 0) {
                        val prevItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == reorderedList[currentIndex - 1].id }
                        if (prevItemInfo != null) {
                            val threshold = -(prevItemInfo.size + spacing) / 2
                            if (dragOffsetY < threshold) {
                                val newList = reorderedList.toMutableList()
                                val item = newList.removeAt(currentIndex)
                                newList.add(currentIndex - 1, item)
                                reorderedList = newList
                                dragOffsetY += (prevItemInfo.size + spacing)
                            }
                        }
                    }
                }

                fun endExerciseDrag() {
                    draggingItemId = null
                    dragOffsetY = 0f
                    // Persist orderIndex
                    val finalOrderedList = reorderedList.mapIndexed { index, ex ->
                        ex.copy(orderIndex = index)
                    }
                    viewModel.reorderExercises(finalOrderedList)
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        },
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HEADER FOR SELECTED DAY
                    item {
                        Card(
                            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                val dayName = when (targetDay.uppercase()) {
                                    "MONDAY" -> getString("monday")
                                    "TUESDAY" -> getString("tuesday")
                                    "WEDNESDAY" -> getString("wednesday")
                                    "THURSDAY" -> getString("thursday")
                                    "FRIDAY" -> getString("friday")
                                    "SATURDAY" -> getString("saturday")
                                    "SUNDAY" -> getString("sunday")
                                    else -> targetDay
                                }

                                val focus = if (dayExercisesList.isNotEmpty()) {
                                    dayExercisesList.map { WorkoutTemplates.getMuscleGroupForExercise(it.name) }
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                            if (isSelectionMode) {
                                Surface(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (selectedExerciseIds.size == dayExercisesList.size) {
                                            selectedExerciseIds.clear()
                                        } else {
                                            selectedExerciseIds.clear()
                                            selectedExerciseIds.addAll(dayExercisesList.map { it.id })
                                        }
                                    },
                                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    val label = if (selectedExerciseIds.size == dayExercisesList.size) getString("deselect_all") else getString("select_all")
                                    Text(
                                        text = "$label (${selectedExerciseIds.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = getString("exercises_count", dayExercisesList.count()),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isSelectionMode) {
                                    IconButton(
                                        onClick = { 
                                            focusManager.clearFocus()
                                            if (selectedExerciseIds.isNotEmpty()) showDeleteMultipleConfirmation = true 
                                        },
                                        enabled = selectedExerciseIds.isNotEmpty()
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected", tint = if (selectedExerciseIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    }

                                    IconButton(onClick = { 
                                        focusManager.clearFocus()
                                        isSelectionMode = false
                                        selectedExerciseIds.clear()
                                    }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                                    }
                                } else {
                                    IconButton(onClick = { 
                                        focusManager.clearFocus()
                                        isSelectionMode = true 
                                    }) {
                                        Icon(imageVector = Icons.Default.Checklist, contentDescription = "Selection Mode", tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary)
                                    }

                                    IconButton(
                                        onClick = { 
                                            focusManager.clearFocus()
                                            showAddDialog = true 
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = getString("add_exercise"),
                                            tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (dayExercisesList.isEmpty()) {
                        item {
                            Card(
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = if (isDeveloper) BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        focusManager.clearFocus()
                                        showAddDialog = true 
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = getString("add_workout"),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        items(dayExercisesList, key = { it.id }) { exercise ->
                            val isSelected = selectedExerciseIds.contains(exercise.id)
                            val isDragging = draggingItemId == exercise.id
                            
                            val dragHandleModifier = if (isSelectionMode) {
                                // Selection mode: immediate drag on the handle
                                Modifier.pointerInput(exercise.id) {
                                    detectDragGestures(
                                        onDragStart = { startExerciseDrag(exercise.id) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            moveExerciseByDrag(exercise.id, dragAmount.y)
                                        },
                                        onDragEnd = { endExerciseDrag() },
                                        onDragCancel = {
                                            draggingItemId = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                            } else Modifier

                            // Outside selection mode: long-press anywhere on the card to start dragging
                            val cardDragModifier = if (!isSelectionMode) {
                                Modifier.pointerInput(exercise.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { startExerciseDrag(exercise.id) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            moveExerciseByDrag(exercise.id, dragAmount.y)
                                        },
                                        onDragEnd = { endExerciseDrag() },
                                        onDragCancel = {
                                            draggingItemId = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                            } else Modifier

                            ExpandableExerciseCard(
                                exercise = exercise,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                dragHandleModifier = dragHandleModifier,
                                isExpanded = expandedExerciseId == exercise.id,
                                onExpandedChange = { expanded ->
                                    expandedExerciseId = if (expanded) exercise.id else null
                                },
                                onSelectedChange = { checked ->
                                    if (checked) selectedExerciseIds.add(exercise.id)
                                    else selectedExerciseIds.remove(exercise.id)
                                },
                                onEdit = { editingExercise = exercise },
                                onDelete = { exerciseToDelete = exercise },
                                onReplace = { replacingExercise = exercise },
                                onSaveVideoUrl = { newUrl ->
                                    viewModel.saveExercise(exercise.copy(videoUrl = newUrl))
                                },
                                modifier = (if (isDragging) {
                                    Modifier.graphicsLayer {
                                        translationY = dragOffsetY
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                        alpha = 0.8f
                                    }.zIndex(1f)
                                } else {
                                    Modifier.animateItem()
                                }).then(cardDragModifier)
                            )
                        }

                        if (!isSelectionMode) {
                            item {
                                Card(
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAddDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = getString("add_exercise"),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }
        }
    }

    // DIALOGS
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

    exerciseToDelete?.let { exercise ->
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
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
                        viewModel.deleteExercise(exercise.id)
                        exerciseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(getString("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    exerciseToDelete = null
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    if (showDeleteMultipleConfirmation) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { showDeleteMultipleConfirmation = false },
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
                    Text(getString("confirm_delete_multiple_msg", selectedExerciseIds.count()))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
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
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showDeleteMultipleConfirmation = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    replacingExercise?.let { exercise ->
        ReplaceExerciseDialog(
            exercise = exercise,
            onDismiss = { replacingExercise = null },
            onSelect = { altName, muscleGroup ->
                viewModel.saveExercise(exercise.copy(name = altName, muscleGroup = muscleGroup, videoUrl = ""))
                replacingExercise = null
            }
        )
    }

    if (showResetDialog) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("reset_program_title"))
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
                    Text(getString("reset_program_text"))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
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
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showResetDialog = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    pendingTemplate?.let { template ->
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { pendingTemplate = null },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            ),
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
                Button(
                    onClick = {
                        focusManager.clearFocus()
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
                TextButton(onClick = {
                    focusManager.clearFocus()
                    pendingTemplate = null
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }
}
