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
    private const val AZURE_ICON_FQN = "com.grafizum.repio.MainActivityAzure"
    private const val OCEAN_ICON_FQN = "com.grafizum.repio.MainActivityOcean"
    private const val ROSE_ICON_FQN = "com.grafizum.repio.MainActivityRose"
    private const val DEVELOPER_ICON_FQN = "com.grafizum.repio.MainActivityRetro"
    private const val BASE_ICON_FQN = "com.grafizum.repio.MainActivity"

    /**
     * The theme whose launcher activity the MANIFEST enables. A manifest can only
     * enable one of them, and this is the one it does, so this is also the theme
     * a fresh install shows before RepIoApplication has run.
     *
     * It is deliberately NOT orange, even though `<application android:icon>` is
     * `@mipmap/ic_launcher_orange`: the application icon is what a launcher shows
     * before any launcher-enabled activity exists, while the enabled activity's
     * own icon wins afterwards. Keeping the two different is how the manifest ends
     * up disagreeing with itself, so the enabled activity is the one that counts.
     */
    const val MANIFEST_DEFAULT_THEME = "BEIGE"

    /**
     * The theme a fresh install should start on: Flame on a dark system, Beige on
     * a light one.
     *
     * This only decides the APP THEME. It does not decide whether the icon has a
     * dark or a light plate - Android does that, from the system night setting,
     * by resolving `mipmap-night-anydpi-v26/` over `mipmap-anydpi-v26/`. On a
     * dark phone EVERY theme shows its dark-plate icon, which is correct and is
     * not something a theme choice can override.
     */
    fun defaultThemeFor(isSystemDark: Boolean): String =
        if (isSystemDark) "FLAME" else MANIFEST_DEFAULT_THEME

    fun switchIcon(context: Context, themeKey: String, killApp: Boolean = true) {
        val packageManager = context.packageManager
        val packageName = context.packageName

        val targetAlias = when (themeKey.uppercase()) {
            "PURPLE" -> PURPLE_ICON_FQN
            "BEIGE" -> BEIGE_ICON_FQN
            "FLAME" -> FLAME_ICON_FQN
            "EMERALD" -> EMERALD_ICON_FQN
            "CRIMSON" -> CRIMSON_ICON_FQN
            "AZURE" -> AZURE_ICON_FQN
            "OCEAN" -> OCEAN_ICON_FQN
            "ROSE" -> ROSE_ICON_FQN
            "DEVELOPER" -> DEVELOPER_ICON_FQN
            else -> BEIGE_ICON_FQN
        }

        val aliases = arrayOf(
            PURPLE_ICON_FQN, BEIGE_ICON_FQN, FLAME_ICON_FQN, EMERALD_ICON_FQN,
            CRIMSON_ICON_FQN, AZURE_ICON_FQN,
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
