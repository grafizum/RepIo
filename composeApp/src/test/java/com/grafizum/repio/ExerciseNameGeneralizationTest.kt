package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseLocalization
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An exercise's NAME says what it is, and an apparatus the pool treats as a
 * real difference is visible in that name.
 *
 * This test previously asserted the OPPOSITE — that no name may lead with an
 * apparatus word — on the reasoning that `equipment` already records it. That
 * reasoning was wrong in use: "Bent Over Row" does not tell a user whether it is
 * a cable row, a smith-machine row or a barbell row, and those are three
 * different exercises with three different ladders, three different equipment
 * gates and three different substitution rules. Stripping the apparatus made a
 * machine movement and the same movement with other kit indistinguishable.
 *
 * Owner rule (2026-10-01): the apparatus is a "(Qualifier)" at the end —
 * "Bent Over Row (Smith Machine)", "Rear Delt Row (Barbell)". A name of three
 * words or fewer keeps the apparatus where it already was, because splitting it
 * reads worse, not better: "Dumbbell Row" stays "Dumbbell Row", never
 * "Row (Dumbbell)".
 */
class ExerciseNameGeneralizationTest {

    /** The longest name that keeps its apparatus in front rather than in brackets. */
    private val shortNameMaxWords = 3

    private val apparatusWords = listOf(
        "Smith Machine", "Machine", "Barbell", "Dumbbell", "Kettlebell", "Cable",
        "Banded", "EZ-Bar", "Trap Bar", "Stability Ball", "TRX", "Band",
    )

    /**
     * The rule itself: a long apparatus-prefixed name carries a trailing
     * "(Qualifier)"; a short one is left exactly as it is.
     */
    @Test
    fun longNamesCarryTheirApparatusAsATrailingQualifier() {
        val offenders = WorkoutTemplates.getAllExerciseNames().filter { name ->
            val words = name.split(" ").size
            if (words <= shortNameMaxWords) return@filter false
            // Leading apparatus AND no trailing qualifier is the bad shape.
            apparatusWords.any { name.startsWith("$it ") } && !name.endsWith(")")
        }
        assertEquals(
            "a long apparatus-prefixed name must end in an '(Apparatus)' qualifier",
            emptyList<String>(),
            offenders
        )
    }

    @Test
    fun shortNamesKeepTheirApparatusInPlace() {
        // The rule the owner asked for by example: these read naturally and
        // splitting them would be silly.
        val short = WorkoutTemplates.getAllExerciseNames().filter { name ->
            name.split(" ").size <= shortNameMaxWords &&
                apparatusWords.any { name.startsWith("$it ") }
        }
        assertTrue("expected short apparatus-leading names in the pool", short.isNotEmpty())
        // Every one of them must still be reachable under its OWN name: a short
        // name that had been rewritten into a qualifier is the exact failure the
        // rule is protecting against.
        val missing = short.filter { ExerciseCatalog.entryFor(it) == null }
        assertEquals("short names must keep their own name", emptyList<String>(), missing)
    }

    @Test
    fun aMachineMovementAndTheSameMovementWithOtherKitAreDifferentExercises() {
        // The property the rename exists to provide. These are three rows, each
        // with its own equipment, and none of them may resolve to another.
        val cable = ExerciseCatalog.entryFor("Cable Bent-Over Row")
        val smith = ExerciseCatalog.entryFor("Bent Over Row (Smith Machine)")
        val barbell = ExerciseCatalog.entryFor("Bent-Over Barbell Row")
        assertNotNull("the cable row must exist", cable)
        assertNotNull("the smith-machine row must exist", smith)
        assertNotNull("the barbell row must exist", barbell)
        assertEquals("Machine", smith?.equipment)
        assertEquals("Barbell", barbell?.equipment)
        // Distinct names, and none resolves through the rename map into another.
        val names = listOfNotNull(cable?.name, smith?.name, barbell?.name).toSet()
        assertEquals("the three bent-over rows must be three distinct exercises", 3, names.size)
    }

    @Test
    fun thePoolIsStillASetOfDistinctExercises() {
        val names = WorkoutTemplates.getAllExerciseNames()
        assertEquals(
            "two exercises share a name, which merges them",
            names.size,
            names.toSet().size
        )
    }

    @Test
    fun apparatusStaysInTheEquipmentField() {
        // The qualifier in the name must agree with the row's own equipment —
        // the name is for the user, the field is what the gate reads.
        val smith = ExerciseCatalog.entryFor("Bent Over Row (Smith Machine)")
        assertNotNull(smith)
        assertEquals("Machine", smith?.equipment)
        val squat = ExerciseCatalog.entryFor("Barbell Back Squat")
        assertNotNull("the barbell squat must exist under its new name", squat)
        assertEquals("Barbell", squat?.equipment)
    }

    @Test
    fun aNameFromBeforeTheRenameStillResolves() {
        val old = ExerciseCatalog.RENAMED
        assertTrue("the rename map should not be empty", old.isNotEmpty())
        old.forEach { (before, after) ->
            assertEquals(
                "'$before' must resolve to '$after' so logged routines keep their exercise",
                after,
                ExerciseLocalization.canonicalFor(before)
            )
        }
    }

    @Test
    fun theOldNameStillFindsTheExerciseInSearch() {
        // A user who knows an exercise by its pre-rename name must still find it.
        val hits = com.grafizum.repio.data.ExerciseSearch.search(
            WorkoutTemplates.getAllExerciseNames(), "Bent Over Row"
        )
        assertTrue("searching the movement must reach the apparatus variants", hits.isNotEmpty())
        assertTrue(
            "the smith-machine row must be reachable by its movement name",
            hits.any { it.contains("Smith Machine") }
        )
    }
}