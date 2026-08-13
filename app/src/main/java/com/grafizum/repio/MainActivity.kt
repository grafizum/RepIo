package com.grafizum.repio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.grafizum.repio.ui.screens.AnalyticsScreen
import com.grafizum.repio.ui.screens.FirstTimeSetupScreen
import com.grafizum.repio.ui.screens.HomeScreen
import com.grafizum.repio.ui.screens.MyWorkoutTabScreen
import com.grafizum.repio.ui.screens.SettingsScreen
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.ui.viewmodel.getTodayDayOfWeek
import com.grafizum.repio.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object MyWorkout : Screen("my_workout", "My Workout", Icons.Default.FitnessCenter)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Assessment)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Onboarding : Screen("onboarding", "Welcome", Icons.Default.Home)
}

open class MainActivity : AppCompatActivity() {

    private val viewModel: WorkoutViewModel by viewModels()

    private val _navTarget = MutableStateFlow<String?>(null)
    val navTarget: StateFlow<String?> = _navTarget.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var isReady by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Create notification channel for rest timer
        NotificationHelper.createNotificationChannel(this)

        // Handle initial intent
        handleIntent(intent)

        setContent {
            val themeOption by viewModel.themeOption.collectAsState()
            val keepScreenOn by viewModel.keepScreenOn.collectAsState()

            SideEffect {
                isReady = true
            }

            val context = LocalContext.current
            DisposableEffect(keepScreenOn) {
                val activity = context as? Activity
                if (keepScreenOn) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            WorkoutTrackerTheme(themeOption = themeOption) {
                MainAppStructure(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("TARGET_SCREEN")?.let {
            _navTarget.value = it
            intent.removeExtra("TARGET_SCREEN")
        }
    }

    fun clearNavTarget() {
        _navTarget.value = null
    }
}

@Composable
fun MainAppStructure(viewModel: WorkoutViewModel) {
    val isFirstTimeUser by viewModel.isFirstTimeUser.collectAsState()
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showExitConfirmation by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        Screen.Home,
        Screen.MyWorkout,
        Screen.Analytics,
        Screen.Settings
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { navigationItems.size }
    )

    // Auto-select today's workout when navigating to the Workout tab (via click or swipe)
    LaunchedEffect(pagerState.currentPage) {
        if (navigationItems[pagerState.currentPage] == Screen.MyWorkout) {
            viewModel.selectDayForWorkoutNow(getTodayDayOfWeek())
        }
    }

    // System Back Button Handling
    BackHandler(enabled = !isFirstTimeUser) {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            showExitConfirmation = true
        }
    }

    if (showExitConfirmation) {
        val activityContext = context.findActivity()
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(stringResource(R.string.exit_app_title)) },
            text = { Text(stringResource(R.string.exit_app_message)) },
            confirmButton = {
                Button(
                    onClick = { activityContext?.finish() }
                ) {
                    Text(stringResource(R.string.exit))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitConfirmation = false }
                ) {
                    Text(stringResource(R.string.stay))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Observe lifecycle events to check rest timer
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val activity = context as? MainActivity
    val navTarget by activity?.navTarget?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(navTarget) {
        navTarget?.let { route ->
            val pageIndex = navigationItems.indexOfFirst { it.route == route }
            if (pageIndex != -1) {
                pagerState.animateScrollToPage(pageIndex)
            } else {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            activity?.clearNavTarget()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    viewModel.tickTimer()
                    viewModel.refreshTodaySelection()
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.checkTimerOnAppClose()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Force Onboarding screen if it's the first time
    if (isFirstTimeUser) {
        FirstTimeSetupScreen(
            viewModel = viewModel,
            onComplete = {
                // ViewModel updates isFirstTimeUser, which triggers recomposition
            },
            onCancel = {
                viewModel.setRequestedWorkoutSubTab(1) // Set sub-tab to "Routine"
                viewModel.cancelFirstTimeSetup()
                coroutineScope.launch {
                    pagerState.scrollToPage(1) // Set main pager to "My Workout"
                }
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    navigationItems.forEachIndexed { index, screen ->
                        val isSelected = pagerState.currentPage == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (screen == Screen.MyWorkout) {
                                    viewModel.setRequestedWorkoutSubTab(0) // Default to "Workout Now" on tab click
                                    viewModel.selectDayForWorkoutNow(getTodayDayOfWeek())
                                }
                                if (pagerState.currentPage != index) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.semantics { testTag = "nav_tab_${screen.route}" }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                beyondViewportPageCount = 3
            ) { pageIndex ->
                when (navigationItems[pageIndex]) {
                    is Screen.Home -> {
                        val navigateToTab: (String) -> Unit = { route ->
                            val targetIndex = navigationItems.indexOfFirst { it.route == route }
                            if (targetIndex != -1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetIndex)
                                }
                            }
                        }

                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToWorkoutNow = { 
                                viewModel.setRequestedWorkoutSubTab(0)
                                navigateToTab(Screen.MyWorkout.route) 
                            }
                        )
                    }

                    is Screen.MyWorkout -> {
                        MyWorkoutTabScreen(
                            viewModel = viewModel,
                            onBack = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }

                    is Screen.Analytics -> {
                        AnalyticsScreen(
                            viewModel = viewModel
                        )
                    }

                    is Screen.Settings -> {
                        SettingsScreen(viewModel = viewModel)
                    }

                    else -> {}
                }
            }
        }
    }
}

