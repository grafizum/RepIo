package com.grafizum.repio.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grafizum.repio.util.getString

import androidx.compose.ui.res.painterResource
import com.grafizum.repio.R


import com.grafizum.repio.BuildConfig
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel

@Composable
fun EasterEggScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val developerThemeEnabled by viewModel.developerThemeEnabled.collectAsState()
    val isDeveloper = LocalDeveloperSkin.current
    val window = LocalWindowSize.current
    val infiniteTransition = rememberInfiniteTransition(label = "EasterEgg")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    val colorRotation by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary,
        targetValue = MaterialTheme.colorScheme.tertiary,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Color"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = window.screenPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = window.headerMinHeight)
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_easter_egg),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Easter Egg",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ICON CIRCLE
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_easter_egg),
                    contentDescription = null,
                    tint = colorRotation,
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // MESSAGE CARD
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You found my easter egg, Hi!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "I'm Matrix. This is my first-ever app made for the public, and you found my easter egg, Thank you so much for using my app! I wonder what app I will create next ^^",
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))

                    Spacer(modifier = Modifier.height(16.dp))

                    // REWARD BUBBLE
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "As a reward, you get a special theme!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // DEVELOPER THEME SWITCH
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 0.dp,
                                color = Color.Transparent,
                                // Matches the 16dp clip above it: a square border inside a
                                // rounded clip only loses its corners.
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            // Keeps the switch at its tap-target size; the label wraps instead.
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Unlock Developer Theme",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = developerThemeEnabled,
                            onCheckedChange = { viewModel.setDeveloperThemeEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "✨ Your favorite developer ✨",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // VERSION LABEL
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp)
        )

        // BY LABEL
        Text(
            text = "by Grafizum",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        )
    }
}
