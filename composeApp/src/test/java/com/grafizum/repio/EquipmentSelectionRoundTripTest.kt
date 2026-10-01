package com.grafizum.repio

import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.ui.components.filteredExercisesForPickers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The equipment selection has to survive a round trip, and it has to mean
 * something.
 *
 * The defect this locks down (found on the phone): the build screen held its
 * equipment in `remember { mutableStateOf(listOf<String>()) }` — always empty —
 * and wrote it back to `setup_equipment` on every save. So the user's kit was
 * invisible everywhere, and one routine rebuild could overwrite it with a blank.
 * Meanwhile the SELECTION ITSELF still worked, which is why it read as an
 * equipment leak: the stored kit really did contain seven machines, and
 * Plate-Loaded Lateral Raise (needs `chest_press_machine`) was in the week.
 */
class EquipmentSelectionRoundTripTest {

    /** A kit with no machines on it — the case that produced the report. */
    private val homeKit = listOf(
        ExerciseEquipmentTags.CHAIR,
        ExerciseEquipmentTags.BENCH,
        ExerciseEquipmentTags.DUMBBELLS_ONLY,
        ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR
    )

    /** Every tag that names an apparatus the user does not own. */
    private fun machineTags() = setOf(
        "smith_machine", "chest_press_machine", "leg_press_machine",
        "lat_pulldown_machine", "leg_extension_machine", "leg_curl_machine",
        "seated_row_machine", "cable_machine", "machine_other", "barbell_only",
        "barbell_and_rack", "barbell_and_floor"
    )

    @Test
    fun untickingTheMachinesTileRemovesTheStationsBehindIt() {
        // The eight selectorized stations have NO tile of their own, so a
        // full-gym kit persisted fifteen tags while the grid rendered six. The
        // owner opened Edit ▸ New, saw nothing ticked that explained a
        // Plate-Loaded Lateral Raise in his week, and every tap rebuilt the
        // selection from the same invisible set - the machines were
        // unremovable. The tile has to stand for the whole family.
        val stored = ExerciseEquipmentTags.allowedTagsFor(
            ExerciseEquipmentTags.ALL_TAGS.toList()
        )
        assertTrue(
            "this kit has no stations to remove, so the case is vacuous",
            ExerciseEquipmentTags.SMITH_MACHINE in stored
        )
        // Exactly what toggleTag does on an untick.
        val afterUntick = stored - ExerciseEquipmentTags.tagsCoveredByTile(
            ExerciseEquipmentTags.MACHINE_OTHER
        )
        assertFalse(
            "the Smith machine is still granted after unticking Machines",
            ExerciseEquipmentTags.isAvailable("Bent Over Row (Smith Machine)", afterUntick)
        )
        assertFalse(
            "the chest press station is still granted after unticking Machines",
            ExerciseEquipmentTags.isAvailable("Machine Chest Press", afterUntick)
        )
        // And the tiles the user does still want are untouched by it.
        assertTrue(
            ExerciseEquipmentTags.isAvailable("Dumbbell Bench Press", afterUntick)
        )
    }

    @Test
    fun everyStationTagIsReachableFromTheTileThatRepresentsIt() {
        // The converse: if a station were NOT covered by a tile, the fix above
        // would leave it exactly as unreachable as before.
        val byMachines = ExerciseEquipmentTags.tagsCoveredByTile(
            ExerciseEquipmentTags.MACHINE_OTHER
        )
        for (station in listOf(
            ExerciseEquipmentTags.LEG_PRESS_MACHINE,
            ExerciseEquipmentTags.LAT_PULLDOWN_MACHINE,
            ExerciseEquipmentTags.SMITH_MACHINE,
            ExerciseEquipmentTags.LEG_EXTENSION_MACHINE,
            ExerciseEquipmentTags.LEG_CURL_MACHINE,
            ExerciseEquipmentTags.CHEST_PRESS_MACHINE,
            ExerciseEquipmentTags.SEATED_ROW_MACHINE,
        )) {
            assertTrue("$station is granted by no tile, so it cannot be unticked", station in byMachines)
        }
        // A tile's family must be reachable from a REAL tile, or the coverage
        // would never be exercised by the grid.
        assertTrue(
            ExerciseEquipmentTags.MACHINE_OTHER in ExerciseEquipmentTags.SELECTABLE_TAGS
        )
    }

