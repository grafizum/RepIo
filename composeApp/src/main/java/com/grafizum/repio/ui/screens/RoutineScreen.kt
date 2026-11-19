package com.grafizum.repio.ui.screens

import android.os.SystemClock
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
import kotlinx.coroutines.isActive
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
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
import com.grafizum.repio.data.ExerciseLocalization
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
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.ScreenHeader
import com.grafizum.repio.ui.components.getExerciseDetails
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.components.effectiveMuscleGroup
import com.grafizum.repio.ui.components.translateMuscleGroup
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.getString
import com.grafizum.repio.util.getQuantityString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    initialShowAddDialog: Boolean = false,
    onAddDialogDismissed: (() -> Unit)? = null,
    onCustomClick: (() -> Unit)? = null
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
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

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

    // Copy Day feature: copy the selected day's exercises to another day
    var showCopyDayDialog by remember { mutableStateOf(false) }
    var copyTargetDay by remember { mutableStateOf("TUESDAY") }
    var copyReplaceExercises by remember { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current
    val window = LocalWindowSize.current

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
                                            text = getQuantityString(template.daysCountKey, template.daysCountArgs.first, template.daysCountArgs.first),
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
            .padding(horizontal = window.screenPadding)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Spacer(modifier = Modifier.height(window.sectionSpacing))

        // COMPACT HEADER — title keeps the leftover width, the two actions keep theirs, so
        // longer translations ("Neu", "Zurück") cannot squeeze the title or vice versa.
        ScreenHeader(
            title = getString("edit"),
            icon = Icons.Default.FitnessCenter,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onCustomClick != null) {
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onCustomClick()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("action_back"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.resetFirstTimeSetup()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(getString("new_action"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(window.sectionSpacing))

        WorkoutDayWheel(
            selectedDay = workoutState.selectedDayOfWeek,
            onDaySelected = { viewModel.selectDayForWorkoutNow(it) },
            today = today
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
                    // CLIPPED, not `SizeTransform(clip = false)` as this was.
                    //
                    // `clip = false` asks the animation to size itself to the UNION of the two
                    // days, so the taller day's cards are measured, laid out and drawn at full
                    // height while both days slide - two complete screens of work on the same
                    // frames the user is watching. Clipping to the container bounds both days to
                    // one screen, which is also what the gesture wants: the day wheel is a fixed
                    // height, so content that grows past it should scroll, not stretch the
                    // transition.
                    //
                    // The slide itself is unchanged in direction; the fades are shortened because
                    // a fade is what the eye reads as "something is animating", and at 300ms it
                    // outlasts the slide it is meant to cover.
                    }.using(SizeTransform(clip = true))
                },
                label = "DayTransition"
            ) { targetDay ->
                val dayExercisesList = reorderedList
                val listState = rememberLazyListState()
                val density = LocalDensity.current

                // Where the finger is, in the viewport's own coordinates (NaN = not placed yet,
                // which is also what a card missing from the visible list reports). Declared
                // before both users of it: the drag handler is the only place it can be READ
                // from, and the glide loop is the only place it can be re-read over time.
                var draggingFingerY by remember { mutableFloatStateOf(Float.NaN) }

                // Shared drag-to-reorder logic (used by the selection-mode handle drag
                // and by long-press drag outside selection mode)
                fun startExerciseDrag(exerciseId: Long) {
                    draggingItemId = exerciseId
                    dragOffsetY = 0f
                    // The finger has not been placed in the list yet; the first move event does it.
                    draggingFingerY = Float.NaN
                }

                // Edge auto-scroll: while the dragged card is held past the
                // scroll margin at the top or bottom of the viewport, the list
                // glides in that direction so an item can travel the whole day
                // without lifting the finger (the move-into-view the owner asked
                // for). moveExerciseByDrag records the finger's distance past
                // each edge as the scroll speed; the loop below applies it —
                // the scroll must come from here rather than from the gesture,
                // or the drag deltas and the viewport would fight each other.
                //
                // OWNER REPORT, 2026-10-08: "moving an exercise doesnt scroll up
                // or down when grabbing the exercise and going to a border". The
                // edge test used to compare dragOffsetY — the distance travelled
                // SINCE THE CARD WAS GRABBED — against this margin. That is not
                // where the finger is: grabbing the first card and pulling it 60px
                // scrolled the list while the card was nowhere near an edge, and
                // a 20px pull on the last visible card at the bottom border did
                // nothing, because 20px of travel never crosses a 56px margin.
                // The finger's real position is its local Y inside the card plus
                // the card's current LAYOUT offset in the viewport — the graphic
                // translation Compose paints during the drag is not part of that
                // coordinate space, so this reading is stable while the list
                // scrolls underneath. Auto-scroll now fires on proximity to the
                // border, at any amount of travel, in either direction.
                //
                // OWNER REPORT, 2026-10-09: "the auto scroll up and down is too
                // sensible you need to make it more natural and less sensible".
                // Three separate things made it twitchy, and all three are gone:
                // the ramp was LINEAR, so a finger a tenth of the way into the band
                // was already asking for a tenth of the top speed; the top speed
                // was a tenth of the VIEWPORT PER TICK, six viewports a second;
                // and it began the moment the finger entered the band, without
                // waiting to see whether the finger meant to stop there. It is now
                // squared in depth (bandDepthRatio), capped at one viewport per 25
                // ticks, and gated behind a dwell the finger has to serve.
                val dragScrollMargin = with(density) { 72.dp.toPx() }

                /**
                 * The travelling half of the drag: [travel] pixels move the held card one
                 * slot at a time and leave the remainder as its painted offset.
                 *
                 * IT IS A SEPARATE FUNCTION BECAUSE THE CARD TRAVELS FOR TWO DIFFERENT
                 * REASONS, and only one of them used to be wired up.
                 *
                 * OWNER REPORT, 2026-10-15: *"moving an exercise wih the auto scrolling up and
                 * down just places the exercise somewhere along the way even though i didnt lift
                 * the finger."* Measured on emulator-5554, long-pressing the first of Monday's nine
                 * exercises and parking the finger past the bottom border: the finger's walk down
                 * the list advanced the card correctly (idx 0 -> 4 over ~830px), and then the glide
                 * ran — `firstVisibleItemIndex` 0 -> 3 -> 5, the list travelling about five rows
                 * under a finger that sent no further events — while the held card's index stayed
                 * 4. The card was therefore left a screen and a half behind the finger it is
                 * supposed to be under, and lifting there commits it at that stale slot: the
                 * owner's "somewhere along the way".
                 *
                 * The missing half is a sign, not a second rule. `dispatchRawDelta` moves the LIST
                 * by N pixels, which is exactly the same event as the FINGER moving N pixels in the
                 * other direction, and the card has to travel for both: the scroll takes the row
                 * out from under the card, so the card must take the row's place. Feeding the glide
                 * through the same function is what keeps one rule — no threshold, no spacing, no
                 * direction is repeated here.
                 */
                fun travelDraggedItem(exerciseId: Long, travel: Float) {
                    val currentIndex = reorderedList.indexOfFirst { it.id == exerciseId }
                    if (currentIndex == -1) return
                    val spacing = with(density) { 16.dp.toPx() }

                    // The neighbours' heights, read from the VISIBLE list - the only place a
                    // row's measured size is known. A neighbour that is not composed reports
                    // null and the step is refused rather than guessed at.
                    val nextSize = listState.layoutInfo.visibleItemsInfo
                        .find { it.key == reorderedList.getOrNull(currentIndex + 1)?.id }?.size
                    val prevSize = listState.layoutInfo.visibleItemsInfo
                        .find { it.key == reorderedList.getOrNull(currentIndex - 1)?.id }?.size

                    val stepped = exerciseDragTravel(
                        index = currentIndex,
                        travel = dragOffsetY + travel,
                        listSize = reorderedList.size,
                        nextSize = nextSize,
                        prevSize = prevSize,
                        spacing = spacing
                    )
                    dragOffsetY = stepped.offset
                    if (stepped.index != currentIndex) {
                        val newList = reorderedList.toMutableList()
                        val item = newList.removeAt(currentIndex)
                        newList.add(stepped.index, item)
                        reorderedList = newList
                    }
                }

                fun moveExerciseByDrag(exerciseId: Long, fingerYInItem: Float, dragDelta: Float) {
                    // WHERE THE FINGER IS, recorded for the glide loop: its local Y
                    // inside the card plus the card's current LAYOUT offset (never its
                    // painted offset, which the drag itself moves). A move event is the
                    // only place that local Y is known — and a finger HELD at a border
                    // sends no further events — so the loop reads this reading every
                    // frame rather than re-asking a gesture that has gone quiet.
                    draggingFingerY = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == exerciseId }?.offset?.toFloat()
                        ?.plus(fingerYInItem)
                        ?: Float.NaN

                    travelDraggedItem(exerciseId, dragDelta)
                }

                fun endExerciseDrag() {
                    draggingItemId = null
                    dragOffsetY = 0f
                    draggingFingerY = Float.NaN
                    // Persist orderIndex
                    val finalOrderedList = reorderedList.mapIndexed { index, ex ->
                        ex.copy(orderIndex = index)
                    }
                    viewModel.reorderExercises(finalOrderedList)
                }

                LaunchedEffect(draggingItemId) {
                    if (draggingItemId == null) return@LaunchedEffect
                    // THE GLIDE'S OWN CLOCK LIVES HERE rather than in the drag handler,
                    // and that is what makes a dwell possible at all: the finger's Y is
                    // only ever reported by MOVE events, so a finger held still at a
                    // border produces no events — a hold can only be measured by
                    // something that ticks. The hold dies with the gesture.
                    var bandHold = DragBandHold(direction = 0, sinceMs = 0L)
                    while (isActive) {
                        val layoutInfo = listState.layoutInfo
                        val viewportHeight =
                            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset)
                                .toFloat().takeIf { it > 0f } ?: 1000f
                        // Re-asked every frame rather than stored: the answer depends on
                        // where the list currently IS, so a tick is a better moment to ask
                        // than the last move event was.
                        val wanted = exerciseDragAutoScrollDelta(
                            fingerYInViewport = draggingFingerY.takeIf { !it.isNaN() },
                            viewportTop = layoutInfo.viewportStartOffset.toFloat(),
                            viewportBottom = layoutInfo.viewportEndOffset.toFloat(),
                            margin = dragScrollMargin,
                            maxSpeed = viewportHeight / AUTO_SCROLL_TICKS_PER_VIEWPORT,
                            canScrollBackward = listState.canScrollBackward,
                            canScrollForward = listState.canScrollForward
                        )
                        val nowMs = SystemClock.uptimeMillis()
                        bandHold = exerciseDragBandHold(wanted.compareTo(0f), nowMs, bandHold)
                        if (exerciseDragAutoScrollEngaged(bandHold.heldMs(nowMs))) {
                            // Non-suspending raw delta, right for a 16ms loop. Nothing is
                            // read back from it: the end of the list shows up in the next
                            // tick anyway, as canScrollForward/Backward going false.
                            listState.dispatchRawDelta(wanted)
                            // AND THE HELD CARD TRAVELS BY THE SAME AMOUNT. Without this
                            // the list slides out from under a motionless finger and the
                            // card is stranded at the slot it happened to be at when the
                            // glide began — see travelDraggedItem.
                            draggingItemId?.let { travelDraggedItem(it, wanted) }
                        }
                        kotlinx.coroutines.delay(16)
                    }
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
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = null,
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
                                    dayExercisesList.map { effectiveMuscleGroup(it.name, it.muscleGroup) }
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
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
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
                                        Icon(imageVector = Icons.Default.Checklist, contentDescription = "Selection Mode", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    IconButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            copyTargetDay = if (workoutState.selectedDayOfWeek == "MONDAY") "TUESDAY" else "MONDAY"
                                            showCopyDayDialog = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = getString("copy_day_title", dayOfWeekDisplayName(workoutState.selectedDayOfWeek)), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                                            tint = MaterialTheme.colorScheme.primary,
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
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = null,
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
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = getString("add_workout"),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
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
                                            moveExerciseByDrag(exercise.id, change.position.y, dragAmount.y)
                                        },
                                        onDragEnd = { endExerciseDrag() },
                                        onDragCancel = {
                                            draggingItemId = null
                                            dragOffsetY = 0f
                                            draggingFingerY = Float.NaN
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
                                            moveExerciseByDrag(exercise.id, change.position.y, dragAmount.y)
                                        },
                                        onDragEnd = { endExerciseDrag() },
                                        onDragCancel = {
                                            draggingItemId = null
                                            dragOffsetY = 0f
                                            draggingFingerY = Float.NaN
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
                                // THE LEVEL THIS ROW IS WORKED AT. Owner, 2026-10-16: the row
                                // showed the reps range instead, which is the plan the level
                                // system then overrides.
                                //
                                // The level is read from the STORED state and nothing else:
                                // [WorkoutViewModel.storedProgressionLevel] answers null for an
                                // exercise that has never been levelled, and the row then says
                                // "New". That is deliberate - the derived value the system uses
                                // to prescribe a first session is not a choice the user made, and
                                // printing it as one is the "it shows the setup default, not
                                // mine" report.
                                levelManaged = viewModel.isExerciseLevelManaged(exercise.name),
                                level = viewModel.storedProgressionLevel(exercise.name),
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
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = null,
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
    // The level system's own switch: a NEW exercise is picked through the graphical
    // picker while it is on (owner, 2026-10-11), and typed into the classic field when
    // the user runs custom mode. An edit always keeps the classic field.
    val levelSystemEnabled by viewModel.levelSystemEnabled.collectAsState()

    if (showAddDialog || editingExercise != null) {
        // The ladder opened from this editor, as an edit pass — same contract as
        // the live-workout editor (owner, 2026-10-05: the level is editable from
        // an exercise editor even while the level system prescribes it).
        var editorLevelLadder by remember { mutableStateOf<String?>(null) }
        // The editor's "switch to Custom" line. Turning the level system off is a
        // whole-app decision, so it goes through a confirmation here exactly as it
        // does on the workout screen.
        var confirmCustomMode by remember { mutableStateOf(false) }
        ExerciseEditDialog(
            allAvailableExercises = allAvailableExercises,
            exercise = editingExercise,
            dayOfWeek = workoutState.selectedDayOfWeek,
            themeKey = themeOption,
            // THE LEVEL FACTS ARE RESOLVED BY NAME, inside the dialog. That is what
            // lets the ADD form (`editingExercise == null`) show the level list: the
            // dialog passes the name the user has picked in its own search field, and
            // the view model answers with the level the system would start that
            // exercise at. Handing the facts in for `editingExercise` - as this call
            // site used to - meant an add could only pass nulls, which is why adding
            // an exercise showed the custom input boxes (owner, 2026-10-09).
            levelDataFor = { levelName, pageStart -> editorLevelData(viewModel, levelName, pageStart) },
            userEquipmentCategories = viewModel.userEquipmentCategories(),
            userGrantedTags = viewModel.userGrantedEquipmentTags(),
            levelSystemOn = levelSystemEnabled,
            // A declined evolution is locked permanently, so this badge is the
            // only way back into the offer. It lives here because this is the
            // editor the owner named ("allow the user to upgrade it when
            // editing that exercise").
            evolutionLocked = editingExercise?.let { viewModel.isEvolutionDeclined(it.name) } == true,
            onUpgrade = {
                editingExercise?.let { viewModel.reopenEvolution(it.name) }
                editingExercise = null
            },
            onDismiss = {
                showAddDialog = false
                editingExercise = null
            },
            onRequestCustomMode = { confirmCustomMode = true },
            onLevelChange = { levelName, level ->
                viewModel.setExerciseLevelFromEditor(levelName, level)
            },
            // A Roman numeral picked under a level: level AND stage in one write.
            onStageChange = { levelName, level, stage ->
                viewModel.setExerciseStateFromEditor(levelName, level, stage)
            },
            onOpenLevelLadder = { levelName -> editorLevelLadder = levelName },
            // Leaving through Cancel puts the level, the stage and the lock back
            // to what the pass opened on — see [WorkoutViewModel.cancelExerciseEditPass].
            onCancel = { levelName -> viewModel.cancelExerciseEditPass(levelName) },
            onSave = { updatedExercise ->
                // Saved: the pass's level writes stand and there is nothing for
                // Cancel to undo.
                viewModel.commitExerciseEditPass(updatedExercise.name)
                viewModel.saveExercise(updatedExercise)
                showAddDialog = false
                editingExercise = null
            }
        )
        if (confirmCustomMode) {
            AlertDialog(
                onDismissRequest = { confirmCustomMode = false },
                title = { Text(getString("custom_mode_title")) },
                text = { Text(getString("custom_mode_body")) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setLevelSystemEnabled(false)
                        confirmCustomMode = false
                    }) { Text(getString("custom_mode_confirm")) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmCustomMode = false }) {
                        Text(getString("cancel"))
                    }
                }
            )
        }
        editorLevelLadder?.let { name ->
            LevelDetailsDialog(
                viewModel = viewModel,
                exerciseName = name,
                fromEditorEditPass = true,
                onDismiss = { editorLevelLadder = null }
            )
        }
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
                    Text(getQuantityString("confirm_delete_multiple_msg", selectedExerciseIds.count()))
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

    if (showCopyDayDialog) {
        val focusManager = LocalFocusManager.current
        val sourceExercises = allExercises.filter { it.dayOfWeek == workoutState.selectedDayOfWeek }
        val sourceDayName = dayOfWeekDisplayName(workoutState.selectedDayOfWeek)
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
                        if (sourceExercises.isNotEmpty() && copyTargetDay != workoutState.selectedDayOfWeek) {
                            if (copyReplaceExercises) {
                                allExercises.filter { it.dayOfWeek == copyTargetDay }
                                    .forEach { viewModel.deleteExercise(it.id) }
                            }
                            val maxOrder = allExercises
                                .filter { it.dayOfWeek == copyTargetDay }
                                .maxOfOrNull { it.orderIndex } ?: 0
                            sourceExercises.sortedBy { it.orderIndex }.forEachIndexed { index, ex ->
                                viewModel.saveExercise(
                                    ex.copy(
                                        id = 0L,
                                        dayOfWeek = copyTargetDay,
                                        orderIndex = maxOrder + index + 1
                                    )
                                )
                            }
                            viewModel.markDayAsWorkoutDay(copyTargetDay)
                        }
                        showCopyDayDialog = false
                    },
                    enabled = sourceExercises.isNotEmpty() && copyTargetDay != workoutState.selectedDayOfWeek
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
        ReplaceExerciseDialog(
            exercise = exercise,
            userEquipmentCategories = viewModel.userEquipmentCategories(),
            userGrantedTags = viewModel.userGrantedEquipmentTags(),
            onDismiss = { replacingExercise = null },
            onReplace = { name, muscleGroup ->
                // Swap the original for the picked exercise, in its slot. The new
                // row is parameterized from the user's settings (goal/difficulty)
                // instead of inheriting the old exercise's sets, reps and rest.
                viewModel.replaceExerciseWith(
                    original = exercise,
                    previousNames = emptyList(),
                    replacements = listOf(
                        viewModel.parameterizedReplacement(
                            original = exercise,
                            newName = ExerciseLocalization.canonicalFor(name) ?: name,
                            newMuscleGroup = muscleGroup
                        )
                    )
                )
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

/**
 * How fast the edit-day list should glide while a dragged exercise is held near the top
 * or bottom border, in pixels per frame. `0f` means "do not scroll".
 *
 * Owner report, 2026-10-08: *"moving an exercise doesnt scroll up or down when grabbing the
 * exercise and going to a border so that the user goes up the ladder or down in case there
 * is too many exercises"*.
 *
 * The bug was never the speed, it was the QUESTION: the old test compared the drag's
 * cumulative offset — how far the card had travelled SINCE IT WAS GRABBED — against the
 * scroll margin. That is not where the finger is. Grabbing the first card and pulling it 60px
 * scrolled the list while the card sat nowhere near a border, and pulling the last visible
 * card 20px at the bottom border did nothing at all, because 20px of travel never crosses a
 * 56px margin. So the function takes the finger's position IN THE VIEWPORT (its local Y inside
 * the card, plus the card's current LAYOUT offset) and answers on proximity to the border,
 * whatever the amount of travel and whichever direction it is.
 *
 * Extracted from the composable so the rule is testable without a composition, and so the
 * next change to it has to state its approximations rather than hide them:
 *
 *  - the speed ramps with the SQUARE of the depth into the margin band and is capped at
 *    [maxSpeed], so a finger that has just entered the band barely moves the list while one parked
 *    at the border glides at the cap (see [bandDepthRatio] for why squared);
 *  - and nothing moves at all until the finger has RESTED in the band for [AUTO_SCROLL_DWELL_MS]
 *    — the caller applies [exerciseDragAutoScrollEngaged], because arriving at a border and
 *    parking there are different intentions;
 *  - the direction is only ever the one with room to move: with [canScrollBackward] false a
 *    finger at the top asks for `0f` rather than fighting a list that cannot move, which is
 *    also why the caller can stop the loop as soon as the raw delta comes back zero;
 *  - a null finger position (the dragged row is not in the viewport's visible list, which
 *    happens on the frame the list is still composing it) asks for no scroll rather than
 *    guessing where it went.
 */
/**
 * Where ONE step of travel leaves the card being dragged: the slot it occupies now, and the
 * travel that is left over as its painted offset.
 */
data class DragTravel(val index: Int, val offset: Float)

/**
 * One step of a dragged card's travel: past half of the next row it takes that row's place, and
 * past half of the previous row it takes that one's — the two thresholds the drag has always had,
 * in one place.
 *
 * THE SAME RULE ANSWERS FOR A MOVING FINGER AND FOR A MOVING LIST, and that is the whole point of
 * it being here rather than inline in the gesture: `dispatchRawDelta` scrolls the list by N pixels,
 * which is the same event as the finger moving N pixels the other way, so a glide that moves the
 * list must move the card too. Measured on emulator-5554 on 2026-10-15, a glide scrolling the list
 * five rows under a parked finger left the held card behind at its old slot - the owner's "places
 * the exercise somewhere along the way". Both callers now feed this one function.
 *
 * A neighbour whose height is unknown ([nextSize]/[prevSize] null, because the row is not composed)
 * refuses the step rather than inventing a threshold, and either end of the list refuses it too.
 * The leftover travel is deliberately allowed to exceed a row: it is the painted offset, and
 * consuming a whole row per step is what keeps the card under the finger.
 */
fun exerciseDragTravel(
    index: Int,
    travel: Float,
    listSize: Int,
    nextSize: Int?,
    prevSize: Int?,
    spacing: Float
): DragTravel {
    if (index < 0 || listSize <= 0) return DragTravel(index, travel)
    if (travel > 0f && index < listSize - 1 && nextSize != null) {
        val threshold = (nextSize + spacing) / 2f
        if (travel > threshold) return DragTravel(index + 1, travel - (nextSize + spacing))
    } else if (travel < 0f && index > 0 && prevSize != null) {
        val threshold = -(prevSize + spacing) / 2f
        if (travel < threshold) return DragTravel(index - 1, travel + (prevSize + spacing))
    }
    return DragTravel(index, travel)
}

fun exerciseDragAutoScrollDelta(
    fingerYInViewport: Float?,
    viewportTop: Float,
    viewportBottom: Float,
    margin: Float,
    maxSpeed: Float,
    canScrollBackward: Boolean,
    canScrollForward: Boolean
): Float {
    if (fingerYInViewport == null || margin <= 0f || maxSpeed <= 0f) return 0f
    return when {
        fingerYInViewport < viewportTop + margin && canScrollBackward ->
            -maxSpeed * bandDepthRatio(viewportTop + margin - fingerYInViewport, margin)
        fingerYInViewport > viewportBottom - margin && canScrollForward ->
            maxSpeed * bandDepthRatio(fingerYInViewport - (viewportBottom - margin), margin)
        else -> 0f
    }
}

/**
 * How much of the band's top speed a finger [depthIntoBand] pixels inside it has earned: the depth
 * as a fraction of the band, SQUARED.
 *
 * Owner report, 2026-10-09: *"the auto scroll up and down is too sensible you need to make it more
 * natural and less sensible"*. The shipped rule was LINEAR — `fingerY - (border + margin)` — so a
 * finger a tenth of the way into a 72dp band asked for a tenth of the top speed immediately, and
 * since that top speed was six viewports a second the list was already moving before the user had
 * decided to hold there. Squaring the entry is what makes the band read as a place the finger
 * settles into rather than a switch it trips: 10% in is 1% of the speed, half in is a quarter, and
 * only a genuine park at the border approaches the cap. Past the border the fraction saturates at
 * 1, so dragging off the edge keeps the same speed instead of accelerating into a fling.
 */
private fun bandDepthRatio(depthIntoBand: Float, margin: Float): Float {
    val fraction = (depthIntoBand / margin).coerceIn(0f, 1f)
    return fraction * fraction
}

/**
 * How long a finger must REST inside the border band before the list glides.
 *
 * It is the other half of the 2026-10-09 "less sensible" ruling, and the half no screenshot can
 * show: passing through the band while placing a card near the bottom of the list is not the same
 * intention as parking there to bring the rest of the day into reach, and only the second should
 * move the list.
 */
private const val AUTO_SCROLL_DWELL_MS = 250L

/**
 * The fastest the edge glide may move the list, as the number of 16ms ticks it takes to cross one
 * viewport: 25 ticks is about two and a half viewports a second.
 *
 * It used to be a tenth of the viewport PER TICK — six viewports a second, which is most of why the
 * glide read as a fling the moment a finger touched the band.
 */
private const val AUTO_SCROLL_TICKS_PER_VIEWPORT = 25f

/**
 * Whether a finger that has been in the border band for [heldInBandMs] has earned a glide yet.
 *
 * Measured by the caller's tick loop rather than by the drag events, and that is not a detail: a
 * held finger reports no events at all, so a dwell that has to be SERVED can only be counted by
 * something that ticks. A quarter of a second is longer than a pass-through and shorter than a
 * deliberate hold.
 */
fun exerciseDragAutoScrollEngaged(
    heldInBandMs: Long,
    dwellMs: Long = AUTO_SCROLL_DWELL_MS
): Boolean = heldInBandMs >= dwellMs

/**
 * Which border the dragged finger is currently asking for (+1 = the bottom band, -1 = the top,
 * 0 = neither) and since when.
 */
data class DragBandHold(val direction: Int, val sinceMs: Long) {
    /** How long the finger has been asking for this border; 0 while it is asking for neither. */
    fun heldMs(nowMs: Long): Long = if (sinceMs == 0L) 0L else nowMs - sinceMs
}

/**
 * The dwell bookkeeping for one tick of the glide loop: the hold, carried forward.
 *
 * Two rules, both of which the dwell would be wrong without, and both of which are invisible in the
 * composable that uses them:
 *
 *  - a DIRECTION CHANGE restarts the dwell. Arriving at the bottom band after hovering in the top
 *    one is a new intention, and without this the second band would inherit the first one's quarter
 *    second and start moving the instant the finger crossed over;
 *  - a zero direction CLEARS it, so a finger that came back inside the list (or is parked at an end
 *    the list cannot pass) has to earn its own dwell rather than carrying a spent one around.
 *
 * Same direction keeps the original timestamp — which is the whole point: that timestamp is the
 * dwell.
 */
fun exerciseDragBandHold(
    direction: Int,
    nowMs: Long,
    previous: DragBandHold
): DragBandHold = when {
    direction == 0 -> DragBandHold(direction = 0, sinceMs = 0L)
    direction != previous.direction -> DragBandHold(direction = direction, sinceMs = nowMs)
    else -> previous
}
