package com.grafizum.repio

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.grafizum.repio.data.AppDatabase
import com.grafizum.repio.data.RoutineExerciseEntity
import com.grafizum.repio.data.WorkoutRepository
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import java.io.File
import org.junit.After
import org.junit.Assert.assertFalse
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
 * THREE OWNER RULINGS FROM THE END OF OCTOBER 2026, all about state the app used to
 * take away on its own.
 *
 * 1. 2026-10-31 (task_0d4343336a522u55252f4v23): *"Never auto unlock an exercise, if
 *    the user locked it, it stays locked until he unlocks it"*. The editor's level
 *    writers used to clear the pin on every tap, and the editor's SAVE wrote the
 *    routine row back with `locked` at its default - a REPLACE that quietly unlocked
 *    the exercise. Both are gone; the card's own lock control is the only writer.
 * 2. 2026-10-27 (task_4u2y69342j5q6i50422r464m): *"Dont load up the app so slowly ...
 *    if the app loaded faster then load in faster but also not instantly minimum
 *    loading time is 0.2s"*. The splash was a fixed 2.1 s script; it is a gate on the
 *    app's own readiness with a 0.2 s floor.
 * 3. 2026-10-31 (task_5d5q2t5k0z3n3g1v3714160r): *"When an exercise is being added
 *    manually and has been done already once (the level is know) then dont ask which
 *    level but simply added based on precious level"*.
 *
 * The composable side is guarded at the SOURCE, the way this project already guards
 * drawing code it cannot lay out in a unit test - the rule is the shape of the call,
 * and a broken shape is what would have caught all three of these.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LockStartupAndKnownLevelTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: WorkoutViewModel

    private val exercise = "Push-Up"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = WorkoutViewModel(WorkoutRepository(db.workoutDao()))
        viewModel.setLevelSystemEnabled(true)
        // NOTE: the routine row is seeded per test, not here. The source-guard cases
        // below do not touch the database at all, and a seed in @Before made them
        // depend on a Room write they have no business waiting for.
    }

    @After
    fun tearDown() = db.close()

    /** A routine row, so the per-name lock UPDATE has something to flip. */
    private fun seedRow() {
        viewModel.saveExercise(
            RoutineExerciseEntity(
                id = 1L, dayOfWeek = "Monday", name = exercise, muscleGroup = "Chest",
                targetSets = 3, targetReps = "8-12", targetIntensity = "Medium",
                targetWeightKg = 0.0, restSeconds = 60, videoUrl = "", orderIndex = 0
            )
        )
        settle("$exercise must be on the routine") {
            viewModel.allRoutineExercises.value.any { it.name == exercise }
        }
    }

    @Test
    fun anExerciseStaysLockedThroughAnEditorLevelPick() {
        seedRow()
        viewModel.setExerciseLocked(exercise, true)
        settle("the pin must be written") { viewModel.isExerciseLockedNow(exercise) }

        viewModel.setExerciseLevelFromEditor(exercise, 3)

        settle("the level pick must still land") {
            viewModel.currentProgressionState(exercise)?.level == 3
        }
        assertTrue(
            "a level picked in the editor must not unlock the exercise (owner, 2026-10-31)",
            viewModel.isExerciseLockedNow(exercise),
        )
    }

    @Test
    fun anExerciseStaysLockedThroughAnEditorStagePick() {
        seedRow()
        viewModel.setExerciseLocked(exercise, true)
        settle("the pin must be written") { viewModel.isExerciseLockedNow(exercise) }

        viewModel.setExerciseStateFromEditor(exercise, 2, 1)

        settle("the stage pick must still land") {
            viewModel.currentProgressionState(exercise)?.level == 2
        }
        assertTrue(
            "a Roman pick must not unlock the exercise either",
            viewModel.isExerciseLockedNow(exercise),
        )
    }

    @Test
    fun startupReadyFlipsOnceThePlanIsOnScreen() {
        settle("the app must report itself ready", 10_000) { viewModel.startupReady.value }
        assertTrue(
            "a ready app has a routine to show",
            viewModel.allRoutineExercises.value.isNotEmpty(),
        )
    }

    @Test
    fun aPickedLevelIsKnownAndAFreshExerciseIsNot() {
        assertNull(
            "nothing is known before the user picks",
            viewModel.storedProgressionLevel(exercise),
        )
        viewModel.setExerciseLevelFromEditor(exercise, 4)
        settle("the pick must be stored") { viewModel.storedProgressionLevel(exercise) == 4 }
        assertNotNull(
            "a picked level is what the add form reads as KNOWN",
            viewModel.storedProgressionLevel(exercise),
        )
    }

    // --- the composable side, guarded at the source -----------------------

    private fun codeOf(relativePath: String): String =
        File("src/main/java/com/grafizum/repio/$relativePath").readText()
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
            .replace(Regex("//[^\\n]*"), "")

    @Test
    fun theSplashIsGatedOnReadinessInsteadOfAFixedScript() {
        val code = codeOf("ui/screens/SplashScreen.kt")
        assertTrue(
            "the splash must take the app's readiness",
            code.contains("fun AppSplashScreen(ready: Boolean"),
        )
        assertTrue(
            "the owner's floor is 0.2 s",
            code.contains("SPLASH_MIN_VISIBLE_MS = 200L"),
        )
        assertFalse(
            "the fixed 1.6 s hold is the bug the owner reported",
            code.contains("delay(1600"),
        )
        assertTrue(
            "the host must feed it the view model's readiness",
            codeOf("MainActivity.kt").contains("AppSplashScreen(ready = startupReady"),
        )
    }

    @Test
    fun theEditorSaveKeepsTheRowsLock() {
        assertTrue(
            "the editor's REPLACE must carry the row's own lock, or saving unlocks it",
            codeOf("ui/components/WorkoutComponents.kt")
                .contains("locked = exercise?.locked ?: false"),
        )
        // And the writers no longer clear it behind the user's back.
        val vm = codeOf("ui/viewmodel/WorkoutViewModel.kt")
        assertFalse(
            "setExerciseLevelFromEditor must not unlock the exercise",
            vm.contains("applyExerciseLevel(exerciseName, level)\n        setExerciseLocked(exerciseName, false)"),
        )
    }

    @Test
    fun theAddFormsSkipTheLevelQuestionWhenItIsKnown() {
        val editors = listOf("ui/components/WorkoutComponents.kt", "ui/screens/FirstTimeSetupScreen.kt")
        for (path in editors) {
            assertTrue(
                "$path must not ask for a level it already knows",
                codeOf(path).contains("!(pickerMode && levelData.levelKnown)"),
            )
        }
        assertTrue(
            "the level data must answer whether the user ever picked one",
            codeOf("ui/screens/WorkoutNowScreen.kt")
                .contains("levelKnown = viewModel.storedProgressionLevel(exerciseName) != null"),
        )
    }

    /** Room's writes land on its own executor, so a Robolectric run has to pump the looper. */
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
