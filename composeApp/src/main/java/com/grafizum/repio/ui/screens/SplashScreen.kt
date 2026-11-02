package com.grafizum.repio.ui.screens

import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * The shortest time the splash may be on screen at all.
 *
 * Owner, 2026-10-27 (dashboard task_4u2y69342j5q6i50422r464m): *"Dont load up the
 * app so slowly, dont force the user to stay on the loading screen for so long, if
 * the app loaded faster then load in faster but also not instantly minimum loading
 * time is 0.2s, if it needs longer to load then of course let it load"*.
 *
 * So this is a FLOOR, not the length: the screen used to run a fixed 2.1 s script
 * (100 ms in, 1600 ms hold, 400 ms out) whether the app was ready in 40 ms or not,
 * which is the "force the user to stay" the owner reported. It now ends when the
 * data is ready ([ready], fed by [com.grafizum.repio.ui.viewmodel.WorkoutViewModel.startupReady])
 * and never sooner than this.
 */
private const val SPLASH_MIN_VISIBLE_MS = 200L

/** The outro, which is decoration over a screen that is already leaving. */
private const val SPLASH_EXIT_MS = 150L

/**
 * A ceiling so a broken database can never trap the user on a logo.
 *
 * Five seconds is well past any measured cold start on this project's devices; if
 * it is ever hit, the app opens on whatever it has rather than waiting forever. It
 * is a failsafe, not a budget - the flash of the indicator after
 * [SPLASH_MIN_VISIBLE_MS] is what tells the user a real load is happening.
 */
private const val SPLASH_HARD_CEILING_MS = 5_000L

/**
 * The launch screen: the brand, and a thin indicator that only appears if the load
 * is genuinely slow.
 *
 * [ready] is the app's own readiness, not a timer - see [SPLASH_MIN_VISIBLE_MS] for
 * why the old fixed script was the bug. The indicator is deliberately late (it
 * fades in only after the 0.2 s floor has passed), so a fast start shows the logo
 * and nothing else rather than a spinner that appears and vanishes.
 */
@Composable
fun AppSplashScreen(ready: Boolean, onComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    // Elapsed real time rather than a countdown: the animations below take their
    // own frames, and the floor has to be measured from when this screen appeared.
    val startedAt = remember { SystemClock.elapsedRealtime() }
    var dataReady by remember { mutableStateOf(false) }
    // True once the floor has passed - i.e. when the app is actually slow.
    var showIndicator by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(ready) { if (ready) dataReady = true }

    LaunchedEffect(Unit) {
        delay(SPLASH_MIN_VISIBLE_MS)
        showIndicator = true
        delay(SPLASH_HARD_CEILING_MS - SPLASH_MIN_VISIBLE_MS)
        // Only reaches here when the load outlived the ceiling: open anyway.
        dataReady = true
    }

    LaunchedEffect(dataReady) {
        if (!dataReady) return@LaunchedEffect
        val waited = SystemClock.elapsedRealtime() - startedAt
        val remaining = (SPLASH_MIN_VISIBLE_MS - waited).coerceAtLeast(0L)
        if (remaining > 0) delay(remaining)
        visible = false
        delay(SPLASH_EXIT_MS)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Branding
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { 12 }, animationSpec = tween(200)),
                exit = fadeOut(tween(SPLASH_EXIT_MS.toInt()))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RepIo",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 6.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "YOUR WORKOUT ASSISTANT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        ),
                        color = primaryColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // A load that outlived the floor is the only case that shows this.
            AnimatedVisibility(
                visible = visible && showIndicator,
                enter = fadeIn(tween(200, delayMillis = 100)),
                exit = fadeOut(tween(100))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(48.dp)
                        .height(2.dp)
                        .clip(CircleShape),
                    color = primaryColor.copy(alpha = 0.4f),
                    trackColor = primaryColor.copy(alpha = 0.05f)
                )
            }
        }
    }
}
