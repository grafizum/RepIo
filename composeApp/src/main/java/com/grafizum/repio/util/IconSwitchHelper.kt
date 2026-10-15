package com.grafizum.repio.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object IconSwitchHelper {

    private const val TAG = "IconSwitchHelper"

    // These MUST match the android:name in the AndroidManifest.xml exactly.
    //
    // THESE ENTRIES ARE HAND-OFFS, NOT THE APP, and that is the fix for the owner's
    // 2026-10-14 report (*"now the app is closing again when changing theme"*). A theme
    // switch disables every entry but one, and disabling an entry takes down whatever was
    // started from it: not just the ActivityRecord that carries it, but the whole TASK whose
    // base intent names it. `DONT_KILL_APP` does not help - it keeps the PROCESS alive, not
    // the window. MEASURED on emulator-5554 with the app started through its theme entry as
    // the home-screen icon does: pid 12111 -> 12111 (the process really survived) while
    // WindowManager logged `Transition CLOSE ... baseIntent=Intent { cmp=…/LauncherOcean }`
    // and the task closed, which is the close the owner kept seeing. The count went the same
    // way when the entries were aliases pointing at an always-enabled target, because the
    // TASK's base intent still named the entry.
    //
    // So the entries stop being what the app runs as: each one starts [MainActivity] and
    // finishes (see [com.grafizum.repio.ThemeLauncherEntry]), and [MainActivity] is enabled in
    // every theme and is never named here. What this file disables is therefore always a
    // momentary entry, which is why switching the theme no longer closes the app.
    private const val PURPLE_ICON_FQN = "com.grafizum.repio.MainActivityPurple"
    private const val BEIGE_ICON_FQN = "com.grafizum.repio.MainActivityBeige"
    private const val FLAME_ICON_FQN = "com.grafizum.repio.MainActivityFlame"
    private const val EMERALD_ICON_FQN = "com.grafizum.repio.MainActivityEmerald"
    private const val CRIMSON_ICON_FQN = "com.grafizum.repio.MainActivityCrimson"
    private const val ROYAL_BLUE_ICON_FQN = "com.grafizum.repio.MainActivityRoyalBlue"
    private const val OCEAN_ICON_FQN = "com.grafizum.repio.MainActivityOcean"
    private const val ROSE_ICON_FQN = "com.grafizum.repio.MainActivityRose"
    private const val DEVELOPER_ICON_FQN = "com.grafizum.repio.MainActivityRetro"
    private const val BASE_ICON_FQN = "com.grafizum.repio.MainActivity"

    /**
     * The theme whose launcher activity the MANIFEST enables. A manifest can only
     * enable one of them, and this is the one it does, so this is also the theme
     * a fresh install shows before RepIoApplication has run.
     *
     * The application-level icon now names the SAME appearance
     * (`@mipmap/ic_launcher_beige_light`), so the icon a launcher shows before any
     * enabled activity exists cannot disagree with the one that wins afterwards.
     *
     * It used to be `@mipmap/ic_launcher_orange` on the reasoning that the fallback
     * should follow the system. That is not what the system does with it: the App
     * info page draws the application icon and resolves it with the SYSTEM's
     * configuration, so on a dark phone the fallback's own `mipmap-night-anydpi-v26`
     * twin was chosen and App info showed the dark plate with orange artwork.
     * OWNER REPORT, 2026-10-09: "when looking at the apps info it shows the dark icon
     * with orange in it but that shouldnt be the default icon, it should be light
     * with beige since thats the overall default theme." Beige is the base theme in
     * both appearances (see [defaultThemeFor]) and its `_light` icon has no night
     * twin, so naming it here is what makes the app look the same everywhere.
     */
    const val MANIFEST_DEFAULT_THEME = "BEIGE"

    /**
     * The theme a fresh install should start on.
     *
     * OWNER RULING 2026-10-04: "change the base theme of the app to be the light
     * beige one no matter if the system is dark or light". The parameter is still
     * taken and still named `isSystemDark`, but it NO LONGER CHANGES THE ANSWER -
     * Beige is the base theme in both appearances.
     *
     * Keeping the parameter rather than deleting it is deliberate. The two callers
     * (`RepIoApplication`, which repairs the launcher component, and
     * `WorkoutTrackerTheme`, which picks the scheme) both already have the system
     * appearance in hand and both pass it here, and ThemeRenameTest asserts
     * against this function for a retired id rather than hardcoding a theme.
     * Removing the argument would mean rewriting all of them to import a constant
     * instead, for no behavioural gain.
     *
     * WHAT THIS DOES NOT CHANGE, and it is the part worth being precise about:
     *  - A user who has ALREADY chosen a theme keeps it. This is the fresh-install
     *    default and the retired-id fallback, not a forced override - the stored
     *    `theme_option` still wins in `WorkoutTrackerTheme`.
     *  - Whether the icon gets a dark or a light plate is NOT decided by the
     *    system night setting, and that decoupling is what makes this ruling safe
     *    on a dark phone. Beige is a LIGHT theme, so it takes the
     *    `ic_launcher_beige_light` plate; because the light icons have no
     *    `mipmap-night-anydpi-v26/` counterpart, Android has nothing to override
     *    and the phone being dark cannot substitute the dark plate. That was the
     *    owner's 2026-10-02 report ("on light beige it shows dark") and it is why
     *    `tools/icons/split_launcher_icons.py` exists. See
     *    `LauncherIconNightVariantTest`.
     */
    fun defaultThemeFor(@Suppress("UNUSED_PARAMETER") isSystemDark: Boolean): String =
        MANIFEST_DEFAULT_THEME

    /**
     * The flags every component-state write below uses: [PackageManager.DONT_KILL_APP], and
     * never 0.
     *
     * WHY THIS IS A NAMED FUNCTION AND NOT A LITERAL. It was `if (killApp) 0 else DONT_KILL_APP`,
     * and every caller took the 0: `PlatformUtils.applyAppIconChange` left the parameter at its
     * `true` default, so picking a theme enabled the target launcher component with flags 0 -
     * which tells the system it MAY kill the process to apply the change. It did. Measured on
     * emulator-5554, 2026-10-14: tap a theme, press OK on the "Restart Required" dialog, and
     * `pidof com.grafizum.repio.debug` came back EMPTY, while the launcher entry only then moved
     * to the new theme's activity. So the icon cost the user a close-and-reopen, and the dialog
     * existed to explain a restart that was entirely self-inflicted.
     *
     * `DONT_KILL_APP` keeps the process: the colours already change live (the theme is a
     * preference the Compose scheme reads), so with the process alive the icon change is
     * seamless and no restart is needed for anything. The only thing that does NOT survive is the
     * splash theme, which by definition applies to the next cold start - and the next cold start
     * lands on the alias this same call enables, so it is already correct.
     *
     * A function rather than a constant so a unit test can hold it: `IconSwitchDoesNotKillTheApp`
     * asserts this is DONT_KILL_APP and is NOT 0, and that test fails against the shape that
     * shipped the close-and-reopen. The repo's own `RepIoApplication` repair path was already
     * passing `killApp = false` for exactly this reason - the app was the odd one out.
     */
    internal fun iconSwitchFlags(): Int = PackageManager.DONT_KILL_APP



    fun switchIcon(context: Context, themeKey: String) {
        val packageManager = context.packageManager
        val packageName = context.packageName

        val targetAlias = when (themeKey.uppercase()) {
            "PURPLE" -> PURPLE_ICON_FQN
            "BEIGE" -> BEIGE_ICON_FQN
            "FLAME" -> FLAME_ICON_FQN
            "EMERALD" -> EMERALD_ICON_FQN
            "CRIMSON" -> CRIMSON_ICON_FQN
            "ROYAL_BLUE" -> ROYAL_BLUE_ICON_FQN
            "OCEAN" -> OCEAN_ICON_FQN
            "ROSE" -> ROSE_ICON_FQN
            "DEVELOPER" -> DEVELOPER_ICON_FQN
            else -> BEIGE_ICON_FQN
        }

        val aliases = arrayOf(
            PURPLE_ICON_FQN, BEIGE_ICON_FQN, FLAME_ICON_FQN, EMERALD_ICON_FQN,
            CRIMSON_ICON_FQN, ROYAL_BLUE_ICON_FQN,
            OCEAN_ICON_FQN, ROSE_ICON_FQN, DEVELOPER_ICON_FQN
        )

        Log.d(TAG, "Switching icon for theme: $themeKey (Target: $targetAlias, Flags: ${iconSwitchFlags()})")

        // 1. Disable all other launcher entries first, with DONT_KILL_APP so the process is
        //    not killed. These are the momentary hand-off entries ([aliases] above): the
        //    screen the user is looking at is [com.grafizum.repio.MainActivity], which is
        //    never named here, so this loop cannot close the window the user is in.
        for (alias in aliases) {
            if (alias == targetAlias) continue
            
            val componentName = ComponentName(packageName, alias)
            try {
                val currentState = packageManager.getComponentEnabledSetting(componentName)
                if (currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    packageManager.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        iconSwitchFlags()
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
                packageManager.setComponentEnabledSetting(baseComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, iconSwitchFlags())
            }
        } catch (e: Exception) {}

        // 3. Enable the target alias last, so the launcher entry never disappears between the
        //    two halves of the switch. Flags come from [iconSwitchFlags] - see there for why it
        //    is not 0, and what 0 cost.
        val targetComponent = ComponentName(packageName, targetAlias)
        try {
            val currentState = packageManager.getComponentEnabledSetting(targetComponent)
            if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                packageManager.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    iconSwitchFlags()
                )
                Log.d(TAG, "Enabled component: $targetAlias (process kept alive)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable component $targetAlias", e)
        }
    }
}
