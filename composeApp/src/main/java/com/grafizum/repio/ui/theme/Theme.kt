package com.grafizum.repio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalLayoutDirection
import com.grafizum.repio.ui.components.LocalSafeArea
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.SafeArea
import com.grafizum.repio.ui.components.frameScaleFor
import com.grafizum.repio.ui.components.rememberWindowSize
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import com.grafizum.repio.R
import com.grafizum.repio.util.IconSwitchHelper


val LocalDeveloperSkin = staticCompositionLocalOf { false }

/**
 * The text scale the app ALWAYS renders at: the system's 1.0.
 *
 * Owner ruling, 2026-10-08: *"keep the scaling at 1.0 for the app, because 1.8 is absolutely
 * not usable as you can see"*, following the same session's *"i want it to look like its on my
 * phone"*. Previously the platform's `fontScale` drove the whole frame (see
 * [WorkoutTrackerTheme]), which kept a large-text device usable but made it a DIFFERENT app -
 * and at 1.8 it showed about a third of a phone's page and scrolled for everything.
 *
 * One constant, read in exactly two places ([WorkoutTrackerTheme]'s density override and the
 * WindowSize it measures with), so "the view is the same on each phone" is a property of the
 * app rather than of the device. Changing this number re-enables partial scaling everywhere at
 * once - which is the reason it is a named constant and not a literal `1f`.
 */
internal const val APP_TEXT_SCALE = 1f

/**
 * True when the active color scheme paints a light background. UI code that
 * must stay readable on BOTH themes (e.g. muscle-group text tones, which the
 * palette deliberately keeps light) reads this instead of guessing.
 */
val LocalLightTheme = staticCompositionLocalOf { false }

private val PurpleColorScheme = darkColorScheme(
    primary = ElegantPurple,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF3B2F63), // Slightly lighter for dark background
    onPrimaryContainer = Color.White,
    secondary = ElegantPurpleLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color.White,
    outline = DarkBorder
)

private val BeigeColorScheme = lightColorScheme(
    primary = Color(0xFF8C6239),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0EAE2),
    onPrimaryContainer = Color(0xFF5D4037),
    secondary = Color(0xFF795548),
    onSecondary = Color.White,
    background = Color(0xFFFDFCFB),
    onBackground = Color(0xFF2C2520),
    surface = Color(0xFFF9F7F4),
    onSurface = Color(0xFF2C2520),
    surfaceVariant = Color(0xFFF4F1ED),
    onSurfaceVariant = Color(0xFF4E443F),
    outline = Color(0xFFD7CCC8)
)

private val FlameColorScheme = darkColorScheme(
    primary = FlameOrange,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF4E342E), // Slightly lighter for dark background
    onPrimaryContainer = Color.White,
    secondary = FlameOrangeLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color.White,
    outline = DarkBorder
)

private val EmeraldColorScheme = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color.White,
    background = Color(0xFFFBFFFF),
    onBackground = Color(0xFF002B26),
    surface = Color(0xFFF4FBFB),
    onSurface = Color(0xFF002B26),
    surfaceVariant = Color(0xFFE8F6F5),
    onSurfaceVariant = Color(0xFF004D40),
    outline = Color(0xFFB2DFDB)
)

private val CrimsonColorScheme = lightColorScheme(
    primary = Color(0xFFC62828),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = Color(0xFFEF5350),
    onSecondary = Color.White,
    background = Color(0xFFFFFBFA),
    onBackground = Color(0xFF3B0000),
    surface = Color(0xFFFFF5F5),
    onSurface = Color(0xFF3B0000),
    surfaceVariant = Color(0xFFFDE8E9),
    onSurfaceVariant = Color(0xFF721C24),
    outline = Color(0xFFE9B9BD)
)

private val RoyalBlueColorScheme = lightColorScheme(
    primary = RoyalBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF003366),
    secondary = RoyalBlueSecondary,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF3A3A3C),
    outline = Color(0xFFC7C7CC)
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF243B42), // Slightly lighter for dark background
    onPrimaryContainer = OceanLight,
    secondary = OceanLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color.White,
    outline = DarkBorder
)

