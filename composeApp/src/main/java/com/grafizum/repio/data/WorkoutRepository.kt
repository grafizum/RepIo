package com.grafizum.repio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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
                if (exercise.videoUrl.contains("youtube") || exercise.videoUrl.contains("youtu.be")) {
                    dao.updateExercise(exercise.copy(videoUrl = ""))
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
        if (exercise.id == 0L) {
            dao.insertExercise(exercise)
        } else {
            dao.updateExercise(exercise)
        }
    }

    suspend fun deleteExercise(id: Long) {
        dao.deleteExerciseById(id)
    }

    suspend fun applyProgram(days: List<RoutineDayEntity>, exercises: List<RoutineExerciseEntity>) {
        dao.clearAllRoutineExercises()
        dao.clearAllRoutineDays()
        dao.insertRoutineDays(days)
        dao.insertRoutineExercises(exercises)
    }

    suspend fun resetRoutineToDefault() {
        dao.clearAllRoutineExercises()
        dao.clearAllRoutineDays()
        val localizedDays = localizeDayTemplates(DefaultData.dayTemplates)
        dao.insertRoutineDays(localizedDays)
        dao.insertRoutineExercises(DefaultData.routineExercises)
    }

    suspend fun clearHistory() {
        dao.clearAllLoggedSets()
    }
}
