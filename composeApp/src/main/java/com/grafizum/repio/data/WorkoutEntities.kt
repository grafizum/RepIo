package com.grafizum.repio.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.grafizum.repio.util.currentTimeMillis

enum class DayOfWeekEnum(val displayName: String, val shortName: String) {
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri"),
    SATURDAY("Saturday", "Sat"),
    SUNDAY("Sunday", "Sun")
}

@Entity(tableName = "routine_days")
data class RoutineDayEntity(
    @PrimaryKey val dayOfWeek: String, // MONDAY, TUESDAY, etc.
    val title: String, // e.g. "Monday — Back + Biceps + Shoulders"
    val description: String = "",
    val isRestDay: Boolean = false
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineDayEntity::class,
            parentColumns = ["dayOfWeek"],
            childColumns = ["dayOfWeek"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dayOfWeek"])]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: String,
    val name: String,
    val muscleGroup: String, // Back, Biceps, Shoulders, Chest, Legs, Abs, Triceps
    val targetSets: Int,
    val targetReps: String, // e.g. "8–12"
    val videoUrl: String = "",
    val orderIndex: Int
)

@Entity(tableName = "logged_sets")
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = currentTimeMillis(),
    val dayOfWeek: String,
    val exerciseName: String,
    val muscleGroup: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val notes: String = ""
)