private val RoseColorScheme = darkColorScheme(
    primary = RosePrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF422230), // Slightly lighter for dark background
    onPrimaryContainer = RoseLight,
    secondary = RoseLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color.White,
    outline = DarkBorder
)

private val RainbowColorScheme = darkColorScheme(
    primary = Color(0xFFFF00FF), // Magenta
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF5E005E), // Slightly lighter for dark background
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF00FFFF), // Cyan
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color.White,
    outline = Color(0xFFFF00FF)
)

/*
 * THE DEVELOPER THEME: TRUE DARK GREY, MEASURED OFF THE OWNER'S PHONE.
 *
 * Owner, 2026-10-08: "i want the color of the app remade (only the developer theme no
 * other themes) ... go look at my usb debugged phone and see the color palette", and then,
 * after seeing the first attempt: "i want rounded corners and a more modern rich darker
 * design not brownisch i want a true dark grey palette lik my app on the phone had".
 *
 * THE PHONE IS THE SPEC, so it was SAMPLED rather than interpreted: `screencap` with RepIo
 * in the foreground came back as #0A0A0A on 87.6% of the screen, #111111 for the layer
 * above it, #181818 for nested blocks, #C3A366 as the single accent, #EDEBE8 text and
 * #A0A0A0 muted text. Every colour below is one of those six or a neutral step between
 * two of them.
 *
 * THE FIRST ATTEMPT GOT THE IDEA BACKWARDS, and it is worth recording why, because it
 * looked reasonable at the time: it pushed BEIGE'S OWN brown (#8C6239, accent #D9A566) into
 * a dark key, on the theory that the phone ran Beige and the theme should therefore be its
 * "dark twin". The phone's stored theme was indeed BEIGE - but what it was RENDERING was a
 * dark grey screen, and the result of that theory was dark chocolate (#12100E canvas,
 * #1A1715 cards, #3A2E1E container). Surfaces only have to be dark and grey; the warmth
 * belongs to the accent, not to the greys. There is no hue in any grey below, and the one
 * accent is the gold the phone actually showed.
 *
 * REVISED 2026-10-10, at the owner's request: "i want that golden color to be a bit
 * stronger just slightly so it has a nice contrast, and for that dont have the other color
 * be pure black but instead very dark grey". So the greys were lifted OFF pure black onto a
 * very dark grey ladder (#131313 canvas, #1B1B1B cards, #252525 nested blocks - still
 * neutral, still no hue) and the gold was strengthened from #C3A366 to #D6B266. The outline
 * had to rise with the surfaces (#3F3F3F -> #4A4A4A): a rim is only visible against the
 * colour it sits on, so lifting the cards without it would have quietly erased the borders.
 *
 * REVISED AGAIN 2026-10-11, and THIS is the revision in force. The owner asked for colours
 * "a bit more vibrant and not so fumed / smoked / boring"; three palettes were rendered for
 * that decision (design/developer-theme-vibrance.html: this scheme with a hotter gold, a
 * cool graphite, and a two-accent one), and the answer rejected the SHAPE rather than the
 * accent - "the palette is ugly it looks horrible, revert it back to how it was and simply
 * make the primary golden color a bit more vivid / vibrant". So the greys, the containers
 * and the rim are back to their 2026-10-10 values (#131313 canvas, #1B1B1B cards, #252525
 * nested blocks, #343434 / #2F2F2F containers, #4A4A4A rim). The lifted one-more-step ladder
 * this revision first shipped was the "smoked" look being complained about, not its cure -
 * a lesson worth keeping: whiteness of surface reads as haze, not as vibrance. ONE value
 * moved, and it is the only one:
 *
 *   * THE ACCENT'S CHROMA, with hue and lightness held exactly: #D6B266 (HSL 41/58/62) ->
 *     #E2B75A (HSL 41/70/62). Saturation is the axis the word "vivid" names; brightening the
 *     gold instead only washes it towards yellow-white against a canvas that did not move.
 *
 * Contrast in force: accent #E2B75A on #131313 is 9.87:1 (9.15:1 on a card and 8.14:1 on a
 * nested block, against 9.22:1 / 8.55:1 / 7.61:1 before the accent moved), the button label
 * #1C1810 on the gold fill is 9.39:1, body #EDEBE8 on #131313 is 15.62:1, muted #A0A0A0 on
 * #252525 is 5.86:1, the container's own text #E9D9B8 on #343434 is 8.94:1 and the rim is
 * 1.94:1 on a card - all clear of the 4.5:1 body, 3:1 large-text and 1.5:1 rim floors
 * DarkThemeContrastTest enforces. The five-step container ramp is back to #131313 / #1B1B1B /
 * #252525 / #2F2F2F / #373737.
 */
