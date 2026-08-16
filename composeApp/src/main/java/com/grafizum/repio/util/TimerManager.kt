package com.grafizum.repio.util

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

interface TimerManager {
    fun scheduleTimer(endTimeMillis: Long)
    fun cancelTimer()
    fun showTimerNotification()
}

private var appInstance: Application? = null

fun initTimerManager(application: Application) {
    appInstance = application
}

class AndroidTimerManager : TimerManager {
    private val context: Context get() = appInstance!!

    override fun scheduleTimer(endTimeMillis: Long) {
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
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            } catch (e2: Exception) {}
        }
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
    }

    override fun showTimerNotification() {
        NotificationHelper.showRestTimerEndedNotification(context)
    }
}

fun getTimerManager(): TimerManager = AndroidTimerManager()
