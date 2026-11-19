package com.grafizum.repio.ui.viewmodel

import com.grafizum.repio.util.NotificationHelper
import com.grafizum.repio.util.getPlatformSettings
import com.grafizum.repio.util.AppEventBus
import com.grafizum.repio.util.getTimerManager
import com.grafizum.repio.util.applyAppIconChange
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.remainingRestSeconds
import com.grafizum.repio.util.startOfTodayMillis
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.HumanCeiling
import com.grafizum.repio.data.ProgressionBranch
import com.grafizum.repio.data.OverallLevelCalculator
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.WeightUnit
import java.util.Locale
import com.grafizum.repio.data.RoutineDayEntity
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.UserStatsEntity
import com.grafizum.repio.data.WorkoutRepository
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.screens.remapTemplateToDays
import com.grafizum.repio.ui.theme.isRetiredThemeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.max

data class WorkoutNowState(
    val selectedDayOfWeek: String = getTodayDayOfWeek(),
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val currentWeightKg: Double = 20.0,
    val recommendedWeightKg: Double = 20.0,
    val currentReps: Int = 10,
    val minReps: Int = 1,
    val maxReps: Int = 100,
    /** The per-set target the progression engine prescribed for THIS set (null = none). */
    val currentSetTargetReps: Int? = null,
    /** True when the last logged set completed the stage and the level advanced. */
    val lastSetLeveledUp: Boolean = false,
    /**
     * v5 level-system display fields for the CURRENT exercise (all null/empty
     * when the exercise is outside the level system). The UI renders these;
     * it never talks to the engine directly.
     */
    val progressionLevel: Int? = null,
    val progressionStage: Int? = null,
    /**
     * Every set's target for the current stage, e.g. [9, 8, 8] for
     * "9 / 8 / 8 Wdh." — padded to the routine row's set count when the
     * published pattern is narrower (see ProgressionEngine.stageTargetsForSets),
     * so the banner and the set pills never disagree on how many sets there are.
     */
    val progressionStageTargetReps: List<Int> = emptyList(),
    /** The prescribed load/variation/duration at the current level. */
    val progressionPrescribed: ProgressionEngine.PrescribedValue? = null,
    val isWorkoutCompleted: Boolean = false,
    val restTimerSeconds: Int = 0,
    val isTimerActive: Boolean = false,
    val isExerciseTimerActive: Boolean = false,
    /**
     * True while the timed exercise's own clock is held.
     *
     * A PAUSED CLOCK IS NOT A RUNNING ONE and must not be read as one: the countdown is frozen at
     * [restTimerSeconds], the stored end time is gone (so the alarm cannot fire and the background
     * service cannot move anything), and the log button is live again - which is what makes "pause,
     * then log" the way to finish early.
     */
    val isExerciseClockPaused: Boolean = false,
    val timerDayOfWeek: String? = null,
    val currentExerciseSeconds: Int = 30,
    val minExerciseSeconds: Int = 10,
    val maxExerciseSeconds: Int = 600,
    val totalSetsLoggedToday: Int = 0,
    /** Exercises that reached their ladder end / human ceiling this workout. */
    val masteredExercises: List<MasteryCardData> = emptyList(),
    /**
     * The evolution offers to put in front of the user, one per mastered
     * exercise that has a branch. The screen shows them as a queue of popups.
     */
    val evolutionOffers: List<ProgressionEngine.EvolutionOffer> = emptyList(),
    /**
     * One entry per mastered exercise that HAS a branch, carrying EVERY way on
     * rather than only the preferred one.
     *
     * The tree popup draws a fork, so it needs the whole fork; [evolutionOffers]
     * is one-per-exercise (the preferred branch only) and cannot express "six
     * ways on". Order is the card's own branch order, and the first entry is the
     * engine's preferred branch, so the popup opens on the coach's choice rather
     * than on whichever row happened to sort first.
     */
    val evolutionTrees: List<EvolutionTreeItem> = emptyList(),

    /**
     * Bumped whenever [evolutionOffers] is recomputed, so the screen can seed
     * its queue once per change instead of on every recomposition.
     */
    val evolutionToken: Int = 0,
)

/**
 * What the user actually did in the session that just finished.
 *
 * The owner asked for this as "instead of having the 'workout completed' screen
 * simply put like that, add graphical information of a quick recap for what was
 * made today" — and the honest answer is that the ONLY number the screen had was
 * the set count. Everything here is accumulated from the sets the user actually
 * logged, not read back and estimated, so a set logged against a stack pin does
 * not add fake kilograms.
 */
data class WorkoutRecap(
    /** Distinct exercises trained. */
    val exercises: Int,
    val sets: Int,
    /**
     * Total load volume in the exercises' own units, as a raw sum. Left as
     * `null` when nothing weighted was trained: a rep-only or timed session has
     * no honest volume figure, and printing "0 kg" would read as "you did
     * nothing".
     */
    val volumeKg: Double?,
    val durationSeconds: Long,
    /** Muscle -> sets trained, most-trained first. Drives the distribution bar. */
    val setsByMuscle: Map<String, Int>,
    /**
     * One row per exercise trained, in the order it was first logged — the
     * "what was actually done" list beside the abstract tiles.
     */
    val exerciseRows: List<RecapExerciseRow> = emptyList(),
)

/**
 * One exercise in the recap's list: everything the row prints, straight from
 * the logged sets of the day.
 */
data class RecapExerciseRow(
    val exerciseName: String,
    val sets: Int,
    /** Logged reps per set, in the order the sets were logged. */
    val reps: List<Int>,
    /**
     * The distinct positive loads used (first-logged order); empty for a
     * bodyweight/timed exercise, which has no honest kilogram figure to show.
     */
    val weightsKg: List<Double>,
)

/**
 * The recap for everything logged on the current calendar day.
 *
 * Pure and top-level, so it is testable without an Android ViewModel (see
 * TodaysRecapFromLogTest) — one input, one answer, no prefs and no clock.
 * Returns null when nothing was trained today, which is the card's whole
 * visibility rule on both surfaces: no sets, no card.
 */
fun todaysRecapFromLog(
    sets: List<LoggedSetEntity>,
    startOfToday: Long,
): WorkoutRecap? {
    val today = sets
        .filter { it.timestamp >= startOfToday }
        .sortedWith(compareBy({ it.timestamp }, { it.setNumber }))
    if (today.isEmpty()) return null

    // First-logged order for the exercise list: the order the session happened.
    val byExercise = linkedMapOf<String, MutableList<LoggedSetEntity>>()
    today.forEach { set ->
        byExercise.getOrPut(set.exerciseName) { mutableListOf() }.add(set)
    }

    // Counts SETS ACTUALLY LOGGED per muscle, from the same muscleGroup field
    // the cards' badges carry, so the split cannot disagree with the rest of the app.
    val byMuscle = linkedMapOf<String, Int>()
    today.forEach { set ->
        val muscle = set.muscleGroup.ifBlank { "Other" }
        byMuscle[muscle] = (byMuscle[muscle] ?: 0) + 1
    }

    return WorkoutRecap(
        exercises = byExercise.size,
        sets = today.size,
        // Null rather than 0.0: a bodyweight or timed session has no honest
        // volume figure, and "0 kg" reads as "you did nothing".
        volumeKg = today.sumOf { it.weightKg * it.reps }.takeIf { it > 0.0 },
        // Span of the logged sets, not a session clock — a restart cannot know
        // how long the user sat between sets, and guessing would lie either way.
        // Timestamps are millis; the field is SECONDS (TodaysRecapFromLogTest
        // caught the missing /1000 that would print a 3-minute session as 3000).
        durationSeconds = ((today.last().timestamp - today.first().timestamp) / 1000L).coerceAtLeast(0L),
        setsByMuscle = byMuscle.entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value },
        exerciseRows = byExercise.map { (name, rows) ->
            RecapExerciseRow(
                exerciseName = name,
                sets = rows.size,
                reps = rows.map { it.reps },
                weightsKg = rows.map { it.weightKg }.filter { it > 0.0 }.distinct(),
            )
        },
    )
}

/**
 * One card on the post-workout Mastery & Branch section: an exercise that hit
 * its variation ladder's end or its researched human ceiling, with the branch
 * choices (or the true-ceiling message when none exist).
 */
data class MasteryCardData(
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String,
    val level: Int,
    val stageRoman: String,
    val load: Double?,
    val unit: String,
    val reason: ProgressionEngine.MasteryReason,
    val ceiling: HumanCeiling?,
    val branches: List<ProgressionBranch>,
    /** Suggested focus exercises shown on a true-ceiling card. */
    val suggestions: List<String>
)

/**
 * One mastered exercise plus every progression branch it offers, resolved.
 *
 * Holds the [MasteryCardData] itself so the popup can draw the trunk (the
 * exercise that was just finished) alongside the legs, rather than receiving an
 * already-paired before/after and losing the rest of the fork.
 */
data class EvolutionTreeItem(
    val card: MasteryCardData,
    /** Parallel to `card.branches`; an unrenderable branch drops out of both. */
    val offers: List<ProgressionEngine.EvolutionOffer>,
)

/** Which confirmation dialog the Settings level-system toggle is asking for. */
enum class LevelSystemConfirmation { NONE, DISABLE, ENABLE }