private val DeveloperColorScheme = darkColorScheme(
    // The one accent: the phone's own gold, its chroma raised on the owner's request
    // (2026-10-11, above), and the only non-grey in the whole scheme.
    primary = Color(0xFFE2B75A),
    onPrimary = Color(0xFF1C1810),
    // A neutral grey block carrying the accent's own tint for its text.
    primaryContainer = Color(0xFF343434),
    onPrimaryContainer = Color(0xFFE9D9B8),
    secondary = Color(0xFFB8B8B8),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF2F2F2F),
    onSecondaryContainer = Color(0xFFE6E6E6),
    // TRUE dark grey, lifted off pure black: three steps of the SAME neutral, no hue in any
    // of them. The source phone rendered #0A0A0A; the owner has since asked for a very dark
    // grey rather than black, which is the one place this scheme deliberately departs from
    // the sampled values. A further lift was tried on 2026-10-11 and reverted - see above.
    background = Color(0xFF131313),
    onBackground = Color(0xFFEDEBE8),
    surface = Color(0xFF1B1B1B),
    onSurface = Color(0xFFEDEBE8),
    surfaceVariant = Color(0xFF252525),
    onSurfaceVariant = Color(0xFFA0A0A0),
    // Light enough to draw the hairline rims a dark UI leans on: 1.94:1 on a card and 1.73:1
    // on a nested block, against the 1.5:1 floor. It rides the raised surfaces - a rim
    // tuned to a black canvas looks drawn but invisible once the cards are lifted.
    outline = Color(0xFF4A4A4A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/**
 * The developer theme's type: MANROPE, and the pixel font is gone.
 *
 * Owner, 2026-10-08: "drop the pixel font find a clean even more rich and modern font
 * to apply." Manrope is a modern geometric grotesque - open apertures, a single-storey
 * 'a' that stays legible at label sizes, and a real weight range from 300 to 800, so a
 * heading can be heavy without its counters closing up. That range is the "rich" part:
 * the old Pixelify Sans had one weight and a pixel grid, which is why every screen read
 * as a debug build.
 *
 * It ships as ONE variable font (`res/font/manrope.ttf`), so each weight is selected on
 * the `wght` axis instead of a second file per weight. `FontVariation` needs API 26+;
 * on anything older the platform renders the font's default instance and synthesises
 * bold. That is acceptable here only because the sizes and line heights below are
 * authored to be readable with or without the weight axis - do not take the weights as
 * guaranteed below API 26.
 */
// `FontVariation.weight` is still marked experimental upstream; the opt-in is deliberate
// rather than a suppression - the weight axis is the whole reason this font is bundled
// as one variable file instead of six statics.
@OptIn(ExperimentalTextApi::class)
@Composable
fun getDeveloperTypography(): androidx.compose.material3.Typography {
    fun manrope(weight: Int, style: FontWeight) = Font(
        resId = R.font.manrope,
        weight = style,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight))
    )

    val manropeFamily = FontFamily(
        manrope(300, FontWeight.Light),
        manrope(400, FontWeight.Normal),
        manrope(500, FontWeight.Medium),
        manrope(600, FontWeight.SemiBold),
        manrope(700, FontWeight.Bold),
        manrope(800, FontWeight.ExtraBold)
    )

    // Display and headline styles earn their name: heavier weight, and slightly negative
    // tracking (a modern grotesque opens up when it is tracked out at large sizes).
    return androidx.compose.material3.Typography(
        displayLarge = Typography.displayLarge.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp
        ),
        displayMedium = Typography.displayMedium.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp
        ),
        displaySmall = Typography.displaySmall.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
        ),
        headlineLarge = Typography.headlineLarge.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
        ),
        headlineMedium = Typography.headlineMedium.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp
        ),
        headlineSmall = Typography.headlineSmall.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp
        ),
        titleLarge = Typography.titleLarge.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.SemiBold
        ),
        titleMedium = Typography.titleMedium.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.SemiBold
        ),
        titleSmall = Typography.titleSmall.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.Medium
        ),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = manropeFamily, fontSize = 16.sp),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = manropeFamily, fontSize = 14.sp),
        bodySmall = Typography.bodySmall.copy(fontFamily = manropeFamily, fontSize = 12.sp),
        labelLarge = Typography.labelLarge.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp
        ),
        labelMedium = Typography.labelMedium.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
        ),
        labelSmall = Typography.labelSmall.copy(
            fontFamily = manropeFamily, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp
        )
    )
}

