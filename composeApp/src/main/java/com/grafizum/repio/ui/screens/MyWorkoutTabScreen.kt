package com.grafizum.repio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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

@Composable
fun WorkoutTabScreen(
    viewModel: WorkoutViewModel,
    onBack: (() -> Unit)? = null,
    initialTab: Int = 0,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val requestedTab by viewModel.requestedWorkoutSubTab.collectAsState()
    var selectedSubTab by remember { mutableIntStateOf(initialTab) }
    var showAddDialogOnLoad by remember { mutableStateOf(false) }

    // Sync internal tab state with external requests (e.g., from Home or Onboarding)
    LaunchedEffect(requestedTab) {
        selectedSubTab = requestedTab
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

    AnimatedContent(
        targetState = selectedSubTab,
        transitionSpec = {
            (fadeIn(animationSpec = tween(500, delayMillis = 100)) + 
             scaleIn(initialScale = 0.92f, animationSpec = tween(500, delayMillis = 100)))
                .togetherWith(fadeOut(animationSpec = tween(200)))
        },
        label = "WorkoutTabTransition",
        modifier = modifier.fillMaxSize()
    ) { targetTab ->
        if (targetTab == 0) {
            WorkoutNowScreen(
                viewModel = viewModel,
                onBack = onBack,
                onChangeRoutine = { selectedSubTab = 1 },
                isActive = isActive
            )
        } else {
            WorkoutScreen(
                viewModel = viewModel,
                onBack = { selectedSubTab = 0 },
                initialShowAddDialog = showAddDialogOnLoad,
                onAddDialogDismissed = { showAddDialogOnLoad = false },
                onCustomClick = { selectedSubTab = 0 }
            )
        }
    }
}
