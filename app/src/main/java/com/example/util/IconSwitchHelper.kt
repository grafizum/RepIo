package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconSwitchHelper {

    const val ORANGE_ICON = "com.example.MainActivityOrange"
    const val BEIGE_ICON = "com.example.MainActivityBeige"

    fun switchIcon(context: Context, targetAlias: String) {
        val packageManager = context.packageManager
        val packageName = context.packageName
        
        val aliases = arrayOf(ORANGE_ICON, BEIGE_ICON)
        
        for (alias in aliases) {
            val newState = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            val componentName = ComponentName(packageName, alias)
            val currentState = try {
                packageManager.getComponentEnabledSetting(componentName)
            } catch (e: Exception) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            }
            
            if (currentState != newState) {
                packageManager.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