/**
 * Scales a single text unit, leaving non-sp values (Unspecified, em) untouched so an
 * unset `lineHeight` or `letterSpacing` never blows up the whole typography object.
 */
private fun TextUnit.scaledBy(factor: Float): TextUnit = if (isSp) this * factor else this

private fun TextStyle.scaledBy(factor: Float): TextStyle = copy(
    fontSize = fontSize.scaledBy(factor),
    lineHeight = lineHeight.scaledBy(factor),
    letterSpacing = letterSpacing.scaledBy(factor)
)

/**
 * Size-aware typography.
 *
 * Only the structural styles (display/headline/title) are scaled: they are what makes a
 * narrow phone feel cramped and what pushes headers into wrapping on long German/Italian
 * labels. Body and label text keeps its authored size so nothing becomes hard to read.
 */
/**
 * Points the M3 surface-container ramp at this scheme's own surfaces.
 *
 * Every palette below stops at `surface` and `surfaceVariant`. The container ramp in
 * between was therefore left at the Material BASELINE palette — and the baseline is
 * purple. Anything that draws its container from that ramp walks straight past the
 * selected theme and paints lavender: `AlertDialog` ("Levels", every delete and
 * confirm prompt, the level picker), `Menu`, and the sheets.
 *
 * On DARK schemes the ramp is now a real four-step shade ladder (surface →
 * variant → high → highest): layered surfaces are what make a dark UI read as
 * elevated and premium instead of one flat grey. On LIGHT schemes the old
 * mapping is kept — their surfaces already differ enough from the background
 * that a further ramp adds noise, not depth.
 *
 * Applied where the scheme is chosen, so a new palette cannot forget it.
 */
internal fun androidx.compose.material3.ColorScheme.withThemedContainers(isDark: Boolean) =
    if (!isDark) copy(
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant
    ) else copy(
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceVariant,
        // DERIVED FROM THE SCHEME ITSELF, not from the shared cold-grey constants these
        // two steps used to read. Those constants made every dark theme's upper ramp a
        // FIXED grey, so any palette whose surfaces were not that grey got a band of the
        // wrong colour under its chips, inputs and sheets. Mixing a little of the scheme's
        // own text colour into its variant gives every dark palette a ramp in ITS OWN
        // tone, and only ever moves the step towards the text, so contrast cannot drop.
        // Measured on the developer scheme, which is now the one that leans on it hardest:
        // #252525 -> #2F2F2F -> #373737, steps of 1.145:1 and 1.125:1, both inside the
        // 1.02-1.2:1 band the ramp test holds every dark theme to.
        surfaceContainerHigh = surfaceVariant.blendTowards(onSurface, 0.05f),
        surfaceContainerHighest = surfaceVariant.blendTowards(onSurface, 0.09f)
    )

