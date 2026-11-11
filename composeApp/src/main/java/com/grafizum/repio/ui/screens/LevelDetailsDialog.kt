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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.platform.LocalConfiguration
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
 * the list opens on the user's current level with a couple of rungs above it kept
 * visible — with 500 rows, starting at the top would hide the one row that matters
 * most, but opening exactly ON it hid the levels BELOW the start, which is where a
 * user goes when the prescribed weight is too heavy.
 */
@Composable
fun LevelDetailsDialog(
    viewModel: WorkoutViewModel,
    exerciseName: String,
    onDismiss: () -> Unit,
    /**
     * True when this picker was opened FROM AN EXERCISE EDITOR, which is an edit
     * pass in itself: the exercise editor now carries its own level control, so
     * a level chosen here has to land exactly as it would on the card.
     *
     * Without it the rungs would refuse every tap on a committed exercise, and
     * the editor's "change the level" would be a door to a list that cannot be
     * used — the "greyed field swallows the tap" failure this project has paid
     * for twice. The writer ([WorkoutViewModel.setExerciseLevelFromEditor]) skips
     * the lock GATE for exactly that reason, and it does not CLEAR the pin either,
     * so an exercise the user locked stays locked through an edit
     * (owner, 2026-10-31: never auto unlock).
     */
    fromEditorEditPass: Boolean = false
) {
    val isDeveloper = LocalDeveloperSkin.current
    // A locked exercise (routine_exercises.locked) pins its level, and the sheet opened from
    // the CARD is now INFORMATION ONLY (owner, 2026-10-08: "when clicking on the level card
    // show only informations dont allow to change anything"). Changing a level has exactly one
    // home - the exercise editor, behind the card's pencil - so this sheet never writes one
    // unless it was opened FROM that editor, where the user is already in an edit context.
    //
    // That also fixed a real mismatch: the sheet's lock switch wrote the GLOBAL
    // `level_lock_enabled` pref while its label read like the exercise's own pin. The
    // exercise's own lock is now a control on the card, and the global pin lives in
    // Settings, so neither is disguised as the other.
    val rungsSelectable = fromEditorEditPass
    var details by remember { mutableStateOf(viewModel.levelDetails(exerciseName)) }
    var canExtend by remember { mutableStateOf(viewModel.canExtendLevelList(exerciseName)) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = levelPickerInitialIndex(
            currentIndex = details.indexOfFirst { it.isCurrent },
            totalRows = details.size
        )
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
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            // THE SHEET IS AS TALL AS ITS LADDER, UP TO A CAP.
            //
            // It used to be `fillMaxHeight(0.88f)` unconditionally: an exercise with six
            // rungs - Superman's whole program is six - opened a sheet that was 88% of the
            // screen tall holding six rows and then a wall of empty surface. Owner, 2026-10-09:
            // *"adapt the size of the level card if there is not many levels to show a less
            // high tall card popup"*.
            //
            // So the height is a MAXIMUM rather than a size, and the ladder is allowed to be
            // shorter than the space left for it. `fill = false` on the list's weight is the
            // half that does the work: a plain `weight(1f)` is measured against the remaining
            // space and always takes ALL of it, which is where the empty surface came from -
            // with fill off, the list takes only the height its rows need and the sheet wraps
            // them. A long ladder (500 rows after extending) still stops at the cap and scrolls
            // inside it, so nothing about the tall case changed.
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * SHEET_MAX_HEIGHT_FRACTION)
        ) {
            // One even 16dp on all four sides, matching the rest of the app's
            // dialogs: the platform's asymmetric 24/16 inset showed up as a step
            // in the left edge of every row.
            // fillMaxWidth, NOT fillMaxSize: the Column used to be told to fill the sheet,
            // so the sheet always reached its cap whatever the ladder held - the empty
            // surface under a six-rung list that the owner asked to stop drawing. Wrapping the
            // height is what lets the surface be as tall as its content and no taller; the
            // list inside is still bounded by the cap and scrolls when a ladder is long.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = getString("level_details_title", translateExerciseName(exerciseName)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = getString(
                        if (fromEditorEditPass) "level_details_intro"
                        else "level_details_intro_info"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )


                // THE ONE THING THE LADDER CANNOT SAY BY BEING A LADDER: that it walks
                // itself. Every row below shows where a rung sits and what it asks for,
                // which leaves the obvious question - do I have to come back here and
                // pick the next one? - unanswered. So it is answered the same way the rest
                // of the system is explained, as a picture: the finished sets, then the
                // chip the exercise becomes. Owner, 2026-10-09: *"the app should also
                // explain that the levels increase automatically after finishing that
                // exercise (show that graphically the same as the other informations for
                // the level panel)"*.
                //
                // It sits ABOVE the ladder rather than below it for the same reason the
                // intro is above it: it is about the ladder as a whole, and the ladder is
                // a scroller - a caption parked at the end of it is a caption nobody
                // scrolls to. It is also the honest place for it, because it is read
                // BEFORE the rows rather than after the user has already wondered.
                details.firstOrNull { it.isCurrent }?.let { current ->
                    LevelAutoAdvanceBlock(
                        currentLevel = current.level,
                        setCount = current.stageReps.size,
                        nextLevel = autoLevelTarget(current.level, details.map { it.level })
                    )
                }

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    // `fill = false`: the ladder is as tall as its rows, never padded out to
                    // the sheet's cap. See the note on the surface above.
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
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
                            enabled = rungsSelectable
                        ) { level ->
                            if (fromEditorEditPass) {
                                viewModel.setExerciseLevelFromEditor(exerciseName, level)
                            } else {
                                viewModel.setExerciseLevel(exerciseName, level)
                            }
                            onDismiss()
                        }
                    }

                    item(key = "ladder_hint") {
                        Text(
                            text = getString(
                                if (rungsSelectable) "level_details_tap_hint"
                                else "level_details_hint_info"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }

                    // The grower: 50 rows at a time keeps the list scannable.
                    // At 500 the ladder simply ends — no more extensions. While
                    // an extend is still running past 200ms the row shows a
                    // spinner + "Loading…" and cannot be re-tapped.
                    //
                    // HIDDEN unless the sheet can write. Extending the ladder is a
                    // stored change (it grows the persisted level list), and this
                    // sheet opened from the CARD promises information only - the
                    // owner's "when clicking on the level card show only informations
                    // dont allow to change anything". Leaving the button in made the
                    // one control in an otherwise read-only sheet the one control
                    // that edited something.
                    if (canExtend && rungsSelectable) {
                        item(key = "extend_row") {
                            Surface(
                                onClick = extend,
                                enabled = !isExtending,
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
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
                                            color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = getString("loading"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary)
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
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                // NO lock control here any more. The owner asked for it out of this popup and
                // onto the card (2026-10-08), where the exercise it locks is the exercise in
                // front of the user; [LevelPrescriptionCard] carries it as a label + icon.
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = getString("done"),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}/**
 * How many rungs ABOVE the current one stay on screen when the picker opens.
 *
 * Every weighted exercise in the pool starts at level 4 of its own ladder (the
 * CSV's Medium rung), so opening the list exactly on the current rung put level 4
 * at the top of the screen and pushed levels 1–3 above the fold. The owner's
 * report — "exercises below level 4 (starting level) are not even shown" — is
 * this: the rungs were in the list, just scrolled out of sight, which reads
 * exactly like their being absent.
 *
 * Two is the lead-in that makes the ladder read as a ladder: enough to show there
 * is something above the current rung, without turning the dialog into a scroller
 * whose position has to be re-found every time.
 */
private const val LEVEL_PICKER_LEAD_IN_ROWS = 2

/**
 * The tallest the level sheet may grow: the fraction of the screen it has always
 * been allowed to occupy.
 *
 * Kept as the CAP rather than the size, because the sheet now wraps a short ladder
 * (owner, 2026-10-09: *"adapt the size of the level card if there is not many levels
 * to show a less high tall card popup"*). 0.88 is not a new number: it is what this
 * sheet used to be pinned to on every ladder, long or short.
 */
private const val SHEET_MAX_HEIGHT_FRACTION = 0.88f

/**
 * The row the picker opens on: the current rung, kept comfortably inside the
 * viewport rather than pinned to its top edge. Public so the behaviour is
 * testable without a composition.
 */
fun levelPickerInitialIndex(currentIndex: Int, totalRows: Int): Int {
    if (totalRows <= 0) return 0
    val current = currentIndex.coerceIn(0, totalRows - 1)
    return (current - LEVEL_PICKER_LEAD_IN_ROWS).coerceIn(0, totalRows - 1)
}

/**
 * One rung of the ladder, drawn graphically: the level label on the left and a
 * weight bar whose fill grows with the level — heavier rungs literally read
 * heavier. Every rung is tappable for a direct jump.
 *
 * [enabled] is the SELECTABILITY of the row, not its legibility. When the sheet
 * is opened from the card it is information only, so the rows answer no taps — but
 * they must still read as the rows they are. It used to pass Material's own
 * `enabled`, which dims the entire content to a disabled alpha: a read-only sheet
 * would have greyed the whole ladder out, and "the graph is grey" is this project's
 * recurring way of saying something is broken rather than merely refused. So a
 * non-selectable row is drawn as a PLAIN surface — same colours, same border, no
 * click surface at all, so it cannot even ripple a promise it will not keep.
 */
@Composable
private fun LevelDetailRowItem(
    row: WorkoutViewModel.LevelDetailRow,
    maxSelectableLevel: Int,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val color = when {
        row.isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val border = BorderStroke(
        1.dp,
        when {
            row.isCurrent -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        }
    )
    val contentColor = when {
        row.isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    if (enabled) {
        Surface(
            onClick = { onSelect(row.level) },
            shape = shape,
            color = color,
            contentColor = contentColor,
            border = border,
            modifier = Modifier.fillMaxWidth()
        ) {
            LevelRowContent(row, maxSelectableLevel)
        }
    } else {
        Surface(
            shape = shape,
            color = color,
            contentColor = contentColor,
            border = border,
            modifier = Modifier.fillMaxWidth()
        ) {
            LevelRowContent(row, maxSelectableLevel)
        }
    }
}

/** The inside of one rung — identical whether the row can be tapped or not. */
@Composable
private fun LevelRowContent(
    row: WorkoutViewModel.LevelDetailRow,
    maxSelectableLevel: Int
) {
    val isDeveloper = LocalDeveloperSkin.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
                text = getString("adjust_levels_n", row.level),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = when {
                    row.isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                // widthIn(min), not width: the column has to hold "Level 12",
                // and at a font scale of 1.8 that label is wider than the 76dp
                // the column was pinned to on the 411dp phone - with the default
                // overflow the numeral was CLIPPED, and a half-printed "Level 1"
                // is not a level at all. As a minimum the column keeps its
                // alignment while every label fits, and the meter beside it -
                // which is a weight(1f) shape, not a number - gives up the
                // pixels instead. Levels 2-9 all measure the same, so the meter
                // still lines up row for row on a ladder of that size.
                modifier = Modifier.widthIn(min = 76.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // The researched max, marked rather than enforced: the rows above it
            // keep climbing, so the user has to be able to see which rung is "max".
            if (row.isMax) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = getString("level_max"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
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
                    // A rep-progressed row (bodyweight/hold/cardio) shows the
                    // REPS it asks for, not a variation name. The variation is
                    // the substitution movement, not the work — "Bicycle
                    // Crunch" told the user nothing about what level 2 asks of
                    // them, which is the whole point of this column.
                    // A TIMED ROW SHOWS ITS SECONDS, not the rep pattern it carries
                    // underneath. `stageReps` is the row's Roman rep pattern, which
                    // exists for a plank and a jumping jack as well - but their
                    // prescription is their DURATION, so printing "8 / 8 / 8 reps"
                    // beside the meter told the user the wrong unit for the very
                    // exercise the level system had just learned to time.
                    row.prescribed?.kind == com.grafizum.repio.data.ProgressionEngine.ValueKind.DURATION ->
                        prescriptionDisplayText(row.prescribed)
                    row.repProgressed && row.stageReps.isNotEmpty() ->
                        row.stageReps.joinToString(" / ") + " " + getString(row.repUnitKey)
                    // A LOADED RUNG CARRIES ITS REPS TOO. The load alone is half of what a
                    // weight level asks for: a clean's rung is "20 kg" AND "2 / 1 / 1 reps",
                    // and printing only the plates left the row's own rep pattern
                    // unreachable from the screen that exists to say what a rung costs
                    // (owner, 2026-10-31: "If the exercise has 2 or 4 reps then it should
                    // show the same amount in the level list"). Same one source as the card -
                    // [com.grafizum.repio.data.ProgressionEngine.prescribedReps] - so the two
                    // surfaces cannot describe one rung two ways.
                    row.prescribed?.kind == com.grafizum.repio.data.ProgressionEngine.ValueKind.LOAD &&
                        row.stageReps.isNotEmpty() ->
                        prescriptionDisplayText(row.prescribed) + " · " +
                            row.stageReps.joinToString(" / ") + " " + getString(row.repUnitKey)
                    row.prescribed != null -> prescriptionDisplayText(row.prescribed)
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    row.isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

/**
 * The rung this exercise moves to BY ITSELF once its sets are met, or null when
 * there is nothing above it to move to.
 *
 * Null is not a failure here, it is the top of the ladder: the picker lists
 * rungs from 1 up to [com.grafizum.repio.data.ProgressionEngine.effectiveMaxLevel]
 * — the exercise's own humane ceiling, the same number
 * [com.grafizum.repio.data.ProgressionEngine.levelUp] refuses past — so "is the
 * next level in this list" is the engine's own answer to "will finishing this
 * move it up", read from the data the panel is already showing rather than
 * guessed from a level count.
 *
 * WHICH IS THE POINT OF IT BEING A FUNCTION. The alternative — drawing the arrow
 * and printing `current + 1` unconditionally — promises a level-up at the top of
 * a ladder where the engine returns null and the mastery flow takes over, which
 * is the one state where the graphic would be a lie. Pure, so
 * [com.grafizum.repio.LevelDetailsAutoAdvanceTest] can pin that state without a
 * composition.
 */
fun autoLevelTarget(current: Int?, ladder: List<Int>): Int? {
    val from = current ?: return null
    val next = from + 1
    return if (ladder.contains(next)) next else null
}

/** The tick count used when the pattern carries no rep row to count sets in. */
private const val DEFAULT_TICK_COUNT = 3

/**
 * How many set ticks the graphic will draw at most.
 *
 * A pattern row is three values wide in this pool and the card pads toward the
 * routine's own set count, so this is headroom rather than a real case — but it
 * is headroom on a fixed-width row, and an unbounded `repeat` here would push the
 * chip off the edge of a narrow phone instead of merely looking wrong.
 */
private const val MAX_TICKS = 5

/** Test handle for the block: the panel's explanation that the ladder walks itself. */
const val LEVEL_AUTO_BLOCK_TAG = "level_auto_advance"

/** Test handle for one finished-set tick inside it. */
const val LEVEL_AUTO_TICK_TAG = "level_auto_set_tick"

/**
 * The panel's picture of how a level is earned: the sets, then the chip.
 *
 * Owner, 2026-10-09: *"the app should also explain that the levels increase
 * automatically after finishing that exercise (show that graphically the same as
 * the other informations for the level panel)"*. The "other informations" in
 * this sheet are read at a glance — a rung is a level chip, a bar and its
 * prescription — so this says its piece the same way instead of as a paragraph: a
 * tick per working set (the thing the user actually does), an arrow ("and then"),
 * and the level chip this exercise becomes, drawn from
 * [levelPillStyle] so it is literally the same pill as the one on the card rather
 * than a second design of it.
 *
 * [setCount] comes from the pattern's own rep row, so the number of ticks is the
 * number of sets this exercise is prescribed — not a decorative three.
 * [nextLevel] is null at the top of the ladder, where the same picture is drawn
 * without the arrow and [currentLevel] takes the chip's place: finishing it there
 * genuinely keeps it where it is, and that is worth saying rather than showing an
 * ascent that cannot happen.
 */
@Composable
fun LevelAutoAdvanceBlock(
    currentLevel: Int,
    setCount: Int,
    nextLevel: Int?,
    modifier: Modifier = Modifier
) {
    val pillStyle = levelPillStyle(editing = false, progressionAvailable = false)
    val ticks = setCount.coerceIn(1, MAX_TICKS)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .semantics { testTag = LEVEL_AUTO_BLOCK_TAG }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = getString("level_details_auto_title"),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // One tick per prescribed set: the work, and the only thing the user
                // does. Filled rather than outlined so "finished" is what it reads as.
                repeat(ticks) { index ->
                    if (index > 0) Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .semantics { testTag = LEVEL_AUTO_TICK_TAG }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(14.dp)
                )

                // The chip this exercise becomes - the card's own pill, and its own chip
                // reading ("Level 5"), so the panel teaches the shape the user will see
                // on the card after they finish.
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(
                        pillStyle.borderWidthDp.dp,
                        MaterialTheme.colorScheme.primary
                    ),
                    shadowElevation = pillStyle.shadowElevationDp.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = pillStyle.horizontalPaddingDp.dp,
                            vertical = pillStyle.verticalPaddingDp.dp
                        )
                    ) {
                        Text(
                            text = getString("level_word_n", nextLevel ?: currentLevel),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        if (nextLevel != null) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                if (nextLevel != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = getString("level_details_auto_arrow"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = getString(
                    if (nextLevel == null) "level_details_auto_top_caption"
                    else "level_details_auto_caption"
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
