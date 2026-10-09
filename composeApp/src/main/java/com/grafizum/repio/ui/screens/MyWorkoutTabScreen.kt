package com.grafizum.repio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getString

@Composable
fun WorkoutTabScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    initialTab: Int = viewModel.requestedWorkoutSubTab.value.tab,
    isActive: Boolean = true
) {
    val requestedTab by viewModel.requestedWorkoutSubTab.collectAsState()
    var selectedSubTab by remember { mutableIntStateOf(initialTab) }
    var showAddDialogOnLoad by remember { mutableStateOf(false) }
    // The "Levels" header button: compact guide popup.
    var showLevelsGuide by remember { mutableStateOf(false) }

    // Sync internal tab state with external requests (e.g., from Home or Onboarding)
    LaunchedEffect(requestedTab) {
        selectedSubTab = requestedTab.tab
    }

    // Exit edit routine screen when leaving the Workout tab
    LaunchedEffect(isActive) {
        if (!isActive) {
            selectedSubTab = 0
            showAddDialogOnLoad = false
        }
    }

    // Handle back button for Routine Screen -> Workout Now Screen
    BackHandler(enabled = selectedSubTab != 0) {
        selectedSubTab = 0
    }

    // "Levels" guide popup: the same guide as the Settings expandable card.
    //
    // A plain Dialog rather than an AlertDialog, for two reasons that both used to
    // show in this popup:
    //   - AlertDialog's text slot pads 24dp horizontally but only 16dp vertically,
    //     so the guide sat in a narrow column with dead space either side.
    //   - AlertDialog sizes itself to the platform default width (~280dp), which is
    //     much too narrow for five example cards and a three-step graphic.
    // One even 20dp padding on all four sides, and a panel that takes more of the
    // window than before: the guide's example cards sit on the theme's tinted
    // surface rather than a flat grey, so they read as part of the active palette.
    if (showLevelsGuide) {
        val isDeveloper = LocalDeveloperSkin.current
        Dialog(
            onDismissRequest = { showLevelsGuide = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .widthIn(max = 640.dp)
                    .fillMaxHeight(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = getString("level_system_guide_title"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // weight(1f) so the guide takes the leftover height and scrolls
                    // inside it, while the title and the button stay put.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        LevelSystemGuideContent()
                    }

                    Button(
                        onClick = { showLevelsGuide = false },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(getString("done"))
                    }
                }
            }
        }
    }

    AnimatedContent(
        targetState = selectedSubTab,
        transitionSpec = {
            (fadeIn(animationSpec = tween(400)) + 
             scaleIn(initialScale = 0.95f, animationSpec = tween(400)))
                .togetherWith(fadeOut(animationSpec = tween(200)))
        },
        label = "WorkoutTabTransition",
        modifier = modifier.fillMaxSize()
    ) { targetTab ->
        when (targetTab) {
            0 -> WorkoutNowScreen(
                viewModel = viewModel,
                onBack = onBack,
                onChangeRoutine = { selectedSubTab = 1 },
                onAdjustLevels = { showLevelsGuide = true },
                isActive = isActive
            )
            else -> WorkoutScreen(
                viewModel = viewModel,
                onBack = { selectedSubTab = 0 },
                initialShowAddDialog = showAddDialogOnLoad,
                onAddDialogDismissed = { showAddDialogOnLoad = false },
                onCustomClick = { selectedSubTab = 0 }
            )
        }
    }
}