/** Mixes [fraction] of [other] into this colour, for deriving a surface step. */
private fun Color.blendTowards(other: Color, fraction: Float) = Color(
    red = red + (other.red - red) * fraction,
    green = green + (other.green - green) * fraction,
    blue = blue + (other.blue - blue) * fraction,
    alpha = alpha
)

private fun androidx.compose.material3.Typography.scaledFor(scale: Float): androidx.compose.material3.Typography {
    if (scale == 1f) return this
    return copy(
        displayLarge = displayLarge.scaledBy(scale),
        displayMedium = displayMedium.scaledBy(scale),
        displaySmall = displaySmall.scaledBy(scale),
        headlineLarge = headlineLarge.scaledBy(scale),
        headlineMedium = headlineMedium.scaledBy(scale),
        headlineSmall = headlineSmall.scaledBy(scale),
        titleLarge = titleLarge.scaledBy(scale),
        titleMedium = titleMedium.scaledBy(scale),
        titleSmall = titleSmall.scaledBy(scale)
    )
}

/**
 * Theme ids that no longer exist, and which must therefore be treated as if the
 * user had never chosen a theme at all.
 *
 * A theme id is a PERSISTED STRING (`theme_option` in SharedPreferences) and its
 * launcher activity is a PERSISTED COMPONENT STATE. Retiring one therefore leaves
 * two broken things on the device, and fixing only the first is the trap:
 *
 *  1. the stored id matches nothing in `resolveColorScheme`, so it falls through
 *     to `else` and the user is silently moved to another theme;
 *  2. the enabled component (`MainActivityAzure`) no longer exists, so NO
 *     launcher activity resolves at all and the app has no icon. Writing a valid
 *     theme id to the preference does nothing for this - only `switchIcon` can.
 *
 * So a retired id is deliberately resolved to null ("no stored choice") rather
 * than to its successor. That routes the user through the SAME fresh-install path
 * a new user takes, which picks the system-appropriate default AND repairs the
 * launcher icon. Mapping `AZURE` -> `ROYAL_BLUE` would have looked tidier and
 * fixed only problem 1.
 *
 * `AZURE` (2026-10-02): the theme's colour was always #007AFF, hue 211 deg - a
 * vivid blue, not azure. True azure (#007FFF) is hue 210 deg, so "make it
 * actually azure" was a 1-degree change that also moved the theme TOWARD OCEAN
 * (hue 188 deg, only 75/441 away) and made the two harder to tell apart in the
 * picker. The colour was fine; the NAME was wrong.
 *
 * Deliberately NOT retired here: anything a user can currently pick. Only ids
 * removed by a rename belong in this set.
 */
private val RETIRED_THEME_OPTIONS = setOf("AZURE")

/**
 * Exposed so a test can assert the set's SHAPE - that it holds dead ids only and
 * never a mapping to a successor - rather than inferring it from behaviour. A
 * `mapOf(old to new)` slipped in here would still pass every behavioural case,
 * because translating a retired id and resetting it both "resolve to something".
 */
internal fun retiredThemeOptions(): Set<String> = RETIRED_THEME_OPTIONS

/**
 * True when [stored] names a theme that has been retired, whatever its casing.
 *
 * Kept separate from [migrateThemeOption] because the two callers need different
 * things: the preference layer must CLEAR the stale id (and the auto-selected
 * flag with it, so the fresh-install path is allowed to run), while the scheme
 * resolver just needs to stop honouring it.
 */
internal fun isRetiredThemeOption(stored: String?): Boolean =
    stored != null && stored.uppercase() in RETIRED_THEME_OPTIONS

/**
 * Maps a stored theme option to the value the app should use for it. A RETIRED
 * id becomes null - "no stored choice" - so every caller's own defaulting stays
 * the single place that decides what an unknown value means. Anything else is
 * returned unchanged, including ids that never existed.
 */
