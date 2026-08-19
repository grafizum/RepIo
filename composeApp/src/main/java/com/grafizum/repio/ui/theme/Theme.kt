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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.grafizum.repio.R


val LocalDeveloperSkin = staticCompositionLocalOf { false }

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

    val typography = if (isDeveloper) getDeveloperTypography() else Typography

    CompositionLocalProvider(LocalDeveloperSkin provides isDeveloper) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
