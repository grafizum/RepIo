package com.grafizum.repio

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle

/**
 * THE LAUNCHER ENTRIES HAND OFF TO THE APP; THEY ARE NOT THE APP ANY MORE.
 *
 * WHY THIS SHAPE. The theme switch changes the home-screen icon by enabling one theme's
 * entry and disabling the rest ([com.grafizum.repio.util.IconSwitchHelper]). Disabling a
 * component closes what is running as it - and not only the ActivityRecord's own component:
 * it closes the TASK whose base intent names it, which is why `DONT_KILL_APP` did not save
 * it (`DONT_KILL_APP` keeps the PROCESS alive, not the window).
 *
 * MEASURED on emulator-5554, 2026-10-14, app started through its theme entry exactly as the
 * home-screen icon does:
 *
 *     IconSwitchHelper: Enabled component: …LauncherFlame (process kept alive)
 *     pid before 12111 -> after 12111          (the process really did survive)
 *     WindowManagerShell: Transition CLOSE … TaskInfo{taskId=154
 *         baseIntent=Intent { cmp=…/LauncherOcean }
 *         baseActivity=ComponentInfo{…/MainActivityOcean} topActivity=…MainActivityOcean}
 *     Task #154 closed, and the owner saw the app close.
 *
 * Note the middle line: even when the record carried an ALIAS's always-enabled target, the
 * task went with the disabled entry, because its base intent named it. So no arrangement in
 * which a LAUNCHER ENTRY is disabled can keep a task alive - the only fix is for the task the
 * user is looking at never to have been started from one.
 *
 * So the launcher starts one of these entries (it carries the icon, the theme and therefore
 * the splash: see AndroidManifest.xml), the entry starts the real screen - [MainActivity],
 * which is enabled in every theme and is never named by the icon switch - and finishes
 * immediately. The entry's own task is transient and empty by the time a theme could be
 * switched, so disabling it costs nothing.
 *
 * The per-theme SPLASH is untouched by this: the entry wears the same
 * `Theme.App.Starting.<Theme>` these entries have always worn, and it is the activity the
 * launcher starts, so its starting window IS the splash the user sees.
 */
abstract class ThemeLauncherEntry : Activity() {

    /**
     * The theme this entry stands for, passed to [MainActivity] so it can settle the theme
     * preference on the way in. [MainActivity] used to read it off its own class name, which
     * no longer identifies a theme: every entry now hands off to that one activity.
     */
    protected abstract val themeKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AN ALREADY-RUNNING UI IS BROUGHT FORWARD, NOT RESTARTED. The home-screen icon is
        // tapped dozens of times a session, and starting a second task each time would put
        // two of them in Recents and throw away what the user was looking at. Asked by
        // identity rather than "is the process alive": the process can outlive its task.
        val manager = getSystemService(ActivityManager::class.java)
        val runningTask = manager?.appTasks?.firstOrNull {
            it.taskInfo.baseIntent.component?.className == MainActivity::class.java.name
        }

        if (runningTask != null) {
            runningTask.moveToFront()
        } else {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // ...and a NEW task rather than a reusable one, because a task left over
                    // from before this change is still rooted in a theme entry, and handing
                    // off into THAT would hand the user straight back the bug above. A task
                    // this entry starts is rooted in MainActivity, which is never disabled.
                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    .putExtra(EXTRA_THEME_KEY, themeKey)
            )
        }

        finish()
    }

    companion object {
        /** The theme a launcher entry hands to [MainActivity]. */
        const val EXTRA_THEME_KEY = "com.grafizum.repio.extra.THEME_KEY"
    }
}

/**
 * These subclasses are the entry points in the AndroidManifest.xml that let each colour theme
 * have its own launcher icon AND its own splash screen. Android activity-aliases cannot carry
 * a theme (an alias's `android:theme` is ignored: measured, an alias declaring
 * `Theme.App.Starting.Ocean` reported its target's theme instead), so a distinct class per
 * theme is the only way to give the launcher entry a themed starting window - hence one
 * subclass each, and hence the theme living here rather than on an alias.
 */
class MainActivityFlame : ThemeLauncherEntry() {
    override val themeKey: String = "FLAME"
}

class MainActivityBeige : ThemeLauncherEntry() {
    override val themeKey: String = "BEIGE"
}

class MainActivityPurple : ThemeLauncherEntry() {
    override val themeKey: String = "PURPLE"
}

class MainActivityEmerald : ThemeLauncherEntry() {
    override val themeKey: String = "EMERALD"
}

class MainActivityCrimson : ThemeLauncherEntry() {
    override val themeKey: String = "CRIMSON"
}

class MainActivityRoyalBlue : ThemeLauncherEntry() {
    override val themeKey: String = "ROYAL_BLUE"
}

class MainActivityOcean : ThemeLauncherEntry() {
    override val themeKey: String = "OCEAN"
}

class MainActivityRose : ThemeLauncherEntry() {
    override val themeKey: String = "ROSE"
}

class MainActivityRetro : ThemeLauncherEntry() {
    override val themeKey: String = "DEVELOPER"
}
