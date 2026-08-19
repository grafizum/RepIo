package com.grafizum.repio.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object AppEventBus {
    private val _restTimerEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restTimerEnded = _restTimerEnded.asSharedFlow()

    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground = _isAppInForeground.asStateFlow()

    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute = _currentRoute.asStateFlow()

    fun onRestTimerEnded() {
        _restTimerEnded.tryEmit(Unit)
    }

    fun setAppForeground(foreground: Boolean) {
        _isAppInForeground.value = foreground
    }

    fun setCurrentRoute(route: String?) {
        _currentRoute.value = route
    }
}
