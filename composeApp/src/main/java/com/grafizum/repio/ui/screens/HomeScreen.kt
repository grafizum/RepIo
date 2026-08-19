package com.grafizum.repio.ui.screens

import com.grafizum.repio.util.getString
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grafizum.repio.data.*
import com.grafizum.repio.ui.components.*
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek

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

    val todayStr = getTodayDayOfWeek()
    val todayRoutineDay = routineDays.find { it.dayOfWeek == todayStr }
    val todayExercises = allExercises.filter { it.dayOfWeek == todayStr }
    
    val todayLoggedSets = remember(loggedSets, todayStr) {
        loggedSets.filter { it.dayOfWeek == todayStr }
    }

    val expandedExerciseIds = remember { mutableStateListOf<Long>() }

    val totalSetsThisWeek = loggedSets.size
    val totalVolumeThisWeek = loggedSets.sumOf { 
        if (com.grafizum.repio.data.WorkoutTemplates.isExerciseTimeBased(it.exerciseName)) 0.0 else it.weightKg * it.reps 
    }
    val isDeveloper = LocalDeveloperSkin.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
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
                                    getString("exercises_scheduled", todayExercises.size)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = getString("total_volume"),
                        value = getString("weight_kg_intl", totalVolumeThisWeek.toInt()),
                        subtitle = getString("all_time_volume"),
                        icon = Icons.Default.FitnessCenter,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = getString("sets_logged"),
                        value = "$totalSetsThisWeek",
                        subtitle = getString("completed_sets"),
                        icon = Icons.Default.CheckCircle,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // TODAY'S EXERCISES LIST PREVIEW
            item {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = getString("todays_schedule"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    val isAllExpanded = todayExercises.isNotEmpty() && expandedExerciseIds.size == todayExercises.size
                    Surface(
                        onClick = {
                            if (isAllExpanded) {
                                expandedExerciseIds.clear()
                            } else {
                                expandedExerciseIds.clear()
                                expandedExerciseIds.addAll(todayExercises.map { it.id })
                            }
                        },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 2.dp, end = 4.dp)
                    ) {
                        Text(
                            text = if (isAllExpanded) getString("collapse_all") else getString("expand_all"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                val nextUpIndex = todayExercises.indexOfFirst { ex ->
                    todayLoggedSets.filter { it.exerciseName == ex.name }.size < ex.targetSets
                }

                itemsIndexed(todayExercises, key = { _, ex -> ex.id }) { index, exercise ->
                    val isLast = index == todayExercises.size - 1
                    val setsForThisExercise = todayLoggedSets.filter { it.exerciseName == exercise.name }
                    val isCompleted = setsForThisExercise.size >= exercise.targetSets
                    val isExpanded = expandedExerciseIds.contains(exercise.id)
                    val isActive = index == nextUpIndex

                    val lineColor = if (isCompleted) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .drawBehind {
                                if (!isLast) {
                                    val centerX = 16.dp.toPx()
                                    val startY = 12.dp.toPx() // Middle of 24.dp bullet Box
                                    drawLine(
                                        color = lineColor,
                                        start = androidx.compose.ui.geometry.Offset(centerX, startY),
                                        end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isExpanded) {
                                    expandedExerciseIds.remove(exercise.id)
                                } else {
                                    if (expandedExerciseIds.size <= 1) {
                                        expandedExerciseIds.clear()
                                    }
                                    expandedExerciseIds.add(exercise.id)
                                }
                            }
                    ) {
                        // Timeline Column (Just the dot/icon)
                        Box(
                            modifier = Modifier.width(32.dp).height(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(if (isActive) 14.dp else 10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Exercise Info Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = translateExerciseName(exercise.name),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isActive || isExpanded) FontWeight.Black else FontWeight.Bold
                                    ),
                                    color = when {
                                        isExpanded -> MaterialTheme.colorScheme.primary
                                        isActive -> MaterialTheme.colorScheme.primary
                                        isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isExpanded) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandLess,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = getString("sets_x_reps", exercise.targetSets, exercise.targetReps),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(getMuscleGroupColor(exercise.muscleGroup))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = translateMuscleGroup(exercise.muscleGroup),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                                    val (description, equipment) = getExerciseDetails(exercise.name, exercise.muscleGroup, exercise.targetIntensity)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (description.isNotBlank()) {
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (equipment.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = equipment,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
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
