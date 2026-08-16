package com.grafizum.repio

import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Entry point for the application UI.
 * Note: MainActivity.kt also contains a MainAppStructure which is currently the active one used by the Android app.
 */
@Composable
fun App(viewModel: WorkoutViewModel) {
    MainAppStructure(viewModel = viewModel)
}
