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
    val targetIntensity: String = "", // e.g. "RPE 8" or "Heavy"
    val targetWeightKg: Double = 0.0,
    val restSeconds: Int = 60,
    val transitionRestSeconds: Int = 90,
    val videoUrl: String = "",
    val orderIndex: Int,
    // Tempo cue from the training style ("explosive", "slow" or "" = balanced).
    val tempo: String = "",
    // Per-exercise level lock: when true the exercise is pinned to its current
    // level and stage — the stepper is disabled and automatic progression skips
    // it. Toggled from the workout card's lock icon; every row of the same
    // exercise (across days) carries the flag, because the progression state
    // itself is keyed by name.
    val locked: Boolean = false
)

/**
 * The app-wide "Overall Level" — a single row (id = 1) holding the cached
 * result of [OverallLevelCalculator].
 *
 * The value is derived, but it is stored rather than recomputed by every
 * reader: the home stat renders on the first frame from the last saved result
 * instead of waiting for the history to load. The row is rewritten whenever
 * the logged history or a per-exercise level changes, so it is never the
 * source of truth — [OverallLevelCalculator] is.
 */
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    /**
     * Recency-weighted average of the qualifying exercises' levels, rounded to
     * one decimal, or null while the user has not trained enough of them.
     */
    val overallStrengthLevel: Double? = null,
    /** False until [OverallLevelCalculator.MIN_EXERCISES] exercises qualify. */
    val overallLevelReady: Boolean = false,
    /** When the row was last recomputed (millis since epoch). */
    val overallLevelLastComputed: Long = 0L,
    /** How many exercises went into [overallStrengthLevel]. */
    val exercisesCountForLevel: Int = 0
) {
    companion object {
        /** There is only ever one overall-level row. */
        const val SINGLETON_ID = 1
    }
}

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
