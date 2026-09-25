package com.grafizum.repio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RoutineDayEntity::class,
        RoutineExerciseEntity::class,
        LoggedSetEntity::class,
        UserStatsEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v5 -> v6: adds the per-exercise tempo cue column (training style). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN tempo TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v6 -> v7: adds the cached overall-level row (see [UserStatsEntity] and
         * [OverallLevelCalculator]). Created empty: the first recomputation fills it
         * in, and an empty table reads as "not enough exercises yet", which is the
         * right answer for an upgrade that has not recomputed yet.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS user_stats (" +
                        "id INTEGER NOT NULL PRIMARY KEY, " +
                        "overallStrengthLevel REAL, " +
                        "overallLevelReady INTEGER NOT NULL, " +
                        "overallLevelLastComputed INTEGER NOT NULL, " +
                        "exercisesCountForLevel INTEGER NOT NULL)"
                )
            }
        }

        /**
         * v7 -> v8: adds the per-exercise level-lock column (see
         * [RoutineExerciseEntity.locked]). Everything starts unlocked; the flag
         * is only ever set by the user from the workout card's lock icon.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN locked INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker_db"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}