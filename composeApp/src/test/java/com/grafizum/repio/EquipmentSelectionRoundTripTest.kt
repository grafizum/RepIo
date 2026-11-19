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

    @Test
    fun tickingTheMachinesTileGrantsTheStationsItStandsFor() {
        // The tile is called "Machines", and its grant used to be `machine_other`
        // alone. That made the tile a lie in the OTHER direction from the
        // invisibility above: a user who ticked it got the battle ropes and the
        // ab wheel and could reach NONE of the ~45 rows the eight stations gate,
        // with no way to say "I have a leg press" short of the Full-gym preset.
        val afterTick = ExerciseEquipmentTags.tagsGrantedByTicking(
            ExerciseEquipmentTags.MACHINE_OTHER
        )
        val granted = ExerciseEquipmentTags.allowedTagsFor(
            listOf(ExerciseEquipmentTags.PRESET_CUSTOM) + afterTick.toList()
        )
        assertTrue(ExerciseEquipmentTags.isAvailable("Leg Press", granted))
        assertTrue(ExerciseEquipmentTags.isAvailable("Leg Extension", granted))
        assertTrue(ExerciseEquipmentTags.isAvailable("Machine Chest Press", granted))
        // ("Lat Pulldown" itself is a CABLE row; this is the machine one.)
        assertTrue(ExerciseEquipmentTags.isAvailable("V-Bar Lat Pulldown", granted))
        // Ticking is still PURELY ADDITIVE - no station is dropped - and the kit
        // still cannot reach a barbell it did not declare.
        assertTrue(ExerciseEquipmentTags.MACHINE_OTHER in granted)
        assertFalse(ExerciseEquipmentTags.isAvailable("Barbell Back Squat", granted))
    }

    @Test
    fun tickThenUntickTheMachinesTileIsANoOp() {
        // Granting the whole family is only safe because tick and untick stand
        // for the SAME set: an asymmetric pair would let one tap add and the next
        // silently remove machinery the user does own.
        val machines = ExerciseEquipmentTags.MACHINE_OTHER
        val startedAs = listOf(
            ExerciseEquipmentTags.PRESET_CUSTOM,
            ExerciseEquipmentTags.DUMBBELLS_ONLY,
        )
        val before = ExerciseEquipmentTags.allowedTagsFor(startedAs)
        val ticked = ExerciseEquipmentTags.allowedTagsFor(
            startedAs + ExerciseEquipmentTags.tagsGrantedByTicking(machines)
        )
        val unticked = ExerciseEquipmentTags.allowedTagsFor(
            startedAs + (ticked - ExerciseEquipmentTags.tagsCoveredByTile(machines))
        )
        assertEquals("tick then untick must return the original kit", before, unticked)
        assertTrue("the tick must add the stations", ExerciseEquipmentTags.isAvailable("Leg Press", ticked))
        assertFalse("the untick must take them away", ExerciseEquipmentTags.isAvailable("Leg Press", unticked))
        assertTrue(
            "the untick must not touch the kit the user did declare",
            ExerciseEquipmentTags.isAvailable("Dumbbell Bicep Curl", unticked),
        )
    }

    /**
     * A row LEAKS when it survives the gate but EVERY one of its alternatives
     * names apparatus the user does not own.
     *
     * "Mentions a machine somewhere" is NOT a leak: a row may list several
     * alternatives and a kit needs only ONE of them, so "Decline Push-Up" lists
     * `[{chair}, {bench}]` and a user with a chair legitimately has it. Testing
     * for the mention flagged such correct rows as broken.
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

    /**
     * The rack and the barbell are two tiles, and the user must be able to tick
     * either one alone.
     *
     * OWNER REPORT, 2026-10-08: "the squat rack and barbell equipment icons are
     * turning on and off together so the user cant select one but not the other".
     * The tiles were nested at the time (Rack covered all three barbell tags),
     * and `isChecked` reads a tile as ticked when ANY of its tags is granted - so
     * ticking Barbell lit Rack up even though Rack's own tag was absent. The pair
     * is now two independent switches, and this walks the screen's own logic
     * (`tagsGrantedByTicking` on a tick, `tagsCoveredByTile` on an untick, both
     * read back through the tile's coverage) rather than asserting the sets
     * directly, because the SET is not the bug - the read-back is.
     */
    @Test
    fun theRackAndBarbellTilesAreTwoIndependentSwitches() {
        val rack = ExerciseEquipmentTags.BARBELL_AND_RACK
        val barbell = ExerciseEquipmentTags.BARBELL_AND_FLOOR
        assertTrue("the rack tile must be a real tile", rack in ExerciseEquipmentTags.GYM_EQUIPMENT_TAGS)
        assertTrue("the barbell tile must be a real tile", barbell in ExerciseEquipmentTags.GYM_EQUIPMENT_TAGS)

        // Exactly what EquipmentTileSection does with the granted tags.
        fun isChecked(granted: Set<String>, tile: String) =
            granted.any { it in ExerciseEquipmentTags.tagsCoveredByTile(tile) }
        fun tick(granted: Set<String>, tile: String) =
            granted + ExerciseEquipmentTags.tagsGrantedByTicking(tile)
        fun untick(granted: Set<String>, tile: String) =
            granted - ExerciseEquipmentTags.tagsCoveredByTile(tile)

        val onlyRack = tick(emptySet(), rack)
        val onlyBarbell = tick(emptySet(), barbell)
        assertTrue("ticking the rack must tick the rack", isChecked(onlyRack, rack))
        assertFalse(
            "ticking the rack must NOT tick the barbell tile beside it",
            isChecked(onlyRack, barbell)
        )
        assertTrue("ticking the barbell must tick the barbell", isChecked(onlyBarbell, barbell))
        assertFalse(
            "ticking the barbell must NOT tick the rack tile beside it",
            isChecked(onlyBarbell, rack)
        )

        // A user who owns both ticks both, and unticking one leaves the other
        // alone - the half of the complaint that a tick-only guard would miss.
        val both = tick(tick(emptySet(), rack), barbell)
        assertTrue(isChecked(both, rack) && isChecked(both, barbell))
        assertFalse("unticking the rack removed nothing", isChecked(untick(both, rack), rack))
        assertTrue(
            "unticking the rack must not take the barbell with it",
            isChecked(untick(both, rack), barbell)
        )
        assertTrue(
            "unticking the barbell must not take the rack with it",
            isChecked(untick(both, barbell), rack)
        )

        // And nothing became unreachable by splitting them: the barbell tile
        // still grants `barbell_only` (the tag the barbell rows that need
        // neither rack nor floor gate on), and the rack tile does not need to.
        assertTrue(
            "a barbell with no rack must still grant barbell_only, or rows like the " +
                "barbell curl and the strict press are unreachable without a rack",
            ExerciseEquipmentTags.BARBELL_ONLY in onlyBarbell
        )
        assertTrue(
            "the rack tag is granted by ticking the rack",
            rack in onlyRack
        )
    }
}