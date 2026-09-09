package com.grafizum.repio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.grafizum.repio.util.startOfTodayMillis

class WorkoutRepository(private val dao: WorkoutDao) {

    val allRoutineDays: Flow<List<RoutineDayEntity>> = dao.getAllRoutineDays()
    val allRoutineExercises: Flow<List<RoutineExerciseEntity>> = dao.getAllRoutineExercises()
    val allLoggedSets: Flow<List<LoggedSetEntity>> = dao.getAllLoggedSets()

    fun getExercisesForDay(dayOfWeek: String): Flow<List<RoutineExerciseEntity>> {
        return dao.getExercisesForDay(dayOfWeek)
    }

    fun getLoggedSetsSince(startTime: Long): Flow<List<LoggedSetEntity>> {
        return dao.getLoggedSetsSince(startTime)
    }

    fun getLoggedSetsForExercise(exerciseName: String): Flow<List<LoggedSetEntity>> {
        return dao.getLoggedSetsForExercise(exerciseName)
    }

    suspend fun ensureDataSeeded() {
        val days = dao.getAllRoutineDays().first()
        if (days.isEmpty()) {
            // Localize days using common getString bridge (requires Composable scope or a non-composable getString bridge)
            // Wait, getString bridge is @Composable. I'll make a non-composable one too.
            // For now, I'll just use the default titles from DefaultData.
            val localizedDays = localizeDayTemplates(DefaultData.dayTemplates)
            dao.insertRoutineDays(localizedDays)
            dao.insertRoutineExercises(DefaultData.routineExercises)
        } else {
            val exercises = dao.getAllRoutineExercises().first()
            exercises.forEach { exercise ->
                var updated = exercise
                if (exercise.videoUrl.contains("youtube") || exercise.videoUrl.contains("youtu.be")) {
                    updated = updated.copy(videoUrl = "")
                }
                // Migration: ensure every exercise targets a rep range of at least 10 reps
                val normalizedReps = WorkoutTemplates.enforceRepRange(exercise.targetReps)
                if (normalizedReps != exercise.targetReps) {
                    updated = updated.copy(targetReps = normalizedReps)
                }
                if (updated != exercise) {
                    dao.updateExercise(updated)
                }
            }
        }
    }

    private fun localizeDayTemplates(templates: List<DayTemplate>): List<RoutineDayEntity> {
        return templates.map { dt ->
            // Use placeholders for now, UI will translate them
            RoutineDayEntity(
                dayOfWeek = dt.dayOfWeek,
                title = dt.dayOfWeek + " — " + dt.dayOfWeek, // Placeholder
                description = "Recovery", // Placeholder
                isRestDay = dt.isRestDay
            )
        }
    }

    suspend fun logSet(
        dayOfWeek: String,
        exerciseName: String,
        muscleGroup: String,
        setNumber: Int,
        weightKg: Double,
        reps: Int,
        notes: String = ""
    ): Long {
        return dao.insertLoggedSet(
            LoggedSetEntity(
                dayOfWeek = dayOfWeek,
                exerciseName = exerciseName,
                muscleGroup = muscleGroup,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                notes = notes
            )
        )
    }

    suspend fun deleteLoggedSet(id: Long) {
        dao.deleteLoggedSet(id)
    }

    suspend fun saveExercise(exercise: RoutineExerciseEntity) {
        // Ensure day exists in routine_days to satisfy foreign key constraint
        val days = dao.getAllRoutineDays().first()
        if (days.none { it.dayOfWeek == exercise.dayOfWeek }) {
            dao.insertRoutineDays(listOf(RoutineDayEntity(
                dayOfWeek = exercise.dayOfWeek,
                title = exercise.dayOfWeek,
                description = "Custom day",
                isRestDay = false
            )))
        }

        var finalExercise = exercise
        if (exercise.videoUrl.isBlank()) {
            val existingUrl = dao.getVideoUrlForExercise(exercise.name)
            if (existingUrl != null) {
                finalExercise = exercise.copy(videoUrl = existingUrl)
            }
        }

        if (finalExercise.id == 0L) {
            dao.insertExercise(finalExercise)
        } else {
            dao.updateExercise(finalExercise)
        }

        // Sync to all others with the same name
        if (finalExercise.videoUrl.isNotBlank()) {
            dao.updateVideoUrlForExercise(finalExercise.name, finalExercise.videoUrl)
        }
    }

    suspend fun reorderExercises(exercises: List<RoutineExerciseEntity>) {
        dao.updateExercises(exercises)
    }

    suspend fun markDayAsWorkoutDay(dayOfWeek: String) {
        val days = dao.getAllRoutineDays().first()
        val existing = days.firstOrNull { it.dayOfWeek == dayOfWeek }
        val updated = existing?.copy(isRestDay = false)
            ?: RoutineDayEntity(dayOfWeek, dayOfWeek, "Custom day", false)
        dao.insertRoutineDays(listOf(updated))
    }

    suspend fun deleteExercise(id: Long) {
        dao.deleteExerciseById(id)
    }

    suspend fun applyProgram(days: List<RoutineDayEntity>, exercises: List<RoutineExerciseEntity>) {
        val existingUrls = dao.getAllRoutineExercises().first()
            .filter { it.videoUrl.isNotBlank() }
            .associate { it.name to it.videoUrl }

        val syncedExercises = exercises.map { ex ->
            if (ex.videoUrl.isBlank()) {
                existingUrls[ex.name]?.let { url -> ex.copy(videoUrl = url) } ?: ex
            } else ex
        }

        dao.clearAllRoutineExercises()
        dao.clearAllRoutineDays()
        dao.insertRoutineDays(days)
        dao.insertRoutineExercises(syncedExercises)
    }

    suspend fun resetRoutineToDefault() {
        val existingUrls = dao.getAllRoutineExercises().first()
            .filter { it.videoUrl.isNotBlank() }
            .associate { it.name to it.videoUrl }

        dao.clearAllRoutineExercises()
        dao.clearAllRoutineDays()
        val localizedDays = localizeDayTemplates(DefaultData.dayTemplates)
        dao.insertRoutineDays(localizedDays)

        val syncedExercises = DefaultData.routineExercises.map { ex ->
            if (ex.videoUrl.isBlank()) {
                existingUrls[ex.name]?.let { url -> ex.copy(videoUrl = url) } ?: ex
            } else ex
        }
        dao.insertRoutineExercises(syncedExercises)
    }

    suspend fun clearHistory() {
        dao.clearAllLoggedSets()
    }

    suspend fun deleteTodayLogs() {
        dao.deleteLoggedSetsSince(startOfTodayMillis())
    }
}
