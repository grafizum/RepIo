package com.grafizum.repio.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs a rest timer outside the UI.
 *
 * The countdown used to be re-posted from the session's one-second tick, so closing the
 * app froze the notification at whatever value it last managed to post — the rest looked
 * stopped even though the end time was still stored. As a foreground service the timer
 * owns the countdown notification and the end-of-rest alert itself, and keeps both
 * running after the activity is gone.
 *
 * The service deliberately keeps no timer state of its own: every second it re-reads the
 * end time from the same preferences the session writes. That makes it follow the
 * session for free — a new timer, a skipped one or a logged set is picked up within a
 * second, and a timer that is no longer stored shuts the service down instead of firing
 * late.
 */
class RestTimerService : Service() {

    /** Countdown work; cancelled with the service. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val endTime = timerPrefs().getLong(KEY_TIMER_END_TIME, 0L)
        if (endTime <= 0L) {
            // Nothing left to count (already skipped, replaced or finished elsewhere):
            // never sit in the foreground with nothing to show.
            stopTimer()
            return START_NOT_STICKY
        }

        startForegroundCountdown(endTime)
        startTicking()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Announces the countdown as this service's foreground notification, so the same
     * notification the session used to post is now owned by something that survives the
     * activity. Android 14+ additionally wants the service type the manifest declares.
     */
    private fun startForegroundCountdown(endTime: Long) {
        NotificationHelper.createNotificationChannel(this)
        val notification = NotificationHelper.restTimerCountdownNotification(
            this,
            remainingRestSeconds(endTime)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.COUNTDOWN_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.COUNTDOWN_NOTIFICATION_ID, notification)
        }
    }

    /** Re-posts the countdown once a second until the timer finishes or disappears. */
    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                when (val tick = restTimerTick(timerPrefs().getLong(KEY_TIMER_END_TIME, 0L))) {
                    is RestTimerTick.Count -> {
                        NotificationHelper.showRestTimerCountdown(
                            this@RestTimerService,
                            tick.remainingSeconds
                        )
                        delay(TICK_MILLIS)
                    }

                    RestTimerTick.Finish -> {
                        finishTimer()
                        return@launch
                    }

                    RestTimerTick.Abandon -> {
                        // The session moved on (skipped, restarted, or the set was logged
                        // early): drop the countdown without alerting.
                        stopTimer()
                        return@launch
                    }
                }
            }
        }
    }

    /**
     * Ends the timer: clears the stored timer *first*, so a late repeat from the fallback
     * alarm finds nothing left to report, then lets the shared notifier raise the alert,
     * vibrate, and tell the session the rest is over.
     */
    private fun finishTimer() {
        val silent = timerPrefs().getBoolean(KEY_SILENT_TIMER, false)
        clearTimerPrefs()
        NotificationHelper.showRestTimerEndedNotification(this, alert = !silent)
        stopSelf()
    }

    /** Stops ticking, takes the countdown out of the tray, and lets the service die. */
    private fun stopTimer() {
        tickJob?.cancel()
        NotificationHelper.cancelRestTimerCountdown(this)
        stopSelf()
    }

    private fun clearTimerPrefs() {
        timerPrefs().edit {
            putLong(KEY_TIMER_END_TIME, 0L)
            putBoolean(KEY_EXERCISE_TIMER, false)
            putBoolean(KEY_SILENT_TIMER, false)
            remove(KEY_TIMER_DAY)
        }
    }

    private fun timerPrefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TICK_MILLIS = 1000L
        private const val PREFS_NAME = "app_settings"
        private const val KEY_TIMER_END_TIME = "timer_end_time_millis"
        private const val KEY_EXERCISE_TIMER = "is_exercise_timer"
        private const val KEY_SILENT_TIMER = "is_silent_timer"
        private const val KEY_TIMER_DAY = "timer_day"

        /** Intent that starts (or re-arms) the background timer. */
        fun intent(context: Context): Intent = Intent(context, RestTimerService::class.java)
    }
}
