package com.grafizum.repio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The level picker, opened by tapping the level on the live card. Lists every
 * level of the exercise's own program with its prescribed weight/variation and
 * the reps of its first stage, so the user can see what each rung means and jump
 * there directly — the fast path next to the card's one-level-at-a-time arrows.
 *
 * Past the end of that program the list keeps going to
 * [com.grafizum.repio.data.ProgressionEngine.PICKER_LEVEL_LIMIT] rows, and those
 * rows are marked and inert: there is no data behind them, so the app prescribes
 * nothing and selecting one is impossible rather than merely discouraged.
 *
 * The dialog is a sized surface rather than a platform AlertDialog (whose ~280dp
 * default was too thin for the row's level + prescription + tag, and whose text
 * slot inset the sides 24dp against 16dp top and bottom). `weight(1f)` keeps the
 * title, the lock row and Done in place while the ladder scrolls between them, and
 * the list opens on the user's current level — with 500 rows, starting at the top
 * would hide the one row that matters most.
 */
@Composable
fun LevelDetailsDialog(
    viewModel: WorkoutViewModel,
    exerciseName: String,
    onDismiss: () -> Unit
) {
    val isDeveloper = LocalDeveloperSkin.current
    val lockEnabled by viewModel.levelLockEnabled.collectAsState()
    // A locked exercise (routine_exercises.locked) pins its level: the rungs
    // render but refuse taps — unlock from the card's lock icon first.
    val exerciseLocked = remember { viewModel.isExerciseLockedNow(exerciseName) }
    var details by remember { mutableStateOf(viewModel.levelDetails(exerciseName)) }
    var canExtend by remember { mutableStateOf(viewModel.canExtendLevelList(exerciseName)) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = details.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
    )

    // Extending rebuilds the whole ladder (up to PICKER_LEVEL_LIMIT rows) and
    // grows it in place. The row building runs OFF the main thread so a slow
    // extend janks nothing, the button is inert while the work is in flight,
    // and — the part that matters perceptually — the button only swaps to a
    // loading state once the work has actually outlived 200ms: a fast extend
    // must not flash a spinner for a frame or two, a slow one must never sit
    // silent.
    val scope = rememberCoroutineScope()
    var isExtending by remember { mutableStateOf(false) }
    var showExtendLoading by remember { mutableStateOf(false) }

    val extend: () -> Unit = {
        if (!isExtending) {
            isExtending = true
            scope.launch {
                val feedbackJob = launch {
                    delay(EXTEND_LOADING_FEEDBACK_DELAY_MS)
                    showExtendLoading = true
                }
                // The prefs write stays on the main thread (cheap, and prefs
                // implementations may be main-thread oriented); the row list
                // is what can take a moment and is built off it.
                viewModel.extendLevelList(exerciseName)
                val (newDetails, newCanExtend) = withContext(Dispatchers.Default) {
                    viewModel.levelDetails(exerciseName) to
                        viewModel.canExtendLevelList(exerciseName)
                }
                feedbackJob.cancel()
                showExtendLoading = false
                details = newDetails
                canExtend = newCanExtend
                isExtending = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
            color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.surface,
            border = if (isDeveloper) BorderStroke(1.dp, Color.White)
                     else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.88f)
        ) {
            // One even 16dp on all four sides, matching the rest of the app's
            // dialogs: the platform's asymmetric 24/16 inset showed up as a step
            // in the left edge of every row.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = getString("level_details_title", translateExerciseName(exerciseName)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = getString("level_details_intro"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Every shown level is genuinely selectable: extrapolated
                    // ladders give each row a real prescription, so picking one
                    // switches to it and closes the dialog — one tap, not two.
                    // Locked exercises are the exception: rows gray out and the
                    // tap hint below explains how to get the stepper back.
                    items(details.size, key = { "rung_$it" }) { index ->
                        LevelDetailRowItem(
                            row = details[index],
                            maxSelectableLevel = details.size,
                            enabled = !exerciseLocked
                        ) { level ->
                            viewModel.setExerciseLevel(exerciseName, level)
                            onDismiss()
                        }
                    }

                    item(key = "ladder_hint") {
                        Text(
                            text = getString(
                                if (exerciseLocked) "level_details_locked_hint"
                                else "level_details_tap_hint"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDeveloper) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }

                    // The grower: 50 rows at a time keeps the list scannable.
                    // At 500 the ladder simply ends — no more extensions. While
                    // an extend is still running past 200ms the row shows a
                    // spinner + "Loading…" and cannot be re-tapped.
                    if (canExtend) {
                        item(key = "extend_row") {
                            Surface(
                                onClick = extend,
                                enabled = !isExtending,
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
                                color = if (isDeveloper) Color.Black
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDeveloper) Color.White
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    if (showExtendLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = if (isDeveloper) Color.White
                                                    else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = getString("loading"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (isDeveloper) Color.White
                                                    else MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = getString(
                                                "level_details_extend",
                                                ProgressionEngine.EXTENSION_CHUNK
                                            ),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (isDeveloper) Color.White
                                                    else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getString("level_lock_title"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = getString("level_lock_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (lockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isDeveloper) Color.White
                               else if (lockEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = lockEnabled,
                        onCheckedChange = { viewModel.setLevelLockEnabled(it) }
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = getString("done"),
                        color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}/**
 * One rung of the ladder, drawn graphically: the level label on the left and a
 * weight bar whose fill grows with the level — heavier rungs literally read
 * heavier. Every rung is tappable for a direct jump.
 */
@Composable
private fun LevelDetailRowItem(
    row: WorkoutViewModel.LevelDetailRow,
    maxSelectableLevel: Int,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit
) {
    val isDeveloper = LocalDeveloperSkin.current

    Surface(
        onClick = { onSelect(row.level) },
        enabled = enabled,
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp),
        color = when {
            isDeveloper -> Color.Black
            row.isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp,
            when {
                isDeveloper -> Color.White
                row.isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = getString("adjust_levels_n", row.level),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = when {
                    isDeveloper -> Color.White
                    row.isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.width(76.dp),
                maxLines = 1
            )
            // The graphical weight: fill scales with the rung, the current one
            // glows in full primary while the others stay quiet.
            LevelWeightBar(
                fraction = levelWeightFraction(row.level, maxSelectableLevel),
                active = row.isCurrent,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
            // The real prescription beside the bar: what this rung trains at.
            // The current row keeps its marker instead.
            Text(
                text = when {
                    row.isCurrent -> getString("level_details_current")
                    row.prescribed != null -> prescriptionDisplayText(row.prescribed)
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isDeveloper -> Color.White
                    row.isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * How long an extend may run silently before the button swaps to its loading
 * state. Under it, showing feedback would only flash; over it, silence reads
 * as a dead tap.
 */
private const val EXTEND_LOADING_FEEDBACK_DELAY_MS = 200L

/** The bar behind each rung: a track with a proportional fill. */
@Composable
private fun LevelWeightBar(fraction: Float, active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
        )
    }
}

/** A rung's bar fill, proportional between the ladder's floor and top. */
private fun levelWeightFraction(level: Int, maxLevel: Int): Float {
    if (level <= 1) return 0.08f
    val span = (maxLevel - 1).coerceAtLeast(1)
    return (0.08f + 0.92f * (level - 1).toFloat() / span).coerceAtMost(1f)
}
