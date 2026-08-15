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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grafizum.repio.util.getString

import org.jetbrains.compose.resources.painterResource
import repio.composeapp.generated.resources.Res
import repio.composeapp.generated.resources.ic_easter_egg

import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel

@Composable
fun EasterEggScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val developerThemeEnabled by viewModel.developerThemeEnabled.collectAsState()
    val isDeveloper = LocalDeveloperSkin.current
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
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
                .height(64.dp)
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_easter_egg),
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
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_easter_egg),
                contentDescription = null,
                tint = colorRotation,
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

    // MESSAGE CARD
    Card(
        shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primaryContainer
        ),
        border = if (isDeveloper) BorderStroke(2.dp, Color.White) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(if (isDeveloper) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "You found my easter egg, Hi!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
                color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "I'm Matrix. This is my first-ever app made for the public, and you found my easter egg, Thank you so much for using my app! I wonder what app I will create next ^^",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                textAlign = TextAlign.Center,
                color = if (isDeveloper) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // REWARD BUBBLE
            Surface(
                color = if (isDeveloper) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondaryContainer,
                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = if (isDeveloper) 1.dp else 1.dp,
                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = "As a reward, you get a special theme!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
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
                    .clip(if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp))
                    .background(if (isDeveloper) Color.Black else MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (isDeveloper) 1.dp else 0.dp,
                        color = if (isDeveloper) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Unlock Developer Theme",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDeveloper) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = developerThemeEnabled,
                    onCheckedChange = { viewModel.setDeveloperThemeEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                        checkedTrackColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.outline
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
}
