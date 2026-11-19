package com.grafizum.repio

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.grafizum.repio.data.AppDatabase
import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.WorkoutRepository
import com.grafizum.repio.util.startOfTodayMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A SWAP RESETS THE EXERCISE IT LEFT.
 *
 * Owner, 2026-11-03: *"When swapping exercises position last second while still
 * being on that exercise, then reset the exercise no matter how many sets were
 * made (since the new exercise is a different one when swapping)."*
 *
 * The log is keyed by exercise NAME, so before this the swapped-out movement
 * kept its sets: the screen looked reset because it filters by the new name, but
 * the old work was still in the row and came back the moment the exercise was
 * restored. The reset is scoped to TODAY - yesterday is history - so both halves
 * are pinned here, the deletion and the line it must not cross.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwapClearsTodaysSetsTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(db.workoutDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun loggedSet(name: String, timestamp: Long) = LoggedSetEntity(
        timestamp = timestamp,
        dayOfWeek = "MONDAY",
        exerciseName = name,
        muscleGroup = "Chest",
        setNumber = 1,
        weightKg = 20.0,
        reps = 10
    )

    @Test
    fun everySetLoggedTodayForTheExerciseGoes() = runBlocking {
        val today = startOfTodayMillis() + 60_000L
        db.workoutDao().insertLoggedSet(loggedSet("Push-Up", today))
        db.workoutDao().insertLoggedSet(loggedSet("Push-Up", today + 1_000L))
        db.workoutDao().insertLoggedSet(loggedSet("Push-Up", today + 2_000L))

        val removed = repository.clearTodaysLoggedSets("Push-Up")

        assertEquals("all three of today's sets are the reset", 3, removed)
        assertEquals(
            "nothing is left to come back",
            0,
            db.workoutDao().getLoggedSetsForExercise("Push-Up").first().size
        )
    }

    @Test
    fun anEarlierDayIsHistoryAndStays() = runBlocking {
        val yesterday = startOfTodayMillis() - 60_000L
        db.workoutDao().insertLoggedSet(loggedSet("Push-Up", yesterday))
        db.workoutDao().insertLoggedSet(loggedSet("Push-Up", startOfTodayMillis() + 60_000L))

        val removed = repository.clearTodaysLoggedSets("Push-Up")

        assertEquals("only today's set is cleared", 1, removed)
        val kept = db.workoutDao().getLoggedSetsForExercise("Push-Up").first()
        assertEquals("the earlier day is untouched", 1, kept.size)
        assertEquals(yesterday, kept.single().timestamp)
    }

    @Test
    fun theExerciseThatStayedBehindIsUntouched() = runBlocking {
        val today = startOfTodayMillis() + 60_000L
        db.workoutDao().insertLoggedSet(loggedSet("Push-Up", today))
        db.workoutDao().insertLoggedSet(loggedSet("Dumbbell Bench Press", today))

        repository.clearTodaysLoggedSets("Push-Up")

        assertEquals(
            "another exercise's sets are not collateral",
            1,
            db.workoutDao().getLoggedSetsForExercise("Dumbbell Bench Press").first().size
        )
    }
}
