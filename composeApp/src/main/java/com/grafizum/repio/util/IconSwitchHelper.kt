package com.grafizum.repio.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object IconSwitchHelper {

    private const val TAG = "IconSwitchHelper"

    // These MUST match the android:name in the AndroidManifest.xml exactly
    private const val PURPLE_ICON_FQN = "com.grafizum.repio.MainActivityPurple"
    private const val BEIGE_ICON_FQN = "com.grafizum.repio.MainActivityBeige"
    private const val FLAME_ICON_FQN = "com.grafizum.repio.MainActivityFlame"
    private const val EMERALD_ICON_FQN = "com.grafizum.repio.MainActivityEmerald"
    private const val CRIMSON_ICON_FQN = "com.grafizum.repio.MainActivityCrimson"
    private const val ROYAL_BLUE_ICON_FQN = "com.grafizum.repio.MainActivityRoyalBlue"
    private const val OCEAN_ICON_FQN = "com.grafizum.repio.MainActivityOcean"
    private const val ROSE_ICON_FQN = "com.grafizum.repio.MainActivityRose"
    private const val DEVELOPER_ICON_FQN = "com.grafizum.repio.MainActivityRetro"
    private const val BASE_ICON_FQN = "com.grafizum.repio.MainActivity"

    /**
     * The theme whose launcher activity the MANIFEST enables. A manifest can only
     * enable one of them, and this is the one it does, so this is also the theme
     * a fresh install shows before RepIoApplication has run.
     *
     * The application-level icon now names the SAME appearance
     * (`@mipmap/ic_launcher_beige_light`), so the icon a launcher shows before any
     * enabled activity exists cannot disagree with the one that wins afterwards.
     *
     * It used to be `@mipmap/ic_launcher_orange` on the reasoning that the fallback
     * should follow the system. That is not what the system does with it: the App
     * info page draws the application icon and resolves it with the SYSTEM's
     * configuration, so on a dark phone the fallback's own `mipmap-night-anydpi-v26`
     * twin was chosen and App info showed the dark plate with orange artwork.
     * OWNER REPORT, 2026-10-09: "when looking at the apps info it shows the dark icon
     * with orange in it but that shouldnt be the default icon, it should be light
     * with beige since thats the overall default theme." Beige is the base theme in
     * both appearances (see [defaultThemeFor]) and its `_light` icon has no night
     * twin, so naming it here is what makes the app look the same everywhere.
     */
    const val MANIFEST_DEFAULT_THEME = "BEIGE"

    /**
     * The theme a fresh install should start on.
     *
     * OWNER RULING 2026-10-04: "change the base theme of the app to be the light
     * beige one no matter if the system is dark or light". The parameter is still
     * taken and still named `isSystemDark`, but it NO LONGER CHANGES THE ANSWER -
     * Beige is the base theme in both appearances.
     *
     * Keeping the parameter rather than deleting it is deliberate. The two callers
     * (`RepIoApplication`, which repairs the launcher component, and
     * `WorkoutTrackerTheme`, which picks the scheme) both already have the system
     * appearance in hand and both pass it here, and ThemeRenameTest asserts
     * against this function for a retired id rather than hardcoding a theme.
     * Removing the argument would mean rewriting all of them to import a constant
     * instead, for no behavioural gain.
     *
     * WHAT THIS DOES NOT CHANGE, and it is the part worth being precise about:
     *  - A user who has ALREADY chosen a theme keeps it. This is the fresh-install
     *    default and the retired-id fallback, not a forced override - the stored
     *    `theme_option` still wins in `WorkoutTrackerTheme`.
     *  - Whether the icon gets a dark or a light plate is NOT decided by the
     *    system night setting, and that decoupling is what makes this ruling safe
     *    on a dark phone. Beige is a LIGHT theme, so it takes the
     *    `ic_launcher_beige_light` plate; because the light icons have no
     *    `mipmap-night-anydpi-v26/` counterpart, Android has nothing to override
     *    and the phone being dark cannot substitute the dark plate. That was the
     *    owner's 2026-10-02 report ("on light beige it shows dark") and it is why
     *    `tools/icons/split_launcher_icons.py` exists. See
     *    `LauncherIconNightVariantTest`.
     */
    fun defaultThemeFor(@Suppress("UNUSED_PARAMETER") isSystemDark: Boolean): String =
        MANIFEST_DEFAULT_THEME

    fun switchIcon(context: Context, themeKey: String, killApp: Boolean = true) {
        val packageManager = context.packageManager
        val packageName = context.packageName

        val targetAlias = when (themeKey.uppercase()) {
            "PURPLE" -> PURPLE_ICON_FQN
            "BEIGE" -> BEIGE_ICON_FQN
            "FLAME" -> FLAME_ICON_FQN
            "EMERALD" -> EMERALD_ICON_FQN
            "CRIMSON" -> CRIMSON_ICON_FQN
            "ROYAL_BLUE" -> ROYAL_BLUE_ICON_FQN
            "OCEAN" -> OCEAN_ICON_FQN
            "ROSE" -> ROSE_ICON_FQN
            "DEVELOPER" -> DEVELOPER_ICON_FQN
            else -> BEIGE_ICON_FQN
        }

        val aliases = arrayOf(
            PURPLE_ICON_FQN, BEIGE_ICON_FQN, FLAME_ICON_FQN, EMERALD_ICON_FQN,
            CRIMSON_ICON_FQN, ROYAL_BLUE_ICON_FQN,
            OCEAN_ICON_FQN, ROSE_ICON_FQN, DEVELOPER_ICON_FQN
        )

        Log.d(TAG, "Switching icon for theme: $themeKey (Target: $targetAlias, Kill: $killApp)")

        // 1. Disable all other aliases first using DONT_KILL_APP to ensure all changes are processed
        for (alias in aliases) {
            if (alias == targetAlias) continue
            
            val componentName = ComponentName(packageName, alias)
            try {
                val currentState = packageManager.getComponentEnabledSetting(componentName)
                if (currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    packageManager.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "Disabled component: $alias")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable component $alias", e)
            }
        }

        // 2. We keep the base MainActivity enabled so Android Studio can always launch it for debugging
        // even if the icon variants are toggled.
        try {
            val baseComponent = ComponentName(packageName, BASE_ICON_FQN)
            if (packageManager.getComponentEnabledSetting(baseComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                packageManager.setComponentEnabledSetting(baseComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            }
        } catch (e: Exception) {}

        // 3. Enable the target alias last. 
        // We use 0 (instead of DONT_KILL_APP) if killApp is true to let the system refresh the launcher.
        val targetComponent = ComponentName(packageName, targetAlias)
        try {
            val currentState = packageManager.getComponentEnabledSetting(targetComponent)
            if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                packageManager.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    if (killApp) 0 else PackageManager.DONT_KILL_APP
                )
                Log.d(TAG, "Enabled component: $targetAlias")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable component $targetAlias", e)
        }
    }
}
