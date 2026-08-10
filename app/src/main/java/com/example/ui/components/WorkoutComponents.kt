package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import com.example.data.RoutineExerciseEntity
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightLime
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.NeonCyan

// Color mappings per muscle group
fun getMuscleGroupColor(muscleGroup: String): Color {
    return when (muscleGroup.lowercase().trim()) {
        "back" -> Color(0xFF3B82F6) // Blue
        "biceps" -> Color(0xFF8B5CF6) // Purple
        "shoulders" -> Color(0xFFEC4899) // Pink
        "chest" -> Color(0xFFEF4444) // Red
        "legs" -> Color(0xFF10B981) // Emerald
        "abs" -> Color(0xFFF59E0B) // Amber
        "triceps" -> Color(0xFF06B6D4) // Cyan
        else -> Color(0xFF64748B) // Slate
    }
}

@Composable
fun MuscleGroupBadge(
    muscleGroup: String,
    modifier: Modifier = Modifier
) {
    val color = getMuscleGroupColor(muscleGroup)
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = muscleGroup,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ExerciseVideoLinkButton(
    videoUrl: String,
    onSaveDemoUrl: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showInputDialog by remember { mutableStateOf(false) }
    var inputUrlText by remember { mutableStateOf(videoUrl) }

    if (videoUrl.isNotBlank()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val formattedUrl = if (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) {
                            "https://$videoUrl"
                        } else {
                            videoUrl
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Invalid link URL", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = modifier.semantics { testTag = "watch_demo_button" }
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Watch Demo Video",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Watch Demo",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (onSaveDemoUrl != null) {
                IconButton(
                    onClick = {
                        inputUrlText = videoUrl
                        showInputDialog = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Demo Link",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    } else {
        Button(
            onClick = {
                inputUrlText = ""
                showInputDialog = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.semantics { testTag = "input_demo_button" }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Input Demo Video",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Input Demo",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    if (showInputDialog && onSaveDemoUrl != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("Demo Video Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a video URL for this exercise demo:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = inputUrlText,
                        onValueChange = { inputUrlText = it },
                        label = { Text("Video URL") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveDemoUrl(inputUrlText.trim())
                        showInputDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Interactive Weekly Volume / Sets Bar Chart
 */
@Composable
fun InteractiveVolumeBarChart(
    dailyData: Map<String, Double>, // "Mon" -> volume
    selectedKey: String?,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val maxVal = (dailyData.values.maxOrNull() ?: 100.0).coerceAtLeast(100.0)

    val primaryColor = MaterialTheme.colorScheme.primary
    val highlightColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Volume Progress (kg)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (selectedKey != null && dailyData.containsKey(selectedKey)) {
                            "$selectedKey Volume: ${dailyData[selectedKey]?.toInt()} kg"
                        } else {
                            "Tap any bar to inspect daily total"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedKey != null) highlightColor else labelColor
                    )
                }
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val barWidth = size.width / keys.size
                                val index = (offset.x / barWidth).toInt().coerceIn(0, keys.size - 1)
                                onKeySelected(keys[index])
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height - 30.dp.toPx() // Reserve space for labels
                    val barWidth = (width / keys.size) * 0.5f

                    // Draw subtle grid lines
                    for (i in 1..3) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = labelColor.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    keys.forEachIndexed { index, key ->
                        val value = dailyData[key] ?: 0.0
                        val barHeight = ((value / maxVal) * height).toFloat()
                        val xCenter = (width / keys.size) * (index + 0.5f)
                        val xLeft = xCenter - (barWidth / 2f)
                        val yTop = height - barHeight

                        val isSelected = key == selectedKey
                        val gradient = if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(highlightColor, highlightColor.copy(alpha = 0.7f))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.5f))
                            )
                        }

                        // Draw Bar
                        if (barHeight > 0) {
                            drawRoundRect(
                                brush = gradient,
                                topLeft = Offset(xLeft, yTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                            )
                        } else {
                            // Empty bar indicator
                            drawRoundRect(
                                color = labelColor.copy(alpha = 0.15f),
                                topLeft = Offset(xLeft, height - 8.dp.toPx()),
                                size = Size(barWidth, 8.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                // X-Axis labels
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(top = 4.dp)
                ) {
                    keys.forEach { key ->
                        Text(
                            text = key,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (key == selectedKey) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (key == selectedKey) highlightColor else labelColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Donut Chart for Muscle Group Distribution
 */
@Composable
fun MuscleGroupDonutChart(
    distribution: Map<String, Int>, // "Back" -> 12 sets
    modifier: Modifier = Modifier
) {
    val total = distribution.values.sum().coerceAtLeast(1)
    val sortedItems = distribution.entries.sortedByDescending { it.value }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Muscle Group Focus",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (distribution.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sets logged yet. Complete workouts to see muscle focus breakdown!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val strokeWidth = 24.dp.toPx()

                            sortedItems.forEach { entry ->
                                val sweepAngle = (entry.value.toFloat() / total.toFloat()) * 360f
                                val color = getMuscleGroupColor(entry.key)

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$total",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "Sets",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        sortedItems.take(5).forEach { entry ->
                            val color = getMuscleGroupColor(entry.key)
                            val percentage = ((entry.value.toFloat() / total.toFloat()) * 100).toInt()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entry.key,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Analyzes routine exercises to check if any major body part / muscle group is omitted.
 */
fun getMissingMuscleGroups(exercises: List<RoutineExerciseEntity>): List<String> {
    val presentGroups = exercises.map { it.muscleGroup.lowercase().trim() }.toSet()

    val requiredGroups = listOf(
        "Chest" to listOf("chest"),
        "Back" to listOf("back", "lats", "traps"),
        "Legs" to listOf("legs", "quads", "hamstrings", "calves", "glutes"),
        "Shoulders" to listOf("shoulders", "delts"),
        "Biceps" to listOf("biceps"),
        "Triceps" to listOf("triceps"),
        "Abs / Core" to listOf("abs", "core", "abdominals")
    )

    return requiredGroups.filter { (_, keywords) ->
        keywords.none { keyword -> presentGroups.any { it.contains(keyword) } }
    }.map { it.first }
}

@Composable
fun MissingBodyPartsWarningCard(
    missingParts: List<String>,
    modifier: Modifier = Modifier
) {
    if (missingParts.isEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF10B981).copy(alpha = 0.15f)
            ),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Balanced Workout Routine",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "All major muscle groups (Chest, Back, Legs, Shoulders, Arms, Core) are covered!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            ),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Missing Body Parts Warning",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Tip",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your custom workout has no exercises for: ${missingParts.joinToString(", ")}. Adding exercises for these muscle groups will ensure balanced strength development.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
