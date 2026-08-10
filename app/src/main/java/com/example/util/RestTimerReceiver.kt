package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Clear saved timer state in SharedPreferences
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putLong("timer_end_time_millis", 0L).apply()

        // Show notification and play sound
        NotificationHelper.showRestTimerEndedNotification(context)
    }
}
