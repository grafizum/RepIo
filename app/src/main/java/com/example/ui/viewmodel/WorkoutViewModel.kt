package com.example.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppDatabase
import com.example.data.LoggedSetEntity
import com.example.data.RoutineDayEntity
import com.example.data.RoutineExerciseEntity
import com.example.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.util.NotificationHelper

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
    val isWorkoutCompleted: Boolean = false,
    val restTimerSeconds: Int = 0,
    val isTimerActive: Boolean = false,
    val totalSetsLoggedToday: Int = 0
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository
    val allRoutineDays: StateFlow<List<RoutineDayEntity>>
    val allRoutineExercises: StateFlow<List<RoutineExerciseEntity>>
    val allLoggedSets: StateFlow<List<LoggedSetEntity>>

    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean("keep_screen_on", false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _defaultRestTimerSeconds = MutableStateFlow(prefs.getInt("default_rest_timer_seconds", 60))
    val defaultRestTimerSeconds: StateFlow<Int> = _defaultRestTimerSeconds.asStateFlow()

    private val _themeOption = MutableStateFlow(prefs.getString("theme_option", "DEFAULT") ?: "DEFAULT")
    val themeOption: StateFlow<String> = _themeOption.asStateFlow()

    private val _customWorkoutName = MutableStateFlow(prefs.getString("custom_workout_name", "Workout") ?: "Workout")
    val customWorkoutName: StateFlow<String> = _customWorkoutName.asStateFlow()

    private val _isFirstTimeUser = MutableStateFlow(prefs.getBoolean("is_first_time", true))
    val isFirstTimeUser: StateFlow<Boolean> = _isFirstTimeUser.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "system") ?: "system")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _workoutNowState = MutableStateFlow(WorkoutNowState())
    val workoutNowState: StateFlow<WorkoutNowState> = _workoutNowState.asStateFlow()

    private val _showRestartDialog = MutableStateFlow(false)
    val showRestartDialog: StateFlow<Boolean> = _showRestartDialog.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).workoutDao()
        repository = WorkoutRepository(dao)

        _workoutNowState.value = loadSavedWorkoutNowState()

        // Apply saved language preference at startup
        val savedLang = prefs.getString("app_language", "system") ?: "system"
        if (savedLang != "system") {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang))
        }

        viewModelScope.launch {
            repository.ensureDataSeeded(application)
        }

        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                tickTimer()
            }
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
    }

    private fun saveWorkoutNowState(state: WorkoutNowState) {
        prefs.edit()
            .putString("saved_workout_day", state.selectedDayOfWeek)
            .putInt("saved_exercise_index", state.currentExerciseIndex)
            .putInt("saved_set_number", state.currentSetNumber)
            .putFloat("saved_weight_kg", state.currentWeightKg.toFloat())
            .putInt("saved_reps", state.currentReps)
            .putBoolean("saved_is_completed", state.isWorkoutCompleted)
            .putInt("saved_total_sets_logged", state.totalSetsLoggedToday)
            .apply()
    }

    private fun loadSavedWorkoutNowState(): WorkoutNowState {
        val savedDay = prefs.getString("saved_workout_day", null) ?: getTodayDayOfWeek()
        val savedExerciseIndex = prefs.getInt("saved_exercise_index", 0)
        val savedSetNumber = prefs.getInt("saved_set_number", 1)
        val savedWeightKg = prefs.getFloat("saved_weight_kg", 20.0f).toDouble()
        val savedReps = prefs.getInt("saved_reps", 10)
        val savedIsCompleted = prefs.getBoolean("saved_is_completed", false)
        val savedTotalSetsLogged = prefs.getInt("saved_total_sets_logged", 0)

        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        val now = System.currentTimeMillis()
        val remainingSecs = if (endTime > now) ((endTime - now) / 1000L).toInt() + 1 else 0

        return WorkoutNowState(
            selectedDayOfWeek = savedDay,
            currentExerciseIndex = savedExerciseIndex,
            currentSetNumber = savedSetNumber,
            currentWeightKg = savedWeightKg,
            currentReps = savedReps,
            isWorkoutCompleted = savedIsCompleted,
            restTimerSeconds = remainingSecs,
            isTimerActive = remainingSecs > 0,
            totalSetsLoggedToday = savedTotalSetsLogged
        )
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        prefs.edit().putBoolean("keep_screen_on", enabled).apply()
    }

    fun setDefaultRestTimerSeconds(seconds: Int) {
        val validSecs = seconds.coerceAtLeast(10)
        _defaultRestTimerSeconds.value = validSecs
        prefs.edit().putInt("default_rest_timer_seconds", validSecs).apply()
    }

    fun setThemeOption(theme: String) {
        val oldTheme = _themeOption.value
        val oldIsLight = isThemeLight(oldTheme)
        val newIsLight = isThemeLight(theme)

        _themeOption.value = theme
        prefs.edit().putString("theme_option", theme).apply()

        // Only trigger restart dialog and icon/splash change if crossing Light/Dark boundary
        if (oldIsLight != newIsLight) {
            // Icon change is delayed until user confirms in the dialog to prevent immediate app close
            _showRestartDialog.value = true
        }
    }

    private fun isThemeLight(theme: String): Boolean {
        return when (theme.uppercase()) {
            "BEIGE", "EMERALD", "CRIMSON", "AZURE" -> true
            else -> false
        }
    }

    fun dismissRestartDialog() {
        _showRestartDialog.value = false
    }

    fun applyThemeRestart() {
        val currentTheme = _themeOption.value
        setAppIcon(isThemeLight(currentTheme))
        dismissRestartDialog()
    }

    fun setAppIcon(isLightTheme: Boolean) {
        val context = getApplication<Application>()
        if (isLightTheme) {
            com.example.util.IconSwitchHelper.switchIcon(context, com.example.util.IconSwitchHelper.getBeigeAlias(context))
        } else {
            com.example.util.IconSwitchHelper.switchIcon(context, com.example.util.IconSwitchHelper.getOrangeAlias(context))
        }
    }

    fun setAppLanguage(languageCode: String) {
        _appLanguage.value = languageCode
        prefs.edit().putString("app_language", languageCode).apply()
        
        val appLocale: LocaleListCompat = if (languageCode == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun updateWorkoutName(newName: String) {
        val trimmed = newName.trim().ifEmpty { "Workout" }
        _customWorkoutName.value = trimmed
        prefs.edit().putString("custom_workout_name", trimmed).apply()
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
            prefs.edit().putBoolean("is_first_time", false).apply()
        }
    }

    fun applyWorkoutTemplate(template: com.example.data.WorkoutTemplate, customName: String? = null) {
        if (!customName.isNullOrBlank()) {
            updateWorkoutName(customName)
        }
        val context = getApplication<Application>()
        
        // Localize days from template
        val localizedDays = template.dayTemplates.map { dt ->
            val shortDay = when(dt.dayOfWeek.uppercase()) {
                "MONDAY" -> context.getString(R.string.monday_short)
                "TUESDAY" -> context.getString(R.string.tuesday_short)
                "WEDNESDAY" -> context.getString(R.string.wednesday_short)
                "THURSDAY" -> context.getString(R.string.thursday_short)
                "FRIDAY" -> context.getString(R.string.friday_short)
                "SATURDAY" -> context.getString(R.string.saturday_short)
                "SUNDAY" -> context.getString(R.string.sunday_short)
                else -> dt.dayOfWeek
            }
            
            val fullTitle = context.getString(R.string.day_title_format, shortDay, context.getString(dt.titleResId))
            
            RoutineDayEntity(
                dayOfWeek = dt.dayOfWeek,
                title = fullTitle,
                description = context.getString(dt.descResId),
                isRestDay = dt.isRestDay
            )
        }

        viewModelScope.launch {
            repository.applyProgram(localizedDays, template.exercises)
            _isFirstTimeUser.value = false
            prefs.edit().putBoolean("is_first_time", false).apply()
        }
    }

    fun resetFirstTimeSetup() {
        _isFirstTimeUser.value = true
        prefs.edit().putBoolean("is_first_time", true).apply()
    }

    fun cancelFirstTimeSetup() {
        _isFirstTimeUser.value = false
        prefs.edit().putBoolean("is_first_time", false).apply()
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
        val newState = _workoutNowState.value.copy(
            currentWeightKg = weight.coerceAtLeast(0.0),
            currentReps = reps.coerceAtLeast(1)
        )
        _workoutNowState.value = newState
        saveWorkoutNowState(newState)
    }

    fun startRestTimer(seconds: Int) {
        if (seconds <= 0) return
        
        // Ensure any existing alarm/timer is fully stopped before starting a new one
        stopTimer()

        val endTime = System.currentTimeMillis() + (seconds * 1000L)
        prefs.edit().putLong("timer_end_time_millis", endTime).apply()
        scheduleTimerAlarm(endTime)
        
        _workoutNowState.update { 
            it.copy(
                restTimerSeconds = seconds,
                isTimerActive = true
            )
        }
    }

    private fun scheduleTimerAlarm(endTimeMillis: Long) {
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(app, com.example.util.RestTimerReceiver::class.java).apply {
            // Add identifying timestamp to distinguish this specific timer instance
            putExtra("TIMER_END_TIME", endTimeMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            app,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            } catch (e2: Exception) {
                // fallback
            }
        }
    }

    private fun cancelTimerAlarm() {
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(app, com.example.util.RestTimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            app,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * CRITICAL USER FEATURE: "Workout Now" real-time single-tap logging.
     * Logs current set and IMMEDIATELY swaps to the next set or exercise of the day!
     */
    fun logCurrentSetAndAdvance(
        exercises: List<RoutineExerciseEntity>
    ) {
        if (exercises.isEmpty()) return

        // Kill any currently active timer before logging a new set
        stopTimer()

        viewModelScope.launch {
            val currentState = _workoutNowState.value
            val exerciseIndex = currentState.currentExerciseIndex.coerceIn(0, exercises.size - 1)
            val currentExercise = exercises[exerciseIndex]

            repository.logSet(
                dayOfWeek = currentState.selectedDayOfWeek,
                exerciseName = currentExercise.name,
                muscleGroup = currentExercise.muscleGroup,
                setNumber = currentState.currentSetNumber,
                weightKg = currentState.currentWeightKg,
                reps = currentState.currentReps
            )

            val restDuration = _defaultRestTimerSeconds.value
            val nextSetNumber = currentState.currentSetNumber + 1
            
            _workoutNowState.update { state ->
                if (nextSetNumber <= currentExercise.targetSets) {
                    // Advance to next set of same exercise
                    state.copy(
                        currentSetNumber = nextSetNumber,
                        totalSetsLoggedToday = state.totalSetsLoggedToday + 1
                    )
                } else {
                    // Exercise completed! Advance to next exercise
                    val nextExerciseIndex = exerciseIndex + 1
                    if (nextExerciseIndex < exercises.size) {
                        state.copy(
                            currentExerciseIndex = nextExerciseIndex,
                            currentSetNumber = 1,
                            totalSetsLoggedToday = state.totalSetsLoggedToday + 1
                        )
                    } else {
                        // All exercises for today completed!
                        state.copy(
                            isWorkoutCompleted = true,
                            totalSetsLoggedToday = state.totalSetsLoggedToday + 1,
                            restTimerSeconds = 0,
                            isTimerActive = false
                        )
                    }
                }
            }
            
            // Start the timer separately to keep logic clean
            val updatedState = _workoutNowState.value
            if (!updatedState.isWorkoutCompleted) {
                val duration = if (updatedState.currentSetNumber == 1) restDuration + 30 else restDuration
                startRestTimer(duration)
            }
            
            saveWorkoutNowState(_workoutNowState.value)
        }
    }

    fun resetWorkoutSession() {
        stopTimer()
        val newState = _workoutNowState.value.copy(
            currentExerciseIndex = 0,
            currentSetNumber = 1,
            isWorkoutCompleted = false,
            totalSetsLoggedToday = 0,
            isTimerActive = false,
            restTimerSeconds = 0
        )
        _workoutNowState.value = newState
        saveWorkoutNowState(newState)
    }

    fun tickTimer() {
        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        if (endTime > 0L) {
            val now = System.currentTimeMillis()
            val remainingMillis = endTime - now
            if (remainingMillis > 0) {
                val remainingSecs = (remainingMillis / 1000L).toInt() + 1
                _workoutNowState.update { 
                    if (it.restTimerSeconds != remainingSecs || !it.isTimerActive) {
                        it.copy(restTimerSeconds = remainingSecs, isTimerActive = true)
                    } else it
                }
            } else {
                // Timer finished!
                val wasActive = _workoutNowState.value.isTimerActive
                
                // CRITICAL: Stop the timer first to prevent double triggers
                stopTimer()
                
                if (wasActive) {
                    NotificationHelper.showRestTimerEndedNotification(getApplication())
                }
            }
        } else if (_workoutNowState.value.isTimerActive) {
            _workoutNowState.update { it.copy(restTimerSeconds = 0, isTimerActive = false) }
        }
    }

    fun stopTimer() {
        prefs.edit().putLong("timer_end_time_millis", 0L).apply()
        cancelTimerAlarm()
        _workoutNowState.value = _workoutNowState.value.copy(restTimerSeconds = 0, isTimerActive = false)
    }

    fun checkTimerOnAppClose() {
        val endTime = prefs.getLong("timer_end_time_millis", 0L)
        if (endTime > 0L) {
            val now = System.currentTimeMillis()
            if (endTime <= now) {
                // Timer finished right as or before app closed
                prefs.edit().putLong("timer_end_time_millis", 0L).apply()
                _workoutNowState.value = _workoutNowState.value.copy(
                    restTimerSeconds = 0,
                    isTimerActive = false
                )
                NotificationHelper.showRestTimerEndedNotification(getApplication())
            } else {
                // Ensure alarm is scheduled for background wakeup
                scheduleTimerAlarm(endTime)
            }
        }
    }

    fun deleteLoggedSet(id: Long) {
        viewModelScope.launch {
            repository.deleteLoggedSet(id)
        }
    }

    fun saveExercise(exercise: RoutineExerciseEntity) {
        viewModelScope.launch {
            repository.saveExercise(exercise)
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch {
            repository.deleteExercise(id)
        }
    }

    fun resetToDefaultProgram() {
        viewModelScope.launch {
            repository.resetRoutineToDefault(getApplication())
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