data class WorkoutSubTabRequest(
    val tab: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Whether a welcome screen that is on display belongs to the "replace the plan
 * I already have" flow.
 *
 * Extracted as a pure function so the rule that fixed the "every launch" trap is
 * testable without an Android ViewModel - see ReplacingPlanFlagTest. Both inputs
 * are PERSISTED, which is the point: the version that consulted only an
 * in-memory flag lost it on a kill and left `is_first_time = true` with no way
 * out of the welcome screen.
 *
 * [savedSetupType] is written by every path that saves a routine, so a plan that
 * exists counts as re-entry on its own - that half repairs an install that is
 * ALREADY stuck, whose transient flag is long gone.
 */
internal fun isReplacingAnExistingPlan(
    manualReentryPersisted: Boolean,
    savedSetupType: String?
): Boolean = manualReentryPersisted || !savedSetupType.isNullOrBlank()

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val allRoutineDays: StateFlow<List<RoutineDayEntity>>
    val allRoutineExercises: StateFlow<List<RoutineExerciseEntity>>
    val allLoggedSets: StateFlow<List<LoggedSetEntity>>

    /**
     * The day's recap, derived straight from today's logged sets so it survives
     * a process restart and can never disagree with the history. Null when
     * nothing has been trained today. See [todaysRecapFromLog].
     */
    val todayRecap: StateFlow<WorkoutRecap?>

    /**
     * The cached overall level for the home stat. Derived from the logged
     * history and the per-exercise levels — see [recomputeOverallLevel] — so the
     * UI only ever reads it, never recomputes it.
     */
    val userStats: StateFlow<UserStatsEntity?>

    /** The newest history seen from the database; the recompute reads this. */
    private var latestLoggedSets: List<LoggedSetEntity> = emptyList()

    /**
     * True once the app's own data is on screen: the default plan has been seeded
     * and the routine has emitted its stored rows.
     *
     * Owner, 2026-10-27 (dashboard task_4u2y69342j5q6i50422r464m): *"Dont load up
     * the app so slowly, dont force the user to stay on the loading screen for so
     * long, if the app loaded faster then load in faster but also not instantly
     * minimum loading time is 0.2s, if it needs longer to load then of course let
     * it load"*. So the splash is a GATE ON THIS FLAG, not a fixed timer: it can
     * only end when the data is there, and the 0.2 s floor lives in the splash
     * itself ([com.grafizum.repio.ui.screens.AppSplashScreen]).
     *
     * Readiness is the SEED *AND* THE FIRST EMISSION, in that order. The DAO flow
     * emits the empty table before the seeded plan lands on a fresh install, so
     * "first emission" alone would open the app on an empty day for a frame - the
     * same jolt the fixed timer was hiding.
     */
    private val _startupReady = MutableStateFlow(false)
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()

    private val prefs = getPlatformSettings("app_settings")
    private val timerManager = getTimerManager()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean("keep_screen_on", false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    // Motivational "you skipped a day" nudges (real athletes' catchphrases).
    private val _motivationalNudges = MutableStateFlow(prefs.getBoolean("motivational_nudges", true))
    val motivationalNudges: StateFlow<Boolean> = _motivationalNudges.asStateFlow()

    private val _defaultRestTimerSeconds = MutableStateFlow(prefs.getInt("default_rest_timer_seconds", 60))
    val defaultRestTimerSeconds: StateFlow<Int> = _defaultRestTimerSeconds.asStateFlow()

    private val _defaultExerciseTimerSeconds = MutableStateFlow(prefs.getInt("default_exercise_timer_seconds", 90))
    val defaultExerciseTimerSeconds: StateFlow<Int> = _defaultExerciseTimerSeconds.asStateFlow()

    private val _themeOption = MutableStateFlow(prefs.getString("theme_option", "DEFAULT") ?: "DEFAULT")

    init {
        // Drop a RETIRED theme id rather than translating it. RepIoApplication
        // already cleared it before any UI exists, so in practice this is a
        // belt-and-braces guard for a value that arrived some other way (a
        // downgrade/upgrade cycle, a restored backup). Writing a translated id
        // back here would be wrong on its own terms: the correct fallback is the
        // system default, and this class cannot know the system appearance - the
        // caller that can is RepIoApplication, which owns the repair.
        val stored = prefs.getString("theme_option", null)
        if (isRetiredThemeOption(stored)) {
            prefs.remove("theme_option")
            _themeOption.value = "DEFAULT"
        }
    }
    val themeOption: StateFlow<String> = _themeOption.asStateFlow()

    // Legacy stored values (goal_olympic, goal_crossfit, …) map onto one of
    // the five canonical goals at read time — no preference migration needed.
    private val _workoutGoal = MutableStateFlow(
        WorkoutTemplates.canonicalGoal(
            prefs.getString("workout_goal", "goal_bodybuilding") ?: "goal_bodybuilding"
        )
    )
    val workoutGoal: StateFlow<String> = _workoutGoal.asStateFlow()

    private val _developerThemeEnabled = MutableStateFlow(prefs.getBoolean("developer_theme_enabled", false))
    val developerThemeEnabled: StateFlow<Boolean> = _developerThemeEnabled.asStateFlow()

    private val _isFirstTimeUser = MutableStateFlow(prefs.getBoolean("is_first_time", true))
    val isFirstTimeUser: StateFlow<Boolean> = _isFirstTimeUser.asStateFlow()

    /**
     * Whether the welcome screen is the "replace the plan I already have" flow
     * rather than a true first run.
     *
     * PERSISTED, and derived from evidence rather than from a transient flag.
     * The owner reported (2026-10-08) "closing and reopening the app asks for a
     * new workout (welcome page shows and level system explanation) which should
     * only show the first time the user opens the app not every time".
     * `resetFirstTimeSetup()` writes `is_first_time = true`, but the re-entry
     * intent used to live ONLY in memory. Closing the app mid-flow therefore
     * left `is_first_time = true` with `isManualReentry = false`, and the
     * welcome screen came back on EVERY launch - with its cancel button and its
     * Back handler also gone, because both are gated on this flag. That is a
     * trap: the only way out was to build a new plan, which is exactly what the
     * "every time" report describes. Two halves, both needed:
     *
     *  - `setup_manual_reentry` persists the intent across the kill, so the
     *    cancel/Back affordance survives with it;
     *  - a routine that was ALREADY saved (`setup_type`, written by the save
     *    path and present on the owner's phone) counts as re-entry on its own,
     *    so an install already stuck in the trap is repaired on next launch
     *    without clearing its data.
     *
     * A genuine first run has neither, so the flag stays false and the welcome
     * screen correctly has no way to leave it.
     */
    private val _isManualReentry = MutableStateFlow(
        isReplacingAnExistingPlan(
            manualReentryPersisted = prefs.getBoolean("setup_manual_reentry", false),
            savedSetupType = prefs.getString("setup_type", null)
        )
    )
    val isManualReentry: StateFlow<Boolean> = _isManualReentry.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "system") ?: "system")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    /**
     * The unit weights are shown and entered in. Seeded once from the phone's
     * own country — a US gym has pounds on the rack — and persisted there, so a
     * later trip does not silently switch the numbers under the user. What they
     * picked always wins from then on.
     */
    private val _weightUnit = MutableStateFlow(
        WeightUnit.fromId(prefs.getString("weight_unit", null))
            ?: WeightUnit.defaultFor(Locale.getDefault()).also {
                prefs.putString("weight_unit", it.id)
            }
    )
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    private val _workoutNowState = MutableStateFlow(WorkoutNowState())
    val workoutNowState: StateFlow<WorkoutNowState> = _workoutNowState.asStateFlow()

    private val _requestedWorkoutSubTab = MutableStateFlow(WorkoutSubTabRequest(0))
    val requestedWorkoutSubTab: StateFlow<WorkoutSubTabRequest> = _requestedWorkoutSubTab.asStateFlow()

    private val _allAvailableExercises = MutableStateFlow<List<String>>(emptyList())
    val allAvailableExercises: StateFlow<List<String>> = _allAvailableExercises.asStateFlow()

    /**
     * Onboarding gate for the v5 level system: shown once per install (fresh
     * or after updating to this version) until the user completes the guide.
     * Cleared when a new routine replaces the old one, so a rebuilt plan
     * re-offers the walkthrough.
     */
    // Bumped from _v5 to _v6 when the Overall Level explainer joined the level
    // guide: the walkthrough now teaches both systems, so an install that already
    // dismissed the levels-only version is owed the combined one exactly once.
    private val _shouldShowLevelGuide = MutableStateFlow(!prefs.getBoolean("has_seen_level_guide_v6", false))
    val shouldShowLevelGuide: StateFlow<Boolean> = _shouldShowLevelGuide.asStateFlow()

    // The app-wide "stay on this level" pin (pref `level_lock_enabled`) used to live here with
    // its own switch in Settings. REMOVED by owner ruling 2026-10-09: "remove 'stay on this
    // level' switch in the settings completely", so the switch, the pref, this state and the
    // gate the level engine read are all gone. The lock a user can still set is the
    // PER-EXERCISE one on the workout card (routine_exercises.locked), which is untouched.

    /**
     * Per-exercise lock, persisted in the routine_exercises table: a locked
     * exercise is pinned to its current level and stage — the card's stepper
     * grays out, the level picker refuses jumps, and automatic progression
     * skips it. Every row of the same exercise (across days) flips together,
     * because the progression state itself is keyed by name.
     */
    fun setExerciseLocked(exerciseName: String, locked: Boolean) {
        viewModelScope.launch { repository.setExerciseLocked(exerciseName, locked) }
    }

    /**
     * Synchronous lock check for UI gating and progression writes, read from
     * the live routine flow (Room re-emits on any row change, so a toggle on
     * the card is visible here immediately).
     */
    fun isExerciseLockedNow(exerciseName: String): Boolean =
        allRoutineExercises.value.any {
            it.name.equals(exerciseName, ignoreCase = true) && it.locked
        }

    /**
     * The level system's master switch, exposed in Settings behind confirmations.
     * When OFF the app runs in "custom" mode: every per-exercise level display is
     * suppressed (the reactive loader clears the fields) and the routine editor's
     * sets/reps/weight/intensity inputs unlock, because nothing prescribes them
     * any more. Turning it back ON asks a second confirmation, then re-derives
     * every exercise's level from the weights the user last actually used — so
     * the prescriptions pick up where the free training left off instead of
     * snapping back to the old levels.
     */
    private val _levelSystemEnabled = MutableStateFlow(prefs.getBoolean("level_system_enabled", true))
    val levelSystemEnabled: StateFlow<Boolean> = _levelSystemEnabled.asStateFlow()

    /** Which confirmation the Settings toggle must show before flipping, if any. */
    val levelSystemConfirmation: StateFlow<LevelSystemConfirmation> =
        MutableStateFlow(LevelSystemConfirmation.NONE)

    /** Settings calls this on every toggle intent; the dialog asks via [confirmLevelSystemChange]. */
    fun requestLevelSystemChange(enabled: Boolean) {
        if (enabled == _levelSystemEnabled.value) return
        (levelSystemConfirmation as MutableStateFlow<LevelSystemConfirmation>).value =
            if (enabled) LevelSystemConfirmation.ENABLE else LevelSystemConfirmation.DISABLE
    }

    fun dismissLevelSystemConfirmation() {
        (levelSystemConfirmation as MutableStateFlow<LevelSystemConfirmation>).value =
            LevelSystemConfirmation.NONE
    }

    /** The confirmed flip: applies the change and, when re-enabling, re-syncs levels. */
    fun confirmLevelSystemChange() {
        val pending = levelSystemConfirmation.value
        dismissLevelSystemConfirmation()
        when (pending) {
            LevelSystemConfirmation.DISABLE -> applyLevelSystemEnabled(false)
            LevelSystemConfirmation.ENABLE -> {
                applyLevelSystemEnabled(true)
                resyncLevelsFromLastUsedWeights()
            }
            LevelSystemConfirmation.NONE -> Unit
        }
    }

    /**
     * Flip without confirmation — for the one in-app flow that already shows
     * its own dialog (the custom-mode confirm on the live workout screen) and
     * must not stack a second one.
     */
    fun setLevelSystemEnabled(enabled: Boolean) = applyLevelSystemEnabled(enabled)

    private fun applyLevelSystemEnabled(enabled: Boolean) {
        prefs.putBoolean("level_system_enabled", enabled)
        _levelSystemEnabled.value = enabled
        // The reactive loader's change key does not include this flag, so the
        // display has to be refreshed by hand: turning the system off clears
        // the level fields at once, turning it on re-derives them.
        if (enabled) {
            currentExerciseName()?.let { refreshProgressionDisplayNow(it) }
        } else {
            _workoutNowState.update {
                it.copy(
                    progressionLevel = null,
                    progressionStage = null,
                    progressionStageTargetReps = emptyList(),
                    progressionPrescribed = null,
                    currentSetTargetReps = null
                )
            }
        }
    }

    /**
     * Re-derives every level-managed exercise's level from the weight the user
     * LAST logged for it (the per-exercise "cache_weight_" memory). For each
     * exercise the stored ladder is walked to the rung whose prescribed load is
     * closest to that weight, and the state is rewritten there — so turning the
     * system back on picks up at roughly the weights the user was training,
     * rather than at the levels they happened to have when it was switched off.
     * Exercises with no remembered weight keep their stored state untouched.
     */
    private fun resyncLevelsFromLastUsedWeights() {
        val exercises = allRoutineExercises.value.map { it.name }.distinct()
        var changed = false
        exercises.forEach { name ->
            val entry = ProgressionEngine.entryFor(name) ?: return@forEach
            if (!ProgressionEngine.isProgressionReady(entry)) return@forEach
            if (ProgressionEngine.isDurationBased(entry)) return@forEach
            val lastWeight = prefs.getFloat("cache_weight_$name", Float.NaN)
            if (lastWeight.isNaN() || lastWeight <= 0f) return@forEach
            val target = lastWeight.toDouble()
            val effMax = ProgressionEngine.effectiveMaxLevel(entry, _weightUnit.value)
            var bestLevel = 1
            var bestDiff = Double.MAX_VALUE
            for (lvl in 1..effMax) {
                val load = ProgressionEngine.valueAtLevelWithCeiling(entry, lvl) ?: continue
                val diff = kotlin.math.abs(load - target)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestLevel = lvl
                }
            }
            val state = currentProgressionState(name)
            if (state == null || state.level != bestLevel) {
                prefs.putString(progressionKey(name), ProgressionEngine.serialize(ProgressionEngine.ProgressionState(bestLevel, 1)))
                changed = true
            }
        }
        if (changed) {
            refreshOverallLevel()
            currentExerciseName()?.let { refreshProgressionDisplayNow(it) }
        }
    }

    fun markLevelGuideSeen() {
        prefs.putBoolean("has_seen_level_guide_v6", true)
        _shouldShowLevelGuide.value = false
    }

    // Hoisted UI states to prevent "rescrolling" on recomposition
    val workoutNowScrollState = ScrollState(0)
    val dayChipsListState = LazyListState()

    init {
        val savedState = loadSavedWorkoutNowState()
        _workoutNowState.value = savedState

        val today = getTodayDayOfWeek()
        if (savedState.selectedDayOfWeek != today) {
            // Only force-select today if it's a new day, which will reset progress correctly
            selectDayForWorkoutNow(today)
        }

        // Pick a timer that was still running back up, countdown and all
        ensureRestTimerRunning()

        // The timer reports back from outside the UI — a finished rest through
        // [AppEventBus.restTimerEnded], a skipped one through restTimerSkipped. Winding the
        // session up here is what keeps it in step when the app was closed at the time.
        viewModelScope.launch {
            AppEventBus.restTimerEnded.collect { onRestTimerFinished() }  // payload: was a timed exercise
        }
        viewModelScope.launch {
            AppEventBus.restTimerSkipped.collect { stopTimer() }
        }

        allRoutineDays = repository.allRoutineDays.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allRoutineExercises = repository.allRoutineExercises.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allLoggedSets = repository.allLoggedSets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        todayRecap = repository.allLoggedSets
            .map { sets -> todaysRecapFromLog(sets, startOfTodayMillis()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        userStats = repository.userStats.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

        // The overall level is derived, so it is recomputed from its sources
        // rather than updated by each write path. Collecting the raw DAO flow (no
        // default value) means the first emission is the stored history, so an app
        // start recomputes against real data and can never overwrite the cached
        // row with a placeholder right before the real one arrives.
        viewModelScope.launch {
            repository.allLoggedSets.collect { sets ->
                latestLoggedSets = sets
                recomputeOverallLevel()
            }
        }

        viewModelScope.launch {
            repository.ensureDataSeeded()
            // The plan is in the database - now wait for the routine flow to report it,
            // then let the splash go. See [startupReady].
            repository.allRoutineExercises.first()
            _startupReady.value = true
        }

        // Pre-load exercise database in background to prevent lag during search
        viewModelScope.launch(Dispatchers.Default) {
            val exercises = WorkoutTemplates.getAllExerciseNames()
            _allAvailableExercises.value = exercises
        }

        // Reactive parameter cache loader
        viewModelScope.launch {
            combine(_workoutNowState, allRoutineExercises) { state, exercises ->
                val currentDayExs = exercises.filter { it.dayOfWeek == state.selectedDayOfWeek }
                val exercise = currentDayExs.getOrNull(state.currentExerciseIndex)
                // The stored level/stage is part of the change key: when a
                // level-up persists, the display fields must recompute even
                // though the routine entity itself did not change. Levels
                // default to 0 when the exercise has no progression state.
                val prog = exercise?.let { currentProgressionState(it.name) }
                Triple(exercise, prog?.level ?: 0, prog?.stage ?: 0)
            }.distinctUntilChanged()
                .collect { (exercise, _, _) ->
                    exercise?.let {
                        val cachedWeight = getCachedWeightForExercise(it.name)
                        val cachedReps = getCachedRepsForExercise(it.name)
                        val cachedSeconds = getCachedSecondsForExercise(it.name)
                        // THE ROUTINE STRING DOES NOT DECIDE THIS, and reading it here is
                        // what put a 30-second timer under a card saying "90 s" (owner,
                        // 2026-10-09: "it shows reps in the card yet the exercise is based
                        // on time and has a timer????"). A timed exercise whose stored
                        // routine row does not literally say "sec" - every row built
                        // before the level system, e.g. Jumping Jacks' "8–12 reps" - was
                        // read as a REP exercise here, so `nextSeconds` kept the restored
                        // default and `nextReps`/`minReps`/`maxReps` were taken from the rep
                        // range. The unit lives in the exercise's own progression row; the
                        // UI card was already switched to it (see the note in
                        // WorkoutNowScreen), and this is the same fix one layer down.
                        val isTimeBased = WorkoutTemplates.isExerciseTimeBased(it.name)
                        val isBodyweight = WorkoutTemplates.isBodyweightExercise(it.name)
                        val (minVal, maxVal) = parseRepsRange(it.targetReps)

                        _workoutNowState.update { state ->
                            // Suggested weight, in priority order: the weight the plan set explicitly
                            // (Edit screen), then what the user last used for this exercise, then the
                            // progression system's per-exercise starting prescription (v5), then the
                            // legacy equipment-class estimate. A remembered weight is never overwritten
                            // — EXCEPT a stale 0.0 on a loadable exercise: pre-v5 sessions stored
                            // 0.0 for rows that were bodyweight-classified by the old pool ("Dumbbell
                            // Shrug still shows bodyweight"), and nothing ever writes 0.0 for an
                            // exercise the current catalog says is loadable, so ignoring it once
                            // lets the prescription surface without erasing anything.
                            val templateWeight = it.targetWeightKg
                            val setupIntensity = prefs.getString("setup_intensity", "") ?: ""
                            val staleZero = cachedWeight != null && cachedWeight == 0.0 && !isBodyweight &&
                                templateWeight <= 0.0
                            val effectiveCached = if (staleZero) null else cachedWeight
                            val suggestedWeight = when {
                                templateWeight > 0.0 -> templateWeight
                                effectiveCached != null -> effectiveCached
                                isBodyweight -> 0.0
                                else -> WorkoutTemplates
                                    .getStartingWeightKg(it.name, setupIntensity)
                                    .takeIf { kg -> kg > 0.0 } ?: 20.0
                            }
                            val showBodyweight = isBodyweight && suggestedWeight == 0.0

                            // The current set's prescribed target (v5 level system).
                            val setTarget = targetRepsForCurrentSet(it.name, state.currentSetNumber)

                            // Level-system display fields for the header chip and
                            // prescription line (null when not in the system or
                            // when training in custom mode).
                            val progState = currentProgressionState(it.name)
                            val progEntry = progState?.let { _ -> ProgressionEngine.entryFor(it.name) }
                            val stageTargetReps = if (progState != null && progEntry != null) {
                                // Padded to the ROW's set count: the published pattern
                                // may be narrower than the template's sets (a 3-wide
                                // pattern under "Satz 1 von 4"), and the banner lists
                                // every set the row actually asks for.
                                ProgressionEngine.stageTargetsForSets(
                                    progEntry, progState.level, progState.stage, it.targetSets
                                )
                            } else emptyList()
                            val prescribed = if (progState != null && progEntry != null) {
                                ProgressionEngine.prescribedValueAtLevel(progEntry, progState.level, _weightUnit.value)
                            } else null
                            // Auto-select reps/seconds: in level mode the progression
                            // prescription leads; in custom mode it is the user's last
                            // choice (clamped to the plan's range), else the middle of
                            // the target rep range, or the upper time bound for time-based.
                            val nextReps = if (!isTimeBased) {
                                setTarget?.takeIf { it in minVal..maxVal }
                                    ?: cachedReps?.takeIf { it in minVal..maxVal }
                                    ?: WorkoutTemplates.getMiddleRep(it.targetReps)
                            } else {
                                state.currentReps
                            }
                            // A TIMED EXERCISE'S SECONDS COME FROM ITS LEVEL, the same way a
                            // rep exercise's reps do. The level card headlines the
                            // prescription ("90 s"), so the button under it has to start a
                            // timer for those 90 seconds - the routine's stored "30 sec"
                            // used to win here, which put a 30s timer on a card promising 90.
                            // Owner's report (2026-10-09): "it shows reps in the card yet the
                            // exercise is based on time and has a timer????" - the card and the
                            // button are one prescription and must not be able to disagree.
                            // A level the engine cannot resolve (custom mode) keeps the old
                            // fallback order: the user's last duration, else the plan's bound.
                            val prescribedSeconds = prescribed
                                ?.takeIf { it.kind == ProgressionEngine.ValueKind.DURATION }
                                ?.value?.toInt()
                                ?.takeIf { it in 15..3600 }
                            // The last fallback is the row's OWN seconds, read with
                            // [WorkoutTemplates.secondsInStoredTarget] instead of the
                            // rep-range bound. `maxVal` is the UPPER REP of the stored
                            // string, so a timed row still carrying "8-12" handed the
                            // timer 12 seconds - a rep range becoming a duration is the
                            // same mistake the unit sniff made, one layer down. A row
                            // with no plain duration falls back to 30.
                            val nextSeconds = if (isTimeBased) {
                                prescribedSeconds
                                    ?: cachedSeconds?.takeIf { it in 15..3600 }
                                    ?: WorkoutTemplates.secondsInStoredTarget(it.targetReps)
                                    ?: 30
                            } else {
                                state.currentExerciseSeconds
                            }

                            state.copy(
                                currentWeightKg = if (showBodyweight) 0.0 else suggestedWeight,
                                recommendedWeightKg = if (showBodyweight) 0.0 else suggestedWeight,
                                currentReps = nextReps,
                                currentSetTargetReps = setTarget,
                                progressionLevel = progState?.level,
                                progressionStage = progState?.stage,
                                progressionStageTargetReps = stageTargetReps,
                                progressionPrescribed = prescribed,
                                minReps = if (!isTimeBased) minVal else 1,
                                maxReps = if (!isTimeBased) maxVal else 100,
                                currentExerciseSeconds = nextSeconds,
                                minExerciseSeconds = if (isTimeBased) 15 else 10,
                                maxExerciseSeconds = if (isTimeBased) 3600 else 600
                            )
                        }
                    }
                }
        }

        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                tickTimer()
                // In-session level writes (arrow taps, bulk adjust) store the
                // level even when no workout state is saved yet — the same
                // path saveWorkoutNowState() covers after a logged set. The
                // reactive loader's change key alone can miss it.
                if (prefs.getLong("timer_end_time_millis", 0L) <= 0L &&
                    prefs.getInt("saved_total_sets_logged", 0) > 0
                ) {
                    currentExerciseName()?.let { currentProgressionState(it) }
                }
            }
        }
    }

    private fun saveWorkoutNowState(state: WorkoutNowState) {
        // ONE transaction, not nine. Each put used to open, serialise, write and fsync the prefs
        // file on its own, and all nine ran inside the frame the user tapped a day on - the
        // micro-lag reported as "lagging slightly when changing training days".
        prefs.edit {
            putString("saved_workout_day", state.selectedDayOfWeek)
            putInt("saved_exercise_index", state.currentExerciseIndex)
            putInt("saved_set_number", state.currentSetNumber)
            putFloat("saved_weight_kg", state.currentWeightKg.toFloat())
            putInt("saved_reps", state.currentReps)
            putInt("saved_exercise_seconds", state.currentExerciseSeconds)
            putBoolean("saved_is_completed", state.isWorkoutCompleted)
            putInt("saved_total_sets_logged", state.totalSetsLoggedToday)
            // The session is only valid for the calendar date it was saved on.
            putLong("saved_session_date", startOfTodayMillis())
        }
        saveDayProgress(state.selectedDayOfWeek, state)
    }

    /** Per-day session progress (exercise index, set number, completion, logged sets). */
    private data class DaySessionProgress(
        val exerciseIndex: Int = 0,
        val setNumber: Int = 1,
        val completed: Boolean = false,
        val totalSetsLogged: Int = 0
    )

    private fun loadDayProgress(day: String): DaySessionProgress? {
        val raw = prefs.getString("day_progress_$day", null) ?: return null
        val parts = raw.split(",")
        // Day progress only applies to the calendar date it was saved on. Progress from a
        // previous date (e.g. last week's same weekday) must not carry over, otherwise an
        // old "completed" session shows up as today's already-done workout.
        val savedDate = parts.getOrNull(4)?.toLongOrNull()
        if (savedDate == null || savedDate != startOfTodayMillis()) return null
        return DaySessionProgress(
            exerciseIndex = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            setNumber = parts.getOrNull(1)?.toIntOrNull() ?: 1,
            completed = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: false,
            totalSetsLogged = parts.getOrNull(3)?.toIntOrNull() ?: 0
        )
    }

    private fun saveDayProgress(day: String, state: WorkoutNowState) {
        prefs.putString(
            "day_progress_$day",
            "${state.currentExerciseIndex},${state.currentSetNumber},${state.isWorkoutCompleted},${state.totalSetsLoggedToday},${startOfTodayMillis()}"
        )
    }

    private fun loadSavedWorkoutNowState(): WorkoutNowState {
        val today = getTodayDayOfWeek()
        val savedDay = prefs.getString("saved_workout_day", null) ?: today
        val dayProgress = loadDayProgress(savedDay)

        // A session saved on a previous calendar date is stale: it must not masquerade
        // as today's workout (e.g. last week's completed Thursday showing as already done
        // this Thursday). Start today fresh instead.
        val sessionDate = prefs.getLong("saved_session_date", 0L)
        val isStaleSession = sessionDate != 0L && sessionDate != startOfTodayMillis()

        if (dayProgress == null || isStaleSession) {
            return WorkoutNowState(selectedDayOfWeek = today)
        }

        val savedExerciseIndex = dayProgress.exerciseIndex
        val savedSetNumber = dayProgress.setNumber
        val savedIsCompleted = dayProgress.completed
        val savedTotalSetsLogged = dayProgress.totalSetsLogged
        val savedWeightKg = prefs.getFloat("saved_weight_kg", 20.0f).toDouble()
        val savedReps = prefs.getInt("saved_reps", 10)
        val savedExSeconds = prefs.getInt("saved_exercise_seconds", 30)

        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        val isExTimer = prefs.getBoolean("is_exercise_timer", false)
        val now = currentTimeMillis()
        val remainingSecs = if (endTime > now) remainingRestSeconds(endTime, now) else 0
        val timerDay = prefs.getString("timer_day", null)
        // A HELD CLOCK HAS NO DEADLINE, so it cannot be found by the arithmetic above - it is the
        // stored "where the user stopped it" that has to bring it back, or a rebuild of this screen
        // (process death, a rotation that outlives the view model, the app being killed while the
        // clock was held) would silently start the user's set over. Its own flags say it was held
        // rather than that it never ran.
        val clockPaused = prefs.getBoolean("exercise_clock_paused", false) && remainingSecs <= 0
        val pausedClockSeconds = prefs.getInt("exercise_clock_remaining", 0)

        return WorkoutNowState(
            selectedDayOfWeek = savedDay,
            currentExerciseIndex = savedExerciseIndex,
            currentSetNumber = savedSetNumber,
            currentWeightKg = savedWeightKg,
            currentReps = savedReps,
            currentExerciseSeconds = savedExSeconds,
            isWorkoutCompleted = savedIsCompleted,
            restTimerSeconds = if (remainingSecs > 0) remainingSecs else if (clockPaused) pausedClockSeconds else 0,
            isTimerActive = remainingSecs > 0,
            isExerciseTimerActive = if (remainingSecs > 0) isExTimer else clockPaused,
            isExerciseClockPaused = clockPaused,
            timerDayOfWeek = if (remainingSecs > 0 || clockPaused) timerDay else null,
            totalSetsLoggedToday = savedTotalSetsLogged
        )
    }

    private fun currentTimeMillis(): Long = com.grafizum.repio.util.currentTimeMillis()

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        prefs.putBoolean("keep_screen_on", enabled)
    }

    fun setMotivationalNudges(enabled: Boolean) {
        _motivationalNudges.value = enabled
        prefs.putBoolean("motivational_nudges", enabled)
    }

    fun setDefaultRestTimerSeconds(seconds: Int) {
        val validSecs = seconds.coerceAtLeast(10)
        _defaultRestTimerSeconds.value = validSecs
        prefs.putInt("default_rest_timer_seconds", validSecs)
    }

    fun overrideAllWorkoutsRestTimer(seconds: Int) {
        val validSecs = seconds.coerceAtLeast(10)
        setDefaultRestTimerSeconds(validSecs)
        viewModelScope.launch {
            val currentExercises = repository.allRoutineExercises.first()
            val currentDays = repository.allRoutineDays.first()
            val updated = currentExercises.map { it.copy(restSeconds = validSecs) }
            repository.applyProgram(currentDays, updated)
        }
    }

    fun setDefaultExerciseTimerSeconds(seconds: Int) {
        val validSecs = seconds.coerceAtLeast(10)
        _defaultExerciseTimerSeconds.value = validSecs
        prefs.putInt("default_exercise_timer_seconds", validSecs)
    }

    fun overrideAllWorkoutsTransitionTimer(seconds: Int) {
        val validSecs = seconds.coerceAtLeast(10)
        setDefaultExerciseTimerSeconds(validSecs)
        viewModelScope.launch {
            val currentExercises = repository.allRoutineExercises.first()
            val currentDays = repository.allRoutineDays.first()
            val updated = currentExercises.map { it.copy(transitionRestSeconds = validSecs) }
            repository.applyProgram(currentDays, updated)
        }
    }

    /**
     * Pick a theme: the whole change happens here, and nothing is left for the user to do.
     *
     * This used to end with `_showRestartDialog.value = true` and a dialog whose single button
     * ran [applyThemeRestart] - i.e. it asked the user to restart, then killed the process to
     * honour the request. Neither half was necessary:
     *
     *  * the COLOURS are already live. `_themeOption` is what the Compose scheme reads, so they
     *    changed on the tap, before the dialog had even drawn - measured on emulator-5554: the
     *    preference flipped and the screen repainted while `pidof` returned the same PID;
     *  * the ICON now switches without killing, because [IconSwitchHelper.switchIcon] writes
     *    `DONT_KILL_APP` (see `iconSwitchFlags`).
     *
     * The splash theme is the one thing a live switch cannot repaint, and it does not need to:
     * it applies to the next cold start, which lands on the alias this call enables. So the
     * dialog is gone rather than reworded - a prompt that exists only to ask for a restart that
     * the app was causing itself is worse than no prompt.
     *
     * OWNER REQUEST, 2026-10-14: "can we change the theme and icon without needing to close and
     * reopen the app manually".
     */
    fun setThemeOption(theme: String) {
        val oldTheme = _themeOption.value
        _themeOption.value = theme
        prefs.putString("theme_option", theme)
        if (oldTheme != theme) {
            applyAppIconChange(theme)
        }
    }

    fun updateThemeOptionSilent(theme: String) {
        if (_themeOption.value != theme) {
            _themeOption.value = theme
            prefs.putString("theme_option", theme)
        }
    }

    fun setDeveloperThemeEnabled(enabled: Boolean) {
        _developerThemeEnabled.value = enabled
        prefs.putBoolean("developer_theme_enabled", enabled)
    }

    fun setAppLanguage(languageCode: String) {
        _appLanguage.value = languageCode
        prefs.putString("app_language", languageCode)
    }

    fun setWeightUnit(unit: WeightUnit) {
        _weightUnit.value = unit
        prefs.putString("weight_unit", unit.id)
    }

    fun setWorkoutGoal(goal: String) {
        val canonical = WorkoutTemplates.canonicalGoal(goal)
        _workoutGoal.value = canonical
        prefs.putString("workout_goal", canonical)
    }

    fun setRequestedWorkoutSubTab(tab: Int) {
        _requestedWorkoutSubTab.value = WorkoutSubTabRequest(tab)
    }


    fun completeFirstTimeSetup(
        days: List<RoutineDayEntity>,
        exercises: List<RoutineExerciseEntity>,
        setupType: String = "CUSTOM",
        bodyParts: List<String> = emptyList(),
        selectedDays: List<String> = emptyList(),
        intensity: String = "",
        templateId: String = "",
        equipment: List<String> = emptyList(),
        workoutDuration: String = WorkoutTemplates.DURATION_STANDARD,
        splitStyle: String = WorkoutTemplates.SplitStyle.DEFAULT.id
    ) {
        viewModelScope.launch {
            // Completing a brand-new build from the Welcome screen replaces the routine,
            // so its memory (last-used weights/reps/seconds and the in-progress session) resets.
            val previousExerciseNames = repository.allRoutineExercises.first().map { it.name }
            // Premade builds can restrict which equipment the template may use.
            // A hand-built (CUSTOM) routine is gated the same way: the default
            // prefill and the picker pool both carry barbell staples, so a user
            // who said they have no barbell must not see one saved into the
            // routine either. Their own hand-set reps/rest are kept as-is.
            val effectiveExercises = when {
                setupType == "PREMADE" && equipment.isNotEmpty() ->
                    mapToEquipmentFiltered(exercises, equipment, reparameterize = true)
                setupType == "CUSTOM" && equipment.isNotEmpty() ->
                    mapToEquipmentFiltered(exercises, equipment, reparameterize = false)
                else -> exercises
            }
            repository.applyProgram(days, effectiveExercises)
            clearExerciseMemories((previousExerciseNames + effectiveExercises.map { it.name }).distinct())
            resetSessionProgress()
            val wasManual = _isManualReentry.value
            _isFirstTimeUser.value = false
            _isManualReentry.value = false
            // ONE commit for the whole setup. These were ten sequential transactions, each
            // opening and fsyncing the prefs file, back to back on the frame the user finished
            // the welcome flow - the longest such burst in the app.
            prefs.edit {
                putBoolean("is_first_time", false)
                // The replacement flow is over, so the persisted intent must not
                // survive it (a later "New" sets it again).
                putBoolean("setup_manual_reentry", false)

                // Persist setup parameters for later goal changes
                putString("setup_type", setupType)
                putString("setup_body_parts", bodyParts.joinToString(","))
                putString("setup_days", selectedDays.joinToString(","))
                putString("setup_intensity", intensity)
                putString("setup_template_id", templateId)
                putString("setup_equipment", equipment.joinToString(","))
                putString("setup_duration", workoutDuration)
                putString("setup_split_style", splitStyle)
            }
            
            setRequestedWorkoutSubTab(if (wasManual) 1 else 0)
        }
    }

    private fun mapToEquipmentFiltered(
        exercises: List<RoutineExerciseEntity>,
        equipment: List<String>,
        reparameterize: Boolean
    ): List<RoutineExerciseEntity> {
        // Fine-tag gate: null = unrestricted, empty granted set = strict
        // bodyweight-only. No-kit movements pass any granted set, so pure
        // bodyweight presets never strip a plan to zero.
        val grantedTags = WorkoutTemplates.grantedTagsForPublic(equipment)
        val base = if (grantedTags == null) exercises else exercises.mapNotNull { ex ->
            if (ExerciseEquipmentTags.isAvailable(ex.name, grantedTags)) ex
            else {
                val sub = WorkoutTemplates.substituteExercise(ex.name, equipment)
                sub?.let { ex.copy(name = it) }
            }
        }
        if (!reparameterize) return base
        return base.map {
            it.copy(
                targetReps = WorkoutTemplates.getGoalReps(it.name, _workoutGoal.value),
                restSeconds = WorkoutTemplates.getGoalRestSeconds(_workoutGoal.value, it.name),
                tempo = ""
            )
        }
    }

    /** The equipment restriction saved with the last build (empty = no restriction). */
    private fun loadSetupEquipment(): List<String> {
        val raw = prefs.getString("setup_equipment", null) ?: return emptyList()
        if (raw.isBlank() || raw == WorkoutTemplates.EQUIPMENT_ALL) return emptyList()
        // Pre-tag-era selections ("Dumbbell", "Bodyweight", …) normalize into
        // the fine-tag model (preset ids / custom tag lists) on read.
        return ExerciseEquipmentTags.migrateLegacy(raw.split(",").map { it.trim() }.filter { it.isNotEmpty() })
    }

    /**
     * The saved equipment as coarse categories ("Barbell", "Dumbbell", …),
     * for picker defaults: an empty list means the user never restricted
     * their kit. A no-barbell user's list simply has no "Barbell" in it —
     * which is what the recommendation filters should start from.
     */
    fun userEquipmentCategories(): List<String> =
        ExerciseEquipmentTags.coarseCategoriesFor(loadSetupEquipment())

    /**
     * The saved equipment EXACTLY as it was written, for pre-filling a picker.
     *
     * [userEquipmentCategories] and [userGrantedEquipmentTags] both go through
     * [ExerciseEquipmentTags], which flattens the selection into other
     * vocabularies — and the coarse one cannot express a chair, a bench or a
     * pull-up bar at all. That is fine for gating, and wrong for a control that
     * has to show the user's own tiles lit: it would open on a selection the
     * user never made. This returns the stored list (preset id first, then fine
     * tags) so the equipment block opens on what is actually saved.
     */
    fun savedEquipmentSelection(): List<String> = loadSetupEquipment()

    /**
     * The saved equipment as the user's granted FINE tags, for the pickers'
     * equipment gate.
     *
     * [userEquipmentCategories] cannot drive that gate on its own: it is the
     * coarse chip vocabulary, which has no category for a chair, a bench or a
     * pull-up bar, so re-expanding it silently drops those grants. Null =
     * unrestricted (the user never restricted their kit), which is a different
     * thing from an empty set = strict bodyweight only.
     */
    fun userGrantedEquipmentTags(): Set<String>? =
        WorkoutTemplates.grantedTagsForPublic(loadSetupEquipment())

    /** The session length saved with the last build (defaults to the standard window). */
    private fun loadSetupDuration(): String =
        prefs.getString("setup_duration", null)
            ?.takeIf { WorkoutTemplates.isValidDuration(it) }
            ?: WorkoutTemplates.DURATION_STANDARD

    /**
     * The week style saved with the last build. An unknown or absent value
     * falls back to the default rather than failing, so a build made before the
     * style picker existed keeps working.
     */
    private fun loadSetupSplitStyle(): WorkoutTemplates.SplitStyle =
        WorkoutTemplates.SplitStyle.fromId(prefs.getString("setup_split_style", null))

    fun regenerateProgramWithNewGoal(newGoal: String) {
        viewModelScope.launch {
            setWorkoutGoal(newGoal)
            
            val setupType = prefs.getString("setup_type", "CUSTOM") ?: "CUSTOM"
            val bodyParts = prefs.getString("setup_body_parts", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val selectedDays = prefs.getString("setup_days", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val intensity = prefs.getString("setup_intensity", "") ?: ""
            val templateId = prefs.getString("setup_template_id", "") ?: ""
            val equipment = loadSetupEquipment()
            val duration = loadSetupDuration()
            val splitStyle = loadSetupSplitStyle()
            
            when (setupType) {
                "ASSISTED" -> {
                    val program = WorkoutTemplates.generateEngineProgram(
                        bodyParts = bodyParts,
                        selectedDays = selectedDays,
                        intensity = intensity,
                        workoutGoal = newGoal,
                        equipment = equipment,
                        duration = duration,
                        splitStyle = splitStyle
                    )
                    repository.applyProgram(program.days, program.exercises)
                }
                "PREMADE" -> {
                    val template = WorkoutTemplates.allTemplates.find { it.id == templateId }
                    if (template != null) {
                        // For premade, we need to know which days were selected. 
                        // setup_days should contain the selected week days.
                        val mapped = remapTemplateToDays(template, selectedDays)
                        val filteredExercises = mapToEquipmentFiltered(mapped.second, equipment, reparameterize = true)
                        repository.applyProgram(mapped.first, filteredExercises)
                    }
                }
                else -> {
                    // CUSTOM: Just update reps for existing exercises
                    val currentExercises = repository.allRoutineExercises.first()
                    val currentDays = repository.allRoutineDays.first()
                    val updatedExercises = currentExercises.map {
                        it.copy(
                            targetReps = WorkoutTemplates.getGoalReps(it.name, newGoal),
                            restSeconds = WorkoutTemplates.getGoalRestSeconds(newGoal, it.name)
                        )
                    }
                    repository.applyProgram(currentDays, updatedExercises)
                }
            }
        }
    }

    fun applyWorkoutTemplate(template: com.grafizum.repio.data.WorkoutTemplate) {
        viewModelScope.launch {
            // The equipment promise outlives the setup wizard: a template
            // applied from the routine screen is gated by the SAME selection,
            // so a no-barbell user picking a barbell-heavy premade gets its
            // rows substituted down the nearest link they can actually load
            // (Barbell Back Squat -> Dumbbell Squat -> …). The template's own
            // sets/reps stay untouched — only the movements are gated.
            val equipment = loadSetupEquipment()
            val gated = if (equipment.isNotEmpty())
                mapToEquipmentFiltered(template.exercises, equipment, reparameterize = false)
            else template.exercises
            repository.applyProgram(template.dayTemplates.map {
                RoutineDayEntity(it.dayOfWeek, it.dayOfWeek, "", it.isRestDay)
            }, gated)
            // The EXPLICIT intent, not the derived flag: this path is reached
            // from a template picker, never from resetFirstTimeSetup's welcome
            // flow, so it must keep landing on Home exactly as it always did.
            // The derived flag is true for every install that has a saved plan.
            val wasManual = prefs.getBoolean("setup_manual_reentry", false)
            _isFirstTimeUser.value = false
            _isManualReentry.value = false
            prefs.putBoolean("is_first_time", false)
            prefs.putBoolean("setup_manual_reentry", false)
            setRequestedWorkoutSubTab(if (wasManual) 1 else 0)
        }
    }

    fun resetFirstTimeSetup() {
        _isFirstTimeUser.value = true
        _isManualReentry.value = true
        prefs.putBoolean("is_first_time", true)
        // Both halves of the state are written together, or a kill in between
        // leaves a welcome screen the user cannot leave - see the property above.
        prefs.putBoolean("setup_manual_reentry", true)
    }

    fun cancelFirstTimeSetup() {
        _isFirstTimeUser.value = false
        _isManualReentry.value = false
        prefs.putBoolean("is_first_time", false)
        prefs.putBoolean("setup_manual_reentry", false)
    }

    fun selectDayForWorkoutNow(dayOfWeek: String) {
        val currentState = _workoutNowState.value
        // If it's already the selected day, don't reset progress (preserves state on tab switch/app resume)
        if (currentState.selectedDayOfWeek == dayOfWeek) {
            return
        }

        // THE FALLBACK FOR A SESSION NEVER FINISHED: a level-up held by a day the
        // user walked away from is applied here rather than being held forever.
        // See [applyPendingLevelUps] - nothing can be lost by forgetting to press
        // finish; it can only be late.
        applyPendingLevelUps()

        // Save the progress of the day we are leaving
        saveDayProgress(currentState.selectedDayOfWeek, currentState)

        // Restore the target day's own progress (fresh defaults if never started)
        val target = loadDayProgress(dayOfWeek) ?: DaySessionProgress()
        val newState = currentState.copy(
            selectedDayOfWeek = dayOfWeek,
            currentExerciseIndex = target.exerciseIndex,
            currentSetNumber = target.setNumber,
            isWorkoutCompleted = target.completed,
            totalSetsLoggedToday = target.totalSetsLogged
        )
        _workoutNowState.value = newState
        saveWorkoutNowState(newState)
    }

    fun navigateToExercise(index: Int, totalExercises: Int) {
        if (index in 0 until totalExercises) {
            _workoutNowState.update { it.copy(currentExerciseIndex = index, currentSetNumber = 1) }
            saveWorkoutNowState(_workoutNowState.value)
        }
    }

    // `progressionLabel()` lived here: it built the "L5 · I" debug readout the
    // workout card printed under its reps banner. Deleted 2026-10-11 with the line
    // it fed - the header's level box already shows the same two numbers, so the
    // label was a duplicate, and a label no caller reads is the dead-parameter
    // class this project has already paid for once (see the skill log's
    // observation on dead callbacks). Do not reintroduce a second level readout on
    // the card; if a screen needs the level and the stage as text, read
    // [currentProgressionState] and format it at that screen.

    /**
     * The highest level the picker and arrows may offer: the program's own
     * ladder end, lifted to the extension grid (50, then 100 … up to 500) the
     * user has unlocked for this exercise.
     *
     * A REP-PROGRESSED row (bodyweight variation, hold, cardio) is the
     * exception, and it is not a small one. The extension grid is honest for a
     * LOAD row because extrapolating a ladder gives a real weight — but a rep
     * ladder has already run out by the time anyone would want to extend it: a
     * bodyweight row stops at its rep CEILING (50 reps, or the hold's own
     * seconds ladder), and past that the rows would repeat. So the picker ends
     * where the ladder does, at [ProgressionEngine.effectiveMaxLevel].
     *
     * This used to be justified differently and wrongly: the reason was that
     * [ProgressionEngine.variationAtLevel] repeated the hardest movement past
     * the last rung, so Crunches showed 50 rows of which 44 read "Dragon Flag".
     * A rep ladder no longer names a movement at all, and the rows below its
     * ceiling are genuinely distinct — but there is still nothing to extend TO,
     * which is the part that decides this.
     */
    fun progressionMaxLevel(exerciseName: String): Int {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return 1
        if (!ProgressionEngine.isProgressionReady(entry)) return 1
        // OWNER RULING 2026-10-04: "for the max level create different max levels
        // for each exercise based on when it cant be increased anymore ... i want
        // each exercise to have their cealing to be humane and nothing over
        // humanly possible. make the max level of each exercise realistic".
        //
        // THIS USED TO FLOOR EVERY EXERCISE AT [ProgressionEngine.EXTENSION_CHUNK]
        // (50), whatever the row's own end was. Measured on the shipped pool: the
        // declared maxLevel is the CSV ladder's LENGTH, not a ceiling - 6, 30, 4,
        // 15, 40, 20, 10 - and it bears no relation to when an exercise stops
        // progressing. So "50 for everything" was not a conservative default, it
        // was an invented one, and the "extend" grid could push it to 500.
        //
        // WHAT REPLACES IT is the answer the row already carries, and it is not
        // invented either: 478 of the 594 rows have a sourced [HumanCeiling]
        // ("elite dumbbell curl ~45-50 kg/arm"), and
        // [ProgressionEngine.effectiveMaxLevel] maps the ladder onto it. The
        // remaining 116 are `variation_step` rows with no load at all, and for
        // those the ceiling is the rep climb - [ProgressionEngine.repRungs], which
        // tops out at the 50-rep end of the owner's checkpoint ladder. So every
        // row now ends where its own evidence says it ends.
        //
        // ADD MORE LEVELS AFTER IT (owner 2026-10-13): the evidence end is a
        // MARKER, not the end of the list. "keep each exercise's max as is and add
        // more levels after it ... past-max levels must be selectable". So the
        // exercised own max is the floor of this list, and the extension grid the
        // user unlocks for the exercise ([extensionLevelFor], in
        // [ProgressionEngine.EXTENSION_CHUNK] steps up to
        // [ProgressionEngine.PICKER_LEVEL_LIMIT]) is how far past it the list runs.
        // The rows above the max are real: `valueAtLevelWithCeiling` keeps climbing
        // by the row's own increment and `repsAtLevel` by its own rep step, which is
        // why the writer can accept a rung up there at all - it is a load the user
        // chose, drawn from the same arithmetic as the rest of the ladder.
        return maxOf(
            ProgressionEngine.effectiveMaxLevel(entry, _weightUnit.value),
            extensionLevelFor(exerciseName)
        )
    }

    /** The number of Roman stages in the exercise's rep pattern (its width). */
    fun patternStageCount(exerciseName: String): Int {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return 1
        return ProgressionEngine.patternLength(entry).coerceAtLeast(1)
    }

    /**
     * The exercise's level for display: the stored one, or the starting
     * prescription of the active difficulty when nothing is stored yet. This
     * is what the adjustment UI should show as the current rung.
     */
    fun progressionDisplayLevel(exerciseName: String): Int? {
        val state = currentProgressionState(exerciseName) ?: return null
        return state.level
    }

    /**
     * The level the USER has for this exercise, or null when none has been chosen yet.
     *
     * WHY THIS EXISTS BESIDE [progressionDisplayLevel]: that one also answers with the level the
     * setup difficulty WOULD start the exercise at when nothing is stored, so every screen that
     * printed it as "the level" showed a number the user never picked. Owner, 2026-10-16: *"It
     * shows the setup default, not mine"* - and measured on his own device that day, 15 of the 23
     * exercises he had actually trained carried no stored state at all, so each of them announced
     * a level that would quietly move the moment he changed his setup difficulty.
     *
     * The difference is not cosmetic. A derived value is a PRESCRIPTION for the next session and
     * has to stay one: a session still needs a rung to train at for an exercise nobody has
     * levelled yet, which is why [progressionDisplayLevel] keeps its meaning. This read answers
     * the other question - has the user actually chosen one - and it is the one a label wants.
     * A caller that prints "New" for null is saying something true; a caller that prints a
     * derived number as the user's own is not.
     */
    fun storedProgressionLevel(exerciseName: String): Int? {
        if (!_levelSystemEnabled.value) return null
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return null
        if (!ProgressionEngine.isProgressionReady(entry)) return null
        val stored = ProgressionEngine.deserialize(prefs.getString(progressionKey(exerciseName), null))
            ?: return null
        // Same bound the writer uses - see [currentProgressionState].
        return stored.level.coerceIn(1, progressionMaxLevel(exerciseName))
    }

    /**
     * The prescribed value at a level for display, or null outside the system.
     * Used by the level-adjust UI to preview what a rung means in kg/variation.
     */
    fun progressionPrescribedAt(exerciseName: String, level: Int): ProgressionEngine.PrescribedValue? {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return null
        if (!ProgressionEngine.isProgressionReady(entry)) return null
        return ProgressionEngine.prescribedValueAtLevel(entry, level, _weightUnit.value)
    }

    /**
     * True when the exercise participates in the level system and can be
     * level-adjusted. Non-ready rows (coach review, manual review) stay fixed.
     */
    fun isLevelAdjustable(exerciseName: String): Boolean {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return false
        return ProgressionEngine.isProgressionReady(entry)
    }

    /**
     * Persists the user's chosen level for an exercise. The stage resets to I
     * — a manual move is a fresh start at the new rung, matching the CSV's
     * rep_reset policy. Never touches history; affects future sessions only.
     * The reactive loader recomputes the display fields from the stored state.
     */
    fun setExerciseLevel(exerciseName: String, level: Int) {
        // A locked exercise stays where it is — manual jumps from the level
        // picker included. Unlock it from the card's lock icon first.
        //
        // The jump is still allowed, and still reaches any level the exercise
        // genuinely has; what it can no longer reach is a level past the ceiling.
        if (isExerciseLockedNow(exerciseName)) return
        applyExerciseLevel(exerciseName, level)
    }

    /**
     * The level write itself, with no lock gate. [setExerciseLevel] adds the
     * gate; [setExerciseLevelFromEditor] deliberately does not.
     */
    private fun applyExerciseLevel(exerciseName: String, level: Int) {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return
        if (!ProgressionEngine.isProgressionReady(entry)) return
        // A manual jump is a SELECTION, not a completion: whatever the exercise
        // had actually finished stops counting (see [markProgressionEarned]).
        clearProgressionEarned(exerciseName)
        // The ceiling is the exercise's OWN humane end - see [progressionMaxLevel].
        // It used to be the extension grid (>= 50, growable to 500), which meant a
        // jump to a level past any humanly possible load was a valid selection and
        // could be logged. [ProgressionEngine.effectiveMaxLevel] stops at the row's
        // sourced HumanCeiling, or at the rep ceiling for a ladder with no load.
        val clamped = level.coerceIn(1, progressionMaxLevel(exerciseName))
        prefs.putString(progressionKey(exerciseName), ProgressionEngine.serialize(ProgressionEngine.ProgressionState(clamped, 1)))
        refreshProgressionDisplayNow(exerciseName)
        // A manual jump changes an input of the overall level without touching
        // the logged history, so the cached value has to be refreshed by hand.
        refreshOverallLevel()
    }

    /**
     * Set the level FROM AN EXERCISE EDITOR, which is an edit pass in itself.
     *
     * Owner's ask: "the editing of an exercise, allow the user to edit the level
     * while in level system mode". The editor already owns the pencil's job — it
     * is where the user goes to change an exercise — so it has to be able to
     * change the level, and the greyed sets/reps/weight fields beside it are
     * exactly the thing that read as "you cannot edit this here".
     *
     * WHY THIS IS NOT JUST [setExerciseLevel]: that one returns early on a
     * locked exercise, which is the pin the card's tick writes. Routing the
     * editor through it would mean the control silently does nothing on every
     * exercise the user has ever committed - the "greyed field swallows the tap"
     * failure this project has now paid for twice (the level fields, the level
     * pill). So the editor's write skips the gate and applies the chosen rung.
     *
     * IT NO LONGER UNLOCKS THE EXERCISE. It used to, on the reasoning that an
     * edit pass is the user's own hand and a committed pin should not outlive it;
     * the owner reversed that on 2026-10-31 (dashboard task_0d4343336a522u55252f4v23):
     * *"Never auto unlock an exercise, if the user locked it, it stays locked until
     * he unlocks it"*. Dropping the pin also has a cost the editor's write never
     * needed to pay: the lock is what stops the day-end level-up, so an edit that
     * silently cleared it changed what the NEXT session would do. The pin is
     * cleared in exactly one place now - the card's own lock control.
     *
     * Level 1 is a real level, so no clamping of the user's choice - only
     * [progressionMaxLevel]'s humane ceiling applies, inside [applyExerciseLevel].
     */
    fun setExerciseLevelFromEditor(exerciseName: String, level: Int) {
        rememberEditPassOrigin(exerciseName)
        applyExerciseLevel(exerciseName, level)
    }

    /**
     * Set the level AND the Roman stage FROM THE EXERCISE EDITOR.
     *
     * Owner, 2026-10-08: the editor's level list shows each level with its Roman
     * stages beneath it, "and when he selects it directly apply it". Choosing a
     * Roman numeral IS a choice of level + stage — level 5 stage III is one
     * instruction, not two — so it lands as ONE write. Routing it through
     * [setExerciseLevelFromEditor] would apply the level and silently reset the
     * stage to I ([applyExerciseLevel] does exactly that), which for the user who
     * just tapped "III" would look like the tap failed.
     *
     * Same edit-pass contract as the level-only writer, INCLUDING the lock: it does
     * not clear the pin either (owner, 2026-10-31: "Never auto unlock an exercise, if
     * the user locked it, it stays locked until he unlocks it"). The chosen stage is
     * still written, so the control answers the tap; what survives is the user's own
     * "stay on this level". The level is clamped to the exercise's own humane ceiling
     * and the stage to that level's pattern length, so neither can be written out of
     * range.
     */
    fun setExerciseStateFromEditor(exerciseName: String, level: Int, stage: Int) {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return
        if (!ProgressionEngine.isProgressionReady(entry)) return
        // The first editor write of this pass owns the snapshot Cancel restores.
        rememberEditPassOrigin(exerciseName)
        // Picking a Roman numeral is a selection like any other — see
        // [applyExerciseLevel] for why the earned marker goes with it.
        clearProgressionEarned(exerciseName)
        val levelRung = level.coerceIn(1, progressionMaxLevel(exerciseName))
        val stageRung = stage.coerceIn(1, ProgressionEngine.patternLength(entry).coerceAtLeast(1))
        prefs.putString(
            progressionKey(exerciseName),
            ProgressionEngine.serialize(
                ProgressionEngine.ProgressionState(levelRung, stageRung)
            )
        )
        refreshProgressionDisplayNow(exerciseName)
        // A manual jump changes an input of the overall level without touching the
        // logged history, so the cached value has to be refreshed by hand - the same
        // reason [applyExerciseLevel] does it.
        refreshOverallLevel()
    }

    /**
     * Recomputes the cached overall level from the newest history. Called
     * automatically when the history changes; call it directly after an edit
     * that changes a per-exercise level without logging anything.
     */
    fun refreshOverallLevel() {
        viewModelScope.launch { recomputeOverallLevel() }
    }

    /**
     * Recomputes [OverallLevelCalculator]'s result and stores it in user_stats.
     *
     * Qualifying exercises are strength exercises the level system can rate and
     * the user has logged at least once; the level comes from the same per-exercise
     * progression state the workout screens show, so the overall level can never
     * disagree with the individual ones. Each exercise's own (ceiling-aware)
     * ladder length goes with the sample, so every exercise lands on the shared
     * 0-100 progress axis before the median is taken — without it a level 6 of 6
     * would be counted level-for-level with a level 6 of 30.
     *
     * The stored figure is that 0-100 median and is backend-only: nothing in the
     * UI prints it, and [OverallLevelRank] turns it into the letter the user sees.
     */
    private suspend fun recomputeOverallLevel() {
        val samples = OverallLevelCalculator.qualifyingSamples(
            loggedSets = latestLoggedSets,
            levelFor = { name -> currentProgressionState(name)?.level }
        )
        val now = currentTimeMillis()
        val result = OverallLevelCalculator.compute(samples, now)
        repository.saveUserStats(
            UserStatsEntity(
                overallStrengthLevel = result.level,
                overallLevelReady = result.ready,
                overallLevelLastComputed = now,
                exercisesCountForLevel = result.exerciseCount
            )
        )
    }

    /** Adjusts the current level by [delta], clamped to the ladder. Locked exercises do not move. */
    fun adjustExerciseLevel(exerciseName: String, delta: Int) {
        if (isExerciseLockedNow(exerciseName)) return
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return
        if (!ProgressionEngine.isProgressionReady(entry)) return
        val current = progressionDisplayLevel(exerciseName) ?: return
        setExerciseLevel(exerciseName, current + delta)
    }

    /**
     * Manual stage stepping WITHIN the current level — what the chip's
     * arrows do. The stage walks I → II → III … up to the pattern's last
     * stage; the arrows move through these Roman stages one at a time and
     * clamp at the ends (the level itself only moves through earned
     * progression or the level-details dialog).
     */
    fun adjustExerciseStage(exerciseName: String, delta: Int) {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return
        if (!ProgressionEngine.isProgressionReady(entry)) return
        val state = currentProgressionState(exerciseName) ?: return
        val lastStage = ProgressionEngine.patternLength(entry).coerceAtLeast(1)
        val next = (state.stage + delta).coerceIn(1, lastStage)
        if (next == state.stage) return
        clearProgressionEarned(exerciseName)
        prefs.putString(progressionKey(exerciseName), ProgressionEngine.serialize(ProgressionEngine.ProgressionState(state.level, next)))
        refreshProgressionDisplayNow(exerciseName)
    }

    /**
     * One Roman stage forward, rolling over into the NEXT level's stage I
     * when the pattern's last stage is already showing. The card's inner
     * single-chevron button drives this: Roman numbers move one at a time,
     * and at the last Roman number the level itself steps up.
     */
    fun advanceStageOrLevel(exerciseName: String) {
        if (isExerciseLockedNow(exerciseName)) return
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return
        if (!ProgressionEngine.isProgressionReady(entry)) return
        val state = currentProgressionState(exerciseName) ?: return
        val lastStage = ProgressionEngine.patternLength(entry).coerceAtLeast(1)
        if (state.stage < lastStage) {
            adjustExerciseStage(exerciseName, +1)
        } else {
            // Clamped by setExerciseLevel, and the reset to stage I is exactly
            // what the roll-over means.
            setExerciseLevel(exerciseName, state.level + 1)
        }
    }

    /**
     * One Roman stage back; from stage I it rolls into the PREVIOUS level's
     * last stage, so walking down replays the same ladder the walk up built.
     */
    fun retreatStageOrLevel(exerciseName: String) {
        if (isExerciseLockedNow(exerciseName)) return
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return
        if (!ProgressionEngine.isProgressionReady(entry)) return
        val state = currentProgressionState(exerciseName) ?: return
        val lastStage = ProgressionEngine.patternLength(entry).coerceAtLeast(1)
        when {
            state.stage > 1 -> adjustExerciseStage(exerciseName, -1)
            state.level > 1 -> {
                prefs.putString(
                    progressionKey(exerciseName),
                    ProgressionEngine.serialize(ProgressionEngine.ProgressionState(state.level - 1, lastStage))
                )
                refreshProgressionDisplayNow(exerciseName)
            }
        }
    }

    /**
     * One row of the level-details list: what a rung trains at.
     *
     * [isBeyondPlan] is legacy: extrapolated ladders give every level a real
     * prescription, so no row is inert any more — it stays false for every
     * row the dialog builds.
     */
    data class LevelDetailRow(
        val level: Int,
        val prescribed: ProgressionEngine.PrescribedValue?,
        val stageReps: List<Int>,
        val isCurrent: Boolean,
        val isBeyondPlan: Boolean,
        /** True on the exercise's max rung, which the row labels "max". */
        val isMax: Boolean = false,
        /**
         * True when this row's prescription is reps rather than a load, so the
         * picker shows the reps instead of the variation name. A bodyweight
         * ladder's variation ("Bicycle Crunch" for "Crunches") is the
         * substitution, not the work — the owner asked to see reps there.
         */
        val repProgressed: Boolean = false,
        /**
         * How this exercise's repeated work is counted - "reps" or "steps" - so the
         * picker's word comes from the exercise rather than from every row assuming
         * reps. See [ProgressionEngine.countsInSteps].
         */
        val repUnitKey: String = "reps"
    )

    /**
     * The ladder for an exercise as the picker should show it: every level up
     * to the extension grid (see [progressionMaxLevel]) with its real —
     * explicit or extrapolated — prescription and the stage-I reps. This is
     * the data behind the level-details dialog: the user sees what weight and
     * reps each level carries before jumping there, and every row is
     * selectable because [setExerciseLevel] accepts the same range.
     */
    fun levelDetails(exerciseName: String): List<LevelDetailRow> {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return emptyList()
        if (!ProgressionEngine.isProgressionReady(entry)) return emptyList()
        val current = progressionDisplayLevel(exerciseName) ?: return emptyList()
        val shown = progressionMaxLevel(exerciseName)
        val max = ProgressionEngine.effectiveMaxLevel(entry)
        return (1..shown).map { level ->
            LevelDetailRow(
                level = level,
                prescribed = ProgressionEngine.prescribedValueAtLevel(entry, level, _weightUnit.value),
                // The RAW pattern, not [ProgressionEngine.stageRowReps]: this row also
                // tells the auto-advance graphic how many sets the level asks for
                // (`stageReps.size`), and a timed exercise's pattern is where that count
                // comes from. What the picker PRINTS is decided by the dialog, which
                // shows the level's seconds for a DURATION row.
                stageReps = ProgressionEngine.prescribedReps(entry, level, 1).orEmpty(),
                isCurrent = level == current,
                isBeyondPlan = false,
                isMax = level == max,
                repProgressed = ProgressionEngine.isRepProgressed(entry),
                repUnitKey = ProgressionEngine.repUnitKey(entry)
            )
        }
    }

    /**
     * Recomputes the CURRENT exercise's progression display fields in place.
     * Level adjustments land through the persisted prefs, but the reactive
     * loader only re-runs when its change key (exercise, level, stage)
     * changes — which a same-exercise re-set does not. Calling this after a
     * write keeps the card and the adjust dialogs in sync immediately.
     */
    private fun refreshProgressionDisplayNow(exerciseName: String) {
        val state = _workoutNowState.value
        if (currentExerciseName() != exerciseName) return
        val progState = currentProgressionState(exerciseName) ?: return
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return
        // Same padding rule as the reactive loader, so a same-exercise
        // re-set cannot shrink the banner back to the pattern's width.
        val targetSets = allRoutineExercises.value
            .firstOrNull { it.name == exerciseName && it.dayOfWeek == state.selectedDayOfWeek }
            ?.targetSets ?: 0
        val stageTargetReps = ProgressionEngine.stageTargetsForSets(
            entry, progState.level, progState.stage, targetSets
        )
        val prescribed = ProgressionEngine.prescribedValueAtLevel(entry, progState.level, _weightUnit.value)
        val setTarget = targetRepsForCurrentSet(exerciseName, state.currentSetNumber)
        // A TIMED EXERCISE'S TIMER FOLLOWS ITS LEVEL HERE TOO. The headline above the
        // button is `prescribed`, so changing the level changed the headline and left
        // the button starting the OLD level's timer - the card promised 90 s while the
        // button offered 30. Measured on the device, 2026-10-09: picking Level 5 for
        // Jumping Jacks moved the banner to "90 s" and left "START 30s TIMER & SAVE"
        // underneath it. The loader sets the seconds on a load, but a level write is
        // a refresh, and this is the refresh path - so the same prescription that
        // updates the headline has to update the seconds with it.
        val prescribedSeconds = prescribed
            ?.takeIf { it.kind == ProgressionEngine.ValueKind.DURATION }
            ?.value?.toInt()
            ?.takeIf { it in 15..3600 }
        _workoutNowState.update {
            it.copy(
                progressionLevel = progState.level,
                progressionStage = progState.stage,
                progressionStageTargetReps = stageTargetReps,
                progressionPrescribed = prescribed,
                currentSetTargetReps = setTarget,
                currentReps = setTarget ?: it.currentReps,
                // A rep exercise is untouched: its duration field is not its target.
                currentExerciseSeconds = prescribedSeconds ?: it.currentExerciseSeconds
            )
        }
    }

    /**
     * Recomputes the progression display fields for whichever exercise is
     * currently active. Used by the custom-mode switches, whose effect on the
     * display does not flow through the reactive loader's change key. When no
     * state resolves (custom mode, non-ready row) the fields are cleared so
     * the level chip and target pills disappear immediately.
     */
    private fun refreshCurrentExerciseDisplay() {
        val name = currentExerciseName() ?: return
        if (currentProgressionState(name) == null) {
            _workoutNowState.update {
                it.copy(
                    progressionLevel = null,
                    progressionStage = null,
                    progressionStageTargetReps = emptyList(),
                    progressionPrescribed = null,
                    currentSetTargetReps = null
                )
            }
            return
        }
        refreshProgressionDisplayNow(name)
    }

    /** The level-adjust visibility flag for the current UI context. */

    /**
     * Arms the background timer. Called when the session starts and every time the app
     * comes back to the foreground, so a process restarted mid-rest picks the timer up
     * again instead of leaving the countdown frozen where it was.
     */
    fun ensureRestTimerRunning() {
        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        if (endTime <= 0L) return
        if (endTime <= currentTimeMillis()) {
            // It ended while nothing was watching: drop it rather than announce a rest
            // that finished minutes (or hours) ago.
            stopTimer()
            return
        }
        timerManager.scheduleTimer(endTime, remainingRestSeconds(endTime))
    }

    fun updateWeightAndReps(weight: Double, reps: Int) {
        val currentState = _workoutNowState.value
        val newWeight = weight.coerceAtLeast(0.0)
        val newReps = reps.coerceIn(currentState.minReps, currentState.maxReps)
        val newState = currentState.copy(
            currentWeightKg = newWeight,
            currentReps = newReps
        )
        _workoutNowState.update { newState }
        saveWorkoutNowState(newState)

        // Remember what the user chose for this exercise so it becomes the suggestion next time.
        // Batched into ONE commit: this used to open a second prefs transaction right after
        // [saveWorkoutNowState]'s own, so a single stepper tap wrote the prefs file twice on the
        // frame the user tapped it.
        currentExerciseName()?.let { name ->
            prefs.edit {
                if (!WorkoutTemplates.isBodyweightExercise(name) || newWeight > 0.0) {
                    putFloat("cache_weight_$name", newWeight.toFloat())
                }
                putInt("cache_reps_$name", newReps)
            }
        }
    }

    fun updateExerciseSeconds(seconds: Int) {
        val currentState = _workoutNowState.value
        val newState = currentState.copy(
            currentExerciseSeconds = seconds.coerceIn(currentState.minExerciseSeconds, currentState.maxExerciseSeconds)
        )
        _workoutNowState.value = newState
        saveWorkoutNowState(newState)

        // Remember the duration the user chose for time-based exercises
        currentExerciseName()?.let { name ->
            prefs.putInt("cache_seconds_$name", newState.currentExerciseSeconds)
        }
    }

    /** Returns the weight the user last used for this exercise, or null when unknown. */
    private fun getCachedWeightForExercise(exerciseName: String): Double? {
        val exerciseWeight = prefs.getFloat("cache_weight_${exerciseName}", -1f)
        return if (exerciseWeight >= 0f) exerciseWeight.toDouble() else null
    }

    // ------------------------------------------------------------------
    // Progression state (v5 level-up system)
    //
    // One "level,stage" pair per exercise, stored under
    // "progression_state_<name>". Written ONLY after a completed workout set
    // that met the prescription; never rewritten by history edits, so logged
    // workouts keep their historical loads untouched.
    // ------------------------------------------------------------------

    private fun progressionKey(exerciseName: String) = "progression_state_$exerciseName"

    // ------------------------------------------------------------------
    // DONE versus SELECTED.
    //
    // Owner, 2026-10-17: "Remove the 'progression available' label if the user
    // only selected the level for progression and didnt do it yet". The label is
    // keyed off [ProgressionEngine.isProgressionPending], which reads the STORED
    // level/stage — and a stored state is written two different ways: by the
    // engine after a stage's every set met its target, and by the picker when a
    // user points at a rung. The second is a CHOICE; the offer is about work.
    //
    // So the exact state the engine advanced through is written down beside the
    // state itself, and every manual write clears it. "Progression available"
    // then means "you are standing on a rung you have actually finished", which
    // is the only reading where the caption cannot promise an offer the user has
    // not earned.
    //
    // HONEST CONSEQUENCE: a state stored by an OLDER build has no marker, so the
    // label is absent for it until the user completes a session at that rung.
    // Deliberate — the alternative keeps the defect for exactly the users who
    // were already bitten by it, and the marker is rebuilt by one logged set.
    // ------------------------------------------------------------------

    /** The exact (level, stage) the user has actually COMPLETED, or null. */
    private fun progressionEarnedKey(exerciseName: String) =
        "progression_earned_state_$exerciseName"

    private fun earnedProgressionState(exerciseName: String): ProgressionEngine.ProgressionState? =
        ProgressionEngine.deserialize(prefs.getString(progressionEarnedKey(exerciseName), null))

    /**
     * Test handle: the raw stored state, or null when nothing was ever written.
     * Internal rather than public because the distinction Cancel depends on —
     * "a state exists" versus "the state reads the same as an unset one" — is
     * invisible through [currentProgressionState], which always has an answer.
     */
    internal fun storedProgressionRawForTest(exerciseName: String): String? =
        prefs.getString(progressionKey(exerciseName), null)

    /** The engine completed [state]: this rung was done, not merely chosen. */
    internal fun markProgressionEarned(
        exerciseName: String,
        state: ProgressionEngine.ProgressionState
    ) {
        prefs.putString(progressionEarnedKey(exerciseName), ProgressionEngine.serialize(state))
    }

    /**
     * The user moved the level by hand, so nothing about it counts as done yet.
     * Every selector goes through here — see the note above.
     *
     * It also drops a HELD level-up: the engine was holding a move to a rung the
     * user has just walked away from by picking another one, and applying it
     * afterwards would undo the tap.
     */
    private fun clearProgressionEarned(exerciseName: String) {
        prefs.remove(progressionEarnedKey(exerciseName))
        prefs.remove(pendingLevelUpKey(exerciseName))
    }

    // ------------------------------------------------------------------
    // THE LEVEL-UP IS HELD UNTIL THE DAY ENDS.
    //
    // Owner, 2026-10-17: *"I want every exercise to only level up once the day
    // ends. Then check what exercises the user did that are marked done and
    // check if they are locked. If they are marked done and not locked then
    // level up that exercise (if of course system level is turned on)"* - and,
    // asked when the day ends: on FINISHING the workout, with a fallback.
    //
    // So a completed stage no longer writes the next rung. It writes it HERE,
    // beside the state, and the level the card shows does not move until the
    // session is finished - which is what makes "you are on level 5" mean
    // "you have finished level 5" rather than "you are half way through it".
    //
    // THE FALLBACK IS THE HALF THAT KEEPS THE FEATURE HONEST: a user who
    // walks away without ever pressing finish would otherwise hold a level-up
    // forever, so [applyPendingLevelUps] runs again when a day's session is
    // opened ([selectDayForWorkoutNow]). Nothing can be lost by forgetting to
    // finish; it can only be late.
    // ------------------------------------------------------------------

    private fun pendingLevelUpKey(exerciseName: String) = "pending_level_up_$exerciseName"

    /**
     * Test handle: hold a level-up exactly as [advanceProgressionIfNeeded] does
     * when a stage's every set met its target, so the DAY-END rule can be tested
     * without driving a whole workout session through the card.
     */
    internal fun holdLevelUpForTest(
        exerciseName: String,
        state: ProgressionEngine.ProgressionState
    ) {
        prefs.putString(pendingLevelUpKey(exerciseName), ProgressionEngine.serialize(state))
    }

    /**
     * Applies every held level-up, for exercises that are DONE and NOT LOCKED,
     * with the level system on - the owner's three conditions, in his order.
     *
     * "Done" is the pending marker itself: it only exists where a stage's every
     * set met its target. "Not locked" is the exercise's own pin
     * (`routine_exercises.locked`), which is also what [advanceProgressionIfNeeded]
     * refuses to move.
     */
    fun applyPendingLevelUps() {
        if (!_levelSystemEnabled.value) return
        val names = allRoutineExercises.value
            .filterNot { it.locked }
            .distinctBy { it.name }
        names.forEach { exercise ->
            val raw = prefs.getString(pendingLevelUpKey(exercise.name), null) ?: return@forEach
            val next = ProgressionEngine.deserialize(raw) ?: return@forEach
            prefs.putString(
                progressionKey(exercise.name),
                ProgressionEngine.serialize(next)
            )
            prefs.remove(pendingLevelUpKey(exercise.name))
            refreshProgressionDisplayNow(exercise.name)
        }
        refreshOverallLevel()
    }

    // ------------------------------------------------------------------
    // WHAT AN EDIT PASS STARTED FROM, so Cancel can put it back.
    //
    // Owner, 2026-10-17: "Cancel button in the level selection doesnt actually
    // cancel." It did not, because the editor's level rows WRITE on the tap
    // (the owner's own 2026-10-08 ruling — "when he selects it directly apply
    // it"), so by the time Cancel was pressed the level, the stage and the
    // unlock had long been persisted and Cancel only closed the dialog.
    //
    // The dialog stays immediate — one tap still moves the card behind it —
    // and Cancel means "undo what this pass changed": the state an edit pass
    // first touched is snapshotted once per exercise, restored on Cancel and
    // dropped on Save. Keyed by name, because the ADD form's name changes as
    // the user picks, and each name is its own history.
    // ------------------------------------------------------------------

    /** name -> the (level, stage, locked) the edit pass opened on. */
    private val editPassOrigin =
        mutableMapOf<String, ProgressionOrigin?>()

    /** The three facts an edit pass may change. */
    data class ProgressionOrigin(
        val state: ProgressionEngine.ProgressionState?,
        val locked: Boolean,
        val hadStoredState: Boolean
    )

    /** Snapshot BEFORE the first editor write of this pass; later taps keep the original. */
    private fun rememberEditPassOrigin(exerciseName: String) {
        if (editPassOrigin.containsKey(exerciseName)) return
        val storedRaw = prefs.getString(progressionKey(exerciseName), null)
        editPassOrigin[exerciseName] = ProgressionOrigin(
            state = ProgressionEngine.deserialize(storedRaw),
            locked = isExerciseLockedNow(exerciseName),
            hadStoredState = storedRaw != null
        )
    }

    /**
     * Save: the pass's writes stand, so there is nothing left for Cancel to undo.
     */
    fun commitExerciseEditPass(exerciseName: String) {
        editPassOrigin.remove(exerciseName)
    }

    /**
     * Cancel: put the level, the stage, the earned marker and the lock back to
     * what they were when this edit pass first wrote. Nothing to restore (the
     * user opened the editor and changed nothing) means nothing happens.
     */
    fun cancelExerciseEditPass(exerciseName: String) {
        val origin = editPassOrigin.remove(exerciseName) ?: return
        val key = progressionKey(exerciseName)
        if (origin.hadStoredState && origin.state != null) {
            prefs.putString(key, ProgressionEngine.serialize(origin.state))
        } else {
            // The exercise had never been set: the tap CREATED a stored state,
            // and cancelling has to take it away again rather than leave a
            // level the user never chose behind the card.
            prefs.remove(key)
            prefs.remove(progressionEarnedKey(exerciseName))
        }
        setExerciseLocked(exerciseName, origin.locked)
        refreshProgressionDisplayNow(exerciseName)
        refreshOverallLevel()
    }

    /** Prefs key for the extension grid an exercise's level list has grown to. */
    private fun extensionKey(exerciseName: String) = "level_extension_$exerciseName"

    /**
     * The extension grid (50/100/150/… up to [ProgressionEngine.PICKER_LEVEL_LIMIT])
     * unlocked for [exerciseName]; 0 means the list still shows the default.
     * Persisted per exercise, so a grown ladder stays grown next session.
     */
    private fun extensionLevelFor(exerciseName: String): Int {
        val stored = prefs.getInt(extensionKey(exerciseName), 0)
        return stored.coerceIn(0, ProgressionEngine.PICKER_LEVEL_LIMIT)
    }

    /**
     * Grows the level list for [exerciseName] by one chunk of
     * [ProgressionEngine.EXTENSION_CHUNK] levels, which is how the rungs PAST the
     * exercise's max are reached (owner 2026-10-13: "add more levels after it").
     *
     * It was a NO-OP between 2026-10-04 and this ruling, when the humane ceiling
     * was the list's end. The grid it writes is persisted per exercise, so a grown
     * ladder stays grown next session, and it stops at
     * [ProgressionEngine.PICKER_LEVEL_LIMIT] - the bound `clampLevel` writes to, so
     * the list can never offer a rung the writer would refuse.
     */
    fun extendLevelList(exerciseName: String) {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return
        if (!ProgressionEngine.isProgressionReady(entry)) return
        val next = (progressionMaxLevel(exerciseName) + ProgressionEngine.EXTENSION_CHUNK)
            .coerceAtMost(ProgressionEngine.PICKER_LEVEL_LIMIT)
        prefs.putInt(extensionKey(exerciseName), next)
    }

    /**
     * True while there is another chunk of levels to unlock, i.e. while the list is
     * still short of [ProgressionEngine.PICKER_LEVEL_LIMIT].
     *
     * Always-false between 2026-10-04 and 2026-10-13, when the ladder ended at the
     * row's own humane ceiling and the button would only have offered rows the app
     * refuses to select.
     */
    fun canExtendLevelList(exerciseName: String): Boolean {
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return false
        if (!ProgressionEngine.isProgressionReady(entry)) return false
        return progressionMaxLevel(exerciseName) < ProgressionEngine.PICKER_LEVEL_LIMIT
    }

    /**
     * True when an exercise is prescribed by the level system AND the user has
     * not switched the system off in Settings. This is the one gate every
     * "is this exercise level-managed?" check in the UI reads.
     */
    fun isExerciseLevelManaged(exerciseName: String): Boolean {
        if (!_levelSystemEnabled.value) return false
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return false
        return ProgressionEngine.isProgressionReady(entry)
    }

    /** The stored level/stage for an exercise, or its starting prescription. */
    fun currentProgressionState(exerciseName: String): ProgressionEngine.ProgressionState? {
        if (!_levelSystemEnabled.value) return null
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return null
        if (!ProgressionEngine.isProgressionReady(entry)) return null
        val stored = ProgressionEngine.deserialize(prefs.getString(progressionKey(exerciseName), null))
        if (stored != null) {
            // A stored level can sit PAST its ladder's end. The ladder's length is
            // derived (the rep step and the ceiling-aware max), so a ruling that
            // changes either - the 2026-10-06 rep-step change SHORTENED every
            // rep ladder - leaves an older number behind on the device. Read
            // back unclamped it showed "Level 7" over a picker that offered four
            // rungs, so the read clamps to the same bound the writer uses.
            //
            // That bound is the LIST's, not the researching exercise's max: a rung
            // past the max is a level the user may legitimately be on now (owner
            // 2026-10-13), and clamping back to the max would silently walk their
            // stored level down every time the screen loaded.
            val max = progressionMaxLevel(exerciseName)
            return stored.copy(level = stored.level.coerceIn(1, max))
        }
        // First session: derive the state from the difficulty chosen in setup.
        val difficulty = when (com.grafizum.repio.data.WorkoutTemplates.classifyIntensity(
            prefs.getString("setup_intensity", "") ?: ""
        )) {
            "Easy" -> ProgressionEngine.Difficulty.EASY
            "Hard" -> ProgressionEngine.Difficulty.HARD
            else -> ProgressionEngine.Difficulty.MEDIUM
        }
        val prescription = ProgressionEngine.startingPrescription(exerciseName, difficulty)
            ?: return null
        return ProgressionEngine.ProgressionState(prescription.level, prescription.stage)
    }

    /**
     * The per-set target reps the progression system prescribes for the
     * CURRENT set of [exerciseName], or null when the exercise is outside the
     * level system (the caller keeps the routine's generic target).
     */
    private fun targetRepsForCurrentSet(exerciseName: String, setNumber: Int): Int? {
        val state = currentProgressionState(exerciseName) ?: return null
        val entry = ProgressionEngine.entryFor(exerciseName) ?: return null
        val stageReps = ProgressionEngine.prescribedReps(entry, state.level, state.stage) ?: return null
        // The stage's reps list is per set: set 1 takes reps[0], set 2 reps[1]…
        // beyond the pattern's set count the last set's target repeats.
        return if (stageReps.isEmpty()) null
        else stageReps[(setNumber - 1).coerceAtMost(stageReps.size - 1)]
    }

    /**
     * Tracks how many sets of the current stage were completed at their target
     * this session, keyed per exercise. Cleared when the exercise changes or a
     * set misses its target (the CSV's completion rule requires EVERY set).
     */
    private val stageSetProgress = mutableMapOf<String, Int>()

    /**
     * Advances the progression state after a logged set. Stage completion is
     * decided per the CSV's `meet_or_exceed_every_set` rule: the level/stage
     * moves only when every set of the stage met its target; a missed set
     * restarts the stage count (standard double progression).
     * Called from [logCurrentSetAndAdvance] only — never rewrites history.
     */
    private fun advanceProgressionIfNeeded(exercise: RoutineExerciseEntity, loggedReps: Int) {
        // The per-exercise lock (routine_exercises.locked) holds THIS exercise
        // exactly where it is — the stepper grays out and the engine skips it.
        if (exercise.locked) return
        val name = exercise.name
        val state = currentProgressionState(name) ?: return
        val entry = ProgressionEngine.entryFor(name) ?: return
        val stageReps = ProgressionEngine.prescribedReps(entry, state.level, state.stage) ?: return
        if (stageReps.isEmpty()) return

        val setNumber = _workoutNowState.value.currentSetNumber
        val requiredReps = stageReps[(setNumber - 1).coerceAtMost(stageReps.size - 1)]
        if (loggedReps < requiredReps) {
            // A missed set resets the stage's completion count.
            stageSetProgress[name] = 0
            return
        }

        val completed = (stageSetProgress[name] ?: 0) + 1
        stageSetProgress[name] = completed

        // The stage is complete once every set of its pattern row was met.
        if (completed >= stageReps.size) {
            stageSetProgress[name] = 0
            // THIS rung was genuinely done — the marker "progression available"
            // reads, and the one a manual selection clears. Written for the state
            // the user was ON, because that is what they just finished; a level-up
            // moves the state on top of it and the next rung is not earned yet.
            markProgressionEarned(name, state)
            val next = ProgressionEngine.levelUp(entry, state) ?: return
            // HELD, NOT WRITTEN — the level the card shows stays put until the day
            // ends. See [applyPendingLevelUps] for the ruling and the fallback.
            prefs.putString(pendingLevelUpKey(name), ProgressionEngine.serialize(next.newState))
        }
    }

    /** Returns the last rep count the user set for this exercise, or null when unknown. */
    private fun getCachedRepsForExercise(exerciseName: String): Int? {
        val reps = prefs.getInt("cache_reps_${exerciseName}", -1)
        return if (reps >= 0) reps else null
    }

    /** Returns the last duration (seconds) the user set for this exercise, or null when unknown. */
    private fun getCachedSecondsForExercise(exerciseName: String): Int? {
        val seconds = prefs.getInt("cache_seconds_${exerciseName}", -1)
        return if (seconds >= 0) seconds else null
    }

    /** The routine exercise currently active in the workout session, if any. */
    private fun currentExerciseName(): String? {
        val dayExercises = allRoutineExercises.value
            .filter { it.dayOfWeek == _workoutNowState.value.selectedDayOfWeek }
            .sortedBy { it.orderIndex }
        return dayExercises.getOrNull(_workoutNowState.value.currentExerciseIndex)?.name
    }

    /** Forgets the remembered per-exercise values (used when a brand-new build replaces the routine). */
    private fun clearExerciseMemories(exerciseNames: List<String>) {
        exerciseNames.forEach { name ->
            prefs.remove("cache_weight_$name")
            prefs.remove("cache_reps_$name")
            prefs.remove("cache_seconds_$name")
            // A replaced routine starts the level system over for its exercises.
            prefs.remove(progressionKey(name))
        }
        stageSetProgress.clear()
    }

    // ------------------------------------------------------------------
    // Mastery & Branch: the post-workout progression hub.
    //
    // When an exercise finishes its ladder (variation end) or its load lands
    // on the researched human ceiling, it stops leveling up and shows up as a
    // mastery card after the workout summary: pick a branch, or acknowledge
    // the ceiling. "Reviewed" cards rest for five workouts before re-showing
    // (per the spec's anti-spam rule), and a chosen branch re-levels the
    // exercise onto its new path immediately.
    // ------------------------------------------------------------------

    /** Prefs key holding the reviewed-at workout count for a mastered exercise. */
    private fun masteryReviewedKey(name: String) = "mastery_reviewed_$name"

    // ------------------------------------------------------------------
    // The day's recap.
    //
    // Built from the LOG, not accumulated in memory. The owner's ask (2026-10-17):
    // the completed workout must PERMANENTLY keep showing what was done and which
    // muscle groups were trained today — and an in-memory accumulator dies with the
    // process that filled it, so the card vanished on a restart while the completed
    // flag survived. The log table is the only place that outlives the process. The
    // old recap also counted PLANNED target sets per muscle (every exercise of the
    // day, done or not); the split now counts the sets actually logged, so the card
    // cannot claim volume the user never did.
    //
    // FILTERED BY CALENDAR DAY (same rule as loadDayProgress): yesterday's sets
    // cannot make today's card lie. shortcut: a recap across midnight refreshes on
    // the next logged set rather than by wall clock — <when to upgrade> if the
    // owner ever keeps the app open across a day flip and expects the card to clear.
    // ------------------------------------------------------------------

    /** Workouts a reviewed card stays quiet before re-offering itself. */
    private val MASTERY_REVIEW_COOLDOWN = 5

    /**
     * The mastery cards for [exercises]: every level-managed exercise that is
     * mastered (ladder end / ceiling) and not currently cooling down. Pure —
     * called when a workout completes.
     */
    private fun computeMasteredExercises(exercises: List<RoutineExerciseEntity>): List<MasteryCardData> {
        val workoutCount = prefs.getInt("completed_workout_count", 0)
        return exercises.distinctBy { it.name }.mapNotNull { ex ->
            val state = currentProgressionState(ex.name) ?: return@mapNotNull null
            val entry = ProgressionEngine.entryFor(ex.name) ?: return@mapNotNull null
            if (!ProgressionEngine.isMastered(entry, state, _weightUnit.value)) return@mapNotNull null
            // A DECLINED evolution is locked, not snoozed. Without this the
            // five-workout cooldown would quietly re-offer it forever, which
            // is the nagging the lock exists to stop. Only the editor
            // reopens it (reopenEvolution) - or, on a rep ladder, the next
            // checkpoint the level climbs to.
            if (!isProgressionAvailable(ex.name)) return@mapNotNull null
            // A reviewed-and-declined card rests before re-showing.
            val reviewedAt = prefs.getInt(masteryReviewedKey(ex.name), -1)
            if (reviewedAt >= 0 && workoutCount - reviewedAt < MASTERY_REVIEW_COOLDOWN) {
                return@mapNotNull null
            }
            val reason = ProgressionEngine.masteryReason(entry, state, _weightUnit.value) ?: return@mapNotNull null
            MasteryCardData(
                exerciseName = ex.name,
                muscleGroup = ex.muscleGroup,
                equipment = ExerciseCatalog.entryForId(entry.id)?.equipment ?: "",
                level = state.level,
                stageRoman = ProgressionEngine.stageName(state.stage),
                // A rep ladder carries no load at all: its value column is the ordinal of a
                // variation, and a number that means nothing must not travel any
                // further as if it were kilograms. The reps are the prescription.
                load = if (ProgressionEngine.isRepLadder(entry)) null
                else ProgressionEngine.valueAtLevelWithCeiling(entry, state.level),
                unit = entry.unit,
                reason = reason,
                ceiling = entry.ceiling,
                branches = ProgressionEngine.branchesFor(entry),
                suggestions = if (reason == ProgressionEngine.MasteryReason.CEILING_REACHED) {
                    ProgressionEngine.suggestedAlternatives(ex.name)
                } else emptyList()
            )
        }
    }

    /**
     * Accepts a branch: the source exercise is marked reviewed (its state stays
     * as a completed history), and the TARGET exercise's stored state is set to
     * the mapped starting level so the next session trains the new path. Both
     * live as ordinary exercises in the routine, so no schema change is needed.
     */
    fun acceptBranch(sourceName: String, targetName: String) {
        val sourceEntry = ProgressionEngine.entryFor(sourceName) ?: return
        val targetEntry = ProgressionEngine.entryFor(targetName) ?: return
        val sourceState = currentProgressionState(sourceName) ?: return
        if (!ProgressionEngine.isProgressionReady(targetEntry)) return
        val startLevel = ProgressionEngine.branchStartLevel(sourceEntry, sourceState, targetEntry)
        prefs.putString(progressionKey(targetName), ProgressionEngine.serialize(ProgressionEngine.ProgressionState(startLevel, 1)))
        prefs.remove(extensionKey(targetName))
        markMasteryReviewed(sourceName)
        refreshMasteredCards()
    }

    /** Keeps an exercise at its cap and silences its card for the cooldown. */
    fun keepExerciseAtCap(sourceName: String) {
        markMasteryReviewed(sourceName)
        refreshMasteredCards()
    }

    // ------------------------------------------------------------------
    // The evolution offer, and the lock a declined one puts on.
    //
    // Declining is a decision, not a snooze. The old behaviour only silenced
    // the card for five workouts, so the same offer came back on its own -
    // which is the nagging the owner wanted gone. Declining now LOCKS the
    // exercise at its cap until the user goes to that exercise in the editor
    // and asks for the offer again. Nothing re-opens it by itself.
    // ------------------------------------------------------------------

    private fun evolutionDeclinedKey(name: String) = "evolution_declined_$name"

    /**
     * The rep checkpoint a declined offer was made at, or null when nothing was
     * declined. Owner ruling 2026-10-03: "if the user chooses no, the exercise
     * could go on with the level higher... if he doesnt simply keep
     * incrementing to maybe 50 and then a show progression again".
     *
     * So a decline is a REP COUNT, not a boolean. The old boolean meant the
     * offer could never come back on its own, which was right for a LOAD row -
     * its ladder genuinely ends - and wrong for a rep ladder, where the user
     * has only said "not yet" at 12 reps and climbs on.
     */
    private fun declinedRepCheckpointKey(name: String) = "evolution_declined_reps_$name"

    /** Whether this exercise's offer has already been put in front of the user. */
    private fun evolutionOfferedKey(name: String) = "evolution_offered_$name"

    /**
     * Whether the progression offer for [name] has ALREADY been shown as a
     * popup at least once.
     *
     * The owner's rule: "for the progression label when a level is reached, i
     * want it to only show at the end of the workout first and not directly
     * when selecting the new exercise. So only show it once the progression
     * popup window came for the user."
     *
     * So this is deliberately NOT the same question as [isProgressionAvailable].
     * That one asks "is an offer pending", which becomes true the instant a
     * level reaches a checkpoint - which is precisely when the user would get
     * the label mid-session, before the workout that will offer it has even
     * finished. This asks the other half: has the offer actually been made
     * yet. Both have to hold for the label to appear.
     */
    fun hasSeenProgressionOffer(name: String): Boolean =
        prefs.getBoolean(evolutionOfferedKey(name), false)

    private fun declinedRepCheckpoint(name: String): Int? =
        prefs.getInt(declinedRepCheckpointKey(name), -1).takeIf { it >= 0 }

    /** True when [name]'s evolution offer has been declined and left locked. */
    fun isEvolutionDeclined(name: String): Boolean {
        if (prefs.getBoolean(evolutionDeclinedKey(name), false)) return true
        val declinedAt = declinedRepCheckpoint(name) ?: return false
        // A declined rep checkpoint re-arms at the next one. Only the last
        // checkpoint is final, because past it there is nowhere to climb to.
        return ProgressionEngine.nextRepCheckpoint(declinedAt) == null
    }

    /**
     * Whether [name] is currently OWED a progression offer - the state the
     * "progression available" label next to the level pill reports.
     *
     * Same question the post-workout card asks, so the two cannot disagree: a
     * label that says "available" while the workout ends without showing it
     * would be the quiet version of the bug this whole flow is about.
     */
    fun isProgressionAvailable(name: String): Boolean {
        val entry = ProgressionEngine.entryFor(name) ?: return false
        val state = currentProgressionState(name) ?: return false
        // THE LABEL IS ABOUT WORK. A level the user merely POINTED AT in the
        // picker satisfies the engine's mastery rule — that rule reads the stored
        // state — so "progression available" used to appear over a rung the user
        // had never trained. See [progressionEarnedKey] for the ruling; a rung
        // with no marker is one this build has not seen finished.
        if (earnedProgressionState(name) != state) return false
        val declinedAt = declinedRepCheckpoint(name)
        // A rep ladder's decline re-arms at the next checkpoint, so it is
        // remembered as a REP COUNT and handed to the engine, which owns the
        // rule. A loaded row keeps the old permanent boolean lock, and the
        // engine cannot know about that flag, so it is applied here.
        if (ProgressionEngine.isRepLadder(entry)) {
            return ProgressionEngine.isProgressionPending(entry, state, declinedAt)
        }
        return ProgressionEngine.isProgressionPending(entry, state, null) &&
            !isEvolutionDeclined(name)
    }

    /**
     * The exercise [name] would be offered next, or null when nothing is
     * pending. The label shows this so the user can see what the progression
     * IS before opening the popup.
     */
    fun pendingProgressionTarget(name: String): String? {
        if (!isProgressionAvailable(name)) return null
        val entry = ProgressionEngine.entryFor(name) ?: return null
        return ProgressionEngine.preferredBranch(entry)?.entry
    }

    /**
     * Declines the offer: the exercise stays at its cap and is locked there.
     * The lock is per-exercise and permanent until [reopenEvolution].
     *
     * A REP LADDER records the checkpoint it was declined at instead, so the
     * offer comes back at the next one once the level has climbed on.
     */
    fun declineEvolution(name: String) {
        val entry = ProgressionEngine.entryFor(name)
        val state = currentProgressionState(name)
        val checkpoint = entry?.let { e ->
            state?.let { ProgressionEngine.repCheckpointAtLevel(e, it.level) }
        }
        if (entry != null && ProgressionEngine.isRepLadder(entry) && checkpoint != null) {
            prefs.putInt(declinedRepCheckpointKey(name), checkpoint)
        } else {
            prefs.putBoolean(evolutionDeclinedKey(name), true)
        }
        markMasteryReviewed(name)
        refreshMasteredCards()
    }

    /**
     * The owner opened a locked exercise's offer from the editor, so drop the
     * lock and let the offer be made again.
     */
    fun reopenEvolution(name: String) {
        prefs.remove(evolutionDeclinedKey(name))
        prefs.remove(declinedRepCheckpointKey(name))
    }

    /** Accepts the offer and clears any lock in one step. */
    fun acceptEvolution(sourceName: String, targetName: String) {
        reopenEvolution(sourceName)
        acceptBranch(sourceName, targetName)
        // ACCEPTING CHANGES THE EXERCISE, NOT ONLY ITS LEVEL.
        //
        // Owner, 2026-10-17: "(if progressing it will automatically change that
        // current exercise with the progressed one as well as all the other same
        // exercises on the week (if there are other days showing that same
        // exercise))". [acceptBranch] only wrote the TARGET's progression state,
        // so the card kept naming the exercise the user had just outgrown and the
        // offer came back next session against a row that no longer matched.
        replaceExerciseEverywhere(sourceName, targetName)
    }

    /**
     * Rewrites every routine row named [sourceName] to [targetName] — all days,
     * not just the one on screen.
     *
     * A day that already carries the target is left alone rather than given a
     * second copy of it: the two rows would fight over one progression state and
     * one set of cached values, which is a worse outcome than that day keeping
     * the movement it already had.
     */
    private fun replaceExerciseEverywhere(sourceName: String, targetName: String) {
        val rows = allRoutineExercises.value.filter { it.name == sourceName }
        if (rows.isEmpty()) return
        val muscle = ExerciseCatalog.entryFor(targetName)?.muscle ?: rows.first().muscleGroup
        viewModelScope.launch {
            rows.groupBy { it.dayOfWeek }.forEach { (_, dayRows) ->
                if (dayRows.any { it.name == targetName }) return@forEach
                dayRows.forEach { row ->
                    repository.saveExercise(row.copy(name = targetName, muscleGroup = muscle))
                }
            }
        }
    }

    /**
     * Locked evolutions among [exercises]: mastered exercises whose offer the
     * user declined. The editor shows an "Upgrade" affordance on exactly
     * these, which is the only way a declined offer comes back.
     */
    fun evolutionLockedExercises(exercises: List<RoutineExerciseEntity>): List<String> =
        exercises.distinctBy { it.name }
            .filter { isEvolutionDeclined(it.name) }
            .map { it.name }

    /**
     * The before/after for [sourceName] -> [targetName], or null when either
     * side is missing. The popup opens on the branch the engine prefers and
     * the section's cards can ask for a specific one.
     */
    fun evolutionOfferFor(sourceName: String, targetName: String): ProgressionEngine.EvolutionOffer? {
        val state = currentProgressionState(sourceName) ?: return null
        return ProgressionEngine.evolutionOffer(sourceName, targetName, state)
    }

    /** The branch the popup opens on for a mastered exercise, if any. */
    fun preferredEvolutionOffer(sourceName: String): ProgressionEngine.EvolutionOffer? {
        val entry = ProgressionEngine.entryFor(sourceName) ?: return null
        val branch = ProgressionEngine.preferredBranch(entry) ?: return null
        return evolutionOfferFor(sourceName, branch.entry)
    }

    /**
     * Every branch [card] offers, as resolved offers, in the card's own order.
     *
     * A branch whose target cannot be rendered (`evolutionOffer` returns null -
     * the target has no level ladder, or the state is missing) is dropped rather
     * than drawn as an option that does nothing when tapped. When that leaves
     * nothing at all, the card has no evolution to show and the caller skips it.
     */
    fun evolutionOffersFor(card: MasteryCardData): List<ProgressionEngine.EvolutionOffer> =
        card.branches.mapNotNull { branch ->
            evolutionOfferFor(card.exerciseName, branch.entry)
        }

    /**
     * The post-workout mastery card for [exerciseName], or null when there is
     * none - the exercise is not mastered, is cooling down, or is locked.
     *
     * Used by the editor's "Upgrade" badge, which has to rebuild the tree from
     * the current list rather than be handed one. Recomputing from the routine
     * (rather than searching the already-built [workoutNowState] list) is what
     * makes the badge work days later: the post-workout list is rebuilt on
     * every completion, so a card from a previous session is not something to
     * go looking for.
     */
    fun masteryCardFor(exerciseName: String): MasteryCardData? =
        computeMasteredExercises(allRoutineExercises.value)
            .firstOrNull { it.exerciseName == exerciseName }

    private fun markMasteryReviewed(name: String) {
        prefs.putInt(masteryReviewedKey(name), prefs.getInt("completed_workout_count", 0))
    }

    private fun refreshMasteredCards() {
        val exercises = allRoutineExercises.value
        if (exercises.isEmpty()) return
        applyMasteryCards(computeMasteredExercises(exercises))
    }

    /**
     * Publishes [cards] as the post-workout mastery state: the cards themselves,
     * the one-per-exercise preferred offers, the whole-fork trees the popup
     * draws, and a bumped token so the screen seeds its queue once per change.
     *
     * The token is bumped even when the offers are unchanged, so the screen can
     * tell "same list, recomposed" from "new decision to make".
     *
     * WHY THIS IS ITS OWN FUNCTION: the workout-completion path used to write
     * only `masteredExercises` into the state, which left `evolutionTrees` at
     * whatever it was (empty on a first completion). The screen seeds its popup
     * queue from `evolutionTrees`, so an exercise that reached its ladder end on
     * the very set that completed the workout got a card and NO POPUP — which
     * is exactly the report "the progression popup is not shown for exercises
     * that reached max level". Every path that produces mastery cards now goes
     * through here, so the card and the popup it offers cannot disagree.
     */
    private fun applyMasteryCards(cards: List<MasteryCardData>) {
        // Every card here is about to be put in front of the user as a popup,
        // so record that the offer was actually MADE. The "progression
        // available" label on the exercise card asks this before it says
        // anything - see hasSeenProgressionOffer.
        cards.forEach { card -> prefs.putBoolean(evolutionOfferedKey(card.exerciseName), true) }
        _workoutNowState.update {
            it.copy(
                masteredExercises = cards,
                evolutionOffers = cards.mapNotNull { card ->
                    preferredEvolutionOffer(card.exerciseName)
                },
                evolutionTrees = cards.mapNotNull { card ->
                    val offers = evolutionOffersFor(card)
                    if (offers.isEmpty()) null else EvolutionTreeItem(card, offers)
                },
                evolutionToken = it.evolutionToken + 1,
            )
        }
    }

    /** Re-offers the level-guide onboarding when a new program replaces the old one. */
    fun rearmLevelGuide() {
        prefs.putBoolean("has_seen_level_guide_v6", false)
        _shouldShowLevelGuide.value = true
    }

    /** Resets an in-progress session so a new program starts at exercise 1 of today. */
    private fun resetSessionProgress() {
        stopTimer()
        listOf(
            "saved_workout_day", "saved_exercise_index", "saved_set_number",
            "saved_weight_kg", "saved_reps", "saved_exercise_seconds",
            "saved_is_completed", "saved_total_sets_logged", "saved_session_date"
        ).forEach { prefs.remove(it) }
        com.grafizum.repio.data.DayOfWeekEnum.entries.forEach { prefs.remove("day_progress_${it.name}") }
        prefs.remove("timer_day")
        _workoutNowState.value = WorkoutNowState(selectedDayOfWeek = getTodayDayOfWeek())
    }

    fun incrementWeight() {
        val currentState = _workoutNowState.value
        val currentWeight = currentState.currentWeightKg
        val nextWeight = (floor(currentWeight * 2.0 + 0.001) + 1.0) / 2.0
        updateWeightAndReps(nextWeight, currentState.currentReps)
    }

    fun decrementWeight() {
        val currentState = _workoutNowState.value
        val currentWeight = currentState.currentWeightKg
        val prevWeight = (ceil(currentWeight * 2.0 - 0.001) - 1.0) / 2.0
        updateWeightAndReps(prevWeight, currentState.currentReps)
    }

    fun startRestTimer(seconds: Int, isExerciseTimer: Boolean = false, silent: Boolean = false, day: String? = null) {
        if (seconds <= 0) return
        stopTimer()
        val timerDay = day ?: _workoutNowState.value.selectedDayOfWeek
        val endTime = currentTimeMillis() + (seconds * 1000L)
        prefs.putLong("timer_end_time_millis", endTime)
        prefs.putBoolean("is_exercise_timer", isExerciseTimer)
        prefs.putBoolean("is_silent_timer", silent)
        prefs.putString("timer_day", timerDay)
        timerManager.scheduleTimer(endTime, seconds)
        _workoutNowState.update { it.copy(restTimerSeconds = seconds, isTimerActive = true, isExerciseTimerActive = isExerciseTimer, timerDayOfWeek = timerDay) }
    }

    fun logCurrentSetAndAdvance(exercises: List<RoutineExerciseEntity>) {
        if (exercises.isEmpty()) return
        stopTimer()
        viewModelScope.launch {
            val currentState = _workoutNowState.value
            val exerciseIndex = currentState.currentExerciseIndex.coerceIn(0, exercises.size - 1)
            val currentExercise = exercises[exerciseIndex]
            
            // Save what the user performed to the per-exercise memory for next time
            if (!WorkoutTemplates.isBodyweightExercise(currentExercise.name) || currentState.currentWeightKg > 0.0) {
                prefs.putFloat("cache_weight_${currentExercise.name}", currentState.currentWeightKg.toFloat())
            }
            // Both branches below ask the SAME question the card asks - "is this
            // exercise measured in seconds?" - and they used to answer it by
            // sniffing the routine's rep string, which mislabelled every timed row
            // built before the level system (Jumping Jacks' "8–12"). See
            // [WorkoutTemplates.isExerciseTimeBased] for why the exercise answers it.
            val isTimeBased = WorkoutTemplates.isExerciseTimeBased(currentExercise.name)
            if (!isTimeBased) {
                prefs.putInt("cache_reps_${currentExercise.name}", currentState.currentReps)
            } else {
                prefs.putInt("cache_seconds_${currentExercise.name}", currentState.currentExerciseSeconds)
            }

            // A TIMED EXERCISE'S SET IS LOGGED IN SECONDS, NOT IN REPS.
            //
            // The two numbers share one column - the history reads a timed row back as "15 s" - and
            // the engine's own prescription for these exercises is a DURATION, so handing it the rep
            // value would both store the wrong number and compare the wrong one against the stage's
            // target. Before the clock was separated from the log button this could not come up:
            // the timed path logged its own `seconds` at the moment the timer started. Now that the
            // log button is the one that records the set (see [startExerciseClock]), it is this
            // function that has to know which unit it is recording.
            val loggedValue = if (isTimeBased) currentState.currentExerciseSeconds else currentState.currentReps

            repository.logSet(currentState.selectedDayOfWeek, currentExercise.name, currentExercise.muscleGroup, currentState.currentSetNumber, currentState.currentWeightKg, loggedValue)

            // v5 progression: advance the level/stage state when the workout's
            // sets met the prescription. Never touches the logged row itself.
            advanceProgressionIfNeeded(currentExercise, loggedValue)

            // A level-up changes an input of the overall level without changing the
            // logged history, and it lands in preferences after the set was
            // inserted — so the history-driven recompute cannot be relied on to see
            // it. Refresh explicitly rather than depending on that ordering.
            recomputeOverallLevel()
            
            // Save current parameters back to template for next week (keep a full rep range, not a single value)
            repository.saveExercise(currentExercise.copy(
                targetWeightKg = currentState.currentWeightKg,
                targetReps = if (isTimeBased) currentExercise.targetReps
                             else WorkoutTemplates.repRangeAround(currentState.currentReps)
            ))

            val nextSetNumber = currentState.currentSetNumber + 1


            var isTransition = false
            // Set when the last set of the last exercise is logged, so the day's
            // level-ups can be applied AFTER the state has moved rather than from
            // inside the update lambda - see [applyPendingLevelUps].
            var workoutFinished = false
            _workoutNowState.update { state ->
                if (nextSetNumber <= currentExercise.targetSets) {
                    state.copy(currentSetNumber = nextSetNumber, totalSetsLoggedToday = state.totalSetsLoggedToday + 1)
                } else {
                    val nextExerciseIndex = exerciseIndex + 1
                    if (nextExerciseIndex < exercises.size) {
                        isTransition = true
                        state.copy(currentExerciseIndex = nextExerciseIndex, currentSetNumber = 1, totalSetsLoggedToday = state.totalSetsLoggedToday + 1)
                    } else {
                        // Workout complete: count it (streaks, mastery cooldowns).
                        // The mastery cards are computed AFTER the held level-ups are
                        // applied, below - they read the stored levels, so computing
                        // them here would describe the pre-level-up exercise and offer
                        // a branch it has just climbed off.
                        prefs.putInt("completed_workout_count", prefs.getInt("completed_workout_count", 0) + 1)
                        workoutFinished = true
                        state.copy(
                            isWorkoutCompleted = true,
                            totalSetsLoggedToday = state.totalSetsLoggedToday + 1,
                            restTimerSeconds = 0,
                            isTimerActive = false,
                            masteredExercises = emptyList()
                        )
                    }
                }
            }
            // THE DAY ENDS HERE (owner, 2026-10-17): every held level-up is applied for
            // the exercises that are done and not locked, and only THEN are the mastery
            // cards computed - so the offer the summary makes is the offer the NEW level
            // makes. The popup queue is its own state field, so publishing the cards
            // through [applyMasteryCards] is what keeps a first completion from leaving
            // it stale and the offer unopened.
            if (workoutFinished) {
                applyPendingLevelUps()
                val cards = computeMasteredExercises(exercises)
                applyMasteryCards(cards)
                _workoutNowState.update { it.copy(masteredExercises = cards) }
            }
            val updatedState = _workoutNowState.value
            if (!updatedState.isWorkoutCompleted) {
                if (isTransition) {
                    startRestTimer(currentExercise.transitionRestSeconds)
                } else {
                    startRestTimer(currentExercise.restSeconds)
                }
            }
            saveWorkoutNowState(_workoutNowState.value)
        }
    }

    fun resetWorkoutSession() {
        stopTimer()
        viewModelScope.launch {
            repository.deleteTodayLogs()
        }
        _workoutNowState.update { it.copy(currentExerciseIndex = 0, currentSetNumber = 1, isWorkoutCompleted = false, totalSetsLoggedToday = 0, isTimerActive = false, restTimerSeconds = 0) }
        saveDayProgress(_workoutNowState.value.selectedDayOfWeek, _workoutNowState.value)
        saveWorkoutNowState(_workoutNowState.value)
    }

    /**
     * Follows the running timer into the session state.
     *
     * here we only read the same stored end time, so the screen always shows the
     * identical remaining time.
     */
    fun tickTimer() {
        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        if (endTime > currentTimeMillis()) {
            _workoutNowState.update {
                it.copy(restTimerSeconds = remainingRestSeconds(endTime), isTimerActive = true)
            }
        } else if (endTime > 0L && endTime <= currentTimeMillis()) {
            // Expired but the alarm has not reported yet. Inexact alarms
            // (Doze / battery optimization) can land seconds late, and the
            // old behavior left the countdown parked at "1" the whole time —
            // with the vibration and sound waiting on that late report.
            // The tick is now the authority for an expired rest: it raises the
            // same end-of-rest alert the receiver would, clears the stored end
            // time (so the alert cannot re-fire on later ticks — there is no
            // longer an end time to expire), and hands over to the report path
            // for the advance. When the late alarm finally lands, the stored
            // end time no longer matches and it reports nothing — the user
            // never gets a second alert.
            val silent = prefs.getBoolean("is_silent_timer", false)
            // Read BEFORE clearing: this is what tells the alert apart from a
            // rest, so a timed exercise no longer announces that rest time is
            // over. See NotificationHelper.showRestTimerEndedNotification.
            val wasExerciseTimer = prefs.getBoolean("is_exercise_timer", false)
            prefs.putLong("timer_end_time_millis", 0L)
            prefs.putBoolean("is_exercise_timer", false)
            prefs.putBoolean("is_silent_timer", false)
            try {
                timerManager.cancelTimer()
            } catch (_: Exception) {
                // Already gone.
            }
            val context = com.grafizum.repio.util.getApplicationContext()
            if (context != null) {
                NotificationHelper.showRestTimerEndedNotification(
                    context,
                    alert = !silent,
                    wasExerciseTimer = wasExerciseTimer
                )
            }
            onRestTimerFinished()
        } else if (endTime <= 0L && _workoutNowState.value.isTimerActive) {
            // Cleared without a report of ours: the rest ended while the app was away, so the
            // session is only left with a stale countdown. Drop that, but leave the
            // timed-exercise flags alone — the report may still be on its way, and it is what
            // decides whether the session moves on.
            _workoutNowState.update { it.copy(restTimerSeconds = 0, isTimerActive = false) }
        }
    }

    /**
     * Winds up a finished rest.
     *
     * The report can arrive twice, once from the service and once from the alarm backing
     * it up, so a timer that has meanwhile been replaced by the next rest must not be
     * cancelled by the late one — hence the look at what is stored now.
     */
    private fun onRestTimerFinished() {
        if (prefs.getLong("timer_end_time_millis", 0L) > currentTimeMillis()) return

        // A FINISHED CLOCK MOVES NOTHING ON BY ITSELF - neither a rest nor the timed exercise's own.
        //
        // The timed clock used to end the set for the user: the set was logged when the timer
        // STARTED, and its expiry advanced to the next one. Owner, 2026-10-16: *"small
        // start/stop/resume/restart by the clock, log button grey to themed"* - i.e. the clock is
        // the WORK interval and the log button is what records the set, so a clock that ran out on
        // its own must not consume a set. All this does is retire the countdown, which is exactly
        // what re-enables (and re-themes) the log button underneath it.
        if (_workoutNowState.value.isTimerActive) stopTimer()
    }

    /**
     * Called when the app leaves the foreground. The timer lives on in the background
     * service, so all this has to do is make sure that service is armed before the UI goes.
     */
    fun checkTimerOnAppClose() {
        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        if (endTime > currentTimeMillis()) {
            timerManager.scheduleTimer(endTime, remainingRestSeconds(endTime))
        }
    }

    fun stopTimer() {
        prefs.putLong("timer_end_time_millis", 0L)
        prefs.putBoolean("is_exercise_timer", false)
        prefs.putBoolean("is_silent_timer", false)
        prefs.putBoolean("exercise_clock_paused", false)
        prefs.remove("timer_day")
        prefs.remove("exercise_clock_remaining")
        timerManager.cancelTimer()
        _workoutNowState.update {
            it.copy(
                restTimerSeconds = 0,
                isTimerActive = false,
                isExerciseTimerActive = false,
                isExerciseClockPaused = false,
                timerDayOfWeek = null,
            )
        }
    }

    /**
     * START THE CLOCK ON A TIMED EXERCISE'S WORK INTERVAL, WITHOUT LOGGING A SET.
     *
     * Owner, 2026-10-16: *"small start/stop/resume/restart by the clock, log button grey to
     * themed"*. The clock and the log button were ONE control: the primary button read "START 45s
     * TIMER & SAVE", logged the set the instant it was tapped and then counted the work interval,
     * and while it ran the same button turned into a counter/skip. So the set was already recorded
     * before the work was done, and the only way to "log" it was to have pressed the timer first.
     *
     * They are two things now - this starts the clock, the log button records the set - and the
     * order the owner asked for is the order the screen shows: start, work, and the button goes
     * from grey (running) to themed (the clock is idle or held) as the clock stops.
     *
     * The countdown is the app's own rest/deadline engine, deliberately: one stored end time, one
     * alarm, one background service, one notification channel - so a work interval that outlives
     * the screen behaves the same way a rest does, instead of being a second timer to keep honest.
     * [isExerciseTimerActive] records that this deadline is the exercise's, [isExerciseClockPaused]
     * that it is held.
     */
    fun startExerciseClock(seconds: Int) {
        if (seconds <= 0) return
        prefs.putBoolean("exercise_clock_paused", false)
        prefs.remove("exercise_clock_remaining")
        startRestTimer(seconds, isExerciseTimer = true, silent = false)
        _workoutNowState.update { it.copy(isExerciseClockPaused = false) }
        saveWorkoutNowState(_workoutNowState.value)
    }

    /**
     * HOLD the clock where it is.
     *
     * The remaining seconds are stored, the deadline is cleared - so the alarm cannot fire and the
     * service has nothing to report - and the state keeps the frozen countdown on screen. The log
     * button comes back to life, which is the point: "pause, then log" is how a set that is already
     * done gets recorded early.
     */
    fun pauseExerciseClock() {
        val state = _workoutNowState.value
        if (!state.isExerciseTimerActive || !state.isTimerActive) return
        prefs.putInt("exercise_clock_remaining", state.restTimerSeconds)
        prefs.putBoolean("exercise_clock_paused", true)
        prefs.putLong("timer_end_time_millis", 0L)
        prefs.putBoolean("is_exercise_timer", false)
        timerManager.cancelTimer()
        _workoutNowState.update { it.copy(isTimerActive = false, isExerciseClockPaused = true) }
        saveWorkoutNowState(_workoutNowState.value)
    }

    /** Continue a held clock from the seconds it was held at. */
    fun resumeExerciseClock() {
        val remaining = prefs.getInt("exercise_clock_remaining", 0)
        if (remaining <= 0) return
        prefs.putBoolean("exercise_clock_paused", false)
        startRestTimer(remaining, isExerciseTimer = true, silent = false)
        _workoutNowState.update { it.copy(isExerciseClockPaused = false) }
        saveWorkoutNowState(_workoutNowState.value)
    }

    /** Back to the full interval, running - the same clock, from the top. */
    fun restartExerciseClock(seconds: Int) = startExerciseClock(seconds)

    fun deleteLoggedSet(id: Long) { viewModelScope.launch { repository.deleteLoggedSet(id) } }
    fun saveExercise(exercise: RoutineExerciseEntity) { viewModelScope.launch { repository.saveExercise(exercise) } }
    fun reorderExercises(exercises: List<RoutineExerciseEntity>) { viewModelScope.launch { repository.reorderExercises(exercises) } }

    /** Live multi-replace: swaps [original] for the currently toggled [replacements]. */
    fun replaceExerciseWith(
        original: RoutineExerciseEntity,
        previousNames: List<String>,
        replacements: List<RoutineExerciseEntity>
    ) {
        viewModelScope.launch {
            // THE LEVEL FOLLOWS THE WORK, NOT THE NAME.
            //
            // Owner, 2026-10-17: "When replacing an exercise i feel like the new
            // exercise doesnt already start from the level that was once selected
            // to do that exercise." The progression state is keyed by exercise
            // NAME, so swapping a row for another movement handed the user a
            // blank ladder for the new name — their level was still there, just
            // no longer on the exercise in front of them.
            //
            // Only when the target has NO state of its own: a movement the user
            // has already trained keeps the level they earned on it, because
            // overwriting that would delete data the app did not create here.
            val carried = currentProgressionState(original.name)
            repository.replaceExercise(original, previousNames, replacements)

            // A SWAPPED-OUT EXERCISE STARTS OVER.
            //
            // Owner, 2026-11-03: "When swapping exercises position last second
            // while still being on that exercise, then reset the exercise no
            // matter how many sets were made (since the new exercise is a
            // different one when swapping)." The sets were logged against the
            // OLD name, so a swap looked reset on screen but the work was still
            // in the log - swap back and the exercise came back finished. Now the
            // movement that left has its sets for TODAY removed, which is the
            // reset he asked for, and the incoming one starts clean.
            //
            // An EMPTY replacement list is the dialog's "restore the original",
            // not a swap, so nothing is cleared: the user is undoing, not leaving.
            if (replacements.isNotEmpty()) {
                (listOf(original.name) + previousNames)
                    .distinct()
                    .filter { name -> replacements.none { it.name == name } }
                    .forEach { departed -> repository.clearTodaysLoggedSets(departed) }
            }
            if (carried != null) {
                replacements
                    .filter { it.name != original.name }
                    .filter { prefs.getString(progressionKey(it.name), null) == null }
                    .forEach { replacement ->
                        prefs.putString(
                            progressionKey(replacement.name),
                            ProgressionEngine.serialize(carried)
                        )
                        // The level was SELECTED here rather than trained on the
                        // new movement, so it is not "done" — see
                        // [progressionEarnedKey].
                        clearProgressionEarned(replacement.name)
                        refreshProgressionDisplayNow(replacement.name)
                    }
            }
        }
    }

    /**
     * A replacement row re-parameterized from the user's saved settings — goal
     * (scheme: reps and rest) and difficulty label. Set count is the universal
     * Guided-mode constant (3) and is not parameterized anymore.
     */
    fun parameterizedReplacement(
        original: RoutineExerciseEntity,
        newName: String,
        newMuscleGroup: String,
        videoUrl: String = ""
    ): RoutineExerciseEntity = WorkoutTemplates.parameterizeReplacement(
        original = original,
        newName = newName,
        newMuscleGroup = newMuscleGroup,
        workoutGoal = _workoutGoal.value,
        intensity = (prefs.getString("setup_intensity", "") ?: "").ifBlank { "Medium" },
        videoUrl = videoUrl
    )

    fun deleteExercise(id: Long) {
        viewModelScope.launch {
            repository.deleteExercise(id)
        }
    }

    fun markDayAsWorkoutDay(dayOfWeek: String) {
        viewModelScope.launch {
            repository.markDayAsWorkoutDay(dayOfWeek)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun resetToDefaultProgram() {
        viewModelScope.launch {
            // The default program assumes a full gym; the user's saved kit
            // (empty = unrestricted) gates its barbell rows down the same
            // substitution chains every other path uses.
            repository.resetRoutineToDefault(loadSetupEquipment())
        }
    }

    private fun parseRepsRange(targetReps: String): Pair<Int, Int> {
        val regex = Regex("(\\d+)")
        val matches = regex.findAll(targetReps).toList()
        return if (matches.size >= 2) {
            val v1 = matches[0].value.toInt()
            val v2 = matches.last().value.toInt()
            min(v1, v2) to max(v1, v2)
        } else if (matches.size == 1) {
            val v = matches[0].value.toInt()
            v to v
        } else {
            10 to 10
        }
    }

    fun refreshTodaySelection() {
        val today = getTodayDayOfWeek()
        selectDayForWorkoutNow(today)
    }
}
