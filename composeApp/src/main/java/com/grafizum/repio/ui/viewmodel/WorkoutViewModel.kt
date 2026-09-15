package com.grafizum.repio.ui.viewmodel

import com.grafizum.repio.util.getPlatformSettings
import com.grafizum.repio.util.AppEventBus
import com.grafizum.repio.util.getTimerManager
import com.grafizum.repio.util.getSupportManager
import com.grafizum.repio.util.applyAppIconChange
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.remainingRestSeconds
import com.grafizum.repio.util.startOfTodayMillis
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.RoutineDayEntity
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutRepository
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.screens.remapTemplateToDays
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
    val isWorkoutCompleted: Boolean = false,
    val restTimerSeconds: Int = 0,
    val isTimerActive: Boolean = false,
    val isExerciseTimerActive: Boolean = false,
    val timerDayOfWeek: String? = null,
    val currentExerciseSeconds: Int = 30,
    val minExerciseSeconds: Int = 10,
    val maxExerciseSeconds: Int = 600,
    val totalSetsLoggedToday: Int = 0
)

data class SupportUiState(
    val isSubmitting: Boolean = false,
    val cooldownMessage: String = "",
    val isCooldownActive: Boolean = false,
    val error: String? = null,
    val errorDetail: String? = null,
    val success: Boolean = false
)

