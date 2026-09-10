package com.grafizum.repio.ui.screens

import com.grafizum.repio.util.getString
import com.grafizum.repio.util.getQuantityString
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.grafizum.repio.R
import com.grafizum.repio.data.*
import com.grafizum.repio.ui.components.*
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.startOfTodayMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.grafizum.repio.ui.theme.LocalDeveloperSkin

@Composable
fun HomeScreen(
    viewModel: WorkoutViewModel,
    onNavigateToWorkoutNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routineDays by viewModel.allRoutineDays.collectAsState()
    val allExercises by viewModel.allRoutineExercises.collectAsState()
    val loggedSets by viewModel.allLoggedSets.collectAsState()
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current

    val todayStr = getTodayDayOfWeek()
    val todayRoutineDay = remember(routineDays, todayStr) { routineDays.find { it.dayOfWeek == todayStr } }
    val todayExercises = remember(allExercises, todayStr) { allExercises.filter { it.dayOfWeek == todayStr } }
    
    // Only sets logged on the actual calendar day count as "done today";
    // filtering by weekday name alone would count same-weekday sets from previous weeks.
    // The day-millis key also refreshes the filter when the calendar day flips.
    val todayLoggedSets = remember(loggedSets, todayStr, startOfTodayMillis()) {
        val startOfToday = startOfTodayMillis()
        loggedSets.filter { it.dayOfWeek == todayStr && it.timestamp >= startOfToday }
    }

    val expandedExerciseIds = remember { mutableStateListOf<Long>() }
    val listState = rememberLazyListState()

    var pendingScrollIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pendingScrollIndex) {
        pendingScrollIndex?.let { index ->
            val headerCount = 5
            val scrollOffset = with(density) { 32.dp.roundToPx() }
            listState.animateScrollToItem(headerCount + index, -scrollOffset)
            pendingScrollIndex = null
        }
    }

    val nextUpIndex = remember(todayExercises, todayLoggedSets) {
        todayExercises.indexOfFirst { ex ->
            todayLoggedSets.filter { it.exerciseName == ex.name }.size < ex.targetSets
        }
    }

    var hasAutoScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(todayExercises) {
        if (todayExercises.isNotEmpty() && !hasAutoScrolled) {
            val indexToScroll = if (nextUpIndex == -1) todayExercises.size - 1 else nextUpIndex
            
            if (expandedExerciseIds.isEmpty() && nextUpIndex != -1) {
                expandedExerciseIds.add(todayExercises[nextUpIndex].id)
            }
            
            val headerCount = 5
            val scrollOffset = with(density) { 16.dp.roundToPx() }
            listState.animateScrollToItem(headerCount + indexToScroll, -scrollOffset)
            hasAutoScrolled = true
        }
    }

    val consecutiveWorkoutDays = remember(loggedSets) {
        WorkoutTemplates.calculateConsecutiveWorkoutDays(loggedSets)
    }
    val skippedWorkoutDays = remember(loggedSets, routineDays) {
        WorkoutTemplates.calculateSkippedWorkoutDays(loggedSets, routineDays)
    }
    val isDeveloper = LocalDeveloperSkin.current

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

        // TOP APP HEADER
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
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(if (isDeveloper) RoundedCornerShape(0.dp) else CircleShape)
                        .background(if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = getString("smart_workout_assistant"),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            // HERO BANNER - WORKOUT NOW PROMINENT TRIGGER
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDeveloper) 0.dp else 6.dp),
                    border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "hero_workout_now_card" }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDeveloper) {
                                    androidx.compose.ui.graphics.SolidColor(Color.Black)
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = if (isDeveloper) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                                    border = if (isDeveloper) BorderStroke(1.dp, Color.White) else null
                                ) {
                                    Text(
                                        text = getString("todays_workout"),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val dayName = when (todayStr.uppercase()) {
                                "MONDAY" -> getString("monday")
                                "TUESDAY" -> getString("tuesday")
                                "WEDNESDAY" -> getString("wednesday")
                                "THURSDAY" -> getString("thursday")
                                "FRIDAY" -> getString("friday")
                                "SATURDAY" -> getString("saturday")
                                "SUNDAY" -> getString("sunday")
                                else -> todayStr
                            }

                            val focusSubtitle = if (todayExercises.isNotEmpty()) {
                                todayExercises.map { it.muscleGroup }
                                    .map { getString(com.grafizum.repio.ui.components.getUpperCategoryKey(it)) }
                                    .distinct()
                                    .joinToString(", ")
                            } else {
                                if (todayRoutineDay != null && !todayRoutineDay.isRestDay) {
                                    val translatedTitle = com.grafizum.repio.ui.components.translateDayTitle(todayRoutineDay.title)
                                    if (translatedTitle.contains("—")) {
                                        translatedTitle.substringAfter("—").trim()
                                    } else if (translatedTitle.contains("-")) {
                                        translatedTitle.substringAfter("-").trim()
                                    } else {
                                        translatedTitle
                                    }
                                } else {
                                    ""
                                }
                            }

                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (focusSubtitle.isNotBlank()) {
                                Text(
                                    text = focusSubtitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = if (todayRoutineDay?.isRestDay == true) {
                                    getString("rest_day_active")
                                } else {
                                    getQuantityString("exercises_scheduled", todayExercises.size)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // SINGLE TAP WORKOUT NOW BUTTON
                            Button(
                                onClick = {
                                    viewModel.selectDayForWorkoutNow(todayStr)
                                    onNavigateToWorkoutNow()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
                                border = if (isDeveloper) BorderStroke(1.dp, Color.Black) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .semantics { testTag = "workout_now_button" }
                            ) {
                                Text(
                                    text = getString("workout_now"),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // QUICK STATS ROW
            item {
                StatCard(
                    title = getString("consecutive_days_title"),
                    value = consecutiveWorkoutDays.toString(),
                    subtitle = "",
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    badgeText = getString("skipped_days_format", skippedWorkoutDays),
                    compact = true
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // TODAY'S EXERCISES LIST PREVIEW
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = getString("todays_schedule"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    val isAllExpanded by remember(todayExercises, expandedExerciseIds) {
                        derivedStateOf { 
                            todayExercises.isNotEmpty() && expandedExerciseIds.size == todayExercises.size 
                        }
                    }
                    Surface(
                        onClick = {
                            if (isAllExpanded) {
                                expandedExerciseIds.clear()
                            } else {
                                expandedExerciseIds.clear()
                                expandedExerciseIds.addAll(todayExercises.map { it.id })
                            }
                        },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(bottom = 2.dp, end = 4.dp)
                    ) {
                        Text(
                            text = if (isAllExpanded) getString("collapse_all") else getString("expand_all"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (todayExercises.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = getString("no_exercises_today"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(todayExercises, key = { _, ex -> ex.id }) { index, exercise ->
                    val isLast = index == todayExercises.size - 1
                    val setsForThisExercise = remember(todayLoggedSets, exercise.name) {
                        todayLoggedSets.filter { it.exerciseName == exercise.name }
                    }
                    val isCompleted = setsForThisExercise.size >= exercise.targetSets
                    val isExpanded = expandedExerciseIds.contains(exercise.id)
                    val isActive = index == nextUpIndex

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val primaryDisabledColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    val outlineColor = MaterialTheme.colorScheme.outline
                    
                    val lineColor = if (isCompleted) primaryColor else primaryDisabledColor

                    // Calculate previous item's line color state
                    val prevCompleted = if (index > 0) {
                        val prevExercise = todayExercises[index - 1]
                        val prevSets = todayLoggedSets.filter { it.exerciseName == prevExercise.name }
                        prevSets.size >= prevExercise.targetSets
                    } else false

                    val prevLineColor = if (prevCompleted) primaryColor else primaryDisabledColor

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .drawBehind {
                                val centerX = 18.dp.toPx()
                                val dotCenterY = 12.dp.toPx() // Align with Title text center (Box height 24 / 2)
                                
                                // Calculate gap based on symbol size
                                val symbolRadius = if (isCompleted) 14.dp.toPx() // Check icon (24dp + small buffer)
                                                  else if (isActive) 10.dp.toPx() // Active dot (16dp + buffer)
                                                  else 8.dp.toPx() // Inactive dot (12dp + buffer)
                                
                                val gap = 2.dp.toPx() // Additional clean space
                                val totalGap = symbolRadius + gap

                                // Line ABOVE the dot (except for the first item)
                                if (index > 0) {
                                    val lineEndY = dotCenterY - totalGap
                                    if (lineEndY > 0) {
                                        drawLine(
                                            color = prevLineColor,
                                            start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                                            end = androidx.compose.ui.geometry.Offset(centerX, lineEndY),
                                            strokeWidth = 3.dp.toPx()
                                        )
                                    }
                                }

                                // Line BELOW the dot (except for the last item)
                                if (!isLast) {
                                    val lineStartY = dotCenterY + totalGap
                                    if (lineStartY < size.height) {
                                        drawLine(
                                            color = lineColor,
                                            start = androidx.compose.ui.geometry.Offset(centerX, lineStartY),
                                            end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                                            strokeWidth = 3.dp.toPx()
                                        )
                                    }
                                }
                            }
                            .clickable {
                                if (isExpanded) {
                                    expandedExerciseIds.remove(exercise.id)
                                } else {
                                    expandedExerciseIds.clear()
                                    expandedExerciseIds.add(exercise.id)
                                    pendingScrollIndex = index
                                }
                            }
                    ) {
                        // Timeline Column (Just the dot/icon)
                        Box(
                            modifier = Modifier.width(36.dp).height(24.dp), // Height 24 to match title line height
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(if (isActive) 16.dp else 12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) primaryColor 
                                            else outlineColor.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Exercise Info Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = translateExerciseName(exercise.name),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isActive || isExpanded) FontWeight.Black else FontWeight.Bold,
                                        lineHeight = 24.sp
                                    ),
                                    color = when {
                                        isExpanded -> MaterialTheme.colorScheme.primary
                                        isActive -> MaterialTheme.colorScheme.primary
                                        isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f),
                                    maxLines = if (isExpanded) 2 else 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isExpanded) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandLess,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = getQuantityString("sets_x_reps_pl", exercise.targetSets, exercise.targetSets, exercise.targetReps),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (exercise.tempo.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = getTempoLabel(exercise.tempo),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(getMuscleGroupColor(exercise.muscleGroup))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatMuscleGroup(exercise.muscleGroup, exercise.name),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                                        expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow), expandFrom = Alignment.Top),
                                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                                       shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow), shrinkTowards = Alignment.Top)
                            ) {
                                Column {
                                    val (description, _) = getExerciseDetails(exercise.name, exercise.muscleGroup, exercise.targetIntensity)
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (description.isNotBlank()) {
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
