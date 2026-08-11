package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.WorkoutViewModel

@Composable
fun MyWorkoutTabScreen(
    viewModel: WorkoutViewModel,
    onBack: (() -> Unit)? = null,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(initialTab) }
    var showAddDialogOnLoad by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (selectedSubTab == 0) {
            WorkoutNowScreen(
                viewModel = viewModel,
                onBack = onBack,
                onChangeRoutine = { selectedSubTab = 1 }
            )
        } else {
            RoutineScreen(
                viewModel = viewModel,
                onBack = { selectedSubTab = 0 },
                onNavigateToWorkoutNow = { day ->
                    viewModel.selectDayForWorkoutNow(day)
                    selectedSubTab = 0
                },
                initialShowAddDialog = showAddDialogOnLoad,
                onAddDialogDismissed = { showAddDialogOnLoad = false },
                onCustomClick = { selectedSubTab = 0 }
            )
        }
    }
}
