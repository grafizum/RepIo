package com.example

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.FirstTimeSetupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyWorkoutTabScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.theme.WorkoutTrackerTheme
import com.example.ui.viewmodel.WorkoutViewModel
import com.example.util.NotificationHelper
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object MyWorkout : Screen("my_workout", "My Workout", Icons.Default.FitnessCenter)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Assessment)
    object Support : Screen("support", "Support", Icons.Default.SupportAgent)
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

        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

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

    val navigationItems = listOf(
        Screen.Home,
        Screen.MyWorkout,
        Screen.Analytics,
        Screen.Support
    )

    val pagerState = rememberPagerState(pageCount = { navigationItems.size })

    // Observe lifecycle events to check rest timer
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
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
                viewModel.cancelFirstTimeSetup()
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
                            onNavigateToWorkoutNow = { navigateToTab(Screen.MyWorkout.route) },
                            onNavigateToRoutine = { navigateToTab(Screen.MyWorkout.route) },
                            onNavigateToCharts = { navigateToTab(Screen.Analytics.route) },
                            onNavigateToWeeklyReport = { navigateToTab(Screen.Analytics.route) },
                            onNavigateToSupport = { navigateToTab(Screen.Support.route) }
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

                    is Screen.Support -> {
                        SupportScreen()
                    }

                    else -> {}
                }
            }
        }
    }
}
