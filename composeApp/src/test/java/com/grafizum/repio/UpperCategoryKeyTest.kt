package com.grafizum.repio

import com.grafizum.repio.ui.components.getMuscleGroupColor
import com.grafizum.repio.ui.components.getUpperCategoryKey
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the display mapping from a stored muscle group to its broad body-part
 * key and colour. The precise "Pectorals" group (what the primary-muscle table
 * stores and what a generated row now saves) must land on the same broad key
 * and colour as "Chest", or a chest day reads as "Other" and a chest dot turns
 * grey.
 */
class UpperCategoryKeyTest {

    @Test
    fun pectoralsMapsToTheChestCategory() {
        assertEquals("chest", getUpperCategoryKey("Pectorals"))
        assertEquals("chest", getUpperCategoryKey("pectorals"))
        assertEquals("chest", getUpperCategoryKey("Chest"))
    }

    @Test
    fun pectoralsSharesTheChestColor() {
        assertEquals(getMuscleGroupColor("Chest"), getMuscleGroupColor("Pectorals"))
    }

    @Test
    fun theOtherPreciseGroupsKeepTheirBroadKeys() {
        assertEquals("back", getUpperCategoryKey("Lats"))
        assertEquals("back", getUpperCategoryKey("Upper Traps"))
        assertEquals("legs", getUpperCategoryKey("Quadriceps"))
        assertEquals("legs", getUpperCategoryKey("Hip Abductors"))
        assertEquals("shoulders", getUpperCategoryKey("Front Deltoids"))
        assertEquals("shoulders", getUpperCategoryKey("Rear Deltoids"))
        assertEquals("arms", getUpperCategoryKey("Biceps"))
        assertEquals("arms", getUpperCategoryKey("Forearms"))
        assertEquals("core", getUpperCategoryKey("Abs"))
        assertEquals("core", getUpperCategoryKey("Obliques"))
    }
}
