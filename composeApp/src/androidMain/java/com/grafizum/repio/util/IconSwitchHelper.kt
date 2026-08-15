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

    fun switchIcon(context: Context, themeKey: String, killApp: Boolean = true) {
        val packageManager = context.packageManager
        
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
            OCEAN_ICON_FQN, ROSE_ICON_FQN, DEVELOPER_ICON_FQN,
            BASE_ICON_FQN
        )
        
        Log.d(TAG, "Switching icon for theme: $themeKey (Target: $targetAlias, Kill: $killApp)")
        
        for (alias in aliases) {
            val newState = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            val componentName = ComponentName(context.packageName, alias)
            
            try {
                val currentState = packageManager.getComponentEnabledSetting(componentName)
                if (currentState != newState) {
                    packageManager.setComponentEnabledSetting(
                        componentName,
                        newState,
                        if (killApp) 0 else PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "Component $alias set to state $newState")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch component state for $alias", e)
            }
        }
    }
}
