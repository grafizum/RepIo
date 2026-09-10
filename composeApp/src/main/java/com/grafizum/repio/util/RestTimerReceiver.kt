package com.grafizum.repio.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SKIP_TIMER) {
            // User pressed "Skip" on the countdown notification: stop the timer quietly.
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit {
                putLong("timer_end_time_millis", 0L)
                putBoolean("is_exercise_timer", false)
                putBoolean("is_silent_timer", false)
                remove("timer_day")
            }
            try {
                getTimerManager().cancelTimer()
            } catch (e: Exception) {}
            return
        }

        val expectedEndTime = intent.getLongExtra("TIMER_END_TIME", 0L)
        
        // Clear saved timer state in SharedPreferences
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val actualEndTime = prefs.getLong("timer_end_time_millis", 0L)

        // If the expected end time from the intent doesn't match what's currently in prefs,
        // it means this timer was replaced by a newer one or cancelled.
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