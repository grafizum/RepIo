package com.grafizum.repio.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A modifier that triggers [onClick] repeatedly while the component is pressed.
 * The delay between repetitions decreases exponentially.
 */
fun Modifier.repeatingClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    initialDelayMillis: Long = 500L,
    minDelayMillis: Long = 60L,
    delayDecayFactor: Float = 0.85f,
    onClick: () -> Unit
): Modifier = composed {
    val currentOnClick by rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope()

    pointerInput(interactionSource, enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown()
            var job: Job? = null
            try {
                job = scope.launch {
                    var currentDelay = initialDelayMillis
                    currentOnClick() // First trigger immediately
                    delay(currentDelay)
                    while (isActive && enabled) {
                        currentOnClick()
                        currentDelay = (currentDelay * delayDecayFactor).toLong().coerceAtLeast(minDelayMillis)
                        delay(currentDelay)
                    }
                }
                waitForUpOrCancellation()
            } finally {
                job?.cancel()
            }
        }
    }
}
