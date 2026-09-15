package com.grafizum.repio.util

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

interface TimerManager {
    /**
     * Starts (or refreshes) the rest timer: the alarm that reports the end of the rest.
     */
    fun scheduleTimer(endTimeMillis: Long, remainingSeconds: Int)

    /** Stops the rest timer. */
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
 * Keeps a rest timer armed while the app is closed.
 *
 * The alarm reports the end of a rest when the app is not in the foreground.
 */
class AndroidTimerManager : TimerManager {
    private val context: Context get() = appInstance!!

    override fun scheduleTimer(endTimeMillis: Long, remainingSeconds: Int) {
        scheduleAlarm(endTimeMillis)
    }

    /**
     * Drops the timer.
     */
    override fun cancelTimer() {
        cancelAlarm()
    }

    /** fires the end-of-rest report. */
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
