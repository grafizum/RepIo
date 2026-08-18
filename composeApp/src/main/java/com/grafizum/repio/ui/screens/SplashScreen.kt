package com.grafizum.repio.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AppSplashScreen(onComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        delay(1600) 
        visible = false
        delay(400)
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
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600)),
                exit = fadeOut(tween(400))
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

            // Subtle Loading Indicator
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 400)),
                exit = fadeOut(tween(200))
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