data class WorkoutSubTabRequest(
    val tab: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val allRoutineDays: StateFlow<List<RoutineDayEntity>>
    val allRoutineExercises: StateFlow<List<RoutineExerciseEntity>>
    val allLoggedSets: StateFlow<List<LoggedSetEntity>>

    private val prefs = getPlatformSettings("app_settings")
    private val timerManager = getTimerManager()
    private val supportManager = getSupportManager()

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
    val themeOption: StateFlow<String> = _themeOption.asStateFlow()

    private val _minSets = MutableStateFlow(prefs.getInt("setup_min_sets", 0))
    val minSets: StateFlow<Int> = _minSets.asStateFlow()

    private val _maxSets = MutableStateFlow(prefs.getInt("setup_max_sets", 0))
    val maxSets: StateFlow<Int> = _maxSets.asStateFlow()

    private val _workoutGoal = MutableStateFlow(prefs.getString("workout_goal", "goal_bodybuilding") ?: "goal_bodybuilding")
    val workoutGoal: StateFlow<String> = _workoutGoal.asStateFlow()

    private val _developerThemeEnabled = MutableStateFlow(prefs.getBoolean("developer_theme_enabled", false))
    val developerThemeEnabled: StateFlow<Boolean> = _developerThemeEnabled.asStateFlow()

    private val _isFirstTimeUser = MutableStateFlow(prefs.getBoolean("is_first_time", true))
    val isFirstTimeUser: StateFlow<Boolean> = _isFirstTimeUser.asStateFlow()

    private val _isManualReentry = MutableStateFlow(false)
    val isManualReentry: StateFlow<Boolean> = _isManualReentry.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "system") ?: "system")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _workoutNowState = MutableStateFlow(WorkoutNowState())
    val workoutNowState: StateFlow<WorkoutNowState> = _workoutNowState.asStateFlow()

    private val _requestedWorkoutSubTab = MutableStateFlow(WorkoutSubTabRequest(0))
    val requestedWorkoutSubTab: StateFlow<WorkoutSubTabRequest> = _requestedWorkoutSubTab.asStateFlow()

    private val _allAvailableExercises = MutableStateFlow<List<String>>(emptyList())
    val allAvailableExercises: StateFlow<List<String>> = _allAvailableExercises.asStateFlow()

    private val _showRestartDialog = MutableStateFlow(false)
    val showRestartDialog: StateFlow<Boolean> = _showRestartDialog.asStateFlow()

    private val _supportUiState = MutableStateFlow(SupportUiState())
    val supportUiState: StateFlow<SupportUiState> = _supportUiState.asStateFlow()

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 0)
    val refreshTrigger = _refreshTrigger.asSharedFlow()

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
            AppEventBus.restTimerEnded.collect { onRestTimerFinished() }
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

        viewModelScope.launch {
            repository.ensureDataSeeded()
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
                currentDayExs.getOrNull(state.currentExerciseIndex)
            }.distinctUntilChanged()
                .collect { exercise ->
                    exercise?.let {
                        val cachedWeight = getCachedWeightForExercise(it.name)
                        val cachedReps = getCachedRepsForExercise(it.name)
                        val cachedSeconds = getCachedSecondsForExercise(it.name)
                        val isTimeBased = it.targetReps.contains("sec", ignoreCase = true)
                        val isBodyweight = WorkoutTemplates.isBodyweightExercise(it.name)
                        val (minVal, maxVal) = parseRepsRange(it.targetReps)

                        _workoutNowState.update { state ->
                            // Suggested weight, in priority order: the weight the plan set explicitly
                            // (Edit screen), then what the user last used for this exercise, then an
                            // estimate adapted to the chosen difficulty + training style. That estimate
                            // is a fallback only — a remembered weight is never overwritten by it.
                            val templateWeight = it.targetWeightKg
                            val setupIntensity = prefs.getString("setup_intensity", "") ?: ""
                            val suggestedWeight = when {
                                templateWeight > 0.0 -> templateWeight
                                cachedWeight != null -> cachedWeight
                                isBodyweight -> 0.0
                                else -> WorkoutTemplates
                                    .getStartingWeightKg(it.name, setupIntensity)
                                    .takeIf { kg -> kg > 0.0 } ?: 20.0
                            }
                            val showBodyweight = isBodyweight && suggestedWeight == 0.0

                            // Auto-select reps/seconds: remember what the user last chose for this exercise
                            // (clamped to the plan's range); otherwise the middle of the target rep range,
                            // or the upper bound of the time range for time-based exercises.
                            val nextReps = if (!isTimeBased) {
                                cachedReps?.takeIf { it in minVal..maxVal } ?: WorkoutTemplates.getMiddleRep(it.targetReps)
                            } else {
                                state.currentReps
                            }
                            val nextSeconds = if (isTimeBased) {
                                cachedSeconds?.takeIf { it in 15..3600 } ?: maxVal
                            } else {
                                state.currentExerciseSeconds
                            }

                            state.copy(
                                currentWeightKg = if (showBodyweight) 0.0 else suggestedWeight,
                                recommendedWeightKg = if (showBodyweight) 0.0 else suggestedWeight,
                                currentReps = nextReps,
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
            }
        }
    }

    private fun saveWorkoutNowState(state: WorkoutNowState) {
        prefs.putString("saved_workout_day", state.selectedDayOfWeek)
        prefs.putInt("saved_exercise_index", state.currentExerciseIndex)
        prefs.putInt("saved_set_number", state.currentSetNumber)
        prefs.putFloat("saved_weight_kg", state.currentWeightKg.toFloat())
        prefs.putInt("saved_reps", state.currentReps)
        prefs.putInt("saved_exercise_seconds", state.currentExerciseSeconds)
        prefs.putBoolean("saved_is_completed", state.isWorkoutCompleted)
        prefs.putInt("saved_total_sets_logged", state.totalSetsLoggedToday)
        // The session is only valid for the calendar date it was saved on.
        prefs.putLong("saved_session_date", startOfTodayMillis())
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

        return WorkoutNowState(
            selectedDayOfWeek = savedDay,
            currentExerciseIndex = savedExerciseIndex,
            currentSetNumber = savedSetNumber,
            currentWeightKg = savedWeightKg,
            currentReps = savedReps,
            currentExerciseSeconds = savedExSeconds,
            isWorkoutCompleted = savedIsCompleted,
            restTimerSeconds = remainingSecs,
            isTimerActive = remainingSecs > 0,
            isExerciseTimerActive = if (remainingSecs > 0) isExTimer else false,
            timerDayOfWeek = if (remainingSecs > 0) timerDay else null,
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

    fun setThemeOption(theme: String) {
        val oldTheme = _themeOption.value
        _themeOption.value = theme
        prefs.putString("theme_option", theme)
        if (oldTheme != theme) {
            _showRestartDialog.value = true
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

    fun dismissRestartDialog() {
        _showRestartDialog.value = false
    }

    fun applyThemeRestart() {
        applyAppIconChange(_themeOption.value)
        _showRestartDialog.value = false
    }

    fun setAppLanguage(languageCode: String) {
        _appLanguage.value = languageCode
        prefs.putString("app_language", languageCode)
    }

    fun setWorkoutGoal(goal: String) {
        _workoutGoal.value = goal
        prefs.putString("workout_goal", goal)
    }

    fun setMinSets(value: Int) {
        _minSets.value = value
        prefs.putInt("setup_min_sets", value)
    }

    fun setMaxSets(value: Int) {
        _maxSets.value = value
        prefs.putInt("setup_max_sets", value)
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
        minSets: Int = 0,
        maxSets: Int = 0,
        equipment: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            // Completing a brand-new build from the Welcome screen replaces the routine,
            // so its memory (last-used weights/reps/seconds and the in-progress session) resets.
            val previousExerciseNames = repository.allRoutineExercises.first().map { it.name }
            // Premade builds can restrict which equipment the template may use.
            val effectiveExercises = if (setupType == "PREMADE" && equipment.isNotEmpty()) {
                mapToEquipmentFiltered(exercises, equipment, _workoutGoal.value)
            } else {
                exercises
            }
            repository.applyProgram(days, effectiveExercises)
            clearExerciseMemories((previousExerciseNames + effectiveExercises.map { it.name }).distinct())
            resetSessionProgress()
            val wasManual = _isManualReentry.value
            _isFirstTimeUser.value = false
            _isManualReentry.value = false
            prefs.putBoolean("is_first_time", false)
            
            // Persist setup parameters for later goal changes
            prefs.putString("setup_type", setupType)
            prefs.putString("setup_body_parts", bodyParts.joinToString(","))
            prefs.putString("setup_days", selectedDays.joinToString(","))
            prefs.putString("setup_intensity", intensity)
            prefs.putString("setup_template_id", templateId)
            prefs.putString("setup_equipment", equipment.joinToString(","))
            prefs.putInt("setup_min_sets", minSets)
            prefs.putInt("setup_max_sets", maxSets)
            
            setRequestedWorkoutSubTab(if (wasManual) 1 else 0)
        }
    }

    private fun mapToEquipmentFiltered(
        exercises: List<RoutineExerciseEntity>,
        equipment: List<String>,
        goal: String
    ): List<RoutineExerciseEntity> {
        val selected = equipment.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val base = if (selected.isEmpty()) exercises
                   else exercises.filter { WorkoutTemplates.getEquipmentForExercise(it.name) in selected }
        return base.map {
            it.copy(
                targetReps = WorkoutTemplates.getGoalReps(it.name, goal),
                restSeconds = WorkoutTemplates.getGoalRestSeconds(goal, it.name),
                tempo = ""
            )
        }
    }

    /** The equipment restriction saved with the last build (empty = no restriction). */
    private fun loadSetupEquipment(): List<String> {
        val raw = prefs.getString("setup_equipment", null) ?: return emptyList()
        if (raw.isBlank() || raw == WorkoutTemplates.EQUIPMENT_ALL) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun regenerateProgramWithNewGoal(newGoal: String) {
        viewModelScope.launch {
            setWorkoutGoal(newGoal)
            
            val setupType = prefs.getString("setup_type", "CUSTOM") ?: "CUSTOM"
            val bodyParts = prefs.getString("setup_body_parts", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val selectedDays = prefs.getString("setup_days", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val intensity = prefs.getString("setup_intensity", "") ?: ""
            val templateId = prefs.getString("setup_template_id", "") ?: ""
            val equipment = loadSetupEquipment()
            
            when (setupType) {
                "ASSISTED" -> {
                    val program = WorkoutTemplates.generateAssistedProgram(
                        bodyParts = bodyParts,
                        selectedDays = selectedDays,
                        intensity = intensity,
                        workoutGoal = newGoal,
                        minSets = prefs.getInt("setup_min_sets", 0),
                        maxSets = prefs.getInt("setup_max_sets", 0),
                        equipment = equipment
                    )
                    repository.applyProgram(program.days, program.exercises)
                }
                "PREMADE" -> {
                    val template = WorkoutTemplates.allTemplates.find { it.id == templateId }
                    if (template != null) {
                        // For premade, we need to know which days were selected. 
                        // setup_days should contain the selected week days.
                        val mapped = remapTemplateToDays(template, selectedDays)
                        val filteredExercises = mapToEquipmentFiltered(mapped.second, equipment, newGoal)
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
            repository.applyProgram(template.dayTemplates.map {
                RoutineDayEntity(it.dayOfWeek, it.dayOfWeek, "", it.isRestDay)
            }, template.exercises)
            val wasManual = _isManualReentry.value
            _isFirstTimeUser.value = false
            _isManualReentry.value = false
            prefs.putBoolean("is_first_time", false)
            setRequestedWorkoutSubTab(if (wasManual) 1 else 0)
        }
    }

    fun resetFirstTimeSetup() {
        _isFirstTimeUser.value = true
        _isManualReentry.value = true
        prefs.putBoolean("is_first_time", true)
    }

    fun cancelFirstTimeSetup() {
        _isFirstTimeUser.value = false
        _isManualReentry.value = false
        prefs.putBoolean("is_first_time", false)
    }

    fun selectDayForWorkoutNow(dayOfWeek: String) {
        val currentState = _workoutNowState.value
        // If it's already the selected day, don't reset progress (preserves state on tab switch/app resume)
        if (currentState.selectedDayOfWeek == dayOfWeek) {
            viewModelScope.launch { _refreshTrigger.emit(Unit) }
            return
        }

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
        viewModelScope.launch { _refreshTrigger.emit(Unit) }
    }

    fun navigateToExercise(index: Int, totalExercises: Int) {
        if (index in 0 until totalExercises) {
            _workoutNowState.update { it.copy(currentExerciseIndex = index, currentSetNumber = 1) }
            saveWorkoutNowState(_workoutNowState.value)
        }
    }

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

        // Remember what the user chose for this exercise so it becomes the suggestion next time
        currentExerciseName()?.let { name ->
            if (!WorkoutTemplates.isBodyweightExercise(name) || newWeight > 0.0) {
                prefs.putFloat("cache_weight_$name", newWeight.toFloat())
            }
            prefs.putInt("cache_reps_$name", newReps)
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
        }
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
            if (!currentExercise.targetReps.contains("sec", ignoreCase = true)) {
                prefs.putInt("cache_reps_${currentExercise.name}", currentState.currentReps)
            }
            
            repository.logSet(currentState.selectedDayOfWeek, currentExercise.name, currentExercise.muscleGroup, currentState.currentSetNumber, currentState.currentWeightKg, currentState.currentReps)
            
            // Save current parameters back to template for next week (keep a full rep range, not a single value)
            repository.saveExercise(currentExercise.copy(
                targetWeightKg = currentState.currentWeightKg,
                targetReps = if (currentExercise.targetReps.contains("sec", ignoreCase = true)) currentExercise.targetReps
                             else WorkoutTemplates.repRangeAround(currentState.currentReps)
            ))

            val nextSetNumber = currentState.currentSetNumber + 1
            
            var isTransition = false
            _workoutNowState.update { state ->
                if (nextSetNumber <= currentExercise.targetSets) {
                    state.copy(currentSetNumber = nextSetNumber, totalSetsLoggedToday = state.totalSetsLoggedToday + 1)
                } else {
                    val nextExerciseIndex = exerciseIndex + 1
                    if (nextExerciseIndex < exercises.size) {
                        isTransition = true
                        state.copy(currentExerciseIndex = nextExerciseIndex, currentSetNumber = 1, totalSetsLoggedToday = state.totalSetsLoggedToday + 1)
                    } else {
                        state.copy(isWorkoutCompleted = true, totalSetsLoggedToday = state.totalSetsLoggedToday + 1, restTimerSeconds = 0, isTimerActive = false)
                    }
                }
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
        } else if (endTime <= 0L && _workoutNowState.value.isTimerActive) {
            // Cleared without a report of ours: the rest ended while the app was away, so the
            // session is only left with a stale countdown. Drop that, but leave the
            // timed-exercise flags alone — the report may still be on its way, and it is what
            // decides whether the session moves on.
            _workoutNowState.update { it.copy(restTimerSeconds = 0, isTimerActive = false) }
        }
        // An expired timer that is still stored is deliberately left alone: the service
        // finishes it on its next tick and reports back through AppEventBus.
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

        val state = _workoutNowState.value
        if (state.isExerciseTimerActive) {
            // A timed exercise: move the session on. advanceAfterTimer clears the finished
            // timer and starts the next rest itself.
            val timerDay = state.timerDayOfWeek ?: state.selectedDayOfWeek
            val dayExercises = allRoutineExercises.value
                .filter { it.dayOfWeek == timerDay }
                .sortedBy { it.orderIndex }
            if (dayExercises.isNotEmpty()) {
                advanceAfterTimer(dayExercises)
            } else {
                stopTimer()
            }
        } else if (state.isTimerActive) {
            stopTimer()
        }
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
        prefs.remove("timer_day")
        timerManager.cancelTimer()
        _workoutNowState.update { it.copy(restTimerSeconds = 0, isTimerActive = false, isExerciseTimerActive = false, timerDayOfWeek = null) }
    }

    fun startExerciseTimerWithLog(exercises: List<RoutineExerciseEntity>, seconds: Int) {
        if (exercises.isEmpty()) return
        val currentState = _workoutNowState.value
        val exerciseIndex = currentState.currentExerciseIndex.coerceIn(0, exercises.size - 1)
        val currentExercise = exercises[exerciseIndex]
        
        // Save what the user performed to the per-exercise memory for next time
        if (!WorkoutTemplates.isBodyweightExercise(currentExercise.name) || currentState.currentWeightKg > 0.0) {
            prefs.putFloat("cache_weight_${currentExercise.name}", currentState.currentWeightKg.toFloat())
        }
        prefs.putInt("cache_seconds_${currentExercise.name}", seconds)

        viewModelScope.launch {
            repository.logSet(currentState.selectedDayOfWeek, currentExercise.name, currentExercise.muscleGroup, currentState.currentSetNumber, currentState.currentWeightKg, seconds)
            
            // Save current parameters back to template for next week
            repository.saveExercise(currentExercise.copy(
                targetWeightKg = currentState.currentWeightKg,
                targetReps = "$seconds sec"
            ))

            _workoutNowState.update { it.copy(totalSetsLoggedToday = it.totalSetsLoggedToday + 1) }
            startRestTimer(seconds, isExerciseTimer = true, silent = false)
            saveWorkoutNowState(_workoutNowState.value)
        }
    }

    fun advanceAfterTimer(exercises: List<RoutineExerciseEntity>) {
        if (exercises.isEmpty()) return
        val timerDay = _workoutNowState.value.timerDayOfWeek ?: _workoutNowState.value.selectedDayOfWeek
        stopTimer()
        val viewedDay = _workoutNowState.value.selectedDayOfWeek
        
        // Advance the timer's own day progress (not the day the user happens to be viewing)
        val progress = loadDayProgress(timerDay) ?: DaySessionProgress()
        val exerciseIndex = progress.exerciseIndex.coerceIn(0, exercises.size - 1)
        val currentExercise = exercises[exerciseIndex]
        val nextSetNumber = progress.setNumber + 1
        
        var isTransition = false
        val updated = if (nextSetNumber <= currentExercise.targetSets) {
            progress.copy(setNumber = nextSetNumber)
        } else {
            val nextExerciseIndex = exerciseIndex + 1
            if (nextExerciseIndex < exercises.size) {
                isTransition = true
                progress.copy(exerciseIndex = nextExerciseIndex, setNumber = 1)
            } else {
                progress.copy(completed = true)
            }
        }
        prefs.putString(
            "day_progress_$timerDay",
            "${updated.exerciseIndex},${updated.setNumber},${updated.completed},${updated.totalSetsLogged},${startOfTodayMillis()}"
        )
        if (timerDay == viewedDay) {
            _workoutNowState.update {
                it.copy(
                    currentExerciseIndex = updated.exerciseIndex,
                    currentSetNumber = updated.setNumber,
                    isWorkoutCompleted = updated.completed
                )
            }
        }
        
        if (!updated.completed) {
            if (isTransition) {
                startRestTimer(currentExercise.transitionRestSeconds, day = timerDay)
            } else {
                startRestTimer(currentExercise.restSeconds, day = timerDay)
            }
        }
        saveWorkoutNowState(_workoutNowState.value)
    }

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
            repository.replaceExercise(original, previousNames, replacements)
        }
    }

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

    fun resetToDefaultProgram() { viewModelScope.launch { repository.resetRoutineToDefault() } }

    fun sendSupportTicket(subject: String, message: String) {
        if (subject.isBlank() || message.isBlank()) {
            _supportUiState.update { it.copy(error = "please_fill_fields") }
            return
        }

        val currentTime = currentTimeMillis()
        val lastSubmitTime = prefs.getLong("last_support_submit_time", 0L)
        val lastResetDay = prefs.getLong("last_support_reset_day", 0L)
        val dailyCount = prefs.getInt("daily_support_ticket_count", 0)

        val isNewDay = (currentTime / (24 * 60 * 60 * 1000L)) != (lastResetDay / (24 * 60 * 60 * 1000L))
        val effectiveDailyCount = if (isNewDay) 0 else dailyCount

        val maxDailyTickets = 5
        val shortCooldownMillis = 10 * 60 * 1000L

        if (effectiveDailyCount >= maxDailyTickets) {
            _supportUiState.update { it.copy(error = "daily_limit_reached", isCooldownActive = true) }
            return
        }

        if (effectiveDailyCount >= 2 && (currentTime - lastSubmitTime < shortCooldownMillis)) {
            val remaining = ((shortCooldownMillis - (currentTime - lastSubmitTime)) / 60000) + 1
            _supportUiState.update { it.copy(error = "please_wait_cooldown", cooldownMessage = remaining.toString(), isCooldownActive = true) }
            return
        }

        viewModelScope.launch {
            _supportUiState.update { it.copy(isSubmitting = true, error = null, success = false) }
            val result = supportManager.sendTicket(subject, message)
            if (result.isSuccess) {
                val now = currentTimeMillis()
                val newCount = if (isNewDay) 1 else effectiveDailyCount + 1
                prefs.putLong("last_support_submit_time", now)
                prefs.putInt("daily_support_ticket_count", newCount)
                prefs.putLong("last_support_reset_day", now)

                _supportUiState.update { it.copy(isSubmitting = false, success = true) }
            } else {
                _supportUiState.update { 
                    it.copy(
                        isSubmitting = false, 
                        error = "internet_unavailable",
                        errorDetail = result.exceptionOrNull()?.message
                    ) 
                }
            }
        }
    }

    fun clearSupportStatus() {
        _supportUiState.update { it.copy(error = null, errorDetail = null, success = false, isCooldownActive = false) }
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
