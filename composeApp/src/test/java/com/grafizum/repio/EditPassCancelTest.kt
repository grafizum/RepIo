package com.grafizum.repio

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.grafizum.repio.data.AppDatabase
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutRepository
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * TWO RULINGS OF 2026-10-17, both about a screen that said one thing and did
 * another.
 *
 * 1. "Cancel button in the level selection doesnt actually cancel." It did
 *    not, because the editor's level rows WRITE on the tap — the owner's own
 *    ruling of 2026-10-08, "when he selects it directly apply it" — so by the
 *    time Cancel was pressed the level, the stage and the unlock were already
 *    persisted and Cancel only closed the window. The tap stays immediate (the
 *    card behind the editor still moves), and Cancel puts the pass back where
 *    it opened.
 *
 * 2. "Remove the 'progression available' label if the user only selected the
 *    level for progression and didnt do it yet." The offer is keyed off the
 *    STORED state, and a stored state is written two ways: by the engine after
 *    every set of a stage met its target, and by the picker when a user points
 *    at a rung. Pointing at the top rung used to make the label appear with no
 *    work behind it.
 *
 * 3. "When replacing an exercise i feel like the new exercise doesnt already
 *    start from the level that was once selected to do that exercise."
 *
 * The three are one story: a level the app can tell apart from the work.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditPassCancelTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: WorkoutViewModel

    /** A pool exercise with a real ladder; the tests are about its state machine. */
    private val exercise = "Push-Up"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = WorkoutViewModel(WorkoutRepository(db.workoutDao()))
        viewModel.setLevelSystemEnabled(true)
        checkNotNull(ProgressionEngine.entryFor(exercise)) {
            "Push-Up must stay in the pool, or these tests are about nothing"
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun cancelPutsTheLevelBackWhereTheEditPassOpened() {
        val before = viewModel.currentProgressionState(exercise)
        val max = viewModel.progressionMaxLevel(exercise)
        val jumpedTo = if (before?.level == max) 1 else max

        viewModel.setExerciseLevelFromEditor(exercise, jumpedTo)
        assertNotEquals(
            "the editor's tap must still apply immediately (the owner's 2026-10-08 ruling)",
            before?.level,
            viewModel.currentProgressionState(exercise)?.level
        )

        viewModel.cancelExerciseEditPass(exercise)

        assertEquals(
            "Cancel must restore the state the pass opened on",
            before,
            viewModel.currentProgressionState(exercise)
        )
    }

    @Test
    fun cancelAfterAPickTheUserNeverHadRemovesTheStoredStateAltogether() {
        assertNull(
            "nothing should be stored before the first pick",
            viewModel.storedProgressionRawForTest(exercise)
        )

        viewModel.setExerciseLevelFromEditor(exercise, viewModel.progressionMaxLevel(exercise))
        assertNotNull(
            "the pick must have stored a state",
            viewModel.storedProgressionRawForTest(exercise)
        )

        viewModel.cancelExerciseEditPass(exercise)

        assertNull(
            "a cancelled pick must not leave a stored level behind the card",
            viewModel.storedProgressionRawForTest(exercise)
        )
    }

    @Test
    fun saveKeepsWhatThePassWrote() {
        val max = viewModel.progressionMaxLevel(exercise)
        viewModel.setExerciseLevelFromEditor(exercise, max)
        val afterSave = viewModel.currentProgressionState(exercise)

        // The dialog's Save commits the pass before it closes.
        viewModel.commitExerciseEditPass(exercise)
        // A later Cancel — from a pass that never wrote anything — is a no-op.
        viewModel.cancelExerciseEditPass(exercise)

        assertEquals(
            "a committed pass must not be undone by a later Cancel",
            afterSave,
            viewModel.currentProgressionState(exercise)
        )
    }

    @Test
    fun selectingTheTopRungWithoutTrainingItOffersNoProgression() {
        walkToTopBySelection()
        assertFalse(
            "a rung the user only POINTED AT is not a rung they have done",
            viewModel.isProgressionAvailable(exercise)
        )
    }

    @Test
    fun aRungTheEngineWalkedThroughStillOffersItsProgression() {
        val state = walkToTopBySelection()

        // What [advanceProgressionIfNeeded] writes when every set of a stage met
        // its target: the rung the user was on is marked done. At the top of the
        // ladder the state does not move on, which is when the offer is due.
        viewModel.markProgressionEarned(exercise, state)
        assertTrue(
            "a rung the engine walked through must still offer its progression",
            viewModel.isProgressionAvailable(exercise)
        )
    }

    @Test
    fun replacingAnExerciseCarriesTheLevelTheUserWasOn() {
        val original = RoutineExerciseEntity(
            id = 1L, dayOfWeek = "Monday", name = exercise, muscleGroup = "Chest",
            targetSets = 3, targetReps = "8-12", targetIntensity = "Medium",
            targetWeightKg = 0.0, restSeconds = 60, videoUrl = "", orderIndex = 0
        )
        viewModel.saveExercise(original)
        settle("the original row must be stored") {
            viewModel.allRoutineExercises.value.any { it.name == exercise }
        }

        val levelBefore = viewModel.currentProgressionState(exercise)?.level
            ?: error("Push-Up must have a starting level")
        viewModel.setExerciseLevelFromEditor(exercise, levelBefore + 1)
        val chosen = viewModel.currentProgressionState(exercise)!!.level

        val replacement = original.copy(id = 2L, name = "Wide Grip Push Ups")
        viewModel.replaceExerciseWith(original, listOf(exercise), listOf(replacement))

        settle("the replacement must inherit the level") {
            viewModel.storedProgressionRawForTest("Wide Grip Push Ups") != null
        }
        assertEquals(
            "the replacement must start on the level the user had selected",
            viewModel.storedProgressionRawForTest(exercise),
            viewModel.storedProgressionRawForTest("Wide Grip Push Ups")
        )
        assertEquals(
            "…at the exact level, read through the engine's own clamp",
            chosen,
            viewModel.currentProgressionState("Wide Grip Push Ups")?.level
        )
    }

    @Test
    fun acceptingAProgressionChangesTheExerciseOnEveryDay() {
        val original = RoutineExerciseEntity(
            id = 1L, dayOfWeek = "Monday", name = exercise, muscleGroup = "Chest",
            targetSets = 3, targetReps = "8-12", targetIntensity = "Medium",
            targetWeightKg = 0.0, restSeconds = 60, videoUrl = "", orderIndex = 0
        )
        viewModel.saveExercise(original)
        settle("Monday's row must be stored") {
            viewModel.allRoutineExercises.value.any { it.name == exercise }
        }
        viewModel.saveExercise(original.copy(id = 2L, dayOfWeek = "Tuesday"))
        settle("Tuesday's row must be stored") {
            viewModel.allRoutineExercises.value.count { it.name == exercise } == 2
        }

        val entry = ProgressionEngine.entryFor(exercise)!!
        val target = ProgressionEngine.preferredBranch(entry)?.entry
            ?: error("$exercise must keep a same-movement offer, or this test is about nothing")

        viewModel.acceptEvolution(exercise, target)
        settle("both days must carry the progressed exercise") {
            viewModel.allRoutineExercises.value.none { it.name == exercise }
        }
        assertEquals(
            "progressing must change the exercise on every day it appears",
            2,
            viewModel.allRoutineExercises.value.count { it.name == target }
        )
    }

    @Test
    fun aHeldLevelUpOnlyMovesTheLevelWhenTheDayEnds() {
        seedDay(exercise)
        val before = viewModel.currentProgressionState(exercise)!!

        // What [advanceProgressionIfNeeded] writes when a stage's every set met
        // its target: the move is HELD, not applied.
        viewModel.holdLevelUpForTest(
            exercise,
            ProgressionEngine.ProgressionState(before.level + 1, 1)
        )

        assertEquals(
            "the level must not move while the day is still running",
            before,
            viewModel.currentProgressionState(exercise)
        )

        // Owner, 2026-10-17: "only level up once the day ends".
        viewModel.applyPendingLevelUps()

        assertEquals(
            "the day ended, so the held level-up lands",
            before.level + 1,
            viewModel.currentProgressionState(exercise)!!.level
        )
    }

    @Test
    fun aLockedExerciseDoesNotLevelUpAtDayEnd() {
        seedDay(exercise)
        viewModel.setExerciseLocked(exercise, true)
        settle("the pin must be written") { viewModel.isExerciseLockedNow(exercise) }
        val before = viewModel.currentProgressionState(exercise)!!

        viewModel.holdLevelUpForTest(
            exercise,
            ProgressionEngine.ProgressionState(before.level + 1, 1)
        )
        viewModel.applyPendingLevelUps()

        assertEquals(
            "a pinned exercise is held where it is, at day end like everywhere else",
            before,
            viewModel.currentProgressionState(exercise)
        )
    }

    @Test
    fun theLevelSystemBeingOffHoldsEverything() {
        seedDay(exercise)
        val before = viewModel.currentProgressionState(exercise)!!
        viewModel.holdLevelUpForTest(
            exercise,
            ProgressionEngine.ProgressionState(before.level + 1, 1)
        )

        viewModel.setLevelSystemEnabled(false)
        viewModel.applyPendingLevelUps()

        viewModel.setLevelSystemEnabled(true)
        assertEquals(
            "with the system off nothing levels up - the owner's third condition",
            before.level,
            viewModel.currentProgressionState(exercise)!!.level
        )
    }

    /** A routine row for the day under test, so day-end has something to read. */
    private fun seedDay(name: String) {
        viewModel.saveExercise(
            RoutineExerciseEntity(
                id = 1L, dayOfWeek = "Monday", name = name, muscleGroup = "Chest",
                targetSets = 3, targetReps = "8-12", targetIntensity = "Medium",
                targetWeightKg = 0.0, restSeconds = 60, videoUrl = "", orderIndex = 0
            )
        )
        settle("$name must be on the routine") {
            viewModel.allRoutineExercises.value.any { it.name == name }
        }
    }

    /**
     * Walk to the last rung's last stage through the SELECTION paths only — the
     * thing the owner's report is about. Returns the state that leaves behind.
     */
    private fun walkToTopBySelection(): ProgressionEngine.ProgressionState {
        val entry = ProgressionEngine.entryFor(exercise)!!
        val max = viewModel.progressionMaxLevel(exercise)
        viewModel.setExerciseLevelFromEditor(exercise, max)
        repeat(ProgressionEngine.patternLength(entry) - 1) {
            viewModel.adjustExerciseStage(exercise, +1)
        }
        val state = viewModel.currentProgressionState(exercise)!!
        check(ProgressionEngine.isMastered(entry, state)) {
            "this ladder must be able to reach mastery by selection, or the offer rule is not tested"
        }
        return state
    }

    /**
     * The view model's writes land on Room's own executor, so a Robolectric run
     * has to keep the main looper moving until the condition holds. A failed
     * settle is an assertion failure, never a silent pass.
     */
    private fun settle(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(20)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(what, condition())
    }
}
