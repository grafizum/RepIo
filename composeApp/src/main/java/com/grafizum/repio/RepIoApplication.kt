package com.grafizum.repio

import android.app.Application
import android.content.res.Configuration
import androidx.core.content.edit
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
        
        if (savedTheme == null && !hasAutoSelected) {
            val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isSystemDark = uiMode == Configuration.UI_MODE_NIGHT_YES
            
            val initialTheme = if (isSystemDark) "FLAME" else "BEIGE"
            prefs.edit {
                putString("theme_option", initialTheme)
                putBoolean("auto_theme_selected", true)
            }
            // We removed the immediate switchIcon call here to prevent duplicate icons during first launch.
            // The app will use the manifest default (Beige) until the user changes it or it's applied later.
        }

        // Initialize platform utils for commonMain
        try {
            com.grafizum.repio.util.initPlatformUtils(this)
            com.grafizum.repio.util.initTimerManager(this)
            com.grafizum.repio.util.initSettings(this)
        } catch (e: Exception) {
            android.util.Log.e("RepIoApplication", "Failed to initialize internal utils", e)
        }

        // Initialize Firebase
        try {
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
        } catch (e: Exception) {
            android.util.Log.e("RepIoApplication", "Firebase initialization failed", e)
        }
    }
}
