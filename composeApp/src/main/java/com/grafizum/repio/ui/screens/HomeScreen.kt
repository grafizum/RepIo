package com.grafizum.repio.ui.screens

import com.grafizum.repio.util.getString
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grafizum.repio.data.*
import com.grafizum.repio.ui.components.*
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek

import androidx.compose.foundation.BorderStroke
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

    var expandedExerciseId by remember { mutableStateOf<Long?>(null) }
    
    val todayStr = getTodayDayOfWeek()
    val todayRoutineDay = routineDays.find { it.dayOfWeek == todayStr }
    val todayExercises = allExercises.filter { it.dayOfWeek == todayStr }

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
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HERO BANNER - WORKOUT NOW PROMINENT TRIGGER
            item {
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
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = getString("todays_workout"),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
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
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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


            // TODAY'S EXERCISES LIST PREVIEW
            item {
                Text(
                    text = getString("todays_schedule"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
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
                items(todayExercises) { exercise ->
                    ExpandableExerciseCard(
                        exercise = exercise,
                        showDescription = true,
                        isExpanded = expandedExerciseId == exercise.id,
                        onExpandedChange = { expanded ->
                            expandedExerciseId = if (expanded) exercise.id else null
                        },
                        showActions = true
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
