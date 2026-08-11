package com.example.data

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

    suspend fun ensureDataSeeded(context: android.content.Context) {
        val days = dao.getAllRoutineDays().first()
        if (days.isEmpty()) {
            val localizedDays = localizeDayTemplates(context, DefaultData.dayTemplates)
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

    private fun localizeDayTemplates(context: android.content.Context, templates: List<DayTemplate>): List<RoutineDayEntity> {
        return templates.map { dt ->
            val shortDay = when(dt.dayOfWeek.uppercase()) {
                "MONDAY" -> context.getString(com.example.R.string.monday_short)
                "TUESDAY" -> context.getString(com.example.R.string.tuesday_short)
                "WEDNESDAY" -> context.getString(com.example.R.string.wednesday_short)
                "THURSDAY" -> context.getString(com.example.R.string.thursday_short)
                "FRIDAY" -> context.getString(com.example.R.string.friday_short)
                "SATURDAY" -> context.getString(com.example.R.string.saturday_short)
                "SUNDAY" -> context.getString(com.example.R.string.sunday_short)
                else -> dt.dayOfWeek
            }
            val fullTitle = context.getString(com.example.R.string.day_title_format, shortDay, context.getString(dt.titleResId))
            RoutineDayEntity(
                dayOfWeek = dt.dayOfWeek,
                title = fullTitle,
                description = context.getString(dt.descResId),
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

    suspend fun resetRoutineToDefault(context: android.content.Context) {
        dao.clearAllRoutineExercises()
        dao.clearAllRoutineDays()
        val localizedDays = localizeDayTemplates(context, DefaultData.dayTemplates)
        dao.insertRoutineDays(localizedDays)
        dao.insertRoutineExercises(DefaultData.routineExercises)
    }

    suspend fun clearHistory() {
        dao.clearAllLoggedSets()
    }
}
