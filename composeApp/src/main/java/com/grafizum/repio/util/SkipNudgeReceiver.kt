package com.grafizum.repio.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.grafizum.repio.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the daily motivational-nudge alarm (and the reboot broadcast).
 *
 * Work happens off the main thread via [goAsync], so the receiver can read the
 * routine and logged sets from Room before deciding whether to post. Every rule
 * lives in [MotivationNudger.maybeNudgeInBackground].
 */
class SkipNudgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Alarms don't survive a reboot — re-arm the daily check.
            if (MotivationNudger.isEnabled(context)) {
                MotivationNudger.scheduleDailyCheck(context)
            }
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(appContext).workoutDao()
                MotivationNudger.maybeNudgeInBackground(
                    context = appContext,
                    routineDays = dao.getAllRoutineDaysOnce(),
                    loggedSets = dao.getAllLoggedSetsOnce()
                )
            } catch (_: Exception) {
                // Nothing to do — a missed nudge is harmless.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
