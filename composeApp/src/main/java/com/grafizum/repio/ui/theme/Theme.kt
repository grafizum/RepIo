package com.grafizum.repio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
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


val LocalDeveloperSkin = staticCompositionLocalOf { false }

/**
 * True when the active color scheme paints a light background. UI code that
 * must stay readable on BOTH themes (e.g. muscle-group text tones, which the
 * palette deliberately keeps light) reads this instead of guessing.
 */
val LocalLightTheme = staticCompositionLocalOf { false }

private val PurpleColorScheme = darkColorScheme(
    primary = ElegantPurple,
    onPrimary = Color.White,
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
    onPrimary = Color.White,
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

private val AzureSkyColorScheme = lightColorScheme(
    primary = AzureSkyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF003366),
    secondary = AzureSkySecondary,
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
    onPrimary = Color.White,
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
    onPrimary = Color.White,
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
    onPrimary = Color.White,
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

@Composable
fun WorkoutTrackerTheme(
    themeOption: String = "DEFAULT",
    pageIndex: Int = -1,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isDeveloper = themeOption.uppercase() == "DEVELOPER"

    val isLightTheme = when (themeOption.uppercase()) {
        "BEIGE", "EMERALD", "CRIMSON", "AZURE" -> true
        "FLAME", "OCEAN", "ROSE", "PURPLE", "DEVELOPER", "RAINBOW" -> false
        else -> !isDark
    }

    val colorScheme = when (themeOption.uppercase()) {
        "BEIGE" -> BeigeColorScheme
        "FLAME" -> FlameColorScheme
        "EMERALD" -> EmeraldColorScheme
        "CRIMSON" -> CrimsonColorScheme
        "AZURE" -> AzureSkyColorScheme
        "OCEAN" -> OceanColorScheme
        "ROSE" -> RoseColorScheme
        "PURPLE" -> PurpleColorScheme
        "DEVELOPER" -> DeveloperColorScheme
        else -> if (isDark) FlameColorScheme else BeigeColorScheme
    }

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