internal fun migrateThemeOption(stored: String?): String? =
    if (isRetiredThemeOption(stored)) null else stored

/**
 * Resolves the colour scheme for a theme option under a given system appearance.
 *
 * Extracted from the composable so the routing is TESTABLE. "The orange dark
 * theme" is not a name in this file - orange has no scheme, and the `else`
 * branch below is what makes it. Asserting contrast against literals instead of
 * this function would pass forever no matter what changed here, which is the
 * opposite of what a contrast test is for.
 */
internal fun resolveColorScheme(themeOption: String, isDark: Boolean): ColorScheme {
    return when (migrateThemeOption(themeOption)?.uppercase()) {
        "BEIGE" -> BeigeColorScheme
        "FLAME" -> FlameColorScheme
        "EMERALD" -> EmeraldColorScheme
        "CRIMSON" -> CrimsonColorScheme
        "ROYAL_BLUE" -> RoyalBlueColorScheme
        "OCEAN" -> OceanColorScheme
        "ROSE" -> RoseColorScheme
        "PURPLE" -> PurpleColorScheme
        "DEVELOPER" -> DeveloperColorScheme
        // An unknown or RETIRED id resolves through the SAME default a fresh
        // install gets, so this asks [IconSwitchHelper.defaultThemeFor] rather
        // than repeating its rule inline.
        //
        // This used to be `if (isDark) FlameColorScheme else BeigeColorScheme` -
        // a second, private copy of the fresh-install default. It was correct
        // while the default followed the system, and it became WRONG the moment
        // the owner ruled (2026-10-04) that the base theme is Beige in both
        // appearances: this branch would have sent a retired id to Flame on a
        // dark phone while every other path sent it to Beige, and the symptom
        // would have been a user whose AZURE selection silently came back as a
        // dark theme. ThemeRenameTest caught it, which is the only reason it is
        // worth being honest that the inline copy was a latent second decision
        // all along.
        else -> when (IconSwitchHelper.defaultThemeFor(isDark).uppercase()) {
            "FLAME" -> FlameColorScheme
            else -> BeigeColorScheme
        }
    }
}

/**
 * Whether a theme's palette was authored for DARK surfaces.
 *
 * Extracted from [WorkoutTrackerTheme] so the answer is TESTABLE and so there is
 * exactly one copy of it. It was inline `when` before 2026-10-04, and
 * `LauncherIconNightVariantTest` had to scrape the branch out of the composable's
 * source text to check the icon split against it - reading Kotlin with a regex,
 * which passes happily when the composable is reformatted and silently checks
 * nothing when it is not.
 *
 * The pairing matters more than it looks. A theme's palette and its container
 * ramp are ONE decision: Beige's colours are authored for a light card, so
 * giving it dark containers produces an unreadable screen - muted text at 2.61:1
 * and outlines at 1.47:1, both under their thresholds. That is not a
 * hypothetical pairing; it is exactly what the app did.
 *
 * THE `else` BRANCH IS THE BUG THIS SHAPE HIDES, and it is why this function
 * exists rather than staying an inline `when`. An unrecognised key - and
 * "DEFAULT", the composable's own parameter default, is the common one - used to
 * fall through to `isSystemDark`. Since the 2026-10-04 ruling the palette for such
 * a key comes from `resolveColorScheme`, whose own `else` resolves through
 * `defaultThemeFor` and is therefore Beige. So on a dark phone the app paired
 * BEIGE'S LIGHT PALETTE with DARK CONTAINERS: light-theme content colours painted
 * onto dark surfaces. Every other call site looked fine, because each passed a
 * real theme id, which is why it reached the device uncaught. `DarkThemeContrastTest`
 * caught it by deriving the ramp instead of assuming it.
 *
 * So the fallback asks the same question [resolveColorScheme] asks. One rule, two
 * answers was the defect.
 */
