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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.TextUnit
import com.grafizum.repio.ui.components.LocalWindowSize
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

private val DeveloperColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Color.White,
    onSecondaryContainer = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color.White,
    outline = Color.White,
    error = Color.White,
    onError = Color.Black
)

@Composable
fun getDeveloperTypography(): androidx.compose.material3.Typography {
    val pixelFontFamily = FontFamily(Font(R.font.pixelify_sans))
    return androidx.compose.material3.Typography(
        displayLarge = Typography.displayLarge.copy(fontFamily = pixelFontFamily),
        displayMedium = Typography.displayMedium.copy(fontFamily = pixelFontFamily),
        displaySmall = Typography.displaySmall.copy(fontFamily = pixelFontFamily),
        headlineLarge = Typography.headlineLarge.copy(fontFamily = pixelFontFamily),
        headlineMedium = Typography.headlineMedium.copy(fontFamily = pixelFontFamily),
        headlineSmall = Typography.headlineSmall.copy(fontFamily = pixelFontFamily),
        titleLarge = Typography.titleLarge.copy(fontFamily = pixelFontFamily),
        titleMedium = Typography.titleMedium.copy(fontFamily = pixelFontFamily),
        titleSmall = Typography.titleSmall.copy(fontFamily = pixelFontFamily),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = pixelFontFamily, fontSize = 16.sp),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = pixelFontFamily, fontSize = 14.sp),
        bodySmall = Typography.bodySmall.copy(fontFamily = pixelFontFamily, fontSize = 12.sp),
        labelLarge = Typography.labelLarge.copy(fontFamily = pixelFontFamily),
        labelMedium = Typography.labelMedium.copy(fontFamily = pixelFontFamily),
        labelSmall = Typography.labelSmall.copy(fontFamily = pixelFontFamily)
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
        surfaceContainerHigh = surfaceContainerHighFallback,
        surfaceContainerHighest = surfaceContainerHighestFallback
    )

/**
 * The two extra dark ramp steps come from the dark palette directly. They are
 * derived, not stored on each ColorScheme (M3 has no slots for them above
 * surfaceVariant), so the dark palettes can't drift out of step with each other.
 */
private val surfaceContainerHighFallback = DarkSurfaceHigh
private val surfaceContainerHighestFallback = DarkSurfaceHighest

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
    val windowSize = rememberWindowSize()
    val typography = (if (isDeveloper) getDeveloperTypography() else Typography)
        .scaledFor(windowSize.typeScale)

    CompositionLocalProvider(
        LocalDeveloperSkin provides isDeveloper,
        LocalLightTheme provides isLightTheme,
        LocalWindowSize provides windowSize
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
