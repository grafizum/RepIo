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
 * Dark palette — ONE shade ramp, shared by EVERY dark theme.
 *
 * Owner, 2026-10-13 (dashboard task_1k6x0b4q3072712v0h496e2l): "i want the card
 * to in the dark themes (develope theme included) to have the background color
 * dark and the cards in the same dark grey palette but less dark so it sees as a
 * front (instead of using the primary theme color for every card), and make sure
 * the develope theme and the dark theme have the same grey shading palette
 * applied."
 *
 * Two things were wrong with the shape this replaces, and both were MEASURED
 * rather than argued about:
 *
 *  1. THE CARDS DID NOT READ AS A FRONT. The old ladder stepped the canvas to
 *     the card by only 1.055:1 (#0D0D0F -> #141416) — under the 1.02-1.2:1 band
 *     the ramp test allows, but at the bottom of it, and the DEVELOPER scheme
 *     had a *different* ladder again (#131313 / #1B1B1B / #252525), so the dark
 *     themes did not even share a grey. The canvas is one notch deeper now, and
 *     every dark scheme reads this same ladder, so the step from stage to card
 *     is 1.10:1 — still a whisper, but a visible one.
 *
 *  2. NO HUE IN ANY GREY. The old ladder was deliberately a touch blue
 *     (0x0D0D0F: B above R and G), which is exactly what the owner rejected on
 *     the developer theme in 2026-10-08 ("a true dark grey palette lik my app on
 *     the phone had"). Every value below has R = G = B. The accent is the only
 *     colour in a dark scheme; the surfaces are the same neutral on every one of
 *     them, so switching from Flame to Developer changes the accent and nothing
 *     else about the shading.
 *
 * The steps are narrow on purpose: adjacent tones must whisper, not shout. Each
 * one is 1.03-1.13:1 from the next, inside the band DarkThemeContrastTest holds
 * every dark theme to.
 *
 * REBASED ONTO THE BEIGE THEME'S OWN STEPS (2026-10-14, third pass, and this is
 * the one that answers the whole question). Owner: *"the grey of the bottom bar
 * and the general cards background color is the one you were supposed to make
 * darker ... and make the card you actually change slightly lighter? i want it
 * to have the same visuals as the light beige theme"*. The beige theme was
 * measured on the device rather than described (`evidence/dark-cards-2026-10-14`,
 * captures of both themes): canvas #FDFCFB, chips/sheets #F9F7F4, general cards
 * #F4F1ED, the workout hero card #F4F0EB, the bottom bar #F0EBE5, the selected
 * tab pill #E6DDD4. So a light screen is a canvas with SMALL steps away from it —
 * 1.044, 1.099, 1.107, 1.157, 1.309:1 — and the dark theme was not that at all.
 * Its canvas is the DARK extreme, so the same picture is the same steps in the
 * other direction, and each value below is the grey that reproduces the matching
 * beige ratio above its #0E0E0E canvas:
 *
 *   role                        beige step   this ladder
 *   canvas                          1.000   #0E0E0E  1.000
 *   chips, sheets, dialogs          1.044   #141414  1.048
 *   general cards, nested blocks    1.099   #191919  1.098
 *   the bottom bar                  1.157   #1B1B1B  1.121
 *   the workout hero cards          1.107   #1E1E1E  1.158
 *   selected pill, pressed states   1.309   #282828  1.309
 *
 * THE HERO CARD AND THE BAR ARE TWO RUNGS, NOT ONE (fourth pass, same day). The
 * row above used to read "hero card, chips, the BAR" against a single #1E1E1E, and
 * the owner caught that too: *"make the card you actually change slightly lighter?"*.
 * Sharing the bar's rung meant the card the whole pass was about was not lighter
 * than the chrome around it - and the frame proved it, the card and the bar both
 * coming back #1E1E1E at 26.3% and 9.2% of the screen. So the BAR moved down one
 * notch (#1B1B1B, 1.121:1) and the hero card kept its step (1.158:1), which puts
 * the card 1.033:1 above the bar and makes it the lightest panel on a dark screen.
 *
 * That deliberately inverts ONE pair against the beige theme, and it is worth
 * naming: beige's hero card (1.107) is a smaller step than its bar (1.157), so a
 * strict mirror would put the dark card UNDER the dark bar. The owner asked for the
 * opposite in as many words, and he is looking at his own screen rather than at the
 * table - a card is a panel and the bar is chrome, so the panel reads as the front
 * one. The steer that survives is the one he gave: bar and general cards down, the
 * changed card up. `theDarkStepsAreTheLightThemesStepsMirrored` pins `bar > cards`
 * (still true, 1.121 over 1.098) and the new `theHeroCardIsLighterThanTheBottomBar`
 * pins the rest.
 *
 * WHAT WAS ACTUALLY WRONG: the bottom bar and every Material card are `surface`
 * plus Material's elevation tint, and that tint was `onSurface` — a near-WHITE
 * ink — so both composited to #2C2C2C, **1.42:1 from the canvas, three times the
 * widest step the beige theme ever takes**. That is the "too light grey" of the
 * report, and it was on the app's chrome rather than on its cards, which is why
 * the previous pass (which moved the card rung) did not fix it. The tint is now
 * [DarkSurfaceHighest]: elevation can lift a surface by a whisper and can never
 * wash it light again. The rungs themselves came down with it, so the hero cards
 * (#1E1E1E) sit LIGHTER than the general cards (#191919) — the owner's second
 * half, and the relationship the beige theme has (its hero card is the lighter,
 * warmer panel of the two).
 *
 * NO HUE IN ANY GREY (unchanged): the old ladder was deliberately a touch blue
 * (0x0D0D0F: B above R and G), which is exactly what the owner rejected on the
 * developer theme in 2026-10-08 ("a true dark grey palette lik my app on the
 * phone had"). Every value below has R = G = B. The accent is the only colour in
 * a dark scheme; the surfaces are the same neutral on every one of them, so
 * switching from Flame to Developer changes the accent and nothing else about
 * the shading. Nothing can drift either: every dark scheme reads these five
 * values.
 */
val DarkBackground = Color(0xFF0E0E0E) // canvas — deepest tone, owns the stage
val DarkSurface = Color(0xFF141414) // first layer above the canvas: sheets, dialogs, chips
val DarkSurfaceVariant = Color(0xFF191919) // second layer: THE GENERAL CARDS, nested wells
val DarkBarSurface = Color(0xFF1B1B1B) // the bottom bar: chrome, one notch UNDER the hero cards
val DarkSurfaceHigh = Color(0xFF1E1E1E) // third layer: THE WORKOUT HERO CARDS - the front panel
val DarkSurfaceHighest = Color(0xFF282828) // fourth layer: pressed/dragged states, the tab pill

/**
 * The WEAKER rim: dividers, list separators, the hairline around a nested block.
 *
 * This is `outlineVariant` on every dark scheme, and it exists because that token was
 * previously set by NOBODY. Material's baseline fills it with 0xFF49454F - a purple-grey - so
 * every divider and card edge in the app was drawn in the stock palette's hue while the rest of
 * the screen used the selected theme's. Measured on the DEVELOPER theme's level guide: the
 * rims came back #323035 (R 50, B 53 - the baseline #49454F anti-aliased over a #1B1B1B card),
 * 1.1% of the frame. Small, and exactly the sort of thing that makes a dark UI read as
 * "stock" rather than as a designed palette. 1.59:1 on `surface`, 1.53:1 on a card
 * ([DarkSurfaceVariant]) and 1.78:1 on the canvas, a deliberate rung below [DarkBorder]
 * (1.94 / 1.88 / 2.18) because a divider should be quieter than the edge of the surface it
 * sits on.
 */
val DarkHairline = Color(0xFF3D3D3D)

/**
 * Structural separators. Was #2C2C2C, which measured 1.32:1 on a card and
 * 1.39:1 on the canvas - the borders, dividers and card rims that carry most of
 * the "graphical information" in a dark UI were all but invisible. 1.5:1 is the
 * floor below which a divider is not doing its job; this sits well above it
 * (2.18 / 1.94 / 1.88 against canvas / surface / surfaceVariant) so it reads as a
 * quiet rim rather than a drawn line, and it is the value the owner's own
 * DEVELOPER scheme already carried.
 */
val DarkBorder = Color(0xFF4A4A4A)

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

// RoyalBlue Sky
val RoyalBluePrimary = Color(0xFF007AFF)
val RoyalBlueSecondary = Color(0xFF5856D6)

// Ocean
val OceanPrimary = Color(0xFF00B8D4)
val OceanLight = Color(0xFF84FFFF)

// Rose
val RosePrimary = Color(0xFFF06292)
val RoseLight = Color(0xFFFFB2C1)
