package com.grafizum.repio.util

class IosTimerManager : TimerManager {
    override fun scheduleTimer(endTimeMillis: Long) {}
    override fun cancelTimer() {}
    override fun showTimerNotification() {}
}

actual fun getTimerManager(): TimerManager = IosTimerManager()
