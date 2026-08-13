package com.grafizum.repio

import android.app.Application
import android.content.res.Configuration
import com.grafizum.repio.util.IconSwitchHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class RepIoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Handle First-Time Launcher Activity based on System Theme
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("is_first_time", true)
        val hasAutoSelected = prefs.getBoolean("auto_theme_selected", false)
        
        if (isFirstTime && !hasAutoSelected) {
            val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isSystemDark = uiMode == Configuration.UI_MODE_NIGHT_YES
            
            // Mark that we've performed the auto-selection
            prefs.edit().putBoolean("auto_theme_selected", true).apply()

            // Set the theme option so the app UI matches the system theme immediately
            // We DO NOT call switchIcon here because it kills the process on first launch
            // Default to BEIGE (Light) if not specifically in dark mode
            if (isSystemDark) {
                prefs.edit().putString("theme_option", "FLAME").apply()
            } else {
                prefs.edit().putString("theme_option", "BEIGE").apply()
            }
        }

        // Initialize platform utils for commonMain
        com.grafizum.repio.util.initPlatformUtils(this)
        com.grafizum.repio.util.initTimerManager(this)
        com.grafizum.repio.util.initSettings(this)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check for security (Abuse Prevention)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
