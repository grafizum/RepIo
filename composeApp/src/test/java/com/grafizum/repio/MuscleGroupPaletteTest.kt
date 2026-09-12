package com.grafizum.repio

import androidx.compose.ui.graphics.Color
import com.grafizum.repio.ui.components.getMuscleGroupColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.math.abs

/**
 * Guards the muscle-group palette. The colours are the only thing telling body
 * parts apart on the Analytics donut, the legend and the detailed breakdown bars,
 * so each muscle group has to keep the exact colour it is drawn with.
 *
 * The palette assigns a distinct colour per muscle (its own tone for Biceps,
 * Triceps, Forearms and the delt heads, Amber for Abs and Yellow for Core)
 * rather than one colour per broad body part.
 */
class MuscleGroupPaletteTest {

    /** Broad body parts, in the order they appear across the app. */
    private val bodyParts = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

    /** Every muscle group and the exact colour it must resolve to. */
    private val expectedColors = mapOf(
        "Chest" to Color(0xFFEF4444),
        "Pectorals" to Color(0xFFEF4444),
        "Abs" to Color(0xFFF59E0B),
        "Abdominals" to Color(0xFFF59E0B),
        "Core" to Color(0xFFEAB308),
        "Obliques" to Color(0xFFEAB308),
        "Legs" to Color(0xFF10B981),
        "Quadriceps" to Color(0xFF10B981),
        "Hamstrings" to Color(0xFF10B981),
        "Glutes" to Color(0xFF10B981),
        "Calves" to Color(0xFF10B981),
        "Hip Adductors" to Color(0xFF10B981),
        "Hip Abductors" to Color(0xFF10B981),
        "Back" to Color(0xFF3B82F6),
        "Lats" to Color(0xFF3B82F6),
        "Upper Back" to Color(0xFF3B82F6),
        "Traps" to Color(0xFF3B82F6),
        "Upper Traps" to Color(0xFF3B82F6),
        "Spinal Erectors" to Color(0xFF3B82F6),
        "Biceps" to Color(0xFF8B5CF6),
        "Triceps" to Color(0xFF06B6D4),
        "Arms" to Color(0xFF6366F1),
        "Forearms" to Color(0xFFF97316),
        "Shoulders" to Color(0xFFEC4899),
        "Deltoids" to Color(0xFFEC4899),
        "Front Deltoids" to Color(0xFFEC4899),
        "Lateral Deltoids" to Color(0xFFEC4899),
        "Rear Deltoids" to Color(0xFFEC4899)
    )

    @Test
    fun everyMuscleGroupKeepsItsColor() {
        expectedColors.forEach { (muscle, expected) ->
            assertEquals(
                "'$muscle' must resolve to $expected",
                expected,
                getMuscleGroupColor(muscle)
            )
        }
    }

    /** Lookup is case-insensitive and tolerates padding, like before. */
    @Test
    fun lookupIsCaseAndWhitespaceInsensitive() {
        assertEquals(expectedColors.getValue("Chest"), getMuscleGroupColor(" CHEST "))
        assertEquals(expectedColors.getValue("Core"), getMuscleGroupColor("Core"))
        assertEquals(expectedColors.getValue("Core"), getMuscleGroupColor("OBLIQUES"))
    }

    @Test
    fun everyBodyPartHasItsOwnColor() {
        val colors = bodyParts.map { getMuscleGroupColor(it) }
        assertEquals(
            "two body parts share a colour: $bodyParts -> $colors",
            bodyParts.size,
            colors.toSet().size
        )
    }

    @Test
    fun coreIsYellow() {
        val core = getMuscleGroupColor("Core")
        assertEquals(core, getMuscleGroupColor("Obliques"))
        assertEquals(expectedColors.getValue("Core"), core)

        assertNotEquals(getMuscleGroupColor("Abs"), core)
        assertEquals(expectedColors.getValue("Abs"), getMuscleGroupColor("Abs"))
        // Yellow: red and green channels high and close together, blue far below.
        assertEquals(
            "Core should be closer to red and green than to blue, " +
                "was r=${core.red} g=${core.green} b=${core.blue}",
            true,
            abs(core.green - core.blue) > abs(core.red - core.green)
        )
    }

    @Test
    fun armsMusclesKeepDistinctColors() {
        val arms = listOf("Biceps", "Triceps", "Arms", "Forearms").map { getMuscleGroupColor(it) }
        assertEquals("arms muscles must stay distinguishable", arms.size, arms.toSet().size)
    }

    @Test
    fun unknownGroupsFallBackToNeutralGrey() {
        val grey = getMuscleGroupColor("Something Made Up")
        assertEquals(Color(0xFF64748B), grey)
        bodyParts.forEach { part ->
            assertNotEquals(
                "the fallback must not collide with a real body part colour",
                getMuscleGroupColor(part),
                grey
            )
        }
    }
}
