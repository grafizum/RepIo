package com.grafizum.repio

import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.ui.components.equipmentTagShortLabelKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE MACHINES GROUP: one tick per machine, cables folded in.
 *
 * Owner, 2026-10-16: *"per-machine ticks under the Machine group, cables folded in"*.
 *
 * The Gym grid drew ONE tile - "Gym machines (press, leg press, pulldown…)" - whose tick granted
 * all eight stations plus the catch-all, so a user with a leg press and nothing else could either
 * claim every machine in the building or claim no machine at all. The tags were never the problem;
 * the CONTROL was. These cases hold the shape the fix put in place:
 *
 *  * the group is COMPLETE (no machine can only be reached by a preset) and DISJOINT (no machine is
 *    drawn twice, once here and once as a tile above it),
 *  * a tick is exactly one tag in BOTH directions, which is the half a family grant cannot express,
 *  * and the cable stack - which is a machine - is asked here, not in a second group beside it.
 *
 * A pure case is the right shape for all of this: the group is a list and a set operation, both of
 * which are data, and the composable that draws the tiles is a thin loop over them.
 */
class MachineSubSelectionTest {

    /**
     * Every machine the data can gate on has its own tick, and the cable station is one of them.
     *
     * The family is read from [ExerciseEquipmentTags.tagsCoveredByTile] rather than typed out here,
     * so a machine added to the data later fails this case instead of quietly joining the ones no
     * tile asks about. The cable stack is UNIONed in because it is not inside that family: it was a
     * tile of its own in the Gym section, and "cables folded in" is the move this case measures.
     */
    @Test
    fun everyMachineHasItsOwnTick() {
        val family = ExerciseEquipmentTags.tagsCoveredByTile(ExerciseEquipmentTags.MACHINE_OTHER)
        assertEquals(
            "the machine ticks must be the machine family plus the folded-in cable stack",
            family + ExerciseEquipmentTags.CABLE_MACHINE,
            ExerciseEquipmentTags.MACHINE_EQUIPMENT_TAGS.toSet(),
        )
        assertTrue(
            "the cable stack is a machine, so its tick belongs in this group",
            ExerciseEquipmentTags.CABLE_MACHINE in ExerciseEquipmentTags.MACHINE_EQUIPMENT_TAGS,
        )
        val grid = ExerciseEquipmentTags.HOME_EQUIPMENT_TAGS +
            ExerciseEquipmentTags.GYM_EQUIPMENT_TAGS +
            ExerciseEquipmentTags.MACHINE_EQUIPMENT_TAGS
        assertEquals(
            "a tag drawn in two groups is the same control twice",
            grid.size,
            grid.toSet().size,
        )
        assertEquals(
            "the grid must offer every tag it knows about",
            ExerciseEquipmentTags.SELECTABLE_TAGS,
            grid,
        )
    }

    /** The old family tiles are gone from the Gym section, so nothing is asked twice. */
    @Test
    fun theMachinesTileIsGoneFromTheGymTiles() {
        for (tag in listOf(ExerciseEquipmentTags.MACHINE_OTHER, ExerciseEquipmentTags.CABLE_MACHINE)) {
            assertFalse(
                "$tag still has a Gym tile as well as its own tick",
                tag in ExerciseEquipmentTags.GYM_EQUIPMENT_TAGS,
            )
            assertTrue(
                "$tag is a tag the grid can still render",
                tag in ExerciseEquipmentTags.SELECTABLE_TAGS,
            )
        }
    }

    /** Every tick can be read: its label names the machine and no two ticks share one. */
    @Test
    fun everyMachineTickHasItsOwnName() {
        val labels = ExerciseEquipmentTags.MACHINE_EQUIPMENT_TAGS.map { equipmentTagShortLabelKey(it) }
        assertFalse(
            "a machine tick renders the generic caption",
            labels.any { it == "equipment" },
        )
        assertEquals("two machine ticks share one caption", labels.size, labels.toSet().size)
    }

    /**
     * Ticking one machine grants THAT machine - the leg press opens the leg press rows and leaves
     * the Smith-gated ones where they were. This is the user the group exists for: a gym leg press
     * and no Smith machine.
     */
    @Test
    fun tickingOneMachineGrantsThatMachineAndNothingBesideIt() {
        val selected = ExerciseEquipmentTags.applyMachineTick(
            listOf(ExerciseEquipmentTags.PRESET_CUSTOM),
            ExerciseEquipmentTags.LEG_PRESS_MACHINE,
        )
        val granted = ExerciseEquipmentTags.allowedTagsFor(selected)
        assertEquals(
            "one tick must grant exactly the machine it names",
            setOf(ExerciseEquipmentTags.LEG_PRESS_MACHINE),
            granted,
        )
        assertTrue(
            "the leg press the user ticked must be able to do leg press rows",
            ExerciseEquipmentTags.isAvailable("leg press", granted),
        )
        assertFalse(
            "the Smith machine was never ticked and must not be assumed",
            ExerciseEquipmentTags.isAvailable("bench press (smith machine)", granted),
        )
    }

    /** ...and unticking one takes that machine, not the eight the user still owns. */
    @Test
    fun untickingOneMachineLeavesTheRestOfTheKitAlone() {
        val selected = ExerciseEquipmentTags.applyMachineTick(
            listOf(ExerciseEquipmentTags.PRESET_FULL_GYM),
            ExerciseEquipmentTags.LEG_PRESS_MACHINE,
        )
        val granted = ExerciseEquipmentTags.allowedTagsFor(selected)
        assertFalse(
            "the unticked machine must be gone",
            ExerciseEquipmentTags.LEG_PRESS_MACHINE in granted,
        )
        for (kept in listOf(
            ExerciseEquipmentTags.CABLE_MACHINE,
            ExerciseEquipmentTags.SMITH_MACHINE,
            ExerciseEquipmentTags.SEATED_ROW_MACHINE,
            ExerciseEquipmentTags.MACHINE_OTHER,
        )) {
            assertTrue(
                "unticking one machine took $kept with it",
                kept in granted,
            )
        }
    }

    /**
     * The tick that was never there still HAPPENS when the machine is already owned - the state
     * model has to survive a preset, or a full-gym kit could not drop a single machine.
     */
    @Test
    fun aTickOnAPresetSwitchesThatKitToCustom() {
        val selected = ExerciseEquipmentTags.applyMachineTick(
            listOf(ExerciseEquipmentTags.PRESET_FULL_GYM),
            ExerciseEquipmentTags.CABLE_MACHINE,
        )
        assertEquals(
            "an edit on top of a preset has to stop claiming to BE that preset",
            ExerciseEquipmentTags.PRESET_CUSTOM,
            selected.first(),
        )
    }
}
