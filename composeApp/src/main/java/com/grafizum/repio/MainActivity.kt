package com.grafizum.repio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.grafizum.repio.ui.screens.*
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getTodayDayOfWeek
import com.grafizum.repio.util.getString
import com.google.android.play.core.appupdate.AppUpdateInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.grafizum.repio.data.AppDatabase
import com.grafizum.repio.data.WorkoutRepository
import com.grafizum.repio.util.AppUpdateChecker
import com.grafizum.repio.util.NotificationHelper
import com.grafizum.repio.ui.components.AdaptiveWidthContainer
import com.grafizum.repio.ui.components.LocalWindowSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.grafizum.repio.R


fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home") { override val icon = Icons.Default.Home }
    object Workout : Screen("workout", "Workout") { override val icon = Icons.Default.FitnessCenter }
    object Analytics : Screen("analytics", "Analytics") { override val icon = Icons.Default.Assessment }
    object Settings : Screen("settings", "Settings") { override val icon = Icons.Default.Settings }
    object EasterEgg : Screen("easter_egg", "Creator") { override val drawableRes = R.drawable.ic_easter_egg }
    object Onboarding : Screen("onboarding", "Welcome") { override val icon = Icons.Default.Home }

    open val icon: ImageVector? = null
    open val drawableRes: Int? = null
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

    // Set when the app is (re)opened via the "update available" notification,
    // so the Play update flow can start as soon as the UI is ready.
    private val _startUpdateFlow = MutableStateFlow(false)
    val startUpdateFlowOnResume: StateFlow<Boolean> = _startUpdateFlow.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lock phones to portrait. Tablets (>=600dp smallest screen width) keep full
        // sensor rotation so they can also be used in landscape.
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Create notification channel for rest timer
        NotificationHelper.createNotificationChannel(this)

        // Handle initial intent
        handleIntent(intent)

        // Sync theme preference with the launched activity variant to prevent startup conflicts
        val launchedActivityName = this::class.java.name
        val themeFromActivity = when (launchedActivityName) {
            "com.grafizum.repio.MainActivityPurple" -> "PURPLE"
            "com.grafizum.repio.MainActivityBeige" -> "BEIGE"
            "com.grafizum.repio.MainActivityFlame" -> "FLAME"
            "com.grafizum.repio.MainActivityEmerald" -> "EMERALD"
            "com.grafizum.repio.MainActivityCrimson" -> "CRIMSON"
            "com.grafizum.repio.MainActivityAzure" -> "AZURE"
            "com.grafizum.repio.MainActivityOcean" -> "OCEAN"
            "com.grafizum.repio.MainActivityRose" -> "ROSE"
            "com.grafizum.repio.MainActivityRetro" -> "DEVELOPER"
            else -> null
        }
        themeFromActivity?.let { viewModel.updateThemeOptionSilent(it) }

        setContent {
            val themeOption by viewModel.themeOption.collectAsState()
            val keepScreenOn by viewModel.keepScreenOn.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()

            LaunchedEffect(appLanguage) {
                val targetLocales = if (appLanguage == "system") {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(appLanguage)
                }
                
                if (AppCompatDelegate.getApplicationLocales() != targetLocales) {
                    AppCompatDelegate.setApplicationLocales(targetLocales)
                }
            }

            LaunchedEffect(Unit) {
                delay(200) // Small warm up to avoid black flicker
            }

            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            // Update foreground state
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> com.grafizum.repio.util.AppEventBus.setAppForeground(true)
                        Lifecycle.Event.ON_STOP -> {
                            com.grafizum.repio.util.AppEventBus.setAppForeground(false)
                            // Timestamp the last use so the background check can tell
                            // how many days the app has been sitting idle.
                            com.grafizum.repio.util.MotivationNudger.recordAppUsage(context)
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    com.grafizum.repio.util.AppEventBus.setAppForeground(false)
                }
            }

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
        if (intent?.getBooleanExtra("START_UPDATE_FLOW", false) == true) {
            _startUpdateFlow.value = true
            intent.removeExtra("START_UPDATE_FLOW")
        }
    }

    fun clearNavTarget() {
        _navTarget.value = null
    }

    fun clearStartUpdateFlow() {
        _startUpdateFlow.value = false
    }
}

