package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PurpleColorScheme = darkColorScheme(
    primary = ElegantPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF231B3A),
    onPrimaryContainer = Color.White,
    secondary = ElegantPurpleLight,
    onSecondary = Color.Black,
    background = ElegantPurpleDark,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFF5F5F5),
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
    primaryContainer = Color(0xFF3E2723),
    onPrimaryContainer = Color.White,
    secondary = FlameOrangeLight,
    onSecondary = Color.Black,
    background = Color(0xFF0F0F0F),
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color.White,
    outline = Color(0xFF3D3D3D)
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

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF000A21),
    onPrimaryContainer = MidnightLight,
    secondary = MidnightLight,
    onSecondary = Color.Black,
    background = Color(0xFF05070A),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF0A0E14),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF1B202B),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099)
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

private val GoldColorScheme = darkColorScheme(
    primary = VibrantGold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF241C00),
    onPrimaryContainer = GoldLight,
    secondary = GoldLight,
    onSecondary = Color.Black,
    background = Color(0xFF0D0D00),
    onBackground = Color(0xFFE6E2D3),
    surface = Color(0xFF141400),
    onSurface = Color(0xFFE6E2D3),
    surfaceVariant = Color(0xFF2E2D1F),
    onSurfaceVariant = Color(0xFFD1CDB9),
    outline = Color(0xFF9A9685)
)

private val SlateColorScheme = darkColorScheme(
    primary = SlateGrey,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B2124),
    onPrimaryContainer = SlateLight,
    secondary = SlateLight,
    onSecondary = Color.Black,
    background = Color(0xFF0D1012),
    onBackground = Color.White,
    surface = Color(0xFF141719),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF21272C),
    onSurfaceVariant = Color(0xFFEEEEEE),
    outline = Color(0xFF546E7A)
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF002026),
    onPrimaryContainer = OceanLight,
    secondary = OceanLight,
    onSecondary = Color.Black,
    background = Color(0xFF00080A),
    onBackground = Color.White,
    surface = Color(0xFF000C10),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF0A1D21),
    onSurfaceVariant = Color(0xFFE0F7FA),
    outline = Color(0xFF00838F)
)

private val RoseColorScheme = darkColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF37001B),
    onPrimaryContainer = RoseLight,
    secondary = RoseLight,
    onSecondary = Color.Black,
    background = Color(0xFF0A0005),
    onBackground = Color.White,
    surface = Color(0xFF140008),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF260A13),
    onSurfaceVariant = Color(0xFFFCE4EC),
    outline = Color(0xFFC2185B)
)

private val ForestColorScheme = darkColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF002403),
    onPrimaryContainer = ForestLight,
    secondary = ForestLight,
    onSecondary = Color.Black,
    background = Color(0xFF000800),
    onBackground = Color.White,
    surface = Color(0xFF001002),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF0F2612),
    onSurfaceVariant = Color(0xFFE8F5E9),
    outline = Color(0xFF388E3C)
)

private val AmberColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E2000),
    onPrimaryContainer = AmberLight,
    secondary = AmberLight,
    onSecondary = Color.Black,
    background = Color(0xFF0A0700),
    onBackground = Color.White,
    surface = Color(0xFF140E00),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF241C0A),
    onSurfaceVariant = Color(0xFFFFF8E1),
    outline = Color(0xFFFFA000)
)

@Composable
fun WorkoutTrackerTheme(
    themeOption: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val isLightTheme = when (themeOption.uppercase()) {
        "BEIGE", "EMERALD", "CRIMSON" -> true
        "FLAME", "MIDNIGHT", "GOLD", "SLATE", "OCEAN", "ROSE", "FOREST", "AMBER", "PURPLE" -> false
        else -> !isDark
    }

    val colorScheme = when (themeOption.uppercase()) {
        "BEIGE" -> BeigeColorScheme
        "FLAME" -> FlameColorScheme
        "EMERALD" -> EmeraldColorScheme
        "MIDNIGHT" -> MidnightColorScheme
        "CRIMSON" -> CrimsonColorScheme
        "GOLD" -> GoldColorScheme
        "SLATE" -> SlateColorScheme
        "OCEAN" -> OceanColorScheme
        "ROSE" -> RoseColorScheme
        "FOREST" -> ForestColorScheme
        "AMBER" -> AmberColorScheme
        "PURPLE" -> PurpleColorScheme
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
