package com.grafizum.repio.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.grafizum.repio.data.LoggedSetEntity
import com.grafizum.repio.data.MotivationPhrases
import com.grafizum.repio.data.RoutineDayEntity
import com.grafizum.repio.data.WorkoutTemplates
import java.util.Calendar

/**
 * Motivational nudge for skipped workout days — delivered as a background
 * notification, never while the app is in use.
 *
 * Currently dormant: [NUDGES_ENABLED] is false, so none of the below runs until
 * the quotes are cleared for use. See that constant to switch it back on.
 *
 * A daily inexact alarm pings [SkipNudgeReceiver] at [CHECK_HOUR_OF_DAY]. The
 * receiver reads the routine + logged sets from the database and posts the nudge
 * only when [shouldNudge] passes every rule:
 *  - the user left the feature on,
 *  - the app is not in the foreground,
 *  - at least one scheduled workout day was skipped,
 *  - the user has training history (never nags a fresh install),
 *  - the app has been unused for at least [INACTIVITY_THRESHOLD_MS],
 *  - no nudge was already shown today.
 *
 * Alarms are re-armed every time the app is opened and after a reboot, so the
 * check keeps running even though the app itself is not.
 */
object MotivationNudger {

    /**
     * Master switch for the whole skipped-day nudge.
     *
     * OFF while we wait for permission to use the athletes' quoted catchphrases:
     * with this false the app arms no alarm, posts nothing, hides the settings
     * toggle and clears anything already sitting in the tray. Everything below —
     * the phrase catalogue, the background receiver, the notification channel — is
     * deliberately left untouched, so re-enabling after permission is granted is a
     * one-line change here and nothing else has to be rebuilt.
     *
     * A plain `val` rather than a `const` on purpose: a const would be folded into
     * the callers at compile time and trip unreachable-code warnings.
     */
    val NUDGES_ENABLED: Boolean = false

    /** The user's toggle lives with the other app settings (shared with the ViewModel). */
    private const val APP_PREFS = "app_settings"
    private const val KEY_ENABLED = "motivational_nudges"

    private const val STATE_PREFS = "motivation_prefs"
    private const val KEY_LAST_ACTIVE = "motivation_last_app_active"
    private const val KEY_LAST_SHOWN_DAY = "motivation_last_shown_day"

    /**
     * "Has not been used in a few days": how long the app must be idle before a
     * nudge is allowed. Three days also covers a full skipped weekend.
     */
    const val INACTIVITY_THRESHOLD_MS = 3L * 24 * 60 * 60 * 1000

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    private const val CHECK_HOUR_OF_DAY = 18
    private const val REQUEST_CODE = 1009

    const val ACTION_SKIP_NUDGE = "com.grafizum.repio.action.SKIP_NUDGE"

    // ---------------------------------------------------------------- scheduling

    /** Remembers that the user was just in the app (drives the inactivity rule). */
    fun recordAppUsage(context: Context) {
        context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .edit { putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()) }
    }

    /**
     * Called whenever the app comes to the foreground: remembers the visit,
     * clears any nudge already sitting in the tray, and keeps the background
     * check armed (or removed when the user turned the feature off).
     */
    fun onAppOpened(context: Context) {
        recordAppUsage(context)
        NotificationHelper.cancelMotivationNotification(context)
        if (isEnabled(context)) scheduleDailyCheck(context) else cancelDailyCheck(context)
    }

    /** The user's setting, gated by the [NUDGES_ENABLED] master switch. */
    fun isEnabled(context: Context): Boolean =
        NUDGES_ENABLED &&
            context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true)

    /** Arms (or re-arms) the once-a-day background check for [CHECK_HOUR_OF_DAY]. */
    fun scheduleDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextCheckTimeMillis(),
                AlarmManager.INTERVAL_DAY,
                checkPendingIntent(context)
            )
        } catch (_: Exception) {
            // Never let a refused alarm crash the app.
        }
    }

    fun cancelDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            alarmManager.cancel(checkPendingIntent(context))
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun checkPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SkipNudgeReceiver::class.java).apply {
            action = ACTION_SKIP_NUDGE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextCheckTimeMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, CHECK_HOUR_OF_DAY)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    // ------------------------------------------------------------------- delivery

    /**
     * Background entry point (called from [SkipNudgeReceiver] with data read from
     * the database). Applies every rule and posts the nudge when allowed.
     */
    fun maybeNudgeInBackground(
        context: Context,
        routineDays: List<RoutineDayEntity>,
        loggedSets: List<LoggedSetEntity>
    ) {
        // Master switch off: post nothing, and sweep away anything a build with
        // the feature enabled may have left in the tray.
        if (!NUDGES_ENABLED) {
            NotificationHelper.cancelMotivationNotification(context)
            return
        }

        val statePrefs = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val enabled = isEnabled(context)
        val skippedDays = WorkoutTemplates.calculateSkippedWorkoutDays(loggedSets, routineDays)

        val allowed = shouldNudge(
            now = now,
            lastActiveMillis = statePrefs.getLong(KEY_LAST_ACTIVE, 0L),
            skippedDays = skippedDays,
            enabled = enabled,
            hasTrainingHistory = loggedSets.isNotEmpty(),
            appInForeground = AppEventBus.isAppInForeground.value,
            lastShownDayKey = statePrefs.getLong(KEY_LAST_SHOWN_DAY, Long.MIN_VALUE)
        )

        if (!allowed) {
            // Clear a stale nudge once there is nothing left to say (trained again
            // or the feature was switched off).
            if (!enabled || skippedDays < 1) {
                NotificationHelper.cancelMotivationNotification(context)
            }
            return
        }

        val dayKey = now / MILLIS_PER_DAY
        val phrase = MotivationPhrases.forSkipCount(skippedDays, dayKey)
        // Nothing to say while the catalogue is held back for permission — never
        // post an empty notification.
        if (phrase.line.isBlank()) return
        NotificationHelper.showMotivationNotification(
            context = context,
            line = phrase.line,
            author = phrase.author
        )
        statePrefs.edit { putLong(KEY_LAST_SHOWN_DAY, dayKey) }
    }

    /**
     * Pure rule set so the gating is unit-testable without Android. Returns true
     * only when the nudge may be posted right now.
     */
    fun shouldNudge(
        now: Long,
        lastActiveMillis: Long,
        skippedDays: Int,
        enabled: Boolean,
        hasTrainingHistory: Boolean,
        appInForeground: Boolean,
        lastShownDayKey: Long
    ): Boolean {
        if (!enabled) return false
        if (appInForeground) return false
        if (!hasTrainingHistory) return false
        if (skippedDays < 1) return false
        if (lastActiveMillis <= 0L) return false
        if (now - lastActiveMillis < INACTIVITY_THRESHOLD_MS) return false
        return lastShownDayKey != now / MILLIS_PER_DAY
    }
}
