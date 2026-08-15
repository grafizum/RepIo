package com.grafizum.repio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Database(
    entities = [RoutineDayEntity::class, RoutineExerciseEntity::class, LoggedSetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Triggered only once when DB is created
                        CoroutineScope(Dispatchers.IO).launch {
                            // Use the instance we are currently building
                            val dao = getDatabase(context).workoutDao()
                            dao.insertRoutineDays(DefaultData.routineDays)
                            dao.insertRoutineExercises(DefaultData.routineExercises)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
