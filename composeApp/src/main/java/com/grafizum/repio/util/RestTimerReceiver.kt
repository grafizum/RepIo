package com.grafizum.repio.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

/**
 * The two ways out of a rest that are driven from outside the UI:
 *
 *  - the "Skip" action on the notification (if any), and
 *  - the fallback alarm, which reports the end of a rest.
 *
 * It clears the stored end time to keep from reporting the same rest twice.
 */
class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        if (intent.action == ACTION_SKIP_TIMER) {
            // User pressed "Skip" on the countdown notification: stop the timer quietly.
            // The session has to be told, or it would sit on a countdown that no longer
            // exists anywhere else.
            prefs.edit {
                putLong("timer_end_time_millis", 0L)
                putBoolean("is_exercise_timer", false)
                putBoolean("is_silent_timer", false)
                remove("timer_day")
            }
            try {
                getTimerManager().cancelTimer()
            } catch (e: Exception) {
                // Already gone.
            }
            AppEventBus.onRestTimerSkipped()
            return
        }

        val expectedEndTime = intent.getLongExtra("TIMER_END_TIME", 0L)
        val actualEndTime = prefs.getLong("timer_end_time_millis", 0L)

        // If the expected end time from the intent doesn't match what's currently in prefs,
        // it means this timer was replaced by a newer one, cancelled, or already reported.
        if (expectedEndTime != 0L && expectedEndTime != actualEndTime) {
            return
        }

        prefs.edit { putLong("timer_end_time_millis", 0L) }

        // Show notification and play sound
        NotificationHelper.showRestTimerEndedNotification(context)
    }

    companion object {
        const val ACTION_SKIP_TIMER = "com.grafizum.repio.action.SKIP_TIMER"
    }
}
