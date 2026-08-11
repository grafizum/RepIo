package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val expectedEndTime = intent.getLongExtra("TIMER_END_TIME", 0L)
        
        // Clear saved timer state in SharedPreferences
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val actualEndTime = prefs.getLong("timer_end_time_millis", 0L)

        // If the expected end time from the intent doesn't match what's currently in prefs,
        // it means this timer was replaced by a newer one or cancelled.
        if (expectedEndTime != 0L && expectedEndTime != actualEndTime) {
            return
        }

        prefs.edit().putLong("timer_end_time_millis", 0L).apply()

        // Show notification and play sound
        NotificationHelper.showRestTimerEndedNotification(context)
    }
}
