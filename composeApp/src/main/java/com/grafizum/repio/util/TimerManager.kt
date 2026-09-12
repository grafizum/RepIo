package com.grafizum.repio.util

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

interface TimerManager {
    /**
     * Starts (or refreshes) the rest timer: the background service that owns the
     * countdown, plus the alarm that backs it up.
     */
    fun scheduleTimer(endTimeMillis: Long, remainingSeconds: Int)

    /** Stops the rest timer and takes its countdown notification out of the tray. */
    fun cancelTimer()
}

/** Seconds left until [endTimeMillis] (rounded UP), shared by the in-app timer and the
 *  countdown notification so they always display the same value. */
fun remainingRestSeconds(endTimeMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
    val remainingMillis = endTimeMillis - nowMillis
    if (remainingMillis <= 0L) return 0
    return ((remainingMillis + 999) / 1000L).toInt()
}

/** Formats [totalSeconds] as m:ss (or h:mm:ss from an hour up), e.g. 90 -> "1:30". */
fun formatRestTime(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val seconds = s % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
           else "%d:%02d".format(minutes, seconds)
}

/**
 * What a running rest timer should do on one tick.
 *
 * The decision is taken from the stored end time alone — never from a count that the
 * service and the screen each keep for themselves — so the background timer and the
 * in-app one cannot drift apart, and a timer the user skipped or replaced is abandoned
 * instead of finishing late.
 */
sealed interface RestTimerTick {
    /** Still counting: [remainingSeconds] left to show. */
    data class Count(val remainingSeconds: Int) : RestTimerTick

    /** The end time has passed: raise the alert, then clear the timer. */
    data object Finish : RestTimerTick

    /** No timer is stored any more: stop ticking, but stay quiet. */
    data object Abandon : RestTimerTick
}

/**
 * One tick of the rest timer against the end time currently stored in the session.
 * An end time of zero or less means the timer was cleared, skipped or replaced, so the
 * caller has nothing left to do.
 */
fun restTimerTick(
    storedEndTimeMillis: Long,
    nowMillis: Long = System.currentTimeMillis()
): RestTimerTick = when {
    storedEndTimeMillis <= 0L -> RestTimerTick.Abandon
    storedEndTimeMillis <= nowMillis -> RestTimerTick.Finish
    else -> RestTimerTick.Count(remainingRestSeconds(storedEndTimeMillis, nowMillis))
}

private var appInstance: Application? = null

fun initTimerManager(application: Application) {
    appInstance = application
}

/**
 * Keeps a rest timer alive while the app is closed.
 *
 * The countdown and the end-of-rest alert are delivered by [RestTimerService], because a
 * notification that only the UI re-posts freezes the moment the process goes away. The
 * alarm stays armed behind the service for the cases a service cannot cover: a refused
 * background start, a reboot, or the OS reclaiming the process.
 */
class AndroidTimerManager : TimerManager {
    private val context: Context get() = appInstance!!

    override fun scheduleTimer(endTimeMillis: Long, remainingSeconds: Int) {
        scheduleAlarm(endTimeMillis)

        // Post the countdown directly as well: a service start isn't instant, and this
        // keeps the indicator visible even if the start below is refused.
        NotificationHelper.showRestTimerCountdown(context, remainingSeconds)

        try {
            ContextCompat.startForegroundService(context, RestTimerService.intent(context))
        } catch (_: Exception) {
            // The alarm alone still ends the timer; the next resume re-arms the service.
        }
    }

    /**
     * Drops the timer and takes its countdown out of the tray.
     *
     * The service is deliberately left running: it notices the timer is gone on its next
     * tick and shuts itself down, whereas stopping it here would mean starting a
     * foreground service again the moment the next rest begins — a start the OS may
     * refuse in the middle of a workout.
     */
    override fun cancelTimer() {
        cancelAlarm()
        NotificationHelper.cancelRestTimerCountdown(context)
    }

    /** Backs the service up: fires the same end-of-rest report if the service is gone. */
    private fun scheduleAlarm(endTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent(endTimeMillis)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            } catch (e2: Exception) {}
        }
    }

    private fun cancelAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.cancel(alarmPendingIntent(0L))
        } catch (e: Exception) {}
    }

    private fun alarmPendingIntent(endTimeMillis: Long): PendingIntent {
        val intent = Intent(context, RestTimerReceiver::class.java).apply {
            putExtra("TIMER_END_TIME", endTimeMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

fun getTimerManager(): TimerManager = AndroidTimerManager()
