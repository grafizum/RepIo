package com.grafizum.repio.ui.theme

import androidx.compose.ui.graphics.Color

// Purple Theme (Darker lavender/violet for better contrast)
val ElegantPurple = Color(0xFF9E7AFF)
val ElegantPurpleLight = Color(0xFFD7C7FF)

// Warm Beige Theme (Elegant Light/Warm Sand)
val WarmBeigeBackground = Color(0xFFF7F4EE)
val WarmBeigeSurface = Color(0xFFEFEAE1)
val WarmBeigeSurfaceVariant = Color(0xFFE2DBD0)
val WarmBeigePrimary = Color(0xFF8C6239) // Rich warm bronze
val WarmBeigeOnPrimary = Color(0xFFFFFFFF)
val WarmBeigeText = Color(0xFF2C2520)
val WarmBeigeTextSecondary = Color(0xFF63564A)

// Flame Theme
val FlameOrange = Color(0xFFFF6D00)
val FlameOrangeLight = Color(0xFFFFE0B2)

// Emerald Theme
val EmeraldPrimary = Color(0xFF00E676)
val EmeraldDark = Color(0xFF004D40)
val EmeraldLight = Color(0xFFA7F3D0)

/*
 * Dark palette — a real shade ramp, not one grey.
 *
 * Every dark theme previously painted background AND surface the same
 * 0xFF121212, and the container ramp was collapsed onto them, so cards sat on
 * a canvas of literally their own colour: nothing could read as elevated, and
 * the UI looked flat regardless of how much spacing or corner rounding it got.
 * "Expensive" dark surfaces are built from MANY closely-stepped layers of a
 * warm-neutral grey — the eye reads the delta between adjacent layers as
 * depth. The steps below (7 → 10 → 14 → 19 → 25 % lightness) are narrow on
 * purpose: adjacent tones must whisper, not shout.
 *
 * The ramp is slightly warm (equal R>B, green the highest channel) so it
 * harmonises with the bronze/gold primary of the light themes and never reads
 * as the blue-cold grey of stock Material dark.
 */
val DarkBackground = Color(0xFF0D0D0F) // canvas — deepest tone, owns the stage
val DarkSurface = Color(0xFF141416) // first layer above the canvas: cards
val DarkSurfaceVariant = Color(0xFF1B1B1F) // second layer: nested blocks, wells
val DarkSurfaceHigh = Color(0xFF232327) // third layer: chips, inputs, thumbs
val DarkSurfaceHighest = Color(0xFF2B2B30) // fourth layer: pressed/dragged states
val DarkHairline = Color(0xFF2E2E33) // 1dp edge on cards — cheap "glass" rim

/**
 * Structural separators. Was #2C2C2C, which measured 1.32:1 on a card and
 * 1.39:1 on the canvas - the borders, dividers and card rims that carry most of
 * the "graphical information" in a dark UI were all but invisible. 1.5:1 is the
 * floor below which a divider is not doing its job; this sits just above it
 * (1.63 / 1.72 / 1.52 against surface / background / surfaceVariant) so it still
 * reads as a quiet rim rather than a drawn line.
 */
val DarkBorder = Color(0xFF3A3A40)

/**
 * The label colour for a filled primary button, across EVERY dark theme.
 *
 * White was the value everywhere, and on a saturated primary it is the worst
 * pair on the screen: 2.82:1 on flame, 3.14:1 on purple, 2.38:1 on ocean,
 * 3.06:1 on rose, 3.14:1 on rainbow. A warm near-black clears all of them
 * (6.0-8.0:1) and stays in the same warm family as the surfaces, where flat
 * black would read as a hole punched through the button.
 *
 * Developer is the one dark theme this does not apply to: its primary is white,
 * so its label is already black at 21:1.
 */
val DarkOnPrimary = Color(0xFF1A0E00)

val TextPrimary = Color(0xFFFFFFFF) // Pure white
val TextSecondary = Color(0xFFE6E6E9) // Light gray for secondary text
val TextMuted = Color(0xFF9E9E9E) // Muted text

// Theme accents mapping
val FlameOrangeDarkAccent = Color(0xFF381E72)
val NeonCyan = Color(0xFFE8DEF8)
val BrightLime = Color(0xFFD0BCFF)

// Crimson Red
val CrimsonRed = Color(0xFFFF1744)
val CrimsonLight = Color(0xFFFF8A80)

// Azure Sky
val AzureSkyPrimary = Color(0xFF007AFF)
val AzureSkySecondary = Color(0xFF5856D6)

// Ocean
val OceanPrimary = Color(0xFF00B8D4)
val OceanLight = Color(0xFF84FFFF)

// Rose
val RosePrimary = Color(0xFFF06292)
val RoseLight = Color(0xFFFFB2C1)
