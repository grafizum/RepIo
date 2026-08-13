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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.grafizum.repio.ui.screens.*
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.ui.viewmodel.getTodayDayOfWeek
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.grafizum.repio.data.AppDatabase
import com.grafizum.repio.data.WorkoutRepository
import com.grafizum.repio.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.jetbrains.compose.resources.painterResource
import repio.composeapp.generated.resources.Res
import repio.composeapp.generated.resources.ic_easter_egg

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home") { override val icon = Icons.Default.Home }
    object MyWorkout : Screen("my_workout", "My Workout") { override val icon = Icons.Default.FitnessCenter }
    object Analytics : Screen("analytics", "Analytics") { override val icon = Icons.Default.Assessment }
    object Settings : Screen("settings", "Settings") { override val icon = Icons.Default.Settings }
    object EasterEgg : Screen("easter_egg", "Creator") { override val drawableRes = Res.drawable.ic_easter_egg }
    object Onboarding : Screen("onboarding", "Welcome") { override val icon = Icons.Default.Home }

    open val icon: ImageVector? = null
    open val drawableRes: org.jetbrains.compose.resources.DrawableResource? = null
}

open class MainActivity : AppCompatActivity() {

    private val viewModel: WorkoutViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(applicationContext)
                val repository = WorkoutRepository(db.workoutDao())
                return WorkoutViewModel(repository) as T
            }
        }
    }

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

            MainAppStructure(viewModel = viewModel)
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
    val themeOption by viewModel.themeOption.collectAsState()
    val isDeveloper = themeOption.uppercase() == "DEVELOPER"
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showExitConfirmation by remember { mutableStateOf(false) }

    var easterEggUnlocked by remember { mutableStateOf(false) }
    var navSequence by remember { mutableStateOf(emptyList<Int>()) }
    var lastInteractionTime by remember { mutableStateOf(0L) }

    val baseNavigationItems = listOf(
        Screen.Home,
        Screen.MyWorkout,
        Screen.Analytics,
        Screen.Settings
    )

    val navigationItems = if (easterEggUnlocked) {
        baseNavigationItems + Screen.EasterEgg
    } else {
        baseNavigationItems
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { navigationItems.size }
    )

    // Auto-scroll to Easter Egg tab when unlocked
    LaunchedEffect(easterEggUnlocked) {
        if (easterEggUnlocked) {
            // Use scrollToPage for "instant" snap as requested
            pagerState.scrollToPage(navigationItems.size - 1)
        }
    }

    // Auto-close logic for Easter Egg tab
    LaunchedEffect(pagerState.currentPage, easterEggUnlocked) {
        if (easterEggUnlocked) {
            val isEasterEggTab = navigationItems.getOrNull(pagerState.currentPage) == Screen.EasterEgg
            if (!isEasterEggTab) {
                // User navigated away from the easter egg tab
                delay(3000)
                // If they are still not on the tab after 3 seconds, lock it
                val stillNotOnTab = navigationItems.getOrNull(pagerState.currentPage) != Screen.EasterEgg
                if (stillNotOnTab) {
                    easterEggUnlocked = false
                    navSequence = emptyList() // Clear sequence when locking
                    // If the current page index is now out of bounds (because the list shrunk),
                    // move to the last available index (Settings)
                    if (pagerState.currentPage >= baseNavigationItems.size) {
                        pagerState.scrollToPage(baseNavigationItems.size - 1)
                    }
                }
            }
        }
    }

    // Auto-select today's workout when navigating to the Workout tab (via click or swipe)
    LaunchedEffect(pagerState.currentPage) {
        val currentTime = System.currentTimeMillis()

        // Timeout check (5 seconds)
        if (currentTime - lastInteractionTime > 5000) {
            navSequence = emptyList()
        }

        if (navigationItems[pagerState.currentPage] == Screen.MyWorkout) {
            viewModel.selectDayForWorkoutNow(getTodayDayOfWeek())
        }

        // Track sequence for Settings (index 3) and Analytics (index 2)
        val currentIndex = baseNavigationItems.indexOf(navigationItems[pagerState.currentPage])
        if (currentIndex == 2 || currentIndex == 3) {
            if (navSequence.lastOrNull() != currentIndex) {
                navSequence = (navSequence + currentIndex).takeLast(5)
                lastInteractionTime = currentTime
            }
        } else {
            // Reset if they go to Home or MyWorkout
            navSequence = emptyList()
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
    WorkoutTrackerTheme(themeOption = themeOption, pageIndex = if (isFirstTimeUser) -1 else pagerState.currentPage) {
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
                        containerColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isDeveloper) 0.dp else 3.dp
                    ) {
                        navigationItems.forEachIndexed { index, screen ->
                            val isSelected = pagerState.currentPage == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    val currentTime = System.currentTimeMillis()

                                    if (screen == Screen.Settings) {
                                        // Easter egg trigger: Settings (3) -> Analytics (2) -> Settings (3) -> Analytics (2) -> Settings (3)
                                        // and then press Settings button
                                        val isCorrectSequence = navSequence == listOf(3, 2, 3, 2, 3) || 
                                                               (navSequence.size == 4 && navSequence == listOf(3, 2, 3, 2) && pagerState.currentPage == 3)
                                        
                                        if (currentTime - lastInteractionTime <= 5000 && isCorrectSequence) {
                                            easterEggUnlocked = true
                                        }
                                    }

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
                                    if (screen.icon != null) {
                                        Icon(
                                            imageVector = screen.icon!!,
                                            contentDescription = screen.title
                                        )
                                    } else if (screen.drawableRes != null) {
                                        Icon(
                                            painter = painterResource(screen.drawableRes!!),
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = if (isDeveloper) {
                                    NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.White,
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                                    )
                                } else {
                                    NavigationBarItemDefaults.colors()
                                },
                                modifier = Modifier
                                    .clip(if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            if (isDeveloper) Color.White.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        } else Color.Transparent
                                    )
                                    .padding(vertical = if (isDeveloper) 4.dp else 0.dp)
                                    .semantics { testTag = "nav_tab_${screen.route}" }
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

                    is Screen.EasterEgg -> {
                        EasterEggScreen(viewModel = viewModel)
                    }

                    else -> {}
                }
            }
        }
    }
}
}

