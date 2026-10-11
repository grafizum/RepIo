package com.grafizum.repio.ui.screens

import androidx.compose.ui.layout.layout
import androidx.compose.ui.window.DialogProperties
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.MeasureResult
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.grafizum.repio.data.DayOfWeekEnum
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.ExerciseVideoLinkButton
import com.grafizum.repio.ui.components.LevelStageLabel
import com.grafizum.repio.ui.components.getMuscleGroupColor
import com.grafizum.repio.ui.components.bandTensionLabel
import com.grafizum.repio.data.WeightUnit
import com.grafizum.repio.ui.components.displayLoadForUnit
import com.grafizum.repio.ui.components.LocalWeightUnit
import com.grafizum.repio.util.NotificationHelper
import com.grafizum.repio.ui.components.loggedLoadSummary
import com.grafizum.repio.ui.components.formatLoadValue
import com.grafizum.repio.ui.components.EvolutionTreeDialog
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.ScreenHeader
import com.grafizum.repio.ui.components.ThemedToast
import com.grafizum.repio.ui.components.MuscleGroupBadge
import com.grafizum.repio.ui.components.translateExerciseName
import com.grafizum.repio.ui.viewmodel.MasteryCardData
import com.grafizum.repio.ui.viewmodel.WorkoutNowState
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.getString
import com.grafizum.repio.util.getQuantityString
import com.grafizum.repio.ui.components.ExerciseEditDialog
import com.grafizum.repio.ui.components.EditorLevelData
import com.grafizum.repio.ui.components.EditorLevelOption
import com.grafizum.repio.ui.components.EditorLevelStage
import kotlinx.coroutines.delay

