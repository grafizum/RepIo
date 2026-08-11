package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object IconSwitchHelper {

    private const val TAG = "IconSwitchHelper"

    // These MUST match the android:name in the AndroidManifest.xml exactly
    private const val ORANGE_ICON_FQN = "com.example.MainActivityOrange"
    private const val BEIGE_ICON_FQN = "com.example.MainActivityBeige"

    fun switchIcon(context: Context, targetAlias: String) {
        val packageManager = context.packageManager
        
        val aliases = arrayOf(ORANGE_ICON_FQN, BEIGE_ICON_FQN)
        
        Log.d(TAG, "Switching icon to: $targetAlias")
        
        for (alias in aliases) {
            val newState = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            // Use the actual application package name for the component resolution
            val componentName = ComponentName(context.packageName, alias)
            
            try {
                val currentState = packageManager.getComponentEnabledSetting(componentName)
                if (currentState != newState) {
                    packageManager.setComponentEnabledSetting(
                        componentName,
                        newState,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "Component $alias set to state $newState")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch component state for $alias", e)
            }
        }
    }

    fun getOrangeAlias(context: Context) = ORANGE_ICON_FQN
    fun getBeigeAlias(context: Context) = BEIGE_ICON_FQN
}
