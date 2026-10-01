package com.grafizum.repio

import android.app.Application
import android.content.res.Configuration
import androidx.core.content.edit
import com.grafizum.repio.util.FirebaseInitializer
import com.grafizum.repio.util.IconSwitchHelper

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
            
            val initialTheme = IconSwitchHelper.defaultThemeFor(isSystemDark)
            prefs.edit {
                putString("theme_option", initialTheme)
                putBoolean("auto_theme_selected", true)
            }

            // The manifest can only enable ONE launcher entry, and it is Beige. On a
            // dark system the theme that should be showing is Flame, so the icon has
            // to be switched - but ONLY when the target differs from the entry the
            // manifest already enabled. Enabling the component that is already
            // enabled is what produced duplicate icons before, and it is also
            // pointless work.
            //
            // killApp = false: this runs inside Application.onCreate, and passing
            // 0 for the flags lets the system kill the process we are standing in.
            if (initialTheme != IconSwitchHelper.MANIFEST_DEFAULT_THEME) {
                IconSwitchHelper.switchIcon(this, initialTheme, killApp = false)
            }
        }

        // Initialize platform utils for commonMain
        try {
            com.grafizum.repio.util.initPlatformUtils(this)
            com.grafizum.repio.util.initTimerManager(this)
            com.grafizum.repio.util.initSettings(this)
        } catch (e: Exception) {
            android.util.Log.e("RepIoApplication", "Failed to initialize internal utils", e)
        }

        // Firebase and App Check are NOT initialized here any more.
        //
        // They used to be, on the main thread, before the first frame - and the only
        // thing in the app that uses them is the support-ticket path, which a user
        // reaches by asking for help. So every launch paid for a subsystem that is
        // idle almost always, and the cost landed straight in the visible startup
        // time because `setContent` had not run yet.
        //
        // `SupportManager` now awaits `FirebaseInitializer.ensureInitialized` on the
        // thread it is already on, so the first ticket initializes it and later ones
        // find it ready. Nothing about the ticket path changes, including its
        // error handling.
        //
        // Deliberately NOT calling FirebaseInitializer.warmUp() here: this is the
        // main thread, and warming up on it would reintroduce exactly the cost
        // being removed. Nothing to do on this path.
    }
}
