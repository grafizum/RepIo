package com.grafizum.repio.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object AppEventBus {
    private val _restTimerEnded = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    /**
     * A timer that just ended. The payload is TRUE when it was a TIMED
     * EXERCISE and false when it was a rest.
     *
     * It is a payload rather than a `Unit` because one event fires for both
     * and the two say different things: telling a user whose 45-second plank
     * just ended that "rest time has ended" is the bug this fixes, and the
     * in-app banner reads the same two strings the notification does.
     */
    val restTimerEnded = _restTimerEnded.asSharedFlow()

    /** A rest the user skipped from the countdown notification: nothing to ring for. */
    private val _restTimerSkipped = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restTimerSkipped = _restTimerSkipped.asSharedFlow()

    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground = _isAppInForeground.asStateFlow()

    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute = _currentRoute.asStateFlow()

    fun onRestTimerEnded(wasExerciseTimer: Boolean = false) {
        _restTimerEnded.tryEmit(wasExerciseTimer)
    }

    fun onRestTimerSkipped() {
        _restTimerSkipped.tryEmit(Unit)
    }

    private val _topBanner = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /**
     * A short refusal to show ABOVE everything, including a dialog.
     *
     * The owner's words: "dont have it inside the popup card but instead show it
     * at the top with a swift snappy drop in and drop out". Rendering it inside
     * the exercise editor's own card put the message in a Box that scrolled,
     * so on a long form the notice appeared somewhere the user was not looking
     * — and it could be clipped off the bottom entirely.
     *
     * A bus event rather than a per-screen state holder because the host has to
     * live ABOVE the dialog, and a dialog composable cannot draw outside its own
     * window bounds. So the dialog asks, and the activity draws.
     */
    val topBanner = _topBanner.asSharedFlow()

    fun showTopBanner(message: String) {
        _topBanner.tryEmit(message)
    }

    fun setAppForeground(foreground: Boolean) {
        _isAppInForeground.value = foreground
    }

    fun setCurrentRoute(route: String?) {
        _currentRoute.value = route
    }
}
