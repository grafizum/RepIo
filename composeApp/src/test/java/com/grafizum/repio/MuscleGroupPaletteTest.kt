package com.grafizum.repio

import androidx.compose.ui.graphics.Color
import com.grafizum.repio.ui.components.getMuscleGroupColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Guards the muscle-group palette. The colours are the only thing telling body
 * parts apart on the Analytics donut, the legend and the detailed breakdown bars,
 * so each muscle group has to keep the exact colour it is drawn with.
 *
 * The palette assigns a distinct (light) colour per muscle — its own tone for
 * Biceps, Triceps, Forearms and the delt heads, Amber for Abs and Yellow for
 * Core — rather than one colour per broad body part. The tones are the light
 * step of each hue by owner request: lighter, never a different hue.
 */
class MuscleGroupPaletteTest {

    /** Broad body parts, in the order they appear across the app. */
    private val bodyParts = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

    /** Every muscle group and the exact (light) colour it must resolve to. */
    private val expectedColors = mapOf(
        "Chest" to Color(0xFFF87171),
        "Pectorals" to Color(0xFFF87171),
        "Abs" to Color(0xFFFBBF24),
        "Abdominals" to Color(0xFFFBBF24),
        "Core" to Color(0xFFFACC15),
        "Obliques" to Color(0xFFFACC15),
        "Legs" to Color(0xFF34D399),
        "Quadriceps" to Color(0xFF34D399),
        "Hamstrings" to Color(0xFF34D399),
        "Glutes" to Color(0xFF34D399),
        "Calves" to Color(0xFF34D399),
        "Hip Adductors" to Color(0xFF34D399),
        "Hip Abductors" to Color(0xFF34D399),
        "Back" to Color(0xFF60A5FA),
        "Lats" to Color(0xFF60A5FA),
        "Upper Back" to Color(0xFF60A5FA),
        "Traps" to Color(0xFF60A5FA),
        "Upper Traps" to Color(0xFF60A5FA),
        "Spinal Erectors" to Color(0xFF60A5FA),
        "Biceps" to Color(0xFFA78BFA),
        "Triceps" to Color(0xFF22D3EE),
        "Arms" to Color(0xFF818CF8),
        "Forearms" to Color(0xFFFB923C),
        "Shoulders" to Color(0xFFF472B6),
        "Deltoids" to Color(0xFFF472B6),
        "Front Deltoids" to Color(0xFFF472B6),
        "Lateral Deltoids" to Color(0xFFF472B6),
        "Rear Deltoids" to Color(0xFFF472B6)
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
        assertEquals(Color(0xFF94A3B8), grey)
        bodyParts.forEach { part ->
            assertNotEquals(
                "the fallback must not collide with a real body part colour",
                getMuscleGroupColor(part),
                grey
            )
        }
    }

    /**
     * The palette is deliberately light (owner request). Measured as HSL
     * lightness — (max channel + min channel) / 2 — every tone must sit above
     * the halfway mark, which the old mid-tones (e.g. emerald 500 at 0.39)
     * fail and every current colour passes, whatever its hue.
     */
    @Test
    fun paletteStaysLight() {
        val all = expectedColors.values + Color(0xFF94A3B8)
        all.forEach { color ->
            val lightness = (listOf(color.red, color.green, color.blue).max() +
                listOf(color.red, color.green, color.blue).min()) / 2f
            assertTrue(
                "palette colour $color is too dark (HSL lightness $lightness) — " +
                    "the muscle-group palette must stay light",
                lightness > 0.5f
            )
        }
    }
}
