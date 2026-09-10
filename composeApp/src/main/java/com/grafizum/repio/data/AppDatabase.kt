package com.grafizum.repio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RoutineDayEntity::class, RoutineExerciseEntity::class, LoggedSetEntity::class],
    version = 6,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker_db"
                )
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}