internal fun isDarkThemeUsedFor(themeKey: String, isSystemDark: Boolean): Boolean =
    when (themeKey.uppercase()) {
        "BEIGE", "EMERALD", "CRIMSON", "ROYAL_BLUE" -> false
        "FLAME", "OCEAN", "ROSE", "PURPLE", "DEVELOPER" -> true
        // Same default as resolveColorScheme: the fresh-install theme, which is
        // Beige in both appearances. NOT isSystemDark - that is the line that put
        // a light palette on a dark ramp.
        //
        // "RAINBOW" was on the `true` line here and is deliberately NOT restored.
        // It is not a theme: there is no `RAINBOW` case in `resolveColorScheme`, no
        // picker entry and no launcher icon, so naming it only guaranteed that an
        // id nobody can select would be handed DARK containers - while the palette
        // it resolved to (the fresh-install default) is light. That is the same
        // mismatch one layer up. With the `else` branch below asking
        // `defaultThemeFor`, any unrecognised id is handled by the one rule rather
        // than by a stale entry.
        else -> isDarkThemeUsedFor(IconSwitchHelper.defaultThemeFor(isSystemDark), isSystemDark)
    }

@Composable
fun WorkoutTrackerTheme(
    themeOption: String = "DEFAULT",
    pageIndex: Int = -1,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // Migrated ONCE here rather than per branch, so the renamed id cannot resolve
    // differently in `resolveColorScheme` than it does in the checks below it.
    // A RETIRED id resolves to the SAME default resolveColorScheme would pick,
    // rather than falling back to the retired id itself. Falling back to
    // `themeOption` here would quietly undo the failsafe for this one branch:
    // the colours would come from the default scheme while the status-bar
    // appearance below was still derived from the dead id.
    val themeKey = migrateThemeOption(themeOption)
        ?: IconSwitchHelper.defaultThemeFor(isDark)
    val isDeveloper = themeKey.uppercase() == "DEVELOPER"

    // A dark scheme is one whose palette was authored for dark surfaces — used
    // both for the container ramp and the status-bar appearance below.
    val isDarkThemeUsed = isDarkThemeUsedFor(themeKey, isDark)

    val isLightTheme = !isDarkThemeUsed

    val colorScheme = resolveColorScheme(themeOption, isDark).withThemedContainers(isDarkThemeUsed)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = isLightTheme
                insetsController.isAppearanceLightNavigationBars = isLightTheme
            }
        }
    }

    // Resolved before MaterialTheme so the type scale can be applied to the typography,
    // and provided to every screen so they share one set of spacing tokens.
    //
    // Measured at [APP_TEXT_SCALE], NOT at the platform's font scale: the tokens have to
    // describe the text the app will actually draw, and since the ruling below the app
    // draws everything at 1.0. Passing the platform's number here was the last thing that
    // kept a 1.8 device on the large-text layout after the density had been pinned.
    val windowSize = rememberWindowSize(APP_TEXT_SCALE)
    val typography = (Typography)
        .scaledFor(windowSize.typeScale)

    // THE APP ALWAYS DRAWS AT 1.0 - THE FONT SCALE IS PINNED HERE, ONCE.
    //
    // Owner, 2026-10-08, after seeing the emulator at the device's own font scale 1.8:
    // "keep the scaling at 1.0 for the app, because 1.8 is absolutely not usable as you can
    // see", and asked again for the app to look the same on every device. So the platform's
    // font scale no longer reaches the UI: `LocalDensity` is re-provided with
    // `fontScale = APP_TEXT_SCALE`, which is where every sp in the tree gets its size.
    //
    // WHAT THIS REPLACES, and it is worth knowing because the code below looks like it does
    // more than it now does: the previous pass made the FRAME ride the font 1:1 (density x k,
    // fontScale / k) so a large system font grew the padding, the gaps and the blocks around
    // the text with it. That was the right fix for "pressed together" and it is still what
    // the numbers below compute - but the owner has ruled that large-text rendering is not a
    // state the app should have, so [APP_TEXT_SCALE] is 1.0 and `frameScale` is therefore
    // always exactly 1. The arithmetic is kept rather than deleted because it is the single
    // place the scale enters the app: changing the constant is the whole change, and it is
    // what makes re-enabling a partial scale (say 1.15) a one-line decision instead of an
    // archaeology exercise. `WindowSizeTest` still pins the 1:1 contract on `frameScaleFor`
    // and `windowSizeFor` themselves.
    //
    // HONEST CONSEQUENCE, stated where the decision lives: a user who set a large system font
    // because they need it now gets RepIo at 1.0 like everyone else. That is a deliberate
    // trade for "the view is the same for each phone", not an oversight - global text size is
    // the usual place users get it back, and the constant above is the dial.
    val platformDensity = LocalDensity.current
    val frameScale = frameScaleFor(APP_TEXT_SCALE)
    // Keyed on the density rather than on the Density instance: the platform can hand back a
    // new wrapper for the same number, and re-creating this would re-lay out the whole tree
    // for nothing.
    val frameDensity = remember(platformDensity.density) {
        Density(
            density = platformDensity.density * frameScale,
            fontScale = APP_TEXT_SCALE / frameScale
        )
    }

    // THE SYSTEM'S OWN CHROME, MEASURED ONCE, FOR THE SCREENS THAT ARE NOT IN THE SCAFFOLD.
    //
    // `Scaffold` applies `WindowInsets.systemBars` to its content for free, so the pager and
    // everything inside it need nothing from here. The level-guide gate and the welcome setup
    // are siblings of the Scaffold, and they used to reserve the system bars by guessing: the
    // guide padded its page by `screenPadding` (16dp), the setup screen left a hard-coded 48dp
    // under its footer. Both numbers are the APP's own margin, and only the second one happened
    // to equal a real inset - see [SafeArea] for the S22+/Pixel 7a measurements that show what
    // that costs on the phone whose system bars are bigger than the guess.
    //
    // Read through [frameDensity], the density the tree below actually lays out with, so the
    // padding it produces is the system bar's own pixel height on any future frame scale.
    //
    // READ IN THE COMPOSITION, NOT INSIDE A `remember`. The system hands a window its insets
    // after the first frame, so on the very first composition these are 0 - and a `remember`
    // whose keys (the singleton `WindowInsets`, the density) do not change would keep that 0
    // forever: the state read would recompose this scope, the calculation would not re-run, and
    // the screens would keep a zero reserve while the app looked "fixed but still wrong".
    // Measured exactly that way once: the padding version of this (`windowInsetsPadding`, which
    // observes the insets itself) moved the level guide's top row to y 162, and the `remember`
    // version of the same numbers left it at y 44 on the same build.
    val systemBars = WindowInsets.systemBars
    val layoutDirection = LocalLayoutDirection.current
    val safeTop = with(frameDensity) { systemBars.getTop(this) }
    val safeBottom = with(frameDensity) { systemBars.getBottom(this) }
    val safeStart = with(frameDensity) { systemBars.getLeft(this, layoutDirection) }
    val safeEnd = with(frameDensity) { systemBars.getRight(this, layoutDirection) }
    val safeArea = remember(safeTop, safeBottom, safeStart, safeEnd) {
        with(frameDensity) {
            SafeArea(safeTop.toDp(), safeBottom.toDp(), safeStart.toDp(), safeEnd.toDp())
        }
    }

    CompositionLocalProvider(
        LocalDeveloperSkin provides isDeveloper,
        LocalLightTheme provides isLightTheme,
        LocalWindowSize provides windowSize,
        LocalSafeArea provides safeArea,
        LocalDensity provides frameDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            // Global override to remove the "grey box" ripple effect and replace with nothing (or custom if needed)
            CompositionLocalProvider(
                LocalIndication provides NoRippleIndication
            ) {
                content()
            }
        }
    }
}

/**
 * A custom Indication that removes the default Material ripple (the "grey box").
 * It draws only the content without any overlay feedback.
 */
private object NoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node {
        return object : Modifier.Node() {}
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}
