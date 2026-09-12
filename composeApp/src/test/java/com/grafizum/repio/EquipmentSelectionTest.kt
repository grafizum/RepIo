package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the equipment chips: "All" is the empty selection (no restriction), and it
 * can never end up combined with a specific type. Picking one type while All is on
 * must narrow the build to that type alone.
 */
class EquipmentSelectionTest {

    private val all = WorkoutTemplates.allEquipmentCategories
    private val bodyweight = WorkoutTemplates.EQUIPMENT_BODYWEIGHT
    private val dumbbell = WorkoutTemplates.EQUIPMENT_DUMBBELL
    private val barbell = WorkoutTemplates.EQUIPMENT_BARBELL
    private val gym = WorkoutTemplates.EQUIPMENT_GYM

    private fun tap(selected: List<String>, tapped: String) =
        WorkoutTemplates.toggleEquipmentSelection(selected, tapped, all)

    @Test
    fun noRestrictionMeansAllIsTheEmptySelection() {
        assertTrue(WorkoutTemplates.EQUIPMENT_ALL !in all)
        assertEquals(emptyList<String>(), tap(emptyList(), WorkoutTemplates.EQUIPMENT_ALL))
        assertEquals(emptyList<String>(), tap(listOf(dumbbell, gym), WorkoutTemplates.EQUIPMENT_ALL))
    }

    @Test
    fun tappingATypeWhileAllIsOnSelectsOnlyThatType() {
        assertEquals(listOf(dumbbell), tap(emptyList(), dumbbell))
        assertEquals(listOf(barbell), tap(emptyList(), barbell))
    }

    @Test
    fun tappingMoreTypesAddsThem() {
        val two = tap(listOf(dumbbell), barbell)
        assertEquals(listOf(dumbbell, barbell), two)
        assertEquals(listOf(dumbbell, barbell, gym), tap(two, gym))
    }

    @Test
    fun tappingASelectedTypeRemovesIt() {
        assertEquals(listOf(gym), tap(listOf(dumbbell, gym), dumbbell))
    }

    @Test
    fun clearingTheLastTypeFallsBackToAll() {
        // Empty means "everything allowed", which is exactly the All chip.
        assertEquals(emptyList<String>(), tap(listOf(bodyweight), bodyweight))
    }

    @Test
    fun tickingEveryTypeCollapsesToAll() {
        // Four specific chips selected is the same restriction as none, so it must not
        // linger as a list — otherwise All and the types would look selected at once.
        val allFour = tap(tap(tap(tap(emptyList(), bodyweight), dumbbell), barbell), gym)
        assertEquals(emptyList<String>(), allFour)
    }

    @Test
    fun aTypeIsNeverCombinedWithAll() {
        // Whatever the sequence, the result is either empty (All) or specific types only.
        var state: List<String> = emptyList()
        listOf(bodyweight, dumbbell, WorkoutTemplates.EQUIPMENT_ALL, gym, barbell, dumbbell, barbell, gym, bodyweight)
            .forEach { state = tap(state, it) }
        assertTrue("'All' must never sit in the list alongside a type", WorkoutTemplates.EQUIPMENT_ALL !in state)
        assertTrue(state.distinct().size == state.size)
    }
}