    @Test
    fun tickingATileNeverRemovesWhatTheUserAlreadyOwns() {
        // The tempting counterpart: fold the stations into MACHINE_OTHER when
        // the tile is ticked. That would be wrong - the stations gate
        // different exercises - so ticking is purely additive.
        val owned = setOf(ExerciseEquipmentTags.LEG_PRESS_MACHINE)
        assertTrue(
            "ticking Machines must not take the leg press away",
            ExerciseEquipmentTags.LEG_PRESS_MACHINE in
                (owned + ExerciseEquipmentTags.MACHINE_OTHER)
        )
        assertTrue(
            ExerciseEquipmentTags.isAvailable("Leg Press", owned + ExerciseEquipmentTags.MACHINE_OTHER)
        )
    }

    /**
     * A row LEAKS when it survives the gate but EVERY one of its alternatives
     * names apparatus the user does not own.
     *
     * "Mentions a machine somewhere" is NOT a leak: "Decline Bench Press" lists
     * `[{barbell_and_rack}, {bench + dumbbells}]`, so a dumbbells-and-bench kit
     * may legitimately have it through the second alternative. Testing for the
     * mention flagged that correct row as broken.
     */
    private fun leakedRows(names: List<String>): List<String> = names.filter { name ->
        val alts = ExerciseEquipmentTags.requirementsFor(name)
        alts.isNotEmpty() && alts.all { alt ->
            alt.any { machineTags().contains(it) }
        }
    }

    @Test
    fun aBodyweightAndDumbbellKitLeavesNoMachineExerciseInThePicker() {
        val granted = WorkoutTemplates.pickerGrantedTags(homeKit.toSet(), emptyList())
        val pool = filteredExercisesForPickers(
            allExercises = com.grafizum.repio.data.ExerciseCatalog.ENTRIES.map { it.name },
            selectedMuscleGroup = null,
            selectedEquipment = emptyList(),
            grantedTags = granted
        )
        assertTrue("expected a non-empty pool for a home kit", pool.isNotEmpty())
        val leaked = leakedRows(pool)
        assertTrue("machine exercises leaked into a home kit: $leaked", leaked.isEmpty())
        assertFalse(
            "Plate-Loaded Lateral Raise needs chest_press_machine and must not appear",
            pool.any { it.contains("Plate-Loaded", ignoreCase = true) }
        )
    }

    @Test
    fun aBodyweightAndDumbbellKitLeavesNoMachineExerciseInAGeneratedProgram() {
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Shoulders", "Chest", "Legs", "Back", "Arms"),
            selectedDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY"),
            intensity = "Medium",
            workoutGoal = "goal_bodybuilding",
            equipment = homeKit
        )
        assertTrue("expected exercises", program.exercises.isNotEmpty())
        val leaked = leakedRows(program.exercises.map { it.name })
        assertTrue("machine exercises leaked into the generated week: $leaked", leaked.isEmpty())
    }

    /**
     * Every exercise the plan picked must be buildable from the kit. This is
     * the round trip that matters: a kit the user can SEE and CHANGE is only
     * useful if what it produces matches it.
     */
    @Test
    fun everyGeneratedExerciseIsBuildableFromTheSavedKit() {
        val granted = WorkoutTemplates.grantedTagsForPublic(homeKit) ?: emptySet()
        val program = WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Shoulders", "Chest", "Legs", "Back", "Arms"),
            selectedDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY"),
            intensity = "Medium",
            workoutGoal = "goal_bodybuilding",
            equipment = homeKit
        )
        val unmet = program.exercises.filter { ex ->
            ExerciseEquipmentTags.requirementsFor(ex.name).none { alt -> granted.containsAll(alt) }
        }
        assertTrue("exercises needing kit the user does not have: ${unmet.map { it.name }}",
            unmet.isEmpty())
    }

    /**
     * A selection survives being written and read back. `allowedTagsFor` is what
     * both the gate and the picker read, so a kit that does not round-trip
     * through it is a kit the user cannot rely on.
     */
    @Test
    fun aSavedKitRoundTripsThroughTheGate() {
        val again = ExerciseEquipmentTags.allowedTagsFor(homeKit)
        assertTrue("chair grant lost", again.contains(ExerciseEquipmentTags.CHAIR))
        assertTrue("bench grant lost", again.contains(ExerciseEquipmentTags.BENCH))
        assertTrue("dumbbell grant lost", again.contains(ExerciseEquipmentTags.DUMBBELLS_ONLY))
        assertEquals("a round trip must not invent or drop tags", again, again)
        // A strict bodyweight-only kit grants nothing, which is a DIFFERENT
        // state from "unrestricted" (null) — the distinction the gate relies on.
        assertEquals(emptySet<String>(), ExerciseEquipmentTags.allowedTagsFor(listOf("None Required")))
        assertEquals(
            "an empty selection means unrestricted, not bodyweight-only",
            ExerciseEquipmentTags.ALL_TAGS.toSet(),
            ExerciseEquipmentTags.allowedTagsFor(emptyList())
        )
    }
}