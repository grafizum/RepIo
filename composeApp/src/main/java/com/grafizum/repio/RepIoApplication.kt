package com.grafizum.repio

import android.app.Application
import android.content.res.Configuration
import androidx.core.content.edit
import com.grafizum.repio.ui.theme.isRetiredThemeOption
import com.grafizum.repio.util.IconSwitchHelper

class RepIoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Handle Launcher Activity Icon and Splash Screen based on saved theme or system theme
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        // FAILSAFE: a RETIRED theme id is cleared outright, which hands the user
        // to the fresh-install path below exactly as if they had never chosen a
        // theme. This has to happen here, at the earliest reader, for TWO reasons:
        //
        //  - this is the only place that can decide the system-appropriate
        //    default before anything else reads the value; and
        //  - writing a valid id to the preference does NOT fix the icon. A user
        //    who had AZURE selected has `MainActivityAzure` enabled, and that
        //    component no longer exists, so NO launcher activity resolves at all
        //    and the app has no icon. Only switchIcon() repairs that, and only the
        //    fresh-install path calls it.
        //
        // `auto_theme_selected` is cleared WITH the id, because otherwise the
        // fresh-install branch below is skipped by its `!hasAutoSelected` guard
        // and the icon is never repaired - the exact failure this is here to
        // prevent. Clearing it is safe: it only means "this device has not been
        // given a theme yet", which is now true.
        val storedTheme = prefs.getString("theme_option", null)
        val isRetired = isRetiredThemeOption(storedTheme)
        if (isRetired) {
            prefs.edit {
                remove("theme_option")
                putBoolean("auto_theme_selected", false)
            }
        }
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
            // No kill flag to pass any more: [IconSwitchHelper.switchIcon] always writes its
            // component states with DONT_KILL_APP. That mattered most here, inside
            // Application.onCreate, where a flags-0 write would let the system kill the process
            // we are standing in. It is not enough on its own, mind: what made the app close on
            // a theme change was the ENTRY it had been launched from being disabled, which is
            // why the entries now hand off to [MainActivity] instead of being the app - see
            // [ThemeLauncherEntry].
            if (initialTheme != IconSwitchHelper.MANIFEST_DEFAULT_THEME) {
                IconSwitchHelper.switchIcon(this, initialTheme)
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

    }
}