@Composable
fun MainAppStructure(viewModel: WorkoutViewModel) {
    val isFirstTimeUser by viewModel.isFirstTimeUser.collectAsState()
    // Collected here as well as in the composition root, which owns the
    // setApplicationLocales effect: this one only feeds the walkthrough's language
    // button, so the button's label follows the choice immediately.
    val appLanguage by viewModel.appLanguage.collectAsState()
    val themeOption by viewModel.themeOption.collectAsState()
    val isDeveloper = themeOption.uppercase() == "DEVELOPER"
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val window = LocalWindowSize.current
    val motivationalNudges by viewModel.motivationalNudges.collectAsState()

    // The skip nudge is delivered by a daily background alarm (only while the app
    // is closed and has been unused for days). Opening the app records the visit,
    // clears any waiting nudge and re-arms/removes the alarm with the setting.
    LaunchedEffect(motivationalNudges) {
        com.grafizum.repio.util.MotivationNudger.onAppOpened(context)
    }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showInAppRestNotification by remember { mutableStateOf(false) }

    var easterEggUnlocked by remember { mutableStateOf(false) }
    var navSequence by remember { mutableStateOf(emptyList<Int>()) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    val baseNavigationItems = listOf(
        Screen.Home,
        Screen.Workout,
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

        if (navigationItems[pagerState.currentPage] == Screen.Workout) {
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
                pagerState.animateScrollToPage(0, animationSpec = tween(500))
            }
        } else {
            showExitConfirmation = true
        }
    }

    // Update current route for AppEventBus
    LaunchedEffect(pagerState.currentPage, navigationItems) {
        val currentRoute = navigationItems.getOrNull(pagerState.currentPage)?.route
        com.grafizum.repio.util.AppEventBus.setCurrentRoute(currentRoute)
    }

    // Listen for rest timer events
    LaunchedEffect(Unit) {
        com.grafizum.repio.util.AppEventBus.restTimerEnded.collect {
            // Show custom notification ONLY if not in Workout tab
            val currentRoute = com.grafizum.repio.util.AppEventBus.currentRoute.value
            if (currentRoute != Screen.Workout.route) {
                showInAppRestNotification = true
            }
        }
    }

    // ---------------- Update Nudge (Play In-App Updates, flexible flow) ----------------
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? MainActivity

    // Latest update info Play reported during this session (null = nothing pending).
    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    // Set when the user asks to update (snackbar action or notification tap).
    var requestedUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    // True once the user has dealt with the nudge during this session.
    var updateNudgeHandled by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Fresh session: clear any stale "update available" notification from the tray.
    LaunchedEffect(Unit) {
        NotificationHelper.cancelUpdateNotification(context)
    }

    val updateAvailableMessage = stringResource(R.string.update_available_message)
    val updateNowLabel = stringResource(R.string.update_now)

    // In-app snackbar nudge + persistent tray notification for a pending update.
    suspend fun nudgeUpdate() {
        if (updateNudgeHandled) return
        NotificationHelper.showUpdateAvailableNotification(context)
        if (snackbarHostState.currentSnackbarData != null) return
        val result = snackbarHostState.showSnackbar(
            message = updateAvailableMessage,
            actionLabel = updateNowLabel,
            duration = SnackbarDuration.Short
        )
        updateNudgeHandled = true
        AppUpdateChecker.markNudgeDismissed(context)
        if (result == SnackbarResult.ActionPerformed) {
            requestedUpdateInfo = pendingUpdateInfo
        }
    }

    // Begin the flexible update via the Play dialog when the user taps the snackbar
    // action or opens the app from the "update available" notification.
    LaunchedEffect(requestedUpdateInfo) {
        requestedUpdateInfo?.let { info ->
            NotificationHelper.cancelUpdateNotification(context)
            activity?.let { AppUpdateChecker.startFlexibleUpdate(it, info) }
            requestedUpdateInfo = null
        }
    }

    // Check Play whenever the app is foregrounded (also covers returning after the
    // user backed out of the Play dialog, which Play marks as CANCELED). Skips
    // silently when offline, within the cooldown, or when no update exists.
    //
    // repeatOnLifecycle rather than an ON_RESUME observer that launches into the
    // composition's scope: that scope is cancelled whenever the composition restarts
    // (the splash screen hands over to the app), which silently threw the check away on
    // the very launch that mattered. This re-runs on every resume instead, and is only
    // cancelled when the Activity goes away.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val info = AppUpdateChecker.checkForUpdate(context) ?: return@repeatOnLifecycle
            pendingUpdateInfo = info
            nudgeUpdate()
        }
    }

    // Opened via the "update available" notification -> go straight to the Play dialog.
    val startUpdateRequested by (activity?.startUpdateFlowOnResume?.collectAsState()
        ?: remember { mutableStateOf(false) })
    LaunchedEffect(startUpdateRequested) {
        if (startUpdateRequested) {
            activity?.clearStartUpdateFlow()
            AppUpdateChecker.checkForUpdate(context, ignoreCooldown = true)?.let { info ->
                updateNudgeHandled = true
                pendingUpdateInfo = info
                requestedUpdateInfo = info
            }
        }
    }

    // Re-nudge later in the same session as soon as connectivity returns,
    // if Play already told us an update was waiting.
    DisposableEffect(lifecycleOwner) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Only surface the nudge once per session; Play is re-queried on next open.
                if (AppUpdateChecker.isWithinCooldown(context)) return
                if (pendingUpdateInfo != null) return
                if (snackbarHostState.currentSnackbarData != null) return

                coroutineScope.launch {
                    val info = AppUpdateChecker.checkForUpdate(context) ?: return@launch
                    pendingUpdateInfo = info
                    nudgeUpdate()
                }
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Connectivity nudging is best-effort only.
        }

        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
                // Callback may already be unregistered.
            }
        }
    }

    // ------------------------------------------------------------------------------

    if (showExitConfirmation) {
        val activityContext = context.findActivity()
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(stringResource(R.string.exit_app_title))
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(stringResource(R.string.exit_app_message))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        activityContext?.finish()
                    }
                ) {
                    Text(stringResource(R.string.exit))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        showExitConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.stay))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Observe lifecycle events to check rest timer
    val navTarget by activity?.navTarget?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(navTarget) {
        navTarget?.let { route ->
            val pageIndex = navigationItems.indexOfFirst { it.route == route }
            if (pageIndex != -1) {
                pagerState.animateScrollToPage(pageIndex, animationSpec = tween(500))
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
                    // Keep the background timer armed: it is what counts the rest down and
                    // alerts when the app is closed.
                    viewModel.ensureRestTimerRunning()
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
        var showAppSplash by remember { mutableStateOf(true) }
        // Observed as state rather than read once through a plain accessor: the
        // dismissal has to recompose this branch away, which only happens when
        // the flag is collected. Reading it as a value left the walkthrough on
        // screen forever, because nothing else changed after the tap.
        val showLevelGuide by viewModel.shouldShowLevelGuide.collectAsState()

        if (showAppSplash) {
            AppSplashScreen(onComplete = { showAppSplash = false })
        } else if (showLevelGuide) {
            // One-time level-guide gate, shown once per install and before the
            // welcome setup: it announces the level system, then dismissing it
            // hands the user straight to the workout chooser. Its description
            // of the Easy/Medium/Hard choice is written in the future tense,
            // since that choice is still ahead of them on a fresh install.
            LevelGuideOnboardingScreen(
                onDismiss = { viewModel.markLevelGuideSeen() },
                // Wired to the same preference as Settings ▸ Language. The
                // setApplicationLocales effect above reacts to it, so a pick here
                // re-renders this very screen in the chosen language.
                appLanguage = appLanguage,
                onLanguageSelected = { viewModel.setAppLanguage(it) }
            )
        } else if (isFirstTimeUser) {
            // Adaptive shell: caps content width and centers it on tablets/foldables.
            AdaptiveWidthContainer {
                FirstTimeSetupScreen(
                    viewModel = viewModel,
                    onComplete = {
                        // ViewModel updates isFirstTimeUser, which triggers recomposition.
                        // The level guide then appears on the next recomposition with
                        // the chosen intensity already baked into every start level.
                    },
                    onCancel = {
                        viewModel.cancelFirstTimeSetup()
                        viewModel.setRequestedWorkoutSubTab(1) // Set sub-tab to "Workout"
                        coroutineScope.launch {
                            pagerState.scrollToPage(1) // Set main pager to "My Workout"
                        }
                    }
                )
            }
        } else {
            val focusManager = LocalFocusManager.current
            Scaffold(
                bottomBar = {
                    Surface(
                        tonalElevation = if (isDeveloper) 0.dp else 3.dp,
                        color = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val tabWidth = this.maxWidth / navigationItems.size
                            val density = LocalDensity.current

                            // Sliding Indicator Background
                            Surface(
                                modifier = Modifier
                                    .width(tabWidth)
                                    .height(80.dp) // Standard NavigationBar height
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                                    .graphicsLayer {
                                        val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                        translationX = with(density) { (pageOffset * tabWidth.toPx()) }
                                    },
                                color = if (isDeveloper) Color.White.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
                            ) {}

                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp // Elevation is handled by the parent Surface
                            ) {
                                navigationItems.forEachIndexed { index, screen ->
                                    val isSelected = pagerState.targetPage == index
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

                                            if (screen == Screen.Workout) {
                                                viewModel.setRequestedWorkoutSubTab(0) // Default to "Workout Now" on tab click
                                                viewModel.selectDayForWorkoutNow(getTodayDayOfWeek())
                                            }
                                            if (pagerState.currentPage != index) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index, animationSpec = tween(500))
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
                                            val labelKey = when(screen.route) {
                                                "home" -> "smart_workout_assistant"
                                                "workout" -> "program_name"
                                                "analytics" -> "analytics_reports"
                                                "settings" -> "settings"
                                                else -> null
                                            }
                                            Text(
                                                text = if (labelKey != null) getString(labelKey) else screen.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                // Narrow phones only have ~90dp per tab, so the label steps
                                                // down one size to show more of "Smart Workout Assistant"
                                                // instead of collapsing to "Smart Wo…".
                                                style = if (window.isNarrow) {
                                                    MaterialTheme.typography.labelSmall
                                                } else {
                                                    MaterialTheme.typography.labelMedium
                                                }
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color.Transparent,
                                            selectedIconColor = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary,
                                            selectedTextColor = if (isDeveloper) Color.White else MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = if (isDeveloper) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = if (isDeveloper) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .clip(if (isDeveloper) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                                            .padding(vertical = if (isDeveloper) 4.dp else 0.dp)
                                            .semantics { testTag = "nav_tab_${screen.route}" }
                                    )
                                }
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    AdaptiveWidthContainer {
                        when (navigationItems[pageIndex]) {
                        is Screen.Home -> {
                            val navigateToTab: (String) -> Unit = { route ->
                                val targetIndex = navigationItems.indexOfFirst { it.route == route }
                                if (targetIndex != -1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(targetIndex, animationSpec = tween(500))
                                    }
                                }
                            }

                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToWorkoutNow = { 
                                    viewModel.setRequestedWorkoutSubTab(0)
                                    navigateToTab(Screen.Workout.route) 
                                }
                            )
                        }

                        is Screen.Workout -> {
                            WorkoutTabScreen(
                                viewModel = viewModel,
                                isActive = pagerState.settledPage == pageIndex,
                                onBack = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0, animationSpec = tween(500))
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
                    } // AdaptiveWidthContainer
                }

                // Custom In-App Rest Timer Notification
                AnimatedVisibility(
                    visible = showInAppRestNotification,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeOut(animationSpec = tween(durationMillis = 500)),
                    modifier = Modifier
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                        .align(androidx.compose.ui.Alignment.TopCenter)
                ) {
                    Surface(
                        onClick = {
                            showInAppRestNotification = false
                            coroutineScope.launch {
                                val workoutIndex = navigationItems.indexOfFirst { it.route == Screen.Workout.route }
                                if (workoutIndex != -1) {
                                    pagerState.animateScrollToPage(workoutIndex, animationSpec = tween(500))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                            
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.notification_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.in_app_rest_ended_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Auto-hide after 2.5 seconds
                    LaunchedEffect(showInAppRestNotification) {
                        if (showInAppRestNotification) {
                            delay(2500)
                            showInAppRestNotification = false
                        }
                    }
                }
            }
        }
    }
}
}

