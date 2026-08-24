package com.grafizum.repio.ui.viewmodel

import com.grafizum.repio.util.getPlatformSettings
import com.grafizum.repio.util.getTimerManager
import com.grafizum.repio.util.getSupportManager
import com.grafizum.repio.util.applyAppIconChange
import com.grafizum.repio.util.getTodayDayOfWeek
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val currentExerciseSeconds: Int = 30,
    val minExerciseSeconds: Int = 10,
    val maxExerciseSeconds: Int = 600,
    val totalSetsLoggedToday: Int = 0,
    val currentExerciseId: Long = -1L,
    val currentExerciseName: String = ""
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

    private val _equipment = MutableStateFlow(prefs.getString("setup_equipment", "Gym")?.split(",")?.filter { it.isNotBlank() } ?: listOf("Gym"))
    val equipment: StateFlow<List<String>> = _equipment.asStateFlow()

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

        // Reactive parameter cache loader
        viewModelScope.launch {
            combine(_workoutNowState, allRoutineExercises) { state, exercises ->
                val currentDayExs = exercises.filter { it.dayOfWeek == state.selectedDayOfWeek }
                currentDayExs.getOrNull(state.currentExerciseIndex)
            }.distinctUntilChanged()
                .collect { exercise ->
                    exercise?.let {
                        val cachedWeight = getCachedWeightForExercise(it.name)
                        val isTimeBased = it.targetReps.contains("sec", ignoreCase = true)
                        val isBodyweight = WorkoutTemplates.isBodyweightExercise(it.name)
                        val (minVal, maxVal) = parseRepsRange(it.targetReps)

                        _workoutNowState.update { state ->
                            val isNewExercise = state.currentExerciseId != it.id || state.currentExerciseName != it.name
                            
                            // If it's the same exercise but the range changed, we keep current progress but respect new limits.
                            // If it's a different exercise, we reset to the new exercise's defaults.
                            val nextReps = if (isNewExercise) {
                                if (!isTimeBased) maxVal else state.currentReps
                            } else {
                                state.currentReps.coerceIn(minVal, maxVal)
                            }
                            
                            val nextSeconds = if (isNewExercise) {
                                if (isTimeBased) maxVal else state.currentExerciseSeconds
                            } else {
                                state.currentExerciseSeconds.coerceIn(minVal, maxVal)
                            }

                            val nextWeight = if (isNewExercise) {
                                if (isBodyweight) 0.0 else cachedWeight
                            } else {
                                state.currentWeightKg
                            }

                            state.copy(
                                currentWeightKg = nextWeight,
                                recommendedWeightKg = if (isBodyweight) 0.0 else cachedWeight,
                                currentReps = nextReps,
                                minReps = if (!isTimeBased) minVal else 1,
                                maxReps = if (!isTimeBased) maxVal else 100,
                                currentExerciseSeconds = nextSeconds,
                                minExerciseSeconds = if (isTimeBased) minVal else 10,
                                maxExerciseSeconds = if (isTimeBased) maxVal else 600,
                                currentExerciseId = it.id,
                                currentExerciseName = it.name
                            )
                        }
                    } ?: run {
                        _workoutNowState.update { it.copy(currentExerciseId = -1L, currentExerciseName = "") }
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
        prefs.putLong("saved_exercise_id", state.currentExerciseId)
        prefs.putString("saved_exercise_name", state.currentExerciseName)
    }

    private fun loadSavedWorkoutNowState(): WorkoutNowState {
        val savedDay = prefs.getString("saved_workout_day", null) ?: getTodayDayOfWeek()
        val savedExerciseIndex = prefs.getInt("saved_exercise_index", 0)
        val savedSetNumber = prefs.getInt("saved_set_number", 1)
        val savedWeightKg = prefs.getFloat("saved_weight_kg", 20.0f).toDouble()
        val savedReps = prefs.getInt("saved_reps", 10)
        val savedExSeconds = prefs.getInt("saved_exercise_seconds", 30)
        val savedIsCompleted = prefs.getBoolean("saved_is_completed", false)
        val savedTotalSetsLogged = prefs.getInt("saved_total_sets_logged", 0)
        val savedExId = prefs.getLong("saved_exercise_id", -1L)
        val savedExName = prefs.getString("saved_exercise_name", "") ?: ""

        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        val isExTimer = prefs.getBoolean("is_exercise_timer", false)
        val now = currentTimeMillis()
        val remainingSecs = if (endTime > now) ((endTime - now) / 1000L).toInt() + 1 else 0

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
            totalSetsLoggedToday = savedTotalSetsLogged,
            currentExerciseId = savedExId,
            currentExerciseName = savedExName
        )
    }

    private fun currentTimeMillis(): Long = com.grafizum.repio.util.currentTimeMillis()

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        prefs.putBoolean("keep_screen_on", enabled)
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

    fun updateEquipment(equipment: List<String>) {
        _equipment.value = equipment
        prefs.putString("setup_equipment", equipment.joinToString(","))
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
        equipment: List<String> = emptyList(),
        bodyParts: List<String> = emptyList(),
        selectedDays: List<String> = emptyList(),
        intensity: String = "",
        templateId: String = "",
        minSets: Int = 0,
        maxSets: Int = 0
    ) {
        viewModelScope.launch {
            repository.applyProgram(days, exercises)
            _isFirstTimeUser.value = false
            _isManualReentry.value = false
            prefs.putBoolean("is_first_time", false)
            
            // Persist setup parameters for later goal changes
            prefs.putString("setup_type", setupType)
            prefs.putString("setup_equipment", equipment.joinToString(","))
            _equipment.value = equipment
            prefs.putString("setup_body_parts", bodyParts.joinToString(","))
            prefs.putString("setup_days", selectedDays.joinToString(","))
            prefs.putString("setup_intensity", intensity)
            prefs.putString("setup_template_id", templateId)
            prefs.putInt("setup_min_sets", minSets)
            prefs.putInt("setup_max_sets", maxSets)
            
            setRequestedWorkoutSubTab(0)
        }
    }

    fun regenerateProgramWithNewGoal(newGoal: String) {
        viewModelScope.launch {
            setWorkoutGoal(newGoal)
            
            val setupType = prefs.getString("setup_type", "CUSTOM") ?: "CUSTOM"
            val equipment = prefs.getString("setup_equipment", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val bodyParts = prefs.getString("setup_body_parts", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val selectedDays = prefs.getString("setup_days", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val intensity = prefs.getString("setup_intensity", "") ?: ""
            val templateId = prefs.getString("setup_template_id", "") ?: ""

            when (setupType) {
                "ASSISTED" -> {
                    val program = WorkoutTemplates.generateAssistedProgram(
                        equipment = equipment,
                        bodyParts = bodyParts,
                        selectedDays = selectedDays,
                        intensity = intensity,
                        workoutGoal = newGoal,
                        minSets = prefs.getInt("setup_min_sets", 0),
                        maxSets = prefs.getInt("setup_max_sets", 0)
                    )
                    repository.applyProgram(program.days, program.exercises)
                }
                "PREMADE" -> {
                    val template = WorkoutTemplates.allTemplates.find { it.id == templateId }
                    if (template != null) {
                        // For premade, we need to know which days were selected. 
                        // setup_days should contain the selected week days.
                        val mapped = remapTemplateToDays(template, selectedDays)
                        val mappedExercises = mapped.second.map { 
                            it.copy(
                                targetReps = WorkoutTemplates.getGoalReps(it.name, newGoal),
                                restSeconds = WorkoutTemplates.getGoalRestSeconds(newGoal, it.name)
                            ) 
                        }
                        repository.applyProgram(mapped.first, mappedExercises)
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
            _isFirstTimeUser.value = false
            _isManualReentry.value = false
            prefs.putBoolean("is_first_time", false)
            setRequestedWorkoutSubTab(0)
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
        setRequestedWorkoutSubTab(0)
    }

    fun selectDayForWorkoutNow(dayOfWeek: String) {
        val currentState = _workoutNowState.value
        // If it's already the selected day, don't reset progress (preserves state on tab switch/app resume)
        if (currentState.selectedDayOfWeek == dayOfWeek) {
            viewModelScope.launch { _refreshTrigger.emit(Unit) }
            return
        }

        val newState = currentState.copy(
            selectedDayOfWeek = dayOfWeek,
            currentExerciseIndex = 0,
            currentSetNumber = 1,
            isWorkoutCompleted = false,
            totalSetsLoggedToday = 0 // Reset for the new day
        )
        _workoutNowState.value = newState
        saveWorkoutNowState(newState)
        viewModelScope.launch { _refreshTrigger.emit(Unit) }
    }

    fun updateWeightAndReps(weight: Double, reps: Int) {
        val currentState = _workoutNowState.value
        val newState = currentState.copy(
            currentWeightKg = weight.coerceAtLeast(0.0),
            currentReps = reps
        )
        _workoutNowState.update { newState }
        saveWorkoutNowState(newState)
    }

    fun updateExerciseSeconds(seconds: Int) {
        val currentState = _workoutNowState.value
        val newState = currentState.copy(
            currentExerciseSeconds = seconds.coerceIn(currentState.minExerciseSeconds, currentState.maxExerciseSeconds)
        )
        _workoutNowState.value = newState
        saveWorkoutNowState(newState)
    }

    private fun getCachedWeightForExercise(exerciseName: String): Double {
        val exerciseWeight = prefs.getFloat("cache_weight_${exerciseName}", -1f)
        if (exerciseWeight != -1f) return exerciseWeight.toDouble()
        return 20.0
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

    fun startRestTimer(seconds: Int, isExerciseTimer: Boolean = false, silent: Boolean = false) {
        if (seconds <= 0) return
        stopTimer()
        val endTime = currentTimeMillis() + (seconds * 1000L)
        prefs.putLong("timer_end_time_millis", endTime)
        prefs.putBoolean("is_exercise_timer", isExerciseTimer)
        prefs.putBoolean("is_silent_timer", silent)
        timerManager.scheduleTimer(endTime)
        _workoutNowState.update { it.copy(restTimerSeconds = seconds, isTimerActive = true, isExerciseTimerActive = isExerciseTimer) }
    }

    fun logCurrentSetAndAdvance(exercises: List<RoutineExerciseEntity>) {
        if (exercises.isEmpty()) return
        stopTimer()
        viewModelScope.launch {
            val currentState = _workoutNowState.value
            val exerciseIndex = currentState.currentExerciseIndex.coerceIn(0, exercises.size - 1)
            val currentExercise = exercises[exerciseIndex]
            
            // Save weight to cache for next time
            if (!WorkoutTemplates.isBodyweightExercise(currentExercise.name)) {
                prefs.putFloat("cache_weight_${currentExercise.name}", currentState.currentWeightKg.toFloat())
            }
            
            repository.logSet(currentState.selectedDayOfWeek, currentExercise.name, currentExercise.muscleGroup, currentState.currentSetNumber, currentState.currentWeightKg, currentState.currentReps)
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
        saveWorkoutNowState(_workoutNowState.value)
    }

    fun tickTimer() {
        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        if (endTime > 0L) {
            val now = currentTimeMillis()
            val remainingMillis = endTime - now
            if (remainingMillis > 0L) {
                val remainingSecs = (remainingMillis / 1000L).toInt() + 1
                _workoutNowState.update { it.copy(restTimerSeconds = remainingSecs, isTimerActive = true) }
            } else {
                val wasActive = _workoutNowState.value.isTimerActive
                val wasExerciseTimer = prefs.getBoolean("is_exercise_timer", false)
                val isSilent = prefs.getBoolean("is_silent_timer", false)
                
                if (wasActive && wasExerciseTimer) {
                    val dayExercises = allRoutineExercises.value
                        .filter { it.dayOfWeek == _workoutNowState.value.selectedDayOfWeek }
                        .sortedBy { it.orderIndex }
                    if (dayExercises.isNotEmpty()) {
                        advanceAfterTimer(dayExercises)
                    } else {
                        stopTimer()
                    }
                } else {
                    stopTimer()
                }
                
                if (wasActive && !isSilent) timerManager.showTimerNotification()
            }
        } else if (_workoutNowState.value.isTimerActive) {
            _workoutNowState.update { it.copy(restTimerSeconds = 0, isTimerActive = false) }
        }
    }

    fun stopTimer() {
        prefs.putLong("timer_end_time_millis", 0L)
        prefs.putBoolean("is_exercise_timer", false)
        prefs.putBoolean("is_silent_timer", false)
        timerManager.cancelTimer()
        _workoutNowState.update { it.copy(restTimerSeconds = 0, isTimerActive = false, isExerciseTimerActive = false) }
    }

    fun checkTimerOnAppClose() {
        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        if (endTime > 0L) {
            val now = currentTimeMillis()
            if (endTime <= now) {
                prefs.putLong("timer_end_time_millis", 0L)
                _workoutNowState.value = _workoutNowState.value.copy(
                    restTimerSeconds = 0,
                    isTimerActive = false
                )
                timerManager.showTimerNotification()
            } else {
                timerManager.scheduleTimer(endTime)
            }
        }
    }

    fun startExerciseTimerWithLog(exercises: List<RoutineExerciseEntity>, seconds: Int) {
        if (exercises.isEmpty()) return
        val currentState = _workoutNowState.value
        val exerciseIndex = currentState.currentExerciseIndex.coerceIn(0, exercises.size - 1)
        val currentExercise = exercises[exerciseIndex]
        
        // Save weight to cache for next time
        if (!WorkoutTemplates.isBodyweightExercise(currentExercise.name)) {
            prefs.putFloat("cache_weight_${currentExercise.name}", currentState.currentWeightKg.toFloat())
        }

        viewModelScope.launch {
            repository.logSet(currentState.selectedDayOfWeek, currentExercise.name, currentExercise.muscleGroup, currentState.currentSetNumber, currentState.currentWeightKg, seconds)
            _workoutNowState.update { it.copy(totalSetsLoggedToday = it.totalSetsLoggedToday + 1) }
            startRestTimer(seconds, isExerciseTimer = true, silent = false)
            saveWorkoutNowState(_workoutNowState.value)
        }
    }

    fun advanceAfterTimer(exercises: List<RoutineExerciseEntity>) {
        if (exercises.isEmpty()) return
        stopTimer()
        val currentState = _workoutNowState.value
        val exerciseIndex = currentState.currentExerciseIndex.coerceIn(0, exercises.size - 1)
        val currentExercise = exercises[exerciseIndex]
        val nextSetNumber = currentState.currentSetNumber + 1
        
        var isTransition = false
        _workoutNowState.update { state ->
            if (nextSetNumber <= currentExercise.targetSets) {
                state.copy(currentSetNumber = nextSetNumber)
            } else {
                val nextExerciseIndex = exerciseIndex + 1
                if (nextExerciseIndex < exercises.size) {
                    isTransition = true
                    state.copy(currentExerciseIndex = nextExerciseIndex, currentSetNumber = 1)
                } else {
                    state.copy(isWorkoutCompleted = true)
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

    fun deleteLoggedSet(id: Long) { viewModelScope.launch { repository.deleteLoggedSet(id) } }
    fun saveExercise(exercise: RoutineExerciseEntity) { viewModelScope.launch { repository.saveExercise(exercise) } }
    fun reorderExercises(exercises: List<RoutineExerciseEntity>) { viewModelScope.launch { repository.reorderExercises(exercises) } }
    fun deleteExercise(id: Long) { viewModelScope.launch { repository.deleteExercise(id) } }

    fun clearHistory() { viewModelScope.launch { repository.clearHistory() } }

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
