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
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }
                    })
                    .build()
                
                // Pre-populate on initial DB creation
                // We use a separate thread and the instance we just built
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = instance.workoutDao()
                    if (dao.getAllRoutineDays().first().isEmpty()) {
                        dao.insertRoutineDays(DefaultData.routineDays)
                        dao.insertRoutineExercises(DefaultData.routineExercises)
                    }
                }

                INSTANCE = instance
                instance
            }
        }
    }
}
