package com.grafizum.repio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.grafizum.repio.util.startOfTodayMillis

class WorkoutRepository(private val dao: WorkoutDao) {

    val allRoutineDays: Flow<List<RoutineDayEntity>> = dao.getAllRoutineDays()
    val allRoutineExercises: Flow<List<RoutineExerciseEntity>> = dao.getAllRoutineExercises()
    val allLoggedSets: Flow<List<LoggedSetEntity>> = dao.getAllLoggedSets()

    /** The cached overall-level row (see [OverallLevelCalculator]), null before the first computation. */
    val userStats: Flow<UserStatsEntity?> = dao.getUserStats()

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

    /**
     * Removes the sets logged TODAY for [exerciseName] and answers how many went.
     *
     * A SWAP MEANS A DIFFERENT EXERCISE. Owner, 2026-11-03: *"When swapping
     * exercises position last second while still being on that exercise, then
     * reset the exercise no matter how many sets were made (since the new
     * exercise is a different one when swapping)."* The log is keyed by exercise
     * NAME, so the movement that was swapped away kept its sets: put it back and
     * it returned already finished, which is the state the owner was looking at.
     *
     * Only the current day is touched. Yesterday's work is history and stays; the
     * reset is about the session in hand, not about forgetting training.
     */
    suspend fun clearTodaysLoggedSets(exerciseName: String): Int {
        val startedToday = startOfTodayMillis()
        val todays = dao.getLoggedSetsForExercise(exerciseName).first()
            .filter { it.timestamp >= startedToday }
        todays.forEach { dao.deleteLoggedSet(it.id) }
        return todays.size
    }

    /** Persists the computed overall level; there is only ever one row. */
    suspend fun saveUserStats(stats: UserStatsEntity) {
        dao.upsertUserStats(stats)
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

    /** Locks/unlocks an exercise for level progression, across all of its rows. */
    suspend fun setExerciseLocked(name: String, locked: Boolean) {
        dao.setExerciseLockedByName(name, locked)
    }

    /** The exercise's lock flag, or null when the exercise is not in the routine. */
    suspend fun isExerciseLocked(name: String): Boolean? =
        dao.isExerciseLockedByName(name)

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

    /**
     * Swaps [original] for [replacements] (may be empty = restore the original),
     * keeping the day's ordering intact. [previousNames] lets a caller that applies
     * several steps in a row drop exercises it inserted earlier; it is empty for the
     * single-shot replace used by the routine editor.
     */
    suspend fun replaceExercise(
        original: RoutineExerciseEntity,
        previousNames: List<String>,
        replacements: List<RoutineExerciseEntity>
    ) {
        // Reuse known demo URLs so swapped-in exercises keep their video links.
        val existingUrls = dao.getAllRoutineExercises().first()
            .filter { it.videoUrl.isNotBlank() }
            .associate { it.name to it.videoUrl }
        val synced = replacements.map { ex ->
            if (ex.videoUrl.isBlank()) existingUrls[ex.name]?.let { url -> ex.copy(videoUrl = url) } ?: ex
            else ex
        }

        val dayExercises = dao.getExercisesForDay(original.dayOfWeek).first().sortedBy { it.orderIndex }
        val kept = dayExercises.filter { it.id != original.id && it.name !in previousNames }
        val anchorIndex = dayExercises.indexOfFirst { it.id == original.id || it.name in previousNames }
            .let { if (it == -1) kept.size else it }

        // Empty selection = restore the original exercise at its slot.
        val effective = if (synced.isEmpty()) listOf(original.copy(id = 0L)) else synced

        // Rebuild the day and renumber it sequentially so the new exercises land
        // exactly where the old one was, in the user's selected order.
        val rebuilt = kept.toMutableList().apply { addAll(anchorIndex, effective) }
        val renumbered = rebuilt.mapIndexed { index, ex -> ex.copy(orderIndex = index + 1) }
        val keptIds = kept.map { it.id }.toSet()
        // Rows that are no longer part of the day have to be deleted explicitly:
        // @Update only touches ids that still exist, so without this the outgoing
        // exercise stayed in the table and the day gained an exercise every swap.
        dayExercises.map { it.id }.filterNot { it in keptIds }.forEach { dao.deleteExerciseById(it) }
        dao.updateExercises(renumbered.filter { it.id in keptIds })
        dao.insertRoutineExercises(renumbered.filter { it.id !in keptIds })

        // Sync video URLs by name to the rest of the routine (same convention as saveExercise).
        effective.filter { it.videoUrl.isNotBlank() }.forEach {
            dao.updateVideoUrlForExercise(it.name, it.videoUrl)
        }
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

    /**
     * Restores the built-in default routine. [equipment] (empty = no
     * restriction) gates the prefill — the default program assumes a full
     * gym, so a user whose saved kit has no barbell gets its barbell rows
     * substituted to the nearest available link instead of rows they cannot
     * perform. Their demo links follow the new movement names.
     */
    suspend fun resetRoutineToDefault(equipment: List<String> = emptyList()) {
        val existingUrls = dao.getAllRoutineExercises().first()
            .filter { it.videoUrl.isNotBlank() }
            .associate { it.name to it.videoUrl }

        val exercises = if (equipment.isEmpty()) DefaultData.routineExercises
        else DefaultData.routineExercises.mapNotNull { ex ->
            WorkoutTemplates.substituteExercise(ex.name, equipment)?.let { sub ->
                if (sub.equals(ex.name, ignoreCase = true)) ex else ex.copy(name = sub)
            }
        }

        dao.clearAllRoutineExercises()
        dao.clearAllRoutineDays()
        val localizedDays = localizeDayTemplates(DefaultData.dayTemplates)
        dao.insertRoutineDays(localizedDays)

        val syncedExercises = exercises.map { ex ->
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
