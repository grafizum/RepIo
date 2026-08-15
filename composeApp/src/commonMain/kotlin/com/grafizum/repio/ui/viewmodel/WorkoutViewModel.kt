package com.grafizum.repio.ui.viewmodel

import com.grafizum.repio.util.getPlatformSettings
import com.grafizum.repio.util.getTimerManager
import com.grafizum.repio.util.getSupportManager
import com.grafizum.repio.util.applyAppIconChange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.RoutineDayEntity
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

fun getTodayDayOfWeek(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "MONDAY"
        Calendar.TUESDAY -> "TUESDAY"
        Calendar.WEDNESDAY -> "WEDNESDAY"
        Calendar.THURSDAY -> "THURSDAY"
        Calendar.FRIDAY -> "FRIDAY"
        Calendar.SATURDAY -> "SATURDAY"
        Calendar.SUNDAY -> "SUNDAY"
        else -> "MONDAY"
    }
}

data class WorkoutNowState(
    val selectedDayOfWeek: String = getTodayDayOfWeek(),
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val currentWeightKg: Double = 20.0,
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
    val totalSetsLoggedToday: Int = 0
)

data class SupportUiState(
    val isSubmitting: Boolean = false,
    val cooldownMessage: String = "",
    val isCooldownActive: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
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

    private val _developerThemeEnabled = MutableStateFlow(prefs.getBoolean("developer_theme_enabled", false))
    val developerThemeEnabled: StateFlow<Boolean> = _developerThemeEnabled.asStateFlow()

    private val _customWorkoutName = MutableStateFlow(prefs.getString("custom_workout_name", "My Workout") ?: "My Workout")
    val customWorkoutName: StateFlow<String> = _customWorkoutName.asStateFlow()

    private val _isFirstTimeUser = MutableStateFlow(prefs.getBoolean("is_first_time", true))
    val isFirstTimeUser: StateFlow<Boolean> = _isFirstTimeUser.asStateFlow()

    private val _isManualReentry = MutableStateFlow(false)
    val isManualReentry: StateFlow<Boolean> = _isManualReentry.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "system") ?: "system")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _workoutNowState = MutableStateFlow(WorkoutNowState())
    val workoutNowState: StateFlow<WorkoutNowState> = _workoutNowState.asStateFlow()

    private val _requestedWorkoutSubTab = MutableStateFlow(0)
    val requestedWorkoutSubTab: StateFlow<Int> = _requestedWorkoutSubTab.asStateFlow()

    private val _showRestartDialog = MutableStateFlow(false)
    val showRestartDialog: StateFlow<Boolean> = _showRestartDialog.asStateFlow()

    private val _supportUiState = MutableStateFlow(SupportUiState())
    val supportUiState: StateFlow<SupportUiState> = _supportUiState.asStateFlow()

    init {
        _workoutNowState.value = loadSavedWorkoutNowState()
        refreshTodaySelection()

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
            }.distinctUntilChanged { old, new -> old?.id == new?.id }
                .collect { exercise ->
                    exercise?.let {
                        val cachedWeight = getCachedWeightForExercise(it.name)
                        val isTimeBased = it.targetReps.contains("sec", ignoreCase = true)
                        val (minVal, maxVal) = parseRepsRange(it.targetReps)
                        
                        _workoutNowState.update { state ->
                            state.copy(
                                currentWeightKg = cachedWeight,
                                currentReps = if (!isTimeBased) maxVal else state.currentReps,
                                minReps = if (!isTimeBased) minVal else 1,
                                maxReps = if (!isTimeBased) maxVal else 100,
                                currentExerciseSeconds = if (isTimeBased) maxVal else state.currentExerciseSeconds,
                                minExerciseSeconds = if (isTimeBased) minVal else 10,
                                maxExerciseSeconds = if (isTimeBased) maxVal else 600
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
            totalSetsLoggedToday = savedTotalSetsLogged
        )
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis() // expect/actual if needed

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        prefs.putBoolean("keep_screen_on", enabled)
    }

    fun setDefaultRestTimerSeconds(seconds: Int) {
        val validSecs = seconds.coerceAtLeast(10)
        _defaultRestTimerSeconds.value = validSecs
        prefs.putInt("default_rest_timer_seconds", validSecs)
    }

    fun setDefaultExerciseTimerSeconds(seconds: Int) {
        val validSecs = seconds.coerceAtLeast(10)
        _defaultExerciseTimerSeconds.value = validSecs
        prefs.putInt("default_exercise_timer_seconds", validSecs)
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

    fun setRequestedWorkoutSubTab(tab: Int) {
        _requestedWorkoutSubTab.value = tab
    }

    fun updateWorkoutName(newName: String) {
        val trimmed = newName.trim().take(20).ifEmpty { "My Workout" }
        _customWorkoutName.value = trimmed
        prefs.putString("custom_workout_name", trimmed)
    }

    fun completeFirstTimeSetup(
        customName: String,
        days: List<RoutineDayEntity>,
        exercises: List<RoutineExerciseEntity>
    ) {
        updateWorkoutName(customName)
        viewModelScope.launch {
            repository.applyProgram(days, exercises)
            _isFirstTimeUser.value = false
            _isManualReentry.value = false
            prefs.putBoolean("is_first_time", false)
            setRequestedWorkoutSubTab(0)
        }
    }

    fun applyWorkoutTemplate(template: com.grafizum.repio.data.WorkoutTemplate, customName: String? = null) {
        if (!customName.isNullOrBlank()) {
            updateWorkoutName(customName)
        }
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
        val newState = _workoutNowState.value.copy(
            selectedDayOfWeek = dayOfWeek,
            currentExerciseIndex = 0,
            currentSetNumber = 1,
            isWorkoutCompleted = false
        )
        _workoutNowState.value = newState
        saveWorkoutNowState(newState)
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
        val nextWeight = (Math.floor(currentWeight * 2.0 + 0.001) + 1.0) / 2.0
        updateWeightAndReps(nextWeight, currentState.currentReps)
    }

    fun decrementWeight() {
        val currentState = _workoutNowState.value
        val currentWeight = currentState.currentWeightKg
        val prevWeight = (Math.ceil(currentWeight * 2.0 - 0.001) - 1.0) / 2.0
        updateWeightAndReps(prevWeight, currentState.currentReps)
    }

    fun startRestTimer(seconds: Int, isExerciseTimer: Boolean = false, silent: Boolean = false) {
        if (seconds <= 0) return
        stopTimer()
        val endTime = System.currentTimeMillis() + (seconds * 1000L)
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
            repository.logSet(currentState.selectedDayOfWeek, currentExercise.name, currentExercise.muscleGroup, currentState.currentSetNumber, currentState.currentWeightKg, currentState.currentReps)
            val nextSetNumber = currentState.currentSetNumber + 1
            _workoutNowState.update { state ->
                if (nextSetNumber <= currentExercise.targetSets) {
                    state.copy(currentSetNumber = nextSetNumber, totalSetsLoggedToday = state.totalSetsLoggedToday + 1)
                } else {
                    val nextExerciseIndex = exerciseIndex + 1
                    if (nextExerciseIndex < exercises.size) {
                        state.copy(currentExerciseIndex = nextExerciseIndex, currentSetNumber = 1, totalSetsLoggedToday = state.totalSetsLoggedToday + 1)
                    } else {
                        state.copy(isWorkoutCompleted = true, totalSetsLoggedToday = state.totalSetsLoggedToday + 1, restTimerSeconds = 0, isTimerActive = false)
                    }
                }
            }
            val updatedState = _workoutNowState.value
            if (!updatedState.isWorkoutCompleted) {
                val duration = if (updatedState.currentSetNumber == 1) 90 else 60
                startRestTimer(duration)
            }
            saveWorkoutNowState(_workoutNowState.value)
        }
    }

    fun resetWorkoutSession() {
        stopTimer()
        _workoutNowState.update { it.copy(currentExerciseIndex = 0, currentSetNumber = 1, isWorkoutCompleted = false, totalSetsLoggedToday = 0, isTimerActive = false, restTimerSeconds = 0) }
        saveWorkoutNowState(_workoutNowState.value)
    }

    fun tickTimer() {
        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        if (endTime > 0L) {
            val now = System.currentTimeMillis()
            val remainingMillis = endTime - now
            if (remainingMillis > 0) {
                val remainingSecs = (remainingMillis / 1000L).toInt() + 1
                _workoutNowState.update { it.copy(restTimerSeconds = remainingSecs, isTimerActive = true) }
            } else {
                val wasActive = _workoutNowState.value.isTimerActive
                val isSilent = prefs.getBoolean("is_silent_timer", false)
                stopTimer()
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
        viewModelScope.launch {
            repository.logSet(currentState.selectedDayOfWeek, currentExercise.name, currentExercise.muscleGroup, currentState.currentSetNumber, currentState.currentWeightKg, currentState.currentReps)
            _workoutNowState.update { it.copy(totalSetsLoggedToday = it.totalSetsLoggedToday + 1) }
            startRestTimer(seconds, isExerciseTimer = true, silent = true)
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
        _workoutNowState.update { state ->
            if (nextSetNumber <= currentExercise.targetSets) state.copy(currentSetNumber = nextSetNumber)
            else {
                val nextExerciseIndex = exerciseIndex + 1
                if (nextExerciseIndex < exercises.size) state.copy(currentExerciseIndex = nextExerciseIndex, currentSetNumber = 1)
                else state.copy(isWorkoutCompleted = true)
            }
        }
        saveWorkoutNowState(_workoutNowState.value)
    }

    fun deleteLoggedSet(id: Long) { viewModelScope.launch { repository.deleteLoggedSet(id) } }
    fun saveExercise(exercise: RoutineExerciseEntity) { viewModelScope.launch { repository.saveExercise(exercise) } }
    fun deleteExercise(id: Long) { viewModelScope.launch { repository.deleteExercise(id) } }

    fun clearHistory() { viewModelScope.launch { repository.clearHistory() } }

    fun resetToDefaultProgram() { viewModelScope.launch { repository.resetRoutineToDefault() } }

    fun sendSupportTicket(subject: String, message: String) {
        if (subject.isBlank() || message.isBlank()) {
            _supportUiState.update { it.copy(error = "please_fill_fields") }
            return
        }

        val currentTime = System.currentTimeMillis()
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
                val now = System.currentTimeMillis()
                val newCount = if (isNewDay) 1 else effectiveDailyCount + 1
                prefs.putLong("last_support_submit_time", now)
                prefs.putInt("daily_support_ticket_count", newCount)
                prefs.putLong("last_support_reset_day", now)
                
                _supportUiState.update { it.copy(isSubmitting = false, success = true) }
            } else {
                _supportUiState.update { it.copy(isSubmitting = false, error = "internet_unavailable") }
            }
        }
    }

    fun clearSupportStatus() {
        _supportUiState.update { it.copy(error = null, success = false, isCooldownActive = false) }
    }

    private fun parseRepsRange(targetReps: String): Pair<Int, Int> {
        val regex = Regex("(\\d+)")
        val matches = regex.findAll(targetReps).toList()
        return if (matches.size >= 2) {
            val v1 = matches[0].value.toInt()
            val v2 = matches.last().value.toInt()
            Math.min(v1, v2) to Math.max(v1, v2)
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
