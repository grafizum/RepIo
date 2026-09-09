package com.grafizum.repio.util

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

interface TimerManager {
    fun scheduleTimer(endTimeMillis: Long, remainingSeconds: Int)
    fun cancelTimer()
    fun showTimerNotification()
    fun showCountdownNotification(remainingSeconds: Int)
    fun cancelCountdownNotification()
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

private var appInstance: Application? = null

fun initTimerManager(application: Application) {
    appInstance = application
}

class AndroidTimerManager : TimerManager {
    private val context: Context get() = appInstance!!

    override fun scheduleTimer(endTimeMillis: Long, remainingSeconds: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RestTimerReceiver::class.java).apply {
            putExtra("TIMER_END_TIME", endTimeMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        // Show a silent, ongoing countdown indicator in the notification shade
        NotificationHelper.showRestTimerCountdown(context, remainingSeconds)
    }

    override fun cancelTimer() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RestTimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {}
        NotificationHelper.cancelRestTimerCountdown(context)
    }

    override fun showTimerNotification() {
        NotificationHelper.showRestTimerEndedNotification(context)
    }

    override fun showCountdownNotification(remainingSeconds: Int) {
        NotificationHelper.showRestTimerCountdown(context, remainingSeconds)
    }

    override fun cancelCountdownNotification() {
        NotificationHelper.cancelRestTimerCountdown(context)
    }
}

fun getTimerManager(): TimerManager = AndroidTimerManager()