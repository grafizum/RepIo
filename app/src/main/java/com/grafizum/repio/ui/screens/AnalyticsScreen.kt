package com.grafizum.repio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.grafizum.repio.R
import com.grafizum.repio.ui.components.InteractiveVolumeBarChart
import com.grafizum.repio.ui.components.MuscleGroupDonutChart
import com.grafizum.repio.ui.components.StatCard
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: WorkoutViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val loggedSets by viewModel.allLoggedSets.collectAsState()
    val allExercises by viewModel.allRoutineExercises.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Logic for Charts
    var selectedChartDay by remember { mutableStateOf<String?>(null) }
    
    val monShort = stringResource(R.string.monday_short)
    val tueShort = stringResource(R.string.tuesday_short)
    val wedShort = stringResource(R.string.wednesday_short)
    val thuShort = stringResource(R.string.thursday_short)
    val friShort = stringResource(R.string.friday_short)
    val satShort = stringResource(R.string.saturday_short)
    val sunShort = stringResource(R.string.sunday_short)

    val dailyVolumeMap = remember(loggedSets, monShort, tueShort, wedShort, thuShort, friShort, satShort, sunShort) {
        val map = mutableMapOf(
            monShort to 0.0,
            tueShort to 0.0,
            wedShort to 0.0,
            thuShort to 0.0,
            friShort to 0.0,
            satShort to 0.0,
            sunShort to 0.0
        )
        loggedSets.forEach { set ->
            val shortDay = when (set.dayOfWeek.uppercase()) {
                "MONDAY" -> monShort
                "TUESDAY" -> tueShort
                "WEDNESDAY" -> wedShort
                "THURSDAY" -> thuShort
                "FRIDAY" -> friShort
                "SATURDAY" -> satShort
                "SUNDAY" -> sunShort
                else -> monShort
            }
            val vol = set.weightKg * set.reps
            map[shortDay] = (map[shortDay] ?: 0.0) + vol
        }
        map
    }

    val muscleGroupDistribution = remember(loggedSets) {
        val map = mutableMapOf<String, Int>()
        loggedSets.forEach { set ->
            map[set.muscleGroup] = (map[set.muscleGroup] ?: 0) + 1
        }
        map
    }

    val exerciseNames = remember(allExercises, loggedSets) {
        val namesFromRoutine = allExercises.map { it.name }
        val namesFromLogs = loggedSets.map { it.exerciseName }
        (namesFromRoutine + namesFromLogs).distinct().sorted()
    }

    var selectedExerciseName by remember(exerciseNames) {
        mutableStateOf(exerciseNames.firstOrNull() ?: "")
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    val exerciseLogs = remember(loggedSets, selectedExerciseName) {
        loggedSets.filter { it.exerciseName.equals(selectedExerciseName, ignoreCase = true) }
    }

    val maxWeightForExercise = exerciseLogs.maxOfOrNull { it.weightKg } ?: 0.0
    val totalVolumeForExercise = exerciseLogs.sumOf { it.weightKg * it.reps }

    // Logic for Weekly Report
    val totalSets = loggedSets.size
    val totalVolume = loggedSets.sumOf { it.weightKg * it.reps }
    val topMuscleGroup = muscleGroupDistribution.maxByOrNull { it.value }?.key ?: "None"
    val activeDaysCount = remember(loggedSets) {
        loggedSets.map { it.dayOfWeek }.distinct().size
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // COMPACT HEADER
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
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                    text = stringResource(R.string.analytics_reports),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                }
            }

            if (loggedSets.isNotEmpty()) {
                IconButton(onClick = { showClearConfirmDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PERFORMANCE SUMMARY CARD
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.weekly_activity),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = stringResource(R.string.training_consistency_overview),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatMiniItem(stringResource(R.string.active_days), "$activeDaysCount / 5", MaterialTheme.colorScheme.primary)
                            StatMiniItem(stringResource(R.string.top_focus), topMuscleGroup, MaterialTheme.colorScheme.primary)
                            StatMiniItem(stringResource(R.string.total_sets), "$totalSets", MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 2. QUICK STAT CARDS
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = stringResource(R.string.total_volume),
                        value = stringResource(R.string.weight_kg_intl, totalVolume.toInt()),
                        subtitle = stringResource(R.string.weight_moved),
                        icon = Icons.Default.FitnessCenter,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = stringResource(R.string.sets_logged),
                        value = "$totalSets",
                        subtitle = stringResource(R.string.completed_sets),
                        icon = Icons.Default.CheckCircle,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. VOLUME BAR CHART
            item {
                InteractiveVolumeBarChart(
                    dailyData = dailyVolumeMap,
                    selectedKey = selectedChartDay,
                    onKeySelected = { selectedChartDay = it }
                )
            }

            // 4. MUSCLE GROUP DONUT CHART
            item {
                MuscleGroupDonutChart(distribution = muscleGroupDistribution)
            }

            // 5. EXERCISE SPECIFIC STRENGTH TRACKER
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.exercise_progression),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (exerciseNames.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = expandedDropdown,
                                onExpandedChange = { expandedDropdown = !expandedDropdown }
                            ) {
                                OutlinedTextField(
                                    value = com.grafizum.repio.ui.components.translateExerciseName(selectedExerciseName),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.select_exercise)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    exerciseNames.forEach { name ->
                                        DropdownMenuItem(
                                            text = { Text(com.grafizum.repio.ui.components.translateExerciseName(name)) },
                                            onClick = {
                                                selectedExerciseName = name
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.max_weight),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.weight_kg_intl, maxWeightForExercise),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column {
                                Text(
                                    text = stringResource(R.string.total_volume),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.weight_kg_intl, totalVolumeForExercise.toInt()),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.recent_history),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (exerciseLogs.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_sets_logged),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            exerciseLogs.take(5).forEach { set ->
                                val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(set.timestamp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.kg_x_reps, set.weightKg, set.reps),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatMiniItem(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
