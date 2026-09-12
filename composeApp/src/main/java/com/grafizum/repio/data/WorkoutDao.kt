package com.grafizum.repio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM routine_days")
    fun getAllRoutineDays(): Flow<List<RoutineDayEntity>>

    /** One-shot read used by the background skip nudge (no Flow to collect). */
    @Query("SELECT * FROM routine_days")
    suspend fun getAllRoutineDaysOnce(): List<RoutineDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineDays(days: List<RoutineDayEntity>)

    @Query("SELECT * FROM routine_exercises ORDER BY orderIndex ASC")
    fun getAllRoutineExercises(): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE dayOfWeek = :dayOfWeek ORDER BY orderIndex ASC")
    fun getExercisesForDay(dayOfWeek: String): Flow<List<RoutineExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: RoutineExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: RoutineExerciseEntity)

    @Update
    suspend fun updateExercises(exercises: List<RoutineExerciseEntity>)

    @Query("UPDATE routine_exercises SET videoUrl = :videoUrl WHERE name = :name")
    suspend fun updateVideoUrlForExercise(name: String, videoUrl: String)

    @Query("SELECT videoUrl FROM routine_exercises WHERE name = :name AND videoUrl != '' LIMIT 1")
    suspend fun getVideoUrlForExercise(name: String): String?

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedSet(loggedSet: LoggedSetEntity): Long

    @Query("DELETE FROM logged_sets WHERE id = :id")
    suspend fun deleteLoggedSet(id: Long)

    @Query("SELECT * FROM logged_sets ORDER BY timestamp DESC")
    fun getAllLoggedSets(): Flow<List<LoggedSetEntity>>

    /** One-shot read used by the background skip nudge (no Flow to collect). */
    @Query("SELECT * FROM logged_sets ORDER BY timestamp DESC")
    suspend fun getAllLoggedSetsOnce(): List<LoggedSetEntity>

    @Query("DELETE FROM logged_sets WHERE timestamp >= :startTime")
    suspend fun deleteLoggedSetsSince(startTime: Long)

    @Query("SELECT * FROM logged_sets WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getLoggedSetsSince(startTime: Long): Flow<List<LoggedSetEntity>>

    @Query("SELECT * FROM logged_sets WHERE exerciseName = :exerciseName ORDER BY timestamp ASC")
    fun getLoggedSetsForExercise(exerciseName: String): Flow<List<LoggedSetEntity>>

    @Query("DELETE FROM routine_exercises")
    suspend fun clearAllRoutineExercises()

    @Query("DELETE FROM routine_days")
    suspend fun clearAllRoutineDays()

    @Query("DELETE FROM logged_sets")
    suspend fun clearAllLoggedSets()
}