import com.grafizum.repio.ui.theme.LocalDeveloperSkin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutNowScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onChangeRoutine: (() -> Unit)? = null,
    onAdjustLevels: () -> Unit = {},
    isActive: Boolean = true
) {
    val workoutState by viewModel.workoutNowState.collectAsState()
    val allExercises by viewModel.allRoutineExercises.collectAsState()
    val isDeveloper = LocalDeveloperSkin.current
    val focusManager = LocalFocusManager.current
    val window = LocalWindowSize.current

    var showRestartDialog by remember { mutableStateOf(false) }
    // Exercise whose level-details dialog is open (name, or null). Opened by
    // tapping the level in the card's header — the full ladder plus the picker
    // that jumps straight to a level.
    var levelDetailsFor by remember { mutableStateOf<String?>(null) }
    var editingExercise by remember { mutableStateOf<RoutineExerciseEntity?>(null) }

    // The evolution popup. A QUEUE of whole trees rather than a pair of names,
    // so three exercises finishing their ladders in one session are three
    // decisions rather than three stacked modals or two silent drops. Each item
    // carries the mastered card plus EVERY branch it offers, because the popup
    // draws a fork and a one-branch list cannot show one.
    var evolutionQueue by remember {
        mutableStateOf<List<com.grafizum.repio.ui.viewmodel.EvolutionTreeItem>>(emptyList())
    }
    // The exercises this pass has ALREADY put in front of the user, whether they
    // accepted, declined or dismissed. Without it a re-published
    // `evolutionTrees` queues the same exercise again — the owner's "earlier i
    // had 2 popups to progress the same exercise". See [seedEvolutionQueue].
    var evolutionShown by remember { mutableStateOf<Set<String>>(emptySet()) }
    // One place retires a popup, whatever ended it: drop it from the queue AND
    // remember that it was shown, so a later re-publish cannot queue it again.
    val markEvolutionShown: (String) -> Unit = { name ->
        evolutionShown = evolutionShown + name
        evolutionQueue = evolutionQueue.drop(1)
    }
    val allAvailableExercises by viewModel.allAvailableExercises.collectAsState()
    val themeKey by viewModel.themeOption.collectAsState()
    // Why the last press of the level pill did not open the ladder. Null = no
    // notice showing; see the pill's onBlockedTap for the two reasons.
    var blockedLevelNotice by remember { mutableStateOf<String?>(null) }
    // Resolved here, not inside the tap lambda: that lambda is invoked from the
    // Surface's click handler, outside composition, and getString is @Composable.
    val pencilBlockedHint = getString("level_pencil_hint")
    val customBlockedHint = getString("level_fields_locked_toast")

    val context = LocalContext.current
    val hostActivity = context as? Activity
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted
        // On Android 13+ a second denial is FINAL: the launcher returns false
        // WITHOUT showing the system dialog, so the tap the user just made looks
        // like it did nothing. If the permission is still refused and the system
        // will no longer offer its prompt, send them to the one screen that can
        // still grant it. (Rationale is true for an ordinary first refusal, so
        // this does not short-circuit a user who can simply try again.)
        if (!isGranted && hostActivity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                hostActivity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            NotificationHelper.openNotificationSettings(context)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset edit state when tab becomes inactive
    LaunchedEffect(isActive) {
        if (!isActive) {
            editingExercise = null
            showRestartDialog = false
        }
    }

    val currentDayExercises = remember(allExercises, workoutState.selectedDayOfWeek) {
        allExercises.filter { it.dayOfWeek == workoutState.selectedDayOfWeek }
    }

    // The evolution popup: "here is where you are, here is what it becomes".
    // Accepting re-levels the target; declining LOCKS the exercise at its cap
    // and nothing re-offers it except the editor.
    evolutionQueue.firstOrNull()?.let { tree ->
        EvolutionTreeDialog(
            card = tree.card,
            offers = tree.offers,
            onEvolve = { target ->
                viewModel.acceptEvolution(tree.card.exerciseName, target)
                markEvolutionShown(tree.card.exerciseName)
            },
            onDecline = {
                viewModel.declineEvolution(tree.card.exerciseName)
                markEvolutionShown(tree.card.exerciseName)
            },
            onDismiss = { markEvolutionShown(tree.card.exerciseName) },
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("restart_workout_session_title"))
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
                    Text(getString("restart_workout_session_text"))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.resetWorkoutSession()
                        showRestartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(getString("restart"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showRestartDialog = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
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
            // ONE SCROLL REGION AT A LARGE TEXT SCALE.
            //
            // The frame now tracks the user's font scale exactly (see frameScaleFor), and
            // the header, the day wheel, the progress row and the action button all scale
            // with it. On a 411x914dp phone at font scale 1.8 that fixed chrome is taller
            // than the window itself, so ANY layout that pins it whole leaves the middle of
            // the screen at roughly nothing - measured, that is how the exercise name and
            // its level card ended up behind the fold the last time this was tried.
            //
            // So at a large text scale the whole page scrolls and nothing is pinned except
            // the navigation bar: the content cannot be starved, and every gap on screen
            // keeps the same proportion to its letters as it does on a font-1.0 phone.
            // (Below the threshold nothing changes: the wheel and progress stay put and
            // only the exercise list scrolls, which is what the layout was designed for.)
            .then(
                if (window.isLargeText) Modifier.verticalScroll(rememberScrollState()) else Modifier
            )
    ) {
        Spacer(modifier = Modifier.height(window.sectionSpacing))

        // COMPACT HEADER — the day title and the "Workout" action share this row. ScreenHeader
        // keeps the action at its intrinsic size while the title is capped with an ellipsis
        // inside the leftover width, so long day names (DE/IT) can no longer squeeze the
        // button or clip mid-glyph on narrow phones (e.g. Pixel 8). No subtitle: the
        // per-exercise muscle badges already say what the day trains.
        //
        // The title is the FULL day name. It used to fall back to the two-letter wheel
        // label ("Do") when it could not fit, and the owner took that back on 2026-10-08:
        // "change the day back from the short version Do to the long one donnerstag but cap
        // it and write ... next to it". One row is still the invariant - a wrapped
        // "Donnerstag" pushed the header's actions down - so the cap is the ellipsis
        // ScreenHeader draws, not a second line.
        ScreenHeader(
            title = dayOfWeekDisplayName(workoutState.selectedDayOfWeek),
            icon = Icons.Default.FitnessCenter,
            trailing = {
                // Both header actions are siblings in one Row, so the gap between
                // them is structural (spacedBy) rather than a start-padding bolted
                // onto one of them — that is what keeps "Levels" clear of
                // "Workout" in every locale and at every font scale.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(window.headerIconSpacing)
                ) {
                    // "Levels": re-opens the guide as a compact popup so the
                    // system stays explainable at any time.
                    HeaderActionPill(
                        label = getString("levels_menu_title"),
                        icon = Icons.Default.FormatListNumbered,
                        onClick = onAdjustLevels
                    )
                    if (onChangeRoutine != null) {
                        HeaderActionPill(
                            label = getString("workout"),
                            icon = Icons.Default.Edit,
                            onClick = onChangeRoutine,
                            modifier = Modifier.semantics { testTag = "change_workout_button" }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(window.sectionSpacing))

        val today = remember { getTodayDayOfWeek() }
        com.grafizum.repio.ui.components.WorkoutDayWheel(
            selectedDay = workoutState.selectedDayOfWeek,
            onDaySelected = { viewModel.selectDayForWorkoutNow(it) },                            today = today
        )

        Spacer(modifier = Modifier.height(window.sectionSpacing))

        Box(
            modifier = if (window.isLargeText) {
                // Part of the page: `weight` cannot resolve against unbounded height, and
                // the day content must be free to be its natural size.
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxSize().weight(1f)
            }
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
                val dayExercises = allExercises.filter { it.dayOfWeek == targetDay }

                // Inside the page scroll this column cannot claim the window height (the
                // constraints are unbounded, so `fillMaxSize` would be a no-op anyway): it
                // is simply as tall as the day's content.
                Column(
                    modifier = if (window.isLargeText) Modifier.fillMaxWidth()
                    else Modifier.fillMaxSize()
                ) {
                    if (dayExercises.isEmpty()) {
                        // Empty / Rest Day State
                        Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                // Without weight(1f), Arrangement.Center + scrolling clips
                                // the TOP of over-tall content on short screens.
                                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                            ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = getString("rest_recovery_day"),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = getString("rest_recovery_recommendation", dayOfWeekDisplayName(targetDay)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (workoutState.isWorkoutCompleted && targetDay == workoutState.selectedDayOfWeek) {
                        // WORKOUT COMPLETED CELEBRATION VIEW
                        Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                // Without weight(1f), Arrangement.Center + scrolling clips
                                // the TOP of over-tall content on short screens.
                                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                            ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = getString("workout_completed_title"),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = getQuantityString("workout_completed_text", workoutState.totalSetsLoggedToday, workoutState.totalSetsLoggedToday, dayOfWeekDisplayName(targetDay)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    // The graphical recap. Everything the user
                                    // just did, read off the sets they logged -
                                    // see WorkoutRecap.
                                    workoutState.workoutRecap?.let { recap ->
                                        Spacer(modifier = Modifier.height(window.sectionSpacing))
                                        WorkoutRecapCard(recap = recap)
                                    }
                                    Spacer(modifier = Modifier.height(window.sectionSpacing))

                                    if (targetDay == today) {
                                        Button(
                                            onClick = { viewModel.resetWorkoutSession() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = getString("start_workout_again"),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }

                            // YOUR PROGRESSION OPPORTUNITIES — the evolution
                            // popup opens on its own when the workout completes.
                            // The owner asked for it "as a popup window to show
                            // the user there is something here", and a section
                            // the user has to notice and scroll to is exactly
                            // the thing that does not do that, so the branch
                            // cards are gone from here.
                            LaunchedEffect(workoutState.evolutionToken) {
                                // MERGED, not assigned: the token bumps on every
                                // mastery refresh, and each refresh re-publishes
                                // every pending offer. Assigning the list is what
                                // produced the owner's "2 popups to progress the
                                // same exercise" — the second one arrived from the
                                // refresh the first popup's own Accept kicked off.
                                evolutionQueue = seedEvolutionQueue(
                                    pending = evolutionQueue,
                                    incoming = workoutState.evolutionTrees,
                                    alreadyShown = evolutionShown
                                )
                            }

                            // A TRUE CEILING with no branch has no evolution to
                            // offer, so no popup opens for it — and deleting the
                            // section would silently delete the fact that the
                            // exercise has finished. Those stay as a line.
                            val ceilings = workoutState.masteredExercises.filter { it.branches.isEmpty() }
                            if (ceilings.isNotEmpty()) {
                                MasteryCeilingSummary(
                                    cards = ceilings,
                                    onKeepAtCap = { viewModel.keepExerciseAtCap(it) }
                                )
                            }
                        }
                    } else {
                        // ACTIVE WORKOUT SESSION VIEW
                        val exerciseIndex = workoutState.currentExerciseIndex.coerceIn(0, dayExercises.size - 1)
                        val currentExercise = dayExercises[exerciseIndex]
                        
                        // WHETHER THIS EXERCISE MEASURES IN SECONDS has ONE answer
                        // app-wide: [WorkoutTemplates.isExerciseTimeBased], which asks
                        // the progression engine first and only falls back to the name
                        // for an exercise the pool does not carry.
                        //
                        // The card, the timer button, the view model's auto-selected
                        // seconds, the editor's time toggle and the routine rows all
                        // read that one function, so none of them can disagree about
                        // the exercise. They used to each decide it for themselves by
                        // sniffing the routine's REP STRING (`targetReps.contains("sec")`),
                        // and every timed row built before the level system stores a rep
                        // range instead - so a jumping jack rendered with rep pills
                        // while a timer ran under it (owner, 2026-10-09: "it shows reps
                        // in the card yet the exercise is based on time and has a
                        // timer????"). The routine string is a rendering of the
                        // prescription, not its source.
                        val isTimeBased = WorkoutTemplates.isExerciseTimeBased(currentExercise.name)
                        val totalExercises = dayExercises.size

                        val nextExercise = if (exerciseIndex + 1 < dayExercises.size) {
                            dayExercises[exerciseIndex + 1]
                        } else null

                        // v5 level system: what to do THIS set. Everything shown
                        // comes from WorkoutNowState — the UI never touches the
                        // engine, and internal fields never reach it. Computed once
                        // here because both cards below read it: the exercise card
                        // stays clean (what to do), the LEVEL card under it carries
                        // the prescription (which level, which weight, which reps).
                        val progLevel = workoutState.progressionLevel
                        val progStage = workoutState.progressionStage
                        val prescribed = workoutState.progressionPrescribed
                        // A VARIATION prescription names a substitution movement
                        // ("Bicycle Crunch" standing in for "Crunches"). The level
                        // card does NOT name it: the exercise card directly above
                        // already says which exercise this is, so the line only
                        // repeated it -- and because `rx.variation` is the raw
                        // canonical pool name, it was the one line on the card
                        // that never went through translateExerciseName and so
                        // stayed English on a German phone. Passing null for a
                        // variation also un-suppresses the per-set action banner
                        // (showChips needs primaryText == null), which is the
                        // layout the card is designed around. DURATION still
                        // has to reach the card: "30 sec" is the prescription
                        // for a time-based exercise, not a name.
                        val durationLabel = prescribed?.let { rx ->
                            when (rx.kind) {
                                ProgressionEngine.ValueKind.VARIATION -> null
                                ProgressionEngine.ValueKind.DURATION -> prescriptionDisplayText(rx)
                                ProgressionEngine.ValueKind.LOAD -> null
                            }
                        } ?: if (isTimeBased) {
                            // A TIMED EXERCISE OUTSIDE THE LEVEL SYSTEM still has a
                            // duration on its card. Custom mode has no prescription, and
                            // the card's summary line fell back to the routine's REP
                            // string - the same "its time based and it shows reps" the
                            // owner reported, one mode over. The number printed is the
                            // one the timer button starts, because both read
                            // `currentExerciseSeconds`: the card and the button cannot
                            // promise different numbers.
                            getString("progression_duration_value", workoutState.currentExerciseSeconds)
                        } else null
                        val loadText = when {
                            prescribed?.kind == ProgressionEngine.ValueKind.LOAD ->
                                prescriptionDisplayText(prescribed)
                            workoutState.currentWeightKg > 0.0 ->
                                // The user's own value, in the exercise's own unit —
                                // a stack's pin is not a kilogram.
                                loggedLoadSummary(currentExercise.name, workoutState.currentWeightKg)
                            else -> null
                        }

                        // Pinned Top Progress
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val labelText = getString(
                                    "exercise_x_of_y", exerciseIndex + 1, totalExercises,
                                    workoutState.currentSetNumber, currentExercise.targetSets
                                )
                                
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDeveloper) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (targetDay == today) {
                                    Button(
                                        onClick = { showRestartDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        border = if (isDeveloper) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .heightIn(min = 32.dp)
                                            .semantics { testTag = "restart_workout_button" }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = getString("restart"),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            getString("restart"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }}
                            Spacer(modifier = Modifier.height(6.dp))
                            val progress = ((exerciseIndex.toFloat() + (workoutState.currentSetNumber.toFloat() / currentExercise.targetSets)) / totalExercises.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(BorderStroke(0.dp, Color.Transparent)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable middle section
                        Column(
                            modifier = Modifier
                                .then(if (window.isLargeText) Modifier else Modifier.weight(1f))
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        focusManager.clearFocus()
                                    })
                                }
                                // The page itself scrolls at a large text scale; a second
                                // viewport here would only fight it for the drag.
                                .then(
                                    if (window.isLargeText) Modifier
                                    else Modifier.verticalScroll(viewModel.workoutNowScrollState)
                                ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // TIMER AREA (only shown on the day the timer belongs to)
                            val isTimerActive = workoutState.isTimerActive && workoutState.restTimerSeconds > 0
                            val isExTimer = workoutState.isExerciseTimerActive
                            val isTimerForThisDay = isTimerActive && targetDay == workoutState.timerDayOfWeek

                            if (isTimerForThisDay && !isExTimer) {
                                // Running Rest Timer Card (Only for Rest)
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                val label = if (workoutState.currentSetNumber == 1) {
                                                    getString("exercise_transition_label")
                                                } else {
                                                    getString("rest_timer")
                                                }
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = getString("seconds_remaining", workoutState.restTimerSeconds),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!notificationsEnabled) {
                                                IconButton(
                                                    onClick = {
                                                        // Android 13+ owns this permission through its own
                                                        // dialog, so always try that first. The launcher's
                                                        // callback falls back to the settings page when the
                                                        // system refuses to show the dialog again. Older
                                                        // releases have no runtime permission: there the tap
                                                        // goes straight to the notification settings page.
                                                        val needsRuntimeGrant =
                                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                                ContextCompat.checkSelfPermission(
                                                                    context,
                                                                    Manifest.permission.POST_NOTIFICATIONS
                                                                ) != PackageManager.PERMISSION_GRANTED
                                                        if (needsRuntimeGrant) {
                                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        } else {
                                                            NotificationHelper.openNotificationSettings(context)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsOff,
                                                        contentDescription = getString("allow_notifications"),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { viewModel.stopTimer() }) {
                                                Icon(
                                                    imageVector = Icons.Default.SkipNext,
                                                    contentDescription = "Skip Timer",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // CURRENT EXERCISE CARD — the hero of the screen. Painted in
                            // the same language as the level card below it: the theme's
                            // primary tint over the surface with a hairline primary rim,
                            // so the two cards read as one family instead of a grey box
                            // above a tinted one. Developer skin keeps its flat look.
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(window.cardPadding)) {
                                    var showPeek by remember { mutableStateOf(false) }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Badge shows the broad body part + precise muscle group together.
                                        MuscleGroupBadge(
                                            muscleGroup = currentExercise.muscleGroup,
                                            exerciseName = currentExercise.name
                                        )
                                        

                                        // Card action: peek at the method description.
                                        //
                                        // The EDIT pencil that used to sit here is GONE
                                        // (owner, 2026-10-08): "instead of the pencil icon
                                        // in the top card where the exercise is shown, we
                                        // should only show the pencil in the bottom card
                                        // where sets and reps are". Two pencils on one
                                        // screen meant two doors to the same editor; the
                                        // one on the level card below is now the only
                                        // one, and it sits next to the sets and reps the
                                        // editor is about.
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { showPeek = !showPeek },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (showPeek) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = getString("description"),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(window.sectionSpacing / 2))

                                    val (description, _) = com.grafizum.repio.ui.components.getExerciseDetails(currentExercise.name, currentExercise.muscleGroup, currentExercise.targetIntensity)

                                    // The name leads, full width now that the actions
                                    // live beside the badge: the user has to know WHAT
                                    // they are about to do before a level or a load
                                    // means anything.
                                    Text(
                                        text = com.grafizum.repio.ui.components.translateExerciseName(currentExercise.name),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        // Two lines are always ALLOWED; they are only
                                        // ever used when the text needs them. A second
                                        // line costs nothing at font scale 1.0, where
                                        // this name fits on one, and it is the cheapest
                                        // way for a long name (or a long translation) to
                                        // stay readable instead of being abbreviated.
                                        // NOTE: this is not the label the owner's 1.8x
                                        // screenshot showed cut to "Dumbbell Benc..." -
                                        // that string is painted by another copy of the
                                        // name in the live card, and pinning which of the
                                        // several `translateExerciseName` call sites
                                        // paints it is still open (recorded in TODO.md).
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    AnimatedVisibility(visible = showPeek) {
                                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                                            // Nested well: one shade of the primary IN — the same
                                            // recipe as the level card's banner — so the description
                                            // reads as recessed into the card family.
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                                modifier = Modifier.fillMaxWidth()                                                ) {
                                                Row(modifier = Modifier.padding(window.cardPadding / 2)) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(window.headerIconSpacing / 2))
                                                    Text(
                                                        text = description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(window.sectionSpacing))

                                    // Tools + context row. The demo button renders nothing
                                    // when the exercise has no video link, so the SPACER
                                    // carries the weight here: "next up" stays pinned to the
                                    // trailing edge whether or not a demo button is present,
                                    // instead of drifting to the left when there is none.
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ExerciseVideoLinkButton(
                                            videoUrl = currentExercise.videoUrl,
                                            onSaveDemoUrl = { newUrl ->
                                                viewModel.saveExercise(currentExercise.copy(videoUrl = newUrl))
                                            },
                                            modifier = Modifier.height(44.dp)
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        // "Next up" as a quiet right-aligned caption —
                                        // no chip surface, so the row reads as one
                                        // modern typographic line with the demo button.
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .padding(start = window.screenPadding / 4)
                                                .semantics { testTag = "next_up_exercise_card" }
                                        ) {
                                            Text(
                                                text = getString("next_up"),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(window.sectionSpacing / 4))
                                            Text(
                                                text = nextExercise?.let { com.grafizum.repio.ui.components.translateExerciseName(it.name) }
                                                       ?: getString("next_up_none"),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // LEVEL CARD — the second card under the exercise card.
                            // The prescription lives HERE and only here: which level,
                            // which stage, and the weight and reps to reach, with the
                            // active set's rep bold. The exercise card above stays a
                            // clean "what am I doing" view. Exercises outside the level
                            // system (custom mode, non-ready rows) get the same card
                            // without the level header, showing the routine's own reps.
                            Spacer(modifier = Modifier.height(window.sectionSpacing))
                            LevelPrescriptionCard(
                                exerciseName = currentExercise.name,
                                level = progLevel,
                                stage = progStage,
                                lastStage = viewModel.patternStageCount(currentExercise.name),
                                adjustable = progLevel != null && progStage != null &&
                                    viewModel.isLevelAdjustable(currentExercise.name) &&
                                    !currentExercise.locked,
                                locked = currentExercise.locked,
                                targetSets = currentExercise.targetSets,
                                repTargets = workoutState.progressionStageTargetReps,
                                currentSetNumber = workoutState.currentSetNumber,
                                fallbackReps = currentExercise.targetReps,
                                primaryText = durationLabel,
                                loadText = loadText,
                                isTimeBased = isTimeBased,
                                // The pencil opens the exercise editor — the ONE
                                // screen the level system is edited on (owner,
                                // 2026-10-08). It also replaced the Roman-stage
                                // stepper that used to step the level from here, and
                                // the pencil that used to sit on the exercise card
                                // above: one edit screen, one door into it.
                                onOpenEditor = { editingExercise = currentExercise },
                                onOpenDetails = { levelDetailsFor = currentExercise.name },
                                // The card's own lock control writes the per-exercise pin,
                                // so the control and its consequence are one fact.
                                onToggleLock = {
                                    viewModel.setExerciseLocked(
                                        currentExercise.name,
                                        !currentExercise.locked
                                    )
                                },
                                // The pill LOOKS pressable always, so a press that
                                // cannot open the ladder has to say why rather than
                                // doing nothing.
                                //
                                // The reason is NOT `locked` alone: an exercise
                                // outside the level system has no ladder to open at
                                // all, whatever its lock state. Keying the message
                                // off `locked` would tell a first-time user to
                                // "switch to custom" when the honest answer is
                                // "press the pencil". So: if the level system is
                                // running at all, the way in is the pencil; only a
                                // fully custom mode has the other answer.
                                onBlockedTap = {
                                    blockedLevelNotice =
                                        when (levelPillBlockedReason(
                                            currentExercise.locked,
                                            viewModel.levelSystemEnabled.value
                                        )) {
                                            LevelPillBlocked.NEEDS_EDIT_PASS -> pencilBlockedHint
                                            LevelPillBlocked.NEEDS_CUSTOM_MODE -> customBlockedHint
                                        }
                                },
                                // "Progression available" beside the level. The
                                // owner asked for this label explicitly: the offer
                                // used to exist ONLY as an end-of-workout popup, so
                                // a user mid-session had no way to know one was
                                // waiting. These two come from one question in the
                                // view model - the same one the post-workout card
                                // asks - so the label cannot promise an offer the
                                // workout would not make.
                                progressionAvailable = viewModel.isProgressionAvailable(
                                    currentExercise.name
                                ) && viewModel.hasSeenProgressionOffer(
                                    // The label waits until the offer has
                                    // ACTUALLY been shown. See
                                    // hasSeenProgressionOffer: the pending
                                    // question alone flips true the moment a
                                    // level hits a checkpoint, which is mid
                                    // session and before the workout that will
                                    // offer it has finished.
                                    currentExercise.name
                                ),
                                progressionTarget = viewModel.pendingProgressionTarget(
                                    currentExercise.name
                                )?.let { translateExerciseName(it) }
                            )

                        }

                        // PRIMARY ACTION (PINNED AT BOTTOM)
                        val isSelectedDayToday = targetDay == today
                        val isTimerActive = workoutState.isTimerActive && workoutState.restTimerSeconds > 0
                        val isExTimerRunning = isTimerActive && workoutState.isExerciseTimerActive && targetDay == workoutState.timerDayOfWeek
                        
                        if (isSelectedDayToday) {
                            Spacer(modifier = Modifier.height(16.dp))
                            if (isExTimerRunning) {
                                // COUNTER / SKIP BUTTON (Full Width)
                                Button(
                                    onClick = { viewModel.advanceAfterTimer(dayExercises) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = null,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${workoutState.restTimerSeconds}s",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "(${getString("skip")})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { 
                                        if (isTimeBased) {
                                            viewModel.startExerciseTimerWithLog(dayExercises, workoutState.currentExerciseSeconds)
                                        } else {
                                            viewModel.logCurrentSetAndAdvance(dayExercises) 
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = null,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        val icon = if (isTimeBased) Icons.Default.Timer else Icons.Default.CheckCircle
                                        val text = if (isTimeBased) {
                                            getString("start_timer_x_and_log", workoutState.currentExerciseSeconds)
                                        } else {
                                            getString("log_set_x_next", workoutState.currentSetNumber)
                                        }

                                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(window.headerIconSpacing / 2))
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(window.sectionSpacing))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(window.headerIconSpacing),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.navigateToExercise(workoutState.currentExerciseIndex - 1, dayExercises.size) },
                                    enabled = workoutState.currentExerciseIndex > 0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = null,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(getString("previous"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Button(
                                    onClick = { viewModel.navigateToExercise(workoutState.currentExerciseIndex + 1, dayExercises.size) },
                                    enabled = workoutState.currentExerciseIndex < dayExercises.size - 1,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = null,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    Text(getString("next"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(window.sectionSpacing))
    }

    // WHY-THE-PILL-DID-NOTHING notice. The pill is drawn pressable in every state,
    // so a press while the level is ticked in has to answer for itself instead of
    // being swallowed - the same failure the greyed level fields had.
    //
    // It goes to the SAME app-level top banner as the edit dialog's refusal,
    // because the owner's rule for the level editing is "do this error message
    // design for the level editing as well" - two different presentations for
    // one refusal is the thing that was being asked to go away.
    LaunchedEffect(blockedLevelNotice) {
        blockedLevelNotice?.let { notice ->
            com.grafizum.repio.util.AppEventBus.showTopBanner(notice)
            blockedLevelNotice = null
        }
    }

    // LEVEL DETAILS DIALOG — jump levels with full weight/reps overview.
    levelDetailsFor?.let { name ->
        LevelDetailsDialog(
            viewModel = viewModel,
            exerciseName = name,
            onDismiss = { levelDetailsFor = null }
        )
    }

    // EXERCISE EDIT DIALOG
    editingExercise?.let { exercise ->
        // Touching a level-owned field (sets/reps/weight/intensity) offers the
        // switch to Custom — never silently switches the mode.
        var confirmCustom by remember { mutableStateOf(false) }
        // The ladder opened FROM the editor. Separate from the card's own
        // `levelDetailsFor` because this one runs as an edit pass: the editor
        // is where the user changes an exercise, so the level it offers has to
        // be reachable even for an exercise the card's tick committed.
        var editorLevelLadder by remember { mutableStateOf<String?>(null) }
        ExerciseEditDialog(
            allAvailableExercises = allAvailableExercises,
            exercise = exercise,
            dayOfWeek = exercise.dayOfWeek,
            themeKey = themeKey,
            // The level facts are resolved by NAME inside the dialog, so the same
            // editor serves an edit and an add. See [editorLevelData].
            levelDataFor = { levelName, pageStart -> editorLevelData(viewModel, levelName, pageStart) },
            userEquipmentCategories = viewModel.userEquipmentCategories(),
            userGrantedTags = viewModel.userGrantedEquipmentTags(),
            // Same contract as the Routine editor: declining locks the
            // exercise, and this badge is the only way to reopen the offer.
            evolutionLocked = viewModel.isEvolutionDeclined(exercise.name),
            onUpgrade = {
                // Reopening from the editor: clear the lock, then put the whole
                // fork back on the queue so the same popup opens again.
                //
                // The card is rebuilt from the mastered list rather than passed
                // in, because the queue item needs the card to draw its trunk.
                // A lock can be cleared on an exercise that is no longer in the
                // post-workout list at all (the user reopens it days later), so
                // when no card matches there is simply nothing to reopen - the
                // badge stops offering a door to a popup that cannot be built.
                viewModel.reopenEvolution(exercise.name)
                editingExercise = null
                val card = viewModel.masteryCardFor(exercise.name)
                if (card != null) {
                    val offers = viewModel.evolutionOffersFor(card)
                    if (offers.isNotEmpty()) {
                        // APPENDED, not assigned: this badge is the way back into
                        // an offer the user declined, and a decision already
                        // waiting in the queue must not be thrown away by it.
                        // Deliberately NOT filtered by [evolutionShown] — reopening
                        // from the editor is exactly the case where an exercise
                        // that was shown before has to be shown again.
                        evolutionQueue = (
                            evolutionQueue +
                                com.grafizum.repio.ui.viewmodel.EvolutionTreeItem(card, offers)
                            ).distinctBy { it.card.exerciseName }
                    }
                }
            },
            onRequestCustomMode = { confirmCustom = true },
            // The level itself, editable here in level-system mode (owner,
            // 2026-10-05). The write unlocks the exercise, because a committed
            // lock is what used to make this control silently do nothing.
            onLevelChange = { levelName, level ->
                viewModel.setExerciseLevelFromEditor(levelName, level)
            },
            // A Roman numeral picked under a level: level AND stage in one write,
            // so the choice the user made is the one the card shows next session.
            onStageChange = { levelName, level, stage ->
                viewModel.setExerciseStateFromEditor(levelName, level, stage)
            },
            onOpenLevelLadder = { levelName -> editorLevelLadder = levelName },
            onDismiss = { editingExercise = null },
            onSave = { updated ->
                viewModel.saveExercise(updated)
                editingExercise = null
            }
        )
        if (confirmCustom) {
            AlertDialog(
                onDismissRequest = { confirmCustom = false },
                title = { Text(getString("custom_mode_title")) },
                text = { Text(getString("custom_mode_body")) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setLevelSystemEnabled(false)
                        confirmCustom = false
                    }) { Text(getString("custom_mode_confirm")) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmCustom = false }) {
                        Text(getString("cancel"))
                    }
                }
            )
        }
        // The editor's own ladder, stacked over the editor: the editor stays
        // open behind it, so a level chosen here lands and the user lands back
        // where they were rather than on the workout screen.
        editorLevelLadder?.let { name ->
            LevelDetailsDialog(
                viewModel = viewModel,
                exerciseName = name,
                fromEditorEditPass = true,
                onDismiss = { editorLevelLadder = null }
            )
        }
    }
}

/**
 * Renders an engine [ProgressionEngine.PrescribedValue] as localized UI text:
 * "8 kg each", "12 kg", "Pin 6", "Medium band", "Incline Push-Up", or "30 s".
 */
@Composable
fun prescriptionDisplayText(rx: ProgressionEngine.PrescribedValue): String = when (rx.kind) {
    ProgressionEngine.ValueKind.VARIATION -> rx.variation ?: ""
    ProgressionEngine.ValueKind.DURATION -> getString("progression_duration_value", rx.value?.toInt() ?: 0)
    ProgressionEngine.ValueKind.LOAD -> {
        // Already in the user's unit: the engine builds the ladder in the unit
        // asked for (see ProgressionEngine.poundLadder), so this only writes it
        // down. Converting here as well would convert it twice.
        val unit = LocalWeightUnit.current
        val amount = rx.value ?: 0.0
        val clean = formatLoadValue(amount)
        when (rx.loadUnit) {
            ProgressionEngine.LoadUnit.KG_EACH -> getString(
                if (unit == WeightUnit.KG) "progression_load_each" else "progression_load_lb_each",
                clean
            )
            ProgressionEngine.LoadUnit.KG -> getString(
                if (unit == WeightUnit.KG) "progression_load_kg" else "progression_load_lb",
                clean
            )
            ProgressionEngine.LoadUnit.STACK_STEP -> getString("progression_load_pin", clean)
            ProgressionEngine.LoadUnit.RESISTANCE_STEP -> getString(
                "progression_load_band",
                bandTensionLabel(ProgressionEngine.bandTensionOf(amount))
            )
            null -> ""
        }
    }
}

/**
 * The one line the exercise editor's level row prints under the stepper:
 * "Stage III · 20 kg".
 *
 * It is built from the SAME two sources the live card uses - the stored stage
 * and the engine's prescription for the current rung - so the editor cannot
 * quote a different number from the card for the same level. That matters
 * because the editor's whole point is "this level is too heavy/too light":
 * a caption that disagreed with the card would make the stepper feel broken.
 *
 * It reads the prescription rather than the display fields, so it also answers
 * for a row measured in pins, bands, seconds or reps - the unit bug this
 * project has already paid for once.
 */
@Composable
fun levelEditorCaption(viewModel: WorkoutViewModel, exerciseName: String): String? {
    val state = viewModel.currentProgressionState(exerciseName) ?: return null
    val entry = ProgressionEngine.entryFor(exerciseName) ?: return null
    val stage = getString("stage_current_format", ProgressionEngine.stageName(state.stage))
    val value = ProgressionEngine.prescribedValueAtLevel(entry, state.level)
        ?.let { prescriptionDisplayText(it) }
        .orEmpty()
    return if (value.isBlank()) stage else "$stage · $value"
}

/**
 * How many rungs ABOVE and BELOW the current one the exercise editor's level list
 * carries.
 *
 * Three each way is seven rows: enough to move up or down a step or two, which is
 * what "this level is too heavy / too light" actually resolves to, and few enough
 * that the form stays a form. The list cannot be the whole ladder: the dialog body
 * is a scrolling column and a lazy list inside it would be measured with unbounded
 * height (see AppDialog's `scrollableBody` note). "See every level" on the same
 * card opens the full picker for any rung outside this window.
 */
const val EDITOR_LEVEL_PAGE = 15

/**
 * The page of rungs the editor's level list shows: [pageSize] levels, aligned so
 * the pages are 1..15, 16..30, ... and clamped to the ladder's end.
 *
 * [requestedStart] is the level the caller wants the page to START at, or 0 for
 * "the page the exercise's own level is on" - which is what opening the editor
 * asks for. Page one therefore ALWAYS contains level 1, and an exercise sitting on
 * level 20 opens on the page that contains level 20 instead of on a slice of its
 * neighbours. A requested start is aligned DOWN to its page boundary, so paging
 * forward from 1..15 lands on 16..30 whatever number it is handed.
 *
 * It is pure and separate from the composable that draws it because the pages are
 * the whole of the owner's rule here - "with min 1 level so dont hide level 1" is
 * an assertion about WHICH rungs are on the first screen, and a rule that lives
 * inline in a list builder can only be checked by looking at a phone.
 */
fun levelPageRange(
    currentLevel: Int,
    maxLevel: Int,
    requestedStart: Int = 0,
    pageSize: Int = EDITOR_LEVEL_PAGE
): IntRange {
    val last = maxLevel.coerceAtLeast(1)
    val size = pageSize.coerceAtLeast(1)
    val anchor = if (requestedStart >= 1) requestedStart else currentLevel
    val clamped = anchor.coerceIn(1, last)
    val from = ((clamped - 1) / size) * size + 1
    return from..minOf(from + size - 1, last)
}

/**
 * The levels the exercise editor's level list offers, in a form the dialog can
 * draw: one page of rungs ([range]), each with the load (or variation or duration)
 * it prescribes and the Roman stages it trains.
 *
 * Every string it produces goes through the SAME helpers the live card and the
 * ladder picker use - [prescriptionDisplayText] for the load,
 * [ProgressionEngine.stageName] for the Roman numeral, [ProgressionEngine]'s own
 * prescribed reps for the stage lines - so the editor cannot quote a different
 * number for a level than the card the user just came from. That matters more here
 * than anywhere: choosing a level in this list is the whole reason the screen
 * exists, and a list that disagreed with the card would make every choice feel
 * like a guess.
 *
 * The rows are the exercise's OWN ladder, including any ceiling the engine applies
 * ([WorkoutViewModel.progressionMaxLevel] is ceiling-aware), so the list cannot
 * offer a rung the card would refuse.
 *
 * It draws ONE PAGE of that ladder ([range], from [levelPageRange]) rather than a
 * slice around the current rung, and it is called through [editorLevelData] by both
 * editors, so the add form and the edit form draw the same rungs for the same
 * exercise.
 */
@Composable
fun levelEditorOptions(
    viewModel: WorkoutViewModel,
    exerciseName: String,
    range: IntRange
): List<EditorLevelOption> {
    val entry = ProgressionEngine.entryFor(exerciseName) ?: return emptyList()
    if (!ProgressionEngine.isProgressionReady(entry)) return emptyList()
    val state = viewModel.currentProgressionState(exerciseName) ?: return emptyList()
    val shown = viewModel.progressionMaxLevel(exerciseName)
    if (shown <= 0) return emptyList()

    // The stages a level is climbed through, in Roman numerals. A level's stage
    // count is the exercise's pattern length - the same number the card's stage
    // dots are drawn from - so the two cannot disagree about how many there are.
    val stageCount = ProgressionEngine.patternLength(entry).coerceAtLeast(1)

    return range.map { level ->
        EditorLevelOption(
            level = level,
            isCurrent = level == state.level,
            loadText = ProgressionEngine.prescribedValueAtLevel(entry, level)
                ?.let { prescriptionDisplayText(it) }
                .orEmpty(),
            stages = roundedStages(
                (1..stageCount).map { stage ->
                    EditorLevelStage(
                        stage = stage,
                        roman = ProgressionEngine.stageName(stage),
                        // Empty for a timed exercise: its work is seconds, and the
                        // stage row would otherwise print the Roman rep pattern the
                        // ladder carries. See ProgressionEngine.stageRowReps.
                        reps = ProgressionEngine.stageRowReps(entry, level, stage),
                        // Marked per LEVEL, not once: only the level the user is actually
                        // on can have a current stage, so the stage the exercise sits on
                        // lights up on its own rung and nowhere else.
                        isCurrent = level == state.level && stage == state.stage
                    )
                }
            )
        )
    }
}

/**
 * EVERY level fact the exercise editor needs about [exerciseName], in one call.
 *
 * It exists because the editor is opened for two different things: a row that is
 * already in the routine, and - the bug this fixes - an exercise the user is adding,
 * whose name lives in the dialog's own search field. Both hosts used to hand the
 * six values in one by one, derived from `editingExercise`; an add has no
 * `editingExercise`, so every value was null and the form fell back to the custom
 * input boxes. A resolver keyed on the NAME gives the same six answers for an
 * exercise that is not in the routine yet: the level is the one the system WOULD
 * start it at ([WorkoutViewModel.currentProgressionState] answers that for any pool
 * exercise, from the stored level or the setup difficulty).
 *
 * A name the pool does not carry (a half-typed search, a custom exercise) resolves
 * to [EditorLevelData.NONE], which is what makes the level UI disappear again while
 * the user is still typing.
 *
 * [requestedPageStart] is the level the caller wants the LIST to start at, or 0 for
 * the page the exercise's own level is on - see [levelPageRange]. It is how the
 * editor's "next 15" control asks for the following block of rungs.
 */
@Composable
fun editorLevelData(
    viewModel: WorkoutViewModel,
    exerciseName: String,
    requestedPageStart: Int = 0
): EditorLevelData {
    if (!viewModel.isExerciseLevelManaged(exerciseName)) return EditorLevelData.NONE
    val state = viewModel.currentProgressionState(exerciseName) ?: return EditorLevelData.NONE
    val maxLevel = viewModel.progressionMaxLevel(exerciseName)
    val range = levelPageRange(state.level, maxLevel, requestedPageStart)
    return EditorLevelData(
        levelsManaged = true,
        level = state.level,
        stage = state.stage,
        caption = levelEditorCaption(viewModel, exerciseName),
        maxLevel = maxLevel,
        options = levelEditorOptions(viewModel, exerciseName, range),
        // The neighbouring blocks, or null at the ends of the ladder - so the list
        // can say where the ladder continues without doing page arithmetic itself.
        previousPageStart = if (range.first > 1) {
            (range.first - EDITOR_LEVEL_PAGE).coerceAtLeast(1)
        } else null,
        nextPageStart = if (range.last < maxLevel) range.last + 1 else null,
    )
}

/**
 * The Roman stages worth SHOWING: the ones whose sets all ask for the same reps.
 *
 * OWNER RULING, 2026-10-09: *"dont show each single roman number, round them up so
 * each number shown is the same reps (IV, 10 10 10 - VIII, 11 11 11.. etc)"*. A
 * published pattern climbs in single reps, so between two round stages sit
 * one-rep-apart mixtures (II 9 9 10, III 9 10 10) that name a state nobody is ever
 * on long enough to act on; on a thirteen-stage pattern they also bury the level the
 * list exists to offer. Rounding at the SHOWING site rather than in the engine keeps
 * every rung, every stored stage and the card's dots exactly as they were - the
 * climbing stages are still applied and still drawn on the workout card, they simply
 * stop being menu entries.
 *
 * The stage the exercise is actually ON survives even when it is a mixed row: hiding
 * it would take the "you are here" tick off the list, and a list that cannot say where
 * the user stands is worse than a busy one. A pattern that is mixed the whole way down
 * (nothing to round to) falls back to every stage rather than showing an empty list.
 */
internal fun roundedStages(all: List<EditorLevelStage>): List<EditorLevelStage> {
    val rounded = all.filter { it.reps.distinct().size <= 1 }
    // No round stage to show - do not hide the level's own ladder entirely.
    if (rounded.isEmpty()) return all
    // ONE ROW PER PRESCRIPTION. Rounding alone is not enough: measured across the
    // pool's 123 rep ladders, every one has at least two stages whose sets all ask
    // the same number, and several are flat the WHOLE way down (Archer Pull Ups runs
    // 12/12/12 on all thirteen stages), so keeping every round stage would print the
    // same numbers a dozen times - the cluttered list the owner objected to, just
    // with more repetition. The first stage of each run survives, which is also the
    // one that reads as the step up (I 28 -> IV 29 -> VII 30).
    val seen = mutableSetOf<List<Int>>()
    val unique = rounded.filter { seen.add(it.reps) }
    val current = all.firstOrNull { it.isCurrent }
    return if (current != null && unique.none { it.stage == current.stage }) {
        (unique + current).sortedBy { it.stage }
    } else unique
}

@Composable
fun dayOfWeekDisplayName(dayOfWeek: String): String {
    return when (dayOfWeek.uppercase()) {
        "MONDAY" -> getString("monday")
        "TUESDAY" -> getString("tuesday")
        "WEDNESDAY" -> getString("wednesday")
        "THURSDAY" -> getString("thursday")
        "FRIDAY" -> getString("friday")
        "SATURDAY" -> getString("saturday")
        "SUNDAY" -> getString("sunday")
        else -> dayOfWeek
    }
}

/**
 * One pill in the workout header. Both header actions ("Levels" and "Workout")
 * go through this, so they always match in height, shape and colour — the gap
 * between them belongs to the Row that holds them, not to one button.
 */
@Composable
private fun HeaderActionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = modifier.heightIn(min = 40.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Where one set of the current stage sits: already logged, now, or still to come. */

/**
 * The LEVEL card — the second card under the exercise card.
 *
 * It carries everything the level system prescribes, and only it does: the
 * level and stage on top, then the weight and the per-set reps to reach. The
 * exercise card above stays a clean "what am I doing" view, so the two never
 * repeat each other.
 *
 * The card is painted from the theme's own palette (a primary tint over the
 * surface, a primary border) rather than a neutral one, so it reads as part of
 * the app's colour system instead of a detached grey box.
 *
 * The level pill between the arrows is an actual button — a tonal fill or a
 * ring, plus a border — so "this opens the level picker" is visible instead of a
 * hint the user has to be told. It casts NO shadow: see
 * [PILL_SHADOW_ELEVATION_DP] for why a label with no fill behind it must not
 * pretend to be lifted. The set the user is on renders bold (and filled); sets
 * already saved are tinted, the rest outlined.
 *
 * [level]/[stage] null (exercise outside the level system: custom mode, or a
 * row the engine cannot rate) drop the header and stage dots; the card then
 * stands alone as the routine's own "this set" row.
 */
/**
 * What the level card's controls are allowed to do, in one answer.
 *
 * REWRITTEN 2026-10-08, on the owner's instruction, and it REPLACES the earlier
 * pencil/tick edit-pass contract rather than adding to it: "instead of having that
 * '+' and '-' to change roman number, we will open the full exercise edit screen
 * like normal ... instead of the pencil icon in the top card where the exercise is
 * shown, we should only show the pencil in the bottom card where sets and reps are".
 *
 * So the Roman-stage stepper and the two-step "pencil -> edit mode -> tick" dance
 * are GONE. The Roman numeral stays on the card as a READOUT of the stage the
 * prescription is on; changing the level is the exercise editor's job now, and
 * that editor is one screen reachable from one pencil. What is left is two rules:
 *
 *   - the PENCIL is always on the card, because it is now the ONLY way into the
 *     exercise editor from a live session (the top card's pencil was removed in the
 *     same instruction). It does not hide for an exercise outside the level system
 *     either: that exercise still has a name, a demo link and - in custom mode -
 *     sets, reps and weight to edit, and hiding the pencil there would leave the
 *     live screen with no route into any of it.
 *   - the LEVEL PILL still answers to the persisted `routine_exercises.locked` pin -
 *     the user's own "stay on this level". Unlocked, it opens the ladder directly;
 *     locked, it refuses and explains itself through [levelPillBlockedReason] rather
 *     than doing nothing. `editing` no longer exists to unlock it: the pin is the
 *     only gate, which is what makes the pill's behaviour one fact instead of two.
 */
data class LevelEditControls(
    val levelPillEnabled: Boolean,
    val showPencil: Boolean
)

fun levelEditControls(adjustable: Boolean, locked: Boolean): LevelEditControls =
    LevelEditControls(
        levelPillEnabled = adjustable && !locked,
        showPencil = true
    )

/**
 * Builds the evolution popup queue from a freshly published list, and the whole
 * reason it MERGES instead of assigning.
 *
 * Owner's report: "earlier i had 2 popups to progress the same exercise". The
 * queue was assigned from `evolutionTrees` on every `evolutionToken` bump, and
 * the token bumps on every mastery refresh — including the refresh the FIRST
 * popup's own Accept triggers. So the exercise the user had just decided about
 * was still in the new list (it only leaves the list once it is marked reviewed
 * and the refresh that follows that), and it came straight back as popup #2.
 *
 * Two rules, and both are needed:
 *
 *  - **Pending wins.** A decision already waiting must not be reordered or
 *    replaced by a re-publish, or the queue is not a queue.
 *  - **Shown is finished.** An exercise already put in front of the user in this
 *    pass is not queued again by a re-publish. This is the fix; dropping the
 *    duplicate from `incoming` alone would only hide it until the next bump.
 *
 * Deliberately NOT applied to the editor's "upgrade" badge, which exists to
 * re-show an exercise that was shown and declined — see the call site.
 *
 * One popup per exercise, and only the pending list decides what is next.
 */
fun seedEvolutionQueue(
    pending: List<com.grafizum.repio.ui.viewmodel.EvolutionTreeItem>,
    incoming: List<com.grafizum.repio.ui.viewmodel.EvolutionTreeItem>,
    alreadyShown: Set<String>
): List<com.grafizum.repio.ui.viewmodel.EvolutionTreeItem> {
    val taken = alreadyShown + pending.map { it.card.exerciseName }
    val fresh = incoming.filterNot { it.card.exerciseName in taken }
    // The published list is built from distinct cards, so `fresh` cannot contain
    // a duplicate of ITSELF — but `pending` and `fresh` are two lists joined
    // here, and an exercise can be in both (accepted above, re-published below).
    // Keying the whole result on the name is what makes "one popup per exercise"
    // true rather than nearly true.
    return (pending + fresh).distinctBy { it.card.exerciseName }
}

/** Why the level pill refused a press, so the toast can say the useful thing. */
enum class LevelPillBlocked {
    /** The level system is running: the way in is the pencil. */
    NEEDS_EDIT_PASS,

    /** No level system at all: the honest answer is the custom mode. */
    NEEDS_CUSTOM_MODE
}

/**
 * Which explanation a blocked level pill owes the user.
 *
 * The pill is drawn pressable in every state, so a press it refuses has to say
 * why — the same lesson as the greyed level fields, where `enabled = false`
 * swallowed the tap and read as broken.
 *
 * The trap, which this function exists to make impossible to get wrong again:
 * the obvious implementation keys off `locked`, and `locked` is FALSE for an
 * exercise that was never locked. Such an exercise is blocked too — the ladder
 * opens only inside an edit pass — so keying off the lock tells a first-time
 * user to "switch to custom", which is not why their tap did nothing.
 */
fun levelPillBlockedReason(
    locked: Boolean,
    levelSystemOn: Boolean
): LevelPillBlocked =
    if (locked || levelSystemOn) LevelPillBlocked.NEEDS_EDIT_PASS
    else LevelPillBlocked.NEEDS_CUSTOM_MODE

/**
 * How the level pill is DRAWN: a solid fill, or a hollow ring.
 *
 * The owner's ruling: "dont put a background color as box and only show a border
 * of the theme color around the level when editing with the edit pencil". So the
 * two states are deliberately different surfaces, not one surface at two alphas:
 *
 *  - **idle / committed** — see the 2026-10-04 swap below; this was the solid
 *    primary fill, and is now the outlined ring.
 *  - **editing** — likewise swapped; this was the outlined slot, and is now the
 *    solid primary fill.
 *
 * WHAT `editing` MEANS NOW (2026-10-08): the card hands it
 * [LevelEditControls.levelPillEnabled], i.e. "this pill can be pressed right now".
 * There is no edit pass left to name the flag after — the pencil opens the exercise
 * editor instead, and the card no longer has an edit mode — and the mapping that
 * falls out is the one that reads best anyway: the SOLID pill is the actionable one,
 * the RING is the one the exercise's own lock is holding shut. Both visuals keep
 * their job; only the trigger changed.
 *
 * THE OWNER HAS SINCE INVERTED THE PAIR (2026-10-04). Asked which of four looks
 * they wanted for a single merged design, the answer was not one of them: "no i
 * want the current edit design to be 'none edit' and the 'none edit' design to be
 * the edit design, invert them". So the two appearances SWAP PLACES — the outlined
 * ring is now what a COMMITTED level looks like, and the solid fill is what an
 * OPEN edit pass looks like. Neither design changed; only which one means what.
 *
 * That is the opposite direction from the previous ruling in this file. The
 * 2026-10-02 ask was "show it clickable WHEN PRESSING ON THE PENCIL", answered by
 * lifting the editing pill to 6dp so the affordance grew with the pencil. Under the
 * swap the editing pill is the SOLID one again, and the committed ring inherited
 * the 6dp — which is the version the owner then reported as "a weird white line
 * under it and a shadow". The elevation is gone from both states now; see
 * [PILL_SHADOW_ELEVATION_DP]. The distinction that survives is still real — the
 * two states do not look alike — and it is signalled by fill and ring alone.
 * [LevelPillStyleTest] pins the mapping in BOTH directions rather than asserting
 * "they differ", because "they differ" was true before the swap too, and an
 * assertion that cannot tell which way round is one that would have passed the
 * exact regression it was written for.
 *
 * [progressionAvailable] makes the pill COMPACT either way (smaller type, less
 * padding), so the pending progression can have the row's vertical space. It is
 * folded in here rather than re-decided at the call site, so "compact" cannot
 * mean one thing for the padding and another for the type.
 */
// NO FILL MODE ANY MORE. There used to be one - a SOLID pill for an open edit
// pass and an OUTLINED ring for a committed level - and it was inverted twice,
// which is exactly the trap this ruling removes.
//
// OWNER RULING, 2026-10-09: *"dont change the level label when locking or
// unlocking an exercise. Keep the label as a thin border around the level
// instead."* So the pill has ONE look, a 1dp ring, and the lock is told apart by
// the lock chip beside it (which does fill when it is on) rather than by
// re-drawing the level itself. A label that changes under the user's finger is
// a label they have to re-read every time they touch the lock.

/**
 * The pill casts NO shadow, in either state.
 *
 * It used to: 6dp on the outlined ring (since the 2026-10-04 swap made the
 * outlined ring the COMMITTED level) and 2dp on the solid fill (the 2026-10-01
 * "the level doesnt look pressable" fix). The owner saw the result on the phone
 * and reported "the level button / label has a weird white line under it and a
 * shadow" — which is what a Material3 `Surface` does with these two settings:
 *
 *  - `shadowElevation` becomes `Modifier.graphicsLayer(shadowElevation = …,
 *    shape = shape)`, and that casts from the shape's OUTLINE. A ring has an
 *    outline but no fill, so the pill still threw a full rounded-rect shadow
 *    while having no mass to lift — a smudge sitting under a label.
 *  - the layer's ambient/spot colours default to plain black, not the theme, so
 *    the smudge is not even the app's own colour.
 *
 * A shadow cannot be rescued by dialling it down here: on a 12dp-tall pill any
 * visible lift reads as a rendering artefact rather than as depth. So the honest
 * answer is that neither state has one, and the two states stay apart by FILL and
 * RING WEIGHT alone — which is what told them apart before 2026-10-02 added
 * elevation in the first place.
 *
 * It stays a field on [LevelPillStyle] rather than a `0.dp` at the call site,
 * because the call site is precisely where the two elevations used to be
 * re-decided, and a test has to be able to pin "no state lifts the pill".
 */
private const val PILL_SHADOW_ELEVATION_DP = 0

data class LevelPillStyle(
    val borderWidthDp: Int,
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    /**
     * How far the pill is lifted off the card — 0 in both states, because a
     * shadow on a shape with no fill is an artefact rather than an affordance.
     * See [PILL_SHADOW_ELEVATION_DP] for what it looked like and why.
     *
     * Decided HERE, beside the fill and the ring, because the three are one
     * affordance: an editor changing the elevation at the call site is how the
     * pill ended up raised in one state and flat in the other — twice — and the
     * owner had to report the result as a stray line under the label.
     */
    val shadowElevationDp: Int,
)

/**
 * The level label's look — the SAME look whether the exercise is locked or not.
 *
 * [editing] is kept as a parameter even though it now changes nothing, and that
 * is the point: the caller still hands in the lock state, and the test asserts
 * that both answers produce an IDENTICAL style. A parameter that is silently
 * ignored is a smell; a parameter whose whole contract is "this must not change
 * the result" is the owner's ruling, written down where the next editor will
 * read it — that is how this pill got inverted twice.
 */
fun levelPillStyle(editing: Boolean, progressionAvailable: Boolean): LevelPillStyle =
    LevelPillStyle(
        // THIN, as asked: one hairline, never a 2dp ring, never a fill.
        borderWidthDp = 1,
        horizontalPaddingDp = if (progressionAvailable) 9 else 12,
        verticalPaddingDp = if (progressionAvailable) 4 else 7,
        shadowElevationDp = PILL_SHADOW_ELEVATION_DP,
    )

/** Which of the card's two body layouts the level prescription uses. */
enum class LevelCardBody {
    /** The tinted action banner: "SET 2 OF 3 - do 10 reps" plus per-set pills. */
    BANNER,

    /**
     * The same banner, but the headline is a DURATION ("45 s") instead of a rep
     * count. A time-based exercise used to fall through to [SUMMARY], which is
     * the plain custom-mode presentation - the owner's report was that time-based
     * exercises "look like custom exercises even though level system is on".
     * Different headline text is not a different card: it is the same card
     * wearing the unit the exercise actually measures in.
     */
    BANNER_DURATION,

    /** The plain summary line. Only for an exercise outside the level system. */
    SUMMARY
}

/**
 * Which body the level card wears.
 *
 * [inSystem] is the whole decision for the "dismiss the level system card"
 * complaint: the owner's words were "i want no exercise to dismiss the level
 * system display card design", so an exercise that HAS a level gets a banner
 * whatever it measures. Time-based exercises used to be excluded by
 * `!isTimeBased`, which is exactly the split the owner saw and disliked.
 *
 * A time-based exercise ALWAYS gets [LevelCardBody.BANNER_DURATION], including when
 * the engine could not resolve a duration ([durationText] == null).
 *
 * It used to fall back to [LevelCardBody.SUMMARY] in that case, on the reasoning
 * that there would be no headline to print. That reasoning produced the owner's
 * second report on the same card: "some exercises dont show the nice level card
 * style and only show rep range". SUMMARY renders `primaryText ?: fallbackReps`,
 * so a timed exercise with an unresolved duration fell back to printing the
 * routine's REP string - which is worse than an empty headline, because a plank
 * showing "12 reps" is not a missing value, it is a wrong prescription. The
 * banner's headline is null-safe upstream, so an unresolvable duration prints no
 * number rather than the wrong one.
 *
 * IT SAYS SO HERE RATHER THAN AT THE CALL SITE. The banner used to be gated on
 * `cardBody == BANNER_DURATION || activeReps != null`, so this function answered
 * BANNER for a timed exercise with no duration and the summary was reached by a
 * second condition two hundred lines away, in a different layer. One decision
 * taken in two places is how "no exercise wears the custom card" stops being
 * true: someone tightening the call-site guard has no idea this function
 * promised a banner it cannot deliver.
 */
fun levelCardBody(inSystem: Boolean, isTimeBased: Boolean, durationText: String?): LevelCardBody = when {
    !inSystem -> LevelCardBody.SUMMARY
    // `durationText` is deliberately NOT consulted. See above: reading it here is
    // what let a timed exercise out of the banner and into a rep summary.
    isTimeBased -> LevelCardBody.BANNER_DURATION
    else -> LevelCardBody.BANNER
}

/**
 * Whether the card renders the per-set action banner - the "level style card".
 *
 * THE SET COUNT IS NOT PART OF THE DECISION, and that is the whole point of this
 * function existing separately. The gate used to read `repTargets.size in 1..3`,
 * so a hypertrophy (4 sets) or strength (5 sets) row silently fell through to the
 * plain summary line and looked like a DIFFERENT card from the 3-set exercises
 * beside it - the owner's report: "I want each exercise no many how many sets, to
 * have the same level style card as the 3 set exercises. Fix it now." A rule that
 * lives inline in the composable is also the reason it could not be tested; this
 * one is pure, so every set count is pinned by [LevelCardSetCountTest].
 *
 * A duration banner needs no rep targets - its headline is the seconds - so it is
 * never gated on them. Only an exercise outside the level system (SUMMARY) or one
 * with no rep targets at all falls back to the plain summary.
 */
fun levelCardShowsPerSetBanner(cardBody: LevelCardBody, repTargetCount: Int): Boolean =
    cardBody == LevelCardBody.BANNER_DURATION ||
        (cardBody == LevelCardBody.BANNER && repTargetCount > 0)

@OptIn(ExperimentalMaterial3Api::class) // Surface(onClick) for the level pill
@Composable
// `internal` rather than `private` ONLY so a test can lay the card out and measure
// it: the lock chip's placement is a property of this row's geometry, and the
// owner read the card's header as "the level lock button is a bit too centered"
// (2026-10-09). [LevelCardLockPlacementTest] renders exactly this composable and
// asserts where the chip sits, which no pure function can express. The card is
// still only ever called from this file.
internal fun LevelPrescriptionCard(
    exerciseName: String,
    level: Int?,
    stage: Int?,
    lastStage: Int,
    adjustable: Boolean,
    locked: Boolean,
    targetSets: Int,
    repTargets: List<Int>,
    currentSetNumber: Int,
    fallbackReps: String,
    primaryText: String?,
    loadText: String?,
    isTimeBased: Boolean,
    /**
     * Opens the exercise editor — the ONE screen the level system is edited on.
     * It replaced both the card's Roman-stage stepper and the exercise card's
     * pencil (owner, 2026-10-08).
     */
    onOpenEditor: () -> Unit,
    onOpenDetails: () -> Unit,
    onBlockedTap: () -> Unit,
    /**
     * Flips THIS exercise's own lock (`routine_exercises.locked`) — the pin that holds it on
     * its current level and stage. It used to live only inside the level popup, as a switch
     * that actually wrote the GLOBAL pref; the owner asked for it out of the popup and on the
     * card (2026-10-08), which also puts it on the surface whose behaviour it changes.
     */
    onToggleLock: () -> Unit,
    progressionAvailable: Boolean,
    progressionTarget: String?
) {
    val window = LocalWindowSize.current
    val inSystem = level != null && stage != null
    // NO edit-mode state any more. It existed to freeze/unfreeze the exercise so
    // the Roman-stage stepper and the level pill only answered inside an explicit
    // edit pass; the stepper is gone and the pill is gated by the persisted lock
    // alone, so a local `editing` flag would have nothing left to gate. The
    // "logging a set commits the edit" effect went with it - there is no edit
    // pass now, and the level the user picked is still carried forward by the
    // stored progression state.
    //
    // The pill's shape/palette, decided once by [levelPillStyle] so the fill,
    // the ring and the padding cannot drift apart. It is handed
    // [LevelEditControls.levelPillEnabled], which is the honest reading of the
    // parameter now: SOLID is the pill that can be pressed, the ring is the one
    // the lock is holding shut.
    // The lock state is handed in and deliberately ignored by [levelPillStyle]:
    // locking an exercise must not redraw its level label (owner, 2026-10-09).
    val pillStyle = levelPillStyle(
        editing = adjustable && !locked,
        progressionAvailable = progressionAvailable
    )
    // One place decides what the level pill and the pencil may do - see
    // [levelEditControls] for why they must not be separate expressions.
    val controls = levelEditControls(adjustable = adjustable, locked = locked)
    // The per-set action banner is LEVEL-MODE ONLY: it narrates the system's
    // prescription. Custom mode keeps the classic read — one big target line,
    // plan summary beside it — so the two modes look and feel different.
    val cardBody = levelCardBody(inSystem = inSystem, isTimeBased = isTimeBased, durationText = primaryText)
    // Every set count wears the banner. See [levelCardShowsPerSetBanner] for why
    // the set count is deliberately not part of the decision.
    val showChips = levelCardShowsPerSetBanner(cardBody, repTargets.size)

    Surface(
        shape = RoundedCornerShape(20.dp),
        // Level mode wears the primary tint; custom mode a neutral surface so
        // the card visually confirms which mode is driving the workout. Both
        // get a hairline rim so they hold their edge against the background.
        color = if (inSystem) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (inSystem) 0.35f else 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(window.cardPadding),
            verticalArrangement = Arrangement.spacedBy(window.sectionSpacing / 2)
        ) {
            // ---- Header: level pill anchored LEFT with the Roman stage as a
            // side note OUTSIDE the pill's background; the stepper (− level +)
            // rides at the far right. Skipped for exercises outside the level
            // system — those get a plain "this set" strip with no level chrome.
            if (level != null && stage != null) {
            // The "progression available" caption sits ABOVE the row rather than
            // inside it. Inside, it would have to compete with the stage dots
            // and the stepper for a phone's width, and the stepper is the one
            // control that must never be pushed off the edge. Above, it costs
            // one line and the header row's geometry is untouched whether or
            // not an offer is pending - which is the "doesnt break any
            // graphics" the owner asked for.
            if (progressionAvailable) {
                // ONE line: the caption and the exercise it can become, with
                // the arrow joining THOSE TWO rather than sitting between the
                // level pill and the name.
                //
                // That was the owner's other complaint: "have the label be shown
                // somehow that its progression and doesnt look like it would
                // indicate the next exercise". An arrow pointing from the LEVEL
                // at an exercise name reads as "your next exercise is X", which
                // is a different promise from "this can become X". Moving the
                // arrow inside the caption line makes it join "progression" to
                // its target instead, and it also removes a row from the card.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "progression_available_label" }
                ) {
                    Text(
                        text = getString("progression_available").uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    if (progressionTarget != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 5.dp)
                                .size(13.dp)
                        )
                        Text(
                            text = progressionTarget,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // The clickable level pill: a raised tonal button rather than bare
                // text, so the tap target is discoverable without a caption.
                //
                // It must LOOK pressable in EVERY state. It used to drop its
                // elevation outside edit mode and to pass Material's `enabled`,
                // which greys the whole surface - so the one control that opens the
                // level ladder read as flat text, and the owner reported it as
                // "doesnt look like its pressabel". Raised, with a rim, always.
                //
                // The CLICK is still gated by the edit contract ("when lock in then
                // not anymore"), but a press that cannot act now explains itself
                // instead of being swallowed - the same lesson as the greyed level
                // fields, where `enabled = false` made a tap do nothing at all and
                // read as broken. Hence `enabled = true` with the decision moved
                // inside, and onBlockedTap carrying WHY it is blocked.
                // ONE BOX, TWO READINGS: "Level 10 | IV".
                //
                // Owner, 2026-10-08: *"i want the card to show 'level 10 | IV' for example as a
                // whole small box instead of having the romand number on the right"*. The level
                // and the stage it is on are one answer read twice, so they share one surface
                // instead of sitting at opposite ends of the row with a gap between them. The
                // stage DOTS stay - now inside the box - because the numeral alone does not say
                // how far along the level's own lattice that stage sits.
                //
                // The box is the control that opens the level INFORMATION sheet. It looks
                // pressable in every state: it used to drop its elevation outside edit mode and
                // pass Material's `enabled`, which greyed the whole surface, so the one control
                // that opens the ladder read as flat text ("the level on the left doesnt look
                // like its pressabel"). Raised, with a rim, always. The CLICK is gated by the
                // exercise's own lock, and a press that cannot act explains itself instead of
                // being swallowed - `enabled = true`, decision inside, [onBlockedTap] says why.
                Surface(
                    onClick = {
                        if (controls.levelPillEnabled) onOpenDetails() else onBlockedTap()
                    },
                    enabled = true,
                    shape = RoundedCornerShape(12.dp),
                    // ONE look, in every state: the level is a label in a thin
                    // ring, and the lock beside it is the only thing that changes
                    // colour when the exercise is locked (owner, 2026-10-09).
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(
                        pillStyle.borderWidthDp.dp,
                        MaterialTheme.colorScheme.primary
                    ),
                    // Read the elevation off [levelPillStyle] rather than
                    // re-deciding it here. This branch used to force 0.dp on
                    // every outlined pill on the grounds that "an edit slot is
                    // not a button, the ring is the affordance", which is also
                    // where the 6dp went when the pair was inverted and the ring
                    // became the resting state. Both halves shipped a shadow that
                    // had nothing to lift: the owner's "a weird white line under
                    // it and a shadow" was this parameter, not the ring.
                    shadowElevation = pillStyle.shadowElevationDp.dp,
                    modifier = Modifier.semantics { testTag = "level_chip" }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = pillStyle.horizontalPaddingDp.dp,
                            vertical = pillStyle.verticalPaddingDp.dp
                        )
                    ) {
                        // The reading itself - "Level 10 | IV" - comes from
                        // [LevelStageLabel] rather than from three literals here, so
                        // that the guide's example of this box is the SAME drawing
                        // instead of a hand-copy of it. The bar between the level and
                        // its stage is DRAWN there, not typed (owner, 2026-10-09):
                        // *"make the line that separates level with roman number,
                        // thinner so it doesnt look like a number or letter"*.
                        LevelStageLabel(
                            level = level,
                            stage = stage ?: 1,
                            // A pending progression makes the box COMPACT: the owner's words
                            // were that the level should stop taking the row's whole vertical
                            // height and give the space to the progression it can become.
                            // Height comes off the padding and the type size rather than from
                            // a fixed height, so nothing can clip at a large font scale.
                            style = if (progressionAvailable) {
                                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            } else {
                                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                            },
                            // The label reads on the CARD's own background now that
                            // there is no fill, so it is primary in both skins - the
                            // developer skin's gold on its dark card included.
                            color = MaterialTheme.colorScheme.primary,
                            trailing = {
                                // Stage progress as dots INSIDE the box: the same lattice the
                                // numeral names, drawn as position rather than as a second number.
                                if (lastStage in 2..8) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StageDots(total = lastStage, current = stage)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // THE RIGHT-HAND PAIR: the pencil, then the lock chip, closing ON the
                // card's right edge.
                //
                // Owner, 2026-10-09: *"the level lock button is a bit too centered in my
                // opinion, it needs to be better positioned and maybe also changed the style
                // slightly so it fits with the rest of the content in that card"*, and then,
                // on where it should go: *"make it far right together with the pencil but
                // also make sure that the user can distinguish them and make it look
                // good"*.
                //
                // WHY IT READ AS CENTRED. The chip used to be a plain sibling of the pencil,
                // so the header's own 8dp spacing AND a fixed 4dp spacer both landed between
                // the two - while the weight spacer above had already eaten the free width.
                // The chip's left edge therefore fell on the card's midline and the control
                // looked like it was floating in the middle of the row, belonging to neither
                // the level box on its left nor the pencil on its right. Making the two their
                // OWN row is what ties them together: the pair travels to the edge as one
                // cluster, and the weight spacer stays the only elastic thing in the header.
                //
                // Owner, 2026-10-11, still on the same chip: *"change the positioning a bit
                // of the level lock to the right so that it doesnt look like its randomly
                // floating and not anchored somehow or aligned somewhere"*.
                //
                // WHAT THE FIRST FIX MISSED. Reaching the right edge is not the same as being
                // ANCHORED to it. Measured on the phone (SM-S906B, 1080px @ 420dpi), the
                // chip's ring ended at x=852 while the card's content column - the line the
                // body's own values (`27.5 kg`, `8 / 8 / 8 / 8 Reps`) and `NEXT UP` end on -
                // is x=983. So its right edge stopped 50dp short of the one line the card is
                // built on, 112dp of nothing sat behind its left edge, and 25dp of the
                // pencil's invisible padding sat in front of it: a control with no reference
                // on either side reads as floating even when it is not centred.
                //
                // THE ORDER, NOT A DISTANCE, IS THE FIX. The pencil's 48dp target is mostly
                // invisible padding, so no nudge could bring the ring close to the glyph
                // without eating that tap target - and parked LEFT of the pencil, the chip's
                // right edge can only ever land on the pencil's padding rather than on the
                // card. With the pencil first and the chip last, the chip's right edge IS the
                // card's content column (the same x `NEXT UP` ends on), so the one control the
                // owner asks users to understand is what closes the header, and the pencil
                // tucks in behind it at ~8dp - the pair still reads as one cluster.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // 2dp, not the header's 8dp. The pencil's 48dp target is mostly
                    // invisible padding (a 20dp glyph centred in it), and the pencil's own
                    // +6dp shift closes the same padding from the other side, so the gap the
                    // eye actually reads between the glyph and the chip's ring is ~8dp -
                    // which is what the two controls stay APART by shape and wording instead
                    // of by distance: a labelled, ringed pill beside a bare circular glyph.
                    // Keeping the pair close also keeps their tap targets from drifting
                    // under each other's finger.
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // THE ONE PENCIL, and the only way into the exercise editor from
                    // a live session - the pencil that used to sit on the exercise card
                    // above was removed in the same instruction that put this one here.
                    //
                    // It OWNS the level system's editing now: the editor it opens is
                    // where the level list lives (and, in custom mode, the sets, reps
                    // and weight fields). There is no tick any more, because there is no
                    // edit pass to commit - the level the user picks is written through
                    // the same stored progression state it always was.
                    if (controls.showPencil) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 48.dp) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { onOpenEditor() }
                                    // The 6dp the glyph used to be pushed toward the card
                                    // edge by now comes off this target's TRAILING side
                                    // instead - the corner belongs to the lock chip since
                                    // 2026-10-11. Keeping it as padding rather than an
                                    // `.offset` is deliberate: an offset would drag the
                                    // whole 48dp target into the chip, and the 4dp nearest
                                    // the chip would then steal the taps meant for this
                                    // pencil.
                                    .padding(end = 6.dp)
                                    .semantics { testTag = "level_edit" },
                                // Centred in what is left, i.e. at the END of the target: the
                                // glyph reads as one cluster with the chip beside it while
                                // the whole 48dp square stays this pencil's.
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = getString("exercise_edit_desc"),
                                    // ALWAYS the theme colour. It was dimmed to 45%
                                    // while idle, which the owner read as "the pencil is
                                    // grayed out at the beginning" - and it is the one
                                    // control that opens the level system, so looking
                                    // disabled is the opposite of the truth.
                                    tint = MaterialTheme.colorScheme.primary,
                                    // 18 -> 20dp: "make the edit pencil slightly
                                    // bigger". The 48dp target around it is what the
                                    // user actually presses. Tagged apart from its target
                                    // because the layout gap the placement test can see on
                                    // the target is NOT the air the eye sees here - the 14dp
                                    // of padding inside it is invisible (owner, 2026-10-11).
                                    modifier = Modifier
                                        .size(20.dp)
                                        .semantics { testTag = "level_edit_glyph" }
                                )
                            }
                        }
                    }

                    // THE EXERCISE'S OWN LOCK, ON THE CARD.
                    //
                    // Owner, 2026-10-08: *"i also want the locked not to be in the popup of the
                    // level but separate on the card somewhere maybe make it with a label
                    // eventually or show it somehow else so that the user can understand that he
                    // can lock that exercise from leveling up"*. The popup's switch toggled a
                    // GLOBAL pref ("stay on this level" for every exercise, level_lock_enabled)
                    // while its label read like the exercise's own pin - so the control on the
                    // card now writes the per-exercise pin the data layer already had
                    // ([WorkoutViewModel.setExerciseLocked], `routine_exercises.locked`), which is
                    // also exactly what the box above reads to decide whether it may open. Label
                    // plus icon, because a bare padlock is the one thing the owner reported as
                    // invisible: "show it somehow else so that the user can understand".
                    //
                    // IT CLOSES THE HEADER. Owner, 2026-10-11: *"change the positioning a bit
                    // of the level lock to the right so that it doesnt look like its randomly
                    // floating and not anchored somehow or aligned somewhere"*. The chip is
                    // the LAST control in the row now, so its right edge lands on the card's
                    // content column instead of on the pencil's padding - the measured 50dp
                    // the pair's own comment above is about.
                    //
                    // IT SPEAKS THE CARD'S OWN LANGUAGE, which is the second half of the
                    // instruction - "changed the style slightly so it fits with the rest of
                    // the content in that card". The level box beside it is a 1dp primary
                    // ring with no fill, so the chip is the same ring in the same family: a
                    // hairline of primary, filled with the 14% primary tint the action
                    // banner below uses once the lock is ON. It used to wear the neutral
                    // `outlineVariant` grey, which is the one colour nothing else in this
                    // card is drawn in - the reason it read as a foreign control.
                    Surface(
                        onClick = onToggleLock,
                        shape = RoundedCornerShape(12.dp),
                        color = if (locked) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (locked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.semantics { testTag = "level_lock_toggle" }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            // 7dp of vertical padding is the level box's own resting
                            // padding, so the two chips in this header stand at the same
                            // height instead of one being visibly stubbier.
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (locked) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = getString(if (locked) "level_locked_short" else "level_lock_short"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                // The WORD stays the quiet one while the lock is off, so
                                // "can I press this?" is answered by the fill and the ring
                                // rather than by the label, and the chip cannot be mistaken
                                // for the pencil - which is always full-strength primary.
                                color = if (locked) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            }

            if (showChips) {
                // ---- Action banner: the ONE thing to do right now ----
                // "SET 2 OF 3 — do 10 reps" at display size in a tinted strip,
                // with one progress pill per set underneath. The current target
                // must read at arm's length mid-set; finished sets collapse to
                // filled ticks, upcoming ones stay visible but quiet.
                val activeSet = currentSetNumber.coerceIn(1, targetSets)
                val activeReps = repTargets.getOrNull(activeSet - 1)
                // A REP banner needs a rep target to headline and a DURATION banner needs its
                // seconds; [levelCardBody] has already resolved which of the two
                // this card is, so the guard below is only "do I have the number
                // that card headlines with".
                if (cardBody != LevelCardBody.SUMMARY &&
                    (cardBody == LevelCardBody.BANNER_DURATION || activeReps != null)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = getString("set_progress_format", activeSet, targetSets).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        // The unit decides the words, not the card:
                                        // a duration says "45 s" and a rep-based
                                        // exercise says "10 reps".
                                        text = if (cardBody == LevelCardBody.BANNER_DURATION) {
                                            primaryText.orEmpty()
                                        } else {
                                            activeReps.toString()
                                        },
                                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    if (cardBody != LevelCardBody.BANNER_DURATION) {
                                        Text(
                                            text = " " + getString("reps"),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }
                                }
                            }
                            Column(
                                // One even rhythm for every line in this stack.
                                // The rows used to be laid out by whatever each
                                // one happened to be (a centred dots row, a
                                // padded load line, a padded reps line), which is
                                // what made the card read as text scattered at
                                // random rather than as one column.
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // One pill per set: filled = done, wide = current,
                                // faint = upcoming. The week's shape at a glance.
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(targetSets) { index ->
                                        val done = index < currentSetNumber - 1
                                        val current = index == currentSetNumber - 1
                                        Box(
                                            modifier = Modifier
                                                .size(width = if (current) 18.dp else 8.dp, height = 8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when {
                                                        current -> MaterialTheme.colorScheme.primary
                                                        done -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                    }
                                                )
                                        )
                                    }
                                }
                                if (loadText != null) {
                                    Text(
                                        text = loadText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                // Small label with EVERY set's target, so the stage's
                                // whole shape ("9 / 8 / 8") reads next to the pills and
                                // nobody has to guess what the remaining sets will ask.
                                if (repTargets.size > 1 && cardBody != LevelCardBody.BANNER_DURATION) {
                                    // A duration banner already states the seconds in
                                    // its headline; listing per-set REP targets beside
                                    // it would mix two units on one card, which is
                                    // the thing a plank must never do.
                                    Text(
                                        text = repTargets.joinToString(" / ") + " " + getString("reps"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Fallback view (time-based / mixed-rep prescriptions): the
                // full plan summary beside the target, no banner needed.
                Text(
                    text = if (repTargets.isEmpty()) getQuantityString("sets_planned_pl", targetSets)
                           else getQuantityString("sets_x_reps_pl", targetSets, targetSets, repTargets.joinToString(" / ")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // weight(fill = false): the target can shrink (and ellipsize) so the
                    // load beside it is never squeezed off the card.
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = primaryText ?: fallbackReps,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            // TWO LINES, because this line is often the exercise's own
                            // name. Measured on the owner's 1.8x emulator: "Dumbbell
                            // Bench Press" needs ~1140px of a 736px row at this style
                            // and a single line cut it to "Dumbbell Benc..." - the
                            // exercise card naming the exercise by its first twelve
                            // letters. At font scale 1.0 the same string needs ~635px
                            // and still sits on one line, so the phone is unaffected.
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (loadText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = loadText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // THE CARD DOES NOT REPEAT THE LEVEL AND THE STAGE HERE ANY MORE.
            //
            // A small "L5 · I (fresh)" line used to sit at the very bottom of the
            // card, under the reps banner - the debug readout's leftover. The
            // header's own level box already prints exactly that ("Level 5" with
            // the stage dots inside it), so the line restated the two numbers the
            // user is looking at, one screen-width below them. Owner, 2026-10-11:
            // "remove the 'L5 | I' labels under the current reps thats absolutely
            // useless since its shown over the reps as well." It is GONE rather
            // than restyled: a second copy of a value is not a design, and the
            // "(fresh)" debug hint that shared the line was only ever a developer
            // convenience.
        }
    }
}

/**
 * What the session just achieved, graphically.
 *
 * The owner's ask: "instead of having the 'workout completed' screen simply put
 * like that, add graphical information of a quick recap for what was made
 * today". A trophy and a set count is not a recap.
 *
 * Three deliberate choices:
 *
 *  - **Volume is omitted, not zeroed.** A bodyweight or timed-only session has
 *    no honest tonnage, and a "0 kg" tile would read as "you did nothing" for a
 *    session that did three sets of chin-ups. The tile is simply absent, and the
 *    remaining tiles stay in the same order rather than closing the gap - a
 *    shifting row of tiles is harder to read than a stable one.
 *  - **The split bar uses the app's own muscle colours.** `getMuscleGroupColor`
 *    is the same function the legend, the cards and the analytics screen use, so
 *    a muscle cannot be one colour here and another there.
 *  - **The bar is sets, not load.** Sets are the unit every exercise shares -
 *    a pin count and a minute of planking are both "a set" - so the segments
 *    are comparable in a way kilograms are not.
 */
@Composable
private fun WorkoutRecapCard(recap: com.grafizum.repio.ui.viewmodel.WorkoutRecap) {
    val durationMinutes = (recap.durationSeconds / 60).toInt()
    // Tonnage follows the unit too, but WITHOUT the grid: it is a sum of many
    // sets, not a load anyone sets on a bar, so it converts and rounds whole.
    val volumeUnit = LocalWeightUnit.current
    val volumeText = recap.volumeKg?.let { kg ->
        val shown = volumeUnit.fromKg(kg)
        val clean = if (shown % 1.0 == 0.0) shown.toInt().toString() else String.format("%.1f", shown)
        getString(
            if (volumeUnit == WeightUnit.KG) "recap_volume_kg" else "recap_volume_lb",
            clean
        )
    }
    val tiles = buildList {
        add(getString("recap_exercises") to recap.exercises.toString())
        add(getString("recap_sets") to recap.sets.toString())
        if (volumeText != null) add(getString("recap_volume") to volumeText)
        if (durationMinutes > 0) add(getString("recap_duration") to getString("recap_duration_format", durationMinutes))
    }
    val totalSets = recap.setsByMuscle.values.sum().coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = getString("recap_title"),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.forEach { (label, value) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (recap.setsByMuscle.isNotEmpty()) {
                Text(
                    text = getString("recap_split_title"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // The distribution bar. Proportional widths from ONE total, so
                // the segments always sum to the full width and a muscle can
                // never be drawn wider than the bar it shares.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                ) {
                    recap.setsByMuscle.forEach { (muscle, sets) ->
                        Box(
                            modifier = Modifier
                                .weight(sets.toFloat() / totalSets.toFloat())
                                .fillMaxHeight()
                                .background(getMuscleGroupColor(muscle))
                        )
                    }
                }
                // The legend, so the colours mean something. Sorted like the bar,
                // so reading down the list matches reading across it.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    recap.setsByMuscle.entries.forEach { (muscle, sets) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(getMuscleGroupColor(muscle))
                            )
                            Text(
                                text = muscle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = sets.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One dot per Roman stage: filled up to the current one. */
@Composable
private fun StageDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

// ----------------------------------------------------------------------
// Mastery — the exercises that finished with nothing to evolve INTO.
//
// The branch cards are gone: an exercise with a branch now gets the
// evolution popup, which says more in one screen than a list of branch
// buttons ever did. But a TRUE CEILING has no branch, so nothing pops for
// it, and deleting the section outright would silently delete the fact that
// the exercise is done. Those stay here as one line each.
// ----------------------------------------------------------------------

@Composable
private fun MasteryCeilingSummary(
    cards: List<MasteryCardData>,
    onKeepAtCap: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.forEach { card ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = translateExerciseName(card.exerciseName),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = getString("mastery_human_limit"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = card.ceiling?.basis ?: getString("mastery_ceiling_default"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (card.suggestions.isNotEmpty()) {
                        val names = card.suggestions.map { translateExerciseName(it) }
                        Text(
                            text = getString("mastery_focus_on") + " " + names.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onKeepAtCap(card.exerciseName) }) {
                        Text(
                            text = getString("mastery_acknowledge"),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}
