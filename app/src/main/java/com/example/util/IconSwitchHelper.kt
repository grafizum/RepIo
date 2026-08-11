package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconSwitchHelper {

    const val ORANGE_ICON = "com.example.MainActivityOrange"
    const val BEIGE_ICON = "com.example.MainActivityBeige"

    fun switchIcon(context: Context, targetAlias: String) {
        val packageManager = context.packageManager
        
        val aliases = arrayOf(ORANGE_ICON, BEIGE_ICON)
        
        for (alias in aliases) {
            val state = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            packageManager.setComponentEnabledSetting(
                ComponentName(context, alias),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        
        /* 
           We do NOT disable the base MainActivity. 
           It just doesn't have an <intent-filter> for the launcher in the manifest.
           Disabling it might prevent aliases from launching their target.
        */
    }
}
