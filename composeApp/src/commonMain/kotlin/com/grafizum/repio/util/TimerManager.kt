package com.grafizum.repio.util

interface TimerManager {
    fun scheduleTimer(endTimeMillis: Long)
    fun cancelTimer()
    fun showTimerNotification()
}

expect fun getTimerManager(): TimerManager
