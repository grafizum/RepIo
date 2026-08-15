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
        
        // Handle Launcher Activity Icon and Splash Screen based on saved theme or system theme
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedTheme = prefs.getString("theme_option", null)
        val hasAutoSelected = prefs.getBoolean("auto_theme_selected", false)
        
        if (savedTheme != null) {
            // Force sync the launcher icon with the saved theme on every startup
            IconSwitchHelper.switchIcon(this, savedTheme, killApp = false)
        } else if (!hasAutoSelected) {
            val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isSystemDark = uiMode == Configuration.UI_MODE_NIGHT_YES
            
            val initialTheme = if (isSystemDark) "FLAME" else "BEIGE"
            prefs.edit().putString("theme_option", initialTheme).putBoolean("auto_theme_selected", true).apply()
            IconSwitchHelper.switchIcon(this, initialTheme, killApp = false)
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
